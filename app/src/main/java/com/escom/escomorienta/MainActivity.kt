package com.escom.escomorienta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.escom.escomorienta.navigation.AppNavigation
import com.escom.escomorienta.ui.settings.AppSettingsViewModel
import com.escom.escomorienta.ui.theme.EscomAppTheme
import com.escom.escomorienta.notifications.ScheduleNotificationScheduler
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val settingsViewModel = ViewModelProvider(this)[AppSettingsViewModel::class.java]
        splashScreen.setKeepOnScreenCondition {
            !settingsViewModel.isInitialSettingsLoaded.value
        }

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val classes by settingsViewModel.classes.collectAsState()
            val settingsLoaded by settingsViewModel.isInitialSettingsLoaded.collectAsState()
            val navController = rememberNavController()
            val onboardingPreferences = remember {
                applicationContext.getSharedPreferences("app_onboarding", MODE_PRIVATE)
            }
            var showNotifOnboarding by remember {
                mutableStateOf(!onboardingPreferences.getBoolean("notif_shown_v1", false) && !settings.enableStartClassNotification)
            }
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    settingsViewModel.setNotificationsEnabled(true)
                }
                onboardingPreferences.edit().putBoolean("notif_shown_v1", true).apply()
                showNotifOnboarding = false
            }


            LaunchedEffect(settings, classes, settingsLoaded) {
                if (settingsLoaded) {
                    ScheduleNotificationScheduler.sync(
                        applicationContext,
                        settings,
                        classes
                    )
                }
            }

            EscomAppTheme(themeMode = settings.themeMode) {
                AppNavigation(navController)
            }

            if (settingsLoaded) {
                if (showNotifOnboarding) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Activa los recordatorios") },
                        text = { Text("¿Quieres recibir avisos antes de tus clases? Puedes configurar la anticipación y las materias desde Ajustes.") },
                        dismissButton = {
                            TextButton(onClick = {
                                onboardingPreferences.edit().putBoolean("notif_shown_v1", true).apply()
                                showNotifOnboarding = false
                            }) { Text("Ahora no") }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    settingsViewModel.setNotificationsEnabled(true)
                                    onboardingPreferences.edit().putBoolean("notif_shown_v1", true).apply()
                                    showNotifOnboarding = false
                                }
                            }) { Text("Activar") }
                        }
                    )
                }
            }
        }
    }
}
