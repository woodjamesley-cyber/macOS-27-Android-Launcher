package com.goldengate.desktop.ui.apps

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.data.SystemSnapshot
import com.goldengate.desktop.data.systemSnapshot
import com.goldengate.desktop.ui.components.DividerLine
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.util.Intents

@Composable
fun SystemActivityApp(state: LauncherState, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(systemSnapshot(context)) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) { snapshot = systemSnapshot(context) }

    LaunchedEffect(state.command) {
        val command = state.command ?: return@LaunchedEffect
        if (command.app == BuiltinApp.ACTIVITY && command.action == "refresh") tick++
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("Activity Monitor", style = MaterialTheme.typography.titleLarge, color = style.text)
        Text(
            "Live values read from Android system services on this device.",
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary
        )
        Spacer(Modifier.height(14.dp))

        MetricCard(
            icon = Icons.Rounded.Memory,
            title = "Memory",
            value = "${(snapshot.memUsedMb * 100 / snapshot.memTotalMb.coerceAtLeast(1))}%",
            detail = "${snapshot.memUsedMb} MB used · ${snapshot.memAvailableMb} MB available · ${snapshot.memTotalMb} MB total",
            fraction = snapshot.memUsedMb.toFloat() / snapshot.memTotalMb.coerceAtLeast(1),
            color = Color(0xFF30D158)
        )
        Spacer(Modifier.height(10.dp))
        MetricCard(
            icon = Icons.Rounded.Storage,
            title = "Storage",
            value = "${(snapshot.storageUsedGb * 100 / snapshot.storageTotalGb.coerceAtLeast(1f)).toInt()}%",
            detail = "${"%.1f".format(snapshot.storageUsedGb)} GB used of ${"%.1f".format(snapshot.storageTotalGb)} GB",
            fraction = snapshot.storageUsedGb / snapshot.storageTotalGb.coerceAtLeast(1f),
            color = Color(0xFF0A84FF)
        )
        Spacer(Modifier.height(10.dp))
        MetricCard(
            icon = Icons.Rounded.BatteryChargingFull,
            title = "Battery",
            value = "${state.batteryLevel}%",
            detail = if (state.charging) "Charging" else "Discharging",
            fraction = state.batteryLevel / 100f,
            color = if (state.batteryLevel < 20) Color(0xFFFF5F57) else Color(0xFF28C840)
        )

        Spacer(Modifier.height(16.dp))
        DividerLine()
        Spacer(Modifier.height(12.dp))

        InfoRow("Device", snapshot.deviceModel)
        InfoRow("System", snapshot.androidVersion)
        InfoRow("Uptime", snapshot.uptimeLabel)
        InfoRow("CPU cores", snapshot.cores.toString())
        InfoRow("Display", "${snapshot.screenDpWidth} × ${snapshot.screenDpHeight} dp")
        InfoRow("Density", "${snapshot.density}× (${(snapshot.density * 160).toInt()} dpi)")
        InfoRow("Refresh rate", "${snapshot.refreshRate.toInt()} Hz")
        InfoRow("Tablet layout", if (snapshot.isTablet) "Yes" else "No")
        InfoRow("Low-RAM device", if (snapshot.lowRam) "Yes — animations reduced" else "No")

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Refresh", prominent = true) { tick++ }
            PillButton("Battery settings") {
                Intents.openSystemSettings(context, Intent.ACTION_POWER_USAGE_SUMMARY)
            }
            PillButton("Storage settings") {
                Intents.openSystemSettings(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Full per-process CPU and memory breakdown is restricted by Android for third-party apps. These are the real values a launcher is allowed to read.",
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    detail: String,
    fraction: Float,
    color: Color
) {
    val style = LocalGlassStyle.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(style.field)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.height(16.dp))
            Spacer(Modifier.padding(3.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = style.secondary, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = style.text)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(style.hairline)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(detail, style = MaterialTheme.typography.labelSmall, color = style.tertiary)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val style = LocalGlassStyle.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = style.secondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = style.text)
    }
}

@Suppress("unused")
private val unusedPhone = Icons.Rounded.PhoneAndroid
