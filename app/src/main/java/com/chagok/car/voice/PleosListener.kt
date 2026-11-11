package com.chagok.car.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

/** Pleos SpeechToText SDK로 차량 마이크의 말을 받아 적는다. */
class PleosListener(context: Context) : Listener {
    private val main = Handler(Looper.getMainLooper())
    private var stt: SpeechToText? = null
    @Volatile private var ready = false

    private var onPartial: (String) -> Unit = {}
    private var onFinal: (String) -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var lastText = ""
    private var delivered = false

    override val isAvailable: Boolean get() = ready

    private val results = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdated(stt: String, completed: Boolean) {
            lastText = stt
            main.post { if (completed) deliver() else onPartial(stt) }
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() { main.post { deliver() } }
        override fun onError(errorType: ErrorType) {
            main.post {
                if (!delivered) {
                    delivered = true
                    onError(errorType.name)
                }
            }
        }
    }

    init {
        try {
            val s = SpeechToText(context.applicationContext, Mode.ON_DEVICE)
            s.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    s.setLocale(Locale.KOREAN)
                    s.addListener(results)
                    stt = s
                    ready = true
                }
                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "Pleos STT 초기화 실패($errorType) → 직접 입력으로 대신")
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "Pleos STT를 쓸 수 없어요: ${e.message}")
        }
    }

    private fun deliver() {
        if (delivered) return
        delivered = true
        onFinal(lastText)
    }

    override fun listen(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (String) -> Unit) {
        val s = stt
        if (!ready || s == null) {
            onError("UNAVAILABLE")
            return
        }
        this.onPartial = onPartial
        this.onFinal = onFinal
        this.onError = onError
        lastText = ""
        delivered = false
        s.request()
    }

    override fun stop() { runCatching { stt?.stop() } }

    override fun release() {
        runCatching { stt?.removeListener(results); stt?.release() }
    }

    private companion object { const val TAG = "ChagokStt" }
}
