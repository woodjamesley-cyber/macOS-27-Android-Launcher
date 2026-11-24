package com.goldengate.desktop.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.components.SegmentedControl
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.ui.wallpaper.WallpaperPresets
import com.goldengate.desktop.ui.wallpaper.WallpaperPreview

@Composable
fun SetupOverlay(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    var step by remember { mutableStateOf(0) }
    val configuration = LocalConfiguration.current
    val maxWidth = if (configuration.screenWidthDp < 620) 360.dp else 520.dp
    val isDefault = shell.isDefaultHome()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier.width(maxWidth).heightIn(max = 640.dp),
            shape = RoundedCornerShape(22.dp),
            elevation = 44.dp
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFFFFE0A8), Color(0xFFE59A5B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GG", style = MaterialTheme.typography.labelLarge, color = Color(0xFF3A2410))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Golden Gate Desktop",
                            style = MaterialTheme.typography.titleMedium,
                            color = style.text,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Unofficial macOS 27 inspired launcher",
                            style = MaterialTheme.typography.labelSmall,
                            color = style.tertiary
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(if (index <= step) MaterialTheme.colorScheme.primary else style.hairline)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (step) {
                        0 -> WelcomeStep(isDefault, prefs, shell)
                        1 -> PermissionStep(prefs, shell, state)
                        else -> AppearanceStep(prefs)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Step ${step + 1} of 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary
                    )
                    Spacer(Modifier.weight(1f))
                    if (step > 0) {
                        PillButton("Back") { step-- }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (step < 2) {
                        PillButton("Continue", prominent = true) { step++ }
                    } else {
                        PillButton("Finish", prominent = true) {
                            prefs.setSetupComplete(true)
                            state.overlay = com.goldengate.desktop.core.Overlay.NONE
                            state.notify("Your desktop is ready")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        "Skip setup for now",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary,
                        modifier = Modifier
                            .clickable {
                                prefs.setSetupComplete(true)
                                state.overlay = com.goldengate.desktop.core.Overlay.NONE
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(isDefault: Boolean, prefs: Prefs, shell: com.goldengate.desktop.core.Shell) {
    val style = LocalGlassStyle.current
    Text("Replace One UI Home", style = MaterialTheme.typography.headlineSmall, color = style.text)
    Spacer(Modifier.height(6.dp))
    Text(
        "Golden Gate Desktop is a real Android Home app. Set it once and the Home button, Home gesture and boot-to-home all lead here.",
        style = MaterialTheme.typography.bodySmall,
        color = style.secondary
    )
    Spacer(Modifier.height(14.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(style.field)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isDefault) Icons.Rounded.Check else Icons.Rounded.Home,
            contentDescription = null,
            tint = if (isDefault) Color(0xFF28C840) else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isDefault) "Already the default Home app" else "Not the default Home app yet",
                style = MaterialTheme.typography.bodyMedium,
                color = style.text
            )
            Text(
                if (isDefault) "Press Home any time to return." else "Android will ask you to confirm.",
                style = MaterialTheme.typography.labelSmall,
                color = style.tertiary
            )
        }
        if (!isDefault) {
            PillButton("Set default", prominent = true) { shell.requestHomeRole() }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "On Samsung One UI you can also go to Settings → Apps → Choose default apps → Home app and pick Golden Gate Desktop.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
    Spacer(Modifier.height(12.dp))
    Text("What you get", style = MaterialTheme.typography.labelMedium, color = style.text)
    listOf(
        "A Liquid Glass desktop with widgets, icons and a magnifying Dock",
        "Draggable windows for Finder, Notes, Calculator, Calendar, Clock, Weather and Activity Monitor",
        "Spotlight search, Launchpad, Control Center and an optional Assistant",
        "Full appearance control: wallpaper, glass tint, accent colour, icon sizes"
    ).forEach { line ->
        Row(Modifier.padding(top = 5.dp)) {
            Text("•", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(line, style = MaterialTheme.typography.bodySmall, color = style.secondary)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Android keeps its own lock screen, notification shade, recents and boot animation. No launcher can replace those, and this app never asks for root.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun PermissionStep(
    prefs: Prefs,
    shell: com.goldengate.desktop.core.Shell,
    state: com.goldengate.desktop.core.LauncherState
) {
    val style = LocalGlassStyle.current
    Text("Optional access", style = MaterialTheme.typography.headlineSmall, color = style.text)
    Spacer(Modifier.height(6.dp))
    Text(
        "Everything below is optional. The launcher is fully usable without any of it — these only add calendar events, voice input and system-wide brightness.",
        style = MaterialTheme.typography.bodySmall,
        color = style.secondary
    )
    Spacer(Modifier.height(14.dp))

    PermissionRow(
        title = "Calendar",
        detail = "Show today's events in widgets and the notification panel",
        granted = prefs.calendarEnabled,
        action = "Allow"
    ) { shell.requestCalendarPermission() }

    PermissionRow(
        title = "Microphone",
        detail = "Dictate prompts to the Assistant. Only active while you hold the mic button",
        granted = false,
        action = "Test"
    ) {
        shell.startVoiceInput { text ->
            if (text != null) state.notify("Heard: $text") else state.notify("Voice input not started")
        }
    }

    PermissionRow(
        title = "Modify system settings",
        detail = "Lets the brightness slider also change Android's system brightness",
        granted = shell.hasWriteSettings(),
        action = "Open"
    ) { shell.openWriteSettings() }

    Spacer(Modifier.height(12.dp))
    Text(
        "Files access is requested separately, only when you tap “Choose folder” in Finder. There is no hidden background access.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun PermissionRow(
    title: String,
    detail: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(style.field)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = style.text)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = style.tertiary)
        }
        if (granted) {
            Text("Granted", style = MaterialTheme.typography.labelSmall, color = Color(0xFF28C840))
        } else {
            PillButton(action) { onClick() }
        }
    }
}

@Composable
private fun AppearanceStep(prefs: Prefs) {
    val style = LocalGlassStyle.current
    Text("Make it yours", style = MaterialTheme.typography.headlineSmall, color = style.text)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WallpaperPresets.take(3).forEach { preset ->
            Column(Modifier.weight(1f).clickable { prefs.setWallpaperId(preset.id) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(9.dp))
                        .then(
                            if (prefs.wallpaperId == preset.id) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                            } else Modifier
                        )
                ) {
                    WallpaperPreview(preset, Modifier.fillMaxSize())
                }
                Text(preset.name, style = MaterialTheme.typography.labelSmall, color = style.secondary)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Appearance", style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
        SegmentedControl(
            options = listOf("Auto", "Light", "Dark"),
            selected = when (prefs.appearance) {
                "light" -> 1
                "dark" -> 2
                else -> 0
            },
            onSelect = { prefs.setAppearance(listOf("auto", "light", "dark")[it]) }
        )
    }
    Spacer(Modifier.height(12.dp))
    Text("Liquid Glass", style = MaterialTheme.typography.bodyLarge, color = style.text)
    Text(
        "Slide from ultraclear to fully tinted. You can fine-tune this later in System Settings → Appearance.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
    Slider(
        value = prefs.glassTint,
        onValueChange = { prefs.setGlassTint(it) },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary
        )
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "You can change the wallpaper from your own photos in System Settings → Wallpaper.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}
