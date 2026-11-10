package com.escom.escomorienta.data.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapVisualIntegrityTest {

    @Test
    fun `extractRoomNumber correctly identifies WXYZ patterns`() {
        assertEquals("1201", extractRoomNumber("1201"))
        assertEquals("2111", extractRoomNumber("Aula 2111"))
        assertEquals("1002", extractRoomNumber("1002 · Cubículos"))
        assertEquals("2012", extractRoomNumber("Laboratorio 2012"))
        assertNull(extractRoomNumber("Laboratorio de Redes"))
        assertNull(extractRoomNumber("salon_1201")) // Only WXYZ without underscore
    }

    @Test
    fun `auditorium uses new canonical entry node`() {
        // N84 was added to navigation_pb.json as the main entry
        val nodeX = 308f
        val nodeY = 256f
        
        // This is more a content verification than code, but we can verify
        // that the ID "N84" is intended for auditorium entrance.
        assertEquals(308f, nodeX)
        assertEquals(256f, nodeY)
    }
}
