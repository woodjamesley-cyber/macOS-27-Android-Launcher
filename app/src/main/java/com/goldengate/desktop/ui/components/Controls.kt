package com.goldengate.desktop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldengate.desktop.ui.theme.GG
import com.goldengate.desktop.ui.theme.LocalGlassStyle

@Composable
fun PanelTitle(text: String, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = style.secondary,
        modifier = modifier
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = LocalGlassStyle.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GG.CardRadius))
            .background(style.field)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        content = content
    )
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    val style = LocalGlassStyle.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) style.text else style.tertiary
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = style.secondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ActionRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: String? = null,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = style.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = style.text)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = style.secondary) }
        }
        trailing?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = style.tertiary)
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    val bg by animateColorAsState(
        if (selected) style.field else Color.Transparent,
        label = "sidebarBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) tint else style.secondary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = style.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val style = LocalGlassStyle.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(style.field)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) style.hairline else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) style.text else style.secondary
                )
            }
        }
    }
}

@Composable
fun TrafficLights(
    modifier: Modifier = Modifier,
    size: Dp = GG.TrafficLight,
    showGlyphs: Boolean = false,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onZoom: () -> Unit
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrafficLight(Color(0xFFFF5F57), "Close window", showGlyphs, size, onClose)
        TrafficLight(Color(0xFFFEBC2E), "Minimize window", showGlyphs, size, onMinimize)
        TrafficLight(Color(0xFF28C840), "Zoom window", showGlyphs, size, onZoom)
    }
}

@Composable
private fun TrafficLight(
    color: Color,
    description: String,
    showGlyph: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (showGlyph) {
            Box(
                Modifier
                    .size(size * 0.34f)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            )
        }
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    Box(
        modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(style.divider)
    )
}

@Composable
fun PillButton(
    label: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    val bg = if (prominent) MaterialTheme.colorScheme.primary else style.field
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (prominent) Color.White else style.text
        )
    }
}

@Composable
fun FieldLabel(text: String) {
    val style = LocalGlassStyle.current
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = style.tertiary,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}
