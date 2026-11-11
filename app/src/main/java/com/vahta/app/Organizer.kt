
package com.vahta.app

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
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

private val orgBackground get() = if(lavenderMode) Color(0xFF1D1630) else Color(0xFF101C2B)
private val orgCard get() = if(lavenderMode) Color(0xFF302641) else Color(0xFF213348)
private val orgAccent get() = if(lavenderMode) Color(0xFFD8B5FF) else Color(0xFF8BE6D0)
private val orgMuted get() = if(lavenderMode) Color(0xFFE6D7F3) else Color(0xFFCCDAE7)
private val orgBorder get() = if(lavenderMode) Color(0xFF675476) else Color(0xFF48637A)

// Различимые пастельные акценты для всех разделов в обеих цветовых темах.
private fun organizerTileColor(category: String): Color = when (category) {
    "Покупки на вахту", "Чемодан" -> if (lavenderMode) Color(0xFF334D53) else Color(0xFF254A53)
    "Подарки", "Отпуск" -> if (lavenderMode) Color(0xFF563C53) else Color(0xFF53445B)
    "Лекарства", "Сон" -> if (lavenderMode) Color(0xFF35495C) else Color(0xFF2B4663)
    "Планы", "Заметки" -> if (lavenderMode) Color(0xFF4C3D66) else Color(0xFF3A4569)
    "Расходы", "Долги" -> if (lavenderMode) Color(0xFF5A413F) else Color(0xFF58463E)
    "Накопления", "Питание" -> if (lavenderMode) Color(0xFF3B5144) else Color(0xFF315747)
    "Тренировки", "Вес" -> if (lavenderMode) Color(0xFF59436B) else Color(0xFF3E4B68)
    else -> orgCard
}
private fun organizerTileAccent(category: String): Color = when (category) {
    "Покупки на вахту", "Чемодан" -> Color(0xFF9CE9D5)
    "Подарки", "Отпуск" -> Color(0xFFFFC8DB)
    "Лекарства", "Сон" -> Color(0xFFAED9FF)
    "Планы", "Заметки" -> Color(0xFFD9C3FF)
    "Расходы", "Долги" -> Color(0xFFFFD0AC)
    "Накопления", "Питание" -> Color(0xFFC4EAA7)
    else -> Color(0xFFE9BDF9)
}

private val organizerCategories = listOf(
    "Покупки на вахту", "Подарки", "Лекарства", "Чемодан",
    "Планы", "Заметки", "Расходы", "Долги", "Накопления",
    "Тренировки", "Питание", "Вес", "Сон",
    "Отпуск"
)

private data class ExerciseTemplate(val group: String,val name: String,val prescription: String,val technique: String)
private val exerciseLibrary = listOf(
    ExerciseTemplate("Зарядка","Разминка суставов","5–7 минут","Плавные вращения плеч, локтей, кистей, таза и голеностопа."),
    ExerciseTemplate("Зарядка","Ходьба на месте","3–5 минут","Умеренный темп, ровное дыхание."),
    ExerciseTemplate("Зарядка","Наклоны корпуса","2 × 10","Двигайся плавно, не округляй резко поясницу."),
    ExerciseTemplate("Зарядка","Приседания без веса","2 × 12","Колени направлены по линии стоп, пятки на полу."),
    ExerciseTemplate("Зарядка","Лёгкая растяжка","5 минут","Без рывков и боли."),
    ExerciseTemplate("Гантели","Жим гантелей стоя","3 × 10","Не прогибай поясницу, контролируй опускание."),
    ExerciseTemplate("Гантели","Сгибание рук с гантелями","3 × 12","Локти близко к корпусу, без раскачивания."),
    ExerciseTemplate("Гантели","Тяга гантели в наклоне","3 × 10 на руку","Спина нейтральная, тяни локоть к тазу."),
    ExerciseTemplate("Гантели","Приседания с гантелью","3 × 12","Держи вес у груди, опускайся подконтрольно."),
    ExerciseTemplate("Гантели","Разведение гантелей в стороны","3 × 12","Лёгкий вес, не поднимай плечи."),
    ExerciseTemplate("Свой вес","Отжимания","3 × 8–15","Корпус прямой; можно выполнять от опоры."),
    ExerciseTemplate("Свой вес","Подтягивания","3 × 4–10","Без рывков, с контролем движения."),
    ExerciseTemplate("Свой вес","Выпады","3 × 10 на ногу","Колено по линии стопы, сохраняй равновесие."),
    ExerciseTemplate("Свой вес","Планка","3 × 30 секунд","Не провисай в пояснице, дыши свободно."),
    ExerciseTemplate("Свой вес","Ягодичный мост","3 × 15","Поднимай таз без переразгибания спины."),
    ExerciseTemplate("Кардио","Быстрая ходьба","20–30 минут","Темп, при котором можно разговаривать."),
    ExerciseTemplate("Кардио","Бег трусцой","15–25 минут","Комфортный темп; увеличивай нагрузку постепенно."),
    ExerciseTemplate("Кардио","Прыжки на месте","3 × 30 секунд","Мягкое приземление, при необходимости замени шагами."),
    ExerciseTemplate("Мобилизация","Кошка-корова","2 × 10","Медленные движения позвоночника."),
    ExerciseTemplate("Мобилизация","Растяжка грудных мышц","2 × 30 секунд","Без боли, плечи расслаблены."),
    ExerciseTemplate("Мобилизация","Растяжка икр","2 × 30 секунд на ногу","Пятка опорной ноги прижата к полу.")
)
private fun exerciseIllustration(name: String): Int = when(name) {
    "Разминка суставов" -> R.drawable.exercise_01
    "Ходьба на месте" -> R.drawable.exercise_02
    "Наклоны корпуса" -> R.drawable.exercise_03
    "Приседания без веса" -> R.drawable.exercise_04
    "Лёгкая растяжка" -> R.drawable.exercise_05
    "Жим гантелей стоя" -> R.drawable.exercise_06
    "Сгибание рук с гантелями" -> R.drawable.exercise_07
    "Тяга гантели в наклоне" -> R.drawable.exercise_08
    "Приседания с гантелью" -> R.drawable.exercise_09
    "Разведение гантелей в стороны" -> R.drawable.exercise_10
    "Отжимания" -> R.drawable.exercise_11
    "Подтягивания" -> R.drawable.exercise_12
    "Выпады" -> R.drawable.exercise_13
    "Планка" -> R.drawable.exercise_14
    "Ягодичный мост" -> R.drawable.exercise_15
    "Быстрая ходьба" -> R.drawable.exercise_16
    "Бег трусцой" -> R.drawable.exercise_17
    "Прыжки на месте" -> R.drawable.exercise_18
    "Кошка-корова" -> R.drawable.exercise_19
    "Растяжка грудных мышц" -> R.drawable.exercise_20
    "Растяжка икр" -> R.drawable.exercise_21
    else -> R.drawable.exercise_01
}

private val exerciseDrawables = intArrayOf(
    R.drawable.exercise_01, R.drawable.exercise_02, R.drawable.exercise_03,
    R.drawable.exercise_04, R.drawable.exercise_05, R.drawable.exercise_06,
    R.drawable.exercise_07, R.drawable.exercise_08, R.drawable.exercise_09,
    R.drawable.exercise_10, R.drawable.exercise_11, R.drawable.exercise_12,
    R.drawable.exercise_13, R.drawable.exercise_14, R.drawable.exercise_15,
    R.drawable.exercise_16, R.drawable.exercise_17, R.drawable.exercise_18,
    R.drawable.exercise_19, R.drawable.exercise_20, R.drawable.exercise_21
)
private fun exerciseDrawable(exercise: ExerciseTemplate): Int {
    val index = exerciseLibrary.indexOfFirst { it.name == exercise.name }
    return exerciseDrawables[index.coerceIn(0, exerciseDrawables.lastIndex)]
}
@Composable
private fun ExerciseDetails(exercise: ExerciseTemplate, onClose: () -> Unit, onAdd: (() -> Unit)? = null) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(exercise.name, color=orgAccent) },
        text = {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Image(
                    painter=painterResource(exerciseDrawable(exercise)),
                    contentDescription="Техника выполнения: ${exercise.name}",
                    modifier=Modifier.fillMaxWidth().height(235.dp),
                    contentScale=ContentScale.Fit
                )
                Text(exercise.prescription, color=orgAccent, fontWeight=FontWeight.Bold)
                Text(exercise.technique, color=Color.White)
                Text("Выполняй движение плавно, без боли.", color=orgMuted, fontSize=12.sp)
            }
        },
        confirmButton = {
            if(onAdd != null) TextButton(onClick=onAdd) { Text("Добавить в мои дела",color=orgAccent) }
            else TextButton(onClick=onClose) { Text("Понятно",color=orgAccent) }
        },
        dismissButton = { if(onAdd != null) TextButton(onClick=onClose) { Text("Закрыть",color=orgMuted) } },
        containerColor=orgCard
    )
}
@Composable
private fun ExerciseVisualCard(exercise: ExerciseTemplate, onClick: () -> Unit) {
    Card(
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),
        colors=CardDefaults.cardColors(containerColor=orgBackground),
        shape=RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment=Alignment.CenterVertically) {
            Image(
                painter=painterResource(exerciseDrawable(exercise)),
                contentDescription=exercise.name,
                modifier=Modifier.size(width=110.dp,height=86.dp),
                contentScale=ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(exercise.name,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                Text(exercise.prescription,color=orgMuted,fontSize=12.sp)
                Text("Посмотреть технику  ›",color=orgAccent,fontSize=12.sp)
            }
        }
    }
}

private data class TrainingProgram(val id: String,val title: String,val duration: Int,val exerciseNames: List<String>)
private val trainingPrograms = listOf(
    TrainingProgram("morning10","Утренняя зарядка",10,listOf("Разминка суставов","Ходьба на месте","Наклоны корпуса","Приседания без веса","Лёгкая растяжка")),
    TrainingProgram("dumbbell30","Гантели: всё тело",30,listOf("Разминка суставов","Приседания с гантелью","Тяга гантели в наклоне","Жим гантелей стоя","Сгибание рук с гантелями","Лёгкая растяжка")),
    TrainingProgram("body20","Без оборудования",20,listOf("Ходьба на месте","Приседания без веса","Отжимания","Выпады","Ягодичный мост","Планка")),
    TrainingProgram("cardio20","Кардио и подвижность",20,listOf("Ходьба на месте","Быстрая ходьба","Кошка-корова","Растяжка икр"))
)
private data class TrainingSession(val programId: String,val day: String,val doneNames: Set<String>,val finished: Boolean)
private fun readTrainingSessions(raw: String): List<TrainingSession> = runCatching {
    val a=JSONArray(raw)
    (0 until a.length()).map { i ->
        val j=a.getJSONObject(i)
        val steps=j.optJSONArray("steps") ?: JSONArray()
        TrainingSession(j.optString("program"),j.optString("day"),
            (0 until steps.length()).map { steps.getString(it) }.toSet(),j.optBoolean("finished"))
    }.filter { it.programId in trainingPrograms.map { p -> p.id } }
}.getOrDefault(emptyList())
private fun writeTrainingSessions(sessions: List<TrainingSession>): String = JSONArray().apply {
    sessions.forEach { session ->
        put(JSONObject().put("program",session.programId).put("day",session.day)
            .put("steps",JSONArray(session.doneNames.toList())).put("finished",session.finished))
    }
}.toString()

@Composable
private fun TrainingProgramsPanel(prefs: android.content.SharedPreferences) {
    var sessions by remember { mutableStateOf(readTrainingSessions(prefs.getString("training_sessions_v1", "[]") ?: "[]")) }
    var selectedProgramId by remember { mutableStateOf<String?>(null) }
    var stepIndex by remember { mutableIntStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var libraryGroup by remember { mutableStateOf("Все") }
    var preview by remember { mutableStateOf<ExerciseTemplate?>(null) }
    val today = LocalDate.now().toString()
    fun save(updated: List<TrainingSession>) {
        sessions = updated
        prefs.edit().putString("training_sessions_v1", writeTrainingSessions(updated)).apply()
    }
    val active = trainingPrograms.firstOrNull { it.id == selectedProgramId }
    val completed = sessions.filter { it.finished }
    val weekly = completed.count { runCatching { !LocalDate.parse(it.day).isBefore(LocalDate.now().minusDays(6)) }.getOrDefault(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (active == null && !showLibrary) {
            Text("ТРЕНИРОВКИ", color = orgAccent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("Выбери программу и занимайся в своём темпе", color = orgMuted, fontSize = 13.sp)
            Card(colors = CardDefaults.cardColors(containerColor = orgBackground), shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, orgBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ТВОЙ ПРОГРЕСС", color = orgMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Последние 7 дней", color = orgMuted, fontSize = 11.sp)
                    }
                    Text("$weekly тренировок", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Всего завершено: ${completed.size}", color = orgMuted, fontSize = 12.sp)
                }
            }
            Text("ПРОГРАММЫ", color = orgAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            trainingPrograms.forEach { program ->
                val finishedCount = completed.count { it.programId == program.id }
                val todaySession = sessions.firstOrNull { it.programId == program.id && it.day == today }
                val done = todaySession?.doneNames?.intersect(program.exerciseNames.toSet())?.size ?: 0
                Card(onClick = {
                    selectedProgramId = program.id
                    stepIndex = program.exerciseNames.indexOfFirst { it !in (todaySession?.doneNames ?: emptySet()) }.let { if (it < 0) 0 else it }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = orgCard), border = BorderStroke(1.dp, orgBorder)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painter = painterResource(exerciseIllustration(program.exerciseNames.first())),
                                contentDescription = program.title, modifier = Modifier.size(92.dp), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(program.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${program.duration} мин · ${program.exerciseNames.size} упражнений", color = orgMuted, fontSize = 12.sp)
                                Text("Завершено $finishedCount раз", color = orgAccent, fontSize = 12.sp)
                            }
                        }
                        if (done > 0) {
                            LinearProgressIndicator(progress = { done.toFloat() / program.exerciseNames.size },
                                modifier = Modifier.fillMaxWidth().height(5.dp), color = orgAccent, trackColor = orgBorder)
                        }
                        Text(if (done > 0) "Продолжить тренировку  →" else "Начать тренировку  →",
                            color = orgAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            OutlinedButton(onClick = { showLibrary = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Каталог всех 21 упражнений", color = orgAccent)
            }
            OutlinedButton(onClick = { showHistory = !showHistory }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showHistory) "Скрыть историю" else "История тренировок", color = orgAccent)
            }
            if (showHistory) {
                if (completed.isEmpty()) Text("Завершённых тренировок пока нет", color = orgMuted)
                completed.sortedByDescending { it.day }.take(30).forEach { record ->
                    val title = trainingPrograms.firstOrNull { it.id == record.programId }?.title ?: record.programId
                    Text("✓ ${record.day} · $title", color = orgMuted, fontSize = 12.sp)
                }
            }
        } else if (active != null) {
            val session = sessions.firstOrNull { it.programId == active.id && it.day == today }
                ?: TrainingSession(active.id, today, emptySet(), false)
            val names = active.exerciseNames
            val index = stepIndex.coerceIn(0, names.lastIndex)
            val exercise = exerciseLibrary.first { it.name == names[index] }
            val doneCount = session.doneNames.intersect(names.toSet()).size
            TextButton(onClick = { selectedProgramId = null }) { Text("‹  Все программы", color = orgAccent) }
            Text(active.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Упражнение ${index + 1} из ${names.size}", color = orgMuted, fontSize = 13.sp)
                Text("Выполнено $doneCount/${names.size}", color = orgAccent, fontSize = 13.sp)
            }
            LinearProgressIndicator(progress = { doneCount.toFloat() / names.size },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = orgAccent, trackColor = orgBorder)
            Card(colors = CardDefaults.cardColors(containerColor = orgBackground),
                shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, orgBorder)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painter = painterResource(exerciseIllustration(exercise.name)),
                        contentDescription = "Техника: ${exercise.name}", modifier = Modifier.fillMaxWidth().height(235.dp),
                        contentScale = ContentScale.Fit)
                    Text(exercise.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(exercise.prescription, color = orgAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(exercise.technique, color = orgMuted, fontSize = 14.sp)
                    Text("Двигайся плавно, без боли. При дискомфорте остановись.", color = orgMuted, fontSize = 12.sp)
                }
            }
            Button(onClick = {
                val updated = session.copy(doneNames = if (exercise.name in session.doneNames) session.doneNames - exercise.name else session.doneNames + exercise.name,
                    finished = false)
                save(sessions.filterNot { it.programId == active.id && it.day == today } + updated)
            }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = orgAccent, contentColor = orgBackground)) {
                Text(if (exercise.name in session.doneNames) "✓ Выполнено · отменить" else "✓ Отметить выполненным")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { stepIndex = (index - 1).coerceAtLeast(0) },
                    enabled = index > 0, modifier = Modifier.weight(1f)) { Text("‹ Назад", color = orgAccent) }
                OutlinedButton(onClick = { stepIndex = (index + 1).coerceAtMost(names.lastIndex) },
                    enabled = index < names.lastIndex, modifier = Modifier.weight(1f)) { Text("Следующее ›", color = orgAccent) }
            }
            if (doneCount == names.size) {
                Button(onClick = {
                    save(sessions.filterNot { it.programId == active.id && it.day == today } + session.copy(finished = true))
                    selectedProgramId = null
                }, enabled = !session.finished, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = orgAccent, contentColor = orgBackground)) {
                    Text(if (session.finished) "Тренировка завершена" else "Завершить тренировку")
                }
            }
            TextButton(onClick = { save(sessions.filterNot { it.programId == active.id && it.day == today }); stepIndex = 0 },
                modifier = Modifier.fillMaxWidth()) { Text("Сбросить прогресс за сегодня", color = orgMuted) }
        } else {
            TextButton(onClick = { showLibrary = false }) { Text("‹  К программам", color = orgAccent) }
            Text("Каталог упражнений", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("21 упражнение с изображениями и техникой", color = orgMuted, fontSize = 12.sp)
            listOf("Все", "Зарядка", "Гантели", "Свой вес", "Кардио", "Мобилизация").forEach { group ->
                if (group == libraryGroup) Text("● $group", color = orgAccent, fontWeight = FontWeight.Bold)
                else TextButton(onClick = { libraryGroup = group }) { Text(group, color = orgMuted) }
            }
            exerciseLibrary.filter { libraryGroup == "Все" || it.group == libraryGroup }.forEach { exercise ->
                ExerciseVisualCard(exercise) { preview = exercise }
            }
        }
    }
    preview?.let { ExerciseDetails(it, onClose = { preview = null }) }
}

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
    var exerciseGroup by remember { mutableStateOf("Зарядка") }
    var exerciseLibraryOpen by remember { mutableStateOf(false) }
    var catalogPreview by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var exercisePreview by remember { mutableStateOf<ExerciseTemplate?>(null) }

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
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (activeCategory == null) {
            Text("МОИ ДЕЛА", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Всё важное — в одном месте", color = orgMuted, fontSize = 14.sp)
            val allRecords = items.size
            val pendingRecords = items.count { !it.done && it.category !in organizerJournalCategories }
            Card(colors = CardDefaults.cardColors(containerColor = orgCard), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Всего записей", color = orgMuted, fontSize = 12.sp)
                        Text("$allRecords", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                    Column(horizontalAlignment = Alignment.End) { Text("Активных дел", color = orgMuted, fontSize = 12.sp)
                        Text("$pendingRecords", color = orgAccent, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Text("РАЗДЕЛЫ", color = orgMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

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
                                .height(144.dp)
                                .clickable {
                                    category = option
                                    clearForm()
                                },
                            colors = CardDefaults.cardColors(containerColor = organizerTileColor(option)),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    organizerCategoryIcon(option),
                                    fontSize = 24.sp,
                                    color = organizerTileAccent(option)
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
                                    color = organizerTileAccent(option)
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
                shape = RoundedCornerShape(24.dp)
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
                    if (activeCategory == "Тренировки") {
                        TrainingProgramsPanel(prefs)
                        HorizontalDivider(color=orgBorder)
                        OutlinedButton(
                            onClick = { exerciseLibraryOpen = !exerciseLibraryOpen },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if(exerciseLibraryOpen) "Скрыть упражнения" else "Выбрать упражнение из каталога", color = orgAccent) }
                        if(exerciseLibraryOpen) {
                            Text("Выбери направление и упражнение. Его можно изменить перед сохранением.",color=orgMuted,fontSize=12.sp)
                            listOf("Зарядка","Гантели","Свой вес","Кардио","Мобилизация").forEach { group ->
                                OutlinedButton(
                                    onClick = { exerciseGroup = group },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if(exerciseGroup==group) orgBorder else Color.Transparent
                                    )
                                ) { Text(group,color=orgAccent) }
                            }
                            exerciseLibrary.filter { it.group==exerciseGroup }.forEach { exercise ->
                                ExerciseVisualCard(exercise) { catalogPreview=exercise }
                            }
                        }
                        Text("Нагрузку подбирай под свою подготовку. При боли прекращай упражнение.",color=orgMuted,fontSize=11.sp)
                    }
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
                shape = RoundedCornerShape(24.dp),
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
                shape = RoundedCornerShape(24.dp)
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
                    shape = RoundedCornerShape(24.dp)
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

    exercisePreview?.let { exercise ->
        AlertDialog(
            onDismissRequest={exercisePreview=null},
            title={Text(exercise.name,color=orgAccent)},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Image(painter=painterResource(id=exerciseIllustration(exercise.name)),
                    contentDescription="Схема двух фаз упражнения: ${exercise.name}",
                    modifier=Modifier.fillMaxWidth().height(195.dp),contentScale=ContentScale.Fit)
                Text("ПЛАН ТРЕНИРОВКИ",color=orgMuted,fontWeight=FontWeight.Bold,fontSize=11.sp)
                Text("Рекомендуемая нагрузка: ${exercise.prescription}",color=orgAccent)
                Text("Как выполнять: ${exercise.technique}",color=Color.White)
                Text("Выполняй плавно, без боли. При дискомфорте прекрати упражнение.",color=orgMuted,fontSize=12.sp)
            }},
            confirmButton={TextButton(onClick={
                clearForm();title=exercise.name;details=exercise.technique;unit=exercise.prescription
                date=LocalDate.now().toString();formOpen=true;exercisePreview=null;exerciseLibraryOpen=false
            }) { Text("Добавить",color=orgAccent) }},
            dismissButton={TextButton(onClick={exercisePreview=null}) { Text("Закрыть") }},
            containerColor=orgCard
        )
    }
    catalogPreview?.let { exercise ->
        ExerciseDetails(exercise, onClose={catalogPreview=null}, onAdd={
            clearForm()
            title=exercise.name
            details=exercise.technique
            unit=exercise.prescription
            date=LocalDate.now().toString()
            formOpen=true
            exerciseLibraryOpen=false
            catalogPreview=null
        })
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
