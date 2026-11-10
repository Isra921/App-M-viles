package com.escom.escomorienta.domain.notifications

import com.escom.escomorienta.data.schedule.ClassSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class ScheduledReminderPlan(
    val classId: Long,
    val subjectName: String,
    val startTime: String,
    val endTime: String,
    val classroomName: String,
    val offsetMinutes: Int,
    val requestCode: Int,
    val notificationId: Int,
    val triggerDateTime: LocalDateTime,
    val triggerEpochMs: Long,
    val classStartEpochMs: Long,
    val classEndEpochMs: Long,
    val dayOfWeek: Int
)

object NotificationCalculationEngine {

    /**
     * Calcula los planes de recordatorio para una sesión de clase dada y un conjunto de offsets.
     */
    fun calculatePlansForClass(
        item: ClassSession,
        offsetsMinutes: List<Int>,
        referenceDateTime: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduledReminderPlan> {
        val startParsed = parseTime(item.startTime) ?: return emptyList()
        val endParsed = parseTime(item.endTime) ?: startParsed.plusHours(1).plusMinutes(30)

        val targetDay = DayOfWeek.of(item.dayOfWeek.coerceIn(1, 7))

        return offsetsMinutes.mapNotNull { offset ->
            calculateSinglePlan(
                item = item,
                offsetMinutes = offset,
                startParsed = startParsed,
                endParsed = endParsed,
                targetDay = targetDay,
                referenceDateTime = referenceDateTime,
                zoneId = zoneId
            )
        }
    }

    /**
     * Calcula el plan de un solo recordatorio (un offset de una clase).
     */
    fun calculateSinglePlan(
        item: ClassSession,
        offsetMinutes: Int,
        startParsed: LocalTime? = null,
        endParsed: LocalTime? = null,
        targetDay: DayOfWeek = DayOfWeek.of(item.dayOfWeek.coerceIn(1, 7)),
        referenceDateTime: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ScheduledReminderPlan? {
        val start = startParsed ?: parseTime(item.startTime) ?: return null
        val end = endParsed ?: parseTime(item.endTime) ?: start.plusHours(1).plusMinutes(30)

        val (classStartDateTime, triggerDateTime) = resolveNextOccurrence(
            targetDay = targetDay,
            startTime = start,
            offsetMinutes = offsetMinutes,
            referenceDateTime = referenceDateTime
        )

        // Fecha de fin de clase (si endTime < startTime, cruza medianoche a la fecha siguiente)
        val classEndDate = if (end.isBefore(start)) {
            classStartDateTime.toLocalDate().plusDays(1)
        } else {
            classStartDateTime.toLocalDate()
        }
        val classEndDateTime = LocalDateTime.of(classEndDate, end)

        val triggerEpochMs = triggerDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val classStartEpochMs = classStartDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val classEndEpochMs = classEndDateTime.atZone(zoneId).toInstant().toEpochMilli()

        // El requestCode en AlarmManager debe ser único por combinación (clase, offset)
        val requestCode = generateRequestCode(item.id, offsetMinutes)
        // El notificationId es único por clase, para que avisos subsecuentes actualicen el banner en vez de apilarse
        val notificationId = generateNotificationId(item.id)

        return ScheduledReminderPlan(
            classId = item.id,
            subjectName = item.subjectName,
            startTime = item.startTime,
            endTime = item.endTime,
            classroomName = item.classroomName.orEmpty(),
            offsetMinutes = offsetMinutes,
            requestCode = requestCode,
            notificationId = notificationId,
            triggerDateTime = triggerDateTime,
            triggerEpochMs = triggerEpochMs,
            classStartEpochMs = classStartEpochMs,
            classEndEpochMs = classEndEpochMs,
            dayOfWeek = item.dayOfWeek
        )
    }

    /**
     * Resuelve la fecha y hora de la próxima ocurrencia de la clase y el momento de disparo,
     * garantizando que el disparo ocurra estrictamente en el futuro respecto a referenceDateTime.
     */
    fun resolveNextOccurrence(
        targetDay: DayOfWeek,
        startTime: LocalTime,
        offsetMinutes: Int,
        referenceDateTime: LocalDateTime
    ): Pair<LocalDateTime, LocalDateTime> {
        val today = referenceDateTime.toLocalDate()
        var classDate = today.with(TemporalAdjusters.nextOrSame(targetDay))
        var classDateTime = LocalDateTime.of(classDate, startTime)
        var triggerDateTime = classDateTime.minusMinutes(offsetMinutes.toLong())

        // Si el trigger calculado ya está en el pasado (o exactamente en el presente),
        // este aviso ya no es viable para la ocurrencia inmediata; se programa para la siguiente semana.
        if (!triggerDateTime.isAfter(referenceDateTime)) {
            classDate = classDate.plusWeeks(1)
            classDateTime = LocalDateTime.of(classDate, startTime)
            triggerDateTime = classDateTime.minusMinutes(offsetMinutes.toLong())
        }

        return Pair(classDateTime, triggerDateTime)
    }

    /**
     * Genera un identificador entero estable y determinista para PendingIntent en AlarmManager.
     */
    fun generateRequestCode(classId: Long, offsetMinutes: Int): Int {
        var hash = 17
        hash = 31 * hash + (classId xor (classId ushr 32)).toInt()
        hash = 31 * hash + offsetMinutes
        return hash and 0x7FFFFFFF
    }

    /**
     * Genera un identificador de notificación estable por materia/sesión para agrupar y actualizar.
     */
    fun generateNotificationId(classId: Long): Int {
        val hash = (classId xor (classId ushr 32)).toInt()
        return (hash and 0x7FFFFFFF) + 1000
    }

    private fun parseTime(raw: String): LocalTime? {
        val normalized = raw.trim().padStart(5, '0')
        return runCatching { LocalTime.parse(normalized) }.getOrNull()
    }
}
