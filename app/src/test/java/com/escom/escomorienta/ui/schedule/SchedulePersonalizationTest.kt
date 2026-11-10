package com.escom.escomorienta.ui.schedule

import com.escom.escomorienta.data.settings.decodeSubjectSelection
import com.escom.escomorienta.data.settings.encodeSubjectSelection
import com.escom.escomorienta.data.settings.subjectSelectionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SchedulePersonalizationTest {
    @Test
    fun automaticAssignmentUsesDifferentColorsForDifferentSubjects() {
        val subjects = listOf("Cálculo", "Bases de datos", "Redes", "Compiladores", "Álgebra")
        val assignments = subjectAccentColors(subjects, CustomScheduleColors)

        assertEquals(5, assignments.size)
        assertEquals(5, assignments.values.toSet().size)
    }

    @Test
    fun customColorsRoundTripAndOverrideAutomaticAssignment() {
        val encoded = encodeCustomScheduleColors(mapOf("calculo" to 7, "redes" to 2))
        val decoded = decodeCustomScheduleColors(encoded)
        val assignments = subjectAccentColors(
            listOf("Cálculo", "Redes"),
            CustomScheduleColors,
            decoded
        )

        assertEquals(CustomScheduleColors[7], assignments[subjectSelectionKey("Cálculo")])
        assertEquals(CustomScheduleColors[2], assignments[subjectSelectionKey("Redes")])
        assertNotEquals(assignments["calculo"], assignments["redes"])
    }

    @Test
    fun notificationSubjectSelectionRoundTripsNormalizedKeys() {
        val selected = setOf(subjectSelectionKey("Cálculo"), subjectSelectionKey("Bases de Datos"))
        assertEquals(selected, decodeSubjectSelection(encodeSubjectSelection(selected)))
    }
}
