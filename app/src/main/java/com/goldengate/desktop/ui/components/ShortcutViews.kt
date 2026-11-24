package com.goldengate.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.goldengate.desktop.core.AppKind
import com.goldengate.desktop.core.BuiltinApp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.core.Shortcut
import com.goldengate.desktop.core.ShortcutKind
import com.goldengate.desktop.ui.desktop.MenuEntry
import com.goldengate.desktop.util.Intents

@Composable
fun ShortcutIcon(shortcut: Shortcut?, state: LauncherState, size: Dp, modifier: Modifier = Modifier) {
    if (shortcut == null) {
        AndroidAppIcon(null, size, modifier)
        return
    }
    when (shortcut.kind) {
        ShortcutKind.BUILTIN -> {
            val app = BuiltinApp.fromId(shortcut.target)
            if (app != null) BuiltinIcon(app, size, modifier) else AndroidAppIcon(null, size, modifier)
        }
        ShortcutKind.APP -> AndroidAppIcon(state.appEntry(shortcut.target)?.icon, size, modifier)
        ShortcutKind.FOLDER -> FolderTile(size, modifier)
    }
}

fun shortcutLabel(shortcut: Shortcut, state: LauncherState): String = when (shortcut.kind) {
    ShortcutKind.BUILTIN -> BuiltinApp.fromId(shortcut.target)?.title ?: "App"
    ShortcutKind.APP -> state.appEntry(shortcut.target)?.label
        ?: shortcut.label.ifBlank { "Android app" }
    ShortcutKind.FOLDER -> state.prefs.folder(shortcut.target)?.name ?: "Folder"
}

/** Context menu shared by the Dock, desktop icons and Launchpad. */
fun shortcutMenu(
    shortcut: Shortcut,
    state: LauncherState,
    prefs: Prefs,
    onRemoveFromDock: (() -> Unit)? = null,
    onRemoveFromDesktop: (() -> Unit)? = null
): List<MenuEntry> {
    val entries = mutableListOf<MenuEntry>()
    entries += MenuEntry.Item("Open") { state.activate(shortcut) }

    when (shortcut.kind) {
        ShortcutKind.BUILTIN -> {
            val app = BuiltinApp.fromId(shortcut.target)
            if (app?.kind == AppKind.WINDOW) {
                entries += MenuEntry.Item("Open New Window") { state.openWindow(app) }
            }
            if (app != null) {
                entries += MenuEntry.Item("Show in Launchpad") { state.overlay = com.goldengate.desktop.core.Overlay.LAUNCHPAD }
            }
        }
        ShortcutKind.APP -> {
            val pkg = shortcut.target.substringBefore('/')
            entries += MenuEntry.Item("App Info") { Intents.openAppDetails(state.context, pkg) }
        }
        ShortcutKind.FOLDER -> {
            val folderId = shortcut.target
            entries += MenuEntry.Item("Open Folder") {
                state.openFolderId = folderId
                state.overlay = com.goldengate.desktop.core.Overlay.FOLDER
            }
            entries += MenuEntry.Item("Rename Folder…") { state.openSettingsTo("folders") }
        }
    }

    if (onRemoveFromDock != null) {
        entries += MenuEntry.Divider
        entries += MenuEntry.Item("Add to Desktop") {
            if (prefs.desktopItems.none { it.key == shortcut.key }) {
                prefs.setDesktopItems(prefs.desktopItems + shortcut)
                state.notify("Added to Desktop")
            } else {
                state.notify("Already on the Desktop")
            }
        }
        entries += MenuEntry.Item("Remove from Dock") { onRemoveFromDock() }
    }

    if (onRemoveFromDesktop != null) {
        entries += MenuEntry.Divider
        entries += MenuEntry.Item("Add to Dock") {
            if (prefs.dockItems.none { it.key == shortcut.key }) {
                prefs.setDockItems(prefs.dockItems + shortcut)
                state.notify("Added to Dock")
            } else {
                state.notify("Already in the Dock")
            }
        }
        entries += MenuEntry.Item("Remove from Desktop") { onRemoveFromDesktop() }
    }

    if (shortcut.kind == ShortcutKind.BUILTIN) {
        val app = BuiltinApp.fromId(shortcut.target)
        if (app != null && app.kind == AppKind.EXTERNAL) {
            entries += MenuEntry.Divider
            entries += MenuEntry.Item("Requires an installed Android app") { }
        }
    }
    return entries
}
