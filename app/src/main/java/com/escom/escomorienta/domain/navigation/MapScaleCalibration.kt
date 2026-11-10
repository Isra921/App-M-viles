package com.escom.escomorienta.domain.navigation

import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Modelo de escala física calibrada por nivel cartográfico.
 *
 * Distingue explícitamente:
 * - Píxeles del frame original de diseño (1260 × 1600 px).
 * - Factores de ampliación cartográfica interna por piso (f_x, f_y).
 * - Escala física efectiva en metros por píxel del sistema de coordenadas de cada piso (s_x, s_y).
 */
data class FloorScale(
    val canonicalFloorId: String,
    val amplificationX: Float,
    val amplificationY: Float,
    val metersPerPixelX: Float,
    val metersPerPixelY: Float
) {
    val isUniform: Boolean
        get() = kotlin.math.abs(metersPerPixelX - metersPerPixelY) < 1e-6f
}

/**
 * Resultado calibrado de distancia física y tiempo estimado de traslado para una ruta.
 */
data class RouteMetricEstimate(
    val isValid: Boolean,
    val horizontalDistanceMeters: Float,
    val verticalDistanceMeters: Float,
    val totalDistanceMeters: Int,
    val estimatedSeconds: Int,
    val totalTimeMinutes: Int
) {
    companion object {
        val Invalid = RouteMetricEstimate(
            isValid = false,
            horizontalDistanceMeters = 0f,
            verticalDistanceMeters = 0f,
            totalDistanceMeters = 0,
            estimatedSeconds = 0,
            totalTimeMinutes = 0
        )
    }
}

/**
 * Calibración física del sistema cartográfico de ESCOM Orienta.
 *
 * Referencia original del diseñador sobre frame de 1260 × 1600 px (Planta Baja):
 * - 3.33 píxeles = 1 metro -> s_PB = 1 / 3.33 = 0.3003003 m/px.
 * - Auditorio en PB: 86 × 39 px (~25.83 m × 11.71 m, nominal 26 m × 12 m).
 *
 * Verificación geométrica en archivos reales (places_*.json y navigation_*.json) sobre frame 1260 × 1600:
 * - Salón 1001 (PB): 35.91 × 22.88 px (36 × 23 px nominales).
 * - Salón 1101 (P1) y 1201 (P2): 72.07 × 46.08 px (72 × 46 px nominales).
 * - Factor de ampliación verificado en P1 y P2 respecto a PB: f_x = 2.0, f_y = 2.0.
 * - Escala física efectiva en P1 y P2: s_P1 = s_P2 = s_PB / 2.0 = 0.15015015 m/px.
 */
object MapScaleCalibration {

    const val ORIGINAL_FRAME_WIDTH_PX = 1260f
    const val ORIGINAL_FRAME_HEIGHT_PX = 1600f

    const val PB_PIXELS_PER_METER = 3.33f
    const val PB_METERS_PER_PIXEL = 1f / PB_PIXELS_PER_METER

    const val PB_AMPLIFICATION_FACTOR_X = 1.0f
    const val PB_AMPLIFICATION_FACTOR_Y = 1.0f

    const val P1_AMPLIFICATION_FACTOR_X = 2.0f
    const val P1_AMPLIFICATION_FACTOR_Y = 2.0f

    const val P2_AMPLIFICATION_FACTOR_X = 2.0f
    const val P2_AMPLIFICATION_FACTOR_Y = 2.0f

    const val P1_METERS_PER_PIXEL = PB_METERS_PER_PIXEL / P1_AMPLIFICATION_FACTOR_X
    const val P2_METERS_PER_PIXEL = PB_METERS_PER_PIXEL / P2_AMPLIFICATION_FACTOR_X

    /**
     * Velocidad media de caminata peatonal en pasillos del plantel: 1.2 m/s (72 m/min).
     */
    const val WALKING_SPEED_METERS_PER_SECOND = 1.2f

    /**
     * Estimación física conservadora por transición vertical de 1 nivel en escaleras:
     * ~7.0 m de desarrollo inclinado de escalera (dos tramos con descanso para ~3.2-3.5 m de entrepiso)
     * y ~18 s de tiempo de ascenso/descenso por nivel.
     */
    const val STAIR_INCLINED_DISTANCE_METERS_PER_FLOOR = 7.0f
    const val STAIR_TRANSITION_SECONDS_PER_FLOOR = 18.0f

    val PbScale = FloorScale(
        canonicalFloorId = "PB",
        amplificationX = PB_AMPLIFICATION_FACTOR_X,
        amplificationY = PB_AMPLIFICATION_FACTOR_Y,
        metersPerPixelX = PB_METERS_PER_PIXEL / PB_AMPLIFICATION_FACTOR_X,
        metersPerPixelY = PB_METERS_PER_PIXEL / PB_AMPLIFICATION_FACTOR_Y
    )

    val P1Scale = FloorScale(
        canonicalFloorId = "P1",
        amplificationX = P1_AMPLIFICATION_FACTOR_X,
        amplificationY = P1_AMPLIFICATION_FACTOR_Y,
        metersPerPixelX = PB_METERS_PER_PIXEL / P1_AMPLIFICATION_FACTOR_X,
        metersPerPixelY = PB_METERS_PER_PIXEL / P1_AMPLIFICATION_FACTOR_Y
    )

    val P2Scale = FloorScale(
        canonicalFloorId = "P2",
        amplificationX = P2_AMPLIFICATION_FACTOR_X,
        amplificationY = P2_AMPLIFICATION_FACTOR_Y,
        metersPerPixelX = PB_METERS_PER_PIXEL / P2_AMPLIFICATION_FACTOR_X,
        metersPerPixelY = PB_METERS_PER_PIXEL / P2_AMPLIFICATION_FACTOR_Y
    )

    private val KnownConnectors = setOf(
        "PB_S01", "PB_S02", "PB_S03", "PB_S04",
        "PB_S05", "PB_S06", "PB_S07", "PB_S08"
    )

    /**
     * Resuelve la escala física calibrada para un identificador o asset de piso.
     * Devuelve null si el piso es desconocido o carece de calibración verificada.
     */
    fun resolveFloorScale(floorIdentifier: String?): FloorScale? {
        if (floorIdentifier.isNullOrBlank()) return null
        val normalized = floorIdentifier.trim().lowercase()
        return when {
            normalized == "pb" ||
                normalized == "0" ||
                normalized == "planta baja" ||
                normalized.contains("places_pb") ||
                normalized.contains("navigation_pb") ||
                normalized.contains("accesos_pb") -> PbScale

            normalized == "p1" ||
                normalized == "1" ||
                normalized == "piso 1" ||
                normalized.contains("places_p1") ||
                normalized.contains("navigation_p1") ||
                normalized.contains("accesos_p1") -> P1Scale

            normalized == "p2" ||
                normalized == "2" ||
                normalized == "piso 2" ||
                normalized.contains("places_p2") ||
                normalized.contains("navigation_p2") ||
                normalized.contains("accesos_p2") -> P2Scale

            else -> null
        }
    }

    /**
     * Convierte una longitud en píxeles del frame de diseño (1260 × 1600) a metros físicos,
     * aplicando el factor de ampliación correspondiente.
     */
    fun framePixelsToMeters(
        pixels: Float,
        amplificationFactor: Float = PB_AMPLIFICATION_FACTOR_X
    ): Float {
        if (!pixels.isFinite() || pixels < 0f) return 0f
        if (!amplificationFactor.isFinite() || amplificationFactor <= 0f) return 0f
        return (pixels * PB_METERS_PER_PIXEL) / amplificationFactor
    }

    /**
     * Calcula la distancia física en metros a partir de desplazamientos en píxeles del frame
     * y la escala física por eje del nivel.
     *
     * d = sqrt(((x2 - x1) * s_x)^2 + ((y2 - y1) * s_y)^2)
     */
    fun pixelDeltaToMeters(
        dxPixels: Float,
        dyPixels: Float,
        scale: FloorScale
    ): Float {
        if (!dxPixels.isFinite() || !dyPixels.isFinite()) return 0f
        val mx = dxPixels * scale.metersPerPixelX
        val my = dyPixels * scale.metersPerPixelY
        val dist = sqrt(mx * mx + my * my)
        return if (dist.isFinite() && dist >= 0f) dist else 0f
    }

    /**
     * Convierte la separación entre dos puntos en coordenadas normalizadas [0, 1]
     * primero a píxeles del frame original (1260 × 1600) y luego a metros físicos
     * según la escala calibrada del piso.
     *
     * El zoom o tamaño de pantalla de Android no interviene en este cálculo.
     */
    fun normalizedDistanceToMeters(
        p1: NormalizedMapPoint,
        p2: NormalizedMapPoint,
        floorIdentifier: String?,
        frameWidthPx: Float = ORIGINAL_FRAME_WIDTH_PX,
        frameHeightPx: Float = ORIGINAL_FRAME_HEIGHT_PX
    ): Float? {
        if (!isValidNormalizedPoint(p1) || !isValidNormalizedPoint(p2)) return null
        if (!frameWidthPx.isFinite() || frameWidthPx <= 0f || !frameHeightPx.isFinite() || frameHeightPx <= 0f) {
            return null
        }
        val scale = resolveFloorScale(floorIdentifier) ?: return null
        val dxPixels = (p2.x - p1.x) * frameWidthPx
        val dyPixels = (p2.y - p1.y) * frameHeightPx
        return pixelDeltaToMeters(dxPixels, dyPixels, scale)
    }

    /**
     * Calcula la longitud física acumulada (en metros) de una polilínea de recorrido en un piso.
     * Suma la trayectoria real segmento por segmento, no la línea recta entre extremos.
     */
    fun polylineDistanceMeters(
        points: List<NormalizedMapPoint>,
        floorIdentifier: String?,
        frameWidthPx: Float = ORIGINAL_FRAME_WIDTH_PX,
        frameHeightPx: Float = ORIGINAL_FRAME_HEIGHT_PX
    ): Float? {
        val scale = resolveFloorScale(floorIdentifier) ?: return null
        if (points.isEmpty()) return 0f
        if (points.any { !isValidNormalizedPoint(it) }) return null
        if (points.size == 1) return 0f

        var totalMeters = 0f
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dxPixels = (p2.x - p1.x) * frameWidthPx
            val dyPixels = (p2.y - p1.y) * frameHeightPx
            val segmentMeters = pixelDeltaToMeters(dxPixels, dyPixels, scale)
            if (!segmentMeters.isFinite() || segmentMeters < 0f) return null
            totalMeters += segmentMeters
        }
        return totalMeters
    }

    /**
     * Determina si una transición vertical entre pisos corresponde a un conector válido y calibrado.
     */
    fun isKnownVerticalTransition(transition: RouteTransition): Boolean {
        if (transition.connectorId.isBlank() || transition.stairNodeId.isBlank()) return false
        if (transition.connectorId !in KnownConnectors) return false
        val fromScale = resolveFloorScale(transition.fromFloorAsset) ?: return false
        val toScale = resolveFloorScale(transition.toFloorAsset) ?: return false
        return fromScale.canonicalFloorId != toScale.canonicalFloorId &&
            isValidNormalizedPoint(transition.point)
    }

    /**
     * Calcula el tiempo estimado en segundos y minutos a partir de la distancia horizontal (m)
     * y el número de transiciones verticales válidas.
     */
    fun estimateTravelTime(
        horizontalDistanceMeters: Float,
        validVerticalTransitions: Int,
        walkingSpeedMetersPerSecond: Float = WALKING_SPEED_METERS_PER_SECOND,
        stairTransitionSecondsPerFloor: Float = STAIR_TRANSITION_SECONDS_PER_FLOOR
    ): Pair<Int, Int> {
        if (!horizontalDistanceMeters.isFinite() || horizontalDistanceMeters < 0f) return 0 to 0
        if (!walkingSpeedMetersPerSecond.isFinite() || walkingSpeedMetersPerSecond <= 0f) return 0 to 0
        if (validVerticalTransitions < 0) return 0 to 0

        val horizontalSeconds = horizontalDistanceMeters / walkingSpeedMetersPerSecond
        val verticalSeconds = validVerticalTransitions * stairTransitionSecondsPerFloor
        val totalSecondsFloat = horizontalSeconds + verticalSeconds
        if (!totalSecondsFloat.isFinite() || totalSecondsFloat <= 0f) return 0 to 0

        val totalSeconds = totalSecondsFloat.roundToInt().coerceAtLeast(1)
        val totalMinutes = (totalSecondsFloat / 60f).roundToInt().coerceAtLeast(1)
        return totalSeconds to totalMinutes
    }

    /**
     * Evalúa una ruta completa [RouteResult] y devuelve su estimación física calibrada.
     */
    fun estimateRouteMetrics(route: RouteResult): RouteMetricEstimate {
        if (!route.success || route.segments.isEmpty()) {
            return RouteMetricEstimate.Invalid
        }

        var horizontalMeters = 0f
        for (segment in route.segments) {
            val floorKey = segment.floorAsset.ifBlank { segment.floorId }
            val segmentMeters = polylineDistanceMeters(segment.points, floorKey)
                ?: return RouteMetricEstimate.Invalid
            horizontalMeters += segmentMeters
        }

        var validTransitionsCount = 0
        for (transition in route.transitions) {
            if (isKnownVerticalTransition(transition)) {
                validTransitionsCount++
            }
        }

        val verticalMeters = validTransitionsCount * STAIR_INCLINED_DISTANCE_METERS_PER_FLOOR
        val totalMetersFloat = horizontalMeters + verticalMeters
        if (!totalMetersFloat.isFinite() || totalMetersFloat < 0.5f) {
            return RouteMetricEstimate.Invalid
        }

        val totalDistanceMeters = totalMetersFloat.roundToInt().coerceAtLeast(1)
        val (estimatedSeconds, totalTimeMinutes) = estimateTravelTime(
            horizontalDistanceMeters = horizontalMeters,
            validVerticalTransitions = validTransitionsCount
        )

        if (estimatedSeconds <= 0 || totalTimeMinutes <= 0) {
            return RouteMetricEstimate.Invalid
        }

        return RouteMetricEstimate(
            isValid = true,
            horizontalDistanceMeters = horizontalMeters,
            verticalDistanceMeters = verticalMeters,
            totalDistanceMeters = totalDistanceMeters,
            estimatedSeconds = estimatedSeconds,
            totalTimeMinutes = totalTimeMinutes
        )
    }

    private fun isValidNormalizedPoint(point: NormalizedMapPoint): Boolean {
        return point.x.isFinite() && point.y.isFinite() &&
            point.x in -0.01f..1.01f && point.y in -0.01f..1.01f
    }
}
