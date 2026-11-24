package com.goldengate.desktop.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.core.AppKind
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Overlay
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.core.LocalShell
import com.goldengate.desktop.ui.components.shortTime
import com.goldengate.desktop.ui.components.shortDate
import com.goldengate.desktop.ui.theme.GG
import com.goldengate.desktop.ui.theme.LocalGlassStyle

sealed interface MenuEntry {
    data class Item(
        val label: String,
        val shortcut: String? = null,
        val enabled: Boolean = true,
        val onClick: () -> Unit
    ) : MenuEntry

    data object Divider : MenuEntry
}

@Composable
fun MenuBar(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    var openMenu by remember { mutableStateOf<String?>(null) }

    val activeWindow = state.windows.firstOrNull { it.key == state.activeKey }
        ?: state.windows.maxByOrNull { it.z }
    val activeApp = activeWindow?.app
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 520

    Box(Modifier.fillMaxWidth().statusBarsPadding()) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth().height(GG.MenuBarHeight),
            shape = RoundedCornerShape(0.dp),
            elevation = 0.dp,
            blurRadius = 18.dp,
            border = false
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMenu(
                    state = state,
                    prefs = prefs,
                    expanded = openMenu == "brand",
                    onToggle = { openMenu = if (openMenu == "brand") null else "brand" },
                    onDismiss = { openMenu = null }
                )

                val menus = activeApp?.let { appMenus(it, state, prefs, shell.isDefaultHome()) } ?: emptyList()
                if (!compact) {
                    Text(
                        text = activeApp?.title ?: "Finder",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = style.text,
                        modifier = Modifier.padding(horizontal = 7.dp)
                    )
                    menus.forEach { (title, entries) ->
                        val key = "menu-$title"
                        MenuAnchor(
                            title = title,
                            expanded = openMenu == key,
                            onToggle = { openMenu = if (openMenu == key) null else key },
                            onDismiss = { openMenu = null },
                            entries = entries
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (!state.isDefaultHome) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FFB020))
                            .clickable { shell.requestHomeRole() }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).background(Color(0xFFFFB020), CircleShape))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Set as Home",
                            style = MaterialTheme.typography.labelSmall,
                            color = style.text
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    StatusButton(Icons.Rounded.BatteryFull, "Battery ${state.batteryLevel}%") {
                        state.toggleOverlay(Overlay.NOTIFICATIONS)
                    }
                    Text(
                        "${state.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = style.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    StatusButton(Icons.Rounded.Wifi, "Network settings") {
                        if (android.os.Build.VERSION.SDK_INT >= 29) {
                            com.goldengate.desktop.util.Intents.openPanel(
                                state.context,
                                android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                            )
                        } else {
                            com.goldengate.desktop.util.Intents.openSystemSettings(
                                state.context,
                                android.provider.Settings.ACTION_WIFI_SETTINGS
                            )
                        }
                    }
                    StatusButton(Icons.Rounded.Search, "Spotlight") {
                        state.overlay = Overlay.SPOTLIGHT
                    }
                    StatusButton(Icons.Rounded.AutoAwesome, "Assistant") {
                        state.overlay = Overlay.ASSISTANT
                    }
                    StatusButton(Icons.Rounded.Tune, "Control Center") {
                        state.toggleOverlay(Overlay.CONTROL_CENTER)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${state.now.shortDate()}" + "  " + state.now.shortTime(prefs.menuBarSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = style.text,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .clickable { state.toggleOverlay(Overlay.NOTIFICATIONS) }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandMenu(
    state: LauncherState,
    prefs: Prefs,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val style = LocalGlassStyle.current
    val shell = LocalShell.current
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .clickable { onToggle() }
                .padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFFE3A8), Color(0xFFF0A868))))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Golden Gate",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = style.text
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            containerColor = style.popover,
            shape = RoundedCornerShape(10.dp)
        ) {
            MenuEntryView(MenuEntry.Item("About This Desktop") {
                onDismiss(); state.openSettingsTo("about")
            }, onDismiss)
            MenuEntryView(MenuEntry.Item("System Settings…", "⌘,") {
                onDismiss(); state.openSettingsTo(null)
            }, onDismiss)
            MenuEntryView(MenuEntry.Item("Launchpad") {
                onDismiss(); state.overlay = Overlay.LAUNCHPAD
            }, onDismiss)
            MenuEntryView(MenuEntry.Item("Spotlight…", "⌘Space") {
                onDismiss(); state.overlay = Overlay.SPOTLIGHT
            }, onDismiss)
            MenuEntryView(MenuEntry.Item("Assistant") {
                onDismiss(); state.overlay = Overlay.ASSISTANT
            }, onDismiss)
            MenuEntryView(MenuEntry.Divider, onDismiss)
            MenuEntryView(
                MenuEntry.Item(if (shell.isDefaultHome()) "Default Home app: this launcher" else "Set as Default Home…") {
                    onDismiss(); shell.requestHomeRole()
                }, onDismiss
            )
            MenuEntryView(MenuEntry.Item("Choose Desktop Picture…") {
                onDismiss(); shell.pickWallpaper()
            }, onDismiss)
            MenuEntryView(MenuEntry.Divider, onDismiss)
            MenuEntryView(MenuEntry.Item("Android Home Settings") {
                onDismiss(); shell.openHomeSettings()
            }, onDismiss)
        }
    }
}

@Composable
fun MenuAnchor(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>
) {
    val style = LocalGlassStyle.current
    Box {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = style.text,
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .clickable { onToggle() }
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )
        MenuPopup(expanded = expanded, onDismiss = onDismiss, entries = entries)
    }
}

@Composable
fun MenuPopup(expanded: Boolean, onDismiss: () -> Unit, entries: List<MenuEntry>) {
    val style = LocalGlassStyle.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = style.popover,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp
    ) {
        entries.forEach { MenuEntryView(it, onDismiss) }
    }
}

@Composable
private fun MenuEntryView(entry: MenuEntry, onDismiss: () -> Unit) {
    when (entry) {
        is MenuEntry.Divider -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = LocalGlassStyle.current.divider
        )
        is MenuEntry.Item -> {
            val style = LocalGlassStyle.current
            DropdownMenuItem(
                text = {
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (entry.enabled) style.text else style.tertiary
                    )
                },
                onClick = {
                    if (entry.enabled) {
                        onDismiss()
                        entry.onClick()
                    }
                },
                enabled = entry.enabled,
                trailingIcon = {
                    entry.shortcut?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = style.tertiary)
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val style = LocalGlassStyle.current
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(5.dp))
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = style.text, modifier = Modifier.size(16.dp))
    }
}

/** Per-app menus; every entry performs a real action rather than being decorative. */
@Composable
private fun appMenus(
    app: BuiltinApp,
    state: LauncherState,
    prefs: Prefs,
    isDefaultHome: Boolean
): List<Pair<String, List<MenuEntry>>> {
    val shell = LocalShell.current
    val windowActions = listOf(
        "Window" to listOf(
            MenuEntry.Item("Minimize") { state.activeKey?.let(state::minimize) },
            MenuEntry.Item("Zoom") { state.activeKey?.let(state::toggleMaximize) },
            MenuEntry.Divider,
            MenuEntry.Item("Close Window", "⌘W") { state.activeKey?.let(state::close) }
        ),
        "Help" to listOf(
            MenuEntry.Item("Golden Gate Help") { state.overlay = Overlay.ASSISTANT },
            MenuEntry.Item("Keyboard Shortcuts") { state.openSettingsTo("shortcuts") }
        )
    )

    val appSpecific: List<Pair<String, List<MenuEntry>>> = when (app) {
        BuiltinApp.FINDER -> listOf(
            "File" to listOf(
                MenuEntry.Item("New Finder Window", "⌘N") { state.openWindow(BuiltinApp.FINDER) },
                MenuEntry.Item("New Folder", "⇧⌘N") { state.sendCommand(app, "new_folder") },
                MenuEntry.Divider,
                MenuEntry.Item("Choose Folder…") { shell.pickFolder() },
                MenuEntry.Item("Close Window", "⌘W") { state.activeKey?.let(state::close) }
            ),
            "View" to listOf(
                MenuEntry.Item("as Icons") { prefs.setFinderView("grid") },
                MenuEntry.Item("as List") { prefs.setFinderView("list") },
                MenuEntry.Divider,
                MenuEntry.Item("Show Sidebar") { state.notify("Sidebar is always shown in Finder") }
            )
        )
        BuiltinApp.NOTES -> listOf(
            "File" to listOf(
                MenuEntry.Item("New Note", "⌘N") { state.sendCommand(app, "new_note") },
                MenuEntry.Item("Delete Selected Note") { state.sendCommand(app, "delete_note") }
            )
        )
        BuiltinApp.CALENDAR -> listOf(
            "File" to listOf(
                MenuEntry.Item("New Event…") { state.sendCommand(app, "new_event") }
            ),
            "View" to listOf(
                MenuEntry.Item("Today") { state.sendCommand(app, "today") },
                MenuEntry.Item("Refresh Events") { state.sendCommand(app, "refresh") }
            )
        )
        BuiltinApp.WEATHER -> listOf(
            "View" to listOf(
                MenuEntry.Item("Refresh Forecast") { state.sendCommand(app, "refresh") }
            )
        )
        BuiltinApp.ACTIVITY -> listOf(
            "View" to listOf(
                MenuEntry.Item("Refresh Statistics") { state.sendCommand(app, "refresh") }
            )
        )
        BuiltinApp.CALCULATOR -> listOf(
            "Edit" to listOf(
                MenuEntry.Item("Clear", "C") { state.sendCommand(app, "clear") }
            )
        )
        BuiltinApp.CLOCK -> listOf(
            "View" to listOf(
                MenuEntry.Item("World Clock") { state.sendCommand(app, "world") },
                MenuEntry.Item("Stopwatch") { state.sendCommand(app, "stopwatch") },
                MenuEntry.Item("Timer") { state.sendCommand(app, "timer") }
            )
        )
        BuiltinApp.SETTINGS -> listOf(
            "View" to listOf(
                MenuEntry.Item("Wallpaper") { state.settingsRequestedPage = "wallpaper" },
                MenuEntry.Item("Appearance") { state.settingsRequestedPage = "appearance" },
                MenuEntry.Item("Dock") { state.settingsRequestedPage = "dock" },
                MenuEntry.Item("Desktop & Widgets") { state.settingsRequestedPage = "desktop" },
                MenuEntry.Item("Default Home App") { state.settingsRequestedPage = "home" }
            )
        )
        else -> emptyList()
    }

    return appSpecific + windowActions
}
