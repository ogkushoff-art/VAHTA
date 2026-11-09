
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
private val restColor = Color(0xFF4389D8)
private val holiday = Color(0xFFFFB45B)

private val ru = Locale.forLanguageTag("ru-RU")
private val money = NumberFormat.getCurrencyInstance(ru)

private data class Payment(
    val date: String,
    val amount: Double,
    val type: String
)

private data class Schedule(
    val arrival: LocalDate,
    val departure: LocalDate,
    val nextArrival: LocalDate,
    val working: Boolean,
    val progress: Float,
    val remaining: Long,
    val periodDays: Long
)

private fun dateInMonth(
    month: YearMonth,
    day: Int
): LocalDate = month.atDay(
    day.coerceIn(1, month.lengthOfMonth())
)

private fun scheduleFor(
    date: LocalDate,
    anchor: LocalDate,
    arrivalDay: Int,
    departureDay: Int
): Schedule {
    val anchorMonth = YearMonth.from(anchor)
    val monthDifference =
        ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(date))

    var index = Math.floorDiv(monthDifference, 3L)

    fun arrival(i: Long): LocalDate =
        dateInMonth(anchorMonth.plusMonths(i * 3), arrivalDay)

    while (date.isBefore(arrival(index))) index--
    while (!date.isBefore(arrival(index + 1))) index++

    val start = arrival(index)
    val end = dateInMonth(
        anchorMonth.plusMonths(index * 3 + 2),
        departureDay
    )
    val next = arrival(index + 1)

    // День выезда относится к вахте.
    val working = !date.isAfter(end)
    val periodStart = if (working) start else end.plusDays(1)
    val periodEnd = if (working) end else next.minusDays(1)

    val total = ChronoUnit.DAYS.between(
        periodStart, periodEnd
    ) + 1

    val passed = ChronoUnit.DAYS.between(
        periodStart, date
    ) + 1

    return Schedule(
        arrival = start,
        departure = end,
        nextArrival = next,
        working = working,
        progress = (passed.toFloat() / total.toFloat())
            .coerceIn(0f, 1f),
        remaining = (total - passed).coerceAtLeast(0),
        periodDays = total
    )
}

private fun LocalDate.pretty(): String =
    format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun parseDate(value: String): LocalDate =
    runCatching { LocalDate.parse(value) }
        .getOrElse { LocalDate.now() }

private fun number(value: String): Double =
    value.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0 } ?: 0.0

private fun paymentDate(
    today: LocalDate,
    day: Int
): LocalDate {
    val month = YearMonth.from(today)
    val current = dateInMonth(month, day)
    return if (current.isBefore(today)) {
        dateInMonth(month.plusMonths(1), day)
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
                SecurityScreen(onUnlocked = { unlocked = true })
            } else {
                var setupDone by remember {
                    mutableStateOf(
                        prefs.getBoolean("setup_complete", false)
                    )
                }

                var name by remember {
                    mutableStateOf(
                        prefs.getString("name", "") ?: ""
                    )
                }
                var anchor by remember {
                    mutableStateOf(
                        prefs.getString("start", "2026-10-25")
                            ?: "2026-10-25"
                    )
                }
                var arrivalDay by remember {
                    mutableStateOf(
                        prefs.getString("arrival_day", "25") ?: "25"
                    )
                }
                var departureDay by remember {
                    mutableStateOf(
                        prefs.getString("departure_day", "10") ?: "10"
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
                var tax by remember {
                    mutableStateOf(
                        prefs.getString("tax", "13") ?: "13"
                    )
                }
                var bonus by remember {
                    mutableStateOf(
                        prefs.getString("bonus", "0") ?: "0"
                    )
                }
                var coefficient by remember {
                    mutableStateOf(
                        prefs.getString("coefficient", "1") ?: "1"
                    )
                }
                var advance by remember {
                    mutableStateOf(
                        prefs.getString("advance", "30") ?: "30"
                    )
                }
                var salary by remember {
                    mutableStateOf(
                        prefs.getString("payday", "15") ?: "15"
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

                fun saveSettings() {
                    prefs.edit()
                        .putString("name", name)
                        .putString("start", anchor)
                        .putString("arrival_day", arrivalDay)
                        .putString("departure_day", departureDay)
                        .putString("hourly", rate)
                        .putString("hours", hours)
                        .putString("tax", tax)
                        .putString("bonus", bonus)
                        .putString("coefficient", coefficient)
                        .putString("advance", advance)
                        .putString("payday", salary)
                        .putString("advance_rule", advanceRule)
                        .putString("fixed_advance", fixedAdvance)
                        .putString("intershift", intershift)
                        .putString("pay_notes", payNotes)
                        .apply()
                }

                if (!setupDone) {
                    SetupWizard(
                        initialName = name,
                        initialAnchor = anchor,
                        initialArrival = arrivalDay,
                        initialDeparture = departureDay,
                        initialRate = rate,
                        initialHours = hours,
                        initialTax = tax,
                        initialBonus = bonus,
                        initialCoefficient = coefficient,
                        initialAdvance = advance,
                        initialSalary = salary,
                        initialAdvanceRule = advanceRule,
                        initialFixedAdvance = fixedAdvance,
                        initialIntershift = intershift,
                        initialNotes = payNotes,
                        onFinish = { values ->
                            name = values[0]
                            anchor = values[1]
                            arrivalDay = values[2]
                            departureDay = values[3]
                            rate = values[4]
                            hours = values[5]
                            tax = values[6]
                            bonus = values[7]
                            coefficient = values[8]
                            advance = values[9]
                            salary = values[10]
                            advanceRule = values[11]
                            fixedAdvance = values[12]
                            intershift = values[13]
                            payNotes = values[14]
                            saveSettings()
                            prefs.edit()
                                .putBoolean("setup_complete", true)
                                .apply()
                            setupDone = true
                        }
                    )
                } else {
                    var tab by remember { mutableIntStateOf(0) }
                    var now by remember {
                        mutableLongStateOf(System.currentTimeMillis())
                    }

                    LaunchedEffect(Unit) {
                        while (true) {
                            now = System.currentTimeMillis()
                            delay(1000)
                        }
                    }

                    val today = LocalDate.now()
                    val baseDate = parseDate(anchor)
                    val inDay = arrivalDay.toIntOrNull()
                        ?.coerceIn(1, 31) ?: 25
                    val outDay = departureDay.toIntOrNull()
                        ?.coerceIn(1, 31) ?: 10

                    val current = scheduleFor(
                        today, baseDate, inDay, outDay
                    )

                    val grossPerDay =
                        number(rate) * number(hours) *
                            number(coefficient)

                    val shiftDays = ChronoUnit.DAYS.between(
                        current.arrival,
                        current.departure
                    ) + 1

                    val completedDays =
                        if (current.working) {
                            ChronoUnit.DAYS.between(
                                current.arrival, today
                            ).coerceAtLeast(0)
                        } else {
                            shiftDays
                        }

                    val fraction = if (current.working) {
                        LocalTime.now().toSecondOfDay() / 86400.0
                    } else 0.0

                    val estimatedGross =
                        (completedDays + fraction) * grossPerDay

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

                    fun savePaymentsNow() {
                        prefs.edit().putString(
                            "payments",
                            encodePayments(payments)
                        ).apply()
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
                            Modifier.fillMaxSize().background(bg)
                        ) {
                            val bodyModifier =
                                if (tab == 3) {
                                    Modifier.weight(1f).fillMaxWidth()
                                } else {
                                    Modifier.weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(
                                            rememberScrollState()
                                        )
                                }

                            Column(
                                modifier = bodyModifier.padding(16.dp),
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
                                    "Привет, ${name.trim()}!",
                                    color = muted,
                                    fontSize = 17.sp
                                )

                                when (tab) {
                                    0 -> {
                                        AppPanel("МОЙ ГРАФИК") {
                                            Text(
                                                if (current.working)
                                                    "НА ВАХТЕ"
                                                else "НА МЕЖВАХТЕ",
                                                color = if (current.working)
                                                    accent else restColor,
                                                fontWeight = FontWeight.Bold
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
                                                color = if (current.working)
                                                    accent else restColor,
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
                                                    color = muted
                                                )
                                                Text(
                                                    "${current.periodDays} дней",
                                                    color = muted
                                                )
                                            }

                                            Text(
                                                "Заезд: ${current.arrival.pretty()}",
                                                color = muted
                                            )
                                            Text(
                                                "Домой: ${current.departure.pretty()} 🎉",
                                                color = holiday
                                            )
                                            Text(
                                                "Следующий заезд: ${current.nextArrival.pretty()}",
                                                color = muted
                                            )
                                        }

                                        AppPanel("РАСЧЁТНЫЙ ЗАРАБОТОК") {
                                            Text(
                                                money.format(estimatedGross),
                                                color = accent,
                                                fontSize = 27.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Ориентировочное начисление по ЧТС и часам. Не является суммой к выплате.",
                                                color = muted,
                                                fontSize = 12.sp
                                            )
                                        }

                                        val nextSalary = paymentDate(
                                            today,
                                            salary.toIntOrNull()
                                                ?.coerceIn(1, 31) ?: 15
                                        )
                                        val nextAdvance = paymentDate(
                                            today,
                                            advance.toIntOrNull()
                                                ?.coerceIn(1, 31) ?: 30
                                        )

                                        AppPanel("БЛИЖАЙШАЯ ВЫПЛАТА") {
                                            val nearest =
                                                if (nextAdvance.isBefore(nextSalary))
                                                    "Аванс" to nextAdvance
                                                else "Зарплата" to nextSalary

                                            Text(
                                                "${nearest.first} · ${nearest.second.pretty()}",
                                                color = Color.White,
                                                fontSize = 19.sp
                                            )
                                            Text(
                                                "Сумма к выплате пока не рассчитана",
                                                color = holiday
                                            )
                                            Text(
                                                "Для точного прогноза необходимо настроить порядок начисления и удержаний.",
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
                                    }

                                    1 -> {
                                        ShiftCalendar(
                                            today = today,
                                            anchor = baseDate,
                                            arrivalDay = inDay,
                                            departureDay = outDay
                                        )
                                    }

                                    2 -> {
                                        AppPanel("НАЧИСЛЕНИЯ") {
                                            Text(
                                                "Предварительно начислено",
                                                color = muted
                                            )
                                            Text(
                                                money.format(estimatedGross),
                                                color = accent,
                                                fontSize = 25.sp
                                            )
                                            Text(
                                                "Расчёт без премий, налогов и прочих удержаний",
                                                color = muted,
                                                fontSize = 12.sp
                                            )
                                        }

                                        AppPanel("ДОБАВИТЬ ВЫПЛАТУ") {
                                            AppDateField(
                                                "Дата получения",
                                                payDate
                                            ) { payDate = it }

                                            AppField(
                                                "Получено, ₽",
                                                payAmount,
                                                true
                                            ) { payAmount = it }

                                            listOf(
                                                "Зарплата",
                                                "Аванс",
                                                "Межвахта",
                                                "Смешанная",
                                                "Отпускные",
                                                "Другое"
                                            ).forEach { type ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            payType = type
                                                        },
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected =
                                                            payType == type,
                                                        onClick = {
                                                            payType = type
                                                        }
                                                    )
                                                    Text(
                                                        type,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            val value = number(payAmount)
                                            Button(
                                                onClick = {
                                                    payments = payments +
                                                        Payment(
                                                            payDate,
                                                            value,
                                                            payType
                                                        )
                                                    savePaymentsNow()
                                                    payAmount = ""
                                                },
                                                enabled = value > 0,
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {
                                                Text("Сохранить выплату")
                                            }
                                        }

                                        AppPanel("ИСТОРИЯ ВЫПЛАТ") {
                                            if (payments.isEmpty()) {
                                                Text(
                                                    "Пока нет записей",
                                                    color = muted
                                                )
                                            }

                                            payments.sortedByDescending {
                                                it.date
                                            }.forEach { item ->
                                                HorizontalDivider()
                                                Text(
                                                    "${item.date} · ${item.type}",
                                                    color = muted
                                                )
                                                Text(
                                                    money.format(item.amount),
                                                    color = accent,
                                                    fontSize = 19.sp
                                                )
                                                TextButton(
                                                    onClick = {
                                                        val index =
                                                            payments.indexOf(item)
                                                        if (index >= 0) {
                                                            payments =
                                                                payments.toMutableList()
                                                                    .also {
                                                                        it.removeAt(index)
                                                                    }
                                                            savePaymentsNow()
                                                        }
                                                    }
                                                ) {
                                                    Text("Удалить")
                                                }
                                            }
                                        }
                                    }

                                    3 -> {
                                        OrganizerScreen(this@MainActivity)
                                    }

                                    4 -> {
                                        AppPanel("ЛИЧНЫЕ ДАННЫЕ") {
                                            AppField("Имя", name) {
                                                name = it
                                                saveSettings()
                                            }
                                        }

                                        AppPanel("ГРАФИК ВАХТ") {
                                            AppDateField(
                                                "Опорная дата заезда",
                                                anchor
                                            ) {
                                                anchor = it
                                                saveSettings()
                                            }

                                            AppField(
                                                "Число заезда",
                                                arrivalDay,
                                                true
                                            ) {
                                                arrivalDay = it
                                                saveSettings()
                                            }

                                            AppField(
                                                "Число выезда",
                                                departureDay,
                                                true
                                            ) {
                                                departureDay = it
                                                saveSettings()
                                            }

                                            Text(
                                                "Повторение графика — каждые 3 месяца. Выезд — через 2 месяца после заезда.",
                                                color = muted,
                                                fontSize = 12.sp
                                            )
                                        }

                                        AppPanel("ЗАРАБОТНАЯ ПЛАТА") {
                                            AppField("ЧТС, ₽/час", rate, true) {
                                                rate = it
                                                saveSettings()
                                            }
                                            AppField(
                                                "Часов в смене",
                                                hours,
                                                true
                                            ) {
                                                hours = it
                                                saveSettings()
                                            }
                                            AppField(
                                                "Районный коэффициент",
                                                coefficient,
                                                true
                                            ) {
                                                coefficient = it
                                                saveSettings()
                                            }
                                            AppField(
                                                "Надбавки / премии, %",
                                                bonus,
                                                true
                                            ) {
                                                bonus = it
                                                saveSettings()
                                            }
                                            AppField("НДФЛ, %", tax, true) {
                                                tax = it
                                                saveSettings()
                                            }
                                        }

                                        AppPanel("ВЫПЛАТЫ") {
                                            AppField(
                                                "Число аванса",
                                                advance,
                                                true
                                            ) {
                                                advance = it
                                                saveSettings()
                                            }
                                            AppField(
                                                "Число зарплаты",
                                                salary,
                                                true
                                            ) {
                                                salary = it
                                                saveSettings()
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
                                                            saveSettings()
                                                        },
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected =
                                                            advanceRule == option,
                                                        onClick = {
                                                            advanceRule = option
                                                            saveSettings()
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
                                                    "Аванс, ₽",
                                                    fixedAdvance,
                                                    true
                                                ) {
                                                    fixedAdvance = it
                                                    saveSettings()
                                                }
                                            }
                                            AppField(
                                                "Межвахтовые выплаты, ₽",
                                                intershift,
                                                true
                                            ) {
                                                intershift = it
                                                saveSettings()
                                            }
                                            AppField(
                                                "Дополнительные условия начислений",
                                                payNotes
                                            ) {
                                                payNotes = it
                                                saveSettings()
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
                                                Text("Заблокировать приложение")
                                            }
                                        }
                                    }
                                }
                            }

                            NavigationBar(containerColor = panel) {
                                listOf(
                                    "Главная", "Календарь",
                                    "Финансы", "Мои дела", "Настройки"
                                ).forEachIndexed { index, title ->
                                    NavigationBarItem(
                                        selected = tab == index,
                                        onClick = { tab = index },
                                        icon = {
                                            Text(
                                                listOf(
                                                    "⌂", "▦", "₽", "☑", "⚙"
                                                )[index]
                                            )
                                        },
                                        label = {
                                            Text(
                                                title,
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
    initialName: String,
    initialAnchor: String,
    initialArrival: String,
    initialDeparture: String,
    initialRate: String,
    initialHours: String,
    initialTax: String,
    initialBonus: String,
    initialCoefficient: String,
    initialAdvance: String,
    initialSalary: String,
    initialAdvanceRule: String,
    initialFixedAdvance: String,
    initialIntershift: String,
    initialNotes: String,
    onFinish: (List<String>) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    var name by remember { mutableStateOf(initialName) }
    var anchor by remember { mutableStateOf(initialAnchor) }
    var arrival by remember { mutableStateOf(initialArrival) }
    var departure by remember { mutableStateOf(initialDeparture) }
    var rate by remember { mutableStateOf(initialRate) }
    var hours by remember { mutableStateOf(initialHours) }
    var tax by remember { mutableStateOf(initialTax) }
    var bonus by remember { mutableStateOf(initialBonus) }
    var coefficient by remember {
        mutableStateOf(initialCoefficient)
    }
    var advance by remember { mutableStateOf(initialAdvance) }
    var salary by remember { mutableStateOf(initialSalary) }
    var rule by remember { mutableStateOf(initialAdvanceRule) }
    var fixed by remember {
        mutableStateOf(initialFixedAdvance)
    }
    var intershift by remember {
        mutableStateOf(initialIntershift)
    }
    var notes by remember { mutableStateOf(initialNotes) }

    val valid = when (step) {
        0 -> name.trim().isNotEmpty()
        1 -> {
            val inDay = arrival.toIntOrNull()
            val outDay = departure.toIntOrNull()
            inDay != null && inDay in 1..31 &&
                outDay != null && outDay in 1..31
        }
        2 -> number(rate) > 0 &&
            number(hours) > 0 &&
            number(hours) <= 24 &&
            number(coefficient) > 0
        3 -> {
            val a = advance.toIntOrNull()
            val s = salary.toIntOrNull()
            a != null && a in 1..31 &&
                s != null && s in 1..31
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
            Modifier.fillMaxSize()
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
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    0 -> {
                        WizardTitle(
                            "Давай знакомиться",
                            "Как тебя зовут? Это имя будет на главном экране."
                        )
                        AppField("Твоё имя", name) { name = it }
                    }

                    1 -> {
                        WizardTitle(
                            "Твой график",
                            "Укажи даты заезда и выезда. Приложение рассчитает остальные вахты."
                        )

                        AppDateField(
                            "Дата известного заезда",
                            anchor
                        ) {
                            anchor = it
                            arrival = parseDate(it)
                                .dayOfMonth.toString()
                        }

                        AppField(
                            "Число заезда",
                            arrival,
                            true
                        ) { arrival = it }

                        AppField(
                            "Число выезда домой",
                            departure,
                            true
                        ) { departure = it }

                        Text(
                            "Пример: заезд 25 октября, домой 10 декабря, следующий заезд 25 января.",
                            color = muted
                        )
                        Text(
                            "Цикл повторяется каждые 3 календарных месяца.",
                            color = accent
                        )
                    }

                    2 -> {
                        WizardTitle(
                            "Работа и заработок",
                            "Эти параметры нужны для предварительного расчёта начислений."
                        )

                        AppField(
                            "Часовая тарифная ставка, ₽",
                            rate,
                            true
                        ) { rate = it }

                        AppField(
                            "Часов в смене",
                            hours,
                            true
                        ) { hours = it }

                        AppField(
                            "Районный коэффициент (например, 1,3)",
                            coefficient,
                            true
                        ) { coefficient = it }

                        AppField(
                            "Надбавки и премии, %",
                            bonus,
                            true
                        ) { bonus = it }

                        AppField(
                            "НДФЛ, %",
                            tax,
                            true
                        ) { tax = it }
                    }

                    3 -> {
                        WizardTitle(
                            "Когда приходят деньги?",
                            "Укажи даты и порядок выплаты аванса."
                        )

                        AppField(
                            "Число аванса",
                            advance,
                            true
                        ) { advance = it }

                        AppField(
                            "Число зарплаты",
                            salary,
                            true
                        ) { salary = it }

                        Text(
                            "Как рассчитывается аванс?",
                            color = muted
                        )

                        listOf(
                            "По отработанным часам",
                            "Фиксированная сумма",
                            "Другой порядок"
                        ).forEach { option ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { rule = option },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = rule == option,
                                    onClick = { rule = option }
                                )
                                Text(option, color = Color.White)
                            }
                        }

                        if (rule == "Фиксированная сумма") {
                            AppField(
                                "Сумма аванса, ₽",
                                fixed,
                                true
                            ) { fixed = it }
                        }

                        AppField(
                            "Межвахтовые выплаты, ₽",
                            intershift,
                            true
                        ) { intershift = it }

                        AppField(
                            "Дополнительные правила начислений",
                            notes
                        ) { notes = it }
                    }

                    4 -> {
                        WizardTitle(
                            "Всё готово!",
                            "Проверь основные настройки перед началом работы."
                        )

                        AppPanel("ТВОИ ДАННЫЕ") {
                            Text("Имя: $name", color = Color.White)
                            Text(
                                "Опорный заезд: ${parseDate(anchor).pretty()}",
                                color = muted
                            )
                            Text(
                                "Заезд: $arrival числа",
                                color = accent
                            )
                            Text(
                                "Выезд домой: $departure числа 🎉",
                                color = holiday
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

                        Text(
                            "После завершения все параметры можно изменить в настройках.",
                            color = muted
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        if (step < 4) {
                            step++
                        } else {
                            onFinish(
                                listOf(
                                    name.trim(),
                                    anchor,
                                    arrival,
                                    departure,
                                    rate,
                                    hours,
                                    tax,
                                    bonus,
                                    coefficient,
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
    anchor: LocalDate,
    arrivalDay: Int,
    departureDay: Int
) {
    var month by remember {
        mutableStateOf(YearMonth.from(today))
    }
    var selected by remember {
        mutableStateOf(today)
    }

    AppPanel("КАЛЕНДАРЬ ВАХТ") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { month = month.minusMonths(1) }
            ) {
                Text("‹", fontSize = 30.sp, color = accent)
            }

            Text(
                month.format(
                    DateTimeFormatter.ofPattern(
                        "LLLL yyyy", ru
                    )
                ).replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { month = month.plusMonths(1) }
            ) {
                Text("›", fontSize = 30.sp, color = accent)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf(
                "Пн", "Вт", "Ср", "Чт",
                "Пт", "Сб", "Вс"
            ).forEach { day ->
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, color = muted, fontSize = 11.sp)
                }
            }
        }

        val offset = month.atDay(1).dayOfWeek.value - 1
        val count = month.lengthOfMonth()
        val rows = (offset + count + 6) / 7

        repeat(rows) { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { col ->
                    val day = row * 7 + col - offset + 1

                    if (day in 1..count) {
                        val date = month.atDay(day)
                        val s = scheduleFor(
                            date, anchor,
                            arrivalDay, departureDay
                        )

                        val isArrival = date == s.arrival
                        val isDeparture = date == s.departure

                        val color = when {
                            isDeparture -> holiday
                            isArrival -> Color(0xFF278E7A)
                            s.working -> Color(0xFF204F4A)
                            else -> Color(0xFF233F65)
                        }

                        Box(
                            modifier = Modifier.weight(1f)
                                .height(50.dp)
                                .background(
                                    color,
                                    RoundedCornerShape(10.dp)
                                )
                                .then(
                                    if (date == selected)
                                        Modifier.border(
                                            2.dp,
                                            Color.White,
                                            RoundedCornerShape(10.dp)
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
                                    day.toString(),
                                    color = if (isDeparture)
                                        bg else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    when {
                                        isDeparture -> "🎉"
                                        isArrival -> "●"
                                        date == today -> "•"
                                        else -> " "
                                    },
                                    fontSize = 11.sp,
                                    lineHeight = 12.sp,
                                    color = accent
                                )
                            }
                        }
                    } else {
                        Spacer(
                            Modifier.weight(1f).height(50.dp)
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Legend(accent, "Вахта")
            Legend(restColor, "Отдых")
            Legend(holiday, "Домой 🎉")
        }

        HorizontalDivider(color = Color(0xFF36506B))

        val selectedSchedule = scheduleFor(
            selected, anchor, arrivalDay, departureDay
        )

        Text(
            selected.pretty(),
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            when {
                selected == selectedSchedule.departure ->
                    "🎉 День выезда домой!"
                selected == selectedSchedule.arrival ->
                    "🚀 День заезда на вахту!"
                selectedSchedule.working ->
                    "На вахте. До выезда: ${selectedSchedule.remaining} дн."
                else ->
                    "Межвахтовый отдых. До заезда: ${selectedSchedule.remaining} дн."
            },
            color = if (
                selected == selectedSchedule.departure
            ) holiday else accent
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier.size(9.dp)
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
    var open by remember { mutableStateOf(false) }

    val date = parseDate(value)

    OutlinedButton(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label: ${date.pretty()}")
    }

    if (open) {
        var month by remember(open) {
            mutableStateOf(YearMonth.from(date))
        }

        AlertDialog(
            onDismissRequest = { open = false },
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
                            Text("‹", fontSize = 25.sp)
                        }

                        Text(
                            month.format(
                                DateTimeFormatter.ofPattern(
                                    "LLLL yyyy", ru
                                )
                            ),
                            color = Color.White
                        )

                        TextButton(
                            onClick = {
                                month = month.plusMonths(1)
                            }
                        ) {
                            Text("›", fontSize = 25.sp)
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        listOf(
                            "Пн", "Вт", "Ср", "Чт",
                            "Пт", "Сб", "Вс"
                        ).forEach {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = muted
                            )
                        }
                    }

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
                                    modifier = Modifier.weight(1f)
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
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    if (day in 1..count) {
                                        Text(
                                            day.toString(),
                                            color = if (
                                                month.atDay(day) == date
                                            ) accent else Color.White
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
    payments.forEach {
        array.put(
            JSONObject()
                .put("date", it.date)
                .put("amount", it.amount)
                .put("type", it.type)
        )
    }
    return array.toString()
}
