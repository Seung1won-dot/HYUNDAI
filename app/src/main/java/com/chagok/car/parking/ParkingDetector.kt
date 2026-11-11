package com.chagok.car.parking

import com.chagok.car.model.Gear

/**
 * 주차 순간을 알아낸다.
 *
 * 규칙: 주행(D·R)을 한 번이라도 한 뒤 P단에 들어가면 주차로 본다.
 * 같은 주차에 대해 두 번 묻지 않도록, 다시 주행하기 전까지는 한 번만 알린다.
 * 기어 정보가 없는 차량은 화면의 '주차 위치 남기기' 버튼으로 기록한다.
 */
class ParkingDetector {
    private var droveSinceLastPark = false

    /** 기어가 바뀔 때마다 호출. 지금 주차 질문을 해야 하면 true. */
    fun onGear(gear: Gear): Boolean = when (gear) {
        Gear.DRIVE, Gear.REVERSE -> {
            droveSinceLastPark = true
            false
        }
        Gear.PARK -> {
            val ask = droveSinceLastPark
            droveSinceLastPark = false
            ask
        }
        else -> false
    }
}
