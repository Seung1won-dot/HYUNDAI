package com.chagok.car.voice

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale
import android.speech.tts.TextToSpeech as AndroidTts
import android.speech.tts.UtteranceProgressListener

/**
 * Pleos TextToSpeech SDK로 말한다.
 * 초기화에 실패하면(예: Pleos 음성 서비스가 없는 기기) Android 기본 TTS로 넘어간다.
 */
class PleosSpeaker(context: Context) : Speaker {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var pleos: TextToSpeech? = null
    private var pleosReady = false
    private var fallback: AndroidTts? = null
    private var fallbackReady = false
    private var pendingDone: (() -> Unit)? = null

    private val events = object : EventListener {
        override fun onStart() = Unit
        override fun onReady() = Unit
        override fun onDone() = finish()
        override fun onStop() = finish()
        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "TTS 오류: $errorType")
            finish()
        }
        override fun onUpdatedRms(rms: Double) = Unit
    }

    init {
        try {
            val tts = TextToSpeech(appContext, Mode.ON_DEVICE)
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    tts.setLocale(Locale.KOREA)
                    tts.addEventListener(events)
                    pleos = tts
                    pleosReady = true
                }
                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "Pleos TTS 초기화 실패($errorType) → 기본 TTS 사용")
                    startFallback()
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "Pleos TTS를 쓸 수 없어요: ${e.message}")
            startFallback()
        }
    }

    private fun startFallback() {
        if (fallback != null) return
        fallback = AndroidTts(appContext) { status ->
            fallbackReady = status == AndroidTts.SUCCESS
            fallback?.language = Locale.KOREAN
            fallback?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finish()
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish()
            })
        }
    }

    private fun finish() {
        main.post {
            val done = pendingDone
            pendingDone = null
            done?.invoke()
        }
    }

    override fun speak(text: String, onDone: () -> Unit) {
        pendingDone = onDone
        val p = pleos
        when {
            pleosReady && p != null -> p.speak(text, true)
            fallbackReady -> fallback?.speak(text, AndroidTts.QUEUE_FLUSH, null, "chagok")
            // 아직 준비 전이면 말하지 않고 바로 다음 단계로 넘어간다 (화면 문구는 그대로 보인다).
            else -> finish()
        }
    }

    override fun stop() {
        runCatching { pleos?.stop() }
        runCatching { fallback?.stop() }
    }

    override fun release() {
        runCatching { pleos?.removeEventListener(events); pleos?.release() }
        runCatching { fallback?.shutdown() }
    }

    private companion object { const val TAG = "ChagokTts" }
}
