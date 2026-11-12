package com.chagok.car.data

import android.content.Context

/**
 * 이 차의 이름과 함께 타는 가족 구성.
 * 앱은 차마다 설치되므로 차 이름은 차에서 한 번만 정한다. 집에 차가 여러 대면 가족 웹앱에서 이 이름으로 구분한다.
 */
class FamilySettings(context: Context) {
    private val prefs = context.getSharedPreferences("family", Context.MODE_PRIVATE)

    var carName: String
        get() = prefs.getString("car", DEFAULT_CAR) ?: DEFAULT_CAR
        set(value) = prefs.edit().putString("car", value).apply()

    var members: List<String>
        get() = prefs.getString("members", null)?.split('\n')?.filter { it.isNotBlank() } ?: DEFAULT_MEMBERS
        set(value) = prefs.edit().putString("members", value.joinToString("\n")).apply()

    /** 다음 사람에게 이미 읽어 준 쪽지. 같은 쪽지를 두 번 읽지 않는다. */
    var handedOverCardId: String?
        get() = prefs.getString("handedOver", null)
        set(value) = prefs.edit().putString("handedOver", value).apply()

    companion object {
        const val DEFAULT_CAR = "우리 집 차"
        /** 아들·딸·형제를 따로 정하지 않고 '자녀'로 묶어 두고, 필요하면 이름을 더한다. */
        val DEFAULT_MEMBERS = listOf("엄마", "아빠", "자녀")
        const val MAX_MEMBERS = 6
    }
}
