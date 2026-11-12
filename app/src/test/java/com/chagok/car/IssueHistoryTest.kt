package com.chagok.car

import com.chagok.car.briefing.IssueHistory
import com.chagok.car.model.ParkingCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueHistoryTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day
    private fun card(daysAgo: Int, vararg issues: String) =
        ParkingCard(id = "$daysAgo${issues.size}", memo = "A", driver = "엄마", parkedAtMillis = now - daysAgo * day, issues = issues.toList())

    private val tire = "타이어 공기압이 낮아요. 가까운 곳에서 점검하세요."

    @Test fun `2주 안에 두 번 이상 나온 이상만 센다`() {
        val cards = listOf(card(1, tire), card(3, tire, "창문이 열려 있어요."), card(5, tire))
        assertEquals(listOf(IssueHistory.Repeated("타이어 공기압이 낮아요", 3)), IssueHistory.repeated(cards, now))
    }

    @Test fun `2주가 지난 기록은 세지 않는다`() {
        assertTrue(IssueHistory.repeated(listOf(card(1, tire), card(20, tire)), now).isEmpty())
    }

    @Test fun `남은 거리가 달라도 같은 연료 부족으로 센다`() {
        val cards = listOf(card(1, "연료가 부족해요. 약 40km 갈 수 있어요."), card(2, "연료가 부족해요. 약 25km 갈 수 있어요."))
        assertEquals(2, IssueHistory.repeated(cards, now).single().times)
    }
}
