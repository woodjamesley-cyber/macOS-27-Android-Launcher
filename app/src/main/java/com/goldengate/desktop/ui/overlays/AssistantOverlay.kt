package com.goldengate.desktop.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.data.AssistantAction
import com.goldengate.desktop.data.AssistantEngine
import com.goldengate.desktop.data.Note
import com.goldengate.desktop.data.NotesStore
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

private data class ChatMessage(val role: String, val content: String)

@Composable
fun AssistantOverlay(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(loadHistory(prefs)) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var speakReplies by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun persist() {
        prefs.setAssistantHistoryJson(
            AssistantEngine.historyToJson(messages.map { it.role to it.content })
        )
    }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || busy) return
        val updated = messages + ChatMessage("user", text)
        messages = updated
        input = ""
        persist()

        val local = AssistantEngine.local(text, state, prefs)
        if (local != null) {
            local.action?.let { applyAction(it, state, prefs) }
            messages = messages + ChatMessage("assistant", local.text)
            persist()
            if (speakReplies) shell.speak(local.text)
            return
        }

        if (prefs.assistantKey.isBlank()) {
            messages = messages + ChatMessage(
                "assistant",
                "I can only act on this device right now. To answer open questions, add an OpenAI-compatible endpoint and key in System Settings → Assistant — or ask me to open an app, search, take a note or change the wallpaper."
            )
            persist()
            return
        }

        busy = true
        scope.launch {
            val history = messages.dropLast(1).map { it.role to it.content }
            val result = AssistantEngine.cloud(text, prefs, history)
            val reply = result.getOrElse {
                "Cloud request failed: ${it.message ?: "unknown error"}. On-device commands still work."
            }
            messages = messages + ChatMessage("assistant", reply)
            persist()
            if (speakReplies) shell.speak(reply)
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        state.assistantSeed?.let { seed ->
            state.assistantSeed = null
            send(seed)
        }
    }

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(messages.lastIndex) }
        }
    }

    OverlayScrim(onDismiss = { state.dismissOverlay() }, alignment = Alignment.BottomEnd) {
        GlassPanel(
            modifier = Modifier
                .padding(end = 16.dp, bottom = 102.dp)
                .width(392.dp)
                .heightIn(min = 320.dp, max = 568.dp),
            shape = RoundedCornerShape(22.dp),
            elevation = 42.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF9BE7FF), Color(0xFF6C7BFF), Color(0xFFB14CFF))
                                )
                            )
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Assistant",
                            style = MaterialTheme.typography.titleSmall,
                            color = style.text,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (prefs.assistantKey.isBlank()) "On-device commands" else "On-device + your cloud AI",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = style.tertiary
                        )
                    }
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = if (speakReplies) "Mute spoken replies" else "Speak replies",
                        tint = if (speakReplies) MaterialTheme.colorScheme.primary else style.tertiary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable { speakReplies = !speakReplies }
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Clear conversation",
                        tint = style.tertiary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable {
                                messages = emptyList()
                                persist()
                            }
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close assistant",
                        tint = style.tertiary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable { state.dismissOverlay() }
                    )
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                "Ask me to open an app, find something in Files, take a quick note, change the wallpaper, or explain what this desktop can do. Type “help” for commands.",
                                style = MaterialTheme.typography.bodySmall,
                                color = style.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(style.field)
                                    .padding(10.dp)
                            )
                        }
                    }
                    itemsIndexed(messages) { _, message ->
                        val isUser = message.role == "user"
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Text(
                                message.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isUser) Color.White else style.text,
                                modifier = Modifier
                                    .widthMax(310.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else style.field
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                    if (busy) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(7.dp))
                                Text("Thinking…", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(style.field)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                            textStyle = TextStyle(color = style.text, fontSize = 13.sp),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (input.isEmpty()) Text("Ask the Assistant…", color = style.tertiary, fontSize = 13.sp)
                                inner()
                            }
                        )
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "Voice input",
                            tint = style.secondary,
                            modifier = Modifier
                                .size(17.dp)
                                .clickable {
                                    shell.startVoiceInput { heard ->
                                        if (heard != null) send(heard)
                                    }
                                }
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (busy) style.hairline else MaterialTheme.colorScheme.primary
                            )
                            .clickable(enabled = !busy) { send(input) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Send, "Send", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Not Siri. On-device by default; cloud answers only with your own configured key.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = style.tertiary
                )
            }
        }
    }
}

private fun Modifier.widthMax(max: androidx.compose.ui.unit.Dp): Modifier =
    this.widthIn(max = max)

private fun loadHistory(prefs: Prefs): List<ChatMessage> = runCatching {
    val array = JSONArray(prefs.assistantHistoryJson)
    (0 until array.length()).mapNotNull { index ->
        array.optJSONObject(index)?.let {
            ChatMessage(it.optString("role", "assistant"), it.optString("content", ""))
        }
    }.filter { it.content.isNotBlank() }
}.getOrElse { emptyList() }

private fun applyAction(action: AssistantAction, state: LauncherState, prefs: Prefs) {
    when (action) {
        is AssistantAction.OpenBuiltin -> state.openApp(action.app)
        is AssistantAction.OpenPackage -> state.launchPackage(action.component, action.label)
        is AssistantAction.Search -> {
            state.spotlightSeed = action.query
            state.overlay = com.goldengate.desktop.core.Overlay.SPOTLIGHT
        }
        is AssistantAction.AddNote -> {
            val notes = NotesStore.load(prefs.notesJson)
            val note = Note(
                id = UUID.randomUUID().toString().take(8),
                title = action.text.take(40).ifBlank { "Assistant Note" },
                body = action.text
            )
            prefs.setNotesJson(NotesStore.save(listOf(note) + notes))
            state.notify("Saved to Notes")
        }
        is AssistantAction.SetWallpaper -> prefs.setWallpaperId(action.id)
        AssistantAction.OpenSettings -> state.openSettingsTo(null)
        AssistantAction.ControlCenter -> state.overlay = com.goldengate.desktop.core.Overlay.CONTROL_CENTER
        AssistantAction.Launchpad -> state.overlay = com.goldengate.desktop.core.Overlay.LAUNCHPAD
    }
}

@Suppress("unused")
private val unusedBuiltin = BuiltinApp.FINDER
