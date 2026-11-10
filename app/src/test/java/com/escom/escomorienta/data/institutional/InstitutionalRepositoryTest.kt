package com.escom.escomorienta.data.institutional

import com.escom.escomorienta.domain.institutional.ResourceType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class InstitutionalRepositoryTest {
    private val parser = InstitutionalJsonParser()

    @Test fun `parses a complete resource and orders steps`() {
        val resource = parser.parseResource(resourceJson("constancia_estudios", "Constancia de estudios", aliases = "[\"constancia\"]", steps = """
            [{"order":2,"title":"Recoger"},{"order":1,"title":"Solicitar","description":"Iniciar solicitud"}]
        """))
        assertEquals(ResourceType.PROCEDURE, resource.type)
        assertEquals(listOf(1, 2), resource.steps.map { it.order })
        assertEquals("constancia", resource.aliases.single())
    }

    @Test fun `optional lists default to empty`() {
        val resource = parser.parseResource("""{"id":"minimal","type":"INFORMATION","category":"General","name":"Mínimo"}""")
        assertTrue(resource.aliases.isEmpty())
        assertTrue(resource.documents.isEmpty())
        assertTrue(resource.steps.isEmpty())
    }

    @Test fun `parses structured documents as display text`() {
        val resource = parser.parseResource("""
            {"id":"documented","type":"PROCEDURE","category":"General","name":"Documentado",
             "documents":[{"id":"letter","name":"Carta de solicitud","required":true,"description":"Dirigida al Director."}]}
        """.trimIndent())
        assertEquals("Carta de solicitud — Dirigida al Director.", resource.documents.single())
    }

    @Test fun `preserves independent request channels`() {
        val resource = parser.parseResource("""
            {"id":"multi","type":"PROCEDURE","category":"General","name":"Multicanal",
             "channels":[
               {"id":"saes","type":"ONLINE_PLATFORM","name":"SAES","recommended":true,
                "estimatedTime":{"type":"VARIABLE","displayText":"Puede procesarse rápidamente."},
                "steps":[{"order":1,"description":"Ingresar al SAES."}]},
               {"id":"office","type":"IN_PERSON","name":"Gestión Escolar",
                "requirements":["Presentar identificación."]},
               {"id":"web","type":"WEB_FORM","name":"Portal ESCOM","url":"https://example.com"}
             ]}
        """.trimIndent())
        assertEquals(3, resource.channels.size)
        assertTrue(resource.channels.first().recommended)
        assertEquals("Ingresar al SAES.", resource.channels.first().steps.single().title)
        assertEquals("https://example.com", resource.channels.last().url)
    }

    @Test fun `searches by name alias case and accents and returns empty`() = runBlocking {
        val repository = repositoryOf(
            resourceJson("gestion", "Gestión escolar", aliases = "[\"Control escolar\"]"),
            resourceJson("library", "Biblioteca")
        )
        assertEquals("gestion", repository.search("gestión").single().id)
        assertEquals("gestion", repository.search("CONTROL").single().id)
        assertEquals("gestion", repository.search("gestion").single().id)
        assertTrue(repository.search("inexistente").isEmpty())
    }

    @Test fun `gets resource by stable id`() = runBlocking {
        val repository = repositoryOf(resourceJson("stable_id", "Recurso"))
        assertEquals("Recurso", repository.getById("stable_id")?.name)
        assertNull(repository.getById("other"))
    }

    @Test fun `detects duplicate ids and keeps first`() {
        val files = mapOf(
            "institutional/manifest.json" to """{"version":1,"resources":["a.json","b.json"]}""",
            "institutional/a.json" to resourceJson("same", "Primero"),
            "institutional/b.json" to resourceJson("same", "Segundo")
        )
        val result = InstitutionalLocalDataSource({ files.getValue(it) }).load()
        assertEquals("Primero", result.resources.single().name)
        assertTrue(result.issues.any { "duplicado" in it.message })
    }

    @Test fun `reads multiple files from manifest and isolates invalid files`() {
        val files = mapOf(
            "institutional/manifest.json" to """{"version":1,"resources":["academic/a.json","services/b.json","bad.json"]}""",
            "institutional/academic/a.json" to resourceJson("a", "A"),
            "institutional/services/b.json" to resourceJson("b", "B"),
            "institutional/bad.json" to "not-json"
        )
        val result = InstitutionalLocalDataSource({ files.getValue(it) }).load()
        assertEquals(listOf("a", "b"), result.resources.map { it.id })
        assertEquals(1, result.issues.size)
    }

    @Test fun `loads complete production manifest with rich services`() {
        val root = File("src/main/assets/institutional")
        val manifestText = File(root, "manifest.json").readText(Charsets.UTF_8)
        assertFalse("manifest.json no debe contener diagonales invertidas", manifestText.contains("\\"))

        val result = InstitutionalLocalDataSource({ path ->
            val relative = path.removePrefix("institutional/")
            File(root, relative).readText(Charsets.UTF_8)
        }).load()
        assertTrue(result.issues.joinToString { it.message }, result.issues.isEmpty())
        assertEquals(46, result.resources.size)
        assertEquals(46, result.resources.map { it.id }.distinct().size)
        assertTrue(result.resources.first { it.id == "servicios_estudiantiles_atencion_salud" }.offerings.isNotEmpty())
        assertEquals(2, result.resources.first { it.id == "dae_reposicion_credencial_institucional" }.channels.size)
    }

    @Test fun `manifest parser normalizes backslashes to forward slashes`() {
        val paths = parser.parseManifest("""{"version":1,"resources":["academic\\test.json","services/other.json"]}""")
        assertEquals(listOf("academic/test.json", "services/other.json"), paths)
    }

    private fun repositoryOf(vararg resources: String): LocalInstitutionalRepository {
        val files = mutableMapOf("institutional/manifest.json" to
            """{"version":1,"resources":[${resources.indices.joinToString { "\"$it.json\"" }}]}""")
        resources.forEachIndexed { index, json -> files["institutional/$index.json"] = json }
        return LocalInstitutionalRepository(InstitutionalLocalDataSource({ files.getValue(it) }))
    }

    private fun resourceJson(id: String, name: String, aliases: String = "[]", steps: String = "[]") = """
        {"id":"$id","type":"PROCEDURE","category":"Académico","name":"$name",
         "shortDescription":"Descripción breve","aliases":$aliases,"keywords":[],"steps":$steps}
    """.trimIndent()
}
