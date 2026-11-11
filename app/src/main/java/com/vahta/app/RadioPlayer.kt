package com.vahta.app

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val RECORD_STREAM = "https://radiorecord.hostingradio.ru/rr_main96.aacp"

/** Lightweight streaming player; no external player or browser is required. */
@Composable
internal fun RadioRecordCard() {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var status by remember { mutableStateOf("Остановлено") }
    var playing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.7f) }

    fun stop() {
        val old = player
        player = null
        playing = false
        loading = false
        status = "Остановлено"
        runCatching { old?.reset(); old?.release() }
    }
    fun start() {
        stop()
        status = "Подключение к эфиру…"
        loading = true
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            mp.setVolume(volume, volume)
            mp.setOnPreparedListener { ready ->
                if (player === ready) {
                    runCatching { ready.start() }.onSuccess {
                        playing = true
                        loading = false
                        status = "Прямой эфир"
                    }.onFailure {
                        stop(); status = "Не удалось запустить эфир"
                    }
                }
            }
            mp.setOnErrorListener { failed, _, _ ->
                if (player === failed) {
                    stop()
                    status = "Ошибка подключения. Проверьте интернет"
                }
                true
            }
            mp.setDataSource(RECORD_STREAM)
            mp.prepareAsync()
        } catch (_: Exception) {
            stop()
            status = "Не удалось подключиться к радио"
        }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { player?.release() }; player = null }
    }
    val dark = !lightMode
    val surface = if (dark) Color(0xFF2B4056) else Color(0xFFEDE8FA)
    val accent = if (dark) Color(0xFFA5F1D7) else Color(0xFF56428A)
    val foreground = if (dark) Color.White else Color(0xFF243249)
    Column(
        Modifier.fillMaxWidth().background(surface, RoundedCornerShape(22.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("♫  RADIO RECORD", color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(status, color = foreground, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { if (!loading) { if (playing) stop() else start() } }, enabled = !loading) {
                Text(if (loading) "Подключаем…" else if (playing) "■ Стоп" else "▶ Слушать")
            }
            if (playing) Text("● LIVE", color = accent, modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
        }
        Text("Громкость", color = foreground, fontSize = 12.sp)
        Slider(value = volume, onValueChange = { v ->
            volume = v
            runCatching { player?.setVolume(v, v) }
        }, valueRange = 0f..1f)
        Text("Для эфира нужен интернет", color = foreground.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}
