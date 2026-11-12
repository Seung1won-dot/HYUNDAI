package com.chagok.car.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chagok.car.R
import com.chagok.car.briefing.CheckRules
import com.chagok.car.briefing.Grade
import com.chagok.car.briefing.IssueHistory
import com.chagok.car.data.FamilySettings
import com.chagok.car.model.CarState
import com.chagok.car.model.Gear
import com.chagok.car.model.ParkingCard
import com.chagok.car.model.PlacePin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 색·글꼴·간격은 docs/design/DESIGN.md 의 기준만 쓴다.

// ---- 색 ----
private val Bg = Color(0xFF101110)
private val Line = Color(0xFF2A2C29)
private val TextHi = Color(0xFFF4F1EA)   // 주조색(아이보리): 제목, 값, 주 버튼
private val TextMid = Color(0xFFA9A79F)
private val TextLow = Color(0xFF6F6E68)
// 아래 세 색은 차 상태를 알릴 때만 쓴다.
private val Ok = Color(0xFF57C785)
private val Warn = Color(0xFFF2B33D)
private val Danger = Color(0xFFF0604D)

// ---- 글꼴: Asta Sans 하나 (Pleos Connect 디자인 가이드 지정) ----
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun asta(weight: Int) =
    Font(R.font.asta_sans, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

private val Asta = FontFamily(asta(400), asta(600), asta(800))
private val Regular = FontWeight(400)
private val Semi = FontWeight(600)
private val Heavy = FontWeight(800)

private fun gradeColor(g: Grade) = when (g) {
    Grade.GREEN -> Ok
    Grade.YELLOW -> Warn
    Grade.RED -> Danger
}

private fun gradeLabel(g: Grade) = when (g) {
    Grade.GREEN -> "이상 없음"
    Grade.YELLOW -> "가까운 곳에서 조치"
    Grade.RED -> "출발 전 조치"
}

@Composable
fun ChagokApp(vm: ChagokViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val car by vm.car.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = darkColorScheme(primary = TextHi, surface = Bg, background = Bg)) {
        CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Asta, color = TextHi)) {
            // 차량 화면은 가로·세로(분할 화면) 모두 올 수 있어 폭에 따라 배치를 바꾼다.
            BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
                val wide = maxWidth > maxHeight
                Column(Modifier.fillMaxSize().padding(horizontal = 40.dp)) {
                    Header(ui, car, vm)
                    Rule()
                    if (wide) {
                        Row(Modifier.weight(1f)) {
                            Column(Modifier.weight(1.5f).fillMaxHeight().padding(top = 32.dp, bottom = 24.dp, end = 40.dp)) {
                                LastParking(ui.cards.firstOrNull(), ui.carName, Modifier.weight(1f).clipToBounds())
                                Spacer(Modifier.height(16.dp))
                                Actions(car, vm)
                            }
                            Box(Modifier.width(1.dp).fillMaxHeight().background(Line))
                            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 40.dp, top = 32.dp)) {
                                // 등급은 언제나 보이게 고정하고, 아래 목록만 넘겨 본다.
                                GradeHead(car)
                                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                                    Side(ui, car, vm)
                                }
                            }
                        }
                    } else {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 32.dp)) {
                            LastParking(ui.cards.firstOrNull(), ui.carName)
                            Spacer(Modifier.height(32.dp))
                            Actions(car, vm)
                            Spacer(Modifier.height(40.dp))
                            GradeHead(car)
                            Side(ui, car, vm)
                        }
                    }
                    if (ui.lastSpoken.isNotEmpty()) Spoken(ui.lastSpoken)
                    if (ui.showTestPanel) TestPanel(car, ui.testPlace, vm)
                }
                (ui.prompt as? Prompt.Asking)?.let { AskScreen(it, car.isDriving, vm) }
            }
        }
    }
}

@Composable
private fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(Line))

@Composable
private fun Label(text: String) = Text(text, color = TextMid, fontSize = 18.sp, fontWeight = Regular)

// ---- 머리줄: 앱 이름, 이 차, 지금 운전자 ----

@Composable
private fun Header(ui: UiState, car: CarState, vm: ChagokViewModel) {
    val parked = !car.isDriving
    Row(Modifier.fillMaxWidth().height(84.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("차곡차곡", fontSize = 22.sp, fontWeight = Heavy)
        Spacer(Modifier.width(16.dp))
        // 집에 차가 여러 대일 때 구분하는 이름. 정차 중에 눌러서 바꾼다.
        Text(
            ui.carName,
            color = TextMid,
            fontSize = 20.sp,
            modifier = Modifier.clickable(enabled = parked) { vm.askCarName() }.padding(vertical = 16.dp, horizontal = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Text("운전자", color = TextLow, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        ui.drivers.forEach { name ->
            val selected = name == ui.driver
            Column(
                Modifier.clickable(enabled = parked) { vm.selectDriver(name) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(name, fontSize = 20.sp, fontWeight = if (selected) Heavy else Regular, color = if (selected) TextHi else TextLow)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(28.dp).height(2.dp).background(if (selected) TextHi else Color.Transparent))
            }
        }
        if (ui.drivers.size < FamilySettings.MAX_MEMBERS) {
            Text(
                "+ 추가",
                color = TextLow,
                fontSize = 15.sp,
                modifier = Modifier.clickable(enabled = parked) { vm.askNewMember() }.padding(horizontal = 14.dp, vertical = 16.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            if (ui.showTestPanel) "테스트 닫기" else "테스트",
            color = TextLow,
            fontSize = 15.sp,
            modifier = Modifier.clickable { vm.toggleTestPanel() }.padding(vertical = 16.dp, horizontal = 8.dp),
        )
    }
}

// ---- 마지막 주차 위치: 이 화면의 주인공 ----

@Composable
private fun LastParking(card: ParkingCard?, carName: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Label("마지막 주차 위치")
        Spacer(Modifier.height(8.dp))
        if (card == null) {
            Text("아직 기록이 없어요", fontSize = 64.sp, lineHeight = 72.sp, fontWeight = Heavy)
            Spacer(Modifier.height(16.dp))
            Text("주차하면 차가 먼저 물어봐요.", color = TextMid, fontSize = 22.sp)
            return@Column
        }
        // 가장 중요한 정보 하나를 가장 크게: 1~2초 안에 읽히도록. 긴 메모는 한 단계 줄인다.
        val big = card.memo.length <= 12
        Text(
            card.memo,
            fontSize = if (big) 88.sp else 64.sp,
            lineHeight = if (big) 96.sp else 72.sp,
            fontWeight = Heavy,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${card.driver}님이 주차", fontSize = 22.sp, fontWeight = Semi)
            Text("   ${formatTime(card.parkedAtMillis)}   ${card.car ?: carName}", color = TextMid, fontSize = 22.sp)
        }
        Spacer(Modifier.height(32.dp))
        // 넘겨받는 사람이 알아야 할 것: 주차 당시 차 상태.
        Label("넘겨받을 때")
        Spacer(Modifier.height(12.dp))
        // 한눈에 읽히도록 세 줄까지만 보여 준다.
        val left = card.rangeKm?.let { "주행 가능 ${it.toInt()}km" } ?: card.fuelPercent?.let { "연료 $it%" }
        if (card.issues.isEmpty()) Mark(Ok, listOfNotNull("차 상태 이상 없음", left).joinToString(" · "))
        card.issues.take(3).forEachIndexed { i, issue ->
            val more = if (i == 2 && card.issues.size > 3) " 외 ${card.issues.size - 3}건" else ""
            val extra = if (i == 0 && left != null && IssueHistory.label(issue).startsWith("연료")) " · $left" else ""
            Mark(Warn, IssueHistory.label(issue) + extra + more)
        }
    }
}

/** 색 점 하나와 한 줄 문장. 알약 모양 꼬리표 대신 쓴다. */
@Composable
private fun Mark(color: Color, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---- 누르는 것 ----

@Composable
private fun Actions(car: CarState, vm: ChagokViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // 주 버튼은 화면에 하나.
        PushButton("주차 위치 남기기", Modifier.weight(1.3f), primary = true, enabled = !car.isDriving) { vm.askParking() }
        PushButton("탑승 브리핑", Modifier.weight(1f)) { vm.playBoardingBriefing() }
        // 누른 뒤에는 말로만 진행한다: "여기 다음에 오자", "그 빵집 안내해 줘".
        PushButton("말하기", Modifier.weight(0.8f)) { vm.askCommand() }
    }
}

/** 한 손으로 누르기 쉬운 큰 버튼 (높이 88dp). 주 버튼은 아이보리, 보조 버튼은 선 테두리. */
@Composable
private fun PushButton(text: String, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(88.dp)
            .clip(shape)
            .then(if (primary && enabled) Modifier.background(TextHi) else Modifier.border(1.dp, Line, shape))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 24.sp,
            fontWeight = Semi,
            maxLines = 1,
            color = when {
                !enabled -> TextLow
                primary -> Bg
                else -> TextHi
            },
        )
    }
}

// ---- 오른쪽 칸: 지금 차 상태, 가족 장소, 이전 기록 ----

/** 3단계 등급 한 줄. 색과 문구로 먼저 판단하게 한다. */
@Composable
private fun GradeHead(car: CarState) {
    val grade = CheckRules.worst(CheckRules.drivingIssues(car) + CheckRules.exitIssues(car))
    Label("지금 차 상태")
    Spacer(Modifier.height(8.dp))
    Text(gradeLabel(grade), color = gradeColor(grade), fontSize = 44.sp, lineHeight = 52.sp, fontWeight = Heavy, maxLines = 1)
    Spacer(Modifier.height(16.dp))
    Rule()
}

@Composable
private fun Side(ui: UiState, car: CarState, vm: ChagokViewModel) {
    Line("기어", gearLabel(car.gear))
    // 차량이 주지 않는 정보는 줄을 만들지 않는다.
    car.rangeKm?.let { Line("주행 가능", "${it.toInt()}km", if (it <= CheckRules.LOW_RANGE_KM) Warn else null) }
    car.fuelPercent?.let { Line("연료", "$it%", if (it <= CheckRules.LOW_FUEL_PERCENT) Warn else null) }
    car.batteryPercent?.let { Line("배터리", "$it%", if (it <= CheckRules.LOW_BATTERY_PERCENT) Warn else null) }
    Line("문", if (car.openDoors.isEmpty()) "닫힘" else "${car.openDoors.joinToString(", ")} 열림", if (car.openDoors.isEmpty()) null else Danger)
    Line("창문", if (car.openWindows.isEmpty()) "닫힘" else "열림", if (car.openWindows.isEmpty()) null else Warn)
    car.headlightOn?.let { Line("라이트", if (it) "켜짐" else "꺼짐", if (it) Warn else null) }
    car.lowTirePressure?.let { Line("타이어", if (it) "공기압 낮음" else "정상", if (it) Warn else null) }
    // 되풀이되는 이상: 가족이 함께 보고 정비소에서 보여 줄 근거.
    IssueHistory.repeated(ui.cards, System.currentTimeMillis()).forEach {
        Line("최근 2주", "${it.label} ${it.times}회", Warn)
    }

    Spacer(Modifier.height(40.dp))
    Label("가족 장소")
    Spacer(Modifier.height(12.dp))
    Rule()
    if (ui.pins.isEmpty()) Hint("“여기 다음에 오자”라고 말하면 저장돼요.")
    ui.pins.take(5).forEach { pin ->
        // 정차 중에는 눌러서, 주행 중에는 말로 길 안내를 부른다.
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !car.isDriving) { vm.guideTo(pin) }.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(pin.name, fontSize = 22.sp, fontWeight = Semi, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${pin.savedBy}님이 저장", color = TextLow, fontSize = 15.sp)
            }
            Text("길 안내", color = if (car.isDriving) TextLow else TextHi, fontSize = 18.sp, fontWeight = Semi)
        }
        Rule()
    }

    Spacer(Modifier.height(40.dp))
    Label("이전 기록")
    Spacer(Modifier.height(12.dp))
    Rule()
    val older = ui.cards.drop(1).take(6)
    if (older.isEmpty()) Hint("주차할 때마다 여기에 쌓여요.")
    older.forEach { c ->
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(c.memo, fontSize = 22.sp, fontWeight = Semi, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            Text("${c.driver} · ${formatTime(c.parkedAtMillis)}", color = TextLow, fontSize = 15.sp, maxLines = 1)
        }
        Rule()
    }
}

/** 항목 이름과 값 한 줄. 이상이 있으면 값에만 상태 색을 쓴다. */
@Composable
private fun Line(label: String, value: String, alert: Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMid, fontSize = 20.sp)
        Spacer(Modifier.width(24.dp))
        Text(
            value,
            color = alert ?: TextHi,
            fontSize = 22.sp,
            fontWeight = if (alert != null) Heavy else Semi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
    Rule()
}

@Composable
private fun Hint(text: String) = Text(text, color = TextLow, fontSize = 18.sp, modifier = Modifier.padding(vertical = 16.dp))

// ---- 차가 한 말 ----

@Composable
private fun Spoken(text: String) {
    Rule()
    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        VoiceBars(TextHi)
        Spacer(Modifier.width(20.dp))
        Text(text, fontSize = 24.sp, lineHeight = 32.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 음성 안내라는 표시. 주행 중 시선을 끌지 않도록 움직이지 않는 막대로 그린다. */
@Composable
private fun VoiceBars(color: Color, scale: Float = 1f) {
    Row(horizontalArrangement = Arrangement.spacedBy((4 * scale).dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(10, 22, 14, 26, 12).forEach { h ->
            Box(Modifier.width((4 * scale).dp).height((h * scale).dp).background(color))
        }
    }
}

// ---- 차가 묻는 화면 ----

/** 차가 묻고 대답을 기다리는 화면. 주행 중에는 입력칸 없이 음성으로만 받는다. */
@Composable
private fun AskScreen(asking: Prompt.Asking, driving: Boolean, vm: ChagokViewModel) {
    var typed by remember(asking.question) { mutableStateOf("") }
    val settings = asking.kind == Ask.CAR_NAME || asking.kind == Ask.MEMBER_NAME || asking.kind == Ask.COMMAND
    Box(Modifier.fillMaxSize().background(Bg).clickable(enabled = false) {}.imePadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 64.dp, vertical = 48.dp).widthIn(max = 960.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VoiceBars(TextHi, scale = 1.4f)
                Spacer(Modifier.width(20.dp))
                Text(if (asking.listening) "듣고 있어요" else "말씀하시거나 아래에 입력해 주세요", color = TextMid, fontSize = 20.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text(asking.question, fontSize = 56.sp, lineHeight = 68.sp, fontWeight = Heavy)
            Spacer(Modifier.height(16.dp))
            if (asking.heard.isNotEmpty()) Text("“${asking.heard}”", fontSize = 36.sp, lineHeight = 44.sp, fontWeight = Semi)
            else Text(askHint(asking), color = TextLow, fontSize = 22.sp)

            if (!driving) {
                Spacer(Modifier.height(40.dp))
                // 평소 자리를 제안했을 때는 한 번 눌러서도 끝낼 수 있다.
                if (asking.suggestion != null) {
                    PushButton("응, 맞아요", Modifier.fillMaxWidth(), primary = true) { vm.onAnswer("응") }
                    Spacer(Modifier.height(24.dp))
                }
                // 밑줄만 있는 입력칸. 키보드가 버튼을 가려도 완료 키로 바로 저장할 수 있다.
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Asta, fontSize = 28.sp, color = TextHi, fontWeight = Semi),
                    cursorBrush = SolidColor(TextHi),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (typed.isNotBlank()) vm.onAnswer(typed) }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { field ->
                        Column {
                            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                                if (typed.isEmpty()) Text("직접 입력", color = TextLow, fontSize = 28.sp)
                                field()
                            }
                            Box(Modifier.fillMaxWidth().height(2.dp).background(if (typed.isEmpty()) Line else TextHi))
                        }
                    },
                )
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PushButton(if (settings) "닫기" else "안 남길게요", Modifier.weight(1f)) { vm.cancelPrompt() }
                    PushButton(
                        if (asking.kind == Ask.COMMAND) "확인" else "저장",
                        Modifier.weight(1f),
                        primary = asking.suggestion == null,
                        enabled = typed.isNotBlank(),
                    ) { vm.onAnswer(typed) }
                }
            }
        }
    }
}

private fun askHint(asking: Prompt.Asking) = when (asking.kind) {
    Ask.PARKING -> if (asking.suggestion != null) "“응”이라고 하거나, 다른 자리면 그대로 말해 주세요" else "예: “지하 2층 B구역 기둥 14”"
    Ask.PARKING_WHERE -> "예: “지하 2층 B구역 기둥 14”"
    Ask.COMMAND -> "예: “여기 다음에 오자”, “그 빵집 안내해 줘”"
    Ask.PLACE_NAME -> "예: “성심당 빵집”"
    Ask.CAR_NAME -> "예: “엄마 차”, “흰색 SUV”. 집에 차가 여러 대일 때 구분하는 이름이에요"
    Ask.MEMBER_NAME -> "예: “아들”, “할머니”"
}

// ---- 테스트 패널 ----

/** 실제 차량 없이 흐름을 확인하는 패널. (Pleos 에뮬레이터에서는 차량 제어판 값이 그대로 들어온다.) */
@Composable
private fun TestPanel(car: CarState, testPlace: String?, vm: ChagokViewModel) {
    Rule()
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("테스트", color = TextLow, fontSize = 15.sp, modifier = Modifier.padding(end = 8.dp))
        listOf(Gear.PARK to "P", Gear.REVERSE to "R", Gear.NEUTRAL to "N", Gear.DRIVE to "D").forEach { (g, label) ->
            TestChip("기어 $label", car.gear == g) { vm.testGear(g) }
        }
        TestChip("창문 열림", car.openWindows.isNotEmpty(), vm::testToggleWindow)
        TestChip("문 열림", car.openDoors.isNotEmpty(), vm::testToggleDoor)
        TestChip("라이트 켜짐", car.headlightOn == true, vm::testToggleLight)
        TestChip("타이어 경고", car.lowTirePressure == true, vm::testToggleTire)
        TestChip("연료 10%", car.fuelPercent == 10) { vm.testFuel(10, 40f) }
        TestChip("연료 70%", car.fuelPercent == 70) { vm.testFuel(70, 420f) }
        // 실제 위치를 못 받는 환경에서 자주 가는 주차장과 장소 저장을 확인하기 위한 가상 위치.
        ChagokViewModel.TEST_PLACES.keys.forEach { name ->
            TestChip("위치 $name", testPlace == name) { vm.testPlace(name) }
        }
        listOf("여기 빵집으로 저장해 줘", "그 빵집 안내해 줘").forEach { say ->
            TestChip("말: $say", false) { vm.testSay(say) }
        }
    }
}

@Composable
private fun TestChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text,
        color = if (selected) Bg else TextMid,
        fontSize = 15.sp,
        fontWeight = if (selected) Semi else Regular,
        modifier = Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(TextHi) else Modifier.border(1.dp, Line, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

private fun gearLabel(g: Gear) = when (g) {
    Gear.PARK -> "P"
    Gear.REVERSE -> "R"
    Gear.NEUTRAL -> "N"
    Gear.DRIVE -> "D"
    Gear.UNKNOWN -> "–"
}

private fun formatTime(millis: Long): String {
    val day = SimpleDateFormat("yyyyMMdd", Locale.KOREAN)
    val pattern = if (day.format(Date(millis)) == day.format(Date())) "오늘 a h:mm" else "M월 d일 a h:mm"
    return SimpleDateFormat(pattern, Locale.KOREAN).format(Date(millis))
}
