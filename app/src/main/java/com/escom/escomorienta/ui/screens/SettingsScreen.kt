package com.escom.escomorienta.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Share
import com.escom.escomorienta.data.map.MapSettingsPreferences
import com.escom.escomorienta.data.schedule.export.ScheduleIcsExporter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.escom.escomorienta.data.schedule.DefaultUserAlias
import com.escom.escomorienta.data.schedule.MaxUserAliasLength
import com.escom.escomorienta.data.settings.ThemeMode
import com.escom.escomorienta.data.settings.SchedulePaletteMode
import com.escom.escomorienta.data.settings.NotificationScope
import com.escom.escomorienta.data.settings.decodeSubjectSelection
import com.escom.escomorienta.data.settings.subjectSelectionKey
import com.escom.escomorienta.ui.settings.AppSettingsViewModel
import com.escom.escomorienta.ui.schedule.schedulePaletteColors
import com.escom.escomorienta.ui.schedule.CustomScheduleColors
import com.escom.escomorienta.ui.schedule.decodeCustomScheduleColors
import com.escom.escomorienta.navigation.Routes
import com.escom.escomorienta.data.diagnostics.trace.ExecutionTracer
import com.escom.escomorienta.data.diagnostics.trace.TraceModule
import com.escom.escomorienta.data.diagnostics.trace.TraceStatus
import com.escom.escomorienta.data.diagnostics.trace.TraceEvent
import com.escom.escomorienta.data.diagnostics.trace.StructuredExecutionTracer
import com.escom.escomorienta.BuildConfig
import com.escom.escomorienta.data.diagnostics.CrashDiagnosticManager
import com.escom.escomorienta.data.diagnostics.CrashDiagnosticReport
import com.escom.escomorienta.notifications.ScheduleNotificationScheduler
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val APP_USEFULNESS_SURVEY_URL = "https://forms.cloud.microsoft/r/dJ9CJknd6Z"
private const val APP_FEEDBACK_SURVEY_URL = "https://forms.cloud.microsoft/r/jHyeBJcxXe"

private enum class SettingsDestructiveAction {
    DELETE_SCHEDULE,
    CLEAR_LOCAL_DATA
}

private enum class ReminderProfile(val label: String) {
    RECOMMENDED("Recomendado"),
    MODERATE("Moderado"),
    INTENSIVE("Intensivo"),
    CUSTOM("Personalizado")
}

private data class ReminderConfiguration(
    val profile: ReminderProfile,
    val count: Int,
    val intervalMinutes: Int
) {
    val offsets: List<Int>
        get() = when (profile) {
            ReminderProfile.RECOMMENDED -> listOf(30, 15)
            ReminderProfile.MODERATE -> listOf(20, 10)
            ReminderProfile.INTENSIVE -> listOf(10, 8, 6, 4, 2)
            ReminderProfile.CUSTOM -> (count downTo 1).map { it * intervalMinutes }
        }

    fun encode(): Int {
        return when (profile) {
            ReminderProfile.RECOMMENDED -> 101
            ReminderProfile.MODERATE -> 201
            ReminderProfile.INTENSIVE -> 301
            ReminderProfile.CUSTOM -> 1000 + count * 100 + intervalMinutes
        }
    }
}

private fun Int.toReminderConfiguration(): ReminderConfiguration {
    return when {
        this == 201 -> ReminderConfiguration(ReminderProfile.MODERATE, 2, 10)
        this == 301 -> ReminderConfiguration(ReminderProfile.INTENSIVE, 5, 2)
        this >= 1102 -> {
            val count = ((this - 1000) / 100).coerceIn(1, 5)
            val interval = ((this - 1000) % 100).coerceIn(2, 15)
            ReminderConfiguration(ReminderProfile.CUSTOM, count, interval)
        }
        else -> ReminderConfiguration(ReminderProfile.RECOMMENDED, 2, 15)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val tracer = remember(context) { StructuredExecutionTracer(context.applicationContext) }

    LaunchedEffect(Unit) { 
        android.util.Log.d("SettingsPersistence", "event=SETTINGS_SCREEN_REOPEN") 
        tracer.record(TraceEvent(
            traceId = tracer.currentTraceId() ?: "settings-nav",
            sequence = 0,
            module = TraceModule.SETTINGS,
            stage = "SETTINGS_SCREEN_ENTER",
            eventType = "LIFECYCLE",
            status = TraceStatus.SUCCESS
        ))
    }
    val settingsViewModel: AppSettingsViewModel = viewModel()
    val settings by settingsViewModel.settings.collectAsState()
    val classes by settingsViewModel.classes.collectAsState()
    val subjects = classes.map { it.subjectName }.distinctBy(::subjectSelectionKey).sorted()
    var aliasDraft by remember { mutableStateOf(settings.userAlias) }
    var showAliasDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<SettingsDestructiveAction?>(null) }
    val reminderConfiguration = settings.classNotificationMinutes.toReminderConfiguration()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var themeExpanded by rememberSaveable { mutableStateOf(false) }
    var colorsExpanded by rememberSaveable { mutableStateOf(false) }
    var notificationsExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val crashStore = remember { CrashDiagnosticManager.store(context) }
    var crashReport by remember { mutableStateOf(crashStore.read()) }
    var showCrashReport by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        settingsViewModel.setNotificationsEnabled(granted)
        if (granted) notificationsExpanded = true
    }

    LaunchedEffect(settings.userAlias) {
        aliasDraft = settings.userAlias
    }


    LaunchedEffect(settings.enableStartClassNotification) {
        if (
            settings.enableStartClassNotification &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            settingsViewModel.setNotificationsEnabled(false)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Configuración",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionTitle(text = "Perfil")
                SettingsSectionCard(
                    contentPadding = PaddingValues(0.dp),
                    contentSpacing = 0.dp
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Alias de bienvenida",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = settings.userAlias.ifBlank { DefaultUserAlias },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    aliasDraft = settings.userAlias
                                    showAliasDialog = true
                                }
                            ) {
                                Text(text = "Editar")
                            }
                        }
                    )
                }
            }

            item {
                SettingsSectionTitle(text = "Apariencia")
                SettingsSectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeExpanded = !themeExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tema de la aplicación",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Elige cómo se muestra la aplicación.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = if (themeExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (themeExpanded) "Contraer" else "Expandir"
                        )
                    }

                    AnimatedVisibility(visible = themeExpanded) {
                        Column {
                            ThemeMode.entries.forEach { mode ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = settings.themeMode == mode,
                                            role = Role.RadioButton,
                                            onClick = { settingsViewModel.setThemeMode(mode) }
                                        )
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = settings.themeMode == mode, onClick = null)
                                    Text(
                                        text = mode.displayName(),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionTitle(text = "Personalización del horario")
                SettingsSectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { colorsExpanded = !colorsExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Colores del horario",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Solo cambia los acentos del horario.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = if (colorsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (colorsExpanded) "Contraer" else "Expandir"
                        )
                    }

                    AnimatedVisibility(visible = colorsExpanded) {
                        Column {
                            SchedulePaletteMode.entries.forEach { mode ->
                                val preview = schedulePaletteColors(mode)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = settings.schedulePaletteMode == mode,
                                            role = Role.RadioButton,
                                            onClick = { settingsViewModel.setSchedulePaletteMode(mode) }
                                        )
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.schedulePaletteMode == mode,
                                        onClick = null
                                    )
                                    Text(
                                        text = mode.displayName(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        preview.accents.take(4).forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(color, RoundedCornerShape(6.dp))
                                            )
                                        }
                                    }
                                }
                            }

                            if (settings.schedulePaletteMode == SchedulePaletteMode.CUSTOM) {
                                val custom = decodeCustomScheduleColors(settings.customScheduleColors)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                if (subjects.isEmpty()) {
                                    Text(
                                        text = "Carga un horario para asignar colores por materia.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                subjects.forEach { subject ->
                                    val selectedIndex = custom[subjectSelectionKey(subject)]
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = subject,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2
                                        )
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CustomScheduleColors.forEachIndexed { index, color ->
                                                Surface(
                                                    modifier = Modifier.size(34.dp),
                                                    color = color,
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = if (selectedIndex == index) {
                                                        androidx.compose.foundation.BorderStroke(
                                                            3.dp,
                                                            MaterialTheme.colorScheme.onSurface
                                                        )
                                                    } else null,
                                                    onClick = {
                                                        settingsViewModel.setCustomScheduleColor(subject, index)
                                                    }
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionTitle(text = "Notificaciones y recordatorios")
                SettingsSectionCard(
                    contentPadding = PaddingValues(0.dp),
                    contentSpacing = 0.dp
                ) {
                    ListItem(
                        modifier = Modifier.clickable {
                            notificationsExpanded = !notificationsExpanded
                        },
                        headlineContent = {
                            Text(
                                text = "Recordatorios de clase",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Activa los avisos locales antes de cada clase.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = settings.enableStartClassNotification,
                                    onCheckedChange = { enabled ->
                                        if (!enabled) {
                                            settingsViewModel.setNotificationsEnabled(false)
                                        } else if (
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        } else {
                                            settingsViewModel.setNotificationsEnabled(true)
                                            notificationsExpanded = true
                                        }
                                    }
                                )
                                Icon(
                                    imageVector = if (notificationsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    if (!settings.enableStartClassNotification && notificationsExpanded) {
                        Text(
                            text = "Los recordatorios están desactivados. Actívalos para configurar la anticipación y elegir las materias.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = settings.enableStartClassNotification && notificationsExpanded,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "Materias con recordatorio",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            NotificationScope.entries.forEach { notificationScope ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = settings.notificationScope == notificationScope,
                                            role = Role.RadioButton,
                                            onClick = {
                                                settingsViewModel.setNotificationScope(notificationScope)
                                            }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.notificationScope == notificationScope,
                                        onClick = null
                                    )
                                    Text(
                                        text = if (notificationScope == NotificationScope.ALL_SUBJECTS) {
                                            "Todas las materias"
                                        } else {
                                            "Solo materias seleccionadas"
                                        }
                                    )
                                }
                            }

                            if (settings.notificationScope == NotificationScope.SELECTED_SUBJECTS) {
                                val selectedSubjects = decodeSubjectSelection(settings.notificationSubjects)
                                if (subjects.isEmpty()) {
                                    Text(
                                        text = "Carga un horario para seleccionar materias.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                subjects.forEach { subject ->
                                    val key = subjectSelectionKey(subject)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                settingsViewModel.setSubjectNotificationEnabled(
                                                    subject,
                                                    key !in selectedSubjects
                                                )
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = key in selectedSubjects,
                                            onCheckedChange = null
                                        )
                                        Text(text = subject, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "Configuración rápida",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReminderProfile.entries.forEach { profile ->
                                    FilterChip(
                                        selected = reminderConfiguration.profile == profile,
                                        onClick = {
                                            val updated = when (profile) {
                                                ReminderProfile.RECOMMENDED ->
                                                    ReminderConfiguration(profile, 2, 15)
                                                ReminderProfile.MODERATE ->
                                                    ReminderConfiguration(profile, 2, 10)
                                                ReminderProfile.INTENSIVE ->
                                                    ReminderConfiguration(profile, 5, 2)
                                                ReminderProfile.CUSTOM ->
                                                    ReminderConfiguration(profile, 2, 5)
                                            }
                                            settingsViewModel.setReminderConfiguration(updated.encode())
                                        },
                                        label = { Text(text = profile.label) }
                                    )
                                }
                            }

                            if (reminderConfiguration.profile == ReminderProfile.CUSTOM) {
                                Text(
                                    text = "Cantidad: ${reminderConfiguration.count}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = reminderConfiguration.count.toFloat(),
                                    onValueChange = { value ->
                                        settingsViewModel.setReminderConfiguration(
                                            reminderConfiguration.copy(
                                                count = value.roundToInt().coerceIn(1, 5)
                                            ).encode()
                                        )
                                    },
                                    valueRange = 1f..5f,
                                    steps = 3
                                )

                                Text(
                                    text = "Intervalo: ${reminderConfiguration.intervalMinutes} minutos",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = reminderConfiguration.intervalMinutes.toFloat(),
                                    onValueChange = { value ->
                                        settingsViewModel.setReminderConfiguration(
                                            reminderConfiguration.copy(
                                                intervalMinutes = value.roundToInt().coerceIn(2, 15)
                                            ).encode()
                                        )
                                    },
                                    valueRange = 2f..15f,
                                    steps = 12
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "Las notificaciones se enviarán:",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = reminderConfiguration.offsets.joinToString(
                                            separator = "\n"
                                        ) { "$it min antes de la clase" },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    settingsViewModel.setReminderConfiguration(
                                        ReminderConfiguration(
                                            profile = ReminderProfile.RECOMMENDED,
                                            count = 2,
                                            intervalMinutes = 15
                                        ).encode()
                                    )
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(text = "Restablecer configuración recomendada")
                            }

                            val canScheduleExact = remember(settings.enableStartClassNotification) {
                                ScheduleNotificationScheduler.canScheduleExactAlarms(context)
                            }
                            val isBatteryOptimized = remember(settings.enableStartClassNotification) {
                                !ScheduleNotificationScheduler.isIgnoringBatteryOptimizations(context)
                            }
                            val isPocoOrXiaomi = remember {
                                val m = Build.MANUFACTURER.lowercase()
                                m.contains("xiaomi") || m.contains("poco") || m.contains("redmi")
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Permiso de alarmas exactas no concedido",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Android requiere autorización para avisar con puntualidad exacta. Si no se concede, el sistema podría demorar o agrupar los recordatorios.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        TextButton(
                                            onClick = {
                                                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Conceder permiso")
                                        }
                                    }
                                }
                            }

                            if (isBatteryOptimized || isPocoOrXiaomi) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = if (isPocoOrXiaomi) "Recomendación para dispositivos POCO / Xiaomi" else "Optimización de batería",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = if (isPocoOrXiaomi) {
                                                "En MIUI / HyperOS, para evitar que las notificaciones se pierdan o acumulen al estar la pantalla bloqueada, configura la batería de la app en 'Sin restricciones' y activa 'Inicio automático'."
                                            } else {
                                                "Si las notificaciones se demoran con la pantalla apagada, desactiva la optimización de batería para ESCOM Orienta en los ajustes del sistema."
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        TextButton(
                                            onClick = {
                                                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Ajustes de batería")
                                        }
                                    }
                                }
                            }

                            if (BuildConfig.DEBUG) {
                                FilledTonalButton(
                                    onClick = {
                                        val scheduled = ScheduleNotificationScheduler.scheduleDebugNotification(context, 2)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (scheduled) "Prueba programada para aproximadamente 2 minutos."
                                                else "No se pudo programar: revisa el permiso de notificaciones."
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Programar notificación de prueba (debug)") }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionTitle(text = "Mejorar ESCOM Orienta")
                SettingsSectionCard(contentPadding = PaddingValues(0.dp), contentSpacing = 0.dp) {
                    ListItem(
                        modifier = Modifier.clickable {
                            openExternalLink(context, APP_USEFULNESS_SURVEY_URL)
                        },
                        headlineContent = {
                            Text(
                                text = "Encuesta de utilidad",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "¿Qué tan útil te resulta ESCOM Orienta?",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir encuesta",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        modifier = Modifier.clickable {
                            openExternalLink(context, APP_FEEDBACK_SURVEY_URL)
                        },
                        headlineContent = {
                            Text(
                                text = "Encuesta de satisfacción",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Comparte tu opinión para mejorar la aplicación.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Feedback,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir encuesta",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )


                    if (crashReport != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ListItem(
                            modifier = Modifier.clickable { showCrashReport = true },
                            headlineContent = { Text("Diagnóstico local", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Se detectó un cierre inesperado.", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Filled.BugReport, null, tint = MaterialTheme.colorScheme.error) },
                            trailingContent = {
                                TextButton(onClick = { shareCrashDiagnostic(context, crashReport) }) {
                                    Icon(Icons.Filled.Share, null)
                                }
                            }
                        )
                    }
                }
            }

            item {
                SettingsSectionTitle(text = "Datos locales")
                SettingsSectionCard(
                    contentPadding = PaddingValues(0.dp),
                    contentSpacing = 0.dp
                ) {


                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Horario procesado",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Elimina las materias importadas o capturadas.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    pendingAction = SettingsDestructiveAction.DELETE_SCHEDULE
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Eliminar",
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Limpiar información local",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Restablece horario, alias, ajustes y búsquedas recientes.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    pendingAction = SettingsDestructiveAction.CLEAR_LOCAL_DATA
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(text = "Limpiar")
                            }
                        }
                    )
                }
            }

            item {
                SettingsSectionTitle(text = "Acerca de la aplicación")
                SettingsSectionCard(
                    contentPadding = PaddingValues(0.dp),
                    contentSpacing = 0.dp
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "ESCOM Orienta",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Build ${BuildConfig.BUILD_COMMIT} / ${BuildConfig.BUILD_TYPE}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAliasDialog) {
        AlertDialog(
            onDismissRequest = { showAliasDialog = false },
            title = { Text(text = "Editar alias") },
            text = {
                OutlinedTextField(
                    value = aliasDraft,
                    onValueChange = { value ->
                        aliasDraft = value.take(MaxUserAliasLength)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Alias") },
                    supportingText = {
                        Text(text = "${aliasDraft.length}/$MaxUserAliasLength")
                    },
                    singleLine = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showAliasDialog = false }) {
                    Text(text = "Cancelar")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedAlias = aliasDraft.trim()
                            .ifBlank { DefaultUserAlias }
                            .take(MaxUserAliasLength)
                        aliasDraft = savedAlias
                        settingsViewModel.saveAlias(savedAlias)
                        showAliasDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Alias guardado")
                        }
                    }
                ) {
                    Text(text = "Guardar")
                }
            }
        )
    }

    if (showCrashReport) {
        crashReport?.let { report ->
            AlertDialog(
                onDismissRequest = { showCrashReport = false },
                title = { Text("Diagnóstico local") },
                text = { Text(crashDiagnosticSummary(report)) },
                confirmButton = {
                    TextButton(onClick = { shareCrashDiagnostic(context, report) }) { Text("Compartir") }
                },
                dismissButton = {
                    TextButton(onClick = { showCrashReport = false }) { Text("Cerrar") }
                }
            )
        }
    }

    pendingAction?.let { action ->
        val clearEverything = action == SettingsDestructiveAction.CLEAR_LOCAL_DATA
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = {
                Text(
                    text = if (clearEverything) {
                        "¿Limpiar información local?"
                    } else {
                        "¿Eliminar horario?"
                    }
                )
            },
            text = {
                Text(
                    text = if (clearEverything) {
                        "Se eliminarán el horario, el alias, los ajustes y las búsquedas recientes. Esta acción no se puede deshacer."
                    } else {
                        "Se eliminarán todas las clases guardadas en este dispositivo."
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(text = "Cancelar")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingAction = null
                        if (clearEverything) {
                            settingsViewModel.clearLocalData()
                            aliasDraft = DefaultUserAlias
                            scope.launch {
                                snackbarHostState.showSnackbar("Información local restablecida")
                            }
                        } else {
                            settingsViewModel.deleteSchedule()
                            scope.launch {
                                snackbarHostState.showSnackbar("Horario eliminado")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = if (clearEverything) "Limpiar" else "Eliminar")
                }
            }
        )
    }
}

private fun openExternalLink(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun shareCrashDiagnostic(context: android.content.Context, report: CrashDiagnosticReport?) {
    if (report == null) return
    runCatching {
        val uri = CrashDiagnosticManager.shareUri(context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir diagnóstico"))
    }
}

internal fun crashDiagnosticSummary(report: CrashDiagnosticReport): String = buildString {
    appendLine("Versión: ${report.appVersion} (${report.versionCode})")
    appendLine("Build: ${report.buildCommit} / ${report.buildType}")
    appendLine("Android: ${report.androidVersion} (API ${report.apiLevel})")
    appendLine("Dispositivo: ${report.manufacturer} ${report.model}")
    appendLine("ABI: ${report.abis.joinToString()}")
    appendLine("Excepción: ${report.exceptionClass}")
    appendLine("Hilo: ${report.thread}")
    appendLine("Etapa: ${report.lifecycleStage}")
    appendLine("Fecha UTC: ${report.timestampUtc}")
    appendLine()
    append(report.stackTrace.take(3_000))
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSectionCard(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    contentSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content
        )
    }
}

private fun ThemeMode.displayName(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "Seguir sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }
}

private fun SchedulePaletteMode.displayName(): String = when (this) {
    SchedulePaletteMode.INSTITUTIONAL -> "Institucional ESCOM-IPN"
    SchedulePaletteMode.PASTEL -> "Pastel"
    SchedulePaletteMode.WARM -> "Cálida"
    SchedulePaletteMode.ACCESSIBLE -> "Alto contraste"
    SchedulePaletteMode.COLD -> "Fría"
    SchedulePaletteMode.CUSTOM -> "Personalizada"
}
