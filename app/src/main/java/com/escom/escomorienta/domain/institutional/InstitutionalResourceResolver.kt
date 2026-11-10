package com.escom.escomorienta.domain.institutional

import java.text.Normalizer

/** Scope for institutional resource searches. SUPPORT remains a distinct resource type. */
enum class InstitutionalResourceExpectation { PROCEDURE, SERVICE_OR_SUPPORT }

sealed interface InstitutionalResourceResolution {
    data class ExactMatch(
        val resource: InstitutionalResource,
        val matchedCanonicalName: String
    ) : InstitutionalResourceResolution

    data class AliasMatch(
        val resource: InstitutionalResource,
        val matchedAlias: String
    ) : InstitutionalResourceResolution

    data class Ambiguous(
        val candidates: List<InstitutionalResource>,
        val matchedTerm: String
    ) : InstitutionalResourceResolution

    data object NotFound : InstitutionalResourceResolution
}

fun interface InstitutionalResourceResolver {
    suspend fun resolve(
        text: String,
        expectedType: InstitutionalResourceExpectation?
    ): InstitutionalResourceResolution
}

/**
 * Deterministic resolver over canonical names and controlled aliases. It deliberately performs
 * no fuzzy matching. Longer controlled terms win over generic aliases ("baja de materia" over
 * "baja"), while equal matches across resources remain ambiguous.
 */
class RepositoryInstitutionalResourceResolver(
    private val repository: InstitutionalRepository
) : InstitutionalResourceResolver {
    override suspend fun resolve(
        text: String,
        expectedType: InstitutionalResourceExpectation?
    ): InstitutionalResourceResolution {
        val normalizedText = text.normalizeInstitutionalText()
        if (normalizedText.isBlank()) return InstitutionalResourceResolution.NotFound

        val resources = repository.getAll().filter { resource ->
            when (expectedType) {
                InstitutionalResourceExpectation.PROCEDURE -> resource.type == ResourceType.PROCEDURE
                InstitutionalResourceExpectation.SERVICE_OR_SUPPORT ->
                    resource.type == ResourceType.SERVICE || resource.type == ResourceType.SUPPORT
                null -> resource.type != ResourceType.INFORMATION
            }
        }
        val matches = resources.flatMap { resource ->
            buildList {
                add(TermMatch(resource, resource.canonicalName, canonical = true))
                resource.aliases.forEach { add(TermMatch(resource, it, canonical = false)) }
            }.filter { it.normalizedTerm.isNotBlank() && normalizedText.containsWholeTerm(it.normalizedTerm) }
        }
        if (matches.isEmpty()) return InstitutionalResourceResolution.NotFound

        val longest = matches.maxOf { it.normalizedTerm.length }
        val strongest = matches.filter { it.normalizedTerm.length == longest }
        
        // Deduplicate candidates by resource ID to avoid multiple aliases of the same resource
        // creating artificial ambiguity.
        val candidates = strongest.distinctBy { it.resource.id }
        
        if (candidates.size > 1) {
            return InstitutionalResourceResolution.Ambiguous(
                candidates = candidates.map { it.resource }.sortedBy(InstitutionalResource::canonicalName),
                matchedTerm = strongest.first().term
            )
        }

        val match = candidates.single()
        return if (match.canonical && normalizedText == match.normalizedTerm) {
            InstitutionalResourceResolution.ExactMatch(match.resource, match.term)
        } else {
            InstitutionalResourceResolution.AliasMatch(match.resource, match.term)
        }
    }

    private data class TermMatch(
        val resource: InstitutionalResource,
        val term: String,
        val canonical: Boolean,
        val normalizedTerm: String = term.normalizeInstitutionalText()
    )
}

fun String.normalizeInstitutionalText(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase()
    .replace("[^\\p{L}\\p{N}]+".toRegex(), " ")
    .trim()
    .replace("\\s+".toRegex(), " ")

private fun String.containsWholeTerm(term: String): Boolean =
    this == term || Regex("(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])").containsMatchIn(this)
