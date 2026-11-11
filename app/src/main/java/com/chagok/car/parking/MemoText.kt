package com.chagok.car.parking

/** 음성 인식 결과를 다듬고, "아니/괜찮아" 같은 거절인지 가려낸다. */
object MemoText {
    private val declines = listOf("아니", "괜찮아", "됐어", "안 해", "안해", "필요 없", "취소")
    private val accepts = listOf("응", "어", "네", "예", "그래", "맞아")

    fun clean(raw: String): String = raw.trim().trimEnd('.', '!', '?', ' ')

    fun isDecline(raw: String): Boolean {
        val t = clean(raw)
        return t.isEmpty() || declines.any { t.startsWith(it) }
    }

    /** "응" 한마디처럼 평소 자리 그대로라는 대답인지. */
    fun isAccept(raw: String): Boolean {
        val t = clean(raw)
        return accepts.any { t == it || t == "$it 맞아" }
    }
}
