
package com.vahta.app

import android.app.DatePickerDialog
import android.content.Context
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

private val orgBackground = Color(0xFF0B1728)
private val orgCard = Color(0xFF1A2D45)
private val orgAccent = Color(0xFF56E4C0)
private val orgMuted = Color(0xFFB4C6D9)
private val orgBorder = Color(0xFF36506B)

private val organizerCategories = listOf(
    "Покупки на вахту", "Подарки", "Лекарства", "Чемодан",
    "Планы", "Заметки", "Расходы", "Долги", "Накопления",
    "Тренировки", "Питание", "Вес", "Сон", "Настроение",
    "Отпуск", "Развлечения"
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
    val debtDueDay: Int = 0
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
        formOpen = false
    }

    val activeCategory = category
    val visible = items.filter { it.category == activeCategory }
    val completed = visible.count { it.done }

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
                                    if (option == "Заметки") "${categoryItems.size} заметок  ›" else if (option == "Долги") "${categoryItems.count { !it.done }} активных  ›" else "$doneCount / ${categoryItems.size} выполнено  ›",
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
                    if (activeCategory != "Заметки") {
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
                        Text("${visible.size} заметок", color = orgMuted, fontSize = 13.sp)
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
                    Text(if (formOpen) "Отменить" else if (activeCategory == "Заметки") "+ Новая заметка" else "+ Добавить запись")
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
                    OrganizerInput(if (activeCategory == "Долги") "Кому должен / название долга" else if (activeCategory == "Заметки") "Заголовок заметки" else "Название", title) { title = it }
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
                        OrganizerInput("Описание, количество, дозировка или сумма", details) { details = it }
                        OrganizerInput("Дата / время", date) { date = it }
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
                        enabled = (title.isNotBlank() || (activeCategory == "Заметки" && details.isNotBlank())) && (activeCategory != "Долги" || (debtBalance.replace(",", ".").toDoubleOrNull()?.let { it > 0 && it.isFinite() } == true && (debtMonthly.isBlank() || debtMonthly.replace(",", ".").toDoubleOrNull()?.let { it >= 0 && it.isFinite() } == true) && (debtDueDay.isBlank() || debtDueDay.toIntOrNull() in 1..31))),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val id = editingId
                            if (id == null) {
                                val nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
                                update(items + OrganizerItem(
                                    nextId, activeCategory, title.trim().ifBlank { "Без названия" }, details.trim(), if (activeCategory == "Заметки") LocalDate.now().toString() else date.trim(), false,
                                    if (activeCategory == "Долги") debtBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    if (activeCategory == "Долги") debtMonthly.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    if (activeCategory == "Долги") debtDueDay.toIntOrNull() ?: 0 else 0
                                ))
                            } else {
                                update(items.map { item ->
                                    if (item.id == id) item.copy(
                                        title = title.trim().ifBlank { "Без названия" },
                                        details = details.trim(),
                                        date = if (activeCategory == "Заметки") LocalDate.now().toString() else date.trim(),
                                        debtBalance = if (activeCategory == "Долги") debtBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.debtBalance,
                                        debtMonthly = if (activeCategory == "Долги") debtMonthly.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.debtMonthly,
                                        debtDueDay = if (activeCategory == "Долги") debtDueDay.toIntOrNull() ?: 0 else item.debtDueDay
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
                            if (activeCategory != "Заметки") Checkbox(
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
                                    editingId = item.id
                                    title = item.title
                                    details = item.details
                                    date = item.date
                                    debtBalance = if (item.debtBalance > 0) item.debtBalance.toString() else ""
                                    debtMonthly = if (item.debtMonthly > 0) item.debtMonthly.toString() else ""
                                    debtDueDay = if (item.debtDueDay > 0) item.debtDueDay.toString() else ""
                                    formOpen = true
                                },
                                color = if (item.done && activeCategory != "Заметки") orgMuted else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                textDecoration = if (item.done && activeCategory != "Заметки") TextDecoration.LineThrough else null
                            )
                        }
                        if (activeCategory == "Долги") {
                            Text("Остаток: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.0f", item.debtBalance)} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
                            Text("В месяц: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.0f", item.debtMonthly.coerceAtMost(item.debtBalance))} ₽" + if (item.debtDueDay in 1..31) " · ${item.debtDueDay}-го числа" else "", color = orgMuted, fontSize = 13.sp)
                        }
                        if (item.details.isNotBlank()) {
                            Text(item.details, color = orgMuted, fontSize = 13.sp)
                        }
                        if (item.date.isNotBlank()) {
                            Text(if (activeCategory == "Заметки") "Изменено: ${item.date}" else "Дата / время: ${item.date}", color = orgAccent, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                editingId = item.id
                                title = item.title
                                details = item.details
                                date = item.date
                                debtBalance = if (item.debtBalance > 0) item.debtBalance.toString() else ""
                                debtMonthly = if (item.debtMonthly > 0) item.debtMonthly.toString() else ""
                                debtDueDay = if (item.debtDueDay > 0) item.debtDueDay.toString() else ""
                                formOpen = true
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
            debtDueDay = obj.optInt("debtDueDay", 0)
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
