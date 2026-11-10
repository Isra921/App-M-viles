package com.escom.escomorienta.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.escom.escomorienta.data.schedule.export.ScheduleIcsExporter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.escom.escomorienta.data.schedule.ClassSession
import com.escom.escomorienta.data.schedule.ClassSessionDraft
import com.escom.escomorienta.data.schedule.ClassroomStatusAvailable
import com.escom.escomorienta.data.schedule.ClassroomStatusPending
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.data.schedule.LocalSettings
import com.escom.escomorienta.data.schedule.ScheduleConflictDetector
import com.escom.escomorienta.data.schedule.isKnownClassroom
import com.escom.escomorienta.data.schedule.normalizeKnownClassroom
import com.escom.escomorienta.navigation.Routes
import com.escom.escomorienta.ui.schedule.ScheduleViewModel
import com.escom.escomorienta.ui.schedule.ScheduleViewModelFactory
import com.escom.escomorienta.ui.schedule.scheduleAccentFor
import com.escom.escomorienta.ui.schedule.schedulePaletteColors
import com.escom.escomorienta.ui.schedule.decodeCustomScheduleColors
import com.escom.escomorienta.ui.schedule.scheduleSubjectKey
import com.escom.escomorienta.ui.schedule.subjectAccentColors
import com.escom.escomorienta.ui.theme.EscomAppTheme
import kotlinx.coroutines.launch
import java.text.Normalizer

private enum class ScheduleViewMode {
    DAY_LIST,
    WEEK_MATRIX
}

private enum class ScheduleConfirmation {
    DELETE_ALL,
    REPLACE_WITH_PDF
}

private enum class SchoolDay(
    val dayOfWeek: Int,
    val shortLabel: String,
    val fullName: String
) {
    MONDAY(1, "L", "Lunes"),
    TUESDAY(2, "M", "Martes"),
    WEDNESDAY(3, "M", "Miercoles"),
    THURSDAY(4, "J", "Jueves"),
    FRIDAY(5, "V", "Viernes"),
    SATURDAY(6, "S", "Sabado");

    companion object {
        fun fromDayOfWeek(dayOfWeek: Int): SchoolDay {
            return entries.firstOrNull { it.dayOfWeek == dayOfWeek } ?: MONDAY
        }
    }
}

private data class TimeSlot(
    val startTime: String,
    val endTime: String,
    val isBreak: Boolean = false
) {
    val label: String = "$startTime - $endTime"
}

private data class DayScheduleInput(
    val day: SchoolDay,
    val enabled: Boolean = false,
    val startTime: String = DefaultClassSlots.first().startTime,
    val endTime: String = DefaultClassSlots.first().endTime
)

private data class ScheduleFormState(
    val subject: String = "",
    val group: String = "1CM1",
    val teacher: String = "",
    val classroom: String = "",
    val daySchedules: List<DayScheduleInput> = SchoolDay.entries.filter { it != SchoolDay.SATURDAY }.map { DayScheduleInput(day = it) }
)

private val DefaultClassSlots = listOf(
    TimeSlot("07:00", "08:30"),
    TimeSlot("08:30", "10:00"),
    TimeSlot("10:30", "12:00"),
    TimeSlot("12:00", "13:30"),
    TimeSlot("13:30", "15:00"),
    TimeSlot("15:00", "16:30"),
    TimeSlot("16:30", "18:00"),
    TimeSlot("18:30", "20:00"),
    TimeSlot("20:00", "21:30")
)

private val BreakSlots = listOf(
    TimeSlot("10:00", "10:30", isBreak = true),
    TimeSlot("18:00", "18:30", isBreak = true)
)

private val SuggestedSubjects = listOf(
    "Calculo",
    "Analisis vectorial",
    "Matematicas discretas",
    "Comunicacion oral y escrita",
    "Fundamentos de programacion",
    "Fundamentos economicos",
    "Mecanica y electromagnetismo",
    "Introduccion a la ciencia de datos"
)

private val MatrixTimeColumnWidth = 98.dp
private val MatrixDayColumnWidth = 112.dp
private val MatrixHeaderHeight = 42.dp
private val MatrixRowHeight = 80.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val repository = remember {
        ScheduleRepository.getInstance(context.applicationContext)
    }
    val scheduleViewModel: ScheduleViewModel = viewModel(
        viewModelStoreOwner = context as ComponentActivity,
        factory = ScheduleViewModelFactory(context.applicationContext)
    )
    val scheduleUiState by scheduleViewModel.uiState.collectAsState()
    val loadedClasses by scheduleViewModel.classes.collectAsState()
    val classes = loadedClasses.orEmpty()
    val localSettings by repository.observeLocalSettings().collectAsState(initial = LocalSettings())
    val scheduleColors = schedulePaletteColors(localSettings.schedulePaletteMode)
    val subjectColors = subjectAccentColors(
        subjects = classes.map { it.subjectName },
        palette = scheduleColors.accents,
        customIndexes = decodeCustomScheduleColors(localSettings.customScheduleColors)
    )
    var selectedDay by remember {
        mutableStateOf(SchoolDay.fromDayOfWeek(java.time.LocalDate.now().dayOfWeek.value))
    }
    var viewMode by remember { mutableStateOf(ScheduleViewMode.DAY_LIST) }
    var editingClass by remember { mutableStateOf<ClassSession?>(null) }
    var selectedClassDetails by remember { mutableStateOf<ClassSession?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingConfirmation by remember { mutableStateOf<ScheduleConfirmation?>(null) }
    var pendingConflictingDrafts by remember { mutableStateOf<List<ClassSessionDraft>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openClassroomInMap: (ClassSession) -> Unit = { item ->
        if (item.canOpenClassroomInMap) {
            navController.navigate(item.classroomName.toScheduleMapRoute()) {
                launchSingleTop = true
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("No se ubica el salon dentro del mapa.")
            }
        }
    }
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { scheduleViewModel.importPdf(it) }
    }
    val openPdfPicker = {
        pdfPickerLauncher.launch(arrayOf("application/pdf"))
    }
    val onExportIcs: () -> Unit = {
        val shareIntent = ScheduleIcsExporter.createShareIntent(context, classes)
        if (shareIntent != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Exportar horario escolar"))
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("No hay materias registradas para exportar.")
            }
        }
    }

    LaunchedEffect(Unit) {
        repository.getOrCreateLocalSettings()
        scheduleViewModel.loadSchedule()
    }

    LaunchedEffect(classes, localSettings) {
        if (loadedClasses != null) {
            com.escom.escomorienta.notifications.ScheduleNotificationScheduler.sync(
                context.applicationContext,
                localSettings,
                classes
            )
        }
    }

    LaunchedEffect(scheduleUiState.successMessage, scheduleUiState.errorMessage) {
        val message = scheduleUiState.errorMessage ?: scheduleUiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            scheduleViewModel.clearMessages()
        }
    }

    LaunchedEffect(classes) {
        if (classes.isNotEmpty() && classes.none { it.dayOfWeek == selectedDay.dayOfWeek }) {
            selectedDay = SchoolDay.fromDayOfWeek(
                classes.minWith(compareBy<ClassSession> { it.dayOfWeek }.thenBy { it.startMinutes }).dayOfWeek
            )
        }
    }

    val todayDow = remember { java.time.LocalDate.now().dayOfWeek.value }
    val classCountByDay = remember(classes) {
        classes.groupBy { it.dayOfWeek }.mapValues { it.value.size }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            EscomBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val uniqueSubjectsCount = remember(classes) { classes.distinctBy { it.subjectName }.size }
                ScheduleHeader(
                    hasClasses = classes.isNotEmpty(),
                    classesCount = classes.size,
                    subjectsCount = uniqueSubjectsCount,
                    onManualClick = {
                        editingClass = null
                        showEditor = true
                    },
                    onPdfClick = {
                        pendingConfirmation = ScheduleConfirmation.REPLACE_WITH_PDF
                    },
                    onExportIcs = onExportIcs,
                    onClearClick = {
                        pendingConfirmation = ScheduleConfirmation.DELETE_ALL
                    }
                )
            }

            if (scheduleUiState.isProcessingPdf) {
                item {
                    PdfProcessingCard(fileName = scheduleUiState.processingFileName)
                }
            }

            if (loadedClasses == null) {
                item {
                    ScheduleSkeleton()
                }
            } else if (classes.isEmpty()) {
                item {
                    EmptyScheduleCard(
                        onManualClick = {
                            editingClass = null
                            showEditor = true
                        },
                        onPdfClick = {
                            openPdfPicker()
                        }
                    )
                }
            } else {
                item {
                    ScheduleViewModeSelector(
                        selectedMode = viewMode,
                        onModeSelected = { viewMode = it }
                    )
                }

                when (viewMode) {
                    ScheduleViewMode.DAY_LIST -> {
                        item {
                            DaySelector(
                                selectedDay = selectedDay,
                                onDaySelected = { selectedDay = it },
                                todayDow = todayDow,
                                classCountByDay = classCountByDay
                            )
                        }

                        val classesForDay = classes
                            .filter { it.dayOfWeek == selectedDay.dayOfWeek }
                            .sortedBy { it.startMinutes }

                        if (classesForDay.isEmpty()) {
                            item {
                                EmptyDayCard(
                                    day = selectedDay,
                                    onManualClick = {
                                        editingClass = null
                                        showEditor = true
                                    }
                                )
                            }
                        } else {
                            val now = java.time.LocalTime.now()
                            val currentMinutes = now.hour * 60 + now.minute
                            val nextUpcomingClassId = if (selectedDay.dayOfWeek == todayDow) {
                                classesForDay.firstOrNull { it.startMinutes > currentMinutes }?.id
                            } else null

                            items(
                                count = classesForDay.size,
                                key = { index -> classesForDay[index].id }
                            ) { index ->
                                val classItem = classesForDay[index]
                                val isCurrent = (classItem.dayOfWeek == todayDow) &&
                                    (currentMinutes in classItem.startMinutes until classItem.endMinutes)
                                val remainingMinutes = if (isCurrent) (classItem.endMinutes - currentMinutes) else 0
                                val isNext = (classItem.id == nextUpcomingClassId)
                                val minutesUntil = if (isNext) (classItem.startMinutes - currentMinutes) else 0

                                ScheduleClassCard(
                                    item = classItem,
                                    accentColor = subjectColors.getValue(
                                        scheduleSubjectKey(classItem.subjectName)
                                    ),
                                    isCurrentClass = isCurrent,
                                    remainingMinutes = remainingMinutes,
                                    isNextClass = isNext,
                                    minutesUntil = minutesUntil,
                                    onClick = {
                                        editingClass = classItem
                                        showEditor = true
                                    },
                                    onClassroomClick = {
                                        openClassroomInMap(classItem)
                                    },
                                    onDelete = {
                                        val deletedSessions = classes.filter {
                                            it.belongsToSameEditableSubject(classItem)
                                        }
                                        scope.launch {
                                            repository.deleteSubjectSessions(classItem)
                                            val result = snackbarHostState.showSnackbar(
                                                message = "${classItem.subjectName} eliminada",
                                                actionLabel = "Deshacer"
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                repository.saveManualClassSessions(
                                                    deletedSessions.map(ClassSession::toDraft)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    ScheduleViewMode.WEEK_MATRIX -> {
                        item {
                            WeeklyMatrix(
                                classes = classes.sortedWith(
                                    compareBy<ClassSession> { it.startMinutes }.thenBy { it.dayOfWeek }
                                ),
                                palette = scheduleColors.accents,
                                onAccent = scheduleColors.onAccent,
                                subjectColors = subjectColors,
                                onClassClick = { item ->
                                    selectedClassDetails = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ScheduleClassEditorDialog(
            initialClass = editingClass,
            relatedClasses = editingClass?.let { selected ->
                classes.filter { it.belongsToSameEditableSubject(selected) }
            }.orEmpty(),
            onDismiss = {
                showEditor = false
                editingClass = null
            },
            onSave = { savedClasses ->
                val existingWithoutEditedSubject = editingClass?.let { edited ->
                    classes.filterNot { it.belongsToSameEditableSubject(edited) }
                } ?: classes
                if (ScheduleConflictDetector.conflictsWithExisting(existingWithoutEditedSubject, savedClasses)) {
                    pendingConflictingDrafts = savedClasses
                    return@ScheduleClassEditorDialog
                }
                scope.launch {
                    if (editingClass != null) {
                        repository.replaceSubjectSessions(
                            originalSession = editingClass!!,
                            replacementClasses = savedClasses
                        )
                    } else {
                        repository.saveManualClassSessions(savedClasses)
                    }
                    savedClasses.firstOrNull()?.let {
                        selectedDay = SchoolDay.fromDayOfWeek(it.dayOfWeek)
                    }
                    showEditor = false
                    editingClass = null
                }
            }
        )
    }

    pendingConflictingDrafts?.let { drafts ->
        AlertDialog(
            onDismissRequest = { pendingConflictingDrafts = null },
            title = { Text("Conflicto de horario") },
            text = { Text("La materia se empalma con una clase registrada. Si continúas, ambas se conservarán y la primera registrada tendrá prioridad visual en la matriz.") },
            dismissButton = { TextButton(onClick = { pendingConflictingDrafts = null }) { Text("Regresar") } },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (editingClass != null) repository.replaceSubjectSessions(editingClass!!, drafts)
                        else repository.saveManualClassSessions(drafts)
                        drafts.firstOrNull()?.let { selectedDay = SchoolDay.fromDayOfWeek(it.dayOfWeek) }
                        pendingConflictingDrafts = null
                        showEditor = false
                        editingClass = null
                    }
                }) { Text("Conservar ambas") }
            }
        )
    }

    selectedClassDetails?.let { item ->
        ScheduleClassDetailsDialog(
            item = item,
            accentColor = subjectColors.getValue(scheduleSubjectKey(item.subjectName)),
            onDismiss = {
                selectedClassDetails = null
            },
            onViewMap = {
                selectedClassDetails = null
                openClassroomInMap(item)
            },
            onEdit = {
                editingClass = item
                selectedClassDetails = null
                showEditor = true
            }
        )
    }

    pendingConfirmation?.let { confirmation ->
        ScheduleConfirmationDialog(
            confirmation = confirmation,
            onDismiss = { pendingConfirmation = null },
            onConfirm = {
                when (confirmation) {
                    ScheduleConfirmation.DELETE_ALL -> {
                        scheduleViewModel.deleteSchedule()
                        selectedDay = SchoolDay.MONDAY
                    }
                    ScheduleConfirmation.REPLACE_WITH_PDF -> {
                        openPdfPicker()
                    }
                }
                pendingConfirmation = null
            }
        )
    }

    if (scheduleUiState.pendingPdfImport != null) {
        LoadTeachersConfirmationDialog(
            onDismiss = { scheduleViewModel.cancelPendingPdfImport() },
            onLoadWithoutTeachers = {
                scheduleViewModel.confirmPendingPdfImport(includeTeachers = false)
            },
            onLoadWithTeachers = {
                scheduleViewModel.confirmPendingPdfImport(includeTeachers = true)
            }
        )
    }
}

@Composable
private fun ScheduleHeader(
    hasClasses: Boolean,
    classesCount: Int,
    subjectsCount: Int,
    onManualClick: () -> Unit,
    onPdfClick: () -> Unit,
    onExportIcs: () -> Unit,
    onClearClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mi Horario",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                text = if (hasClasses) {
                    "$subjectsCount materias · $classesCount sesiones semanales"
                } else {
                    "Planifica tu semestre académico"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp
            )
        }

        if (hasClasses) {
            FilledTonalIconButton(
                onClick = onManualClick,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Agregar materia",
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Cambiar PDF") },
                        leadingIcon = {
                            Icon(Icons.Filled.Description, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onPdfClick()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Eliminar horario",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onClearClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScheduleCard(
    onManualClick: () -> Unit,
    onPdfClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(46.dp)
                )
            }

            Text(
                text = "No hay horario cargado",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Puedes cargar el PDF del SAES o capturar tus clases manualmente.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            Button(
                onClick = onPdfClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Cargar horario PDF")
            }

            OutlinedButton(
                onClick = onManualClick,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Crear horario manual", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ScheduleSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(22.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(7.dp)
                            )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(16.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(7.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfProcessingCard(
    fileName: String?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "Procesando PDF...",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = fileName ?: "Validando estructura del horario SAES",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProcessingStepText(text = "Validando archivo PDF")
                ProcessingStepText(text = "Detectando celdas académicas")
                ProcessingStepText(text = "Extrayendo materias, horarios y salones")
            }
        }
    }
}

@Composable
private fun ProcessingStepText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ScheduleViewModeSelector(
    selectedMode: ScheduleViewMode,
    onModeSelected: (ScheduleViewMode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ScheduleModeButton(
                text = "Día a día",
                selected = selectedMode == ScheduleViewMode.DAY_LIST,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ScheduleViewMode.DAY_LIST) }
            )

            ScheduleModeButton(
                text = "Matriz semanal",
                selected = selectedMode == ScheduleViewMode.WEEK_MATRIX,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ScheduleViewMode.WEEK_MATRIX) }
            )
        }
    }
}

@Composable
private fun ScheduleModeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "scheduleModeBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "scheduleModeContent"
    )

    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(18.dp),
        color = background,
        shadowElevation = if (selected) 2.dp else 0.dp,
        tonalElevation = if (selected) 2.dp else 0.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDay: SchoolDay,
    onDaySelected: (SchoolDay) -> Unit,
    todayDow: Int,
    classCountByDay: Map<Int, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SchoolDay.entries.forEach { day ->
                val isSelected = selectedDay == day
                val isToday = day.dayOfWeek == todayDow
                val count = classCountByDay[day.dayOfWeek] ?: 0

                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(200),
                    label = "daySelectorBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(200),
                    label = "daySelectorText"
                )

                Surface(
                    onClick = { onDaySelected(day) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = containerColor,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = day.shortLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = if (count > 0) "$count" else "·",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCard(
    day: SchoolDay,
    onManualClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            RoundedCornerShape(18.dp)
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sin clases el ${day.fullName.lowercase()}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Puedes registrar una materia para este día o seleccionar otro día arriba.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(onClick = onManualClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Agregar materia")
            }
        }
    }
}

@Composable
private fun ScheduleClassCard(
    item: ClassSession,
    accentColor: Color,
    isCurrentClass: Boolean = false,
    remainingMinutes: Int = 0,
    isNextClass: Boolean = false,
    minutesUntil: Int = 0,
    onClick: () -> Unit,
    onClassroomClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirmation = true
                false
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.75f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled ||
                dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
                dismissState.progress > 0.05f
            if (isSwiping) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null
                        )
                        Text(
                            text = "Eliminar",
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isCurrentClass) 1.5.dp else 1.dp,
                    color = when {
                        isCurrentClass -> MaterialTheme.colorScheme.primary
                        isNextClass -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = when {
                    isCurrentClass -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f).compositeOver(MaterialTheme.colorScheme.surface)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (isCurrentClass) 3.dp else 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(accentColor, RoundedCornerShape(3.dp))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status badge row (if in progress or next class)
                    if (isCurrentClass || isNextClass) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isCurrentClass) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(Color(0xFF4ADE80), CircleShape)
                                        )
                                        Text(
                                            text = "EN CURSO AHORA",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                        if (remainingMinutes > 0) {
                                            Text(
                                                text = "• Termina en $remainingMinutes min",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (minutesUntil in 1..60) "PRÓXIMA CLASE • en $minutesUntil min" else "PRÓXIMA CLASE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar materia",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Subject title with generous space for long names
                    Text(
                        text = item.subjectName.trim(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp
                    )

                    // Group badge and Teacher row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Grupo ${item.groupName?.trim().orEmpty().ifBlank { "—" }}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        val teacher = item.teacherName?.trim()?.takeIf { it.isNotBlank() }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (teacher != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = teacher ?: "Sin profesor",
                                color = if (teacher != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isCurrentClass && !isNextClass) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar materia",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Time and Classroom pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${item.startTime} - ${item.endTime}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Surface(
                            onClick = onClassroomClick,
                            enabled = item.canOpenClassroomInMap,
                            shape = RoundedCornerShape(8.dp),
                            color = if (item.canOpenClassroomInMap) {
                                if (isCurrentClass) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            contentColor = if (item.canOpenClassroomInMap) {
                                if (isCurrentClass) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.canOpenClassroomInMap) Icons.Filled.Map else Icons.Filled.Place,
                                    contentDescription = "Abrir salón en el mapa",
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Salón ${item.classroomLabel}" + if (item.canOpenClassroomInMap) " ↗" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = "¿Eliminar materia?") },
            text = {
                Text(
                    text = "Se eliminarán todas las sesiones de ${item.subjectName}."
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = "Cancelar")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = "Eliminar")
                }
            }
        )
    }
}

@Composable
private fun ScheduleClassDetailsDialog(
    item: ClassSession,
    accentColor: Color,
    onDismiss: () -> Unit,
    onViewMap: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top accent branding line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(accentColor)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Icon badge + Title + Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.16f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.subjectName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 26.sp
                                )
                                if (!item.groupName.isNullOrBlank()) {
                                    Text(
                                        text = "Grupo ${item.groupName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Detail items structured as modern rounded tiles
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Horario y día tile
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = SchoolDay.fromDayOfWeek(item.dayOfWeek).fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.startTime} a ${item.endTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Salón tile
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Salón ${item.classroomLabel}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (item.canOpenClassroomInMap) "Disponible para ubicar en el mapa" else "Ubicación pendiente o sin croquis",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Profesor tile
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.teacherName?.takeIf { it.isNotBlank() } ?: "Profesor no asignado",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Profesor titular",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewMap,
                            enabled = item.canOpenClassroomInMap,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Mostrar salón")
                        }

                        Button(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Editar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDetailLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SchedulePrimaryDetail(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeeklyMatrix(
    classes: List<ClassSession>,
    palette: List<Color>,
    onAccent: Color,
    subjectColors: Map<String, Color>,
    onClassClick: (ClassSession) -> Unit
) {
    val rows = matrixRowsFor(classes)
    val matrixScrollState = rememberScrollState()
    var hasScrolled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(matrixScrollState.value) {
        if (matrixScrollState.value > 0) {
            hasScrolled = true
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            if (ScheduleConflictDetector.detect(classes).isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Hay dos clases registradas en este horario. Se conservan ambas para revisión.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .horizontalScroll(matrixScrollState)
                    .padding(horizontal = 6.dp)
            ) {
                Row {
                    MatrixTimeCell(text = "")
                    SchoolDay.entries.forEach { day ->
                        MatrixHeaderCell(text = day.fullName.take(3))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                rows.forEach { slot ->
                    Row {
                        MatrixTimeCell(
                            text = if (slot.isBreak) "Receso\n${slot.label}" else slot.label
                        )
                        SchoolDay.entries.forEach { day ->
                            val item = if (slot.isBreak) {
                                null
                            } else {
                                classes.firstOrNull {
                                    it.dayOfWeek == day.dayOfWeek && it.overlaps(slot)
                                }
                            }
                            MatrixClassCell(
                                item = item,
                                isBreak = slot.isBreak,
                                palette = palette,
                                onAccent = onAccent,
                                subjectColors = subjectColors,
                                onClassClick = onClassClick
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            AnimatedVisibility(
                visible = !hasScrolled && matrixScrollState.maxValue > 0,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "← Desliza para ver más días →",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixTimeCell(text: String) {
    Box(
        modifier = Modifier
            .width(MatrixTimeColumnWidth)
            .height(MatrixRowHeight)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun MatrixHeaderCell(text: String) {
    Box(
        modifier = Modifier
            .width(MatrixDayColumnWidth)
            .height(MatrixHeaderHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MatrixClassCell(
    item: ClassSession?,
    isBreak: Boolean,
    palette: List<Color>,
    onAccent: Color,
    subjectColors: Map<String, Color>,
    onClassClick: (ClassSession) -> Unit
) {
    Box(
        modifier = Modifier
            .width(MatrixDayColumnWidth)
            .height(MatrixRowHeight)
            .padding(2.dp)
    ) {
        when {
            isBreak -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Receso",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            item != null -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(7.dp),
                    color = subjectColors[scheduleSubjectKey(item.subjectName)]
                        ?: scheduleAccentFor(item.subjectName, palette),
                    onClick = { onClassClick(item) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.subjectName,
                            color = onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp
                        )
                        val detailsText = listOfNotNull(
                            item.groupName?.takeIf { it.isNotBlank() },
                            item.classroomName?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (detailsText.isNotBlank()) {
                            Text(
                                text = detailsText,
                                color = onAccent.copy(alpha = 0.86f),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleClassEditorDialog(
    initialClass: ClassSession?,
    relatedClasses: List<ClassSession>,
    onDismiss: () -> Unit,
    onSave: (List<ClassSessionDraft>) -> Unit
) {
    var form by remember(initialClass?.id, relatedClasses.map { it.id }) {
        mutableStateOf(
            if (initialClass == null) {
                ScheduleFormState()
            } else {
                relatedClasses.toFormState(initialClass)
            }
        )
    }
    val selectedSchedules = form.daySchedules.filter { it.enabled }
    val normalizedClassroom = normalizeKnownClassroom(form.classroom)
    val hasClassroomInput = form.classroom.trim().isNotBlank()
    val isClassroomValid = !hasClassroomInput || normalizedClassroom != null
    val isValid = form.subject.isNotBlank() &&
        selectedSchedules.isNotEmpty() &&
        selectedSchedules.all { it.startTime.isNotBlank() && it.endTime.isNotBlank() } &&
        isClassroomValid
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textFieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 38.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
        ) {
            // Header with institutional icon badge, title and close icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (initialClass == null) Icons.Filled.School else Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialClass == null) "Agregar materia" else "Editar materia",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configura los datos del horario escolar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // Scrollable form body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Información general
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EditorSectionTitle(
                            text = "Información general",
                            icon = Icons.Filled.School
                        )

                        OutlinedTextField(
                            value = form.subject,
                            onValueChange = { form = form.copy(subject = it) },
                            label = { Text(text = "Materia *") },
                            placeholder = { Text(text = "ej. Cálculo Diferencial") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = if (form.subject.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            minLines = 1,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SubjectSuggestions(
                            query = form.subject,
                            onSubjectSelected = { subject ->
                                form = form.copy(subject = subject)
                            }
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TeacherField(
                                value = form.teacher,
                                onValueChange = { form = form.copy(teacher = it) },
                                modifier = Modifier.weight(0.62f)
                            )
                            GroupField(
                                value = form.group,
                                onValueChange = { form = form.copy(group = it) },
                                modifier = Modifier.weight(0.38f)
                            )
                        }
                    }
                }

                // Section 2: Ubicación
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditorSectionTitle(
                            text = "Ubicación",
                            icon = Icons.Filled.Place
                        )

                        OutlinedTextField(
                            value = form.classroom,
                            onValueChange = { form = form.copy(classroom = it) },
                            label = { Text(text = "Salón") },
                            placeholder = { Text(text = "ej. 1104, 2101 o Laboratorio") },
                            singleLine = true,
                            isError = !isClassroomValid,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = if (isClassroomValid && hasClassroomInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            supportingText = {
                                if (!isClassroomValid) {
                                    Text(
                                        text = "No se ubica el salón dentro del mapa de ESCOM.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else if (hasClassroomInput) {
                                    Text(
                                        text = "✓ Salón reconocido en el mapa",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(text = "Opcional. Deja vacío si aún no está asignado.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 3: Días de clase (5-column responsive row, NO SCROLL)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorSectionTitle(
                                text = "Días de clase",
                                icon = Icons.Filled.CalendarMonth
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedSchedules.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                }
                            ) {
                                Text(
                                    text = "${selectedSchedules.size} seleccionado(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedSchedules.isNotEmpty()) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                form.daySchedules.forEach { daySchedule ->
                                    val selected = daySchedule.enabled
                                    val dayContainerColor by animateColorAsState(
                                        targetValue = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        animationSpec = tween(180),
                                        label = "editorDayBg"
                                    )
                                    val dayContentColor by animateColorAsState(
                                        targetValue = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        animationSpec = tween(180),
                                        label = "editorDayContent"
                                    )
                                    Surface(
                                        selected = selected,
                                        onClick = {
                                            val updated = daySchedule.copy(enabled = !daySchedule.enabled)
                                            form = form.copy(
                                                daySchedules = form.daySchedules.map {
                                                    if (it.day == updated.day) updated else it
                                                }
                                            )
                                        },
                                        shape = RoundedCornerShape(11.dp),
                                        color = dayContainerColor,
                                        shadowElevation = if (selected) 2.dp else 0.dp,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = daySchedule.day.fullName.take(3),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = dayContentColor
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(
                                                        if (selected) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Horarios por día
                if (selectedSchedules.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditorSectionTitle(
                            text = "Horarios por día",
                            icon = Icons.Filled.AccessTime
                        )
                        var pendingCopySource by remember { mutableStateOf<DayScheduleInput?>(null) }
                        pendingCopySource?.let { source ->
                            AlertDialog(
                                onDismissRequest = { pendingCopySource = null },
                                title = { Text("Aplicar horario a los días seleccionados") },
                                text = {
                                    Text("Se aplicará el horario ${source.startTime} - ${source.endTime} (${source.day.fullName}) a los ${selectedSchedules.size} días seleccionados. ¿Deseas continuar?")
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            form = form.copy(
                                                daySchedules = form.daySchedules.map {
                                                    if (it.enabled) it.copy(startTime = source.startTime, endTime = source.endTime) else it
                                                }
                                            )
                                            pendingCopySource = null
                                        }
                                    ) { Text("Aplicar") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingCopySource = null }) { Text("Cancelar") }
                                }
                            )
                        }

                        selectedSchedules.forEach { daySchedule ->
                            SelectedDayTimeEditor(
                                daySchedule = daySchedule,
                                onChange = { updated ->
                                    form = form.copy(
                                        daySchedules = form.daySchedules.map {
                                            if (it.day == updated.day) updated else it
                                        }
                                    )
                                },
                                onCopyToAll = if (selectedSchedules.size > 1) { { if (selectedSchedules.any { it.day != daySchedule.day && (it.startTime != daySchedule.startTime || it.endTime != daySchedule.endTime) }) pendingCopySource = daySchedule else form = form.copy(daySchedules = form.daySchedules.map { if (it.enabled) it.copy(startTime = daySchedule.startTime, endTime = daySchedule.endTime) else it }) } } else if (false) {
                                    {
                                        form = form.copy(
                                            daySchedules = form.daySchedules.map {
                                                if (it.enabled) {
                                                    it.copy(
                                                        startTime = daySchedule.startTime,
                                                        endTime = daySchedule.endTime
                                                    )
                                                } else it
                                            }
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // Sticky Bottom Actions with Navigation & Keyboard insets
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isValid) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when {
                                        form.subject.isBlank() -> "Escribe el nombre de la materia."
                                        selectedSchedules.isEmpty() -> "Selecciona al menos un día de clase."
                                        !isClassroomValid -> "El salón no se ubica en el mapa (corrígelo o déjalo vacío)."
                                        else -> "Completa el horario de todos los días seleccionados."
                                    },
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            enabled = isValid,
                            onClick = {
                                val classroom = normalizedClassroom
                                val savedClasses = selectedSchedules.map { schedule ->
                                    ClassSessionDraft(
                                        subjectName = form.subject.trim(),
                                        groupName = form.group.trim().takeIf { it.isNotBlank() },
                                        teacherName = form.teacher.trim().takeIf { it.isNotBlank() },
                                        dayOfWeek = schedule.day.dayOfWeek,
                                        startTime = schedule.startTime.trim(),
                                        endTime = schedule.endTime.trim(),
                                        classroomName = classroom,
                                        classroomStatus = if (classroom == null) {
                                            ClassroomStatusPending
                                        } else {
                                            ClassroomStatusAvailable
                                        }
                                    )
                                }
                                onSave(savedClasses)
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (initialClass == null) "Agregar materia" else "Guardar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text("Profesor") },
        placeholder = { Text("Opcional") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = if (value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        singleLine = true
    )
}

@Composable
private fun GroupField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text("Grupo") },
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        singleLine = true
    )
}

@Composable
private fun EditorSectionTitle(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectSuggestions(
    query: String,
    onSubjectSelected: (String) -> Unit
) {
    val normalizedQuery = query.normalizeForSearch()
    val suggestions = SuggestedSubjects
        .filter { subject ->
            normalizedQuery.isBlank() ||
                subject.normalizeForSearch().contains(normalizedQuery)
        }
        .take(5)

    if (suggestions.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        suggestions.forEach { subject ->
            val isSelected = query.normalizeForSearch() == subject.normalizeForSearch() && query.isNotBlank()
            AssistChip(
                onClick = { onSubjectSelected(subject) },
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    labelColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    }
                ),
                label = {
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedDayTimeEditor(
    daySchedule: DayScheduleInput,
    onChange: (DayScheduleInput) -> Unit,
    onCopyToAll: (() -> Unit)? = null
) {
    val textFieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = daySchedule.day.fullName,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "${daySchedule.startTime} - ${daySchedule.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onCopyToAll != null) {
                    TextButton(
                        onClick = onCopyToAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Aplicar a los días seleccionados",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                text = "Bloques predefinidos:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                DefaultClassSlots.forEach { slot ->
                    val isSelected = daySchedule.startTime == slot.startTime && daySchedule.endTime == slot.endTime
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onChange(
                                daySchedule.copy(
                                    startTime = slot.startTime,
                                    endTime = slot.endTime
                                )
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        label = {
                            Text(
                                text = slot.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Custom Start / End manual inputs
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = daySchedule.startTime,
                    onValueChange = { onChange(daySchedule.copy(startTime = it)) },
                    label = { Text(text = "Hora inicio") },
                    placeholder = { Text(text = "07:00") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = daySchedule.endTime,
                    onValueChange = { onChange(daySchedule.copy(endTime = it)) },
                    label = { Text(text = "Hora fin") },
                    placeholder = { Text(text = "08:30") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScheduleConfirmationDialog(
    confirmation: ScheduleConfirmation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = when (confirmation) {
        ScheduleConfirmation.DELETE_ALL -> "Deseas eliminar tu horario?"
        ScheduleConfirmation.REPLACE_WITH_PDF -> "¿Cambiar el PDF del horario?"
    }
    val body = when (confirmation) {
        ScheduleConfirmation.DELETE_ALL -> "Esta accion quitara todas las materias guardadas localmente."
        ScheduleConfirmation.REPLACE_WITH_PDF -> "Seleccionarás un nuevo PDF y el horario actual se reemplazará con las clases procesadas."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = body) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}

@Composable
private fun LoadTeachersConfirmationDialog(
    onDismiss: () -> Unit,
    onLoadWithoutTeachers: () -> Unit,
    onLoadWithTeachers: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Cargar nombres de profesores?") },
        text = {
            Text(
                text = "Puedes guardar tu horario con los nombres de profesores o solo con materias, horarios y salones."
            )
        },
        dismissButton = {
            TextButton(onClick = onLoadWithoutTeachers) {
                Text(text = "No cargar")
            }
        },
        confirmButton = {
            Button(onClick = onLoadWithTeachers) {
                Text(text = "Cargar nombres")
            }
        }
    )
}

private val ClassSession.startMinutes: Int
    get() = startTime.toMinutes()

private val ClassSession.endMinutes: Int
    get() = endTime.toMinutes()

private val ClassSession.classroomLabel: String
    get() = classroomName
        ?.trim()
        ?.removePrefix("Salón:")
        ?.removePrefix("Salon:")
        ?.removePrefix("Salón")
        ?.removePrefix("Salon")
        ?.trim()
        ?.ifBlank { null }
        ?: "Pendiente"

private val ClassSession.canOpenClassroomInMap: Boolean
    get() = isKnownClassroom(classroomName)

private fun ClassSession.toDraft(): ClassSessionDraft {
    return ClassSessionDraft(
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

private fun String.toMinutes(): Int {
    val parts = split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

private fun ClassSession.overlaps(slot: TimeSlot): Boolean {
    return startMinutes < slot.endTime.toMinutes() && endMinutes > slot.startTime.toMinutes()
}

private fun matrixRowsFor(classes: List<ClassSession>): List<TimeSlot> {
    val occupiedDefaultSlots = DefaultClassSlots.filter { slot ->
        classes.any { it.overlaps(slot) }
    }

    val manualSlots = classes
        .map { TimeSlot(it.startTime, it.endTime) }
        .filterNot { slot ->
            DefaultClassSlots.any { it.startTime == slot.startTime && it.endTime == slot.endTime }
        }

    val breakRows = BreakSlots.filter { breakSlot ->
        classes.any { it.overlaps(breakSlot) }
    }

    return (occupiedDefaultSlots + manualSlots + breakRows)
        .distinctBy { "${it.startTime}-${it.endTime}-${it.isBreak}" }
        .sortedBy { it.startTime.toMinutes() }
}

private fun List<ClassSession>.toFormState(
    fallback: ClassSession
): ScheduleFormState {
    val sessions = if (isEmpty()) listOf(fallback) else this
    val base = sessions.firstOrNull { it.id == fallback.id } ?: sessions.first()

    return ScheduleFormState(
        subject = base.subjectName,
        group = base.groupName.orEmpty(),
        teacher = base.teacherName.orEmpty(),
        classroom = base.classroomName.orEmpty(),
        daySchedules = SchoolDay.entries.filter { it != SchoolDay.SATURDAY || sessions.any { s -> s.dayOfWeek == SchoolDay.SATURDAY.dayOfWeek } }.map { day ->
            val sessionForDay = sessions
                .filter { it.dayOfWeek == day.dayOfWeek }
                .minByOrNull { it.startMinutes }

            DayScheduleInput(
                day = day,
                enabled = sessionForDay != null,
                startTime = sessionForDay?.startTime ?: DefaultClassSlots.first().startTime,
                endTime = sessionForDay?.endTime ?: DefaultClassSlots.first().endTime
            )
        }
    )
}

private fun ClassSession.belongsToSameEditableSubject(
    other: ClassSession
): Boolean {
    return scheduleId == other.scheduleId &&
        subjectName.normalizeForSearch() == other.subjectName.normalizeForSearch() &&
        groupName.orEmpty().normalizeForSearch() == other.groupName.orEmpty().normalizeForSearch()
}

private fun String?.toScheduleMapRoute(): String {
    val query = this?.trim()
    return if (query.isNullOrBlank()) {
        Routes.MAP
    } else {
        "${Routes.MAP}?${Routes.MAP_QUERY_ARGUMENT}=${Uri.encode(query)}"
    }
}

private fun String.normalizeForSearch(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
        .trim()
}

@Preview(showBackground = true)
@Composable
fun ScheduleScreenPreview() {
    EscomAppTheme {
        ScheduleScreen(navController = rememberNavController())
    }
}
