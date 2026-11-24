package com.goldengate.desktop.ui.apps

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.BuildConfig
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.InstalledApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.core.Shortcut
import com.goldengate.desktop.core.ShortcutKind
import com.goldengate.desktop.core.WidgetKind
import com.goldengate.desktop.data.systemSnapshot
import com.goldengate.desktop.ui.components.ActionRow
import com.goldengate.desktop.ui.components.AndroidAppIcon
import com.goldengate.desktop.ui.components.DividerLine
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.components.PanelTitle
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.components.SectionCard
import com.goldengate.desktop.ui.components.SegmentedControl
import com.goldengate.desktop.ui.components.SidebarItem
import com.goldengate.desktop.ui.components.ToggleRow
import com.goldengate.desktop.ui.components.shortcutLabel
import com.goldengate.desktop.ui.theme.AccentPalette
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.ui.wallpaper.WallpaperPresets
import com.goldengate.desktop.ui.wallpaper.WallpaperPreview
import com.goldengate.desktop.util.Intents

private data class SettingsCategory(val id: String, val title: String, val icon: ImageVector)

private val settingsCategories = listOf(
    SettingsCategory("appearance", "Appearance", Icons.Rounded.Palette),
    SettingsCategory("wallpaper", "Wallpaper", Icons.Rounded.Wallpaper),
    SettingsCategory("desktop", "Desktop & Widgets", Icons.Rounded.Dashboard),
    SettingsCategory("dock", "Dock", Icons.Rounded.ViewWeek),
    SettingsCategory("launchpad", "Launchpad", Icons.Rounded.Apps),
    SettingsCategory("folders", "Folders", Icons.Rounded.FolderCopy),
    SettingsCategory("home", "Default Home App", Icons.Rounded.Home),
    SettingsCategory("assistant", "Assistant", Icons.Rounded.AutoAwesome),
    SettingsCategory("weather", "Weather", Icons.Rounded.Cloud),
    SettingsCategory("calendar", "Calendar", Icons.Rounded.CalendarMonth),
    SettingsCategory("finder", "Finder", Icons.Rounded.Folder),
    SettingsCategory("accessibility", "Accessibility", Icons.Rounded.Accessibility),
    SettingsCategory("privacy", "Privacy & Security", Icons.Rounded.Lock),
    SettingsCategory("shortcuts", "Keyboard Shortcuts", Icons.Rounded.Keyboard),
    SettingsCategory("advanced", "Advanced", Icons.Rounded.Build),
    SettingsCategory("about", "About", Icons.Rounded.Info)
)

@Composable
fun SettingsApp(state: LauncherState, prefs: Prefs, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    var page by remember { mutableStateOf("appearance") }
    var query by remember { mutableStateOf("") }
    var appPickerOpen by remember { mutableStateOf(false) }
    var folderPickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.settingsRequestedPage) {
        state.settingsRequestedPage?.let {
            page = it
            state.settingsRequestedPage = null
        }
    }

    val filteredCategories = settingsCategories.filter {
        query.isBlank() || it.title.contains(query, true)
    }

    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 720.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                Column(
                    Modifier
                        .width(212.dp)
                        .fillMaxHeight()
                        .background(style.field.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(style.field)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = style.tertiary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = style.text, fontSize = 13.sp),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (query.isEmpty()) Text("Search settings", color = style.tertiary, fontSize = 13.sp)
                                inner()
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn {
                        items(filteredCategories, key = { it.id }) { category ->
                            SidebarItem(
                                label = category.title,
                                icon = category.icon,
                                selected = page == category.id,
                                onClick = { page = category.id }
                            )
                        }
                    }
                }
                Box(Modifier.width(0.6.dp).fillMaxHeight().background(style.divider))
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (!wide) {
                    LazyRow(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(settingsCategories, key = { it.id }) { category ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (page == category.id) MaterialTheme.colorScheme.primary else style.field)
                                    .clickable { page = category.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    category.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (page == category.id) Color.White else style.text
                                )
                            }
                        }
                    }
                    DividerLine()
                }

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp)) {
                    val title = settingsCategories.firstOrNull { it.id == page }?.title ?: "Settings"
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = style.text)
                    Spacer(Modifier.height(14.dp))

                    when (page) {
                        "appearance" -> AppearancePage(prefs)
                        "wallpaper" -> WallpaperPage(prefs)
                        "desktop" -> DesktopPage(state, prefs)
                        "dock" -> DockPage(state, prefs, onAddApp = { appPickerOpen = true }, onAddFolder = { folderPickerOpen = true })
                        "launchpad" -> LaunchpadPage(state, prefs)
                        "folders" -> FoldersPage(state, prefs)
                        "home" -> HomePage(state, prefs)
                        "assistant" -> AssistantPage(state, prefs)
                        "weather" -> WeatherPage(state, prefs)
                        "calendar" -> CalendarPage(state, prefs)
                        "finder" -> FinderSettingsPage(state, prefs)
                        "accessibility" -> AccessibilityPage(state, prefs)
                        "privacy" -> PrivacyPage(state, prefs)
                        "shortcuts" -> ShortcutsPage()
                        "advanced" -> AdvancedPage(state, prefs)
                        "about" -> AboutPage(state)
                    }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }

    if (appPickerOpen) {
        AppPickerDialog(
            title = "Add app to Dock",
            apps = state.apps,
            onDismiss = { appPickerOpen = false },
            onPick = { app ->
                prefs.setDockItems(prefs.dockItems + Shortcut.app(app.component, app.label))
                appPickerOpen = false
                state.notify("${app.label} added to Dock")
            }
        )
    }

    if (folderPickerOpen) {
        AlertDialog(
            onDismissRequest = { folderPickerOpen = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { folderPickerOpen = false }) { Text("Close") } },
            title = { Text("Add a folder to the Dock") },
            text = {
                Column {
                    if (prefs.folders.isEmpty()) {
                        Text("Create a folder in Settings → Folders first.")
                    } else {
                        prefs.folders.forEach { folder ->
                            ActionRow(folder.name, subtitle = "${folder.shortcuts.size} items") {
                                prefs.setDockItems(prefs.dockItems + Shortcut.folder(folder.id, folder.name))
                                folderPickerOpen = false
                                state.notify("${folder.name} added to Dock")
                            }
                        }
                    }
                }
            }
        )
    }
}

// ------------------------------------------------------------------ pages

@Composable
private fun AppearancePage(prefs: Prefs) {
    val style = LocalGlassStyle.current
    val appearanceIndex = when (prefs.appearance) {
        "light" -> 1
        "dark" -> 2
        else -> 0
    }
    SectionCard {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Appearance", style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = listOf("Auto", "Light", "Dark"),
                selected = appearanceIndex,
                onSelect = { prefs.setAppearance(listOf("auto", "light", "dark")[it]) }
            )
        }
        DividerLine()
        Column(Modifier.padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Liquid Glass", style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
                Text("${(prefs.glassTint * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = style.secondary)
            }
            Text(
                "From ultraclear to fully tinted. Every glass surface updates together.",
                style = MaterialTheme.typography.labelSmall,
                color = style.tertiary
            )
            Slider(
                value = prefs.glassTint,
                onValueChange = { prefs.setGlassTint(it) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(Modifier.fillMaxWidth()) {
                Text("Ultraclear", style = MaterialTheme.typography.labelSmall, color = style.tertiary, modifier = Modifier.weight(1f))
                Text("Tinted", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
            }
        }
        DividerLine()
        ToggleRow(
            title = "Backdrop blur",
            subtitle = "Blur the wallpaper behind glass. Disable to save battery.",
            checked = prefs.blurEnabled,
            onChange = { prefs.setBlurEnabled(it) }
        )
        DividerLine()
        Column(Modifier.padding(vertical = 10.dp)) {
            Text("Accent colour", style = MaterialTheme.typography.bodyLarge, color = style.text)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentPalette.forEach { accent ->
                    val selected = prefs.accent == accent.id
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(accent.color)
                            .then(if (selected) Modifier.border(2.dp, style.text, CircleShape) else Modifier)
                            .clickable { prefs.setAccent(accent.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.Check, accent.label, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        DividerLine()
        ToggleRow(
            title = "Reduce motion",
            subtitle = "Shortens or removes window and panel animations.",
            checked = prefs.reduceMotion,
            onChange = { prefs.setReduceMotion(it) }
        )
        DividerLine()
        ToggleRow(
            title = "Window animations",
            subtitle = "Fade and scale windows when they open.",
            checked = prefs.windowAnimations,
            enabled = !prefs.reduceMotion,
            onChange = { prefs.setWindowAnimations(it) }
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "The Liquid Glass look here is an original Compose implementation. No Apple materials or code are included.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun WallpaperPage(prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    var selectedPhoto = prefs.wallpaperId == "photo"

    SectionCard {
        Text("Desktop picture", style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.padding(vertical = 8.dp))
        WallpaperPresets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { preset ->
                    val selected = prefs.wallpaperId == preset.id
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                prefs.setWallpaperId(preset.id)
                                selectedPhoto = false
                            }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.7f)
                                .clip(RoundedCornerShape(9.dp))
                                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)) else Modifier)
                        ) {
                            WallpaperPreview(preset, Modifier.fillMaxSize())
                            if (selected) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(5.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(preset.name, style = MaterialTheme.typography.labelMedium, color = style.text)
                        Text(preset.caption, style = MaterialTheme.typography.labelSmall, color = style.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        DividerLine()
        ActionRow(
            "Choose a photo…",
            subtitle = "Copied into the launcher's private storage so the picker permission stays temporary.",
            icon = Icons.Rounded.Upload
        ) { shell.pickWallpaper() }
        if (selectedPhoto) {
            Text("Using your photo as the desktop picture.", style = MaterialTheme.typography.labelSmall, color = style.secondary)
            ActionRow("Switch back to a built-in picture", icon = Icons.Rounded.Wallpaper) {
                prefs.setWallpaperId(WallpaperPresets.first().id)
                selectedPhoto = false
            }
        }
    }
}

@Composable
private fun DesktopPage(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    var addMenu by remember { mutableStateOf(false) }
    SectionCard {
        ToggleRow("Show desktop icons", checked = prefs.showDesktopIcons, onChange = { prefs.setShowDesktopIcons(it) })
        DividerLine()
        ToggleRow("Show icon labels", checked = prefs.iconLabels, onChange = { prefs.setIconLabels(it) })
        DividerLine()
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Icon size", style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = listOf("Small", "Medium", "Large"),
                selected = prefs.desktopIconSize,
                onSelect = { prefs.setDesktopIconSize(it) }
            )
        }
        DividerLine()
        ToggleRow("Show widgets", checked = prefs.showDesktopWidgets, onChange = { prefs.setShowDesktopWidgets(it) })
    }

    Spacer(Modifier.height(16.dp))
    PanelTitle("Widgets")
    Spacer(Modifier.height(6.dp))
    SectionCard {
        prefs.widgets.forEach { widget ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(widget.kind.title, style = MaterialTheme.typography.bodyMedium, color = style.text, modifier = Modifier.width(96.dp))
                SegmentedControl(
                    options = listOf("S", "M", "L"),
                    selected = widget.size,
                    onSelect = { size ->
                        prefs.setWidgets(prefs.widgets.map { if (it.id == widget.id) it.copy(size = size) else it })
                    }
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove ${widget.kind.title} widget",
                    tint = style.secondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            prefs.setWidgets(prefs.widgets.filterNot { it.id == widget.id })
                        }
                )
            }
            DividerLine()
        }
        Box {
            ActionRow("Add widget", icon = Icons.Rounded.Add) { addMenu = true }
            androidx.compose.material3.DropdownMenu(
                expanded = addMenu,
                onDismissRequest = { addMenu = false },
                containerColor = style.popover
            ) {
                WidgetKind.entries.forEach { kind ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(kind.title, color = style.text) },
                        onClick = {
                            addMenu = false
                            prefs.addWidget(kind)
                            state.notify("${kind.title} widget added")
                        }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    PanelTitle("Desktop items")
    Spacer(Modifier.height(6.dp))
    SectionCard {
        prefs.desktopItems.forEachIndexed { index, shortcut ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shortcutLabel(shortcut, state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = style.text,
                    modifier = Modifier.weight(1f)
                )
                ReorderButtons(
                    canUp = index > 0,
                    canDown = index < prefs.desktopItems.lastIndex,
                    onUp = {
                        val list = prefs.desktopItems.toMutableList()
                        val item = list.removeAt(index)
                        list.add(index - 1, item)
                        prefs.setDesktopItems(list)
                    },
                    onDown = {
                        val list = prefs.desktopItems.toMutableList()
                        val item = list.removeAt(index)
                        list.add(index + 1, item)
                        prefs.setDesktopItems(list)
                    },
                    onRemove = {
                        prefs.setDesktopItems(prefs.desktopItems.filterNot { it.key == shortcut.key })
                    }
                )
            }
            DividerLine()
        }
        ActionRow("Add app from Launchpad", icon = Icons.Rounded.Apps) {
            state.overlay = com.goldengate.desktop.core.Overlay.LAUNCHPAD
        }
    }
}

@Composable
private fun DockPage(
    state: LauncherState,
    prefs: Prefs,
    onAddApp: () -> Unit,
    onAddFolder: () -> Unit
) {
    val style = LocalGlassStyle.current
    SectionCard {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Dock size", style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = listOf("Small", "Medium", "Large"),
                selected = prefs.dockIconSize,
                onSelect = { prefs.setDockIconSize(it) }
            )
        }
        DividerLine()
        ToggleRow(
            "Magnification",
            "Icons grow under the pointer, like the macOS Dock.",
            prefs.dockMagnification
        ) { prefs.setDockMagnification(it) }
        DividerLine()
        ToggleRow(
            "Automatically hide the Dock",
            "A small handle at the bottom reveals it.",
            prefs.dockAutoHide
        ) { prefs.setDockAutoHide(it) }
    }

    Spacer(Modifier.height(16.dp))
    PanelTitle("Dock items")
    Spacer(Modifier.height(6.dp))
    SectionCard {
        prefs.dockItems.forEachIndexed { index, shortcut ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shortcutLabel(shortcut, state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = style.text,
                    modifier = Modifier.weight(1f)
                )
                ReorderButtons(
                    canUp = index > 0,
                    canDown = index < prefs.dockItems.lastIndex,
                    onUp = {
                        val list = prefs.dockItems.toMutableList()
                        val item = list.removeAt(index)
                        list.add(index - 1, item)
                        prefs.setDockItems(list)
                    },
                    onDown = {
                        val list = prefs.dockItems.toMutableList()
                        val item = list.removeAt(index)
                        list.add(index + 1, item)
                        prefs.setDockItems(list)
                    },
                    onRemove = {
                        prefs.setDockItems(prefs.dockItems.filterNot { it.key == shortcut.key })
                    }
                )
            }
            DividerLine()
        }
        ActionRow("Add Android app", icon = Icons.Rounded.Add, onClick = onAddApp)
        ActionRow("Add folder", icon = Icons.Rounded.FolderCopy, onClick = onAddFolder)
        ActionRow("Restore default Dock", icon = Icons.Rounded.Refresh) {
            prefs.resetLayout()
            state.notify("Dock and desktop reset")
        }
    }
}

@Composable
private fun LaunchpadPage(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    SectionCard {
        ActionRow(
            "Open Launchpad",
            subtitle = "${state.apps.size} Android apps discovered",
            icon = Icons.Rounded.Apps
        ) { state.overlay = com.goldengate.desktop.core.Overlay.LAUNCHPAD }
        DividerLine()
        ActionRow("Rescan installed apps", icon = Icons.Rounded.Refresh) {
            state.refreshApps(force = true)
            state.notify("Scanning installed apps…")
        }
        DividerLine()
        ToggleRow(
            "Show icon labels in Launchpad",
            checked = prefs.iconLabels,
            onChange = { prefs.setIconLabels(it) }
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Launchpad lists every activity that registers a launcher icon, including work profiles the system exposes to this app. Long-press an app there to pin it, open app info, or start a folder.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun FoldersPage(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    var newFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var pickerFor by remember { mutableStateOf<String?>(null) }

    ActionRow("New folder", icon = Icons.Rounded.Add) {
        folderName = "New Folder"
        newFolder = true
    }
    Spacer(Modifier.height(6.dp))
    SectionCard {
        if (prefs.folders.isEmpty()) {
            Text("No folders yet.", style = MaterialTheme.typography.bodySmall, color = style.tertiary, modifier = Modifier.padding(vertical = 8.dp))
        }
        prefs.folders.forEach { folder ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FolderCopy, null, tint = style.secondary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(folder.name, style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
                    Text("${folder.shortcuts.size}", style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                    Spacer(Modifier.width(8.dp))
                    PillButton(if (editingId == folder.id) "Done" else "Edit") {
                        editingId = if (editingId == folder.id) null else folder.id
                    }
                }
                if (editingId == folder.id) {
                    Spacer(Modifier.height(6.dp))
                    folder.shortcuts.forEach { shortcut ->
                        Row(Modifier.fillMaxWidth().padding(start = 24.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                shortcutLabel(shortcut, state),
                                style = MaterialTheme.typography.bodySmall,
                                color = style.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Rounded.Close,
                                "Remove from folder",
                                tint = style.tertiary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        prefs.updateFolder(folder.copy(shortcuts = folder.shortcuts.filterNot { it.key == shortcut.key }))
                                    }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillButton("Add app") { pickerFor = folder.id }
                        PillButton("Rename") {
                            folderName = folder.name
                            editingId = folder.id
                            newFolder = true
                        }
                        PillButton("Delete") {
                            prefs.deleteFolder(folder.id)
                            prefs.setDesktopItems(prefs.desktopItems.filterNot { it.kind == ShortcutKind.FOLDER && it.target == folder.id })
                            prefs.setDockItems(prefs.dockItems.filterNot { it.kind == ShortcutKind.FOLDER && it.target == folder.id })
                            editingId = null
                        }
                    }
                }
            }
            DividerLine()
        }
        ActionRow("Show folders on the Desktop", icon = Icons.Rounded.Folder) {
            val folderItems = prefs.folders.map { Shortcut.folder(it.id, it.name) }
            val kept = prefs.desktopItems.filterNot { it.kind == ShortcutKind.FOLDER }
            prefs.setDesktopItems(kept + folderItems)
            state.notify("Desktop folders updated")
        }
    }

    if (newFolder) {
        AlertDialog(
            onDismissRequest = { newFolder = false },
            confirmButton = {
                TextButton(onClick = {
                    val name = folderName.trim().ifBlank { "New Folder" }
                    val existing = prefs.folders.firstOrNull { it.name == folderName.trim() }
                    if (editingId != null && existing != null) {
                        prefs.updateFolder(existing.copy(name = name))
                    } else {
                        prefs.createFolder(name)
                    }
                    newFolder = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { newFolder = false }) { Text("Cancel") } },
            title = { Text("Folder name") },
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

    pickerFor?.let { folderId ->
        AppPickerDialog(
            title = "Add app to folder",
            apps = state.apps,
            onDismiss = { pickerFor = null },
            onPick = { app ->
                val folder = prefs.folder(folderId)
                if (folder != null) {
                    prefs.updateFolder(folder.copy(shortcuts = folder.shortcuts + Shortcut.app(app.component, app.label)))
                }
                pickerFor = null
            }
        )
    }
}

@Composable
private fun HomePage(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    val defaultHome = shell.isDefaultHome()
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (defaultHome) Color(0xFF28C840) else Color(0xFFFFB020))
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (defaultHome) "This launcher is your Home app" else "Not the default Home app",
                    style = MaterialTheme.typography.bodyLarge,
                    color = style.text
                )
                Text(
                    if (defaultHome) "Pressing Home returns to this desktop." else "Press Home and Android will still open One UI Home.",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.secondary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        PillButton(if (defaultHome) "Check again" else "Set as default Home", prominent = true) {
            if (defaultHome) {
                state.isDefaultHome = shell.isDefaultHome()
                state.notify(if (state.isDefaultHome) "Still the default Home app" else "Another launcher is the Home app")
            } else {
                shell.requestHomeRole()
            }
        }
        DividerLine()
        ActionRow("Open Android Home settings", icon = Icons.Rounded.Home) { shell.openHomeSettings() }
        ActionRow("Show onboarding again", icon = Icons.Rounded.Info) {
            prefs.setSetupComplete(false)
            state.overlay = com.goldengate.desktop.core.Overlay.SETUP
        }
    }
    Spacer(Modifier.height(14.dp))
    PanelTitle("On Samsung One UI")
    Spacer(Modifier.height(6.dp))
    Text(
        "1. Tap “Set as default Home” above and choose Golden Gate Desktop.\n" +
            "2. If Android shows a chooser, pick Golden Gate Desktop and tap Always.\n" +
            "3. On One UI you can also use Settings → Apps → Choose default apps → Home app.\n" +
            "4. Press the Home button or swipe up from the gesture bar to come back here.\n\n" +
            "Android still owns the lock screen, notification shade, recent apps and boot animation. A launcher cannot replace those without system privileges or root.",
        style = MaterialTheme.typography.bodySmall,
        color = style.secondary
    )
}

@Composable
private fun AssistantPage(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    var endpoint by remember { mutableStateOf(prefs.assistantEndpoint) }
    var model by remember { mutableStateOf(prefs.assistantModel) }
    var key by remember { mutableStateOf(prefs.assistantKey) }
    var testResult by remember { mutableStateOf<String?>(null) }

    SectionCard {
        Text(
            "On-device commands always work. Cloud answers are optional and use the endpoint you provide.",
            style = MaterialTheme.typography.bodySmall,
            color = style.secondary,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        DividerLine()
        SettingsField("Endpoint", endpoint, "https://api.openai.com/v1/chat/completions") { endpoint = it }
        DividerLine()
        SettingsField("Model", model, "gpt-4o-mini") { model = it }
        DividerLine()
        SettingsField("API key", key, "sk-…", secret = true) { key = it }
        DividerLine()
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Save", prominent = true) {
                prefs.setAssistantEndpoint(endpoint.trim())
                prefs.setAssistantModel(model.trim())
                prefs.setAssistantKey(key.trim())
                state.notify("Assistant settings saved")
            }
            PillButton("Test") {
                testResult = "Testing…"
                state.sendCommand(BuiltinApp.SETTINGS, "assistant_test")
            }
            PillButton("Clear key") {
                prefs.setAssistantKey("")
                key = ""
                state.notify("API key removed from this device")
            }
        }
        testResult?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = style.secondary)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "The key is stored in this app's private preferences and is only sent to the endpoint you configure. It is never bundled in the source or uploaded anywhere else. This is not Siri or Apple Intelligence; it is an optional third-party AI connection.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun WeatherPage(state: LauncherState, prefs: Prefs) {
    var city by remember { mutableStateOf(prefs.weatherCity) }
    SectionCard {
        SettingsField("City", city, "San Francisco") { city = it }
        DividerLine()
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Units", style = MaterialTheme.typography.bodyLarge, color = LocalGlassStyle.current.text, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = listOf("Celsius", "Fahrenheit"),
                selected = if (prefs.weatherUnit == "f") 1 else 0,
                onSelect = { prefs.setWeatherUnit(if (it == 1) "f" else "c") }
            )
        }
        DividerLine()
        ActionRow("Save and refresh", icon = Icons.Rounded.Refresh) {
            prefs.setWeatherCity(city.trim().ifBlank { "San Francisco" })
            state.notify("Weather city set to ${prefs.weatherCity}")
        }
        DividerLine()
        ActionRow("Open Weather", icon = Icons.Rounded.Cloud) { state.openWindow(BuiltinApp.WEATHER) }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Forecasts come from Open-Meteo, which needs no account or key. Only the city name leaves the device.",
        style = MaterialTheme.typography.labelSmall,
        color = LocalGlassStyle.current.tertiary
    )
}

@Composable
private fun CalendarPage(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    SectionCard {
        ToggleRow(
            "Show calendar events",
            "Reads events through Android's Calendar provider. Without this permission the launcher simply shows no events.",
            checked = prefs.calendarEnabled
        ) { enabled ->
            if (enabled) shell.requestCalendarPermission() else prefs.setCalendarEnabled(false)
        }
        DividerLine()
        ActionRow("Calendar permission in Android settings", icon = Icons.Rounded.Lock) {
            Intents.openSystemSettings(
                state.context,
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", state.context.packageName, null)
            )
        }
        DividerLine()
        ActionRow("Open Android calendar app", icon = Icons.Rounded.CalendarMonth) {
            state.openApp(BuiltinApp.CALENDAR)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "The launcher never reads notification content. Real Android notifications stay in the system shade, which a third-party launcher cannot replace.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun FinderSettingsPage(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    SectionCard {
        Text(
            "Current folder",
            style = MaterialTheme.typography.labelMedium,
            color = style.secondary
        )
        Text(
            prefs.fileRootUri ?: "Not chosen — Finder uses its private Documents folder",
            style = MaterialTheme.typography.bodySmall,
            color = style.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Choose folder…", prominent = true) { shell.pickFolder() }
            if (prefs.fileRootUri != null) {
                PillButton("Forget folder") {
                    prefs.setFileRootUri(null)
                    state.notify("Finder folder access cleared")
                }
            }
        }
        DividerLine()
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Default view", style = MaterialTheme.typography.bodyLarge, color = style.text, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = listOf("Icons", "List"),
                selected = if (prefs.finderView == "grid") 0 else 1,
                onSelect = { prefs.setFinderView(if (it == 0) "grid" else "list") }
            )
        }
        DividerLine()
        ActionRow("Open Finder", icon = Icons.Rounded.Folder) { state.openWindow(BuiltinApp.FINDER) }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Android's scoped storage means no app can browse all of internal storage without the All files access special permission. This launcher deliberately uses the system folder picker instead, so access is explicit and revocable.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun AccessibilityPage(state: LauncherState, prefs: Prefs) {
    val context = LocalContext.current
    val style = LocalGlassStyle.current
    SectionCard {
        ToggleRow("Reduce motion", "Stops panel and window animations in this launcher.", prefs.reduceMotion) {
            prefs.setReduceMotion(it)
        }
        DividerLine()
        ActionRow("Android font size", "Opens the system setting this launcher follows.", icon = Icons.Rounded.Accessibility) {
            Intents.openSystemSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
        }
        ActionRow("Android accessibility settings", icon = Icons.Rounded.Accessibility) {
            Intents.openSystemSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
        DividerLine()
        Text(
            "Every control has a spoken label and a minimum 44 dp touch target. Text uses scalable sp units, so Android's font size and display size settings apply here too.",
            style = MaterialTheme.typography.labelSmall,
            color = style.secondary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun PrivacyPage(state: LauncherState, prefs: Prefs) {
    val context = LocalContext.current
    val style = LocalGlassTheme()
    SectionCard {
        Text("What this launcher stores", style = MaterialTheme.typography.bodyLarge, color = style.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "• Layout, wallpaper choice, accent colour and Dock contents\n" +
                "• Notes you write and Assistant history\n" +
                "• The folder you explicitly grant to Finder\n" +
                "• Optional Assistant endpoint and key\n\n" +
                "Nothing is uploaded anywhere by default. The only network calls are weather (city name) and, if you configure it, your own AI endpoint.",
            style = MaterialTheme.typography.bodySmall,
            color = style.secondary
        )
        DividerLine()
        ActionRow("Permissions used by Android", icon = Icons.Rounded.Lock) {
            Intents.openAppDetails(context, context.packageName)
        }
        ActionRow("Clear Assistant history", icon = Icons.Rounded.Close) {
            prefs.setAssistantHistoryJson("[]")
            state.notify("Assistant history deleted")
        }
        ActionRow("Clear Assistant API key", icon = Icons.Rounded.Lock) {
            prefs.setAssistantKey("")
            state.notify("API key removed")
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Permissions requested: Calendar (optional events), Microphone (optional voice input), Modify system settings (optional brightness), Internet (weather and optional AI). Each is only requested when you enable the matching feature.",
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary
    )
}

@Composable
private fun LocalGlassTheme(): com.goldengate.desktop.ui.theme.GlassStyle = LocalGlassStyle.current

@Composable
private fun ShortcutsPage() {
    val style = LocalGlassStyle.current
    SectionCard {
        listOf(
            "⌘/Ctrl + Space" to "Open Spotlight",
            "⌘/Ctrl + Tab" to "Cycle through open windows",
            "⌘/Ctrl + S" to "Open the Assistant",
            "⌘/Ctrl + ," to "Open System Settings",
            "Esc" to "Close the front panel or window",
            "Search key" to "Open Spotlight",
            "Double-click title bar" to "Zoom the window",
            "Right-click Dock icon" to "Context menu"
        ).forEach { (keys, action) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                Text(keys, style = MaterialTheme.typography.labelMedium, color = style.text, modifier = Modifier.width(160.dp))
                Text(action, style = MaterialTheme.typography.bodySmall, color = style.secondary)
            }
            DividerLine()
        }
        Text(
            "A hardware keyboard gets the shortcuts above; a mouse or trackpad gets hover magnification and right-click menus.",
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun AdvancedPage(state: LauncherState, prefs: Prefs) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val style = LocalGlassStyle.current
    SectionCard {
        ActionRow("Copy settings JSON", "Useful for bug reports; contains no API key.", icon = Icons.Rounded.Upload) {
            clipboard.setText(AnnotatedString(prefs.exportJson()))
            state.notify("Settings copied to clipboard")
        }
        DividerLine()
        ActionRow("Reset desktop layout", "Restores default Dock, icons, widgets and folders.", icon = Icons.Rounded.Refresh) {
            prefs.resetLayout()
            state.notify("Layout reset")
        }
        DividerLine()
        ActionRow("App info in Android settings", icon = Icons.Rounded.Info) {
            Intents.openAppDetails(context, context.packageName)
        }
        DividerLine()
        Text(
            "This launcher never requests root, never disables system security and does not use hidden APIs.",
            style = MaterialTheme.typography.labelSmall,
            color = style.tertiary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun AboutPage(state: LauncherState) {
    val context = LocalContext.current
    val style = LocalGlassStyle.current
    val snapshot = remember { systemSnapshot(context) }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF2B3240), Color(0xFF0E1117))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("GG", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD9A1))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Golden Gate Desktop", style = MaterialTheme.typography.titleMedium, color = style.text)
                Text(
                    "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.secondary
                )
                Text(
                    "macOS 27 inspired, built natively for Android",
                    style = MaterialTheme.typography.labelSmall,
                    color = style.tertiary
                )
            }
        }
        DividerLine()
        Text(snapshot.deviceModel, style = MaterialTheme.typography.bodyMedium, color = style.text)
        Text(snapshot.androidVersion, style = MaterialTheme.typography.labelSmall, color = style.secondary)
        DividerLine()
        Text(
            "An independent, unofficial project. It is not affiliated with, endorsed by, or connected to Apple Inc. macOS, Finder, Safari and Siri are Apple trademarks; this app recreates a similar visual style with original artwork and Android code only.",
            style = MaterialTheme.typography.labelSmall,
            color = style.secondary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        DividerLine()
        ActionRow("License", subtitle = "MIT licensed source", icon = Icons.Rounded.Info) {
            state.notify("Source is provided under the MIT license")
        }
        ActionRow("Research gallery", subtitle = "Reference screenshots and notes in the repository", icon = Icons.Rounded.Info) {
            state.notify("Open research/gallery.md in the project repository")
        }
    }
}

// ------------------------------------------------------------------ helpers

@Composable
private fun SettingsField(
    label: String,
    value: String,
    placeholder: String,
    secret: Boolean = false,
    onChange: (String) -> Unit
) {
    val style = LocalGlassStyle.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = style.secondary)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = TextStyle(color = style.text, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(style.field)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = style.tertiary, fontSize = 14.sp)
                inner()
            }
        )
    }
}

@Composable
private fun ReorderButtons(
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    val style = LocalGlassStyle.current
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.KeyboardArrowUp,
            "Move up",
            tint = if (canUp) style.secondary else style.tertiary.copy(alpha = 0.3f),
            modifier = Modifier
                .size(20.dp)
                .clickable(enabled = canUp, onClick = onUp)
        )
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            "Move down",
            tint = if (canDown) style.secondary else style.tertiary.copy(alpha = 0.3f),
            modifier = Modifier
                .size(20.dp)
                .clickable(enabled = canDown, onClick = onDown)
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Rounded.Close,
            "Remove",
            tint = style.secondary,
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onRemove)
        )
    }
}

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit
) {
    val style = LocalGlassStyle.current
    var query by remember { mutableStateOf("") }
    val filtered = apps.filter { query.isBlank() || it.label.contains(query, true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            Column(Modifier.height(380.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(style.field)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = style.tertiary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = style.text, fontSize = 13.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search apps", color = style.tertiary, fontSize = 13.sp)
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(filtered, key = { it.component }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { onPick(app) }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AndroidAppIcon(app.icon, 26.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = style.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text("No apps matched.", color = style.tertiary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    )
}

@Suppress("unused")
private fun unusedColumnScope(): ColumnScope? = null
