package com.goldengate.desktop.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val updated: Long = System.currentTimeMillis(),
    val pinned: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("body", body)
        put("updated", updated)
        put("pinned", pinned)
    }

    companion object {
        fun fromJson(o: JSONObject): Note? = runCatching {
            Note(
                id = o.getString("id"),
                title = o.optString("title", "New Note"),
                body = o.optString("body", ""),
                updated = o.optLong("updated", System.currentTimeMillis()),
                pinned = o.optBoolean("pinned", false)
            )
        }.getOrNull()
    }
}

object NotesStore {

    fun load(raw: String): List<Note> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(Note::fromJson) }
    }.getOrElse { emptyList() }

    fun save(notes: List<Note>): String =
        JSONArray().also { arr -> notes.forEach { arr.put(it.toJson()) } }.toString()

    fun seed(): List<Note> = listOf(
        Note(
            id = "welcome",
            title = "Welcome to Golden Gate Desktop",
            body = "This desktop is a native Android home screen, not a mockup.\n\n" +
                "• Long-press a Dock icon to rearrange or remove it.\n" +
                "• Open Spotlight with Ctrl+Space or the menu-bar search glyph.\n" +
                "• Windows can be dragged by their title bar and resized from the bottom-right corner.\n" +
                "• System Settings → Desktop controls icons, widgets and folders.\n\n" +
                "Everything here is stored on this device only.",
            pinned = true
        ),
        Note(
            id = "shortcuts",
            title = "Keyboard shortcuts",
            body = "Ctrl/⌘ + Space — Spotlight\nCtrl/⌘ + Tab — cycle windows\nCtrl/⌘ + S — Assistant\nCtrl/⌘ + , — Settings\nEsc — close the front panel\nSearch key — Spotlight"
        )
    )
}
