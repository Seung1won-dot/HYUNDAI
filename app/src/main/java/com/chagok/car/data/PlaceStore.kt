package com.chagok.car.data

import android.content.Context
import com.chagok.car.model.PlacePin
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 가족 장소 목록을 차 안에 JSON 파일로 보관한다. (가족 공유는 클라우드 연결 뒤 동기화) */
class PlaceStore(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "place_pins.json"))

    fun load(): List<PlacePin> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                PlacePin(o.getString("id"), o.getString("name"), o.getString("by"), o.getLong("at"), o.getDouble("lat"), o.getDouble("lng"))
            }
        }.getOrDefault(emptyList())
    }

    /** 새 장소를 맨 앞에 넣는다. 같은 이름이 있으면 새 위치로 바꾼다. */
    fun add(pin: PlacePin): List<PlacePin> {
        val all = (listOf(pin) + load().filter { it.name != pin.name }).take(KEEP)
        val arr = JSONArray()
        all.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("by", it.savedBy)
                put("at", it.savedAtMillis); put("lat", it.latitude); put("lng", it.longitude)
            })
        }
        file.writeText(arr.toString())
        return all
    }

    private companion object { const val KEEP = 100 }
}
