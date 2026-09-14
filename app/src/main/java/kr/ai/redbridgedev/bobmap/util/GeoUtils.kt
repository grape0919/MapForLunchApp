package kr.ai.redbridgedev.bobmap.util

import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** 두 좌표 사이 거리(m), haversine. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** 도보 분: 70m/분, 최소 1분. */
    fun walkMinutes(meters: Int): Int = maxOf(1, ceil(meters / 70.0).toInt())

    fun radiusLabel(meters: Int): String = if (meters >= 1000) "${meters / 1000}km" else "${meters}m"
}
