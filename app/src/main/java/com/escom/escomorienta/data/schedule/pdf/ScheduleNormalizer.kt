package com.escom.escomorienta.data.schedule.pdf

import com.escom.escomorienta.data.schedule.ClassSessionDraft
import com.escom.escomorienta.data.schedule.ClassroomStatusAvailable
import com.escom.escomorienta.data.schedule.ClassroomStatusPending
import java.text.Normalizer
import java.util.Locale

class ScheduleNormalizer {
    private val spanishMexico = Locale.forLanguageTag("es-MX")

    fun normalize(rawSessions: List<RawScheduleSession>): List<ClassSessionDraft> {
        return rawSessions.mapNotNull { raw ->
            val dayOfWeek = WeekDay.fromLabel(raw.dayName)?.value ?: return@mapNotNull null
            val startTime = normalizeTime(raw.startTime) ?: return@mapNotNull null
            val endTime = normalizeTime(raw.endTime) ?: return@mapNotNull null
            val classroom = normalizeClassroom(raw.classroomName)

            ClassSessionDraft(
                subjectName = normalizeSubject(raw.subjectName),
                groupName = raw.groupName?.cleanSpaces()?.uppercase(Locale.ROOT),
                teacherName = raw.teacherName?.cleanSpaces()?.takeIf { it.isNotBlank() },
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                classroomName = classroom,
                classroomStatus = if (classroom == null) ClassroomStatusPending else ClassroomStatusAvailable,
                rawText = raw.rawText
            )
        }.distinctBy {
            "${it.subjectName}|${it.groupName}|${it.dayOfWeek}|${it.startTime}|${it.endTime}|${it.classroomName}"
        }.sortedWith(
            compareBy<ClassSessionDraft> { it.dayOfWeek }
                .thenBy { it.startTime }
                .thenBy { it.subjectName }
        )
    }

    private fun normalizeSubject(value: String): String {
        val cleaned = value
            .replace("|", " / ")
            .cleanSpaces()

        return cleaned.toReadableCase()
    }

    private fun normalizeClassroom(value: String?): String? {
        return value
            ?.replace("Salón:", "", ignoreCase = true)
            ?.replace("Salon:", "", ignoreCase = true)
            ?.replace("Aula:", "", ignoreCase = true)
            ?.cleanSpaces()
            ?.takeIf { it.isNotBlank() }
    }

    private fun normalizeTime(value: String): String? {
        val match = TimeRegex.find(value.cleanSpaces()) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        return "%02d:%02d".format(hour, minute)
    }

    private fun String.toReadableCase(): String {
        val normalized = lowercase(spanishMexico)
        return normalized.split(" ").joinToString(" ") { word ->
            if (word.length <= 2 && word != "de" && word != "la" && word != "el" && word != "en") {
                word.uppercase(spanishMexico)
            } else {
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(spanishMexico) else char.toString()
                }
            }
        }
    }

    private fun String.cleanSpaces(): String {
        return replace("\\s+".toRegex(), " ").trim()
    }

    private enum class WeekDay(val value: Int, vararg val labels: String) {
        MONDAY(1, "lunes"),
        TUESDAY(2, "martes"),
        WEDNESDAY(3, "miercoles", "miércoles"),
        THURSDAY(4, "jueves"),
        FRIDAY(5, "viernes"),
        SATURDAY(6, "sabado", "sábado");

        companion object {
            fun fromLabel(value: String): WeekDay? {
                val normalized = normalizeDayLabel(value)
                return entries.firstOrNull { day ->
                    day.labels.any { normalizeDayLabel(it) == normalized }
                }
            }

            private fun normalizeDayLabel(value: String): String {
                return Normalizer.normalize(value, Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    .lowercase(Locale.ROOT)
                    .trim()
            }
        }
    }

    private companion object {
        val TimeRegex = Regex("""(\d{1,2})[:.](\d{2})""")
    }
}
