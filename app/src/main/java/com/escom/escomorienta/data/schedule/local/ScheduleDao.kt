package com.escom.escomorienta.data.schedule.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insertSchedule(schedule: ProcessedScheduleEntity): Long

    @Insert
    suspend fun insertClassSessions(classes: List<ClassSessionEntity>): List<Long>

    @Update
    suspend fun updateClassSession(classSession: ClassSessionEntity)

    @Delete
    suspend fun deleteClassSession(classSession: ClassSessionEntity)

    @Query("DELETE FROM class_sessions WHERE id = :classSessionId")
    suspend fun deleteClassSessionById(classSessionId: Long)

    @Query(
        """
        DELETE FROM class_sessions
        WHERE scheduleId = :scheduleId
        AND subjectName = :subjectName
        AND ((groupName IS NULL AND :groupName IS NULL) OR groupName = :groupName)
        """
    )
    suspend fun deleteClassSessionsBySubject(
        scheduleId: Long,
        subjectName: String,
        groupName: String?
    )

    @Query("DELETE FROM processed_schedules WHERE id = :scheduleId")
    suspend fun deleteScheduleById(scheduleId: Long)

    @Query("DELETE FROM processed_schedules")
    suspend fun deleteAllSchedules()

    @Query("SELECT id FROM processed_schedules ORDER BY loadedAt DESC, id DESC LIMIT 1")
    suspend fun getActiveScheduleId(): Long?

    @Query("SELECT * FROM class_sessions WHERE id = :classSessionId LIMIT 1")
    suspend fun getClassSessionById(classSessionId: Long): ClassSessionEntity?

    @Query("SELECT COUNT(*) FROM class_sessions WHERE scheduleId = :scheduleId")
    suspend fun countClassesForSchedule(scheduleId: Long): Int

    @Query("UPDATE processed_schedules SET totalClasses = :totalClasses WHERE id = :scheduleId")
    suspend fun updateScheduleClassCount(scheduleId: Long, totalClasses: Int)

    @Transaction
    @Query("SELECT * FROM processed_schedules ORDER BY loadedAt DESC, id DESC LIMIT 1")
    fun observeActiveScheduleWithClasses(): Flow<ProcessedScheduleWithClasses?>

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeAllClasses(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun observeClassesByDay(dayOfWeek: Int): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getClassesByDay(dayOfWeek: Int): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllClasses(): List<ClassSessionEntity>

    @Query("SELECT DISTINCT subjectName FROM class_sessions")
    suspend fun getUniqueSubjects(): List<String>
}
