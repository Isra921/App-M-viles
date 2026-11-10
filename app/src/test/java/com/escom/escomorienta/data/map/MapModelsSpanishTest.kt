package com.escom.escomorienta.data.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapModelsSpanishTest {
    @Test
    fun parsesSpanishPlaceClassifications() {
        val classifications = mapOf(
            "AULA" to PlaceType.CLASSROOM,
            "LABORATORIO" to PlaceType.LABORATORY,
            "LABORATORIO_COMPUTO" to PlaceType.COMPUTER_LAB,
            "AREA_ADMINISTRATIVA" to PlaceType.ADMINISTRATIVE,
            "OFICINA" to PlaceType.OFFICE,
            "SERVICIO" to PlaceType.SERVICE,
            "BAÑO" to PlaceType.RESTROOM,
            "BIBLIOTECA" to PlaceType.LIBRARY,
            "AUDITORIO" to PlaceType.AUDITORIUM,
            "CLUB" to PlaceType.CLUB,
            "SALA_REUNIONES" to PlaceType.MEETING_ROOM,
            "MANTENIMIENTO" to PlaceType.MAINTENANCE,
            "AREA_INSTITUCIONAL" to PlaceType.INSTITUTIONAL_AREA,
            "AREA_DEPORTIVA" to PlaceType.SPORTS_AREA,
            "AREA_RECREATIVA" to PlaceType.RECREATION_AREA,
            "AREA_ESTUDIO" to PlaceType.STUDY_AREA,
            "ACCESIBILIDAD" to PlaceType.ACCESSIBILITY,
            "ESTACIONAMIENTO" to PlaceType.PARKING,
            "AREA_VERDE" to PlaceType.GREEN_AREA,
            "AREA_FUMADORES" to PlaceType.SMOKING_AREA,
            "ENTRADA" to PlaceType.ENTRANCE,
            "REFERENCIA" to PlaceType.REFERENCE,
            "GENERAL" to PlaceType.GENERAL
        )

        classifications.forEach { (jsonValue, expectedType) ->
            assertEquals(expectedType, PlaceType.fromJson(jsonValue))
        }
    }

    @Test
    fun parsesSpanishNavigationClassifications() {
        assertEquals(MapNavigationNodeType.PATH, MapNavigationNodeType.fromJson("CAMINO"))
        assertEquals(MapNavigationNodeType.STAIRS, MapNavigationNodeType.fromJson("ESCALERAS"))
    }
}
