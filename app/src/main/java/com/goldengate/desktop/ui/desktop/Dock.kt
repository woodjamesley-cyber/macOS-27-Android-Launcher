package com.goldengate.desktop.ui.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.core.Shortcut
import com.goldengate.desktop.core.ShortcutKind
import com.goldengate.desktop.ui.components.BuiltinIcon
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.components.ShortcutIcon
import com.goldengate.desktop.ui.components.shortcutLabel
import com.goldengate.desktop.ui.components.shortcutMenu
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

fun dockIconSize(index: Int): Dp = when (index) {
    0 -> 40.dp
    2 -> 58.dp
    else -> 48.dp
}

@Composable
fun DockBar(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    if (!prefs.dockAutoHide) {
        DockContent(state = state, prefs = prefs, modifier = modifier)
        return
    }

    var revealed by remember { mutableStateOf(false) }
    Box(
        modifier.navigationBarsPadding().padding(bottom = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            DockContent(state = state, prefs = prefs, modifier = Modifier)
        }
        Box(
            Modifier
                .padding(bottom = 2.dp)
                .size(width = 84.dp, height = 6.dp)
                .clip(CircleShape)
                .background(LocalGlassStyle.current.hairline)
                .clickable { revealed = !revealed }
                .semantics { contentDescription = "Reveal Dock" }
        )
    }
}

@Composable
private fun DockContent(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    val iconSize = dockIconSize(prefs.dockIconSize)
    val items = prefs.dockItems
    var hoverX by remember { mutableStateOf<Float?>(null) }
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragAmount by remember { mutableStateOf(0f) }

    val itemStridePx = with(androidx.compose.ui.platform.LocalDensity.current) { (iconSize + 6.dp).toPx() }
    val halfIconPx = with(androidx.compose.ui.platform.LocalDensity.current) { iconSize.toPx() } / 2f

    Box(
        modifier.navigationBarsPadding().padding(bottom = 9.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassPanel(
            modifier = Modifier.wrapContentWidth().height(iconSize + 26.dp),
            shape = RoundedCornerShape(22.dp),
            elevation = 30.dp,
            blurRadius = 30.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                when (event.type) {
                                    PointerEventType.Move, PointerEventType.Enter ->
                                        hoverX = event.changes.firstOrNull()?.position?.x
                                    PointerEventType.Exit -> hoverX = null
                                    else -> Unit
                                }
                            }
                        }
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEachIndexed { index, shortcut ->
                    val centerX = 10.dp.value.let { 0f } + halfIconPx + index * itemStridePx
                    val distance = hoverX?.let { abs(it - (centerX + 10f * 3f)) } ?: Float.MAX_VALUE
                    val hoverScale = if (prefs.dockMagnification && hoverX != null && items.size > 1) {
                        1f + 0.30f * exp(-((distance / (itemStridePx * 1.25f)) * (distance / (itemStridePx * 1.25f))))
                    } else 1f

                    val isDragging = dragIndex == index
                    val pressedScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.12f else hoverScale,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
                        label = "dockScale"
                    )

                    var menuOpen by remember(shortcut.key) { mutableStateOf(false) }

                    Box(
                        Modifier
                            .zIndex(if (isDragging) 8f else 1f)
                            .graphicsLayer {
                                scaleX = pressedScale
                                scaleY = pressedScale
                                translationY = if (isDragging) -18f else -((pressedScale - 1f) * iconSize.toPx() * 0.5f)
                                translationX = if (isDragging) dragAmount else 0f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            }
                    ) {
                        DockTile(
                            shortcut = shortcut,
                            iconSize = iconSize,
                            state = state,
                            running = dockItemRunning(shortcut, state)
                        ) {
                            state.activate(shortcut)
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = style.popover,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val entries = shortcutMenu(
                                shortcut = shortcut,
                                state = state,
                                prefs = prefs,
                                onRemoveFromDock = {
                                    prefs.setDockItems(prefs.dockItems.filterNot { it.key == shortcut.key })
                                    state.notify("Removed from Dock")
                                }
                            )
                            entries.forEach { entry ->
                                when (entry) {
                                    is MenuEntry.Divider -> androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = style.divider
                                    )
                                    is MenuEntry.Item -> androidx.compose.material3.DropdownMenuItem(
                                        text = {
                                            Text(
                                                entry.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = style.text
                                            )
                                        },
                                        onClick = {
                                            menuOpen = false
                                            entry.onClick()
                                        },
                                        trailingIcon = entry.shortcut?.let {
                                            {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = style.tertiary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Drag + tap gesture layer sits in front of the tile.
                    Box(
                        Modifier
                            .size(iconSize)
                            .zIndex(9f)
                            .pointerInput(shortcut.key, items.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragIndex = index
                                        dragAmount = 0f
                                        hoverX = null
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragAmount += amount.x
                                    },
                                    onDragEnd = {
                                        val moved = dragAmount
                                        val from = index
                                        dragIndex = null
                                        dragAmount = 0f
                                        if (abs(moved) < 14f) {
                                            menuOpen = true
                                        } else {
                                            val shift = (moved / itemStridePx).roundToInt()
                                            val target = (from + shift).coerceIn(0, items.lastIndex)
                                            if (target != from) {
                                                val mutable = items.toMutableList()
                                                val item = mutable.removeAt(from)
                                                mutable.add(target, item)
                                                prefs.setDockItems(mutable)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        dragIndex = null
                                        dragAmount = 0f
                                    }
                                )
                            }
                            .pointerInput(shortcut.key) {
                                detectTapGestures(
                                    onTap = { state.activate(shortcut) },
                                    onDoubleTap = {
                                        if (shortcut.kind == ShortcutKind.BUILTIN) {
                                            BuiltinApp.fromId(shortcut.target)?.takeIf {
                                                it.kind == com.goldengate.desktop.core.AppKind.WINDOW
                                            }?.let(state::openWindow)
                                        }
                                    }
                                )
                            }
                            .semantics {
                                contentDescription = "Dock: ${shortcutLabel(shortcut, state)}"
                            }
                    )
                }

                Box(
                    Modifier
                        .width(1.dp)
                        .height(iconSize * 0.8f)
                        .align(Alignment.CenterVertically)
                        .background(style.divider)
                )

                var launchpadMenu by remember { mutableStateOf(false) }
                Box {
                    Box(
                        Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(iconSize * 0.235f))
                            .clickable {
                                state.overlay = if (state.overlay == com.goldengate.desktop.core.Overlay.LAUNCHPAD) {
                                    com.goldengate.desktop.core.Overlay.NONE
                                } else {
                                    com.goldengate.desktop.core.Overlay.LAUNCHPAD
                                }
                            }
                            .semantics { contentDescription = "Launchpad" }
                    ) {
                        BuiltinIcon(BuiltinApp.LAUNCHPAD, iconSize)
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = launchpadMenu,
                        onDismissRequest = { launchpadMenu = false },
                        containerColor = style.popover,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Open Launchpad", color = style.text) },
                            onClick = {
                                launchpadMenu = false
                                state.overlay = com.goldengate.desktop.core.Overlay.LAUNCHPAD
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Refresh app list", color = style.text) },
                            onClick = {
                                launchpadMenu = false
                                state.refreshApps(force = true)
                                state.notify("App list refreshed")
                            }
                        )
                    }
                }
            }
        }
    }
    // Keep the unused shell reference meaningful for future actions without changing behavior.
    @Suppress("UNUSED_EXPRESSION")
    shell
}

@Composable
private fun DockTile(
    shortcut: Shortcut,
    iconSize: Dp,
    state: LauncherState,
    running: Boolean,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Box(Modifier.width(iconSize).height(iconSize + 10.dp)) {
        Box(
            Modifier
                .size(iconSize)
                .align(Alignment.TopCenter)
        ) {
            ShortcutIcon(shortcut, state, iconSize)
        }
        if (running) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(4.dp)
                    .background(style.text.copy(alpha = 0.75f), CircleShape)
            )
        }
    }
}

private fun dockItemRunning(shortcut: Shortcut, state: LauncherState): Boolean = when (shortcut.kind) {
    ShortcutKind.BUILTIN -> BuiltinApp.fromId(shortcut.target)?.let { state.builtinRunning(it) } ?: false
    ShortcutKind.APP -> state.packageRunning(shortcut.target.substringBefore('/'))
    ShortcutKind.FOLDER -> false
}
