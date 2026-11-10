package com.escom.escomorienta.ui.schedule

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.escom.escomorienta.data.schedule.ClassSessionDraft
import com.escom.escomorienta.data.schedule.ClassSession
import com.escom.escomorienta.data.schedule.ScheduleStatusComplete
import com.escom.escomorienta.data.schedule.ScheduleStatusNoRoomsPublished
import com.escom.escomorienta.data.schedule.ScheduleStatusPartialRooms
import com.escom.escomorienta.data.schedule.ScheduleRepository
import com.escom.escomorienta.data.schedule.pdf.PdfScheduleProcessor
import com.escom.escomorienta.data.schedule.pdf.ScheduleProcessingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScheduleUiState(
    val isProcessingPdf: Boolean = false,
    val processingFileName: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val pendingPdfImport: PendingPdfImport? = null
)

data class PendingPdfImport(
    val sourceFileName: String?,
    val status: String,
    val classes: List<ClassSessionDraft>
)

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val processor: PdfScheduleProcessor
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()
    val classes: StateFlow<List<ClassSession>?> =
        repository.observeAllClasses()
            .map { loadedClasses -> loadedClasses as List<ClassSession>? }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    fun loadSchedule() {
        clearMessages()
    }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingPdf = true,
                    processingFileName = null,
                    successMessage = null,
                    errorMessage = null,
                    pendingPdfImport = null
                )
            }

            val result = withContext(Dispatchers.IO) {
                processor.process(uri)
            }

            when (result) {
                is ScheduleProcessingResult.Success -> {
                    val pendingImport = PendingPdfImport(
                        sourceFileName = result.sourceFileName,
                        status = result.status,
                        classes = result.classes
                    )

                    if (pendingImport.hasTeacherNames()) {
                        _uiState.update {
                            it.copy(
                                isProcessingPdf = false,
                                processingFileName = result.sourceFileName,
                                pendingPdfImport = pendingImport,
                                successMessage = null,
                                errorMessage = null
                            )
                        }
                    } else {
                        saveProcessedSchedule(
                            pendingImport = pendingImport,
                            includeTeachers = false,
                            extraMessage = "No se detectaron nombres de profesores en el PDF."
                        )
                    }
                }

                is ScheduleProcessingResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            processingFileName = null,
                            successMessage = null,
                            errorMessage = result.message,
                            pendingPdfImport = null
                        )
                    }
                }
            }
        }
    }

    fun confirmPendingPdfImport(includeTeachers: Boolean) {
        val pendingImport = _uiState.value.pendingPdfImport ?: return
        viewModelScope.launch {
            saveProcessedSchedule(
                pendingImport = pendingImport,
                includeTeachers = includeTeachers
            )
        }
    }

    fun cancelPendingPdfImport() {
        _uiState.update { it.copy(pendingPdfImport = null) }
    }

    fun deleteSchedule() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteCurrentSchedule()
            }
            _uiState.update {
                it.copy(
                    successMessage = "Horario eliminado.",
                    errorMessage = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                successMessage = null,
                errorMessage = null
            )
        }
    }

    private fun ScheduleProcessingResult.Success.toSuccessMessage(): String {
        return PendingPdfImport(
            sourceFileName = sourceFileName,
            status = status,
            classes = classes
        ).toSuccessMessage()
    }

    private suspend fun saveProcessedSchedule(
        pendingImport: PendingPdfImport,
        includeTeachers: Boolean,
        extraMessage: String? = null
    ) {
        val classesToSave = if (includeTeachers) {
            pendingImport.classes
        } else {
            pendingImport.classes.map { it.copy(teacherName = null) }
        }

        withContext(Dispatchers.IO) {
            repository.insertCompleteScheduleWithClasses(
                publicationStatus = pendingImport.status,
                sourceFileName = pendingImport.sourceFileName,
                classes = classesToSave
            )
        }

        _uiState.update {
            it.copy(
                isProcessingPdf = false,
                processingFileName = pendingImport.sourceFileName,
                pendingPdfImport = null,
                successMessage = listOfNotNull(
                    pendingImport.toSuccessMessage(),
                    extraMessage
                ).joinToString(separator = " "),
                errorMessage = null
            )
        }
    }

    private fun PendingPdfImport.toSuccessMessage(): String {
        return when (status) {
            ScheduleStatusNoRoomsPublished -> {
                "Horario cargado sin salones. Actualiza tu PDF o espera un par de dias a que SAES publique los salones."
            }
            ScheduleStatusPartialRooms -> {
                val pending = classes.countPendingClassrooms()
                "Horario cargado con $pending clase(s) sin salon. Puedes actualizar el PDF despues o agregarlas manualmente."
            }
            ScheduleStatusComplete -> {
                "Horario PDF cargado correctamente."
            }
            else -> {
                "Horario PDF cargado correctamente."
            }
        }
    }

    private fun PendingPdfImport.hasTeacherNames(): Boolean {
        return classes.any { !it.teacherName.isNullOrBlank() }
    }

    private fun List<ClassSessionDraft>.countPendingClassrooms(): Int {
        return count { it.classroomName.isNullOrBlank() }
    }
}

class ScheduleViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            val appContext = context.applicationContext
            return ScheduleViewModel(
                repository = ScheduleRepository.getInstance(appContext),
                processor = PdfScheduleProcessor(appContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
