package com.escom.escomorienta.domain.notifications

/**
 * Resultado de la validación de vigencia y puntualidad de una notificación académica.
 */
sealed interface NotificationValidityDecision {
    /**
     * La notificación llegó a tiempo (desfase menor o igual a la tolerancia normal).
     * Se debe presentar al usuario.
     */
    data class DeliverOnTime(
        val delaySeconds: Long
    ) : NotificationValidityDecision

    /**
     * La notificación sufrió un retraso tolerable por Doze/fabricante, pero la clase
     * aún no ha comenzado y todavía resulta útil para el estudiante.
     * Se debe presentar al usuario.
     */
    data class DeliverWithAcceptableDelay(
        val delaySeconds: Long,
        val remainingMinutesToClass: Int
    ) : NotificationValidityDecision

    /**
     * Descartada: La clase ya terminó en el momento de la ejecución.
     * NUNCA debe mostrarse como recordatorio.
     */
    data class DiscardedClassEnded(
        val actualEpochMs: Long,
        val classEndEpochMs: Long,
        val elapsedSinceEndMinutes: Int
    ) : NotificationValidityDecision

    /**
     * Descartada: La clase ya inició en el momento de la ejecución.
     * No tiene sentido avisar que la clase "comenzará" si ya está en curso.
     */
    data class DiscardedClassAlreadyStarted(
        val actualEpochMs: Long,
        val classStartEpochMs: Long,
        val elapsedSinceStartMinutes: Int
    ) : NotificationValidityDecision

    /**
     * Descartada: El retraso fue tan severo que queda menos del umbral mínimo útil
     * (por ejemplo, menos de 2 minutos antes del inicio de clase).
     */
    data class DiscardedExcessiveDelay(
        val delaySeconds: Long,
        val remainingMinutesToClass: Int
    ) : NotificationValidityDecision

    /**
     * Descartada: Las notificaciones fueron desactivadas por el usuario.
     */
    data object DiscardedNotificationsDisabled : NotificationValidityDecision

    /**
     * Descartada: La materia o clase ya no forma parte del horario activo del alumno.
     */
    data object DiscardedClassNotInSchedule : NotificationValidityDecision
}

/**
 * Validador de caducidad y vigencia para recordatorios académicos.
 * Implementa la política estricta de obsolescencia para evitar notificaciones desfasadas o acumuladas.
 */
object NotificationCaducidadValidator {

    /**
     * Margen en segundos dentro del cual se considera una entrega puntual (hasta 60 segundos).
     */
    const val ON_TIME_TOLERANCE_SECONDS = 60L

    /**
     * Mínimo de minutos restantes antes de que inicie la clase para que un recordatorio
     * retrasado se considere aún útil para el estudiante.
     */
    const val MIN_USEFUL_REMAINING_MINUTES = 2

    /**
     * Evalúa si un recordatorio debe mostrarse o descartarse en función de los tiempos
     * de clase, hora programada y hora real de recepción.
     *
     * @param scheduledTriggerEpochMs Época (ms) en la que debió dispararse la alarma.
     * @param actualEpochMs Época (ms) en la que se ejecuta efectivamente el receptor.
     * @param classStartEpochMs Época (ms) de inicio formal de la clase.
     * @param classEndEpochMs Época (ms) de término formal de la clase.
     * @param isClassInSchedule Indica si la clase aún existe en la base de datos local.
     * @param isNotificationsEnabled Indica si las notificaciones están activas en ajustes.
     */
    fun evaluate(
        scheduledTriggerEpochMs: Long,
        actualEpochMs: Long,
        classStartEpochMs: Long,
        classEndEpochMs: Long,
        isClassInSchedule: Boolean = true,
        isNotificationsEnabled: Boolean = true
    ): NotificationValidityDecision {
        if (!isNotificationsEnabled) {
            return NotificationValidityDecision.DiscardedNotificationsDisabled
        }

        if (!isClassInSchedule) {
            return NotificationValidityDecision.DiscardedClassNotInSchedule
        }

        // Si tenemos timestamps de clase válidos (> 0):
        if (classEndEpochMs > 0 && actualEpochMs >= classEndEpochMs) {
            val elapsedMinutes = (((actualEpochMs - classEndEpochMs) / 60_000L).coerceAtLeast(0L)).toInt()
            return NotificationValidityDecision.DiscardedClassEnded(
                actualEpochMs = actualEpochMs,
                classEndEpochMs = classEndEpochMs,
                elapsedSinceEndMinutes = elapsedMinutes
            )
        }

        if (classStartEpochMs > 0 && actualEpochMs >= classStartEpochMs) {
            val elapsedMinutes = (((actualEpochMs - classStartEpochMs) / 60_000L).coerceAtLeast(0L)).toInt()
            return NotificationValidityDecision.DiscardedClassAlreadyStarted(
                actualEpochMs = actualEpochMs,
                classStartEpochMs = classStartEpochMs,
                elapsedSinceStartMinutes = elapsedMinutes
            )
        }

        val delayMs = actualEpochMs - scheduledTriggerEpochMs
        val delaySeconds = delayMs / 1000L

        // Entrega puntual (incluso si se adelanta ligeramente por sincronización de reloj)
        if (delaySeconds <= ON_TIME_TOLERANCE_SECONDS) {
            return NotificationValidityDecision.DeliverOnTime(
                delaySeconds = delaySeconds.coerceAtLeast(0L)
            )
        }

        // La alarma sufrió retraso: verificar si aún resulta útil antes del inicio de clase
        if (classStartEpochMs > 0) {
            val remainingMs = classStartEpochMs - actualEpochMs
            val remainingMinutes = (remainingMs / 60_000L).toInt()

            return if (remainingMinutes >= MIN_USEFUL_REMAINING_MINUTES) {
                NotificationValidityDecision.DeliverWithAcceptableDelay(
                    delaySeconds = delaySeconds,
                    remainingMinutesToClass = remainingMinutes
                )
            } else {
                NotificationValidityDecision.DiscardedExcessiveDelay(
                    delaySeconds = delaySeconds,
                    remainingMinutesToClass = remainingMinutes
                )
            }
        }

        // Si no se proporcionó classStartEpochMs (fallback histórico/debug)
        return if (delaySeconds <= 15 * 60L) {
            NotificationValidityDecision.DeliverWithAcceptableDelay(
                delaySeconds = delaySeconds,
                remainingMinutesToClass = 0
            )
        } else {
            NotificationValidityDecision.DiscardedExcessiveDelay(
                delaySeconds = delaySeconds,
                remainingMinutesToClass = 0
            )
        }
    }
}
