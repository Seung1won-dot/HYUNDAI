package com.chagok.car.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import android.content.Context
import android.util.Log
import com.chagok.car.model.Gear

/**
 * Pleos Vehicle SDK → [CarStateHub] 연결.
 *
 * 각 항목을 한 번 조회(get)하고, 변화를 구독(register)한다.
 * 차량이 지원하지 않는 항목은 onFailed 로 떨어지고, 그 항목은 null 로 남아 안내에서 빠진다.
 */
class PleosVehicleBinder(context: Context, private val hub: CarStateHub) {

    private val vehicle = Vehicle(context.applicationContext)
    private val releasers = mutableListOf<() -> Unit>()

    private val doors = mapOf(
        DoorValue.ROW_1_LEFT to "운전석",
        DoorValue.ROW_1_RIGHT to "조수석",
        DoorValue.ROW_2_LEFT to "뒷좌석 왼쪽",
        DoorValue.ROW_2_RIGHT to "뒷좌석 오른쪽",
    )
    private val windows = mapOf(
        WindowValue.ROW_1_LEFT to "운전석",
        WindowValue.ROW_1_RIGHT to "조수석",
        WindowValue.ROW_2_LEFT to "뒷좌석 왼쪽",
        WindowValue.ROW_2_RIGHT to "뒷좌석 오른쪽",
    )

    fun start() {
        try {
            vehicle.initialize()
            bindGear()
            bindDoors()
            bindWindows()
            bindLight()
            bindEnergy()
            bindOdometer()
            bindTelltales()
        } catch (e: Throwable) {
            // Pleos 차량 서비스가 없는 기기(일반 에뮬레이터 등)에서는 테스트 패널로만 동작한다.
            Log.w(TAG, "Vehicle SDK를 쓸 수 없어요: ${e.message}")
        }
    }

    fun stop() {
        releasers.forEach { runCatching(it) }
        releasers.clear()
        runCatching { vehicle.release() }
    }

    private fun fail(what: String): (Exception) -> Unit = { e -> Log.i(TAG, "$what 미지원 또는 실패: ${e.message}") }

    private fun bindGear() {
        val gear = vehicle.getGear()
        val apply = { g: VehicleGear? -> hub.update { it.copy(gear = g.toGear()) } }
        gear.getCurrentGearSelectionState({ apply(it) }, fail("기어"))
        val l = object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) = apply(gear)
            override fun onFailed(e: Exception) = fail("기어")(e)
        }
        gear.registerGearSelection(l)
        releasers += { gear.unregisterGearSelection(l) }
    }

    private fun bindDoors() {
        val door = vehicle.getDoor()
        doors.forEach { (value, label) ->
            val area = DoorArea(value)
            val apply = { pos: Int? ->
                hub.update { s ->
                    s.copy(openDoors = if ((pos ?: 0) > 0) s.openDoors + label else s.openDoors - label)
                }
            }
            door.getCurrentDoorPosition(area, { apply(it) }, fail("$label 문"))
            val l = object : DoorPositionListener {
                override fun onPositionUpdated(pos: Int?) = apply(pos)
                override fun onFailed(e: Exception) = fail("$label 문")(e)
            }
            door.registerDoorPosition(area, l)
            releasers += { door.unregisterDoorPosition(area, l) }
        }
    }

    private fun bindWindows() {
        val window = vehicle.getWindow()
        windows.forEach { (value, label) ->
            val area = WindowArea(value)
            val apply = { pos: Int? ->
                hub.update { s ->
                    s.copy(openWindows = if ((pos ?: 0) > 0) s.openWindows + label else s.openWindows - label)
                }
            }
            window.getCurrentWindowPosition(area, { apply(it) }, fail("$label 창문"))
            val l = object : WindowPositionListener {
                override fun onWindowPositionUpdated(pos: Int?) = apply(pos)
                override fun onFailed(e: Exception) = fail("$label 창문")(e)
            }
            window.registerWindowPosition(area, l)
            releasers += { window.unregisterWindowPosition(area, l) }
        }
    }

    private fun bindLight() {
        val light = vehicle.getLight()
        val apply = { st: LightState? -> hub.update { it.copy(headlightOn = st?.let { v -> v == LightState.ON }) } }
        light.getCurrentLowBeamLightState({ apply(it) }, fail("라이트"))
        val l = object : LightStateListener {
            override fun onLightStateUpdated(state: LightState?) = apply(state)
            override fun onFailed(e: Exception) = fail("라이트")(e)
        }
        light.registerLowBeamLightState(l)
        releasers += { light.unregisterLowBeamLightState(l) }
    }

    private fun bindEnergy() {
        val fuel = vehicle.getFuel()
        fuel.getFuelPercent({ p -> hub.update { it.copy(fuelPercent = p) } }, fail("연료"))
        val fl = object : FuelPercentListener {
            override fun onFuelPercentUpdated(percent: Int?) = hub.update { it.copy(fuelPercent = percent) }
            override fun onFailed(e: Exception) = fail("연료")(e)
        }
        fuel.registerFuelPercent(fl)
        releasers += { fuel.unregisterFuelPercent(fl) }

        val ev = vehicle.getEvBattery()
        ev.getEvBatteryStateOfCharge({ p -> hub.update { it.copy(batteryPercent = p) } }, fail("배터리"))
        val el = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(soc: Int?) = hub.update { it.copy(batteryPercent = soc) }
            override fun onFailed(e: Exception) = fail("배터리")(e)
        }
        ev.registerEvBatteryStateOfCharge(el)
        releasers += { ev.unregisterEvBatteryStateOfCharge(el) }
    }

    private fun bindOdometer() {
        val odo = vehicle.getOdometer()
        odo.getCurrentDistanceDriven({ km -> hub.update { it.copy(odometerKm = km) } }, fail("주행거리"))
        val dl = object : DistanceDrivenListener {
            override fun onDistanceDrivenUpdated(distance: Float?) = hub.update { it.copy(odometerKm = distance) }
            override fun onFailed(e: Exception) = fail("주행거리")(e)
        }
        odo.registerDistanceDriven(dl)
        releasers += { odo.unregisterDistanceDriven(dl) }

        odo.getRangeRemainingDistance({ km -> hub.update { it.copy(rangeKm = km) } }, fail("주행가능거리"))
        val rl = object : RangeRemainingListener {
            override fun onRangeRemainingChanged(range: Float?) = hub.update { it.copy(rangeKm = range) }
            override fun onFailed(e: Exception) = fail("주행가능거리")(e)
        }
        odo.registerRangeRemainingListener(rl)
        releasers += { odo.unregisterRangeRemainingListener(rl) }
    }

    private fun bindTelltales() {
        val safety = vehicle.getSafety()
        val apply = { t: Telltales? ->
            val on = t?.telltales.orEmpty()
                .filter { it.state != TelltaleState.OFF }
                .map { it.type.name }
                .toSet()
            hub.update { it.copy(warnings = on, lowTirePressure = "TPMS" in on) }
        }
        safety.getTelltalesInfo({ apply(it) }, fail("경고등"))
        val l = object : TelltaleInfoListener {
            override fun onTelltaleInfoUpdated(telltales: Telltales?) = apply(telltales)
            override fun onFailed(e: Exception) = fail("경고등")(e)
        }
        safety.registerTelltales(l)
        releasers += { safety.unregisterTelltales(l) }
    }

    private fun VehicleGear?.toGear(): Gear = when (this) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        null, VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
        else -> Gear.DRIVE
    }

    private companion object { const val TAG = "ChagokVehicle" }
}
