package com.escom.escomorienta.data.schedule.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalSettingsDao {
    @Query("SELECT * FROM local_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<LocalSettingsEntity?>

    @Query("SELECT * FROM local_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): LocalSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: LocalSettingsEntity)

    @Update
    suspend fun updateSettings(settings: LocalSettingsEntity)
}
