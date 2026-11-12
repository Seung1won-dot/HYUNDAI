package com.chagok.car

import com.chagok.car.briefing.BoardingDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardingDetectorTest {
    private fun BoardingDetector.openAndClose(): Boolean {
        onDriverDoor(true)
        return onDriverDoor(false)
    }

    @Test fun `주차한 사람이 내릴 때는 읽지 않고 다음 사람이 탈 때 읽는다`() {
        val d = BoardingDetector()
        d.onParked()
        assertFalse(d.openAndClose()) // 내림
        assertTrue(d.openAndClose())  // 다음 사람이 탐
    }

    @Test fun `쪽지 한 장은 한 번만 읽는다`() {
        val d = BoardingDetector()
        d.onParked()
        d.openAndClose()
        d.openAndClose()
        assertFalse(d.openAndClose())
    }

    @Test fun `앱이 새로 켜졌고 안 읽은 쪽지가 있으면 첫 문 닫힘에 읽는다`() {
        assertTrue(BoardingDetector(unreadFromBefore = true).openAndClose())
        assertFalse(BoardingDetector(unreadFromBefore = false).openAndClose())
    }

    @Test fun `버튼으로 이미 읽었거나 그냥 출발했으면 읽지 않는다`() {
        val d = BoardingDetector()
        d.onParked()
        d.onHandedOver()
        d.openAndClose()
        assertFalse(d.openAndClose())
    }

    @Test fun `문이 닫힌 채로 오는 신호는 세지 않는다`() {
        val d = BoardingDetector(unreadFromBefore = true)
        assertFalse(d.onDriverDoor(false))
        assertTrue(d.openAndClose())
    }
}
