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

    // 말이 없으면 마이크를 계속 켜 두지 않는다. (Pleos 에뮬레이터에서 무음일 때 인식이 끝나지 않는 것을 확인)
    private val silence = Runnable {
        if (!delivered) {
            runCatching { stt?.stop() }
            if (lastText.isNotBlank()) deliver() else { delivered = true; onError("TIMEOUT") }
        }
    }
    private fun waitForSpeech() {
        main.removeCallbacks(silence)
        main.postDelayed(silence, SILENCE_MILLIS)
    }

    override val isAvailable: Boolean get() = ready

    private val results = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdated(stt: String, completed: Boolean) {
            lastText = stt
            main.post { if (completed) deliver() else { waitForSpeech(); onPartial(stt) } }
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
                    s.setLocale(Locale.KOREA)
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
        main.removeCallbacks(silence)
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
        waitForSpeech()
    }

    override fun stop() {
        main.removeCallbacks(silence)
        runCatching { stt?.stop() }
    }

    override fun release() {
        runCatching { stt?.removeListener(results); stt?.release() }
    }

    private companion object {
        const val TAG = "ChagokStt"
        const val SILENCE_MILLIS = 10_000L
    }
}
