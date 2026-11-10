package com.escom.escomorienta.data.schedule.pdf

import android.util.Log
import com.escom.escomorienta.BuildConfig
import com.escom.escomorienta.data.schedule.normalizeKnownClassroom
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

data class RawScheduleSession(
    val subjectName: String,
    val groupName: String?,
    val teacherName: String?,
    val dayName: String,
    val startTime: String,
    val endTime: String,
    val classroomName: String?,
    val rawText: String?,
    val teacherSourceText: String? = null
)

internal data class TeacherName(val sourceText: String, val displayText: String?)

internal data class TableColumns(
    val groupStartX: Float,
    val subjectStartX: Float,
    val creditsStartX: Float,
    val teacherStartX: Float,
    val mondayStartX: Float,
    val tuesdayStartX: Float,
    val wednesdayStartX: Float,
    val thursdayStartX: Float,
    val fridayStartX: Float
)

class ScheduleParser {
    fun parse(document: PdfTextDocument): List<RawScheduleSession> {
        val positioned = parsePositionedTable(document)
        return positioned.ifEmpty { parsePlainTextFallback(document.plainText) }
    }

    private fun parsePositionedTable(document: PdfTextDocument): List<RawScheduleSession> {
        if (document.tokens.isEmpty()) return emptyList()

        val dayColumns = findDayColumns(document.tokens)
        if (dayColumns.size < 5) return emptyList()

        val headerYByPage = dayColumns.groupBy { it.pageIndex }
            .mapValues { entry -> entry.value.map { it.y }.average().toFloat() }
        val footerYByPage = document.tokens
            .filter { it.isFooterStart }
            .groupBy { it.pageIndex }
            .mapValues { entry -> entry.value.maxOf { it.y } }
        val sessions = mutableListOf<RawScheduleSession>()
        val groupTokens = document.tokens.filter { GroupRegex.matches(it.text.trim()) }

        groupTokens.groupBy { it.pageIndex }.forEach { (pageIndex, pageGroupTokens) ->
            val pageColumns = detectTableColumns(document.tokens, pageIndex)
            val creditX = pageColumns?.creditsStartX
                ?: document.tokens.firstOrNull {
                    it.pageIndex == pageIndex && it.normalized == "creditos"
                }?.x
                ?: 230f
            val headerY = headerYByPage[pageIndex]
            val footerY = footerYByPage[pageIndex]
            val pageTokens = document.tokens
                .filter { it.pageIndex == pageIndex }
                .filter { token -> !token.isBelowFooter(headerY = headerY, footerY = footerY) }
            val rows = buildRowRanges(
                groupTokens = pageGroupTokens,
                headerY = headerY
            )
            val pageDayColumns = dayColumns.filter { it.pageIndex == pageIndex }

            rows.forEach { row ->
                val rowTokens = pageTokens.filter { it.y in row.lowerY..row.upperY }
                val groupName = row.groupToken.text.trim()
                val readDescending = shouldReadDescending(headerYByPage[pageIndex], row.groupToken.y)
                val subject = extractSubject(rowTokens, creditX, row.groupToken.endX, readDescending)
                if (subject.isBlank()) return@forEach

                val teacher = pageColumns?.let { columns ->
                    extractTeacher(
                        rowTokens = rowTokens,
                        columns = columns
                    )
                } ?: extractTeacherFromPlainRow(document.plainText.rowForGroup(groupName))

                pageDayColumns.forEach { dayColumn ->
                    val cellTokens = rowTokens
                        .filter { it.centerX >= dayColumn.startX && it.centerX < dayColumn.endX }
                        .filterNot { token -> token.normalized in DayNames }

                    val cellLines = cellTokens.toLines(readDescending)
                    cellLines
                        .filter { TimeRangeRegex.containsMatchIn(it.text) }
                        .forEach { line ->
                            TimeRangeRegex.findAll(line.text).forEach { match ->
                                val rawCellText = cellLines.joinToString(separator = " ") { it.text }
                                sessions += RawScheduleSession(
                                    subjectName = subject,
                                    groupName = groupName,
                                    teacherName = teacher?.displayText,
                                    dayName = dayColumn.dayName,
                                    startTime = "${match.groupValues[1]}:${match.groupValues[2]}",
                                    endTime = "${match.groupValues[3]}:${match.groupValues[4]}",
                                    classroomName = extractClassroom(rawCellText),
                                    rawText = listOf(groupName, subject, dayColumn.dayName, rawCellText)
                                        .joinToString(separator = " ")
                                        .cleanSpaces(),
                                    teacherSourceText = teacher?.sourceText
                                )
                            }
                        }
                }
            }
        }

        return sessions.distinctBy {
            "${it.groupName}|${it.subjectName}|${it.dayName}|${it.startTime}|${it.endTime}|${it.classroomName}"
        }
    }

    private fun findDayColumns(tokens: List<PdfTextToken>): List<DayColumn> {
        val headerTokens = tokens
            .filter { it.normalized in DayNames }
            .distinctBy { "${it.pageIndex}-${it.normalized}" }
            .sortedWith(compareBy<PdfTextToken> { it.pageIndex }.thenBy { it.x })

        return headerTokens
            .groupBy { it.pageIndex }
            .flatMap { (_, pageTokens) ->
                val sorted = pageTokens.sortedBy { it.x }
                sorted.mapIndexed { index, token ->
                    val previous = sorted.getOrNull(index - 1)
                    val next = sorted.getOrNull(index + 1)
                    val startX = previous?.let { (it.x + token.x) / 2f } ?: (token.x - 35f)
                    val endX = next?.let { (it.x + token.x) / 2f } ?: (token.endX + 65f)
                    DayColumn(
                        dayName = DayNameByNormalized.getValue(token.normalized),
                        pageIndex = token.pageIndex,
                        x = token.x,
                        y = token.y,
                        startX = startX,
                        endX = endX
                    )
                }
            }
    }

    private fun detectTableColumns(tokens: List<PdfTextToken>, pageIndex: Int): TableColumns? {
        val pageTokens = tokens.filter { it.pageIndex == pageIndex }
        val professor = pageTokens.findHeaderToken("PROFESOR") ?: return null
        val monday = pageTokens.findHeaderToken("LUNES") ?: return null
        if (professor.x >= monday.x) return null

        return TableColumns(
            groupStartX = pageTokens.findHeaderToken("GPO", "GRUPO")?.x ?: 0f,
            subjectStartX = pageTokens.findHeaderPhrase("UNIDAD DE APRENDIZAJE")?.x
                ?: pageTokens.findHeaderToken("UNIDAD")?.x ?: 60f,
            creditsStartX = pageTokens.findHeaderToken("CREDITOS")?.x ?: 230f,
            teacherStartX = professor.x,
            mondayStartX = monday.x,
            tuesdayStartX = pageTokens.findHeaderToken("MARTES")?.x ?: monday.x,
            wednesdayStartX = pageTokens.findHeaderToken("MIERCOLES")?.x ?: monday.x,
            thursdayStartX = pageTokens.findHeaderToken("JUEVES")?.x ?: monday.x,
            fridayStartX = pageTokens.findHeaderToken("VIERNES")?.x ?: monday.x
        ).also {
            debugLog("Columnas página $pageIndex: Profesor=${it.teacherStartX}, Lunes=${it.mondayStartX}")
        }
    }

    private fun List<PdfTextToken>.findHeaderToken(vararg candidates: String): PdfTextToken? {
        val expected = candidates.map(::normalizeForMatching).toSet()
        return firstOrNull { normalizeForMatching(it.text) in expected }
    }

    private fun List<PdfTextToken>.findHeaderPhrase(phrase: String): PdfTextToken? {
        val expected = normalizeForMatching(phrase)
        return groupIntoTokenLines().firstNotNullOfOrNull { line ->
            val sorted = line.sortedBy { it.x }
            sorted.indices.firstNotNullOfOrNull { start ->
                var combined = ""
                for (end in start..sorted.lastIndex) {
                    combined = "$combined ${sorted[end].text}".trim()
                    val normalized = normalizeForMatching(combined)
                    if (normalized == expected) return@firstNotNullOfOrNull sorted[start]
                    if (normalized.length > expected.length + 8) break
                }
                null
            }
        }
    }

    private fun buildRowRanges(
        groupTokens: List<PdfTextToken>,
        headerY: Float?
    ): List<RowRange> {
        val sorted = groupTokens.sortedBy { it.y }
        return sorted.mapIndexed { index, token ->
            var lower = sorted.getOrNull(index - 1)?.let { (it.y + token.y) / 2f } ?: Float.NEGATIVE_INFINITY
            var upper = sorted.getOrNull(index + 1)?.let { (it.y + token.y) / 2f } ?: Float.POSITIVE_INFINITY

            if (headerY != null) {
                if (headerY > token.y && index == sorted.lastIndex) {
                    upper = (headerY + token.y) / 2f
                } else if (headerY < token.y && index == 0) {
                    lower = (headerY + token.y) / 2f
                }
            }

            RowRange(
                groupToken = token,
                lowerY = lower,
                upperY = upper
            )
        }
    }

    private fun extractSubject(
        rowTokens: List<PdfTextToken>,
        creditX: Float,
        groupEndX: Float,
        readDescending: Boolean
    ): String {
        val subjectTokens = rowTokens
            .filter { it.x > groupEndX + 4f && it.centerX < creditX - 4f }
            .filterNot { TimeRangeRegex.containsMatchIn(it.text) }

        return subjectTokens
            .joinForReading(readDescending)
            .replace(CourseCodePrefixRegex, "")
            .replace("|", " / ")
            .cleanSpaces()
    }

    internal fun extractTeacher(
        rowTokens: List<PdfTextToken>,
        columns: TableColumns
    ): TeacherName? {
        // A long final surname can cross the visual boundary while still belonging to
        // the teacher column. centerX preserves it and still rejects Monday tokens.
        val accepted = rowTokens
            .filter {
                it.x >= columns.teacherStartX - ColumnMargin &&
                    it.centerX < columns.mondayStartX - ColumnMargin
            }
            .filterNot { it.isTeacherNoiseToken() }
            .sortedWith(compareBy<PdfTextToken> { it.y }.thenBy { it.x })
        val result = teacherNameFromSource(accepted.joinToString(" ") { it.text })
        return result
    }

    private fun extractClassroom(text: String): String? {
        val building = BuildingRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        RoomRegex.find(text)?.let { match ->
            val visibleRoom = match.groupValues.getOrNull(1)
                ?.takeIf { it.isNotBlank() }
                ?.cleanSpaces()
                ?: return null

            return normalizeSaesClassroom(
                building = building,
                visibleRoom = visibleRoom
            )
        }

        LabRegex.find(text)?.let { match ->
            return match.value.cleanSpaces()
        }

        return null
    }

    private fun normalizeSaesClassroom(
        building: Int?,
        visibleRoom: String
    ): String? {
        val digits = visibleRoom.filter { it.isDigit() }
        if (digits.isBlank()) return null

        val candidate = when {
            digits.length == 4 -> digits
            building != null && digits.length <= 3 -> "$building${digits.padStart(3, '0')}"
            else -> digits
        }

        return normalizeKnownClassroom(candidate)
    }

    private fun parsePlainTextFallback(text: String): List<RawScheduleSession> {
        val cleanedText = text.replace('\r', '\n').trimFooterText()
        val matches = GroupRegex.findAll(cleanedText).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.flatMapIndexed { index, match ->
            val nextStart = matches.getOrNull(index + 1)?.range?.first ?: cleanedText.length
            val row = cleanedText.substring(match.range.first, nextStart).cleanSpaces()
            val groupName = match.value
            val subject = extractSubjectFromPlainRow(row)
            val teacher = extractTeacherFromPlainRow(row)
            val dayCursor = PlainTextDayOrder.iterator()

            TimeRangeRegex.findAll(row).mapNotNull { timeMatch ->
                if (!dayCursor.hasNext()) return@mapNotNull null
                val afterTime = row.substring(timeMatch.range.last + 1).take(120)
                RawScheduleSession(
                    subjectName = subject,
                    groupName = groupName,
                    teacherName = teacher?.displayText,
                    dayName = dayCursor.next(),
                    startTime = "${timeMatch.groupValues[1]}:${timeMatch.groupValues[2]}",
                    endTime = "${timeMatch.groupValues[3]}:${timeMatch.groupValues[4]}",
                    classroomName = extractClassroom(afterTime),
                    rawText = row,
                    teacherSourceText = teacher?.sourceText
                )
            }.toList()
        }
    }

    private fun extractSubjectFromPlainRow(row: String): String {
        val afterGroup = row.replaceFirst(GroupRegex, "").trim()
        val beforeCredits = CreditsRegex.find(afterGroup)?.range?.first?.let { afterGroup.take(it) } ?: afterGroup
        return beforeCredits
            .replace(CourseCodePrefixRegex, "")
            .replace("|", " / ")
            .cleanSpaces()
    }

    internal fun extractTeacherFromPlainRow(row: String): TeacherName? {
        val creditMatch = CreditsRegex.find(row) ?: return null
        val afterCredits = row.substring(creditMatch.range.last + 1)
        return teacherNameFromSource(afterCredits)
    }

    private fun List<PdfTextToken>.toLines(readDescending: Boolean): List<PdfTextLine> {
        if (isEmpty()) return emptyList()

        val lines = groupIntoTokenLines()

        val sortedLines = if (readDescending) {
            lines.sortedByDescending { it.map { token -> token.y }.average() }
        } else {
            lines.sortedBy { it.map { token -> token.y }.average() }
        }

        return sortedLines.map { lineTokens ->
            val sorted = lineTokens.sortedBy { it.x }
            PdfTextLine(
                text = sorted.joinToString(separator = " ") { it.text }.cleanSpaces(),
                pageIndex = sorted.first().pageIndex,
                x = sorted.minOf { it.x },
                y = sorted.map { it.y }.average().toFloat(),
                endX = sorted.maxOf { it.endX }
            )
        }
    }

    private fun List<PdfTextToken>.groupIntoTokenLines(): List<List<PdfTextToken>> {
        val lines = mutableListOf<MutableList<PdfTextToken>>()
        sortedBy { it.y }.forEach { token ->
            val line = lines.firstOrNull { existing ->
                val reference = existing.first()
                val tolerance = maxOf(
                    DefaultLineTolerance,
                    reference.height * RelativeLineTolerance,
                    token.height * RelativeLineTolerance
                )
                abs(reference.y - token.y) <= tolerance
            }
            if (line == null) {
                lines += mutableListOf(token)
            } else {
                line += token
            }
        }
        return lines
    }

    private fun List<PdfTextToken>.joinForReading(readDescending: Boolean): String {
        return toLines(readDescending)
            .joinToString(separator = " ") { it.text }
            .cleanSpaces()
    }

    private fun shouldReadDescending(headerY: Float?, rowY: Float): Boolean {
        return headerY == null || headerY > rowY
    }

    private val PdfTextToken.normalized: String
        get() = text.normalizeForParser()

    private fun PdfTextToken.isTeacherNoiseToken(): Boolean {
        val raw = text.trim()
        val value = normalized.trimEnd(':', '.', ',')
        return raw.isBlank() ||
            CreditsRegex.matches(raw) ||
            TimeTokenRegex.matches(raw) ||
            NumericNoiseRegex.matches(raw) ||
            value in DayNames ||
            value == "edificio" ||
            value.startsWith("salon") ||
            value == "aula"
    }

    private val PdfTextToken.isFooterStart: Boolean
        get() {
            val value = normalized.trimEnd(':', '.', ',')
            return value == "nota" || value.startsWith("dae_web")
        }

    private fun PdfTextToken.isBelowFooter(
        headerY: Float?,
        footerY: Float?
    ): Boolean {
        if (headerY == null || footerY == null) return false
        return if (headerY > footerY) {
            y <= footerY + FooterPadding
        } else {
            y >= footerY - FooterPadding
        }
    }

    private fun String.normalizeForParser(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .trim()
    }

    private fun String.trimFooterText(): String {
        val footerMatch = FooterTextRegex.find(this) ?: return this
        return substring(0, footerMatch.range.first)
    }

    private fun String.cleanSpaces(): String {
        return replace("\\s+".toRegex(), " ").trim()
    }

    private fun String.rowForGroup(groupName: String): String {
        val cleaned = replace('\r', '\n').trimFooterText()
        val match = GroupRegex.findAll(cleaned).firstOrNull { it.value == groupName } ?: return ""
        val next = GroupRegex.find(cleaned, match.range.last + 1)?.range?.first ?: cleaned.length
        return cleaned.substring(match.range.first, next).cleanSpaces()
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) runCatching { Log.d(LogTag, message) }
    }

    private data class DayColumn(
        val dayName: String,
        val pageIndex: Int,
        val x: Float,
        val y: Float,
        val startX: Float,
        val endX: Float
    )

    private data class RowRange(
        val groupToken: PdfTextToken,
        val lowerY: Float,
        val upperY: Float
    )

    private companion object {
        val DayNameByNormalized = mapOf(
            "lunes" to "Lunes",
            "martes" to "Martes",
            "miercoles" to "Miercoles",
            "jueves" to "Jueves",
            "viernes" to "Viernes",
            "sabado" to "Sabado"
        )
        val DayNames = DayNameByNormalized.keys
        val PlainTextDayOrder = listOf("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado")
        val GroupRegex = Regex("""\b\d+[A-Z]{2}\d\b""")
        val CreditsRegex = Regex("""\b\d{1,2}(?:\.\d{1,2})\b""")
        val CourseCodePrefixRegex = Regex("""^[A-Z]\d{3}\s*[-–—]\s*""")
        val TimeRangeRegex = Regex("""(\d{1,2})[:.](\d{2})\s*[-–—]\s*(\d{1,2})[:.](\d{2})""")
        val TimeTokenRegex = Regex("""\d{1,2}[:.]\d{2}""")
        val NumericNoiseRegex = Regex("""[\d.]+""")
        val BuildingRegex = Regex("""(?i)edificio\s*:?\s*([1-4])""")
        val RoomRegex = Regex("""(?i)(?:sal[oó]n|aula)\s*:?\s*([A-Za-z0-9-]+)""")
        val LabRegex = Regex("""(?i)\b(?:lab|laboratorio)\s*[A-Za-z0-9-]*\b""")
        val FooterTextRegex = Regex("""(?i)\b(?:nota\s*:|dae_web)\b""")
        const val ColumnMargin = 2f
        const val DefaultLineTolerance = 3.5f
        const val RelativeLineTolerance = 0.4f
        const val FooterPadding = 4f
        const val LogTag = "ScheduleParser"
    }
}

internal fun normalizeForMatching(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .uppercase(Locale.ROOT)
        .replace("[^A-Z0-9\\s]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

internal fun cleanTeacherCell(raw: String): String? {
    val cleaned = raw.replace("\\s+".toRegex(), " ").trim()
    if (cleaned.isBlank()) return null
    val stop = TeacherStopRegex.find(cleaned)
    return cleaned.take(stop?.range?.first ?: cleaned.length)
        .trim(' ', '-', '–', '—', ',', ';', ':')
        .replace("\\s+".toRegex(), " ")
        .takeIf { it.isNotBlank() }
}

internal fun teacherNameFromSource(raw: String): TeacherName? {
    val source = cleanTeacherCell(raw) ?: return null
    if (!isPlausibleTeacherName(source)) return null
    return TeacherName(
        sourceText = source,
        displayText = if (normalizeForMatching(source) in UnassignedTeacherValues) {
            null
        } else {
            formatTeacherName(source.trim().split(Regex("\\s+")).filter(String::isNotBlank))
        }
    )
}

internal fun isPlausibleTeacherName(value: String): Boolean {
    val normalized = normalizeForMatching(value)
    if (normalized in UnassignedTeacherValues) return true
    val words = value.trim().split(Regex("\\s+"))
    if (words.size !in 2..8 || ':' in value) return false
    if (TimeRangeValidationRegex.containsMatchIn(value)) return false
    if (GroupCodeValidationRegex.containsMatchIn(value)) return false
    if (NoiseTeacherWords.any { it in normalized.split(" ") }) return false
    val letters = value.count(Char::isLetter)
    val meaningful = value.count { it.isLetterOrDigit() }
    return meaningful > 0 && letters.toFloat() / meaningful >= 0.85f
}

private fun formatTeacherName(words: List<String>): String {
    val locale = Locale.forLanguageTag("es-MX")
    return words.joinToString(" ") { word ->
        val lower = word.lowercase(locale)
        if (normalizeForMatching(word) in SurnameParticles) lower
        else lower.replaceFirstChar { it.titlecase(locale) }
    }
}

private val TeacherStopRegex = Regex(
    """(?i)\b(?:edificio|sal[oó]n|aula|nota|dae[_\s-]*web|lunes|martes|miércoles|miercoles|jueves|viernes|sábado|sabado)\b|\b\d{1,2}[:.]\d{2}\s*[-–—]\s*\d{1,2}[:.]\d{2}\b"""
)
private val TimeRangeValidationRegex =
    Regex("""\b\d{1,2}[:.]\d{2}\s*[-–—]\s*\d{1,2}[:.]\d{2}\b""")
private val GroupCodeValidationRegex = Regex("""\b\d+[A-Z]{2}\d\b|\b\d{7,}\b""", RegexOption.IGNORE_CASE)
private val NoiseTeacherWords = setOf(
    "EDIFICIO", "SALON", "AULA", "NOTA", "DAE", "WEB",
    "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO"
)
private val UnassignedTeacherValues =
    setOf("SIN ASIGNAR", "NO ASIGNADO", "POR ASIGNAR", "PENDIENTE")
private val SurnameParticles = setOf("DE", "DEL", "LA", "LAS", "LOS", "Y")
