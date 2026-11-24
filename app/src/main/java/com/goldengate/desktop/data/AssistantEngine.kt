package com.goldengate.desktop.data

import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.wallpaper.WallpaperPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface AssistantAction {
    data class OpenBuiltin(val app: BuiltinApp) : AssistantAction
    data class OpenPackage(val component: String, val label: String) : AssistantAction
    data class Search(val query: String) : AssistantAction
    data class AddNote(val text: String) : AssistantAction
    data class SetWallpaper(val id: String, val label: String) : AssistantAction
    data object OpenSettings : AssistantAction
    data object ControlCenter : AssistantAction
    data object Launchpad : AssistantAction
}

data class AssistantReply(val text: String, val action: AssistantAction? = null, val source: String = "on-device")

/**
 * A launcher assistant, not Siri. It runs real commands on the device and can optionally talk to a
 * user-configured OpenAI-compatible endpoint for free-form answers.
 */
object AssistantEngine {

    private const val GUARDRAIL =
        "You are the assistant inside Golden Gate Desktop, a macOS-inspired Android launcher. " +
            "You are not Siri and must never claim to be Apple software or Apple Intelligence. " +
            "Answer briefly and helpfully."

    fun local(prompt: String, state: LauncherState, prefs: Prefs): AssistantReply? {
        val text = prompt.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase(Locale.getDefault())

        if (lower in setOf("hi", "hello", "hey", "hello there")) {
            return AssistantReply(
                "Hello. I can launch apps, find files, take notes and answer questions. Type “help” for the full list.",
                source = "on-device"
            )
        }

        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("run ")) {
            val target = text.substringAfter(' ').trim().removeSuffix("app").trim()
            if (target.isNotEmpty()) {
                BuiltinApp.entries.firstOrNull { it.title.equals(target, true) || it.id == target.lowercase() }
                    ?.let { return AssistantReply("Opening ${it.title}.", AssistantAction.OpenBuiltin(it)) }
                state.apps.firstOrNull { it.label.equals(target, true) }
                    ?.let {
                        return AssistantReply(
                            "Opening ${it.label}.",
                            AssistantAction.OpenPackage(it.component, it.label)
                        )
                    }
                state.apps.firstOrNull { it.label.contains(target, true) }
                    ?.let {
                        return AssistantReply(
                            "Opening ${it.label}.",
                            AssistantAction.OpenPackage(it.component, it.label)
                        )
                    }
                return AssistantReply("I could not find an app called “$target” on this device.")
            }
        }

        when {
            lower.contains("what time") || lower == "time" -> {
                return AssistantReply(
                    "It is " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "."
                )
            }
            lower.contains("what day") || lower.contains("today's date") || lower == "date" -> {
                return AssistantReply(
                    "Today is " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) + "."
                )
            }
            lower.contains("battery") -> {
                return AssistantReply(
                    "Battery is at ${state.batteryLevel}% and " +
                        if (state.charging) "charging." else "not charging."
                )
            }
            lower.startsWith("search ") || lower.startsWith("find ") -> {
                val query = text.substringAfter(' ').trim()
                if (query.isEmpty()) return AssistantReply("What should I search for?")
                return AssistantReply("Searching for “$query”.", AssistantAction.Search(query))
            }
            lower.startsWith("note ") || lower.startsWith("remember ") -> {
                val body = text.substringAfter(' ').trim()
                return AssistantReply("Saved to Notes.", AssistantAction.AddNote(body))
            }
            lower.startsWith("wallpaper") -> {
                val wanted = text.substringAfter(' ').trim()
                val match = WallpaperPresets.firstOrNull {
                    it.name.equals(wanted, true) || it.id.replace('_', ' ') == wanted.lowercase()
                } ?: WallpaperPresets.firstOrNull { it.name.lowercase().contains(wanted) }
                return if (match != null) {
                    AssistantReply("Setting the desktop picture to ${match.name}.", AssistantAction.SetWallpaper(match.id, match.name))
                } else {
                    AssistantReply("Setting the desktop picture to Golden Hour.", AssistantAction.SetWallpaper(WallpaperPresets.first().id, WallpaperPresets.first().name))
                }
            }
            lower.contains("settings") -> return AssistantReply("Opening System Settings.", AssistantAction.OpenSettings)
            lower.contains("control center") -> return AssistantReply("Here is Control Center.", AssistantAction.ControlCenter)
            lower.contains("launchpad") || lower.contains("app library") || lower.contains("all apps") ->
                return AssistantReply("Opening Launchpad.", AssistantAction.Launchpad)
            lower.contains("are you siri") || lower.contains("apple intelligence") ->
                return AssistantReply(
                    "No. I am an optional assistant built into this Android launcher. Siri and Apple Intelligence are Apple services and are not available on Android."
                )
            lower.contains("weather") -> return AssistantReply(
                "Opening Weather — it uses live Open-Meteo data for ${prefs.weatherCity}.",
                AssistantAction.OpenBuiltin(BuiltinApp.WEATHER)
            )
            lower == "help" || lower.contains("what can you do") -> {
                return AssistantReply(
                    "I work offline for launcher commands:\n" +
                        "• open <app> — launch any installed app\n" +
                        "• find <text> — Spotlight search\n" +
                        "• note <text> — save a note\n" +
                        "• wallpaper <name> — switch desktop picture\n" +
                        "• time, date, battery\n" +
                        "• settings, control center, launchpad\n\n" +
                        "Add an OpenAI-compatible endpoint and key in System Settings → Assistant for free-form answers."
                )
            }
        }
        return null
    }

    fun historyToJson(history: List<Pair<String, String>>): String {
        val array = JSONArray()
        history.forEach { (role, content) ->
            array.put(JSONObject().put("role", role).put("content", content))
        }
        return array.toString()
    }

    /**
     * Sends the conversation to the user's own configured endpoint. The key never leaves the
     * device except in the Authorization header of that request, and nothing is sent when unset.
     */
    suspend fun cloud(
        prompt: String,
        prefs: Prefs,
        history: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = prefs.assistantKey
            val endpoint = prefs.assistantEndpoint
            if (key.isBlank()) throw IllegalStateException("No API key configured")
            if (!endpoint.startsWith("https://")) throw IllegalStateException("Endpoint must use https://")

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", GUARDRAIL))
            history.takeLast(8).forEach { (role, content) ->
                messages.put(JSONObject().put("role", role).put("content", content))
            }
            messages.put(JSONObject().put("role", "user").put("content", prompt))

            val body = JSONObject()
                .put("model", prefs.assistantModel.ifBlank { "gpt-4o-mini" })
                .put("messages", messages)
                .put("temperature", 0.6)
                .toString()

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $key")
            }
            connection.outputStream.use { it.write(body.toByteArray()) }
            val responseText = (if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText() } ?: ""
            val code = connection.responseCode
            connection.disconnect()
            if (code !in 200..299) {
                val detail = runCatching {
                    JSONObject(responseText).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IllegalStateException(detail ?: "Request failed with HTTP $code")
            }
            val json = JSONObject(responseText)
            json.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The endpoint returned no message")
        }
    }
}
