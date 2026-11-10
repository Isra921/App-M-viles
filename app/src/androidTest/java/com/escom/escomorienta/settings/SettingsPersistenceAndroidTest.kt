package com.escom.escomorienta.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.escom.escomorienta.data.local.AppDatabase
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.data.settings.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPersistenceAndroidTest {
    @Test fun SET_01_to_05_atomic_updates_preserve_sibling_preferences() = runBlocking {
        fixture().use { f ->
            f.repository.getOrCreateLocalSettings()
            f.repository.updateLocalSettings { copy(userAlias = "TestAlias") } // SET-01
            assertEquals("TestAlias", f.repository.getOrCreateLocalSettings().userAlias)

            f.repository.updateLocalSettings { copy(themeMode = ThemeMode.LIGHT) } // SET-02/03
            var reloaded = f.repository.getOrCreateLocalSettings()
            assertEquals("TestAlias", reloaded.userAlias)
            assertEquals(ThemeMode.LIGHT, reloaded.themeMode)

            f.repository.updateLocalSettings { copy(enableStartClassNotification = true) } // SET-04
            reloaded = f.repository.getOrCreateLocalSettings()
            assertEquals("TestAlias", reloaded.userAlias)
            assertEquals(ThemeMode.LIGHT, reloaded.themeMode)

            f.repository.updateLocalSettings { copy(enableStartClassNotification = false) } // SET-05
            reloaded = f.repository.getOrCreateLocalSettings()
            assertFalse(reloaded.enableStartClassNotification)
            assertEquals("TestAlias", reloaded.userAlias)
        }
    }

    @Test fun SET_06_and_07_new_repository_and_theme_recreation_do_not_reset_values() = runBlocking {
        fixture().use { f ->
            f.repository.getOrCreateLocalSettings()
            f.repository.updateLocalSettings { copy(userAlias = "Persisted", themeMode = ThemeMode.DARK) }
            val recreatedRepository = ScheduleRepository.createForTests(f.database)
            val restored = recreatedRepository.getOrCreateLocalSettings()
            assertEquals("Persisted", restored.userAlias)
            assertEquals(ThemeMode.DARK, restored.themeMode)
            recreatedRepository.updateLocalSettings { copy(themeMode = ThemeMode.LIGHT) }
            assertEquals("Persisted", recreatedRepository.getOrCreateLocalSettings().userAlias)
        }
    }

    private fun fixture(): Fixture {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        return Fixture(database, ScheduleRepository.createForTests(database))
    }

    private data class Fixture(val database: AppDatabase, val repository: ScheduleRepository) : AutoCloseable {
        override fun close() = database.close()
    }
}
