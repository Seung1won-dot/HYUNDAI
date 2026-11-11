package com.chagok.car.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.util.Log

/** 주차한 순간의 좌표만 한 번 읽는다. 실시간으로 따라가며 기록하지 않는다. */
class PleosLocation(context: Context) {
    private var fused: FusedLocation? = null

    init {
        try {
            fused = FusedLocation(context.applicationContext).also { it.initialize() }
        } catch (e: Throwable) {
            Log.w(TAG, "FusedLocation을 쓸 수 없어요: ${e.message}")
        }
    }

    /** 좌표를 얻지 못하면 null 을 넘긴다 (메모만 저장). */
    fun current(onResult: (Pair<Double, Double>?) -> Unit) {
        val f = fused
        if (f == null || runCatching { f.isSupported() }.getOrDefault(false).not()) {
            onResult(null)
            return
        }
        try {
            f.getLastKnownLocation(object : FusedLocationListener {
                override fun onChanged(location: LocationData?) = onResult(location?.let { it.latitude to it.longitude })
                override fun onError(e: Exception) = onResult(null)
            })
        } catch (e: Throwable) {
            onResult(null)
        }
    }

    fun release() { runCatching { fused?.release() } }

    private companion object { const val TAG = "ChagokLocation" }
}
