package com.escom.escomorienta.data.schedule.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleParserTeacherTest {
    @Test
    fun preservesSourceOrderForAmbiguousTeacherNames() {
        assertEquals("Marco Antonio", teacherNameFromSource("MARCO ANTONIO")?.displayText)
        assertEquals("Ana Maria Lopez", teacherNameFromSource("ANA MARIA LOPEZ")?.displayText)
        assertEquals("Ortiz Ramirez", teacherNameFromSource("ORTIZ RAMIREZ")?.displayText)
        assertEquals("de la Cruz Perez Juan Carlos", teacherNameFromSource("DE LA CRUZ PEREZ JUAN CARLOS")?.displayText)
    }

    @Test
    fun cleansContaminationAfterTeacherCell() {
        assertEquals(
            "ORTIZ RAMIREZ JOSE ANTONIO",
            cleanTeacherCell("ORTIZ RAMIREZ JOSE ANTONIO EDIFICIO: 4 SALÓN: 109")
        )
        assertEquals(
            "MORALES ANA",
            cleanTeacherCell("MORALES ANA 18:30 - 20:00 EDIFICIO: 4")
        )
    }

    @Test
    fun unassignedTeacherKeepsSourceAndHasNoDisplayName() {
        val teacher = teacherNameFromSource("SIN ASIGNAR")
        assertEquals("SIN ASIGNAR", teacher?.sourceText)
        assertNull(teacher?.displayText)
    }

    @Test
    fun rejectsImplausibleFallbackValues() {
        assertNull(teacherNameFromSource("7CV3 EDIFICIO 4"))
        assertNull(teacherNameFromSource("2021630074 JOSE"))
        assertNull(teacherNameFromSource("JOSE ANTONIO: 4"))
    }

    @Test
    fun geometricExtractionIncludesOnlyProfessorColumn() {
        val result = parser.extractTeacher(
            rowTokens = listOf(
                token("ORTIZ", x = 302f, endX = 326f, y = 100f),
                token("RAMIREZ", x = 329f, endX = 366f, y = 100f),
                token("JOSE", x = 302f, endX = 322f, y = 106f),
                token("ANTONIO", x = 325f, endX = 360f, y = 106f),
                token("18:30", x = 402f, endX = 426f, y = 100f),
                token("Edificio:", x = 430f, endX = 470f, y = 100f),
                token("Nota:", x = 40f, endX = 64f, y = 400f)
            ),
            columns = columns
        )

        assertEquals("ORTIZ RAMIREZ JOSE ANTONIO", result?.sourceText)
        assertEquals("Ortiz Ramirez Jose Antonio", result?.displayText)
    }

    @Test
    fun geometricExtractionWorksWhenMondayIsEmptyAndForUnassignedTeacher() {
        val result = parser.extractTeacher(
            rowTokens = listOf(
                token("SIN", x = 302f, endX = 318f, y = 120f),
                token("ASIGNAR", x = 321f, endX = 355f, y = 120f),
                token("10:30", x = 490f, endX = 514f, y = 120f)
            ),
            columns = columns
        )

        assertEquals("SIN ASIGNAR", result?.sourceText)
        assertNull(result?.displayText)
    }

    @Test
    fun keepsLongFinalNameTokenWhenItsCenterIsInsideTeacherColumn() {
        val result = parser.extractTeacher(
            rowTokens = listOf(
                token("MARCO", x = 302f, endX = 330f, y = 100f),
                token("ANTONIO", x = 333f, endX = 368f, y = 100f),
                token("HERNANDEZ", x = 365f, endX = 407f, y = 100f)
            ),
            columns = columns
        )
        assertEquals("MARCO ANTONIO HERNANDEZ", result?.sourceText)
        assertEquals("Marco Antonio Hernandez", result?.displayText)
    }

    @Test
    fun matchingIgnoresAccentsCaseAndPunctuation() {
        assertEquals("CREDITOS", normalizeForMatching("Créditos:"))
        assertEquals("UNIDAD DE APRENDIZAJE", normalizeForMatching("Unidad  de aprendizaje"))
        assertTrue(isPlausibleTeacherName("ORTIZ RAMIREZ JOSE ANTONIO"))
    }

    private fun token(text: String, x: Float, endX: Float, y: Float) =
        PdfTextToken(text, pageIndex = 0, x = x, y = y, endX = endX, height = 8f)

    private companion object {
        val parser = ScheduleParser()
        val columns = TableColumns(
            groupStartX = 20f,
            subjectStartX = 60f,
            creditsStartX = 230f,
            teacherStartX = 300f,
            mondayStartX = 400f,
            tuesdayStartX = 480f,
            wednesdayStartX = 560f,
            thursdayStartX = 640f,
            fridayStartX = 720f
        )
    }
}
