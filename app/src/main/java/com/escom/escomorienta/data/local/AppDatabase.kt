package com.escom.escomorienta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.escom.escomorienta.data.schedule.local.ClassSessionEntity
import com.escom.escomorienta.data.schedule.local.LocalSettingsDao
import com.escom.escomorienta.data.schedule.local.LocalSettingsEntity
import com.escom.escomorienta.data.schedule.local.ProcessedScheduleEntity
import com.escom.escomorienta.data.schedule.local.ScheduleDao

@Database(
    entities = [
        ProcessedScheduleEntity::class,
        ClassSessionEntity::class,
        LocalSettingsEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun localSettingsDao(): LocalSettingsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "escom_orienta.db"
                )
                    .addMigrations(
                        Migration1To2,
                        Migration2To3,
                        Migration3To4,
                        Migration4To5,
                        Migration5To6
                    )
                    .build()
                    .also { instance = it }
            }
        }

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE class_sessions ADD COLUMN rawText TEXT")
            }
        }

        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE local_settings " +
                        "ADD COLUMN userAlias TEXT NOT NULL DEFAULT 'Alumno'"
                )
            }
        }

        private val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE local_settings " +
                        "ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'"
                )
            }
        }

        private val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE local_settings " +
                        "ADD COLUMN schedulePaletteMode TEXT NOT NULL DEFAULT 'INSTITUTIONAL'"
                )
            }
        }

        private val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE local_settings " +
                        "ADD COLUMN customScheduleColors TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE local_settings " +
                        "ADD COLUMN notificationScope TEXT NOT NULL DEFAULT 'ALL_SUBJECTS'"
                )
                db.execSQL(
                    "ALTER TABLE local_settings " +
                        "ADD COLUMN notificationSubjects TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
