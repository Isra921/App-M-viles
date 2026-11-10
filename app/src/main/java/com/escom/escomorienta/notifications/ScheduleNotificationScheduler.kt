package com.escom.escomorienta.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.escom.escomorienta.BuildConfig
import com.escom.escomorienta.data.schedule.ClassSession
import com.escom.escomorienta.data.schedule.LocalSettings
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.data.settings.NotificationScope
import com.escom.escomorienta.data.settings.decodeSubjectSelection
import com.escom.escomorienta.data.settings.subjectSelectionKey
import com.escom.escomorienta.domain.notifications.NotificationCalculationEngine
import com.escom.escomorienta.domain.notifications.ReminderScheme
import com.escom.escomorienta.domain.notifications.ScheduledReminderPlan
import kotlinx.coroutines.flow.first

object ScheduleNotificationScheduler {
    const val ChannelId = "class_reminders"
    const val ExtraSubject = "subject"
    const val ExtraTime = "time"
    const val ExtraEndTime = "end_time"
    const val ExtraClassroom = "classroom"
    const val ExtraNotificationId = "notification_id"
    const val ExtraClassId = "class_id"
    const val ExtraOffsetMinutes = "offset_minutes"
    const val ExtraScheduledTriggerEpoch = "scheduled_trigger_epoch"
    const val ExtraClassStartEpoch = "class_start_epoch"
    const val ExtraClassEndEpoch = "class_end_epoch"

    private const val PreferencesName = "schedule_notification_alarms"
    private const val RequestCodesKey = "request_codes"
    private const val TAG = "ScheduleNotifications"
    private const val DEBUG_REQUEST_CODE = 0x1200

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            ChannelId,
            "Recordatorios de clases",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos antes del inicio de las materias seleccionadas"
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        val registered = manager.getNotificationChannel(ChannelId)
        Log.i(TAG, "event=channel_audit importance=${registered?.importance} sound=${registered?.sound != null} vibration=${registered?.shouldVibrate()}")
    }

    fun sync(context: Context, settings: LocalSettings, classes: List<ClassSession>) {
        Log.i(TAG, "diag_sync_start total_classes=${classes.size} enabled=${settings.enableStartClassNotification} permission=${canPostNotifications(context)}")
        createChannel(context)
        cancelAll(context)
        if (!settings.enableStartClassNotification || !canPostNotifications(context)) {
            Log.i(TAG, "event=notification_sync_skipped enabled=${settings.enableStartClassNotification} permission=${canPostNotifications(context)}")
            return
        }

        val selected = decodeSubjectSelection(settings.notificationSubjects)
        val filtered = classes.filter { item ->
            settings.notificationScope == NotificationScope.ALL_SUBJECTS ||
                subjectSelectionKey(item.subjectName) in selected
        }
        val scheme = ReminderScheme.fromEncodedValue(settings.classNotificationMinutes)
        val offsets = scheme.offsetsMinutes
        val requestCodes = mutableSetOf<Int>()

        filtered.forEach { item ->
            val plans = NotificationCalculationEngine.calculatePlansForClass(item, offsets)
            plans.forEach { plan ->
                scheduleAlarm(context, plan)
                requestCodes += plan.requestCode
            }
        }

        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(RequestCodesKey, requestCodes.map(Int::toString).toSet())
            .apply()
        Log.i(TAG, "event=notification_sync_complete class_count=${filtered.size} alarm_count=${requestCodes.size}")
    }

    /**
     * Reprograma únicamente la siguiente semana para una sesión y offset específico,
     * SIN cancelar ni alterar las alarmas pendientes de las demás materias.
     */
    suspend fun rescheduleNextWeek(
        context: Context,
        classId: Long,
        offsetMinutes: Int
    ) {
        val repository = ScheduleRepository.getInstance(context.applicationContext)
        val settings = repository.getOrCreateLocalSettings()
        if (!settings.enableStartClassNotification || !canPostNotifications(context)) {
            Log.i(TAG, "event=reschedule_skipped reason=disabled_or_no_permission class_id=$classId")
            return
        }

        val classes = repository.observeAllClasses().first()
        val classItem = classes.firstOrNull { it.id == classId } ?: run {
            Log.w(TAG, "event=reschedule_skipped reason=class_not_found class_id=$classId")
            return
        }

        val plan = NotificationCalculationEngine.calculateSinglePlan(
            item = classItem,
            offsetMinutes = offsetMinutes
        ) ?: run {
            Log.w(TAG, "event=reschedule_skipped reason=calculation_failed class_id=$classId")
            return
        }

        scheduleAlarm(context, plan)
        registerRequestCode(context, plan.requestCode)
        Log.i(TAG, "event=reschedule_next_week_success class_id=$classId offset=$offsetMinutes trigger=${plan.triggerEpochMs}")
    }

    private fun scheduleAlarm(context: Context, plan: ScheduledReminderPlan) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = buildNotificationIntent(context, plan)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "event=alarm_scheduled_inexact reason=exact_alarm_permission_missing requestCode=${plan.requestCode} class=${plan.subjectName}")
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerEpochMs, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerEpochMs, pendingIntent)
                Log.d(TAG, "event=alarm_scheduled_exact requestCode=${plan.requestCode} trigger=${plan.triggerEpochMs} class=${plan.subjectName} offset=${plan.offsetMinutes}")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "event=alarm_scheduled_inexact reason=security_exception message=${e.message} requestCode=${plan.requestCode}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerEpochMs, pendingIntent)
        }
    }

    private fun buildNotificationIntent(
        context: Context,
        plan: ScheduledReminderPlan
    ): PendingIntent {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            putExtra(ExtraNotificationId, plan.notificationId)
            putExtra(ExtraClassId, plan.classId)
            putExtra(ExtraSubject, plan.subjectName)
            putExtra(ExtraTime, plan.startTime)
            putExtra(ExtraEndTime, plan.endTime)
            putExtra(ExtraClassroom, plan.classroomName)
            putExtra(ExtraOffsetMinutes, plan.offsetMinutes)
            putExtra(ExtraScheduledTriggerEpoch, plan.triggerEpochMs)
            putExtra(ExtraClassStartEpoch, plan.classStartEpochMs)
            putExtra(ExtraClassEndEpoch, plan.classEndEpochMs)
        }
        return PendingIntent.getBroadcast(
            context,
            plan.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun registerRequestCode(context: Context, requestCode: Int) {
        val prefs = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(RequestCodesKey, emptySet()).orEmpty().toMutableSet()
        current.add(requestCode.toString())
        prefs.edit().putStringSet(RequestCodesKey, current).apply()
    }

    fun cancelAll(context: Context) {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        preferences.getStringSet(RequestCodesKey, emptySet()).orEmpty().forEach { raw ->
            raw.toIntOrNull()?.let { requestCode ->
                val intent = Intent(context, ScheduleNotificationReceiver::class.java)
                val pending = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pending)
            }
        }
        preferences.edit().remove(RequestCodesKey).apply()
        Log.i(TAG, "event=all_alarms_cancelled")
    }

    /** Debug-only end-to-end probe. It carries no schedule or academic data. */
    fun scheduleDebugNotification(context: Context, delayMinutes: Int = 2): Boolean {
        if (!BuildConfig.DEBUG || !canPostNotifications(context)) return false
        createChannel(context)
        val requestCode = DEBUG_REQUEST_CODE
        val triggerEpoch = System.currentTimeMillis() + delayMinutes.coerceIn(1, 10) * 60_000L
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            putExtra(ExtraSubject, "Prueba de notificación")
            putExtra(ExtraTime, "diagnóstico local")
            putExtra(ExtraClassroom, "")
            putExtra(ExtraScheduledTriggerEpoch, triggerEpoch)
            // Debug start epoch in the future so it passes caducidad check
            putExtra(ExtraClassStartEpoch, triggerEpoch + 15 * 60_000L)
            putExtra(ExtraClassEndEpoch, triggerEpoch + 90 * 60_000L)
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pending)
        }
        Log.i(TAG, "event=debug_notification_scheduled delay_minutes=${delayMinutes.coerceIn(1, 10)}")
        return true
    }

    fun cancelDebugNotification(context: Context) {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, DEBUG_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)?.cancel(pending)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
