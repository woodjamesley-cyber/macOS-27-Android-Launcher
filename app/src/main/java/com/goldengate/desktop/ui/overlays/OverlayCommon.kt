package com.goldengate.desktop.ui.overlays

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.goldengate.desktop.ui.theme.LocalGlassStyle

@Composable
fun OverlayScrim(
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.TopCenter,
    dim: Float = 0.35f,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalGlassStyle.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(if (style.blur) 150 else 0),
        label = "overlayAlpha"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = dim * alpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = alignment
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = 0.985f + 0.015f * alpha
                    scaleY = 0.985f + 0.015f * alpha
                }
                .pointerInput(Unit) { detectTapGestures { } },
            content = content
        )
    }
}
