
package com.vahta.app

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration

private val orgBackground get() = if(lightMode) Color(0xFFF7F8FC) else if(lavenderMode) Color(0xFF1B1930) else Color(0xFF101E2B)
private val orgCard get() = if(lightMode) Color.White else if(lavenderMode) Color(0xFF332D50) else Color(0xFF213B50)
private val orgAccent get() = if(lightMode) Color(0xFF247D77) else if(lavenderMode) Color(0xFFE9B8FF) else Color(0xFF7CF0D1)
private val orgMuted get() = if(lightMode) Color(0xFF526176) else if(lavenderMode) Color(0xFFEDE2F5) else Color(0xFFD9EAF5)
private val orgBorder get() = if(lightMode) Color(0xFFD8D5E7) else if(lavenderMode) Color(0xFF88749B) else Color(0xFF5B8591)

// 14 самостоятельных палитр: отдельные цвета для каждой категории.
private fun organizerTileColor(category: String): Color = if (lightMode) when (category) {
    "Покупки на вахту" -> Color(0xFFDDF6EB)
    "Подарки" -> Color(0xFFFFE3ED)
    "Лекарства" -> Color(0xFFDCEFFF)
    "Чемодан" -> Color(0xFFEEE4FF)
    "Планы" -> Color(0xFFFFE9D7)
    "Заметки" -> Color(0xFFFFF4C9)
    "Расходы" -> Color(0xFFFFE2D9)
    "Долги" -> Color(0xFFFADBEA)
    "Накопления" -> Color(0xFFE0F5D9)
    "Тренировки" -> Color(0xFFE9E2FF)
    "Питание" -> Color(0xFFFFF0D9)
    "Вес" -> Color(0xFFD6F3F1)
    "Сон" -> Color(0xFFE2E9FF)
    "Отпуск" -> Color(0xFFDBF0FF)
    else -> orgCard
} else if (lavenderMode) when (category) {
    "Покупки на вахту" -> Color(0xFF336F68)
    "Подарки" -> Color(0xFF854E78)
    "Лекарства" -> Color(0xFF41658D)
    "Чемодан" -> Color(0xFF6A5196)
    "Планы" -> Color(0xFF8B5A4A)
    "Заметки" -> Color(0xFF7E693E)
    "Расходы" -> Color(0xFF86505D)
    "Долги" -> Color(0xFF884A77)
    "Накопления" -> Color(0xFF39775E)
    "Тренировки" -> Color(0xFF71539A)
    "Питание" -> Color(0xFF895D4B)
    "Вес" -> Color(0xFF33737E)
    "Сон" -> Color(0xFF50658F)
    "Отпуск" -> Color(0xFF427499)
    else -> orgCard
} else when (category) {
    "Покупки на вахту" -> Color(0xFF246B62)
    "Подарки" -> Color(0xFF75405F)
    "Лекарства" -> Color(0xFF2E5F87)
    "Чемодан" -> Color(0xFF554B91)
    "Планы" -> Color(0xFF865640)
    "Заметки" -> Color(0xFF736332)
    "Расходы" -> Color(0xFF854B51)
    "Долги" -> Color(0xFF7E3C68)
    "Накопления" -> Color(0xFF316F50)
    "Тренировки" -> Color(0xFF625195)
    "Питание" -> Color(0xFF865A3B)
    "Вес" -> Color(0xFF1F707A)
    "Сон" -> Color(0xFF425A8A)
    "Отпуск" -> Color(0xFF2D698D)
    else -> orgCard
}
private fun organizerTileAccent(category: String): Color = if (lightMode) Color(0xFF27394B) else when (category) {
    "Покупки на вахту" -> Color(0xFFA3EBD2)
    "Подарки" -> Color(0xFFFFC1DA)
    "Лекарства" -> Color(0xFFA7DAFF)
    "Чемодан" -> Color(0xFFD6C0FF)
    "Планы" -> Color(0xFFFFD0A7)
    "Заметки" -> Color(0xFFFFE4A0)
    "Расходы" -> Color(0xFFFFC0AF)
    "Долги" -> Color(0xFFFFB5D6)
    "Накопления" -> Color(0xFFC3ECA6)
    "Тренировки" -> Color(0xFFD8BFFF)
    "Питание" -> Color(0xFFFFDAA6)
    "Вес" -> Color(0xFF9EE4E1)
    "Сон" -> Color(0xFFC0D1FF)
    "Отпуск" -> Color(0xFFAEDFFF)
    else -> orgAccent
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
                Text(exercise.technique, color=vahtaText)
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
                Text(exercise.name,color=vahtaText,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
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
    var confirmReset by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf("") }
    var historyLimit by remember { mutableIntStateOf(10) }
    val today = LocalDate.now().toString()
    fun save(updated: List<TrainingSession>) {
        sessions = updated
        val success = prefs.edit().putString("training_sessions_v1", writeTrainingSessions(updated)).commit()
        savedMessage = if (success) "Прогресс сохранён" else "Не удалось сохранить прогресс"
    }
    val active = trainingPrograms.firstOrNull { it.id == selectedProgramId }
    val completed = sessions.filter { it.finished }
    val weekly = completed.count { runCatching {
        val date = LocalDate.parse(it.day)
        !date.isBefore(LocalDate.now().minusDays(6)) && !date.isAfter(LocalDate.now())
    }.getOrDefault(false) }
    val daysTrained = completed.map { it.day }.distinct().size
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
                    Text("$weekly тренировок", color = vahtaText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Всего завершено: ${completed.size} · Дней с тренировками: $daysTrained", color = orgMuted, fontSize = 12.sp)
                    val weeklyGoal = 3
                    Text("Недельная цель: $weekly из $weeklyGoal", color = orgMuted, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { (weekly.toFloat() / weeklyGoal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp), color = orgAccent, trackColor = orgBorder)
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
                                Text(program.title, color = vahtaText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                completed.sortedByDescending { it.day }.take(historyLimit).forEach { record ->
                    val title = trainingPrograms.firstOrNull { it.id == record.programId }?.title ?: record.programId
                    Card(colors = CardDefaults.cardColors(containerColor = orgCard),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("✓ $title", color = vahtaText, fontWeight = FontWeight.SemiBold)
                            Text("${record.day} · ${record.doneNames.size} упражнений", color = orgMuted, fontSize = 12.sp)
                        }
                    }
                }
                if (completed.size > historyLimit) {
                    OutlinedButton(onClick = { historyLimit += 20 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Показать ещё", color = orgAccent)
                    }
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
            Text(active.title, color = vahtaText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Упражнение ${index + 1} из ${names.size}", color = orgMuted, fontSize = 13.sp)
                Text("Выполнено $doneCount/${names.size}", color = orgAccent, fontSize = 13.sp)
            }
            LinearProgressIndicator(progress = { doneCount.toFloat() / names.size },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = orgAccent, trackColor = orgBorder)
            Card(colors = CardDefaults.cardColors(containerColor = orgBackground),
                shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, orgBorder)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painter = painterResource(exerciseIllustration(exercise.name)),
                        contentDescription = "Техника: ${exercise.name}", modifier = Modifier.fillMaxWidth().height(235.dp),
                        contentScale = ContentScale.Fit)
                    Text(exercise.name, color = vahtaText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Сбросить прогресс за сегодня", color = orgMuted)
            }
            if (savedMessage.isNotEmpty()) Text(savedMessage, color = orgAccent, fontSize = 12.sp)
            if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
                title = { Text("Сбросить тренировку?") },
                text = { Text("Все отметки этой программы за сегодня будут удалены. История других дней сохранится.") },
                confirmButton = { TextButton(onClick = {
                    save(sessions.filterNot { it.programId == active.id && it.day == today })
                    stepIndex = 0
                    confirmReset = false
                }) { Text("Удалить") } },
                dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } })
        } else {
            TextButton(onClick = { showLibrary = false }) { Text("‹  К программам", color = orgAccent) }
            Text("Каталог упражнений", color = vahtaText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
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

// Sleep times are stored in existing fields for backwards-compatible JSON persistence.
// unit = bedtime, time = wake time, medicineForm = nap minutes, amount = total hours.
private fun sleepMinutes(start: String, end: String): Long? {
    val from = runCatching { LocalTime.parse(start) }.getOrNull() ?: return null
    val to = runCatching { LocalTime.parse(end) }.getOrNull() ?: return null
    val minutes = Duration.between(from, to).toMinutes().let { if (it <= 0) it + 1440 else it }
    return minutes.takeIf { it in 1..1440 }
}
private fun sleepDuration(start: String, end: String, nap: String): Double? {
    val minutes = sleepMinutes(start, end) ?: return null
    val extra = nap.trim().ifBlank { "0" }.toIntOrNull() ?: return null
    if (extra !in 0..720) return null
    return (minutes + extra) / 60.0
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
    val debtDirection: String = "Я должен",
    val debtPaid: Double = 0.0,
    val amount: Double = 0.0,
    val unit: String = "",
    val time: String = "",
    val medicineForm: String = "",
    val medicineStock: Int = 0,
    val medicineExpiry: String = "",
    val pinned: Boolean = false,
    val expenseRecurring: Boolean = false,
    val savingsTarget: Double = 0.0,
    val savingsBalance: Double = 0.0,
    val vacationEnd: String = "",
    val vacationBudget: Double = 0.0,
    val vacationBooking: String = "",
    val vacationChecklist: String = "",
    val plannedCost: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(context: Context, resetToRoot: Int = 0) {
    val prefs = remember(context) {
        context.getSharedPreferences("vahta_organizer", Context.MODE_PRIVATE)
    }
    var items by remember {
        mutableStateOf(readOrganizer(prefs.getString("items", "[]") ?: "[]"))
    }
    var category by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(resetToRoot) { category = null }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var formOpen by remember { mutableStateOf(false) }
    var saveNotice by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var debtBalance by remember { mutableStateOf("") }
    var debtMonthly by remember { mutableStateOf("") }
    var debtDueDay by remember { mutableStateOf("") }
    var debtDirection by remember { mutableStateOf("Я должен") }
    var debtPaid by remember { mutableStateOf("") }
    var repaymentId by remember { mutableStateOf<Long?>(null) }
    var repaymentAmount by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var shoppingStep by remember { mutableStateOf(100) }
    var time by remember { mutableStateOf("") }
    var medicineForm by remember { mutableStateOf("") }
    var medicineStock by remember { mutableStateOf("") }
    var medicineExpiry by remember { mutableStateOf("") }
    var notePinned by remember { mutableStateOf(false) }
    var noteChecklist by remember { mutableStateOf("") }
    var expenseRecurring by remember { mutableStateOf(false) }
    var savingsTarget by remember { mutableStateOf("") }
    var savingsBalance by remember { mutableStateOf("") }
    var vacationEnd by remember { mutableStateOf("") }
    var vacationBudget by remember { mutableStateOf("") }
    var plannedCost by remember { mutableStateOf("") }
    var vacationBooking by remember { mutableStateOf("") }
    var vacationChecklist by remember { mutableStateOf("") }
    var savingsTransactionId by remember { mutableStateOf<Long?>(null) }
    var savingsTransactionAmount by remember { mutableStateOf("") }
    var savingsWithdrawal by remember { mutableStateOf(false) }
    var expenseMonth by remember { mutableStateOf(java.time.YearMonth.now().toString()) }
    var quickTemplatesOpen by remember { mutableStateOf(false) }
    var recordFilter by remember { mutableStateOf("Все") }
    var exerciseGroup by remember { mutableStateOf("Зарядка") }
    var exerciseLibraryOpen by remember { mutableStateOf(false) }
    var catalogPreview by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var exercisePreview by remember { mutableStateOf<ExerciseTemplate?>(null) }

    fun update(newItems: List<OrganizerItem>): Boolean {
        val saved = prefs.edit().putString("items", writeOrganizer(newItems)).commit()
        if (saved) items = newItems
        return saved
    }

    fun clearForm() {
        editingId = null
        title = ""
        details = ""
        date = ""
        debtBalance = ""
        debtMonthly = ""
        debtDueDay = ""
        debtDirection = "Я должен"
        debtPaid = ""
        amount = ""
        unit = ""
        time = ""
        medicineForm = ""
        medicineStock = ""
        medicineExpiry = ""
        notePinned = false
        noteChecklist = ""
        expenseRecurring = false
        savingsTarget = ""
        savingsBalance = ""
        vacationEnd = ""
        vacationBudget = ""
        plannedCost = ""
        vacationBooking = ""
        vacationChecklist = ""
        formOpen = false
    }

    repaymentId?.let { debtId ->
        val target = items.firstOrNull { it.id == debtId }
        if (target != null) {
            AlertDialog(
                onDismissRequest = { repaymentId = null; repaymentAmount = "" },
                title = { Text(if (target.debtDirection == "Мне должны") "Получен возврат" else "Погасить часть долга") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Остаток: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.2f", target.debtBalance)} ₽")
                        OutlinedTextField(value = repaymentAmount, onValueChange = { repaymentAmount = it }, label = { Text("Сумма, ₽") }, singleLine = true)
                    }
                },
                confirmButton = {
                    val payment = repaymentAmount.replace(",", ".").toDoubleOrNull()
                    TextButton(enabled = payment != null && payment.isFinite() && payment > 0 && payment <= target.debtBalance,
                        onClick = {
                            val paid = repaymentAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val saved = update(items.map { item -> if (item.id == debtId) item.copy(
                                debtBalance = (item.debtBalance - paid).coerceAtLeast(0.0),
                                debtPaid = item.debtPaid + paid,
                                done = item.debtBalance - paid < 0.005
                            ) else item })
                            saveNotice = if (saved) "Платёж сохранён" else "Не удалось сохранить платёж"
                            if (saved) { repaymentId = null; repaymentAmount = "" }
                        }) { Text("Подтвердить платёж") }
                },
                dismissButton = { TextButton(onClick = { repaymentId = null; repaymentAmount = "" }) { Text("Отмена") } }
            )
        }
    }
    if (savingsTransactionId != null) {
        val goal = items.firstOrNull { it.id == savingsTransactionId }
        if (goal != null) AlertDialog(
            onDismissRequest = { savingsTransactionId = null },
            title = { Text(if (savingsWithdrawal) "Снять с накоплений" else "Пополнить накопления") },
            text = {
                Column {
                    Text(goal.title)
                    OrganizerInput("Сумма, ₽", savingsTransactionAmount) { savingsTransactionAmount = it }
                    Text("Текущий баланс: ${goal.savingsBalance.formatOrg()} ₽")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = savingsTransactionAmount.replace(",", ".").toDoubleOrNull()?.let {
                        it.isFinite() && it > 0 && (!savingsWithdrawal || it <= goal.savingsBalance)
                    } == true,
                    onClick = {
                        val delta = savingsTransactionAmount.replace(",", ".").toDouble()
                        val newBalance = goal.savingsBalance + if (savingsWithdrawal) -delta else delta
                        val saved = update(items.map { if (it.id == goal.id) it.copy(savingsBalance = newBalance) else it })
                        saveNotice = if (saved) "Баланс накоплений сохранён" else "Ошибка сохранения баланса"
                        savingsTransactionId = null
                        savingsTransactionAmount = ""
                    }
                ) { Text("Подтвердить") }
            },
            dismissButton = { TextButton(onClick = { savingsTransactionId = null }) { Text("Отмена") } }
        )
    }
    if (saveNotice != null) {
        AlertDialog(
            onDismissRequest = { saveNotice = null },
            title = { Text("Готово") },
            text = { Text(saveNotice ?: "Изменения сохранены") },
            confirmButton = { TextButton(onClick = { saveNotice = null }) { Text("ОК") } }
        )
    }
    var debtSubsection by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(category) { debtSubsection = null }
    BackHandler(enabled = category != null) {
        if (category == "Долги" && debtSubsection != null) debtSubsection = null else category = null
        formOpen = false
        editingId = null
    }
    val activeCategory = category
    val visible = items.filter { it.category == activeCategory && (activeCategory != "Долги" || debtSubsection == null || it.debtDirection == debtSubsection) }
    val completed = visible.count { it.done }
    fun startEdit(item: OrganizerItem) {
        editingId = item.id
        title = item.title
        details = if (item.category == "Заметки") item.details.lines().filterNot { it.trimStart().startsWith("☐ ") || it.trimStart().startsWith("☑ ") }.joinToString("\n").trim() else item.details
        noteChecklist = if (item.category == "Заметки") item.details.lines().filter { it.trimStart().startsWith("☐ ") || it.trimStart().startsWith("☑ ") }.joinToString("\n") { it.trimStart().removePrefix("☐ ").removePrefix("☑ ") } else ""
        date = item.date
        debtBalance = if (item.debtBalance > 0) item.debtBalance.toString() else ""
        debtMonthly = if (item.debtMonthly > 0) item.debtMonthly.toString() else ""
        debtDueDay = if (item.debtDueDay > 0) item.debtDueDay.toString() else ""
        debtDirection = item.debtDirection
        debtPaid = if (item.debtPaid > 0) item.debtPaid.toString() else ""
        amount = if (item.amount != 0.0) item.amount.toString() else ""
        unit = item.unit
        time = item.time
        medicineForm = item.medicineForm
        medicineStock = item.medicineStock.toString()
        medicineExpiry = item.medicineExpiry
        notePinned = item.pinned
        expenseRecurring = item.expenseRecurring
        savingsTarget = if (item.savingsTarget > 0) item.savingsTarget.toString() else ""
        savingsBalance = item.savingsBalance.toString()
        vacationEnd = item.vacationEnd
        vacationBudget = if (item.vacationBudget > 0) item.vacationBudget.toString() else ""
        plannedCost = if (item.plannedCost > 0) item.plannedCost.toString() else ""
        vacationBooking = item.vacationBooking
        vacationChecklist = item.vacationChecklist
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
            Text("МОИ ДЕЛА", color = vahtaText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            
            val allRecords = items.size
            val pendingRecords = items.count { !it.done && it.category !in organizerJournalCategories }
            Card(colors = CardDefaults.cardColors(containerColor = orgCard), shape = RoundedCornerShape(26.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Всего записей", color = orgMuted, fontSize = 12.sp)
                        Text("$allRecords", color = vahtaText, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
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
                                .height(138.dp)
                                .clickable {
                                    category = option
                                    recordFilter = "Все"
                                    clearForm()
                                },
                            colors = CardDefaults.cardColors(containerColor = organizerTileColor(option)),
                            shape = RoundedCornerShape(26.dp)
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
                                    color = vahtaText,
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
            Card(colors=CardDefaults.cardColors(containerColor=organizerTileColor(activeCategory)),shape=RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text(organizerCategoryIcon(activeCategory),fontSize=34.sp,color=organizerTileAccent(activeCategory))
                    Column {
                        Text(activeCategory,color=vahtaText,fontSize=24.sp,fontWeight=FontWeight.Bold)
                        Text("${visible.size} записей · ${visible.count { !it.done }} активных",color=organizerTileAccent(activeCategory),fontSize=12.sp)
                    }
                }
            }

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
                    if (activeCategory == "Сон") {
                        val logs = visible.filter { it.amount > 0 && runCatching { LocalDate.parse(it.date) }.isSuccess }
                            .sortedWith(compareByDescending<OrganizerItem> { it.date }.thenByDescending { it.id })
                        Card(colors = CardDefaults.cardColors(containerColor = organizerTileColor("Сон")), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("СТАТИСТИКА СНА", color = organizerTileAccent("Сон"), fontWeight = FontWeight.Bold)
                                if (logs.isEmpty()) Text("Добавь первую запись сна", color = vahtaText)
                                else {
                                    val recent = logs.take(7)
                                    Text("Последняя ночь: ${logs.first().amount.formatOrg()} ч", color = vahtaText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("Среднее за последние ${recent.size} записей: ${(recent.sumOf { it.amount } / recent.size).formatOrg()} ч", color = vahtaText)
                                    Text("Записей: ${logs.size}", color = orgMuted)
                                    recent.asReversed().forEach { entry ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(entry.date.takeLast(5), color = orgMuted, fontSize = 11.sp)
                                            LinearProgressIndicator(progress = { (entry.amount / 12.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(8.dp), color = organizerTileAccent("Сон"), trackColor = orgBorder)
                                            Text("${entry.amount.formatOrg()} ч", color = vahtaText, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (activeCategory == "Вес") {
                        val measurements = visible.filter { it.amount > 0.0 && it.amount.isFinite() }
                            .sortedWith(compareBy<OrganizerItem> { runCatching { LocalDate.parse(it.date) }.getOrDefault(LocalDate.MIN) }.thenBy { it.id })
                        val latest = measurements.lastOrNull()
                        val first = measurements.firstOrNull()
                        val goal = latest?.unit?.replace(",", ".")?.toDoubleOrNull()?.takeIf { it > 0.0 }
                        Card(colors = CardDefaults.cardColors(containerColor = organizerTileColor("Вес")), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("ДИНАМИКА ВЕСА", color = organizerTileAccent("Вес"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (latest == null) {
                                    Text("Добавь первое измерение, чтобы увидеть динамику.", color = vahtaText)
                                } else {
                                    Text("${latest.amount.formatOrg()} кг", color = vahtaText, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                    if (first != null && measurements.size > 1) {
                                        val delta = latest.amount - first.amount
                                        Text("С первого измерения: ${if (delta > 0) "+" else ""}${delta.formatOrg()} кг", color = organizerTileAccent("Вес"))
                                    }
                                    if (goal != null) {
                                        val remaining = kotlin.math.abs(latest.amount - goal)
                                        Text("Цель: ${goal.formatOrg()} кг · Разница: ${remaining.formatOrg()} кг", color = vahtaText)
                                    }
                                    Text("Последние измерения", color = orgMuted, fontSize = 12.sp)
                                    val recent = measurements.takeLast(8)
                                    val minWeight = recent.minOf { it.amount }
                                    val maxWeight = recent.maxOf { it.amount }
                                    recent.forEach { entry ->
                                        val fraction = if (maxWeight - minWeight < 0.01) 0.65f else (0.18 + 0.82 * (entry.amount - minWeight) / (maxWeight - minWeight)).toFloat().coerceIn(0f, 1f)
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(entry.date.takeLast(5), color = orgMuted, fontSize = 11.sp, modifier = Modifier.width(42.dp))
                                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f).height(8.dp), color = organizerTileAccent("Вес"), trackColor = orgBorder)
                                            Text("${entry.amount.formatOrg()} кг", color = vahtaText, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (activeCategory == "Питание") {
                        val today = LocalDate.now()
                        val todayMeals = visible.filter { it.date == today.toString() }
                        val todayCost = todayMeals.sumOf { it.time.replace(",", ".").toDoubleOrNull() ?: 0.0 }
                        val monthMeals = visible.filter { runCatching { java.time.YearMonth.from(LocalDate.parse(it.date)) == java.time.YearMonth.from(today) }.getOrDefault(false) }
                        Card(colors = CardDefaults.cardColors(containerColor = organizerTileColor("Питание")), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("ДНЕВНИК ПИТАНИЯ", color = organizerTileAccent("Питание"), fontWeight = FontWeight.Bold)
                                Text("Сегодня: ${todayMeals.size} приёмов пищи · ${todayMeals.sumOf { it.amount }.formatOrg()} ккал", color = vahtaText)
                                Text("Личные траты сегодня: ${todayCost.formatOrg()} ₽", color = vahtaText)
                                Text("За текущий месяц: ${monthMeals.sumOf { it.time.replace(",", ".").toDoubleOrNull() ?: 0.0 }.formatOrg()} ₽", color = orgMuted)
                                
                            }
                        }
                    }
                    if (activeCategory == "Расходы") {
                        val month = runCatching { java.time.YearMonth.parse(expenseMonth) }.getOrDefault(java.time.YearMonth.now())
                        val monthRecords = visible.filter { runCatching { java.time.YearMonth.from(LocalDate.parse(it.date)) == month }.getOrDefault(false) }
                        val monthlyTotal = monthRecords.sumOf { it.amount }
                        val plannedRecurring = visible.filter { it.expenseRecurring && runCatching { !java.time.YearMonth.from(LocalDate.parse(it.date)).isAfter(month) }.getOrDefault(false) }.sumOf { it.amount }
                        Card(colors = CardDefaults.cardColors(containerColor = organizerTileColor(activeCategory)), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("РАСХОДЫ ЗА МЕСЯЦ", color = organizerTileAccent(activeCategory), fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { expenseMonth = month.minusMonths(1).toString() }) { Text("‹", color = orgAccent) }
                                    Text("${month.monthValue.toString().padStart(2, '0')}.${month.year}", color = vahtaText, fontWeight = FontWeight.Bold)
                                    OutlinedButton(onClick = { expenseMonth = month.plusMonths(1).toString() }) { Text("›", color = orgAccent) }
                                }
                                val shoppingSpend = items.filter { it.category == "Покупки на вахту" && it.done && it.date.startsWith(month.toString()) }.sumOf { purchase ->
                                    val rate = purchase.time.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    purchase.amount * rate * if (purchase.unit == "г" || purchase.unit == "мл") 0.001 else 1.0
                                }
                                Text("Всего с покупками: ${(monthlyTotal + shoppingSpend).formatOrg()} ₽", color = vahtaText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Другие расходы: ${monthlyTotal.formatOrg()} ₽ · Купленные товары: ${shoppingSpend.formatOrg()} ₽", color = orgMuted, fontSize = 12.sp)
                                Text("Записей: ${monthRecords.size} · Регулярные по плану: ${plannedRecurring.formatOrg()} ₽/мес", color = orgMuted, fontSize = 12.sp)
                                monthRecords.groupBy { it.unit.ifBlank { "Без категории" } }.toList().sortedByDescending { it.second.sumOf { r -> r.amount } }.forEach { (name, records) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(name, color = orgMuted, fontSize = 12.sp)
                                        Text("${records.sumOf { it.amount }.formatOrg()} ₽", color = orgAccent, fontSize = 12.sp)
                                    }
                                }
                                Text("Личный журнал: суммы не вычитаются повторно из зарплаты.", color = orgMuted, fontSize = 11.sp)
                            }
                        }
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
                        val totalUnits = visible.sumOf { it.amount.toInt().coerceAtLeast(1) }
                        val packedUnits = visible.filter { it.done }.sumOf { it.amount.toInt().coerceAtLeast(1) }
                        Text("Собрано $packedUnits из $totalUnits вещей · " +
                            (if (totalUnits == 0) "0" else (packedUnits * 100 / totalUnits).toString()) + "%",
                            color = organizerTileAccent(activeCategory), fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { if (totalUnits == 0) 0f else packedUnits.toFloat() / totalUnits },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = organizerTileAccent(activeCategory), trackColor = orgBorder
                        )
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
                                        val group = when (thing) {
                                            "Документы" -> "Документы"
                                            "Телефон и зарядка", "Наушники" -> "Техника"
                                            "Обувь" -> "Обувь"
                                            "Зубная щётка и паста", "Средства гигиены", "Полотенце", "Аптечка" -> "Гигиена"
                                            else -> "Одежда"
                                        }
                                        if (update(items + OrganizerItem((items.maxOfOrNull { it.id } ?: 0L) + 1L, "Чемодан", thing, "", "", false, amount = 1.0, unit = group))) {
                                            saveNotice = "Вещь «$thing» добавлена в чемодан"
                                        } else saveNotice = "Не удалось сохранить вещь"
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
                            if (activeCategory == "Чемодан") amount = "1"
                            if (activeCategory == "Вес") date = LocalDate.now().toString()
                            formOpen = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = orgAccent,
                        contentColor = orgBackground
                    )
                ) {
                    Text(if (formOpen) "Закрыть форму" else if (activeCategory == "Заметки") "+ Новая заметка" else "+ Добавить")
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
                    Text(if (editingId == null) "Новая запись" else "Редактирование", color = orgAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (activeCategory != "Вес") OrganizerInput(organizerTitleLabel(activeCategory), title) { title = it }
                    if (activeCategory == "Долги") {
                        Text("Направление задолженности", color = orgMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Я должен", "Мне должны").forEach { direction ->
                                FilterChip(selected = debtDirection == direction, onClick = { debtDirection = direction }, label = { Text(direction) })
                            }
                        }
                        OrganizerInput("Остаток долга, ₽", debtBalance) { debtBalance = it }
                        OrganizerInput("Уже погашено, ₽", debtPaid) { debtPaid = it }
                        OrganizerInput("Обязательный платёж в месяц, ₽", debtMonthly) { debtMonthly = it }
                        OrganizerInput("Число платежа (1–31, необязательно)", debtDueDay) { debtDueDay = it }
                        
                    }
                    if (activeCategory == "Заметки") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = notePinned, onCheckedChange = { notePinned = it })
                            Text("Закрепить заметку", color = orgMuted)
                        }

                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text("Текст заметки") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = vahtaText,
                                unfocusedTextColor = vahtaText,
                                focusedBorderColor = orgAccent,
                                unfocusedBorderColor = orgBorder
                            )
                        )
                        OutlinedTextField(
                            value = noteChecklist,
                            onValueChange = { noteChecklist = it },
                            label = { Text("Чек-лист (по одному пункту)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = vahtaText,
                                unfocusedTextColor = vahtaText,
                                focusedBorderColor = orgAccent,
                                unfocusedBorderColor = orgBorder
                            )
                        )

                    } else {
                        if (activeCategory == "Планы") {
                            OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text("Этапы плана — каждый с новой строки") },
                                placeholder = { Text("Подготовить материалы\nСогласовать сроки\nВыполнить задачу") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                                minLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = vahtaText,
                                    unfocusedTextColor = vahtaText,
                                    focusedBorderColor = orgAccent,
                                    unfocusedBorderColor = orgBorder
                                )
                            )

                        } else if (activeCategory != "Вес") OrganizerInput(organizerDetailsLabel(activeCategory), details) { details = it }
                        if (activeCategory in organizerNumberCategories) {
                            if (activeCategory != "Сон" && activeCategory != "Покупки на вахту") OrganizerInput(organizerNumberLabel(activeCategory), amount) { amount = it }
                            if (activeCategory == "Покупки на вахту" || activeCategory == "Чемодан" || activeCategory == "Подарки") {
                                val shopping = activeCategory == "Покупки на вахту"
                                val fractional = shopping && unit in listOf("кг", "л")
                                val step = if (shopping) {
                                    if (unit in listOf("г", "мл")) shoppingStep.toDouble()
                                    else if (fractional) shoppingStep / 1000.0
                                    else 1.0
                                } else 1.0
                                fun quantityText(value: Double): String = if (fractional) "%.3f".format(java.util.Locale("ru", "RU"), value) else value.toInt().toString()
                                Text(if (shopping) "Количество, ${unit.ifBlank { "шт" }}" else "Количество", color = orgMuted, fontSize = 13.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { amount = quantityText(((amount.replace(",", ".").toDoubleOrNull() ?: step) - step).coerceAtLeast(if (fractional) 0.001 else 1.0)) }) { Text("−", fontSize = 20.sp) }
                                    OutlinedTextField(
                                        value = amount,
                                        onValueChange = { value ->
                                            if (value.isBlank() || (if (fractional) value.matches(Regex("[0-9]*[.,]?[0-9]{0,3}")) else value.matches(Regex("[0-9]*")))) amount = value
                                        },
                                        label = { Text("Количество") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (fractional) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                    OutlinedButton(onClick = { amount = quantityText((amount.replace(",", ".").toDoubleOrNull() ?: 0.0) + step) }) { Text("+", fontSize = 20.sp) }
                                }
                                if (shopping && unit in listOf("г", "кг", "мл", "л")) {
                                    Text("Шаг кнопок", color = orgMuted, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(10, 50, 100).forEach { choice ->
                                            FilterChip(selected = shoppingStep == choice, onClick = { shoppingStep = choice }, label = { Text("$choice ${if (unit in listOf("г", "кг")) "г" else "мл"}") })
                                        }
                                    }
                                }
                            }
                        }
                        if (activeCategory == "Покупки на вахту") {
                            Text("Единица измерения", color = orgMuted, fontSize = 13.sp)
                            listOf(listOf("шт", "г", "кг"), listOf("мл", "л", "уп")).forEach { options ->
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    options.forEach { option ->
                                        FilterChip(selected = unit == option, onClick = {
                                            if (unit != option) {
                                                val old = amount.replace(",", ".").toDoubleOrNull()
                                                val converted = when {
                                                    unit == "г" && option == "кг" || unit == "мл" && option == "л" -> old?.div(1000.0)
                                                    unit == "кг" && option == "г" || unit == "л" && option == "мл" -> old?.times(1000.0)
                                                    else -> null
                                                }
                                                amount = when {
                                                    converted == null -> "1"
                                                    option in listOf("кг", "л") -> "%.3f".format(java.util.Locale("ru", "RU"), converted)
                                                    else -> converted.toInt().toString()
                                                }
                                                unit = option
                                            }
                                        }, label = { Text(option) })
                                    }
                                }
                            }
                            OrganizerInput(if (unit in listOf("г", "кг")) "Цена за 1 кг, ₽" else if (unit in listOf("мл", "л")) "Цена за 1 л, ₽" else "Цена за единицу, ₽", time) { value ->
                                if (value.isBlank() || value.matches(Regex("[0-9]*[.,]?[0-9]{0,2}"))) time = value
                            }
                            val qty = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val price = time.replace(",", ".").toDoubleOrNull() ?: 0.0
                            Text("Стоимость: %.2f ₽".format(java.util.Locale("ru", "RU"), (if (unit in listOf("г", "мл")) qty / 1000.0 else qty) * price), color = organizerTileAccent(activeCategory), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        if (activeCategory == "Подарки") {
                            OrganizerInput("Кому подарок (получатель)", unit) { unit = it }
                            OrganizerInput("Бюджет на один подарок, ₽", time) { value ->
                                if (value.isBlank() || value.matches(Regex("[0-9]*[.,]?[0-9]{0,2}"))) time = value
                            }
                            val quantity = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val budget = time.replace(",", ".").toDoubleOrNull() ?: 0.0
                            Text("Общий бюджет: %.2f ₽".format(java.util.Locale.forLanguageTag("ru-RU"), quantity * budget),
                                color = organizerTileAccent(activeCategory), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            
                        }
                        if (activeCategory == "Чемодан") {
                            Text("Категория вещей", color = orgMuted, fontSize = 13.sp)
                            listOf(listOf("Документы", "Одежда", "Обувь"), listOf("Гигиена", "Техника", "Прочее")).forEach { group ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    group.forEach { groupName ->
                                        FilterChip(
                                            selected = unit == groupName,
                                            onClick = { unit = groupName },
                                            label = { Text(groupName, fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                        }
                        if (activeCategory == "Планы") {
                            Text("Приоритет", color = orgMuted, fontSize = 13.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf("Низкий", "Средний", "Высокий").forEach { priority ->
                                    FilterChip(
                                        selected = unit == priority,
                                        onClick = { unit = priority },
                                        label = { Text(priority, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            val stages = details.lines().filter { it.isNotBlank() }
                            val finishedStages = stages.count { it.trimStart().startsWith("✓") }
                            Text("Этапы: $finishedStages / ${stages.size} выполнено", color = organizerTileAccent(activeCategory), fontSize = 13.sp)
                            LinearProgressIndicator(
                                progress = { if (stages.isEmpty()) 0f else finishedStages.toFloat() / stages.size },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = organizerTileAccent(activeCategory), trackColor = orgBorder
                            )
                            
                        }
                        if (activeCategory == "Расходы") {
                            listOf(listOf("Продукты", "Транспорт", "Связь"), listOf("Жильё", "Здоровье", "Развлечения"), listOf("Покупки", "Другое")).forEach { group ->
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                                    group.forEach { option ->
                                        FilterChip(selected = unit == option, onClick = { unit = option },
                                            label = { Text(option, fontSize = 10.sp) }, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = expenseRecurring, onCheckedChange = { expenseRecurring = it })
                                Text("Повторяется каждый месяц", color = orgMuted, fontSize = 13.sp)
                            }
                            
                        }
                        if (activeCategory == "Накопления") {
                            OrganizerInput("Целевая сумма, ₽", savingsTarget) { savingsTarget = it }
                            OrganizerInput("Уже накоплено, ₽", savingsBalance) { savingsBalance = it }
                            val target = savingsTarget.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val balance = savingsBalance.replace(",", ".").toDoubleOrNull() ?: 0.0
                            Text("Прогресс: ${if (target > 0) ((balance / target) * 100).toInt().coerceAtLeast(0) else 0}%", color = orgAccent)
                            if (target > 0) LinearProgressIndicator(
                                progress = { (balance / target).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(), color = orgAccent, trackColor = orgBorder
                            )
                            
                        }
                        if (activeCategory == "Отпуск") {
                            OrganizerInput("Место отдыха / направление", unit) { unit = it }
                            OrganizerInput("Дата окончания (ГГГГ-ММ-ДД)", vacationEnd) { vacationEnd = it }
                            OutlinedButton(onClick = {
                                val initial = runCatching { LocalDate.parse(vacationEnd) }.getOrDefault(LocalDate.now())
                                DatePickerDialog(context, { _, y, m, d -> vacationEnd = LocalDate.of(y, m + 1, d).toString() },
                                    initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                            }, modifier = Modifier.fillMaxWidth()) { Text("Выбрать окончание отпуска") }
                            val startDay = runCatching { LocalDate.parse(date) }.getOrNull()
                            val endDay = runCatching { LocalDate.parse(vacationEnd) }.getOrNull()
                            if (startDay != null && endDay != null && !endDay.isBefore(startDay)) {
                                Text("Продолжительность: ${java.time.temporal.ChronoUnit.DAYS.between(startDay, endDay) + 1} дней", color = organizerTileAccent("Отпуск"), fontWeight = FontWeight.Bold)
                            }
                            OrganizerInput("Бюджет поездки, ₽", vacationBudget) { vacationBudget = it }
                            OrganizerInput("Бронирование / номер / адрес", vacationBooking) { vacationBooking = it }
                            
                            OutlinedTextField(value = vacationChecklist, onValueChange = { vacationChecklist = it },
                                label = { Text("Билеты, документы, сборы…") }, modifier = Modifier.fillMaxWidth(),
                                minLines = 3, maxLines = 8)
                        }
                        if (activeCategory == "Питание") {
                            listOf(listOf("Завтрак", "Обед"), listOf("Ужин", "Перекус")).forEach { choices ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    choices.forEach { meal ->
                                        FilterChip(selected = unit == meal, onClick = { unit = meal }, label = { Text(meal) }, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            OrganizerInput("Стоимость, ₽ (необязательно)", time) { time = it }
                            
                        }
                        if (activeCategory == "Настроение") OrganizerInput("Оценка настроения от 1 до 5", unit) { unit = it }
                        if (activeCategory in listOf("Лекарства", "Планы", "Питание")) {
                            OrganizerInput("Запланированные затраты, ₽ (необязательно)", plannedCost) { value ->
                                if (value.isBlank() || value.matches(Regex("[0-9]*[.,]?[0-9]{0,2}"))) plannedCost = value
                            }
                        }
                        if (activeCategory == "Лекарства") {
                            OrganizerInput("Дозировка (например, 1 таблетка)", unit) { unit = it }
                            Text("Форма препарата", color = orgMuted, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Таблетки", "Капсулы", "Капли").forEach { form ->
                                    FilterChip(selected = medicineForm == form, onClick = { medicineForm = form },
                                        label = { Text(form, fontSize = 11.sp) })
                                }
                            }
                            OrganizerInput("Остаток в упаковке, шт.", medicineStock) { medicineStock = it.filter { ch -> ch.isDigit() } }
                            OutlinedButton(onClick = {
                                val initial = runCatching { LocalDate.parse(medicineExpiry) }.getOrDefault(LocalDate.now())
                                DatePickerDialog(context, { _, y, m, d -> medicineExpiry = LocalDate.of(y, m + 1, d).toString() },
                                    initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (medicineExpiry.isBlank()) "Выбрать срок годности" else "Срок годности: $medicineExpiry")
                            }
                            
                            OrganizerInput("Время приёма (например, 08:00)", time) { time = it }
                            
                        }
                        if (activeCategory == "Тренировки") OrganizerInput("Подходы / повторения / длительность", unit) { unit = it }
                        if (activeCategory == "Сон") {
                            OrganizerInput("Заснул (ЧЧ:ММ)", unit) { unit = it }
                            OrganizerInput("Проснулся (ЧЧ:ММ)", time) { time = it }
                            OrganizerInput("Дневной сон, минут (необязательно)", medicineForm) { medicineForm = it.filter { ch -> ch.isDigit() } }
                            val calculated = sleepDuration(unit, time, medicineForm)
                            Text(if (calculated == null) "Укажи время в формате 23:00 и 07:00" else "Общая продолжительность: ${calculated.formatOrg()} ч", color = organizerTileAccent("Сон"), fontWeight = FontWeight.Bold)
                            
                        }
                        if (activeCategory != "Вес") OrganizerInput(if (activeCategory == "Лекарства") "Дата начала (ГГГГ-ММ-ДД)" else "Дата (ГГГГ-ММ-ДД)", date) { date = it }
                        else Text("Дата: ${date.ifBlank { LocalDate.now().toString() }}", color = vahtaText)
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
                        enabled = (title.isNotBlank() || activeCategory == "Вес" || (activeCategory == "Заметки" && (details.isNotBlank() || noteChecklist.isNotBlank()))) && (activeCategory !in organizerNumberCategories || (amount.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it > 0 && (activeCategory != "Покупки на вахту" || unit in listOf("кг", "л") || it % 1.0 == 0.0) } == true)) && (activeCategory !in setOf("Покупки на вахту", "Подарки") || (time.isBlank() || (time.replace(",", ".").toDoubleOrNull()?.let { it >= 0 && it.isFinite() } == true))) && (activeCategory != "Сон" || sleepDuration(unit, time, medicineForm) != null) && (activeCategory != "Вес" || (amount.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it > 0.0 && it < 1000.0 } == true && (unit.isBlank() || unit.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it > 0.0 && it < 1000.0 } == true))) && (activeCategory != "Питание" || (time.isBlank() || time.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true)) && (activeCategory != "Лекарства" || ((medicineStock.isBlank() || medicineStock.toIntOrNull() != null) && (medicineExpiry.isBlank() || runCatching { LocalDate.parse(medicineExpiry) }.isSuccess) && (time.isBlank() || runCatching { java.time.LocalTime.parse(time) }.isSuccess))) && (activeCategory != "Накопления" || (savingsTarget.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && savingsBalance.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true)) && (activeCategory != "Отпуск" || (runCatching { LocalDate.parse(date) }.getOrNull()?.let { start -> runCatching { LocalDate.parse(vacationEnd) }.getOrNull()?.let { end -> !end.isBefore(start) } } == true && (vacationBudget.isBlank() || vacationBudget.replace(",", ".").toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true))) && (activeCategory != "Долги" || (debtBalance.replace(",", ".").toDoubleOrNull()?.let { it > 0 && it.isFinite() } == true && (debtMonthly.isBlank() || debtMonthly.replace(",", ".").toDoubleOrNull()?.let { it >= 0 && it.isFinite() } == true) && (debtDueDay.isBlank() || debtDueDay.toIntOrNull() in 1..31) && (debtPaid.isBlank() || debtPaid.replace(",", ".").toDoubleOrNull()?.let { it >= 0 && it.isFinite() } == true))),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val id = editingId
                            val saved = if (id == null) {
                                val nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
                                update(items + OrganizerItem(
                                    nextId, activeCategory, (if (activeCategory == "Вес") "Вес · ${date.ifBlank { LocalDate.now().toString() }}" else title.trim().ifBlank { "Без названия" }), if (activeCategory == "Заметки") (details.trim() + (if (noteChecklist.isBlank()) "" else "\n" + noteChecklist.lines().filter { it.isNotBlank() }.joinToString("\n") { line ->
                                        val cleaned = line.trim().removePrefix("☐ ").removePrefix("☑ ")
                                        val existing = items.firstOrNull { it.id == editingId }?.details?.lines().orEmpty()
                                        (if (existing.any { it.trim() == "☑ $cleaned" }) "☑ " else "☐ ") + cleaned
                                    })).trim() else details.trim(), if (activeCategory == "Заметки" || (activeCategory in organizerJournalCategories && date.isBlank())) LocalDate.now().toString() else date.trim(), false,
                                    if (activeCategory == "Долги") debtBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    if (activeCategory == "Долги") debtMonthly.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    if (activeCategory == "Долги") debtDueDay.toIntOrNull() ?: 0 else 0,
                                    if (activeCategory == "Долги") debtDirection else "Я должен",
                                    if (activeCategory == "Долги") debtPaid.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    (if (activeCategory == "Сон") sleepDuration(unit, time, medicineForm) else amount.replace(",", ".").toDoubleOrNull()) ?: 0.0,
                                    unit.trim(), time.trim(),
                                    if (activeCategory == "Лекарства" || activeCategory == "Сон") medicineForm.trim() else "",
                                    if (activeCategory == "Лекарства") medicineStock.toIntOrNull() ?: 0 else 0,
                                    if (activeCategory == "Лекарства") medicineExpiry.trim() else "",
                                    pinned = activeCategory == "Заметки" && notePinned,
                                    expenseRecurring = activeCategory == "Расходы" && expenseRecurring,
                                    savingsTarget = if (activeCategory == "Накопления") savingsTarget.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    savingsBalance = if (activeCategory == "Накопления") savingsBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    vacationEnd = if (activeCategory == "Отпуск") vacationEnd.trim() else "",
                                    vacationBudget = if (activeCategory == "Отпуск") vacationBudget.replace(",", ".").toDoubleOrNull() ?: 0.0 else 0.0,
                                    vacationBooking = if (activeCategory == "Отпуск") vacationBooking.trim() else "",
                                    vacationChecklist = if (activeCategory == "Отпуск") vacationChecklist.trim() else "",
                                    plannedCost = plannedCost.replace(",", ".").toDoubleOrNull() ?: 0.0
                                ))
                            } else {
                                update(items.map { item ->
                                    if (item.id == id) item.copy(
                                        title = (if (activeCategory == "Вес") "Вес · ${date.ifBlank { LocalDate.now().toString() }}" else title.trim().ifBlank { "Без названия" }),
                                        details = if (activeCategory == "Заметки") (details.trim() + (if (noteChecklist.isBlank()) "" else "\n" + noteChecklist.lines().filter { it.isNotBlank() }.joinToString("\n") { line ->
                                        val cleaned = line.trim().removePrefix("☐ ").removePrefix("☑ ")
                                        val existing = items.firstOrNull { it.id == editingId }?.details?.lines().orEmpty()
                                        (if (existing.any { it.trim() == "☑ $cleaned" }) "☑ " else "☐ ") + cleaned
                                    })).trim() else details.trim(),
                                        date = if (activeCategory == "Заметки" || (activeCategory in organizerJournalCategories && date.isBlank())) LocalDate.now().toString() else date.trim(),
                                        debtBalance = if (activeCategory == "Долги") debtBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.debtBalance,
                                        debtMonthly = if (activeCategory == "Долги") debtMonthly.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.debtMonthly,
                                        debtDueDay = if (activeCategory == "Долги") debtDueDay.toIntOrNull() ?: 0 else item.debtDueDay,
                                        debtDirection = if (activeCategory == "Долги") debtDirection else item.debtDirection,
                                        debtPaid = if (activeCategory == "Долги") debtPaid.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.debtPaid,
                                        amount = (if (activeCategory == "Сон") sleepDuration(unit, time, medicineForm) else amount.replace(",", ".").toDoubleOrNull()) ?: 0.0,
                                        unit = unit.trim(), time = time.trim(),
                                        medicineForm = if (activeCategory == "Лекарства" || activeCategory == "Сон") medicineForm.trim() else item.medicineForm,
                                        medicineStock = if (activeCategory == "Лекарства") medicineStock.toIntOrNull() ?: 0 else item.medicineStock,
                                        medicineExpiry = if (activeCategory == "Лекарства") medicineExpiry.trim() else item.medicineExpiry,
                                        pinned = if (activeCategory == "Заметки") notePinned else item.pinned,
                                        expenseRecurring = if (activeCategory == "Расходы") expenseRecurring else item.expenseRecurring,
                                        savingsTarget = if (activeCategory == "Накопления") savingsTarget.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.savingsTarget,
                                        savingsBalance = if (activeCategory == "Накопления") savingsBalance.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.savingsBalance,
                                        vacationEnd = if (activeCategory == "Отпуск") vacationEnd.trim() else item.vacationEnd,
                                        vacationBudget = if (activeCategory == "Отпуск") vacationBudget.replace(",", ".").toDoubleOrNull() ?: 0.0 else item.vacationBudget,
                                        vacationBooking = if (activeCategory == "Отпуск") vacationBooking.trim() else item.vacationBooking,
                                        vacationChecklist = if (activeCategory == "Отпуск") vacationChecklist.trim() else item.vacationChecklist,
                                        plannedCost = plannedCost.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    ) else item
                                })
                            }
                            // Сообщение об успехе показывается только после фактической записи в SharedPreferences.
                            // См. сохранение через update() выше.
                            saveNotice = if (!saved) "Ошибка сохранения. Повтори попытку." else if (id == null) "Запись добавлена в раздел «$activeCategory»" else "Изменения в разделе «$activeCategory» сохранены"
                            if (saved) clearForm()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = orgAccent,
                            contentColor = orgBackground
                        )
                    ) {
                        Text(if (editingId == null) "Добавить" else "Сохранить изменения")
                    }
                }
            }
        }

        if (activeCategory == "Долги" && debtSubsection == null) {
            listOf("Я должен", "Мне должны").forEach { direction ->
                val records = items.filter { it.category == "Долги" && it.debtDirection == direction }
                val total = records.filter { !it.done }.sumOf { it.debtBalance }
                val tint = if (direction == "Я должен") Color(0xFFFF9B9B) else Color(0xFF9DECC0)
                Card(modifier = Modifier.fillMaxWidth().clickable { debtSubsection = direction; recordFilter = "Все"; clearForm(); debtDirection = direction },
                    colors = CardDefaults.cardColors(containerColor = if (direction == "Я должен") Color(0xFF613B49) else Color(0xFF275C4C)),
                    shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (direction == "Я должен") "↗  Я должен" else "↙  Мне должны", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("${records.count { !it.done }} активных", color = Color.White.copy(alpha = .8f))
                        Text("${total.formatOrg()} ₽", color = tint, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (activeCategory == "Долги" && debtSubsection != null) {
            OutlinedButton(onClick = { debtSubsection = null; formOpen = false }, modifier = Modifier.fillMaxWidth()) {
                Text("‹  К двум подразделам долгов", color = orgAccent)
            }
        }
        // Компактная панель записей без поиска: оставлены полезные фильтры и счётчик.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = orgCard)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Записи · ${visible.size}", color = vahtaText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(organizerCategoryIcon(activeCategory), fontSize = 30.sp)
                }
                if (activeCategory !in organizerJournalCategories) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Все", "Активные", "Готово").forEach { filter ->
                            FilterChip(
                                selected = recordFilter == filter,
                                onClick = { recordFilter = filter },
                                label = { Text(filter, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = organizerTileColor(activeCategory),
                                    selectedLabelColor = organizerTileAccent(activeCategory),
                                    labelColor = orgMuted
                                )
                            )
                        }
                    }
                }
            }
        }
        val filteredRecords = visible.filter { item ->
            activeCategory in organizerJournalCategories || recordFilter == "Все" || (recordFilter == "Готово" && item.done) || (recordFilter == "Активные" && !item.done)
        }
        if (filteredRecords.isEmpty() && (activeCategory != "Долги" || debtSubsection != null)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = orgCard),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    if (visible.isEmpty()) "Пока пусто. Добавь первую запись кнопкой выше." else "По выбранному фильтру ничего не найдено",
                    color = orgMuted,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        filteredRecords.filter { activeCategory != "Долги" || debtSubsection != null }.sortedWith(if (activeCategory == "Заметки") compareByDescending<OrganizerItem> { it.pinned }.thenByDescending { it.id } else compareBy<OrganizerItem> { it.done }.thenBy { it.id })
            .forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = organizerTileColor(activeCategory)),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
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
                        if (activeCategory == "Заметки") {
                            OutlinedButton(onClick = {
                                val saved = update(items.map { if (it.id == item.id) it.copy(pinned = !item.pinned) else it })
                                saveNotice = if (saved) (if (item.pinned) "Заметка откреплена" else "Заметка закреплена") else "Не удалось сохранить закрепление"
                            }) { Text(if (item.pinned) "📌 Закреплено · Открепить" else "Закрепить заметку", color = organizerTileAccent(activeCategory)) }
                            item.details.lines().forEachIndexed { index, line ->
                                val trimmed = line.trimStart()
                                if (trimmed.startsWith("☐ ") || trimmed.startsWith("☑ ")) {
                                    val checked = trimmed.startsWith("☑ ")
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(checked = checked, onCheckedChange = { nowChecked ->
                                            val lines = item.details.lines().toMutableList()
                                            lines[index] = line.replaceFirst(if (checked) "☑ " else "☐ ", if (nowChecked) "☑ " else "☐ ")
                                            val saved = update(items.map { if (it.id == item.id) it.copy(details = lines.joinToString("\n"), date = LocalDate.now().toString()) else it })
                                            saveNotice = if (saved) "Пункт обновлён" else "Не удалось сохранить пункт"
                                        })
                                        Text(trimmed.drop(2), modifier = Modifier.weight(1f), color = if (checked) orgMuted else vahtaText,
                                            textDecoration = if (checked) TextDecoration.LineThrough else null)
                                    }
                                } else if (line.isNotBlank()) {
                                    Text(line, color = orgMuted, fontSize = 14.sp)
                                }
                            }
                        }
                        if (activeCategory == "Отпуск") {
                            val start = runCatching { LocalDate.parse(item.date) }.getOrNull()
                            val end = runCatching { LocalDate.parse(item.vacationEnd) }.getOrNull()
                            Text("${item.date} — ${item.vacationEnd}" + if (start != null && end != null && !end.isBefore(start))
                                " · ${java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1} дней" else "",
                                color = organizerTileAccent("Отпуск"), fontWeight = FontWeight.Bold)
                            if (item.vacationBudget > 0) Text("Бюджет: ${item.vacationBudget.formatOrg()} ₽", color = orgAccent)
                            if (item.vacationBooking.isNotBlank()) Text("Бронирование: ${item.vacationBooking}", color = orgMuted)
                            val tasks = item.vacationChecklist.lines().filter { it.isNotBlank() }
                            if (tasks.isNotEmpty()) {
                                val doneTasks = tasks.count { it.trimStart().startsWith("✓") }
                                Text("Подготовка: $doneTasks из ${tasks.size}", color = orgAccent)
                                LinearProgressIndicator(progress = { doneTasks.toFloat() / tasks.size },
                                    modifier = Modifier.fillMaxWidth(), color = organizerTileAccent("Отпуск"), trackColor = orgBorder)
                                tasks.forEachIndexed { index, task ->
                                    val checked = task.trimStart().startsWith("✓")
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = checked, onCheckedChange = { value ->
                                            val updated = tasks.toMutableList()
                                            updated[index] = if (value) "✓ " + task.removePrefix("✓").trim() else task.removePrefix("✓").trim()
                                            val saved = update(items.map { if (it.id == item.id) it.copy(vacationChecklist = updated.joinToString("\n")) else it })
                                            if (!saved) saveNotice = "Не удалось сохранить список подготовки"
                                        })
                                        Text(task.removePrefix("✓").trim(), color = orgMuted, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        if (activeCategory == "Накопления") {
                            val target = item.savingsTarget
                            val balance = item.savingsBalance
                            Text("Накоплено: ${balance.formatOrg()} ₽ из ${target.formatOrg()} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
                            if (target > 0) LinearProgressIndicator(
                                progress = { (balance / target).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(), color = orgAccent, trackColor = orgBorder
                            )
                            Text("Осталось: ${(target - balance).coerceAtLeast(0.0).formatOrg()} ₽", color = orgMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { savingsTransactionId = item.id; savingsWithdrawal = false; savingsTransactionAmount = "" }, modifier = Modifier.weight(1f)) { Text("Пополнить") }
                                OutlinedButton(onClick = { savingsTransactionId = item.id; savingsWithdrawal = true; savingsTransactionAmount = "" }, enabled = balance > 0, modifier = Modifier.weight(1f)) { Text("Снять") }
                            }
                        }
                        if (activeCategory == "Долги") {
                            Text("${item.debtDirection} · Погашено: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.0f", item.debtPaid)} ₽", color = orgMuted, fontSize = 13.sp)
                            Text("Остаток: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.0f", item.debtBalance)} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
                            Text("В месяц: ${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.0f", item.debtMonthly.coerceAtMost(item.debtBalance))} ₽" + if (item.debtDueDay in 1..31) " · ${item.debtDueDay}-го числа" else "", color = orgMuted, fontSize = 13.sp)
                            if (item.debtBalance > 0 && !item.done) {
                                OutlinedButton(onClick = { repaymentId = item.id; repaymentAmount = "" }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (item.debtDirection == "Мне должны") "Записать возврат" else "Внести погашение")
                                }
                            }
                        }
                        if (activeCategory in organizerNumberCategories) {
                            Text(organizerNumberDisplay(activeCategory, item.amount), color = orgAccent, fontWeight = FontWeight.Bold)
                        }
                        if (activeCategory == "Сон") {
                            Text("Ночь: ${item.unit} – ${item.time}" + if (item.medicineForm.toIntOrNull()?.let { it > 0 } == true) " · Дневной сон: ${item.medicineForm} мин" else "", color = orgMuted, fontSize = 13.sp)
                        } else if (item.unit.isNotBlank()) Text(item.unit, color = orgMuted, fontSize = 13.sp)
                        if (item.time.isNotBlank() && activeCategory != "Сон") Text(when (activeCategory) {
                            "Подарки" -> "Бюджет за штуку: ${item.time} ₽"
                            "Покупки на вахту" -> "Цена за ${if (item.unit in listOf("г", "кг")) "кг" else if (item.unit in listOf("мл", "л")) "л" else "единицу"}: ${item.time} ₽"
                            "Питание" -> "Стоимость: ${item.time} ₽"
                            else -> "Время: ${item.time}"
                        }, color = orgAccent, fontSize = 13.sp)
                        if (activeCategory == "Планы") {
                            val stages = item.details.lines().filter { it.isNotBlank() }
                            val finishedStages = stages.count { it.trimStart().startsWith("✓") }
                            Text("Приоритет: ${item.unit.ifBlank { "Средний" }}" + if (item.date.isNotBlank()) " · Срок: ${item.date}" else "",
                                color = organizerTileAccent(activeCategory), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (stages.isNotEmpty()) {
                                LinearProgressIndicator(
                                    progress = { finishedStages.toFloat() / stages.size },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = organizerTileAccent(activeCategory), trackColor = orgBorder
                                )
                                Text("Этапы: $finishedStages из ${stages.size}", color = orgMuted, fontSize = 12.sp)
                                stages.forEach { stage ->
                                    val doneStage = stage.trimStart().startsWith("✓")
                                    OutlinedButton(
                                        onClick = {
                                            val lines = item.details.lines().toMutableList()
                                            val idx = lines.indexOfFirst { it == stage }
                                            if (idx >= 0) {
                                                lines[idx] = if (doneStage) stage.trimStart().removePrefix("✓").trimStart() else "✓ " + stage
                                                val saved = update(items.map { if (it.id == item.id) it.copy(details = lines.joinToString("\n")) else it })
                                                saveNotice = if (saved) "Этап плана сохранён" else "Не удалось сохранить этап"
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text((if (doneStage) "☑ " else "☐ ") + stage.trimStart().removePrefix("✓").trimStart(), color = organizerTileAccent(activeCategory)) }
                                }
                            }
                        }
                        if (activeCategory == "Чемодан") {
                            Text("${item.amount.toInt().coerceAtLeast(1)} шт. · ${item.unit.ifBlank { "Без категории" }} · ${if (item.done) "Упаковано" else "Не упаковано"}",
                                color = organizerTileAccent(activeCategory), fontWeight = FontWeight.Bold)
                        }
                        if (activeCategory == "Подарки") {
                            val price = item.time.replace(",", ".").toDoubleOrNull() ?: 0.0
                            Text("Итого: %.2f ₽ · %s".format(java.util.Locale.forLanguageTag("ru-RU"), item.amount * price, if (item.done) "Куплено" else "Нужно купить"),
                                color = organizerTileAccent(activeCategory), fontWeight = FontWeight.Bold)
                        }
                        if (activeCategory == "Лекарства") {
                            Text("${item.medicineForm.ifBlank { "Форма не указана" }} · Остаток: ${item.medicineStock} шт.",
                                color = organizerTileAccent(activeCategory), fontWeight = FontWeight.Bold)
                            if (item.medicineExpiry.isNotBlank()) {
                                val expired = runCatching { LocalDate.parse(item.medicineExpiry).isBefore(LocalDate.now()) }.getOrDefault(false)
                                Text("Срок годности: ${item.medicineExpiry}" + if (expired) " · ИСТЁК" else "",
                                    color = if (expired) Color(0xFFFF9C9C) else orgMuted)
                            }
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
                        if (item.details.isNotBlank() && activeCategory != "Заметки" && activeCategory != "Планы") {
                            Text(item.details, color = orgMuted, fontSize = 13.sp)
                        }
                        if (item.date.isNotBlank()) {
                            Text(item.date, color = orgAccent, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { startEdit(item) }, shape = RoundedCornerShape(12.dp)) {
                                Text("✎  Изменить", color = organizerTileAccent(activeCategory))
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
                Text("Как выполнять: ${exercise.technique}",color=vahtaText)
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
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = vahtaText,
            unfocusedTextColor = vahtaText,
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
            debtDirection = obj.optString("debtDirection", "Я должен"),
            debtPaid = obj.optDouble("debtPaid", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            amount = obj.optDouble("amount", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            unit = obj.optString("unit"),
            time = obj.optString("time"),
            medicineForm = obj.optString("medicineForm"),
            medicineStock = obj.optInt("medicineStock", 0).coerceAtLeast(0),
            medicineExpiry = obj.optString("medicineExpiry"),
            pinned = obj.optBoolean("pinned", false),
            expenseRecurring = obj.optBoolean("expenseRecurring", false),
            savingsTarget = obj.optDouble("savingsTarget", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            savingsBalance = obj.optDouble("savingsBalance", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            vacationEnd = obj.optString("vacationEnd"),
            vacationBudget = obj.optDouble("vacationBudget", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
            vacationBooking = obj.optString("vacationBooking"),
            vacationChecklist = obj.optString("vacationChecklist"),
            plannedCost = obj.optDouble("plannedCost", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
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
                .put("debtDirection", item.debtDirection)
                .put("debtPaid", item.debtPaid)
                .put("amount", item.amount)
                .put("unit", item.unit)
                .put("time", item.time)
                .put("medicineForm", item.medicineForm)
                .put("medicineStock", item.medicineStock)
                .put("medicineExpiry", item.medicineExpiry)
                .put("pinned", item.pinned)
                .put("expenseRecurring", item.expenseRecurring)
                .put("savingsTarget", item.savingsTarget)
                .put("savingsBalance", item.savingsBalance)
                .put("vacationEnd", item.vacationEnd)
                .put("vacationBudget", item.vacationBudget)
                .put("vacationBooking", item.vacationBooking)
                .put("vacationChecklist", item.vacationChecklist)
                .put("plannedCost", item.plannedCost)
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
        .filter { it.category == "Долги" && it.debtDirection != "Мне должны" && !it.done && it.debtBalance > 0 }
        .map { VahtaDebt(it.title, it.debtBalance, it.debtMonthly.coerceAtMost(it.debtBalance), it.debtDueDay) }
}

data class VahtaSpending(val other: Double, val purchases: Double) {
    val total: Double get() = other + purchases
}
fun readVahtaSpending(context: Context, month: java.time.YearMonth): VahtaSpending {
    val prefs = context.getSharedPreferences("vahta_organizer", Context.MODE_PRIVATE)
    val entries = readOrganizer(prefs.getString("items", "[]") ?: "[]")
    val expenses = entries.filter { it.category == "Расходы" && it.date.startsWith(month.toString()) }.sumOf { it.amount }
    val purchases = entries.filter { it.category == "Покупки на вахту" && it.done && it.date.startsWith(month.toString()) }.sumOf { item ->
        item.amount * (item.time.replace(",", ".").toDoubleOrNull() ?: 0.0) * if (item.unit == "г" || item.unit == "мл") 0.001 else 1.0
    }
    return VahtaSpending(expenses, purchases)
}

data class VahtaBudgetForecast(val byCategory: Map<String, Double>, val actual: Double) {
    val planned: Double get() = byCategory.values.sum()
}
fun readVahtaBudgetForecast(context: Context, month: java.time.YearMonth): VahtaBudgetForecast {
    val prefs = context.getSharedPreferences("vahta_organizer", Context.MODE_PRIVATE)
    val entries = readOrganizer(prefs.getString("items", "[]") ?: "[]")
    val categories = linkedMapOf<String, Double>()
    val currentMonth = java.time.YearMonth.now()
    fun inMonth(date: String): Boolean = if (date.isBlank()) month == currentMonth
        else runCatching { java.time.YearMonth.from(java.time.LocalDate.parse(date)) == month }.getOrDefault(false)
    fun add(name: String, value: Double) {
        if (value.isFinite() && value > 0) categories[name] = (categories[name] ?: 0.0) + value
    }
    entries.forEach { item ->
        if (!inMonth(item.date)) return@forEach
        when (item.category) {
            "Покупки на вахту" -> if (!item.done) add("Покупки", item.amount * (item.time.replace(",", ".").toDoubleOrNull() ?: 0.0) * if (item.unit in listOf("г", "мл")) 0.001 else 1.0)
            "Подарки" -> if (!item.done) add("Подарки", item.amount * (item.time.replace(",", ".").toDoubleOrNull() ?: 0.0))
            "Отпуск" -> if (!item.done) add("Отпуск", item.vacationBudget)
            "Лекарства", "Планы", "Питание" -> if (!item.done) add(item.category, item.plannedCost)
            // "Расходы" already represent actual spending, not an additional plan.
        }
    }
    return VahtaBudgetForecast(categories, readVahtaSpending(context, month).total)
}

private val organizerJournalCategories = setOf("Заметки", "Питание", "Вес", "Сон", "Расходы", "Накопления")
private val organizerNumberCategories = setOf("Покупки на вахту", "Питание", "Вес", "Сон", "Расходы", "Подарки", "Чемодан")
private fun organizerTitleLabel(category: String): String = when (category) {
    "Долги" -> "Имя человека / название долга"
    "Заметки" -> "Заголовок заметки"
    "Питание" -> "Продукт или блюдо"
    "Вес" -> "Вес"
    "Сон" -> "Запись сна"
    "Расходы" -> "На что потрачено"
    "Накопления" -> "Цель или пополнение"
    "Лекарства" -> "Название лекарства"
    "Подарки" -> "Что подарить"
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
    "Покупки на вахту" -> "Примечание к покупке"
    "Чемодан" -> "Примечание: размер, цвет, где лежит"
    "Расходы" -> "Категория расхода / комментарий"
    "Подарки" -> "Повод / пожелания"
    "Накопления" -> "Комментарий к накоплению"
    else -> "Описание / примечание"
}
private fun organizerNumberLabel(category: String): String = when (category) {
    "Подарки" -> "Количество подарков, шт."
    "Чемодан" -> "Количество вещей, шт."
    "Питание" -> "Калорийность порции, ккал"
    "Вес" -> "Вес, кг"
    "Сон" -> "Продолжительность сна, часы (рассчитывается автоматически)"
    "Расходы" -> "Сумма расхода, ₽"
    "Накопления" -> "Сумма пополнения, ₽"
    else -> "Значение"
}
private fun organizerNumberDisplay(category: String, amount: Double): String = when (category) {
    "Питание" -> "${amount.formatOrg()} ккал"
    "Вес" -> "${amount.formatOrg()} кг"
    "Сон" -> "${amount.formatOrg()} ч"
    "Подарки", "Чемодан" -> "${amount.formatOrg()} шт."
    else -> "${amount.formatOrg()} ₽"
}
private fun Double.formatOrg(): String = String.format(java.util.Locale.forLanguageTag("ru-RU"), "%,.1f", this).removeSuffix(",0")
@Composable
private fun OrganizerSummary(category: String, visible: List<OrganizerItem>) {
    val today = LocalDate.now().toString()
    when (category) {
        "Питание" -> Text("Сегодня: ${visible.filter { it.date == today }.sumOf { it.amount }.formatOrg()} ккал", color = orgAccent, fontWeight = FontWeight.Bold)
        "Расходы" -> Text("Отдельные расходы: ${visible.sumOf { it.amount }.formatOrg()} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
        "Накопления" -> Text("Всего накоплено: ${visible.sumOf { it.savingsBalance }.formatOrg()} ₽", color = orgAccent, fontWeight = FontWeight.Bold)
        "Вес" -> visible.filter { it.amount > 0 }.maxWithOrNull(compareBy<OrganizerItem> { it.date }.thenBy { it.id })?.let { Text("Последнее измерение: ${it.amount.formatOrg()} кг", color = orgAccent) }
        "Сон" -> if (visible.isNotEmpty()) Text("Средняя продолжительность: ${(visible.sumOf { it.amount } / visible.size).formatOrg()} ч", color = orgAccent)
        "Чемодан" -> Text("Осталось собрать: ${visible.count { !it.done }}", color = orgAccent)
        "Долги" -> Text("Остаток в выбранном подразделе: ${visible.filter { !it.done }.sumOf { it.debtBalance }.formatOrg()} ₽", color = orgAccent)
        "Покупки на вахту", "Подарки" -> Text("Осталось купить: ${visible.count { !it.done }}", color = orgAccent)
        "Планы", "Отпуск" -> Text("В планах: ${visible.count { !it.done }}", color = orgAccent)
        "Тренировки" -> Text("Выполнено тренировок: ${visible.count { it.done }}", color = orgAccent)
    }
}
