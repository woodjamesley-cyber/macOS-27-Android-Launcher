package com.goldengate.desktop.ui.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Overlay
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.components.ConnectBatteryState
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.ui.components.LocalRootSize
import com.goldengate.desktop.ui.components.LocalWallpaper
import com.goldengate.desktop.ui.overlays.AssistantOverlay
import com.goldengate.desktop.ui.overlays.ControlCenterOverlay
import com.goldengate.desktop.ui.overlays.FolderOverlay
import com.goldengate.desktop.ui.overlays.LaunchpadOverlay
import com.goldengate.desktop.ui.overlays.NotificationCenterOverlay
import com.goldengate.desktop.ui.overlays.SetupOverlay
import com.goldengate.desktop.ui.overlays.SpotlightOverlay
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.ui.windows.WindowLayer
import com.goldengate.desktop.ui.wallpaper.WallpaperArt
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun DesktopRoot(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    ConnectBatteryState(state)

    LaunchedEffect(Unit) {
        while (true) {
            state.now = LocalDateTime.now()
            delay(1000)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val size = IntSize(
            with(density) { maxWidth.toPx() }.toInt(),
            with(density) { maxHeight.toPx() }.toInt()
        )
        LaunchedEffect(size) { state.desktopSize = size }

        CompositionLocalProvider(
            LocalRootSize provides size,
            LocalWallpaper provides state.wallpaperSpec()
        ) {
            WallpaperArt(state.wallpaperSpec(), Modifier.fillMaxSize())

            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            ) {
                MenuBar(state = state, prefs = prefs)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    DesktopSurface(state = state, prefs = prefs, modifier = Modifier.fillMaxSize())
                    WindowLayer(state = state, prefs = prefs, modifier = Modifier.fillMaxSize())
                }
                DockBar(state = state, prefs = prefs, modifier = Modifier.fillMaxWidth())
            }

            when (state.overlay) {
                Overlay.SPOTLIGHT -> SpotlightOverlay(state, prefs)
                Overlay.LAUNCHPAD -> LaunchpadOverlay(state, prefs)
                Overlay.CONTROL_CENTER -> ControlCenterOverlay(state, prefs)
                Overlay.NOTIFICATIONS -> NotificationCenterOverlay(state, prefs)
                Overlay.ASSISTANT -> AssistantOverlay(state, prefs)
                Overlay.FOLDER -> FolderOverlay(state, prefs)
                else -> Unit
            }

            if (!prefs.setupComplete || state.overlay == Overlay.SETUP) {
                SetupOverlay(state, prefs)
            }

            TransientMessage(state)
        }
    }
}

@Composable
private fun TransientMessage(state: LauncherState) {
    val style = LocalGlassStyle.current
    var visible by remember(state.message) { mutableStateOf(state.message != null) }
    LaunchedEffect(state.snackbarKey) {
        if (state.message != null) {
            visible = true
            delay(2600)
            visible = false
        }
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 44.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible && state.message != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }
        ) {
            GlassPanel(
                modifier = Modifier.padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
                elevation = 24.dp
            ) {
                androidx.compose.material3.Text(
                    text = state.message?.second ?: "",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = style.text,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}
