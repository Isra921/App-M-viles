package com.escom.escomorienta.domain.notifications

import android.content.Context
import com.escom.escomorienta.data.schedule.ClassSession
import com.escom.escomorienta.data.schedule.LocalSettings
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.data.settings.NotificationScope
import com.escom.escomorienta.data.settings.decodeSubjectSelection
import com.escom.escomorienta.data.settings.encodeSubjectSelection
import com.escom.escomorienta.data.settings.subjectSelectionKey
import com.escom.escomorienta.notifications.ScheduleNotificationScheduler
import kotlinx.coroutines.flow.first

data class NotificationConfigSummary(
    val isEnabled: Boolean,
    val scheme: ReminderScheme,
    val scope: NotificationScope,
    val selectedSubjects: Set<String>,
    val description: String
)

sealed interface NotificationConfigResult {
    data class Success(val summary: NotificationConfigSummary, val message: String) : NotificationConfigResult
    data class InvalidAdvance(val requestedMinutes: Int, val maxAllowed: Int = ReminderScheme.MAX_ADVANCE_MINUTES) : NotificationConfigResult
    data class InvalidScheme(val reason: String) : NotificationConfigResult
    data class SubjectNotFound(val subject: String) : NotificationConfigResult
}

interface NotificationSchedulerContract {
    fun sync(context: Context, settings: LocalSettings, classes: List<ClassSession>)
    fun scheduleDebugNotification(context: Context, delayMinutes: Int): Boolean
}

object DefaultNotificationSchedulerContract : NotificationSchedulerContract {
    override fun sync(context: Context, settings: LocalSettings, classes: List<ClassSession>) {
        ScheduleNotificationScheduler.sync(context, settings, classes)
    }

    override fun scheduleDebugNotification(context: Context, delayMinutes: Int): Boolean {
        return ScheduleNotificationScheduler.scheduleDebugNotification(context, delayMinutes)
    }
}

class ConfigureNotificationsUseCase(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val scheduler: NotificationSchedulerContract = DefaultNotificationSchedulerContract
) {
    suspend fun getCurrentConfiguration(): NotificationConfigSummary {
        val settings = scheduleRepository.getOrCreateLocalSettings()
        val scheme = ReminderScheme.fromEncodedValue(settings.classNotificationMinutes)
        val selected = decodeSubjectSelection(settings.notificationSubjects)
        return NotificationConfigSummary(
            isEnabled = settings.enableStartClassNotification,
            scheme = scheme,
            scope = settings.notificationScope,
            selectedSubjects = selected,
            description = buildDescription(settings.enableStartClassNotification, scheme, settings.notificationScope, selected)
        )
    }

    suspend fun getNotificationSummary(): String = getCurrentConfiguration().description

    suspend fun setEnabled(enabled: Boolean): NotificationConfigResult {
        scheduleRepository.updateLocalSettings {
            copy(enableStartClassNotification = enabled)
        }
        syncAlarms()
        val summary = getCurrentConfiguration()
        val message = if (enabled) "Notificaciones activadas." else "Notificaciones desactivadas."
        return NotificationConfigResult.Success(summary, message)
    }

    suspend fun setReminderAdvance(minutes: Int): NotificationConfigResult {
        if (minutes <= 0 || minutes > ReminderScheme.MAX_ADVANCE_MINUTES) {
            return NotificationConfigResult.InvalidAdvance(minutes)
        }
        val scheme = ReminderScheme.fromAdvanceMinutes(minutes)
            ?: return NotificationConfigResult.InvalidAdvance(minutes)
        
        scheduleRepository.updateLocalSettings {
            copy(
                classNotificationMinutes = scheme.encode(),
                enableStartClassNotification = true
            )
        }
        syncAlarms()
        val summary = getCurrentConfiguration()
        return NotificationConfigResult.Success(summary, "Anticipación configurada a $minutes minutos antes.")
    }

    suspend fun setReminderCount(count: Int): NotificationConfigResult {
        val current = getCurrentConfiguration().scheme
        return setReminderScheme(count, current.intervalMinutes)
    }

    suspend fun setReminderInterval(intervalMinutes: Int): NotificationConfigResult {
        val current = getCurrentConfiguration().scheme
        return setReminderScheme(current.count, intervalMinutes)
    }

    suspend fun setReminderScheme(count: Int, intervalMinutes: Int): NotificationConfigResult {
        val furthest = count * intervalMinutes
        if (furthest > ReminderScheme.MAX_ADVANCE_MINUTES) {
            return NotificationConfigResult.InvalidScheme(
                "El aviso más lejano ($furthest min) supera el límite máximo permitido de ${ReminderScheme.MAX_ADVANCE_MINUTES} minutos."
            )
        }
        val scheme = ReminderScheme.custom(count, intervalMinutes)
            ?: return NotificationConfigResult.InvalidScheme("Parámetros de recordatorio inválidos (count: $count, interval: $intervalMinutes)")

        scheduleRepository.updateLocalSettings {
            copy(
                classNotificationMinutes = scheme.encode(),
                enableStartClassNotification = true
            )
        }
        syncAlarms()
        val summary = getCurrentConfiguration()
        return NotificationConfigResult.Success(
            summary,
            "Esquema configurado: $count avisos cada $intervalMinutes minutos (hasta $furthest min antes)."
        )
    }

    suspend fun setSubjectNotification(subjectName: String, enabled: Boolean): NotificationConfigResult {
        val allSubjects = scheduleRepository.getAllSubjects()
        val key = subjectSelectionKey(subjectName)
        val matchedSubject = allSubjects.firstOrNull { subjectSelectionKey(it) == key }
            ?: return NotificationConfigResult.SubjectNotFound(subjectName)

        val settings = scheduleRepository.getOrCreateLocalSettings()
        val selected = decodeSubjectSelection(settings.notificationSubjects).toMutableSet()

        if (settings.notificationScope == NotificationScope.ALL_SUBJECTS && !enabled) {
            val allKeys = allSubjects.map { subjectSelectionKey(it) }.toSet()
            val newSelected = allKeys - key
            scheduleRepository.updateLocalSettings {
                copy(
                    notificationScope = NotificationScope.SELECTED_SUBJECTS,
                    notificationSubjects = encodeSubjectSelection(newSelected)
                )
            }
        } else {
            if (enabled) {
                selected.add(key)
            } else {
                selected.remove(key)
            }
            scheduleRepository.updateLocalSettings {
                copy(
                    notificationScope = NotificationScope.SELECTED_SUBJECTS,
                    notificationSubjects = encodeSubjectSelection(selected)
                )
            }
        }
        syncAlarms()
        val summary = getCurrentConfiguration()
        val message = if (enabled) "Avisos activados para $matchedSubject." else "Avisos desactivados para $matchedSubject."
        return NotificationConfigResult.Success(summary, message)
    }

    fun triggerTestNotification(delayMinutes: Int = 2): Boolean {
        return scheduler.scheduleDebugNotification(context, delayMinutes)
    }

    suspend fun syncAlarms() {
        val settings = scheduleRepository.getOrCreateLocalSettings()
        val classes = scheduleRepository.observeAllClasses().first()
        scheduler.sync(context, settings, classes)
    }

    private fun buildDescription(
        enabled: Boolean,
        scheme: ReminderScheme,
        scope: NotificationScope,
        selected: Set<String>
    ): String {
        if (!enabled) return "Las notificaciones de clases están desactivadas."
        val schemeDesc = "${scheme.profile.displayName}: ${scheme.offsetsMinutes.joinToString(", ")} min antes"
        val scopeDesc = if (scope == NotificationScope.ALL_SUBJECTS) "todas las materias" else "${selected.size} materias seleccionadas"
        return "Notificaciones activadas ($schemeDesc) para $scopeDesc."
    }
}
