package com.escom.escomorienta.data.schedule.local

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "processed_schedules")
data class ProcessedScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loadedAt: Long,
    val publicationStatus: String,
    val sourceFileName: String?,
    val totalClasses: Int
)

@Entity(
    tableName = "class_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProcessedScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scheduleId"]),
        Index(value = ["dayOfWeek"])
    ]
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
)

@Entity(tableName = "local_settings")
data class LocalSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(defaultValue = "'Alumno'")
    val userAlias: String = "Alumno",
    @ColumnInfo(defaultValue = "'SYSTEM'")
    val themeMode: String = "SYSTEM",
    @ColumnInfo(defaultValue = "'INSTITUTIONAL'")
    val schedulePaletteMode: String = "INSTITUTIONAL",
    @ColumnInfo(defaultValue = "''")
    val customScheduleColors: String = "",
    val classNotificationMinutes: Int = 10,
    val enableStartClassNotification: Boolean = true,
    @ColumnInfo(defaultValue = "'ALL_SUBJECTS'")
    val notificationScope: String = "ALL_SUBJECTS",
    @ColumnInfo(defaultValue = "''")
    val notificationSubjects: String = "",
    val interfaceLanguage: String = "es"
)

data class ProcessedScheduleWithClasses(
    @Embedded
    val schedule: ProcessedScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleId"
    )
    val classes: List<ClassSessionEntity>
)
