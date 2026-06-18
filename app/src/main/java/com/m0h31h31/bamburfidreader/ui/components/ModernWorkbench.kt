package com.m0h31h31.bamburfidreader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ModernWorkbenchTokens {
    val Orange: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    val OrangeSoft: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val Ink: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val Muted: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Line: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val Card: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val Page: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val Danger: Color
        @Composable get() = MaterialTheme.colorScheme.error
    val Success: Color
        @Composable get() = Color(0xFF16A34A)
}

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(radius),
            spotColor = Color.Black.copy(alpha = 0.08f),
            ambientColor = Color.Black.copy(alpha = 0.04f)
        ),
        shape = RoundedCornerShape(radius),
        color = ModernWorkbenchTokens.Card,
        border = BorderStroke(1.dp, ModernWorkbenchTokens.Line)
    ) {
        content()
    }
}

@Composable
fun ModernPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    filled: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true
) {
    val accent = if (danger) ModernWorkbenchTokens.Danger else MaterialTheme.colorScheme.primary
    val background = when {
        filled -> accent
        selected -> accent.copy(alpha = 0.12f)
        else -> ModernWorkbenchTokens.Card
    }
    val textColor = when {
        filled -> Color.White
        danger -> ModernWorkbenchTokens.Danger
        selected -> accent
        else -> ModernWorkbenchTokens.Ink
    }
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (enabled) background else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            when {
                filled -> Color.Transparent
                selected || danger -> accent
                else -> ModernWorkbenchTokens.Line
            }
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = if (enabled) textColor else ModernWorkbenchTokens.Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ModernSegmentedRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = ModernWorkbenchTokens.Card,
        border = BorderStroke(1.dp, ModernWorkbenchTokens.Line)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun ModernSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = ModernWorkbenchTokens.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = ModernWorkbenchTokens.Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ModernDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ModernWorkbenchTokens.Line)
    )
}

@Composable
fun ModernSettingRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = ModernWorkbenchTokens.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = ModernWorkbenchTokens.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing()
    }
}

@Composable
fun ModernDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, RoundedCornerShape(999.dp))
    )
}
