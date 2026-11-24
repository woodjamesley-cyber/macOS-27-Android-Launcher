package com.goldengate.desktop.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.data.Note
import com.goldengate.desktop.data.NotesStore
import com.goldengate.desktop.ui.components.DividerLine
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun NotesApp(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    var notes by remember {
        mutableStateOf(
            NotesStore.load(prefs.notesJson).ifEmpty { NotesStore.seed() }
        )
    }
    var selectedId by remember { mutableStateOf<String?>(notes.firstOrNull()?.id) }
    var query by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    fun persist(list: List<Note>) {
        notes = list
        prefs.setNotesJson(NotesStore.save(list))
    }

    val selected = notes.firstOrNull { it.id == selectedId }

    LaunchedEffect(selectedId) {
        title = selected?.title ?: ""
        body = selected?.body ?: ""
    }

    LaunchedEffect(title, body, selectedId) {
        val id = selectedId ?: return@LaunchedEffect
        delay(300)
        val current = notes.firstOrNull { it.id == id } ?: return@LaunchedEffect
        if (current.title != title || current.body != body) {
            persist(
                notes.map {
                    if (it.id == id) it.copy(
                        title = title.ifBlank { "Untitled Note" },
                        body = body,
                        updated = System.currentTimeMillis()
                    ) else it
                }
            )
        }
    }

    LaunchedEffect(state.command) {
        val command = state.command ?: return@LaunchedEffect
        if (command.app != BuiltinApp.NOTES) return@LaunchedEffect
        when (command.action) {
            "new_note" -> {
                val note = Note(UUID.randomUUID().toString().take(8), "New Note", "")
                persist(listOf(note) + notes)
                selectedId = note.id
            }
            "delete_note" -> selectedId?.let { id ->
                persist(notes.filterNot { it.id == id })
                selectedId = notes.firstOrNull { it.id != id }?.id
            }
            "open_note" -> command.payload?.let { selectedId = it }
        }
    }

    val filtered = notes
        .filter { it.pinned }
        .plus(notes.filterNot { it.pinned })
        .filter {
            query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true)
        }

    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 640.dp
        Row(Modifier.fillMaxSize()) {
            if (wide || selectedId == null) {
                Column(
                    Modifier
                        .then(if (wide) Modifier.width(230.dp) else Modifier.fillMaxWidth())
                        .fillMaxHeight()
                        .background(style.field.copy(alpha = 0.4f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = style.text,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(style.field)
                                .clickable {
                                    val note = Note(UUID.randomUUID().toString().take(8), "New Note", "")
                                    persist(listOf(note) + notes)
                                    selectedId = note.id
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, "New note", tint = style.text, modifier = Modifier.size(16.dp))
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(style.field)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = style.tertiary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = style.text, fontSize = 12.sp),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (query.isEmpty()) Text("Search notes", color = style.tertiary, fontSize = 12.sp)
                                inner()
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        items(filtered, key = { it.id }) { note ->
                            NoteRow(
                                note = note,
                                selected = note.id == selectedId,
                                onClick = { selectedId = note.id }
                            )
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    "No notes yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = style.tertiary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
                if (wide) Box(Modifier.width(0.6.dp).fillMaxHeight().background(style.divider))
            }

            if (wide || selectedId != null) {
                Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = style.text,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (title.isEmpty()) Text("Title", color = style.tertiary, fontSize = 22.sp)
                                inner()
                            }
                        )
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = if (selected?.pinned == true) "Unpin note" else "Pin note",
                            tint = if (selected?.pinned == true) Color(0xFFFFD60A) else style.tertiary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    val id = selectedId ?: return@clickable
                                    persist(notes.map { if (it.id == id) it.copy(pinned = !it.pinned) else it })
                                }
                                .padding(2.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete note",
                            tint = style.secondary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    val id = selectedId ?: return@clickable
                                    val remaining = notes.filterNot { it.id == id }
                                    persist(remaining)
                                    selectedId = remaining.firstOrNull()?.id
                                }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        selected?.updated?.let {
                            "Edited " + DateTimeFormatter.ofPattern("d MMM, HH:mm")
                                .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                        } ?: "New note",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary
                    )
                    Spacer(Modifier.height(10.dp))
                    DividerLine()
                    Spacer(Modifier.height(10.dp))
                    BasicTextField(
                        value = body,
                        onValueChange = { body = it },
                        textStyle = TextStyle(color = style.text, fontSize = 15.sp, lineHeight = 22.sp),
                        modifier = Modifier.fillMaxSize(),
                        decorationBox = { inner ->
                            if (body.isEmpty()) {
                                Text(
                                    "Start writing. Notes are stored only on this device.",
                                    color = style.tertiary,
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        }
                    )
                    if (!wide) {
                        Spacer(Modifier.height(8.dp))
                        PillButton("All Notes") { selectedId = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, selected: Boolean, onClick: () -> Unit) {
    val style = LocalGlassStyle.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) style.hairline else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.pinned) {
                Icon(
                    Icons.Rounded.PushPin,
                    null,
                    tint = Color(0xFFFFD60A),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                note.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = style.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            note.body.replace('\n', ' ').ifBlank { "No additional text" },
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Suppress("unused")
private val unusedArrangement = Arrangement.Top
