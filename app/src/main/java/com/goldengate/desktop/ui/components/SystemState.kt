package com.goldengate.desktop.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.goldengate.desktop.core.LauncherState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ConnectBatteryState(state: LauncherState) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (level >= 0) state.batteryLevel = (level * 100f / scale.coerceAtLeast(1)).toInt()
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                state.charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
}

private val timeShort = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val timeLong = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
private val dateShort = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val dateLong = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())

fun LocalDateTime.shortTime(seconds: Boolean = false): String =
    format(if (seconds) timeLong else timeShort)

fun LocalDateTime.shortDate(): String = format(dateShort)

fun LocalDateTime.longDate(): String = format(dateLong)

fun batteryGlyph(level: Int, charging: Boolean): String = when {
    charging -> "⚡"
    level >= 95 -> "▮"
    level >= 70 -> "▮"
    level >= 40 -> "▯"
    else -> "▯"
}
