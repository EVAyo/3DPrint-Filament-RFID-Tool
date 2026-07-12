package com.m0h31h31.bamburfidreader.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 全局界面缩放比例。弹窗会在独立窗口子组合中重置 [LocalDensity],导致主题层的缩放失效,
 * 因此通过该 CompositionLocal 把缩放比例透传进弹窗,再由 [WithUiScale] 重新应用。
 */
val LocalUiScale = staticCompositionLocalOf { 1f }

/** 在弹窗内部按全局缩放比例重新覆盖 density,使弹窗内元素跟随全局缩放。 */
@Composable
fun WithUiScale(content: @Composable () -> Unit) {
    val scale = LocalUiScale.current
    if (scale == 1f) {
        content()
    } else {
        val d = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(d.density * scale, d.fontScale)
        ) { content() }
    }
}

/** 跟随全局缩放的 AlertDialog,内部各槽位内容按 [LocalUiScale] 缩放。 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { WithUiScale { confirmButton() } },
        modifier = modifier,
        dismissButton = dismissButton?.let { slot -> { WithUiScale { slot() } } },
        icon = icon?.let { slot -> { WithUiScale { slot() } } },
        title = title?.let { slot -> { WithUiScale { slot() } } },
        text = text?.let { slot -> { WithUiScale { slot() } } },
        shape = shape,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}

/** 跟随全局缩放的 Dialog,内容整体按 [LocalUiScale] 缩放。 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        WithUiScale { content() }
    }
}
