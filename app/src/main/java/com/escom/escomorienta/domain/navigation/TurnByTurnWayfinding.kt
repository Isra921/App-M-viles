package com.escom.escomorienta.domain.navigation

import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Tipo de acción de wayfinding para la navegación peatonal dentro de ESCOM.
 */
enum class NavigationStepType {
    START,
    WALK_STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    STAIRS_UP,
    STAIRS_DOWN,
    DESTINATION
}

/**
 * Modelo de una instrucción individual de navegación paso a paso.
 */
data class NavigationInstruction(
    val stepIndex: Int,
    val type: NavigationStepType,
    val title: String,
    val detail: String,
    val floorName: String,
    val distanceMeters: Int,
    val estimatedSeconds: Int
)

/**
 * Resumen global del wayfinding para la ruta calculada.
 */
data class WayfindingSummary(
    val instructions: List<NavigationInstruction>,
    val totalDistanceMeters: Int,
    val totalTimeMinutes: Int,
    val totalSteps: Int,
    val isAccessible: Boolean = true,
    val horizontalDistanceMeters: Float = 0f,
    val verticalDistanceMeters: Float = 0f,
    val estimatedSeconds: Int = 0,
    val isMetricValid: Boolean = totalDistanceMeters > 0 && totalTimeMinutes > 0
) {
    val hasValidMetrics: Boolean
        get() = isMetricValid &&
            totalDistanceMeters > 0 &&
            totalTimeMinutes > 0 &&
            horizontalDistanceMeters.isFinite() &&
            horizontalDistanceMeters >= 0f &&
            verticalDistanceMeters.isFinite() &&
            verticalDistanceMeters >= 0f
}

/**
 * Motor de Wayfinding peatonal para ESCOM_Orienta.
 * Traduce secuencias de coordenadas normalizadas y transiciones de nivel en instrucciones comprensibles
 * utilizando el sistema físico calibrado ([MapScaleCalibration]).
 */
object TurnByTurnWayfinding {

    fun generateInstructions(
        route: RouteResult,
        originName: String = "Punto de partida",
        destinationName: String = "Destino",
        floorNameLookup: (String) -> String = ::defaultFloorNameLookup
    ): WayfindingSummary {
        if (!route.success || route.segments.isEmpty()) {
            return WayfindingSummary(
                instructions = emptyList(),
                totalDistanceMeters = 0,
                totalTimeMinutes = 0,
                totalSteps = 0,
                isAccessible = true,
                horizontalDistanceMeters = 0f,
                verticalDistanceMeters = 0f,
                estimatedSeconds = 0,
                isMetricValid = false
            )
        }

        val metricEstimate = MapScaleCalibration.estimateRouteMetrics(route)
        val instructions = mutableListOf<NavigationInstruction>()
        var stepCounter = 1

        // Paso inicial
        val firstSegment = route.segments.first()
        val firstFloorKey = firstSegment.floorAsset.ifBlank { firstSegment.floorId }
        instructions.add(
            NavigationInstruction(
                stepIndex = stepCounter++,
                type = NavigationStepType.START,
                title = "Inicio de ruta",
                detail = "Comienza tu recorrido desde $originName",
                floorName = floorNameLookup(firstFloorKey),
                distanceMeters = 0,
                estimatedSeconds = 0
            )
        )

        route.segments.forEachIndexed { segmentIndex, segment ->
            val points = segment.points
            val floorKey = segment.floorAsset.ifBlank { segment.floorId }
            val currentFloorName = floorNameLookup(floorKey)
            val floorScale = MapScaleCalibration.resolveFloorScale(floorKey)

            if (points.size >= 2 && floorScale != null) {
                var currentAccumDist = 0f

                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val segmentDist = MapScaleCalibration.normalizedDistanceToMeters(
                        p1 = p1,
                        p2 = p2,
                        floorIdentifier = floorKey
                    ) ?: 0f
                    currentAccumDist += segmentDist

                    // Si hay un giro o cambio de dirección en el siguiente punto
                    if (i < points.size - 2) {
                        val p3 = points[i + 2]
                        val turn = detectTurn(p1, p2, p3, floorScale)
                        if (turn != NavigationStepType.WALK_STRAIGHT && currentAccumDist >= 4f) {
                            val distInt = currentAccumDist.roundToInt().coerceAtLeast(1)
                            val seconds = (currentAccumDist / MapScaleCalibration.WALKING_SPEED_METERS_PER_SECOND)
                                .roundToInt()
                                .coerceAtLeast(1)
                            instructions.add(
                                NavigationInstruction(
                                    stepIndex = stepCounter++,
                                    type = NavigationStepType.WALK_STRAIGHT,
                                    title = "Avanza por el pasillo",
                                    detail = "Camina aproximadamente $distInt m por el pasillo",
                                    floorName = currentFloorName,
                                    distanceMeters = distInt,
                                    estimatedSeconds = seconds
                                )
                            )

                            val turnDetail = if (turn == NavigationStepType.TURN_LEFT) {
                                "Gira a la izquierda"
                            } else {
                                "Gira a la derecha"
                            }
                            instructions.add(
                                NavigationInstruction(
                                    stepIndex = stepCounter++,
                                    type = turn,
                                    title = turnDetail,
                                    detail = "$turnDetail y continúa por el pasillo",
                                    floorName = currentFloorName,
                                    distanceMeters = 0,
                                    estimatedSeconds = 0
                                )
                            )
                            currentAccumDist = 0f
                        }
                    }
                }

                // Tramo final del segmento de piso
                if (currentAccumDist >= 1f) {
                    val distInt = currentAccumDist.roundToInt().coerceAtLeast(1)
                    val seconds = (currentAccumDist / MapScaleCalibration.WALKING_SPEED_METERS_PER_SECOND)
                        .roundToInt()
                        .coerceAtLeast(1)
                    instructions.add(
                        NavigationInstruction(
                            stepIndex = stepCounter++,
                            type = NavigationStepType.WALK_STRAIGHT,
                            title = "Sigue recto",
                            detail = "Avanza aproximadamente $distInt m hacia el punto de conexión o destino",
                            floorName = currentFloorName,
                            distanceMeters = distInt,
                            estimatedSeconds = seconds
                        )
                    )
                }
            }

            // Si hay una transición vertical a otro piso inmediatamente después de este segmento
            if (segmentIndex < route.transitions.size) {
                val transition = route.transitions[segmentIndex]
                val fromFloor = floorNameLookup(transition.fromFloorAsset)
                val toFloor = floorNameLookup(transition.toFloorAsset)
                val isGoingUp = isUpwardTransition(transition.fromFloorAsset, transition.toFloorAsset)
                val transitionType = if (isGoingUp) NavigationStepType.STAIRS_UP else NavigationStepType.STAIRS_DOWN
                val transitionTitle = if (isGoingUp) "Sube por las escaleras" else "Baja por las escaleras"
                val isKnownTransition = MapScaleCalibration.isKnownVerticalTransition(transition)
                val stairMeters = if (isKnownTransition) {
                    MapScaleCalibration.STAIR_INCLINED_DISTANCE_METERS_PER_FLOOR.roundToInt()
                } else {
                    0
                }
                val stairSeconds = if (isKnownTransition) {
                    MapScaleCalibration.STAIR_TRANSITION_SECONDS_PER_FLOOR.roundToInt()
                } else {
                    0
                }

                instructions.add(
                    NavigationInstruction(
                        stepIndex = stepCounter++,
                        type = transitionType,
                        title = transitionTitle,
                        detail = "Usa las escaleras para trasladarte de $fromFloor a $toFloor",
                        floorName = toFloor,
                        distanceMeters = stairMeters,
                        estimatedSeconds = stairSeconds
                    )
                )
            }
        }

        // Paso de llegada
        val lastSegment = route.segments.last()
        val lastFloorKey = lastSegment.floorAsset.ifBlank { lastSegment.floorId }
        instructions.add(
            NavigationInstruction(
                stepIndex = stepCounter++,
                type = NavigationStepType.DESTINATION,
                title = "Llegada al destino",
                detail = "Has llegado a $destinationName",
                floorName = floorNameLookup(lastFloorKey),
                distanceMeters = 0,
                estimatedSeconds = 0
            )
        )

        return WayfindingSummary(
            instructions = instructions,
            totalDistanceMeters = if (metricEstimate.isValid) metricEstimate.totalDistanceMeters else 0,
            totalTimeMinutes = if (metricEstimate.isValid) metricEstimate.totalTimeMinutes else 0,
            totalSteps = instructions.size,
            isAccessible = route.transitions.isEmpty(),
            horizontalDistanceMeters = if (metricEstimate.isValid) metricEstimate.horizontalDistanceMeters else 0f,
            verticalDistanceMeters = if (metricEstimate.isValid) metricEstimate.verticalDistanceMeters else 0f,
            estimatedSeconds = if (metricEstimate.isValid) metricEstimate.estimatedSeconds else 0,
            isMetricValid = metricEstimate.isValid
        )
    }

    private fun detectTurn(
        p1: NormalizedMapPoint,
        p2: NormalizedMapPoint,
        p3: NormalizedMapPoint,
        scale: FloorScale
    ): NavigationStepType {
        // Calculamos el ángulo en el espacio físico calibrado (metros) respetando proporción 1260×1600
        val dx1 = (p2.x - p1.x) * MapScaleCalibration.ORIGINAL_FRAME_WIDTH_PX * scale.metersPerPixelX
        val dy1 = (p2.y - p1.y) * MapScaleCalibration.ORIGINAL_FRAME_HEIGHT_PX * scale.metersPerPixelY
        val dx2 = (p3.x - p2.x) * MapScaleCalibration.ORIGINAL_FRAME_WIDTH_PX * scale.metersPerPixelX
        val dy2 = (p3.y - p2.y) * MapScaleCalibration.ORIGINAL_FRAME_HEIGHT_PX * scale.metersPerPixelY

        val angle1 = atan2(dy1.toDouble(), dx1.toDouble())
        val angle2 = atan2(dy2.toDouble(), dx2.toDouble())
        var diff = angle2 - angle1

        while (diff > Math.PI) diff -= 2 * Math.PI
        while (diff < -Math.PI) diff += 2 * Math.PI

        val degrees = Math.toDegrees(diff)
        return when {
            degrees in 35.0..145.0 -> NavigationStepType.TURN_RIGHT
            degrees in -145.0..-35.0 -> NavigationStepType.TURN_LEFT
            else -> NavigationStepType.WALK_STRAIGHT
        }
    }

    private fun isUpwardTransition(from: String, to: String): Boolean {
        val rank = mapOf("pb" to 0, "0" to 0, "p1" to 1, "1" to 1, "p2" to 2, "2" to 2)
        val fromRank = rank.entries.firstOrNull { from.contains(it.key, ignoreCase = true) }?.value ?: 0
        val toRank = rank.entries.firstOrNull { to.contains(it.key, ignoreCase = true) }?.value ?: 0
        return toRank > fromRank
    }

    private fun defaultFloorNameLookup(floorKey: String): String {
        return when (MapScaleCalibration.resolveFloorScale(floorKey)?.canonicalFloorId) {
            "PB" -> "Planta Baja"
            "P1" -> "Piso 1"
            "P2" -> "Piso 2"
            else -> floorKey
        }
    }
}
