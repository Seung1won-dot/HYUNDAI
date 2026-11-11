package com.chagok.car.model

/** 기어 위치. 주차 감지는 P단 진입으로 판단한다. */
enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

/**
 * 차에서 읽은 상태를 한데 모은 값.
 * null 은 "차량이 그 정보를 주지 않는다"는 뜻이고, 그런 항목은 안내에서 숨긴다.
 */
data class CarState(
    val gear: Gear = Gear.UNKNOWN,
    val openDoors: Set<String> = emptySet(),
    val openWindows: Set<String> = emptySet(),
    val headlightOn: Boolean? = null,
    val lowTirePressure: Boolean? = null,
    val fuelPercent: Int? = null,
    val batteryPercent: Int? = null,
    val rangeKm: Float? = null,
    val odometerKm: Float? = null,
    /** 켜져 있는 경고등 (SDK의 TelltaleType 이름 그대로). */
    val warnings: Set<String> = emptySet(),
) {
    val isDriving: Boolean get() = gear == Gear.DRIVE || gear == Gear.REVERSE
}
