package com.chagok.car.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chagok.car.briefing.Briefing
import com.chagok.car.briefing.CheckRules
import com.chagok.car.data.ParkingStore
import com.chagok.car.location.PleosLocation
import com.chagok.car.model.CarState
import com.chagok.car.model.Gear
import com.chagok.car.model.ParkingCard
import com.chagok.car.parking.MemoText
import com.chagok.car.parking.ParkingDetector
import com.chagok.car.vehicle.CarStateHub
import com.chagok.car.vehicle.PleosVehicleBinder
import com.chagok.car.voice.Listener
import com.chagok.car.voice.PleosListener
import com.chagok.car.voice.PleosSpeaker
import com.chagok.car.voice.Speaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** 주차 질문 진행 단계. */
sealed interface Prompt {
    data object None : Prompt
    /** 차가 묻고 대답을 기다리는 중. [heard]는 지금까지 들린 말. */
    data class Asking(val question: String, val heard: String = "", val listening: Boolean = false) : Prompt
}

data class UiState(
    val cards: List<ParkingCard> = emptyList(),
    val drivers: List<String> = listOf("엄마", "아빠", "딸"),
    val driver: String = "엄마",
    val prompt: Prompt = Prompt.None,
    /** 차가 마지막으로 한 말 (소리를 못 들었을 때를 위해 화면에도 보여 준다). */
    val lastSpoken: String = "",
    val showTestPanel: Boolean = false,
)

class ChagokViewModel(app: Application) : AndroidViewModel(app) {

    private val hub = CarStateHub()
    private val binder = PleosVehicleBinder(app, hub)
    private val speaker: Speaker = PleosSpeaker(app)
    private val listener: Listener = PleosListener(app)
    private val location = PleosLocation(app)
    private val store = ParkingStore(app)
    private val detector = ParkingDetector()

    val car: StateFlow<CarState> = hub.state

    private val _ui = MutableStateFlow(UiState(cards = store.load()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var spokenIssues = emptySet<String>()

    init {
        binder.start()
        // 기어가 P로 들어오면 차가 먼저 묻는다.
        viewModelScope.launch {
            hub.state.distinctUntilChangedBy { it.gear }.collect { s ->
                if (detector.onGear(s.gear)) askParking()
                if (s.isDriving && _ui.value.prompt != Prompt.None) cancelPrompt()
            }
        }
        // 운전 중 새로 생긴 이상은 음성 한 줄로만 알린다.
        viewModelScope.launch {
            hub.state.collect { s ->
                val now = CheckRules.drivingIssues(s).map { it.message }.toSet()
                val fresh = now - spokenIssues
                spokenIssues = now
                if (s.isDriving && fresh.isNotEmpty()) say(fresh.first())
            }
        }
    }

    // ---- 주차 음성 메모 ----

    /** 차가 주차를 감지했거나, 사용자가 버튼을 눌렀을 때. */
    fun askParking(question: String = "주차 위치를 남길까요?") {
        _ui.update { it.copy(prompt = Prompt.Asking(question), lastSpoken = question) }
        speaker.speak(question) { startListening() }
    }

    private fun startListening() {
        val asking = _ui.value.prompt as? Prompt.Asking ?: return
        if (!listener.isAvailable) return // 음성 인식을 못 쓰면 화면에서 직접 입력한다.
        _ui.update { it.copy(prompt = asking.copy(listening = true)) }
        listener.listen(
            onPartial = { text -> updateAsking { it.copy(heard = text) } },
            onFinal = { text -> onAnswer(text) },
            onError = { updateAsking { it.copy(listening = false) } },
        )
    }

    /** 음성 또는 직접 입력으로 받은 대답. */
    fun onAnswer(raw: String) {
        if (_ui.value.prompt !is Prompt.Asking) return
        when {
            MemoText.isDecline(raw) -> {
                cancelPrompt()
                say("알겠어요.")
            }
            MemoText.isAccept(raw) -> askParking("어디에 주차하셨어요?")
            else -> saveCard(MemoText.clean(raw))
        }
    }

    fun cancelPrompt() {
        listener.stop()
        _ui.update { it.copy(prompt = Prompt.None) }
    }

    private fun saveCard(memo: String) {
        listener.stop()
        val state = hub.state.value
        val driver = _ui.value.driver
        location.current { latLng ->
            val card = ParkingCard(
                id = UUID.randomUUID().toString(),
                memo = memo,
                driver = driver,
                parkedAtMillis = System.currentTimeMillis(),
                latitude = latLng?.first,
                longitude = latLng?.second,
                fuelPercent = state.fuelPercent,
                batteryPercent = state.batteryPercent,
                rangeKm = state.rangeKm,
                odometerKm = state.odometerKm,
                issues = (CheckRules.drivingIssues(state) + CheckRules.exitIssues(state)).map { it.message },
            )
            val all = store.add(card)
            _ui.update { it.copy(cards = all, prompt = Prompt.None) }
            say(Briefing.parked(memo, state))
        }
    }

    // ---- 탑승 브리핑 ----

    fun playBoardingBriefing() = say(Briefing.boarding(_ui.value.cards.firstOrNull(), hub.state.value))

    private fun say(text: String) {
        _ui.update { it.copy(lastSpoken = text) }
        speaker.speak(text)
    }

    private fun updateAsking(change: (Prompt.Asking) -> Prompt.Asking) {
        _ui.update { s -> (s.prompt as? Prompt.Asking)?.let { s.copy(prompt = change(it)) } ?: s }
    }

    // ---- 화면 조작 ----

    fun selectDriver(name: String) = _ui.update { it.copy(driver = name) }
    fun toggleTestPanel() = _ui.update { it.copy(showTestPanel = !it.showTestPanel) }

    // ---- 테스트 패널: 차량 없이도 흐름을 확인할 수 있게 상태를 직접 바꾼다 ----

    fun testGear(gear: Gear) = hub.update { it.copy(gear = gear) }
    fun testToggleWindow() = hub.update {
        it.copy(openWindows = if (it.openWindows.isEmpty()) setOf("운전석") else emptySet())
    }
    fun testToggleDoor() = hub.update {
        it.copy(openDoors = if (it.openDoors.isEmpty()) setOf("운전석") else emptySet())
    }
    fun testToggleLight() = hub.update { it.copy(headlightOn = it.headlightOn != true) }
    fun testToggleTire() = hub.update { it.copy(lowTirePressure = it.lowTirePressure != true) }
    fun testFuel(percent: Int, rangeKm: Float) = hub.update { it.copy(fuelPercent = percent, rangeKm = rangeKm) }

    override fun onCleared() {
        binder.stop()
        speaker.release()
        listener.release()
        location.release()
    }
}
