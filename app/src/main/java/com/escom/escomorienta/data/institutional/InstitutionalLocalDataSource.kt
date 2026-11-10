package com.escom.escomorienta.data.institutional

import android.content.Context
import com.escom.escomorienta.domain.institutional.InstitutionalResource

fun interface InstitutionalAssetReader { fun read(path: String): String }

data class InstitutionalLoadIssue(val path: String, val message: String)
data class InstitutionalLoadResult(
    val resources: List<InstitutionalResource>,
    val issues: List<InstitutionalLoadIssue>
)

class InstitutionalLocalDataSource(
    private val assetReader: InstitutionalAssetReader,
    private val parser: InstitutionalJsonParser = InstitutionalJsonParser()
) {
    fun load(manifestPath: String = "institutional/manifest.json"): InstitutionalLoadResult {
        val paths = try {
            parser.parseManifest(assetReader.read(manifestPath))
        } catch (error: Exception) {
            return InstitutionalLoadResult(emptyList(), listOf(InstitutionalLoadIssue(manifestPath, error.message ?: "Error de lectura")))
        }
        val resources = mutableListOf<InstitutionalResource>()
        val issues = mutableListOf<InstitutionalLoadIssue>()
        paths.forEach { relativePath ->
            val normalizedRelative = relativePath.replace('\\', '/').trimStart('/')
            val path = "institutional/$normalizedRelative"
            runCatching { parser.parseResource(assetReader.read(path)) }
                .onSuccess(resources::add)
                .onFailure { issues += InstitutionalLoadIssue(path, it.message ?: "JSON inválido") }
        }
        resources.groupBy { it.id }.filterValues { it.size > 1 }.forEach { (id, duplicates) ->
            issues += InstitutionalLoadIssue(manifestPath, "ID duplicado '$id' (${duplicates.size} recursos); se conserva el primero")
        }
        return InstitutionalLoadResult(resources.distinctBy { it.id }, issues)
    }

    companion object {
        fun from(context: Context) = InstitutionalLocalDataSource(
            InstitutionalAssetReader { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
        )
    }
}
