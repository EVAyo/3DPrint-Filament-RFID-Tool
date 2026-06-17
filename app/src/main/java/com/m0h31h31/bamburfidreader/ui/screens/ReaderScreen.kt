package com.m0h31h31.bamburfidreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import com.m0h31h31.bamburfidreader.model.CrealityMaterial
import com.m0h31h31.bamburfidreader.model.CrealityTagData
import com.m0h31h31.bamburfidreader.model.NfcUiState
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.model.ReaderBrand
import com.m0h31h31.bamburfidreader.model.SnapmakerTagData
import com.m0h31h31.bamburfidreader.logging.LogCollector
import com.m0h31h31.bamburfidreader.logging.logDebug
import com.m0h31h31.bamburfidreader.openTtsSettings
import com.m0h31h31.bamburfidreader.ui.components.AppIcons
import com.m0h31h31.bamburfidreader.ui.components.ColorSwatch
import com.m0h31h31.bamburfidreader.ui.components.InfoLine
import com.m0h31h31.bamburfidreader.ui.components.AppSlider
import com.m0h31h31.bamburfidreader.ui.components.AppSwitch
import com.m0h31h31.bamburfidreader.ui.components.AppCircularProgressIndicator
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.ModernCard
import com.m0h31h31.bamburfidreader.ui.components.ModernDivider
import com.m0h31h31.bamburfidreader.ui.components.ModernDot
import com.m0h31h31.bamburfidreader.ui.components.ModernPillButton
import com.m0h31h31.bamburfidreader.ui.components.ModernSectionHeader
import com.m0h31h31.bamburfidreader.ui.components.ModernWorkbenchTokens
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.BambuRfidReaderTheme
import com.m0h31h31.bamburfidreader.ui.theme.LocalAppUiStyle
import com.m0h31h31.bamburfidreader.util.parseColorValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.selection.SelectionContainer

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.merge(
                TextStyle(color = MaterialTheme.colorScheme.onSurface)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private const val MERIT_PREFS = "merit_prefs"
private const val MERIT_KEY_COUNT = "merit_count"
private const val MERIT_KEY_HMAC = "merit_hmac"
private val MERIT_HMAC_SECRET = byteArrayOf(
    0x4d, 0x65, 0x72, 0x69, 0x74, 0x5f, 0x42, 0x61,
    0x6d, 0x62, 0x75, 0x5f, 0x52, 0x46, 0x49, 0x44
)

private fun computeMeritHmac(count: Long): String {
    return try {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(MERIT_HMAC_SECRET, "HmacSHA256"))
        mac.doFinal(count.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { "" }
}

private fun loadMeritCount(context: android.content.Context): Long {
    val prefs = context.getSharedPreferences(MERIT_PREFS, android.content.Context.MODE_PRIVATE)
    val count = prefs.getLong(MERIT_KEY_COUNT, 0L)
    val storedHmac = prefs.getString(MERIT_KEY_HMAC, "").orEmpty()
    return if (storedHmac == computeMeritHmac(count)) count else 0L
}

private fun saveMeritCount(context: android.content.Context, count: Long) {
    context.getSharedPreferences(MERIT_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putLong(MERIT_KEY_COUNT, count)
        .putString(MERIT_KEY_HMAC, computeMeritHmac(count))
        .apply()
}

@Composable
fun ReaderScreen(
    state: NfcUiState,
    voiceEnabled: Boolean,
    ttsReady: Boolean,
    ttsLanguageReady: Boolean,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onTrayOutbound: (String) -> Unit,
    showRecoveryAction: Boolean,
    onAttemptRecovery: () -> Unit,
    onRemainingChange: (String, Float, Int?) -> Unit,
    onNotesChange: (String, String, String) -> Unit = { _, _, _ -> },
    readerBrand: ReaderBrand = ReaderBrand.BAMBU,
    onBrandChange: (ReaderBrand) -> Unit = {},
    readerCrealityTagData: CrealityTagData? = null,
    readerCrealityMaterial: CrealityMaterial? = null,
    readerSnapmakerTagData: SnapmakerTagData? = null,
    readerBrandStatus: String = "",
    onReportAnomaly: ((cardUid: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MODERN_WORKBENCH || uiStyle == AppUiStyle.MODERN_WORKBENCH_COMPOSE) {
        ModernReaderScreen(
            state = state,
            voiceEnabled = voiceEnabled,
            ttsReady = ttsReady,
            ttsLanguageReady = ttsLanguageReady,
            onVoiceEnabledChange = onVoiceEnabledChange,
            onTrayOutbound = onTrayOutbound,
            showRecoveryAction = showRecoveryAction,
            onAttemptRecovery = onAttemptRecovery,
            onRemainingChange = onRemainingChange,
            readerBrand = readerBrand,
            onBrandChange = onBrandChange,
            readerCrealityTagData = readerCrealityTagData,
            readerCrealityMaterial = readerCrealityMaterial,
            readerSnapmakerTagData = readerSnapmakerTagData,
            readerBrandStatus = readerBrandStatus,
            onReportAnomaly = onReportAnomaly,
            modifier = modifier
        )
        return
    }
    val meritToastPalette = if (uiStyle == AppUiStyle.MIUIX) {
        listOf(
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
        )
    } else {
        listOf(
            Color(0xFFE8F5E9) to Color(0xFF2E7D32),
            Color(0xFFE3F2FD) to Color(0xFF1565C0),
            Color(0xFFFFF3E0) to Color(0xFFEF6C00),
            Color(0xFFFCE4EC) to Color(0xFFC2185B),
            Color(0xFFEDE7F6) to Color(0xFF5E35B1),
            Color(0xFFE0F2F1) to Color(0xFF00695C)
        )
    }
    val context = LocalContext.current
    var logoTapCount by remember { mutableStateOf(0) }
    var logoLastTapAt by remember { mutableStateOf(0L) }
    var meritToastVisible by remember { mutableStateOf(false) }
    var meritToastNonce by remember { mutableStateOf(0) }
    var meritTotal by remember { mutableStateOf(loadMeritCount(context)) }
    var meritToastPaletteIndex by remember { mutableStateOf(0) }
    var showOutboundConfirm by remember(state.trayUidHex) { mutableStateOf(false) }
    var showAnomalyConfirm by remember { mutableStateOf(false) }
    var anomalyReportResult by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var editOriginalMaterial by remember { mutableStateOf(state.originalMaterial) }
    var editNotes by remember { mutableStateOf(state.notes) }
    val notesDebounceJob = remember { mutableStateOf<Job?>(null) }
    // 扫卡或 UID 切换时，从 state 同步最新的原始耗材与备注（DB 中存储的值）
    LaunchedEffect(state.trayUidHex, state.originalMaterial, state.notes) {
        editOriginalMaterial = state.originalMaterial
        editNotes = state.notes
    }
    val baseLogoTint = state.displayColors.firstNotNullOfOrNull { parseColorValue(it) }
        ?: parseColorValue(state.displayColorCode)
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    val logoTintColor = if (baseLogoTint.alpha < 0.45f) {
        baseLogoTint.copy(alpha = 0.75f)
    } else {
        baseLogoTint
    }
    val animatedLogoTintColor by animateColorAsState(
        targetValue = logoTintColor,
        animationSpec = tween(durationMillis = 550),
        label = "reader_logo_tint"
    )
    val meritToastShape = RoundedCornerShape(14.dp)
    val meritToastColors = meritToastPalette[meritToastPaletteIndex]
    val meritToastBackgroundColor by animateColorAsState(
        targetValue = meritToastColors.first.copy(alpha = 0.97f),
        animationSpec = tween(durationMillis = 280),
        label = "merit_toast_background"
    )
    val meritToastTextColor by animateColorAsState(
        targetValue = meritToastColors.second,
        animationSpec = tween(durationMillis = 280),
        label = "merit_toast_text"
    )
    val meritToastBorderColor by animateColorAsState(
        targetValue = meritToastColors.second.copy(alpha = 0.24f),
        animationSpec = tween(durationMillis = 280),
        label = "merit_toast_border"
    )
    LaunchedEffect(meritToastNonce) {
        if (meritToastNonce == 0) return@LaunchedEffect
        meritToastVisible = true
        delay(720)
        meritToastVisible = false
    }
    Surface(
        modifier = modifier.fillMaxSize().neuBackground(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 状态文本：Bambu 用 state.status，其他品牌用 readerBrandStatus（始终显示）
                val displayStatus = if (readerBrand == ReaderBrand.BAMBU) state.status else readerBrandStatus
                if (readerBrand != ReaderBrand.BAMBU || displayStatus.isNotBlank()) {
                    NeuPanel(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusIsWaiting = displayStatus.contains("正在") ||
                                displayStatus.contains("请稍候") ||
                                displayStatus.contains("准备就绪") ||
                                displayStatus.contains("请将目标") ||
                                displayStatus.contains("in progress", ignoreCase = true) ||
                                displayStatus.contains("please wait", ignoreCase = true) ||
                                displayStatus.contains("ready:", ignoreCase = true) ||
                                displayStatus.contains("keep the", ignoreCase = true) ||
                                displayStatus.contains("downloading", ignoreCase = true) ||
                                displayStatus.contains("waiting for rfid", ignoreCase = true)
                            if (statusIsWaiting) {
                                AppCircularProgressIndicator(modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = displayStatus,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            val voiceHint = when {
                                voiceEnabled && !ttsReady -> stringResource(R.string.voice_status_engine_not_ready)
                                voiceEnabled && !ttsLanguageReady -> stringResource(R.string.voice_status_language_unavailable)
                                voiceEnabled -> stringResource(R.string.voice_status_on)
                                else -> stringResource(R.string.voice_status_off)
                            }
                            val canOpenTtsSettings = voiceEnabled && (!ttsReady || !ttsLanguageReady)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppSwitch(
                                    checked = voiceEnabled,
                                    onCheckedChange = onVoiceEnabledChange,
                                    modifier = Modifier.scale(0.8f),
                                )
                                Text(
                                    text = if (canOpenTtsSettings) stringResource(R.string.action_voice_settings)
                                           else stringResource(R.string.voice_status_prefix, voiceHint),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (canOpenTtsSettings) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = if (canOpenTtsSettings) TextDecoration.Underline else null
                                    ),
                                    modifier = if (canOpenTtsSettings) {
                                        Modifier.padding(start = 6.dp).clickable {
                                            val opened = openTtsSettings(context)
                                            if (!opened) logDebug("无法打开语音设置")
                                        }
                                    } else {
                                        Modifier.padding(start = 6.dp)
                                    }
                                )
                            }
                        }
                    }
                }

                // ── 主卡片面板（全品牌显示）──────────────────────────────────────────
                val trayUidAvailable = state.trayUidHex.isNotBlank()
                val totalWeight = state.totalWeightGrams
                val hasWeight = totalWeight > 0
                val derivedGrams = if (hasWeight) {
                    (totalWeight * state.remainingPercent / 100.0).roundToInt()
                } else {
                    0
                }
                var gramsValue by remember(
                    state.trayUidHex,
                    state.remainingPercent,
                    state.totalWeightGrams
                ) {
                    mutableStateOf(derivedGrams.toFloat())
                }
                var gramsText by remember(
                    state.trayUidHex,
                    state.remainingPercent,
                    state.totalWeightGrams
                ) {
                    mutableStateOf(if (hasWeight && derivedGrams > 0) derivedGrams.toString() else "")
                }
                val gramsInt = gramsValue.roundToInt().coerceIn(0, totalWeight.coerceAtLeast(0))
                val percentValue = if (hasWeight) {
                    ((gramsInt * 100f / totalWeight) * 10).roundToInt() / 10f
                } else {
                    state.remainingPercent
                }
                
                // 添加防抖机制
                val scope = rememberCoroutineScope()
                val debounceJob = remember {
                    mutableStateOf<Job?>(null)
                }
                NeuPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ── 左侧色块 ──────────────────────────────────────────────
                            Box(modifier = Modifier.size(120.dp)) {
                                when (readerBrand) {
                                    ReaderBrand.BAMBU -> {
                                        ColorSwatch(
                                            colorValues = state.displayColors,
                                            colorType = state.displayColorType,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    val now = System.currentTimeMillis()
                                                    if (now - logoLastTapAt > 1500) logoTapCount = 0
                                                    logoLastTapAt = now
                                                    logoTapCount += 1
                                                    if (logoTapCount >= 5) {
                                                        logoTapCount = 0
                                                        val result = LogCollector.packageLogs(context)
                                                        logDebug(result)
                                                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                        )
                                        if (trayUidAvailable) {
                                            val outboundContainerColor = if (uiStyle == AppUiStyle.MIUIX) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer
                                            val outboundContentColor  = if (uiStyle == AppUiStyle.MIUIX) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(outboundContainerColor)
                                                    .border(1.dp, outboundContentColor.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                                    .clickable { showOutboundConfirm = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.reader_outbound),
                                                    color = outboundContentColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    ReaderBrand.CREALITY -> {
                                        val hex = readerCrealityTagData?.colorHex ?: ""
                                        ColorSwatch(
                                            colorValues = if (hex.isNotBlank()) listOf(hex) else emptyList(),
                                            colorType = "",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    ReaderBrand.SNAPMAKER -> {
                                        val hex = readerSnapmakerTagData?.let { "%06X".format(it.rgb1) } ?: ""
                                        ColorSwatch(
                                            colorValues = if (hex.isNotBlank()) listOf(hex) else emptyList(),
                                            colorType = "",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            // ── 右侧文字信息 ──────────────────────────────────────────
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                when (readerBrand) {
                                    ReaderBrand.BAMBU -> {
                                        Text(
                                            // 显示完整子类型（如 PLA Basic），匹配不到时退回大类型。
                                            text = state.displayDetailedType
                                                .ifBlank { state.displayType }
                                                .ifBlank { stringResource(R.string.label_unknown) },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = modifier.padding(3.dp),
                                            fontSize = 18.sp,
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = state.displayColorName.ifBlank { stringResource(R.string.label_unknown) },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = modifier.padding(3.dp)
                                            )
                                            Text(text = "-", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = modifier.padding(3.dp))
                                            Text(
                                                // 显示数字耗材色号（fila_color_code，如 10300），匹配不到时退回短色号。
                                                text = state.displayFilaColorCode
                                                    .ifBlank { state.displayColorCode }
                                                    .ifBlank { stringResource(R.string.label_unknown) },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = modifier.padding(3.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                QuantityButtonGroup(
                                                    value = gramsText,
                                                    enabled = trayUidAvailable && hasWeight,
                                                    onValueChange = { text ->
                                                        val digits = text.filter { it.isDigit() }
                                                        if (trayUidAvailable && hasWeight) {
                                                            val next = digits.toIntOrNull()?.coerceIn(0, totalWeight) ?: 0
                                                            gramsValue = next.toFloat()
                                                            gramsText = if (digits.isEmpty() || next == 0) "" else next.toString()
                                                            debounceJob.value?.cancel()
                                                            debounceJob.value = scope.launch {
                                                                delay(500)
                                                                val finalGrams = gramsText.toIntOrNull()?.coerceIn(0, totalWeight) ?: 0
                                                                val finalPercent = if (totalWeight > 0) ((finalGrams * 100f / totalWeight) * 10).roundToInt() / 10f else 0f
                                                                onRemainingChange(state.trayUidHex, finalPercent, finalGrams)
                                                            }
                                                        } else {
                                                            gramsText = digits
                                                        }
                                                    },
                                                    onDecrease = {
                                                        val next = (gramsInt - 1).coerceAtLeast(0)
                                                        gramsValue = next.toFloat(); gramsText = next.toString()
                                                        onRemainingChange(state.trayUidHex, (next * 100f / totalWeight), next)
                                                    },
                                                    onIncrease = {
                                                        val next = (gramsInt + 1).coerceAtMost(totalWeight)
                                                        gramsValue = next.toFloat(); gramsText = next.toString()
                                                        onRemainingChange(state.trayUidHex, (next * 100f / totalWeight), next)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                    ReaderBrand.CREALITY -> {
                                        val mat = readerCrealityMaterial
                                        Text(
                                            text = mat?.brand?.ifBlank { "-" } ?: "-",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = modifier.padding(3.dp),
                                            fontSize = 18.sp,
                                        )
                                        Text(
                                            text = mat?.materialType?.ifBlank { "-" } ?: "-",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = modifier.padding(3.dp)
                                        )
                                        Text(
                                            text = mat?.name?.ifBlank { "-" } ?: "-",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = modifier.padding(3.dp)
                                        )
                                    }
                                    ReaderBrand.SNAPMAKER -> {
                                        val d = readerSnapmakerTagData
                                        Text(
                                            text = d?.vendor?.ifBlank { "-" } ?: "-",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = modifier.padding(3.dp),
                                            fontSize = 18.sp,
                                        )
                                        Text(
                                            text = if (d != null) "${d.mainType} ${d.subType}".trim().ifBlank { "-" } else "-",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = modifier.padding(3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── 余量滑块：仅拓竹品牌显示 ─────────────────────────────────
                        if (readerBrand == ReaderBrand.BAMBU) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).padding(vertical = 3.dp)) {
                                    AppSlider(
                                        value = gramsValue,
                                        onValueChange = { value ->
                                            if (trayUidAvailable && hasWeight) {
                                                val next = value.roundToInt().coerceIn(0, totalWeight)
                                                gramsValue = next.toFloat()
                                                gramsText = next.toString()
                                            }
                                        },
                                        valueRange = 0f..(if (hasWeight) totalWeight.toFloat() else 1f),
                                        enabled = trayUidAvailable && hasWeight,
                                        modifier = Modifier.fillMaxWidth().height(30.dp),
                                        onValueChangeFinished = {
                                            if (trayUidAvailable && hasWeight) {
                                                val finalGrams = gramsValue.roundToInt().coerceIn(0, totalWeight)
                                                val finalPercent = if (totalWeight > 0) ((finalGrams * 100f / totalWeight) * 10).roundToInt() / 10f else 0f
                                                onRemainingChange(state.trayUidHex, finalPercent, finalGrams)
                                            }
                                        }
                                    )
                                }
                                Text(
                                    text = String.format("%.1f%%", percentValue),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp).width(56.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
                if (showRecoveryAction) {
                    NeuButton(
                        text = stringResource(R.string.reader_recovery),
                        onClick = onAttemptRecovery,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showOutboundConfirm) {
                    AlertDialog(
                        onDismissRequest = { showOutboundConfirm = false },
                        title = { Text(stringResource(R.string.reader_outbound_confirm_title)) },
                        text = { Text(stringResource(R.string.reader_outbound_confirm_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onTrayOutbound(state.trayUidHex)
                                    showOutboundConfirm = false
                                }
                            ) {
                                Text(stringResource(R.string.action_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showOutboundConfirm = false }
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }
                if (showAnomalyConfirm && onReportAnomaly != null) {
                    val reportSuccessText = stringResource(R.string.anomaly_report_success)
                    val reportFailText = stringResource(R.string.anomaly_report_fail)
                    AlertDialog(
                        onDismissRequest = { showAnomalyConfirm = false },
                        title = { Text(stringResource(R.string.anomaly_dialog_title)) },
                        text = { Text(stringResource(R.string.anomaly_dialog_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val cardUid = state.uidHex
                                    showAnomalyConfirm = false
                                    coroutineScope.launch {
                                        onReportAnomaly(cardUid)
                                        anomalyReportResult = reportSuccessText
                                        kotlinx.coroutines.delay(3000)
                                        anomalyReportResult = ""
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.anomaly_dialog_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAnomalyConfirm = false }) {
                                Text(stringResource(R.string.anomaly_dialog_cancel))
                            }
                        }
                    )
                }
                if (anomalyReportResult.isNotBlank()) {
                    Text(
                        text = anomalyReportResult,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

//                if (state.blockHexes.any { it.isNotBlank() }) {
//                    Card(modifier = Modifier.fillMaxWidth()) {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(12.dp),
//                            verticalArrangement = Arrangement.spacedBy(4.dp)
//                        ) {
//                            Text(
//                                text = "原始区块（含Trailer）",
//                                style = MaterialTheme.typography.titleSmall
//                            )
//                            state.blockHexes.forEachIndexed { index, hex ->
//                                if (hex.isNotBlank()) {
//                                    val isTrailer = index % 4 == 3
//                                    InfoLine(
//                                        label = if (isTrailer) "Block$index (Trailer)" else "Block$index",
//                                        value = hex,
//                                        style = MaterialTheme.typography.bodySmall
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }

                // ── 品牌切换按钮行 ─────────────────────────────────────────────────────
                NeuPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val brands = listOf(
                            ReaderBrand.BAMBU    to stringResource(R.string.tab_tag),
                            ReaderBrand.CREALITY to stringResource(R.string.tab_creality),
                            ReaderBrand.SNAPMAKER to stringResource(R.string.tab_snapmaker)
                        )
                        brands.forEach { (brand, label) ->
                            val selected = readerBrand == brand
                            val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                          else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .clickable { onBrandChange(brand) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = textColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // ── 信息面板：填满剩余高度，支持滚动 ──────────────────────────────────
                NeuPanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val infoScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(infoScrollState)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                when (readerBrand) {
                                    ReaderBrand.BAMBU -> {
                                        SelectionContainer {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = stringResource(R.string.label_other_info),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        InfoLine(
                                            label = stringResource(R.string.reader_card_uid),
                                            value = state.uidHex.ifBlank { "-" },
                                            style = MaterialTheme.typography.bodySmall,
                                            inline = true
                                        )
                                        state.secondaryFields.forEach { field ->
                                            InfoLine(
                                                label = field.label,
                                                value = field.value,
                                                style = MaterialTheme.typography.bodySmall,
                                                inline = true
                                            )
                                        }
                                        } // end SelectionContainer Column
                                        } // end SelectionContainer
                                        if (trayUidAvailable) {
                                            CompactField(
                                                value = editOriginalMaterial,
                                                onValueChange = { newVal ->
                                                    editOriginalMaterial = newVal
                                                    notesDebounceJob.value?.cancel()
                                                    notesDebounceJob.value = scope.launch {
                                                        delay(500)
                                                        onNotesChange(state.trayUidHex, editOriginalMaterial, editNotes)
                                                    }
                                                },
                                                label = stringResource(R.string.reader_original_material),
                                                modifier = Modifier.fillMaxWidth(0.7f)
                                            )
                                            CompactField(
                                                value = editNotes,
                                                onValueChange = { newVal ->
                                                    editNotes = newVal
                                                    notesDebounceJob.value?.cancel()
                                                    notesDebounceJob.value = scope.launch {
                                                        delay(500)
                                                        onNotesChange(state.trayUidHex, editOriginalMaterial, editNotes)
                                                    }
                                                },
                                                label = stringResource(R.string.reader_notes),
                                                modifier = Modifier.fillMaxWidth(0.7f)
                                            )
                                        }
                                    }
                                    ReaderBrand.CREALITY -> {
                                        SelectionContainer {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = stringResource(R.string.label_other_info),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (readerCrealityTagData != null) {
                                            val d = readerCrealityTagData
                                            if (d.uidHex.isNotBlank())
                                                InfoLine(label = stringResource(R.string.reader_label_card_uid),  value = d.uidHex, style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_material_id),   value = d.materialId, style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_color),     value = "#${d.colorHex}", style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_weight),     value = d.weight, style = MaterialTheme.typography.bodySmall, inline = true)
                                            if (d.serial.isNotBlank())
                                                InfoLine(label = stringResource(R.string.reader_label_serial), value = d.serial, style = MaterialTheme.typography.bodySmall, inline = true)
                                            if (d.vendorId.isNotBlank())
                                                InfoLine(label = stringResource(R.string.reader_label_vendor_id), value = d.vendorId, style = MaterialTheme.typography.bodySmall, inline = true)
                                            if (d.mfDate.isNotBlank())
                                                InfoLine(label = stringResource(R.string.reader_label_mfdate), value = d.mfDate, style = MaterialTheme.typography.bodySmall, inline = true)
                                            if (d.batch.isNotBlank())
                                                InfoLine(label = stringResource(R.string.reader_label_batch),   value = d.batch, style = MaterialTheme.typography.bodySmall, inline = true)
                                            readerCrealityMaterial?.let { mat ->
                                                if (mat.minTemp > 0 || mat.maxTemp > 0)
                                                    InfoLine(label = stringResource(R.string.reader_label_print_temp), value = "${mat.minTemp}–${mat.maxTemp} °C", style = MaterialTheme.typography.bodySmall, inline = true)
                                                if (mat.diameter.isNotBlank())
                                                    InfoLine(label = stringResource(R.string.reader_label_diameter), value = "${mat.diameter} mm", style = MaterialTheme.typography.bodySmall, inline = true)
                                            }
                                        } else {
                                            Text(
                                                text = stringResource(R.string.reader_creality_scan_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        } // end Column
                                        } // end SelectionContainer
                                    }
                                    ReaderBrand.SNAPMAKER -> {
                                        SelectionContainer {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = stringResource(R.string.reader_snapmaker_info_title),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (readerSnapmakerTagData != null) {
                                            val d = readerSnapmakerTagData
                                            InfoLine(label = stringResource(R.string.reader_label_brand),     value = d.vendor, style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_type),     value = "${d.mainType} ${d.subType}".trim(), style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_color_count),   value = "${d.colorCount}", style = MaterialTheme.typography.bodySmall, inline = true)
                                            val colorHexStr = buildString {
                                                val colors = listOf(d.rgb1, d.rgb2, d.rgb3, d.rgb4, d.rgb5)
                                                    .take(d.colorCount.coerceIn(1, 5))
                                                append(colors.joinToString(" ") { "#%06X".format(it) })
                                            }
                                            InfoLine(label = stringResource(R.string.reader_label_color),     value = colorHexStr, style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_diameter),     value = if (d.diameter > 0) "${"%.2f".format(d.diameter / 100.0)} mm" else "-", style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_weight),     value = if (d.weight > 0) "${d.weight} g" else "-", style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_print_temp), value = if (d.hotendMinTemp > 0) "${d.hotendMinTemp}–${d.hotendMaxTemp} °C" else "-", style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_bed_temp), value = if (d.bedTemp > 0) "${d.bedTemp} °C" else "-", style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_drying),     value = if (d.dryingTemp > 0) "${d.dryingTemp} °C / ${d.dryingTime} h" else "-", style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_mfdate), value = d.mfDate, style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_official), value = if (d.isOfficial) "✓" else "✗", style = MaterialTheme.typography.bodySmall, inline = true)
                                            InfoLine(label = stringResource(R.string.reader_label_card_uid),  value = d.uidHex, style = MaterialTheme.typography.bodySmall, inline = true)
                                        } else {
                                            Text(
                                                text = stringResource(R.string.reader_snapmaker_scan_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        } // end Column
                                        } // end SelectionContainer
                                    }
                                }
                            }
                            // 右侧 Logo / 品牌占位
                            Box(
                                modifier = Modifier.size(88.dp, 250.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (readerBrand) {
                                    ReaderBrand.BAMBU -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                        if (onReportAnomaly != null && state.uidHex.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .width(74.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFD32F2F))
                                                    .clickable { showAnomalyConfirm = true }
                                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.reader_anomaly_report_btn),
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = R.drawable.logo_mark),
                                            contentDescription = stringResource(R.string.content_logo),
                                            colorFilter = ColorFilter.tint(animatedLogoTintColor),
                                            modifier = Modifier
                                                .size(80.dp, 200.dp)
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                                ) {
                                                    meritToastVisible = false
                                                    meritTotal += 1
                                                    saveMeritCount(context, meritTotal)
                                                    meritToastPaletteIndex = Random.nextInt(meritToastPalette.size)
                                                    meritToastNonce += 1
                                                }
                                        )
                                        }
                                    }
                                    ReaderBrand.CREALITY -> {
                                        val crealityHex = readerCrealityTagData?.colorHex ?: ""
                                        val crealityBase = parseColorValue(crealityHex)
                                            ?: MaterialTheme.colorScheme.onSurfaceVariant
                                        val crealityTint = if (crealityBase.alpha < 0.45f) crealityBase.copy(alpha = 0.75f) else crealityBase
                                        val animatedCrealityTint by animateColorAsState(
                                            targetValue = crealityTint,
                                            animationSpec = tween(durationMillis = 550),
                                            label = "creality_logo_tint"
                                        )
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = R.drawable.creality_logo_mask),
                                            contentDescription = stringResource(R.string.content_logo_creality),
                                            colorFilter = ColorFilter.tint(animatedCrealityTint),
                                            modifier = Modifier.size(80.dp, 250.dp)
                                        )
                                    }
                                    ReaderBrand.SNAPMAKER -> {
                                        val snapHex = readerSnapmakerTagData?.let { "%06X".format(it.rgb1) } ?: ""
                                        val snapBase = parseColorValue(snapHex)
                                            ?: MaterialTheme.colorScheme.onSurfaceVariant
                                        val snapTint = if (snapBase.alpha < 0.45f) snapBase.copy(alpha = 0.75f) else snapBase
                                        val animatedSnapTint by animateColorAsState(
                                            targetValue = snapTint,
                                            animationSpec = tween(durationMillis = 550),
                                            label = "snapmaker_logo_tint"
                                        )
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = R.drawable.snapmaker_logo_mask),
                                            contentDescription = stringResource(R.string.content_logo_snapmaker),
                                            colorFilter = ColorFilter.tint(animatedSnapTint),
                                            modifier = Modifier.size(80.dp, 250.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
//            BoostFooter(
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .padding(horizontal = 2.dp, vertical = 0.dp)
//            )
            androidx.compose.animation.AnimatedVisibility(
                visible = meritToastVisible,
                enter = fadeIn(
                    animationSpec = tween(180)
                ) + scaleIn(
                    initialScale = 0.86f,
                    animationSpec = tween(
                        durationMillis = 260,
                        easing = FastOutSlowInEasing
                    )
                ) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(260)
                ),
                exit = fadeOut(
                    animationSpec = tween(150)
                ) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(150)
                ) + slideOutVertically(
                    targetOffsetY = { -it / 3 },
                    animationSpec = tween(150)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 132.dp, end = 18.dp)
                    .zIndex(10f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(meritToastShape)
                        .background(meritToastBackgroundColor)
                        .border(
                            width = 1.dp,
                            color = meritToastBorderColor,
                            shape = meritToastShape
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.reader_merit_format,
                            meritTotal.coerceAtLeast(1)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = meritToastTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityButtonGroup(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val leftShape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
    val rightShape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val leftBg = if (enabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val rightBg = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val valueBg = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .height(44.dp)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(color = MaterialTheme.colorScheme.surface, shape = shape)
    ) {
        Box(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight()
                .clip(leftShape)
                .background(leftBg, shape = leftShape)
                .clickable(enabled = enabled, onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .background(borderColor)
                .size(width = 1.dp, height = 44.dp)
        )
        Box(
            modifier = Modifier
                .weight(2.1f)
                .fillMaxHeight()
                .background(valueBg),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(start = 6.dp, end = 28.dp)
                )
                Text(
                    text = stringResource(R.string.unit_grams),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .background(borderColor)
                .size(width = 1.dp, height = 44.dp)
        )
        Box(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight()
                .clip(rightShape)
                .background(rightBg, shape = rightShape)
                .clickable(enabled = enabled, onClick = onIncrease),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModernReaderScreen(
    state: NfcUiState,
    voiceEnabled: Boolean,
    ttsReady: Boolean,
    ttsLanguageReady: Boolean,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onTrayOutbound: (String) -> Unit,
    showRecoveryAction: Boolean,
    onAttemptRecovery: () -> Unit,
    onRemainingChange: (String, Float, Int?) -> Unit,
    readerBrand: ReaderBrand,
    onBrandChange: (ReaderBrand) -> Unit,
    readerCrealityTagData: CrealityTagData?,
    readerCrealityMaterial: CrealityMaterial?,
    readerSnapmakerTagData: SnapmakerTagData?,
    readerBrandStatus: String,
    onReportAnomaly: ((cardUid: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val displayStatus = if (readerBrand == ReaderBrand.BAMBU) state.status else readerBrandStatus
    val statusText = displayStatus.ifBlank { stringResource(R.string.status_waiting_tag) }
    val trayUidAvailable = state.trayUidHex.isNotBlank()
    val totalWeight = state.totalWeightGrams
    val hasWeight = totalWeight > 0
    val initialGrams = when {
        state.remainingGrams > 0 -> state.remainingGrams
        hasWeight -> (totalWeight * state.remainingPercent / 100f).roundToInt()
        else -> 0
    }.coerceAtLeast(0)
    var gramsText by remember(state.trayUidHex, state.remainingPercent, state.remainingGrams, totalWeight) {
        mutableStateOf(if (initialGrams > 0) initialGrams.toString() else "")
    }
    val gramsInt = gramsText.toIntOrNull()?.coerceIn(0, totalWeight.coerceAtLeast(0)) ?: 0
    val percentValue = if (hasWeight) {
        ((gramsInt * 100f / totalWeight) * 10).roundToInt() / 10f
    } else {
        state.remainingPercent
    }
    var showOutboundConfirm by remember(state.trayUidHex) { mutableStateOf(false) }
    var showAnomalyConfirm by remember { mutableStateOf(false) }
    var anomalyReportResult by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ModernWorkbenchTokens.Page
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ModernDot(
                        color = if (statusText.contains("成功") || statusText.contains("success", ignoreCase = true)) {
                            ModernWorkbenchTokens.Success
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        size = 16.dp
                    )
                    Text(
                        text = statusText,
                        color = ModernWorkbenchTokens.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier.clickable { onVoiceEnabledChange(!voiceEnabled) },
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ModernWorkbenchTokens.Line)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (voiceEnabled) AppIcons.VolumeUp else AppIcons.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (voiceEnabled) MaterialTheme.colorScheme.primary else ModernWorkbenchTokens.Muted
                        )
                        Text(
                            text = when {
                                voiceEnabled && !ttsReady -> stringResource(R.string.voice_status_engine_not_ready)
                                voiceEnabled && !ttsLanguageReady -> stringResource(R.string.voice_status_language_unavailable)
                                voiceEnabled -> stringResource(R.string.voice_status_on)
                                else -> stringResource(R.string.voice_status_off)
                            },
                            color = if (voiceEnabled) MaterialTheme.colorScheme.primary else ModernWorkbenchTokens.Ink,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            ModernCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val swatchValues = when (readerBrand) {
                            ReaderBrand.BAMBU -> state.displayColors
                            ReaderBrand.CREALITY -> readerCrealityTagData?.colorHex?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
                            ReaderBrand.SNAPMAKER -> readerSnapmakerTagData?.let { listOf("%06X".format(it.rgb1)) }.orEmpty()
                        }
                        ColorSwatch(
                            colorValues = swatchValues,
                            colorType = if (readerBrand == ReaderBrand.BAMBU) state.displayColorType else "",
                            modifier = Modifier.size(68.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val title = when (readerBrand) {
                                ReaderBrand.BAMBU -> state.displayDetailedType.ifBlank { state.displayType }.ifBlank { stringResource(R.string.label_unknown) }
                                ReaderBrand.CREALITY -> readerCrealityMaterial?.name?.ifBlank { null } ?: readerCrealityMaterial?.materialType ?: "-"
                                ReaderBrand.SNAPMAKER -> readerSnapmakerTagData?.let { "${it.mainType} ${it.subType}".trim() }?.ifBlank { null } ?: "-"
                            }
                            val subtitle = when (readerBrand) {
                                ReaderBrand.BAMBU -> listOf(
                                    state.displayColorName,
                                    state.displayFilaColorCode.ifBlank { state.displayColorCode }
                                ).filter { it.isNotBlank() }.joinToString(" - ")
                                ReaderBrand.CREALITY -> listOf(
                                    readerCrealityMaterial?.brand.orEmpty(),
                                    readerCrealityTagData?.materialId.orEmpty()
                                ).filter { it.isNotBlank() }.joinToString(" - ")
                                ReaderBrand.SNAPMAKER -> readerSnapmakerTagData?.vendor.orEmpty()
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ModernWorkbenchTokens.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle.ifBlank { stringResource(R.string.label_unknown) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = ModernWorkbenchTokens.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (readerBrand == ReaderBrand.BAMBU && state.uidHex.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFBFBFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ModernWorkbenchTokens.Line)
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.reader_label_card_uid)}: ${state.uidHex}",
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        color = ModernWorkbenchTokens.Muted,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (readerBrand == ReaderBrand.BAMBU && trayUidAvailable) {
                            ModernIconButton(
                                text = stringResource(R.string.reader_outbound),
                                icon = AppIcons.Logout,
                                onClick = { showOutboundConfirm = true },
                                selected = true,
                                compact = true
                            )
                        }
                    }
                    if (readerBrand == ReaderBrand.BAMBU) {
                        ModernDivider()
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        var barWidthPx by remember { mutableStateOf(0f) }
                        var isDragging by remember { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.inventory_remaining_grams_label),
                                        color = ModernWorkbenchTokens.Muted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = String.format("%.1f%%", percentValue),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (hasWeight) "$gramsInt${stringResource(R.string.unit_grams)} / ${totalWeight}${stringResource(R.string.unit_grams)}" else "-",
                                    color = ModernWorkbenchTokens.Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            val thumbSizeDp = if (isDragging) 22.dp else 16.dp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(maxOf(thumbSizeDp, 8.dp))
                                    .onSizeChanged { barWidthPx = it.width.toFloat() }
                                    .pointerInput(trayUidAvailable, hasWeight, totalWeight, barWidthPx) {
                                        if (!trayUidAvailable || !hasWeight || totalWeight <= 0 || barWidthPx == 0f) return@pointerInput
                                        awaitEachGesture {
                                            val down = awaitFirstDown()
                                            isDragging = true
                                            val applyX = { x: Float ->
                                                val clamped = x.coerceIn(0f, barWidthPx)
                                                val newGrams = (clamped / barWidthPx * totalWeight).roundToInt().coerceIn(0, totalWeight)
                                                gramsText = newGrams.toString()
                                                onRemainingChange(state.trayUidHex, newGrams * 100f / totalWeight, newGrams)
                                            }
                                            applyX(down.position.x)
                                            do {
                                                val event = awaitPointerEvent()
                                                val ch = event.changes.firstOrNull() ?: break
                                                if (ch.pressed) { ch.consume(); applyX(ch.position.x) }
                                            } while (event.changes.any { it.pressed })
                                            isDragging = false
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isDragging) 10.dp else 8.dp)
                                        .align(Alignment.Center)
                                        .background(ModernWorkbenchTokens.Line, RoundedCornerShape(999.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((percentValue / 100f).coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                                    )
                                }
                                if (trayUidAvailable && hasWeight && barWidthPx > 0f) {
                                    val fraction = (percentValue / 100f).coerceIn(0f, 1f)
                                    val thumbOffsetDp = with(density) { (barWidthPx * fraction).toDp() } - thumbSizeDp / 2
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = thumbOffsetDp.coerceAtLeast(0.dp))
                                            .size(thumbSizeDp)
                                            .shadow(if (isDragging) 4.dp else 1.dp, androidx.compose.foundation.shape.CircleShape)
                                            .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ModernWorkbenchTokens.Line)
                        ) {
                            QuantityButtonGroup(
                                value = gramsText.ifBlank { "0" },
                                enabled = trayUidAvailable && hasWeight,
                                onValueChange = { text ->
                                    val digits = text.filter { it.isDigit() }
                                    val next = digits.toIntOrNull()?.coerceIn(0, totalWeight) ?: 0
                                    gramsText = if (digits.isEmpty()) "" else next.toString()
                                    if (trayUidAvailable && hasWeight) {
                                        val nextPercent = ((next * 100f / totalWeight) * 10).roundToInt() / 10f
                                        onRemainingChange(state.trayUidHex, nextPercent, next)
                                    }
                                },
                                onDecrease = {
                                    val next = (gramsInt - 1).coerceAtLeast(0)
                                    gramsText = next.toString()
                                    onRemainingChange(state.trayUidHex, (next * 100f / totalWeight), next)
                                },
                                onIncrease = {
                                    val next = (gramsInt + 1).coerceAtMost(totalWeight)
                                    gramsText = next.toString()
                                    onRemainingChange(state.trayUidHex, (next * 100f / totalWeight), next)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ReaderBrand.BAMBU to stringResource(R.string.tab_tag),
                    ReaderBrand.CREALITY to stringResource(R.string.tab_creality),
                    ReaderBrand.SNAPMAKER to stringResource(R.string.tab_snapmaker)
                ).forEach { (brand, label) ->
                    ModernIconButton(
                        text = label,
                        icon = when (brand) {
                            ReaderBrand.BAMBU -> ImageVector.vectorResource(R.drawable.bambu)
                            ReaderBrand.CREALITY -> ImageVector.vectorResource(R.drawable.chuangxiang)
                            ReaderBrand.SNAPMAKER -> ImageVector.vectorResource(R.drawable.snapmaker)
                        },
                        onClick = { onBrandChange(brand) },
                        selected = readerBrand == brand,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ModernInfoList(
                onReportAnomaly = if (onReportAnomaly != null && state.uidHex.isNotBlank()) {
                    { showAnomalyConfirm = true }
                } else null,
                rows = when (readerBrand) {
                    ReaderBrand.BAMBU -> buildList {
                        val colorHex = state.displayColors.firstOrNull()
                            ?.let { if (it.length == 9) it.removeRange(7, 9) else it }
                            ?.ifBlank { null }
                            ?: "-"
                        add(stringResource(R.string.reader_label_color) to colorHex)
                        add(stringResource(R.string.reader_label_weight) to if (state.totalWeightGrams > 0) "${state.totalWeightGrams} ${stringResource(R.string.unit_grams)}" else "-")
                        state.secondaryFields.take(8).forEach { add(it.label to it.value) }
                    }
                    ReaderBrand.CREALITY -> buildList {
                        val d = readerCrealityTagData
                        add(stringResource(R.string.reader_label_material_id) to (d?.materialId ?: "-"))
                        add(stringResource(R.string.reader_label_color) to (d?.colorHex?.let { "#$it" } ?: "-"))
                        add(stringResource(R.string.reader_label_weight) to (d?.weight ?: "-"))
                        readerCrealityMaterial?.let {
                            add(stringResource(R.string.reader_label_print_temp) to "${it.minTemp}-${it.maxTemp} °C")
                            add(stringResource(R.string.reader_label_diameter) to "${it.diameter} mm")
                        }
                    }
                    ReaderBrand.SNAPMAKER -> buildList {
                        val d = readerSnapmakerTagData
                        add(stringResource(R.string.reader_label_brand) to (d?.vendor ?: "-"))
                        add(stringResource(R.string.reader_label_weight) to (d?.weight?.takeIf { it > 0 }?.let { "$it g" } ?: "-"))
                        add(stringResource(R.string.reader_label_diameter) to (d?.diameter?.takeIf { it > 0 }?.let { "%.2f mm".format(it / 100.0) } ?: "-"))
                        add(stringResource(R.string.reader_label_print_temp) to (d?.hotendMinTemp?.takeIf { it > 0 }?.let { "$it-${d.hotendMaxTemp} °C" } ?: "-"))
                        add(stringResource(R.string.reader_label_bed_temp) to (d?.bedTemp?.takeIf { it > 0 }?.let { "$it °C" } ?: "-"))
                    }
                }
            )
        }
    }

    if (showOutboundConfirm) {
        AlertDialog(
            onDismissRequest = { showOutboundConfirm = false },
            title = { Text(stringResource(R.string.reader_outbound_confirm_title)) },
            text = { Text(stringResource(R.string.reader_outbound_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTrayOutbound(state.trayUidHex)
                        showOutboundConfirm = false
                    }
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showOutboundConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    if (showAnomalyConfirm && onReportAnomaly != null) {
        val reportSuccessText = stringResource(R.string.anomaly_report_success)
        AlertDialog(
            onDismissRequest = { showAnomalyConfirm = false },
            title = { Text(stringResource(R.string.anomaly_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.anomaly_dialog_message))
                    if (anomalyReportResult.isNotBlank()) {
                        Text(anomalyReportResult, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReportAnomaly(state.uidHex)
                        anomalyReportResult = reportSuccessText
                        showAnomalyConfirm = false
                    }
                ) { Text(stringResource(R.string.anomaly_dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAnomalyConfirm = false }) {
                    Text(stringResource(R.string.anomaly_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun ModernIconButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    filled: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val accent = if (danger) ModernWorkbenchTokens.Danger else MaterialTheme.colorScheme.primary
    val background = when {
        filled -> accent
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> Color.White
    }
    val contentColor = when {
        filled -> Color.White
        danger -> ModernWorkbenchTokens.Danger
        selected -> MaterialTheme.colorScheme.primary
        else -> ModernWorkbenchTokens.Ink
    }
    Surface(
        modifier = modifier
            .height(if (compact) 36.dp else 44.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) background else Color(0xFFF4F4F5),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                filled -> Color.Transparent
                selected || danger -> accent
                else -> ModernWorkbenchTokens.Line
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                tint = if (enabled) contentColor else ModernWorkbenchTokens.Muted
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = if (enabled) contentColor else ModernWorkbenchTokens.Muted,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModernInfoList(
    rows: List<Pair<String, String>>,
    onReportAnomaly: (() -> Unit)? = null
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    ModernCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reader_filament_detail_title),
                    color = ModernWorkbenchTokens.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (onReportAnomaly != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onReportAnomaly() }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.ErrorOutline,
                            contentDescription = stringResource(R.string.reader_anomaly_report_btn),
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFD32F2F)
                        )
                        Text(
                            text = stringResource(R.string.reader_anomaly_report_btn).replace("\n", ""),
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                val value = row.second.ifBlank { "-" }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(value))
                                Toast.makeText(context, value, Toast.LENGTH_SHORT).show()
                            }
                        )
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = modernInfoIcon(index),
                        contentDescription = row.first,
                        modifier = Modifier.size(18.dp),
                        tint = ModernWorkbenchTokens.Muted
                    )
                    Text(
                        text = row.first,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ModernWorkbenchTokens.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = row.second.ifBlank { "-" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ModernWorkbenchTokens.Ink,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (index != rows.lastIndex) {
                    ModernDivider()
                }
            }
        }
    }
}

private fun modernInfoIcon(index: Int): ImageVector = when (index) {
    0 -> AppIcons.Palette          // 颜色
    1 -> AppIcons.FitnessCenter    // 重量
    2 -> AppIcons.FormatColorFill  // 颜色类型
    3 -> AppIcons.Scale            // 耗材重量
    4 -> AppIcons.Straighten       // 耗材直径
    5 -> AppIcons.Thermostat       // 烘干温度
    else -> AppIcons.CalendarToday
}


@Composable
private fun BoostFooter(boostLink: String, modifier: Modifier = Modifier) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    if (boostLink.isBlank()) return
    TextButton(
        onClick = { uriHandler.openUri(boostLink) },
        modifier = modifier.padding(0.dp)
    ) {
        Text(text = stringResource(R.string.action_boost_open_bambu))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewReaderScreen() {
    BambuRfidReaderTheme {
        ReaderScreen(
            state = NfcUiState(
                status = "Read success",
                displayType = "Support For PLA-PETG",
                displayColorName = "Orange",
                displayColorCode = "10300",
                displayColorType = "单色",
                displayColors = listOf("#FF6A13FF"),
                trayUidHex = "AABBCCDDEEFF00112233445566778899",
                remainingPercent = 75.0f,
                totalWeightGrams = 1000
            ),
            voiceEnabled = false,
            ttsReady = true,
            ttsLanguageReady = true,
            onVoiceEnabledChange = {},
            onTrayOutbound = {},
            showRecoveryAction = true,
            onAttemptRecovery = {},
            onRemainingChange = { _, _, _ -> }
)
    }
}
