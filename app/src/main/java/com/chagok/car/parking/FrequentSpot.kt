package com.chagok.car.parking

import com.chagok.car.model.ParkingCard
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 자주 가는 주차장: 지금 위치 근처에서 같은 자리를 여러 번 기록했으면 그 자리를 먼저 제안한다.
 * 그러면 "응" 한마디로 기록이 끝난다.
 */
object FrequentSpot {
    /** 같은 주차장으로 보는 반경. 지하에서는 좌표가 흔들리므로 넉넉히 잡는다. */
    const val NEAR_METERS = 150.0
    /** 이만큼 반복됐을 때부터 '평소 자리'로 본다. */
    const val MIN_TIMES = 2

    fun suggest(cards: List<ParkingCard>, latitude: Double?, longitude: Double?): String? {
        if (latitude == null || longitude == null) return null
        val near = cards.filter { c ->
            val lat = c.latitude
            val lng = c.longitude
            lat != null && lng != null && distanceMeters(lat, lng, latitude, longitude) <= NEAR_METERS
        }
        // 가장 자주 쓴 메모. 횟수가 같으면 더 최근 것(목록 앞쪽)을 고른다.
        val best = near.groupingBy { it.memo }.eachCount().maxByOrNull { it.value } ?: return null
        return best.key.takeIf { best.value >= MIN_TIMES }
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
