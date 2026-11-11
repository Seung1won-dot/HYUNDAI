package com.chagok.car.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chagok.car.briefing.CheckRules
import com.chagok.car.briefing.Grade
import com.chagok.car.model.CarState
import com.chagok.car.model.Gear
import com.chagok.car.model.ParkingCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---- 색 ----
// 어두운 실내에서 눈부시지 않게 배경은 짙게, 글자는 대비를 크게 둔다.
private val Bg = Color(0xFF0A0D12)
private val Panel = Color(0xFF141922)
private val PanelHigh = Color(0xFF1C2330)
private val Line = Color(0xFF263041)
private val TextHi = Color(0xFFF2F5F9)
private val TextMid = Color(0xFFA7B1C2)
private val TextLow = Color(0xFF6B7688)
private val Blue = Color(0xFF3D8BFF)
private val Green = Color(0xFF34C77B)
private val Yellow = Color(0xFFFFC043)
private val Red = Color(0xFFFF5D5D)

private fun gradeColor(g: Grade) = when (g) {
    Grade.GREEN -> Green
    Grade.YELLOW -> Yellow
    Grade.RED -> Red
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

    MaterialTheme(colorScheme = darkColorScheme(primary = Blue, surface = Panel, background = Bg)) {
        // 차량 화면은 가로·세로(분할 화면) 모두 올 수 있어 폭에 따라 배치를 바꾼다.
        BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
            val wide = maxWidth > maxHeight
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header(ui, car, vm)
                if (wide) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1.35f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ParkingHero(ui.cards.firstOrNull(), Modifier.weight(1f))
                            Actions(car, vm)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CarStatus(car)
                            History(ui.cards.drop(1), Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ParkingHero(ui.cards.firstOrNull(), Modifier.height(360.dp))
                        Actions(car, vm)
                        CarStatus(car)
                    }
                }
                if (ui.lastSpoken.isNotEmpty()) SpokenBar(ui.lastSpoken)
                if (ui.showTestPanel) TestPanel(car, vm)
            }
            (ui.prompt as? Prompt.Asking)?.let { AskOverlay(it, car.isDriving, vm) }
        }
    }
}

// ---- 머리줄 ----

@Composable
private fun Header(ui: UiState, car: CarState, vm: ChagokViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Blue),
            contentAlignment = Alignment.Center,
        ) { Text("P", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(12.dp))
        Text("차곡차곡", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextHi)
        Spacer(Modifier.width(28.dp))
        // 운전자 선택: 하나로 묶인 선택 막대. 주행 중에는 바꿀 수 없다.
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(Panel).border(1.dp, Line, RoundedCornerShape(50)).padding(4.dp),
        ) {
            ui.drivers.forEach { name ->
                val selected = name == ui.driver
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Blue else Color.Transparent)
                        .clickable(enabled = !car.isDriving) { vm.selectDriver(name) }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                ) {
                    Text(
                        name,
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else TextMid,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (ui.showTestPanel) "테스트 닫기" else "테스트",
            color = TextLow,
            fontSize = 16.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable { vm.toggleTestPanel() }.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

// ---- 최근 주차 위치 ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParkingHero(card: ParkingCard?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF16233B), Panel)))
            .border(1.dp, Line, RoundedCornerShape(28.dp))
            .padding(32.dp),
    ) {
        if (card == null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("최근 주차 위치")
                Text("아직 기록이 없어요", color = TextHi, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text("주차하면 차가 먼저 물어봐요.", color = TextMid, fontSize = 22.sp)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Eyebrow("최근 주차 위치")
                Spacer(Modifier.height(10.dp))
                // 가장 중요한 정보 하나를 가장 크게: 1~2초 안에 읽히도록.
                Text(
                    card.memo,
                    color = TextHi,
                    fontSize = 72.sp,
                    lineHeight = 80.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(card.driver, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("${card.driver}님이 주차", color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("  ·  ${formatTime(card.parkedAtMillis)}", color = TextMid, fontSize = 22.sp)
                }
                Spacer(Modifier.weight(1f))
                // 넘겨받는 사람이 알아야 할 것: 주차 당시 차 상태.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    card.rangeKm?.let { Tag("주행 가능 ${it.toInt()}km", TextMid) }
                    if (card.rangeKm == null) card.fuelPercent?.let { Tag("연료 $it%", TextMid) }
                    card.issues.forEach { Tag(it.substringBefore(". ").trimEnd('.'), Yellow) }
                    if (card.issues.isEmpty()) Tag("차 상태 이상 없음", Green)
                }
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, color = Blue, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Tag(text: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(color, 8.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Dot(color: Color, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
private fun Avatar(name: String, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(PanelHigh).border(1.dp, Line, CircleShape), contentAlignment = Alignment.Center) {
        Text(name.take(1), color = TextHi, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold)
    }
}

// ---- 큰 버튼 ----

@Composable
private fun Actions(car: CarState, vm: ChagokViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BigButton("주차 위치 남기기", enabled = !car.isDriving, modifier = Modifier.weight(1f)) { vm.askParking() }
        BigButton("탑승 브리핑 듣기", enabled = true, modifier = Modifier.weight(1f), primary = false) { vm.playBoardingBriefing() }
    }
}

/** 한 손으로 누르기 쉬운 큰 버튼 (높이 96dp). */
@Composable
private fun BigButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, primary: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val bg = when {
        !enabled -> PanelHigh
        primary -> Blue
        else -> Panel
    }
    Box(
        modifier
            .height(96.dp)
            .clip(shape)
            .background(bg)
            .then(if (primary || !enabled) Modifier else Modifier.border(1.5.dp, Line, shape))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else TextLow,
        )
    }
}

// ---- 차 상태 ----

@Composable
private fun CarStatus(car: CarState) {
    val issues = CheckRules.drivingIssues(car) + CheckRules.exitIssues(car)
    val grade = CheckRules.worst(issues)
    val color = gradeColor(grade)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(28.dp)),
    ) {
        // 등급 띠: 색과 한 줄 문구로 먼저 판단하게 한다.
        Row(
            Modifier.fillMaxWidth().background(color.copy(alpha = 0.14f)).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(color, 14.dp)
            Spacer(Modifier.width(12.dp))
            Text(gradeLabel(grade), color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("차 상태", color = TextMid, fontSize = 16.sp)
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("기어", gearLabel(car.gear), Modifier.weight(1f))
                // 차량이 주지 않는 정보는 칸을 만들지 않는다.
                car.rangeKm?.let { Stat("주행 가능", "${it.toInt()}", Modifier.weight(1f), unit = "km") }
                (car.fuelPercent ?: car.batteryPercent)?.let {
                    Stat(if (car.fuelPercent != null) "연료" else "배터리", "$it", Modifier.weight(1f), unit = "%", bar = it / 100f,
                        barColor = if (it <= CheckRules.LOW_BATTERY_PERCENT) Yellow else Blue)
                }
            }
            CheckRow("문", car.openDoors.isEmpty(), if (car.openDoors.isEmpty()) "닫힘" else "${car.openDoors.joinToString(", ")} 열림", Red)
            CheckRow("창문", car.openWindows.isEmpty(), if (car.openWindows.isEmpty()) "닫힘" else "열림", Yellow)
            car.headlightOn?.let { CheckRow("라이트", !it, if (it) "켜짐" else "꺼짐", Yellow) }
            car.lowTirePressure?.let { CheckRow("타이어", !it, if (it) "공기압 낮음" else "정상", Yellow) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, unit: String = "", bar: Float? = null, barColor: Color = Blue) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(PanelHigh).padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text(label, color = TextMid, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = TextHi, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotEmpty()) Text(" $unit", color = TextMid, fontSize = 18.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (bar != null) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Line)) {
                Box(Modifier.fillMaxWidth(bar.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(50)).background(barColor))
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean, value: String, alertColor: Color) {
    val color = if (ok) Green else alertColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(if (ok) "✓" else "!", color = color, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextMid, fontSize = 20.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = if (ok) TextHi else color, fontSize = 20.sp, fontWeight = if (ok) FontWeight.Normal else FontWeight.Bold)
    }
}

// ---- 이전 기록 ----

@Composable
private fun History(cards: List<ParkingCard>, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Panel).border(1.dp, Line, RoundedCornerShape(28.dp)).padding(24.dp),
    ) {
        Text("이전 기록", color = TextMid, fontSize = 16.sp)
        if (cards.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("주차할 때마다 여기에 쌓여요.", color = TextLow, fontSize = 18.sp)
        }
        LazyColumn(contentPadding = PaddingValues(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(cards, key = { it.id }) { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.driver, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(c.memo, color = TextHi, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Text(formatTime(c.parkedAtMillis), color = TextLow, fontSize = 16.sp)
                }
            }
        }
    }
}

// ---- 차가 한 말 ----

@Composable
private fun SpokenBar(text: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Blue.copy(alpha = 0.14f)).padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoiceBars(Blue)
        Spacer(Modifier.width(14.dp))
        Text(text, color = TextHi, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 음성 안내라는 표시. 주행 중 시선을 끌지 않도록 움직이지 않는 막대로 그린다. */
@Composable
private fun VoiceBars(color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(10, 22, 14, 26, 12).forEach { h ->
            Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

// ---- 주차 질문 ----

/** 차가 묻고 대답을 기다리는 화면. 주행 중에는 입력칸 없이 음성으로만 받는다. */
@Composable
private fun AskOverlay(asking: Prompt.Asking, driving: Boolean, vm: ChagokViewModel) {
    var typed by remember(asking.question) { mutableStateOf("") }
    Box(
        Modifier.fillMaxSize().background(Color(0xE6050709)).clickable(enabled = false) {}.imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 820.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Panel)
                .border(1.dp, Line, RoundedCornerShape(32.dp))
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 듣는 중임을 알리는 원
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(Blue.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(Blue), contentAlignment = Alignment.Center) {
                    VoiceBars(Color.White)
                }
            }
            Text(asking.question, color = TextHi, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            if (asking.heard.isNotEmpty()) {
                Text("“${asking.heard}”", color = Blue, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(
                    if (asking.listening) "듣고 있어요" else "말씀하시거나 아래에 입력해 주세요",
                    color = TextMid,
                    fontSize = 24.sp,
                )
                Text("예: “지하 2층 B구역 기둥 14”", color = TextLow, fontSize = 20.sp)
            }
            if (!driving) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 24.sp, color = TextHi),
                    placeholder = { Text("직접 입력", fontSize = 24.sp, color = TextLow) },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue,
                        unfocusedBorderColor = Line,
                        focusedContainerColor = PanelHigh,
                        unfocusedContainerColor = PanelHigh,
                    ),
                    // 키보드가 버튼을 가려도 완료 키로 바로 저장할 수 있게 한다.
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (typed.isNotBlank()) vm.onAnswer(typed) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BigButton("안 남길게요", enabled = true, modifier = Modifier.weight(1f), primary = false) { vm.cancelPrompt() }
                    BigButton("저장", enabled = typed.isNotBlank(), modifier = Modifier.weight(1f)) { vm.onAnswer(typed) }
                }
            }
        }
    }
}

// ---- 테스트 패널 ----

/** 실제 차량 없이 흐름을 확인하는 패널. (Pleos 에뮬레이터에서는 차량 제어판 값이 그대로 들어온다.) */
@Composable
private fun TestPanel(car: CarState, vm: ChagokViewModel) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).horizontalScroll(rememberScrollState()).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("테스트", color = TextLow, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 8.dp))
        listOf(Gear.PARK to "P", Gear.REVERSE to "R", Gear.NEUTRAL to "N", Gear.DRIVE to "D").forEach { (g, label) ->
            TestChip("기어 $label", car.gear == g) { vm.testGear(g) }
        }
        TestChip("창문 열림", car.openWindows.isNotEmpty(), vm::testToggleWindow)
        TestChip("문 열림", car.openDoors.isNotEmpty(), vm::testToggleDoor)
        TestChip("라이트 켜짐", car.headlightOn == true, vm::testToggleLight)
        TestChip("타이어 경고", car.lowTirePressure == true, vm::testToggleTire)
        TestChip("연료 10%", car.fuelPercent == 10) { vm.testFuel(10, 40f) }
        TestChip("연료 70%", car.fuelPercent == 70) { vm.testFuel(70, 420f) }
    }
}

@Composable
private fun TestChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.White else TextMid,
        fontSize = 16.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Blue.copy(alpha = 0.55f) else PanelHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
