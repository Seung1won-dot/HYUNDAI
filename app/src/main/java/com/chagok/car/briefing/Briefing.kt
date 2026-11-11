package com.chagok.car.briefing

import com.chagok.car.model.CarState
import com.chagok.car.model.ParkingCard

/** 음성으로 읽어 줄 문장을 만든다. 네트워크 없이 동작한다. */
object Briefing {

    /** 탈 때: 직전 운전자의 기록 + 지금 차 상태. */
    fun boarding(last: ParkingCard?, state: CarState): String {
        val parts = mutableListOf<String>()
        if (last != null) parts += "${last.driver}님이 ${last.memo}에 주차했어요."
        val issues = CheckRules.drivingIssues(state)
        if (issues.isEmpty()) parts += "차 상태는 이상 없어요."
        else parts += issues.map { it.message }
        return parts.joinToString(" ")
    }

    /** 내릴 때: 저장 확인 + 놓친 것. */
    fun parked(memo: String, state: CarState): String {
        val parts = mutableListOf("$memo, 저장했어요.")
        parts += CheckRules.exitIssues(state).map { it.message }
        return parts.joinToString(" ")
    }
}
