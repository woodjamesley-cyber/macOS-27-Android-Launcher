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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goldengate.desktop.core.LauncherState
import com.goldengate.desktop.core.Prefs
import com.goldengate.desktop.ui.components.GlassPanel
import com.goldengate.desktop.ui.components.PillButton
import com.goldengate.desktop.ui.components.ShortcutIcon
import com.goldengate.desktop.ui.components.shortcutLabel
import com.goldengate.desktop.ui.theme.LocalGlassStyle

@Composable
fun FolderOverlay(state: LauncherState, prefs: Prefs) {
    val style = LocalGlassStyle.current
    val folder = state.openFolderId?.let { prefs.folder(it) }

    OverlayScrim(onDismiss = { state.dismissOverlay() }, alignment = Alignment.Center) {
        GlassPanel(
            modifier = Modifier.width(360.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 36.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        folder?.name ?: "Folder",
                        style = MaterialTheme.typography.titleMedium,
                        color = style.text,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit folder",
                        tint = style.secondary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable {
                                state.dismissOverlay()
                                state.openSettingsTo("folders")
                            }
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close folder",
                        tint = style.secondary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable { state.dismissOverlay() }
                    )
                }
                Spacer(Modifier.height(12.dp))
                val shortcuts = folder?.shortcuts.orEmpty()
                if (shortcuts.isEmpty()) {
                    Text(
                        "This folder is empty. Add apps from System Settings → Folders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = style.tertiary
                    )
                    Spacer(Modifier.height(12.dp))
                    PillButton("Edit Folders") {
                        state.dismissOverlay()
                        state.openSettingsTo("folders")
                    }
                } else {
                    shortcuts.chunked(4).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { shortcut ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            state.activate(shortcut)
                                            state.dismissOverlay()
                                        }
                                        .padding(vertical = 5.dp)
                                ) {
                                    ShortcutIcon(shortcut, state, 50.dp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        shortcutLabel(shortcut, state),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 3.dp)
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
