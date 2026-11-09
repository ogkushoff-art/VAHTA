
package com.vahta.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val bg = Color(0xFF0B1728)
private val panel = Color(0xFF1A2D45)
private val accent = Color(0xFF56E4C0)
private val muted = Color(0xFFB4C6D9)
private val blue = Color(0xFF4389D8)
private val orange = Color(0xFFFFB45B)
private val ru = Locale.forLanguageTag("ru-RU")

private data class Payment(
    val date: String,
    val amount: Double,
    val type: String
)

private data class Shift(
    val arrival: LocalDate,
    val departure: LocalDate,
    val nextArrival: LocalDate,
    val working: Boolean,
    val progress: Float,
    val remaining: Long,
    val periodDays: Long
)

private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()

private fun LocalDate.pretty(): String =
    format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun number(value: String): Double =
    value.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

private fun monthlyDate(
    original: LocalDate,
    months: Long
): LocalDate {
    val month = YearMonth.from(original).plusMonths(months)
    return month.atDay(
        original.dayOfMonth.coerceAtMost(month.lengthOfMonth())
    )
}

private fun calculateShift(
    date: LocalDate,
    firstArrival: LocalDate,
    firstDeparture: LocalDate,
    intervalMonths: Int
): Shift {
    val interval = intervalMonths.coerceIn(1, 24).toLong()

    fun arrival(index: Long): LocalDate =
        monthlyDate(firstArrival, index * interval)

    fun departure(index: Long): LocalDate =
        monthlyDate(firstDeparture, index * interval)

    val monthDifference = ChronoUnit.MONTHS.between(
        YearMonth.from(firstArrival),
        YearMonth.from(date)
    )

    var index = Math.floorDiv(monthDifference, interval)

    while (date.isBefore(arrival(index))) index--
    while (!date.isBefore(arrival(index + 1))) index++

    val start = arrival(index)
    val end = departure(index)
    val next = arrival(index + 1)

    val working = !date.isAfter(end)
    val periodStart = if (working) start else end.plusDays(1)
    val periodEnd = if (working) end else next.minusDays(1)

    val total = (
        ChronoUnit.DAYS.between(periodStart, periodEnd) + 1
    ).coerceAtLeast(1)

    val elapsed = (
        ChronoUnit.DAYS.between(periodStart, date) + 1
    ).coerceIn(0, total)

    return Shift(
        arrival = start,
        departure = end,
        nextArrival = next,
        working = working,
        progress = elapsed.toFloat() / total.toFloat(),
        remaining = total - elapsed,
        periodDays = total
    )
}

private fun nextPaymentDate(
    today: LocalDate,
    day: Int
): LocalDate {
    val currentMonth = YearMonth.from(today)

    fun inMonth(month: YearMonth): LocalDate =
        month.atDay(day.coerceIn(1, month.lengthOfMonth()))

    val current = inMonth(currentMonth)
    return if (current.isBefore(today)) {
        inMonth(currentMonth.plusMonths(1))
    } else current
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "vahta_settings",
            MODE_PRIVATE
        )

        setContent {
            var unlocked by remember { mutableStateOf(false) }

            if (!unlocked) {
                SecurityScreen(
                    onUnlocked = { unlocked = true }
                )
            } else {
                var setupComplete by remember {
                    mutableStateOf(
                        prefs.getBoolean("setup_complete", false)
                    )
                }

                var name by remember {
                    mutableStateOf(
                        prefs.getString("name", "") ?: ""
                    )
                }

                var arrivalDate by remember {
                    mutableStateOf(
                        prefs.getString(
                            "arrival_date",
                            prefs.getString("start", "") ?: ""
                        ) ?: ""
                    )
                }

                var departureDate by remember {
                    mutableStateOf(
                        prefs.getString("departure_date", "") ?: ""
                    )
                }

                var interval by remember {
                    mutableStateOf(
                        prefs.getString("interval_months", "3") ?: "3"
                    )
                }

                var rate by remember {
                    mutableStateOf(
                        prefs.getString("hourly", "") ?: ""
                    )
                }

                var hours by remember {
                    mutableStateOf(
                        prefs.getString("hours", "") ?: ""
                    )
                }

                var coefficient by remember {
                    mutableStateOf(
                        prefs.getString("coefficient", "1") ?: "1"
                    )
                }

                var bonus by remember {
                    mutableStateOf(
                        prefs.getString("bonus", "0") ?: "0"
                    )
                }

                var tax by remember {
                    mutableStateOf(
                        prefs.getString("tax", "13") ?: "13"
                    )
                }

                var advance by remember {
                    mutableStateOf(
                        prefs.getString("advance", "") ?: ""
                    )
                }

                var salary by remember {
                    mutableStateOf(
                        prefs.getString("payday", "") ?: ""
                    )
                }

                var advanceRule by remember {
                    mutableStateOf(
                        prefs.getString(
                            "advance_rule",
                            "По отработанным часам"
                        ) ?: "По отработанным часам"
                    )
                }

                var fixedAdvance by remember {
                    mutableStateOf(
                        prefs.getString("fixed_advance", "") ?: ""
                    )
                }

                var intershift by remember {
                    mutableStateOf(
                        prefs.getString("intershift", "") ?: ""
                    )
                }

                var payNotes by remember {
                    mutableStateOf(
                        prefs.getString("pay_notes", "") ?: ""
                    )
                }

                // Перенос старого графика без удаления данных.
                LaunchedEffect(Unit) {
                    if (
                        departureDate.isBlank() &&
                        setupComplete &&
                        arrivalDate.isNotBlank()
                    ) {
                        val oldArrival = parseDate(arrivalDate)
                        val oldDepartureDay = prefs.getString(
                            "departure_day", ""
                        )?.toIntOrNull()

                        if (
                            oldArrival != null &&
                            oldDepartureDay != null
                        ) {
                            val month = YearMonth.from(
                                oldArrival
                            ).plusMonths(2)

                            departureDate = month.atDay(
                                oldDepartureDay.coerceIn(
                                    1, month.lengthOfMonth()
                                )
                            ).toString()

                            prefs.edit().putString(
                                "departure_date",
                                departureDate
                            ).apply()
                        }
                    }
                }

                fun save() {
                    prefs.edit()
                        .putString("name", name)
                        .putString("arrival_date", arrivalDate)
                        .putString("departure_date", departureDate)
                        .putString("interval_months", interval)
                        .putString("hourly", rate)
                        .putString("hours", hours)
                        .putString("coefficient", coefficient)
                        .putString("bonus", bonus)
                        .putString("tax", tax)
                        .putString("advance", advance)
                        .putString("payday", salary)
                        .putString("advance_rule", advanceRule)
                        .putString("fixed_advance", fixedAdvance)
                        .putString("intershift", intershift)
                        .putString("pay_notes", payNotes)
                        .apply()
                }

                if (!setupComplete) {
                    SetupWizard(
                        onFinish = { values ->
                            name = values[0]
                            arrivalDate = values[1]
                            departureDate = values[2]
                            interval = values[3]
                            rate = values[4]
                            hours = values[5]
                            coefficient = values[6]
                            bonus = values[7]
                            tax = values[8]
                            advance = values[9]
                            salary = values[10]
                            advanceRule = values[11]
                            fixedAdvance = values[12]
                            intershift = values[13]
                            payNotes = values[14]

                            save()

                            prefs.edit()
                                .putBoolean(
                                    "setup_complete", true
                                )
                                .apply()

                            setupComplete = true
                        }
                    )
                } else {
                    var tab by remember {
                        mutableIntStateOf(0)
                    }

                    var clock by remember {
                        mutableLongStateOf(
                            System.currentTimeMillis()
                        )
                    }

                    LaunchedEffect(Unit) {
                        while (true) {
                            clock = System.currentTimeMillis()
                            delay(1000)
                        }
                    }

                    val today = remember(clock / 1000) {
                        LocalDate.now()
                    }

                    val firstArrival = parseDate(arrivalDate)
                    val firstDeparture = parseDate(departureDate)

                    val months = (
                        interval.toIntOrNull() ?: 3
                    ).coerceIn(1, 24)

                    val validSchedule =
                        firstArrival != null &&
                        firstDeparture != null &&
                        firstDeparture.isAfter(firstArrival) &&
                        firstDeparture.isBefore(
                            monthlyDate(firstArrival, months)
                        )

                    val current = if (validSchedule) {
                        calculateShift(
                            today,
                            firstArrival!!,
                            firstDeparture!!,
                            months
                        )
                    } else null

                    val hourly = number(rate)
                    val dailyHours = number(hours)
                    val factor = number(coefficient)

                    val elapsedDays = if (current == null) {
                        0L
                    } else if (current.working) {
                        ChronoUnit.DAYS.between(
                            current.arrival, today
                        ).coerceAtLeast(0)
                    } else {
                        ChronoUnit.DAYS.between(
                            current.arrival,
                            current.departure
                        ) + 1
                    }

                    val estimatedGross =
                        elapsedDays * hourly * dailyHours * factor

                    val formatter = remember {
                        NumberFormat.getCurrencyInstance(ru)
                    }

                    var payDate by remember {
                        mutableStateOf(today.toString())
                    }

                    var payAmount by remember {
                        mutableStateOf("")
                    }

                    var payType by remember {
                        mutableStateOf("Зарплата")
                    }

                    var payments by remember {
                        mutableStateOf(
                            loadPayments(
                                prefs.getString("payments", "[]")
                                    ?: "[]"
                            )
                        )
                    }

                    fun savePayments() {
                        prefs.edit()
                            .putString(
                                "payments",
                                encodePayments(payments)
                            )
                            .apply()
                    }

                    MaterialTheme(
                        colorScheme = darkColorScheme(
                            primary = accent,
                            background = bg,
                            surface = panel,
                            onSurface = Color.White,
                            onBackground = Color.White
                        )
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(bg)
                        ) {
                            val bodyModifier =
                                if (tab == 3) {
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                } else {
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(
                                            rememberScrollState()
                                        )
                                }

                            Column(
                                modifier = bodyModifier
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
                                    if (name.isBlank())
                                        "Добро пожаловать!"
                                    else "Привет, ${name.trim()}!",
                                    color = muted,
                                    fontSize = 18.sp
                                )

                                when (tab) {
                                    0 -> {
                                        AppPanel("МОЙ ГРАФИК") {
                                            if (current == null) {
                                                Text(
                                                    "Необходимо настроить даты заезда и выезда.",
                                                    color = orange
                                                )

                                                Button(
                                                    onClick = {
                                                        tab = 4
                                                    }
                                                ) {
                                                    Text(
                                                        "Настроить график"
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    if (current.working)
                                                        "НА ВАХТЕ"
                                                    else "НА МЕЖВАХТЕ",
                                                    color =
                                                        if (current.working)
                                                            accent
                                                        else blue,
                                                    fontWeight =
                                                        FontWeight.Bold
                                                )

                                                Text(
                                                    if (current.working)
                                                        "До выезда домой: ${current.remaining} дн."
                                                    else
                                                        "До заезда: ${current.remaining} дн.",
                                                    color = Color.White,
                                                    fontSize = 22.sp
                                                )

                                                LinearProgressIndicator(
                                                    progress = {
                                                        current.progress
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(10.dp),
                                                    color =
                                                        if (current.working)
                                                            accent
                                                        else blue,
                                                    trackColor =
                                                        Color(0xFF354C65)
                                                )

                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement =
                                                        Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "Пройдено ${(current.progress * 100).toInt()}%",
                                                        color = muted,
                                                        fontSize = 12.sp
                                                    )

                                                    Text(
                                                        "${current.periodDays} дней",
                                                        color = muted,
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                Text(
                                                    "Заезд: ${current.arrival.pretty()}",
                                                    color = muted
                                                )

                                                Text(
                                                    "Домой: ${current.departure.pretty()} 🎉",
                                                    color = orange
                                                )

                                                Text(
                                                    "Следующий заезд: ${current.nextArrival.pretty()}",
                                                    color = muted
                                                )
                                            }
                                        }

                                        AppPanel("РАСЧЁТНЫЙ ЗАРАБОТОК") {
                                            Text(
                                                formatter.format(
                                                    estimatedGross
                                                ),
                                                color = accent,
                                                fontSize = 27.sp,
                                                fontWeight =
                                                    FontWeight.Bold
                                            )

                                            Text(
                                                "Предварительный расчёт за завершённые календарные дни. Без учёта выходных, фактических часов, налогов и премий.",
                                                color = muted,
                                                fontSize = 12.sp
                                            )
                                        }

                                        AppPanel("БЛИЖАЙШАЯ ВЫПЛАТА") {
                                            val salaryDay =
                                                salary.toIntOrNull()

                                            val advanceDay =
                                                advance.toIntOrNull()

                                            val options =
                                                mutableListOf<
                                                    Pair<String, LocalDate>
                                                >()

                                            if (
                                                salaryDay != null &&
                                                salaryDay in 1..31
                                            ) {
                                                options.add(
                                                    "Зарплата" to
                                                        nextPaymentDate(
                                                            today,
                                                            salaryDay
                                                        )
                                                )
                                            }

                                            if (
                                                advanceDay != null &&
                                                advanceDay in 1..31
                                            ) {
                                                options.add(
                                                    "Аванс" to
                                                        nextPaymentDate(
                                                            today,
                                                            advanceDay
                                                        )
                                                )
                                            }

                                            val nearest =
                                                options.minByOrNull {
                                                    it.second
                                                }

                                            if (nearest != null) {
                                                Text(
                                                    "${nearest.first} · ${nearest.second.pretty()}",
                                                    color = Color.White,
                                                    fontSize = 19.sp
                                                )
                                            } else {
                                                Text(
                                                    "Даты выплат не указаны",
                                                    color = orange
                                                )
                                            }

                                            Text(
                                                "Расчёт суммы к получению пока не настроен.",
                                                color = muted,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Button(
                                            onClick = { tab = 1 },
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        ) {
                                            Text("Открыть календарь")
                                        }
                                    }

                                    1 -> {
                                        if (
                                            firstArrival != null &&
                                            firstDeparture != null &&
                                            validSchedule
                                        ) {
                                            ShiftCalendar(
                                                today = today,
                                                firstArrival = firstArrival,
                                                firstDeparture = firstDeparture,
                                                intervalMonths = months
                                            )
                                        } else {
                                            AppPanel("КАЛЕНДАРЬ") {
                                                Text(
                                                    "Сначала выбери даты заезда и выезда в настройках.",
                                                    color = muted
                                                )
                                            }
                                        }
                                    }

                                    2 -> {
                                        AppPanel("НАЧИСЛЕНИЯ") {
                                            Text(
                                                "Предварительно начислено",
                                                color = muted
                                            )

                                            Text(
                                                formatter.format(
                                                    estimatedGross
                                                ),
                                                color = accent,
                                                fontSize = 25.sp
                                            )
                                        }

                                        AppPanel("ДОБАВИТЬ ВЫПЛАТУ") {
                                            AppDateField(
                                                "Дата получения",
                                                payDate
                                            ) {
                                                payDate = it
                                            }

                                            AppField(
                                                "Получено, ₽",
                                                payAmount,
                                                true
                                            ) {
                                                payAmount = it
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
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            payType = option
                                                        },
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected =
                                                            payType == option,
                                                        onClick = {
                                                            payType = option
                                                        }
                                                    )

                                                    Text(
                                                        option,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            val value =
                                                number(payAmount)

                                            Button(
                                                onClick = {
                                                    payments =
                                                        payments + Payment(
                                                            payDate,
                                                            value,
                                                            payType
                                                        )
                                                    savePayments()
                                                    payAmount = ""
                                                },
                                                enabled = value > 0,
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "Сохранить выплату"
                                                )
                                            }
                                        }

                                        AppPanel("ИСТОРИЯ ВЫПЛАТ") {
                                            if (payments.isEmpty()) {
                                                Text(
                                                    "Пока нет записей",
                                                    color = muted
                                                )
                                            }

                                            payments
                                                .sortedByDescending {
                                                    it.date
                                                }
                                                .forEach { item ->
                                                    HorizontalDivider()

                                                    Text(
                                                        "${item.date} · ${item.type}",
                                                        color = muted
                                                    )

                                                    Text(
                                                        formatter.format(
                                                            item.amount
                                                        ),
                                                        color = accent,
                                                        fontSize = 19.sp
                                                    )

                                                    TextButton(
                                                        onClick = {
                                                            val index =
                                                                payments.indexOf(
                                                                    item
                                                                )

                                                            if (index >= 0) {
                                                                payments =
                                                                    payments
                                                                        .toMutableList()
                                                                        .also {
                                                                            it.removeAt(
                                                                                index
                                                                            )
                                                                        }
                                                                savePayments()
                                                            }
                                                        }
                                                    ) {
                                                        Text("Удалить")
                                                    }
                                                }
                                        }
                                    }

                                    3 -> {
                                        OrganizerScreen(
                                            this@MainActivity
                                        )
                                    }

                                    4 -> {
                                        AppPanel("ЛИЧНЫЕ ДАННЫЕ") {
                                            AppField(
                                                "Имя",
                                                name
                                            ) {
                                                name = it
                                                save()
                                            }
                                        }

                                        AppPanel("ГРАФИК ВАХТ") {
                                            AppDateField(
                                                "Дата заезда",
                                                arrivalDate
                                            ) {
                                                arrivalDate = it
                                                save()
                                            }

                                            AppDateField(
                                                "Дата выезда домой",
                                                departureDate
                                            ) {
                                                departureDate = it
                                                save()
                                            }

                                            AppField(
                                                "Интервал между заездами, месяцев",
                                                interval,
                                                true
                                            ) {
                                                interval = it
                                                save()
                                            }

                                            Text(
                                                "Заезд и выезд выбираются отдельно. Повторение графика рассчитывается по календарным месяцам.",
                                                color = muted,
                                                fontSize = 12.sp
                                            )

                                            if (!validSchedule) {
                                                Text(
                                                    "Проверь даты: выезд должен быть позже заезда и раньше следующего заезда.",
                                                    color = orange,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        AppPanel("ЗАРАБОТНАЯ ПЛАТА") {
                                            AppField(
                                                "ЧТС, ₽/час",
                                                rate,
                                                true
                                            ) {
                                                rate = it
                                                save()
                                            }

                                            AppField(
                                                "Часов в смене",
                                                hours,
                                                true
                                            ) {
                                                hours = it
                                                save()
                                            }

                                            AppField(
                                                "Районный коэффициент",
                                                coefficient,
                                                true
                                            ) {
                                                coefficient = it
                                                save()
                                            }

                                            AppField(
                                                "Надбавки и премии, %",
                                                bonus,
                                                true
                                            ) {
                                                bonus = it
                                                save()
                                            }

                                            AppField(
                                                "НДФЛ, %",
                                                tax,
                                                true
                                            ) {
                                                tax = it
                                                save()
                                            }
                                        }

                                        AppPanel("ВЫПЛАТЫ") {
                                            AppField(
                                                "Число аванса",
                                                advance,
                                                true
                                            ) {
                                                advance = it
                                                save()
                                            }

                                            AppField(
                                                "Число зарплаты",
                                                salary,
                                                true
                                            ) {
                                                salary = it
                                                save()
                                            }

                                            Text(
                                                "Порядок расчёта аванса",
                                                color = muted
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
                                                            advanceRule = option
                                                            save()
                                                        },
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected =
                                                            advanceRule == option,
                                                        onClick = {
                                                            advanceRule = option
                                                            save()
                                                        }
                                                    )

                                                    Text(
                                                        option,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            if (
                                                advanceRule ==
                                                "Фиксированная сумма"
                                            ) {
                                                AppField(
                                                    "Сумма аванса, ₽",
                                                    fixedAdvance,
                                                    true
                                                ) {
                                                    fixedAdvance = it
                                                    save()
                                                }
                                            }

                                            AppField(
                                                "Межвахтовые выплаты, ₽",
                                                intershift,
                                                true
                                            ) {
                                                intershift = it
                                                save()
                                            }

                                            AppField(
                                                "Дополнительные условия",
                                                payNotes
                                            ) {
                                                payNotes = it
                                                save()
                                            }
                                        }

                                        AppPanel("БЕЗОПАСНОСТЬ") {
                                            OutlinedButton(
                                                onClick = {
                                                    unlocked = false
                                                },
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "Заблокировать приложение"
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            NavigationBar(
                                containerColor = panel
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
                                                    "⌂", "▦", "₽", "☑", "⚙"
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
                }
            }
        }
    }
}

@Composable
private fun SetupWizard(
    onFinish: (List<String>) -> Unit
) {
    var step by remember {
        mutableIntStateOf(0)
    }

    var name by remember { mutableStateOf("") }
    var arrival by remember { mutableStateOf("") }
    var departure by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("3") }

    var rate by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var coefficient by remember { mutableStateOf("1") }
    var bonus by remember { mutableStateOf("0") }
    var tax by remember { mutableStateOf("13") }

    var advance by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }

    var rule by remember {
        mutableStateOf("По отработанным часам")
    }

    var fixed by remember { mutableStateOf("") }
    var intershift by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val first = parseDate(arrival)
    val last = parseDate(departure)
    val months = interval.toIntOrNull()

    val valid = when (step) {
        0 -> name.trim().isNotEmpty()

        1 -> first != null &&
            last != null &&
            last.isAfter(first) &&
            months != null &&
            months in 1..24 &&
            last.isBefore(
                monthlyDate(first, months)
            )

        2 -> number(rate) > 0 &&
            number(hours) > 0 &&
            number(hours) <= 24 &&
            number(coefficient) > 0 &&
            number(tax) <= 100

        3 -> {
            val a = advance.toIntOrNull()
            val s = salary.toIntOrNull()

            a != null && a in 1..31 &&
                s != null && s in 1..31 &&
                (
                    rule != "Фиксированная сумма" ||
                    number(fixed) > 0
                )
        }

        else -> true
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            background = bg,
            surface = panel,
            onSurface = Color.White,
            onBackground = Color.White
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(20.dp)
        ) {
            Text(
                "ВАХТА",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                "ПЕРВОНАЧАЛЬНАЯ НАСТРОЙКА",
                color = accent,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { (step + 1) / 5f },
                modifier = Modifier.fillMaxWidth(),
                color = accent
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Шаг ${step + 1} из 5",
                color = muted
            )

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    0 -> {
                        WizardTitle(
                            "Давай знакомиться",
                            "Как тебя зовут?"
                        )

                        AppField(
                            "Твоё имя",
                            name
                        ) {
                            name = it
                        }
                    }

                    1 -> {
                        WizardTitle(
                            "Твой график",
                            "Выбери две реальные даты в календаре."
                        )

                        AppDateField(
                            "Дата заезда на вахту",
                            arrival
                        ) {
                            arrival = it
                        }

                        AppDateField(
                            "Дата выезда домой",
                            departure
                        ) {
                            departure = it
                        }

                        AppField(
                            "Интервал между заездами, месяцев",
                            interval,
                            true
                        ) {
                            interval = it
                        }

                        if (first != null && last != null) {
                            if (last.isAfter(first)) {
                                val days =
                                    ChronoUnit.DAYS.between(
                                        first, last
                                    ) + 1

                                Text(
                                    "Продолжительность вахты: $days дн.",
                                    color = accent
                                )
                            } else {
                                Text(
                                    "Выезд должен быть позже заезда.",
                                    color = orange
                                )
                            }
                        }

                        Text(
                            "Даты не заданы заранее. Каждый пользователь выбирает свой график.",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }

                    2 -> {
                        WizardTitle(
                            "Работа и заработок",
                            "Заполни параметры начисления зарплаты."
                        )

                        AppField(
                            "ЧТС, ₽/час",
                            rate,
                            true
                        ) {
                            rate = it
                        }

                        AppField(
                            "Часов в смене",
                            hours,
                            true
                        ) {
                            hours = it
                        }

                        AppField(
                            "Районный коэффициент",
                            coefficient,
                            true
                        ) {
                            coefficient = it
                        }

                        AppField(
                            "Надбавки и премии, %",
                            bonus,
                            true
                        ) {
                            bonus = it
                        }

                        AppField(
                            "НДФЛ, %",
                            tax,
                            true
                        ) {
                            tax = it
                        }
                    }

                    3 -> {
                        WizardTitle(
                            "Даты выплат",
                            "Когда и по каким правилам приходят деньги?"
                        )

                        AppField(
                            "Число аванса",
                            advance,
                            true
                        ) {
                            advance = it
                        }

                        AppField(
                            "Число зарплаты",
                            salary,
                            true
                        ) {
                            salary = it
                        }

                        Text(
                            "Порядок расчёта аванса",
                            color = muted
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
                                        rule = option
                                    },
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = rule == option,
                                    onClick = {
                                        rule = option
                                    }
                                )

                                Text(
                                    option,
                                    color = Color.White
                                )
                            }
                        }

                        if (
                            rule == "Фиксированная сумма"
                        ) {
                            AppField(
                                "Сумма аванса, ₽",
                                fixed,
                                true
                            ) {
                                fixed = it
                            }
                        }

                        AppField(
                            "Межвахтовые выплаты, ₽",
                            intershift,
                            true
                        ) {
                            intershift = it
                        }

                        AppField(
                            "Дополнительные условия начислений",
                            notes
                        ) {
                            notes = it
                        }
                    }

                    4 -> {
                        WizardTitle(
                            "Проверь настройки",
                            "Всё можно изменить позже."
                        )

                        AppPanel("ЛИЧНЫЕ ДАННЫЕ") {
                            Text(
                                "Имя: $name",
                                color = Color.White
                            )

                            Text(
                                "Заезд: ${first?.pretty() ?: "—"}",
                                color = accent
                            )

                            Text(
                                "Выезд домой: ${last?.pretty() ?: "—"} 🎉",
                                color = orange
                            )

                            Text(
                                "Интервал: $interval мес.",
                                color = muted
                            )

                            Text(
                                "ЧТС: $rate ₽/час",
                                color = muted
                            )

                            Text(
                                "Часов в смене: $hours",
                                color = muted
                            )

                            Text(
                                "Аванс: $advance числа",
                                color = muted
                            )

                            Text(
                                "Зарплата: $salary числа",
                                color = muted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = {
                            step--
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Назад")
                    }
                }

                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            onFinish(
                                listOf(
                                    name.trim(),
                                    arrival,
                                    departure,
                                    interval,
                                    rate,
                                    hours,
                                    coefficient,
                                    bonus,
                                    tax,
                                    advance,
                                    salary,
                                    rule,
                                    fixed,
                                    intershift,
                                    notes
                                )
                            )
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (step == 4)
                            "Начать работу"
                        else "Продолжить"
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardTitle(
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
private fun ShiftCalendar(
    today: LocalDate,
    firstArrival: LocalDate,
    firstDeparture: LocalDate,
    intervalMonths: Int
) {
    var month by remember {
        mutableStateOf(
            YearMonth.from(today)
        )
    }

    var selected by remember {
        mutableStateOf(today)
    }

    AppPanel("КАЛЕНДАРЬ ВАХТ") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    month = month.minusMonths(1)
                }
            ) {
                Text(
                    "‹",
                    fontSize = 30.sp,
                    color = accent
                )
            }

            Text(
                month.format(
                    DateTimeFormatter.ofPattern(
                        "LLLL yyyy",
                        ru
                    )
                ).replaceFirstChar {
                    it.uppercase()
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    month = month.plusMonths(1)
                }
            ) {
                Text(
                    "›",
                    fontSize = 30.sp,
                    color = accent
                )
            }
        }

        Row(
            Modifier.fillMaxWidth()
        ) {
            listOf(
                "Пн", "Вт", "Ср", "Чт",
                "Пт", "Сб", "Вс"
            ).forEach { day ->
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day,
                        color = muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        val offset =
            month.atDay(1).dayOfWeek.value - 1

        val count = month.lengthOfMonth()
        val rows = (offset + count + 6) / 7

        repeat(rows) { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { col ->
                    val day =
                        row * 7 + col - offset + 1

                    if (day in 1..count) {
                        val date = month.atDay(day)

                        val state = calculateShift(
                            date,
                            firstArrival,
                            firstDeparture,
                            intervalMonths
                        )

                        val isArrival =
                            date == state.arrival

                        val isDeparture =
                            date == state.departure

                        val cellColor = when {
                            isDeparture -> orange
                            isArrival -> Color(0xFF278E7A)
                            state.working ->
                                Color(0xFF204F4A)
                            else ->
                                Color(0xFF233F65)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(
                                    cellColor,
                                    RoundedCornerShape(10.dp)
                                )
                                .then(
                                    if (date == selected) {
                                        Modifier.border(
                                            2.dp,
                                            Color.White,
                                            RoundedCornerShape(10.dp)
                                        )
                                    } else Modifier
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
                                    day.toString(),
                                    color = if (isDeparture)
                                        bg else Color.White,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    when {
                                        isDeparture -> "🎉"
                                        isArrival -> "●"
                                        date == today -> "•"
                                        else -> " "
                                    },
                                    color = accent,
                                    fontSize = 11.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Legend(accent, "Вахта")
            Legend(blue, "Отдых")
            Legend(orange, "Домой 🎉")
        }

        HorizontalDivider(
            color = Color(0xFF36506B)
        )

        val state = calculateShift(
            selected,
            firstArrival,
            firstDeparture,
            intervalMonths
        )

        Text(
            selected.pretty(),
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            when {
                selected == state.departure ->
                    "🎉 День выезда домой!"
                selected == state.arrival ->
                    "🚀 День заезда на вахту!"
                state.working ->
                    "На вахте. До выезда: ${state.remaining} дн."
                else ->
                    "Межвахтовый отдых. До заезда: ${state.remaining} дн."
            },
            color = if (
                selected == state.departure
            ) orange else accent
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
private fun Legend(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, CircleShape)
        )

        Text(
            label,
            color = muted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun AppDateField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    var open by remember {
        mutableStateOf(false)
    }

    val selectedDate = parseDate(value)

    OutlinedButton(
        onClick = {
            open = true
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (selectedDate == null)
                "$label: Выбрать дату"
            else
                "$label: ${selectedDate.pretty()}"
        )
    }

    if (open) {
        var month by remember(open) {
            mutableStateOf(
                YearMonth.from(
                    selectedDate ?: LocalDate.now()
                )
            )
        }

        AlertDialog(
            onDismissRequest = {
                open = false
            },
            title = {
                Text("Выберите дату")
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                month = month.minusMonths(1)
                            }
                        ) {
                            Text(
                                "‹",
                                fontSize = 25.sp
                            )
                        }

                        Text(
                            month.format(
                                DateTimeFormatter.ofPattern(
                                    "LLLL yyyy",
                                    ru
                                )
                            ).replaceFirstChar {
                                it.uppercase()
                            },
                            color = Color.White
                        )

                        TextButton(
                            onClick = {
                                month = month.plusMonths(1)
                            }
                        ) {
                            Text(
                                "›",
                                fontSize = 25.sp
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "Пн", "Вт", "Ср", "Чт",
                            "Пт", "Сб", "Вс"
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

                    val offset =
                        month.atDay(1).dayOfWeek.value - 1

                    val count =
                        month.lengthOfMonth()

                    val rows =
                        (offset + count + 6) / 7

                    repeat(rows) { row ->
                        Row(
                            Modifier.fillMaxWidth()
                        ) {
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
                                            day.toString(),
                                            color =
                                                if (
                                                    month.atDay(day) ==
                                                    selectedDate
                                                ) accent
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
                TextButton(
                    onClick = {
                        open = false
                    }
                ) {
                    Text("Закрыть")
                }
            },
            containerColor = panel
        )
    }
}

@Composable
private fun AppPanel(
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
private fun AppField(
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
            keyboardType = if (numeric)
                KeyboardType.Decimal
            else KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun loadPayments(
    raw: String
): List<Payment> = runCatching {
    val array = JSONArray(raw)

    (0 until array.length()).map { index ->
        val obj = array.getJSONObject(index)

        Payment(
            obj.getString("date"),
            obj.getDouble("amount"),
            obj.getString("type")
        )
    }
}.getOrDefault(emptyList())

private fun encodePayments(
    payments: List<Payment>
): String {
    val array = JSONArray()

    payments.forEach { payment ->
        array.put(
            JSONObject()
                .put("date", payment.date)
                .put("amount", payment.amount)
                .put("type", payment.type)
        )
    }

    return array.toString()
}
