package com.goldengate.desktop.ui.apps

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.data.CalEvent
import com.goldengate.desktop.data.CalendarStore
import com.goldengate.desktop.ui.components.DividerLine
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.util.Intents
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarApp(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    val context = LocalContext.current
    val shell = LocalShell.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var events by remember { mutableStateOf<List<CalEvent>>(emptyList()) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(month, prefs.calendarEnabled, tick) {
        events = if (prefs.calendarEnabled) {
            CalendarStore.events(
                context,
                month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        } else emptyList()
    }

    LaunchedEffect(state.command) {
        val command = state.command ?: return@LaunchedEffect
        if (command.app != BuiltinApp.CALENDAR) return@LaunchedEffect
        when (command.action) {
            "today" -> {
                month = YearMonth.now()
                selectedDay = LocalDate.now()
            }
            "refresh" -> tick++
            "new_event" -> openNewEvent(context, selectedDay)
        }
    }

    val dayEvents = events.filter {
        Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDay
    }.sortedBy { it.start }

    BoxWithConstraints(modifier) {
        val wide = maxWidth > 720.dp
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleLarge,
                        color = style.text,
                        modifier = Modifier.weight(1f)
                    )
                    CalendarArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month") {
                        month = month.minusMonths(1)
                    }
                    CalendarArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month") {
                        month = month.plusMonths(1)
                    }
                    Spacer(Modifier.width(6.dp))
                    PillButton("Today") {
                        month = YearMonth.now()
                        selectedDay = LocalDate.now()
                    }
                    Spacer(Modifier.width(6.dp))
                    CalendarArrow(Icons.Rounded.Add, "New event") { openNewEvent(context, selectedDay) }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        .forEach { day ->
                            Text(
                                day.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = style.tertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                }
                Spacer(Modifier.height(4.dp))
                DividerLine()
                MonthGrid(
                    month = month,
                    selected = selectedDay,
                    eventDays = events.map {
                        Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalDate()
                    }.toSet(),
                    onSelect = { selectedDay = it }
                )
            }

            if (wide) {
                Box(Modifier.width(0.6.dp).fillMaxSize().background(style.divider))
                Column(Modifier.width(270.dp).fillMaxSize().padding(14.dp)) {
                    Text(
                        selectedDay.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                        color = style.text
                    )
                    Spacer(Modifier.height(8.dp))
                    Agenda(dayEvents, prefs, shell)
                }
            }
        }
    }
}

@Composable
private fun Agenda(events: List<CalEvent>, prefs: Prefs, shell: com.goldengate.desktop.core.Shell) {
    val style = LocalGlassStyle.current
    Column {
        if (!prefs.calendarEnabled) {
            Text(
                "Calendar access has not been granted, so no events can be shown. Android keeps Calendar data behind an explicit permission.",
                style = MaterialTheme.typography.bodySmall,
                color = style.secondary
            )
            Spacer(Modifier.height(10.dp))
            PillButton("Enable Calendar Access", prominent = true) { shell.requestCalendarPermission() }
        } else if (events.isEmpty()) {
            Text("Nothing scheduled.", style = MaterialTheme.typography.bodySmall, color = style.tertiary)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(events, key = { it.start.toString() + it.title }) { event ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(style.field)
                            .padding(8.dp)
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFFF6B6B))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = style.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (event.allDay) "All day" else {
                                    Instant.ofEpochMilli(event.start).atZone(ZoneId.systemDefault())
                                        .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                                        " – " +
                                        Instant.ofEpochMilli(event.end).atZone(ZoneId.systemDefault())
                                            .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = style.tertiary
                            )
                            event.location?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = style.tertiary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    eventDays: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit
) {
    val style = LocalGlassStyle.current
    val firstOfMonth = month.atDay(1)
    val leading = (firstOfMonth.dayOfWeek.value - 1)
    val days = month.lengthOfMonth()
    val today = LocalDate.now()
    val cells = leading + days
    val rows = (cells + 6) / 7

    Column(Modifier.fillMaxWidth()) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - leading + 1
                    Box(Modifier.weight(1f).aspectRatio(1.25f).padding(1.dp)) {
                        if (dayNumber in 1..days) {
                            val date = month.atDay(dayNumber)
                            val isSelected = date == selected
                            val isToday = date == today
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Transparent)
                                    .clickable { onSelect(date) }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> style.text
                                    },
                                    fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Spacer(Modifier.weight(1f))
                                if (date in eventDays) {
                                    Box(
                                        Modifier
                                            .size(4.dp)
                                            .background(
                                                if (isSelected) Color.White else Color(0xFFFF6B6B),
                                                CircleShape
                                            )
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val style = LocalGlassStyle.current
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = style.secondary, modifier = Modifier.size(19.dp))
    }
}

private fun openNewEvent(context: android.content.Context, day: LocalDate) {
    val begin = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = day.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        .putExtra(CalendarContract.Events.TITLE, "New Event")
    Intents.launchIntent(context, intent)
}
