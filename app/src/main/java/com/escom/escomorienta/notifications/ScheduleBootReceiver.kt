package com.escom.escomorienta.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.escom.escomorienta.data.schedule.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receptor de eventos del sistema operativo para restaurar y mantener sincronizadas
 * las alarmas de clases ante reinicio, actualización de la app o cambio de reloj/zona horaria.
 */
class ScheduleBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "diag_system_receiver_enter action=$action")

        val isHandled = when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> true
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
                ) {
                    true
                } else {
                    false
                }
            }
        }

        if (!isHandled) {
            Log.w(TAG, "diag_system_receiver_unhandled_action action=$action")
            return
        }

        val pending = goAsync()
        Log.i(TAG, "diag_system_receiver_go_async_started action=$action")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "diag_system_db_access_start")
                val repository = ScheduleRepository.getInstance(context.applicationContext)
                val settings = repository.getOrCreateLocalSettings()
                Log.i(TAG, "diag_system_settings_loaded enabled=${settings.enableStartClassNotification} scope=${settings.notificationScope}")
                val classes = repository.observeAllClasses().first()
                Log.i(TAG, "diag_system_classes_loaded class_count=${classes.size}")
                ScheduleNotificationScheduler.sync(context, settings, classes)
                Log.i(TAG, "diag_system_sync_invoked action=$action")
            } catch (e: Throwable) {
                Log.e(TAG, "diag_system_receiver_exception action=$action error=${e::class.java.simpleName} message=${e.message}", e)
            } finally {
                pending.finish()
                Log.i(TAG, "diag_system_receiver_pending_finished action=$action")
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiverDiag"
    }
}
