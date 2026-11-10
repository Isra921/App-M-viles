package com.escom.escomorienta.data.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapPlaceIndexTest {
    private val ground = place("gestion_escolar", "Gestión Escolar", "PB")
    private val room1201 = place("salon_1201", "1201", "2", listOf("salón 1201", "aula 1201"))
    private val room1203 = place("salon_1203", "1203", "2", listOf("salón 1203"))
    private val index = MapPlaceIndex(listOf(ground, room1201, room1203))

    @Test fun `global search finds 1201 regardless of visible ground floor`() {
        assertEquals("salon_1201", index.search("1201").single().id)
        assertEquals("2", index.search("1201").single().floorId)
        assertEquals("places_p2.json", MapAssetCatalog.assetForFloor("2"))
    }

    @Test fun `search with typo 12p9 resolves room 1209`() {
        val room1209 = place("salon_1209", "1209", "2")
        val idx = MapPlaceIndex(listOf(room1209))
        assertEquals("salon_1209", idx.search("12p9").single().id)
        assertEquals("salon_1209", idx.search("12o9").single().id)
    }

    @Test fun `extracts one destination or ordered origin and destination`() {
        assertEquals(listOf("salon_1203"), index.mentioned("¿Dónde está el 1203?").map { it.id })
        assertEquals(listOf("salon_1203", "gestion_escolar"),
            index.mentioned("¿Cómo voy del 1203 a Gestión Escolar?").map { it.id })
    }

    @Test fun `conservative classroom typo resolves only one canonical room`() {
        val room1209 = place("salon_1209", "1209", "2")
        val result = MapPlaceIndex(listOf(room1209)).mentionDetails("¿Dónde está el 12p9?")

        assertEquals("salon_1209", result.single().place.id)
        assertEquals("12p9", result.single().matchedText)
        assertEquals("1209", result.single().interpretedAs)
    }

    @Test fun `typo correction rejects unknown and ambiguous candidates`() {
        assertEquals(emptyList<MapPlaceIndex.Mention>(), index.mentionDetails("busca el 12p9"))
        val duplicate = listOf(
            place("salon_1209_a", "1209", "2"),
            place("salon_1209_b", "1209", "3")
        )
        assertEquals(emptyList<MapPlaceIndex.Mention>(), MapPlaceIndex(duplicate).mentionDetails("12p9"))
    }

    @Test fun `correction does not alter arbitrary words or identifiers`() {
        val with1209 = MapPlaceIndex(listOf(place("salon_1209", "1209", "2")))
        assertEquals(emptyList<MapPlaceIndex.Mention>(), with1209.mentionDetails("apoyo"))
        assertEquals(emptyList<MapPlaceIndex.Mention>(), with1209.mentionDetails("ABC-12P9-X"))
    }

    @Test fun `route keeps ordered exact and corrected mentions`() {
        val room1209 = place("salon_1209", "1209", "2")
        val mixed = MapPlaceIndex(listOf(room1209, ground)).mentionDetails("del 12p9 a Gestión Escolar")
        assertEquals(listOf("salon_1209", "gestion_escolar"), mixed.map { it.place.id })
    }

    @Test fun `canonical laboratory alias resolves descriptive room`() {
        val lab = place("salon_1104", "1104 · Laboratorio de Redes", "1", listOf("1104", "laboratorio de redes"))
        assertEquals("salon_1104", MapPlaceIndex(listOf(lab)).mentioned("Laboratorio de redes").single().id)
    }

    private fun place(id: String, name: String, floor: String, aliases: List<String> = listOf(name)) = MapPlace(
        id, name, aliases, PlaceType.CLASSROOM, "Edificio 1", floor,
        if (floor == "PB") "Planta baja" else "Segundo piso", "", emptyList()
    )
}
