package com.chagok.car

import com.chagok.car.briefing.Briefing
import com.chagok.car.briefing.CheckRules
import com.chagok.car.briefing.Grade
import com.chagok.car.model.CarState
import com.chagok.car.model.ParkingCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckRulesTest {
    @Test fun `이상이 없으면 초록`() {
        val issues = CheckRules.drivingIssues(CarState(fuelPercent = 80, rangeKm = 500f))
        assertEquals(Grade.GREEN, CheckRules.worst(issues))
    }

    @Test fun `차량이 주지 않는 정보는 판단하지 않는다`() {
        assertTrue(CheckRules.drivingIssues(CarState()).isEmpty())
        assertTrue(CheckRules.exitIssues(CarState()).isEmpty())
    }

    @Test fun `연료 부족과 타이어는 노랑`() {
        val issues = CheckRules.drivingIssues(CarState(fuelPercent = 10, rangeKm = 40f, lowTirePressure = true))
        assertEquals(2, issues.size)
        assertEquals(Grade.YELLOW, CheckRules.worst(issues))
        assertTrue(issues.any { "40km" in it.message })
    }

    @Test fun `문이 열려 있으면 빨강`() {
        val issues = CheckRules.drivingIssues(CarState(openDoors = setOf("운전석")))
        assertEquals(Grade.RED, CheckRules.worst(issues))
    }

    @Test fun `내릴 때 창문과 라이트를 알려 준다`() {
        val issues = CheckRules.exitIssues(CarState(openWindows = setOf("운전석"), headlightOn = true))
        assertEquals(2, issues.size)
    }

    @Test fun `탑승 브리핑은 직전 운전자 기록과 상태를 함께 말한다`() {
        val card = ParkingCard("1", "B2 기둥 14", "엄마", 0L)
        val text = Briefing.boarding(card, CarState(lowTirePressure = true))
        assertTrue(text.startsWith("엄마님이 B2 기둥 14에 주차했어요."))
        assertTrue("타이어" in text)
    }

    @Test fun `주차 저장 안내는 놓친 것을 덧붙인다`() {
        val text = Briefing.parked("B2 기둥 14", CarState(openWindows = setOf("운전석")))
        assertEquals("B2 기둥 14, 저장했어요. 창문이 열려 있어요.", text)
    }
}
