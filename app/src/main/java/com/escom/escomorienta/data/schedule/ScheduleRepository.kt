package com.escom.escomorienta.data.schedule

import android.content.Context
import androidx.room.withTransaction
import com.escom.escomorienta.data.local.AppDatabase
import com.escom.escomorienta.data.schedule.local.ClassSessionEntity
import com.escom.escomorienta.data.schedule.local.LocalSettingsEntity
import com.escom.escomorienta.data.schedule.local.ProcessedScheduleEntity
import com.escom.escomorienta.data.schedule.local.ProcessedScheduleWithClasses
import com.escom.escomorienta.data.settings.ThemeMode
import com.escom.escomorienta.data.settings.SchedulePaletteMode
import com.escom.escomorienta.data.settings.NotificationScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log

open class ScheduleRepository internal constructor(
    database: AppDatabase? = null
) {
    private val _database: AppDatabase? = database
    protected val database: AppDatabase get() = _database ?: error("Database not initialized for test fake")
    protected val scheduleDao get() = database.scheduleDao()
    protected val localSettingsDao get() = database.localSettingsDao()

    open fun observeActiveScheduleWithClasses(): Flow<ProcessedSchedule?> {
        return scheduleDao.observeActiveScheduleWithClasses()
            .map { it?.toDomain() }
    }

    open fun observeAllClasses(): Flow<List<ClassSession>> {
        return scheduleDao.observeAllClasses()
            .map { classes -> classes.map { it.toDomain() } }
    }

    open fun observeClassesByDay(dayOfWeek: Int): Flow<List<ClassSession>> {
        return scheduleDao.observeClassesByDay(dayOfWeek)
            .map { classes -> classes.map { it.toDomain() } }
    }

    open fun observeLocalSettings(): Flow<LocalSettings> {
        return localSettingsDao.observeSettings()
            .map { settings -> settings?.toDomain() ?: LocalSettings() }
    }

    open suspend fun getCurrentClass(dayOfWeek: Int, currentTimeMinutes: Int): ClassSession? {
        Log.d("ScheduleTrace", "event=CURRENT_CLASS_LOOKUP day=$dayOfWeek time=$currentTimeMinutes")
        return scheduleDao.getClassesByDay(dayOfWeek)
            .map { it.toDomain() }
            .firstOrNull { it.startMinutes <= currentTimeMinutes && it.endMinutes > currentTimeMinutes }
    }

    open suspend fun getNextClass(dayOfWeek: Int, currentTimeMinutes: Int): ClassSession? {
        Log.d("ScheduleTrace", "event=NEXT_CLASS_LOOKUP day=$dayOfWeek time=$currentTimeMinutes")
        return scheduleDao.getClassesByDay(dayOfWeek)
            .map { it.toDomain() }
            .filter { it.startMinutes > currentTimeMinutes }
            .minByOrNull { it.startMinutes }
    }

    open suspend fun getClassesByDay(dayOfWeek: Int): List<ClassSession> {
        val db = _database ?: return emptyList()
        return db.scheduleDao().getClassesByDay(dayOfWeek).map { it.toDomain() }
    }

    open suspend fun getAllClasses(): List<ClassSession> {
        val db = _database ?: return emptyList()
        return db.scheduleDao().getAllClasses().map { it.toDomain() }
    }

    open suspend fun getAllSubjects(): List<String> = scheduleDao.getUniqueSubjects()

    suspend fun insertCompleteScheduleWithClasses(
        publicationStatus: String,
        sourceFileName: String?,
        classes: List<ClassSessionDraft>,
        loadedAt: Long = System.currentTimeMillis()
    ): Long {
        return database.withTransaction {
            scheduleDao.deleteAllSchedules()
            val scheduleId = scheduleDao.insertSchedule(
                ProcessedScheduleEntity(
                    loadedAt = loadedAt,
                    publicationStatus = publicationStatus,
                    sourceFileName = sourceFileName,
                    totalClasses = classes.size
                )
            )
            scheduleDao.insertClassSessions(
                classes.map { it.toEntity(scheduleId = scheduleId) }
            )
            scheduleId
        }
    }

    suspend fun saveManualClassSessions(classes: List<ClassSessionDraft>) {
        if (classes.isEmpty()) return

        database.withTransaction {
            val scheduleId = scheduleDao.getActiveScheduleId()
                ?: scheduleDao.insertSchedule(
                    ProcessedScheduleEntity(
                        loadedAt = System.currentTimeMillis(),
                        publicationStatus = PublicationStatusManual,
                        sourceFileName = null,
                        totalClasses = 0
                    )
                )

            scheduleDao.insertClassSessions(
                classes.map { it.toEntity(scheduleId = scheduleId) }
            )
            refreshClassCount(scheduleId)
        }
    }

    suspend fun updateClassSession(classSession: ClassSession) {
        database.withTransaction {
            scheduleDao.updateClassSession(classSession.toEntity())
            refreshClassCount(classSession.scheduleId)
        }
    }

    suspend fun replaceClassSessionWithManualSessions(
        classSessionId: Long,
        replacementClasses: List<ClassSessionDraft>
    ) {
        database.withTransaction {
            val current = scheduleDao.getClassSessionById(classSessionId)
            val scheduleId = current?.scheduleId
                ?: scheduleDao.getActiveScheduleId()
                ?: scheduleDao.insertSchedule(
                    ProcessedScheduleEntity(
                        loadedAt = System.currentTimeMillis(),
                        publicationStatus = PublicationStatusManual,
                        sourceFileName = null,
                        totalClasses = 0
                    )
                )

            if (current != null) {
                scheduleDao.deleteClassSessionById(classSessionId)
            }

            if (replacementClasses.isNotEmpty()) {
                scheduleDao.insertClassSessions(
                    replacementClasses.map { it.toEntity(scheduleId = scheduleId) }
                )
            }
            refreshClassCount(scheduleId)
        }
    }

    suspend fun replaceSubjectSessions(
        originalSession: ClassSession,
        replacementClasses: List<ClassSessionDraft>
    ) {
        database.withTransaction {
            scheduleDao.deleteClassSessionsBySubject(
                scheduleId = originalSession.scheduleId,
                subjectName = originalSession.subjectName,
                groupName = originalSession.groupName
            )

            if (replacementClasses.isNotEmpty()) {
                scheduleDao.insertClassSessions(
                    replacementClasses.map { it.toEntity(scheduleId = originalSession.scheduleId) }
                )
            }
            refreshClassCount(originalSession.scheduleId)
        }
    }

    suspend fun deleteClassSession(classSessionId: Long) {
        database.withTransaction {
            val current = scheduleDao.getClassSessionById(classSessionId)
            scheduleDao.deleteClassSessionById(classSessionId)
            current?.let { refreshClassCount(it.scheduleId) }
        }
    }

    suspend fun deleteSubjectSessions(classSession: ClassSession) {
        database.withTransaction {
            scheduleDao.deleteClassSessionsBySubject(
                scheduleId = classSession.scheduleId,
                subjectName = classSession.subjectName,
                groupName = classSession.groupName
            )
            refreshClassCount(classSession.scheduleId)
        }
    }

    open suspend fun deleteCurrentSchedule() {
        database.withTransaction {
            scheduleDao.getActiveScheduleId()?.let { scheduleId ->
                scheduleDao.deleteScheduleById(scheduleId)
            }
        }
    }

    open suspend fun getOrCreateLocalSettings(): LocalSettings {
        Log.d("SettingsPersistence", "event=SETTINGS_LOAD_START")
        return database.withTransaction {
            val current = localSettingsDao.getSettings()
            if (current != null) {
                current.toDomain()
            } else {
                val defaultSettings = LocalSettingsEntity()
                localSettingsDao.insertSettings(defaultSettings)
                defaultSettings.toDomain()
            }
        }.also { Log.d("SettingsPersistence", "event=SETTINGS_LOAD_END") }
    }

    open suspend fun updateLocalSettings(settings: LocalSettings) {
        localSettingsDao.insertSettings(settings.toEntity())
    }

    /** Serializes read-modify-write in Room so concurrent UI changes cannot overwrite siblings. */
    open suspend fun updateLocalSettings(transform: LocalSettings.() -> LocalSettings) {
        database.withTransaction {
            val current = (localSettingsDao.getSettings() ?: LocalSettingsEntity()).toDomain()
            localSettingsDao.insertSettings(current.transform().toEntity())
        }
    }

    open suspend fun clearLocalData() {
        database.withTransaction {
            scheduleDao.deleteAllSchedules()
            localSettingsDao.insertSettings(LocalSettingsEntity())
        }
    }

    suspend fun loadDummySchedule() {
        insertCompleteScheduleWithClasses(
            publicationStatus = PublicationStatusPdfProcessed,
            sourceFileName = "SAES-Horario-demo.pdf",
            classes = DummyClassSessions
        )
    }

    private suspend fun refreshClassCount(scheduleId: Long) {
        scheduleDao.updateScheduleClassCount(
            scheduleId = scheduleId,
            totalClasses = scheduleDao.countClassesForSchedule(scheduleId)
        )
    }

    companion object {
        @Volatile
        private var instance: ScheduleRepository? = null

        fun getInstance(context: Context): ScheduleRepository {
            return instance ?: synchronized(this) {
                instance ?: ScheduleRepository(
                    database = AppDatabase.getInstance(context)
                ).also { instance = it }
            }
        }

        internal fun createForTests(database: AppDatabase): ScheduleRepository = ScheduleRepository(database)
    }
}

private val DummyClassSessions = listOf(
    ClassSessionDraft(
        subjectName = "Calculo",
        groupName = "1CM1",
        dayOfWeek = 1,
        startTime = "07:00",
        endTime = "08:30",
        classroomName = "1104",
        classroomStatus = ClassroomStatusAvailable
    ),
    ClassSessionDraft(
        subjectName = "Fundamentos de programacion",
        groupName = "1CM1",
        dayOfWeek = 2,
        startTime = "10:30",
        endTime = "12:00",
        classroomName = "2201",
        classroomStatus = ClassroomStatusAvailable
    ),
    ClassSessionDraft(
        subjectName = "Matematicas discretas",
        groupName = "1CM1",
        dayOfWeek = 4,
        startTime = "08:30",
        endTime = "10:00",
        classroomName = null,
        classroomStatus = ClassroomStatusPending
    )
)

private fun ProcessedScheduleWithClasses.toDomain(): ProcessedSchedule {
    return ProcessedSchedule(
        id = schedule.id,
        loadedAt = schedule.loadedAt,
        publicationStatus = schedule.publicationStatus,
        sourceFileName = schedule.sourceFileName,
        totalClasses = schedule.totalClasses,
        classes = classes.map { it.toDomain() }
    )
}

private fun ClassSessionEntity.toDomain(): ClassSession {
    return ClassSession(
        id = id,
        scheduleId = scheduleId,
        subjectName = subjectName,
        groupName = groupName,
        teacherName = teacherName,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        classroomName = classroomName,
        classroomStatus = classroomStatus,
        rawText = rawText
    )
}

private fun ClassSession.toEntity(): ClassSessionEntity {
    return ClassSessionEntity(
        id = id,
        scheduleId = scheduleId,
        subjectName = subjectName,
        groupName = groupName,
        teacherName = teacherName,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        classroomName = classroomName,
        classroomStatus = classroomStatus,
        rawText = rawText
    )
}

private fun ClassSessionDraft.toEntity(scheduleId: Long): ClassSessionEntity {
    return ClassSessionEntity(
        scheduleId = scheduleId,
        subjectName = subjectName,
        groupName = groupName,
        teacherName = teacherName,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        classroomName = classroomName,
        classroomStatus = classroomStatus,
        rawText = rawText
    )
}

private fun LocalSettingsEntity.toDomain(): LocalSettings {
    return LocalSettings(
        id = id,
        userAlias = userAlias.ifBlank { DefaultUserAlias },
        themeMode = ThemeMode.fromStoredValue(themeMode),
        schedulePaletteMode = SchedulePaletteMode.fromStoredValue(schedulePaletteMode),
        customScheduleColors = customScheduleColors,
        classNotificationMinutes = classNotificationMinutes,
        enableStartClassNotification = enableStartClassNotification,
        notificationScope = NotificationScope.fromStoredValue(notificationScope),
        notificationSubjects = notificationSubjects,
        interfaceLanguage = interfaceLanguage
    )
}

private fun LocalSettings.toEntity(): LocalSettingsEntity {
    return LocalSettingsEntity(
        id = id,
        userAlias = userAlias.trim().ifBlank { DefaultUserAlias }.take(MaxUserAliasLength),
        themeMode = themeMode.name,
        schedulePaletteMode = schedulePaletteMode.name,
        customScheduleColors = customScheduleColors,
        classNotificationMinutes = classNotificationMinutes,
        enableStartClassNotification = enableStartClassNotification,
        notificationScope = notificationScope.name,
        notificationSubjects = notificationSubjects,
        interfaceLanguage = interfaceLanguage
    )
}
