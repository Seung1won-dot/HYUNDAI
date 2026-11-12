package com.chagok.car.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chagok.car.briefing.Briefing
import com.chagok.car.briefing.CheckRules
import com.chagok.car.data.FamilySettings
import com.chagok.car.data.ParkingStore
import com.chagok.car.data.PlaceStore
import com.chagok.car.location.PleosLocation
import com.chagok.car.model.CarState
import com.chagok.car.model.Gear
import com.chagok.car.model.ParkingCard
import com.chagok.car.model.PlacePin
import com.chagok.car.navi.PleosNavi
import com.chagok.car.parking.FrequentSpot
import com.chagok.car.place.VoiceCommand
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
    data class Asking(
        val question: String,
        val kind: Ask = Ask.PARKING,
        /** 자주 가는 주차장이라 차가 먼저 제안한 자리. "응"이면 이 자리로 저장한다. */
        val suggestion: String? = null,
        val heard: String = "",
        val listening: Boolean = false,
    ) : Prompt
}

/** 차가 지금 무엇을 묻고 있는지. 대답을 어떻게 처리할지와 화면 안내 문구가 달라진다. */
enum class Ask { PARKING, PARKING_WHERE, COMMAND, PLACE_NAME, CAR_NAME, MEMBER_NAME }

data class UiState(
    val cards: List<ParkingCard> = emptyList(),
    val pins: List<PlacePin> = emptyList(),
    val carName: String = FamilySettings.DEFAULT_CAR,
    val drivers: List<String> = FamilySettings.DEFAULT_MEMBERS,
    val driver: String = FamilySettings.DEFAULT_MEMBERS.first(),
    val prompt: Prompt = Prompt.None,
    /** 차가 마지막으로 한 말 (소리를 못 들었을 때를 위해 화면에도 보여 준다). */
    val lastSpoken: String = "",
    val showTestPanel: Boolean = false,
    /** 테스트 패널에서 고른 가상 위치 이름 (실제 위치를 못 받는 환경용). */
    val testPlace: String? = null,
)

class ChagokViewModel(app: Application) : AndroidViewModel(app) {

    private val hub = CarStateHub()
    private val binder = PleosVehicleBinder(app, hub)
    private val speaker: Speaker = PleosSpeaker(app)
    private val listener: Listener = PleosListener(app)
    private val location = PleosLocation(app)
    private val store = ParkingStore(app)
    private val places = PlaceStore(app)
    private val family = FamilySettings(app)
    private val navi = PleosNavi(app)
    private val detector = ParkingDetector()

    val car: StateFlow<CarState> = hub.state

    private val _ui = MutableStateFlow(UiState(
        cards = store.load(),
        pins = places.load(),
        carName = family.carName,
        drivers = family.members,
        driver = family.members.first(),
    ))
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

    /** 차가 주차를 감지했거나, 사용자가 버튼을 눌렀을 때. 자주 가는 주차장이면 평소 자리를 먼저 제안한다. */
    fun askParking() {
        whereAmI { latLng ->
            val usual = FrequentSpot.suggest(_ui.value.cards, latLng?.first, latLng?.second)
            if (usual != null) ask("평소처럼 ${usual}에 주차하셨어요?", Ask.PARKING, usual)
            else ask("주차 위치를 남길까요?", Ask.PARKING)
        }
    }

    /** 음성 명령 듣기: "여기 다음에 오자", "그 빵집 안내해 줘". */
    fun askCommand() = ask("무엇을 도와드릴까요?", Ask.COMMAND)

    private fun ask(question: String, kind: Ask, suggestion: String? = null) {
        listener.stop()
        _ui.update { it.copy(prompt = Prompt.Asking(question, kind, suggestion), lastSpoken = question) }
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
        val asking = _ui.value.prompt as? Prompt.Asking ?: return
        if (MemoText.isDecline(raw)) {
            cancelPrompt()
            say("알겠어요.")
            return
        }
        when (asking.kind) {
            Ask.PARKING ->
                if (MemoText.isAccept(raw)) {
                    asking.suggestion?.let { saveCard(it) } ?: ask("어디에 주차하셨어요?", Ask.PARKING_WHERE)
                } else saveCard(MemoText.clean(raw))
            Ask.PARKING_WHERE -> saveCard(MemoText.clean(raw))
            Ask.COMMAND -> onCommand(VoiceCommand.parse(raw))
            Ask.PLACE_NAME -> savePin(MemoText.clean(raw))
            Ask.CAR_NAME -> renameCar(MemoText.clean(raw))
            Ask.MEMBER_NAME -> addMember(MemoText.clean(raw))
        }
    }

    // ---- 음성 장소 핀 ----

    private fun onCommand(command: VoiceCommand) {
        when (command) {
            is VoiceCommand.SavePlace -> command.name?.let { savePin(it) } ?: ask("이곳을 뭐라고 부를까요?", Ask.PLACE_NAME)
            is VoiceCommand.Navigate -> {
                cancelPrompt()
                val pin = VoiceCommand.findPin(_ui.value.pins, command.query)
                if (pin == null) say("${command.query}, 저장된 장소에서 찾지 못했어요.") else guideTo(pin)
            }
            VoiceCommand.Unknown -> {
                cancelPrompt()
                say("잘 못 알아들었어요. 여기 저장해 줘, 또는 빵집 안내해 줘처럼 말해 주세요.")
            }
        }
    }

    /** 지금 있는 곳을 가족 장소로 저장한다. 위치를 모르면 저장하지 않는다. */
    private fun savePin(name: String) {
        listener.stop()
        val driver = _ui.value.driver
        whereAmI { latLng ->
            _ui.update { it.copy(prompt = Prompt.None) }
            if (latLng == null) {
                say("지금 위치를 알 수 없어서 저장하지 못했어요.")
                return@whereAmI
            }
            val pin = PlacePin(UUID.randomUUID().toString(), name, driver, System.currentTimeMillis(), latLng.first, latLng.second)
            val all = places.add(pin)
            _ui.update { it.copy(pins = all) }
            say("$name, 가족 장소에 저장했어요.")
        }
    }

    /** 저장한 장소로 길 안내. 목록에서 누르거나 음성으로 부른다. */
    fun guideTo(pin: PlacePin) {
        if (navi.routeTo(pin)) say("${VoiceCommand.toward(pin.name)} 안내할게요.")
        else say("${pin.name}, 지금은 내비에 연결할 수 없어요.")
    }

    /** 지금 위치를 한 번만 읽는다. 테스트 패널에서 가상 위치를 골랐으면 그 값을 쓴다. */
    private fun whereAmI(onResult: (Pair<Double, Double>?) -> Unit) {
        val fake = _ui.value.testPlace?.let { TEST_PLACES[it] }
        if (fake != null) onResult(fake) else location.current(onResult)
    }

    fun cancelPrompt() {
        listener.stop()
        _ui.update { it.copy(prompt = Prompt.None) }
    }

    private fun saveCard(memo: String) {
        listener.stop()
        val state = hub.state.value
        val driver = _ui.value.driver
        whereAmI { latLng ->
            val card = ParkingCard(
                id = UUID.randomUUID().toString(),
                memo = memo,
                driver = driver,
                car = _ui.value.carName,
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

    // ---- 차 이름과 가족 구성 (정차 중에만) ----

    fun askCarName() = ask("이 차를 뭐라고 부를까요?", Ask.CAR_NAME)
    fun askNewMember() = ask("누구를 추가할까요?", Ask.MEMBER_NAME)

    private fun renameCar(name: String) {
        family.carName = name
        _ui.update { it.copy(carName = name, prompt = Prompt.None) }
        say("이 차를 ${VoiceCommand.toward(name)} 부를게요.")
    }

    private fun addMember(name: String) {
        val current = _ui.value.drivers
        val all = if (name in current) current else (current + name).takeLast(FamilySettings.MAX_MEMBERS)
        family.members = all
        _ui.update { it.copy(drivers = all, driver = name, prompt = Prompt.None) }
        say("$name, 추가했어요.")
    }
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
    /** 음성 인식을 못 쓰는 환경에서 말한 것처럼 명령을 넣어 본다. */
    fun testSay(text: String) = onCommand(VoiceCommand.parse(text))
    /** 가상 위치를 고른다. 같은 것을 다시 누르면 해제한다. */
    fun testPlace(name: String) = _ui.update { it.copy(testPlace = if (it.testPlace == name) null else name) }
    fun testFuel(percent: Int, rangeKm: Float) = hub.update { it.copy(fuelPercent = percent, rangeKm = rangeKm) }

    override fun onCleared() {
        binder.stop()
        speaker.release()
        listener.release()
        location.release()
        navi.release()
    }

    companion object {
        /** 테스트용 가상 위치. 실제 차량에서는 FusedLocation 값이 쓰인다. */
        val TEST_PLACES = linkedMapOf(
            "집" to (36.7712 to 126.9318),
            "마트" to (36.7836 to 127.0041),
            "빵집" to (36.7907 to 127.0025),
        )
    }
}
