package com.escom.escomorienta.domain.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationCaducidadValidatorTest {

    private val zone = ZoneId.systemDefault()

    // Clase: Lunes 07:00 a 08:30
    private val classStart = LocalDateTime.of(2026, 11, 9, 7, 0).atZone(zone).toInstant().toEpochMilli()
    private val classEnd = LocalDateTime.of(2026, 11, 9, 8, 30).atZone(zone).toInstant().toEpochMilli()
    // Recordatorio programado a 15 min antes: 06:45
    private val scheduledTrigger = LocalDateTime.of(2026, 11, 9, 6, 45).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `notificacion entregada exactamente a tiempo`() {
        val actual = scheduledTrigger // 06:45:00.000
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DeliverOnTime)
        assertEquals(0L, (decision as NotificationValidityDecision.DeliverOnTime).delaySeconds)
    }

    @Test
    fun `notificacion entregada con desfase leve de 25 segundos es considerada puntual`() {
        val actual = scheduledTrigger + 25_000L // 06:45:25
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DeliverOnTime)
        assertEquals(25L, (decision as NotificationValidityDecision.DeliverOnTime).delaySeconds)
    }

    @Test
    fun `notificacion con retraso de 2 minutos es entregada con retraso aceptable`() {
        val actual = scheduledTrigger + 2 * 60_000L // 06:47:00 (quedan 13 min para las 07:00)
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DeliverWithAcceptableDelay)
        val result = decision as NotificationValidityDecision.DeliverWithAcceptableDelay
        assertEquals(120L, result.delaySeconds)
        assertEquals(13, result.remainingMinutesToClass)
    }

    @Test
    fun `notificacion con retraso de 5 minutos es entregada con retraso aceptable`() {
        val actual = scheduledTrigger + 5 * 60_000L // 06:50:00 (quedan 10 min para las 07:00)
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DeliverWithAcceptableDelay)
        val result = decision as NotificationValidityDecision.DeliverWithAcceptableDelay
        assertEquals(300L, result.delaySeconds)
        assertEquals(10, result.remainingMinutesToClass)
    }

    @Test
    fun `notificacion con retraso de 10 minutos es entregada si aun faltan 5 minutos para la clase`() {
        val actual = scheduledTrigger + 10 * 60_000L // 06:55:00 (quedan 5 min)
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DeliverWithAcceptableDelay)
        val result = decision as NotificationValidityDecision.DeliverWithAcceptableDelay
        assertEquals(5, result.remainingMinutesToClass)
    }

    @Test
    fun `notificacion con retraso de 14 minutos es descartada por tiempo insuficiente menor a 2 minutos`() {
        val actual = scheduledTrigger + 14 * 60_000L // 06:59:00 (queda 1 min para la clase)
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DiscardedExcessiveDelay)
        val result = decision as NotificationValidityDecision.DiscardedExcessiveDelay
        assertEquals(1, result.remainingMinutesToClass)
    }

    @Test
    fun `notificacion que llega cuando la clase ya inicio es descartada`() {
        val actual = classStart + 5 * 60_000L // 07:05:00 (inició hace 5 min)
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DiscardedClassAlreadyStarted)
        val result = decision as NotificationValidityDecision.DiscardedClassAlreadyStarted
        assertEquals(5, result.elapsedSinceStartMinutes)
    }

    @Test
    fun `notificacion con retraso de 30 minutos cuando la clase ya inicio es descartada`() {
        val actual = scheduledTrigger + 30 * 60_000L // 07:15:00 (inició hace 15 min)
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DiscardedClassAlreadyStarted)
        val result = decision as NotificationValidityDecision.DiscardedClassAlreadyStarted
        assertEquals(15, result.elapsedSinceStartMinutes)
    }

    @Test
    fun `notificacion con retraso de 60 minutos cuando la clase sigue en curso es descartada`() {
        val actual = scheduledTrigger + 60 * 60_000L // 07:45:00
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DiscardedClassAlreadyStarted)
        val result = decision as NotificationValidityDecision.DiscardedClassAlreadyStarted
        assertEquals(45, result.elapsedSinceStartMinutes)
    }

    @Test
    fun `notificacion que llega despues de que la clase termino es descartada`() {
        val actual = classEnd + 10 * 60_000L // 08:40:00 (terminó a las 08:30)
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd
        )
        assertTrue(decision is NotificationValidityDecision.DiscardedClassEnded)
        val result = decision as NotificationValidityDecision.DiscardedClassEnded
        assertEquals(10, result.elapsedSinceEndMinutes)
    }

    @Test
    fun `rafaga de multiples recordatorios vencidos consecutivos son todos descartados`() {
        // Simula despertar de Doze a las 09:00 AM para 3 clases matutinas que ya concluyeron
        val wakeUpTime = LocalDateTime.of(2026, 11, 9, 9, 0).atZone(zone).toInstant().toEpochMilli()

        val class1End = LocalDateTime.of(2026, 11, 9, 8, 30).atZone(zone).toInstant().toEpochMilli()
        val class2End = LocalDateTime.of(2026, 11, 9, 8, 0).atZone(zone).toInstant().toEpochMilli()

        val d1 = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = wakeUpTime,
            classStartEpochMs = classStart,
            classEndEpochMs = class1End
        )
        val d2 = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger - 3600_000L,
            actualEpochMs = wakeUpTime,
            classStartEpochMs = classStart - 3600_000L,
            classEndEpochMs = class2End
        )

        assertTrue("Clase 1 terminada debe descartarse", d1 is NotificationValidityDecision.DiscardedClassEnded)
        assertTrue("Clase 2 terminada debe descartarse", d2 is NotificationValidityDecision.DiscardedClassEnded)
    }

    @Test
    fun `notificaciones descartadas si el usuario las desactivo en ajustes`() {
        val actual = scheduledTrigger
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd,
            isNotificationsEnabled = false
        )
        assertTrue(decision is NotificationValidityDecision.DiscardedNotificationsDisabled)
    }

    @Test
    fun `notificaciones descartadas si la materia fue eliminada del horario`() {
        val actual = scheduledTrigger
        val decision = NotificationCaducidadValidator.evaluate(
            scheduledTriggerEpochMs = scheduledTrigger,
            actualEpochMs = actual,
            classStartEpochMs = classStart,
            classEndEpochMs = classEnd,
            isClassInSchedule = false
        )
        assertTrue(decision is NotificationValidityDecision.DiscardedClassNotInSchedule)
    }
}
