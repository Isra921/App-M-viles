package com.escom.escomorienta.domain.navigation

import com.escom.escomorienta.data.map.MapFloorAccessData
import com.escom.escomorienta.data.map.MapNavigationGraph
import com.escom.escomorienta.data.map.MapPlace

/**
 * Domain service contract for calculating pedestrian multi-floor routes.
 * Fully decoupled from Android UI and Compose.
 */
interface RouteCalculationService {

    /**
     * Calculates a route between two endpoints using available repository data.
     */
    suspend fun calculateRoute(
        origin: RouteEndpoint,
        destination: RouteEndpoint
    ): RouteResult

    /**
     * Convenience method to calculate route using place IDs.
     */
    suspend fun calculateRouteByIds(
        originPlaceId: String,
        destinationPlaceId: String
    ): RouteResult

    /**
     * Calculates a multi-floor route with preloaded graphs and access data.
     */
    fun calculateRouteWithGraphs(
        graphsByFloor: Map<String, MapNavigationGraph>,
        originFloorAsset: String?,
        originPoint: NormalizedMapPoint?,
        originPlace: MapPlace?,
        destinationFloorAsset: String?,
        destinationPlace: MapPlace?,
        accessDataByFloor: Map<String, MapFloorAccessData>
    ): RouteResult
}
