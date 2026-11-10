package com.escom.escomorienta.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.escom.escomorienta.MainActivity
import com.escom.escomorienta.R
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.domain.notifications.NotificationCaducidadValidator
import com.escom.escomorienta.domain.notifications.NotificationValidityDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScheduleNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val actualEpoch = System.currentTimeMillis()
        Log.i(TAG, "event=receiver_entered action=${intent.action}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "event=receiver_skipped reason=permission_denied")
            return
        }

        val subject = intent.getStringExtra(ScheduleNotificationScheduler.ExtraSubject)
            ?: return
        val time = intent.getStringExtra(ScheduleNotificationScheduler.ExtraTime).orEmpty()
        val classroom = intent.getStringExtra(ScheduleNotificationScheduler.ExtraClassroom).orEmpty()
        val notificationId = intent.getIntExtra(
            ScheduleNotificationScheduler.ExtraNotificationId,
            subject.hashCode()
        )
        val classId = intent.getLongExtra(ScheduleNotificationScheduler.ExtraClassId, -1L)
        val offsetMinutes = intent.getIntExtra(ScheduleNotificationScheduler.ExtraOffsetMinutes, 0)
        val scheduledEpoch = intent.getLongExtra(ScheduleNotificationScheduler.ExtraScheduledTriggerEpoch, 0L)
        val classStartEpoch = intent.getLongExtra(ScheduleNotificationScheduler.ExtraClassStartEpoch, 0L)
        val classEndEpoch = intent.getLongExtra(ScheduleNotificationScheduler.ExtraClassEndEpoch, 0L)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ScheduleRepository.getInstance(context.applicationContext)
                val settings = repository.getOrCreateLocalSettings()
                val allClasses = repository.observeAllClasses().first()
                val isClassInSchedule = classId <= 0 || allClasses.any { it.id == classId }

                val decision = NotificationCaducidadValidator.evaluate(
                    scheduledTriggerEpochMs = scheduledEpoch,
                    actualEpochMs = actualEpoch,
                    classStartEpochMs = classStartEpoch,
                    classEndEpochMs = classEndEpoch,
                    isClassInSchedule = isClassInSchedule,
                    isNotificationsEnabled = settings.enableStartClassNotification
                )

                when (decision) {
                    is NotificationValidityDecision.DiscardedClassEnded -> {
                        Log.w(
                            TAG,
                            "event=notification_discarded reason=CLASS_ENDED subject=$subject id=$notificationId elapsed_min=${decision.elapsedSinceEndMinutes}"
                        )
                    }
                    is NotificationValidityDecision.DiscardedClassAlreadyStarted -> {
                        Log.w(
                            TAG,
                            "event=notification_discarded reason=CLASS_ALREADY_STARTED subject=$subject id=$notificationId elapsed_min=${decision.elapsedSinceStartMinutes}"
                        )
                    }
                    is NotificationValidityDecision.DiscardedExcessiveDelay -> {
                        Log.w(
                            TAG,
                            "event=notification_discarded reason=EXCESSIVE_DELAY subject=$subject id=$notificationId delay_s=${decision.delaySeconds}"
                        )
                    }
                    is NotificationValidityDecision.DiscardedClassNotInSchedule -> {
                        Log.i(
                            TAG,
                            "event=notification_discarded reason=CLASS_NOT_IN_SCHEDULE subject=$subject id=$notificationId"
                        )
                    }
                    is NotificationValidityDecision.DiscardedNotificationsDisabled -> {
                        Log.i(
                            TAG,
                            "event=notification_discarded reason=NOTIFICATIONS_DISABLED subject=$subject id=$notificationId"
                        )
                    }
                    is NotificationValidityDecision.DeliverOnTime -> {
                        postNotification(
                            context = context,
                            subject = subject,
                            time = time,
                            classroom = classroom,
                            notificationId = notificationId,
                            delayNote = null
                        )
                        Log.i(
                            TAG,
                            "event=notification_posted mode=ON_TIME id=$notificationId subject=$subject delay_s=${decision.delaySeconds}"
                        )
                    }
                    is NotificationValidityDecision.DeliverWithAcceptableDelay -> {
                        val delayNote = if (decision.remainingMinutesToClass > 0) {
                            "en ${decision.remainingMinutesToClass} min"
                        } else null
                        postNotification(
                            context = context,
                            subject = subject,
                            time = time,
                            classroom = classroom,
                            notificationId = notificationId,
                            delayNote = delayNote
                        )
                        Log.i(
                            TAG,
                            "event=notification_posted mode=ACCEPTABLE_DELAY id=$notificationId subject=$subject delay_s=${decision.delaySeconds} remaining_min=${decision.remainingMinutesToClass}"
                        )
                    }
                }

                // Autonomía: reprogramar únicamente la semana siguiente para este slot específico
                if (classId > 0 && offsetMinutes > 0 && isClassInSchedule && settings.enableStartClassNotification) {
                    ScheduleNotificationScheduler.rescheduleNextWeek(context, classId, offsetMinutes)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "event=receiver_processing_failed error=${e.message}", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        subject: String,
        time: String,
        classroom: String,
        notificationId: Int,
        delayNote: String?
    ) {
        val details = buildString {
            append("Comienza a las $time")
            if (!delayNote.isNullOrBlank()) {
                append(" ($delayNote)")
            }
            if (classroom.isNotBlank()) {
                append(" · Salón $classroom")
            }
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            ScheduleNotificationScheduler.ChannelId
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(subject)
            .setContentText(details)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private companion object {
        const val TAG = "ScheduleNotifications"
    }
}
