package com.m0h31h31.bamburfidreader.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 应用内置的 Material 图标。
 *
 * 路径数据复制自 Material Design Icons（Apache 2.0），与
 * androidx.compose.material:material-icons-* 生成的图标一致。
 * 仅内置实际用到的几个图标，避免引入完整的 material-icons-extended
 * 依赖（未启用代码压缩时会使 APK 增大数 MB）。
 */
object AppIcons {
    val ArrowDownward: ImageVector by lazy {
        materialIcon(
            "Filled.ArrowDownward",
            "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"
        )
    }

    val ArrowUpward: ImageVector by lazy {
        materialIcon(
            "Filled.ArrowUpward",
            "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"
        )
    }

    val Check: ImageVector by lazy {
        materialIcon(
            "Filled.Check",
            "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
        )
    }

    val Close: ImageVector by lazy {
        materialIcon(
            "Filled.Close",
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
        )
    }

    val DragHandle: ImageVector by lazy {
        materialIcon(
            "Filled.DragHandle",
            "M20 9H4v2h16V9zM4 15h16v-2H4v2z"
        )
    }

    val PhotoCamera: ImageVector by lazy {
        materialIcon(
            "Filled.PhotoCamera",
            "M12 12m-3.2 0a3.2 3.2 0 1 1 6.4 0 3.2 3.2 0 1 1-6.4 0",
            "M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"
        )
    }

    val Sort: ImageVector by lazy {
        materialIcon(
            "AutoMirrored.Filled.Sort",
            "M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z",
            autoMirror = true
        )
    }
}

private fun materialIcon(
    name: String,
    vararg pathData: String,
    autoMirror: Boolean = false
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror
    )
    for (data in pathData) {
        builder.addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }
    return builder.build()
}
