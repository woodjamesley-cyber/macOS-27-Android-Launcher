package com.goldengate.desktop.ui.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface WallpaperSpec {
    data class Preset(val id: String) : WallpaperSpec
    data class Photo(val uri: String) : WallpaperSpec
}

data class WallpaperPreset(
    val id: String,
    val name: String,
    val caption: String,
    val colors: List<Color>,
    val glow: Color,
    val accentGlow: Color? = null,
    val dunes: Boolean = false,
    val ribbons: Boolean = false
)

/** Original artwork generated in code, so nothing is redistributed from macOS. */
val WallpaperPresets = listOf(
    WallpaperPreset(
        id = "golden_hour",
        name = "Golden Hour",
        caption = "Warm dunes at the edge of the bay",
        colors = listOf(Color(0xFF141322), Color(0xFF3A2430), Color(0xFF7A4A34), Color(0xFFE39B62)),
        glow = Color(0xFFFFD9A1),
        accentGlow = Color(0xFF7C4B7A),
        dunes = true
    ),
    WallpaperPreset(
        id = "fog_city",
        name = "Fog City",
        caption = "Low cloud rolling over the bridge",
        colors = listOf(Color(0xFF0A121B), Color(0xFF1C2B38), Color(0xFF44606F), Color(0xFF8AA6B6)),
        glow = Color(0xFFC9DEEA),
        accentGlow = Color(0xFF2E5C6E)
    ),
    WallpaperPreset(
        id = "midnight_bay",
        name = "Midnight Bay",
        caption = "City lights under a deep sky",
        colors = listOf(Color(0xFF04060C), Color(0xFF0B1728), Color(0xFF123247), Color(0xFF1D5A63)),
        glow = Color(0xFF57E0D2),
        accentGlow = Color(0xFF23408E)
    ),
    WallpaperPreset(
        id = "sea_glass",
        name = "Sea Glass",
        caption = "Soft surf and sea-foam",
        colors = listOf(Color(0xFF07201F), Color(0xFF125152), Color(0xFF2F8C82), Color(0xFFA8E4D4)),
        glow = Color(0xFFE4FFF7),
        accentGlow = Color(0xFF1B6E7A)
    ),
    WallpaperPreset(
        id = "copper_dusk",
        name = "Copper Dusk",
        caption = "Copper light before nightfall",
        colors = listOf(Color(0xFF1B0A10), Color(0xFF4A191F), Color(0xFFA04431), Color(0xFFF0A16A)),
        glow = Color(0xFFFFD2AE),
        accentGlow = Color(0xFF5C2340),
        dunes = true
    ),
    WallpaperPreset(
        id = "aurora_night",
        name = "Aurora Night",
        caption = "Northern ribbons over still water",
        colors = listOf(Color(0xFF040C14), Color(0xFF081C2A), Color(0xFF0D3040), Color(0xFF14546A)),
        glow = Color(0xFF5BF0BE),
        accentGlow = Color(0xFF8A6BFF),
        ribbons = true
    ),
    WallpaperPreset(
        id = "porcelain",
        name = "Porcelain",
        caption = "Bright studio light for day mode",
        colors = listOf(Color(0xFFEFF3F8), Color(0xFFDCE5F0), Color(0xFFC4D3E4), Color(0xFFAEBFCE)),
        glow = Color(0xFFFFFFFF),
        accentGlow = Color(0xFFB9D2EA)
    )
)

fun presetFor(id: String): WallpaperPreset =
    WallpaperPresets.firstOrNull { it.id == id } ?: WallpaperPresets.first()

/** Draws the wallpaper artwork. Used on the desktop and, offset + blurred, inside glass panels. */
@Composable
fun WallpaperArt(spec: WallpaperSpec, modifier: Modifier = Modifier) {
    when (spec) {
        is WallpaperSpec.Photo -> PhotoWallpaper(spec.uri, modifier)
        is WallpaperSpec.Preset -> {
            val p = presetFor(spec.id)
            Canvas(modifier) {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.linearGradient(
                        colors = p.colors,
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.45f, h)
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(p.glow.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(w * 0.78f, h * 0.16f),
                        radius = w.coerceAtLeast(h) * 0.7f
                    )
                )
                p.accentGlow?.let { accent ->
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(w * 0.16f, h * 0.82f),
                            radius = w.coerceAtLeast(h) * 0.6f
                        )
                    )
                }
                if (p.ribbons) {
                    val ribbon = Path().apply {
                        moveTo(-w * 0.1f, h * 0.62f)
                        cubicTo(w * 0.25f, h * 0.26f, w * 0.55f, h * 0.78f, w * 1.1f, h * 0.34f)
                        lineTo(w * 1.1f, h * 0.46f)
                        cubicTo(w * 0.6f, h * 0.9f, w * 0.3f, h * 0.42f, -w * 0.1f, h * 0.76f)
                        close()
                    }
                    drawPath(
                        ribbon,
                        Brush.linearGradient(
                            listOf(p.glow.copy(alpha = 0.32f), p.accentGlow?.copy(alpha = 0.28f) ?: Color.Transparent),
                            start = Offset(0f, h * 0.3f),
                            end = Offset(w, h * 0.8f)
                        )
                    )
                }
                if (p.dunes) {
                    val d1 = Path().apply {
                        moveTo(0f, h * 0.70f)
                        cubicTo(w * 0.22f, h * 0.60f, w * 0.34f, h * 0.78f, w * 0.56f, h * 0.74f)
                        cubicTo(w * 0.78f, h * 0.70f, w * 0.88f, h * 0.58f, w, h * 0.64f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(d1, Color.Black.copy(alpha = 0.16f))
                    val d2 = Path().apply {
                        moveTo(0f, h * 0.82f)
                        cubicTo(w * 0.3f, h * 0.72f, w * 0.52f, h * 0.9f, w, h * 0.78f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(d2, Color.Black.copy(alpha = 0.26f))
                }
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w.coerceAtLeast(h) * 0.85f
                    )
                )
            }
        }
    }
}

@Composable
private fun PhotoWallpaper(uri: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
    }
    Box(modifier.background(Color(0xFF0B0D12))) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun loadBitmap(context: android.content.Context, uri: String): ImageBitmap? = runCatching {
    val parsed = Uri.parse(uri)
    val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, parsed)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(2)
        }
    } else {
        context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it) }
    }
    bitmap?.asImageBitmap()
}.getOrNull()

/** Small preview used in Settings. */
@Composable
fun WallpaperPreview(preset: WallpaperPreset, modifier: Modifier = Modifier) {
    Box(modifier) {
        WallpaperArt(WallpaperSpec.Preset(preset.id), Modifier.fillMaxSize())
    }
}

@Suppress("unused")
private val unusedSize = Size.Zero
