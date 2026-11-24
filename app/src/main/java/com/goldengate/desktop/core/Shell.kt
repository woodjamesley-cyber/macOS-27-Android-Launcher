package com.goldengate.desktop.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Everything that needs an Activity (permissions, SAF pickers, role requests, speech, brightness)
 * goes through this bridge so the composables stay testable and free of Activity lookups.
 */
interface Shell {
    fun requestHomeRole()
    fun isDefaultHome(): Boolean
    fun openHomeSettings()
    fun pickWallpaper()
    fun pickFolder()
    fun requestCalendarPermission()
    fun requestMicPermission()
    fun openWriteSettings()
    fun hasWriteSettings(): Boolean
    fun startVoiceInput(onResult: (String?) -> Unit)
    fun setSystemBrightness(percent: Int)
    fun setAppBrightness(percent: Int)
    fun openUrl(url: String)
    fun launch(component: String): Boolean
    fun speak(text: String)
}

val LocalShell = staticCompositionLocalOf<Shell> {
    error("Shell was not provided")
}
