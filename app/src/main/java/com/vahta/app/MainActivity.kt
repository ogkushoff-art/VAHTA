
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
import kotlin.math.abs

private val bg = Color(0xFF0B1728)
private val panel = Color(0xFF192D45)
private val mint = Color(0xFF56E4C0)
private val muted = Color(0xFFB4C6D9)
private val blue = Color(0xFF4389D8)
private val gold = Color(0xFFFFB45B)
private val ru = Locale.forLanguageTag("ru-RU")

private val rotations = listOf(
    "15×15", "30×30", "45×45", "60×60", "60×30"
)

private fun parseDate(v: String): LocalDate? =
    runCatching { LocalDate.parse(v) }.getOrNull()

private fun LocalDate.pretty() =
    format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun num(v: String): Double =
    v.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0 } ?: 0.0

private fun rub(v: Double) =
    NumberFormat.getCurrencyInstance(ru).format(v)

private fun rotationWork(v: String) =
    v.substringBefore('×').toIntOrNull() ?: 45

private fun rotationRest(v: String) =
    v.substringAfter('×').toIntOrNull() ?: 45

private fun rotationMonths(v: String) = when (v) {
    "15×15" -> 1
    "30×30" -> 2
    "60×60" -> 4
    else -> 3
}

private fun anchoredDate(
    anchor: LocalDate,
    months: Long
): LocalDate {
    val month = YearMonth.from(anchor).plusMonths(months)
    return month.atDay(
        anchor.dayOfMonth.coerceAtMost(month.lengthOfMonth())
    )
}

private data class Config(
    val name: String = "",
    val rotation: String = "",
    val arrival: String = "",
    val departure: String = "",
    val hourly: String = "",
    val hours: String = "",
    val workDays: String = "",
    val restDays: String = "",
    val coefficient: String = "",
    val north: String = "",
    val premium: String = "",
    val allowance: String = "",
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
    val notes: String = "",
    val reminders: String = "",
    val remindArrival: Boolean = false,
    val remindDeparture: Boolean = false,
    val remindSalary: Boolean = false,
    val remindAdvance: Boolean = false,
    val remindEvents: Boolean = false,
    val remindDays: String = "1",
    val remindTime: String = "09:00"
)

private fun readConfig(
    p: SharedPreferences,
    prefix: String = ""
): Config {
    fun g(k: String) = p.getString(prefix + k, "") ?: ""
    fun b(k: String) = p.getBoolean(prefix + k, false)

    return Config(
        name = g("name"),
        rotation = g("rotation"),
        arrival = g("arrival"),
        departure = g("departure"),
        hourly = g("hourly"),
        hours = g("hours"),
        workDays = g("workDays"),
        restDays = g("restDays"),
        coefficient = g("coefficient"),
        north = g("north"),
        premium = g("premium"),
        allowance = g("allowance"),
        travelHours = g("travelHours"),
        intershiftHours = g("intershiftHours"),
        intershiftRate = g("intershiftRate"),
        tax = g("tax"),
        meals = g("meals"),
        enforcement = g("enforcement"),
        otherDeductions = g("otherDeductions"),
        advanceDay = g("advanceDay"),
        salaryDay = g("salaryDay"),
        advanceMethod = g("advanceMethod").ifBlank {
            "По отработанным часам"
        },
        fixedAdvance = g("fixedAdvance"),
        notes = g("notes"),
        reminders = g("reminders"),
        remindArrival = b("remindArrival"),
        remindDeparture = b("remindDeparture"),
        remindSalary = b("remindSalary"),
        remindAdvance = b("remindAdvance"),
        remindEvents = b("remindEvents"),
        remindDays = g("remindDays").ifBlank { "1" },
        remindTime = g("remindTime").ifBlank { "09:00" }
    )
}

private fun saveConfig(
    p: SharedPreferences,
    s: Config,
    prefix: String = ""
) {
    p.edit().apply {
        fun put(k: String, v: String) {
            putString(prefix + k, v)
        }

        fun put(k: String, v: Boolean) {
            putBoolean(prefix + k, v)
        }

        put("name", s.name)
        put("rotation", s.rotation)
        put("arrival", s.arrival)
        put("departure", s.departure)
        put("hourly", s.hourly)
        put("hours", s.hours)
        put("workDays", s.workDays)
        put("restDays", s.restDays)
        put("coefficient", s.coefficient)
        put("north", s.north)
        put("premium", s.premium)
        put("allowance", s.allowance)
        put("travelHours", s.travelHours)
        put("intershiftHours", s.intershiftHours)
        put("intershiftRate", s.intershiftRate)
        put("tax", s.tax)
        put("meals", s.meals)
        put("enforcement", s.enforcement)
        put("otherDeductions", s.otherDeductions)
        put("advanceDay", s.advanceDay)
        put("salaryDay", s.salaryDay)
        put("advanceMethod", s.advanceMethod)
        put("fixedAdvance", s.fixedAdvance)
        put("notes", s.notes)
        put("reminders", s.reminders)
        put("remindArrival", s.remindArrival)
        put("remindDeparture", s.remindDeparture)
        put("remindSalary", s.remindSalary)
        put("remindAdvance", s.remindAdvance)
        put("remindEvents", s.remindEvents)
        put("remindDays", s.remindDays)
        put("remindTime", s.remindTime)
        commit()
    }
}

private fun clearDraft(p: SharedPreferences) {
    p.edit().apply {
        p.all.keys
            .filter { it.startsWith("draft_") }
            .forEach { remove(it) }
        remove("setup_step")
        apply()
    }
}

private data class Period(
    val start: LocalDate,
    val end: LocalDate,
    val next: LocalDate,
    val working: Boolean,
    val elapsed: Long,
    val total: Long,
    val remaining: Long,
    val progress: Float
)

private fun periodFor(
    day: LocalDate,
    first: LocalDate,
    last: LocalDate,
    rotation: String
): Period {
    val step = rotationMonths(rotation).toLong()

    val monthDelta = ChronoUnit.MONTHS.between(
        YearMonth.from(first),
        YearMonth.from(day)
    )

    var i = Math.floorDiv(monthDelta, step)

    fun start(n: Long) =
        anchoredDate(first, n * step)

    while (start(i).isAfter(day)) i--
    while (!start(i + 1).isAfter(day)) i++

    val a = start(i)
    val next = start(i + 1)
    val rawEnd = anchoredDate(last, i * step)

    val end = when {
        rawEnd.isBefore(a) -> a
        !rawEnd.isBefore(next) -> next.minusDays(1)
        else -> rawEnd
    }

    val working = !day.isAfter(end)

    val begin = if (working) a else end.plusDays(1)
    val finish = if (working) end else next.minusDays(1)

    val total = (
        ChronoUnit.DAYS.between(begin, finish) + 1
    ).coerceAtLeast(1)

    val elapsed = (
        ChronoUnit.DAYS.between(begin, day) + 1
    ).coerceIn(0, total)

    return Period(
        a, end, next, working,
        elapsed, total, total - elapsed,
        elapsed.toFloat() / total
    )
}

private data class Payment(
    val date: String,
    val amount: Double,
    val type: String,
    val forecast: Double = -1.0
)

private fun loadPayments(raw: String): List<Payment> =
    runCatching {
        val a = JSONArray(raw)
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Payment(
                o.optString("date"),
                o.optDouble("amount"),
                o.optString("type"),
                o.optDouble("forecast", -1.0)
            )
        }
    }.getOrDefault(emptyList())

private fun encodePayments(
    items: List<Payment>
) = JSONArray().apply {
    items.forEach {
        put(
            JSONObject()
                .put("date", it.date)
                .put("amount", it.amount)
                .put("type", it.type)
                .put("forecast", it.forecast)
        )
    }
}.toString()

private data class CalendarEvent(
    val date: String,
    val title: String
)

private fun loadEvents(raw: String): List<CalendarEvent> =
    runCatching {
        val a = JSONArray(raw)
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            CalendarEvent(
                o.optString("date"),
                o.optString("title")
            )
        }
    }.getOrDefault(emptyList())

private fun encodeEvents(
    items: List<CalendarEvent>
) = JSONArray().apply {
    items.forEach {
        put(
            JSONObject()
                .put("date", it.date)
                .put("title", it.title)
        )
    }
}.toString()

// Общероссийские нерабочие дни и переносы 2026 года.
// Для других годов пока используются только
// основные федеральные праздники и выходные.

private val nonWorking2026 = setOf(
    "2026-01-01",
    "2026-01-02",
    "2026-01-05",
    "2026-01-06",
    "2026-01-07",
    "2026-01-08",
    "2026-01-09",
    "2026-02-23",
    "2026-03-09",
    "2026-05-01",
    "2026-05-11",
    "2026-06-12",
    "2026-11-04",
    "2026-12-31"
)

private fun isNonWorkingDay(
    date: LocalDate
): Boolean {
    if (
        date.dayOfWeek == DayOfWeek.SATURDAY ||
        date.dayOfWeek == DayOfWeek.SUNDAY
    ) return true

    if (date.year == 2026) {
        return date.toString() in nonWorking2026
    }

    return when (date.monthValue) {
        1 -> date.dayOfMonth in 1..8
        2 -> date.dayOfMonth == 23
        3 -> date.dayOfMonth == 8
        5 -> date.dayOfMonth == 1 ||
             date.dayOfMonth == 9
        6 -> date.dayOfMonth == 12
        11 -> date.dayOfMonth == 4
        else -> false
    }
}

private fun payDate(
    month: YearMonth,
    dayText: String
): LocalDate? {
    val day = dayText.toIntOrNull()
        ?.takeIf { it in 1..31 }
        ?: return null

    var date = month.atDay(
        day.coerceAtMost(month.lengthOfMonth())
    )

    while (isNonWorkingDay(date)) {
        date = date.minusDays(1)
    }

    return date
}

// Учитываем перенос выплаты с начала
// следующего месяца на конец текущего.

private fun isPayDate(
    date: LocalDate,
    dayText: String
): Boolean {
    val month = YearMonth.from(date)

    return payDate(month, dayText) == date ||
        payDate(
            month.plusMonths(1),
            dayText
        ) == date
}

private fun nextPayDate(
    today: LocalDate,
    text: String
): LocalDate? {
    val month = YearMonth.from(today)

    for (offset in 0L..3L) {
        val date = payDate(
            month.plusMonths(offset),
            text
        ) ?: return null

        if (!date.isBefore(today)) {
            return date
        }
    }

    return null
}

private fun grossForRange(
    s: Config,
    start: LocalDate,
    end: LocalDate
): Double {
    if (end.isBefore(start)) return 0.0

    val first = parseDate(s.arrival) ?: return 0.0
    val last = parseDate(s.departure) ?: return 0.0

    if (last.isBefore(first)) return 0.0
    if (s.rotation !in rotations) return 0.0

    val hourly = num(s.hourly)
    val hours = num(s.hours)

    if (hourly <= 0.0 || hours <= 0.0) return 0.0

    val workDays = (
        s.workDays.toIntOrNull() ?: 1
    ).coerceIn(1, 366)

    val restDays = (
        s.restDays.toIntOrNull() ?: 0
    ).coerceIn(0, 366)

    val cycle = workDays + restDays

    val coefficient = num(s.coefficient)
        .takeIf { it > 0.0 } ?: 1.0

    val north = num(s.north) / 100.0
    val premium = num(s.premium) / 100.0
    val allowance = num(s.allowance)

    var total = 0.0
    var day = start

    while (!day.isAfter(end)) {
        val period = periodFor(
            day, first, last, s.rotation
        )

        if (period.working) {
            val offset = ChronoUnit.DAYS.between(
                period.start, day
            )

            val cycleDay = Math.floorMod(
                offset,
                cycle.toLong()
            )

            if (cycleDay < workDays) {
                val base = hourly * hours

                total += base * (
                    coefficient + north + premium
                )
            }

            total += allowance
        }

        day = day.plusDays(1)
    }

    return total
}

private fun netFromGross(
    s: Config,
    gross: Double
): Double {
    val tax = gross * num(s.tax) / 100.0

    val deductions =
        num(s.meals) +
        num(s.enforcement) +
        num(s.otherDeductions)

    return (
        gross - tax - deductions
    ).coerceAtLeast(0.0)
}

/*
 * Предварительная оценка выплаты.
 *
 * Для зарплаты берётся предыдущий
 * календарный месяц.
 *
 * Для фиксированного аванса
 * используется указанная сумма.
 *
 * Для остальных способов аванса
 * прогноз не создаётся автоматически,
 * поскольку расчётный период
 * работодателя может отличаться.
 */
private fun estimateForPayday(
    s: Config,
    date: LocalDate,
    type: String
): Double? {
    if (type == "Аванс") {
        return if (
            s.advanceMethod == "Фиксированная сумма" &&
            num(s.fixedAdvance) > 0.0
        ) {
            num(s.fixedAdvance)
        } else {
            null
        }
    }

    if (type != "Зарплата") return null

    val previousMonth = YearMonth.from(date)
        .minusMonths(1)

    val gross = grossForRange(
        s,
        previousMonth.atDay(1),
        previousMonth.atEndOfMonth()
    )

    return netFromGross(s, gross)
}

/*
 * Сумма начислений с начала текущей
 * вахты до текущего момента.
 *
 * Счётчик обновляется каждую секунду.
 * Это оценка, не бухгалтерский расчёт.
 */
private fun liveEarnings(
    s: Config,
    now: LocalDateTime
): Double {
    val first = parseDate(s.arrival) ?: return 0.0
    val last = parseDate(s.departure) ?: return 0.0

    if (last.isBefore(first)) return 0.0
    if (s.rotation !in rotations) return 0.0

    val today = now.toLocalDate()

    val period = periodFor(
        today, first, last, s.rotation
    )

    val completedEnd = if (period.working) {
        today.minusDays(1)
    } else {
        period.end
    }

    var gross = grossForRange(
        s,
        period.start,
        completedEnd
    )

    if (period.working) {
        val workDays = (
            s.workDays.toIntOrNull() ?: 1
        ).coerceIn(1, 366)

        val restDays = (
            s.restDays.toIntOrNull() ?: 0
        ).coerceIn(0, 366)

        val cycle = workDays + restDays

        val offset = ChronoUnit.DAYS.between(
            period.start, today
        )

        val workingToday = Math.floorMod(
            offset,
            cycle.toLong()
        ) < workDays

        val fraction =
            now.toLocalTime().toSecondOfDay() / 86400.0

        val coefficient = num(s.coefficient)
            .takeIf { it > 0.0 } ?: 1.0

        val north = num(s.north) / 100.0
        val premium = num(s.premium) / 100.0

        if (workingToday) {
            val base = num(s.hourly) * num(s.hours)

            gross += base * (
                coefficient + north + premium
            ) * fraction
        }

        gross += num(s.allowance) * fraction
    }

    return gross
}

class MainActivity : FragmentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "vahta_settings",
            Context.MODE_PRIVATE
        )

        setContent {
            var unlocked by remember {
                mutableStateOf(false)
            }

            var setupDone by remember {
                mutableStateOf(
                    prefs.getBoolean(
                        "setup_complete_v2",
                        false
                    )
                )
            }

            var config by remember {
                mutableStateOf(
                    readConfig(prefs)
                )
            }

            val owner = LocalLifecycleOwner.current

            var backgrounded by remember {
                mutableStateOf(false)
            }

            DisposableEffect(owner) {
                val observer = LifecycleEventObserver {
                    _, event ->

                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (unlocked) {
                                backgrounded = true
                            }
                        }

                        Lifecycle.Event.ON_START -> {
                            if (backgrounded) {
                                unlocked = false
                                backgrounded = false
                            }
                        }

                        else -> Unit
                    }
                }

                owner.lifecycle.addObserver(observer)

                onDispose {
                    owner.lifecycle.removeObserver(
                        observer
                    )
                }
            }

            VahtaTheme {
                when {
                    !unlocked -> {
                        SecurityScreen(
                            onUnlocked = {
                                unlocked = true
                            }
                        )
                    }

                    !setupDone -> {
                        Wizard(
                            prefs = prefs,
                            onFinish = { result ->
                                config = result

                                saveConfig(
                                    prefs,
                                    result
                                )

                                prefs.edit()
                                    .putBoolean(
                                        "setup_complete_v2",
                                        true
                                    )
                                    .apply()

                                clearDraft(prefs)

                                setupDone = true
                            }
                        )
                    }

                    else -> {
                        Dashboard(
                            s = config,
                            onChange = { updated ->
                                config = updated

                                saveConfig(
                                    prefs,
                                    updated
                                )
                            },
                            prefs = prefs,
                            onLock = {
                                unlocked = false
                            },
                            organizerContext =
                                this@MainActivity
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VahtaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = mint,
            background = bg,
            surface = panel,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
private fun Wizard(
    prefs: SharedPreferences,
    onFinish: (Config) -> Unit
) {
    var s by remember {
        mutableStateOf(readConfig(prefs, "draft_"))
    }

    var step by remember {
        mutableIntStateOf(
            prefs.getInt("setup_step", 0).coerceIn(0, 6)
        )
    }

    fun update(value: Config) {
        s = value
        saveConfig(prefs, value, "draft_")
    }

    fun move(next: Int) {
        step = next
        prefs.edit()
            .putInt("setup_step", next)
            .apply()
    }

    val first = parseDate(s.arrival)
    val last = parseDate(s.departure)

    val datesOk =
        first != null &&
        last != null &&
        !last.isBefore(first) &&
        s.rotation in rotations

    val working = s.workDays.toIntOrNull()
    val resting = s.restDays.toIntOrNull()

    val canNext = when (step) {
        0 -> s.name.isNotBlank()

        1 -> datesOk

        2 -> {
            num(s.hourly) > 0.0 &&
            num(s.hours) in 0.01..24.0 &&
            working != null &&
            working in 1..366 &&
            resting != null &&
            resting in 0..366
        }

        4 -> {
            s.tax.isBlank() ||
            num(s.tax) <= 100.0
        }

        5 -> {
            val advance = s.advanceDay.toIntOrNull()
            val salary = s.salaryDay.toIntOrNull()

            (s.advanceDay.isBlank() ||
                advance != null && advance in 1..31) &&
            (s.salaryDay.isBlank() ||
                salary != null && salary in 1..31) &&
            (s.advanceMethod != "Фиксированная сумма" ||
                num(s.fixedAdvance) > 0.0)
        }

        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(18.dp)
    ) {
        Text(
            "ВАХТА",
            fontSize = 31.sp,
            color = Color.White,
            fontWeight = FontWeight.Black
        )

        Text(
            "ПЕРВОНАЧАЛЬНАЯ НАСТРОЙКА",
            color = mint,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { (step + 1) / 7f },
            modifier = Modifier.fillMaxWidth(),
            color = mint
        )

        Text(
            "Шаг ${step + 1} из 7 · " +
                "Ответы сохраняются автоматически",
            color = muted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (step) {
                0 -> {
                    Heading(
                        "Знакомство",
                        "Как к тебе обращаться?"
                    )

                    Field("Имя", s.name) {
                        update(s.copy(name = it))
                    }
                }

                1 -> {
                    Heading(
                        "График вахты",
                        "Выбери тип вахты и точные даты " +
                            "первого заезда и выезда."
                    )

                    RotationSelect(s.rotation) {
                        update(s.copy(rotation = it))
                    }

                    DateField("Дата заезда", s.arrival) {
                        update(s.copy(arrival = it))
                    }

                    DateField("Дата выезда домой", s.departure) {
                        update(s.copy(departure = it))
                    }

                    if (
                        first != null &&
                        last != null &&
                        !last.isBefore(first)
                    ) {
                        val actual =
                            ChronoUnit.DAYS.between(
                                first, last
                            ) + 1

                        Text(
                            "Продолжительность первой вахты: " +
                                "$actual календарных дней",
                            color = mint
                        )

                        if (s.rotation in rotations) {
                            val nextArrival = anchoredDate(
                                first,
                                rotationMonths(s.rotation).toLong()
                            )

                            val nextDeparture = anchoredDate(
                                last,
                                rotationMonths(s.rotation).toLong()
                            )

                            Info(
                                "Следующий заезд",
                                nextArrival.pretty()
                            )

                            Info(
                                "Следующий выезд",
                                nextDeparture.pretty()
                            )
                        }
                    }

                    Text(
                        "Последующие заезды и выезды " +
                            "повторяются по числам месяца. " +
                            "31-е число учитывается. " +
                            "Если в месяце нет нужного числа, " +
                            "используется последний день " +
                            "только этого месяца.",
                        color = muted,
                        fontSize = 12.sp
                    )
                }

                2 -> {
                    Heading(
                        "Рабочее время",
                        "Укажи часовую ставку, " +
                            "длительность смены и " +
                            "чередование рабочих и выходных."
                    )

                    Field(
                        "ЧТС, ₽/час",
                        s.hourly,
                        true
                    ) {
                        update(s.copy(hourly = it))
                    }

                    Field(
                        "Часов в смене",
                        s.hours,
                        true
                    ) {
                        update(s.copy(hours = it))
                    }

                    Field(
                        "Рабочих дней подряд",
                        s.workDays,
                        true
                    ) {
                        update(s.copy(workDays = it))
                    }

                    Field(
                        "Выходных дней подряд " +
                            "(0, если нет)",
                        s.restDays,
                        true
                    ) {
                        update(s.copy(restDays = it))
                    }

                    if (!canNext) {
                        Text(
                            "Проверь значения: ставка " +
                                "больше нуля, смена до 24 часов, " +
                                "рабочие дни от 1 до 366, " +
                                "выходные от 0 до 366.",
                            color = gold,
                            fontSize = 12.sp
                        )
                    }
                }

                3 -> {
                    Heading(
                        "Надбавки и премии",
                        "Неприменимые начисления " +
                            "можно оставить пустыми."
                    )

                    Field(
                        "Районный коэффициент",
                        s.coefficient,
                        true
                    ) {
                        update(s.copy(coefficient = it))
                    }

                    Field(
                        "Северная надбавка, %",
                        s.north,
                        true
                    ) {
                        update(s.copy(north = it))
                    }

                    Field(
                        "Производственная премия, %",
                        s.premium,
                        true
                    ) {
                        update(s.copy(premium = it))
                    }

                    Field(
                        "Вахтовая надбавка, ₽/день",
                        s.allowance,
                        true
                    ) {
                        update(s.copy(allowance = it))
                    }

                    Field(
                        "Оплачиваемых часов дороги " +
                            "за вахту",
                        s.travelHours,
                        true
                    ) {
                        update(s.copy(travelHours = it))
                    }
                }

                4 -> {
                    Heading(
                        "Удержания",
                        "Укажи известные удержания. " +
                            "Неизвестные значения " +
                            "можно заполнить позже."
                    )

                    Field(
                        "НДФЛ, %",
                        s.tax,
                        true
                    ) {
                        update(s.copy(tax = it))
                    }

                    Field(
                        "Питание, ₽",
                        s.meals,
                        true
                    ) {
                        update(s.copy(meals = it))
                    }

                    Field(
                        "Исполнительные удержания, ₽",
                        s.enforcement,
                        true
                    ) {
                        update(s.copy(enforcement = it))
                    }

                    Field(
                        "Прочие удержания, ₽",
                        s.otherDeductions,
                        true
                    ) {
                        update(s.copy(otherDeductions = it))
                    }

                    Text(
                        "Оплату межвахтового отдыха " +
                            "можно настроить позднее, " +
                            "когда появятся данные " +
                            "из расчётного листка.",
                        color = muted,
                        fontSize = 12.sp
                    )
                }

                5 -> {
                    Heading(
                        "Выплаты",
                        "Настрой даты аванса и зарплаты. " +
                            "Позже их можно будет выбрать " +
                            "нажатием на календарь."
                    )

                    Field(
                        "Число аванса (необязательно)",
                        s.advanceDay,
                        true
                    ) {
                        update(s.copy(advanceDay = it))
                    }

                    Field(
                        "Число зарплаты (необязательно)",
                        s.salaryDay,
                        true
                    ) {
                        update(s.copy(salaryDay = it))
                    }

                    Text(
                        "Как рассчитывается аванс?",
                        color = Color.White
                    )

                    listOf(
                        "По отработанным часам",
                        "Фиксированная сумма",
                        "Другой порядок"
                    ).forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    update(
                                        s.copy(
                                            advanceMethod = option
                                        )
                                    )
                                },
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    s.advanceMethod == option,
                                onClick = {
                                    update(
                                        s.copy(
                                            advanceMethod = option
                                        )
                                    )
                                }
                            )

                            Text(
                                option,
                                color = Color.White
                            )
                        }
                    }

                    if (
                        s.advanceMethod ==
                        "Фиксированная сумма"
                    ) {
                        Field(
                            "Сумма аванса, ₽",
                            s.fixedAdvance,
                            true
                        ) {
                            update(
                                s.copy(fixedAdvance = it)
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "Напоминания",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Включить напоминания?",
                        color = muted
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = s.reminders == "Да",
                            onClick = {
                                update(
                                    s.copy(reminders = "Да")
                                )
                            }
                        )

                        Text("Да", color = Color.White)

                        Spacer(Modifier.width(20.dp))

                        RadioButton(
                            selected = s.reminders == "Нет",
                            onClick = {
                                update(
                                    s.copy(
                                        reminders = "Нет",
                                        remindArrival = false,
                                        remindDeparture = false,
                                        remindSalary = false,
                                        remindAdvance = false,
                                        remindEvents = false
                                    )
                                )
                            }
                        )

                        Text("Нет", color = Color.White)
                    }

                    if (s.reminders == "Да") {
                        ReminderChoice(
                            "Заезд на вахту",
                            s.remindArrival
                        ) {
                            update(
                                s.copy(remindArrival = it)
                            )
                        }

                        ReminderChoice(
                            "Выезд домой",
                            s.remindDeparture
                        ) {
                            update(
                                s.copy(remindDeparture = it)
                            )
                        }

                        ReminderChoice(
                            "Зарплата",
                            s.remindSalary
                        ) {
                            update(
                                s.copy(remindSalary = it)
                            )
                        }

                        ReminderChoice(
                            "Аванс",
                            s.remindAdvance
                        ) {
                            update(
                                s.copy(remindAdvance = it)
                            )
                        }

                        ReminderChoice(
                            "Личные события",
                            s.remindEvents
                        ) {
                            update(
                                s.copy(remindEvents = it)
                            )
                        }

                        Text(
                            "Когда напоминать?",
                            color = Color.White
                        )

                        listOf(
                            "0" to "В день события",
                            "1" to "За 1 день",
                            "3" to "За 3 дня",
                            "7" to "За 7 дней"
                        ).forEach { (days, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        update(
                                            s.copy(
                                                remindDays = days
                                            )
                                        )
                                    },
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected =
                                        s.remindDays == days,
                                    onClick = {
                                        update(
                                            s.copy(
                                                remindDays = days
                                            )
                                        )
                                    }
                                )

                                Text(
                                    label,
                                    color = Color.White
                                )
                            }
                        }

                        Field(
                            "Время напоминания (ЧЧ:ММ)",
                            s.remindTime
                        ) {
                            update(
                                s.copy(remindTime = it)
                            )
                        }

                        Text(
                            "Настройки будут сохранены. " +
                                "Для отправки системных " +
                                "уведомлений потребуется " +
                                "подключить службу Android.",
                            color = gold,
                            fontSize = 12.sp
                        )
                    }
                }

                6 -> {
                    Heading(
                        "Проверка данных",
                        "Проверь настройки перед " +
                            "началом работы."
                    )

                    Panel("МОЙ ПРОФИЛЬ") {
                        Info("Имя", s.name)
                        Info("График", s.rotation)

                        Info(
                            "Первый заезд",
                            first?.pretty() ?: "—"
                        )

                        Info(
                            "Первый выезд",
                            last?.pretty() ?: "—"
                        )

                        if (
                            first != null &&
                            last != null &&
                            !last.isBefore(first)
                        ) {
                            Info(
                                "Календарных дней",
                                "${
                                    ChronoUnit.DAYS.between(
                                        first, last
                                    ) + 1
                                }"
                            )
                        }

                        Info(
                            "Часовая ставка",
                            "${s.hourly} ₽"
                        )

                        Info(
                            "Часов в смене",
                            s.hours
                        )

                        Info(
                            "Работа / отдых",
                            "${s.workDays} / " +
                                "${s.restDays}"
                        )

                        Info(
                            "День аванса",
                            s.advanceDay.ifBlank { "—" }
                        )

                        Info(
                            "День зарплаты",
                            s.salaryDay.ifBlank { "—" }
                        )

                        Info(
                            "Напоминания",
                            when (s.reminders) {
                                "Да" -> "Выбраны"
                                else -> "Выключены"
                            }
                        )
                    }

                    Text(
                        "После завершения настройки " +
                            "данные можно будет изменить " +
                            "в разделе «Настройки».",
                        color = muted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = {
                        move(step - 1)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Назад")
                }
            }

            Button(
                onClick = {
                    if (step < 6) {
                        move(step + 1)
                    } else {
                        onFinish(s)
                    }
                },
                enabled = canNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (step == 6) {
                        "Начать работу"
                    } else {
                        "Продолжить"
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderChoice(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange
        )

        Text(
            label,
            color = Color.White
        )
    }
}

@Composable
private fun RotationSelect(
    value: String,
    onChange: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Тип вахты: ${
                    value.ifBlank { "Выбрать" }
                }"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            rotations.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Dashboard(
    s: Config,
    onChange: (Config) -> Unit,
    prefs: SharedPreferences,
    onLock: () -> Unit,
    organizerContext: Context
) {
    var tab by remember { mutableIntStateOf(0) }
    var clock by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(Unit) {
        while (true) {
            clock = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val now = remember(clock / 1000L) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(clock),
            ZoneId.systemDefault()
        )
    }

    val today = now.toLocalDate()
    val first = parseDate(s.arrival)
    val last = parseDate(s.departure)

    val valid =
        first != null &&
        last != null &&
        !last.isBefore(first) &&
        s.rotation in rotations

    val period = if (valid) {
        periodFor(today, first!!, last!!, s.rotation)
    } else null

    val earnings = liveEarnings(s, now)

    var payments by remember {
        mutableStateOf(
            loadPayments(
                prefs.getString("payments", "[]") ?: "[]"
            )
        )
    }

    fun savePayments(items: List<Payment>) {
        payments = items
        prefs.edit()
            .putString("payments", encodePayments(items))
            .apply()
    }

    var events by remember {
        mutableStateOf(
            loadEvents(
                prefs.getString("calendar_events", "[]") ?: "[]"
            )
        )
    }

    fun saveEvents(items: List<CalendarEvent>) {
        events = items
        prefs.edit()
            .putString("calendar_events", encodeEvents(items))
            .apply()
    }

    Column(
        Modifier.fillMaxSize().background(bg)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (tab == 3) Modifier
                    else Modifier.verticalScroll(
                        rememberScrollState()
                    )
                )
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "ВАХТА",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                "Привет, ${s.name}!",
                color = muted,
                fontSize = 17.sp
            )

            when (tab) {
                0 -> {
                    Panel("МОЙ ГРАФИК") {
                        if (period == null) {
                            Text(
                                "Настрой даты вахты",
                                color = gold
                            )
                        } else {
                            Text(
                                if (period.working)
                                    "НА ВАХТЕ"
                                else
                                    "НА МЕЖВАХТЕ",
                                color = if (period.working)
                                    mint else blue,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                if (period.working)
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
                                color = mint
                            )

                            Info(
                                "Прогресс",
                                "${(period.progress * 100).toInt()}%"
                            )
                            Info("График", s.rotation)
                            Info("Заезд", period.start.pretty())
                            Info("Выезд", period.end.pretty())
                            Info(
                                "Следующий заезд",
                                period.next.pretty()
                            )
                        }
                    }

                    Panel("ЖИВОЙ СЧЁТЧИК НАЧИСЛЕНИЙ") {
                        Text(
                            rub(earnings),
                            color = mint,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            if (period?.working == true)
                                "Обновляется каждую секунду"
                            else
                                "На межвахте счётчик остановлен",
                            color = muted
                        )

                        Text(
                            "Приблизительное начисление " +
                                "до удержаний. Не является " +
                                "расчётным листком.",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }

                    Panel("БЛИЖАЙШИЕ ВЫПЛАТЫ") {
                        val advance = nextPayDate(
                            today, s.advanceDay
                        )
                        val salary = nextPayDate(
                            today, s.salaryDay
                        )

                        Info(
                            "Аванс",
                            advance?.pretty() ?: "Не задан"
                        )
                        Info(
                            "Зарплата",
                            salary?.pretty() ?: "Не задана"
                        )

                        if (advance != null) {
                            estimateForPayday(
                                s, advance, "Аванс"
                            )?.let {
                                Info(
                                    "Прогноз аванса",
                                    rub(it)
                                )
                            }
                        }

                        if (salary != null) {
                            estimateForPayday(
                                s, salary, "Зарплата"
                            )?.let {
                                Info(
                                    "Прогноз зарплаты",
                                    rub(it)
                                )
                            }
                        }

                        Text(
                            "Если установленная дата " +
                                "выпадает на выходной или " +
                                "праздник, выплата " +
                                "переносится на предыдущий " +
                                "рабочий день.",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { tab = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Открыть календарь")
                    }

                    OutlinedButton(
                        onClick = { tab = 2 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Проверить зарплату")
                    }
                }

                1 -> {
                    if (valid) {
                        ShiftCalendar(
                            today = today,
                            first = first!!,
                            last = last!!,
                            rotation = s.rotation,
                            config = s,
                            onConfigChange = onChange,
                            payments = payments,
                            events = events,
                            onEventsChange = ::saveEvents
                        )
                    } else {
                        Panel("КАЛЕНДАРЬ") {
                            Text(
                                "Укажи даты в настройках",
                                color = muted
                            )
                        }
                    }
                }

                2 -> Finance(
                    s = s,
                    today = today,
                    earnings = earnings,
                    payments = payments,
                    onChange = ::savePayments
                )

                3 -> OrganizerScreen(organizerContext)

                4 -> Settings(
                    s = s,
                    onChange = onChange,
                    onLock = onLock
                )
            }
        }

        NavigationBar(containerColor = panel) {
            val labels = listOf(
                "Главная",
                "Календарь",
                "Финансы",
                "Мои дела",
                "Настройки"
            )

            val icons = listOf(
                "⌂", "▦", "₽", "☑", "⚙"
            )

            labels.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = tab == index,
                    onClick = { tab = index },
                    icon = { Text(icons[index]) },
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

@Composable
private fun ShiftCalendar(
    today: LocalDate,
    first: LocalDate,
    last: LocalDate,
    rotation: String,
    config: Config,
    onConfigChange: (Config) -> Unit,
    payments: List<Payment>,
    events: List<CalendarEvent>,
    onEventsChange: (List<CalendarEvent>) -> Unit
) {
    var month by remember {
        mutableStateOf(YearMonth.from(today))
    }

    var selected by remember {
        mutableStateOf(today)
    }

    var eventTitle by remember {
        mutableStateOf("")
    }

    val selectedPeriod = periodFor(
        selected, first, last, rotation
    )

    Panel("КАЛЕНДАРЬ ВАХТ И ВЫПЛАТ") {
        MonthHeader(
            month,
            { month = month.minusMonths(1) },
            { month = month.plusMonths(1) }
        )

        WeekHeader()

        val offset =
            month.atDay(1).dayOfWeek.value - 1

        val count = month.lengthOfMonth()

        repeat((offset + count + 6) / 7) { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                repeat(7) { col ->
                    val day = row * 7 + col - offset + 1

                    if (day in 1..count) {
                        val date = month.atDay(day)
                        val period = periodFor(
                            date, first, last, rotation
                        )

                        val arrival = date == period.start
                        val departure = date == period.end

                        val advance = isPayDate(
                            date, config.advanceDay
                        )

                        val salary = isPayDate(
                            date, config.salaryDay
                        )

                        val hasEvent = events.any {
                            it.date == date.toString()
                        }

                        val hasPayment = payments.any {
                            it.date == date.toString()
                        }

                        val cellColor = when {
                            departure -> gold
                            arrival -> Color(0xFF278E7A)
                            period.working -> Color(0xFF204F4A)
                            else -> Color(0xFF233F65)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(63.dp)
                                .background(
                                    cellColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .then(
                                    if (date == selected)
                                        Modifier.border(
                                            2.dp,
                                            Color.White,
                                            RoundedCornerShape(8.dp)
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
                                    color = if (departure)
                                        bg else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                Text(
                                    buildString {
                                        if (arrival) append("↑")
                                        if (departure) append("⌂")
                                        if (advance) append("А")
                                        if (salary) append("₽")
                                        if (hasEvent) append("•")
                                        if (hasPayment) append("✓")
                                    }.ifBlank {
                                        if (date == today) "●"
                                        else " "
                                    },
                                    color = if (departure)
                                        bg else mint,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .height(63.dp)
                        )
                    }
                }
            }
        }

        Text(
            "↑ Заезд   ⌂ Выезд   А Аванс   " +
                "₽ Зарплата   • Событие   ✓ Получено",
            color = muted,
            fontSize = 11.sp
        )

        HorizontalDivider()

        Text(
            selected.pretty(),
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            when {
                selected == selectedPeriod.start ->
                    "День заезда на вахту"

                selected == selectedPeriod.end ->
                    "День выезда домой"

                selectedPeriod.working ->
                    "На вахте · осталось " +
                        "${selectedPeriod.remaining} дн."

                else ->
                    "На межвахте · осталось " +
                        "${selectedPeriod.remaining} дн."
            },
            color = mint
        )

        val selectedAdvance = isPayDate(
            selected, config.advanceDay
        )

        val selectedSalary = isPayDate(
            selected, config.salaryDay
        )

        if (selectedAdvance) {
            Text(
                "АВАНС",
                color = gold,
                fontWeight = FontWeight.Bold
            )

            estimateForPayday(
                config, selected, "Аванс"
            )?.let {
                Info("Прогноз", rub(it))
            }
        }

        if (selectedSalary) {
            Text(
                "ДЕНЬ ЗАРПЛАТЫ",
                color = mint,
                fontWeight = FontWeight.Bold
            )

            estimateForPayday(
                config, selected, "Зарплата"
            )?.let {
                Info("Предварительный прогноз", rub(it))
            }
        }

        val dayPayments = payments.filter {
            it.date == selected.toString()
        }

        if (dayPayments.isNotEmpty()) {
            HorizontalDivider()

            Text(
                "ФАКТИЧЕСКИ ПОЛУЧЕНО",
                color = mint,
                fontWeight = FontWeight.Bold
            )

            dayPayments.forEach { payment ->
                Info(
                    payment.type,
                    rub(payment.amount)
                )

                if (payment.forecast >= 0.0) {
                    Info(
                        "Прогноз при сохранении",
                        rub(payment.forecast)
                    )

                    Info(
                        "Разница",
                        rub(
                            payment.amount -
                                payment.forecast
                        )
                    )
                }
            }
        }

        HorizontalDivider()

        Text(
            "Назначить выбранную дату",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onConfigChange(
                        config.copy(
                            advanceDay =
                                selected.dayOfMonth.toString()
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Аванс")
            }

            OutlinedButton(
                onClick = {
                    onConfigChange(
                        config.copy(
                            salaryDay =
                                selected.dayOfMonth.toString()
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Зарплата")
            }
        }

        Text(
            "Число выплаты сохраняется в настройках. " +
                "Если оно приходится на нерабочий день, " +
                "отметка переносится на предыдущий " +
                "рабочий день.",
            color = muted,
            fontSize = 11.sp
        )

        HorizontalDivider()

        Text(
            "ЛИЧНЫЕ СОБЫТИЯ",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        events.filter {
            it.date == selected.toString()
        }.forEach { event ->
            Text(
                "• ${event.title}",
                color = mint
            )

            TextButton(
                onClick = {
                    onEventsChange(
                        events.filterNot { it == event }
                    )
                }
            ) {
                Text("Удалить")
            }
        }

        Field("Новое событие", eventTitle) {
            eventTitle = it
        }

        Button(
            onClick = {
                if (eventTitle.isNotBlank()) {
                    onEventsChange(
                        events + CalendarEvent(
                            selected.toString(),
                            eventTitle.trim()
                        )
                    )
                    eventTitle = ""
                }
            },
            enabled = eventTitle.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Добавить событие")
        }

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
private fun Finance(
    s: Config,
    today: LocalDate,
    earnings: Double,
    payments: List<Payment>,
    onChange: (List<Payment>) -> Unit
) {
    var date by remember {
        mutableStateOf(today.toString())
    }

    var amount by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf("Зарплата")
    }

    var editingIndex by remember {
        mutableIntStateOf(-1)
    }

    val selectedDate = parseDate(date) ?: today

    val forecast = estimateForPayday(
        s, selectedDate, type
    )

    Panel("НАЧИСЛЕНО ПО РАСЧЁТУ") {
        Text(
            rub(earnings),
            color = mint,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Ориентировочное начисление " +
                "за текущую вахту до удержаний.",
            color = muted,
            fontSize = 12.sp
        )
    }

    Panel(
        if (editingIndex >= 0)
            "ИЗМЕНИТЬ ВЫПЛАТУ"
        else
            "ДОБАВИТЬ ВЫПЛАТУ"
    ) {
        DateField("Дата поступления", date) {
            date = it
        }

        Field(
            "Фактически получено, ₽",
            amount,
            true
        ) {
            amount = it
        }

        Text(
            "Тип выплаты",
            color = Color.White
        )

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
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = type == option,
                    onClick = { type = option }
                )

                Text(
                    option,
                    color = Color.White
                )
            }
        }

        if (forecast != null) {
            Info(
                "Предварительный прогноз",
                rub(forecast)
            )
        } else {
            Text(
                "Для этого типа выплаты " +
                    "автоматический прогноз отсутствует.",
                color = muted,
                fontSize = 12.sp
            )
        }

        if (num(amount) > 0 && forecast != null) {
            val diff = num(amount) - forecast

            Info(
                "Расхождение",
                rub(diff)
            )

            Text(
                if (diff < 0)
                    "Получено меньше прогноза"
                else if (diff > 0)
                    "Получено больше прогноза"
                else
                    "Суммы совпадают",
                color = if (diff == 0.0) mint else gold
            )
        }

        Button(
            onClick = {
                val payment = Payment(
                    date = date,
                    amount = num(amount),
                    type = type,
                    forecast = forecast ?: -1.0
                )

                if (
                    editingIndex >= 0 &&
                    editingIndex < payments.size
                ) {
                    val updated = payments.toMutableList()
                    updated[editingIndex] = payment
                    onChange(updated)
                } else {
                    onChange(payments + payment)
                }

                editingIndex = -1
                amount = ""
            },
            enabled =
                num(amount) > 0 &&
                parseDate(date) != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (editingIndex >= 0)
                    "Сохранить изменения"
                else
                    "Сохранить выплату"
            )
        }
    }

    Panel("ИСТОРИЯ ВЫПЛАТ") {
        Info(
            "Всего получено",
            rub(payments.sumOf { it.amount })
        )

        if (payments.isEmpty()) {
            Text(
                "Пока нет записей",
                color = muted
            )
        }

        payments.indices
            .sortedByDescending { payments[it].date }
            .forEach { index ->
                val item = payments[index]

                HorizontalDivider()

                Text(
                    "${item.date} · ${item.type}",
                    color = muted
                )

                Text(
                    rub(item.amount),
                    color = mint,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (item.forecast >= 0.0) {
                    Info(
                        "Сохранённый прогноз",
                        rub(item.forecast)
                    )

                    val diff =
                        item.amount - item.forecast

                    Info(
                        "Расхождение",
                        rub(diff)
                    )

                    if (abs(diff) > 0.01) {
                        Text(
                            "Возможные причины: разница " +
                                "в оплаченных часах, премиях, " +
                                "надбавках или удержаниях. " +
                                "Для точной проверки нужен " +
                                "расчётный листок.",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }
                }

                Row {
                    TextButton(
                        onClick = {
                            editingIndex = index
                            date = item.date
                            amount = item.amount.toString()
                            type = item.type
                        }
                    ) {
                        Text("Изменить")
                    }

                    TextButton(
                        onClick = {
                            val updated =
                                payments.toMutableList()

                            updated.removeAt(index)
                            onChange(updated)

                            if (editingIndex == index) {
                                editingIndex = -1
                                amount = ""
                            }
                        }
                    ) {
                        Text("Удалить")
                    }
                }
            }
    }
}

@Composable
private fun Settings(
    s: Config,
    onChange: (Config) -> Unit,
    onLock: () -> Unit
) {
    Panel("ЛИЧНЫЕ ДАННЫЕ") {
        Field("Имя", s.name) {
            onChange(s.copy(name = it))
        }
    }

    Panel("ГРАФИК ВАХТ") {
        RotationSelect(s.rotation) {
            onChange(s.copy(rotation = it))
        }

        DateField("Первый заезд", s.arrival) {
            onChange(s.copy(arrival = it))
        }

        DateField("Первый выезд", s.departure) {
            onChange(s.copy(departure = it))
        }

        Field(
            "Рабочих дней подряд",
            s.workDays,
            true
        ) {
            onChange(s.copy(workDays = it))
        }

        Field(
            "Выходных дней подряд",
            s.restDays,
            true
        ) {
            onChange(s.copy(restDays = it))
        }

        val first = parseDate(s.arrival)
        val last = parseDate(s.departure)

        if (
            first != null &&
            last != null &&
            !last.isBefore(first)
        ) {
            Info(
                "Первая вахта",
                "${ChronoUnit.DAYS.between(first, last) + 1} дн."
            )

            if (s.rotation in rotations) {
                Info(
                    "Следующий заезд",
                    anchoredDate(
                        first,
                        rotationMonths(s.rotation).toLong()
                    ).pretty()
                )
            }
        }
    }

    Panel("ОПЛАТА ТРУДА") {
        Field("ЧТС, ₽/час", s.hourly, true) {
            onChange(s.copy(hourly = it))
        }

        Field("Часов в смене", s.hours, true) {
            onChange(s.copy(hours = it))
        }

        Field(
            "Районный коэффициент",
            s.coefficient,
            true
        ) {
            onChange(s.copy(coefficient = it))
        }

        Field(
            "Северная надбавка, %",
            s.north,
            true
        ) {
            onChange(s.copy(north = it))
        }

        Field(
            "Производственная премия, %",
            s.premium,
            true
        ) {
            onChange(s.copy(premium = it))
        }

        Field(
            "Вахтовая надбавка, ₽/день",
            s.allowance,
            true
        ) {
            onChange(s.copy(allowance = it))
        }

        Field(
            "Оплачиваемых часов дороги",
            s.travelHours,
            true
        ) {
            onChange(s.copy(travelHours = it))
        }
    }

    Panel("ДОПОЛНИТЕЛЬНЫЕ НАЧИСЛЕНИЯ") {
        Text(
            "Заполняй после получения " +
                "расчётного листка.",
            color = muted
        )

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

        Field(
            "Исполнительные удержания, ₽",
            s.enforcement,
            true
        ) {
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

    Panel("ВЫПЛАТЫ") {
        Field(
            "Число аванса",
            s.advanceDay,
            true
        ) {
            onChange(s.copy(advanceDay = it))
        }

        Field(
            "Число зарплаты",
            s.salaryDay,
            true
        ) {
            onChange(s.copy(salaryDay = it))
        }

        Text(
            "Порядок расчёта аванса",
            color = Color.White
        )

        listOf(
            "По отработанным часам",
            "Фиксированная сумма",
            "Другой порядок"
        ).forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onChange(
                            s.copy(advanceMethod = option)
                        )
                    },
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = s.advanceMethod == option,
                    onClick = {
                        onChange(
                            s.copy(advanceMethod = option)
                        )
                    }
                )

                Text(option, color = Color.White)
            }
        }

        if (
            s.advanceMethod ==
            "Фиксированная сумма"
        ) {
            Field(
                "Фиксированный аванс, ₽",
                s.fixedAdvance,
                true
            ) {
                onChange(s.copy(fixedAdvance = it))
            }
        }
    }

    Panel("НАПОМИНАНИЯ") {
        Text(
            "Включить напоминания?",
            color = Color.White
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = s.reminders == "Да",
                onClick = {
                    onChange(s.copy(reminders = "Да"))
                }
            )
            Text("Да", color = Color.White)

            Spacer(Modifier.width(16.dp))

            RadioButton(
                selected = s.reminders == "Нет",
                onClick = {
                    onChange(
                        s.copy(
                            reminders = "Нет",
                            remindArrival = false,
                            remindDeparture = false,
                            remindSalary = false,
                            remindAdvance = false,
                            remindEvents = false
                        )
                    )
                }
            )
            Text("Нет", color = Color.White)
        }

        if (s.reminders == "Да") {
            ReminderChoice(
                "Заезд",
                s.remindArrival
            ) {
                onChange(s.copy(remindArrival = it))
            }

            ReminderChoice(
                "Выезд",
                s.remindDeparture
            ) {
                onChange(s.copy(remindDeparture = it))
            }

            ReminderChoice(
                "Зарплата",
                s.remindSalary
            ) {
                onChange(s.copy(remindSalary = it))
            }

            ReminderChoice(
                "Аванс",
                s.remindAdvance
            ) {
                onChange(s.copy(remindAdvance = it))
            }

            ReminderChoice(
                "Личные события",
                s.remindEvents
            ) {
                onChange(s.copy(remindEvents = it))
            }

            Text(
                "Предупреждать",
                color = Color.White
            )

            listOf(
                "0" to "В день события",
                "1" to "За 1 день",
                "3" to "За 3 дня",
                "7" to "За 7 дней"
            ).forEach { (days, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onChange(
                                s.copy(remindDays = days)
                            )
                        },
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = s.remindDays == days,
                        onClick = {
                            onChange(
                                s.copy(remindDays = days)
                            )
                        }
                    )

                    Text(label, color = Color.White)
                }
            }

            Field(
                "Время (ЧЧ:ММ)",
                s.remindTime
            ) {
                onChange(s.copy(remindTime = it))
            }
        }

        Text(
            "Параметры сохраняются, но системные " +
                "уведомления Android ещё не подключены.",
            color = gold,
            fontSize = 12.sp
        )
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
private fun MonthHeader(
    month: YearMonth,
    previous: () -> Unit,
    next: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        IconButton(onClick = previous) {
            Text(
                "‹",
                fontSize = 30.sp,
                color = mint
            )
        }

        Text(
            month.format(
                DateTimeFormatter.ofPattern(
                    "LLLL yyyy", ru
                )
            ).replaceFirstChar {
                it.uppercase()
            },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = next) {
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
            "Пн", "Вт", "Ср",
            "Чт", "Пт", "Сб", "Вс"
        ).forEach { day ->
            Text(
                day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = muted
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
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "$label: ${
                selected?.pretty() ?: "Выбрать дату"
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
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    MonthHeader(
                        month,
                        { month = month.minusMonths(1) },
                        { month = month.plusMonths(1) }
                    )

                    WeekHeader()

                    val offset =
                        month.atDay(1).dayOfWeek.value - 1

                    val count = month.lengthOfMonth()

                    repeat(
                        (offset + count + 6) / 7
                    ) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val day =
                                    row * 7 + col - offset + 1

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .then(
                                            if (day in 1..count) {
                                                Modifier.clickable {
                                                    onChange(
                                                        month.atDay(day)
                                                            .toString()
                                                    )
                                                    open = false
                                                }
                                            } else Modifier
                                        ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    if (day in 1..count) {
                                        Text(
                                            "$day",
                                            color = if (
                                                month.atDay(day) ==
                                                selected
                                            ) mint else Color.White
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
                    onClick = { open = false }
                ) {
                    Text("Закрыть")
                }
            },
            containerColor = panel
        )
    }
}

@Composable
private fun Heading(
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
        color = muted,
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
            containerColor = panel
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                color = muted,
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
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            label,
            color = muted,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.End
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
            else
                KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
