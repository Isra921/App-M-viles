package com.escom.escomorienta.domain.notifications

import android.content.Context
import android.content.ContextWrapper
import com.escom.escomorienta.data.schedule.ClassSession
import com.escom.escomorienta.data.schedule.LocalSettings
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.data.settings.NotificationScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConfigureNotificationsUseCaseTest {

    private var currentSettings = LocalSettings()
    private val classesFlow = MutableStateFlow<List<ClassSession>>(emptyList())
    private var syncCalled = false
    private var debugNotificationScheduled = false

    private val fakeRepo = object : ScheduleRepository() {
        override suspend fun getOrCreateLocalSettings(): LocalSettings = currentSettings

        override suspend fun updateLocalSettings(transform: LocalSettings.() -> LocalSettings) {
            currentSettings = currentSettings.transform()
        }

        override suspend fun getAllSubjects(): List<String> = listOf("Cálculo Aplicado", "Física Clásica", "Programación")

        override fun observeAllClasses(): Flow<List<ClassSession>> = classesFlow.asStateFlow()
    }

    private val fakeScheduler = object : NotificationSchedulerContract {
        override fun sync(context: Context, settings: LocalSettings, classes: List<ClassSession>) {
            syncCalled = true
        }

        override fun scheduleDebugNotification(context: Context, delayMinutes: Int): Boolean {
            debugNotificationScheduled = true
            return true
        }
    }

    private val context: Context = ContextWrapper(null)
    private lateinit var useCase: ConfigureNotificationsUseCase

    @Before
    fun setup() {
        currentSettings = LocalSettings()
        syncCalled = false
        debugNotificationScheduled = false
        useCase = ConfigureNotificationsUseCase(context, fakeRepo, fakeScheduler)
    }

    @Test
    fun `60 minutes advance is valid and syncs alarms`() = runBlocking {
        val result = useCase.setReminderAdvance(60)
        assertTrue("60 min must be valid", result is NotificationConfigResult.Success)
        assertEquals(1160, currentSettings.classNotificationMinutes)
        assertTrue(currentSettings.enableStartClassNotification)
        assertTrue("Sync must be called on alarm change", syncCalled)
    }

    @Test
    fun `61 minutes advance is rejected by 60-minute safety rule`() = runBlocking {
        val initialEncoded = currentSettings.classNotificationMinutes
        val result = useCase.setReminderAdvance(61)
        assertTrue("61 min must be rejected", result is NotificationConfigResult.InvalidAdvance)
        assertEquals(initialEncoded, currentSettings.classNotificationMinutes)
        assertFalse("Sync must not be called when rejected", syncCalled)
    }

    @Test
    fun `2 hours advance is rejected`() = runBlocking {
        val result = useCase.setReminderAdvance(120)
        assertTrue("120 min must be rejected", result is NotificationConfigResult.InvalidAdvance)
    }

    @Test
    fun `scheme with furthest trigger exceeding 60 minutes is rejected`() = runBlocking {
        val result = useCase.setReminderScheme(count = 5, intervalMinutes = 20)
        assertTrue("Scheme exceeding 60 minutes must be rejected", result is NotificationConfigResult.InvalidScheme)
        val scheme = result as NotificationConfigResult.InvalidScheme
        assertTrue(scheme.reason.contains("supera el límite máximo"))
    }

    @Test
    fun `scheme 3 alerts every 5 minutes is valid and encoded correctly`() = runBlocking {
        val result = useCase.setReminderScheme(count = 3, intervalMinutes = 5)
        assertTrue("3 alerts every 5 min must be valid", result is NotificationConfigResult.Success)
        assertEquals(1305, currentSettings.classNotificationMinutes)
        val summary = (result as NotificationConfigResult.Success).summary
        assertEquals(listOf(15, 10, 5), summary.scheme.offsetsMinutes)
        assertTrue(syncCalled)
    }

    @Test
    fun `setEnabled updates persistence state and triggers sync`() = runBlocking {
        useCase.setEnabled(false)
        assertFalse(currentSettings.enableStartClassNotification)
        assertTrue(syncCalled)

        syncCalled = false
        useCase.setEnabled(true)
        assertTrue(currentSettings.enableStartClassNotification)
        assertTrue(syncCalled)
    }

    @Test
    fun `setSubjectNotification toggles subject notification scope`() = runBlocking {
        val result = useCase.setSubjectNotification("Cálculo Aplicado", false)
        assertTrue(result is NotificationConfigResult.Success)
        assertEquals(NotificationScope.SELECTED_SUBJECTS, currentSettings.notificationScope)
        assertFalse(currentSettings.notificationSubjects.contains("calculo"))
        assertTrue(syncCalled)
    }

    @Test
    fun `setSubjectNotification returns error for unknown subject`() = runBlocking {
        val result = useCase.setSubjectNotification("Materia Inexistente", false)
        assertTrue(result is NotificationConfigResult.SubjectNotFound)
    }

    @Test
    fun `triggerTestNotification schedules debug notification`() {
        val success = useCase.triggerTestNotification(2)
        assertTrue(success)
        assertTrue(debugNotificationScheduled)
    }
}
