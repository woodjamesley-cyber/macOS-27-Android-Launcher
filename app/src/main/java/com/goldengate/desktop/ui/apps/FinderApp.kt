package com.goldengate.desktop.ui.apps

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.components.ActionRow
import com.goldengate.desktop.ui.components.DividerLine
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.components.PanelTitle
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.components.SidebarItem
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.util.Intents
import com.goldengate.desktop.util.fileKindLabel
import com.goldengate.desktop.util.formatBytes
import com.goldengate.desktop.util.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private class FinderEntry(val file: DocumentFile) {
    val name: String get() = file.name ?: "Untitled"
    val isDir: Boolean get() = file.isDirectory
    val size: Long get() = file.length()
    val modified: Long get() = file.lastModified()
    val mime: String? get() = file.type
    val key: String get() = file.uri.toString()
}

private fun iconFor(entry: FinderEntry): ImageVector {
    if (entry.isDir) return Icons.Rounded.Folder
    val mime = entry.mime ?: ""
    val ext = entry.name.substringAfterLast('.', "").lowercase()
    return when {
        mime.startsWith("image/") -> Icons.Rounded.Image
        mime.startsWith("video/") -> Icons.Rounded.VideoFile
        mime.startsWith("audio/") -> Icons.Rounded.MusicNote
        mime == "application/pdf" -> Icons.Rounded.PictureAsPdf
        ext in setOf("txt", "md", "json", "xml", "kt", "java", "js", "html", "css", "py") -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }
}

@Composable
fun FinderApp(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shell = LocalShell.current
    val style = LocalGlassStyle.current
    val scope = rememberCoroutineScope()

    val appDocs = remember { File(context.filesDir, "Documents").apply { mkdirs() } }
    var stack by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var entries by remember { mutableStateOf<List<FinderEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Name") }
    var ascending by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<FinderEntry?>(null) }
    var refreshTick by remember { mutableStateOf(0) }
    var newFolderOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FinderEntry?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }

    val root: DocumentFile? = remember(prefs.fileRootUri) {
        prefs.fileRootUri?.let { uri ->
            runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uri)) }.getOrNull()
        }
    }
    val documentsRoot = remember { DocumentFile.fromFile(appDocs) }

    LaunchedEffect(prefs.fileRootUri) {
        val start = root ?: documentsRoot
        stack = listOf(start)
        selected = null
    }

    val current = stack.lastOrNull()

    LaunchedEffect(current, refreshTick) {
        val dir = current ?: return@LaunchedEffect
        loading = true
        entries = withContext(Dispatchers.IO) {
            runCatching { dir.listFiles().map { FinderEntry(it) } }.getOrElse { emptyList() }
        }
        loading = false
    }

    LaunchedEffect(state.command) {
        val command = state.command ?: return@LaunchedEffect
        if (command.app != BuiltinApp.FINDER) return@LaunchedEffect
        when (command.action) {
            "new_folder" -> newFolderOpen = true
            "choose_folder" -> shell.pickFolder()
            "refresh" -> refreshTick++
        }
    }

    val sorted = entries
        .filter { showHidden || !it.name.startsWith(".") }
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedWith(
            (when (sortMode) {
                "Date" -> compareBy<FinderEntry> { it.modified }
                "Size" -> compareBy<FinderEntry> { it.size }
                "Kind" -> compareBy<FinderEntry> { it.name.substringAfterLast('.', "") }
                else -> compareBy<FinderEntry> { it.name.lowercase() }
            }.let { comparator -> if (ascending) comparator else comparator.reversed() })
                .thenBy { entry -> !entry.isDir }
        )

    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 700.dp
        Row(Modifier.fillMaxSize()) {
            if (wide && current != null) {
                Column(
                    Modifier
                        .width(184.dp)
                        .fillMaxHeight()
                        .background(style.field.copy(alpha = 0.45f))
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    PanelTitle("Favourites", Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                    SidebarItem(
                        label = "Documents",
                        icon = Icons.Rounded.Folder,
                        selected = current.uri == documentsRoot.uri,
                        onClick = {
                            stack = listOf(documentsRoot)
                            selected = null
                        }
                    )
                    if (root != null) {
                        SidebarItem(
                            label = root.name ?: "Chosen folder",
                            icon = Icons.Rounded.Folder,
                            selected = current.uri == root.uri,
                            onClick = {
                                stack = listOf(root)
                                selected = null
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    PanelTitle("Locations", Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                    ActionRow("Choose folder…", icon = Icons.Rounded.Add) { shell.pickFolder() }
                    ActionRow("Refresh", icon = Icons.Rounded.Refresh) { refreshTick++ }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${entries.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                DividerVertical()
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                // ----------------------------------------------- toolbar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", enabled = stack.size > 1) {
                        if (stack.size > 1) {
                            stack = stack.dropLast(1)
                            selected = null
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(style.field)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, null, tint = style.tertiary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = style.text, fontSize = 13.sp),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) {
                                        Text("Search folder", color = style.tertiary, fontSize = 13.sp)
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                    ToolIcon(Icons.Rounded.CreateNewFolder, "New folder") {
                        folderName = "Untitled Folder"
                        newFolderOpen = true
                    }
                    ToolIcon(
                        if (prefs.finderView == "grid") Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        "Change view"
                    ) {
                        prefs.setFinderView(if (prefs.finderView == "grid") "list" else "grid")
                    }
                    Box {
                        ToolIcon(Icons.Rounded.Sort, "Sort") { sortMenu = true }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf("Name", "Date", "Size", "Kind").forEach { mode ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$mode${if (sortMode == mode) " ✓" else ""}",
                                            color = style.text
                                        )
                                    },
                                    onClick = {
                                        if (sortMode == mode) ascending = !ascending else {
                                            sortMode = mode
                                            ascending = true
                                        }
                                        sortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                DividerLine()

                // ----------------------------------------------- path bar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stack.forEachIndexed { index, dir ->
                        if (index > 0) {
                            Text("›", color = style.tertiary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        Text(
                            dir.name ?: "Documents",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index == stack.lastIndex) style.text else style.secondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    stack = stack.take(index + 1)
                                    selected = null
                                }
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (loading) {
                        Text("Loading…", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                    }
                }
                DividerLine()

                // ----------------------------------------------- content
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (current == null) {
                        EmptyState(
                            title = "Choose a folder",
                            message = "Finder needs a folder that you grant access to. Android keeps the rest of storage private.",
                            action = "Choose Folder…"
                        ) { shell.pickFolder() }
                    } else if (sorted.isEmpty() && !loading) {
                        EmptyState(
                            title = if (query.isBlank()) "This folder is empty" else "No matches",
                            message = if (query.isBlank()) "Create a folder or choose another location." else "Try a different search term.",
                            action = if (query.isBlank()) "New Folder" else null
                        ) {
                            folderName = "Untitled Folder"
                            newFolderOpen = true
                        }
                    } else if (prefs.finderView == "grid") {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(96.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(sorted, key = { it.key }) { entry ->
                                GridEntry(
                                    entry = entry,
                                    selected = selected?.key == entry.key,
                                    onOpen = { openEntry(entry, state, context, stack) { stack = it } },
                                    onSelect = { selected = entry }
                                )
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
                            items(sorted, key = { it.key }) { entry ->
                                ListEntry(
                                    entry = entry,
                                    selected = selected?.key == entry.key,
                                    onOpen = { openEntry(entry, state, context, stack) { stack = it } },
                                    onSelect = { selected = entry }
                                )
                            }
                        }
                    }
                }

                DividerLine()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${sorted.size} items" + (query.takeIf { it.isNotBlank() }?.let { " · “$it”" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary
                    )
                    Spacer(Modifier.weight(1f))
                    if (!wide) {
                        PillButton("Choose Folder…") { shell.pickFolder() }
                    }
                }
            }

            if (wide && selected != null) {
                DividerVertical()
                PreviewPane(
                    entry = selected!!,
                    modifier = Modifier.width(220.dp).fillMaxHeight(),
                    onOpen = {
                        selected?.let { entry -> openEntry(entry, state, context, stack) { stack = it } }
                    },
                    onDelete = {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { selected?.file?.delete() == true }
                            state.notify(if (ok) "Moved to deleted items" else "Could not delete that item")
                            selected = null
                            refreshTick++
                        }
                    },
                    onRename = {
                        renameTarget = selected
                        renameValue = selected?.name ?: ""
                    }
                )
            }
        }
    }

    if (newFolderOpen) {
        AlertDialog(
            onDismissRequest = { newFolderOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val name = folderName.trim()
                    newFolderOpen = false
                    if (name.isNotEmpty()) {
                        scope.launch {
                            val created = withContext(Dispatchers.IO) {
                                runCatching { current?.createDirectory(name) != null }.getOrDefault(false)
                            }
                            state.notify(if (created) "Created “$name”" else "Could not create the folder")
                            refreshTick++
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { newFolderOpen = false }) { Text("Cancel") } },
            title = { Text("New Folder") },
            text = {
                BasicTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    textStyle = TextStyle(color = style.text, fontSize = 16.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(style.field)
                        .padding(10.dp)
                )
            }
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameTarget
                    val name = renameValue.trim()
                    renameTarget = null
                    if (target != null && name.isNotEmpty()) {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { runCatching { target.file.renameTo(name) }.getOrDefault(false) }
                            state.notify(if (ok) "Renamed to “$name”" else "Rename not supported here")
                            refreshTick++
                        }
                    }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
            title = { Text("Rename") },
            text = {
                BasicTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    textStyle = TextStyle(color = style.text, fontSize = 16.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(style.field)
                        .padding(10.dp)
                )
            }
        )
    }
}

private fun openEntry(
    entry: FinderEntry,
    state: LauncherState,
    context: android.content.Context,
    stack: List<DocumentFile>,
    updateStack: (List<DocumentFile>) -> Unit
) {
    if (entry.isDir) {
        updateStack(stack + entry.file)
    } else {
        val opened = Intents.openFile(context, entry.file.uri, entry.mime)
        if (!opened) state.notify("No installed app can open ${entry.name}")
    }
}

@Composable
private fun DividerVertical() {
    Box(Modifier.width(0.6.dp).fillMaxHeight().background(LocalGlassStyle.current.divider))
}

@Composable
private fun ToolIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) style.secondary else style.tertiary.copy(alpha = 0.4f),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun GridEntry(
    entry: FinderEntry,
    selected: Boolean,
    onOpen: () -> Unit,
    onSelect: () -> Unit
) {
    val style = LocalGlassStyle.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) style.field else Color.Transparent)
            .pointerInput(entry.key) {
                detectTapGestures(
                    onTap = { onSelect() },
                    onDoubleTap = { onOpen() }
                )
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            iconFor(entry),
            contentDescription = null,
            tint = if (entry.isDir) Color(0xFF6BB8FF) else style.secondary,
            modifier = Modifier.size(38.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            entry.name,
            style = MaterialTheme.typography.labelSmall,
            color = style.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ListEntry(
    entry: FinderEntry,
    selected: Boolean,
    onOpen: () -> Unit,
    onSelect: () -> Unit
) {
    val style = LocalGlassStyle.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) style.field else Color.Transparent)
            .pointerInput(entry.key) {
                detectTapGestures(onTap = { onSelect() }, onDoubleTap = { onOpen() })
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            iconFor(entry),
            null,
            tint = if (entry.isDir) Color(0xFF6BB8FF) else style.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            entry.name,
            style = MaterialTheme.typography.bodyMedium,
            color = style.text,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            fileKindLabel(entry.name, entry.mime, entry.isDir),
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary,
            modifier = Modifier.width(70.dp)
        )
        Text(
            if (entry.isDir) "—" else formatBytes(entry.size),
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary,
            modifier = Modifier.width(72.dp)
        )
        Text(
            formatDate(entry.modified),
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary
        )
    }
}

@Composable
private fun PreviewPane(
    entry: FinderEntry,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    val style = LocalGlassStyle.current
    Column(modifier.background(style.field.copy(alpha = 0.35f)).padding(14.dp)) {
        Icon(
            iconFor(entry),
            null,
            tint = if (entry.isDir) Color(0xFF6BB8FF) else style.secondary,
            modifier = Modifier.size(58.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            entry.name,
            style = MaterialTheme.typography.titleSmall,
            color = style.text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(14.dp))
        InfoLine("Kind", fileKindLabel(entry.name, entry.mime, entry.isDir))
        InfoLine("Size", if (entry.isDir) "—" else formatBytes(entry.size))
        InfoLine("Modified", formatDate(entry.modified))
        InfoLine("Type", entry.mime ?: "Unknown")
        Spacer(Modifier.height(14.dp))
        PillButton("Open", Modifier.fillMaxWidth(), prominent = true, onClick = onOpen)
        Spacer(Modifier.height(8.dp))
        PillButton("Rename", Modifier.fillMaxWidth(), onClick = onRename)
        Spacer(Modifier.height(8.dp))
        ActionRow("Delete", icon = Icons.Rounded.Delete, onClick = onDelete)
        Spacer(Modifier.weight(1f))
        Text(
            "Access is limited to folders you grant through Android's system picker.",
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    val style = LocalGlassStyle.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = style.tertiary, modifier = Modifier.width(64.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = style.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyState(title: String, message: String, action: String?, onAction: () -> Unit) {
    val style = LocalGlassStyle.current
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Folder, null, tint = style.tertiary, modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = style.text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = style.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            PillButton(action, prominent = true, onClick = onAction)
        }
    }
}

@Suppress("unused")
private val unusedMore = Icons.Rounded.MoreVert
