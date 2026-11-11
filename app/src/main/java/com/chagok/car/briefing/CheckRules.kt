package com.chagok.car.briefing

import com.chagok.car.model.CarState

/** 3단계 등급. 사람이 정한 아래 규칙표로만 정하고, AI가 바꾸지 않는다. */
enum class Grade { GREEN, YELLOW, RED }

data class Issue(val grade: Grade, val message: String)

/**
 * 상태 → 안내 규칙표.
 * 차량이 주지 않는 정보(null)는 판단하지 않고 건너뛴다.
 */
object CheckRules {
    const val LOW_FUEL_PERCENT = 15
    const val LOW_BATTERY_PERCENT = 20
    const val LOW_RANGE_KM = 50f

    /** 탈 때·운전 중 확인할 항목. */
    fun drivingIssues(s: CarState): List<Issue> = buildList {
        if (s.openDoors.isNotEmpty()) {
            add(Issue(Grade.RED, "${s.openDoors.joinToString(", ")} 문이 열려 있어요. 출발 전에 닫아 주세요."))
        }
        if (s.lowTirePressure == true || "TPMS" in s.warnings) {
            add(Issue(Grade.YELLOW, "타이어 공기압이 낮아요. 가까운 곳에서 점검하세요."))
        }
        val range = s.rangeKm
        val lowFuel = (s.fuelPercent != null && s.fuelPercent <= LOW_FUEL_PERCENT) || "LOW_FUEL" in s.warnings
        val lowBattery = (s.batteryPercent != null && s.batteryPercent <= LOW_BATTERY_PERCENT) || "LOW_EV_BATTERY" in s.warnings
        val lowRange = range != null && range <= LOW_RANGE_KM
        if (lowFuel || lowBattery || lowRange) {
            val what = if (lowBattery && !lowFuel) "배터리가" else "연료가"
            val detail = when {
                range != null -> "약 ${range.toInt()}km 갈 수 있어요."
                lowBattery && s.batteryPercent != null -> "${s.batteryPercent}% 남았어요."
                s.fuelPercent != null -> "${s.fuelPercent}% 남았어요."
                else -> ""
            }
            add(Issue(Grade.YELLOW, "$what 부족해요. $detail".trim()))
        }
        if ("ENGINE_OIL_PRESSURE" in s.warnings || "ENGINE" in s.warnings) {
            add(Issue(Grade.RED, "엔진 경고등이 켜져 있어요. 출발 전에 확인하세요."))
        }
    }

    /** 내릴 때 놓치기 쉬운 항목. */
    fun exitIssues(s: CarState): List<Issue> = buildList {
        if (s.openWindows.isNotEmpty()) add(Issue(Grade.YELLOW, "창문이 열려 있어요."))
        if (s.headlightOn == true) add(Issue(Grade.YELLOW, "라이트가 켜져 있어요."))
    }

    fun worst(issues: List<Issue>): Grade = issues.maxOfOrNull { it.grade } ?: Grade.GREEN
}
