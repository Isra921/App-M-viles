package com.escom.escomorienta.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.escom.escomorienta.data.schedule.DefaultUserAlias
import com.escom.escomorienta.data.schedule.LocalSettings
import com.escom.escomorienta.data.schedule.MaxUserAliasLength
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.data.schedule.ClassSession
import com.escom.escomorienta.data.settings.ThemeMode
import com.escom.escomorienta.data.settings.SchedulePaletteMode
import com.escom.escomorienta.data.settings.NotificationScope
import com.escom.escomorienta.data.settings.decodeSubjectSelection
import com.escom.escomorienta.data.settings.encodeSubjectSelection
import com.escom.escomorienta.data.settings.subjectSelectionKey
import com.escom.escomorienta.ui.schedule.decodeCustomScheduleColors
import com.escom.escomorienta.ui.schedule.encodeCustomScheduleColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log

private const val MapPreferencesName = "map_recent_searches"

class AppSettingsViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = ScheduleRepository.getInstance(application.applicationContext)

    private val _settings = MutableStateFlow(LocalSettings())
    val settings: StateFlow<LocalSettings> = _settings.asStateFlow()
    val classes: StateFlow<List<ClassSession>> = repository.observeAllClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isInitialSettingsLoaded = MutableStateFlow(false)
    val isInitialSettingsLoaded: StateFlow<Boolean> = _isInitialSettingsLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            // Read the persisted value explicitly before allowing the app UI to appear.
            // Room can emit an empty row first, which previously caused the dark-to-light flash.
            _settings.value = repository.getOrCreateLocalSettings()
            _isInitialSettingsLoaded.value = true

            repository.observeLocalSettings().collectLatest { storedSettings ->
                _settings.value = storedSettings
            }
        }
    }

    fun saveAlias(alias: String) {
        updateSettings("ALIAS_WRITE") {
            copy(
                userAlias = alias.trim()
                    .ifBlank { DefaultUserAlias }
                    .take(MaxUserAliasLength)
            )
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        updateSettings("THEME_WRITE") { copy(themeMode = themeMode) }
    }

    fun setSchedulePaletteMode(mode: SchedulePaletteMode) {
        updateSettings { copy(schedulePaletteMode = mode) }
    }

    fun setCustomScheduleColor(subject: String, colorIndex: Int) {
        updateSettings {
            val colors = decodeCustomScheduleColors(customScheduleColors).toMutableMap()
            colors[subjectSelectionKey(subject)] = colorIndex
            copy(
                schedulePaletteMode = SchedulePaletteMode.CUSTOM,
                customScheduleColors = encodeCustomScheduleColors(colors)
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        updateSettings("NOTIFICATION_WRITE") { copy(enableStartClassNotification = enabled) }
    }

    fun setReminderConfiguration(encodedConfiguration: Int) {
        updateSettings { copy(classNotificationMinutes = encodedConfiguration) }
    }

    fun setNotificationScope(scope: NotificationScope) {
        updateSettings { copy(notificationScope = scope) }
    }

    fun setSubjectNotificationEnabled(subject: String, enabled: Boolean) {
        updateSettings {
            val selected = decodeSubjectSelection(notificationSubjects).toMutableSet()
            val key = subjectSelectionKey(subject)
            if (enabled) selected += key else selected -= key
            copy(notificationSubjects = encodeSubjectSelection(selected))
        }
    }

    fun deleteSchedule() {
        viewModelScope.launch {
            repository.deleteCurrentSchedule()
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            repository.clearLocalData()
            getApplication<Application>()
                .getSharedPreferences(MapPreferencesName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    private fun updateSettings(event: String = "SETTINGS_WRITE", transform: LocalSettings.() -> LocalSettings) {
        viewModelScope.launch {
            Log.d("SettingsPersistence", "event=$event")
            repository.updateLocalSettings(transform)
        }
    }
}
