package com.escom.escomorienta.data.institutional

import com.escom.escomorienta.domain.institutional.InstitutionalMapActionValidator
import com.escom.escomorienta.domain.institutional.MapActionValidation
import com.escom.escomorienta.domain.location.LocationInfo
import com.escom.escomorienta.domain.location.LocationRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InstitutionalMapActionAuditTest {
    @Test fun `all production top-level map actions reference an existing map place`() = runBlocking {
        val assetRoot = File("src/main/assets")
        val mapIds = File(assetRoot, "maps").listFiles { file -> file.name.startsWith("places_") && file.extension == "json" }
            .orEmpty().flatMap { file ->
                val places = JSONObject(file.readText()).getJSONArray("places")
                (0 until places.length()).map { places.getJSONObject(it).getString("id") }
            }.toSet()
        val load = InstitutionalLocalDataSource(
            assetReader = InstitutionalAssetReader { path -> File(assetRoot, path).readText() }
        ).load()
        val validator = InstitutionalMapActionValidator(FakeLocations(mapIds))
        val invalid = load.resources.filter { resource ->
            resource.location?.mapLocationId != null && validator.validate(resource) != MapActionValidation.VALID
        }
        assertTrue(invalid.joinToString { "${it.id}:${it.location?.mapLocationId}" }, invalid.isEmpty())
        assertEquals(MapActionValidation.OFF_CAMPUS, validator.validate(load.resources.first { it.id == "dae_reposicion_credencial_institucional" }))
    }

    private class FakeLocations(private val ids: Set<String>) : LocationRepository {
        override fun search(query: String) = emptyList<LocationInfo>()
        override fun findMentioned(text: String) = emptyList<LocationInfo>()
        override fun findById(id: String) = id.takeIf(ids::contains)?.let { LocationInfo(it, it, "", "", "", "", it) }
    }
}
