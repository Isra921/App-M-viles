package com.escom.escomorienta.data.schedule.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.escom.escomorienta.data.schedule.ClassSession
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Exportador de horarios escolares de ESCOM a formato estándar iCalendar (.ics - RFC 5545).
 * Permite integrar la carga académica con Google Calendar, Outlook y calendarios nativos.
 */
object ScheduleIcsExporter {

    private val dayOfWeekToIcsDay = mapOf(
        1 to "MO",
        2 to "TU",
        3 to "WE",
        4 to "TH",
        5 to "FR",
        6 to "SA",
        7 to "SU"
    )

    /**
     * Genera el contenido textual en formato RFC 5545.
     */
    fun generateIcsContent(classes: List<ClassSession>): String {
        val nowFormatted = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "T000000Z"
        val sb = StringBuilder()

        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//ESCOM IPN//ESCOM Orienta v1.0//ES\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        sb.append("X-WR-CALNAME:Horario ESCOM IPN\r\n")
        sb.append("X-WR-TIMEZONE:America/Mexico_City\r\n")

        // Calcular la fecha base para el día de la semana correspondiente
        val today = LocalDate.now()

        classes.forEach { session ->
            val byDay = dayOfWeekToIcsDay[session.dayOfWeek] ?: return@forEach
            val targetDayOfWeek = when (session.dayOfWeek) {
                1 -> DayOfWeek.MONDAY
                2 -> DayOfWeek.TUESDAY
                3 -> DayOfWeek.WEDNESDAY
                4 -> DayOfWeek.THURSDAY
                5 -> DayOfWeek.FRIDAY
                6 -> DayOfWeek.SATURDAY
                else -> DayOfWeek.SUNDAY
            }

            val eventDate = today.with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
            val dateStr = eventDate.format(DateTimeFormatter.BASIC_ISO_DATE)

            val startTimeClean = session.startTime.replace(":", "") + "00"
            val endTimeClean = session.endTime.replace(":", "") + "00"

            val uid = "${UUID.randomUUID()}@escomorienta.ipn.mx"
            val room = session.classroomName?.takeIf { it.isNotBlank() } ?: "Aula por confirmar"
            val group = session.groupName?.takeIf { it.isNotBlank() } ?: "N/D"
            val teacher = session.teacherName?.takeIf { it.isNotBlank() } ?: "Docente por asignar"

            val description = "Materia: ${session.subjectName}\\nGrupo: $group\\nProfesor: $teacher\\nAula: $room\\nEscuela Superior de Cómputo - IPN"

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:$uid\r\n")
            sb.append("DTSTAMP:$nowFormatted\r\n")
            sb.append("DTSTART;TZID=America/Mexico_City:${dateStr}T$startTimeClean\r\n")
            sb.append("DTEND;TZID=America/Mexico_City:${dateStr}T$endTimeClean\r\n")
            sb.append("RRULE:FREQ=WEEKLY;BYDAY=$byDay\r\n")
            sb.append("SUMMARY:${escapeIcsText(session.subjectName)}\r\n")
            sb.append("LOCATION:${escapeIcsText(room)}, ESCOM IPN\r\n")
            sb.append("DESCRIPTION:$description\r\n")
            sb.append("STATUS:CONFIRMED\r\n")
            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    /**
     * Guarda el archivo .ics en el directorio cache de la aplicación y crea el Intent para compartir.
     */
    fun createShareIntent(context: Context, classes: List<ClassSession>): Intent? {
        if (classes.isEmpty()) return null

        val icsText = generateIcsContent(classes)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "horario_escom.ics")
        file.writeText(icsText, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Horario Escolar - ESCOM IPN")
            putExtra(Intent.EXTRA_TEXT, "Adjunto mi horario escolar de ESCOM IPN para importar en el calendario.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escapeIcsText(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
    }
}
