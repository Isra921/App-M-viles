package com.escom.escomorienta.domain.navigation

import com.escom.escomorienta.data.map.MapPlace

/**
 * Represents an origin or destination endpoint for routing.
 */
sealed class RouteEndpoint {
    data class Place(val mapPlace: MapPlace) : RouteEndpoint()
    data class ManualPoint(val floorAsset: String, val point: NormalizedMapPoint) : RouteEndpoint()
    data class NamedPlaceId(val placeId: String) : RouteEndpoint()
    object None : RouteEndpoint()
}

/**
 * A continuous path segment on a specific floor.
 */
data class RouteSegment(
    val floorAsset: String,
    val floorId: String,
    val points: List<NormalizedMapPoint>,
    val cost: Float = 0f
)

/**
 * Vertical transition between two floors (via stairs or elevator).
 */
data class RouteTransition(
    val connectorId: String,
    val fromFloorAsset: String,
    val toFloorAsset: String,
    val stairNodeId: String,
    val point: NormalizedMapPoint
)

/**
 * Complete calculated route result.
 */
data class RouteResult(
    val success: Boolean,
    val origin: RouteEndpoint = RouteEndpoint.None,
    val destination: RouteEndpoint = RouteEndpoint.None,
    val segments: List<RouteSegment> = emptyList(),
    val transitions: List<RouteTransition> = emptyList(),
    val totalCost: Float = 0f,
    val pointsByFloor: Map<String, List<NormalizedMapPoint>> = emptyMap(),
    val isMultiFloor: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun failure(message: String): RouteResult = RouteResult(
            success = false,
            errorMessage = message
        )
    }
}
