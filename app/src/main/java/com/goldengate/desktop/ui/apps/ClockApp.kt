package com.goldengate.desktop.ui.apps

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.ui.components.DividerLine
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.components.SegmentedControl
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ClockApp(state: LauncherState, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(state.command) {
        val command = state.command ?: return@LaunchedEffect
        if (command.app != BuiltinApp.CLOCK) return@LaunchedEffect
        tab = when (command.action) {
            "stopwatch" -> 1
            "timer" -> 2
            else -> 0
        }
    }

    Column(modifier.padding(16.dp)) {
        SegmentedControl(
            options = listOf("World Clock", "Stopwatch", "Timer"),
            selected = tab,
            onSelect = { tab = it }
        )
        Spacer(Modifier.height(16.dp))
        when (tab) {
            0 -> WorldClock()
            1 -> Stopwatch()
            else -> TimerPanel()
        }
    }
}

@Composable
private fun WorldClock() {
    val style = LocalGlassStyle.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val zones = listOf(
        "Local" to ZoneId.systemDefault(),
        "Cupertino" to ZoneId.of("America/Los_Angeles"),
        "New York" to ZoneId.of("America/New_York"),
        "London" to ZoneId.of("Europe/London"),
        "Berlin" to ZoneId.of("Europe/Berlin"),
        "Tokyo" to ZoneId.of("Asia/Tokyo"),
        "Sydney" to ZoneId.of("Australia/Sydney")
    )
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text("City", style = MaterialTheme.typography.labelSmall, color = style.tertiary, modifier = Modifier.weight(1f))
            Text("Offset", style = MaterialTheme.typography.labelSmall, color = style.tertiary, modifier = Modifier.width(68.dp))
            Text("Time", style = MaterialTheme.typography.labelSmall, color = style.tertiary, modifier = Modifier.width(80.dp))
        }
        DividerLine()
        LazyColumn {
            items(zones, key = { it.first }) { (label, zone) ->
                val zoned = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone)
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
                    Text(
                        zoned.format(DateTimeFormatter.ofPattern("XXX")),
                        style = MaterialTheme.typography.labelMedium,
                        color = style.secondary,
                        modifier = Modifier.width(68.dp)
                    )
                    Text(
                        zoned.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        style = MaterialTheme.typography.bodyLarge,
                        color = style.text,
                        modifier = Modifier.width(80.dp)
                    )
                }
                DividerLine()
            }
        }
    }
}

@Composable
private fun Stopwatch() {
    val style = LocalGlassStyle.current
    var running by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0L) }
    var startedAt by remember { mutableStateOf(0L) }
    var laps by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(running) {
        if (running) {
            startedAt = System.currentTimeMillis() - elapsed
            while (running) {
                elapsed = System.currentTimeMillis() - startedAt
                delay(47)
            }
        }
    }

    fun label(ms: Long): String {
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        val hundredths = (ms % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Text(
            label(elapsed),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
            color = style.text,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(if (running) "Stop" else "Start", prominent = true) { running = !running }
            PillButton("Lap") {
                if (running) laps = listOf(elapsed) + laps
            }
            PillButton("Reset") {
                running = false
                elapsed = 0
                laps = emptyList()
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(laps) { lap ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text("Lap ${laps.indexOf(lap) + 1}", color = style.secondary, modifier = Modifier.weight(1f))
                    Text(label(lap), color = style.text)
                }
                DividerLine()
            }
        }
    }
}

@Composable
private fun TimerPanel() {
    val style = LocalGlassStyle.current
    var totalSeconds by remember { mutableStateOf(300) }
    var remaining by remember { mutableStateOf(300) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(running, remaining) {
        if (running && remaining > 0) {
            delay(1000)
            remaining -= 1
            if (remaining == 0) {
                running = false
                finished = true
                runCatching {
                    val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1200)
                    kotlin.concurrent.thread {
                        Thread.sleep(1600)
                        runCatching { tone.release() }
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Text(
            String.format(Locale.US, "%02d:%02d", remaining / 60, remaining % 60),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
            color = style.text,
            fontWeight = FontWeight.Light
        )
        Text(
            if (finished) "Timer finished" else if (running) "Running" else "Ready",
            style = MaterialTheme.typography.labelMedium,
            color = if (finished) MaterialTheme.colorScheme.primary else style.secondary
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 300, 600, 1500).forEach { seconds ->
                PillButton("${seconds / 60} min", prominent = totalSeconds == seconds) {
                    totalSeconds = seconds
                    remaining = seconds
                    running = false
                    finished = false
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(if (running) "Pause" else "Start", prominent = true) {
                if (remaining > 0) {
                    running = !running
                    finished = false
                }
            }
            PillButton("Reset") {
                running = false
                finished = false
                remaining = totalSeconds
            }
        }
        Spacer(Modifier.weight(1.4f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(style.field)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((remaining.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Suppress("unused")
private fun unusedClickable() = Modifier.clickable {}

@Suppress("unused")
private fun unusedPadding() = Modifier.size(1.dp)
