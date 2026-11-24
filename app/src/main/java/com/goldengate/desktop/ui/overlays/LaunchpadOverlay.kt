package com.goldengate.desktop.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.AppKind
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.InstalledApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.core.Shortcut
import com.goldengate.desktop.ui.components.AndroidAppIcon
import com.goldengate.desktop.ui.components.BuiltinIcon
import com.goldengate.desktop.ui.components.FolderTile
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.util.Intents
import com.goldengate.desktop.ui.wallpaper.WallpaperArt

@Composable
fun LaunchpadOverlay(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var folderNameFor by remember { mutableStateOf<Shortcut?>(null) }
    var newName by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<Shortcut?>(null) }

    val builtins = BuiltinApp.entries.filter { it.kind != AppKind.OVERLAY }
        .filter { query.isBlank() || it.title.contains(query, true) }
    val apps = state.apps.filter { query.isBlank() || it.label.contains(query, true) }
    val folders = prefs.folders.filter { query.isBlank() || it.name.contains(query, true) }

    Box(Modifier.fillMaxSize()) {
        WallpaperArt(state.wallpaperSpec(), Modifier.fillMaxSize().blur(38.dp))
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (style.dark) 0.42f else 0.22f)))

        Column(Modifier.fillMaxSize().padding(top = 54.dp, start = 22.dp, end = 22.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(style.field)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = style.tertiary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = style.text, fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search apps", color = style.tertiary, fontSize = 14.sp)
                            inner()
                        }
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Launchpad", style = MaterialTheme.typography.titleMedium, color = style.text)
                    Text(
                        "${state.apps.size} apps · ${builtins.size} built in",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(style.field)
                        .clickable { state.dismissOverlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, "Close Launchpad", tint = style.text, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                contentPadding = PaddingValues(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (folders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { GridSectionTitle("Folders") }
                    items(folders, key = { "folder-${it.id}" }) { folder ->
                        LaunchpadTile(
                            label = folder.name,
                            onClick = {
                                state.openFolderId = folder.id
                                state.overlay = com.goldengate.desktop.core.Overlay.FOLDER
                            }
                        ) { FolderTile(56.dp) }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) { GridSectionTitle("Built-in apps") }
                items(builtins, key = { "builtin-${it.id}" }) { app ->
                    LaunchpadTile(
                        label = app.title,
                        onClick = { state.openApp(app) },
                        onLongPress = null
                    ) { BuiltinIcon(app, 56.dp) }
                }

                item(span = { GridItemSpan(maxLineSpan) }) { GridSectionTitle("Applications") }
                items(apps, key = { "app-${it.component}" }) { app ->
                    LaunchpadTile(
                        label = app.label,
                        onClick = { state.launchPackage(app.component, app.label) },
                        onLongPress = {
                            menuFor = Shortcut.app(app.component, app.label)
                        }
                    ) { AndroidAppIcon(app.icon, 56.dp) }
                }

                if (apps.isEmpty() && builtins.isEmpty() && state.appsLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Scanning installed apps…", color = style.secondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Long-press an Android app to pin it, open app info, or start a folder. Launchpad lists the same apps Android reports to any launcher.",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.tertiary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }

    menuFor?.let { shortcut ->
        val app = state.appEntry(shortcut.target)
        AlertDialog(
            onDismissRequest = { menuFor = null },
            confirmButton = {
                TextButton(onClick = {
                    state.launchPackage(shortcut.target, app?.label ?: "app")
                    menuFor = null
                }) { Text("Open") }
            },
            dismissButton = { TextButton(onClick = { menuFor = null }) { Text("Cancel") } },
            title = { Text(app?.label ?: "Application") },
            text = {
                Column {
                    androidx.compose.material3.TextButton(onClick = {
                        if (prefs.dockItems.none { it.key == shortcut.key }) {
                            prefs.setDockItems(prefs.dockItems + shortcut)
                            state.notify("Added ${app?.label ?: "app"} to Dock")
                        } else {
                            state.notify("Already in the Dock")
                        }
                        menuFor = null
                    }) { Text("Keep in Dock") }
                    androidx.compose.material3.TextButton(onClick = {
                        if (prefs.desktopItems.none { it.key == shortcut.key }) {
                            prefs.setDesktopItems(prefs.desktopItems + shortcut)
                            state.notify("Added to Desktop")
                        }
                        menuFor = null
                    }) { Text("Add to Desktop") }
                    androidx.compose.material3.TextButton(onClick = {
                        newName = app?.label ?: "New Folder"
                        folderNameFor = shortcut
                        menuFor = null
                    }) { Text("New folder with this app…") }
                    androidx.compose.material3.TextButton(onClick = {
                        app?.let { Intents.openAppDetails(context, it.packageName) }
                        menuFor = null
                    }) { Text("App info") }
                }
            }
        )
    }

    if (folderNameFor != null) {
        AlertDialog(
            onDismissRequest = { folderNameFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val shortcut = folderNameFor
                    val name = newName.trim().ifBlank { "New Folder" }
                    if (shortcut != null) {
                        prefs.createFolder(name, listOf(shortcut))
                        state.notify("Folder “$name” created")
                    }
                    folderNameFor = null
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { folderNameFor = null }) { Text("Cancel") } },
            title = { Text("New folder") },
            text = {
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
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

@Composable
private fun GridSectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalGlassStyle.current.secondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun LaunchpadTile(
    label: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    icon: @Composable () -> Unit
) {
    val style = LocalGlassStyle.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
            .padding(vertical = 4.dp)
    ) {
        icon()
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Suppress("unused")
private fun unusedModifierWidth() = Modifier.width(1.dp)

@Suppress("unused")
private fun unusedPadding() = Modifier.padding(1.dp)
