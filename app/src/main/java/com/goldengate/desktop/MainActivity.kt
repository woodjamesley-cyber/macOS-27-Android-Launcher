package com.goldengate.desktop

import android.Manifest
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.goldengate.desktop.core.AppKind
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Overlay
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.core.Shell
import com.goldengate.desktop.ui.components.LocalRootSize
import com.goldengate.desktop.ui.components.LocalWallpaper
import com.goldengate.desktop.ui.desktop.DesktopRoot
import com.goldengate.desktop.ui.theme.GlassStyle
import com.goldengate.desktop.ui.theme.GoldenGateTheme
import com.goldengate.desktop.ui.theme.accentFor
import com.goldengate.desktop.util.Intents
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity(), Shell {

    private lateinit var prefs: Prefs
    private lateinit var state: LauncherState
    private var pendingVoice: ((String?) -> Unit)? = null
    private var pendingMicAction: (() -> Unit)? = null
    private var tts: TextToSpeech? = null

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        state.isDefaultHome = isDefaultHomeForNow()
        if (state.isDefaultHome) {
            prefs.setSetupComplete(true)
            state.notify("Golden Gate Desktop is now your Home app")
        }
    }

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        val target = File(filesDir, "wallpaper_${System.currentTimeMillis()}.jpg")
        val copied = runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (!copied) {
            state.notify("Could not read that photo")
            return@registerForActivityResult
        }
        prefs.wallpaperPhotoUri?.let { old ->
            runCatching {
                val oldFile = File(Uri.parse(old).path ?: return@runCatching)
                if (oldFile.exists() && oldFile.absolutePath != target.absolutePath) oldFile.delete()
            }
        }
        prefs.setWallpaperPhotoUri(Uri.fromFile(target).toString())
        prefs.setWallpaperId("photo")
        state.notify("Desktop picture updated")
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.onFailure {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        prefs.setFileRootUri(uri.toString())
        state.notify("Finder now browses that folder")
    }

    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        prefs.setCalendarEnabled(granted)
        state.notify(if (granted) "Calendar access granted" else "Calendar stays disabled — you can enable it later")
    }

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingMicAction
        pendingMicAction = null
        if (granted) {
            action?.invoke()
        } else {
            pendingVoice?.invoke(null)
            pendingVoice = null
            state.notify("Microphone permission is needed for voice input")
        }
    }

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        pendingVoice?.invoke(text)
        pendingVoice = null
    }

    private val writeSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        state.notify(
            if (hasWriteSettings()) "System brightness control enabled" else "System brightness permission not granted"
        )
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            state.refreshApps(force = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        state = LauncherState(this, prefs)
        state.isDefaultHome = isDefaultHomeForNow()

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (!prefs.setupComplete && !state.isDefaultHome) state.overlay = Overlay.SETUP

        tts = runCatching {
            TextToSpeech(this) { /* status ignored, speak() no-ops when unready */ }
        }.getOrNull()

        setContent {
            val systemDark = isSystemInDarkTheme()
            val dark = when (prefs.appearance) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            val glass = GlassStyle(
                dark = dark,
                tint = prefs.glassTint,
                blur = prefs.blurEnabled,
                blurRadius = androidx.compose.ui.unit.Dp(26f)
            )
            val accent = accentFor(prefs.accent).color
            val wallpaper = state.wallpaperSpec()

            GoldenGateTheme(glass = glass, accent = accent) {
                CompositionLocalProvider(
                    com.goldengate.desktop.core.LocalShell provides this@MainActivity,
                    LocalWallpaper provides wallpaper,
                    LocalRootSize provides state.desktopSize
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().background(Color.Black)
                    ) {
                        DesktopRoot(state = state, prefs = prefs)
                    }
                }
            }
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }

        state.refreshApps(force = true)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        state.overlay != Overlay.NONE && state.overlay != Overlay.SETUP -> state.dismissOverlay()
                        state.activeKey != null -> state.activeKey?.let { state.close(it) }
                        else -> Unit
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        state.isDefaultHome = isDefaultHomeForNow()
        state.refreshApps()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        runCatching { registerReceiver(packageReceiver, filter) }
    }

    override fun onPause() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onPause()
    }

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val command = event.isCtrlPressed || event.isMetaPressed
            val typing = state.overlay == Overlay.ASSISTANT || state.overlay == Overlay.SPOTLIGHT
            when {
                command && event.keyCode == KeyEvent.KEYCODE_SPACE -> {
                    state.overlay = if (state.overlay == Overlay.SPOTLIGHT) state.overlay else Overlay.SPOTLIGHT
                    return true
                }
                command && event.keyCode == KeyEvent.KEYCODE_TAB -> {
                    state.cycleWindows()
                    return true
                }
                command && event.keyCode == KeyEvent.KEYCODE_COMMA -> {
                    state.openSettingsTo(null)
                    return true
                }
                command && event.keyCode == KeyEvent.KEYCODE_S -> {
                    state.toggleOverlay(Overlay.ASSISTANT)
                    return true
                }
                event.keyCode == KeyEvent.KEYCODE_ESCAPE && state.overlay != Overlay.NONE -> {
                    state.dismissOverlay()
                    return true
                }
                !typing && event.keyCode == KeyEvent.KEYCODE_SEARCH -> {
                    state.overlay = Overlay.SPOTLIGHT
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ------------------------------------------------------------------ Shell
    override fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    state.isDefaultHome = true
                    prefs.setSetupComplete(true)
                    state.notify("Already set as your Home app")
                } else {
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                }
                return
            }
        }
        openHomeSettings()
    }

    override fun isDefaultHome(): Boolean = isDefaultHomeForNow()

    private fun isDefaultHomeForNow(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    override fun openHomeSettings() = Intents.openHomeSettings(this)

    override fun pickWallpaper() {
        runCatching {
            wallpaperPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }.onFailure { state.notify("Photo picker unavailable on this device") }
    }

    override fun pickFolder() = folderPicker.launch(null)

    override fun requestCalendarPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            calendarPermission.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    override fun requestMicPermission() = beginVoice()

    override fun openWriteSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeSettingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            )
        }
    }

    override fun hasWriteSettings(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this)

    override fun startVoiceInput(onResult: (String?) -> Unit) {
        pendingVoice = onResult
        beginVoice()
    }

    private fun beginVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingMicAction = { launchRecognizer() }
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        launchRecognizer()
    }

    private fun launchRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask the assistant")
        }
        val available = intent.resolveActivity(packageManager) != null
        if (!available) {
            pendingVoice?.invoke(null)
            pendingVoice = null
            state.notify("No speech recognition service is installed")
            return
        }
        runCatching { speechLauncher.launch(intent) }.onFailure {
            pendingVoice?.invoke(null)
            pendingVoice = null
            state.notify("Voice input is unavailable")
        }
    }

    override fun setSystemBrightness(percent: Int) {
        if (!hasWriteSettings()) {
            state.notify("Grant “Modify system settings” to change system brightness")
            openWriteSettings()
            return
        }
        runCatching {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (percent.coerceIn(1, 100) * 255 / 100)
            )
        }.onFailure { state.notify("This device blocks system brightness changes") }
    }

    override fun setAppBrightness(percent: Int) {
        window.attributes = window.attributes.apply {
            screenBrightness = (percent.coerceIn(2, 100) / 100f)
        }
    }

    override fun openUrl(url: String) {
        Intents.openUrl(this, url)
    }

    override fun launch(component: String): Boolean = Intents.launchComponent(this, component)

    override fun speak(text: String) {
        val engine = tts ?: return
        runCatching {
            engine.language = Locale.getDefault()
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant")
        }
    }
}
