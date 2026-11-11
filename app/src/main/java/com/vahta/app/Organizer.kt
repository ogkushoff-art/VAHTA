
package com.vahta.app

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val orgBackground get() = if(lavenderMode) Color(0xFF1B102A) else Color(0xFF0B1728)
private val orgCard get() = if(lavenderMode) Color(0xFF352148) else Color(0xFF1A2D45)
private val orgAccent get() = if(lavenderMode) Color(0xFFD9A5FF) else Color(0xFF56E4C0)
private val orgMuted get() = if(lavenderMode) Color(0xFFE0C9F0) else Color(0xFFB4C6D9)
private val orgBorder get() = if(lavenderMode) Color(0xFF79528E) else Color(0xFF36506B)

private val organizerCategories = listOf(
    "Покупки на вахту", "Подарки", "Лекарства", "Чемодан",
    "Планы", "Заметки", "Расходы", "Долги", "Накопления",
    "Тренировки", "Питание", "Вес", "Сон",
    "Отпуск"
)

private data class OrganizerItem(
    val id: Long,
    val category: String,
    val title: String,
    val details: String,
    val date: String,
    val done: Boolean,
    val debtBalance: Double = 0.0,
    val debtMonthly: Double = 0.0,
    val debtDueDay: Int = 0,
    val amount: Double = 0.0,
    val unit: String = "",
    val time: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(context: Context) {
    val prefs = remember(context) {
        context.getSharedPreferences("vahta_organizer", Context.MODE_PRIVATE)
    }
    var items by remember {
        mutableStateOf(readOrganizer(prefs.getString("items", "[]") ?: "[]"))
    }
    var category by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var formOpen by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var debtBalance by remember { mutableStateOf("") }
    var debtMonthly by remember { mutableStateOf("") }
    var debtDueDay by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var quickTemplatesOpen by remember { mutableStateOf(false) }

    fun update(newItems: List<OrganizerItem>) {
        items = newItems
        prefs.edit().putString("items", writeOrganizer(newItems)).apply()
    }

    fun clearForm() {
        editingId = null
        title = ""
        details = ""
        date = ""
        debtBalance = ""
        debtMonthly = ""
        debtDueDay = ""
        amount = ""
        unit = ""
        time = ""
        formOpen = false
    }

    val activeCategory = category
    val visible = items.filter { it.category == activeCategory }
    val completed = visible.count { it.done }
    fun startEdit(item: OrganizerItem) {
        editingId = item.id
        title = item.title
        details = item.details
        date = item.date
        debtBalance = if (item.debtBalance > 0) item.debtBalance.toString() else ""
        debtMonthly = if (item.debtMonthly > 0) item.debtMonthly.toString() else ""
        debtDueDay = if (item.debtDueDay > 0) item.debtDueDay.toString() else ""
        amount = if (item.amount != 0.0) item.amount.toString() else ""
        unit = item.unit
        time = item.time
        formOpen = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(orgBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (activeCategory == null) {
            Text("МОИ ДЕЛА", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Выберите раздел", color = orgMuted, fontSize = 13.sp)

            organizerCategories.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { option ->
                        val categoryItems = items.filter { it.category == option }
                        val doneCount = categoryItems.count { it.done }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(128.dp)
                                .clickable {
                                    category = option
                                    clearForm()
                                },
                            colors = CardDefaults.cardColors(containerColor = orgCard),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(13.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    organizerCategoryIcon(option),
                                    fontSize = 24.sp,
                                    color = orgAccent
                                )
                                Text(
                                    option,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 2
                                )
                                Text(
                                    if (option == "Заметки") "${categoryItems.size} заметок  ›" else if (option in listOf("Питание", "Вес", "Сон", "Расходы", "Накопления")) "${categoryItems.size} записей  ›" else if (option == "Долги") "${categoryItems.count { !it.done }} активных  ›" else "$doneCount / ${categoryItems.size} выполнено  ›",
                                    fontSize = 11.sp,
                                    color = orgMuted
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            TextButton(onClick = {
                category = null
                clearForm()
            }) {
                Text("‹  Все разделы", color = orgAccent)
            }
            Text(activeCategory, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = orgCard),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (activeCategory !in organizerJournalCategories) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Выполнено", color = orgMuted, fontSize = 13.sp)
                            Text("$completed / ${visible.size}", color = orgAccent, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { if (visible.isEmpty()) 0f else completed.toFloat() / visible.size },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = orgAccent,
                            trackColor = orgBorder
                        )
                    } else {
                        Text("${visible.size} записей", color = orgMuted, fontSize = 13.sp)
                    }
                    OrganizerSummary(activeCategory, visible)
                    if (activeCategory == "Чемодан") {
                        OutlinedButton(onClick = { quickTemplatesOpen = !quickTemplatesOpen }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (quickTemplatesOpen) "Скрыть шаблон" else "Добавить вещи по шаблону", color = orgAccent)
                        }
                        if (quickTemplatesOpen) {
                            Text("Отметь готовые вещи после добавления. Уже существующие позиции не дублируются.", color = orgMuted, fontSize = 12.sp)
                            listOf("Документы", "Телефон и зарядка", "Рабочая одежда", "Повседневная одежда", "Нижнее бельё", "Носки", "Обувь", "Зубная щётка и паста", "Средства гигиены", "Аптечка", "Полотенце", "Наушники").forEach { thing ->
                                val exists = items.any { it.category == "Чемодан" && it.title.equals(thing, true) }
                                OutlinedButton(
                                    enabled = !exists,
                                    onClick = {
                                        update(items + OrganizerItem((items.maxOfOrNull { it.id } ?: 0L) + 1L, "Чемодан", thing, "", "", false))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (exists) "✓ $thing" else "+ $thing", color = orgAccent) }
                            }
                        }
                    }
                Button(
                    onClick = {
                        if (formOpen) clearForm() else {
                            clearForm()
                            formOpen = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = orgAccent,
                        contentColor = orgBackground
                    )
                ) {
                    Text(if (formOpen) "Отменить" else if (activeCategory == "Заметки") "+ Новая заметка" else "+ Добавить")
                }
            }
        }

        if (formOpen) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = orgCard)
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (editingId == null) "НОВАЯ ЗАПИСЬ" else "РЕДАКТИРОВАНИЕ",
                        color = orgAccent,
                        fontWeight = FontWeight.Bold
                    )
                    OrganizerInput(organizerTitleLabel(activeCategory), title) { title = it }
                    if (activeCategory == "Долги") {
                        OrganizerInput("Остаток долга, ₽", debtBalance) { debtBalance = it }
                        OrganizerInput("Обязательный платёж в месяц, ₽", debtMonthly) { debtMonthly = it }
                        OrganizerInput("Число платежа (1–31, необязательно)", debtDueDay) { debtDueDay = it }
                        Text("Если платёж не указан, долг учитывается в общей задолженности, но не вычитается из месячного бюджета.", color = orgMuted, fontSize = 12.sp)
                    }
                    if (activeCategory == "Заметки") {
                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text("Текст заметки") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                            minLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = orgAccent,
                                unfocusedBorderColor = orgBorder
                            )
                        )
                    } else {
                        OrganizerInput(organizerDetailsLabel(activeCategory), details) { details = it }
                        if (activeCategory in organizerNumberCategories) {
                            OrganizerInput(organizerNumberLabel(activeCategory), amount) { amount = it }
                        }
                        if (activeCategory == "Питание") OrganizerInput("Приём пищи (завтрак / обед / ужин / перекус)", unit) { unit = it }
                        if (activeCategory == "Настроение") OrganizerInput("Оценка настроения от 1 до 5", unit) { unit = it }
                        if (activeCategory == "Лекарства") {
                            OrganizerInput("Дозировка (например, 1 таблетка)", unit) { unit = it }
                            OrganizerInput("Время приёма (например, 08:00)", time) { time = it }
                            Text("Для напоминания добавь приём в календарь из сохранённой карточки.", color = orgMuted, fontSize = 12.sp)
                        }
                        if (activeCategory == "Тренировки") OrganizerInput("Подходы / повторения / длительность", unit) { unit = it }
                        if (activeCategory == "Сон") OrganizerInput("Время отхода ко сну / подъёма", unit) { unit = it }
                        OrganizerInput(if (activeCategory == "Лекарства") "Дата начала (ГГГГ-ММ-ДД)" else "Дата (ГГГГ-ММ-ДД)", date) { date = it }
                    }
                    val pickerContext = LocalContext.current
                    if (activeCategory != "Заметки") OutlinedButton(
                        onClick = {
                            val initial = runCatching { LocalDate.parse(date) }
                                .getOrDefault(LocalDate.now())
                            DatePickerDialog(
                                pickerContext,
                                { _, year, month, day ->
                                    date = LocalDate.of(year, month + 1, day).toString()
                                },
                                initial.year,
                                initial.monthValue - 1,
                                initial.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Выбрать дату в календаре", color = orgAccent)
                    }
                    Button(
                        enabled = (title.isNotBlank() || (activeCategory == "Заметки" && details.isNotBlank())) && (activeCategory !in organizerNumberCategories || (amount.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true)) && (activeCategory != "Долги" || (debtBalance.replace(",", ".").toDoubleOrNull()?.let { it > 0 && it.isFinite() } == true && (debtMonthly.isBlank() || debtMonthly.replace(",", ".").toDoubleOrNull()?.let { it >= 0 && it.isFinite() } == true) && (debtDueDay.isBlank() || debtDueDay.toIntOrNull() in 1..31))),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val id = editingId
                            if (id == null) {
                                val nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
                                update(items + OrganizerItem(
                                    nextId, activeCategory, title.trim().ifBlank { "Без названия" }, details.trim(), if (activeCategory == "Заметки" || (activeCategory in organizerJournalCategories && date.isBlank())) LocalDate.now().toString() else date.trim(), false,
                                    if (activeCategory == "Долги") debtBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    if (activeCategory == "Долги") debtMonthly.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    if (activeCategory == "Долги") debtDueDay.toIntOrNull() ?: 0 else 0,
                                    amount.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    unit.trim(), time.trim()
                                ))
                            } else {
                                update(items.map { item ->
                                    if (item.id == id) item.copy(
                                        title = title.trim().ifBlank { "Без названия" },
                                        details = details.trim(),
                                        date = if (activeCategory == "Заметки" || (activeCategory in organizerJournalCategories && date.isBlank())) LocalDate.now().toString() else date.trim(),
                                        debtBalance = if (activeCategory == "Долги") debtBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.debtBalance,
                                        debtMonthly = if (activeCategory == "Долги") debtMonthly.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.debtMonthly,
                                        debtDueDay = if (activeCategory == "Долги") debtDueDay.toIntOrNull() ?: 0 else item.debtDueDay,
                                        amount = amount.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                        unit = unit.trim(), time = time.trim()
                                    ) else item
                                })
                            }
                            clearForm()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = orgAccent,
                            contentColor = orgBackground
                        )
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = orgCard),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    "В этом разделе пока нет записей",
                    color = orgMuted,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        visible.sortedWith(if (activeCategory == "Заметки") compareByDescending<OrganizerItem> { it.id } else compareBy<OrganizerItem> { it.done }.thenBy { it.id })
            .forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = orgCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeCategory !in organizerJournalCategories) Checkbox(
                                checked = item.done,
                                onCheckedChange = { checked ->
                                    update(items.map {
                                        if (it.id == item.id) it.copy(done = checked) else it
                                    })
                                },
                                colors = CheckboxDefaults.colors(checkedColor = orgAccent)
                            )
                            Text(
                                item.title,
                                modifier = Modifier.weight(1f).clickable {
                                    startEdit(item)
                                },
                                color = if (item.done && activeCategory !in organizerJournalCategories) orgMuted else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                textDecoration = if (item.done && activeCategory !in organizerJournalCategories) TextDecoration.LineThrough else null
                            )
                        }
                        if (activeCategory == "Долги") {
                            Text("Остаток: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.0f", item.debtBalance)} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
                            Text("В месяц: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.0f", item.debtMonthly.coerceAtMost(item.debtBalance))} ₽" + if (item.debtDueDay in 1..31) " · ${item.debtDueDay}-го числа" else "", color = orgMuted, fontSize = 13.sp)
                        }
                        if (activeCategory in organizerNumberCategories) {
                            Text(organizerNumberDisplay(activeCategory, item.amount), color = orgAccent, fontWeight = FontWeight.Bold)
                        }
                        if (item.unit.isNotBlank()) Text(item.unit, color = orgMuted, fontSize = 13.sp)
                        if (item.time.isNotBlank()) Text("Время: ${item.time}", color = orgAccent, fontSize = 13.sp)
                        if (activeCategory == "Лекарства") {
                            OutlinedButton(onClick = {
                                val start = runCatching { LocalDate.parse(item.date) }.getOrDefault(LocalDate.now())
                                val parsed = runCatching { java.time.LocalTime.parse(item.time) }.getOrDefault(java.time.LocalTime.of(9, 0))
                                val millis = start.atTime(parsed).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                                    .putExtra(CalendarContract.Events.TITLE, "Приём: ${item.title}")
                                    .putExtra(CalendarContract.Events.DESCRIPTION, "${item.unit} ${item.details}".trim())
                                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, millis)
                                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, millis + 15 * 60 * 1000)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                            }) { Text("Напоминание в календаре", color = orgAccent) }
                        }
                        if (item.details.isNotBlank()) {
                            Text(item.details, color = orgMuted, fontSize = 13.sp)
                        }
                        if (item.date.isNotBlank()) {
                            Text(if (activeCategory == "Заметки") "Изменено: ${item.date}" else "Дата / время: ${item.date}", color = orgAccent, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                startEdit(item)
                            }) {
                                Text("Изменить", color = orgAccent)
                            }
                            TextButton(onClick = { deleteId = item.id }) {
                                Text("Удалить", color = Color(0xFFFF9C9C))
                            }
                        }
                    }
                }
            }
        }
    }

    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Удалить запись?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteId
                    update(items.filterNot { it.id == id })
                    if (editingId == id) clearForm()
                    deleteId = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun OrganizerInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = orgAccent,
            unfocusedBorderColor = orgBorder,
            focusedLabelColor = orgAccent,
            unfocusedLabelColor = orgMuted
        )
    )
}

private fun readOrganizer(raw: String): List<OrganizerItem> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val obj = array.getJSONObject(index)
        OrganizerItem(
            id = obj.getLong("id"),
            category = obj.getString("category"),
            title = obj.getString("title"),
            details = obj.optString("details"),
            date = obj.optString("date"),
            done = obj.optBoolean("done"),
            debtBalance = obj.optDouble("debtBalance", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            debtMonthly = obj.optDouble("debtMonthly", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            debtDueDay = obj.optInt("debtDueDay", 0),
            amount = obj.optDouble("amount", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            unit = obj.optString("unit"),
            time = obj.optString("time")
        )
    }
}.getOrDefault(emptyList())

private fun writeOrganizer(items: List<OrganizerItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("category", item.category)
                .put("title", item.title)
                .put("details", item.details)
                .put("date", item.date)
                .put("done", item.done)
                .put("debtBalance", item.debtBalance)
                .put("debtMonthly", item.debtMonthly)
                .put("debtDueDay", item.debtDueDay)
                .put("amount", item.amount)
                .put("unit", item.unit)
                .put("time", item.time)
        )
    }
    return array.toString()
}

private fun organizerCategoryIcon(category: String): String = when (category) {
    "Покупки на вахту" -> "🛒"
    "Подарки" -> "🎁"
    "Лекарства" -> "💊"
    "Чемодан" -> "🧳"
    "Планы" -> "📋"
    "Заметки" -> "📝"
    "Расходы" -> "💳"
    "Долги" -> "📑"
    "Накопления" -> "💰"
    "Тренировки" -> "🏋"
    "Питание" -> "🍽"
    "Вес" -> "⚖"
    "Сон" -> "🌙"
    "Настроение" -> "☺"
    "Отпуск" -> "🏖"
    "Развлечения" -> "🎮"
    else -> "▣"
}

// Public read-only summary for the Finance tab. Uses the existing organizer storage.
data class VahtaDebt(val name: String, val balance: Double, val monthly: Double, val dueDay: Int)
fun readVahtaDebts(context: Context): List<VahtaDebt> {
    val prefs = context.getSharedPreferences("vahta_organizer", Context.MODE_PRIVATE)
    return readOrganizer(prefs.getString("items", "[]") ?: "[]")
        .filter { it.category == "Долги" && !it.done && it.debtBalance > 0 }
        .map { VahtaDebt(it.title, it.debtBalance, it.debtMonthly.coerceAtMost(it.debtBalance), it.debtDueDay) }
}

private val organizerJournalCategories = setOf("Заметки", "Питание", "Вес", "Сон", "Расходы", "Накопления")
private val organizerNumberCategories = setOf("Питание", "Вес", "Сон", "Расходы", "Накопления")
private fun organizerTitleLabel(category: String): String = when (category) {
    "Долги" -> "Кому должен / название долга"
    "Заметки" -> "Заголовок заметки"
    "Питание" -> "Продукт или блюдо"
    "Вес" -> "Измерение (например, утро)"
    "Сон" -> "Запись сна"
    "Расходы" -> "На что потрачено"
    "Накопления" -> "Цель или пополнение"
    "Лекарства" -> "Название лекарства"
    "Чемодан" -> "Что положить в чемодан"
    "Тренировки" -> "Упражнение / тренировка"
    "Настроение" -> "Событие / настроение"
    "Отпуск" -> "Пункт плана отпуска"
    else -> "Название"
}
private fun organizerDetailsLabel(category: String): String = when (category) {
    "Лекарства" -> "Особенности приёма / рекомендации врача"
    "Питание" -> "Порция, состав или заметка"
    "Сон" -> "Самочувствие после сна"
    "Тренировки" -> "Упражнения / нагрузка"
    "Покупки на вахту", "Чемодан" -> "Количество / примечание"
    "Расходы" -> "Категория расхода / комментарий"
    "Накопления" -> "Комментарий к накоплению"
    else -> "Описание / примечание"
}
private fun organizerNumberLabel(category: String): String = when (category) {
    "Питание" -> "Калорийность порции, ккал"
    "Вес" -> "Вес, кг"
    "Сон" -> "Продолжительность сна, часы"
    "Расходы" -> "Сумма расхода, ₽"
    "Накопления" -> "Сумма пополнения, ₽"
    else -> "Значение"
}
private fun organizerNumberDisplay(category: String, amount: Double): String = when (category) {
    "Питание" -> "${amount.formatOrg()} ккал"
    "Вес" -> "${amount.formatOrg()} кг"
    "Сон" -> "${amount.formatOrg()} ч"
    else -> "${amount.formatOrg()} ₽"
}
private fun Double.formatOrg(): String = String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.1f", this).removeSuffix(",0")
@Composable
private fun OrganizerSummary(category: String, visible: List<OrganizerItem>) {
    val today = LocalDate.now().toString()
    when (category) {
        "Питание" -> Text("Сегодня: ${visible.filter { it.date == today }.sumOf { it.amount }.formatOrg()} ккал", color = orgAccent, fontWeight = FontWeight.Bold)
        "Расходы" -> Text("Всего расходов: ${visible.sumOf { it.amount }.formatOrg()} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
        "Накопления" -> Text("Всего отложено: ${visible.sumOf { it.amount }.formatOrg()} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
        "Вес" -> visible.maxByOrNull { it.id }?.let { Text("Последнее измерение: ${it.amount.formatOrg()} кг", color = orgAccent) }
        "Сон" -> if (visible.isNotEmpty()) Text("Средняя продолжительность: ${(visible.sumOf { it.amount } / visible.size).formatOrg()} ч", color = orgAccent)
        "Чемодан" -> Text("Осталось собрать: ${visible.count { !it.done }}", color = orgAccent)
        "Долги" -> Text("Остаток задолженности: ${visible.filter { !it.done }.sumOf { it.debtBalance }.formatOrg()} ₽", color = orgAccent)
        "Лекарства" -> Text("Расписание и напоминания через календарь телефона", color = orgMuted, fontSize = 12.sp)
        "Настроение" -> Text("Записывай события и оценивай настроение от 1 до 5", color = orgMuted, fontSize = 12.sp)
        "Покупки на вахту", "Подарки" -> Text("Осталось купить: ${visible.count { !it.done }}", color = orgAccent)
        "Планы", "Отпуск" -> Text("В планах: ${visible.count { !it.done }}", color = orgAccent)
        "Тренировки" -> Text("Выполнено тренировок: ${visible.count { it.done }}", color = orgAccent)
    }
}
