package com.goldengate.desktop.ui.overlays

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Language
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.goldengate.desktop.core.AppKind
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.InstalledApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.data.Note
import com.goldengate.desktop.data.NotesStore
import com.goldengate.desktop.ui.components.AndroidAppIcon
import com.goldengate.desktop.ui.components.BuiltinIcon
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.util.Intents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class FileHit(val name: String, val uri: String, val mime: String?)

private sealed interface SpotlightRow {
    data class Builtin(val app: BuiltinApp) : SpotlightRow
    data class Installed(val app: InstalledApp) : SpotlightRow
    data class NoteRow(val note: Note) : SpotlightRow
    data class FileRow(val hit: FileHit) : SpotlightRow
    data class Web(val query: String) : SpotlightRow
    data class Ask(val query: String) : SpotlightRow
}

@Composable
fun SpotlightOverlay(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf(state.spotlightSeed ?: "") }
    var files by remember { mutableStateOf<List<FileHit>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        state.spotlightSeed = null
        focusRequester.requestFocus()
    }

    LaunchedEffect(query) {
        state.spotlightSeed = query
        if (query.length < 2) {
            files = emptyList()
            return@LaunchedEffect
        }
        val rootUri = prefs.fileRootUri
        files = withContext(Dispatchers.IO) {
            if (rootUri == null) emptyList() else searchFiles(context, rootUri, query)
        }
    }

    val notes = remember(query, prefs.notesJson) {
        if (query.isBlank()) emptyList()
        else NotesStore.load(prefs.notesJson).filter {
            it.title.contains(query, true) || it.body.contains(query, true)
        }.take(3)
    }
    val builtins = remember(query) {
        if (query.isBlank()) BuiltinApp.entries.filter { it.kind == AppKind.WINDOW }
        else BuiltinApp.entries.filter { it.title.contains(query, true) }
    }
    val apps = remember(query, state.apps) {
        if (query.isBlank()) state.apps.take(6)
        else state.apps.filter { it.label.contains(query, true) }.take(8)
    }

    val rows = buildList<SpotlightRow> {
        apps.forEach { add(SpotlightRow.Installed(it)) }
        builtins.forEach { add(SpotlightRow.Builtin(it)) }
        notes.forEach { add(SpotlightRow.NoteRow(it)) }
        files.forEach { add(SpotlightRow.FileRow(it)) }
        if (query.isNotBlank()) {
            add(SpotlightRow.Web(query))
            add(SpotlightRow.Ask(query))
        }
    }

    fun activate(row: SpotlightRow) {
        when (row) {
            is SpotlightRow.Builtin -> state.openApp(row.app)
            is SpotlightRow.Installed -> state.launchPackage(row.app.component, row.app.label)
            is SpotlightRow.NoteRow -> {
                state.openWindow(BuiltinApp.NOTES)
                state.sendCommand(BuiltinApp.NOTES, "open_note", row.note.id)
            }
            is SpotlightRow.FileRow -> Intents.openFile(context, Uri.parse(row.hit.uri), row.hit.mime)
            is SpotlightRow.Web -> shell.openUrl("https://duckduckgo.com/?q=" + Uri.encode(row.query))
            is SpotlightRow.Ask -> {
                state.assistantSeed = row.query
                state.overlay = com.goldengate.desktop.core.Overlay.ASSISTANT
            }
        }
        if (row !is SpotlightRow.Ask) state.dismissOverlay()
    }

    OverlayScrim(onDismiss = { state.dismissOverlay() }, alignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth
            GlassPanel(
                modifier = Modifier
                    .padding(top = 72.dp)
                    .width(if (width < 640.dp) width - 24.dp else 620.dp)
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = 40.dp
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = style.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it; selectedIndex = 0 },
                            textStyle = TextStyle(color = style.text, fontSize = 19.sp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.key) {
                                            Key.DirectionDown -> {
                                                selectedIndex = (selectedIndex + 1).coerceAtMost(rows.lastIndex.coerceAtLeast(0))
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                                true
                                            }
                                            Key.Enter -> {
                                                rows.getOrNull(selectedIndex)?.let(::activate)
                                                true
                                            }
                                            Key.Escape -> {
                                                state.dismissOverlay()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text("Search or Ask", color = style.tertiary, fontSize = 19.sp)
                                }
                                inner()
                            }
                        )
                        Text("esc", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                    }
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(style.divider))
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(rows, key = { it.toString() }) { row ->
                            val index = rows.indexOf(row)
                            SpotlightResultRow(
                                row = row,
                                selected = index == selectedIndex,
                                onClick = { activate(row) }
                            )
                        }
                        if (rows.isEmpty()) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                                    Text("No results", style = MaterialTheme.typography.bodyMedium, color = style.text)
                                    Text(
                                        "Try a different name, or open Assistant to ask a question.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = style.tertiary
                                    )
                                }
                            }
                        }
                        item {
                            Text(
                                if (prefs.fileRootUri == null) {
                                    "File search needs a folder: Settings → Finder → Choose folder"
                                } else {
                                    "Searches apps, built-in apps, notes and the folder you granted to Finder."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = style.tertiary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightResultRow(row: SpotlightRow, selected: Boolean, onClick: () -> Unit) {
    val style = LocalGlassStyle.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) style.hairline else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (row) {
            is SpotlightRow.Builtin -> {
                BuiltinIcon(row.app, 28.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.app.title, style = MaterialTheme.typography.bodyMedium, color = style.text)
                    Text("Built-in app", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                }
            }
            is SpotlightRow.Installed -> {
                AndroidAppIcon(row.app.icon, 28.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        row.app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = style.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Application", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                }
            }
            is SpotlightRow.NoteRow -> {
                Icon(Icons.Rounded.Description, null, tint = Color(0xFFFFD60A), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.note.title, style = MaterialTheme.typography.bodyMedium, color = style.text)
                    Text(
                        row.note.body.take(70),
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is SpotlightRow.FileRow -> {
                Icon(Icons.Rounded.InsertDriveFile, null, tint = style.secondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.hit.name, style = MaterialTheme.typography.bodyMedium, color = style.text)
                    Text("File", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                }
            }
            is SpotlightRow.Web -> {
                Icon(Icons.Rounded.Language, null, tint = Color(0xFF64D2FF), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Search the web for “${row.query}”", style = MaterialTheme.typography.bodyMedium, color = style.text)
                    Text("Opens your browser", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                }
            }
            is SpotlightRow.Ask -> {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFBF5AF2), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ask the Assistant", style = MaterialTheme.typography.bodyMedium, color = style.text)
                    Text("On-device commands, optional cloud AI", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                }
            }
        }
        if (selected) {
            Text("↵", style = MaterialTheme.typography.labelMedium, color = style.tertiary)
        }
    }
}

private fun searchFiles(
    context: android.content.Context,
    rootUri: String,
    query: String
): List<FileHit> {
    val root = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(rootUri)) }.getOrNull()
        ?: return emptyList()
    val results = mutableListOf<FileHit>()
    fun walk(dir: DocumentFile, depth: Int) {
        if (depth > 4 || results.size >= 8) return
        val children = runCatching { dir.listFiles() }.getOrElse { return }
        for (child in children) {
            if (results.size >= 8) return
            val name = child.name ?: continue
            if (!child.isDirectory && name.contains(query, true)) {
                results += FileHit(name, child.uri.toString(), child.type)
            } else if (child.isDirectory) {
                walk(child, depth + 1)
            }
        }
    }
    walk(root, 0)
    return results
}

@Suppress("unused")
private val unusedArrangement = Arrangement.Top
@Suppress("unused")
private val unusedFontWeight = FontWeight.Normal
