
package com.vahta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VahtaApp() }
    }
}

@Composable
fun VahtaApp() {
    val bg = Color(0xFF101C2C)
    val card = Color(0xFF20344B)
    val green = Color(0xFF58DDB2)

    val startDate = LocalDate.of(2026, 11, 1)
    val workDays = 30
    val restDays = 30

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val today = LocalDate.now()
    val elapsed = ChronoUnit.DAYS.between(startDate, today)
    val position = Math.floorMod(elapsed, (workDays + restDays).toLong()).toInt()
    val remaining = if (position < workDays) workDays - position else 0

    val hourlyRate = 600.0
    val workedHours = 11.0
    val earned = (elapsed.coerceAtLeast(0) * workedHours * hourlyRate)

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = bg,
            surface = card,
            primary = green,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("ВАХТА", fontSize = 32.sp, color = Color.White)
            Text("Работа. Деньги. Жизнь.", color = Color.LightGray)

            Card(
                colors = CardDefaults.cardColors(containerColor = card),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("ДО МЕЖВАХТЫ", color = Color.LightGray)
                    Text("$remaining дней", fontSize = 34.sp, color = green)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = card),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("ЗАРАБОТАНО ЗА ВАХТУ", color = Color.LightGray)
                    Text("%.2f ₽".format(earned), fontSize = 30.sp, color = green)
                }
            }

            Text("Календарь • Финансы • Заметки", color = Color.White)
        }
    }
}
