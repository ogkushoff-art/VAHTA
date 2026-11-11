package com.vahta.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalContext
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

internal var lavenderMode by mutableStateOf(false)
private val bg get() = if(lavenderMode) Color(0xFF1B102A) else Color(0xFF0B1728)
private val panel get() = if(lavenderMode) Color(0xFF352148) else Color(0xFF192D45)
private val mint get() = if(lavenderMode) Color(0xFFD9A5FF) else Color(0xFF56E4C0)
private val muted get() = if(lavenderMode) Color(0xFFE0C9F0) else Color(0xFFB4C6D9)
private val blue get() = if(lavenderMode) Color(0xFFAF8DFF) else Color(0xFF4389D8)
private val gold = Color(0xFFFFB45B)
private val ru = Locale.forLanguageTag("ru-RU")
private val rotations = listOf("15×15", "30×30", "45×45", "60×60", "60×30")
private fun parseDate(v: String): LocalDate? = runCatching { LocalDate.parse(v) }.getOrNull()
private fun LocalDate.pretty() = format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
private fun num(v: String): Double = v.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
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
    val hourly: String = "", val hours: String = "", val workDays: String = "", val restDays: String = "",
    val coefficient: String = "", val north: String = "", val premium: String = "", val allowance: String = "",
    val travelHours: String = "", val intershiftHours: String = "", val intershiftRate: String = "",
    val tax: String = "", val meals: String = "", val enforcement: String = "", val otherDeductions: String = "",
    val advanceDay: String = "", val salaryDay: String = "", val advanceMethod: String = "По отработанным часам",
    val fixedAdvance: String = "", val nightHours: String = "", val nightPercent: String = "", val harmfulPercent: String = "",
    val safetyBonus: String = "", val oneTimeBonus: String = "", val holidayHours: String = "",
    val mealDays: String = "", val travelRate: String = "", val shiftRestDays: String = "",
    val mvoActualHours: String = "", val mvoNormHours: String = "", val mvoExcludedHours: String = "",
    val personalName: String = "", val personalValue: String = "", val personalMode: String = "Процент",
    val personalBase: String = "Тарифный заработок", val personalProrate: Boolean = true,
    val personalCoefficient: Boolean = false, val personalMonthly: Boolean = true,
    val allowanceTaxFree: Boolean = true, val notes: String = "", val reminders: String = "",
    val remindArrival: Boolean = false, val remindDeparture: Boolean = false,
    val remindSalary: Boolean = false, val remindAdvance: Boolean = false, val remindEvents: Boolean = false,
    val remindDays: String = "1", val remindTime: String = "09:00"
)
private fun readConfig(p: SharedPreferences, prefix: String = ""): Config {
    fun g(k: String) = p.getString(prefix + k, "") ?: ""
    fun b(k: String) = p.getBoolean(prefix + k, false)
    return Config(
        name=g("name"), rotation=g("rotation"), arrival=g("arrival"), departure=g("departure"),
        hourly=g("hourly"), hours=g("hours"), workDays=g("workDays"), restDays=g("restDays"),
        coefficient=g("coefficient"), north=g("north"), premium=g("premium"), allowance=g("allowance"),
        travelHours=g("travelHours"), intershiftHours=g("intershiftHours"), intershiftRate=g("intershiftRate"),
        tax=g("tax"), meals=g("meals"), enforcement=g("enforcement"), otherDeductions=g("otherDeductions"),
        advanceDay=g("advanceDay"), salaryDay=g("salaryDay"), advanceMethod=g("advanceMethod").ifBlank { "По отработанным часам" },
        fixedAdvance=g("fixedAdvance"), nightHours=g("nightHours"), nightPercent=g("nightPercent"),
        harmfulPercent=g("harmfulPercent"), safetyBonus=g("safetyBonus"), oneTimeBonus=g("oneTimeBonus"),
        holidayHours=g("holidayHours"), mealDays=g("mealDays"), travelRate=g("travelRate"),
        shiftRestDays=g("shiftRestDays"), mvoActualHours=g("mvoActualHours"),
        mvoNormHours=g("mvoNormHours"), mvoExcludedHours=g("mvoExcludedHours"), personalName=g("personalName"), personalValue=g("personalValue"), personalMode=g("personalMode").ifBlank { "Процент" },
        personalBase=g("personalBase").ifBlank { "Тарифный заработок" }, personalProrate=p.getBoolean(prefix+"personalProrate",true),
        personalCoefficient=b("personalCoefficient"), personalMonthly=p.getBoolean(prefix+"personalMonthly",true),
        allowanceTaxFree=p.getBoolean(prefix+"allowanceTaxFree",true), notes=g("notes"), reminders=g("reminders"),
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
        put("hourly",s.hourly); put("hours",s.hours); put("workDays",s.workDays); put("restDays",s.restDays)
        put("coefficient",s.coefficient); put("north",s.north); put("premium",s.premium); put("allowance",s.allowance)
        put("travelHours",s.travelHours); put("intershiftHours",s.intershiftHours); put("intershiftRate",s.intershiftRate)
        put("tax",s.tax); put("meals",s.meals); put("enforcement",s.enforcement); put("otherDeductions",s.otherDeductions)
        put("advanceDay",s.advanceDay); put("salaryDay",s.salaryDay); put("advanceMethod",s.advanceMethod)
        put("fixedAdvance",s.fixedAdvance); put("nightHours",s.nightHours); put("nightPercent",s.nightPercent)
        put("harmfulPercent",s.harmfulPercent); put("safetyBonus",s.safetyBonus); put("oneTimeBonus",s.oneTimeBonus)
        put("holidayHours",s.holidayHours); put("mealDays",s.mealDays); put("travelRate",s.travelRate)
        put("shiftRestDays",s.shiftRestDays); put("mvoActualHours",s.mvoActualHours);
        put("mvoNormHours",s.mvoNormHours); put("mvoExcludedHours",s.mvoExcludedHours); put("personalName",s.personalName); put("personalValue",s.personalValue); put("personalMode",s.personalMode)
        put("personalBase",s.personalBase); put("personalProrate",s.personalProrate); put("personalCoefficient",s.personalCoefficient)
        put("personalMonthly",s.personalMonthly); put("allowanceTaxFree",s.allowanceTaxFree); put("notes",s.notes); put("reminders",s.reminders)
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
private fun wageForDays(s: Config, workDays: Int, shiftDays: Int, fraction: Double = 1.0): WageBreakdown {
    val base = workDays * num(s.hours) * num(s.hourly)
    val night = num(s.nightHours) * num(s.hourly) * num(s.nightPercent)/100 * fraction
    val harmful = base * num(s.harmfulPercent)/100
    val premium = base * num(s.premium)/100
    val personalBaseAmount = if(s.personalBase=="Тариф + доплаты") base+night+harmful else base
    val personalRaw = if(s.personalMode=="Сумма, ₽") num(s.personalValue) * (if(s.personalProrate) fraction else 1.0)
        else personalBaseAmount * num(s.personalValue).coerceAtMost(1000.0)/100.0
    val coefficientFactor = (num(s.coefficient).takeIf { it>0 } ?: 1.0) + num(s.north)/100
    val adjusted = (base + night + harmful + premium) * coefficientFactor +
        personalRaw * (if(s.personalCoefficient) coefficientFactor else 1.0)
    val safety = num(s.safetyBonus)*fraction
    val oneTime = num(s.oneTimeBonus)*fraction
    val holiday = num(s.holidayHours)*num(s.hourly)*2*fraction
    val travel = num(s.travelHours)*num(s.travelRate).takeIf { it>0 }.let { it ?: num(s.hourly) }*fraction
    val mvo = (mvoDays(s)?.toDouble()?.times(8.0) ?: if(num(s.shiftRestDays)>0) num(s.shiftRestDays)*8 else num(s.intershiftHours))*
        num(s.intershiftRate).takeIf { it>0 }.let { it ?: num(s.hourly) }*fraction
    val allowance = shiftDays*num(s.allowance)
    val taxable = adjusted + safety + oneTime + holiday + travel + mvo + if(s.allowanceTaxFree) 0.0 else allowance
    val free = if(s.allowanceTaxFree) allowance else 0.0
    val tax = taxable*num(s.tax).coerceIn(0.0,100.0)/100
    val meals = if(num(s.mealDays)>0) num(s.mealDays)*num(s.meals)*fraction else num(s.meals)*fraction
    val deductions = meals + (num(s.enforcement)+num(s.otherDeductions))*fraction
    return WageBreakdown(taxable,free,taxable+free,tax,deductions,(taxable+free-tax-deductions).coerceAtLeast(0.0))
}
private fun countShiftDays(s: Config, start: LocalDate, end: LocalDate): Pair<Int,Int> {
    val anchor = parseDate(s.arrival) ?: return 0 to 0
    val departure = parseDate(s.departure) ?: return 0 to 0
    if(departure.isBefore(anchor) || s.rotation !in rotations || end.isBefore(start)) return 0 to 0
    val work = (s.workDays.toIntOrNull() ?: 1).coerceIn(1,366)
    val rest = (s.restDays.toIntOrNull() ?: 0).coerceIn(0,366)
    var worked=0; var shifts=0; var day=start
    while(!day.isAfter(end)) {
        val period=periodFor(day,anchor,departure,s.rotation)
        if(period.working) {
            shifts++
            val idx=ChronoUnit.DAYS.between(period.start,day)
            if(Math.floorMod(idx,(work+rest).toLong())<work) worked++
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
private fun netEstimate(gross: Double, s: Config): Double = (gross*(1-num(s.tax).coerceIn(0.0,100.0)/100)-num(s.enforcement)-num(s.otherDeductions)).coerceAtLeast(0.0)
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
        val factor=(num(s.coefficient).takeIf { it>0 } ?: 1.0)+num(s.north)/100
        return (basic*factor*(1-num(s.tax).coerceIn(0.0,100.0)/100)).coerceAtLeast(0.0)
    }
    val breakdown=wageForDays(s,worked,shifts)
    val advance=estimateForPayday(s,month.atDay(30.coerceAtMost(month.lengthOfMonth())),"Аванс") ?: 0.0
    return (breakdown.net-advance).coerceAtLeast(0.0)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs=getSharedPreferences("vahta_settings",Context.MODE_PRIVATE)
        lavenderMode=getSharedPreferences("vahta_appearance",Context.MODE_PRIVATE).getBoolean("lavender",false)
        setContent {
            var unlocked by remember { mutableStateOf(false) }
            var setupDone by remember { mutableStateOf(prefs.getBoolean("setup_complete_v2",false)) }
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
                if(!unlocked) SecurityScreen(onUnlocked={ unlocked=true })
                else if(!setupDone) Wizard(prefs) { result ->
                    config=result; saveConfig(prefs,result)
                    prefs.edit().putBoolean("setup_complete_v2",true).commit()
                    clearDraft(prefs); setupDone=true
                }
                else Dashboard(config,{ config=it; saveConfig(prefs,it) },prefs,{ unlocked=false },this@MainActivity)
            }
        }
    }
}
@Composable private fun VahtaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=mint,background=bg,surface=panel,onBackground=Color.White,onSurface=Color.White),content=content)
}
@Composable private fun Wizard(prefs: SharedPreferences,onFinish: (Config)->Unit) {
    var s by remember { mutableStateOf(readConfig(prefs,"draft_")) }
    // Новый короткий опрос. Старый черновик сохраняется, если настройка уже была начата.
    var step by remember { mutableIntStateOf(0) }
    fun update(v: Config) { s=v; saveConfig(prefs,v,"draft_") }
    val first=parseDate(s.arrival)
    val last=parseDate(s.departure)
    val datesOk=first!=null && last!=null && !last.isBefore(first) && s.rotation in rotations
    val canNext=when(step) {
        0 -> s.name.isNotBlank()
        1 -> datesOk
        else -> true
    }
    Column(Modifier.fillMaxSize().background(bg).padding(18.dp)) {
        Text("Вахтовичок",fontSize=30.sp,color=Color.White,fontWeight=FontWeight.Black)
        Text("БЫСТРАЯ НАСТРОЙКА",color=mint,fontSize=11.sp,letterSpacing=2.sp)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress={ (step+1)/4f },modifier=Modifier.fillMaxWidth(),color=mint)
        Text("Шаг ${step+1} из 4 · Обычно 1–2 минуты",color=muted,fontSize=12.sp)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(step) {
                0 -> {
                    Heading("Привет!", "Как к тебе обращаться? Название предприятия указывать не нужно.")
                    Field("Имя или никнейм",s.name) { update(s.copy(name=it)) }
                    Text("Приложение подходит работникам любых предприятий. Все данные можно изменить позже.",color=muted,fontSize=13.sp)
                }
                1 -> {
                    Heading("Твой график", "Выбери график и укажи даты ближайшей вахты.")
                    RotationSelect(s.rotation) { update(s.copy(rotation=it)) }
                    DateField("Заезд",s.arrival) { update(s.copy(arrival=it)) }
                    DateField("Выезд",s.departure) { update(s.copy(departure=it)) }
                    if(datesOk) Text("Длительность вахты: ${ChronoUnit.DAYS.between(first,last)+1} дн.",color=mint)
                    Text("Если график нестандартный, его можно уточнить в настройках после запуска.",color=muted,fontSize=12.sp)
                }
                2 -> {
                    Heading("Расчёт зарплаты", "Необязательно. Если не знаешь цифры — пропусти и заполни позже.")
                    Field("Ставка, ₽ за час",s.hourly,true) { update(s.copy(hourly=it)) }
                    Field("Часов в смене",s.hours,true) { update(s.copy(hours=it)) }
                    Field("Рабочих дней подряд",s.workDays,true) { update(s.copy(workDays=it)) }
                    Field("Выходных дней подряд",s.restDays,true) { update(s.copy(restDays=it)) }
                    Text("Премии, районные и северные надбавки, питание, МВО и удержания настраиваются отдельно. У разных работодателей правила отличаются.",color=muted,fontSize=12.sp)
                }
                3 -> {
                    Heading("Всё готово!", "Проверь основное. Остальные параметры можно добавить в настройках.")
                    Panel("ТВОИ ДАННЫЕ") {
                        Info("Имя",s.name)
                        Info("График",s.rotation)
                        Info("Заезд",first?.pretty() ?: "—")
                        Info("Выезд",last?.pretty() ?: "—")
                        Info("Ставка",if(s.hourly.isBlank()) "Пока не указана" else "${s.hourly} ₽/час")
                    }
                    Text("Без данных о зарплате приложение покажет календарь, а финансовый прогноз можно будет настроить позднее.",color=muted,fontSize=12.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            if(step>0) OutlinedButton(onClick={step--},modifier=Modifier.weight(1f)) { Text("Назад") }
            if(step==2) OutlinedButton(onClick={step=3},modifier=Modifier.weight(1f)) { Text("Пропустить") }
            Button(onClick={if(step<3) { step++ } else onFinish(s)},enabled=canNext,modifier=Modifier.weight(1f)) {
                Text(if(step==3) "Открыть приложение" else "Далее")
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
        Text(label,color=Color.White)
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
@Composable private fun Dashboard(s: Config,onChange: (Config)->Unit,prefs: SharedPreferences,onLock: ()->Unit,organizerContext: Context) {
    var tab by remember { mutableIntStateOf(0) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { clock=System.currentTimeMillis(); delay(1000) } }
    val now=remember(clock/1000) { LocalDateTime.ofInstant(Instant.ofEpochMilli(clock),ZoneId.systemDefault()) }
    val today=now.toLocalDate()
    val first=parseDate(s.arrival); val last=parseDate(s.departure)
    val valid=first!=null && last!=null && last.isAfter(first) && s.rotation in rotations
    val period=if(valid) periodFor(today,first!!,last!!,s.rotation) else null
    val wholeGross=if(period!=null) grossForRange(s,period.start,period.end,if(period.working) today.minusDays(1) else period.end) else 0.0
    val fractional=if(period?.working==true) {
        val work=(s.workDays.toIntOrNull() ?: 1).coerceIn(1,366)
        val rest=(s.restDays.toIntOrNull() ?: 0).coerceIn(0,366)
        val idx=ChronoUnit.DAYS.between(period.start,today)
        val factor=(num(s.coefficient).takeIf { it>0 } ?: 1.0)+num(s.north)/100+num(s.premium)/100
        val dayGross=if(idx%(work+rest)<work) num(s.hours)*num(s.hourly)*factor else 0.0
        (dayGross+num(s.allowance))*now.toLocalTime().toSecondOfDay()/86400.0
    } else 0.0
    val earnings=wholeGross+fractional
    var payments by remember { mutableStateOf(loadPayments(prefs.getString("payments","[]") ?: "[]")) }
    var events by remember { mutableStateOf(loadEvents(prefs.getString("calendar_events","[]") ?: "[]")) }
    fun savePayments(v: List<Payment>) { payments=v; prefs.edit().putString("payments",encodePayments(v)).commit() }
    fun saveEvents(v: List<CalendarEvent>) { events=v; prefs.edit().putString("calendar_events",encodeEvents(v)).commit() }
    Column(Modifier.fillMaxSize().background(bg)) {
        // Горизонтальный свайп переключает пять основных разделов.
        // Срабатывает только после заметного движения, чтобы не мешать прокрутке вниз.
        var swipeDistance by remember { mutableFloatStateOf(0f) }
        Column(Modifier.weight(1f).fillMaxWidth()
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
            .then(if(tab==3) Modifier else Modifier.verticalScroll(rememberScrollState())).padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Вахтовичок",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Black)
            Text("Привет, ${s.name}!",color=muted,fontSize=17.sp)
            when(tab) {
                0 -> {
                    Panel("МОЙ ГРАФИК") {
                        if(period==null) Text("Укажи даты заезда и выезда",color=gold)
                        else {
                            Text(if(period.working) "НА ВАХТЕ" else "НА МЕЖВАХТЕ",color=if(period.working) mint else blue,fontWeight=FontWeight.Bold)
                            Text(if(period.working) "До выезда: ${period.remaining} дн." else "До заезда: ${period.remaining} дн.",color=Color.White,fontSize=23.sp)
                            RunningShiftProgress(period.progress)
                            Info("Прогресс","${(period.progress*100).toInt()}% · ${period.total} дней")
                            Info("График",s.rotation); Info("Заезд",period.start.pretty())
                            Info("Выезд",period.end.pretty()); Info("Следующий заезд",period.next.pretty())
                        }
                    }
                    Panel("ЖИВОЙ СЧЁТЧИК") {
                        Text(rub(earnings),color=mint,fontSize=28.sp,fontWeight=FontWeight.Bold)
                        Text("Приблизительное начисление до удержаний",color=muted)
                        Text("Часы смены условно распределены по суткам. Дорога и межвахтовые начисления не включены.",color=muted,fontSize=12.sp)
                    }
                    Panel("БЛИЖАЙШИЕ ВЫПЛАТЫ") {
                        val dates=listOfNotNull(nextPayDate(today,s.salaryDay)?.let { "Зарплата" to it },nextPayDate(today,s.advanceDay)?.let { "Аванс" to it }).sortedBy { it.second }
                        if(dates.isEmpty()) Text("Назначь дни выплат в настройках",color=muted)
                        dates.forEach { (type,date) ->
                            val actual=payments.filter { it.date==date.toString() && it.type==type }.sumOf { it.amount }
                            Text("$type · ${date.pretty()}${if(actual>0) " · ${rub(actual)}" else ""}",color=Color.White)
                        }
                    }
                    Button(onClick={tab=1}) { Text("Открыть календарь") }
                }
                1 -> ShiftCalendar(today,first,last,s,onChange,payments,events,::saveEvents)
                2 -> Finance(s,earnings,payments,::savePayments,organizerContext)
                3 -> OrganizerScreen(organizerContext)
                4 -> Settings(s,onChange,onLock)
            }
        }
        NavigationBar(containerColor=panel) {
            listOf("Главная","Календарь","Финансы","Мои дела","Настройки").forEachIndexed { i,label ->
                NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("⌂","▦","₽","☑","⚙")[i])},label={Text(label,fontSize=9.sp,maxLines=1)})
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

@Composable private fun Finance(s: Config,earnings: Double,payments: List<Payment>,onChange: (List<Payment>)->Unit,context: Context) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Зарплата") }
    val debts = remember(context) { readVahtaDebts(context) }
    val today = LocalDate.now()
    val avgMonthly = remember(s, today.year, today.monthValue) { monthlyIncomeEstimate(s, today) }
    val monthPayments = debts.sumOf { it.monthly }
    val totalDebt = debts.sumOf { it.balance }
    val monthFree = avgMonthly?.minus(monthPayments)
    val fullFree = avgMonthly?.minus(totalDebt)
    Panel("МОЙ ФИНАНСОВЫЙ ПРОГНОЗ") {
        Text("СРЕДНИЙ ДОХОД ЗА МЕСЯЦ", color=muted, fontSize=12.sp)
        Text(avgMonthly?.let { rub(it) } ?: "Недостаточно данных", color=mint, fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text("Оценка после НДФЛ по графику вахт за 12 месяцев. Не фактическая выплата.",color=muted,fontSize=12.sp)
        HorizontalDivider()
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
    Panel("КОМУ И СКОЛЬКО ОТДАТЬ") {
        if(debts.isEmpty()) Text("Активных долгов нет. Добавь их в «Мои дела» → «Долги».",color=muted)
        debts.forEach { debt ->
            HorizontalDivider()
            Text(debt.name,color=Color.White,fontWeight=FontWeight.SemiBold)
            Info("Остаток долга",rub(debt.balance))
            Info("Платёж за месяц",rub(debt.monthly))
            if(debt.dueDay in 1..31) Info("Число платежа",debt.dueDay.toString())
            if(debt.monthly == 0.0) Text("Месячный платёж не указан",color=gold,fontSize=12.sp)
        }
        Text("Редактировать долги: «Мои дела» → «Долги». Изменения появятся здесь при повторном открытии вкладки.",color=muted,fontSize=12.sp)
    }
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
                RadioButton(selected=type==option,onClick={type=option}); Text(option,color=Color.White)
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
        val wage=wageForDays(s,counts.first,counts.second)
        Info("Расчётный месяц",selectedMonth.toString())
        Info("Начислено (прогноз)",rub(wage.gross))
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
            Text(rub(p.amount),color=Color.White,fontSize=20.sp)
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
        Field("Из декабрьского остатка оставить на первый отдых следующего года",reserve,true) { reserve=it }
        val earned=kotlin.math.floor((num(actual)-num(norm)-num(excluded)).coerceAtLeast(0.0)/8).toInt()
        Info("Начисляется дней МВО за выбранный месяц",earned.toString())
        Button(onClick={
            val e=MvoEntry(month,num(actual),num(norm),num(excluded),num(used).toInt(),num(vacation).toInt(),num(reserve).toInt())
            val next=entries.filterNot { it.month==month }+e
            saveMvoEntries(context,next); entries=next
        },enabled=ym!=null && actual.isNotBlank() && norm.isNotBlank(),modifier=Modifier.fillMaxWidth()) { Text("Сохранить месяц МВО") }
        if(balance!=null) {
            HorizontalDivider()
            Info("Начислено и перенесено дней за ${ym?.year}",balance.accrued.toString())
            Info("Использовано дней",balance.used.toString())
            Info("Остаток дней",balance.available.toString())
            Info("Оценка декабрьской выплаты",rub(balance.decemberPayout*8.0*(num(s.intershiftRate).takeIf { it>0 } ?: num(s.hourly))))
            Info("Перенос на первый отдых следующего года",balance.nextYearCarry.toString()+" дн.")
        }
        Text("Остаток считается по внесённым месяцам. Дни ежегодного отпуска фиксируются для контроля, но не вычитаются как использованные МВО. Декабрьская выплата предварительная; порядок переноса уточняется по табелю и графику отдыха.",color=muted,fontSize=12.sp)
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
        Text("🏃",fontSize=25.sp,modifier=Modifier.offset(x=distance*p,y=(-bounce).dp))
        Text("⚑",fontSize=16.sp,color=mint,modifier=Modifier.align(Alignment.TopEnd))
    }
}

@Composable private fun Settings(s: Config,onChange: (Config)->Unit,onLock: ()->Unit) {
    Panel("ЛИЧНЫЕ ДАННЫЕ") { Field("Имя",s.name) { onChange(s.copy(name=it)) } }
    Panel("ГРАФИК ВАХТ") {
        RotationSelect(s.rotation) { onChange(s.copy(rotation=it)) }
        DateField("Дата первого заезда",s.arrival) { onChange(s.copy(arrival=it)) }
        DateField("Дата первого выезда",s.departure) { onChange(s.copy(departure=it)) }
        Field("Рабочих дней подряд",s.workDays,true) { onChange(s.copy(workDays=it)) }
        Field("Выходных дней подряд",s.restDays,true) { onChange(s.copy(restDays=it)) }
        val a=parseDate(s.arrival); val b=parseDate(s.departure)
        if(a!=null && b!=null && !b.isBefore(a)) Text("Фактически: ${ChronoUnit.DAYS.between(a,b)+1} дней",color=mint)
    }
    Panel("ОФОРМЛЕНИЕ") {
        val context = LocalContext.current
        Text("Цветовая тема приложения",color=Color.White)
        listOf("Классическая", "Лаванда").forEach { option ->
            val active=(option=="Лаванда")==lavenderMode
            Row(Modifier.fillMaxWidth().clickable {
                lavenderMode=option=="Лаванда"
                context.getSharedPreferences("vahta_appearance",Context.MODE_PRIVATE).edit().putBoolean("lavender",lavenderMode).apply()
            },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=active,onClick={
                    lavenderMode=option=="Лаванда"
                    context.getSharedPreferences("vahta_appearance",Context.MODE_PRIVATE).edit().putBoolean("lavender",lavenderMode).apply()
                })
                Text(option,color=Color.White)
            }
        }
    }
    Panel("ОПЛАТА ТРУДА") {
        Field("ЧТС, ₽/час",s.hourly,true) { onChange(s.copy(hourly=it)) }
        Field("Часов в смене",s.hours,true) { onChange(s.copy(hours=it)) }
        Field("Районный коэффициент",s.coefficient,true) { onChange(s.copy(coefficient=it)) }
        Field("Северная надбавка, %",s.north,true) { onChange(s.copy(north=it)) }
        Field("Премия, %",s.premium,true) { onChange(s.copy(premium=it)) }
        Field("Вахтовая надбавка, ₽/день",s.allowance,true) { onChange(s.copy(allowance=it)) }
        Field("Оплачиваемые часы дороги",s.travelHours,true) { onChange(s.copy(travelHours=it)) }
    }
    Panel("ИНДИВИДУАЛЬНАЯ НАДБАВКА") {
        Field("Название надбавки",s.personalName) { onChange(s.copy(personalName=it)) }
        Field("Размер надбавки",s.personalValue,true) { onChange(s.copy(personalValue=it)) }
        Text("Способ начисления",color=muted)
        listOf("Процент","Сумма, ₽").forEach { mode ->
            Row(Modifier.clickable { onChange(s.copy(personalMode=mode)) },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=s.personalMode==mode,onClick={onChange(s.copy(personalMode=mode))})
                Text(mode,color=Color.White)
            }
        }
        if(s.personalMode=="Процент") {
            Text("База начисления",color=muted)
            listOf("Тарифный заработок","Тариф + доплаты").forEach { base ->
                Row(Modifier.clickable { onChange(s.copy(personalBase=base)) },verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(selected=s.personalBase==base,onClick={onChange(s.copy(personalBase=base))})
                    Text(base,color=Color.White)
                }
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(s.personalCoefficient,{onChange(s.copy(personalCoefficient=it))})
            Text("Применять районный коэффициент и северную надбавку",color=Color.White,fontSize=12.sp)
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(s.personalProrate,{onChange(s.copy(personalProrate=it))})
            Text("Пропорционально периоду (для фиксированной суммы)",color=Color.White,fontSize=12.sp)
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
        Field("Праздничные часы с двойной оплатой",s.holidayHours,true) { onChange(s.copy(holidayHours=it)) }
        Text("Не включай праздничные часы повторно в обычные часы: иначе получится двойной учёт.",color=muted,fontSize=12.sp)
    }
    Panel("ДОПОЛНИТЕЛЬНЫЕ НАЧИСЛЕНИЯ (НЕОБЯЗАТЕЛЬНО)") {
        Field("Оплачиваемые часы межвахты",s.intershiftHours,true) { onChange(s.copy(intershiftHours=it)) }
        Field("Ставка межвахты, ₽/час",s.intershiftRate,true) { onChange(s.copy(intershiftRate=it)) }
        Field("Дней МВО к оплате за месяц (ручной режим)",s.shiftRestDays,true) { onChange(s.copy(shiftRestDays=it)) }
        Text("АВТОМАТИЧЕСКИЙ РАСЧЁТ МВО ПО ТАБЕЛЮ",color=Color.White,fontWeight=FontWeight.Bold)
        Field("Фактические часы по табелю",s.mvoActualHours,true) { onChange(s.copy(mvoActualHours=it)) }
        Field("Норма часов по производственному календарю",s.mvoNormHours,true) { onChange(s.copy(mvoNormHours=it)) }
        Field("Часы, исключаемые из МВО (праздники / двойная оплата)",s.mvoExcludedHours,true) { onChange(s.copy(mvoExcludedHours=it)) }
        val calculatedMvo = mvoDays(s)
        if (calculatedMvo != null) Text("Рассчитано дней МВО: $calculatedMvo. Оплата: ${rub(calculatedMvo * 8.0 * (num(s.intershiftRate).takeIf { it > 0 } ?: num(s.hourly)))}",color=Color.White)
        Text("При заполнении фактических часов и нормы применяется автоматический расчёт; ручные дни МВО тогда не используются.",color=muted,fontSize=12.sp)
        Field("Ставка оплаты дороги, ₽/час",s.travelRate,true) { onChange(s.copy(travelRate=it)) }
        Text("Расчёт МВО по формуле из презентации Ареал. Вводи часы за один расчётный период; перенос остатков между периодами пока не автоматизирован.",color=muted,fontSize=12.sp)
    }
    Panel("УДЕРЖАНИЯ") {
        Field("НДФЛ, %",s.tax,true) { onChange(s.copy(tax=it)) }
        Field("Питание, ₽/день (или всего за месяц)",s.meals,true) { onChange(s.copy(meals=it)) }
        Field("Дней питания за месяц (0 = сумма за месяц)",s.mealDays,true) { onChange(s.copy(mealDays=it)) }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=s.allowanceTaxFree,onCheckedChange={onChange(s.copy(allowanceTaxFree=it))})
            Text("Вахтовая надбавка не облагается НДФЛ",color=Color.White,fontSize=13.sp)
        }
        Field("Исполнительные удержания, ₽",s.enforcement,true) { onChange(s.copy(enforcement=it)) }
        Field("Прочие удержания, ₽",s.otherDeductions,true) { onChange(s.copy(otherDeductions=it)) }
    }
    Panel("ВЫПЛАТЫ") {
        DayPicker("День зарплаты",s.salaryDay) { onChange(s.copy(salaryDay=it)) }
        DayPicker("День аванса",s.advanceDay) { onChange(s.copy(advanceDay=it)) }
        listOf("По отработанным часам","Фиксированная сумма","Другой порядок").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { onChange(s.copy(advanceMethod=option)) },verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=s.advanceMethod==option,onClick={onChange(s.copy(advanceMethod=option))}); Text(option,color=Color.White)
            }
        }
        if(s.advanceMethod=="Фиксированная сумма") Field("Аванс, ₽",s.fixedAdvance,true) { onChange(s.copy(fixedAdvance=it)) }
    }
    Panel("НАПОМИНАНИЯ") {
        Text("Включить напоминания?",color=Color.White)
        Row(verticalAlignment=Alignment.CenterVertically) {
            RadioButton(selected=s.reminders=="yes",onClick={onChange(s.copy(reminders="yes"))}); Text("Да",color=Color.White)
            RadioButton(selected=s.reminders=="no",onClick={onChange(s.copy(reminders="no"))}); Text("Нет",color=Color.White)
        }
        if(s.reminders=="yes") ReminderOptions(s,onChange)
        Text("Настройки сохранены. Системные уведомления вне приложения пока не подключены.",color=muted,fontSize=12.sp)
    }
    Panel("БЕЗОПАСНОСТЬ") { OutlinedButton(onClick=onLock,modifier=Modifier.fillMaxWidth()) { Text("Заблокировать приложение") } }
}
@Composable private fun ShiftCalendar(today: LocalDate,first: LocalDate?,last: LocalDate?,s: Config,onChange: (Config)->Unit,
    payments: List<Payment>,events: List<CalendarEvent>,onEvents: (List<CalendarEvent>)->Unit) {
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var eventTitle by remember { mutableStateOf("") }
    val valid=first!=null && last!=null && last.isAfter(first) && s.rotation in rotations
    Panel("КАЛЕНДАРЬ ВАХТ И ВЫПЛАТ") {
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
                        val color=when { departure -> gold; arrival -> Color(0xFF278E7A); p?.working==true -> Color(0xFF204F4A); else -> Color(0xFF233F65) }
                        Box(Modifier.weight(1f).height(58.dp).background(color,RoundedCornerShape(9.dp))
                            .then(if(date==selected) Modifier.border(2.dp,Color.White,RoundedCornerShape(9.dp)) else Modifier)
                            .clickable { selected=date },contentAlignment=Alignment.Center) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                Text("$day",color=if(departure) bg else Color.White,fontWeight=FontWeight.Bold)
                                Text(buildString { if(salary) append("₽"); if(advance) append("₳"); if(hasEvent) append("•")
                                    if(arrival) append("↑"); if(departure) append("⌂") },fontSize=11.sp,lineHeight=13.sp,color=if(departure) bg else mint,maxLines=1)
                            }
                        }
                    } else Spacer(Modifier.weight(1f).height(58.dp))
                }
            }
        }
        Text("₽ зарплата · ₳ аванс · ↑ заезд · ⌂ выезд · • событие",color=muted,fontSize=12.sp)
        HorizontalDivider()
        Text(selected.pretty(),color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
        if(valid) {
            val p=periodFor(selected,first!!,last!!,s.rotation)
            Text(when { selected==p.start -> "↑ День заезда"; selected==p.end -> "⌂ День выезда домой"
                p.working -> "На вахте · до выезда ${p.remaining} дн."; else -> "Межвахта · до заезда ${p.remaining} дн." },color=mint)
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
        if(dayPayments.isNotEmpty()) Text("Всего поступило: ${rub(dayPayments.sumOf { it.amount })}",color=Color.White)
        HorizontalDivider()
        Text("Личные события",color=Color.White,fontWeight=FontWeight.Bold)
        events.filter { it.date==selected.toString() }.forEach { event ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("• ${event.title}",color=Color.White,modifier=Modifier.weight(1f))
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
        Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",ru)).replaceFirstChar { it.uppercase() },color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold)
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
    Text(title,color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
    Text(description,color=muted,fontSize=14.sp)
}
@Composable private fun Panel(title: String,content: @Composable ColumnScope.()->Unit) {
    Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=panel)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(title,color=muted,fontSize=12.sp,fontWeight=FontWeight.Bold)
            content()
        }
    }
}
@Composable private fun Info(label: String,value: String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,color=muted,fontSize=13.sp)
        Text(value,color=Color.White,fontSize=13.sp)
    }
}
@Composable private fun Field(label: String,value: String,numeric: Boolean=false,onChange: (String)->Unit) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=if(numeric) KeyboardType.Decimal else KeyboardType.Text),modifier=Modifier.fillMaxWidth())
}
