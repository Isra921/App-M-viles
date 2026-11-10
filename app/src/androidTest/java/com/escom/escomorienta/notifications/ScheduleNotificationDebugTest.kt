package com.escom.escomorienta.notifications

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleNotificationDebugTest {
    @Test fun BUG_NOTIF_01_debug_probe_can_schedule_and_cancel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}"
        ).close()
        assertTrue(ScheduleNotificationScheduler.scheduleDebugNotification(context, 2))
        ScheduleNotificationScheduler.cancelDebugNotification(context)
    }
}
