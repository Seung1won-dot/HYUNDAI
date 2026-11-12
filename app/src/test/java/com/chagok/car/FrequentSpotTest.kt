package com.chagok.car

import com.chagok.car.model.ParkingCard
import com.chagok.car.parking.FrequentSpot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrequentSpotTest {
    private fun card(memo: String, lat: Double?, lng: Double?) =
        ParkingCard(id = memo + lat, memo = memo, driver = "엄마", parkedAtMillis = 0, latitude = lat, longitude = lng)

    private val home = 36.7700 to 126.9330
    private val mart = 36.7800 to 127.0000

    @Test fun `같은 곳에서 두 번 이상 쓴 자리를 제안한다`() {
        val cards = listOf(card("지하 1층 A구역", home.first, home.second), card("지하 1층 A구역", home.first + 0.0002, home.second), card("B2 기둥 14", mart.first, mart.second))
        assertEquals("지하 1층 A구역", FrequentSpot.suggest(cards, home.first, home.second))
    }

    @Test fun `한 번만 쓴 자리는 제안하지 않는다`() {
        val cards = listOf(card("B2 기둥 14", mart.first, mart.second))
        assertNull(FrequentSpot.suggest(cards, mart.first, mart.second))
    }

    @Test fun `다른 주차장의 기록은 세지 않는다`() {
        val cards = listOf(card("지하 1층 A구역", home.first, home.second), card("지하 1층 A구역", home.first, home.second))
        assertNull(FrequentSpot.suggest(cards, mart.first, mart.second))
    }

    @Test fun `위치를 모르면 제안하지 않는다`() {
        val cards = listOf(card("A", null, null), card("A", null, null))
        assertNull(FrequentSpot.suggest(cards, null, null))
        assertNull(FrequentSpot.suggest(cards, home.first, home.second))
    }
}
