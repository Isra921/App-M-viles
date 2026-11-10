package com.escom.escomorienta.data.map

data class MapFloor(
    val id: String,
    val name: String,
    val assetFileName: String,
    val imageResName: String
)

data class MapPlace(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val type: PlaceType,
    val building: String,
    val floorId: String,
    val floorName: String,
    val description: String,
    val bounds: List<MapBounds>,
    val navigationNodeId: String? = null,
    /** Draws one selection marker at the center of each disconnected mapped area. */
    val markerPerBound: Boolean = false,
    val code: String? = null
)

data class MapBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }
}

data class MapNavigationGraph(
    val floorId: String,
    val coordinateWidth: Float,
    val coordinateHeight: Float,
    val mapWidth: Float,
    val mapHeight: Float,
    val verticalOffset: Float,
    val nodes: List<MapNavigationNode>,
    val edges: List<MapNavigationEdge>
)

data class MapNavigationNode(
    val id: String,
    val x: Float,
    val y: Float,
    val type: MapNavigationNodeType
)

enum class MapNavigationNodeType {
    PATH,
    STAIRS;

    companion object {
        fun fromJson(value: String): MapNavigationNodeType = when (value.trim().uppercase()) {
            "STAIRS", "ESCALERAS" -> STAIRS
            else -> PATH
        }
    }
}

data class MapNavigationEdge(
    val from: String,
    val to: String,
    val weight: Float
)

data class MapAccessPoint(
    val id: String,
    val x: Float,
    val y: Float,
    val type: String,
    val graphNodeIds: List<String>
)

data class MapFloorAccessData(
    val floorId: String,
    val coordinateWidth: Float,
    val mapHeight: Float,
    val verticalOffset: Float,
    val accessPointsByPlaceId: Map<String, List<MapAccessPoint>>
)

enum class PlaceType(
    val displayName: String
) {
    CLASSROOM("Aula"),
    LABORATORY("Laboratorio"),
    COMPUTER_LAB("Aula de cómputo"),
    ADMINISTRATIVE("Área administrativa"),
    OFFICE("Oficina"),
    SERVICE("Servicio"),
    RESTROOM("Baño"),
    LIBRARY("Biblioteca"),
    AUDITORIUM("Auditorio"),
    CLUB("Club"),
    MEETING_ROOM("Sala de reuniones"),
    MAINTENANCE("Mantenimiento"),
    INSTITUTIONAL_AREA("Área institucional"),
    SPORTS_AREA("Área deportiva"),
    RECREATION_AREA("Área recreativa"),
    STUDY_AREA("Área de estudio"),
    ACCESSIBILITY("Accesibilidad"),
    PARKING("Estacionamiento"),
    GREEN_AREA("Área verde"),
    SMOKING_AREA("Área para fumar"),
    ENTRANCE("Entrada"),
    REFERENCE("Referencia"),
    GENERAL("Área general");

    companion object {
        fun fromJson(value: String): PlaceType {
            return when (value.trim().uppercase()) {
                "CLASSROOM", "AULA" -> CLASSROOM
                "LABORATORY", "LABORATORIO" -> LABORATORY
                "COMPUTER_LAB", "LABORATORIO_COMPUTO", "AULA_COMPUTO" -> COMPUTER_LAB
                "ADMINISTRATIVE", "ADMINISTRATIVO", "AREA_ADMINISTRATIVA" -> ADMINISTRATIVE
                "OFFICE", "OFICINA" -> OFFICE
                "SERVICE", "SERVICIO" -> SERVICE
                "RESTROOM", "BANO", "BAÑO", "SANITARIO" -> RESTROOM
                "LIBRARY", "BIBLIOTECA" -> LIBRARY
                "AUDITORIUM", "AUDITORIO" -> AUDITORIUM
                "CLUB" -> CLUB
                "MEETING_ROOM", "SALA_REUNIONES" -> MEETING_ROOM
                "MAINTENANCE", "MANTENIMIENTO" -> MAINTENANCE
                "INSTITUTIONAL_AREA", "AREA_INSTITUCIONAL" -> INSTITUTIONAL_AREA
                "SPORTS_AREA", "AREA_DEPORTIVA" -> SPORTS_AREA
                "RECREATION_AREA", "AREA_RECREATIVA" -> RECREATION_AREA
                "STUDY_AREA", "AREA_ESTUDIO" -> STUDY_AREA
                "ACCESSIBILITY", "ACCESIBILIDAD" -> ACCESSIBILITY
                "PARKING", "ESTACIONAMIENTO" -> PARKING
                "GREEN_AREA", "AREA_VERDE" -> GREEN_AREA
                "SMOKING_AREA", "AREA_FUMADORES" -> SMOKING_AREA
                "ENTRANCE", "ENTRADA" -> ENTRANCE
                "REFERENCE", "REFERENCIA" -> REFERENCE
                else -> GENERAL
            }
        }
    }
}

object MapAssetCatalog {
    val floorAssets = listOf("places_pb.json", "places_p1.json", "places_p2.json")
    fun assetForFloor(floorId: String) = floorAssets.firstOrNull {
        when (floorId) { "PB" -> it.contains("pb"); "1" -> it.contains("p1"); "2" -> it.contains("p2"); else -> false }
    }
}

internal fun extractRoomNumber(name: String): String? {
    val regex = Regex("\\b(\\d{4})\\b")
    return regex.find(name)?.groupValues?.get(1)
}

internal fun extractRoomNumber(place: MapPlace): String? {
    place.code?.takeIf { it.isNotBlank() }?.let { return it }
    place.aliases.firstOrNull { it.matches(Regex("^\\d{4}$")) }?.let { return it }
    Regex("(\\d{4})").find(place.id)?.groupValues?.get(1)?.let { return it }
    return extractRoomNumber(place.name)
}
