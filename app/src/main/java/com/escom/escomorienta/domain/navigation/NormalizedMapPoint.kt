package com.escom.escomorienta.domain.navigation

import kotlin.math.sqrt

/**
 * Normalized 2D coordinate point on a floor map (values between 0.0 and 1.0).
 * Completely decoupled from Android UI and Jetpack Compose.
 */
data class NormalizedMapPoint(
    val x: Float,
    val y: Float
) {
    fun distanceSquaredTo(other: NormalizedMapPoint): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    fun distanceTo(other: NormalizedMapPoint): Float = sqrt(distanceSquaredTo(other))
}
