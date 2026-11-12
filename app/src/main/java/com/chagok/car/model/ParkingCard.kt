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
    /** 어느 차의 기록인지 (집에 차가 여러 대일 때 구분). */
    val car: String? = null,
    /** 다음에 타는 사람에게 남긴 한마디. ("트렁크에 수박 있어") */
    val word: String? = null,
)
