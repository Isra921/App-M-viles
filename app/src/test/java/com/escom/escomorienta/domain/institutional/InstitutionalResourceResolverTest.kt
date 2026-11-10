package com.escom.escomorienta.domain.institutional

import com.escom.escomorienta.data.institutional.InstitutionalAssetReader
import com.escom.escomorienta.data.institutional.InstitutionalLocalDataSource
import com.escom.escomorienta.data.institutional.LocalInstitutionalRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InstitutionalResourceResolverTest {
    private val repository = productionRepository()
    private val resolver = RepositoryInstitutionalResourceResolver(repository)

    @Test fun `canonical names and controlled aliases resolve against single catalog`() = runBlocking {
        repository.getAll().forEach { resource ->
            val canonical = resolver.resolve(resource.canonicalName, null)
            assertEquals(resource.id, canonical.singleResourceId())
            resource.aliases.forEach { alias ->
                when (val result = resolver.resolve(alias, null)) {
                    is InstitutionalResourceResolution.Ambiguous ->
                        assertTrue("$alias must include ${resource.id}", result.candidates.any { it.id == resource.id })
                    else -> assertEquals("alias=$alias", resource.id, result.singleResourceId())
                }
            }
        }
    }

    @Test fun `specific terms beat generic ambiguous aliases`() = runBlocking {
        listOf("baja de materia", "quiero dar de baja una materia", "cómo doy de baja una materia").forEach { query ->
            assertEquals("gestion_escolar_baja_materia", resolver.resolve(query, null).singleResourceId())
        }
        val baja = resolver.resolve("baja", InstitutionalResourceExpectation.PROCEDURE)
            as InstitutionalResourceResolution.Ambiguous
        assertEquals(3, baja.candidates.size)
        assertEquals(
            setOf("gestion_escolar_baja_materia", "gestion_escolar_baja_temporal", "gestion_escolar_baja_definitiva"),
            baja.candidates.map { it.id }.toSet()
        )
    }

    @Test fun `constancia singular and plural remain explicit ambiguity`() = runBlocking {
        listOf("constancia", "constancias").forEach { query ->
            val result = resolver.resolve(query, InstitutionalResourceExpectation.PROCEDURE)
                as InstitutionalResourceResolution.Ambiguous
            assertEquals(4, result.candidates.size)
        }
    }

    @Test fun `medical and scholarship variants resolve canonical resources with context`() = runBlocking {
        listOf("servicio médico", "servicio medico", "médico", "medico").forEach { query ->
            assertEquals(
                "servicios_estudiantiles_atencion_salud",
                resolver.resolve(query, InstitutionalResourceExpectation.SERVICE_OR_SUPPORT).singleResourceId()
            )
        }
        listOf("beca", "becas").forEach { query ->
            assertEquals(
                "apoyos_educativos_becas",
                resolver.resolve(query, InstitutionalResourceExpectation.SERVICE_OR_SUPPORT).singleResourceId()
            )
        }
    }

    @Test fun `normalization is conservative and unknown or wrong type does not match`() = runBlocking {
        assertEquals("servicio medico", "  Servicio,  MÉDICO!! ".normalizeInstitutionalText())
        assertTrue(resolver.resolve("medicamento", null) is InstitutionalResourceResolution.NotFound)
        assertTrue(resolver.resolve("servicio médico", InstitutionalResourceExpectation.PROCEDURE) is InstitutionalResourceResolution.NotFound)
    }

    private fun InstitutionalResourceResolution.singleResourceId(): String? = when (this) {
        is InstitutionalResourceResolution.ExactMatch -> resource.id
        is InstitutionalResourceResolution.AliasMatch -> resource.id
        else -> null
    }

    private fun productionRepository(): InstitutionalRepository {
        val root = File("src/main/assets")
        return LocalInstitutionalRepository(InstitutionalLocalDataSource(InstitutionalAssetReader { path ->
            File(root, path).readText(Charsets.UTF_8)
        }))
    }
}
