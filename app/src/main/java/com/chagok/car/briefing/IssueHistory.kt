package com.chagok.car.briefing

import com.chagok.car.model.ParkingCard

/**
 * 주차 카드에 쌓인 상태 이상 기록에서 되풀이되는 것을 찾는다.
 * 가족이 함께 보고, 정비소에서 그대로 보여 줄 수 있는 근거가 된다. (고장 원인은 판단하지 않는다)
 */
object IssueHistory {
    const val DAYS = 14
    const val MIN_TIMES = 2

    data class Repeated(val label: String, val times: Int)

    fun repeated(cards: List<ParkingCard>, nowMillis: Long): List<Repeated> {
        val since = nowMillis - DAYS * 24L * 60 * 60 * 1000
        return cards.filter { it.parkedAtMillis >= since }
            .flatMap { c -> c.issues.map(::label).distinct() }
            // 창문·라이트처럼 내릴 때 챙기면 되는 것은 차의 이상이 아니므로 세지 않는다.
            .filterNot { l -> habits.any { l.startsWith(it) } }
            .groupingBy { it }.eachCount()
            .filter { it.value >= MIN_TIMES }
            .map { Repeated(it.key, it.value) }
            .sortedByDescending { it.times }
    }

    private val habits = listOf("창문", "라이트")

    /** 안내 문장의 첫 문장만 이름으로 쓴다. ("연료가 부족해요. 약 40km…" → "연료가 부족해요") */
    fun label(message: String) = message.substringBefore(". ").trimEnd('.')
}
