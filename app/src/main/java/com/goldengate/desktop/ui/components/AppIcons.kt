package com.goldengate.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goldengate.desktop.core.BuiltinApp

private data class IconStyle(
    val colors: List<Color>,
    val glyph: ImageVector,
    val darkTile: Boolean = false,
    val glyphTint: Color = Color.White
)

private fun styleFor(app: BuiltinApp): IconStyle = when (app) {
    BuiltinApp.FINDER -> IconStyle(
        listOf(Color(0xFF69B7FF), Color(0xFF1E63D6)),
        Icons.Rounded.Folder
    )
    BuiltinApp.NOTES -> IconStyle(
        listOf(Color(0xFFFFE07A), Color(0xFFF2B21D)),
        Icons.Rounded.Description,
        glyphTint = Color(0xCC3B2B00)
    )
    BuiltinApp.CALCULATOR -> IconStyle(
        listOf(Color(0xFFFFB259), Color(0xFFE8770C)),
        Icons.Rounded.Calculate
    )
    BuiltinApp.CALENDAR -> IconStyle(
        listOf(Color(0xFFFF7A72), Color(0xFFD92B2B)),
        Icons.Rounded.CalendarMonth
    )
    BuiltinApp.CLOCK -> IconStyle(
        listOf(Color(0xFF6B7380), Color(0xFF20242B)),
        Icons.Rounded.Schedule
    )
    BuiltinApp.WEATHER -> IconStyle(
        listOf(Color(0xFF7BD3FF), Color(0xFF2E86D8)),
        Icons.Rounded.Cloud
    )
    BuiltinApp.ACTIVITY -> IconStyle(
        listOf(Color(0xFF63E0A0), Color(0xFF12805C)),
        Icons.Rounded.Speed
    )
    BuiltinApp.SETTINGS -> IconStyle(
        listOf(Color(0xFFAEB6C2), Color(0xFF5A6472)),
        Icons.Rounded.Settings
    )
    BuiltinApp.PHOTOS -> IconStyle(
        listOf(Color(0xFFFFD36E), Color(0xFFFF6FB5), Color(0xFF7F7BFF), Color(0xFF44D6E0)),
        Icons.Rounded.PhotoLibrary
    )
    BuiltinApp.SAFARI -> IconStyle(
        listOf(Color(0xFF8FE3FF), Color(0xFF1477D8)),
        Icons.Rounded.Language
    )
    BuiltinApp.LAUNCHPAD -> IconStyle(
        listOf(Color(0x66FFFFFF), Color(0x33FFFFFF)),
        Icons.Rounded.GridView,
        glyphTint = Color(0xFFF2F5FA)
    )
}

/** A squircle app tile: gradient face, inner top highlight and hairline rim. */
@Composable
fun BuiltinIcon(app: BuiltinApp, size: Dp, modifier: Modifier = Modifier) {
    val style = styleFor(app)
    val shape = RoundedCornerShape(size * 0.235f)
    Box(
        modifier = modifier
            .size(size)
            .shadowed(shape)
            .clip(shape)
            .background(Brush.linearGradient(style.colors)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent, Color.Black.copy(alpha = 0.08f))
                    )
                )
        )
        Icon(
            imageVector = style.glyph,
            contentDescription = null,
            tint = style.glyphTint,
            modifier = Modifier.size(size * 0.58f)
        )
        Box(
            Modifier
                .fillMaxSize()
                .border(0.8.dp, Color.White.copy(alpha = 0.35f), shape)
        )
    }
}

/** Wraps any Android app icon in the same squircle so third-party icons feel native to the desktop. */
@Composable
fun AndroidAppIcon(icon: ImageBitmap?, size: Dp, modifier: Modifier = Modifier, badge: Boolean = false) {
    val shape = RoundedCornerShape(size * 0.235f)
    Box(
        modifier = modifier
            .size(size)
            .shadowed(shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFF4F6FA), Color(0xFFD6DCE6)))),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.68f)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = null,
                tint = Color(0xFF5A6472),
                modifier = Modifier.size(size * 0.5f)
            )
        }
        Box(Modifier.fillMaxSize().border(0.8.dp, Color.White.copy(alpha = 0.5f), shape))
    }
}

/** A Finder-style folder tile. */
@Composable
fun FolderTile(size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.235f)
    Box(
        modifier = modifier
            .size(size)
            .shadowed(shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF9FD4FF), Color(0xFF3D8DE0)))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
        )
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
        Box(Modifier.fillMaxSize().border(0.8.dp, Color.White.copy(alpha = 0.4f), shape))
    }
}

@Composable
private fun Modifier.shadowed(shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.shadow(elevation = 6.dp, shape = shape, clip = false)
