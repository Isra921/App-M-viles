package com.escom.escomorienta.data.schedule.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.escom.escomorienta.data.schedule.ClassSessionDraft
import com.escom.escomorienta.data.schedule.ScheduleStatusComplete
import com.escom.escomorienta.data.schedule.ScheduleStatusNoRoomsPublished
import com.escom.escomorienta.data.schedule.ScheduleStatusPartialRooms

class PdfScheduleProcessor(
    private val context: Context,
    private val extractor: PdfTextExtractor = PdfTextExtractor(context),
    private val parser: ScheduleParser = ScheduleParser(),
    private val normalizer: ScheduleNormalizer = ScheduleNormalizer()
) {
    fun process(uri: Uri): ScheduleProcessingResult {
        return try {
            if (!isPdf(uri)) {
                return ScheduleProcessingResult.Error("El archivo seleccionado no es PDF.")
            }

            val document = extractor.extract(uri)
            if (document.plainText.isBlank()) {
                return ScheduleProcessingResult.Error(
                    "El PDF no contiene texto extraíble. Sube el horario original descargado del SAES."
                )
            }

            val rawSessions = parser.parse(document)
            val classes = preferSessionsWithClassroom(
                normalizer.normalize(rawSessions)
            )
            if (classes.isEmpty()) {
                return ScheduleProcessingResult.Error(
                    "No se pudo identificar el horario. Verifica que el archivo corresponda al horario PDF del SAES."
                )
            }

            ScheduleProcessingResult.Success(
                sourceFileName = displayName(uri),
                status = classifyStatus(classes),
                classes = classes
            )
        } catch (exception: Exception) {
            ScheduleProcessingResult.Error(
                message = "No se pudo procesar el PDF. Verifica que el archivo corresponda al horario PDF del SAES.",
                cause = exception
            )
        }
    }

    private fun isPdf(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == "application/pdf") return true
        return displayName(uri)?.endsWith(".pdf", ignoreCase = true) == true
    }

    private fun displayName(uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }

    private fun classifyStatus(classes: List<ClassSessionDraft>): String {
        val withRoom = classes.count { !it.classroomName.isNullOrBlank() }
        return when (withRoom) {
            0 -> ScheduleStatusNoRoomsPublished
            classes.size -> ScheduleStatusComplete
            else -> ScheduleStatusPartialRooms
        }
    }

    private fun preferSessionsWithClassroom(
        classes: List<ClassSessionDraft>
    ): List<ClassSessionDraft> {
        return classes
            .groupBy { session ->
                listOf(
                    session.subjectName.normalizeKey(),
                    session.groupName.orEmpty().normalizeKey(),
                    session.dayOfWeek.toString(),
                    session.startTime,
                    session.endTime
                ).joinToString("|")
            }
            .values
            .map { sessions ->
                sessions.firstOrNull { !it.classroomName.isNullOrBlank() } ?: sessions.first()
            }
            .sortedWith(
                compareBy<ClassSessionDraft> { it.dayOfWeek }
                    .thenBy { it.startTime }
                    .thenBy { it.subjectName }
            )
    }

    private fun String.normalizeKey(): String {
        return lowercase().replace("\\s+".toRegex(), " ").trim()
    }
}

sealed class ScheduleProcessingResult {
    data class Success(
        val sourceFileName: String?,
        val status: String,
        val classes: List<ClassSessionDraft>
    ) : ScheduleProcessingResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ScheduleProcessingResult()
}
