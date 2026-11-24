package com.goldengate.desktop.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** macOS 27 Golden Gate inspired measurements, tightened from the rounder 2025 design. */
object GG {
    val WindowRadius = 12.dp
    val PanelRadius = 18.dp
    val CardRadius = 13.dp
    val MenuBarHeight = 30.dp
    val TrafficLight = 12.dp
    val DockIcon = 46.dp
    val DockIconLarge = 58.dp
    val TileRadius = 22.dp
}

data class AccentColor(val id: String, val label: String, val color: Color)

val AccentPalette = listOf(
    AccentColor("graphite", "Graphite", Color(0xFF9A9AA1)),
    AccentColor("blue", "Blue", Color(0xFF0A84FF)),
    AccentColor("purple", "Purple", Color(0xFFBF5AF2)),
    AccentColor("pink", "Pink", Color(0xFFFF375F)),
    AccentColor("orange", "Orange", Color(0xFFFF9F0A)),
    AccentColor("green", "Green", Color(0xFF30D158)),
    AccentColor("teal", "Teal", Color(0xFF40C8E0))
)

fun accentFor(id: String): AccentColor = AccentPalette.firstOrNull { it.id == id } ?: AccentPalette[1]

/**
 * All glass surfaces read their look from here, so one slider in Settings changes the whole
 * desktop consistently from ultraclear to fully tinted.
 */
data class GlassStyle(
    val dark: Boolean = true,
    val tint: Float = 0.42f,
    val blur: Boolean = true,
    val blurRadius: Dp = 26.dp
) {
    val text: Color get() = if (dark) Color(0xFFF3F5F9) else Color(0xFF16181D)
    val secondary: Color get() = if (dark) Color(0xB8EBEEF4) else Color(0x991F2129)
    val tertiary: Color get() = if (dark) Color(0x7AEBEEF4) else Color(0x661F2129)
    val divider: Color get() = if (dark) Color(0x1FFFFFFF) else Color(0x14000000)
    val field: Color get() = if (dark) Color(0x26FFFFFF) else Color(0x0F000000)
    val hairline: Color get() = if (dark) Color(0x33FFFFFF) else Color(0x1F000000)
    val popover: Color get() = if (dark) Color(0xE81A1D24) else Color(0xEEF7F8FA)
}

val LocalGlassStyle = staticCompositionLocalOf { GlassStyle() }

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    secondary = Color(0xFF64D2FF),
    background = Color.Transparent,
    onBackground = Color(0xFFF3F5F9),
    surface = Color(0x1FFFFFFF),
    onSurface = Color(0xFFF3F5F9),
    surfaceVariant = Color(0x14FFFFFF),
    onSurfaceVariant = Color(0xFFC7CBD3),
    outline = Color(0x33FFFFFF),
    error = Color(0xFFFF6961)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0071E3),
    onPrimary = Color.White,
    secondary = Color(0xFF0088A8),
    background = Color.Transparent,
    onBackground = Color(0xFF16181D),
    surface = Color(0xCCFFFFFF),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0x14000000),
    onSurfaceVariant = Color(0xFF4A4E57),
    outline = Color(0x1F000000),
    error = Color(0xFFD70015)
)

private val GGType = Typography(
    displayLarge = Typography().displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-1.5).sp),
    displayMedium = Typography().displayMedium.copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    displaySmall = Typography().displaySmall.copy(fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp),
    bodyLarge = Typography().bodyLarge.copy(letterSpacing = (-0.1).sp),
    bodyMedium = Typography().bodyMedium.copy(letterSpacing = 0.sp),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    labelMedium = Typography().labelMedium.copy(letterSpacing = 0.1.sp),
    labelSmall = Typography().labelSmall.copy(letterSpacing = 0.2.sp)
)

@Composable
fun GoldenGateTheme(
    glass: GlassStyle,
    accent: Color,
    content: @Composable () -> Unit
) {
    val base = if (glass.dark) DarkColors else LightColors
    val colors = base.copy(primary = accent, secondary = accent)
    CompositionLocalProvider(LocalGlassStyle provides glass) {
        MaterialTheme(colorScheme = colors, typography = GGType, content = content)
    }
}

val TitleBarShape = RoundedCornerShape(topStart = GG.WindowRadius, topEnd = GG.WindowRadius)
val WindowShape = RoundedCornerShape(GG.WindowRadius)
val PanelShape = RoundedCornerShape(GG.PanelRadius)
