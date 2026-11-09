
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

private val navy = Color(0xFF0B1728)
private val surface = Color(0xFF192D45)
private val mint = Color(0xFF56E4C0)
private val secondary = Color(0xFFB4C6D9)
private val blue = Color(0xFF4389D8)
private val gold = Color(0xFFFFB45B)
private val locale = Locale.forLanguageTag("ru-RU")

private fun LocalDate.display(): String =
    format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()

private fun decimal(value: String): Double =
    value.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(locale).format(value)

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

/*
 * Расчёт без отдельного интервала.
 * Расстояние между заездами = 2 * расстояние
 * между первым заездом и первым выездом.
 *
 * 25.10 -> 10.12 -> следующий заезд 25.01.
 */
private fun periodFor(
    day: LocalDate,
    firstArrival: LocalDate,
    firstDeparture: LocalDate
): ShiftPeriod {
    val shiftDistance = ChronoUnit.DAYS.between(
        firstArrival,
        firstDeparture
    ).coerceAtLeast(1)

    val cycleDays = shiftDistance * 2
    val difference = ChronoUnit.DAYS.between(
        firstArrival,
        day
    )

    val index = Math.floorDiv(difference, cycleDays)

    val start = firstArrival.plusDays(index * cycleDays)
    val end = firstDeparture.plusDays(index * cycleDays)
    val next = start.plusDays(cycleDays)

    val working = !day.isAfter(end)

    val beginning = if (working) start else end.plusDays(1)
    val ending = if (working) end else next.minusDays(1)

    val total = (
        ChronoUnit.DAYS.between(beginning, ending) + 1
    ).coerceAtLeast(1)

    val elapsed = (
        ChronoUnit.DAYS.between(beginning, day) + 1
    ).coerceIn(0, total)

    return ShiftPeriod(
        start = start,
        end = end,
        nextStart = next,
        atWork = working,
        elapsed = elapsed,
        total = total,
        remaining = total - elapsed,
        progress = elapsed.toFloat() / total.toFloat()
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
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Payment(
                date = item.optString("date"),
                amount = item.optDouble("amount"),
                type = item.optString("type")
            )
        }
    }.getOrDefault(emptyList())

private fun writePayments(items: List<Payment>): String {
    val array = JSONArray()
    items.forEach { payment ->
        array.put(
            JSONObject()
                .put("date", payment.date)
                .put("amount", payment.amount)
                .put("type", payment.type)
        )
    }
    return array.toString()
}

private data class Settings(
    val name: String = "",
    val arrival: String = "",
    val departure: String = "",
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

private fun readSettings(
    preferences: SharedPreferences
): Settings = Settings(
    name = preferences.getString("name", "") ?: "",
    arrival = preferences.getString(
        "arrival_date",
        preferences.getString("start", "") ?: ""
    ) ?: "",
    departure = preferences.getString("departure_date", "") ?: "",
    hourly = preferences.getString("hourly", "") ?: "",
    hours = preferences.getString("hours", "") ?: "",
    workDays = preferences.getString("work_days", "") ?: "",
    restDays = preferences.getString("rest_days", "") ?: "",
    coefficient = preferences.getString("coefficient", "") ?: "",
    north = preferences.getString("north", "") ?: "",
    premium = preferences.getString("bonus", "") ?: "",
    shiftAllowance = preferences.getString(
        "shift_allowance", ""
    ) ?: "",
    travelHours = preferences.getString("travel_hours", "") ?: "",
    intershiftHours = preferences.getString(
        "intershift_hours", ""
    ) ?: "",
    intershiftRate = preferences.getString(
        "intershift_rate", ""
    ) ?: "",
    tax = preferences.getString("tax", "") ?: "",
    meals = preferences.getString("meals", "") ?: "",
    enforcement = preferences.getString("enforcement", "") ?: "",
    otherDeductions = preferences.getString(
        "other_deductions", ""
    ) ?: "",
    advanceDay = preferences.getString("advance", "") ?: "",
    salaryDay = preferences.getString("payday", "") ?: "",
    advanceMethod = preferences.getString(
        "advance_rule",
        "По отработанным часам"
    ) ?: "По отработанным часам",
    fixedAdvance = preferences.getString(
        "fixed_advance", ""
    ) ?: "",
    notes = preferences.getString("pay_notes", "") ?: ""
)

private fun saveSettings(
    preferences: SharedPreferences,
    settings: Settings
) {
    preferences.edit()
        .putString("name", settings.name)
        .putString("arrival_date", settings.arrival)
        .putString("departure_date", settings.departure)
        .putString("hourly", settings.hourly)
        .putString("hours", settings.hours)
        .putString("work_days", settings.workDays)
        .putString("rest_days", settings.restDays)
        .putString("coefficient", settings.coefficient)
        .putString("north", settings.north)
        .putString("bonus", settings.premium)
        .putString("shift_allowance", settings.shiftAllowance)
        .putString("travel_hours", settings.travelHours)
        .putString("intershift_hours", settings.intershiftHours)
        .putString("intershift_rate", settings.intershiftRate)
        .putString("tax", settings.tax)
        .putString("meals", settings.meals)
        .putString("enforcement", settings.enforcement)
        .putString("other_deductions", settings.otherDeductions)
        .putString("advance", settings.advanceDay)
        .putString("payday", settings.salaryDay)
        .putString("advance_rule", settings.advanceMethod)
        .putString("fixed_advance", settings.fixedAdvance)
        .putString("pay_notes", settings.notes)
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
            var unlocked by remember {
                mutableStateOf(false)
            }

            var setupComplete by remember {
                mutableStateOf(
                    preferences.getBoolean(
                        "setup_complete",
                        false
                    )
                )
            }

            var settings by remember {
                mutableStateOf(readSettings(preferences))
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            var wentToBackground by remember {
                mutableStateOf(false)
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (unlocked) {
                                wentToBackground = true
                            }
                        }

                        Lifecycle.Event.ON_START -> {
                            if (wentToBackground) {
                                unlocked = false
                                wentToBackground = false
                            }
                        }

                        else -> Unit
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            AppTheme {
                when {
                    !unlocked -> {
                        SecurityScreen(
                            onUnlocked = {
                                unlocked = true
                            }
                        )
                    }

                    !setupComplete -> {
                        SetupWizard(
                            initial = settings,
                            onFinish = { result ->
                                settings = result
                                saveSettings(preferences, result)

                                preferences.edit()
                                    .putBoolean(
                                        "setup_complete",
                                        true
                                    )
                                    .apply()

                                setupComplete = true
                            }
                        )
                    }

                    else -> {
                        MainApp(
                            settings = settings,
                            onSettingsChange = { result ->
                                settings = result
                                saveSettings(preferences, result)
                            },
                            preferences = preferences,
                            onLock = {
                                unlocked = false
                            },
                            organizerContext = this@MainActivity
                        )
                    }
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
    var settings by remember {
        mutableStateOf(initial)
    }

    var step by remember {
        mutableIntStateOf(0)
    }

    val arrival = parseDate(settings.arrival)
    val departure = parseDate(settings.departure)

    val scheduleValid =
        arrival != null &&
        departure != null &&
        departure.isAfter(arrival)

    val workDays = settings.workDays.toIntOrNull()
    val restDays = settings.restDays.toIntOrNull()

    val stepValid = when (step) {
        0 -> settings.name.trim().isNotEmpty()

        1 -> scheduleValid

        2 -> {
            decimal(settings.hourly) > 0.0 &&
                decimal(settings.hours) > 0.0 &&
                decimal(settings.hours) <= 24.0 &&
                workDays != null &&
                workDays in 1..366 &&
                restDays != null &&
                restDays in 0..366
        }

        3 -> true
        4 -> true

        5 -> {
            settings.advanceDay.toIntOrNull() in 1..31 &&
                settings.salaryDay.toIntOrNull() in 1..31 &&
                (
                    settings.advanceMethod != "Фиксированная сумма" ||
                        decimal(settings.fixedAdvance) > 0.0
                )
        }

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

        Text(
            "Шаг ${step + 1} из 7",
            color = secondary
        )

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

                    Field("Имя", settings.name) {
                        settings = settings.copy(name = it)
                    }
                }

                1 -> {
                    StepTitle(
                        "График вахт",
                        "Выбери дату заезда и дату выезда домой."
                    )

                    DateField(
                        "Дата заезда",
                        settings.arrival
                    ) {
                        settings = settings.copy(arrival = it)
                    }

                    DateField(
                        "Дата выезда домой",
                        settings.departure
                    ) {
                        settings = settings.copy(departure = it)
                    }

                    if (arrival != null && departure != null) {
                        val days = ChronoUnit.DAYS.between(
                            arrival,
                            departure
                        ) + 1

                        Text(
                            if (days > 0)
                                "Продолжительность вахты: $days дней"
                            else
                                "Выезд должен быть позже заезда",
                            color = if (days > 0) mint else gold
                        )

                        if (days > 1) {
                            val next = departure.plusDays(days - 1)

                            Text(
                                "Расчётный следующий заезд: ${next.display()}",
                                color = mint
                            )
                        }
                    }

                    Text(
                        "Следующие заезды рассчитываются автоматически. Для прогноза используется чередование вахты и межвахты сопоставимой продолжительности.",
                        color = secondary,
                        fontSize = 12.sp
                    )
                }

                2 -> {
                    StepTitle(
                        "Рабочее время",
                        "Укажи свой режим работы и часовую ставку."
                    )

                    Field(
                        "ЧТС, ₽/час",
                        settings.hourly,
                        true
                    ) {
                        settings = settings.copy(hourly = it)
                    }

                    Field(
                        "Часов в смене",
                        settings.hours,
                        true
                    ) {
                        settings = settings.copy(hours = it)
                    }

                    Field(
                        "Рабочих дней подряд",
                        settings.workDays,
                        true
                    ) {
                        settings = settings.copy(workDays = it)
                    }

                    Field(
                        "Выходных дней подряд",
                        settings.restDays,
                        true
                    ) {
                        settings = settings.copy(restDays = it)
                    }

                    Text(
                        "Можно указать 46, 60 и более рабочих дней подряд. Если выходных нет, введи 0.",
                        color = secondary,
                        fontSize = 12.sp
                    )

                    if (!stepValid) {
                        Text(
                            "Проверь ЧТС, часы смены и режим работы. Рабочие дни: от 1 до 366, выходные: от 0 до 366.",
                            color = gold,
                            fontSize = 12.sp
                        )
                    }
                }

                3 -> {
                    StepTitle(
                        "Надбавки и премии",
                        "Укажи только применяемые к тебе начисления."
                    )

                    Field(
                        "Районный коэффициент, например 1,2",
                        settings.coefficient,
                        true
                    ) {
                        settings = settings.copy(coefficient = it)
                    }

                    Field(
                        "Северная надбавка, %",
                        settings.north,
                        true
                    ) {
                        settings = settings.copy(north = it)
                    }

                    Field(
                        "Производственная премия, %",
                        settings.premium,
                        true
                    ) {
                        settings = settings.copy(premium = it)
                    }

                    Field(
                        "Вахтовая надбавка, ₽/день",
                        settings.shiftAllowance,
                        true
                    ) {
                        settings = settings.copy(
                            shiftAllowance = it
                        )
                    }

                    Field(
                        "Оплачиваемых часов дороги за вахту",
                        settings.travelHours,
                        true
                    ) {
                        settings = settings.copy(travelHours = it)
                    }
                }

                4 -> {
                    StepTitle(
                        "Межвахта и удержания",
                        "Необязательные параметры можно пропустить."
                    )

                    Field(
                        "Оплачиваемых часов межвахты",
                        settings.intershiftHours,
                        true
                    ) {
                        settings = settings.copy(
                            intershiftHours = it
                        )
                    }

                    Field(
                        "Ставка оплаты межвахты, ₽/час",
                        settings.intershiftRate,
                        true
                    ) {
                        settings = settings.copy(
                            intershiftRate = it
                        )
                    }

                    Field("НДФЛ, %", settings.tax, true) {
                        settings = settings.copy(tax = it)
                    }

                    Field(
                        "Удержание за питание, ₽",
                        settings.meals,
                        true
                    ) {
                        settings = settings.copy(meals = it)
                    }

                    Field(
                        "Исполнительные удержания, ₽",
                        settings.enforcement,
                        true
                    ) {
                        settings = settings.copy(
                            enforcement = it
                        )
                    }

                    Field(
                        "Другие удержания, ₽",
                        settings.otherDeductions,
                        true
                    ) {
                        settings = settings.copy(
                            otherDeductions = it
                        )
                    }
                }

                5 -> {
                    StepTitle(
                        "Выплата зарплаты",
                        "Укажи даты аванса и зарплаты."
                    )

                    Field(
                        "Число аванса",
                        settings.advanceDay,
                        true
                    ) {
                        settings = settings.copy(
                            advanceDay = it
                        )
                    }

                    Field(
                        "Число зарплаты",
                        settings.salaryDay,
                        true
                    ) {
                        settings = settings.copy(
                            salaryDay = it
                        )
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
                                    settings = settings.copy(
                                        advanceMethod = method
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    settings.advanceMethod == method,
                                onClick = {
                                    settings = settings.copy(
                                        advanceMethod = method
                                    )
                                }
                            )

                            Text(
                                method,
                                color = Color.White
                            )
                        }
                    }

                    if (
                        settings.advanceMethod ==
                        "Фиксированная сумма"
                    ) {
                        Field(
                            "Сумма аванса, ₽",
                            settings.fixedAdvance,
                            true
                        ) {
                            settings = settings.copy(
                                fixedAdvance = it
                            )
                        }
                    }

                    Field(
                        "Другие особенности расчёта",
                        settings.notes
                    ) {
                        settings = settings.copy(notes = it)
                    }
                }

                6 -> {
                    StepTitle(
                        "Всё готово",
                        "Проверь введённые данные."
                    )

                    Panel("ТВОИ НАСТРОЙКИ") {
                        Info("Имя", settings.name)
                        Info(
                            "Заезд",
                            arrival?.display() ?: "—"
                        )
                        Info(
                            "Выезд",
                            departure?.display() ?: "—"
                        )
                        Info(
                            "ЧТС",
                            "${settings.hourly} ₽/час"
                        )
                        Info(
                            "Смена",
                            "${settings.hours} часов"
                        )
                        Info(
                            "Работа / отдых",
                            "${settings.workDays} / ${settings.restDays} дней"
                        )
                        Info(
                            "Аванс",
                            "${settings.advanceDay} числа"
                        )
                        Info(
                            "Зарплата",
                            "${settings.salaryDay} числа"
                        )
                    }

                    Text(
                        "После завершения все параметры можно изменить в настройках.",
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
                    if (step < 6) {
                        step++
                    } else {
                        onFinish(settings)
                    }
                },
                enabled = stepValid,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (step == 6)
                        "Начать работу"
                    else
                        "Продолжить"
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
    var tab by remember {
        mutableIntStateOf(0)
    }

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

    val valid =
        arrival != null &&
        departure != null &&
        departure.isAfter(arrival)

    val period = if (valid) {
        periodFor(
            today,
            arrival!!,
            departure!!
        )
    } else {
        null
    }

    val hourly = decimal(settings.hourly)
    val hours = decimal(settings.hours)

    val coefficient = decimal(settings.coefficient)
        .takeIf { it > 0.0 } ?: 1.0

    val workDays = (
        settings.workDays.toIntOrNull() ?: 1
    ).coerceIn(1, 366)

    val restDays = (
        settings.restDays.toIntOrNull() ?: 0
    ).coerceIn(0, 366)

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

        val workCycle = workDays + restDays

        fun isWorkingDay(index: Long): Boolean =
            (index % workCycle) < workDays

        var completedWorkDays = 0L

        for (index in 0 until completed) {
            if (isWorkingDay(index)) {
                completedWorkDays++
            }
        }

        val dayFraction = if (
            period.atWork &&
            completed < daysInShift &&
            isWorkingDay(completed)
        ) {
            now.toLocalTime().toSecondOfDay() / 86400.0
        } else {
            0.0
        }

        val estimatedHours =
            (completedWorkDays + dayFraction) * hours

        val base = estimatedHours * hourly

        val elapsedShiftDays =
            completed +
                if (period.atWork) dayFraction else 0.0

        base * (coefficient + north + premium) +
            elapsedShiftDays *
                decimal(settings.shiftAllowance)
    }

    var payments by remember {
        mutableStateOf(
            readPayments(
                preferences.getString(
                    "payments",
                    "[]"
                ) ?: "[]"
            )
        )
    }

    fun persistPayments(items: List<Payment>) {
        payments = items

        preferences.edit()
            .putString(
                "payments",
                writePayments(items)
            )
            .apply()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(navy)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (tab == 3) {
                        Modifier
                    } else {
                        Modifier.verticalScroll(
                            rememberScrollState()
                        )
                    }
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
                if (settings.name.isBlank())
                    "Добро пожаловать"
                else
                    "Привет, ${settings.name}!",
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

                            Button(
                                onClick = { tab = 4 }
                            ) {
                                Text("Открыть настройки")
                            }
                        } else {
                            Text(
                                if (period.atWork)
                                    "НА ВАХТЕ"
                                else
                                    "НА МЕЖВАХТЕ",
                                color = if (period.atWork)
                                    mint
                                else
                                    blue,
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
                                progress = {
                                    period.progress
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp),
                                color = if (period.atWork)
                                    mint
                                else
                                    blue,
                                trackColor = Color(0xFF354C65)
                            )

                            Info(
                                "Прогресс",
                                "${(period.progress * 100).toInt()}% из ${period.total} дней"
                            )

                            Info(
                                "Заезд",
                                period.start.display()
                            )

                            Info(
                                "Выезд 🎉",
                                period.end.display()
                            )

                            Info(
                                "Следующий заезд",
                                period.nextStart.display()
                            )
                        }
                    }

                    Panel("ЖИВОЙ СЧЁТЧИК") {
                        Text(
                            money(earnings),
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
                            "Предварительное начисление по заданному режиму. Часы внутри дня распределены условно. Это не подтверждённая сумма к выплате.",
                            color = secondary,
                            fontSize = 12.sp
                        )
                    }

                    Panel("БЛИЖАЙШАЯ ВЫПЛАТА") {
                        val options = listOfNotNull(
                            nextPayDate(
                                today,
                                settings.advanceDay
                            )?.let {
                                "Аванс" to it
                            },

                            nextPayDate(
                                today,
                                settings.salaryDay
                            )?.let {
                                "Зарплата" to it
                            }
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
                            "Точная сумма зависит от расчётного периода и фактических удержаний.",
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
                            departure = departure!!
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

                2 -> {
                    FinanceScreen(
                        earnings = earnings,
                        payments = payments,
                        onPaymentsChange = ::persistPayments
                    )
                }

                3 -> {
                    OrganizerScreen(organizerContext)
                }

                4 -> {
                    SettingsScreen(
                        settings = settings,
                        onChange = onSettingsChange,
                        onLock = onLock
                    )
                }
            }
        }

        NavigationBar(
            containerColor = surface
        ) {
            listOf(
                "Главная",
                "Календарь",
                "Финансы",
                "Мои дела",
                "Настройки"
            ).forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = tab == index,
                    onClick = {
                        tab = index
                    },
                    icon = {
                        Text(
                            listOf(
                                "⌂",
                                "▦",
                                "₽",
                                "☑",
                                "⚙"
                            )[index]
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
        ?.takeIf { it in 1..31 }
        ?: return null

    fun inMonth(month: YearMonth): LocalDate =
        month.atDay(
            day.coerceAtMost(month.lengthOfMonth())
        )

    val current = inMonth(YearMonth.from(today))

    return if (current.isBefore(today)) {
        inMonth(
            YearMonth.from(today).plusMonths(1)
        )
    } else {
        current
    }
}

@Composable
private fun FinanceScreen(
    earnings: Double,
    payments: List<Payment>,
    onPaymentsChange: (List<Payment>) -> Unit
) {
    var date by remember {
        mutableStateOf(
            LocalDate.now().toString()
        )
    }

    var amount by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf("Зарплата")
    }

    Panel("РАСЧЁТНЫЕ НАЧИСЛЕНИЯ") {
        Text(
            money(earnings),
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
        DateField(
            "Дата получения",
            date
        ) {
            date = it
        }

        Field(
            "Получено, ₽",
            amount,
            true
        ) {
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
                    .clickable {
                        type = option
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = type == option,
                    onClick = {
                        type = option
                    }
                )

                Text(
                    option,
                    color = Color.White
                )
            }
        }

        Button(
            onClick = {
                onPaymentsChange(
                    payments + Payment(
                        date = date,
                        amount = decimal(amount),
                        type = type
                    )
                )

                amount = ""
            },
            enabled =
                decimal(amount) > 0.0 &&
                    parseDate(date) != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить выплату")
        }
    }

    Panel("ИСТОРИЯ ВЫПЛАТ") {
        Text(
            "Всего получено: ${
                money(payments.sumOf { it.amount })
            }",
            color = mint
        )

        if (payments.isEmpty()) {
            Text(
                "Записей пока нет",
                color = secondary
            )
        }

        payments
            .sortedByDescending { it.date }
            .forEach { payment ->
                HorizontalDivider()

                Text(
                    "${payment.date} · ${payment.type}",
                    color = secondary
                )

                Text(
                    money(payment.amount),
                    color = mint,
                    fontSize = 19.sp
                )

                TextButton(
                    onClick = {
                        val index = payments.indexOf(payment)

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
    settings: Settings,
    onChange: (Settings) -> Unit,
    onLock: () -> Unit
) {
    Panel("ЛИЧНЫЕ ДАННЫЕ") {
        Field(
            "Имя",
            settings.name
        ) {
            onChange(settings.copy(name = it))
        }
    }

    Panel("ГРАФИК ВАХТ") {
        DateField(
            "Дата заезда",
            settings.arrival
        ) {
            onChange(settings.copy(arrival = it))
        }

        DateField(
            "Дата выезда",
            settings.departure
        ) {
            onChange(settings.copy(departure = it))
        }

        Field(
            "Рабочих дней подряд",
            settings.workDays,
            true
        ) {
            onChange(settings.copy(workDays = it))
        }

        Field(
            "Выходных дней подряд",
            settings.restDays,
            true
        ) {
            onChange(settings.copy(restDays = it))
        }

        Text(
            "Следующие заезды рассчитываются автоматически по первым датам.",
            color = secondary,
            fontSize = 12.sp
        )
    }

    Panel("ОПЛАТА ТРУДА") {
        Field(
            "ЧТС, ₽/час",
            settings.hourly,
            true
        ) {
            onChange(settings.copy(hourly = it))
        }

        Field(
            "Часов в смене",
            settings.hours,
            true
        ) {
            onChange(settings.copy(hours = it))
        }

        Field(
            "Районный коэффициент",
            settings.coefficient,
            true
        ) {
            onChange(settings.copy(coefficient = it))
        }

        Field(
            "Северная надбавка, %",
            settings.north,
            true
        ) {
            onChange(settings.copy(north = it))
        }

        Field(
            "Производственная премия, %",
            settings.premium,
            true
        ) {
            onChange(settings.copy(premium = it))
        }

        Field(
            "Вахтовая надбавка, ₽/день",
            settings.shiftAllowance,
            true
        ) {
            onChange(
                settings.copy(shiftAllowance = it)
            )
        }

        Field(
            "Оплачиваемых часов дороги",
            settings.travelHours,
            true
        ) {
            onChange(settings.copy(travelHours = it))
        }

        Field(
            "Оплачиваемых часов межвахты",
            settings.intershiftHours,
            true
        ) {
            onChange(
                settings.copy(intershiftHours = it)
            )
        }

        Field(
            "Ставка межвахты, ₽/час",
            settings.intershiftRate,
            true
        ) {
            onChange(
                settings.copy(intershiftRate = it)
            )
        }
    }

    Panel("УДЕРЖАНИЯ") {
        Field(
            "НДФЛ, %",
            settings.tax,
            true
        ) {
            onChange(settings.copy(tax = it))
        }

        Field(
            "Питание, ₽",
            settings.meals,
            true
        ) {
            onChange(settings.copy(meals = it))
        }

        Field(
            "Исполнительные удержания, ₽",
            settings.enforcement,
            true
        ) {
            onChange(
                settings.copy(enforcement = it)
            )
        }

        Field(
            "Прочие удержания, ₽",
            settings.otherDeductions,
            true
        ) {
            onChange(
                settings.copy(otherDeductions = it)
            )
        }
    }

    Panel("ДАТЫ ВЫПЛАТ") {
        Field(
            "Число аванса",
            settings.advanceDay,
            true
        ) {
            onChange(
                settings.copy(advanceDay = it)
            )
        }

        Field(
            "Число зарплаты",
            settings.salaryDay,
            true
        ) {
            onChange(
                settings.copy(salaryDay = it)
            )
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
                        onChange(
                            settings.copy(
                                advanceMethod = method
                            )
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected =
                        settings.advanceMethod == method,
                    onClick = {
                        onChange(
                            settings.copy(
                                advanceMethod = method
                            )
                        )
                    }
                )

                Text(
                    method,
                    color = Color.White
                )
            }
        }

        if (
            settings.advanceMethod ==
            "Фиксированная сумма"
        ) {
            Field(
                "Сумма аванса, ₽",
                settings.fixedAdvance,
                true
            ) {
                onChange(
                    settings.copy(fixedAdvance = it)
                )
            }
        }

        Field(
            "Особенности начислений",
            settings.notes
        ) {
            onChange(settings.copy(notes = it))
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
    departure: LocalDate
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                repeat(7) { col ->
                    val day =
                        row * 7 + col - offset + 1

                    if (day in 1..count) {
                        val date = month.atDay(day)
                        val period = periodFor(
                            date,
                            arrival,
                            departure
                        )

                        val isArrival =
                            date == period.start

                        val isDeparture =
                            date == period.end

                        val cellColor = when {
                            isDeparture -> gold
                            isArrival -> Color(0xFF278E7A)
                            period.atWork -> Color(0xFF204F4A)
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
                                    if (date == selected) {
                                        Modifier.border(
                                            2.dp,
                                            Color.White,
                                            RoundedCornerShape(9.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    selected = date
                                },
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$day",
                                    color =
                                        if (isDeparture)
                                            navy
                                        else
                                            Color.White,
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
                            Modifier
                                .weight(1f)
                                .height(49.dp)
                        )
                    }
                }
            }
        }

        Info("Бирюзовый", "Вахта")
        Info("Синий", "Межвахта")
        Info("Золотой 🎉", "Выезд домой")

        HorizontalDivider()

        val selectedPeriod = periodFor(
            selected,
            arrival,
            departure
        )

        Text(
            selected.display(),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            when {
                selected == selectedPeriod.end ->
                    "🎉 День выезда домой!"

                selected == selectedPeriod.start ->
                    "🚀 День заезда!"

                selectedPeriod.atWork ->
                    "На вахте. До выезда: ${selectedPeriod.remaining} дн."

                else ->
                    "На межвахте. До заезда: ${selectedPeriod.remaining} дн."
            },
            color =
                if (selected == selectedPeriod.end)
                    gold
                else
                    mint
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
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Text(
                "‹",
                fontSize = 30.sp,
                color = mint
            )
        }

        Text(
            month.format(
                DateTimeFormatter.ofPattern(
                    "LLLL yyyy",
                    locale
                )
            ).replaceFirstChar {
                it.uppercase()
            },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNext) {
            Text(
                "›",
                fontSize = 30.sp,
                color = mint
            )
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(Modifier.fillMaxWidth()) {
        listOf(
            "Пн", "Вт", "Ср", "Чт",
            "Пт", "Сб", "Вс"
        ).forEach { day ->
            Text(
                day,
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
    var open by remember {
        mutableStateOf(false)
    }

    val selected = parseDate(value)

    OutlinedButton(
        onClick = {
            open = true
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "$label: ${
                selected?.display() ?: "Выбрать дату"
            }"
        )
    }

    if (open) {
        var month by remember(open) {
            mutableStateOf(
                YearMonth.from(
                    selected ?: LocalDate.now()
                )
            )
        }

        AlertDialog(
            onDismissRequest = {
                open = false
            },
            title = {
                Text(label)
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    MonthHeader(
                        month = month,
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
                                            if (day in 1..count) {
                                                Modifier.clickable {
                                                    onChange(
                                                        month
                                                            .atDay(day)
                                                            .toString()
                                                    )
                                                    open = false
                                                }
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    if (day in 1..count) {
                                        Text(
                                            "$day",
                                            color =
                                                if (
                                                    month.atDay(day) ==
                                                    selected
                                                ) {
                                                    mint
                                                } else {
                                                    Color.White
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        open = false
                    }
                ) {
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
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
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
        horizontalArrangement =
            Arrangement.SpaceBetween
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
        label = {
            Text(label)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType =
                if (numeric)
                    KeyboardType.Decimal
                else
                    KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
