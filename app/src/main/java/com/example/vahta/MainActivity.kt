package com.vahta.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

private object LocalAppearanceHolder { var prefs: SharedPreferences? = null }
// 0 — тёмная бирюзовая, 1 — тёмная лаванда, 2 — светлая пастельная.
internal var appearanceMode by mutableIntStateOf(0)
internal val lavenderMode get() = appearanceMode == 1
internal val lightMode get() = appearanceMode == 2
internal val vahtaText get() = if (lightMode) Color(0xFF243249) else Color.White
private val bg get() = when (appearanceMode) { 1 -> Color(0xFF19172C); 2 -> Color(0xFFF7F8FC); else -> Color(0xFF0D192B) }
private val panel get() = when (appearanceMode) { 1 -> Color(0xFF342B51); 2 -> Color(0xFFFFFFFF); else -> Color(0xFF223650) }
private val mint get() = when (appearanceMode) { 1 -> Color(0xFFE9B8FF); 2 -> Color(0xFF376C65); else -> Color(0xFF85E7CD) }
private val muted get() = when (appearanceMode) { 1 -> Color(0xFFEDE2F5); 2 -> Color(0xFF526176); else -> Color(0xFFD9EAF5) }
private val blue get() = when (appearanceMode) { 1 -> Color(0xFFC6B7FF); 2 -> Color(0xFF466AA1); else -> Color(0xFF9DCBFF) }
private val gold get() = if (lightMode) Color(0xFF805018) else Color(0xFFFFD18F)

// Coordinated semantic surfaces shared by all screens. Existing tile layout stays intact.
private fun themedPanelColor(title: String): Color {
    val key = title.lowercase(java.util.Locale.ROOT)
    val family = when {
        listOf("вахт", "график", "смен", "календар").any { key.contains(it) } -> 0
        listOf("финанс", "зарплат", "выплат", "доход", "аванс", "расход").any { key.contains(it) } -> 1
        listOf("дел", "задач", "план", "органайзер").any { key.contains(it) } -> 2
        listOf("оформ", "тема", "личн", "профил").any { key.contains(it) } -> 3
        listOf("напомин", "уведом", "безопас", "настрой").any { key.contains(it) } -> 4
        else -> 5
    }
    val palette = when (appearanceMode) {
        1 -> listOf(0xFF35325D,0xFF60405D,0xFF345967,0xFF584475,0xFF594B62,0xFF36344F)
        2 -> listOf(0xFFE9EDFF,0xFFFFEDF0,0xFFE4F4EF,0xFFF0E8FF,0xFFFFF2E2,0xFFFFFFFF)
        else -> listOf(0xFF233F68,0xFF533B58,0xFF244F51,0xFF493D6C,0xFF5A453D,0xFF24394F)
    }
    return Color(palette[family])
}
private fun themedPanelAccent(title: String): Color {
    val key = title.lowercase(java.util.Locale.ROOT)
    return when {
        listOf("финанс", "зарплат", "выплат", "аванс", "доход").any { key.contains(it) } -> if(lightMode) Color(0xFF9B4B66) else Color(0xFFFFB4C6)
        listOf("график", "вахт", "смен", "календар").any { key.contains(it) } -> if(lightMode) Color(0xFF365BBA) else Color(0xFF9EC7FF)
        listOf("оформ", "личн", "профил").any { key.contains(it) } -> if(lightMode) Color(0xFF7650A8) else Color(0xFFD9B4FF)
        listOf("настрой", "безопас", "напомин").any { key.contains(it) } -> if(lightMode) Color(0xFF95611C) else Color(0xFFFFD18F)
        else -> if(lightMode) Color(0xFF287B6A) else Color(0xFF9BE7C7)
    }
}

private val ru = Locale.forLanguageTag("ru-RU")
private val rotations = listOf("15×15", "30×30", "45×45", "60×60", "60×30")
private fun parseDate(v: String): LocalDate? = runCatching { LocalDate.parse(v) }.getOrNull()
private fun LocalDate.pretty() = format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
private fun num(v: String): Double = v.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
private fun regionalFactor(s: Config): Double {
    val rk = num(s.coefficient)
    return 1.0 + (if (rk in 1.0..1.99) (rk - 1.0) else rk / 100.0) + num(s.north) / 100.0
}
private fun rub(v: Double) = NumberFormat.getCurrencyInstance(ru).format(v)
private fun rotationWork(v: String) = v.substringBefore('×').toIntOrNull() ?: 45
private fun rotationRest(v: String) = v.substringAfter('×').toIntOrNull() ?: 45
private fun rotationMonths(v: String) = when (v) { "15×15" -> 1; "30×30" -> 2; "60×60" -> 4; else -> 3 }
private fun anchoredDate(anchor: LocalDate, months: Long): LocalDate {
    val month = YearMonth.from(anchor).plusMonths(months)
    return month.atDay(anchor.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
}

private data class Config(
    val name: String = "", val rotation: String = "", val arrival: String = "", val departure: String = "",
    val inboundRoad: String = "", val outboundRoad: String = "",
    val hourly: String = "", val hours: String = "", val workDays: String = "", val restDays: String = "",
    val coefficient: String = "", val north: String = "", val premium: String = "", val allowance: String = "",
    val travelHoursByDate: String = "", val travelHours: String = "", val travelMode: String = "hour", val travelDayRate: String = "", val travelDates: String = "", val intershiftHours: String = "", val intershiftRate: String = "",
    val tax: String = "", val meals: String = "", val enforcement: String = "", val otherDeductions: String = "",
    val advanceDay: String = "", val salaryDay: String = "", val advanceMethod: String = "По отработанным часам",
    val fixedAdvance: String = "", val nightHours: String = "", val nightPercent: String = "", val harmfulPercent: String = "",
    val safetyBonus: String = "", val oneTimeBonus: String = "", val holidayHours: String = "",
    val mealDays: String = "", val mealMode: String = "", val travelRate: String = "", val shiftRestDays: String = "",
    val mvoActualHours: String = "", val mvoNormHours: String = "", val mvoExcludedHours: String = "",
    val shiftMonthSupplement: String = "", val foodCompensation: String = "", val annualBonusPercent: String = "", val annualVacationDays: String = "",
    val personalName: String = "", val personalValue: String = "", val personalMode: String = "Процент",
    val personalBase: String = "Тарифный заработок", val personalProrate: Boolean = true,
    val personalCoefficient: Boolean = false, val personalMonthly: Boolean = true,
    val deductTax: Boolean = true, val allowanceTaxFree: Boolean = true, val notes: String = "", val reminders: String = "",
    val remindArrival: Boolean = false, val remindDeparture: Boolean = false,
    val remindSalary: Boolean = false, val remindAdvance: Boolean = false, val remindEvents: Boolean = false,
    val remindDays: String = "1", val remindTime: String = "09:00"
)
private fun readConfig(p: SharedPreferences, prefix: String = ""): Config {
    fun g(k: String) = p.getString(prefix + k, "") ?: ""
    fun b(k: String) = p.getBoolean(prefix + k, false)
    return Config(
        name=g("name"), rotation=g("rotation"), arrival=g("arrival"), departure=g("departure"),
        inboundRoad=g("inboundRoad"), outboundRoad=g("outboundRoad"),
        hourly=g("hourly"), hours=g("hours"), workDays=g("workDays"), restDays=g("restDays"),
        coefficient=g("coefficient"), north=g("north"), premium=g("premium"), allowance=g("allowance"),
        travelHoursByDate=g("travelHoursByDate"), travelHours=g("travelHours"), travelMode=g("travelMode").ifBlank { "hour" }, travelDayRate=g("travelDayRate"), travelDates=g("travelDates"), intershiftHours=g("intershiftHours"), intershiftRate=g("intershiftRate"),
        tax=g("tax"), meals=g("meals"), enforcement=g("enforcement"), otherDeductions=g("otherDeductions"),
        advanceDay=g("advanceDay"), salaryDay=g("salaryDay"), advanceMethod=g("advanceMethod").ifBlank { "По отработанным часам" },
        fixedAdvance=g("fixedAdvance"), nightHours=g("nightHours"), nightPercent=g("nightPercent"),
        harmfulPercent=g("harmfulPercent"), safetyBonus=g("safetyBonus"), oneTimeBonus=g("oneTimeBonus"),
        holidayHours=g("holidayHours"), mealDays=g("mealDays"), mealMode=g("mealMode"), travelRate=g("travelRate"),
        shiftRestDays=g("shiftRestDays"), mvoActualHours=g("mvoActualHours"),
        mvoNormHours=g("mvoNormHours"), mvoExcludedHours=g("mvoExcludedHours"),
        shiftMonthSupplement=g("shiftMonthSupplement"), foodCompensation=g("foodCompensation"), annualBonusPercent=g("annualBonusPercent"), annualVacationDays=g("annualVacationDays"), personalName=g("personalName"), personalValue=g("personalValue"), personalMode=g("personalMode").ifBlank { "Процент" },
        personalBase=g("personalBase").ifBlank { "Тарифный заработок" }, personalProrate=p.getBoolean(prefix+"personalProrate",true),
        personalCoefficient=b("personalCoefficient"), personalMonthly=p.getBoolean(prefix+"personalMonthly",true),
        deductTax=p.getBoolean(prefix+"deductTax",true), allowanceTaxFree=p.getBoolean(prefix+"allowanceTaxFree",true), notes=g("notes"), reminders=g("reminders"),
        remindArrival=b("remindArrival"), remindDeparture=b("remindDeparture"),
        remindSalary=b("remindSalary"), remindAdvance=b("remindAdvance"), remindEvents=b("remindEvents"),
        remindDays=g("remindDays").ifBlank { "1" }, remindTime=g("remindTime").ifBlank { "09:00" }
    )
}
private fun saveConfig(p: SharedPreferences, s: Config, prefix: String = "") {
    p.edit().apply {
        fun put(k: String, v: String) { putString(prefix+k, v) }
        fun put(k: String, v: Boolean) { putBoolean(prefix+k, v) }
        put("name",s.name); put("rotation",s.rotation); put("arrival",s.arrival); put("departure",s.departure)
        put("inboundRoad",s.inboundRoad); put("outboundRoad",s.outboundRoad)
        put("hourly",s.hourly); put("hours",s.hours); put("workDays",s.workDays); put("restDays",s.restDays)
        put("coefficient",s.coefficient); put("north",s.north); put("premium",s.premium); put("allowance",s.allowance)
        put("travelHoursByDate",s.travelHoursByDate); put("travelHours",s.travelHours); put("travelMode",s.travelMode); put("travelDayRate",s.travelDayRate); put("travelDates",s.travelDates); put("intershiftHours",s.intershiftHours); put("intershiftRate",s.intershiftRate)
        put("tax",s.tax); put("meals",s.meals); put("enforcement",s.enforcement); put("otherDeductions",s.otherDeductions)
        put("advanceDay",s.advanceDay); put("salaryDay",s.salaryDay); put("advanceMethod",s.advanceMethod)
        put("fixedAdvance",s.fixedAdvance); put("nightHours",s.nightHours); put("nightPercent",s.nightPercent)
        put("harmfulPercent",s.harmfulPercent); put("safetyBonus",s.safetyBonus); put("oneTimeBonus",s.oneTimeBonus)
        put("holidayHours",s.holidayHours); put("mealDays",s.mealDays); put("mealMode",s.mealMode); put("travelRate",s.travelRate)
        put("shiftRestDays",s.shiftRestDays); put("mvoActualHours",s.mvoActualHours);
        put("mvoNormHours",s.mvoNormHours); put("mvoExcludedHours",s.mvoExcludedHours);
        put("shiftMonthSupplement",s.shiftMonthSupplement); put("foodCompensation",s.foodCompensation); put("annualBonusPercent",s.annualBonusPercent); put("annualVacationDays",s.annualVacationDays); put("personalName",s.personalName); put("personalValue",s.personalValue); put("personalMode",s.personalMode)
        put("personalBase",s.personalBase); put("personalProrate",s.personalProrate); put("personalCoefficient",s.personalCoefficient)
        put("personalMonthly",s.personalMonthly); put("deductTax",s.deductTax); put("allowanceTaxFree",s.allowanceTaxFree); put("notes",s.notes); put("reminders",s.reminders)
        put("remindArrival",s.remindArrival); put("remindDeparture",s.remindDeparture)
        put("remindSalary",s.remindSalary); put("remindAdvance",s.remindAdvance); put("remindEvents",s.remindEvents)
        put("remindDays",s.remindDays); put("remindTime",s.remindTime)
        commit()
    }
}
private fun clearDraft(p: SharedPreferences) {
    p.edit().apply { p.all.keys.filter { it.startsWith("draft_") }.forEach { remove(it) }; remove("setup_step"); apply() }
}
private data class Period(val start: LocalDate, val end: LocalDate, val next: LocalDate,
    val working: Boolean, val elapsed: Long, val total: Long, val remaining: Long, val progress: Float)
private fun periodFor(day: LocalDate, first: LocalDate, last: LocalDate, rotation: String): Period {
    val step = rotationMonths(rotation).toLong()
    val monthDelta = ChronoUnit.MONTHS.between(YearMonth.from(first), YearMonth.from(day))
    var i = Math.floorDiv(monthDelta, step)
    fun start(n: Long) = anchoredDate(first, n*step)
    while (start(i).isAfter(day)) i--
    while (!start(i+1).isAfter(day)) i++
    val a = start(i)
    val next = start(i+1)
    val rawEnd = anchoredDate(last, i*step)
    val end = if (rawEnd.isBefore(a)) a else if (!rawEnd.isBefore(next)) next.minusDays(1) else rawEnd
    val working = !day.isAfter(end)
    val begin = if (working) a else end.plusDays(1)
    val finish = if (working) end else next.minusDays(1)
    val total = (ChronoUnit.DAYS.between(begin,finish)+1).coerceAtLeast(1)
    val elapsed = (ChronoUnit.DAYS.between(begin,day)+1).coerceIn(0,total)
    return Period(a,end,next,working,elapsed,total,total-elapsed,elapsed.toFloat()/total)
}
// The road dates belong to the configured first rotation and repeat with its anchor.
private enum class RotationStage { BEFORE, INBOUND, WORK, OUTBOUND, REST }
private fun rotationRoadDate(base: String, s: Config, period: Period): LocalDate? {
    val original=parseDate(base) ?: return null
    val first=parseDate(s.arrival) ?: return null
    return period.start.plusDays(ChronoUnit.DAYS.between(first,original))
}
private fun rotationStage(today: LocalDate, s: Config, period: Period?): RotationStage {
    if(period==null) return RotationStage.BEFORE
    val inbound=rotationRoadDate(s.inboundRoad,s,period)
    val nextInbound=parseDate(s.inboundRoad)?.let { base ->
        val anchor=parseDate(s.arrival)!!
        period.next.plusDays(ChronoUnit.DAYS.between(anchor,base))
    }
    val outbound=rotationRoadDate(s.outboundRoad,s,period)
    return when {
        inbound==today || nextInbound==today -> RotationStage.INBOUND
        outbound==today -> RotationStage.OUTBOUND
        period.working -> RotationStage.WORK
        else -> RotationStage.REST
    }
}
private fun stageTitle(stage: RotationStage): String = when(stage) {
    RotationStage.BEFORE -> "График не настроен"
    RotationStage.INBOUND -> "Дорога на вахту"
    RotationStage.WORK -> "На вахте"
    RotationStage.OUTBOUND -> "Дорога домой"
    RotationStage.REST -> "Межвахтовый отдых"
}
private data class Payment(val date: String, val amount: Double, val type: String, val forecast: Double = -1.0)
private fun loadPayments(raw: String): List<Payment> = runCatching {
    val a=JSONArray(raw); (0 until a.length()).map { val o=a.getJSONObject(it)
        Payment(o.optString("date"),o.optDouble("amount"),o.optString("type"),o.optDouble("forecast",-1.0)) }
}.getOrDefault(emptyList())
private fun encodePayments(items: List<Payment>) = JSONArray().apply {
    items.forEach { put(JSONObject().put("date",it.date).put("amount",it.amount).put("type",it.type).put("forecast",it.forecast)) }
}.toString()
private data class CalendarEvent(val date: String, val title: String)
private fun loadEvents(raw: String): List<CalendarEvent> = runCatching {
    val a=JSONArray(raw); (0 until a.length()).map { val o=a.getJSONObject(it); CalendarEvent(o.optString("date"),o.optString("title")) }
}.getOrDefault(emptyList())
private fun encodeEvents(items: List<CalendarEvent>) = JSONArray().apply {
    items.forEach { put(JSONObject().put("date",it.date).put("title",it.title)) }
}.toString()
private val holidays2026 = setOf(
    "2026-01-01", "2026-01-02", "2026-01-05", "2026-01-06", "2026-01-07",
    "2026-01-08", "2026-01-09", "2026-02-23", "2026-03-09", "2026-05-01",
    "2026-05-11", "2026-06-12", "2026-11-04", "2026-12-31"
)
private fun officialHolidayName(date: LocalDate): String? = when (date.monthValue to date.dayOfMonth) {
    1 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 5, 1 to 6, 1 to 8 -> "Новогодние каникулы"
    1 to 7 -> "Рождество Христово"
    2 to 23 -> "День защитника Отечества"
    3 to 8 -> "Международный женский день"
    5 to 1 -> "Праздник Весны и Труда"
    5 to 9 -> "День Победы"
    6 to 12 -> "День России"
    11 to 4 -> "День народного единства"
    else -> null
}
private fun isNonWorking(date: LocalDate): Boolean {
    if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return true
    if (date.year == 2026) return date.toString() in holidays2026
    return when (date.monthValue) {
        1 -> date.dayOfMonth in 1..8
        2 -> date.dayOfMonth == 23
        3 -> date.dayOfMonth == 8
        5 -> date.dayOfMonth == 1 || date.dayOfMonth == 9
        6 -> date.dayOfMonth == 12
        11 -> date.dayOfMonth == 4
        else -> false
    }
}
private fun payDate(month: YearMonth, dayText: String): LocalDate? {
    val d=dayText.toIntOrNull()?.takeIf { it in 1..31 } ?: return null
    var date=month.atDay(d.coerceAtMost(month.lengthOfMonth()))
    while (isNonWorking(date)) date=date.minusDays(1)
    return date
}
private fun isActualPayDate(date: LocalDate, text: String): Boolean {
    val month=YearMonth.from(date)
    return payDate(month,text)==date || payDate(month.plusMonths(1),text)==date
}
private fun nextPayDate(today: LocalDate, text: String): LocalDate? {
    val month=YearMonth.from(today)
    for (i in 0L..3L) {
        val date=payDate(month.plusMonths(i),text) ?: return null
        if (!date.isBefore(today)) return date
    }
    return null
}
// Employer-specific model. Unknown inputs remain editable; estimates are not payslips.
private data class WageBreakdown(val taxable: Double, val taxFree: Double, val gross: Double, val tax: Double, val deductions: Double, val net: Double)
private data class MvoEntry(
    val month: String, val actualHours: Double, val normHours: Double,
    val excludedHours: Double, val usedDays: Int, val vacationDays: Int,
    val firstRestCarryDays: Int
)
private fun loadMvoEntries(context: Context): List<MvoEntry> = runCatching {
    val raw=context.getSharedPreferences("vahta_mvo_ledger",Context.MODE_PRIVATE).getString("entries","[]") ?: "[]"
    val array=JSONArray(raw)
    (0 until array.length()).map { i ->
        val o=array.getJSONObject(i)
        MvoEntry(o.getString("month"),o.optDouble("actualHours"),o.optDouble("normHours"),
            o.optDouble("excludedHours"),o.optInt("usedDays"),o.optInt("vacationDays"),o.optInt("firstRestCarryDays"))
    }
}.getOrDefault(emptyList())
private fun saveMvoEntries(context: Context, entries: List<MvoEntry>) {
    val a=JSONArray()
    entries.sortedBy { it.month }.forEach { e ->
        a.put(JSONObject().put("month",e.month).put("actualHours",e.actualHours)
            .put("normHours",e.normHours).put("excludedHours",e.excludedHours)
            .put("usedDays",e.usedDays).put("vacationDays",e.vacationDays)
            .put("firstRestCarryDays",e.firstRestCarryDays))
    }
    context.getSharedPreferences("vahta_mvo_ledger",Context.MODE_PRIVATE)
        .edit().putString("entries",a.toString()).apply()
}
private data class MvoBalance(val accrued: Int, val used: Int, val available: Int, val decemberPayout: Int, val nextYearCarry: Int)
private fun mvoTariff(s: Config): Double = num(s.intershiftRate).takeIf { it > 0 } ?: num(s.hourly)
private fun mvoPayout(days: Int, s: Config): Double = days.coerceAtLeast(0) * 8.0 * mvoTariff(s)
private fun mvoBalance(entries: List<MvoEntry>, year: Int): MvoBalance {
    val inYear=entries.filter { YearMonth.parse(it.month).year==year }.sortedBy { it.month }
    val earned=inYear.sumOf { kotlin.math.floor((it.actualHours-it.normHours-it.excludedHours).coerceAtLeast(0.0)/8.0).toInt() }
    val used=inYear.sumOf { it.usedDays }
    // At year end only days reserved for the first rest of next year are carried over.
    val previousDecember=entries.firstOrNull { it.month=="${year-1}-12" }
    val carried=previousDecember?.firstRestCarryDays?.coerceAtLeast(0) ?: 0
    val balance=(earned+carried-used).coerceAtLeast(0)
    val reserved=inYear.lastOrNull { it.month.endsWith("-12") }?.firstRestCarryDays?.coerceIn(0,balance) ?: 0
    return MvoBalance(earned+carried,used,balance,balance-reserved,reserved)
}

private fun mvoDays(s: Config): Int? {
    if (s.mvoActualHours.isBlank() || s.mvoNormHours.isBlank()) return null
    val eligibleHours = (num(s.mvoActualHours) - num(s.mvoNormHours) - num(s.mvoExcludedHours)).coerceAtLeast(0.0)
    return kotlin.math.floor(eligibleHours / 8.0).toInt()
}
private fun confirmedTravelDates(s: Config): List<LocalDate> = s.travelDates.split(",")
    .mapNotNull { parseDate(it.trim()) }.distinct().sorted()
private fun travelHoursForDate(s: Config, date: LocalDate): String =
    s.travelHoursByDate.split(";").firstOrNull { it.substringBefore("=") == date.toString() }
        ?.substringAfter("=", "") ?: ""
private fun withTravelHoursForDate(s: Config, date: LocalDate, hours: String): Config {
    val entries = s.travelHoursByDate.split(";").filter { it.contains("=") &&
        it.substringBefore("=") != date.toString() }.toMutableList()
    if (hours.isNotBlank()) entries.add("${date}=${hours}")
    return s.copy(travelHoursByDate=entries.joinToString(";"))
}
private fun travelPay(s: Config): Double {
    val dates=confirmedTravelDates(s)
    if (dates.isEmpty()) return 0.0
    if (s.travelMode == "day") return dates.size * num(s.travelDayRate)
    val perDateHours=dates.sumOf { num(travelHoursForDate(s,it)) }
    // Older configurations used one aggregate hours field; retain it as fallback.
    val hours=if (dates.any { travelHoursForDate(s,it).isNotBlank() }) perDateHours else num(s.travelHours)
    return hours * (num(s.travelRate).takeIf { it > 0 } ?: num(s.hourly))
}
private fun holidayHoursForPeriod(s: Config, start: LocalDate, end: LocalDate): Double {
    if (end.isBefore(start)) return 0.0
    var d = start
    var hours = 0.0
    while (!d.isAfter(end)) {
        val anchor = parseDate(s.arrival)
        val departure = parseDate(s.departure)
        if (anchor != null && departure != null && s.rotation in rotations && officialHolidayName(d) != null) {
            val period = periodFor(d, anchor, departure, s.rotation)
            if (period.working && d.isAfter(period.start) && !d.isAfter(period.end)) hours += num(s.hours)
        }
        d = d.plusDays(1)
    }
    return hours
}
private fun wageForDays(s: Config, workDays: Int, shiftDays: Int, fraction: Double = 1.0): WageBreakdown {
    val base = workDays * num(s.hours) * num(s.hourly)
    val night = num(s.nightHours) * num(s.hourly) * num(s.nightPercent)/100 * fraction
    val harmful = base * num(s.harmfulPercent)/100
    val premium = base * num(s.premium)/100
    val personalBaseAmount = if(s.personalBase=="Тариф + доплаты") base+night+harmful else base
    val personalRaw = if(s.personalMode=="Сумма, ₽") num(s.personalValue) * (if(s.personalProrate) fraction else 1.0)
        else personalBaseAmount * num(s.personalValue).coerceAtMost(1000.0)/100.0
    val coefficientFactor = regionalFactor(s)
    val adjusted = (base + night + harmful + premium) * coefficientFactor +
        personalRaw * (if(s.personalCoefficient) coefficientFactor else 1.0)
    val safety = num(s.safetyBonus)*fraction
    val oneTime = num(s.oneTimeBonus)*fraction
    val holiday = num(s.holidayHours)*num(s.hourly)*2*fraction // Legacy manual setting preserved for existing data
    val travel = travelPay(s)*fraction
    val mvo = (mvoDays(s)?.toDouble()?.times(8.0) ?: if(num(s.shiftRestDays)>0) num(s.shiftRestDays)*8 else num(s.intershiftHours))*
        num(s.intershiftRate).takeIf { it>0 }.let { it ?: num(s.hourly) }*fraction
    val allowance = shiftDays*num(s.allowance)
    val foodCompensation = workDays*num(s.foodCompensation)
    val shiftSupplement = if(workDays > 0) num(s.shiftMonthSupplement)*fraction else 0.0
    val taxable = adjusted + safety + oneTime + shiftSupplement + holiday + travel + mvo + if(s.allowanceTaxFree) 0.0 else allowance
    val free = (if(s.allowanceTaxFree) allowance else 0.0) + foodCompensation
    val tax = if(s.deductTax) taxable*num(s.tax).coerceIn(0.0,100.0)/100 else 0.0
    val meals = (if(s.mealMode=="monthly" || (s.mealMode.isBlank() && num(s.mealDays)==0.0)) num(s.meals)
        else num(s.meals)*num(s.mealDays))*fraction
    val deductions = meals + (num(s.enforcement)+num(s.otherDeductions))*fraction
    return WageBreakdown(taxable,free,taxable+free,tax,deductions,(taxable+free-tax-deductions).coerceAtLeast(0.0))
}
private var shiftDayPreferences: SharedPreferences? = null

private fun countShiftDays(s: Config, start: LocalDate, end: LocalDate): Pair<Int,Int> {
    val anchor = parseDate(s.arrival) ?: return 0 to 0
    val departure = parseDate(s.departure) ?: return 0 to 0
    if(departure.isBefore(anchor) || s.rotation !in rotations || end.isBefore(start)) return 0 to 0
    // Every shift day is a working day unless the user marked it as a day off.
    val excluded = shiftDayPreferences?.let { loadWorkDays(it) }
        ?.filter { it.kind in setOf("МВО", "Отпуск", "Больничный", "Дорога") }
        ?.map { it.date }?.toSet().orEmpty()
    var worked=0; var shifts=0; var day=start
    while(!day.isAfter(end)) {
        val period=periodFor(day,anchor,departure,s.rotation)
        if(period.working) {
            shifts++
            val shiftLength=(ChronoUnit.DAYS.between(period.start,period.end)+1).toInt().coerceAtLeast(1)
            val offCount=num(s.restDays).toInt().coerceIn(0,shiftLength)
            val index=(ChronoUnit.DAYS.between(period.start,day)+1).toInt()
            val autoOff=(index*offCount/shiftLength) > ((index-1)*offCount/shiftLength)
            if(day.toString() !in excluded && !autoOff) worked++
        }
        day=day.plusDays(1)
    }
    return worked to shifts
}
private fun grossForRange(s: Config, first: LocalDate, last: LocalDate, until: LocalDate): Double {
    if(until.isBefore(first)) return 0.0
    val finish=if(until.isAfter(last)) last else until
    val counts=countShiftDays(s,first,finish)
    return wageForDays(s,counts.first,counts.second).gross
}
private fun netEstimate(gross: Double, s: Config): Double = (gross*(1-(if(s.deductTax) num(s.tax).coerceIn(0.0,100.0)/100 else 0.0))-num(s.enforcement)-num(s.otherDeductions)).coerceAtLeast(0.0)
private fun estimateForPayday(s: Config, date: LocalDate, type: String): Double? {
    if(num(s.hourly)<=0 || num(s.hours)<=0) return null
    val month = if(type=="Аванс") YearMonth.from(date) else YearMonth.from(date).minusMonths(1)
    val first=month.atDay(1)
    val end=if(type=="Аванс") month.atDay(15) else month.atEndOfMonth()
    val (worked, shifts)=countShiftDays(s,first,end)
    if(type=="Аванс") {
        if(s.advanceMethod=="Фиксированная сумма" && num(s.fixedAdvance)>0) return num(s.fixedAdvance)
        // The presentation says hours from 1–15, without premium. This is an estimate before reconciliation.
        val basic = worked*num(s.hours)*num(s.hourly)
        val factor=regionalFactor(s)
        return (basic*factor*(1-(if(s.deductTax) num(s.tax).coerceIn(0.0,100.0)/100 else 0.0))).coerceAtLeast(0.0)
    }
    val breakdown=wageForDays(s,worked,shifts)
    val advance=estimateForPayday(s,month.atDay(30.coerceAtMost(month.lengthOfMonth())),"Аванс") ?: 0.0
    return (breakdown.net-advance).coerceAtLeast(0.0)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs=getSharedPreferences("vahta_settings",Context.MODE_PRIVATE)
        shiftDayPreferences=prefs
        val appearancePrefs=getSharedPreferences("vahta_appearance",Context.MODE_PRIVATE)
        LocalAppearanceHolder.prefs=appearancePrefs
        appearanceMode=appearancePrefs.getInt("mode",if(appearancePrefs.getBoolean("lavender",false)) 1 else 0).coerceIn(0,2)
        setContent {
            var unlocked by rememberSaveable { mutableStateOf(false) }
            var setupDone by rememberSaveable { mutableStateOf(prefs.getBoolean("setup_complete_v2",false)) }
            var config by remember { mutableStateOf(readConfig(prefs)) }
            val owner=LocalLifecycleOwner.current
            var backgrounded by remember { mutableStateOf(false) }
            DisposableEffect(owner) {
                val observer=LifecycleEventObserver { _,event -> when(event) {
                    Lifecycle.Event.ON_STOP -> if(unlocked) backgrounded=true
                    Lifecycle.Event.ON_START -> if(backgrounded) { unlocked=false; backgrounded=false }
                    else -> Unit
                } }
                owner.lifecycle.addObserver(observer)
                onDispose { owner.lifecycle.removeObserver(observer) }
            }
            VahtaTheme {
                val appFocus = LocalFocusManager.current
                val appKeyboard = LocalSoftwareKeyboardController.current
                Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures { appFocus.clearFocus(force = true); appKeyboard?.hide() }
                }) {
                    if(!setupDone) Wizard(prefs) { result ->
                        config=result; saveConfig(prefs,result)
                        prefs.edit().putString("gender",prefs.getString("draft_gender", "")).apply()
                        prefs.edit().putBoolean("setup_complete_v2",true).commit()
                        clearDraft(prefs); setupDone=true
                    }
                    else Dashboard(config,{ config=it; saveConfig(prefs,it) },prefs,{ unlocked=false },this@MainActivity)
                    // Keep the existing screen composed beneath the system unlock overlay.
                    if(!unlocked) Box(Modifier.fillMaxSize().background(bg)) {
                        SystemUnlockScreen(onUnlocked={ unlocked=true })
                    }
                }
            }
        }
    }
}
@Composable private fun VahtaTheme(content: @Composable () -> Unit) {
    val scheme=if(lightMode) lightColorScheme(
        primary=Color(0xFF247D77), onPrimary=Color.White,
        secondary=Color(0xFF8065BA), background=bg, onBackground=vahtaText,
        surface=panel, onSurface=vahtaText, surfaceVariant=Color(0xFFECEAF7),
        onSurfaceVariant=Color(0xFF47536A), outline=Color(0xFF9BA6B8)
    ) else darkColorScheme(
        primary=mint, onPrimary=bg,
        secondary=blue, onSecondary=bg,
        tertiary=gold, onTertiary=bg,
        background=bg, onBackground=vahtaText,
        surface=panel, onSurface=vahtaText,
        surfaceVariant=if (appearanceMode == 1) Color(0xFF4B3C69) else Color(0xFF294A66),
        onSurfaceVariant=muted,
        primaryContainer=if (appearanceMode == 1) Color(0xFF554174) else Color(0xFF24665D),
        onPrimaryContainer=Color.White,
        secondaryContainer=if (appearanceMode == 1) Color(0xFF444F86) else Color(0xFF345C82),
        onSecondaryContainer=Color.White,
        tertiaryContainer=if (appearanceMode == 1) Color(0xFF79516A) else Color(0xFF78576B),
        onTertiaryContainer=Color.White,
        outline=if (appearanceMode == 1) Color(0xFF9E8AB2) else Color(0xFF7499A4)
    )
    MaterialTheme(colorScheme=scheme,content=content)
}
@Composable private fun Wizard(prefs: SharedPreferences,onFinish: (Config)->Unit) {
    var s by remember { mutableStateOf(readConfig(prefs,"draft_").copy(restDays="0").let { draft ->
        // Исправляем только явно невозможную дату дороги домой из старого черновика.
        val lastDay = parseDate(draft.departure)
        val roadHome = parseDate(draft.outboundRoad)
        if (lastDay != null && roadHome != null && !roadHome.isAfter(lastDay)) {
            draft.copy(outboundRoad = lastDay.plusDays(1).toString())
        } else draft
    }) }
    // Новый короткий опрос. Старый черновик сохраняется, если настройка уже была начата.
    var step by remember { mutableIntStateOf(0) }
    var gender by remember { mutableStateOf(prefs.getString("draft_gender", "") ?: "") }
    var datesConfirmed by remember { mutableStateOf(false) }
    fun update(v: Config) {
        // При смене последней рабочей даты исправляем только устаревший день выезда,
        // если он оказался раньше или совпал с последней сменой.
        val revised = if (v.departure != s.departure) {
            val lastDay = parseDate(v.departure)
            val roadHome = parseDate(v.outboundRoad)
            if (lastDay != null && roadHome != null && !roadHome.isAfter(lastDay))
                v.copy(outboundRoad = lastDay.plusDays(1).toString()) else v
        } else v
        if (revised.arrival != s.arrival || revised.departure != s.departure ||
            revised.inboundRoad != s.inboundRoad || revised.outboundRoad != s.outboundRoad ||
            revised.rotation != s.rotation) datesConfirmed = false
        s=revised; saveConfig(prefs,revised,"draft_")
    }
    val first=parseDate(s.arrival)
    val last=parseDate(s.departure)
    val roadIn=parseDate(s.inboundRoad)
    val roadOut=parseDate(s.outboundRoad)
    val datesOk=first!=null && last!=null && roadIn!=null && roadOut!=null &&
        roadIn.isBefore(first) && !last.isBefore(first) && roadOut.isAfter(last) && s.rotation in rotations
    val canNext=when(step) {
        0 -> s.name.isNotBlank() && gender.isNotBlank()
        1 -> datesOk && datesConfirmed
        3 -> datesOk && datesConfirmed
        else -> true
    }
    val wizardFocus=LocalFocusManager.current
    val wizardKeyboard=LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize().background(bg).pointerInput(Unit) { detectTapGestures { wizardFocus.clearFocus(force = true); wizardKeyboard?.hide() } }.padding(18.dp)) {
        Text("Вахтовичок",fontSize=30.sp,color=vahtaText,fontWeight=FontWeight.Black)
        Text("ПЕРВОНАЧАЛЬНАЯ НАСТРОЙКА",color=mint,fontSize=11.sp,letterSpacing=2.sp)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress={ (step+1)/4f },modifier=Modifier.fillMaxWidth(),color=mint)
        Text("Шаг ${step+1} из 4 · Обычно 1–2 минуты",color=muted,fontSize=12.sp)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(step) {
                0 -> {
                    Heading("Давай знакомиться", "Укажи имя и выбери оформление приложения.")
                    Field("Имя",s.name) { update(s.copy(name=it)) }
                    Text("Пол",color=vahtaText,fontWeight=FontWeight.SemiBold)
                    listOf("Мужской", "Женский").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable {
                            gender=option
                            prefs.edit().putString("draft_gender", option).apply()
                            val mode=if(option=="Женский") 1 else 0
                            appearanceMode=mode
                            prefs.edit().putString("gender",option).apply()
                            val appearancePrefs=LocalAppearanceHolder.prefs
                            appearancePrefs?.edit()?.putInt("mode",mode)?.apply()
                        },verticalAlignment=Alignment.CenterVertically) {
                            RadioButton(selected=gender==option,onClick={
                                gender=option
                                prefs.edit().putString("draft_gender",option).putString("gender",option).apply()
                                val mode=if(option=="Женский") 1 else 0
                                appearanceMode=mode
                                LocalAppearanceHolder.prefs?.edit()?.putInt("mode",mode)?.apply()
                            })
                            Text(option,color=vahtaText)
                        }
                    }
                    Text("Тему можно изменить позже в настройках.",color=muted,fontSize=12.sp)
                }
                1 -> {
                    Heading("Твой график", "Выбери график и укажи даты ближайшей вахты.")
                    RotationSelect(s.rotation) { update(s.copy(rotation=it)) }
                    Text("Выходных за вахту: 0. Изменить можно в разделе Финансы → Настройки.",color=muted,fontSize=12.sp)
                    DateField("1. День в пути на вахту",s.inboundRoad) { update(s.copy(inboundRoad=it)) }
                    DateField("2. Начало вахты (первая смена)",s.arrival) { update(s.copy(arrival=it)) }
                    DateField("3. Окончание вахты (последняя смена)",s.departure) { update(s.copy(departure=it)) }
                    DateField("4. День выезда домой",s.outboundRoad) { update(s.copy(outboundRoad=it)) }
                    if (last != null && roadOut != null && !roadOut.isAfter(last)) {
                        Text("Дата выезда должна быть после последней смены. Выбери дату выезда домой.",color=gold,fontSize=12.sp)
                    }
                    Text("Даты дороги и рабочих смен учитываются отдельно. Проверь последовательность четырёх дат.",color=muted,fontSize=12.sp)
                    if(datesOk) Text("Длительность вахты: ${ChronoUnit.DAYS.between(first,last)+1} дн.",color=mint)
                    if (!datesOk) {
                        Text("Укажите четыре даты по порядку: дорога → первая смена → последняя смена → выезд домой.",color=gold,fontSize=12.sp)
                    }
                    Button(
                        onClick = { datesConfirmed = true },
                        enabled = datesOk,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (datesConfirmed) "✓ Четыре даты подтверждены" else "Подтвердить четыре даты") }
                    Text("После изменения любой даты подтверждение потребуется повторить. Даты дороги не считаются рабочими сменами.",color=muted,fontSize=12.sp)
                    Text("Если график нестандартный, его можно уточнить в настройках после запуска.",color=muted,fontSize=12.sp)
                }
                2 -> {
                    Heading("Расчёт зарплаты", "Необязательно. Если не знаешь цифры — пропусти и заполни позже.")
                    Field("Ставка, ₽ за час",s.hourly,true) { update(s.copy(hourly=it)) }
                    Field("Часов в смене",s.hours,true) { update(s.copy(hours=it)) }
                    Text("Рабочие дни определяются автоматически по датам вахты. Выходные отмечай в календаре.",color=muted,fontSize=12.sp)
                    Text("Премии, районные и северные надбавки, питание, МВО и удержания настраиваются отдельно. У разных работодателей правила отличаются.",color=muted,fontSize=12.sp)
                }
                3 -> {
                    Heading("Всё готово!", "Проверь основное. Остальные параметры можно добавить в настройках.")
                    Panel("ТВОИ ДАННЫЕ") {
                        Info("Имя",s.name)
                        Info("График",s.rotation)
                        Info("Дорога на вахту",roadIn?.pretty() ?: "—")
                        Info("Первая смена",first?.pretty() ?: "—")
                        Info("Последняя смена",last?.pretty() ?: "—")
                        Info("Дорога домой",roadOut?.pretty() ?: "—")
                        Info("Ставка",if(s.hourly.isBlank()) "Пока не указана" else "${s.hourly} ₽/час")
                    }
                    Text("Рабочие смены: ${if(first!=null && last!=null) (ChronoUnit.DAYS.between(first,last)+1-num(s.restDays).toLong()).coerceAtLeast(0) else 0}. Выходных: ${s.restDays.ifBlank { "0" }}.",color=mint,fontSize=13.sp)
                    Text("Без данных о зарплате приложение покажет календарь, а финансовый прогноз можно будет настроить позднее.",color=muted,fontSize=12.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            if(step>0) OutlinedButton(onClick={step--},modifier=Modifier.weight(1f)) { Text("Назад") }
            if(step==2) OutlinedButton(onClick={step=3},modifier=Modifier.weight(1f)) { Text("Пропустить") }
            Button(onClick={if(step<3) { step++ } else onFinish(s.copy(travelDates=listOf(s.inboundRoad,s.outboundRoad).joinToString(",")))},enabled=canNext,modifier=Modifier.weight(1f)) {
                Text(if(step==3) "Подтвердить и открыть" else "Далее")
            }
        }
    }
}
@Composable private fun ReminderOptions(s: Config,update: (Config)->Unit) {
    ReminderToggle("Заезд",s.remindArrival) { update(s.copy(remindArrival=it)) }
    ReminderToggle("Выезд",s.remindDeparture) { update(s.copy(remindDeparture=it)) }
    ReminderToggle("Зарплата",s.remindSalary) { update(s.copy(remindSalary=it)) }
    ReminderToggle("Аванс",s.remindAdvance) { update(s.copy(remindAdvance=it)) }
    ReminderToggle("Личные события",s.remindEvents) { update(s.copy(remindEvents=it)) }
    Text("Предупреждать за:",color=muted)
    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        listOf("0","1","3","7").forEach { v ->
            FilterChip(selected=s.remindDays==v,onClick={update(s.copy(remindDays=v))},label={Text("$v дн.")})
        }
    }
    Field("Время (ЧЧ:ММ)",s.remindTime) { update(s.copy(remindTime=it)) }
}
@Composable private fun ReminderToggle(label: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Checkbox(checked=checked,onCheckedChange=onChange)
        Text(label,color=vahtaText)
    }
}
@Composable private fun RotationSelect(value: String,onChange: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text("Тип вахты: ${value.ifBlank { "Выбрать" }}") }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            rotations.forEach { item -> DropdownMenuItem(text={Text(item)},onClick={onChange(item);expanded=false}) }
        }
    }
}
private data class WorkDay(val date: String,val kind: String,val hours: Double,val night: Double,val holiday: Double)
private fun loadWorkDays(p: SharedPreferences): List<WorkDay> = runCatching {
    val a=JSONArray(p.getString("workday_entries_v1","[]") ?: "[]")
    (0 until a.length()).map { i ->
        val j=a.getJSONObject(i)
        WorkDay(j.optString("date"),j.optString("kind","Рабочий"),
            j.optDouble("hours",0.0),j.optDouble("night",0.0),j.optDouble("holiday",0.0))
    }.filter { parseDate(it.date)!=null }
}.getOrDefault(emptyList())
private fun saveWorkDays(p: SharedPreferences, items: List<WorkDay>) {
    val a=JSONArray()
    items.forEach { a.put(JSONObject().put("date",it.date).put("kind",it.kind)
        .put("hours",it.hours).put("night",it.night).put("holiday",it.holiday)) }
    p.edit().putString("workday_entries_v1",a.toString()).apply()
}
private fun backupJson(context: Context): String {
    val root=JSONObject().put("format","vahta-backup-v1")
    listOf("vahta_settings","vahta_appearance","vahta_mvo_ledger","vahta_organizer").forEach { name ->
        val prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE)
        val values=JSONObject()
        prefs.all.forEach { (key,value) ->
            when(value) {
                is String -> values.put(key,JSONObject().put("type","string").put("value",value))
                is Boolean -> values.put(key,JSONObject().put("type","boolean").put("value",value))
                is Int -> values.put(key,JSONObject().put("type","int").put("value",value))
                is Long -> values.put(key,JSONObject().put("type","long").put("value",value))
                is Float -> values.put(key,JSONObject().put("type","float").put("value",value.toDouble()))
            }
        }
        root.put(name,values)
    }
    return root.toString()
}
private fun restoreBackup(context: Context, raw: String): Boolean = runCatching {
    val root=JSONObject(raw)
    require(root.optString("format")=="vahta-backup-v1")
    val names=listOf("vahta_settings","vahta_appearance","vahta_mvo_ledger","vahta_organizer")
    // Validate all data before modifying any preferences.
    names.forEach { name ->
        val values=root.getJSONObject(name)
        val keys=values.keys()
        while(keys.hasNext()) {
            val obj=values.getJSONObject(keys.next())
            require(obj.getString("type") in listOf("string","boolean","int","long","float"))
        }
    }
    names.forEach { name ->
        val values=root.getJSONObject(name)
        val editor=context.getSharedPreferences(name,Context.MODE_PRIVATE).edit()
        val keys=values.keys()
        while(keys.hasNext()) {
            val key=keys.next(); val obj=values.getJSONObject(key)
            when(obj.getString("type")) {
                "string" -> editor.putString(key,obj.getString("value"))
                "boolean" -> editor.putBoolean(key,obj.getBoolean("value"))
                "int" -> editor.putInt(key,obj.getInt("value"))
                "long" -> editor.putLong(key,obj.getLong("value"))
                "float" -> editor.putFloat(key,obj.getDouble("value").toFloat())
            }
        }
        editor.commit()
    }
    true
}.getOrDefault(false)
private fun timesheetMode(p: SharedPreferences): String =
    p.getString("timesheet_mode_v2","off")?.takeIf { it in listOf("off","simple","detailed") } ?: "off"

private fun monthTimesheet(p: SharedPreferences, month: YearMonth): List<WorkDay> =
    loadWorkDays(p).filter { parseDate(it.date)?.let { d -> YearMonth.from(d)==month }==true }

private fun monthTimesheetForecast(s: Config, p: SharedPreferences, month: YearMonth): WageBreakdown? {
    val entries=monthTimesheet(p,month)
    // An empty or partial month must never masquerade as a complete payroll calculation.
    if(entries.isEmpty() || num(s.hourly)<=0) return null
    val worked=entries.filter { it.kind=="Рабочий" }
    val totalHours=worked.sumOf { it.hours }
    if(totalHours<=0) return null
    val shiftDays=worked.size
    val nightHours=worked.sumOf { it.night }
    val holidayHours=worked.sumOf { it.holiday }
    val base=totalHours*num(s.hourly)
    val night=nightHours*num(s.hourly)*num(s.nightPercent)/100
    val harmful=base*num(s.harmfulPercent)/100
    val premium=base*num(s.premium)/100
    val personalBase=if(s.personalBase=="Тариф + доплаты") base+night+harmful else base
    val personal=if(s.personalMode=="Сумма, ₽") num(s.personalValue) else
        personalBase*num(s.personalValue).coerceAtMost(1000.0)/100
    val factor=regionalFactor(s)
    val taxable=(base+night+harmful+premium)*factor+
        personal*(if(s.personalCoefficient) factor else 1.0)+
        num(s.safetyBonus)+num(s.oneTimeBonus)+num(s.shiftMonthSupplement)+holidayHours*num(s.hourly)*2+
        travelPay(s)
    val allowance=shiftDays*num(s.allowance)
    val foodCompensation=shiftDays*num(s.foodCompensation)
    val taxableTotal=taxable+if(s.allowanceTaxFree) 0.0 else allowance
    val tax=if(s.deductTax) taxableTotal*num(s.tax).coerceIn(0.0,100.0)/100 else 0.0
    val meals=if(s.mealMode=="monthly" || (s.mealMode.isBlank() && num(s.mealDays)==0.0)) num(s.meals)
        else num(s.meals)*num(s.mealDays)
    val deductions=meals+num(s.enforcement)+num(s.otherDeductions)
    return WageBreakdown(taxableTotal,(if(s.allowanceTaxFree) allowance else 0.0)+foodCompensation,
        taxableTotal+allowance+foodCompensation,tax,deductions,(taxableTotal+allowance+foodCompensation-tax-deductions).coerceAtLeast(0.0))
}
private fun readHomeOrder(prefs: SharedPreferences): List<String> {
    val defaults=listOf("schedule","counter","payments")
    val saved=prefs.getString("home_order","")?.split(",").orEmpty().filter { it in defaults }.distinct()
    return saved+defaults.filterNot { it in saved }
}
@Composable private fun Dashboard(s: Config,onChange: (Config)->Unit,prefs: SharedPreferences,onLock: ()->Unit,organizerContext: Context) {
    // Restore the last selected section after biometric/system unlock.
    var tab by rememberSaveable { mutableIntStateOf(prefs.getInt("last_main_tab", 0).coerceIn(0, 4)) }
    LaunchedEffect(tab) { prefs.edit().putInt("last_main_tab", tab).apply() }
    var organizerRootRequest by rememberSaveable { mutableIntStateOf(0) }
    var settingsRootRequest by rememberSaveable { mutableIntStateOf(0) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { clock=System.currentTimeMillis(); delay(1000) } }
    val now=remember(clock/1000) { LocalDateTime.ofInstant(Instant.ofEpochMilli(clock),ZoneId.systemDefault()) }
    val today=now.toLocalDate()
    val first=parseDate(s.arrival); val last=parseDate(s.departure)
    val valid=first!=null && last!=null && last.isAfter(first) && s.rotation in rotations
    val period=if(valid) periodFor(today,first!!,last!!,s.rotation) else null
    val stage=rotationStage(today,s,period)
    val ledgerEntries=remember(tab,clock/86400000L) { loadMvoEntries(organizerContext) }
    val ledgerBalance=mvoBalance(ledgerEntries,today.year)
    // Live work earnings must never include MVO: it is a separate entitlement/payment.
    // Exclude road and MVO from the running shift estimate; those have their own rules.
    val workOnlyConfig=s.copy(intershiftHours="",shiftRestDays="",mvoActualHours="",mvoNormHours="",
        mvoExcludedHours="",travelHours="",travelHoursByDate="",travelDates="",travelDayRate="")
    val completedThrough=if(period?.working==true) today.minusDays(1) else period?.end
    val completedWorkDays=if(period!=null && completedThrough!=null)
        countShiftDays(s,period.start,completedThrough).first else 0
    val wholeGross=if(period!=null) wageForDays(workOnlyConfig,completedWorkDays,completedWorkDays).gross else 0.0
    val mvoWorkDays=if(period!=null) countShiftDays(s,period.start,
        if(period.working) today.minusDays(1) else period.end).first else 0
    val mvoPreliminaryHours=mvoWorkDays*(num(s.hours)-8.0).coerceAtLeast(0.0)
    val mvoVerifiedHours=if(s.mvoActualHours.isNotBlank() && s.mvoNormHours.isNotBlank())
        (num(s.mvoActualHours)-num(s.mvoNormHours)-num(s.mvoExcludedHours)).coerceAtLeast(0.0) else null
    val mvoHours=mvoVerifiedHours ?: mvoPreliminaryHours
    val mvoWholeDays=kotlin.math.floor(mvoHours/8.0).toInt()
    val mvoRemainder=mvoHours-mvoWholeDays*8.0
    val mvoDailyRate=(num(s.intershiftRate).takeIf { it>0 } ?: num(s.hourly))*8.0
    val mvoEstimatedPay=mvoWholeDays*mvoDailyRate
    val fractional=if(stage==RotationStage.WORK) {
        val isDayOff=countShiftDays(s,today,today).first==0
        val factor=regionalFactor(s)+num(s.premium)/100
        val dayGross=if(!isDayOff) num(s.hours)*num(s.hourly)*factor else 0.0
        (dayGross+num(s.allowance))*now.toLocalTime().toSecondOfDay()/86400.0
    } else 0.0
    val earnings=wholeGross+fractional
    val showSchedule = prefs.getBoolean("home_schedule",true)
    val showCounter = prefs.getBoolean("home_counter",true)
    val showPayments = prefs.getBoolean("home_payments",true)
    var payments by remember { mutableStateOf(loadPayments(prefs.getString("payments","[]") ?: "[]")) }
    var events by remember { mutableStateOf(loadEvents(prefs.getString("calendar_events","[]") ?: "[]")) }
    fun savePayments(v: List<Payment>) { payments=v; prefs.edit().putString("payments",encodePayments(v)).commit() }
    fun saveEvents(v: List<CalendarEvent>) { events=v; prefs.edit().putString("calendar_events",encodeEvents(v)).commit() }
    val dashboardFocus=LocalFocusManager.current
    val dashboardKeyboard=LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize().background(bg).pointerInput(Unit) { detectTapGestures { dashboardFocus.clearFocus(force = true); dashboardKeyboard?.hide() } }) {
        // Горизонтальный свайп переключает пять основных разделов.
        // Срабатывает только после заметного движения, чтобы не мешать прокрутке вниз.
        var swipeDistance by remember { mutableFloatStateOf(0f) }
        val tabFocus = LocalFocusManager.current
        val tabKeyboard = LocalSoftwareKeyboardController.current
        Column(Modifier.weight(1f).fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures { tabFocus.clearFocus(force = true); tabKeyboard?.hide() } }
            .pointerInput(tab) {
                detectHorizontalDragGestures(
                    onDragStart={ swipeDistance=0f },
                    onHorizontalDrag={ change, distance ->
                        swipeDistance += distance
                        change.consume()
                    },
                    onDragEnd={
                        if(swipeDistance < -90f) tab=(tab+1).coerceAtMost(4)
                        else if(swipeDistance > 90f) tab=(tab-1).coerceAtLeast(0)
                        swipeDistance=0f
                    },
                    onDragCancel={ swipeDistance=0f }
                )
            }
            .then(if(tab==3) Modifier else Modifier.verticalScroll(rememberScrollState()))
            .padding(top=WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(if (tab == 0) "Вахтовичок" else listOf("Главная", "Календарь", "Финансы", "Мои дела", "Настройки")[tab],color=vahtaText,fontSize=if (tab == 0) 30.sp else 25.sp,fontWeight=FontWeight.Black)
            if (tab == 0 && s.name.isNotBlank()) Text("Привет, ${s.name}!",color=muted,fontSize=17.sp)
            when(tab) {
                0 -> {
                    val order = readHomeOrder(prefs)
                    order.forEach { card ->
                        when(card) {
                            "schedule" -> {                    if(showSchedule) Panel("МОЙ ГРАФИК") {
                        if(period==null) Text("Укажи даты заезда и выезда",color=gold)
                        else {
                            Text(stageTitle(stage).uppercase(ru),color=if(stage==RotationStage.WORK) mint else blue,fontWeight=FontWeight.Bold)
                            Text(if(period.working) "До окончания смен: ${ChronoUnit.DAYS.between(today,period.end).coerceAtLeast(0)} дн." else "До следующей вахты: ${ChronoUnit.DAYS.between(today,period.next).coerceAtLeast(0)} дн.",color=vahtaText,fontSize=23.sp)
                            RunningShiftProgress(period.progress)
                            Info("Прогресс","${(period.progress*100).toInt()}% · ${period.total} дней")
                            Info("Осталось", "${period.remaining} из ${period.total} дней")
                            Info("График",s.rotation); Info("Заезд",(rotationRoadDate(s.inboundRoad,s,period) ?: period.start.minusDays(1)).pretty())
                            Info("Выезд",period.end.pretty()); Info("Следующий заезд",(parseDate(s.inboundRoad)?.let { period.next.plusDays(ChronoUnit.DAYS.between(parseDate(s.arrival)!!,it)) } ?: period.next.minusDays(1)).pretty())
                        }
                    }
                            }
                            "counter" -> {                    if(showCounter) Panel("ЖИВОЙ СЧЁТЧИК") {
                        Text(rub(earnings),color=mint,fontSize=28.sp,fontWeight=FontWeight.Bold)
                        Text(if(stage==RotationStage.WORK) "Заработок за смены · предварительно"
                            else "Заработок за завершённые смены · новые начисления остановлены",color=muted)
                        Text("Режим: ${stageTitle(stage)}",color=blue,fontWeight=FontWeight.SemiBold)
                        if(period!=null && num(s.hours)>0) {
                            HorizontalDivider()
                            Text("МЕЖВАХТОВЫЙ ОТДЫХ · МВО",color=blue,fontWeight=FontWeight.Bold)
                            Info("Расчётные часы МВО", "${String.format(ru,"%.1f",mvoHours)} ч")
                            Info("Полных дней по 8 ч", "$mvoWholeDays дн.")
                            Info("Остаток часов", "${String.format(ru,"%.1f",mvoRemainder)} ч")
                            Info("Ориентир оплаты МВО",rub(mvoEstimatedPay))
                            if(ledgerEntries.isNotEmpty()) {
                                HorizontalDivider()
                                Text("МВО ПО СОХРАНЁННОМУ ТАБЕЛЮ",color=gold,fontWeight=FontWeight.SemiBold)
                                Info("Доступно дней МВО", "${ledgerBalance.available} дн.")
                                Info("Стоимость остатка",rub(mvoPayout(ledgerBalance.available,s)))
                                if(today.monthValue==12) {
                                    Info("К выплате за декабрь (прогноз)",rub(mvoPayout(ledgerBalance.decemberPayout,s)))
                                    Info("Перенос в следующий год", "${ledgerBalance.nextYearCarry} дн.")
                                }
                            }
                            Text(if(mvoVerifiedHours!=null)
                                "По введённым фактическим часам, норме и исключениям. Сверь с табелем."
                                else "Предварительно: $mvoWorkDays смен × (${num(s.hours)} − 8) ч. " +
                                    "Окончательные дни зависят от нормы за учётный период и табеля.",
                                color=muted,fontSize=12.sp)
                            if(stage!=RotationStage.WORK) Text("Новые смены не начисляются. Оплата МВО — отдельно от заработка за вахту.",color=gold,fontSize=12.sp)
                        }
                        val netEstimate = if(s.tax.isNotBlank() && s.deductTax) {
                            (earnings*(1.0-num(s.tax).coerceIn(0.0,100.0)/100.0)).coerceAtLeast(0.0)
                        } else null
                        if(netEstimate!=null) Text("Ориентир после НДФЛ: ${rub(netEstimate)}",
                            color=vahtaText,fontSize=15.sp)
                        Text("Без вычета питания, аванса и других удержаний.",color=muted,fontSize=11.sp)
                        Text("Прогноз, не расчётный листок. Часы смены условно распределены по суткам; дорога и МВО не включены в заработок за смены.",color=muted,fontSize=12.sp)
                    }
                            }
                            "payments" -> {                    if(showPayments) Panel("БЛИЖАЙШИЕ ВЫПЛАТЫ") {
                        val dates=listOfNotNull(nextPayDate(today,s.salaryDay)?.let { "Зарплата" to it },nextPayDate(today,s.advanceDay)?.let { "Аванс" to it }).sortedBy { it.second }
                        if(dates.isEmpty()) Text("Назначь дни выплат в «Финансы» → «Настройки»",color=muted)
                        dates.forEach { (type,date) ->
                            val forecast=estimateForPayday(s,date,type)
                            if(forecast!=null) Text("Прогноз $type: ${rub(forecast)}",color=mint,fontSize=13.sp)
                            val actual=payments.filter { it.date==date.toString() && it.type==type }.sumOf { it.amount }
                            Text("$type · ${date.pretty()}${if(actual>0) " · ${rub(actual)}" else ""}",color=vahtaText)
                        }
                    }
                            }
                        }
                    }
                    Button(onClick={tab=1}) { Text("Открыть календарь") }
                }
                1 -> ShiftCalendar(today,first,last,s,onChange,payments,events,::saveEvents,prefs)
                2 -> Finance(s,earnings,payments,::savePayments,organizerContext,onChange,prefs)
                3 -> OrganizerScreen(organizerContext, organizerRootRequest)
                4 -> key(settingsRootRequest) { Settings(s,onChange,onLock,prefs) }
            }
        }
        NavigationBar(containerColor=if(lightMode) Color(0xFFF0F2FA) else if(lavenderMode) Color(0xFF241C40) else Color(0xFF0B2345)) {
            listOf("Главная","Календарь","Финансы","Мои дела","Настройки").forEachIndexed { i,label ->
                NavigationBarItem(colors=NavigationBarItemDefaults.colors(selectedIconColor=Color.White,selectedTextColor=vahtaText,indicatorColor=if(lightMode) Color(0xFF5265B0) else if(lavenderMode) Color(0xFF8546C9) else Color(0xFFCB6B18)),selected=tab==i,onClick={if (i == 3 && tab == 3) organizerRootRequest++ else if (i == 4 && tab == 4) settingsRootRequest++ else tab=i},icon={Text(listOf("⌂","▦","₽","☑","⚙")[i])},label={Text(label,fontSize=9.sp,maxLines=1)})
            }
        }
    }
}
// Average monthly net estimate across 12 complete calendar months from the current month.
// Recurring rotation periods are used, not only the currently active shift.
private fun monthlyIncomeEstimate(s: Config, today: LocalDate): Double? {
    if(num(s.hourly)<=0 || num(s.hours)<=0 || parseDate(s.arrival)==null || parseDate(s.departure)==null) return null
    val start=YearMonth.from(today).atDay(1)
    var total=0.0
    for(i in 0 until 12) {
        val month=YearMonth.from(start).plusMonths(i.toLong())
        val (worked,shifts)=countShiftDays(s,month.atDay(1),month.atEndOfMonth())
        total+=wageForDays(s,worked,shifts).net
    }
    return total/12.0
}

@Composable private fun FinanceSettings(s: Config, onChange: (Config)->Unit) {
    Panel("ПРОФИЛЬ УСЛОВИЙ · ООО «ОЗЕРНОЕ»") {
        Text("Параметры из предоставленных таблиц. Применение изменит только перечисленные настройки; существующие даты, тариф, табель и поступления сохраняются.",color=muted,fontSize=12.sp)
        Button(onClick={onChange(s.copy(rotation="45×45",hours="11",coefficient="20",north="30",premium="20",allowance="700",safetyBonus="15000",foodCompensation="542.74",annualBonusPercent="15",annualVacationDays="36",tax="13",shiftMonthSupplement="0",deductTax=true,allowanceTaxFree=true,travelMode="day",travelDayRate="4794.96",meals="",mealDays="",mealMode="monthly"))},modifier=Modifier.fillMaxWidth()) { Text("Применить условия Озёрного") }
        Text("ПБиОТ 15 000 ₽ — сумма из расчётной таблицы, не гарантированная выплата. ЧТС не подставляется. НДФЛ 13% — расчётная ставка профиля; фактический налог зависит от годового дохода. Годовая премия и отпускные не прибавляются автоматически. Питание 542,74 ₽ начисляется за каждый рабочий день отдельно от зарплаты.",color=gold,fontSize=12.sp)
    }
    Panel("ОТДЕЛЬНЫЕ ВЫПЛАТЫ · СПРАВОЧНО") {
        Info("Питание за рабочий день (автоматически)",rub(num(s.foodCompensation)))
        
        Text("Вахтовая надбавка 700 ₽ начисляется за день вахты; питание 542,74 ₽ — за рабочий день. Отдельная месячная доплата не применяется.",color=muted,fontSize=12.sp)
        Field("Годовая премия, % (условная)",s.annualBonusPercent,true) { onChange(s.copy(annualBonusPercent=it)) }
        Field("Отпуск, календарных дней за год",s.annualVacationDays,true) { onChange(s.copy(annualVacationDays=it)) }
        val days = 45.0
        Info("Питание за 45 рабочих дней (пример)",rub(num(s.foodCompensation)*days))
        Text("Компенсация питания автоматически включается в прогноз отдельной строкой по рабочим дням; фактическое налогообложение и выплата зависят от расчётного листка. Годовая премия не гарантирована.",color=muted,fontSize=12.sp)
    }
    Panel("ОПЛАТА ТРУДА") {
        Field("ЧТС, ₽/час",s.hourly,true) { onChange(s.copy(hourly=it)) }
        Field("Часов в смене",s.hours,true) { onChange(s.copy(hours=it)) }
        Field("Районный коэффициент, % (например, 20)",s.coefficient,true) { onChange(s.copy(coefficient=it)) }
        Field("Северная надбавка, %",s.north,true) { onChange(s.copy(north=it)) }
        Field("Премия, %",s.premium,true) { onChange(s.copy(premium=it)) }
        Field("Вахтовая надбавка, ₽/день",s.allowance,true) { onChange(s.copy(allowance=it)) }
        Text("По условиям Озёрного: 4 794,96 ₽ за подтверждённый день в пути. Ставка и режим оплаты устанавливаются автоматически.",color=muted,fontSize=12.sp)
    }
    Panel("ИНДИВИДУАЛЬНАЯ НАДБАВКА") {
        Field("Название надбавки",s.personalName) { onChange(s.copy(personalName=it)) }
        Field("Размер надбавки",s.personalValue,true) { onChange(s.copy(personalValue=it)) }
        Text("Способ начисления",color=muted)
        listOf("Процент","Сумма, ₽").forEach { mode ->
            Row(Modifier.clickable { onChange(s.copy(personalMode=mode)) },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=s.personalMode==mode,onClick={onChange(s.copy(personalMode=mode))})
                Text(mode,color=vahtaText)
            }
        }
        if(s.personalMode=="Процент") {
            Text("База начисления",color=muted)
            listOf("Тарифный заработок","Тариф + доплаты").forEach { base ->
                Row(Modifier.clickable { onChange(s.copy(personalBase=base)) },verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(selected=s.personalBase==base,onClick={onChange(s.copy(personalBase=base))})
                    Text(base,color=vahtaText)
                }
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(s.personalCoefficient,{onChange(s.copy(personalCoefficient=it))})
            Text("Применять районный коэффициент и северную надбавку",color=vahtaText,fontSize=12.sp)
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(s.personalProrate,{onChange(s.copy(personalProrate=it))})
            Text("Пропорционально периоду (для фиксированной суммы)",color=vahtaText,fontSize=12.sp)
        }
        Text("В расчёте учитывается одна индивидуальная надбавка. Условия уточняй по договору и расчётному листку.",color=muted,fontSize=12.sp)
    }
    Panel("ПОДРОБНЫЙ РАСЧЁТ ЗАРПЛАТЫ · НЕОБЯЗАТЕЛЬНО") {
        Text("Для предприятий с почасовой оплатой. Значения по умолчанию не подставляются: укажи условия своего работодателя.",color=muted,fontSize=12.sp)
        Field("Ночных часов за месяц",s.nightHours,true) { onChange(s.copy(nightHours=it)) }
        Field("Доплата за ночные, %",s.nightPercent,true) { onChange(s.copy(nightPercent=it)) }
        Field("Вредные условия, %",s.harmfulPercent,true) { onChange(s.copy(harmfulPercent=it)) }
        Field("Премия за ОТ и ПБ, ₽/месяц",s.safetyBonus,true) { onChange(s.copy(safetyBonus=it)) }
        Field("Разовые премии, ₽/месяц",s.oneTimeBonus,true) { onChange(s.copy(oneTimeBonus=it)) }
        Text("★ Праздники РФ отмечаются в календаре автоматически. Оплата праздничных смен зависит от условий работодателя; ручное поле скрыто, старые данные сохранены.",color=gold,fontSize=12.sp)
    }
    Panel("ДОПОЛНИТЕЛЬНЫЕ НАЧИСЛЕНИЯ (НЕОБЯЗАТЕЛЬНО)") {
        Field("Оплачиваемые часы межвахты",s.intershiftHours,true) { onChange(s.copy(intershiftHours=it)) }
        Field("Ставка межвахты, ₽/час",s.intershiftRate,true) { onChange(s.copy(intershiftRate=it)) }
        Field("Дней МВО к оплате за месяц (ручной режим)",s.shiftRestDays,true) { onChange(s.copy(shiftRestDays=it)) }
        Text("АВТОМАТИЧЕСКИЙ РАСЧЁТ МВО ПО ТАБЕЛЮ",color=vahtaText,fontWeight=FontWeight.Bold)
        Field("Фактические часы по табелю",s.mvoActualHours,true) { onChange(s.copy(mvoActualHours=it)) }
        Field("Норма часов по производственному календарю",s.mvoNormHours,true) { onChange(s.copy(mvoNormHours=it)) }
        Field("Часы, исключаемые из МВО (праздники / двойная оплата)",s.mvoExcludedHours,true) { onChange(s.copy(mvoExcludedHours=it)) }
        val calculatedMvo = mvoDays(s)
        if (calculatedMvo != null) Text("Рассчитано дней МВО: $calculatedMvo. Оплата: ${rub(calculatedMvo * 8.0 * (num(s.intershiftRate).takeIf { it > 0 } ?: num(s.hourly)))}",color=vahtaText)
        Text("При заполнении фактических часов и нормы применяется автоматический расчёт; ручные дни МВО тогда не используются.",color=muted,fontSize=12.sp)
        Text("ОПЛАТА ДОРОГИ",color=vahtaText,fontWeight=FontWeight.Bold)
        listOf("day" to "За подтверждённый день", "hour" to "По оплачиваемым часам").forEach { (mode,label) ->
            Row(Modifier.fillMaxWidth().clickable { onChange(s.copy(travelMode=mode)) },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=s.travelMode==mode,onClick={onChange(s.copy(travelMode=mode))})
                Text(label,color=vahtaText,fontSize=13.sp)
            }
        }
        if(s.travelMode=="day") {
            Field("Оплата одного дня в пути, ₽",s.travelDayRate,true) { onChange(s.copy(travelDayRate=it)) }
            Text("Подтверди конкретные даты дороги в календаре. Без подтверждения начисления не будет.",color=muted,fontSize=12.sp)
            Info("Подтверждено дней",confirmedTravelDates(s).size.toString())
        } else {
            Field("Оплачиваемые часы дороги (общая сумма, старый режим)",s.travelHours,true) { onChange(s.copy(travelHours=it)) }
            Field("Ставка оплаты дороги, ₽/час",s.travelRate,true) { onChange(s.copy(travelRate=it)) }
            Text("Часы для каждой подтверждённой даты можно указать в календаре. Если указаны часы по датам, общая сумма часов не используется.",color=muted,fontSize=12.sp)
            Info("Подтверждено дней",confirmedTravelDates(s).size.toString())
        }
        Info("Начисление за дорогу",rub(travelPay(s)))
        Text("МВО рассчитывается по табелю. Не подставляй 3 часа за каждую смену как окончательный результат: норма, праздники и оплаченные в двойном размере часы могут изменить итог.",color=muted,fontSize=12.sp)
    }
    Panel("УДЕРЖАНИЯ") {
        Field("Количество выходных за вахту",s.restDays,true) { onChange(s.copy(restDays=it)) }
        Field("НДФЛ, %",s.tax,true) { onChange(s.copy(tax=it)) }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=s.deductTax,onCheckedChange={onChange(s.copy(deductTax=it))})
            Text("Вычитать НДФЛ из начислений",color=vahtaText,fontSize=13.sp)
        }
        Text("Как удерживают стоимость питания?",color=vahtaText,fontWeight=FontWeight.SemiBold)
        val daily=s.mealMode=="daily" || (s.mealMode.isBlank() && num(s.mealDays)>0)
        listOf("daily" to "За день", "monthly" to "За месяц").forEach { (mode,label) ->
            Row(Modifier.fillMaxWidth().clickable { onChange(s.copy(mealMode=mode)) },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=(mode=="daily")==daily,onClick={onChange(s.copy(mealMode=mode))})
                Text(label,color=vahtaText)
            }
        }
        Field(if(daily) "Стоимость питания, ₽/день" else "Всего за месяц, ₽",s.meals,true) { onChange(s.copy(meals=it)) }
        if(daily) Text("Количество дней для удержания питания задаётся отдельно от компенсации Озёрного. Компенсация 542,74 ₽ автоматически начисляется за каждый рабочий день.",color=muted,fontSize=12.sp)
        Text("Удержание за питание: ${rub(if(daily) num(s.meals)*num(s.mealDays) else num(s.meals))}",color=mint,fontSize=15.sp)
        
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=s.allowanceTaxFree,onCheckedChange={onChange(s.copy(allowanceTaxFree=it))})
            Text("Вахтовая надбавка не облагается НДФЛ в пределах установленного лимита (700 ₽/день по РФ)",color=vahtaText,fontSize=13.sp)
        }
        Field("Исполнительные удержания, ₽",s.enforcement,true) { onChange(s.copy(enforcement=it)) }
        Field("Прочие удержания, ₽",s.otherDeductions,true) { onChange(s.copy(otherDeductions=it)) }
    }
    Panel("ВЫПЛАТЫ") {
        Text("Выбери число месяца в календаре. Выплаты повторяются каждый месяц; выходные учитываются при прогнозе.",color=muted,fontSize=12.sp)
        DayPicker("Дата зарплаты",s.salaryDay) { onChange(s.copy(salaryDay=it)) }
        DayPicker("Дата аванса",s.advanceDay) { onChange(s.copy(advanceDay=it)) }
        val current=LocalDate.now()
        nextPayDate(current,s.advanceDay)?.let { Text("Следующий аванс: ${it.pretty()}",color=mint) }
        nextPayDate(current,s.salaryDay)?.let { Text("Следующая зарплата: ${it.pretty()}",color=mint) }
        listOf("По отработанным часам","Фиксированная сумма","Другой порядок").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { onChange(s.copy(advanceMethod=option)) },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=s.advanceMethod==option,onClick={onChange(s.copy(advanceMethod=option))}); Text(option,color=vahtaText)
            }
        }
        if(s.advanceMethod=="Фиксированная сумма") Field("Аванс, ₽",s.fixedAdvance,true) { onChange(s.copy(fixedAdvance=it)) }
    }
}

@Composable private fun Finance(s: Config,earnings: Double,payments: List<Payment>,onChange: (List<Payment>)->Unit,context: Context, onConfigChange: (Config)->Unit,prefs: SharedPreferences) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Зарплата") }
    val debts = remember(context) { readVahtaDebts(context) }
    val today = LocalDate.now()
    val sheetMode=timesheetMode(prefs)
    val thisMonth=YearMonth.from(today)
    val sheetEntries=monthTimesheet(prefs,thisMonth)
    val sheetForecast=if(sheetMode=="detailed") monthTimesheetForecast(s,prefs,thisMonth) else null
    val avgMonthly = remember(s, today.year, today.monthValue) { monthlyIncomeEstimate(s, today) }
    val monthPayments = debts.sumOf { it.monthly }
    val totalDebt = debts.sumOf { it.balance }
    val monthFree = avgMonthly?.minus(monthPayments)
    val fullFree = avgMonthly?.minus(totalDebt)
    val budgetForecast = readVahtaBudgetForecast(context, thisMonth)
    val afterPlans = monthFree?.minus(budgetForecast.planned)
    var financeTab by rememberSaveable { mutableIntStateOf(0) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        listOf("Прогноз", "Начисления", "Настройки").forEachIndexed { index, name ->
            OutlinedButton(onClick={financeTab=index}, modifier=Modifier.weight(1f),
                contentPadding=PaddingValues(horizontal=2.dp,vertical=8.dp)) {
                Text(name, fontSize=11.sp, color=if(financeTab==index) mint else muted)
            }
        }
    }
    if(financeTab==0) {
    val spending = readVahtaSpending(LocalContext.current, thisMonth)
    Panel("ЛИЧНЫЕ ЗАТРАТЫ ЗА МЕСЯЦ") {
        Text(rub(spending.total), color=mint, fontSize=25.sp, fontWeight=FontWeight.Bold)
        Info("Покупки (отмечены как купленные)", rub(spending.purchases))
        Info("Прочие расходы", rub(spending.other))
        Text("Личный учёт, без повторного вычета из зарплаты. Покупки считаются по дате записи.", color=muted, fontSize=11.sp)
    }
    Panel("МОЙ ФИНАНСОВЫЙ ПРОГНОЗ") {
        Text("СРЕДНИЙ ДОХОД ЗА МЕСЯЦ", color=muted, fontSize=12.sp)
        Text(avgMonthly?.let { rub(it) } ?: "Недостаточно данных", color=mint, fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text("Оценка после НДФЛ по графику вахт за 12 месяцев. Не фактическая выплата.",color=muted,fontSize=12.sp)
        if(sheetMode=="detailed") {
            HorizontalDivider()
            Text("ПРОГНОЗ ПО ТАБЕЛЮ ЗА ${thisMonth}",color=muted,fontSize=12.sp)
            Text(sheetForecast?.let { rub(it.net) } ?: "Нет заполненных рабочих смен",color=mint,fontSize=22.sp)
            Info("Записей за месяц",sheetEntries.size.toString())
            Info("Часов по рабочим сменам",sheetEntries.filter { it.kind=="Рабочий" }.sumOf { it.hours }.toString())
            Text("Это предварительный расчёт только по внесённым сменам, без автоматического определения полноты табеля, МВО и всех правил работодателя.",color=gold,fontSize=11.sp)
        }

        HorizontalDivider()
        Info("Обязательные платежи в месяц", rub(monthPayments))
        HorizontalDivider()
        Text("ЗАПЛАНИРОВАННЫЕ ТРАТЫ ИЗ ОРГАНАЙЗЕРА", color=muted, fontSize=12.sp)
        budgetForecast.byCategory.forEach { (name, cost) -> Info(name, rub(cost)) }
        if (budgetForecast.byCategory.isEmpty()) Text("На этот месяц расходов пока не запланировано", color=muted, fontSize=12.sp)
        Info("Всего запланировано", rub(budgetForecast.planned))
        Text("ОСТАНЕТСЯ ПОСЛЕ ПЛАТЕЖЕЙ И ПЛАНОВ", color=muted, fontSize=12.sp)
        Text(afterPlans?.let { rub(it) } ?: "—", color=if ((afterPlans ?: 0.0) < 0) gold else mint, fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text("Учитываются покупки, подарки, лекарства, отпуск и другие планы с суммой. Выполненные покупки исключаются из плана и отражаются в фактических расходах. План не вычитается из начисленной зарплаты повторно.", color=muted, fontSize=11.sp)
        Info("Обязательные платежи в месяц", rub(monthPayments))
        Text("ОСТАНЕТСЯ ПОСЛЕ ПЛАТЕЖЕЙ",color=muted,fontSize=12.sp)
        Text(monthFree?.let { rub(it) } ?: "—",color=if((monthFree ?: 0.0)<0) gold else mint,fontSize=25.sp,fontWeight=FontWeight.Bold)
        HorizontalDivider()
        Info("Все оставшиеся долги",rub(totalDebt))
        Text("ЕСЛИ ПОГАСИТЬ ВСЕ ДОЛГИ СРАЗУ",color=muted,fontSize=12.sp)
        Text(fullFree?.let { rub(it) } ?: "—",color=if((fullFree ?: 0.0)<0) gold else mint,fontSize=25.sp,fontWeight=FontWeight.Bold)
        if(fullFree != null && fullFree < 0) Text("Для полного погашения не хватает ${rub(-fullFree)}",color=gold,fontSize=12.sp)
        Text("Расчёт не включает повседневные расходы и другие обязательства, если они не внесены в долги.",color=muted,fontSize=12.sp)
    }
    if(sheetMode!="off") Panel("СТАТИСТИКА ПО ТАБЕЛЮ") {
        val records=loadWorkDays(context.getSharedPreferences("vahta_settings",Context.MODE_PRIVATE))
        val year=LocalDate.now().year
        val yearly=records.filter { parseDate(it.date)?.year==year }
        Info("Год",year.toString())
        Info("Отмечено дней",yearly.size.toString())
        Info("Рабочих дней",yearly.count { it.kind=="Рабочий" }.toString())
        Info("Дней МВО",yearly.count { it.kind=="МВО" }.toString())
        Info("Часов по табелю",yearly.sumOf { it.hours }.toString())
        Info("Ночных часов",yearly.sumOf { it.night }.toString())
        Info("Праздничных часов",yearly.sumOf { it.holiday }.toString())
        val received=payments.filter { parseDate(it.date)?.year==year }.sumOf { it.amount }
        Info("Фактически внесено выплат",rub(received))
        Text("Статистика по вручную заполненным дням и внесённым выплатам.",color=muted,fontSize=11.sp)
    }
    Panel("КОМУ И СКОЛЬКО ОТДАТЬ") {
        if(debts.isEmpty()) Text("Активных долгов нет. Добавь их в «Мои дела» → «Долги».",color=muted)
        debts.forEach { debt ->
            HorizontalDivider()
            Text(debt.name,color=vahtaText,fontWeight=FontWeight.SemiBold)
            Info("Остаток долга",rub(debt.balance))
            Info("Платёж за месяц",rub(debt.monthly))
            if(debt.dueDay in 1..31) Info("Число платежа",debt.dueDay.toString())
            if(debt.monthly == 0.0) Text("Месячный платёж не указан",color=gold,fontSize=12.sp)
        }
        Text("Редактировать долги: «Мои дела» → «Долги». Изменения появятся здесь при повторном открытии вкладки.",color=muted,fontSize=12.sp)
    }
    }
    if(financeTab==1) {
    Panel("РАСЧЁТНЫЕ НАЧИСЛЕНИЯ") {
        Text(rub(earnings),color=mint,fontSize=26.sp)
        Text("Прогноз начислений текущей вахты до удержаний",color=muted)
        Text("Дополнительные начисления задаются за месяц. Для прогноза выплат проверь все поля в настройках.",color=muted,fontSize=12.sp)
    }
    Panel("ДОБАВИТЬ ФАКТИЧЕСКУЮ ВЫПЛАТУ") {
        DateField("Дата получения",date) { date=it }
        Field("Поступило на карту, ₽",amount,true) { amount=it }
        listOf("Зарплата","Аванс","Межвахта","Смешанная","Отпускные","Другое").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { type=option },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=type==option,onClick={type=option}); Text(option,color=vahtaText)
            }
        }
        val forecast=parseDate(date)?.let { estimateForPayday(s,it,type) }
        if(forecast!=null) {
            Text("Предварительный прогноз: ${rub(forecast)}",color=mint)
            Text("Оценка основана на предыдущем календарном месяце, без подтверждённых начислений за дорогу, межвахту и отдельных удержаний.",color=muted,fontSize=12.sp)
        }
        Button(onClick={
            val savedForecast=forecast ?: -1.0
            onChange(payments+Payment(date,num(amount),type,savedForecast)); amount=""
        },enabled=num(amount)>0 && parseDate(date)!=null,modifier=Modifier.fillMaxWidth()) { Text("Сохранить выплату") }
    }
    MvoLedger(context,s)
    Panel("СВЕРКА ВЫПЛАТ ПО РАСЧЁТНОМУ ПЕРИОДУ") {
        val selectedMonth=YearMonth.from(today).minusMonths(1)
        val paidAdvance=payments.filter { it.type=="Аванс" &&
            parseDate(it.date)?.let { d -> YearMonth.from(d)==selectedMonth }==true }.sumOf { it.amount }
        val counts=countShiftDays(s,selectedMonth.atDay(1),selectedMonth.atEndOfMonth())
        val calendarWage=wageForDays(s,counts.first,counts.second)
        val recorded=monthTimesheet(prefs,selectedMonth)
        var sheetConfirmed by remember(selectedMonth) {
            mutableStateOf(prefs.getBoolean("sheet_confirmed_${selectedMonth}",false))
        }
        val useSheet=sheetMode=="detailed" && sheetConfirmed && recorded.any { it.kind=="Рабочий" }
        val wage=if(useSheet) monthTimesheetForecast(s,prefs,selectedMonth) ?: calendarWage else calendarWage
        Info("Расчётный месяц",selectedMonth.toString())
        if(sheetMode=="detailed") {
            Info("Внесено дней в табель",recorded.size.toString())
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=sheetConfirmed,onCheckedChange={ value ->
                    sheetConfirmed=value
                    prefs.edit().putBoolean("sheet_confirmed_${selectedMonth}",value).apply()
                })
                Text("Табель за месяц заполнен",color=vahtaText,fontSize=12.sp)
            }
            Text(if(useSheet) "Используется прогноз по подтверждённым сменам."
                else "Используется прогноз по графику вахт. Для расчёта по табелю заполни месяц и отметь подтверждение.",
                color=muted,fontSize=11.sp)
        }
        Info("Начислено (прогноз)",rub(wage.gross))
        if(useSheet) Text("МВО, сложные налоговые случаи и правила конкретного работодателя требуют отдельной сверки.",color=gold,fontSize=11.sp)
        Info("НДФЛ (оценка)",rub(wage.tax))
        Info("Другие удержания",rub(wage.deductions))
        Info("Фактически внесённые авансы",rub(paidAdvance))
        Text("Остаток после учтённых авансов: ${rub((wage.net-paidAdvance).coerceAtLeast(0.0))}",color=mint,fontWeight=FontWeight.Bold)
        Text("Для корректной сверки внеси фактический аванс в разделе выше. Сумма не заменяет расчётный листок; переходящий долг работодателя и отпускные нужно сверять отдельно.",color=muted,fontSize=12.sp)
    }
    Panel("ИСТОРИЯ И ПРОВЕРКА РАСХОЖДЕНИЙ") {
        Text("Всего поступило: ${rub(payments.sumOf { it.amount })}",color=mint)
        if(payments.isEmpty()) Text("Выплат пока нет",color=muted)
        payments.sortedByDescending { it.date }.forEach { p ->
            HorizontalDivider()
            Text("${parseDate(p.date)?.pretty() ?: p.date} · ${p.type}",color=muted)
            Text(rub(p.amount),color=vahtaText,fontSize=20.sp)
            if(p.forecast>=0) {
                val difference=p.amount-p.forecast
                Text("Первоначальный прогноз: ${rub(p.forecast)}",color=muted)
                Text("Расхождение: ${if(difference>0) "+" else ""}${rub(difference)}",color=if(abs(difference)<1) mint else gold)
                Text("Возможные причины: расчётный период, премия, табель, НДФЛ или удержания. Это не доказательство ошибки бухгалтерии.",color=muted,fontSize=12.sp)
            } else Text("Для этой выплаты точный прогноз не определён. Сумма сохранена без подгонки расчёта.",color=muted,fontSize=12.sp)
            TextButton(onClick={ val index=payments.indexOf(p); if(index>=0) onChange(payments.toMutableList().also { it.removeAt(index) }) }) { Text("Удалить") }
        }
    }
    }
    if(financeTab==2) FinanceSettings(s,onConfigChange)
}
@Composable private fun MvoLedger(context: Context,s: Config) {
    var entries by remember { mutableStateOf(loadMvoEntries(context)) }
    var month by remember { mutableStateOf(YearMonth.now().toString()) }
    var actual by remember { mutableStateOf("") }
    var norm by remember { mutableStateOf("") }
    var excluded by remember { mutableStateOf("") }
    var used by remember { mutableStateOf("") }
    var vacation by remember { mutableStateOf("") }
    var reserve by remember { mutableStateOf("") }
    val ym=runCatching { YearMonth.parse(month) }.getOrNull()
    val balance=ym?.let { mvoBalance(entries,it.year) }
    val yearEnd=ym?.monthValue==12
    val tariff=mvoTariff(s)
    Panel("МВО · ПОМЕСЯЧНЫЙ УЧЁТ") {
        Text("Введи месяц в формате ГГГГ-ММ. Записи сохраняются отдельно по месяцам и не заменяют настройки зарплаты.",color=muted,fontSize=12.sp)
        Field("Месяц (ГГГГ-ММ)",month) { v ->
            month=v
            val entry=entries.firstOrNull { it.month==v }
            actual=entry?.actualHours?.toString() ?: ""
            norm=entry?.normHours?.toString() ?: ""
            excluded=entry?.excludedHours?.toString() ?: ""
            used=entry?.usedDays?.toString() ?: ""
            vacation=entry?.vacationDays?.toString() ?: ""
            reserve=entry?.firstRestCarryDays?.toString() ?: ""
        }
        Field("Фактические часы по табелю",actual,true) { actual=it }
        Field("Норма часов за период",norm,true) { norm=it }
        Field("Исключаемые часы (двойная оплата)",excluded,true) { excluded=it }
        Field("Использовано дней МВО",used,true) { used=it }
        Field("Дней ежегодного отпуска (для контроля)",vacation,true) { vacation=it }
        if(yearEnd) Field("Из декабрьского остатка перенести на первый отдых следующего года",reserve,true) { reserve=it }
        val earned=kotlin.math.floor((num(actual)-num(norm)-num(excluded)).coerceAtLeast(0.0)/8).toInt()
        Info("Начисляется дней МВО за выбранный месяц",earned.toString())
        Info("Тариф МВО без РК, СН и надбавок",rub(tariff)+"/ч")
        Info("Оплата одного дня МВО (8 ч)",rub(tariff*8.0))
        if(tariff<=0.0) Text("Укажи часовую тарифную ставку в «Финансы → Параметры». Без неё сумма МВО не рассчитывается.",color=gold,fontSize=12.sp)
        Button(onClick={
            val e=MvoEntry(month,num(actual),num(norm),num(excluded),num(used).toInt(),num(vacation).toInt(),if(yearEnd) num(reserve).toInt() else 0)
            val next=entries.filterNot { it.month==month }+e
            saveMvoEntries(context,next); entries=next
        },enabled=ym!=null && actual.isNotBlank() && norm.isNotBlank(),modifier=Modifier.fillMaxWidth()) { Text("Сохранить месяц МВО") }
        if(balance!=null) {
            HorizontalDivider()
            Info("Начислено и перенесено дней за ${ym?.year}",balance.accrued.toString())
            Info("Использовано дней",balance.used.toString())
            Info("Остаток дней",balance.available.toString())
            if(yearEnd) {
                Info("Ориентир выплаты за неиспользованное МВО в декабре",rub(mvoPayout(balance.decemberPayout,s)))
                Info("Перенос на первый отдых следующего года",balance.nextYearCarry.toString()+" дн.")
            } else {
                Info("Ориентир стоимости накопленного остатка",rub(mvoPayout(balance.available,s)))
                Text("Это ещё не сумма к выплате: декабрьский расчёт появится при выборе месяца 12.",color=muted,fontSize=12.sp)
            }
        }
        Text("Данные табеля вносятся за каждый месяц отдельно. Если норма или фактические часы не введены, приложение не может достоверно рассчитать дни МВО. Декабрьский перенос вводится по графику первого отдыха следующего года.",color=gold,fontSize=12.sp)
        Text("Формула работодателя: тарифная часовая ставка без РК, СН и надбавок × 8 ч × оплачиваемые дни МВО. Начисление дней: полные 8-часовые дни из разницы факта и нормы после исключений. Отпуск записывается для контроля, но не вычитается из МВО автоматически. Декабрьский остаток за вычетом переноса — только прогноз; фактическую выплату сверяй с табелем и расчётным листком.",color=muted,fontSize=12.sp)
    }
}


@Composable private fun RunningShiftProgress(progress: Float) {
    val transition=rememberInfiniteTransition(label="runner")
    val bounce by transition.animateFloat(initialValue=0f,targetValue=4f,
        animationSpec=infiniteRepeatable(animation=tween(260,easing=LinearEasing),repeatMode=RepeatMode.Reverse),label="running")
    BoxWithConstraints(Modifier.fillMaxWidth().height(42.dp)) {
        val p=progress.coerceIn(0f,1f)
        val runnerWidth=26.dp
        val distance=(maxWidth-runnerWidth).coerceAtLeast(0.dp)
        LinearProgressIndicator(progress={p},modifier=Modifier.fillMaxWidth().padding(top=25.dp).height(9.dp),color=mint)
        Text("🏃",fontSize=25.sp,modifier=Modifier.offset(x=distance*p,y=(-bounce).dp).graphicsLayer(scaleX=-1f))
        Text(if(p>=1f) "🎉" else "⚑",fontSize=16.sp,color=mint,modifier=Modifier.align(Alignment.TopEnd))
    }
}

@Composable private fun Settings(s: Config,onChange: (Config)->Unit,onLock: ()->Unit,prefs: SharedPreferences) {
    var settingsPage by rememberSaveable { mutableStateOf<String?>(null) }
    val settingsImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val settingsKeyboard = LocalSoftwareKeyboardController.current
    val settingsFocus = LocalFocusManager.current
    BackHandler(enabled = settingsImeVisible) { settingsKeyboard?.hide(); settingsFocus.clearFocus() }
    BackHandler(enabled = settingsPage != null && !settingsImeVisible) { settingsPage = null }
    if (settingsPage == null) {
        val sections = listOf(
            Triple("ГРАФИК ВАХТ", "📅", "Даты, выходные и рабочие смены"),
            Triple("ЛИЧНЫЕ ДАННЫЕ", "👤", "Имя"),
            Triple("ОФОРМЛЕНИЕ", "🎨", "Цветовая тема"),
            Triple("ГЛАВНЫЙ ЭКРАН", "▦", "Карточки и их порядок"),
            Triple("НАПОМИНАНИЯ", "🔔", "Параметры напоминаний"),
            Triple("БЕЗОПАСНОСТЬ", "🔒", "Блокировка")
        )
        sections.forEach { (key, icon, description) ->
            Card(modifier=Modifier.fillMaxWidth().clickable { settingsPage=key },
                colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=13.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(icon,fontSize=23.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(key.lowercase().replaceFirstChar { it.uppercase() },color=vahtaText,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                        Text(description,color=muted,fontSize=11.sp)
                    }
                    Text("›",color=mint,fontSize=23.sp)
                }
            }
        }
    } else {
        TextButton(onClick={settingsPage=null}) { Text("‹  Все настройки",color=mint) }
    }
    if (settingsPage == "ЛИЧНЫЕ ДАННЫЕ")     Panel("ЛИЧНЫЕ ДАННЫЕ") { Field("Имя",s.name) { onChange(s.copy(name=it)) } }
    if (settingsPage == "ГРАФИК ВАХТ")     Panel("ГРАФИК ВАХТ") {
        RotationSelect(s.rotation) { onChange(s.copy(rotation=it)) }
        DateField("Дорога на вахту",s.inboundRoad) { onChange(s.copy(inboundRoad=it)) }
        DateField("Первая рабочая смена",s.arrival) { onChange(s.copy(arrival=it)) }
        DateField("Последняя рабочая смена",s.departure) { onChange(s.copy(departure=it)) }
        DateField("Дорога домой",s.outboundRoad) { onChange(s.copy(outboundRoad=it)) }
        Button(onClick={onChange(s.copy(travelDates=listOf(s.inboundRoad,s.outboundRoad).filter { parseDate(it)!=null }.joinToString(",")))},modifier=Modifier.fillMaxWidth()) { Text("Подтвердить дни дороги") }
        Text("Выходные за вахту изменяются в разделе Финансы → Настройки.",color=muted,fontSize=12.sp)
        val shiftStart = parseDate(s.arrival)
        val shiftEnd = parseDate(s.departure)
        if (shiftStart != null && shiftEnd != null && !shiftEnd.isBefore(shiftStart)) {
            val daysOff = loadWorkDays(prefs).count { day ->
                day.kind == "Выходной" && runCatching { LocalDate.parse(day.date) }.getOrNull()?.let { !it.isBefore(shiftStart) && !it.isAfter(shiftEnd) } == true
            }
            val calendarDays = ChronoUnit.DAYS.between(shiftStart, shiftEnd).toInt() + 1
            val plannedDaysOff = num(s.restDays).toInt().coerceIn(0,calendarDays)
            Text("Выходных по графику: $plannedDaysOff · Рабочих смен: ${calendarDays - plannedDaysOff}", color=mint, fontWeight=FontWeight.SemiBold)

        }
        val a=parseDate(s.arrival); val b=parseDate(s.departure)
        if(a!=null && b!=null && !b.isBefore(a)) Text("Фактически: ${ChronoUnit.DAYS.between(a,b)+1} дней",color=mint)
    }
    if (settingsPage == "ОФОРМЛЕНИЕ")     Panel("ОФОРМЛЕНИЕ") {
        val context=LocalContext.current

        listOf("Тёмная · бирюза", "Тёмная · лаванда", "Светлая · пастель").forEachIndexed { index,option ->
            Row(Modifier.fillMaxWidth().clickable {
                appearanceMode=index
                context.getSharedPreferences("vahta_appearance",Context.MODE_PRIVATE).edit().putInt("mode",index).apply()
            },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=appearanceMode==index,onClick={
                    appearanceMode=index
                    context.getSharedPreferences("vahta_appearance",Context.MODE_PRIVATE).edit().putInt("mode",index).apply()
                })
                Text(option,color=vahtaText)
            }
        }
    }
    if (settingsPage == "ГЛАВНЫЙ ЭКРАН")     Panel("ГЛАВНЫЙ ЭКРАН") {
        Text("Показывать карточки и менять порядок",color=muted)
        var revision by remember { mutableIntStateOf(0) }
        val order=remember(revision) { readHomeOrder(prefs) }
        order.forEachIndexed { index,key ->
            val title=when(key) { "schedule"->"График вахты";"counter"->"Живой счётчик";else->"Ближайшие выплаты" }
            val prefKey=when(key) { "schedule"->"home_schedule";"counter"->"home_counter";else->"home_payments" }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=prefs.getBoolean(prefKey,true),onCheckedChange={
                    prefs.edit().putBoolean(prefKey,it).apply();revision++
                })
                Text(title,color=vahtaText,modifier=Modifier.weight(1f),fontSize=13.sp)
                TextButton(onClick={
                    if(index>0) {
                        val updated=order.toMutableList()
                        val swap=updated[index-1];updated[index-1]=updated[index];updated[index]=swap
                        prefs.edit().putString("home_order",updated.joinToString(",")).apply();revision++
                    }
                },enabled=index>0) { Text("↑") }
                TextButton(onClick={
                    if(index<order.lastIndex) {
                        val updated=order.toMutableList()
                        val swap=updated[index+1];updated[index+1]=updated[index];updated[index]=swap
                        prefs.edit().putString("home_order",updated.joinToString(",")).apply();revision++
                    }
                },enabled=index<order.lastIndex) { Text("↓") }
            }
        }
        Text("Изменения применяются при возвращении на главную.",color=muted,fontSize=11.sp)
    }
    if (settingsPage == "НАПОМИНАНИЯ")     Panel("НАПОМИНАНИЯ") {
        Text("Включить напоминания?",color=vahtaText)
        Row(verticalAlignment=Alignment.CenterVertically) {
            RadioButton(selected=s.reminders=="yes",onClick={onChange(s.copy(reminders="yes"))}); Text("Да",color=vahtaText)
            RadioButton(selected=s.reminders=="no",onClick={onChange(s.copy(reminders="no"))}); Text("Нет",color=vahtaText)
        }
        if(s.reminders=="yes") ReminderOptions(s,onChange)
        Text("Параметры напоминаний сохраняются. Push-уведомления пока не подключены.",color=muted,fontSize=12.sp)
    }
    if (settingsPage == "БЕЗОПАСНОСТЬ")     Panel("БЕЗОПАСНОСТЬ") { OutlinedButton(onClick=onLock,modifier=Modifier.fillMaxWidth()) { Text("Заблокировать приложение") } }
}
@Composable private fun ShiftCalendar(today: LocalDate,first: LocalDate?,last: LocalDate?,s: Config,onChange: (Config)->Unit,
    payments: List<Payment>,events: List<CalendarEvent>,onEvents: (List<CalendarEvent>)->Unit,prefs: SharedPreferences) {
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var eventTitle by remember { mutableStateOf("") }
    var workDays by remember { mutableStateOf(loadWorkDays(prefs)) }
    var kind by remember(selected) { mutableStateOf(workDays.firstOrNull { it.date==selected.toString() }?.kind ?: "Рабочий") }
    var workHours by remember(selected) { mutableStateOf(workDays.firstOrNull { it.date==selected.toString() }?.hours?.toString() ?: "") }
    var nightHours by remember(selected) { mutableStateOf(workDays.firstOrNull { it.date==selected.toString() }?.night?.toString() ?: "") }
    var holidayHours by remember(selected) { mutableStateOf(workDays.firstOrNull { it.date==selected.toString() }?.holiday?.toString() ?: "") }
    val sheetMode=timesheetMode(prefs)
    val valid=first!=null && last!=null && last.isAfter(first) && s.rotation in rotations
    Panel("КАЛЕНДАРЬ ВАХТ И ВЫПЛАТ") {
        var calendarDrag by remember { mutableFloatStateOf(0f) }
        Column(Modifier.fillMaxWidth().pointerInput(month) {
            detectHorizontalDragGestures(onDragStart={calendarDrag=0f},onHorizontalDrag={change,dx -> calendarDrag+=dx;change.consume()},
                onDragEnd={if(calendarDrag < -75f) month=month.plusMonths(1) else if(calendarDrag > 75f) month=month.minusMonths(1);calendarDrag=0f},
                onDragCancel={calendarDrag=0f})
        },verticalArrangement=Arrangement.spacedBy(7.dp)) {
        MonthHeader(month,{month=month.minusMonths(1)},{month=month.plusMonths(1)})
        WeekHeader()
        val offset=month.atDay(1).dayOfWeek.value-1
        val count=month.lengthOfMonth()
        repeat((offset+count+6)/7) { row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                repeat(7) { col ->
                    val day=row*7+col-offset+1
                    if(day in 1..count) {
                        val date=month.atDay(day)
                        val p=if(valid) periodFor(date,first!!,last!!,s.rotation) else null
                        val arrival=p!=null && date==p.start
                        val departure=p!=null && date==p.end
                        val salary=isActualPayDate(date,s.salaryDay)
                        val advance=isActualPayDate(date,s.advanceDay)
                        val hasEvent=events.any { it.date==date.toString() }
                        val recorded=workDays.firstOrNull { it.date==date.toString() }
                        val holidayName = officialHolidayName(date)
                        val travelMarked=date in confirmedTravelDates(s)
                        val color=when { travelMarked -> Color(0xFF7359A0); recorded?.kind=="Рабочий" -> Color(0xFF376D58); recorded?.kind=="Выходной" -> Color(0xFF49435E); departure -> gold; arrival -> Color(0xFF278E7A); p?.working==true -> Color(0xFF204F4A); else -> Color(0xFF233F65) }
                        Box(Modifier.weight(1f).height(58.dp).background(if (holidayName != null) Color(0xFF77513B) else color,RoundedCornerShape(9.dp))
                            .then(if(date==selected) Modifier.border(2.dp,if(lightMode) Color(0xFF2E405A) else Color.White,RoundedCornerShape(9.dp)) else Modifier)
                            .clickable { selected=date },contentAlignment=Alignment.Center) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                Text("$day",color=if(departure) bg else Color.White,fontWeight=FontWeight.Bold)
                                Text(buildString { if(salary) append("₽"); if(advance) append("₳"); if(hasEvent) append("•")
                                    if(holidayName != null) append("★"); if(travelMarked) append("✈"); if(arrival) append("↑"); if(departure) append("✓"); if(recorded!=null) append("✎") },fontSize=11.sp,lineHeight=13.sp,color=if(departure) bg else mint,maxLines=1)
                            }
                        }
                    } else Spacer(Modifier.weight(1f).height(58.dp))
                }
            }
        }
        } // Жесты внутри сетки листают месяцы
        Text("★ праздник РФ · ₽ зарплата · ₳ аванс · ✈ дорога · • событие",color=muted,fontSize=12.sp)
        HorizontalDivider()
        Text(selected.pretty(),color=vahtaText,fontSize=20.sp,fontWeight=FontWeight.Bold)
        // Planned days off are entered once in settings and distributed automatically.
        // Existing calendar absence records remain available for timesheet history.
        officialHolidayName(selected)?.let { holiday ->
            Text("★ $holiday · официальный праздник РФ", color=Color(0xFFFFD3A4), fontWeight=FontWeight.SemiBold)
        }
        if(valid) {
            val p=periodFor(selected,first!!,last!!,s.rotation)
            Text(when { selected==p.start -> "✈ Заезд — день в пути (подтверди ниже)"; selected==p.end -> "✓ Последний рабочий день"; selected==periodFor(selected.minusDays(1),first!!,last!!,s.rotation).end.plusDays(1) -> "✈ Дорога домой (подтверди ниже)"
                p.working -> "На вахте · до выезда ${p.remaining} дн."; else -> "Межвахта · до заезда ${p.remaining} дн." },color=mint)
        }
        val confirmed = confirmedTravelDates(s)
        val isTravelDay = selected in confirmed
        val proposedTravel = if(valid) {
            val period = periodFor(selected,first!!,last!!,s.rotation)
            selected==period.start || (valid && selected==periodFor(selected.minusDays(1),first!!,last!!,s.rotation).end.plusDays(1))
        } else false
        if (proposedTravel || isTravelDay) {
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF44365D)),shape=RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text("✈ ДЕНЬ В ПУТИ",color=Color(0xFFE8CFFF),fontWeight=FontWeight.Bold)
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=isTravelDay,onCheckedChange={ checked ->
                            val dates=if(checked) confirmed+selected else confirmed.filterNot { it==selected }
                            onChange(s.copy(travelDates=dates.distinct().sorted().joinToString(",")))
                        })
                        Text("Подтвердить оплату за эту дату",color=vahtaText,fontSize=13.sp)
                    }
                    if (s.travelMode=="hour" && isTravelDay) {
                        Field("Оплачиваемых часов за ${selected.pretty()}",travelHoursForDate(s,selected),true) { value ->
                            onChange(withTravelHoursForDate(s,selected,value))
                        }
                    }
                    Text(if(s.travelMode=="day") "Оплата: ${rub(num(s.travelDayRate))} за день" else
                        "Оплата за дату: ${rub(num(travelHoursForDate(s,selected)) * (num(s.travelRate).takeIf { it>0 } ?: num(s.hourly)))}",color=muted,fontSize=12.sp)
                }
            }
        }
        val isSalary=isActualPayDate(selected,s.salaryDay)
        val isAdvance=isActualPayDate(selected,s.advanceDay)
        if(isSalary || isAdvance) {
            val type=if(isSalary) "Зарплата" else "Аванс"
            val forecast=estimateForPayday(s,selected,type)
            Text("$type · ${forecast?.let { "прогноз ${rub(it)}" } ?: "сумма не определена"}",color=mint)
        }
        val dayPayments=payments.filter { it.date==selected.toString() }
        dayPayments.forEach { Text("Получено: ${it.type} · ${rub(it.amount)}",color=gold) }
        if(dayPayments.isNotEmpty()) Text("Всего поступило: ${rub(dayPayments.sumOf { it.amount })}",color=vahtaText)
        HorizontalDivider()
        if(sheetMode!="off") {
        Text("ТАБЕЛЬ ЗА ДЕНЬ",color=vahtaText,fontWeight=FontWeight.Bold)
        Text("Ручные записи не меняют базовый график вахт.",color=muted,fontSize=11.sp)
        listOf("Рабочий","Выходной","Дорога","МВО","Отпуск","Больничный").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { kind=option },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=kind==option,onClick={kind=option})
                Text(option,color=vahtaText,fontSize=13.sp)
            }
        }
        if(kind=="Рабочий" || kind=="Дорога") {
            if(sheetMode=="detailed") {
                Field("Отработано часов",workHours,true) { workHours=it }
                Field("Из них ночных",nightHours,true) { nightHours=it }
                Field("Из них праздничных",holidayHours,true) { holidayHours=it }
            } else Text("Часов по графику: ${num(s.hours)}",color=muted,fontSize=12.sp)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={
                val actualHours=if(kind=="Рабочий" || kind=="Дорога") {
                    if(sheetMode=="simple") num(s.hours) else num(workHours)
                } else 0.0
                val item=WorkDay(selected.toString(),kind,actualHours,
                    if(sheetMode=="detailed") num(nightHours).coerceAtMost(actualHours) else 0.0,
                    if(sheetMode=="detailed") num(holidayHours).coerceAtMost(actualHours) else 0.0)
                workDays=workDays.filterNot { it.date==item.date }+item
                saveWorkDays(prefs,workDays)
                prefs.edit().putBoolean("sheet_confirmed_${YearMonth.from(selected)}",false).apply()
            }) { Text("Сохранить день") }
            OutlinedButton(onClick={
                workDays=workDays.filterNot { it.date==selected.toString() }
                saveWorkDays(prefs,workDays)
                prefs.edit().putBoolean("sheet_confirmed_${YearMonth.from(selected)}",false).apply()
            }) { Text("Сбросить") }
        }
        val monthItems=workDays.filter { parseDate(it.date)?.let { d -> YearMonth.from(d)==month }==true }
        Info("Заполнено дней за месяц",monthItems.size.toString())
        Info("Часов по табелю",monthItems.sumOf { it.hours }.toString())
        Info("Ночных часов",monthItems.sumOf { it.night }.toString())
        Info("Праздничных часов",monthItems.sumOf { it.holiday }.toString())
        Text(if(sheetMode=="detailed") "По введённым сменам доступен предварительный расчёт в «Финансы» → «Прогноз». МВО и правила предприятия сверяй отдельно."
            else "Простой табель — учёт дней и часов по графику. Для фактических часов переключись на подробный режим в настройках.",color=muted,fontSize=11.sp)
        }
        HorizontalDivider()
        Text("Личные события",color=vahtaText,fontWeight=FontWeight.Bold)
        events.filter { it.date==selected.toString() }.forEach { event ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("• ${event.title}",color=vahtaText,modifier=Modifier.weight(1f))
                TextButton(onClick={val i=events.indexOf(event);if(i>=0) onEvents(events.toMutableList().also { it.removeAt(i) })}) { Text("Удалить") }
            }
        }
        Field("Название события",eventTitle) { eventTitle=it }
        Button(onClick={onEvents(events+CalendarEvent(selected.toString(),eventTitle.trim()));eventTitle=""},enabled=eventTitle.isNotBlank()) { Text("Добавить событие") }
        OutlinedButton(onClick={month=YearMonth.from(today);selected=today},modifier=Modifier.fillMaxWidth()) { Text("Сегодня") }
    }
}
@Composable private fun DayPicker(label: String,value: String,onChange: (String)->Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) { Text("$label: ${value.ifBlank { "Выбрать" }}") }
    if(open) AlertDialog(onDismissRequest={open=false},title={Text(label)},text={
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            repeat(5) { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    repeat(7) { col ->
                        val day=row*7+col+1
                        if(day<=31) Box(Modifier.weight(1f).height(38.dp).clickable { onChange(day.toString());open=false },contentAlignment=Alignment.Center) {
                            Text("$day",color=if(value==day.toString()) mint else Color.White)
                        } else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    },confirmButton={TextButton(onClick={onChange("");open=false}) { Text("Очистить") }},dismissButton={TextButton(onClick={open=false}) { Text("Закрыть") }},containerColor=panel)
}
@Composable private fun MonthHeader(month: YearMonth,previous: ()->Unit,next: ()->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=previous) { Text("‹",fontSize=30.sp,color=mint) }
        Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",ru)).replaceFirstChar { it.uppercase() },color=vahtaText,fontSize=17.sp,fontWeight=FontWeight.Bold)
        IconButton(onClick=next) { Text("›",fontSize=30.sp,color=mint) }
    }
}
@Composable private fun WeekHeader() {
    Row(Modifier.fillMaxWidth()) { listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach {
        Text(it,modifier=Modifier.weight(1f),textAlign=TextAlign.Center,fontSize=11.sp,color=muted)
    } }
}
@Composable private fun DateField(label: String,value: String,onChange: (String)->Unit) {
    var open by remember { mutableStateOf(false) }
    val selected=parseDate(value)
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) { Text("$label: ${selected?.pretty() ?: "Выбрать дату"}") }
    if(open) {
        var month by remember(open) { mutableStateOf(YearMonth.from(selected ?: LocalDate.now())) }
        AlertDialog(onDismissRequest={open=false},title={Text(label)},text={
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                MonthHeader(month,{month=month.minusMonths(1)},{month=month.plusMonths(1)})
                WeekHeader()
                val offset=month.atDay(1).dayOfWeek.value-1
                val count=month.lengthOfMonth()
                repeat((offset+count+6)/7) { row ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val day=row*7+col-offset+1
                            Box(Modifier.weight(1f).height(38.dp).then(if(day in 1..count) Modifier.clickable { onChange(month.atDay(day).toString());open=false } else Modifier),contentAlignment=Alignment.Center) {
                                if(day in 1..count) Text("$day",color=if(month.atDay(day)==selected) mint else Color.White)
                            }
                        }
                    }
                }
            }
        },confirmButton={TextButton(onClick={open=false}) { Text("Закрыть") }},containerColor=panel)
    }
}
@Composable private fun Heading(title: String,description: String) {
    Text(title,color=vahtaText,fontSize=27.sp,fontWeight=FontWeight.Bold)
    Text(description,color=muted,fontSize=14.sp)
}
@Composable private fun Panel(title: String,content: @Composable ColumnScope.()->Unit) {
    Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),
        colors=CardDefaults.cardColors(containerColor=themedPanelColor(title)),
        elevation=CardDefaults.cardElevation(defaultElevation=3.dp)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(title,color=themedPanelAccent(title),fontSize=12.sp,fontWeight=FontWeight.Bold)
            content()
        }
    }
}
@Composable private fun Info(label: String,value: String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,color=muted,fontSize=13.sp)
        Text(value,color=vahtaText,fontSize=13.sp)
    }
}
@Composable private fun Field(label: String,value: String,numeric: Boolean=false,onChange: (String)->Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=if(numeric) KeyboardType.Decimal else KeyboardType.Text, capitalization=if(numeric) androidx.compose.ui.text.input.KeyboardCapitalization.None else androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction=ImeAction.Done),
        keyboardActions=KeyboardActions(onDone={ focus.clearFocus(force = true); keyboard?.hide() }),
        shape=RoundedCornerShape(15.dp),
        colors=OutlinedTextFieldDefaults.colors(
            focusedBorderColor=mint,focusedLabelColor=mint,
            unfocusedBorderColor=muted.copy(alpha=0.45f)),
        modifier=Modifier.fillMaxWidth())
}
