package com.goldengate.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.goldengate.desktop.ui.theme.GG
import com.goldengate.desktop.ui.theme.GlassStyle
import com.goldengate.desktop.ui.theme.LocalGlassStyle
import com.goldengate.desktop.ui.wallpaper.WallpaperArt
import com.goldengate.desktop.ui.wallpaper.WallpaperSpec
import kotlin.math.roundToInt

/** The wallpaper currently behind the desktop, so glass panels can sample and blur it. */
val LocalWallpaper = staticCompositionLocalOf<WallpaperSpec> { WallpaperSpec.Preset("fog_city") }

/** Root window size, needed to redraw the wallpaper inside a panel at the correct scale. */
val LocalRootSize = staticCompositionLocalOf { IntSize.Zero }

@Composable
fun glassTintBrush(style: GlassStyle): Brush {
    val base = if (style.dark) Color(0xFF0A0E15) else Color(0xFFF8FAFD)
    val alpha = if (style.dark) {
        0.05f + style.tint * 0.36f
    } else {
        0.24f + style.tint * 0.52f
    }
    return Brush.linearGradient(
        colors = listOf(base.copy(alpha = alpha * 0.9f), base.copy(alpha = alpha)),
        start = Offset(0f, 0f),
        end = Offset(0f, 1400f)
    )
}

/**
 * A macOS-style Liquid Glass panel: blurred and refracted wallpaper, a tint layer, a hairline
 * highlight and a soft shadow. Everything derives from the global glass style so the opacity
 * slider affects the entire interface at once.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GG.PanelRadius),
    tint: Float = LocalGlassStyle.current.tint,
    blurRadius: Dp = LocalGlassStyle.current.blurRadius,
    elevation: Dp = 20.dp,
    border: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalGlassStyle.current
    val wallpaper = LocalWallpaper.current
    val root = LocalRootSize.current
    val density = LocalDensity.current
    var position by remember { mutableStateOf(Offset.Zero) }

    val rootWidthDp = with(density) { root.width.toDp() }
    val rootHeightDp = with(density) { root.height.toDp() }

    Box(
        modifier = modifier
            .onGloballyPositioned { position = it.positionInRoot() }
            .shadow(elevation, shape, clip = false)
            .clip(shape)
    ) {
        if (style.blur && root.width > 0) {
            Box(Modifier.matchParentSize().clipToBounds()) {
                WallpaperArt(
                    spec = wallpaper,
                    modifier = Modifier
                        .requiredSize(rootWidthDp, rootHeightDp)
                        .then(
                            Modifier.offsetPx(
                                -position.x.roundToInt(),
                                -position.y.roundToInt()
                            )
                        )
                        .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .background(glassTintBrush(style.copy(tint = tint)))
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (style.dark) 0.055f else 0.42f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (style.dark) 0.05f else 0.02f)
                        )
                    )
                )
        )
        if (border) {
            Box(
                Modifier
                    .matchParentSize()
                    .border(
                        width = 0.8.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (style.dark) 0.30f else 0.75f),
                                Color.White.copy(alpha = if (style.dark) 0.05f else 0.25f)
                            )
                        ),
                        shape = shape
                    )
            )
        }
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = contentAlignment,
            content = content
        )
    }
}

/** Offset without entering the layout phase, so the wallpaper copy stays pinned to the screen. */
private fun Modifier.offsetPx(x: Int, y: Int): Modifier =
    this.offset { IntOffset(x, y) }

/** A compact glass surface for menus, chips and inline cards. */
@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GG.CardRadius),
    tint: Float = LocalGlassStyle.current.tint,
    content: @Composable BoxScope.() -> Unit
) = GlassPanel(
    modifier = modifier,
    shape = shape,
    tint = tint,
    elevation = 8.dp,
    content = content
)
