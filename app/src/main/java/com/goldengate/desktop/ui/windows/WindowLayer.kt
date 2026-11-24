package com.goldengate.desktop.ui.windows

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.goldengate.desktop.core.AppWindow
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.FrameDp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.apps.CalculatorApp
import com.goldengate.desktop.ui.apps.CalendarApp
import com.goldengate.desktop.ui.apps.ClockApp
import com.goldengate.desktop.ui.apps.FinderApp
import com.goldengate.desktop.ui.apps.NotesApp
import com.goldengate.desktop.ui.apps.SettingsApp
import com.goldengate.desktop.ui.apps.SystemActivityApp
import com.goldengate.desktop.ui.apps.WeatherApp
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.ui.components.TrafficLights
import com.goldengate.desktop.ui.theme.GG
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.ui.theme.WindowShape

@Composable
fun WindowLayer(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val surfaceW = maxWidth
        val surfaceH = maxHeight
        state.windows
            .filterNot { it.minimized }
            .sortedBy { it.z }
            .forEach { window ->
                androidx.compose.runtime.key(window.key) {
                    LauncherWindow(
                        window = window,
                        state = state,
                        prefs = prefs,
                        surfaceW = surfaceW,
                        surfaceH = surfaceH
                    )
                }
            }
    }
}

@Composable
private fun LauncherWindow(
    window: AppWindow,
    state: LauncherState,
    prefs: Prefs,
    surfaceW: Dp,
    surfaceH: Dp
) {
    val style = LocalGlassStyle.current
    val density = LocalDensity.current
    val isActive = state.activeKey == window.key
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val appear by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = if (prefs.reduceMotion || !prefs.windowAnimations) 0 else 180),
        label = "windowAppear"
    )

    val target = if (window.maximized) {
        FrameDp(2f, 2f, surfaceW.value - 4f, surfaceH.value - 4f)
    } else {
        window.frame.clampTo(surfaceW.value, surfaceH.value)
    }

    var menuOpen by remember { mutableStateOf(false) }
    var hoverTitle by remember { mutableStateOf(false) }
    var tappableMenu by remember { mutableStateOf(false) }

    Box(
        Modifier
            .offset(x = target.x.dp, y = target.y.dp)
            .size(width = target.width.dp, height = target.height.dp)
            .zIndex(window.z.toFloat())
            .graphicsLayer {
                alpha = appear
                scaleX = 0.985f + 0.015f * appear
                scaleY = 0.985f + 0.015f * appear
            }
            .pointerInput(window.key) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) state.focus(window.key)
                    }
                }
            }
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxSize(),
            shape = WindowShape,
            elevation = if (isActive) 36.dp else 22.dp,
            blurRadius = 30.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                // ------------------------------------------------ title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .pointerInput(window.key) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                state.moveWindow(
                                    window.key,
                                    amount.x / density.density,
                                    amount.y / density.density,
                                    surfaceW.value,
                                    surfaceH.value
                                )
                            }
                        }
                        .pointerInput(window.key) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    when (event.type) {
                                        PointerEventType.Enter -> hoverTitle = true
                                        PointerEventType.Exit -> hoverTitle = false
                                        PointerEventType.Press ->
                                            if (event.buttons.isSecondaryPressed) tappableMenu = true
                                        else -> Unit
                                    }
                                }
                            }
                        }
                        .pointerInput(window.key) {
                            detectTapGestures(
                                onDoubleTap = { state.toggleMaximize(window.key) }
                            )
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrafficLights(
                        showGlyphs = hoverTitle,
                        onClose = { state.close(window.key) },
                        onMinimize = { state.minimize(window.key) },
                        onZoom = { state.toggleMaximize(window.key) }
                    )
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            window.app.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isActive) style.text else style.secondary
                        )
                        Text(
                            window.app.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = style.tertiary,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.width(52.dp), contentAlignment = Alignment.CenterEnd) {
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .pointerInput(window.key) {
                                    detectTapGestures(onTap = { tappableMenu = true })
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hoverTitle) {
                                Text("•••", color = style.secondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(0.6.dp).background(style.divider))

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AppContent(window.app, state, prefs)
                }
            }
        }

        // Resize handle
        if (!window.maximized) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .pointerInput(window.key, surfaceW, surfaceH) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            state.resizeWindow(
                                window.key,
                                amount.x / density.density,
                                amount.y / density.density,
                                surfaceW.value,
                                surfaceH.value
                            )
                        }
                    }
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 5.dp)
                        .size(9.dp)
                        .border(1.dp, style.tertiary, RoundedCornerShape(2.dp))
                )
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = tappableMenu || menuOpen,
            onDismissRequest = {
                tappableMenu = false
                menuOpen = false
            },
            containerColor = style.popover,
            shape = RoundedCornerShape(10.dp)
        ) {
            listOf(
                "Minimize" to { state.minimize(window.key) },
                "Zoom" to { state.toggleMaximize(window.key) },
                "Close" to { state.close(window.key) }
            ).forEach { (label, action) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(label, color = style.text) },
                    onClick = {
                        tappableMenu = false
                        action()
                    }
                )
            }
        }
    }
}

@Composable
private fun AppContent(app: BuiltinApp, state: LauncherState, prefs: Prefs) {
    when (app) {
        BuiltinApp.FINDER -> FinderApp(state, prefs, Modifier.fillMaxSize())
        BuiltinApp.NOTES -> NotesApp(state, prefs, Modifier.fillMaxSize())
        BuiltinApp.CALCULATOR -> CalculatorApp(state, Modifier.fillMaxSize())
        BuiltinApp.CALENDAR -> CalendarApp(state, prefs, Modifier.fillMaxSize())
        BuiltinApp.CLOCK -> ClockApp(state, Modifier.fillMaxSize())
        BuiltinApp.WEATHER -> WeatherApp(state, prefs, Modifier.fillMaxSize())
        BuiltinApp.ACTIVITY -> SystemActivityApp(state, Modifier.fillMaxSize())
        BuiltinApp.SETTINGS -> SettingsApp(state, prefs, Modifier.fillMaxSize())
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(app.title, color = Color.White)
        }
    }
}

@Suppress("unused")
private val unusedOffset = Offset.Zero

@Suppress("unused")
private val unusedRadius = GG.WindowRadius
