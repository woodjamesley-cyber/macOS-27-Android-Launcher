package com.goldengate.desktop.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Single source of truth for user preferences. Values are exposed as Compose state so the whole
 * desktop recomposes when a setting changes, and every setter writes through to SharedPreferences.
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("golden_gate_prefs", Context.MODE_PRIVATE)

    private fun str(key: String, def: String) = sp.getString(key, def) ?: def
    private fun bool(key: String, def: Boolean) = sp.getBoolean(key, def)
    private fun flt(key: String, def: Float) = sp.getFloat(key, def)
    private fun integer(key: String, def: Int) = sp.getInt(key, def)
    private fun put(key: String, value: String) = sp.edit().putString(key, value).apply()
    private fun put(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    private fun put(key: String, value: Float) = sp.edit().putFloat(key, value).apply()
    private fun put(key: String, value: Int) = sp.edit().putInt(key, value).apply()

    // ---------------------------------------------------------------- appearance
    var appearance by mutableStateOf(str("appearance", "dark"))
    @JvmName("saveAppearance")
    fun setAppearance(v: String) { appearance = v; put("appearance", v) }

    var glassTint by mutableStateOf(flt("glass_tint", 0.42f))
    @JvmName("saveGlassTint")
    fun setGlassTint(v: Float) { glassTint = v; put("glass_tint", v) }

    var blurEnabled by mutableStateOf(bool("blur_enabled", true))
    @JvmName("saveBlurEnabled")
    fun setBlurEnabled(v: Boolean) { blurEnabled = v; put("blur_enabled", v) }

    var accent by mutableStateOf(str("accent", "blue"))
    @JvmName("saveAccent")
    fun setAccent(v: String) { accent = v; put("accent", v) }

    var reduceMotion by mutableStateOf(bool("reduce_motion", false))
    @JvmName("saveReduceMotion")
    fun setReduceMotion(v: Boolean) { reduceMotion = v; put("reduce_motion", v) }

    var windowAnimations by mutableStateOf(bool("window_animations", true))
    @JvmName("saveWindowAnimations")
    fun setWindowAnimations(v: Boolean) { windowAnimations = v; put("window_animations", v) }

    // ---------------------------------------------------------------- wallpaper
    var wallpaperId by mutableStateOf(str("wallpaper_id", "golden_hour"))
    @JvmName("saveWallpaperId")
    fun setWallpaperId(v: String) { wallpaperId = v; put("wallpaper_id", v) }

    var wallpaperPhotoUri by mutableStateOf(sp.getString("wallpaper_uri", null))
    @JvmName("saveWallpaperPhotoUri")
    fun setWallpaperPhotoUri(v: String?) {
        wallpaperPhotoUri = v
        sp.edit().apply { if (v == null) remove("wallpaper_uri") else putString("wallpaper_uri", v) }.apply()
    }

    // ---------------------------------------------------------------- desktop + dock
    var desktopIconSize by mutableStateOf(integer("desktop_icon_size", 1))
    @JvmName("saveDesktopIconSize")
    fun setDesktopIconSize(v: Int) { desktopIconSize = v; put("desktop_icon_size", v) }

    var dockIconSize by mutableStateOf(integer("dock_icon_size", 1))
    @JvmName("saveDockIconSize")
    fun setDockIconSize(v: Int) { dockIconSize = v; put("dock_icon_size", v) }

    var dockMagnification by mutableStateOf(bool("dock_magnify", true))
    @JvmName("saveDockMagnification")
    fun setDockMagnification(v: Boolean) { dockMagnification = v; put("dock_magnify", v) }

    var dockAutoHide by mutableStateOf(bool("dock_autohide", false))
    @JvmName("saveDockAutoHide")
    fun setDockAutoHide(v: Boolean) { dockAutoHide = v; put("dock_autohide", v) }

    var showDesktopIcons by mutableStateOf(bool("show_desktop_icons", true))
    @JvmName("saveShowDesktopIcons")
    fun setShowDesktopIcons(v: Boolean) { showDesktopIcons = v; put("show_desktop_icons", v) }

    var showDesktopWidgets by mutableStateOf(bool("show_desktop_widgets", true))
    @JvmName("saveShowDesktopWidgets")
    fun setShowDesktopWidgets(v: Boolean) { showDesktopWidgets = v; put("show_desktop_widgets", v) }

    var iconLabels by mutableStateOf(bool("icon_labels", true))
    @JvmName("saveIconLabels")
    fun setIconLabels(v: Boolean) { iconLabels = v; put("icon_labels", v) }

    var menuBarSeconds by mutableStateOf(bool("menu_bar_seconds", false))
    @JvmName("saveMenuBarSeconds")
    fun setMenuBarSeconds(v: Boolean) { menuBarSeconds = v; put("menu_bar_seconds", v) }

    var dockItems by mutableStateOf(loadShortcuts("dock_items") ?: defaultDock())
    @JvmName("saveDockItems")
    fun setDockItems(v: List<Shortcut>) { dockItems = v; put("dock_items", shortcutsToJson(v)) }

    var desktopItems by mutableStateOf(loadShortcuts("desktop_items") ?: defaultDesktop())
    @JvmName("saveDesktopItems")
    fun setDesktopItems(v: List<Shortcut>) { desktopItems = v; put("desktop_items", shortcutsToJson(v)) }

    var folders by mutableStateOf(loadFolders())
        private set

    @JvmName("saveFolders")
    fun setFolders(v: List<AppFolder>) {
        folders = v
        put("folders", JSONArray().also { arr -> v.forEach { arr.put(it.toJson()) } }.toString())
    }

    fun createFolder(name: String, shortcuts: List<Shortcut> = emptyList()): AppFolder {
        val folder = AppFolder(UUID.randomUUID().toString().take(8), name, shortcuts)
        setFolders(folders + folder)
        return folder
    }

    fun updateFolder(folder: AppFolder) = setFolders(folders.map { if (it.id == folder.id) folder else it })
    fun deleteFolder(id: String) = setFolders(folders.filterNot { it.id == id })
    fun folder(id: String): AppFolder? = folders.firstOrNull { it.id == id }

    var widgets by mutableStateOf(loadWidgets() ?: defaultWidgets())
    @JvmName("saveWidgets")
    fun setWidgets(v: List<DesktopWidget>) {
        widgets = v
        put("widgets", JSONArray().also { arr -> v.forEach { arr.put(it.toJson()) } }.toString())
    }

    fun addWidget(kind: WidgetKind) {
        val w = DesktopWidget(
            id = UUID.randomUUID().toString().take(8),
            kind = kind,
            size = 1,
            x = (0.42f + (widgets.size % 3) * 0.06f).coerceIn(0.02f, 0.7f),
            y = (0.12f + (widgets.size % 4) * 0.16f).coerceIn(0.05f, 0.7f)
        )
        setWidgets(widgets + w)
    }

    // ---------------------------------------------------------------- setup + permissions
    var setupComplete by mutableStateOf(bool("setup_complete", false))
    @JvmName("saveSetupComplete")
    fun setSetupComplete(v: Boolean) { setupComplete = v; put("setup_complete", v) }

    var calendarEnabled by mutableStateOf(bool("calendar_enabled", false))
    @JvmName("saveCalendarEnabled")
    fun setCalendarEnabled(v: Boolean) { calendarEnabled = v; put("calendar_enabled", v) }

    var fileRootUri by mutableStateOf(sp.getString("file_root_uri", null))
    @JvmName("saveFileRootUri")
    fun setFileRootUri(v: String?) {
        fileRootUri = v
        sp.edit().apply { if (v == null) remove("file_root_uri") else putString("file_root_uri", v) }.apply()
    }

    var finderView by mutableStateOf(str("finder_view", "grid"))
    @JvmName("saveFinderView")
    fun setFinderView(v: String) { finderView = v; put("finder_view", v) }

    // ---------------------------------------------------------------- notes
    var notesJson by mutableStateOf(str("notes_json", "[]"))
    @JvmName("saveNotesJson")
    fun setNotesJson(v: String) { notesJson = v; put("notes_json", v) }

    // ---------------------------------------------------------------- assistant
    var assistantEndpoint by mutableStateOf(str("assistant_endpoint", "https://api.openai.com/v1/chat/completions"))
    @JvmName("saveAssistantEndpoint")
    fun setAssistantEndpoint(v: String) { assistantEndpoint = v; put("assistant_endpoint", v) }

    var assistantModel by mutableStateOf(str("assistant_model", "gpt-4o-mini"))
    @JvmName("saveAssistantModel")
    fun setAssistantModel(v: String) { assistantModel = v; put("assistant_model", v) }

    /** Stored only in this app's private SharedPreferences on the device. Never bundled with the app. */
    var assistantKey by mutableStateOf(str("assistant_key", ""))
        private set
    @JvmName("saveAssistantKey")
    fun setAssistantKey(v: String) { assistantKey = v; put("assistant_key", v) }

    var assistantHistoryJson by mutableStateOf(str("assistant_history", "[]"))
    @JvmName("saveAssistantHistoryJson")
    fun setAssistantHistoryJson(v: String) { assistantHistoryJson = v; put("assistant_history", v) }

    // ---------------------------------------------------------------- weather
    var weatherCity by mutableStateOf(str("weather_city", "San Francisco"))
    @JvmName("saveWeatherCity")
    fun setWeatherCity(v: String) { weatherCity = v; put("weather_city", v) }

    var weatherUnit by mutableStateOf(str("weather_unit", "c"))
    @JvmName("saveWeatherUnit")
    fun setWeatherUnit(v: String) { weatherUnit = v; put("weather_unit", v) }

    // ---------------------------------------------------------------- helpers
    private fun loadShortcuts(key: String): List<Shortcut>? = shortcutsFromJson(sp.getString(key, null))

    private fun loadFolders(): List<AppFolder> = runCatching {
        val raw = sp.getString("folders", null) ?: return@runCatching defaultFolders()
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(AppFolder::fromJson) }
    }.getOrNull() ?: defaultFolders()

    private fun loadWidgets(): List<DesktopWidget>? = runCatching {
        val raw = sp.getString("widgets", null) ?: return null
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(DesktopWidget::fromJson) }
    }.getOrNull()

    private fun defaultDock(): List<Shortcut> = listOf(
        Shortcut.builtin(BuiltinApp.FINDER),
        Shortcut.builtin(BuiltinApp.SAFARI),
        Shortcut.builtin(BuiltinApp.NOTES),
        Shortcut.builtin(BuiltinApp.CALENDAR),
        Shortcut.builtin(BuiltinApp.PHOTOS),
        Shortcut.builtin(BuiltinApp.WEATHER),
        Shortcut.builtin(BuiltinApp.ACTIVITY),
        Shortcut.builtin(BuiltinApp.SETTINGS)
    )

    private fun defaultDesktop(): List<Shortcut> = listOf(
        Shortcut.builtin(BuiltinApp.FINDER),
        Shortcut("folder:tools", ShortcutKind.FOLDER, "tools", "Tools"),
        Shortcut.builtin(BuiltinApp.NOTES),
        Shortcut.builtin(BuiltinApp.SETTINGS)
    )

    private fun defaultFolders(): List<AppFolder> = listOf(
        AppFolder(
            id = "tools",
            name = "Tools",
            shortcuts = listOf(
                Shortcut.builtin(BuiltinApp.CALCULATOR),
                Shortcut.builtin(BuiltinApp.CLOCK),
                Shortcut.builtin(BuiltinApp.ACTIVITY),
                Shortcut.builtin(BuiltinApp.WEATHER)
            )
        )
    )

    private fun defaultWidgets(): List<DesktopWidget> = listOf(
        DesktopWidget("w_clock", WidgetKind.CLOCK, 1, 0.56f, 0.10f),
        DesktopWidget("w_weather", WidgetKind.WEATHER, 1, 0.70f, 0.10f),
        DesktopWidget("w_calendar", WidgetKind.CALENDAR, 1, 0.56f, 0.36f),
        DesktopWidget("w_battery", WidgetKind.BATTERY, 0, 0.70f, 0.36f)
    )

    fun resetLayout() {
        setDesktopItems(defaultDesktop())
        setDockItems(defaultDock())
        setWidgets(defaultWidgets())
        setFolders(defaultFolders())
    }

    fun exportJson(): String = JSONObject().apply {
        sp.all.forEach { (k, v) ->
            when (v) {
                is String -> put(k, v)
                is Boolean -> put(k, v)
                is Int -> put(k, v)
                is Float -> put(k, v.toDouble())
                is Long -> put(k, v)
            }
        }
    }.toString()
}
