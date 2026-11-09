
package com.vahta.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private val organizerCategories = listOf(
    "Покупки на вахту",
    "Подарки",
    "Лекарства",
    "Чемодан",
    "Планы",
    "Заметки",
    "Расходы",
    "Долги",
    "Накопления",
    "Тренировки",
    "Питание",
    "Вес",
    "Сон",
    "Настроение",
    "Отпуск",
    "Развлечения"
)

private data class OrganizerItem(
    val id: Long,
    val category: String,
    val title: String,
    val details: String,
    val date: String,
    val done: Boolean
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(context: Context) {
    val prefs = remember(context) {
        context.getSharedPreferences(
            "vahta_organizer",
            Context.MODE_PRIVATE
        )
    }

    var items by remember {
        mutableStateOf(
            readOrganizer(prefs.getString("items", "[]") ?: "[]")
        )
    }

    var category by remember {
        mutableStateOf(organizerCategories.first())
    }
    var expanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    fun update(newItems: List<OrganizerItem>) {
        items = newItems
        prefs.edit()
            .putString("items", writeOrganizer(newItems))
            .apply()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "МОИ ДЕЛА",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Покупки, подарки, лекарства и планы")

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Раздел") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                organizerCategories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            category = option
                            expanded = false
                        }
                    )
                }
            }
        }

        val visible = items.filter {
            it.category == category
        }

        Text(
            "Выполнено: ${visible.count { it.done }} " +
                "из ${visible.size}"
        )

        Button(
            onClick = { showForm = !showForm },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showForm) "Закрыть" else "+ Добавить запись")
        }

        if (showForm) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = {
                    Text("Описание, дозировка, сумма или комментарий")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Дата или время (необязательно)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val nextId = (
                        items.maxOfOrNull { it.id } ?: 0L
                    ) + 1L

                    update(
                        items + OrganizerItem(
                            id = nextId,
                            category = category,
                            title = title.trim(),
                            details = details.trim(),
                            date = date.trim(),
                            done = false
                        )
                    )

                    title = ""
                    details = ""
                    date = ""
                    showForm = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }

        if (visible.isEmpty()) {
            Text("В этом разделе пока нет записей")
        }

        visible.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { checked ->
                                update(
                                    items.map {
                                        if (it.id == item.id) {
                                            it.copy(done = checked)
                                        } else it
                                    }
                                )
                            }
                        )

                        Text(
                            item.title,
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (item.details.isNotBlank()) {
                        Text(item.details)
                    }

                    if (item.date.isNotBlank()) {
                        Text("Дата / время: ${item.date}")
                    }

                    TextButton(
                        onClick = {
                            update(
                                items.filterNot {
                                    it.id == item.id
                                }
                            )
                        }
                    ) {
                        Text("Удалить")
                    }
                }
            }
        }
    }
}

private fun readOrganizer(raw: String): List<OrganizerItem> {
    return runCatching {
        val array = JSONArray(raw)

        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)

            OrganizerItem(
                id = obj.getLong("id"),
                category = obj.getString("category"),
                title = obj.getString("title"),
                details = obj.optString("details"),
                date = obj.optString("date"),
                done = obj.optBoolean("done")
            )
        }
    }.getOrDefault(emptyList())
}

private fun writeOrganizer(
    items: List<OrganizerItem>
): String {
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
        )
    }

    return array.toString()
}
