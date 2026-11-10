package com.escom.escomorienta.data.schedule

import com.escom.escomorienta.data.settings.ThemeMode
import com.escom.escomorienta.data.settings.SchedulePaletteMode
import com.escom.escomorienta.data.settings.NotificationScope

data class ProcessedSchedule(
    val id: Long,
    val loadedAt: Long,
    val publicationStatus: String,
    val sourceFileName: String?,
    val totalClasses: Int,
    val classes: List<ClassSession>
)

data class ClassSession(
    val id: Long,
    val scheduleId: Long,
    val subjectName: String,
    val groupName: String?,
    val teacherName: String?,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val classroomName: String?,
    val classroomStatus: String,
    val rawText: String? = null
) {
    val startMinutes: Int get() = startTime.toMinutes()
    val endMinutes: Int get() = endTime.toMinutes()

    private fun String.toMinutes(): Int {
        val parts = split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }
}

data class ClassSessionDraft(
    val subjectName: String,
    val groupName: String?,
    val teacherName: String? = null,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val classroomName: String?,
    val classroomStatus: String,
    val rawText: String? = null
) {
    val startMinutes: Int get() = startTime.toMinutes()
    val endMinutes: Int get() = endTime.toMinutes()

    private fun String.toMinutes(): Int {
        val parts = split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }
}

data class LocalSettings(
    val id: Int = LocalSettingsId,
    val userAlias: String = DefaultUserAlias,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val schedulePaletteMode: SchedulePaletteMode = SchedulePaletteMode.INSTITUTIONAL,
    val customScheduleColors: String = "",
    val classNotificationMinutes: Int = 10,
    val enableStartClassNotification: Boolean = true,
    val notificationScope: NotificationScope = NotificationScope.ALL_SUBJECTS,
    val notificationSubjects: String = "",
    val interfaceLanguage: String = "es"
)

const val LocalSettingsId = 1
const val DefaultUserAlias = "Alumno"
const val MaxUserAliasLength = 20
const val PublicationStatusManual = "MANUAL"
const val PublicationStatusPdfProcessed = "PDF_PROCESSED"
const val ScheduleStatusNoRoomsPublished = "NO_ROOMS_PUBLISHED"
const val ScheduleStatusPartialRooms = "PARTIAL_ROOMS"
const val ScheduleStatusComplete = "COMPLETE"
const val ClassroomStatusAvailable = "AVAILABLE"
const val ClassroomStatusPending = "PENDING"
