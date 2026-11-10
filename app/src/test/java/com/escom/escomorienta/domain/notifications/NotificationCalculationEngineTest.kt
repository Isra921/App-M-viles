package com.escom.escomorienta.domain.notifications

import com.escom.escomorienta.data.schedule.ClassSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class NotificationCalculationEngineTest {

    private val zone = ZoneId.systemDefault()

    private fun createClass(
        id: Long = 101L,
        dayOfWeek: Int = 1, // Lunes
        startTime: String = "07:00",
        endTime: String = "08:30",
        subject: String = "Compiladores",
        classroom: String = "1201"
    ) = ClassSession(
        id = id,
        scheduleId = 1L,
        subjectName = subject,
        groupName = "3CM1",
        teacherName = "Profesor",
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        classroomName = classroom,
        classroomStatus = "AVAILABLE"
    )

    @Test
    fun `clase con un solo recordatorio calcula el disparo correcto en el futuro`() {
        // Referencia: Domingo 08 de Noviembre de 2026, 12:00 PM
        val ref = LocalDateTime.of(2026, 11, 8, 12, 0)
        val session = createClass(dayOfWeek = 1, startTime = "07:00") // Lunes 07:00 AM

        val plans = NotificationCalculationEngine.calculatePlansForClass(
            item = session,
            offsetsMinutes = listOf(15),
            referenceDateTime = ref,
            zoneId = zone
        )

        assertEquals(1, plans.size)
        val plan = plans.first()
        // El lunes es 09 de Noviembre de 2026. 15 min antes son las 06:45 AM
        assertEquals(LocalDateTime.of(2026, 11, 9, 6, 45), plan.triggerDateTime)
        assertEquals(LocalDateTime.of(2026, 11, 9, 7, 0).atZone(zone).toInstant().toEpochMilli(), plan.classStartEpochMs)
        assertEquals(LocalDateTime.of(2026, 11, 9, 8, 30).atZone(zone).toInstant().toEpochMilli(), plan.classEndEpochMs)
    }

    @Test
    fun `clase con multiples intervalos de anticipacion genera planes independientes`() {
        val ref = LocalDateTime.of(2026, 11, 8, 12, 0)
        val session = createClass(dayOfWeek = 1, startTime = "13:00")

        val plans = NotificationCalculationEngine.calculatePlansForClass(
            item = session,
            offsetsMinutes = listOf(30, 15),
            referenceDateTime = ref,
            zoneId = zone
        )

        assertEquals(2, plans.size)
        val plan30 = plans.first { it.offsetMinutes == 30 }
        val plan15 = plans.first { it.offsetMinutes == 15 }

        assertEquals(LocalDateTime.of(2026, 11, 9, 12, 30), plan30.triggerDateTime)
        assertEquals(LocalDateTime.of(2026, 11, 9, 12, 45), plan15.triggerDateTime)

        // Los requestCodes en AlarmManager deben ser distintos para coexistir en el kernel
        assertNotEquals(plan30.requestCode, plan15.requestCode)
        // El notificationId debe ser identico para que actualice la misma notificacion en el panel
        assertEquals(plan30.notificationId, plan15.notificationId)
    }

    @Test
    fun `clases consecutivas generan identificadores unicos sin colisiones`() {
        val ref = LocalDateTime.of(2026, 11, 8, 12, 0)
        val class1 = createClass(id = 101L, dayOfWeek = 1, startTime = "07:00", endTime = "08:30")
        val class2 = createClass(id = 102L, dayOfWeek = 1, startTime = "08:30", endTime = "10:00")

        val plans1 = NotificationCalculationEngine.calculatePlansForClass(class1, listOf(15), ref, zone)
        val plans2 = NotificationCalculationEngine.calculatePlansForClass(class2, listOf(15), ref, zone)

        assertNotEquals(plans1.first().requestCode, plans2.first().requestCode)
        assertNotEquals(plans1.first().notificationId, plans2.first().notificationId)
    }

    @Test
    fun `recordatorio cuyo intervalo de anticipacion cae en el dia anterior`() {
        // Clase el Lunes a las 00:10 (12:10 AM). Anticipación de 20 minutos -> Domingo 23:50
        val ref = LocalDateTime.of(2026, 11, 8, 20, 0) // Domingo 20:00
        val session = createClass(dayOfWeek = 1, startTime = "00:10")

        val plans = NotificationCalculationEngine.calculatePlansForClass(
            item = session,
            offsetsMinutes = listOf(20),
            referenceDateTime = ref,
            zoneId = zone
        )

        assertEquals(1, plans.size)
        val plan = plans.first()
        // Clase: Lunes 9 de nov a las 00:10. Disparo: Domingo 8 de nov a las 23:50
        assertEquals(LocalDateTime.of(2026, 11, 8, 23, 50), plan.triggerDateTime)
        assertTrue(plan.triggerDateTime.isAfter(ref))
    }

    @Test
    fun `clase que empieza temprano en el dia avanza a la siguiente semana si la hora de hoy ya paso`() {
        // Hoy es Lunes 09 de Noviembre a las 08:00 AM. La clase era a las 07:00 AM (offset 15m -> 06:45 AM ya pasó)
        val ref = LocalDateTime.of(2026, 11, 9, 8, 0)
        val session = createClass(dayOfWeek = 1, startTime = "07:00")

        val plans = NotificationCalculationEngine.calculatePlansForClass(
            item = session,
            offsetsMinutes = listOf(15),
            referenceDateTime = ref,
            zoneId = zone
        )

        assertEquals(1, plans.size)
        val plan = plans.first()
        // Debe programarse para el siguiente Lunes (16 de Noviembre) a las 06:45 AM
        assertEquals(LocalDateTime.of(2026, 11, 16, 6, 45), plan.triggerDateTime)
        assertTrue(plan.triggerDateTime.isAfter(ref))
    }

    @Test
    fun `clase en dia posterior de la semana se programa para la misma semana`() {
        // Hoy es Lunes 09 de Noviembre. La clase es el Miércoles 11 a las 10:00 AM
        val ref = LocalDateTime.of(2026, 11, 9, 10, 0)
        val session = createClass(dayOfWeek = 3, startTime = "10:00") // Miércoles

        val plans = NotificationCalculationEngine.calculatePlansForClass(
            item = session,
            offsetsMinutes = listOf(15),
            referenceDateTime = ref,
            zoneId = zone
        )

        assertEquals(1, plans.size)
        val plan = plans.first()
        assertEquals(LocalDateTime.of(2026, 11, 11, 9, 45), plan.triggerDateTime)
    }

    @Test
    fun `rescheduleNextWeek calcula exactamente 7 dias despues`() {
        val (firstClass, firstTrigger) = NotificationCalculationEngine.resolveNextOccurrence(
            targetDay = DayOfWeek.MONDAY,
            startTime = LocalTime.of(7, 0),
            offsetMinutes = 15,
            referenceDateTime = LocalDateTime.of(2026, 11, 8, 12, 0)
        )
        // Primera ocurrencia: Lunes 9 a las 06:45
        assertEquals(LocalDateTime.of(2026, 11, 9, 6, 45), firstTrigger)

        // Tras consumirse el Lunes 9 a las 06:45, se reprograma con referencia a ese momento
        val (secondClass, secondTrigger) = NotificationCalculationEngine.resolveNextOccurrence(
            targetDay = DayOfWeek.MONDAY,
            startTime = LocalTime.of(7, 0),
            offsetMinutes = 15,
            referenceDateTime = firstTrigger
        )
        // Siguiente ocurrencia: Lunes 16 a las 06:45 (exactamente 7 días después)
        assertEquals(LocalDateTime.of(2026, 11, 16, 6, 45), secondTrigger)
        assertEquals(firstTrigger.plusWeeks(1), secondTrigger)
    }
}
