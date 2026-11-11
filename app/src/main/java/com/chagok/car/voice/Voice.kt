package com.chagok.car.voice

/** 말하기. Pleos TextToSpeech SDK를 먼저 쓰고, 없으면 Android 기본 TTS로 대신한다. */
interface Speaker {
    fun speak(text: String, onDone: () -> Unit = {})
    fun stop()
    fun release()
}

/** 듣기. Pleos SpeechToText SDK를 쓴다. 쓸 수 없으면 [isAvailable]이 false 이고 화면에서 직접 입력한다. */
interface Listener {
    val isAvailable: Boolean
    fun listen(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (String) -> Unit)
    fun stop()
    fun release()
}
