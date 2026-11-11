package com.chagok.car.data

import android.content.Context
import com.chagok.car.model.ParkingCard
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 주차 카드를 차 안(앱 저장소)에 JSON 파일로 보관한다.
 * 네트워크 없이도 브리핑이 되도록 차 쪽 기록이 기준이다. (가족 공유는 2단계에서 클라우드와 동기화)
 */
class ParkingStore(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "parking_cards.json"))

    fun load(): List<ParkingCard> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    /** 새 카드를 맨 앞에 넣고 최근 [KEEP]장만 남긴다. */
    fun add(card: ParkingCard): List<ParkingCard> {
        val all = (listOf(card) + load()).take(KEEP)
        val arr = JSONArray()
        all.forEach { arr.put(toJson(it)) }
        file.writeText(arr.toString())
        return all
    }

    private fun toJson(c: ParkingCard) = JSONObject().apply {
        put("id", c.id)
        put("memo", c.memo)
        put("driver", c.driver)
        put("parkedAt", c.parkedAtMillis)
        putOpt("lat", c.latitude)
        putOpt("lng", c.longitude)
        putOpt("place", c.placeName)
        putOpt("fuel", c.fuelPercent)
        putOpt("battery", c.batteryPercent)
        putOpt("range", c.rangeKm?.toDouble())
        putOpt("odometer", c.odometerKm?.toDouble())
        put("issues", JSONArray(c.issues))
    }

    private fun fromJson(o: JSONObject) = ParkingCard(
        id = o.getString("id"),
        memo = o.getString("memo"),
        driver = o.getString("driver"),
        parkedAtMillis = o.getLong("parkedAt"),
        latitude = if (o.has("lat")) o.getDouble("lat") else null,
        longitude = if (o.has("lng")) o.getDouble("lng") else null,
        placeName = if (o.has("place")) o.getString("place") else null,
        fuelPercent = if (o.has("fuel")) o.getInt("fuel") else null,
        batteryPercent = if (o.has("battery")) o.getInt("battery") else null,
        rangeKm = if (o.has("range")) o.getDouble("range").toFloat() else null,
        odometerKm = if (o.has("odometer")) o.getDouble("odometer").toFloat() else null,
        issues = o.optJSONArray("issues")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty(),
    )

    private companion object { const val KEEP = 50 }
}
