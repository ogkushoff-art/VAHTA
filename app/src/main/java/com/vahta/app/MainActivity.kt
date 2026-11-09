
package com.vahta.app

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.text.NumberFormat
import java.util.Locale

private val navy = Color(0xFF0B1728)
private val surface = Color(0xFF1A2D45)
private val cyan = Color(0xFF56E4C0)
private val soft = Color(0xFFB4C6D9)

private data class Payment(
    val date: String,
    val amount: Double,
    val type: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "vahta_settings",
            MODE_PRIVATE
        )

        setContent {
            var tab by remember { mutableIntStateOf(0) }

            var start by remember {
                mutableStateOf(
                    prefs.getString("start", "2026-10-25")
                        ?: "2026-10-25"
                )
            }

            var work by remember {
                mutableStateOf(
                    prefs.getString("work", "45") ?: "45"
                )
            }

            var rest by remember {
                mutableStateOf(
                    prefs.getString("rest", "45") ?: "45"
                )
            }

            var rate by remember {
                mutableStateOf(
                    prefs.getString("hourly", "599.37")
                        ?: "599.37"
                )
            }

            var hours by remember {
                mutableStateOf(
                    prefs.getString("hours", "11") ?: "11"
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

            var payDate by remember {
                mutableStateOf(LocalDate.now().toString())
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

            var seconds by remember {
                mutableLongStateOf(System.currentTimeMillis())
            }

            LaunchedEffect(Unit) {
                while (true) {
                    seconds = System.currentTimeMillis()
                    delay(1000)
                }
            }

            fun save() {
                prefs.edit()
                    .putString("start", start)
                    .putString("work", work)
                    .putString("rest", rest)
                    .putString("hourly", rate)
                    .putString("hours", hours)
                    .putString("advance", advance)
                    .putString("payday", salary)
                    .apply()
            }

            val today = LocalDate.now()

            val startDate = runCatching {
                LocalDate.parse(start)
            }.getOrNull() ?: today

            val workDays = (
                work.toIntOrNull() ?: 45
            ).coerceIn(1, 365)

            val restDays = (
                rest.toIntOrNull() ?: 45
            ).coerceIn(1, 365)

            val cycle = workDays + restDays

            val position = Math.floorMod(
                ChronoUnit.DAYS.between(startDate, today),
                cycle.toLong()
            ).toInt()

            val working = position < workDays

            val remaining = if (working) {
                workDays - position
            } else {
                cycle - position
            }

            val hourlyRate = (
                rate.replace(',', '.').toDoubleOrNull()
                    ?: 0.0
            ).coerceAtLeast(0.0)

            val dailyHours = (
                hours.replace(',', '.').toDoubleOrNull()
                    ?: 0.0
            ).coerceIn(0.0, 24.0)

            val elapsedToday = (
                seconds % 86400000L
            ).toDouble() / 86400000.0

            val completed = if (working) {
                position
            } else {
                workDays
            }

            val earned = (
                completed +
                    if (working) elapsedToday else 0.0
            ) * dailyHours * hourlyRate

            val formatter = remember {
                NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("ru-RU")
                )
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = cyan,
                    background = navy,
                    surface = surface,
                    onSurface = Color.White,
                    onBackground = Color.White
                )
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(navy)
                ) {

                    val mainModifier = if (tab == 3) {
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
                        modifier = mainModifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        Text(
                            "ВАХТА",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            "Работа · Деньги · Жизнь",
                            color = soft
                        )

                        when (tab) {

                            0 -> {
                                Panel("МОЙ ГРАФИК") {
                                    Text(
                                        if (working) {
                                            "НА ВАХТЕ"
                                        } else {
                                            "НА МЕЖВАХТЕ"
                                        },
                                        color = cyan,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "До ${
                                            if (working) "межвахты"
                                            else "вахты"
                                        }: $remaining дн.",
                                        fontSize = 23.sp,
                                        color = Color.White
                                    )

                                    LinearProgressIndicator(
                                        progress = {
                                            position.toFloat() /
                                                cycle.toFloat()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        color = cyan
                                    )

                                    Text(
                                        "График $workDays/$restDays",
                                        color = soft
                                    )
                                }

                                Panel(
                                    "ЖИВОЙ СЧЁТЧИК ЗАРАБОТКА"
                                ) {
                                    Text(
                                        formatter.format(earned),
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cyan
                                    )

                                    Text(
                                        "Оценка заработка за " +
                                            "рабочий период",
                                        color = soft
                                    )

                                    Text(
                                        "Без налогов, премий, " +
                                            "межвахтовых начислений",
                                        fontSize = 12.sp,
                                        color = soft
                                    )
                                }

                                Panel("БЛИЖАЙШИЕ ВЫПЛАТЫ") {
                                    PaymentDateRow(
                                        "Зарплата",
                                        salary,
                                        today
                                    )

                                    PaymentDateRow(
                                        "Аванс",
                                        advance,
                                        today
                                    )

                                    Text(
                                        "Это календарные даты. " +
                                            "Суммы и межвахтовые " +
                                            "выплаты рассчитываются " +
                                            "отдельно.",
                                        color = soft,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            1 -> {
                                Panel("КАЛЕНДАРЬ ВАХТ") {
                                    Text(
                                        "Дата начала цикла",
                                        color = soft
                                    )

                                    CalendarField(start) {
                                        start = it
                                        save()
                                    }

                                    Text(
                                        "Сегодня: ${today.formatRu()}",
                                        color = Color.White
                                    )

                                    Text(
                                        if (working) {
                                            "Рабочий период"
                                        } else {
                                            "Межвахтовый отдых"
                                        },
                                        color = cyan
                                    )

                                    Text(
                                        "Осталось $remaining дн.",
                                        color = soft
                                    )
                                }

                                Panel("НАСТРОЙКА ГРАФИКА") {
                                    InputField(
                                        "Дней вахты",
                                        work
                                    ) {
                                        work = it
                                        save()
                                    }

                                    InputField(
                                        "Дней межвахты",
                                        rest
                                    ) {
                                        rest = it
                                        save()
                                    }
                                }
                            }

                            2 -> {
                                Panel("НАЧИСЛЕНИЯ") {
                                    Text(
                                        "Оценка за рабочий период",
                                        color = soft
                                    )

                                    Text(
                                        formatter.format(earned),
                                        fontSize = 25.sp,
                                        color = cyan
                                    )

                                    InputField(
                                        "ЧТС, ₽/час",
                                        rate
                                    ) {
                                        rate = it
                                        save()
                                    }

                                    InputField(
                                        "Часов в смене",
                                        hours
                                    ) {
                                        hours = it
                                        save()
                                    }
                                }

                                Panel(
                                    "ДОБАВИТЬ ФАКТИЧЕСКУЮ ВЫПЛАТУ"
                                ) {
                                    CalendarField(payDate) {
                                        payDate = it
                                    }

                                    InputField(
                                        "Получено, ₽",
                                        payAmount
                                    ) {
                                        payAmount = it
                                    }

                                    Text(
                                        "Тип поступления",
                                        color = soft
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
                                            verticalAlignment =
                                                Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    payType = option
                                                }
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

                                    val amountValue = payAmount
                                        .replace(',', '.')
                                        .toDoubleOrNull()

                                    Button(
                                        onClick = {
                                            val value = amountValue

                                            if (
                                                value != null &&
                                                value.isFinite() &&
                                                value > 0
                                            ) {
                                                payments =
                                                    payments + Payment(
                                                        payDate,
                                                        value,
                                                        payType
                                                    )

                                                prefs.edit()
                                                    .putString(
                                                        "payments",
                                                        savePayments(
                                                            payments
                                                        )
                                                    )
                                                    .apply()

                                                payAmount = ""
                                            }
                                        },
                                        enabled =
                                            amountValue != null &&
                                            amountValue.isFinite() &&
                                            amountValue > 0,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            "Сохранить поступление"
                                        )
                                    }
                                }

                                Panel("ИСТОРИЯ ВЫПЛАТ") {
                                    if (payments.isEmpty()) {
                                        Text(
                                            "Поступлений пока нет",
                                            color = soft
                                        )
                                    }

                                    payments
                                        .sortedByDescending {
                                            it.date
                                        }
                                        .forEach { payment ->
                                            HorizontalDivider(
                                                color =
                                                    Color(0xFF36506B)
                                            )

                                            Text(
                                                "${payment.date} · " +
                                                    payment.type,
                                                color = soft
                                            )

                                            Text(
                                                formatter.format(
                                                    payment.amount
                                                ),
                                                color = cyan,
                                                fontSize = 20.sp
                                            )

                                            TextButton(
                                                onClick = {
                                                    val index =
                                                        payments.indexOf(
                                                            payment
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

                                                        prefs.edit()
                                                            .putString(
                                                                "payments",
                                                                savePayments(
                                                                    payments
                                                                )
                                                            )
                                                            .apply()
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
                                Panel("ДАТЫ ВЫПЛАТ") {
                                    InputField(
                                        "Число зарплаты",
                                        salary
                                    ) {
                                        salary = it
                                        save()
                                    }

                                    InputField(
                                        "Число аванса",
                                        advance
                                    ) {
                                        advance = it
                                        save()
                                    }

                                    Text(
                                        "Межвахтовые выплаты " +
                                            "и смешанные начисления " +
                                            "пока не рассчитываются.",
                                        color = soft
                                    )
                                }

                                Panel("ДАННЫЕ") {
                                    Text(
                                        "Настройки, история выплат " +
                                            "и записи органайзера " +
                                            "сохраняются на телефоне.",
                                        color = soft
                                    )

                                    Text(
                                        "Защита по отпечатку " +
                                            "пальца и PIN-коду " +
                                            "пока не подключена.",
                                        color = soft
                                    )
                                }
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
        }
    }
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                color = soft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            content()
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(label)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CalendarField(
    value: String,
    onChange: (String) -> Unit
) {
    val context = LocalContext.current

    val date = runCatching {
        LocalDate.parse(value)
    }.getOrElse {
        LocalDate.now()
    }

    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onChange(
                        LocalDate.of(
                            year,
                            month + 1,
                            day
                        ).toString()
                    )
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("📅 ${date.formatRu()}")
    }
}

private fun LocalDate.formatRu(): String =
    "%02d.%02d.%04d".format(
        dayOfMonth,
        monthValue,
        year
    )

@Composable
private fun PaymentDateRow(
    label: String,
    dayText: String,
    today: LocalDate
) {
    val day = (
        dayText.toIntOrNull() ?: 1
    ).coerceIn(1, 31)

    fun dateFor(month: YearMonth): LocalDate {
        return month.atDay(
            day.coerceAtMost(
                month.lengthOfMonth()
            )
        )
    }

    val month = YearMonth.from(today)
    val current = dateFor(month)

    val next = if (current.isBefore(today)) {
        dateFor(month.plusMonths(1))
    } else {
        current
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color.White
        )

        Text(
            next.formatRu(),
            color = cyan
        )
    }
}

private fun loadPayments(
    raw: String
): List<Payment> {
    return runCatching {
        val array = JSONArray(raw)

        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            Payment(
                date = item.getString("date"),
                amount = item.getDouble("amount"),
                type = item.getString("type")
            )
        }
    }.getOrDefault(emptyList())
}

private fun savePayments(
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
