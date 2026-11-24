package com.goldengate.desktop.ui.overlays

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.util.Intents

@Composable
fun ControlCenterOverlay(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var brightness by remember { mutableStateOf(82f) }

    OverlayScrim(onDismiss = { state.dismissOverlay() }, alignment = Alignment.TopEnd) {
        GlassPanel(
            modifier = Modifier
                .padding(top = 46.dp, end = 14.dp)
                .width(372.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 40.dp
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Control Center", style = MaterialTheme.typography.titleSmall, color = style.text)
                    Spacer(Modifier.weight(1f))
                    Text("Android limits apply", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                }
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControlTile(
                        icon = Icons.Rounded.Wifi,
                        title = "Wi-Fi",
                        subtitle = "System panel",
                        active = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (Build.VERSION.SDK_INT >= 29) {
                            Intents.openPanel(context, Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                        } else {
                            Intents.openSystemSettings(context, Settings.ACTION_WIFI_SETTINGS)
                        }
                    }
                    ControlTile(
                        icon = Icons.Rounded.Bluetooth,
                        title = "Bluetooth",
                        subtitle = "System panel",
                        active = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Intents.openSystemSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControlTile(
                        icon = Icons.Rounded.AirplanemodeActive,
                        title = "Airplane",
                        subtitle = "System panel",
                        active = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Intents.openSystemSettings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                    }
                    ControlTile(
                        icon = Icons.Rounded.DoNotDisturbOn,
                        title = "Focus",
                        subtitle = "Do Not Disturb",
                        active = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Intents.openSystemSettings(context, "android.settings.ZEN_MODE_SETTINGS")
                    }
                }

                Spacer(Modifier.height(12.dp))
                SliderBlock(
                    icon = Icons.Rounded.Brightness6,
                    title = "Display brightness",
                    value = brightness,
                    valueRange = 2f..100f,
                    onChange = {
                        brightness = it
                        shell.setAppBrightness(it.toInt())
                    }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        if (shell.hasWriteSettings()) "Tap “System” to store it device-wide" else "Applies while this launcher is on screen",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(style.field)
                            .clickable {
                                if (shell.hasWriteSettings()) {
                                    shell.setSystemBrightness(brightness.toInt())
                                    state.notify("System brightness set")
                                } else {
                                    shell.openWriteSettings()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("System", style = MaterialTheme.typography.labelSmall, color = style.text)
                    }
                }

                Spacer(Modifier.height(6.dp))
                SliderBlock(
                    icon = Icons.Rounded.VolumeUp,
                    title = "Media volume",
                    value = volume.toFloat(),
                    valueRange = 0f..maxVolume.toFloat(),
                    onChange = {
                        volume = it.toInt()
                        runCatching {
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppearanceTile(
                        icon = Icons.Rounded.LightMode,
                        label = "Light",
                        active = prefs.appearance == "light",
                        modifier = Modifier.weight(1f)
                    ) { prefs.setAppearance("light") }
                    AppearanceTile(
                        icon = Icons.Rounded.DarkMode,
                        label = "Dark",
                        active = prefs.appearance == "dark",
                        modifier = Modifier.weight(1f)
                    ) { prefs.setAppearance("dark") }
                    AppearanceTile(
                        icon = Icons.Rounded.ScreenRotation,
                        label = "Rotation",
                        active = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Intents.openSystemSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Wi-Fi, Bluetooth, Airplane mode, Focus and rotation lock are protected Android system toggles. A launcher cannot flip them silently, so each one opens the official system panel where you can change it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary
                )
            }
        }
    }
}

@Composable
private fun ControlTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else style.field)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) Color.White.copy(alpha = 0.25f) else style.hairline),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = style.text, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = style.text)
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = style.tertiary)
        }
    }
}

@Composable
private fun SliderBlock(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    val style = LocalGlassStyle.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = style.secondary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = style.text)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun AppearanceTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else style.field)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = style.text, modifier = Modifier.size(17.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = style.text)
    }
}
