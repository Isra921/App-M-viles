package com.escom.escomorienta.data.schedule

import java.time.LocalTime

data class ScheduleConflict(val first: ClassSession, val second: ClassSession)

object ScheduleConflictDetector {
    fun conflictsWithExisting(existing: List<ClassSession>, drafts: List<ClassSessionDraft>): Boolean =
        drafts.any { draft -> existing.any { saved ->
            saved.dayOfWeek == draft.dayOfWeek && overlapTimes(
                saved.startTime, saved.endTime, draft.startTime, draft.endTime
            )
        } }
    fun detect(classes: List<ClassSession>): List<ScheduleConflict> = classes
        .groupBy { it.dayOfWeek }
        .values
        .flatMap { sameDay ->
            sameDay.flatMapIndexed { index, first ->
                sameDay.drop(index + 1).mapNotNull { second ->
                    if (overlap(first, second)) ScheduleConflict(first, second) else null
                }
            }
        }

    internal fun overlap(first: ClassSession, second: ClassSession): Boolean {
        return overlapTimes(first.startTime, first.endTime, second.startTime, second.endTime)
    }

    private fun overlapTimes(startAText: String, endAText: String, startBText: String, endBText: String): Boolean {
        val startA = startAText.asTime() ?: return false
        val endA = endAText.asTime() ?: return false
        val startB = startBText.asTime() ?: return false
        val endB = endBText.asTime() ?: return false
        return startA < endB && startB < endA
    }

    private fun String.asTime() = runCatching { LocalTime.parse(this) }.getOrNull()
}
