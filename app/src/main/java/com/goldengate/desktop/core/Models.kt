package com.goldengate.desktop.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import org.json.JSONArray
import org.json.JSONObject

/** Apps that ship with the launcher. */
enum class AppKind { WINDOW, EXTERNAL, OVERLAY }

enum class BuiltinApp(
    val id: String,
    val title: String,
    val kind: AppKind,
    val subtitle: String
) {
    FINDER("finder", "Finder", AppKind.WINDOW, "Browse files and folders"),
    NOTES("notes", "Notes", AppKind.WINDOW, "Write, pin and search notes"),
    CALCULATOR("calculator", "Calculator", AppKind.WINDOW, "Standard calculator"),
    CALENDAR("calendar", "Calendar", AppKind.WINDOW, "Month grid and agenda"),
    CLOCK("clock", "Clock", AppKind.WINDOW, "World clock, timer, stopwatch"),
    WEATHER("weather", "Weather", AppKind.WINDOW, "Live forecast, no account needed"),
    ACTIVITY("activity", "Activity Monitor", AppKind.WINDOW, "Live device statistics"),
    SETTINGS("settings", "System Settings", AppKind.WINDOW, "Wallpaper, Dock, appearance"),
    PHOTOS("photos", "Photos", AppKind.EXTERNAL, "Opens your gallery app"),
    SAFARI("safari", "Safari", AppKind.EXTERNAL, "Opens your web browser"),
    LAUNCHPAD("launchpad", "Launchpad", AppKind.OVERLAY, "Every installed app");

    companion object {
        fun fromId(id: String): BuiltinApp? = entries.firstOrNull { it.id == id }
        val windowApps: List<BuiltinApp> get() = entries.filter { it.kind == AppKind.WINDOW }
    }
}

enum class ShortcutKind { BUILTIN, APP, FOLDER }

/**
 * A single desktop or Dock item. Stable across launches because it is persisted by [key].
 */
@Immutable
data class Shortcut(
    val key: String,
    val kind: ShortcutKind,
    val target: String,
    val label: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("key", key)
        put("kind", kind.name)
        put("target", target)
        put("label", label)
    }

    companion object {
        fun builtin(app: BuiltinApp) = Shortcut("builtin:${app.id}", ShortcutKind.BUILTIN, app.id)
        fun app(component: String, label: String) = Shortcut("app:$component", ShortcutKind.APP, component, label)
        fun folder(id: String, label: String = "") = Shortcut("folder:$id", ShortcutKind.FOLDER, id, label)
        fun fromJson(o: JSONObject): Shortcut? = runCatching {
            Shortcut(
                key = o.getString("key"),
                kind = ShortcutKind.valueOf(o.getString("kind")),
                target = o.getString("target"),
                label = o.optString("label", "")
            )
        }.getOrNull()
    }
}

@Immutable
data class InstalledApp(
    val label: String,
    val packageName: String,
    val component: String,
    val icon: ImageBitmap? = null
)

@Immutable
data class AppFolder(
    val id: String,
    val name: String,
    val shortcuts: List<Shortcut> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("shortcuts", JSONArray().also { arr -> shortcuts.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(o: JSONObject): AppFolder? = runCatching {
            val arr = o.optJSONArray("shortcuts") ?: JSONArray()
            AppFolder(
                id = o.getString("id"),
                name = o.getString("name"),
                shortcuts = (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(Shortcut::fromJson) }
            )
        }.getOrNull()
    }
}

enum class WidgetKind(val id: String, val title: String) {
    CLOCK("clock", "Clock"),
    CALENDAR("calendar", "Calendar"),
    WEATHER("weather", "Weather"),
    BATTERY("battery", "Battery"),
    NOTES("notes", "Notes"),
    ACTIVITY("activity", "Activity");

    companion object {
        fun fromId(id: String): WidgetKind = entries.firstOrNull { it.id == id } ?: CLOCK
    }
}

@Immutable
data class DesktopWidget(
    val id: String,
    val kind: WidgetKind,
    /** 0 = small, 1 = medium, 2 = large */
    val size: Int = 1,
    /** Fractional top-left position on the desktop surface. */
    val x: Float = 0.55f,
    val y: Float = 0.1f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("kind", kind.id)
        put("size", size)
        put("x", x.toDouble())
        put("y", y.toDouble())
    }

    companion object {
        fun fromJson(o: JSONObject): DesktopWidget? = runCatching {
            DesktopWidget(
                id = o.getString("id"),
                kind = WidgetKind.fromId(o.getString("kind")),
                size = o.optInt("size", 1),
                x = o.optDouble("x", 0.55).toFloat(),
                y = o.optDouble("y", 0.1).toFloat()
            )
        }.getOrNull()
    }
}

fun shortcutsToJson(list: List<Shortcut>): String =
    JSONArray().also { arr -> list.forEach { arr.put(it.toJson()) } }.toString()

fun shortcutsFromJson(raw: String?): List<Shortcut>? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(Shortcut::fromJson) }
    }.getOrNull()
}
