
package com.vahta.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

private val navy = Color(0xFF0B1728)
private val surface = Color(0xFF1A2D45)
private val cyan = Color(0xFF56E4C0)
private val soft = Color(0xFFB4C6D9)
private val blue = Color(0xFF4688D8)
private val orange = Color(0xFFFFB45B)

private data class Payment(
    val date: String,
    val amount: Double,
    val type: String
)

private data class ShiftState(
    val working: Boolean,
    val position: Int,
    val remaining: Int,
    val progress: Float,
    val departure: Boolean,
    val arrival: Boolean
)

private fun shiftState(
    date: LocalDate,
    start: LocalDate,
    workDays: Int,
    restDays: Int
): ShiftState {
    val cycle = workDays + restDays
    val position = Math.floorMod(
        ChronoUnit.DAYS.between(start, date),
        cycle.toLong()
    ).toInt()
    val working = position < workDays
    val periodPosition = if (working) position else position - workDays
    val periodLength = if (working) workDays else restDays

    return ShiftState(
        working = working,
        position = position,
        remaining = periodLength - periodPosition,
        progress = periodPosition.toFloat() / periodLength.toFloat(),
        departure = position == workDays - 1,
        arrival = position == 0
    )
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
                var tab by remember { mutableIntStateOf(0) }

                var name by remember {
                    mutableStateOf(prefs.getString("name", "") ?: "")
                }
                var start by remember {
                    mutableStateOf(
                        prefs.getString("start", "2026-10-25")
                            ?: "2026-10-25"
                    )
                }
                var work by remember {
                    mutableStateOf(prefs.getString("work", "45") ?: "45")
                }
                var rest by remember {
                    mutableStateOf(prefs.getString("rest", "45") ?: "45")
                }
                var rate by remember {
                    mutableStateOf(
                        prefs.getString("hourly", "599.37") ?: "599.37"
                    )
                }
                var hours by remember {
                    mutableStateOf(prefs.getString("hours", "11") ?: "11")
                }
                var advance by remember {
                    mutableStateOf(prefs.getString("advance", "30") ?: "30")
                }
                var salary by remember {
                    mutableStateOf(prefs.getString("payday", "15") ?: "15")
                }

                var payDate by remember {
                    mutableStateOf(LocalDate.now().toString())
                }
                var payAmount by remember { mutableStateOf("") }
                var payType by remember { mutableStateOf("Зарплата") }

                var payments by remember {
                    mutableStateOf(
                        loadPayments(
                            prefs.getString("payments", "[]") ?: "[]"
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
                        .putString("name", name)
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

                val workDays = (work.toIntOrNull() ?: 45)
                    .coerceIn(1, 365)
                val restDays = (rest.toIntOrNull() ?: 45)
                    .coerceIn(1, 365)

                val shift = shiftState(
                    today, startDate, workDays, restDays
                )

                val hourlyRate = (
                    rate.replace(',', '.').toDoubleOrNull() ?: 0.0
                ).coerceAtLeast(0.0)

                val dailyHours = (
                    hours.replace(',', '.').toDoubleOrNull() ?: 0.0
                ).coerceIn(0.0, 24.0)

                val localTime = LocalTime.now()
                val dayFraction =
                    localTime.toSecondOfDay().toDouble() / 86400.0

                val completedDays = if (shift.working) {
                    shift.position
                } else {
                    workDays
                }

                val earned = (
                    completedDays +
                        if (shift.working) dayFraction else 0.0
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
                        val contentModifier = if (tab == 3) {
                            Modifier.weight(1f).fillMaxWidth()
                        } else {
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        }

                        Column(
                            modifier = contentModifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "ВАХТА",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                if (name.isBlank()) "Добро пожаловать!"
                                else "Привет, ${name.trim()}!",
                                color = soft,
                                fontSize = 18.sp
                            )

                            when (tab) {
                                0 -> {
                                    Panel("МОЙ ГРАФИК") {
                                        Text(
                                            if (shift.working) "НА ВАХТЕ"
                                            else "НА МЕЖВАХТЕ",
                                            color = if (shift.working) cyan else blue,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            "До ${if (shift.working) "межвахты" else "вахты"}: ${shift.remaining} дн.",
                                            fontSize = 23.sp,
                                            color = Color.White
                                        )

                                        LinearProgressIndicator(
                                            progress = { shift.progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp),
                                            color = if (shift.working) cyan else blue,
                                            trackColor = Color(0xFF354C65)
                                        )

                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Пройдено ${(shift.progress * 100).toInt()}%",
                                                color = soft,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                "График $workDays/$restDays",
                                                color = soft,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Panel("ЗАРАБОТОК ЗА ВАХТУ") {
                                        Text(
                                            formatter.format(earned),
                                            fontSize = 27.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cyan
                                        )
                                        Text(
                                            "Предварительная оценка без налогов, премий и других начислений",
                                            color = soft,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Panel("БЛИЖАЙШИЕ ВЫПЛАТЫ") {
                                        PaymentDateRow(
                                            "Зарплата", salary, today
                                        )
                                        PaymentDateRow(
                                            "Аванс", advance, today
                                        )
                                        Text(
                                            "Расчёт ожидаемой суммы ближайшей выплаты добавим после настройки правил начисления зарплаты.",
                                            color = soft,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Panel("КАЛЕНДАРЬ") {
                                        Text(
                                            "Открой вкладку «Календарь», чтобы увидеть весь график и даты выезда.",
                                            color = soft
                                        )
                                        Button(
                                            onClick = { tab = 1 },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Открыть календарь")
                                        }
                                    }
                                }

                                1 -> {
                                    ShiftCalendar(
                                        startDate = startDate,
                                        workDays = workDays,
                                        restDays = restDays,
                                        today = today
                                    )

                                    Panel("МОЙ ГРАФИК") {
                                        Text("Начало рабочего цикла", color = soft)
                                        CalendarField(start) {
                                            start = it
                                            save()
                                        }

                                        InputField("Дней вахты", work) {
                                            work = it
                                            save()
                                        }
                                        InputField("Дней межвахты", rest) {
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
                                        InputField("ЧТС, ₽/час", rate) {
                                            rate = it
                                            save()
                                        }
                                        InputField("Часов в смене", hours) {
                                            hours = it
                                            save()
                                        }
                                    }

                                    Panel("ДОБАВИТЬ ФАКТИЧЕСКУЮ ВЫПЛАТУ") {
                                        CalendarField(payDate) {
                                            payDate = it
                                        }
                                        InputField("Получено, ₽", payAmount) {
                                            payAmount = it
                                        }
                                        Text("Тип поступления", color = soft)

                                        listOf(
                                            "Зарплата", "Аванс",
                                            "Межвахта", "Смешанная",
                                            "Отпускные", "Другое"
                                        ).forEach { option ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { payType = option },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = payType == option,
                                                    onClick = { payType = option }
                                                )
                                                Text(option, color = Color.White)
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
                                                    payments = payments + Payment(
                                                        payDate, value, payType
                                                    )
                                                    prefs.edit()
                                                        .putString(
                                                            "payments",
                                                            savePayments(payments)
                                                        ).apply()
                                                    payAmount = ""
                                                }
                                            },
                                            enabled = amountValue != null &&
                                                amountValue.isFinite() &&
                                                amountValue > 0,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Сохранить поступление")
                                        }
                                    }

                                    Panel("ИСТОРИЯ ВЫПЛАТ") {
                                        if (payments.isEmpty()) {
                                            Text(
                                                "Поступлений пока нет",
                                                color = soft
                                            )
                                        }

                                        payments.sortedByDescending {
                                            it.date
                                        }.forEach { payment ->
                                            HorizontalDivider(
                                                color = Color(0xFF36506B)
                                            )
                                            Text(
                                                "${payment.date} · ${payment.type}",
                                                color = soft
                                            )
                                            Text(
                                                formatter.format(payment.amount),
                                                color = cyan,
                                                fontSize = 20.sp
                                            )
                                            TextButton(
                                                onClick = {
                                                    val index = payments.indexOf(payment)
                                                    if (index >= 0) {
                                                        payments = payments
                                                            .toMutableList()
                                                            .also { it.removeAt(index) }
                                                        prefs.edit()
                                                            .putString(
                                                                "payments",
                                                                savePayments(payments)
                                                            ).apply()
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
                                    Panel("ЛИЧНЫЕ НАСТРОЙКИ") {
                                        InputField("Ваше имя", name) {
                                            name = it
                                            save()
                                        }
                                    }

                                    Panel("ДАТЫ ВЫПЛАТ") {
                                        InputField("Число зарплаты", salary) {
                                            salary = it
                                            save()
                                        }
                                        InputField("Число аванса", advance) {
                                            advance = it
                                            save()
                                        }
                                    }

                                    Panel("БЕЗОПАСНОСТЬ") {
                                        Text(
                                            "Вход по PIN-коду и отпечатку пальца",
                                            color = soft
                                        )
                                        OutlinedButton(
                                            onClick = { unlocked = false },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Заблокировать приложение")
                                        }
                                    }

                                    Panel("ДАННЫЕ") {
                                        Text(
                                            "Настройки и записи сохраняются на телефоне.",
                                            color = soft
                                        )
                                    }
                                }
                            }
                        }

                        NavigationBar(containerColor = surface) {
                            listOf(
                                "Главная", "Календарь", "Финансы",
                                "Мои дела", "Настройки"
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
            }
        }
    }
}

@Composable
private fun ShiftCalendar(
    startDate: LocalDate,
    workDays: Int,
    restDays: Int,
    today: LocalDate
) {
    var month by remember {
        mutableStateOf(YearMonth.from(today))
    }
    var selected by remember {
        mutableStateOf(today)
    }

    val monthName = month.format(
        DateTimeFormatter.ofPattern(
            "LLLL yyyy",
            Locale.forLanguageTag("ru-RU")
        )
    ).replaceFirstChar { it.uppercase() }

    Panel("КАЛЕНДАРЬ ВАХТ") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Text("‹", fontSize = 32.sp, color = cyan)
            }
            Text(
                monthName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Text("›", fontSize = 32.sp, color = cyan)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                .forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day, color = soft, fontSize = 12.sp)
                    }
                }
        }

        val offset = month.atDay(1).dayOfWeek.value - 1
        val count = month.lengthOfMonth()
        val rows = (offset + count + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(rows) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(7) { col ->
                        val number = row * 7 + col - offset + 1

                        if (number in 1..count) {
                            val date = month.atDay(number)
                            val state = shiftState(
                                date, startDate, workDays, restDays
                            )

                            val bg = when {
                                state.departure -> orange
                                state.arrival -> Color(0xFF278E7A)
                                state.working -> Color(0xFF204F4A)
                                else -> Color(0xFF233F65)
                            }

                            val isSelected = date == selected

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(49.dp)
                                    .background(
                                        bg,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            Color.White,
                                            RoundedCornerShape(10.dp)
                                        ) else Modifier
                                    )
                                    .clickable { selected = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        number.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.departure)
                                            navy else Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        when {
                                            state.departure -> "🎉"
                                            state.arrival -> "●"
                                            date == today -> "•"
                                            else -> " "
                                        },
                                        color = cyan,
                                        fontSize = 11.sp,
                                        lineHeight = 12.sp
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
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendDot(cyan, "Вахта")
            LegendDot(blue, "Межвахта")
            LegendDot(orange, "Домой 🎉")
        }

        HorizontalDivider(color = Color(0xFF36506B))

        val selectedState = shiftState(
            selected, startDate, workDays, restDays
        )

        Text(
            selected.formatRu(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            when {
                selectedState.departure ->
                    "🎉 День выезда домой! Вахта завершается."
                selectedState.arrival ->
                    "🚀 Первый день вахты!"
                selectedState.working ->
                    "Рабочий день. До межвахты: ${selectedState.remaining} дн."
                else ->
                    "Межвахтовый отдых. До вахты: ${selectedState.remaining} дн."
            },
            color = if (selectedState.departure) orange else cyan
        )

        OutlinedButton(
            onClick = {
                month = YearMonth.from(today)
                selected = today
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Вернуться к сегодняшнему дню")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, CircleShape)
        )
        Text(label, color = soft, fontSize = 10.sp)
    }
}

@Composable
private fun CalendarField(
    value: String,
    onChange: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }

    val date = runCatching {
        LocalDate.parse(value)
    }.getOrElse { LocalDate.now() }

    OutlinedButton(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("📅 ${date.formatRu()}")
    }

    if (open) {
        var month by remember {
            mutableStateOf(YearMonth.from(date))
        }

        AlertDialog(
            onDismissRequest = { open = false },
            title = {
                Text("Выберите дату", color = Color.White)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { month = month.minusMonths(1) }
                        ) {
                            Text("‹", fontSize = 27.sp)
                        }

                        Text(
                            month.format(
                                DateTimeFormatter.ofPattern(
                                    "LLLL yyyy",
                                    Locale.forLanguageTag("ru-RU")
                                )
                            ),
                            color = Color.White
                        )

                        TextButton(
                            onClick = { month = month.plusMonths(1) }
                        ) {
                            Text("›", fontSize = 27.sp)
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                            .forEach {
                                Text(
                                    it,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    color = soft,
                                    fontSize = 11.sp
                                )
                            }
                    }

                    val offset = month.atDay(1).dayOfWeek.value - 1
                    val count = month.lengthOfMonth()
                    val rows = (offset + count + 6) / 7

                    repeat(rows) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val day = row * 7 + col - offset + 1

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .then(
                                            if (day in 1..count) {
                                                Modifier.clickable {
                                                    onChange(
                                                        month.atDay(day).toString()
                                                    )
                                                    open = false
                                                }
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day in 1..count) {
                                        Text(
                                            day.toString(),
                                            color = if (
                                                month.atDay(day) == date
                                            ) cyan else Color.White
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
private fun Panel(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun LocalDate.formatRu(): String =
    "%02d.%02d.%04d".format(
        dayOfMonth, monthValue, year
    )

@Composable
private fun PaymentDateRow(
    label: String,
    dayText: String,
    today: LocalDate
) {
    val day = (dayText.toIntOrNull() ?: 1).coerceIn(1, 31)

    fun dateFor(month: YearMonth): LocalDate =
        month.atDay(day.coerceAtMost(month.lengthOfMonth()))

    val month = YearMonth.from(today)
    val current = dateFor(month)
    val next = if (current.isBefore(today)) {
        dateFor(month.plusMonths(1))
    } else current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White)
        Text(next.formatRu(), color = cyan)
    }
}

private fun loadPayments(raw: String): List<Payment> =
    runCatching {
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

private fun savePayments(payments: List<Payment>): String {
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
