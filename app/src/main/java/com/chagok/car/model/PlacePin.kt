package com.chagok.car.model

/** "여기 다음에 오자"로 남긴 가족 장소 하나. */
data class PlacePin(
    val id: String,
    val name: String,
    val savedBy: String,
    val savedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
)
