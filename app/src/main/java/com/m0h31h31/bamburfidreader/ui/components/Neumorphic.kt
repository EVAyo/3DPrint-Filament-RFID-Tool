package com.m0h31h31.bamburfidreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.basic.InputField as MiuixInputField
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator as MiuixInfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.SearchBar as MiuixSearchBar
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

private val NeuShape = RoundedCornerShape(24.dp)
private val NeuInnerShape = RoundedCornerShape(18.dp)
private val ModernShape = RoundedCornerShape(16.dp)
private val ModernInnerShape = RoundedCornerShape(12.dp)

private fun AppUiStyle.isModernWorkbenchStyle(): Boolean =
    this == AppUiStyle.MODERN_WORKBENCH || this == AppUiStyle.MODERN_WORKBENCH_COMPOSE

@Composable
fun Modifier.neuBackground(): Modifier {
    val uiStyle = LocalAppUiStyle.current
    val colors = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
        AppUiStyle.MIUIX -> listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        )
        AppUiStyle.MODERN_WORKBENCH,
        AppUiStyle.MODERN_WORKBENCH_COMPOSE -> listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    }
    return background(brush = Brush.verticalGradient(colors = colors))
}

@Composable
fun Modifier.neuCard(
    shape: Shape = NeuShape,
    elevated: Boolean = true
): Modifier {
    val uiStyle = LocalAppUiStyle.current
    val base = MaterialTheme.colorScheme.surface
    val isModernWorkbench = uiStyle.isModernWorkbenchStyle()
    val resolvedShape = if (isModernWorkbench && shape == NeuShape) {
        ModernShape
    } else {
        shape
    }
    val darkShadow = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        AppUiStyle.MIUIX -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f)
        AppUiStyle.MODERN_WORKBENCH,
        AppUiStyle.MODERN_WORKBENCH_COMPOSE -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
    }
    val lightShadow = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> Color.White.copy(alpha = 0.92f)
        AppUiStyle.MIUIX -> Color.White.copy(alpha = 0.45f)
        AppUiStyle.MODERN_WORKBENCH,
        AppUiStyle.MODERN_WORKBENCH_COMPOSE -> Color.White.copy(alpha = 0.0f)
    }
    val borderColor = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> Color.White.copy(alpha = 0.7f)
        AppUiStyle.MIUIX -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        AppUiStyle.MODERN_WORKBENCH,
        AppUiStyle.MODERN_WORKBENCH_COMPOSE -> MaterialTheme.colorScheme.outlineVariant
    }
    val shadowed = if (elevated) {
        when (uiStyle) {
            AppUiStyle.NEUMORPHIC -> this
                .shadow(
                    elevation = 10.dp,
                    shape = resolvedShape,
                    ambientColor = darkShadow,
                    spotColor = darkShadow
                )
                .shadow(
                    elevation = 2.dp,
                    shape = resolvedShape,
                    ambientColor = lightShadow,
                    spotColor = lightShadow
                )
            AppUiStyle.MIUIX -> this.shadow(
                elevation = 4.dp,
                shape = resolvedShape,
                ambientColor = darkShadow,
                spotColor = darkShadow
            )
            AppUiStyle.MODERN_WORKBENCH,
            AppUiStyle.MODERN_WORKBENCH_COMPOSE -> this.shadow(
                elevation = 1.dp,
                shape = resolvedShape,
                ambientColor = darkShadow,
                spotColor = darkShadow
            )
        }
    } else {
        this
    }
    return shadowed
        .clip(resolvedShape)
        .background(base)
        .border(
            width = 1.dp,
            color = borderColor,
            shape = resolvedShape
        )
}

@Composable
fun NeuPanel(
    modifier: Modifier = Modifier,
    shape: Shape = NeuShape,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neuCard(shape = shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val uiStyle = LocalAppUiStyle.current
    val buttonModifier = when (uiStyle) {
        AppUiStyle.MIUIX, AppUiStyle.MODERN_WORKBENCH, AppUiStyle.MODERN_WORKBENCH_COMPOSE -> modifier
        AppUiStyle.NEUMORPHIC -> modifier.neuCard(shape = NeuInnerShape, elevated = true)
    }
    val buttonColors = when (uiStyle) {
        AppUiStyle.MIUIX -> ButtonDefaults.buttonColors()
        AppUiStyle.MODERN_WORKBENCH,
        AppUiStyle.MODERN_WORKBENCH_COMPOSE -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppUiStyle.NEUMORPHIC -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = buttonModifier,
        shape = when (uiStyle) {
            AppUiStyle.NEUMORPHIC -> NeuInnerShape
            AppUiStyle.MIUIX -> MaterialTheme.shapes.medium
            AppUiStyle.MODERN_WORKBENCH,
            AppUiStyle.MODERN_WORKBENCH_COMPOSE -> ModernInnerShape
        },
        colors = buttonColors,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = if (uiStyle.isModernWorkbenchStyle()) 10.dp else 12.dp)
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontWeight = if (uiStyle == AppUiStyle.MIUIX) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}

@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX || uiStyle.isModernWorkbenchStyle()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            label = { Text(label) },
            modifier = modifier,
            shape = if (uiStyle.isModernWorkbenchStyle()) ModernInnerShape else MaterialTheme.shapes.medium,
            colors = if (uiStyle.isModernWorkbenchStyle()) {
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            }
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            label = { Text(label) },
            modifier = modifier.neuCard(
                shape = NeuInnerShape,
                elevated = true
            ),
            shape = NeuInnerShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun AppSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        val collapsed = remember { { _: Boolean -> } }
        MiuixSearchBar(
            inputField = {
                MiuixInputField(
                    value,
                    onValueChange,
                    { query ->
                        onValueChange(query)
                        onSearch?.invoke()
                    },
                    false,
                    collapsed,
                    Modifier.fillMaxWidth(),
                    placeholder,
                    true,
                    TextStyle.Default
                )
            },
            onExpandedChange = collapsed,
            modifier = modifier
        ) {}
    } else if (uiStyle.isModernWorkbenchStyle()) {
        NeuTextField(
            value = value,
            onValueChange = onValueChange,
            label = placeholder,
            modifier = modifier
        )
    } else {
        NeuTextField(
            value = value,
            onValueChange = onValueChange,
            label = placeholder,
            modifier = modifier
        )
    }
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        MiuixSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange ?: {},
            modifier = modifier
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            colors = if (uiStyle.isModernWorkbenchStyle()) {
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            } else {
                SwitchDefaults.colors()
            }
        )
    }
}

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished
        )
    } else {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            colors = androidx.compose.material3.SliderDefaults.colors()
        )
    }
}

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        if (progress == null) {
            MiuixInfiniteProgressIndicator(modifier = modifier)
        } else {
            MiuixCircularProgressIndicator(
                modifier = modifier,
                progress = progress.coerceIn(0f, 1f)
            )
        }
    } else {
        if (progress == null) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = modifier
            )
        } else {
            androidx.compose.material3.CircularProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = modifier
            )
        }
    }
}

@Composable
fun AppLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        MiuixLinearProgressIndicator(
            modifier = modifier,
            progress = progress?.coerceIn(0f, 1f)
        )
    } else {
        if (progress == null) {
            androidx.compose.material3.LinearProgressIndicator(
                modifier = modifier
            )
        } else {
            androidx.compose.material3.LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = modifier
            )
        }
    }
}
