package com.escom.escomorienta.data.map

import java.text.Normalizer

/** Floor-independent index. Visible floor may rank results later, never filter them. */
class MapPlaceIndex(private val places: List<MapPlace>) {
    data class Mention(val place: MapPlace, val matchedText: String, val interpretedAs: String? = null)
    fun search(query: String): List<MapPlace> {
        val normalized = query.normalized()
        if (normalized.isBlank()) return emptyList()
        val direct = places.filter { place ->
            (listOf(place.id, place.name) + place.aliases).any { candidate ->
                candidate.normalized().contains(normalized)
            }
        }.sortedWith(compareBy<MapPlace> { !it.name.normalized().startsWith(normalized) }.thenBy(MapPlace::name))
        if (direct.isNotEmpty()) return direct

        val typoMatch = Regex("(?<![a-z0-9-])[0-9opil]{4}(?![a-z0-9-])").find(normalized)
        if (typoMatch != null && typoMatch.value.any(Char::isLetter)) {
            val interpreted = typoMatch.value.map { char ->
                when (char) {
                    'o', 'p' -> '0'
                    'i', 'l' -> '1'
                    else -> char
                }
            }.joinToString("")
            val corrected = places.filter { place ->
                (listOf(place.id, place.name) + place.aliases).any { candidate ->
                    candidate.normalized().contains(interpreted)
                }
            }.sortedWith(compareBy<MapPlace> { !it.name.normalized().startsWith(interpreted) }.thenBy(MapPlace::name))
            if (corrected.isNotEmpty()) return corrected
        }
        return emptyList()
    }

    fun mentioned(text: String): List<MapPlace> {
        return mentionDetails(text).map(Mention::place)
    }

    fun mentionDetails(text: String): List<Mention> {
        val normalizedText = text.normalized()
        val exact = places.mapNotNull { place ->
            val term = (place.aliases + place.name + place.id).map { it.normalized() }
                .filter(String::isNotBlank)
                .filter { Regex("(?<![a-z0-9])${Regex.escape(it)}(?![a-z0-9])").containsMatchIn(normalizedText) }
                .maxByOrNull(String::length) ?: return@mapNotNull null
            Triple(normalizedText.indexOf(term), term.length, Mention(place, term))
        }

        // OCR/keyboard-style classroom correction is deliberately narrow: exactly four
        // characters, at least one non-digit substitution, and one canonical match only.
        val corrected = Regex("(?<![a-z0-9-])[0-9opil]{4}(?![a-z0-9-])")
            .findAll(normalizedText)
            .filter { it.value.any(Char::isLetter) }
            .mapNotNull { match ->
                val interpreted = match.value.map { char -> when (char) {
                    'o', 'p' -> '0'; 'i', 'l' -> '1'; else -> char
                } }.joinToString("")
                val candidates = places.filter { place ->
                    (place.aliases + place.name).any { it.normalized() == interpreted }
                }.distinctBy(MapPlace::id)
                candidates.singleOrNull()?.let { place ->
                    Triple(match.range.first, match.value.length, Mention(place, match.value, interpreted))
                }
            }.toList()
        return (exact + corrected)
            .sortedWith(compareBy<Triple<Int, Int, Mention>> { it.first }.thenByDescending { it.second })
            .distinctBy { it.third.place.id }
            .map { it.third }
    }

    private fun String.normalized(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase().trim()
}
