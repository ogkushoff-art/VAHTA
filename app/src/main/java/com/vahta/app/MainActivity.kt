
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val navy = Color(0xFF0B1728)
private val surface = Color(0xFF192D45)
private val mint = Color(0xFF56E4C0)
private val secondary = Color(0xFFB4C6D9)
private val blue = Color(0xFF4389D8)
private val gold = Color(0xFFFFB45B)
private val locale = Locale.forLanguageTag("ru-RU")

private fun LocalDate.display(): String =
    format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun parseDate(s: String): LocalDate? =
    runCatching { LocalDate.parse(s) }.getOrNull()

private fun decimal(s: String): Double =
    s.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

private fun rubles(value: Double): String =
    NumberFormat.getCurrencyInstance(locale).format(value)

private fun shiftedDate(
    date: LocalDate,
    months: Long
): LocalDate {
    val target = YearMonth.from(date).plusMonths(months)
    return target.atDay(
        date.dayOfMonth.coerceAtMost(target.lengthOfMonth())
    )
}

private data class ShiftPeriod(
    val start: LocalDate,
    val end: LocalDate,
    val nextStart: LocalDate,
    val atWork: Boolean,
    val elapsed: Long,
    val total: Long,
    val remaining: Long,
    val progress: Float
)

private fun periodFor(
    day: LocalDate,
    arrival: LocalDate,
    departure: LocalDate,
    interval: Int
): ShiftPeriod {
    val months = interval.coerceIn(1, 24).toLong()
    val difference = ChronoUnit.MONTHS.between(
        YearMonth.from(arrival),
        YearMonth.from(day)
    )

    var index = Math.floorDiv(difference, months)

    fun start(i: Long) = shiftedDate(arrival, i * months)
    fun end(i: Long) = shiftedDate(departure, i * months)

    while (day.isBefore(start(index))) index--
    while (!day.isBefore(start(index + 1))) index++

    val shiftStart = start(index)
    val shiftEnd = end(index)
    val next = start(index + 1)
    val working = !day.isAfter(shiftEnd)

    val beginning = if (working) shiftStart else shiftEnd.plusDays(1)
    val ending = if (working) shiftEnd else next.minusDays(1)

    val total = (
        ChronoUnit.DAYS.between(beginning, ending) + 1
    ).coerceAtLeast(1)

    val elapsed = (
        ChronoUnit.DAYS.between(beginning, day) + 1
    ).coerceIn(0, total)

    return ShiftPeriod(
        start = shiftStart,
        end = shiftEnd,
        nextStart = next,
        atWork = working,
        elapsed = elapsed,
        total = total,
        remaining = total - elapsed,
        progress = elapsed.toFloat() / total
    )
}

private data class Payment(
    val date: String,
    val amount: Double,
    val type: String
)

private fun readPayments(raw: String): List<Payment> =
    runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map {
            val item = array.getJSONObject(it)
            Payment(
                item.optString("date"),
                item.optDouble("amount"),
                item.optString("type")
            )
        }
    }.getOrDefault(emptyList())

private fun writePayments(items: List<Payment>): String {
    val array = JSONArray()
    items.forEach {
        array.put(
            JSONObject()
                .put("date", it.date)
                .put("amount", it.amount)
                .put("type", it.type)
        )
    }
    return array.toString()
}

private data class Settings(
    val name: String = "",
    val arrival: String = "",
    val departure: String = "",
    val interval: String = "",
    val hourly: String = "",
    val hours: String = "",
    val workDays: String = "",
    val restDays: String = "",
    val coefficient: String = "",
    val north: String = "",
    val premium: String = "",
    val shiftAllowance: String = "",
    val travelHours: String = "",
    val intershiftHours: String = "",
    val intershiftRate: String = "",
    val tax: String = "",
    val meals: String = "",
    val enforcement: String = "",
    val otherDeductions: String = "",
    val advanceDay: String = "",
    val salaryDay: String = "",
    val advanceMethod: String = "По отработанным часам",
    val fixedAdvance: String = "",
    val notes: String = ""
)

private fun readSettings(p: SharedPreferences): Settings =
    Settings(
        name = p.getString("name", "") ?: "",
        arrival = p.getString(
            "arrival_date",
            p.getString("start", "") ?: ""
        ) ?: "",
        departure = p.getString("departure_date", "") ?: "",
        interval = p.getString("interval_months", "") ?: "",
        hourly = p.getString("hourly", "") ?: "",
        hours = p.getString("hours", "") ?: "",
        workDays = p.getString("work_days", "") ?: "",
        restDays = p.getString("rest_days", "") ?: "",
        coefficient = p.getString("coefficient", "") ?: "",
        north = p.getString("north", "") ?: "",
        premium = p.getString("bonus", "") ?: "",
        shiftAllowance = p.getString("shift_allowance", "") ?: "",
        travelHours = p.getString("travel_hours", "") ?: "",
        intershiftHours = p.getString("intershift_hours", "") ?: "",
        intershiftRate = p.getString("intershift_rate", "") ?: "",
        tax = p.getString("tax", "") ?: "",
        meals = p.getString("meals", "") ?: "",
        enforcement = p.getString("enforcement", "") ?: "",
        otherDeductions = p.getString("other_deductions", "") ?: "",
        advanceDay = p.getString("advance", "") ?: "",
        salaryDay = p.getString("payday", "") ?: "",
        advanceMethod = p.getString(
            "advance_rule",
            "По отработанным часам"
        ) ?: "По отработанным часам",
        fixedAdvance = p.getString("fixed_advance", "") ?: "",
        notes = p.getString("pay_notes", "") ?: ""
    )

private fun saveSettings(
    p: SharedPreferences,
    s: Settings
) {
    p.edit()
        .putString("name", s.name)
        .putString("arrival_date", s.arrival)
        .putString("departure_date", s.departure)
        .putString("interval_months", s.interval)
        .putString("hourly", s.hourly)
        .putString("hours", s.hours)
        .putString("work_days", s.workDays)
        .putString("rest_days", s.restDays)
        .putString("coefficient", s.coefficient)
        .putString("north", s.north)
        .putString("bonus", s.premium)
        .putString("shift_allowance", s.shiftAllowance)
        .putString("travel_hours", s.travelHours)
        .putString("intershift_hours", s.intershiftHours)
        .putString("intershift_rate", s.intershiftRate)
        .putString("tax", s.tax)
        .putString("meals", s.meals)
        .putString("enforcement", s.enforcement)
        .putString("other_deductions", s.otherDeductions)
        .putString("advance", s.advanceDay)
        .putString("payday", s.salaryDay)
        .putString("advance_rule", s.advanceMethod)
        .putString("fixed_advance", s.fixedAdvance)
        .putString("pay_notes", s.notes)
        .apply()
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(
            "vahta_settings",
            Context.MODE_PRIVATE
        )

        setContent {
            var unlocked by remember { mutableStateOf(false) }
            var setupComplete by remember {
                mutableStateOf(
                    preferences.getBoolean("setup_complete", false)
                )
            }
            var settings by remember {
                mutableStateOf(readSettings(preferences))
            }

            val owner = LocalLifecycleOwner.current
            var leftForeground by remember { mutableStateOf(false) }

            DisposableEffect(owner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (unlocked) leftForeground = true
                        }
                        Lifecycle.Event.ON_START -> {
                            if (leftForeground) {
                                unlocked = false
                                leftForeground = false
                            }
                        }
                        else -> Unit
                    }
                }
                owner.lifecycle.addObserver(observer)
                onDispose {
                    owner.lifecycle.removeObserver(observer)
                }
            }

            AppTheme {
                if (!unlocked) {
                    SecurityScreen(
                        onUnlocked = { unlocked = true }
                    )
                } else if (!setupComplete) {
                    SetupWizard(
                        initial = settings,
                        onFinish = {
                            settings = it
                            saveSettings(preferences, it)
                            preferences.edit()
                                .putBoolean("setup_complete", true)
                                .apply()
                            setupComplete = true
                        }
                    )
                } else {
                    MainApp(
                        settings = settings,
                        onSettingsChange = {
                            settings = it
                            saveSettings(preferences, it)
                        },
                        preferences = preferences,
                        onLock = { unlocked = false },
                        organizerContext = this@MainActivity
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = mint,
            background = navy,
            surface = surface,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
private fun SetupWizard(
    initial: Settings,
    onFinish: (Settings) -> Unit
) {
    var s by remember { mutableStateOf(initial) }
    var step by remember { mutableIntStateOf(0) }

    val arrival = parseDate(s.arrival)
    val departure = parseDate(s.departure)
    val interval = s.interval.toIntOrNull()

    val scheduleValid =
        arrival != null &&
        departure != null &&
        interval != null &&
        interval in 1..24 &&
        departure.isAfter(arrival) &&
        departure.isBefore(shiftedDate(arrival, interval.toLong()))

    val valid = when (step) {
        0 -> s.name.trim().isNotBlank()
        1 -> scheduleValid
        2 -> decimal(s.hourly) > 0 &&
            decimal(s.hours) in 0.01..24.0 &&
            s.workDays.toIntOrNull() in 1..31 &&
            s.restDays.toIntOrNull() in 0..31
        3 -> true
        4 -> true
        5 -> s.advanceDay.toIntOrNull() in 1..31 &&
            s.salaryDay.toIntOrNull() in 1..31 &&
            (
                s.advanceMethod != "Фиксированная сумма" ||
                decimal(s.fixedAdvance) > 0
            )
        else -> true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(navy)
            .padding(18.dp)
    ) {
        Text(
            "ВАХТА",
            fontSize = 31.sp,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
        Text(
            "НАСТРОЙКА ПРИЛОЖЕНИЯ",
            color = mint,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { (step + 1) / 7f },
            modifier = Modifier.fillMaxWidth(),
            color = mint
        )

        Spacer(Modifier.height(10.dp))
        Text("Шаг ${step + 1} из 7", color = secondary)
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            when (step) {
                0 -> {
                    StepTitle(
                        "Давай знакомиться",
                        "Как к тебе обращаться?"
                    )
                    Field("Имя", s.name) {
                        s = s.copy(name = it)
                    }
                }

                1 -> {
                    StepTitle(
                        "График вахт",
                        "Выбери реальные даты заезда и выезда. Никакие даты не установлены за тебя."
                    )
                    DateField("Дата заезда", s.arrival) {
                        s = s.copy(arrival = it)
                    }
                    DateField("Дата выезда домой", s.departure) {
                        s = s.copy(departure = it)
                    }
                    Field(
                        "Интервал между заездами, месяцев",
                        s.interval,
                        true
                    ) {
                        s = s.copy(interval = it)
                    }

                    if (arrival != null && departure != null) {
                        val days = ChronoUnit.DAYS.between(
                            arrival, departure
                        ) + 1
                        Text(
                            if (days > 0)
                                "Продолжительность первой вахты: $days дней"
                            else
                                "Дата выезда должна быть позже даты заезда",
                            color = if (days > 0) mint else gold
                        )
                    }

                    Text(
                        "Например, при интервале 3 месяца следующий заезд рассчитывается через 3 календарных месяца. Можно выбрать другое значение.",
                        color = secondary,
                        fontSize = 12.sp
                    )
                }

                2 -> {
                    StepTitle(
                        "Рабочее время",
                        "Укажи свой режим работы и часовую ставку."
                    )
                    Field("ЧТС, ₽/час", s.hourly, true) {
                        s = s.copy(hourly = it)
                    }
                    Field("Часов в смене", s.hours, true) {
                        s = s.copy(hours = it)
                    }
                    Field(
                        "Рабочих дней подряд",
                        s.workDays,
                        true
                    ) {
                        s = s.copy(workDays = it)
                    }
                    Field(
                        "Выходных дней подряд",
                        s.restDays,
                        true
                    ) {
                        s = s.copy(restDays = it)
                    }
                    Text(
                        "Если выходных нет, укажи 0. Расчётный счётчик использует этот график.",
                        color = secondary,
                        fontSize = 12.sp
                    )
                }

                3 -> {
                    StepTitle(
                        "Надбавки и премии",
                        "Заполни только те начисления, которые применяются у тебя."
                    )
                    Field(
                        "Районный коэффициент, например 1,2",
                        s.coefficient,
                        true
                    ) {
                        s = s.copy(coefficient = it)
                    }
                    Field(
                        "Северная надбавка, %",
                        s.north,
                        true
                    ) {
                        s = s.copy(north = it)
                    }
                    Field(
                        "Производственная премия, %",
                        s.premium,
                        true
                    ) {
                        s = s.copy(premium = it)
                    }
                    Field(
                        "Вахтовая надбавка, ₽/день",
                        s.shiftAllowance,
                        true
                    ) {
                        s = s.copy(shiftAllowance = it)
                    }
                    Field(
                        "Оплачиваемых часов дороги за вахту",
                        s.travelHours,
                        true
                    ) {
                        s = s.copy(travelHours = it)
                    }
                }

                4 -> {
                    StepTitle(
                        "Межвахта и удержания",
                        "Необязательные параметры можно оставить пустыми."
                    )
                    Field(
                        "Оплачиваемых часов межвахты",
                        s.intershiftHours,
                        true
                    ) {
                        s = s.copy(intershiftHours = it)
                    }
                    Field(
                        "Ставка оплаты межвахты, ₽/час",
                        s.intershiftRate,
                        true
                    ) {
                        s = s.copy(intershiftRate = it)
                    }
                    Field("НДФЛ, %", s.tax, true) {
                        s = s.copy(tax = it)
                    }
                    Field(
                        "Удержание за питание, ₽",
                        s.meals,
                        true
                    ) {
                        s = s.copy(meals = it)
                    }
                    Field(
                        "Исполнительные удержания, ₽",
                        s.enforcement,
                        true
                    ) {
                        s = s.copy(enforcement = it)
                    }
                    Field(
                        "Другие удержания, ₽",
                        s.otherDeductions,
                        true
                    ) {
                        s = s.copy(otherDeductions = it)
                    }
                }

                5 -> {
                    StepTitle(
                        "Выплата зарплаты",
                        "Укажи свои даты аванса и окончательного расчёта."
                    )
                    Field(
                        "Число аванса",
                        s.advanceDay,
                        true
                    ) {
                        s = s.copy(advanceDay = it)
                    }
                    Field(
                        "Число зарплаты",
                        s.salaryDay,
                        true
                    ) {
                        s = s.copy(salaryDay = it)
                    }

                    Text(
                        "Как рассчитывается аванс?",
                        color = Color.White
                    )
                    listOf(
                        "По отработанным часам",
                        "Фиксированная сумма",
                        "Другой порядок"
                    ).forEach { method ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    s = s.copy(advanceMethod = method)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = s.advanceMethod == method,
                                onClick = {
                                    s = s.copy(advanceMethod = method)
                                }
                            )
                            Text(method, color = Color.White)
                        }
                    }

                    if (s.advanceMethod == "Фиксированная сумма") {
                        Field(
                            "Сумма аванса, ₽",
                            s.fixedAdvance,
                            true
                        ) {
                            s = s.copy(fixedAdvance = it)
                        }
                    }

                    Field(
                        "Другие особенности расчёта",
                        s.notes
                    ) {
                        s = s.copy(notes = it)
                    }
                }

                6 -> {
                    StepTitle(
                        "Всё готово",
                        "Проверь основные данные. Их всегда можно изменить."
                    )
                    Panel("ТВОИ НАСТРОЙКИ") {
                        Info("Имя", s.name)
                        Info("Заезд", arrival?.display() ?: "—")
                        Info("Выезд", departure?.display() ?: "—")
                        Info("Интервал", "${s.interval} мес.")
                        Info("ЧТС", "${s.hourly} ₽/час")
                        Info("Смена", "${s.hours} часов")
                        Info(
                            "Работа / отдых",
                            "${s.workDays} / ${s.restDays} дней"
                        )
                        Info("Аванс", "${s.advanceDay} числа")
                        Info("Зарплата", "${s.salaryDay} числа")
                    }
                    Text(
                        "Личные данные хранятся в настройках приложения. Незаполненные необязательные начисления не учитываются.",
                        color = secondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Назад")
                }
            }

            Button(
                onClick = {
                    if (step < 6) step++
                    else onFinish(s)
                },
                enabled = valid,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (step == 6) "Начать работу"
                    else "Продолжить"
                )
            }
        }
    }
}

@Composable
private fun MainApp(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    preferences: SharedPreferences,
    onLock: () -> Unit,
    organizerContext: Context
) {
    var tab by remember { mutableIntStateOf(0) }
    var tick by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1000)
        }
    }

    val now = remember(tick / 1000) {
        LocalDateTime.now()
    }
    val today = now.toLocalDate()

    val arrival = parseDate(settings.arrival)
    val departure = parseDate(settings.departure)
    val months = settings.interval.toIntOrNull()

    val valid =
        arrival != null &&
        departure != null &&
        months != null &&
        months in 1..24 &&
        departure.isAfter(arrival) &&
        departure.isBefore(
            shiftedDate(arrival, months.toLong())
        )

    val period = if (valid) {
        periodFor(today, arrival!!, departure!!, months!!)
    } else null

    val hourly = decimal(settings.hourly)
    val hours = decimal(settings.hours)
    val coefficient = decimal(settings.coefficient)
        .takeIf { it > 0 } ?: 1.0

    val workDays = (
        settings.workDays.toIntOrNull() ?: 1
    ).coerceIn(1, 31)
    val restDays = (
        settings.restDays.toIntOrNull() ?: 0
    ).coerceIn(0, 31)

    val north = decimal(settings.north) / 100.0
    val premium = decimal(settings.premium) / 100.0

    val earnings = if (period == null) {
        0.0
    } else {
        val daysInShift = (
            ChronoUnit.DAYS.between(
                period.start,
                period.end
            ) + 1
        ).coerceAtLeast(0)

        val completed = if (period.atWork) {
            ChronoUnit.DAYS.between(
                period.start,
                today
            ).coerceIn(0, daysInShift)
        } else {
            daysInShift
        }

        val cycle = workDays + restDays

        fun worked(dayIndex: Long): Boolean =
            (dayIndex % cycle) < workDays

        var workedDays = 0L
        for (i in 0 until completed) {
            if (worked(i)) workedDays++
        }

        val fraction = if (
            period.atWork &&
            worked(completed) &&
            completed < daysInShift
        ) {
            now.toLocalTime().toSecondOfDay() / 86400.0
        } else 0.0

        val estimatedHours =
            (workedDays + fraction) * hours

        val base = estimatedHours * hourly

        val shiftDaysElapsed =
            completed + if (period.atWork) fraction else 0.0

        base * (coefficient + north + premium) +
            shiftDaysElapsed * decimal(settings.shiftAllowance)
    }

    var payments by remember {
        mutableStateOf(
            readPayments(
                preferences.getString("payments", "[]") ?: "[]"
            )
        )
    }

    fun persistPayments(items: List<Payment>) {
        payments = items
        preferences.edit()
            .putString("payments", writePayments(items))
            .apply()
    }

    Column(
        Modifier.fillMaxSize().background(navy)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (tab == 3) Modifier
                    else Modifier.verticalScroll(rememberScrollState())
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "ВАХТА",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                if (settings.name.isBlank()) "Добро пожаловать"
                else "Привет, ${settings.name}!",
                color = secondary,
                fontSize = 17.sp
            )

            when (tab) {
                0 -> {
                    Panel("МОЙ ГРАФИК") {
                        if (period == null) {
                            Text(
                                "Настрой даты заезда и выезда",
                                color = gold
                            )
                            Button(onClick = { tab = 4 }) {
                                Text("Открыть настройки")
                            }
                        } else {
                            Text(
                                if (period.atWork) "НА ВАХТЕ"
                                else "НА МЕЖВАХТЕ",
                                color = if (period.atWork) mint else blue,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                if (period.atWork)
                                    "До выезда: ${period.remaining} дн."
                                else
                                    "До заезда: ${period.remaining} дн.",
                                color = Color.White,
                                fontSize = 23.sp
                            )

                            LinearProgressIndicator(
                                progress = { period.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp),
                                color = if (period.atWork) mint else blue,
                                trackColor = Color(0xFF354C65)
                            )

                            Info(
                                "Прогресс",
                                "${(period.progress * 100).toInt()}% из ${period.total} дней"
                            )
                            Info("Заезд", period.start.display())
                            Info("Выезд 🎉", period.end.display())
                            Info(
                                "Следующий заезд",
                                period.nextStart.display()
                            )
                        }
                    }

                    Panel("ЖИВОЙ СЧЁТЧИК") {
                        Text(
                            rubles(earnings),
                            color = mint,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (period?.atWork == true)
                                "Расчёт обновляется каждую секунду"
                            else
                                "На межвахте счётчик остановлен",
                            color = secondary
                        )
                        Text(
                            "Ориентировочное начисление по графику, а не подтверждённая сумма к выплате. Время смен распределено условно в пределах суток.",
                            color = secondary,
                            fontSize = 12.sp
                        )
                    }

                    Panel("БЛИЖАЙШАЯ ВЫПЛАТА") {
                        val options = listOfNotNull(
                            nextPayDate(
                                today,
                                settings.advanceDay
                            )?.let { "Аванс" to it },
                            nextPayDate(
                                today,
                                settings.salaryDay
                            )?.let { "Зарплата" to it }
                        )
                        val nearest = options.minByOrNull {
                            it.second
                        }
                        Text(
                            nearest?.let {
                                "${it.first} · ${it.second.display()}"
                            } ?: "Даты выплат не настроены",
                            color = Color.White,
                            fontSize = 19.sp
                        )
                        Text(
                            "Прогноз суммы требует подтверждения расчётного периода и порядка удержаний.",
                            color = secondary,
                            fontSize = 12.sp
                        )
                    }
                }

                1 -> {
                    if (valid) {
                        ShiftCalendar(
                            today = today,
                            arrival = arrival!!,
                            departure = departure!!,
                            interval = months!!
                        )
                    } else {
                        Panel("КАЛЕНДАРЬ") {
                            Text(
                                "Сначала настрой даты вахты",
                                color = secondary
                            )
                        }
                    }
                }

                2 -> FinanceScreen(
                    earnings = earnings,
                    payments = payments,
                    onPaymentsChange = ::persistPayments
                )

                3 -> OrganizerScreen(organizerContext)

                4 -> SettingsScreen(
                    s = settings,
                    onChange = onSettingsChange,
                    onLock = onLock
                )
            }
        }

        NavigationBar(containerColor = surface) {
            listOf(
                "Главная",
                "Календарь",
                "Финансы",
                "Мои дела",
                "Настройки"
            ).forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = tab == index,
                    onClick = { tab = index },
                    icon = {
                        Text(
                            listOf("⌂", "▦", "₽", "☑", "⚙")[index]
                        )
                    },
                    label = {
                        Text(
                            label,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }
    }
}

private fun nextPayDate(
    today: LocalDate,
    value: String
): LocalDate? {
    val day = value.toIntOrNull()
        ?.takeIf { it in 1..31 } ?: return null

    fun inMonth(month: YearMonth) =
        month.atDay(day.coerceAtMost(month.lengthOfMonth()))

    val current = inMonth(YearMonth.from(today))
    return if (current.isBefore(today)) {
        inMonth(YearMonth.from(today).plusMonths(1))
    } else current
}

@Composable
private fun FinanceScreen(
    earnings: Double,
    payments: List<Payment>,
    onPaymentsChange: (List<Payment>) -> Unit
) {
    var date by remember {
        mutableStateOf(LocalDate.now().toString())
    }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Зарплата") }

    Panel("РАСЧЁТНЫЕ НАЧИСЛЕНИЯ") {
        Text(
            rubles(earnings),
            color = mint,
            fontSize = 25.sp
        )
        Text(
            "Предварительная оценка, не сумма к получению",
            color = secondary,
            fontSize = 12.sp
        )
    }

    Panel("ДОБАВИТЬ ВЫПЛАТУ") {
        DateField("Дата получения", date) {
            date = it
        }
        Field("Получено, ₽", amount, true) {
            amount = it
        }

        listOf(
            "Зарплата",
            "Аванс",
            "Межвахта",
            "Смешанная",
            "Отпускные",
            "Другое"
        ).forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { type = option },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = type == option,
                    onClick = { type = option }
                )
                Text(option, color = Color.White)
            }
        }

        Button(
            onClick = {
                onPaymentsChange(
                    payments + Payment(
                        date,
                        decimal(amount),
                        type
                    )
                )
                amount = ""
            },
            enabled = decimal(amount) > 0 &&
                parseDate(date) != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить выплату")
        }
    }

    Panel("ИСТОРИЯ ВЫПЛАТ") {
        Text(
            "Всего получено: ${
                rubles(payments.sumOf { it.amount })
            }",
            color = mint
        )

        if (payments.isEmpty()) {
            Text("Записей пока нет", color = secondary)
        }

        payments.sortedByDescending { it.date }
            .forEach { item ->
                HorizontalDivider()
                Text(
                    "${item.date} · ${item.type}",
                    color = secondary
                )
                Text(
                    rubles(item.amount),
                    color = mint,
                    fontSize = 19.sp
                )
                TextButton(
                    onClick = {
                        val index = payments.indexOf(item)
                        if (index >= 0) {
                            onPaymentsChange(
                                payments.toMutableList().also {
                                    it.removeAt(index)
                                }
                            )
                        }
                    }
                ) {
                    Text("Удалить")
                }
            }
    }
}

@Composable
private fun SettingsScreen(
    s: Settings,
    onChange: (Settings) -> Unit,
    onLock: () -> Unit
) {
    Panel("ЛИЧНЫЕ ДАННЫЕ") {
        Field("Имя", s.name) {
            onChange(s.copy(name = it))
        }
    }

    Panel("ГРАФИК ВАХТ") {
        DateField("Дата заезда", s.arrival) {
            onChange(s.copy(arrival = it))
        }
        DateField("Дата выезда", s.departure) {
            onChange(s.copy(departure = it))
        }
        Field(
            "Интервал между заездами, месяцев",
            s.interval,
            true
        ) {
            onChange(s.copy(interval = it))
        }
        Field("Рабочих дней подряд", s.workDays, true) {
            onChange(s.copy(workDays = it))
        }
        Field("Выходных дней подряд", s.restDays, true) {
            onChange(s.copy(restDays = it))
        }
    }

    Panel("ОПЛАТА ТРУДА") {
        Field("ЧТС, ₽/час", s.hourly, true) {
            onChange(s.copy(hourly = it))
        }
        Field("Часов в смене", s.hours, true) {
            onChange(s.copy(hours = it))
        }
        Field("Районный коэффициент", s.coefficient, true) {
            onChange(s.copy(coefficient = it))
        }
        Field("Северная надбавка, %", s.north, true) {
            onChange(s.copy(north = it))
        }
        Field("Производственная премия, %", s.premium, true) {
            onChange(s.copy(premium = it))
        }
        Field(
            "Вахтовая надбавка, ₽/день",
            s.shiftAllowance,
            true
        ) {
            onChange(s.copy(shiftAllowance = it))
        }
        Field("Оплачиваемых часов дороги", s.travelHours, true) {
            onChange(s.copy(travelHours = it))
        }
        Field(
            "Оплачиваемых часов межвахты",
            s.intershiftHours,
            true
        ) {
            onChange(s.copy(intershiftHours = it))
        }
        Field(
            "Ставка межвахты, ₽/час",
            s.intershiftRate,
            true
        ) {
            onChange(s.copy(intershiftRate = it))
        }
    }

    Panel("УДЕРЖАНИЯ") {
        Field("НДФЛ, %", s.tax, true) {
            onChange(s.copy(tax = it))
        }
        Field("Питание, ₽", s.meals, true) {
            onChange(s.copy(meals = it))
        }
        Field("Исполнительные удержания, ₽", s.enforcement, true) {
            onChange(s.copy(enforcement = it))
        }
        Field(
            "Прочие удержания, ₽",
            s.otherDeductions,
            true
        ) {
            onChange(s.copy(otherDeductions = it))
        }
    }

    Panel("ДАТЫ ВЫПЛАТ") {
        Field("Число аванса", s.advanceDay, true) {
            onChange(s.copy(advanceDay = it))
        }
        Field("Число зарплаты", s.salaryDay, true) {
            onChange(s.copy(salaryDay = it))
        }

        listOf(
            "По отработанным часам",
            "Фиксированная сумма",
            "Другой порядок"
        ).forEach { method ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onChange(s.copy(advanceMethod = method))
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = s.advanceMethod == method,
                    onClick = {
                        onChange(s.copy(advanceMethod = method))
                    }
                )
                Text(method, color = Color.White)
            }
        }

        if (s.advanceMethod == "Фиксированная сумма") {
            Field("Сумма аванса, ₽", s.fixedAdvance, true) {
                onChange(s.copy(fixedAdvance = it))
            }
        }

        Field("Особенности начислений", s.notes) {
            onChange(s.copy(notes = it))
        }
    }

    Panel("БЕЗОПАСНОСТЬ") {
        OutlinedButton(
            onClick = onLock,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Заблокировать приложение")
        }
    }
}

@Composable
private fun ShiftCalendar(
    today: LocalDate,
    arrival: LocalDate,
    departure: LocalDate,
    interval: Int
) {
    var month by remember {
        mutableStateOf(YearMonth.from(today))
    }
    var selected by remember {
        mutableStateOf(today)
    }

    Panel("КАЛЕНДАРЬ ВАХТ") {
        MonthHeader(
            month = month,
            onPrevious = { month = month.minusMonths(1) },
            onNext = { month = month.plusMonths(1) }
        )

        WeekHeader()

        val offset = month.atDay(1).dayOfWeek.value - 1
        val count = month.lengthOfMonth()
        val rows = (offset + count + 6) / 7

        repeat(rows) { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(7) { col ->
                    val day = row * 7 + col - offset + 1

                    if (day in 1..count) {
                        val date = month.atDay(day)
                        val p = periodFor(
                            date, arrival, departure, interval
                        )

                        val isArrival = date == p.start
                        val isDeparture = date == p.end

                        val cellColor = when {
                            isDeparture -> gold
                            isArrival -> Color(0xFF278E7A)
                            p.atWork -> Color(0xFF204F4A)
                            else -> Color(0xFF233F65)
                        }

                        Box(
                            Modifier
                                .weight(1f)
                                .height(49.dp)
                                .background(
                                    cellColor,
                                    RoundedCornerShape(9.dp)
                                )
                                .then(
                                    if (date == selected)
                                        Modifier.border(
                                            2.dp,
                                            Color.White,
                                            RoundedCornerShape(9.dp)
                                        )
                                    else Modifier
                                )
                                .clickable { selected = date },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$day",
                                    color = if (isDeparture)
                                        navy else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    when {
                                        isDeparture -> "🎉"
                                        isArrival -> "●"
                                        date == today -> "•"
                                        else -> " "
                                    },
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    color = mint
                                )
                            }
                        }
                    } else {
                        Spacer(
                            Modifier.weight(1f).height(49.dp)
                        )
                    }
                }
            }
        }

        Info("Бирюзовый", "Вахта")
        Info("Синий", "Межвахта")
        Info("Золотой 🎉", "Выезд домой")

        HorizontalDivider()

        val p = periodFor(
            selected, arrival, departure, interval
        )

        Text(
            selected.display(),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            when {
                selected == p.end -> "🎉 День выезда домой!"
                selected == p.start -> "🚀 День заезда!"
                p.atWork ->
                    "На вахте. До выезда: ${p.remaining} дн."
                else ->
                    "На межвахте. До заезда: ${p.remaining} дн."
            },
            color = if (selected == p.end) gold else mint
        )

        OutlinedButton(
            onClick = {
                month = YearMonth.from(today)
                selected = today
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сегодня")
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Text("‹", fontSize = 30.sp, color = mint)
        }
        Text(
            month.format(
                DateTimeFormatter.ofPattern("LLLL yyyy", locale)
            ).replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Text("›", fontSize = 30.sp, color = mint)
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(Modifier.fillMaxWidth()) {
        listOf(
            "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"
        ).forEach {
            Text(
                it,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = secondary
            )
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val selected = parseDate(value)

    OutlinedButton(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "$label: ${selected?.display() ?: "Выбрать дату"}"
        )
    }

    if (open) {
        var month by remember(open) {
            mutableStateOf(
                YearMonth.from(selected ?: LocalDate.now())
            )
        }

        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MonthHeader(
                        month,
                        onPrevious = {
                            month = month.minusMonths(1)
                        },
                        onNext = {
                            month = month.plusMonths(1)
                        }
                    )

                    WeekHeader()

                    val offset =
                        month.atDay(1).dayOfWeek.value - 1
                    val count = month.lengthOfMonth()
                    val rows = (offset + count + 6) / 7

                    repeat(rows) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val day =
                                    row * 7 + col - offset + 1

                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .then(
                                            if (day in 1..count)
                                                Modifier.clickable {
                                                    onChange(
                                                        month.atDay(day)
                                                            .toString()
                                                    )
                                                    open = false
                                                }
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day in 1..count) {
                                        Text(
                                            "$day",
                                            color =
                                                if (
                                                    month.atDay(day) ==
                                                    selected
                                                ) mint
                                                else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text("Закрыть")
                }
            },
            containerColor = surface
        )
    }
}

@Composable
private fun StepTitle(
    title: String,
    description: String
) {
    Text(
        title,
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        description,
        color = secondary,
        fontSize = 14.sp
    )
}

@Composable
private fun Panel(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = surface
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                color = secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun Info(
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = secondary,
            fontSize = 13.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric)
                KeyboardType.Decimal
            else KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
