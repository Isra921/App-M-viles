package com.escom.escomorienta.data.institutional

import android.util.Log
import com.escom.escomorienta.domain.institutional.*
import java.text.Normalizer

class LocalInstitutionalRepository(private val dataSource: InstitutionalLocalDataSource) : InstitutionalRepository {
    @Volatile private var cache: List<InstitutionalResource>? = null
    private fun resources(): List<InstitutionalResource> = cache ?: synchronized(this) {
        cache ?: dataSource.load().also { result ->
            result.issues.forEach { Log.e("InstitutionalData", "${it.path}: ${it.message}") }
            if (result.resources.isEmpty() && result.issues.isNotEmpty()) {
                throw IllegalStateException("No se pudo cargar el catálogo institucional: ${result.issues.first().message}")
            }
        }.resources.also { cache = it }
    }
    override suspend fun getAll() = resources()
    override suspend fun getById(id: String) = resources().firstOrNull { it.id == id }
    override suspend fun getByCategory(category: String) = resources().filter { it.category.equals(category, true) }
    override suspend fun search(query: String): List<InstitutionalResource> {
        val needle = query.normalized()
        if (needle.isBlank()) return resources()
        return resources().filter { resource ->
            sequenceOf(resource.name, resource.shortDescription).plus(resource.aliases).plus(resource.keywords)
                .any { needle in it.normalized() }
        }.sortedWith(compareBy<InstitutionalResource> { !it.name.normalized().startsWith(needle) }.thenBy { it.name })
    }
}

internal fun String.normalized(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "").lowercase().trim().replace("\\s+".toRegex(), " ")
