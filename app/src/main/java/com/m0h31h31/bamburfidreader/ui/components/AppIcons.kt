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

    val ContentCopy: ImageVector by lazy {
        materialIcon(
            "Outlined.ContentCopy",
            "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
        )
    }

    val ErrorOutline: ImageVector by lazy {
        materialIcon(
            "Outlined.ErrorOutline",
            "M11 15h2v2h-2v-2zm0-8h2v6h-2V7zm.99-5C6.48 2 2 6.48 2 12s4.48 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z"
        )
    }

    val MoreHoriz: ImageVector by lazy {
        materialIcon(
            "Filled.MoreHoriz",
            "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"
        )
    }

    val VolumeUp: ImageVector by lazy {
        materialIcon(
            "Filled.VolumeUp",
            "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1-3.29-2.5-4.03v8.05c1.5-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"
        )
    }

    val VolumeOff: ImageVector by lazy {
        materialIcon(
            "Outlined.VolumeOff",
            "M16.5 12c0-1.77-1-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.62 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73L16.25 17 17.52 15.73 4.27 3zM12 4L9.91 6.09 12 8.18V4z"
        )
    }

    val Label: ImageVector by lazy {
        materialIcon(
            "Outlined.Label",
            "M17.63 5.84C17.27 5.33 16.67 5 16 5H5c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h11c.67 0 1.27-.33 1.63-.84L22 12l-4.37-6.16zM16 17H5V7h11l3.55 5L16 17z"
        )
    }

    val Palette: ImageVector by lazy {
        materialIcon(
            "Outlined.Palette",
            "M12 3C7.03 3 3 6.58 3 11c0 3.31 2.69 6 6 6h1.5c.83 0 1.5.67 1.5 1.5S12.67 20 13.5 20H15c3.31 0 6-2.69 6-6.5C21 7.7 16.97 3 12 3zm3 15h-1.17c.11-.31.17-.65.17-1 0-1.66-1.34-3-3-3H9c-2.21 0-4-1.79-4-4 0-3.31 3.13-6 7-6s7 3.13 7 7.5C19 15.09 17.21 18 15 18z",
            "M6.5 10C5.67 10 5 9.33 5 8.5S5.67 7 6.5 7 8 7.67 8 8.5 7.33 10 6.5 10zm3-3C8.67 7 8 6.33 8 5.5S8.67 4 9.5 4 11 4.67 11 5.5 10.33 7 9.5 7zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 4 14.5 4 16 4.67 16 5.5 15.33 7 14.5 7zm3 3c-.83 0-1.5-.67-1.5-1.5S16.67 7 17.5 7 19 7.67 19 8.5 18.33 10 17.5 10z"
        )
    }

    val Scale: ImageVector by lazy {
        materialIcon(
            "Outlined.Scale",
            "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM7.5 13h2v4h-2v-4zm3.5-6h2v10h-2V7zm3.5 3h2v7h-2v-7z"
        )
    }

    val Straighten: ImageVector by lazy {
        materialIcon(
            "Outlined.Straighten",
            "M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 10H3V8h2v4h2V8h2v4h2V8h2v4h2V8h2v4h2V8h2v8z"
        )
    }

    val Thermostat: ImageVector by lazy {
        materialIcon(
            "Outlined.Thermostat",
            "M15 13.13V5c0-1.66-1.34-3-3-3S9 3.34 9 5v8.13C7.79 14 7 15.42 7 17c0 2.76 2.24 5 5 5s5-2.24 5-5c0-1.58-.79-3-2-3.87zM12 20c-1.65 0-3-1.35-3-3 0-1.01.51-1.9 1.28-2.45l.72-.52V5c0-.55.45-1 1-1s1 .45 1 1v9.03l.72.52C14.49 15.1 15 15.99 15 17c0 1.65-1.35 3-3 3z"
        )
    }

    val CalendarToday: ImageVector by lazy {
        materialIcon(
            "Outlined.CalendarToday",
            "M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z"
        )
    }

    val Logout: ImageVector by lazy {
        materialIcon(
            "Outlined.Logout",
            "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.59L17 17l5-5-5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"
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
