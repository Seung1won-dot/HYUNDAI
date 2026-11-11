package com.chagok.car.model

/** 주차할 때 남기는 기록 한 장. */
data class ParkingCard(
    val id: String,
    val memo: String,
    val driver: String,
    val parkedAtMillis: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null,
    /** 주차 시점의 차 상태 요약 (다음 운전자에게 넘겨줄 내용). */
    val fuelPercent: Int? = null,
    val batteryPercent: Int? = null,
    val rangeKm: Float? = null,
    val odometerKm: Float? = null,
    val issues: List<String> = emptyList(),
)
