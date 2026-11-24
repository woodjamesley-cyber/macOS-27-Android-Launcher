package com.goldengate.desktop.ui.overlays

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
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
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.data.CalEvent
import com.goldengate.desktop.data.CalendarStore
import com.goldengate.desktop.data.systemSnapshot
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.components.longDate
import com.goldengate.desktop.ui.components.shortTime
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.util.Intents
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationCenterOverlay(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<CalEvent>>(emptyList()) }
    val snapshot = remember { systemSnapshot(context) }

    LaunchedEffect(prefs.calendarEnabled) {
        events = if (prefs.calendarEnabled) CalendarStore.todayEvents(context) else emptyList()
    }

    OverlayScrim(onDismiss = { state.dismissOverlay() }, alignment = Alignment.TopEnd) {
        GlassPanel(
            modifier = Modifier.padding(top = 46.dp, end = 14.dp).width(372.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 40.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.now.longDate(),
                            style = MaterialTheme.typography.labelMedium,
                            color = style.secondary
                        )
                        Text(
                            state.now.shortTime(true),
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp),
                            color = style.text
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${state.batteryLevel}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = style.text
                        )
                        Text(
                            if (state.charging) "Charging" else "Battery",
                            style = MaterialTheme.typography.labelSmall,
                            color = style.tertiary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(style.field)
                        .padding(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Today", style = MaterialTheme.typography.labelMedium, color = style.text)
                        Text(
                            if (prefs.calendarEnabled) {
                                if (events.isEmpty()) "No events" else "${events.size} event${if (events.size == 1) "" else "s"}"
                            } else {
                                "Calendar access not granted"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = style.tertiary
                        )
                    }
                    if (!prefs.calendarEnabled) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { shell.requestCalendarPermission() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Enable", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                events.take(4).forEach { event ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FF6B6B))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (event.allDay) "all-day" else Instant.ofEpochMilli(event.start)
                                    .atZone(ZoneId.systemDefault()).toLocalTime()
                                    .format(DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.labelSmall,
                                color = style.text
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = style.text,
                                maxLines = 1
                            )
                            event.location?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = style.tertiary, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLink(Icons.Rounded.Notifications, "Android\nnotifications", Modifier.weight(1f)) {
                        Intents.openSystemSettings(context, "android.settings.NOTIFICATION_SETTINGS")
                    }
                    QuickLink(Icons.Rounded.CalendarMonth, "Calendar", Modifier.weight(1f)) {
                        state.openWindow(BuiltinApp.CALENDAR)
                    }
                    QuickLink(Icons.Rounded.Tune, "Settings", Modifier.weight(1f)) {
                        state.openSettingsTo("calendar")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Storage ${"%.0f".format(snapshot.storageUsedGb)} of ${"%.0f".format(snapshot.storageTotalGb)} GB · " +
                        "Memory ${snapshot.memUsedMb}/${snapshot.memTotalMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Real Android notifications live in the system shade above this launcher. Android does not let a third-party Home app replace or read that shade, so this panel shows calendar, battery and system shortcuts instead of faking notifications.",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary
                )
            }
        }
    }
}

@Composable
private fun QuickLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(style.field)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = style.text, modifier = Modifier.height(17.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = style.secondary,
            maxLines = 2
        )
    }
}
