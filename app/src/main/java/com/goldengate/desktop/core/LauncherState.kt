package com.goldengate.desktop.core

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.goldengate.desktop.util.Intents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class Overlay { NONE, SPOTLIGHT, LAUNCHPAD, CONTROL_CENTER, NOTIFICATIONS, ASSISTANT, SETUP, FOLDER }

/** Window frame in dp, relative to the desktop surface (below the menu bar). */
@Immutable
data class FrameDp(val x: Float = 80f, val y: Float = 60f, val width: Float = 720f, val height: Float = 480f) {
    fun clampTo(maxWidth: Float, maxHeight: Float): FrameDp {
        val w = width.coerceIn(300f, maxWidth.coerceAtLeast(300f))
        val h = height.coerceIn(240f, maxHeight.coerceAtLeast(240f))
        return copy(
            x = x.coerceIn(-w * 0.15f, (maxWidth - w * 0.55f).coerceAtLeast(0f)),
            y = y.coerceIn(0f, (maxHeight - 56f).coerceAtLeast(0f)),
            width = w,
            height = h
        )
    }
}

@Immutable
data class AppCommand(
    val app: BuiltinApp,
    val action: String,
    val payload: String? = null,
    val id: Long = 0L
)

@Immutable
data class AppWindow(
    val key: String,
    val app: BuiltinApp,
    val frame: FrameDp,
    val restoreFrame: FrameDp = frame,
    val maximized: Boolean = false,
    val minimized: Boolean = false,
    val z: Int = 0
)

class LauncherState(val context: Context, val prefs: Prefs) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appsRepository = AppsRepository(context)

    var apps by mutableStateOf<List<InstalledApp>>(emptyList())
        private set
    var appsLoading by mutableStateOf(true)
        private set

    var windows by mutableStateOf<List<AppWindow>>(emptyList())
        private set
    var activeKey by mutableStateOf<String?>(null)
    var overlay by mutableStateOf(Overlay.NONE)
    var openFolderId by mutableStateOf<String?>(null)
    var running by mutableStateOf<Set<String>>(emptySet())
        private set
    var desktopSize by mutableStateOf(IntSize.Zero)
    var message by mutableStateOf<Pair<Long, String>?>(null)
        private set
    var now by mutableStateOf(LocalDateTime.now())
    var isDefaultHome by mutableStateOf(false)
    var snackbarKey by mutableStateOf(0L)
    var batteryLevel by mutableStateOf(100)
    var charging by mutableStateOf(false)
    var command by mutableStateOf<AppCommand?>(null)
    var spotlightSeed by mutableStateOf<String?>(null)
    var assistantSeed by mutableStateOf<String?>(null)

    private var nextZ = 1
    private var windowSeq = 0
    private var lastAppRefresh = 0L

    fun refreshApps(force: Boolean = false) {
        val stamp = System.currentTimeMillis()
        if (!force && stamp - lastAppRefresh < 4_000) return
        lastAppRefresh = stamp
        scope.launch {
            appsLoading = true
            apps = appsRepository.load()
            appsLoading = false
        }
    }

    fun notify(text: String) {
        message = System.currentTimeMillis() to text
        snackbarKey = System.currentTimeMillis()
    }

    fun appEntry(component: String): InstalledApp? = apps.firstOrNull { it.component == component }

    fun builtinRunning(app: BuiltinApp): Boolean =
        windows.any { it.app == app } || running.contains("builtin:${app.id}")

    fun packageRunning(packageName: String): Boolean = running.contains(packageName)

    // ------------------------------------------------------------- window management
    private fun defaultFrame(): FrameDp {
        val size = desktopSize
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val widthDp = size.width / density
        val heightDp = size.height / density
        val compact = widthDp < 620f
        val winW = if (compact) (widthDp * 0.94f).coerceIn(310f, 620f) else (widthDp * 0.60f).coerceIn(520f, 1080f)
        val winH = if (compact) (heightDp * 0.66f).coerceIn(340f, 720f) else (heightDp * 0.66f).coerceIn(420f, 860f)
        val stagger = (windowSeq++ % 5) * 18f
        return FrameDp(
            x = ((widthDp - winW) / 2f + stagger).coerceAtLeast(6f),
            y = ((heightDp - winH) / 2f - 8f + stagger).coerceAtLeast(4f),
            width = winW,
            height = winH
        )
    }

    fun openApp(app: BuiltinApp) {
        when (app.kind) {
            AppKind.WINDOW -> openWindow(app)
            AppKind.OVERLAY -> overlay = Overlay.LAUNCHPAD
            AppKind.EXTERNAL -> when (app) {
                BuiltinApp.PHOTOS -> if (!Intents.openGallery(context)) notify("No gallery app installed")
                BuiltinApp.SAFARI -> if (!Intents.openBrowser(context)) notify("No browser installed")
                else -> Unit
            }
        }
    }

    fun openWindow(app: BuiltinApp) {
        val existing = windows.firstOrNull { it.app == app }
        if (existing != null) {
            focus(existing.key)
            if (existing.minimized) update(existing.key) { it.copy(minimized = false) }
            return
        }
        val frame = defaultFrame()
        val win = AppWindow(
            key = "win-${System.currentTimeMillis()}-${windows.size}",
            app = app,
            frame = frame,
            z = nextZ++
        )
        windows = windows + win
        activeKey = win.key
        running = running + "builtin:${app.id}"
    }

    fun activate(shortcut: Shortcut) {
        when (shortcut.kind) {
            ShortcutKind.BUILTIN -> BuiltinApp.fromId(shortcut.target)?.let(::openApp)
            ShortcutKind.FOLDER -> {
                openFolderId = shortcut.target
                overlay = Overlay.FOLDER
            }
            ShortcutKind.APP -> {
                val component = shortcut.target
                if (Intents.launchComponent(context, component)) {
                    val pkg = component.substringBefore('/')
                    running = running + pkg
                    notify("Opened ${shortcut.label.ifBlank { appEntry(component)?.label ?: "app" }}")
                } else {
                    notify("That app is no longer installed")
                }
            }
        }
    }

    fun launchPackage(component: String, label: String) {
        if (Intents.launchComponent(context, component)) {
            running = running + component.substringBefore('/')
            notify("Opened $label")
        } else {
            notify("$label could not be opened")
        }
    }

    fun close(key: String) {
        windows = windows.filterNot { it.key == key }
        if (activeKey == key) activeKey = windows.maxByOrNull { it.z }?.key
    }

    fun closeApp(app: BuiltinApp) {
        windows.filter { it.app == app }.forEach { close(it.key) }
    }

    fun focus(key: String) = update(key) { it.copy(z = nextZ++, minimized = false) }

    fun minimize(key: String) = update(key) { it.copy(minimized = true) }

    fun restore(key: String) {
        update(key) { it.copy(minimized = false, z = nextZ++) }
        activeKey = key
    }

    fun toggleMaximize(key: String) {
        update(key) { win ->
            if (win.maximized) {
                win.copy(maximized = false, frame = win.restoreFrame, z = nextZ++)
            } else {
                win.copy(maximized = true, restoreFrame = win.frame, z = nextZ++)
            }
        }
    }

    fun moveWindow(key: String, dx: Float, dy: Float, surfaceWidth: Float, surfaceHeight: Float) {
        update(key) { win ->
            if (win.maximized) win
            else win.copy(
                frame = win.frame.copy(
                    x = (win.frame.x + dx).coerceIn(-win.frame.width * 0.55f, surfaceWidth - 90f),
                    y = (win.frame.y + dy).coerceIn(0f, (surfaceHeight - 44f).coerceAtLeast(0f))
                )
            )
        }
    }

    fun resizeWindow(key: String, dw: Float, dh: Float, surfaceWidth: Float, surfaceHeight: Float) {
        update(key) { win ->
            if (win.maximized) win
            else win.copy(
                frame = win.frame.copy(
                    width = (win.frame.width + dw).coerceIn(300f, surfaceWidth.coerceAtLeast(320f)),
                    height = (win.frame.height + dh).coerceIn(240f, surfaceHeight.coerceAtLeast(260f))
                )
            )
        }
    }

    private fun update(key: String, transform: (AppWindow) -> AppWindow) {
        windows = windows.map { if (it.key == key) transform(it) else it }
        activeKey = key
    }

    fun sendCommand(app: BuiltinApp, action: String, payload: String? = null) {
        command = AppCommand(app, action, payload, System.currentTimeMillis())
    }

    fun cycleWindows() {
        val visible = windows.filterNot { it.minimized }
        if (visible.isEmpty()) return
        val ordered = visible.sortedBy { it.z }
        val index = ordered.indexOfFirst { it.key == activeKey }
        val next = ordered[(index + 1) % ordered.size]
        focus(next.key)
    }

    // ------------------------------------------------------------- overlays
    fun toggleOverlay(target: Overlay) {
        overlay = if (overlay == target) Overlay.NONE else target
    }

    fun dismissOverlay() {
        if (overlay == Overlay.SETUP) return
        overlay = Overlay.NONE
        openFolderId = null
    }

    fun openSettingsTo(page: String? = null) {
        settingsRequestedPage = page
        overlay = Overlay.NONE
        openWindow(BuiltinApp.SETTINGS)
    }

    var settingsRequestedPage by mutableStateOf<String?>(null)

    fun wallpaperSpec(): com.goldengate.desktop.ui.wallpaper.WallpaperSpec {
        val photo = prefs.wallpaperPhotoUri
        return if (prefs.wallpaperId == "photo" && !photo.isNullOrBlank()) {
            com.goldengate.desktop.ui.wallpaper.WallpaperSpec.Photo(photo)
        } else {
            com.goldengate.desktop.ui.wallpaper.WallpaperSpec.Preset(prefs.wallpaperId)
        }
    }
}
