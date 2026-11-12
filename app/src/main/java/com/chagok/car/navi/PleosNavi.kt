package com.chagok.car.navi

import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import android.content.Context
import android.util.Log
import com.chagok.car.model.PlacePin

/** 저장한 장소로 차량 내비의 길 안내를 요청한다. (Pleos NaviHelper SDK) */
class PleosNavi(context: Context) {
    private var navi: NaviHelper? = null
    @Volatile private var ready = false

    init {
        try {
            navi = NaviHelper(context.applicationContext).also {
                it.initialize({ ready = true }, { e -> Log.w(TAG, "내비에 연결하지 못했어요: ${e.message}") })
            }
        } catch (e: Throwable) {
            Log.w(TAG, "NaviHelper를 쓸 수 없어요: ${e.message}")
        }
    }

    /** 길 안내를 요청했으면 true. 내비가 없는 환경이면 false. */
    fun routeTo(pin: PlacePin): Boolean {
        val n = navi ?: return false
        if (!ready) return false
        return try {
            n.requestRoute(RouteInfo(latitude = pin.latitude, longitude = pin.longitude, poiName = pin.name, poiId = "", address = "", poiSubId = ""))
            true
        } catch (e: Throwable) {
            Log.w(TAG, "길 안내 요청 실패: ${e.message}")
            false
        }
    }

    fun release() { runCatching { navi?.release() } }

    private companion object { const val TAG = "ChagokNavi" }
}
