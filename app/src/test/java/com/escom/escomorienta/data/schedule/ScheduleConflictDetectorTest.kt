package com.escom.escomorienta.data.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleConflictDetectorTest {
    @Test fun `keeps and reports overlapping classes`() {
        val first = session("A", "10:00", "11:30")
        val second = session("B", "11:00", "12:00")
        val classes = listOf(first, second)
        assertEquals(listOf(ScheduleConflict(first, second)), ScheduleConflictDetector.detect(classes))
        assertEquals(2, classes.size)
    }

    @Test fun `touching boundaries are not a conflict`() {
        assertTrue(ScheduleConflictDetector.detect(listOf(session("A", "10:00", "11:00"), session("B", "11:00", "12:00"))).isEmpty())
    }

    @Test fun `draft conflict is detected before persistence`() {
        val existing = listOf(session("Primera", "10:00", "11:30"))
        val overlap = ClassSessionDraft("Nueva", null, null, 1, "11:00", "12:00", null, ClassroomStatusPending)
        val boundary = overlap.copy(startTime = "11:30")

        assertTrue(ScheduleConflictDetector.conflictsWithExisting(existing, listOf(overlap)))
        assertTrue(!ScheduleConflictDetector.conflictsWithExisting(existing, listOf(boundary)))
    }

    private fun session(name: String, start: String, end: String) = ClassSession(
        id = name.hashCode().toLong(), scheduleId = 1, subjectName = name, groupName = null,
        teacherName = null, dayOfWeek = 1, startTime = start, endTime = end,
        classroomName = null, classroomStatus = ClassroomStatusPending
    )
}
