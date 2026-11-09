
package com.vahta.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val bg = Color(0xFF0B1728)
private val panel = Color(0xFF192D45)
private val mint = Color(0xFF56E4C0)
private val muted = Color(0xFFB4C6D9)
private val blue = Color(0xFF4389D8)
private val gold = Color(0xFFFFB45B)
private val ru = Locale.forLanguageTag("ru-RU")
private val rotations = listOf("15×15", "30×30", "45×45", "60×60", "60×30")

private fun parseDate(v: String): LocalDate? = runCatching { LocalDate.parse(v) }.getOrNull()
private fun LocalDate.pretty(): String = format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
private fun num(v: String): Double = v.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
private fun rub(v: Double): String = NumberFormat.getCurrencyInstance(ru).format(v)
private fun rotationRest(v: String): Int = v.substringAfter('×', "45").toIntOrNull() ?: 45
private fun rotationWork(v: String): Int = v.substringBefore('×').toIntOrNull() ?: 45

private data class Config(
    val name: String = "", val rotation: String = "", val arrival: String = "", val departure: String = "",
    val hourly: String = "", val hours: String = "", val workDays: String = "", val restDays: String = "",
    val coefficient: String = "", val north: String = "", val premium: String = "", val allowance: String = "",
    val travelHours: String = "", val intershiftHours: String = "", val intershiftRate: String = "",
    val tax: String = "", val meals: String = "", val enforcement: String = "", val otherDeductions: String = "",
    val advanceDay: String = "", val salaryDay: String = "", val advanceMethod: String = "По отработанным часам",
    val fixedAdvance: String = "", val notes: String = ""
)

private fun readConfig(p: SharedPreferences, prefix: String = ""): Config {
    fun get(k: String) = p.getString(prefix + k, "") ?: ""
    return Config(
        name = get("name"), rotation = get("rotation"), arrival = get("arrival"), departure = get("departure"),
        hourly = get("hourly"), hours = get("hours"), workDays = get("workDays"), restDays = get("restDays"),
        coefficient = get("coefficient"), north = get("north"), premium = get("premium"), allowance = get("allowance"),
        travelHours = get("travelHours"), intershiftHours = get("intershiftHours"), intershiftRate = get("intershiftRate"),
        tax = get("tax"), meals = get("meals"), enforcement = get("enforcement"), otherDeductions = get("otherDeductions"),
        advanceDay = get("advanceDay"), salaryDay = get("salaryDay"),
        advanceMethod = get("advanceMethod").ifBlank { "По отработанным часам" },
        fixedAdvance = get("fixedAdvance"), notes = get("notes")
    )
}

private fun saveConfig(p: SharedPreferences, s: Config, prefix: String = "") {
    p.edit().apply {
        fun put(k: String, v: String) { putString(prefix + k, v) }
        put("name", s.name); put("rotation", s.rotation); put("arrival", s.arrival); put("departure", s.departure)
        put("hourly", s.hourly); put("hours", s.hours); put("workDays", s.workDays); put("restDays", s.restDays)
        put("coefficient", s.coefficient); put("north", s.north); put("premium", s.premium); put("allowance", s.allowance)
        put("travelHours", s.travelHours); put("intershiftHours", s.intershiftHours); put("intershiftRate", s.intershiftRate)
        put("tax", s.tax); put("meals", s.meals); put("enforcement", s.enforcement); put("otherDeductions", s.otherDeductions)
        put("advanceDay", s.advanceDay); put("salaryDay", s.salaryDay); put("advanceMethod", s.advanceMethod)
        put("fixedAdvance", s.fixedAdvance); put("notes", s.notes)
        apply()
    }
}

private fun clearDraft(p: SharedPreferences) {
    p.edit().apply {
        p.all.keys.filter { it.startsWith("draft_") }.forEach { remove(it) }
        remove("setup_step")
        apply()
    }
}

private data class Period(
    val start: LocalDate, val end: LocalDate, val next: LocalDate,
    val working: Boolean, val elapsed: Long, val total: Long, val remaining: Long, val progress: Float
)

private fun periodFor(day: LocalDate, first: LocalDate, last: LocalDate, rotation: String): Period {
    val work = (ChronoUnit.DAYS.between(first, last) + 1).coerceAtLeast(1)
    val rest = rotationRest(rotation).toLong()
    val cycle = work + rest
    val offset = ChronoUnit.DAYS.between(first, day)
    val index = Math.floorDiv(offset, cycle)
    val start = first.plusDays(index * cycle)
    val end = start.plusDays(work - 1)
    val next = start.plusDays(cycle)
    val working = !day.isAfter(end)
    val begin = if (working) start else end.plusDays(1)
    val finish = if (working) end else next.minusDays(1)
    val total = ChronoUnit.DAYS.between(begin, finish) + 1
    val elapsed = (ChronoUnit.DAYS.between(begin, day) + 1).coerceIn(0, total)
    return Period(start, end, next, working, elapsed, total, total - elapsed, elapsed.toFloat() / total)
}

private data class Payment(val date: String, val amount: Double, val type: String)
private fun loadPayments(raw: String): List<Payment> = runCatching {
    val a = JSONArray(raw)
    (0 until a.length()).map { i -> val o = a.getJSONObject(i); Payment(o.optString("date"), o.optDouble("amount"), o.optString("type")) }
}.getOrDefault(emptyList())
private fun encodePayments(items: List<Payment>): String = JSONArray().apply {
    items.forEach { put(JSONObject().put("date", it.date).put("amount", it.amount).put("type", it.type)) }
}.toString()

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("vahta_settings", Context.MODE_PRIVATE)
        setContent {
            var unlocked by remember { mutableStateOf(false) }
            var setupDone by remember { mutableStateOf(prefs.getBoolean("setup_complete_v2", false)) }
            var config by remember { mutableStateOf(readConfig(prefs)) }
            val owner = LocalLifecycleOwner.current
            var backgrounded by remember { mutableStateOf(false) }
            DisposableEffect(owner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> if (unlocked) backgrounded = true
                        Lifecycle.Event.ON_START -> if (backgrounded) { unlocked = false; backgrounded = false }
                        else -> Unit
                    }
                }
                owner.lifecycle.addObserver(observer)
                onDispose { owner.lifecycle.removeObserver(observer) }
            }
            VahtaTheme {
                if (!unlocked) SecurityScreen(onUnlocked = { unlocked = true })
                else if (!setupDone) Wizard(prefs) { result ->
                    config = result
                    saveConfig(prefs, result)
                    prefs.edit().putBoolean("setup_complete_v2", true).apply()
                    clearDraft(prefs)
                    setupDone = true
                }
                else Dashboard(config, onChange = { config = it; saveConfig(prefs, it) }, prefs = prefs,
                    onLock = { unlocked = false }, organizerContext = this@MainActivity)
            }
        }
    }
}

@Composable private fun VahtaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = mint, background = bg, surface = panel,
        onBackground = Color.White, onSurface = Color.White), content = content)
}

@Composable private fun Wizard(prefs: SharedPreferences, onFinish: (Config) -> Unit) {
    var s by remember { mutableStateOf(readConfig(prefs, "draft_")) }
    var step by remember { mutableIntStateOf(prefs.getInt("setup_step", 0).coerceIn(0, 6)) }
    fun update(value: Config) { s = value; saveConfig(prefs, value, "draft_") }
    fun move(next: Int) { step = next; prefs.edit().putInt("setup_step", next).apply() }
    val first = parseDate(s.arrival)
    val last = parseDate(s.departure)
    val datesOk = first != null && last != null && last.isAfter(first) && s.rotation in rotations
    val working = s.workDays.toIntOrNull()
    val resting = s.restDays.toIntOrNull()
    val canNext = when (step) {
        0 -> s.name.isNotBlank()
        1 -> datesOk
        2 -> num(s.hourly) > 0 && num(s.hours) in 0.01..24.0 && working != null && working in 1..366 && resting != null && resting in 0..366
        5 -> (s.advanceDay.isBlank() || s.advanceDay.toIntOrNull() in 1..31) &&
            (s.salaryDay.isBlank() || s.salaryDay.toIntOrNull() in 1..31) &&
            (s.advanceMethod != "Фиксированная сумма" || num(s.fixedAdvance) > 0)
        else -> true
    }
    Column(Modifier.fillMaxSize().background(bg).padding(18.dp)) {
        Text("ВАХТА", fontSize = 31.sp, color = Color.White, fontWeight = FontWeight.Black)
        Text("ПЕРВОНАЧАЛЬНАЯ НАСТРОЙКА", color = mint, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { (step + 1) / 7f }, modifier = Modifier.fillMaxWidth(), color = mint)
        Text("Шаг ${step + 1} из 7 · Ответы сохраняются автоматически", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                0 -> {
                    Heading("Знакомство", "Как к тебе обращаться?")
                    Field("Имя", s.name) { update(s.copy(name = it)) }
                }
                1 -> {
                    Heading("График вахты", "Выбери тип вахты, затем точные даты заезда и выезда.")
                    RotationSelect(s.rotation) { update(s.copy(rotation = it)) }
                    DateField("Дата заезда", s.arrival) { update(s.copy(arrival = it)) }
                    DateField("Дата выезда домой", s.departure) { update(s.copy(departure = it)) }
                    if (first != null && last != null && !last.isBefore(first)) {
                        val actual = ChronoUnit.DAYS.between(first, last) + 1
                        Text("Фактически: $actual календарных дней (31-е число учитывается)", color = mint)
                        if (s.rotation in rotations) {
                            Text("Формально: ${rotationWork(s.rotation)} дней. Межвахта: ${rotationRest(s.rotation)} дней.", color = muted)
                            Text("Следующий заезд: ${last.plusDays(rotationRest(s.rotation).toLong() + 1).pretty()}", color = mint)
                        }
                    }
                    Text("Следующие циклы повторяют фактическую продолжительность первой вахты и выбранную длительность межвахты.", color = muted, fontSize = 12.sp)
                }
                2 -> {
                    Heading("Рабочее время", "Укажи ЧТС, часы и чередование рабочих и выходных дней.")
                    Field("ЧТС, ₽/час", s.hourly, true) { update(s.copy(hourly = it)) }
                    Field("Часов в смене", s.hours, true) { update(s.copy(hours = it)) }
                    Field("Рабочих дней подряд", s.workDays, true) { update(s.copy(workDays = it)) }
                    Field("Выходных дней подряд (0, если нет)", s.restDays, true) { update(s.copy(restDays = it)) }
                    if (!canNext) Text("ЧТС и часы должны быть больше нуля. Рабочих дней — от 1 до 366, выходных — от 0 до 366.", color = gold, fontSize = 12.sp)
                }
                3 -> {
                    Heading("Надбавки и премии", "Неприменимые начисления оставь пустыми.")
                    Field("Районный коэффициент (например, 1,2)", s.coefficient, true) { update(s.copy(coefficient = it)) }
                    Field("Северная надбавка, %", s.north, true) { update(s.copy(north = it)) }
                    Field("Производственная премия, %", s.premium, true) { update(s.copy(premium = it)) }
                    Field("Вахтовая надбавка, ₽/день", s.allowance, true) { update(s.copy(allowance = it)) }
                    Field("Оплачиваемых часов дороги за вахту", s.travelHours, true) { update(s.copy(travelHours = it)) }
                }
                4 -> {
                    Heading("Межвахта и удержания", "Необязательные значения можно пропустить.")
                    Field("Оплачиваемых часов межвахты", s.intershiftHours, true) { update(s.copy(intershiftHours = it)) }
                    Field("Ставка межвахты, ₽/час", s.intershiftRate, true) { update(s.copy(intershiftRate = it)) }
                    Field("НДФЛ, %", s.tax, true) { update(s.copy(tax = it)) }
                    Field("Питание, ₽", s.meals, true) { update(s.copy(meals = it)) }
                    Field("Исполнительные удержания, ₽", s.enforcement, true) { update(s.copy(enforcement = it)) }
                    Field("Прочие удержания, ₽", s.otherDeductions, true) { update(s.copy(otherDeductions = it)) }
                }
                5 -> {
                    Heading("Выплаты", "Укажи даты и способ расчёта аванса.")
                    Field("Число аванса (необязательно)", s.advanceDay, true) { update(s.copy(advanceDay = it)) }
                    Field("Число зарплаты (необязательно)", s.salaryDay, true) { update(s.copy(salaryDay = it)) }
                    Text("Как рассчитывается аванс?", color = Color.White)
                    listOf("По отработанным часам", "Фиксированная сумма", "Другой порядок").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable { update(s.copy(advanceMethod = option)) }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = s.advanceMethod == option, onClick = { update(s.copy(advanceMethod = option)) })
                            Text(option, color = Color.White)
                        }
                    }
                    if (s.advanceMethod == "Фиксированная сумма") Field("Сумма аванса, ₽", s.fixedAdvance, true) { update(s.copy(fixedAdvance = it)) }
                    Field("Другие условия расчёта", s.notes) { update(s.copy(notes = it)) }
                }
                6 -> {
                    Heading("Проверка", "Все данные можно будет изменить в настройках.")
                    Panel("ТВОИ ДАННЫЕ") {
                        Info("Имя", s.name); Info("График", s.rotation)
                        Info("Заезд", first?.pretty() ?: "—"); Info("Выезд", last?.pretty() ?: "—")
                        Info("Фактическая вахта", if (first != null && last != null) "${ChronoUnit.DAYS.between(first, last) + 1} дней" else "—")
                        Info("ЧТС", "${s.hourly} ₽/час"); Info("Смена", "${s.hours} часов")
                        Info("Работа / отдых", "${s.workDays} / ${s.restDays} дней")
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { move(step - 1) }, modifier = Modifier.weight(1f)) { Text("Назад") }
            Button(onClick = { if (step < 6) move(step + 1) else onFinish(s) }, enabled = canNext, modifier = Modifier.weight(1f)) {
                Text(if (step == 6) "Начать работу" else "Продолжить")
            }
        }
    }
}

@Composable private fun RotationSelect(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Тип вахты: ${value.ifBlank { "Выбрать" }}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            rotations.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false }) }
        }
    }
}

@Composable private fun Dashboard(
    s: Config, onChange: (Config) -> Unit, prefs: SharedPreferences,
    onLock: () -> Unit, organizerContext: Context
) {
    var tab by remember { mutableIntStateOf(0) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { clock = System.currentTimeMillis(); delay(1000) } }
    val now = remember(clock / 1000) { LocalDateTime.ofInstant(Instant.ofEpochMilli(clock), ZoneId.systemDefault()) }
    val today = now.toLocalDate()
    val first = parseDate(s.arrival)
    val last = parseDate(s.departure)
    val valid = first != null && last != null && last.isAfter(first) && s.rotation in rotations
    val period = if (valid) periodFor(today, first!!, last!!, s.rotation) else null
    val hourly = num(s.hourly)
    val hours = num(s.hours)
    val workDays = (s.workDays.toIntOrNull() ?: 1).coerceIn(1, 366)
    val restDays = (s.restDays.toIntOrNull() ?: 0).coerceIn(0, 366)
    val workCycle = workDays + restDays
    val earnings = if (period == null) 0.0 else {
        val shiftLength = ChronoUnit.DAYS.between(period.start, period.end) + 1
        val completed = if (period.working) ChronoUnit.DAYS.between(period.start, today).coerceIn(0, shiftLength) else shiftLength
        val wholeWorkDays = (0 until completed).count { it % workCycle < workDays }
        val fraction = if (period.working && completed < shiftLength && completed % workCycle < workDays)
            now.toLocalTime().toSecondOfDay() / 86400.0 else 0.0
        val base = (wholeWorkDays + fraction) * hours * hourly
        val coefficient = num(s.coefficient).takeIf { it > 0 } ?: 1.0
        val shiftDays = completed + if (period.working) now.toLocalTime().toSecondOfDay() / 86400.0 else 0.0
        base * (coefficient + num(s.north) / 100 + num(s.premium) / 100) + shiftDays * num(s.allowance)
    }
    var payments by remember { mutableStateOf(loadPayments(prefs.getString("payments", "[]") ?: "[]")) }
    fun savePayments(items: List<Payment>) { payments = items; prefs.edit().putString("payments", encodePayments(items)).apply() }
    Column(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.weight(1f).fillMaxWidth().then(if (tab == 3) Modifier else Modifier.verticalScroll(rememberScrollState())).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("ВАХТА", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Привет, ${s.name}!", color = muted, fontSize = 17.sp)
            when (tab) {
                0 -> {
                    Panel("МОЙ ГРАФИК") {
                        if (period == null) Text("Настрой даты заезда и выезда", color = gold)
                        else {
                            Text(if (period.working) "НА ВАХТЕ" else "НА МЕЖВАХТЕ", color = if (period.working) mint else blue, fontWeight = FontWeight.Bold)
                            Text(if (period.working) "До выезда: ${period.remaining} дн." else "До заезда: ${period.remaining} дн.", color = Color.White, fontSize = 23.sp)
                            LinearProgressIndicator(progress = { period.progress }, modifier = Modifier.fillMaxWidth().height(10.dp), color = mint)
                            Info("Прогресс", "${(period.progress * 100).toInt()}% · ${period.total} дней")
                            Info("Тип графика", s.rotation)
                            Info("Заезд", period.start.pretty()); Info("Выезд 🎉", period.end.pretty()); Info("Следующий заезд", period.next.pretty())
                        }
                    }
                    Panel("ЖИВОЙ СЧЁТЧИК") {
                        Text(rub(earnings), color = mint, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(if (period?.working == true) "Обновляется каждую секунду" else "На межвахте счётчик остановлен", color = muted)
                        Text("Ориентировочные начисления по календарю, часы смены условно распределены по суткам. Не включает дорогу, межвахту, налоги и удержания.", color = muted, fontSize = 12.sp)
                    }
                    Panel("БЛИЖАЙШАЯ ВЫПЛАТА") {
                        val nearest = listOfNotNull(nextPayDate(today, s.advanceDay)?.let { "Аванс" to it },
                            nextPayDate(today, s.salaryDay)?.let { "Зарплата" to it }).minByOrNull { it.second }
                        Text(nearest?.let { "${it.first} · ${it.second.pretty()}" } ?: "Даты выплат не указаны", color = Color.White)
                        Text("Сумма к выплате не подтверждена расчётным листком.", color = muted, fontSize = 12.sp)
                    }
                    Button(onClick = { tab = 1 }) { Text("Открыть календарь") }
                }
                1 -> if (valid) ShiftCalendar(today, first!!, last!!, s.rotation) else Panel("КАЛЕНДАРЬ") { Text("Укажи график и даты в настройках", color = muted) }
                2 -> Finance(earnings, payments, ::savePayments)
                3 -> OrganizerScreen(organizerContext)
                4 -> Settings(s, onChange, onLock)
            }
        }
        NavigationBar(containerColor = panel) {
            listOf("Главная", "Календарь", "Финансы", "Мои дела", "Настройки").forEachIndexed { i, label ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i },
                    icon = { Text(listOf("⌂", "▦", "₽", "☑", "⚙")[i]) }, label = { Text(label, fontSize = 9.sp, maxLines = 1) })
            }
        }
    }
}

private fun nextPayDate(today: LocalDate, text: String): LocalDate? {
    val day = text.toIntOrNull()?.takeIf { it in 1..31 } ?: return null
    fun date(month: YearMonth) = month.atDay(day.coerceAtMost(month.lengthOfMonth()))
    val current = date(YearMonth.from(today))
    return if (current.isBefore(today)) date(YearMonth.from(today).plusMonths(1)) else current
}

@Composable private fun Finance(earnings: Double, payments: List<Payment>, onChange: (List<Payment>) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Зарплата") }
    Panel("РАСЧЁТНЫЕ НАЧИСЛЕНИЯ") { Text(rub(earnings), color = mint, fontSize = 26.sp); Text("Оценка, не сумма к получению", color = muted) }
    Panel("ДОБАВИТЬ ВЫПЛАТУ") {
        DateField("Дата получения", date) { date = it }
        Field("Получено, ₽", amount, true) { amount = it }
        listOf("Зарплата", "Аванс", "Межвахта", "Смешанная", "Отпускные", "Другое").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { type = option }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = type == option, onClick = { type = option }); Text(option, color = Color.White)
            }
        }
        Button(onClick = { onChange(payments + Payment(date, num(amount), type)); amount = "" },
            enabled = num(amount) > 0 && parseDate(date) != null, modifier = Modifier.fillMaxWidth()) { Text("Сохранить выплату") }
    }
    Panel("ИСТОРИЯ ВЫПЛАТ") {
        Text("Всего получено: ${rub(payments.sumOf { it.amount })}", color = mint)
        if (payments.isEmpty()) Text("Пока нет записей", color = muted)
        payments.sortedByDescending { it.date }.forEach { item ->
            HorizontalDivider(); Text("${item.date} · ${item.type}", color = muted)
            Text(rub(item.amount), color = mint, fontSize = 19.sp)
            TextButton(onClick = { val i = payments.indexOf(item); if (i >= 0) onChange(payments.toMutableList().also { it.removeAt(i) }) }) { Text("Удалить") }
        }
    }
}

@Composable private fun Settings(s: Config, onChange: (Config) -> Unit, onLock: () -> Unit) {
    Panel("ЛИЧНЫЕ ДАННЫЕ") { Field("Имя", s.name) { onChange(s.copy(name = it)) } }
    Panel("ГРАФИК ВАХТ") {
        RotationSelect(s.rotation) { onChange(s.copy(rotation = it)) }
        DateField("Дата заезда", s.arrival) { onChange(s.copy(arrival = it)) }
        DateField("Дата выезда", s.departure) { onChange(s.copy(departure = it)) }
        Field("Рабочих дней подряд", s.workDays, true) { onChange(s.copy(workDays = it)) }
        Field("Выходных дней подряд", s.restDays, true) { onChange(s.copy(restDays = it)) }
        val a = parseDate(s.arrival); val d = parseDate(s.departure)
        if (a != null && d != null && !d.isBefore(a)) Text("Фактическая вахта: ${ChronoUnit.DAYS.between(a, d) + 1} дней", color = mint)
    }
    Panel("ОПЛАТА ТРУДА") {
        Field("ЧТС, ₽/час", s.hourly, true) { onChange(s.copy(hourly = it)) }
        Field("Часов в смене", s.hours, true) { onChange(s.copy(hours = it)) }
        Field("Районный коэффициент", s.coefficient, true) { onChange(s.copy(coefficient = it)) }
        Field("Северная надбавка, %", s.north, true) { onChange(s.copy(north = it)) }
        Field("Производственная премия, %", s.premium, true) { onChange(s.copy(premium = it)) }
        Field("Вахтовая надбавка, ₽/день", s.allowance, true) { onChange(s.copy(allowance = it)) }
        Field("Оплачиваемых часов дороги", s.travelHours, true) { onChange(s.copy(travelHours = it)) }
        Field("Оплачиваемых часов межвахты", s.intershiftHours, true) { onChange(s.copy(intershiftHours = it)) }
        Field("Ставка межвахты, ₽/час", s.intershiftRate, true) { onChange(s.copy(intershiftRate = it)) }
    }
    Panel("УДЕРЖАНИЯ") {
        Field("НДФЛ, %", s.tax, true) { onChange(s.copy(tax = it)) }
        Field("Питание, ₽", s.meals, true) { onChange(s.copy(meals = it)) }
        Field("Исполнительные удержания, ₽", s.enforcement, true) { onChange(s.copy(enforcement = it)) }
        Field("Прочие удержания, ₽", s.otherDeductions, true) { onChange(s.copy(otherDeductions = it)) }
    }
    Panel("ВЫПЛАТЫ") {
        Field("Число аванса", s.advanceDay, true) { onChange(s.copy(advanceDay = it)) }
        Field("Число зарплаты", s.salaryDay, true) { onChange(s.copy(salaryDay = it)) }
        listOf("По отработанным часам", "Фиксированная сумма", "Другой порядок").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { onChange(s.copy(advanceMethod = option)) }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = s.advanceMethod == option, onClick = { onChange(s.copy(advanceMethod = option)) })
                Text(option, color = Color.White)
            }
        }
        if (s.advanceMethod == "Фиксированная сумма") Field("Сумма аванса, ₽", s.fixedAdvance, true) { onChange(s.copy(fixedAdvance = it)) }
        Field("Особенности начислений", s.notes) { onChange(s.copy(notes = it)) }
    }
    Panel("БЕЗОПАСНОСТЬ") { OutlinedButton(onClick = onLock, modifier = Modifier.fillMaxWidth()) { Text("Заблокировать приложение") } }
}

@Composable private fun ShiftCalendar(today: LocalDate, first: LocalDate, last: LocalDate, rotation: String) {
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    Panel("КАЛЕНДАРЬ ВАХТ") {
        MonthHeader(month, { month = month.minusMonths(1) }, { month = month.plusMonths(1) })
        WeekHeader()
        val offset = month.atDay(1).dayOfWeek.value - 1
        val count = month.lengthOfMonth()
        repeat((offset + count + 6) / 7) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { col ->
                    val day = row * 7 + col - offset + 1
                    if (day in 1..count) {
                        val date = month.atDay(day)
                        val p = periodFor(date, first, last, rotation)
                        val isArrival = date == p.start
                        val isDeparture = date == p.end
                        val cellColor = when { isDeparture -> gold; isArrival -> Color(0xFF278E7A); p.working -> Color(0xFF204F4A); else -> Color(0xFF233F65) }
                        Box(Modifier.weight(1f).height(49.dp).background(cellColor, RoundedCornerShape(9.dp))
                            .then(if (date == selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(9.dp)) else Modifier)
                            .clickable { selected = date }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$day", color = if (isDeparture) bg else Color.White, fontWeight = FontWeight.Bold)
                                Text(when { isDeparture -> "🎉"; isArrival -> "●"; date == today -> "•"; else -> " " }, fontSize = 10.sp, lineHeight = 11.sp, color = mint)
                            }
                        }
                    } else Spacer(Modifier.weight(1f).height(49.dp))
                }
            }
        }
        Info("Бирюзовый", "Вахта"); Info("Синий", "Межвахта"); Info("Золотой 🎉", "Выезд домой")
        HorizontalDivider()
        val p = periodFor(selected, first, last, rotation)
        Text(selected.pretty(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(when { selected == p.end -> "🎉 День выезда домой!"; selected == p.start -> "🚀 День заезда!"
            p.working -> "На вахте. До выезда: ${p.remaining} дн."; else -> "На межвахте. До заезда: ${p.remaining} дн." }, color = mint)
        OutlinedButton(onClick = { month = YearMonth.from(today); selected = today }, modifier = Modifier.fillMaxWidth()) { Text("Сегодня") }
    }
}

@Composable private fun MonthHeader(month: YearMonth, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = previous) { Text("‹", fontSize = 30.sp, color = mint) }
        Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", ru)).replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = next) { Text("›", fontSize = 30.sp, color = mint) }
    }
}

@Composable private fun WeekHeader() {
    Row(Modifier.fillMaxWidth()) {
        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = muted) }
    }
}

@Composable private fun DateField(label: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = parseDate(value)
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${selected?.pretty() ?: "Выбрать дату"}") }
    if (open) {
        var month by remember(open) { mutableStateOf(YearMonth.from(selected ?: LocalDate.now())) }
        AlertDialog(onDismissRequest = { open = false }, title = { Text(label) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                MonthHeader(month, { month = month.minusMonths(1) }, { month = month.plusMonths(1) })
                WeekHeader()
                val offset = month.atDay(1).dayOfWeek.value - 1
                val count = month.lengthOfMonth()
                repeat((offset + count + 6) / 7) { row ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val day = row * 7 + col - offset + 1
                            Box(Modifier.weight(1f).height(38.dp).then(if (day in 1..count) Modifier.clickable {
                                onChange(month.atDay(day).toString()); open = false
                            } else Modifier), contentAlignment = Alignment.Center) {
                                if (day in 1..count) Text("$day", color = if (month.atDay(day) == selected) mint else Color.White)
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text("Закрыть") } }, containerColor = panel)
    }
}

@Composable private fun Heading(title: String, description: String) {
    Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Text(description, color = muted, fontSize = 14.sp)
}

@Composable private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = muted, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp)
    }
}

@Composable private fun Field(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text), modifier = Modifier.fillMaxWidth())
}
