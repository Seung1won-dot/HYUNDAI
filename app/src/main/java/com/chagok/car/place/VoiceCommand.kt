package com.chagok.car.place

import com.chagok.car.model.PlacePin

/** 음성 명령을 가려낸다. 정해진 말투만 알아듣는 단순한 규칙이라 네트워크 없이 동작한다. */
sealed interface VoiceCommand {
    /** "여기 다음에 오자", "여기 빵집으로 저장해 줘" */
    data class SavePlace(val name: String?) : VoiceCommand
    /** "그 빵집 안내해 줘", "학교 가자" */
    data class Navigate(val query: String) : VoiceCommand
    data object Unknown : VoiceCommand

    companion object {
        private val saveWords = listOf("저장", "기억", "오자", "다음에", "또 오")
        private val named = Regex("여기(?:를|는)?\\s*(.+?)\\s*(?:으로|로|이라고|라고)\\s*(?:저장|기억)")
        private val navigate = Regex("^(?:그\\s+)?(.+?)\\s*(?:으로|로)?\\s*(?:안내|가자|가 ?줘|데려다)")

        fun parse(raw: String): VoiceCommand {
            val t = raw.trim().trimEnd('.', '!', '?', ' ')
            if (t.startsWith("여기") && saveWords.any { it in t }) {
                return SavePlace(named.find(t)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() })
            }
            navigate.find(t)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return Navigate(it) }
            return Unknown
        }

        /** 말한 이름과 가장 잘 맞는 저장 장소. 띄어쓰기는 무시하고, 여러 개면 최근 것. */
        fun findPin(pins: List<PlacePin>, query: String): PlacePin? {
            val q = key(query)
            if (q.isEmpty()) return null
            return pins.firstOrNull { key(it.name) == q }
                ?: pins.firstOrNull { key(it.name).contains(q) || q.contains(key(it.name)) }
        }

        private fun key(s: String) = s.replace(" ", "").lowercase()

        /** 받침에 따라 "으로/로"를 붙인다. */
        fun toward(name: String): String {
            val last = name.lastOrNull() ?: return name
            if (last !in '가'..'힣') return "${name}로"
            val jong = (last - '가') % 28
            return if (jong == 0 || jong == 8) "${name}로" else "${name}으로"
        }
    }
}
