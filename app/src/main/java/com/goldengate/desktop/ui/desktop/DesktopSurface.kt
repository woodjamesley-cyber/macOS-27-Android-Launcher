package com.goldengate.desktop.ui.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.goldengate.desktop.core.DesktopWidget
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.core.WidgetKind
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.ui.components.ShortcutIcon
import com.goldengate.desktop.ui.components.shortcutLabel
import com.goldengate.desktop.ui.components.shortDate
import com.goldengate.desktop.ui.components.shortTime
import com.goldengate.desktop.ui.components.shortcutMenu
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import kotlin.math.roundToInt

fun desktopIconSize(index: Int): Dp = when (index) {
    0 -> 44.dp
    2 -> 66.dp
    else -> 56.dp
}

@Composable
fun DesktopSurface(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    Box(modifier.padding(horizontal = 14.dp)) {
        if (prefs.showDesktopWidgets) {
            DesktopWidgets(state, prefs, Modifier.fillMaxSize())
        }
        if (prefs.showDesktopIcons) {
            DesktopIcons(
                state = state,
                prefs = prefs,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun DesktopIcons(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    val items = prefs.desktopItems
    val iconSize = desktopIconSize(prefs.desktopIconSize)
    val style = LocalGlassStyle.current
    var menuKey by remember { mutableStateOf<String?>(null) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    BoxWithConstraints(modifier) {
        val columns = when {
            maxWidth >= 840.dp -> 3
            maxWidth >= 620.dp -> 2
            maxWidth >= 420.dp -> 2
            else -> 1
        }
        val cellWidth = iconSize + 34.dp
        val cellHeight = iconSize + 46.dp

        Column(horizontalAlignment = Alignment.End) {
            items.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowItems.forEach { shortcut ->
                        val index = items.indexOf(shortcut)
                        val isDragged = draggedKey == shortcut.key
                        val lift by animateFloatAsState(
                            if (isDragged) 1.08f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
                            label = "desktopIconLift"
                        )
                        Box(
                            Modifier
                                .width(cellWidth)
                                .height(cellHeight)
                                .zIndex(if (isDragged) 6f else 1f)
                                .graphicsLayer {
                                    scaleX = lift
                                    scaleY = lift
                                    translationX = if (isDragged) dragDelta.x else 0f
                                    translationY = if (isDragged) dragDelta.y else 0f
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp)
                            ) {
                                ShortcutIcon(shortcut, state, iconSize)
                                if (prefs.iconLabels) {
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        text = shortcutLabel(shortcut, state),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.28f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Box(
                                Modifier
                                    .size(iconSize + 10.dp)
                                    .align(Alignment.TopCenter)
                                    .pointerInput(shortcut.key, items.size) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedKey = shortcut.key
                                                dragDelta = androidx.compose.ui.geometry.Offset.Zero
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragDelta += amount
                                            },
                                            onDragEnd = {
                                                val moved = dragDelta
                                                draggedKey = null
                                                dragDelta = androidx.compose.ui.geometry.Offset.Zero
                                                if (moved.getDistance() < 14f) {
                                                    menuKey = shortcut.key
                                                } else {
                                                    val density = this
                                                    val colShift = (moved.x / (cellWidth.toPx())).roundToInt()
                                                    val rowShift = (moved.y / (cellHeight.toPx())).roundToInt()
                                                    val target = (index + colShift + rowShift * columns)
                                                        .coerceIn(0, items.lastIndex)
                                                    if (target != index) {
                                                        val mutable = items.toMutableList()
                                                        val item = mutable.removeAt(index)
                                                        mutable.add(target, item)
                                                        prefs.setDesktopItems(mutable)
                                                    }
                                                    @Suppress("UNUSED_EXPRESSION") density
                                                }
                                            },
                                            onDragCancel = {
                                                draggedKey = null
                                                dragDelta = androidx.compose.ui.geometry.Offset.Zero
                                            }
                                        )
                                    }
                                    .pointerInput(shortcut.key) {
                                        detectTapGestures(
                                            onTap = { state.activate(shortcut) },
                                            onDoubleTap = { state.activate(shortcut) }
                                        )
                                    }
                                    .semantics {
                                        contentDescription = "Desktop: ${shortcutLabel(shortcut, state)}"
                                    }
                            )

                            androidx.compose.material3.DropdownMenu(
                                expanded = menuKey == shortcut.key,
                                onDismissRequest = { menuKey = null },
                                containerColor = style.popover,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                shortcutMenu(
                                    shortcut = shortcut,
                                    state = state,
                                    prefs = prefs,
                                    onRemoveFromDesktop = {
                                        prefs.setDesktopItems(prefs.desktopItems.filterNot { it.key == shortcut.key })
                                        state.notify("Removed from Desktop")
                                    }
                                ).forEach { entry ->
                                    when (entry) {
                                        is MenuEntry.Divider -> androidx.compose.material3.HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = style.divider
                                        )
                                        is MenuEntry.Item -> androidx.compose.material3.DropdownMenuItem(
                                            text = {
                                                Text(entry.label, color = style.text, style = MaterialTheme.typography.bodyMedium)
                                            },
                                            onClick = {
                                                menuKey = null
                                                entry.onClick()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopWidgets(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val surfaceW = maxWidth
        val surfaceH = maxHeight
        val compact = surfaceW < 620.dp
        prefs.widgets.forEach { widget ->
            WidgetCard(
                widget = widget,
                state = state,
                prefs = prefs,
                bounds = androidx.compose.ui.unit.DpSize(surfaceW, surfaceH),
                compact = compact
            )
        }
    }
}

@Composable
private fun WidgetCard(
    widget: DesktopWidget,
    state: LauncherState,
    prefs: Prefs,
    bounds: androidx.compose.ui.unit.DpSize,
    compact: Boolean
) {
    val style = LocalGlassStyle.current
    val density = LocalDensity.current
    val baseSize = when (widget.size) {
        0 -> androidx.compose.ui.unit.DpSize(154.dp, 154.dp)
        2 -> androidx.compose.ui.unit.DpSize(306.dp, 306.dp)
        else -> androidx.compose.ui.unit.DpSize(306.dp, 154.dp)
    }
    val size = if (compact) {
        androidx.compose.ui.unit.DpSize(
            (baseSize.width * 0.82f).coerceAtMost(bounds.width - 28.dp),
            (baseSize.height * 0.82f)
        )
    } else baseSize

    val maxX = (bounds.width - size.width).coerceAtLeast(0.dp)
    val maxY = (bounds.height - size.height).coerceAtLeast(0.dp)
    val x = (maxX * widget.x).coerceIn(0.dp, maxX)
    val y = (maxY * widget.y).coerceIn(0.dp, maxY)

    var menuOpen by remember(widget.id) { mutableStateOf(false) }
    var dragOffset by remember(widget.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dragging by remember(widget.id) { mutableStateOf(false) }

    Box(
        Modifier
            .offset(x = x, y = y)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                scaleX = if (dragging) 1.02f else 1f
                scaleY = if (dragging) 1.02f else 1f
            }
            .zIndex(if (dragging) 5f else 2f)
    ) {
        GlassPanel(
            modifier = Modifier
                .size(size)
                .pointerInput(widget.id, bounds) {
                    val widthPx = bounds.width.toPx().coerceAtLeast(1f)
                    val heightPx = bounds.height.toPx().coerceAtLeast(1f)
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragging = true
                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount
                        },
                        onDragEnd = {
                            val moved = dragOffset
                            dragging = false
                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                            if (moved.getDistance() < 14f) {
                                menuOpen = true
                            } else {
                                val newX = (x + with(density) { moved.x.toDp() }) / bounds.width
                                val newY = (y + with(density) { moved.y.toDp() }) / bounds.height
                                prefs.setWidgets(
                                    prefs.widgets.map {
                                        if (it.id == widget.id) {
                                            it.copy(
                                                x = newX.coerceIn(0f, 0.95f),
                                                y = newY.coerceIn(0f, 0.95f)
                                            )
                                        } else it
                                    }
                                )
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                        }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            elevation = 18.dp
        ) {
            WidgetBody(widget.kind, state, prefs, compact)
        }

        androidx.compose.material3.DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = style.popover,
            shape = RoundedCornerShape(10.dp)
        ) {
            listOf("Small", "Medium", "Large").forEachIndexed { index, label ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            "$label widget",
                            color = if (widget.size == index) MaterialTheme.colorScheme.primary else style.text
                        )
                    },
                    onClick = {
                        menuOpen = false
                        prefs.setWidgets(prefs.widgets.map { if (it.id == widget.id) it.copy(size = index) else it })
                    }
                )
            }
            androidx.compose.material3.HorizontalDivider(color = style.divider)
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Remove Widget", color = style.text) },
                onClick = {
                    menuOpen = false
                    prefs.setWidgets(prefs.widgets.filterNot { it.id == widget.id })
                    state.notify("${widget.kind.title} widget removed")
                }
            )
        }
    }
}

@Composable
private fun WidgetBody(kind: WidgetKind, state: LauncherState, prefs: Prefs, compact: Boolean) {
    val style = LocalGlassStyle.current
    val padding = if (compact) 12.dp else 15.dp
    when (kind) {
        WidgetKind.CLOCK -> Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                state.now.shortTime(prefs.menuBarSeconds),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = if (compact) 34.sp else 43.sp),
                color = style.text
            )
            Spacer(Modifier.weight(1f))
            Text(state.now.shortDate(), style = MaterialTheme.typography.labelMedium, color = style.secondary)
            Text("Golden Gate", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
        }
        WidgetKind.WEATHER -> {
            val data = com.goldengate.desktop.data.WeatherStore.data
            androidx.compose.runtime.LaunchedEffect(prefs.weatherCity) {
                com.goldengate.desktop.data.WeatherStore.refresh(prefs.weatherCity)
            }
            Column(
                Modifier.fillMaxSize().padding(padding).clickable { state.openWindow(com.goldengate.desktop.core.BuiltinApp.WEATHER) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        com.goldengate.desktop.data.weatherGlyph(data?.code ?: 0),
                        style = MaterialTheme.typography.titleLarge,
                        color = style.text
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        data?.displayTemp(prefs.weatherUnit) ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = style.text
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    data?.city ?: prefs.weatherCity,
                    style = MaterialTheme.typography.labelMedium,
                    color = style.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    data?.let { com.goldengate.desktop.data.weatherDescription(it.code) } ?: "Tap to load",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary
                )
            }
        }
        WidgetKind.BATTERY -> Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Battery", style = MaterialTheme.typography.labelMedium, color = style.secondary)
            Text(
                "${state.batteryLevel}%",
                style = MaterialTheme.typography.headlineLarge,
                color = style.text
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(style.field)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(state.batteryLevel / 100f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (state.batteryLevel < 20) Color(0xFFFF5F57) else Color(0xFF28C840))
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                if (state.charging) "Charging" else "On battery",
                style = MaterialTheme.typography.labelSmall,
                color = style.tertiary
            )
        }
        WidgetKind.CALENDAR -> {
            var events by remember { mutableStateOf<List<com.goldengate.desktop.data.CalEvent>>(emptyList()) }
            androidx.compose.runtime.LaunchedEffect(prefs.calendarEnabled) {
                events = if (prefs.calendarEnabled) {
                    com.goldengate.desktop.data.CalendarStore.todayEvents(state.context)
                } else emptyList()
            }
            val next = com.goldengate.desktop.data.CalendarStore.upcoming(events)
            Column(
                Modifier.fillMaxSize().padding(padding).clickable {
                    state.openWindow(com.goldengate.desktop.core.BuiltinApp.CALENDAR)
                }
            ) {
                Text(state.now.shortDate().uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF6B6B))
                Spacer(Modifier.height(6.dp))
                Text(
                    next?.title ?: if (prefs.calendarEnabled) "No events today" else "Calendar access off",
                    style = MaterialTheme.typography.titleMedium,
                    color = style.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Text(
                    next?.let {
                        java.time.Instant.ofEpochMilli(it.start)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    } ?: if (prefs.calendarEnabled) "Enjoy the quiet" else "Enable in Settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary
                )
            }
        }
        WidgetKind.NOTES -> {
            val notes = com.goldengate.desktop.data.NotesStore.load(prefs.notesJson)
            val note = notes.firstOrNull { it.pinned } ?: notes.firstOrNull()
            Column(
                Modifier.fillMaxSize().padding(padding).clickable {
                    state.openWindow(com.goldengate.desktop.core.BuiltinApp.NOTES)
                }
            ) {
                Text("Notes", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFD60A))
                Spacer(Modifier.height(4.dp))
                Text(
                    note?.title ?: "No notes yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = style.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Text(
                    note?.body?.take(90) ?: "Tap to write one",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        WidgetKind.ACTIVITY -> {
            val stats = remember { com.goldengate.desktop.data.systemSnapshot(state.context) }
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text("Memory", style = MaterialTheme.typography.labelMedium, color = style.secondary)
                Text(
                    "${(stats.memUsedMb * 100 / stats.memTotalMb.coerceAtLeast(1))}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = style.text
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${stats.memUsedMb} MB used of ${stats.memTotalMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary,
                    maxLines = 2
                )
            }
        }
    }
}
