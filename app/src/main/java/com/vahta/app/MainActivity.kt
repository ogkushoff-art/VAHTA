
package com.vahta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.text.NumberFormat
import java.util.Locale

private val bg = Color(0xFF101C2C)
private val panel = Color(0xFF20344B)
private val accent = Color(0xFF58DDB2)
private val muted = Color(0xFFB3C4D7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("vahta_settings", MODE_PRIVATE)
        setContent {
            var start by remember { mutableStateOf(prefs.getString("start", "2026-11-01") ?: "2026-11-01") }
            var work by remember { mutableStateOf(prefs.getString("work", "30") ?: "30") }
            var rest by remember { mutableStateOf(prefs.getString("rest", "30") ?: "30") }
            var hourly by remember { mutableStateOf(prefs.getString("hourly", "600") ?: "600") }
            var salary by remember { mutableStateOf(prefs.getString("salary", "150000") ?: "150000") }
            var hours by remember { mutableStateOf(prefs.getString("hours", "11") ?: "11") }
            var mode by remember { mutableStateOf(prefs.getString("mode", "hourly") ?: "hourly") }
            var advanceDay by remember { mutableStateOf(prefs.getString("advance", "25") ?: "25") }
            var salaryDay by remember { mutableStateOf(prefs.getString("payday", "10") ?: "10") }
            var restDay by remember { mutableStateOf(prefs.getString("restday", "15") ?: "15") }
            var restSeparate by remember { mutableStateOf(prefs.getBoolean("restSeparate", false)) }
            var tab by remember { mutableStateOf(0) }
            var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) { while (true) { clock = System.currentTimeMillis(); delay(1000) } }
            fun save() {
                prefs.edit().putString("start", start).putString("work", work)
                    .putString("rest", rest).putString("hourly", hourly)
                    .putString("salary", salary).putString("hours", hours)
                    .putString("mode", mode).putString("advance", advanceDay)
                    .putString("payday", salaryDay).putString("restday", restDay)
                    .putBoolean("restSeparate", restSeparate).apply()
            }
            val startDate = runCatching { LocalDate.parse(start) }.getOrNull()
            val w = work.toIntOrNull()?.coerceIn(1, 365) ?: 30
            val r = rest.toIntOrNull()?.coerceIn(1, 365) ?: 30
            val today = LocalDate.now()
            val cyclePos = startDate?.let { Math.floorMod(ChronoUnit.DAYS.between(it, today), (w+r).toLong()).toInt() } ?: 0
            val isWorking = cyclePos < w
            val remaining = if (isWorking) w-cyclePos else w+r-cyclePos
            val elapsedDays = if (isWorking) cyclePos else w
            val hourlyAmount = hourly.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            val shiftHours = hours.toDoubleOrNull()?.coerceIn(0.0,24.0) ?: 0.0
            val monthlySalary = salary.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            val earned = if (mode == "hourly") elapsedDays*shiftHours*hourlyAmount else elapsedDays*monthlySalary/30.0
            val money = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU")) }
            MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = panel, onSurface = Color.White, onBackground = Color.White)) {
                Column(Modifier.fillMaxSize().background(bg)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("ВАХТА", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Работа · Деньги · Жизнь", color = muted)
                        when(tab) {
                            0 -> {
                                Tile("МОЯ ВАХТА") {
                                    Text(if (isWorking) "До межвахты: $remaining дн." else "До вахты: $remaining дн.", fontSize = 24.sp, color = accent)
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(progress = { cyclePos.toFloat()/(w+r) }, modifier = Modifier.fillMaxWidth(), color = accent)
                                    Text("График $w/$r · ${if (isWorking) "На вахте" else "На межвахте"}", color = muted)
                                }
                                Tile("РАСЧЁТНЫЙ ЗАРАБОТОК ЗА ЦИКЛ") {
                                    Text(money.format(earned), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accent)
                                    Text("Предварительно, до налогов и надбавок", color = muted, fontSize = 12.sp)
                                }
                                Tile("БЛИЖАЙШИЕ ВЫПЛАТЫ") {
                                    PaymentRow("Аванс", advanceDay, today)
                                    PaymentRow("Зарплата", salaryDay, today)
                                    if (restSeparate) PaymentRow("Межвахта", restDay, today)
                                    else Text("Межвахта: в составе зарплаты", color = muted)
                                }
                            }
                            1 -> {
                                Tile("КАЛЕНДАРЬ ВАХТ") {
                                    Text("Сегодня: $today", color = muted)
                                    Text(if (isWorking) "Рабочий период" else "Межвахтовый отдых", color = accent)
                                    Text("До смены периода: $remaining дн.", color = Color.White)
                                }
                                Tile("ГРАФИК ВЫПЛАТ") {
                                    PaymentRow("Аванс", advanceDay, today)
                                    PaymentRow("Зарплата", salaryDay, today)
                                    if (restSeparate) PaymentRow("Межвахта", restDay, today)
                                }
                            }
                            2 -> {
                                Tile("СПОСОБ ОПЛАТЫ") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = mode=="hourly", onClick = { mode="hourly"; save() }, label = { Text("Почасовая") })
                                        FilterChip(selected = mode=="salary", onClick = { mode="salary"; save() }, label = { Text("Оклад") })
                                    }
                                    if(mode=="hourly") {
                                        Field("ЧТС, ₽/час", hourly) { hourly=it; save() }
                                        Field("Часов в смене", hours) { hours=it; save() }
                                    } else Field("Оклад, ₽/месяц", salary) { salary=it; save() }
                                }
                                Tile("ГРАФИК ПЕРЕЧИСЛЕНИЙ") {
                                    Field("День аванса (1–31)", advanceDay) { advanceDay=it; save() }
                                    Field("День зарплаты (1–31)", salaryDay) { salaryDay=it; save() }
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Checkbox(checked=restSeparate, onCheckedChange={restSeparate=it; save()})
                                        Text("Межвахта отдельной выплатой")
                                    }
                                    if(restSeparate) Field("День выплаты межвахты", restDay) { restDay=it; save() }
                                    Text("Даты предварительные: переносы на выходные и праздники ещё не учитываются.", color=muted, fontSize=12.sp)
                                }
                            }
                            3 -> {
                                Tile("ГРАФИК РАБОТЫ") {
                                    Field("Начало вахты (ГГГГ-ММ-ДД)", start) { start=it; save() }
                                    if(startDate==null) Text("Неверная дата", color=Color(0xFFFF8888))
                                    Field("Дней вахты", work) { work=it; save() }
                                    Field("Дней межвахты", rest) { rest=it; save() }
                                }
                                Tile("О ПРОГНОЗЕ") {
                                    Text("Это тестовая версия. Оплата межвахтовых дней, отпускные, налог, ночные и переработки пока не включены в сумму.", color=muted)
                                }
                            }
                        }
                    }
                    NavigationBar(containerColor = panel) {
                        listOf("Главная", "Календарь", "Финансы", "Настройки").forEachIndexed { index, label ->
                            NavigationBarItem(selected=tab==index, onClick={tab=index}, icon={Text(listOf("⌂","▦","₽","⚙")[index])}, label={Text(label, fontSize=10.sp)})
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(18.dp), modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text(title, fontSize=12.sp, fontWeight=FontWeight.Bold, color=muted)
            content()
        }
    }
}

@Composable
private fun Field(label: String, value: String, change: (String)->Unit) {
    OutlinedTextField(value=value, onValueChange=change, label={Text(label)}, modifier=Modifier.fillMaxWidth(), singleLine=true)
}

@Composable
private fun PaymentRow(label: String, dayText: String, today: LocalDate) {
    val day = dayText.toIntOrNull()?.coerceIn(1,31) ?: 1
    fun dateFor(month: YearMonth): LocalDate = month.atDay(day.coerceAtMost(month.lengthOfMonth()))
    val month = YearMonth.from(today)
    val candidate = dateFor(month)
    val date = if(candidate.isBefore(today)) dateFor(month.plusMonths(1)) else candidate
    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label, color=Color.White)
        Text(date.toString(), color=accent)
    }
}
