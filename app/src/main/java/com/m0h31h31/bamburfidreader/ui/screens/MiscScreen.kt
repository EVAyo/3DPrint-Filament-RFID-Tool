package com.m0h31h31.bamburfidreader.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.AppSwitch
import com.m0h31h31.bamburfidreader.ui.components.ModernCard
import com.m0h31h31.bamburfidreader.ui.components.ModernDivider
import com.m0h31h31.bamburfidreader.ui.components.ModernDot
import com.m0h31h31.bamburfidreader.ui.components.ModernPillButton
import com.m0h31h31.bamburfidreader.ui.components.ModernSectionHeader
import com.m0h31h31.bamburfidreader.ui.components.ModernSettingRow
import com.m0h31h31.bamburfidreader.ui.components.ModernWorkbenchTokens
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import com.m0h31h31.bamburfidreader.nfc.NfcCompatibilityMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import com.m0h31h31.bamburfidreader.ui.components.AppCircularProgressIndicator
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.ColorPalette
import com.m0h31h31.bamburfidreader.ui.theme.DEFAULT_APP_UI_STYLE
import com.m0h31h31.bamburfidreader.ui.theme.LocalAppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.ThemeMode
import com.m0h31h31.bamburfidreader.utils.AnalyticsReporter
import com.m0h31h31.bamburfidreader.utils.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.OutlinedTextField

internal enum class StatusTone {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

private const val MISC_PREFS = "misc_screen_prefs"
private const val KEY_NOTICE_EXPANDED = "notice_expanded"
private const val KEY_AD_EXPANDED = "ad_expanded"
private const val KEY_NICKNAME = "user_nickname"

internal fun resolveStatusTone(message: String): StatusTone {
    val text = message.lowercase()
    if (listOf("失败", "错误", "异常", "取消", "不可用", "未找到").any { it in text }) {
        return StatusTone.ERROR
    }
    if (listOf("成功", "完成", "已保存", "已打包", "已停止", "已导入", "此卡可用").any { it in text }) {
        return StatusTone.SUCCESS
    }
    if (listOf("提醒", "警告", "请", "等待", "准备", "覆盖", "正在").any { it in text }) {
        return StatusTone.WARNING
    }
    return when {
        listOf(
            "失败",
            "错误",
            "异常",
            "取消",
            "不可用",
            "failed",
            "error",
            "cancel",
            "unavailable"
        ).any { it in text } -> StatusTone.ERROR

        listOf(
            "成功",
            "完成",
            "已保存",
            "已打包",
            "已停止",
            "已导入",
            "此卡可用",
            "success",
            "completed",
            "saved",
            "exported",
            "stopped",
            "imported",
            "card is usable"
        ).any { it in text } -> StatusTone.SUCCESS

        listOf(
            "提醒",
            "警告",
            "请",
            "等待",
            "准备",
            "覆盖",
            "warning",
            "please",
            "wait",
            "ready",
            "overwrite"
        ).any { it in text } -> StatusTone.WARNING

        else -> StatusTone.INFO
    }
}

@Composable
private fun ModernMiscScreen(
    statusText: String,
    setMessage: (String) -> Unit,
    uiStyle: AppUiStyle,
    onUiStyleChange: (AppUiStyle) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    colorPalette: ColorPalette,
    onColorPaletteChange: (ColorPalette) -> Unit,
    readAllSectors: Boolean,
    onReadAllSectorsChange: (Boolean) -> Unit,
    saveKeysToFile: Boolean,
    onSaveKeysToFileChange: (Boolean) -> Unit,
    nfcCompatibilityMode: NfcCompatibilityMode,
    onNfcCompatibilityModeChange: (NfcCompatibilityMode) -> Unit,
    nfcCompatibilityTestInProgress: Boolean,
    onStartNfcCompatibilityReadTest: () -> String,
    onStartNfcCompatibilityWriteTest: () -> String,
    onCancelNfcCompatibilityTest: () -> String,
    bambuTagEnabled: Boolean,
    onBambuTagEnabledChange: (Boolean) -> Unit,
    crealityEnabled: Boolean,
    onCrealityEnabledChange: (Boolean) -> Unit,
    snapmakerTagEnabled: Boolean,
    onSnapmakerTagEnabledChange: (Boolean) -> Unit,
    cloudConnectEnabled: Boolean,
    onCloudConnectEnabledChange: (Boolean) -> Unit,
    costEnabled: Boolean,
    onCostEnabledChange: (Boolean) -> Unit,
    inventoryEnabled: Boolean,
    onInventoryEnabledChange: (Boolean) -> Unit,
    autoDetectBrand: Boolean,
    onAutoDetectBrandChange: (Boolean) -> Unit,
    autoShareTag: Boolean,
    onAutoShareTagChange: (Boolean) -> Unit,
    onCheckDownloadPermission: suspend () -> String?,
    onDownloadTagPackage: suspend (brand: String, onProgress: (Int) -> Unit, onImportStatus: (String) -> Unit) -> String,
    onLoadMySharedUids: suspend () -> List<String>,
    hideCopiedTags: Boolean,
    onHideCopiedTagsChange: (Boolean) -> Unit,
    dualTagMode: Boolean,
    onDualTagModeChange: (Boolean) -> Unit,
    tagViewMode: String,
    onTagViewModeChange: (String) -> Unit,
    formatInProgress: Boolean,
    onClearFuid: () -> String,
    onCancelClearFuid: () -> String,
    cuidTestInProgress: Boolean,
    onEnqueueCuidTest: () -> String,
    onCancelCuidTest: () -> String,
    selfTagCount: Int,
    onExportTagPackage: () -> String,
    onSelectImportTagPackage: () -> String,
    onSelectImportSnapmakerTagPackage: () -> String,
    onClearSelfTags: () -> String,
    onClearShareTags: () -> String,
    forceOverwriteImport: Boolean,
    onForceOverwriteImportChange: (Boolean) -> Unit,
    onBackupDatabase: () -> String,
    onImportDatabase: () -> String,
    boostLink: ConfigManager.AppLinkConfig,
    logoLinks: Map<String, ConfigManager.AppLinkConfig>,
    appConfigMessage: String,
    appConfigAdMessage: String,
    pendingUpdateInfo: com.m0h31h31.bamburfidreader.utils.UpdateInfo?,
    isDownloadingUpdate: Boolean,
    onStartUpdate: (com.m0h31h31.bamburfidreader.utils.UpdateInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val miscPrefs = remember(context) {
        context.getSharedPreferences(MISC_PREFS, android.content.Context.MODE_PRIVATE)
    }
    val installId = remember(context) { AnalyticsReporter.getInstallId(context) }
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }
    val normalizedNoticeMessage = remember(appConfigMessage) { normalizeConfigMessage(appConfigMessage) }
    val normalizedAdMessage = remember(appConfigAdMessage) { normalizeConfigMessage(appConfigAdMessage) }
    val logoOrder = remember { listOf("makerworld", "xianyu", "douyin", "qq", "gitee", "github") }
    val logoShape = remember { RoundedCornerShape(14.dp) }

    var nickname by remember { mutableStateOf(miscPrefs.getString(KEY_NICKNAME, "").orEmpty()) }
    var noticeExpanded by remember { mutableStateOf(miscPrefs.getBoolean(KEY_NOTICE_EXPANDED, true)) }
    var adExpanded by remember { mutableStateOf(miscPrefs.getBoolean(KEY_AD_EXPANDED, true)) }

    var showPaletteDialog by remember { mutableStateOf(false) }
    var showReadAllSectorsDialog by remember { mutableStateOf(false) }
    var showImportDatabaseConfirmDialog by remember { mutableStateOf(false) }
    var showClearSelfTagsConfirmDialog by remember { mutableStateOf(false) }
    var showClearShareTagsConfirmDialog by remember { mutableStateOf(false) }
    var showImportTypeDialog by remember { mutableStateOf(false) }
    var showAutoShareDisableConfirm by remember { mutableStateOf(false) }
    var showCuidDisclaimerDialog by remember { mutableStateOf(false) }

    var dlChecking by remember { mutableStateOf(false) }
    var dlDeniedMsg by remember { mutableStateOf("") }
    var dlShowBrandMenu by remember { mutableStateOf(false) }
    var dlInProgress by remember { mutableStateOf(false) }
    var dlProgress by remember { mutableStateOf(0) }
    var dlBrand by remember { mutableStateOf("") }
    var dlImportStatus by remember { mutableStateOf("") }
    var mySharesDialogVisible by remember { mutableStateOf(false) }
    var mySharesUids by remember { mutableStateOf<List<String>>(emptyList()) }

    val footerLogos by produceState(initialValue = emptyList<Pair<String, androidx.compose.ui.graphics.ImageBitmap>>(), context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.list("logos").orEmpty()
                    .sortedWith(
                        compareBy<String> {
                            val baseName = it.substringBeforeLast('.').lowercase()
                            logoOrder.indexOf(baseName).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE }
                        }.thenBy { it.lowercase() }
                    )
                    .mapNotNull { fileName ->
                        context.assets.open("logos/$fileName").use { input ->
                            BitmapFactory.decodeStream(input)?.asImageBitmap()?.let { bmp -> fileName to bmp }
                        }
                    }
            }.getOrDefault(emptyList())
        }
    }

    fun handleReadAllSectorsChange(checked: Boolean) {
        if (checked) showReadAllSectorsDialog = true else onReadAllSectorsChange(false)
    }

    val paletteOptions = remember { modernPaletteOptions() }
    val paletteColor = paletteOptions.firstOrNull { it.first == colorPalette }
        ?.let { Color(it.second.second) } ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ModernWorkbenchTokens.Page
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status card (pinned — always visible at top)
            ModernCard(modifier = Modifier.fillMaxWidth(), radius = 14.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernDot(color = MaterialTheme.colorScheme.primary, size = 12.dp)
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusText,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

          // Scrollable content below the pinned status card
          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f)
                  .verticalScroll(rememberScrollState())
                  .padding(bottom = 14.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Appearance
            ModernConfigSection(
                title = stringResource(R.string.misc_modern_section_appearance),
                icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Palette
            ) {
                ModernSettingRow(
                    title = stringResource(R.string.misc_ui_style),
                    subtitle = stringResource(R.string.misc_modern_style_hint)
                ) {
                    ModernSegmented(
                        options = listOf(
                            ModernSeg(AppUiStyle.MODERN_WORKBENCH, stringResource(R.string.misc_ui_style_modern_workbench)),
                            ModernSeg(AppUiStyle.NEUMORPHIC, stringResource(R.string.misc_ui_style_neumorphism)),
                            ModernSeg(AppUiStyle.MIUIX, stringResource(R.string.misc_ui_style_miuix))
                        ),
                        selectedKey = uiStyle,
                        onSelect = { onUiStyleChange(it as AppUiStyle) }
                    )
                }
                ModernDivider()
                ModernSettingRow(title = stringResource(R.string.misc_theme_mode)) {
                    ModernSegmented(
                        options = listOf(
                            ModernSeg(ThemeMode.SYSTEM, stringResource(R.string.misc_theme_mode_system)),
                            ModernSeg(ThemeMode.LIGHT, stringResource(R.string.misc_theme_mode_light)),
                            ModernSeg(ThemeMode.DARK, stringResource(R.string.misc_theme_mode_dark))
                        ),
                        selectedKey = themeMode,
                        onSelect = { onThemeModeChange(it as ThemeMode) }
                    )
                }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.misc_color_palette),
                    subtitle = stringResource(
                        paletteOptions.firstOrNull { it.first == colorPalette }?.second?.first
                            ?: R.string.palette_ocean
                    ),
                    modifier = Modifier.clickable { showPaletteDialog = true }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(paletteColor, RoundedCornerShape(999.dp))
                        )
                        Icon(
                            imageVector = com.m0h31h31.bamburfidreader.ui.components.AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = ModernWorkbenchTokens.Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // NFC
            ModernConfigSection(
                title = stringResource(R.string.misc_modern_section_nfc),
                icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Nfc
            ) {
                ModernSettingRow(
                    title = stringResource(R.string.misc_read_all_sectors),
                    subtitle = stringResource(R.string.misc_read_all_sectors_desc)
                ) {
                    AppSwitch(checked = readAllSectors, onCheckedChange = ::handleReadAllSectorsChange)
                }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.misc_save_keys),
                    subtitle = stringResource(R.string.misc_save_keys_desc)
                ) {
                    AppSwitch(checked = saveKeysToFile, onCheckedChange = onSaveKeysToFileChange)
                }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.nfc_compat_mode_title),
                    subtitle = stringResource(R.string.nfc_compat_mode_short_desc)
                ) {
                    ModernSegmented(
                        options = listOf(
                            ModernSeg(NfcCompatibilityMode.FAST, stringResource(R.string.nfc_compat_mode_fast)),
                            ModernSeg(NfcCompatibilityMode.BALANCED, stringResource(R.string.nfc_compat_mode_balanced)),
                            ModernSeg(NfcCompatibilityMode.STABLE, stringResource(R.string.nfc_compat_mode_stable))
                        ),
                        selectedKey = nfcCompatibilityMode,
                        onSelect = { onNfcCompatibilityModeChange(it as NfcCompatibilityMode) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Shield,
                        title = if (nfcCompatibilityTestInProgress) stringResource(R.string.nfc_compat_cancel_test)
                        else stringResource(R.string.nfc_compat_read_test),
                        subtitle = stringResource(R.string.nfc_compat_read_test_desc),
                        onClick = {
                            setMessage(
                                if (nfcCompatibilityTestInProgress) onCancelNfcCompatibilityTest()
                                else onStartNfcCompatibilityReadTest()
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Edit,
                        title = stringResource(R.string.nfc_compat_write_test),
                        subtitle = stringResource(R.string.nfc_compat_write_test_desc),
                        onClick = { setMessage(onStartNfcCompatibilityWriteTest()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Features
            ModernConfigSection(
                title = stringResource(R.string.misc_modern_section_features),
                icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.GridView
            ) {
                ModernSettingRow(
                    title = stringResource(R.string.config_bambu_feature),
                    subtitle = stringResource(R.string.config_bambu_feature_desc)
                ) { AppSwitch(checked = bambuTagEnabled, onCheckedChange = onBambuTagEnabledChange) }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_creality_feature),
                    subtitle = stringResource(R.string.config_creality_feature_desc)
                ) { AppSwitch(checked = crealityEnabled, onCheckedChange = onCrealityEnabledChange) }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_snapmaker_feature),
                    subtitle = stringResource(R.string.config_snapmaker_feature_desc)
                ) { AppSwitch(checked = snapmakerTagEnabled, onCheckedChange = onSnapmakerTagEnabledChange) }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_cloud_connect_feature),
                    subtitle = stringResource(R.string.config_cloud_connect_feature_desc)
                ) { AppSwitch(checked = cloudConnectEnabled, onCheckedChange = onCloudConnectEnabledChange) }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_cost_feature),
                    subtitle = stringResource(R.string.config_cost_feature_desc)
                ) { AppSwitch(checked = costEnabled, onCheckedChange = onCostEnabledChange) }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_inventory_feature),
                    subtitle = stringResource(R.string.config_inventory_feature_desc)
                ) { AppSwitch(checked = inventoryEnabled, onCheckedChange = onInventoryEnabledChange) }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_auto_detect_brand),
                    subtitle = stringResource(R.string.config_auto_detect_brand_desc)
                ) { AppSwitch(checked = autoDetectBrand, onCheckedChange = onAutoDetectBrandChange) }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_auto_share_tag),
                    subtitle = stringResource(R.string.config_auto_share_tag_desc)
                ) {
                    AppSwitch(
                        checked = autoShareTag,
                        onCheckedChange = {
                            if (it) onAutoShareTagChange(true) else showAutoShareDisableConfirm = true
                        }
                    )
                }
                AnimatedVisibility(
                    visible = autoShareTag,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ModernPillButton(
                                text = if (dlChecking) stringResource(R.string.dl_state_checking)
                                else stringResource(R.string.download_shared_tag_library),
                                onClick = {
                                    if (!dlChecking && !dlInProgress) {
                                        dlChecking = true
                                        coroutineScope.launch {
                                            val denied = withContext(Dispatchers.IO) { onCheckDownloadPermission() }
                                            dlChecking = false
                                            if (denied != null) dlDeniedMsg = denied else dlShowBrandMenu = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = dlShowBrandMenu,
                                onDismissRequest = { dlShowBrandMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.download_bambu_tag_package)) },
                                    onClick = {
                                        dlShowBrandMenu = false
                                        dlBrand = "bambu"; dlProgress = 0; dlImportStatus = ""; dlInProgress = true
                                        coroutineScope.launch {
                                            val msg = onDownloadTagPackage("bambu", { p -> dlProgress = p }, { s -> dlImportStatus = s })
                                            dlInProgress = false; dlProgress = 0; dlImportStatus = ""
                                            if (msg.isNotBlank()) setMessage(msg)
                                        }
                                    }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(stringResource(R.string.download_snapmaker_tag_package)) },
                                    onClick = {
                                        dlShowBrandMenu = false
                                        dlBrand = "snapmaker"; dlProgress = 0; dlImportStatus = ""; dlInProgress = true
                                        coroutineScope.launch {
                                            val msg = onDownloadTagPackage("snapmaker", { p -> dlProgress = p }, { s -> dlImportStatus = s })
                                            dlInProgress = false; dlProgress = 0; dlImportStatus = ""
                                            if (msg.isNotBlank()) setMessage(msg)
                                        }
                                    }
                                )
                            }
                        }
                        ModernPillButton(
                            text = stringResource(R.string.misc_my_shares),
                            onClick = {
                                coroutineScope.launch {
                                    mySharesUids = withContext(Dispatchers.IO) { onLoadMySharedUids() }
                                    mySharesDialogVisible = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ModernDivider()
                ModernSettingRow(
                    title = stringResource(R.string.config_hide_copied_tags),
                    subtitle = stringResource(R.string.config_hide_copied_tags_desc)
                ) { AppSwitch(checked = hideCopiedTags, onCheckedChange = onHideCopiedTagsChange) }
                AnimatedVisibility(visible = hideCopiedTags) {
                    ModernSettingRow(
                        modifier = Modifier.padding(start = 12.dp),
                        title = stringResource(R.string.config_dual_tag_mode),
                        subtitle = stringResource(R.string.config_dual_tag_mode_desc)
                    ) { AppSwitch(checked = dualTagMode, onCheckedChange = onDualTagModeChange) }
                }
                ModernDivider()
                ModernSettingRow(title = stringResource(R.string.config_tag_view_mode)) {
                    ModernSegmented(
                        options = listOf(
                            ModernSeg("list", stringResource(R.string.config_tag_view_list)),
                            ModernSeg("category", stringResource(R.string.config_tag_view_category))
                        ),
                        selectedKey = tagViewMode,
                        onSelect = { onTagViewModeChange(it as String) }
                    )
                }
            }

            // Tags & data tools
            ModernConfigSection(
                title = stringResource(R.string.misc_modern_section_data),
                icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Storage
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Label,
                        title = if (formatInProgress) stringResource(R.string.misc_cancel_format)
                        else stringResource(R.string.misc_format_tag),
                        subtitle = stringResource(R.string.misc_format_tag_desc),
                        onClick = { setMessage(if (formatInProgress) onCancelClearFuid() else onClearFuid()) },
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.ContentCopy,
                        title = if (cuidTestInProgress) stringResource(R.string.misc_cuid_test_cancel)
                        else stringResource(R.string.misc_cuid_test),
                        subtitle = stringResource(R.string.misc_cuid_test_desc),
                        onClick = {
                            if (cuidTestInProgress) setMessage(onCancelCuidTest())
                            else showCuidDisclaimerDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.FileUpload,
                        title = stringResource(R.string.misc_export_tag_package_with_count, selfTagCount),
                        subtitle = stringResource(R.string.misc_export_tag_package_desc),
                        onClick = { setMessage(onExportTagPackage()) },
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.FileDownload,
                        title = stringResource(R.string.misc_import_tag_package),
                        subtitle = stringResource(R.string.misc_import_tag_package_desc),
                        onClick = { showImportTypeDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Save,
                        title = stringResource(R.string.action_backup_db),
                        subtitle = stringResource(R.string.action_backup_db_desc),
                        onClick = { setMessage(onBackupDatabase()) },
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Storage,
                        title = stringResource(R.string.action_import_db),
                        subtitle = stringResource(R.string.action_import_db_desc),
                        onClick = { showImportDatabaseConfirmDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.DeleteOutline,
                        title = stringResource(R.string.misc_clear_self_tags),
                        subtitle = stringResource(R.string.misc_clear_self_tags_desc),
                        danger = true,
                        onClick = { showClearSelfTagsConfirmDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionCard(
                        icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.DeleteOutline,
                        title = stringResource(R.string.misc_clear_share_tags),
                        subtitle = stringResource(R.string.misc_clear_share_tags_desc),
                        danger = true,
                        onClick = { showClearShareTagsConfirmDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                ModernDivider()
                ModernSettingRow(title = stringResource(R.string.misc_force_overwrite_import)) {
                    AppSwitch(checked = forceOverwriteImport, onCheckedChange = onForceOverwriteImportChange)
                }
                if (boostLink.isUsable) {
                    ModernPillButton(
                        text = stringResource(R.string.action_boost_open_bambu),
                        filled = true,
                        onClick = { uriHandler.openUri(boostLink.value) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            // Account / device
            ModernConfigSection(
                title = stringResource(R.string.misc_modern_section_account),
                icon = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Person
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.misc_device_id_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = ModernWorkbenchTokens.Muted,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = installId,
                            style = MaterialTheme.typography.bodySmall,
                            color = ModernWorkbenchTokens.Ink
                        )
                    }
                }
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { if (it.length <= 16) nickname = it },
                    label = { Text(stringResource(R.string.misc_nickname_label)) },
                    placeholder = { Text(stringResource(R.string.misc_nickname_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                ModernPillButton(
                    text = stringResource(R.string.misc_nickname_save),
                    filled = true,
                    onClick = {
                        val trimmed = nickname.trim()
                        if (trimmed.isEmpty() || trimmed.length > 16) {
                            setMessage(context.getString(R.string.misc_nickname_invalid))
                            return@ModernPillButton
                        }
                        miscPrefs.edit().putString(KEY_NICKNAME, trimmed).apply()
                        coroutineScope.launch {
                            val ok = AnalyticsReporter.saveNickname(context, trimmed)
                            setMessage(
                                if (ok) context.getString(R.string.misc_nickname_saved)
                                else context.getString(R.string.misc_nickname_save_failed)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // Footer logos
            if (footerLogos.isNotEmpty()) {
                ModernCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        footerLogos.forEach { (fileName, logoBitmap) ->
                            val logoKey = fileName.substringBeforeLast('.').lowercase()
                            val linkConfig = logoLinks[logoKey]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(logoShape)
                                    .let { base ->
                                        if (linkConfig?.isUsable == true) {
                                            base.clickable { uriHandler.openUri(linkConfig.value) }
                                        } else base
                                    }
                            ) {
                                Image(
                                    bitmap = logoBitmap,
                                    contentDescription = fileName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }

            // Notice
            if (normalizedNoticeMessage.isNotBlank()) {
                ModernCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    noticeExpanded = !noticeExpanded
                                    miscPrefs.edit().putBoolean(KEY_NOTICE_EXPANDED, noticeExpanded).apply()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.misc_notice_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (noticeExpanded) "▲" else "▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = ModernWorkbenchTokens.Muted
                            )
                        }
                        AnimatedVisibility(
                            visible = noticeExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = normalizedNoticeMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ModernWorkbenchTokens.Ink,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Ad
            if (normalizedAdMessage.isNotBlank()) {
                ModernCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    adExpanded = !adExpanded
                                    miscPrefs.edit().putBoolean(KEY_AD_EXPANDED, adExpanded).apply()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.misc_ad_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (adExpanded) "▲" else "▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = ModernWorkbenchTokens.Muted
                            )
                        }
                        AnimatedVisibility(
                            visible = adExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = normalizedAdMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ModernWorkbenchTokens.Ink,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Version
            if (appVersion.isNotBlank()) {
                ModernCard(modifier = Modifier.fillMaxWidth(), radius = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.misc_version_format, appVersion),
                            style = MaterialTheme.typography.bodySmall,
                            color = ModernWorkbenchTokens.Muted
                        )
                        when {
                            isDownloadingUpdate -> Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            ) {
                                Text(
                                    text = stringResource(R.string.update_downloading),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                            }
                            pendingUpdateInfo != null -> Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ModernWorkbenchTokens.Danger,
                                modifier = Modifier.clickable { onStartUpdate(pendingUpdateInfo) }
                            ) {
                                Text(
                                    text = stringResource(R.string.update_badge),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
          } // end scrollable content Column
        }
    }

    // Dialogs
    if (dlDeniedMsg.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { dlDeniedMsg = "" },
            title = { Text(stringResource(R.string.download_not_allowed_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(dlDeniedMsg)
                    Text(
                        text = stringResource(R.string.download_not_allowed_boost_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { dlDeniedMsg = "" }) { Text(stringResource(R.string.action_ok)) }
            }
        )
    }

    if (dlInProgress) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(stringResource(
                    R.string.downloading_tag_package_brand,
                    if (dlBrand == "bambu") stringResource(R.string.brand_bambu) else stringResource(R.string.brand_snapmaker)
                ))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        dlImportStatus.isNotBlank() -> {
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { dlProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(dlImportStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        dlProgress in 1..99 -> {
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { dlProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(stringResource(R.string.format_percent, dlProgress), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> {
                            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                if (dlProgress == 0) stringResource(R.string.dl_state_connecting) else stringResource(R.string.dl_state_importing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (mySharesDialogVisible) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { mySharesDialogVisible = false }) {
            Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = stringResource(R.string.misc_my_shares_dialog_title), style = MaterialTheme.typography.titleMedium)
                    if (mySharesUids.isEmpty()) {
                        Text(
                            text = stringResource(R.string.misc_my_shares_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("#", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("UID", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.misc_my_shares_col_time), modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
                        ) {
                            itemsIndexed(mySharesUids) { index, entry ->
                                val parts = entry.split("\n", limit = 2)
                                val uid = parts[0]
                                val time = if (parts.size > 1) parts[1] else ""
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(uid, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                                    Text(time, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                    TextButton(onClick = { mySharesDialogVisible = false }, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }

    if (showPaletteDialog) {
        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            title = { Text(stringResource(R.string.misc_color_palette_dialog_title)) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(paletteOptions) { (palette, meta) ->
                        val (nameRes, colorInt) = meta
                        val selected = colorPalette == palette
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                onColorPaletteChange(palette)
                                showPaletteDialog = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(Color(colorInt), RoundedCornerShape(12.dp))
                                    .then(
                                        if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(nameRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaletteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showReadAllSectorsDialog) {
        AlertDialog(
            onDismissRequest = { showReadAllSectorsDialog = false },
            title = { Text(stringResource(R.string.misc_read_all_title)) },
            text = { Text(stringResource(R.string.misc_read_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showReadAllSectorsDialog = false
                    onReadAllSectorsChange(true)
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showReadAllSectorsDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showImportDatabaseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportDatabaseConfirmDialog = false },
            title = { Text(stringResource(R.string.misc_import_db_title)) },
            text = { Text(stringResource(R.string.misc_import_db_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportDatabaseConfirmDialog = false
                    setMessage(onImportDatabase())
                }) { Text(stringResource(R.string.misc_confirm_import)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDatabaseConfirmDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showClearSelfTagsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearSelfTagsConfirmDialog = false },
            title = { Text(stringResource(R.string.misc_clear_self_tags_title)) },
            text = { Text(stringResource(R.string.misc_clear_self_tags_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearSelfTagsConfirmDialog = false
                    setMessage(onClearSelfTags())
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearSelfTagsConfirmDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showClearShareTagsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearShareTagsConfirmDialog = false },
            title = { Text(stringResource(R.string.misc_clear_share_tags_title)) },
            text = { Text(stringResource(R.string.misc_clear_share_tags_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearShareTagsConfirmDialog = false
                    setMessage(onClearShareTags())
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearShareTagsConfirmDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showCuidDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showCuidDisclaimerDialog = false },
            title = { Text(stringResource(R.string.misc_cuid_test)) },
            text = { Text(stringResource(R.string.misc_cuid_test_disclaimer)) },
            confirmButton = {
                TextButton(onClick = {
                    showCuidDisclaimerDialog = false
                    setMessage(onEnqueueCuidTest())
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showCuidDisclaimerDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showAutoShareDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showAutoShareDisableConfirm = false },
            title = { Text(stringResource(R.string.auto_share_disable_confirm_title)) },
            text = { Text(stringResource(R.string.auto_share_disable_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showAutoShareDisableConfirm = false
                    onAutoShareTagChange(false)
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAutoShareDisableConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showImportTypeDialog) {
        val importingTagMsg = stringResource(R.string.misc_importing_tag_package)
        AlertDialog(
            onDismissRequest = { showImportTypeDialog = false },
            title = { Text(stringResource(R.string.misc_import_tag_type_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModernPillButton(
                        text = stringResource(R.string.misc_import_bambu_tag_package),
                        onClick = {
                            showImportTypeDialog = false
                            setMessage(importingTagMsg)
                            val result = onSelectImportTagPackage()
                            if (result.isNotBlank()) setMessage(result)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ModernPillButton(
                        text = stringResource(R.string.misc_import_snapmaker_tag_package),
                        onClick = {
                            showImportTypeDialog = false
                            setMessage(importingTagMsg)
                            val result = onSelectImportSnapmakerTagPackage()
                            if (result.isNotBlank()) setMessage(result)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportTypeDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

private fun modernPaletteOptions(): List<Pair<ColorPalette, Pair<Int, Int>>> = listOf(
    ColorPalette.ORANGE to Pair(R.string.palette_orange, 0xFFF99963.toInt()),
    ColorPalette.SKY_BLUE to Pair(R.string.palette_sky_blue, 0xFF56B7E6.toInt()),
    ColorPalette.OCEAN to Pair(R.string.palette_ocean, 0xFF0078BF.toInt()),
    ColorPalette.ICE_BLUE to Pair(R.string.palette_ice_blue, 0xFFA3D8E1.toInt()),
    ColorPalette.NIGHT_BLUE to Pair(R.string.palette_night_blue, 0xFF042F56.toInt()),
    ColorPalette.GUN_GRAY to Pair(R.string.palette_gun_gray, 0xFF757575.toInt()),
    ColorPalette.ROCK_GRAY to Pair(R.string.palette_rock_gray, 0xFF9B9EA0.toInt()),
    ColorPalette.FRUIT_GREEN to Pair(R.string.palette_fruit_green, 0xFFC2E189.toInt()),
    ColorPalette.GRASS_GREEN to Pair(R.string.palette_grass_green, 0xFF61C680.toInt()),
    ColorPalette.NIGHT_GREEN to Pair(R.string.palette_night_green, 0xFF68724D.toInt()),
    ColorPalette.CHARCOAL to Pair(R.string.palette_charcoal, 0xFF000000.toInt()),
    ColorPalette.DARK_BROWN to Pair(R.string.palette_dark_brown, 0xFF4D3324.toInt()),
    ColorPalette.LATTE to Pair(R.string.palette_latte, 0xFFD3B7A7.toInt()),
    ColorPalette.NIGHT_BROWN to Pair(R.string.palette_night_brown, 0xFF7D6556.toInt()),
    ColorPalette.SAND_BROWN to Pair(R.string.palette_sand_brown, 0xFFAE835B.toInt()),
    ColorPalette.SAKURA to Pair(R.string.palette_sakura, 0xFFE8AFCF.toInt()),
    ColorPalette.LILAC to Pair(R.string.palette_lilac, 0xFFAE96D4.toInt()),
    ColorPalette.CRIMSON to Pair(R.string.palette_crimson, 0xFFDE4343.toInt()),
    ColorPalette.BRICK_RED to Pair(R.string.palette_brick_red, 0xFFB15533.toInt()),
    ColorPalette.BERRY to Pair(R.string.palette_berry, 0xFF950051.toInt()),
    ColorPalette.NIGHT_RED to Pair(R.string.palette_night_red, 0xFFBB3D43.toInt()),
    ColorPalette.IVORY to Pair(R.string.palette_ivory, 0xFFFFFFFF.toInt()),
    ColorPalette.BONE to Pair(R.string.palette_bone, 0xFFCBC6B8.toInt()),
    ColorPalette.LEMON to Pair(R.string.palette_lemon, 0xFFF7D959.toInt()),
    ColorPalette.DESERT to Pair(R.string.palette_desert, 0xFFE8DBB7.toInt())
)

@Composable
private fun ModernConfigSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable () -> Unit
) {
    ModernCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    color = ModernWorkbenchTokens.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

private data class ModernSeg(val key: Any?, val label: String)

@Composable
private fun ModernSegmented(
    options: List<ModernSeg>,
    selectedKey: Any?,
    onSelect: (Any?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val selected = option.key == selectedKey
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clickable { onSelect(option.key) }
                ) {
                    Text(
                        text = option.label,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        color = if (selected) Color.White else ModernWorkbenchTokens.Ink,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    val accent = if (danger) ModernWorkbenchTokens.Danger else MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = ModernWorkbenchTokens.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, ModernWorkbenchTokens.Line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = if (danger) ModernWorkbenchTokens.Danger else ModernWorkbenchTokens.Ink,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = ModernWorkbenchTokens.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun normalizeConfigMessage(message: String): String {
    return message
        .replace("\r\n", "\n")
        .trim()
}

@Composable
private fun statusToneColor(tone: StatusTone): Color {
    val uiStyle = LocalAppUiStyle.current
    return when (tone) {
        StatusTone.SUCCESS -> if (uiStyle == AppUiStyle.MIUIX) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFF2E8B57)
        }

        StatusTone.ERROR -> MaterialTheme.colorScheme.error
        StatusTone.WARNING -> if (uiStyle == AppUiStyle.MIUIX) {
            MaterialTheme.colorScheme.tertiary
        } else {
            Color(0xFFB7791F)
        }

        StatusTone.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Preview
@Composable
fun MiscScreen(
    onBackupDatabase: () -> String = { "" },
    onImportDatabase: () -> String = { "" },
    onClearFuid: () -> String = { "" },
    onCancelClearFuid: () -> String = { "" },
    onClearSelfTags: () -> String = { "" },
    onClearShareTags: () -> String = { "" },
    onResetDatabase: () -> String = { "" },
    miscStatusMessage: String = "",
    onExportTagPackage: () -> String = { "" },
    onSelectImportTagPackage: () -> String = { "" },
    onSelectImportSnapmakerTagPackage: () -> String = { "" },
    snapmakerTagEnabled: Boolean = false,
    onSnapmakerTagEnabledChange: (Boolean) -> Unit = {},
    cloudConnectEnabled: Boolean = true,
    onCloudConnectEnabledChange: (Boolean) -> Unit = {},
    costEnabled: Boolean = false,
    onCostEnabledChange: (Boolean) -> Unit = {},
    selfTagCount: Int = 0,
    appConfigMessage: String = "",
    appConfigAdMessage: String = "",
    boostLink: ConfigManager.AppLinkConfig = ConfigManager.AppLinkConfig("", ""),
    logoLinks: Map<String, ConfigManager.AppLinkConfig> = emptyMap(),
    uiStyle: AppUiStyle = DEFAULT_APP_UI_STYLE,
    onUiStyleChange: (AppUiStyle) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    colorPalette: ColorPalette = ColorPalette.OCEAN,
    onColorPaletteChange: (ColorPalette) -> Unit = {},
    readAllSectors: Boolean = false,
    onReadAllSectorsChange: (Boolean) -> Unit = {},
    saveKeysToFile: Boolean = false,
    onSaveKeysToFileChange: (Boolean) -> Unit = {},
    nfcCompatibilityMode: NfcCompatibilityMode = NfcCompatibilityMode.BALANCED,
    onNfcCompatibilityModeChange: (NfcCompatibilityMode) -> Unit = {},
    nfcCompatibilityStatusMessage: String = "",
    nfcCompatibilityTestInProgress: Boolean = false,
    onStartNfcCompatibilityReadTest: () -> String = { "" },
    onStartNfcCompatibilityWriteTest: () -> String = { "" },
    onCancelNfcCompatibilityTest: () -> String = { "" },
    formatTagDebugEnabled: Boolean = false,
    onFormatTagDebugEnabledChange: (Boolean) -> Unit = {},
    forceOverwriteImport: Boolean = false,
    onForceOverwriteImportChange: (Boolean) -> Unit = {},
    formatInProgress: Boolean = false,
    cuidTestInProgress: Boolean = false,
    onEnqueueCuidTest: () -> String = { "" },
    onCancelCuidTest: () -> String = { "" },
    bambuTagEnabled: Boolean = true,
    onBambuTagEnabledChange: (Boolean) -> Unit = {},
    crealityEnabled: Boolean = false,
    onCrealityEnabledChange: (Boolean) -> Unit = {},
    inventoryEnabled: Boolean = false,
    onInventoryEnabledChange: (Boolean) -> Unit = {},
    autoDetectBrand: Boolean = false,
    onAutoDetectBrandChange: (Boolean) -> Unit = {},
    autoShareTag: Boolean = true,
    onAutoShareTagChange: (Boolean) -> Unit = {},
    onCheckDownloadPermission: suspend () -> String? = { null },
    onDownloadTagPackage: suspend (brand: String, onProgress: (Int) -> Unit, onImportStatus: (String) -> Unit) -> String = { _, _, _ -> "" },
    onLoadMySharedUids: suspend () -> List<String> = { emptyList() },
    hideCopiedTags: Boolean = true,
    onHideCopiedTagsChange: (Boolean) -> Unit = {},
    dualTagMode: Boolean = false,
    onDualTagModeChange: (Boolean) -> Unit = {},
    tagViewMode: String = "list",
    onTagViewModeChange: (String) -> Unit = {},
    scrollToNotice: Boolean = false,
    onScrollToNoticeDone: () -> Unit = {},
    pendingUpdateInfo: com.m0h31h31.bamburfidreader.utils.UpdateInfo? = null,
    isDownloadingUpdate: Boolean = false,
    onStartUpdate: (com.m0h31h31.bamburfidreader.utils.UpdateInfo) -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val miscPrefs = remember(context) {
        context.getSharedPreferences(MISC_PREFS, android.content.Context.MODE_PRIVATE)
    }
    val logoOrder = remember {
        listOf("makerworld", "xianyu", "douyin", "qq", "gitee", "github")
    }
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }
    val normalizedNoticeMessage = remember(appConfigMessage) {
        normalizeConfigMessage(appConfigMessage)
    }
    val normalizedAdMessage = remember(appConfigAdMessage) {
        normalizeConfigMessage(appConfigAdMessage)
    }
    var message by remember { mutableStateOf("") }
    var showCuidDisclaimerDialog by remember { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val effectiveMiscStatusMessage = miscStatusMessage.ifBlank { nfcCompatibilityStatusMessage }
    var visibleStatusMessage by remember { mutableStateOf("") }
    var lastMiscStatusMessage by remember { mutableStateOf(effectiveMiscStatusMessage) }
    var lastPageMessage by remember { mutableStateOf(message) }
    var dismissedStatusMessage by rememberSaveable { mutableStateOf("") }
    var noticeExpanded by remember {
        mutableStateOf(miscPrefs.getBoolean(KEY_NOTICE_EXPANDED, true))
    }
    var adExpanded by remember {
        mutableStateOf(miscPrefs.getBoolean(KEY_AD_EXPANDED, true))
    }

    LaunchedEffect(scrollToNotice) {
        if (scrollToNotice) {
            noticeExpanded = true
            onScrollToNoticeDone()
        }
    }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showReadAllSectorsDialog by remember { mutableStateOf(false) }
    var showImportDatabaseConfirmDialog by remember { mutableStateOf(false) }
    var showClearSelfTagsConfirmDialog by remember { mutableStateOf(false) }
    var showClearShareTagsConfirmDialog by remember { mutableStateOf(false) }
    var showImportTypeDialog by remember { mutableStateOf(false) }
    var showAutoShareDisableConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val installId = remember(context) { AnalyticsReporter.getInstallId(context) }
    var nickname by remember {
        mutableStateOf(miscPrefs.getString(KEY_NICKNAME, "").orEmpty())
    }
    var versionTapCount by rememberSaveable { mutableStateOf(0) }
    var versionEggVisible by remember { mutableStateOf(false) }
    var versionEggNonce by remember { mutableStateOf(0) }
    val versionEggPalette = if (uiStyle == AppUiStyle.MIUIX) {
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
    } else {
        listOf(
            Color(0xFFE8F2FF),
            Color(0xFFFFF4DD),
            Color(0xFFEAF9F0),
            Color(0xFFFFEBF1),
            Color(0xFFF2ECFF)
        )
    }
    val versionEggAccent = versionEggPalette[versionEggNonce % versionEggPalette.size]
    val versionEggMessageRes = when {
        versionTapCount >= 8 -> R.string.misc_easter_egg_5
        versionTapCount >= 6 -> R.string.misc_easter_egg_4
        versionTapCount >= 4 -> R.string.misc_easter_egg_3
        versionTapCount >= 2 -> R.string.misc_easter_egg_2
        else -> R.string.misc_easter_egg_1
    }

    LaunchedEffect(effectiveMiscStatusMessage, message) {
        val trimmedMiscStatus = effectiveMiscStatusMessage.trim()
        val trimmedPageMessage = message.trim()
        if (
            dismissedStatusMessage.isNotBlank() &&
            trimmedMiscStatus != dismissedStatusMessage &&
            trimmedPageMessage != dismissedStatusMessage
        ) {
            dismissedStatusMessage = ""
        }
        val nextMessage = when {
            trimmedPageMessage != lastPageMessage && trimmedPageMessage.isNotBlank() -> trimmedPageMessage
            trimmedMiscStatus != lastMiscStatusMessage && trimmedMiscStatus.isNotBlank() -> trimmedMiscStatus
            trimmedPageMessage.isNotBlank() -> trimmedPageMessage
            else -> trimmedMiscStatus
        }
        lastMiscStatusMessage = trimmedMiscStatus
        lastPageMessage = trimmedPageMessage
        if (nextMessage.isBlank()) {
            visibleStatusMessage = ""
            return@LaunchedEffect
        }
        if (nextMessage == dismissedStatusMessage) {
            visibleStatusMessage = ""
            return@LaunchedEffect
        }
        visibleStatusMessage = nextMessage
        delay(10000)
        if (visibleStatusMessage == nextMessage) {
            visibleStatusMessage = ""
            dismissedStatusMessage = nextMessage
        }
    }

    LaunchedEffect(versionEggVisible, versionEggNonce) {
        if (!versionEggVisible) return@LaunchedEffect
        delay(900)
        versionEggVisible = false
        versionTapCount = 0
    }

    fun handleReadAllSectorsChange(checked: Boolean) {
        if (checked) {
            showReadAllSectorsDialog = true
        } else {
            onReadAllSectorsChange(false)
        }
    }

    fun confirmReadAllSectors() {
        showReadAllSectorsDialog = false
        onReadAllSectorsChange(true)
    }

    fun confirmImportDatabase() {
        showImportDatabaseConfirmDialog = false
        message = onImportDatabase()
    }

    // 图片解码放到 IO 线程，避免首次进入页面时阻塞合成
    if (uiStyle == AppUiStyle.MODERN_WORKBENCH || uiStyle == AppUiStyle.MODERN_WORKBENCH_COMPOSE) {
        ModernMiscScreen(
            statusText = visibleStatusMessage.ifBlank { stringResource(R.string.misc_status_idle) },
            setMessage = { message = it },
            uiStyle = uiStyle,
            onUiStyleChange = onUiStyleChange,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            colorPalette = colorPalette,
            onColorPaletteChange = onColorPaletteChange,
            readAllSectors = readAllSectors,
            onReadAllSectorsChange = onReadAllSectorsChange,
            saveKeysToFile = saveKeysToFile,
            onSaveKeysToFileChange = onSaveKeysToFileChange,
            nfcCompatibilityMode = nfcCompatibilityMode,
            onNfcCompatibilityModeChange = onNfcCompatibilityModeChange,
            nfcCompatibilityTestInProgress = nfcCompatibilityTestInProgress,
            onStartNfcCompatibilityReadTest = onStartNfcCompatibilityReadTest,
            onStartNfcCompatibilityWriteTest = onStartNfcCompatibilityWriteTest,
            onCancelNfcCompatibilityTest = onCancelNfcCompatibilityTest,
            bambuTagEnabled = bambuTagEnabled,
            onBambuTagEnabledChange = onBambuTagEnabledChange,
            crealityEnabled = crealityEnabled,
            onCrealityEnabledChange = onCrealityEnabledChange,
            snapmakerTagEnabled = snapmakerTagEnabled,
            onSnapmakerTagEnabledChange = onSnapmakerTagEnabledChange,
            cloudConnectEnabled = cloudConnectEnabled,
            onCloudConnectEnabledChange = onCloudConnectEnabledChange,
            costEnabled = costEnabled,
            onCostEnabledChange = onCostEnabledChange,
            inventoryEnabled = inventoryEnabled,
            onInventoryEnabledChange = onInventoryEnabledChange,
            autoDetectBrand = autoDetectBrand,
            onAutoDetectBrandChange = onAutoDetectBrandChange,
            autoShareTag = autoShareTag,
            onAutoShareTagChange = onAutoShareTagChange,
            onCheckDownloadPermission = onCheckDownloadPermission,
            onDownloadTagPackage = onDownloadTagPackage,
            onLoadMySharedUids = onLoadMySharedUids,
            hideCopiedTags = hideCopiedTags,
            onHideCopiedTagsChange = onHideCopiedTagsChange,
            dualTagMode = dualTagMode,
            onDualTagModeChange = onDualTagModeChange,
            tagViewMode = tagViewMode,
            onTagViewModeChange = onTagViewModeChange,
            formatInProgress = formatInProgress,
            onClearFuid = onClearFuid,
            onCancelClearFuid = onCancelClearFuid,
            cuidTestInProgress = cuidTestInProgress,
            onEnqueueCuidTest = onEnqueueCuidTest,
            onCancelCuidTest = onCancelCuidTest,
            selfTagCount = selfTagCount,
            onExportTagPackage = onExportTagPackage,
            onSelectImportTagPackage = onSelectImportTagPackage,
            onSelectImportSnapmakerTagPackage = onSelectImportSnapmakerTagPackage,
            onClearSelfTags = onClearSelfTags,
            onClearShareTags = onClearShareTags,
            forceOverwriteImport = forceOverwriteImport,
            onForceOverwriteImportChange = onForceOverwriteImportChange,
            onBackupDatabase = onBackupDatabase,
            onImportDatabase = onImportDatabase,
            boostLink = boostLink,
            logoLinks = logoLinks,
            appConfigMessage = appConfigMessage,
            appConfigAdMessage = appConfigAdMessage,
            pendingUpdateInfo = pendingUpdateInfo,
            isDownloadingUpdate = isDownloadingUpdate,
            onStartUpdate = onStartUpdate,
            modifier = modifier
        )
        return
    }

    val footerLogos by produceState(initialValue = emptyList<Pair<String, androidx.compose.ui.graphics.ImageBitmap>>(), context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.list("logos")
                    .orEmpty()
                    .sortedWith(
                        compareBy<String> {
                            val baseName = it.substringBeforeLast('.').lowercase()
                            logoOrder.indexOf(baseName).let { index ->
                                if (index >= 0) index else Int.MAX_VALUE
                            }
                        }.thenBy { it.lowercase() }
                    )
                    .mapNotNull { fileName ->
                        context.assets.open("logos/$fileName").use { input ->
                            BitmapFactory.decodeStream(input)?.asImageBitmap()?.let { bitmap ->
                                fileName to bitmap
                            }
                        }
                    }
            }.getOrDefault(emptyList())
        }
    }

    val logoShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(14.dp) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .neuBackground(),
        color = MaterialTheme.colorScheme.background
    ) {
        val statusText = visibleStatusMessage.ifBlank { stringResource(R.string.misc_status_idle) }
        val visibleStatusColor = statusToneColor(resolveStatusTone(visibleStatusMessage))
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Fixed status bar — always visible at top ────────────────────
            NeuPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                val statusIsWaiting = visibleStatusMessage.isNotBlank() && (
                        visibleStatusMessage.contains("正在") ||
                                visibleStatusMessage.contains("请稍候") ||
                                visibleStatusMessage.contains("准备就绪") ||
                                visibleStatusMessage.contains("请将目标")
                        )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (statusIsWaiting) {
                        AppCircularProgressIndicator(modifier = Modifier.size(16.dp))
                    }
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (visibleStatusMessage.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                visibleStatusColor
                            }
                        )
                    }
                }
            }
            // ── Scrollable content ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                NeuPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = stringResource(R.string.misc_ui_style))
                            val styleOptions = listOf(
                                AppUiStyle.MODERN_WORKBENCH to stringResource(R.string.misc_ui_style_modern_workbench),
                                AppUiStyle.NEUMORPHIC to stringResource(R.string.misc_ui_style_neumorphism),
                                AppUiStyle.MIUIX to stringResource(R.string.misc_ui_style_miuix)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                styleOptions.forEach { (style, label) ->
                                    val selected = uiStyle == style
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = if (selected) {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        },
                                        modifier = Modifier.clickable { onUiStyleChange(style) }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.misc_theme_mode),
                                modifier = Modifier.weight(1f)
                            )
                            val themeModes = listOf(
                                ThemeMode.LIGHT to stringResource(R.string.misc_theme_mode_light),
                                ThemeMode.DARK to stringResource(R.string.misc_theme_mode_dark),
                                ThemeMode.SYSTEM to stringResource(R.string.misc_theme_mode_system)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                themeModes.forEach { (mode, label) ->
                                    val selected = themeMode == mode
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = if (selected) {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        },
                                        modifier = Modifier.clickable { onThemeModeChange(mode) }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ── Color palette row ─────────────────────────────────────
                        val allPaletteOptions = listOf(
                            ColorPalette.ORANGE to Pair(
                                R.string.palette_orange,
                                0xFFF99963.toInt()
                            ),
                            ColorPalette.SKY_BLUE to Pair(
                                R.string.palette_sky_blue,
                                0xFF56B7E6.toInt()
                            ),
                            ColorPalette.OCEAN to Pair(R.string.palette_ocean, 0xFF0078BF.toInt()),
                            ColorPalette.ICE_BLUE to Pair(
                                R.string.palette_ice_blue,
                                0xFFA3D8E1.toInt()
                            ),
                            ColorPalette.NIGHT_BLUE to Pair(
                                R.string.palette_night_blue,
                                0xFF042F56.toInt()
                            ),
                            ColorPalette.GUN_GRAY to Pair(
                                R.string.palette_gun_gray,
                                0xFF757575.toInt()
                            ),
                            ColorPalette.ROCK_GRAY to Pair(
                                R.string.palette_rock_gray,
                                0xFF9B9EA0.toInt()
                            ),
                            ColorPalette.FRUIT_GREEN to Pair(
                                R.string.palette_fruit_green,
                                0xFFC2E189.toInt()
                            ),
                            ColorPalette.GRASS_GREEN to Pair(
                                R.string.palette_grass_green,
                                0xFF61C680.toInt()
                            ),
                            ColorPalette.NIGHT_GREEN to Pair(
                                R.string.palette_night_green,
                                0xFF68724D.toInt()
                            ),
                            ColorPalette.CHARCOAL to Pair(
                                R.string.palette_charcoal,
                                0xFF000000.toInt()
                            ),
                            ColorPalette.DARK_BROWN to Pair(
                                R.string.palette_dark_brown,
                                0xFF4D3324.toInt()
                            ),
                            ColorPalette.LATTE to Pair(R.string.palette_latte, 0xFFD3B7A7.toInt()),
                            ColorPalette.NIGHT_BROWN to Pair(
                                R.string.palette_night_brown,
                                0xFF7D6556.toInt()
                            ),
                            ColorPalette.SAND_BROWN to Pair(
                                R.string.palette_sand_brown,
                                0xFFAE835B.toInt()
                            ),
                            ColorPalette.SAKURA to Pair(
                                R.string.palette_sakura,
                                0xFFE8AFCF.toInt()
                            ),
                            ColorPalette.LILAC to Pair(R.string.palette_lilac, 0xFFAE96D4.toInt()),
                            ColorPalette.CRIMSON to Pair(
                                R.string.palette_crimson,
                                0xFFDE4343.toInt()
                            ),
                            ColorPalette.BRICK_RED to Pair(
                                R.string.palette_brick_red,
                                0xFFB15533.toInt()
                            ),
                            ColorPalette.BERRY to Pair(R.string.palette_berry, 0xFF950051.toInt()),
                            ColorPalette.NIGHT_RED to Pair(
                                R.string.palette_night_red,
                                0xFFBB3D43.toInt()
                            ),
                            ColorPalette.IVORY to Pair(R.string.palette_ivory, 0xFFFFFFFF.toInt()),
                            ColorPalette.BONE to Pair(R.string.palette_bone, 0xFFCBC6B8.toInt()),
                            ColorPalette.LEMON to Pair(R.string.palette_lemon, 0xFFF7D959.toInt()),
                            ColorPalette.DESERT to Pair(
                                R.string.palette_desert,
                                0xFFE8DBB7.toInt()
                            ),
                        )
                        val paletteColor = allPaletteOptions
                            .firstOrNull { it.first == colorPalette }
                            ?.let { androidx.compose.ui.graphics.Color(it.second.second) }
                            ?: androidx.compose.ui.graphics.Color(0xFF0078BF.toInt())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaletteDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.misc_color_palette))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(paletteColor, RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = stringResource(
                                        allPaletteOptions.firstOrNull { it.first == colorPalette }
                                            ?.second?.first ?: R.string.palette_ocean
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showPaletteDialog) {
                            AlertDialog(
                                onDismissRequest = { showPaletteDialog = false },
                                title = { Text(stringResource(R.string.misc_color_palette_dialog_title)) },
                                text = {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.heightIn(max = 400.dp)
                                    ) {
                                        items(allPaletteOptions) { (palette, meta) ->
                                            val (nameRes, colorInt) = meta
                                            val selected = colorPalette == palette
                                            val itemColor =
                                                androidx.compose.ui.graphics.Color(colorInt)
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.clickable {
                                                    onColorPaletteChange(palette)
                                                    showPaletteDialog = false
                                                }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .background(
                                                            itemColor,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .then(
                                                            if (selected) Modifier.border(
                                                                2.dp,
                                                                MaterialTheme.colorScheme.onSurface,
                                                                RoundedCornerShape(12.dp)
                                                            ) else Modifier
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (selected) {
                                                        Icon(
                                                            imageVector = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Check,
                                                            contentDescription = null,
                                                            tint = androidx.compose.ui.graphics.Color.White,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = stringResource(nameRes),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showPaletteDialog = false }) {
                                        Text(stringResource(R.string.action_cancel))
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.misc_read_all_sectors))
                            AppSwitch(
                                checked = readAllSectors,
                                onCheckedChange = ::handleReadAllSectorsChange
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.misc_save_keys))
                            AppSwitch(
                                checked = saveKeysToFile,
                                onCheckedChange = onSaveKeysToFileChange
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = stringResource(R.string.nfc_compat_mode_title))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    NfcCompatibilityMode.FAST to stringResource(R.string.nfc_compat_mode_fast),
                                    NfcCompatibilityMode.BALANCED to stringResource(R.string.nfc_compat_mode_balanced),
                                    NfcCompatibilityMode.STABLE to stringResource(R.string.nfc_compat_mode_stable)
                                ).forEach { (mode, label) ->
                                    val selected = nfcCompatibilityMode == mode
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onNfcCompatibilityModeChange(mode) }
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeuButton(
                                    text = if (nfcCompatibilityTestInProgress) {
                                        stringResource(R.string.nfc_compat_cancel_test)
                                    } else {
                                        stringResource(R.string.nfc_compat_read_test)
                                    },
                                    onClick = {
                                        message = if (nfcCompatibilityTestInProgress) {
                                            onCancelNfcCompatibilityTest()
                                        } else {
                                            onStartNfcCompatibilityReadTest()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                NeuButton(
                                    text = stringResource(R.string.nfc_compat_write_test),
                                    onClick = { message = onStartNfcCompatibilityWriteTest() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_bambu_feature))
                                Text(
                                    text = stringResource(R.string.config_bambu_feature_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = bambuTagEnabled,
                                onCheckedChange = { onBambuTagEnabledChange(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_creality_feature))
                                Text(
                                    text = stringResource(R.string.config_creality_feature_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = crealityEnabled,
                                onCheckedChange = { onCrealityEnabledChange(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_snapmaker_feature))
                                Text(
                                    text = stringResource(R.string.config_snapmaker_feature_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = snapmakerTagEnabled,
                                onCheckedChange = { onSnapmakerTagEnabledChange(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_cloud_connect_feature))
                                Text(
                                    text = stringResource(R.string.config_cloud_connect_feature_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = cloudConnectEnabled,
                                onCheckedChange = { onCloudConnectEnabledChange(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_cost_feature))
                                Text(
                                    text = stringResource(R.string.config_cost_feature_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = costEnabled,
                                onCheckedChange = { onCostEnabledChange(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_inventory_feature))
                                Text(
                                    text = stringResource(R.string.config_inventory_feature_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = inventoryEnabled,
                                onCheckedChange = { onInventoryEnabledChange(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_auto_detect_brand))
                                Text(
                                    text = stringResource(R.string.config_auto_detect_brand_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = autoDetectBrand,
                                onCheckedChange = { onAutoDetectBrandChange(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_auto_share_tag))
                                Text(
                                    text = stringResource(R.string.config_auto_share_tag_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = autoShareTag,
                                onCheckedChange = {
                                    if (it) {
                                        onAutoShareTagChange(true)
                                    } else {
                                        showAutoShareDisableConfirm = true
                                    }
                                }
                            )
                        }

                        // ── 下载共享标签库（仅自动共享开启时显示）────────────
                        // 状态在外层保持，避免 AnimatedVisibility 里 remember 被重置
                        var dlChecking      by remember { mutableStateOf(false) }
                        var dlDeniedMsg     by remember { mutableStateOf("") }
                        var dlShowBrandMenu by remember { mutableStateOf(false) }
                        var dlInProgress    by remember { mutableStateOf(false) }
                        var dlProgress      by remember { mutableStateOf(0) }
                        var dlBrand         by remember { mutableStateOf("") }
                        var dlImportStatus  by remember { mutableStateOf("") }
                        val dlScope = rememberCoroutineScope()
                        // ── 我的共享状态（同样提到外层）──────────────────────
                        var mySharesDialogVisible by remember { mutableStateOf(false) }
                        var mySharesUids by remember { mutableStateOf<List<String>>(emptyList()) }

                        // 权限拒绝提示弹窗
                        if (dlDeniedMsg.isNotBlank()) {
                            AlertDialog(
                                onDismissRequest = { dlDeniedMsg = "" },
                                title = { Text(stringResource(R.string.download_not_allowed_title)) },
                                text  = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(dlDeniedMsg)
                                        Text(
                                            text = stringResource(R.string.download_not_allowed_boost_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { dlDeniedMsg = "" }) {
                                        Text(stringResource(R.string.action_ok))
                                    }
                                }
                            )
                        }

                        // 下载进度弹窗
                        if (dlInProgress) {
                            AlertDialog(
                                onDismissRequest = { /* 下载中不允许关闭 */ },
                                title = {
                                    Text(stringResource(
                                        R.string.downloading_tag_package_brand,
                                        if (dlBrand == "bambu") stringResource(R.string.brand_bambu) else stringResource(R.string.brand_snapmaker)
                                    ))
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        when {
                                            dlImportStatus.isNotBlank() -> {
                                                // 导入阶段：确定性进度条 + "正在导入 X/Y"
                                                androidx.compose.material3.LinearProgressIndicator(
                                                    progress = { dlProgress / 100f },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    dlImportStatus,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            dlProgress in 1..99 -> {
                                                // 下载阶段（已知大小）：确定性进度条 + 百分比
                                                androidx.compose.material3.LinearProgressIndicator(
                                                    progress = { dlProgress / 100f },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    stringResource(R.string.format_percent, dlProgress),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            else -> {
                                                // 连接中 / 未知大小下载中 / 下载完等待导入
                                                androidx.compose.material3.LinearProgressIndicator(
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    if (dlProgress == 0) stringResource(R.string.dl_state_connecting)
                                                    else stringResource(R.string.dl_state_importing),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {}
                            )
                        }

                        AnimatedVisibility(
                            visible = autoShareTag,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                NeuButton(
                                    text = when {
                                        dlChecking  -> stringResource(R.string.dl_state_checking)
                                        else        -> stringResource(R.string.download_shared_tag_library)
                                    },
                                    onClick = {
                                        if (!dlChecking && !dlInProgress) {
                                            dlChecking = true
                                            dlScope.launch {
                                                val denied = kotlinx.coroutines.withContext(
                                                    kotlinx.coroutines.Dispatchers.IO
                                                ) { onCheckDownloadPermission() }
                                                dlChecking = false
                                                if (denied != null) {
                                                    dlDeniedMsg = denied
                                                } else {
                                                    dlShowBrandMenu = true
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = dlShowBrandMenu,
                                    onDismissRequest = { dlShowBrandMenu = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.download_bambu_tag_package)) },
                                        onClick = {
                                            dlShowBrandMenu = false
                                            dlBrand = "bambu"; dlProgress = 0; dlImportStatus = ""; dlInProgress = true
                                            dlScope.launch {
                                                val msg = onDownloadTagPackage("bambu",
                                                    { p -> dlProgress = p },
                                                    { s -> dlImportStatus = s }
                                                )
                                                dlInProgress = false
                                                dlProgress = 0
                                                dlImportStatus = ""
                                                if (msg.isNotBlank()) message = msg
                                            }
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.download_snapmaker_tag_package)) },
                                        onClick = {
                                            dlShowBrandMenu = false
                                            dlBrand = "snapmaker"; dlProgress = 0; dlImportStatus = ""; dlInProgress = true
                                            dlScope.launch {
                                                val msg = onDownloadTagPackage("snapmaker",
                                                    { p -> dlProgress = p },
                                                    { s -> dlImportStatus = s }
                                                )
                                                dlInProgress = false
                                                dlProgress = 0
                                                dlImportStatus = ""
                                                if (msg.isNotBlank()) message = msg
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = autoShareTag,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            NeuButton(
                                text = stringResource(R.string.misc_my_shares),
                                onClick = {
                                    dlScope.launch {
                                        mySharesUids = kotlinx.coroutines.withContext(
                                            kotlinx.coroutines.Dispatchers.IO
                                        ) { onLoadMySharedUids() }
                                        mySharesDialogVisible = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (mySharesDialogVisible) {
                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = { mySharesDialogVisible = false }
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    tonalElevation = 6.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.misc_my_shares_dialog_title),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (mySharesUids.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.misc_my_shares_empty),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            // 表头
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "#",
                                                    modifier = Modifier.weight(0.5f),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "UID",
                                                    modifier = Modifier.weight(1.5f),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = stringResource(R.string.misc_my_shares_col_time),
                                                    modifier = Modifier.weight(2f),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            HorizontalDivider()
                                            // 表格行
                                            androidx.compose.foundation.lazy.LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 360.dp)
                                            ) {
                                                itemsIndexed(mySharesUids) { index, entry ->
                                                    val parts = entry.split("\n", limit = 2)
                                                    val uid  = parts[0]
                                                    val time = if (parts.size > 1) parts[1] else ""
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 5.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${index + 1}",
                                                            modifier = Modifier.weight(0.5f),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = uid,
                                                            modifier = Modifier.weight(1.5f),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                        Text(
                                                            text = time,
                                                            modifier = Modifier.weight(2f),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                    )
                                                }
                                            }
                                        }
                                        TextButton(
                                            onClick = { mySharesDialogVisible = false },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(stringResource(android.R.string.ok))
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = stringResource(R.string.config_hide_copied_tags))
                                Text(
                                    text = stringResource(R.string.config_hide_copied_tags_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSwitch(
                                checked = hideCopiedTags,
                                onCheckedChange = onHideCopiedTagsChange
                            )
                        }

                        if (hideCopiedTags) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(text = stringResource(R.string.config_dual_tag_mode))
                                    Text(
                                        text = stringResource(R.string.config_dual_tag_mode_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AppSwitch(
                                    checked = dualTagMode,
                                    onCheckedChange = onDualTagModeChange
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.config_tag_view_mode))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                listOf(
                                    "list" to R.string.config_tag_view_list,
                                    "category" to R.string.config_tag_view_category
                                ).forEach { (mode, labelRes) ->
                                    val selected = tagViewMode == mode
                                    androidx.compose.material3.FilterChip(
                                        selected = selected,
                                        onClick = { onTagViewModeChange(mode) },
                                        label = {
                                            Text(
                                                stringResource(labelRes),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showReadAllSectorsDialog) {
                    AlertDialog(
                        onDismissRequest = { showReadAllSectorsDialog = false },
                        title = { Text(text = stringResource(R.string.misc_read_all_title)) },
                        text = {
                            Text(
                                text = stringResource(R.string.misc_read_all_message)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = ::confirmReadAllSectors) {
                                Text(text = stringResource(R.string.action_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReadAllSectorsDialog = false }) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (showImportDatabaseConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showImportDatabaseConfirmDialog = false },
                        title = { Text(text = stringResource(R.string.misc_import_db_title)) },
                        text = {
                            Text(text = stringResource(R.string.misc_import_db_message))
                        },
                        confirmButton = {
                            TextButton(onClick = ::confirmImportDatabase) {
                                Text(text = stringResource(R.string.misc_confirm_import))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportDatabaseConfirmDialog = false }) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (showClearSelfTagsConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearSelfTagsConfirmDialog = false },
                        title = { Text(text = stringResource(R.string.misc_clear_self_tags_title)) },
                        text = {
                            Text(text = stringResource(R.string.misc_clear_self_tags_message))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showClearSelfTagsConfirmDialog = false
                                    message = onClearSelfTags()
                                }
                            ) {
                                Text(text = stringResource(R.string.action_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearSelfTagsConfirmDialog = false }) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (showCuidDisclaimerDialog) {
                    AlertDialog(
                        onDismissRequest = { showCuidDisclaimerDialog = false },
                        title = { Text(text = stringResource(R.string.misc_cuid_test)) },
                        text = { Text(text = stringResource(R.string.misc_cuid_test_disclaimer)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showCuidDisclaimerDialog = false
                                    message = onEnqueueCuidTest()
                                }
                            ) {
                                Text(text = stringResource(R.string.action_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCuidDisclaimerDialog = false }) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (showClearShareTagsConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearShareTagsConfirmDialog = false },
                        title = { Text(text = stringResource(R.string.misc_clear_share_tags_title)) },
                        text = {
                            Text(text = stringResource(R.string.misc_clear_share_tags_message))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showClearShareTagsConfirmDialog = false
                                    message = onClearShareTags()
                                }
                            ) {
                                Text(text = stringResource(R.string.action_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearShareTagsConfirmDialog = false }) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (showAutoShareDisableConfirm) {
                    AlertDialog(
                        onDismissRequest = { showAutoShareDisableConfirm = false },
                        title = { Text(text = stringResource(R.string.auto_share_disable_confirm_title)) },
                        text = { Text(text = stringResource(R.string.auto_share_disable_confirm_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showAutoShareDisableConfirm = false
                                onAutoShareTagChange(false)
                            }) {
                                Text(text = stringResource(R.string.action_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAutoShareDisableConfirm = false }) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                if (showImportTypeDialog) {
                    val importingTagMsg = stringResource(R.string.misc_importing_tag_package)
                    AlertDialog(
                        onDismissRequest = { showImportTypeDialog = false },
                        title = { Text(text = stringResource(R.string.misc_import_tag_type_title)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                NeuButton(
                                    text = stringResource(R.string.misc_import_bambu_tag_package),
                                    onClick = {
                                        showImportTypeDialog = false
                                        message = importingTagMsg
                                        val result = onSelectImportTagPackage()
                                        if (result.isNotBlank()) message = result
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                NeuButton(
                                    text = stringResource(R.string.misc_import_snapmaker_tag_package),
                                    onClick = {
                                        showImportTypeDialog = false
                                        message = importingTagMsg
                                        val result = onSelectImportSnapmakerTagPackage()
                                        if (result.isNotBlank()) message = result
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showImportTypeDialog = false }) {
                                Text(text = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuButton(
                        text = if (formatInProgress) {
                            stringResource(R.string.misc_cancel_format)
                        } else {
                            stringResource(R.string.misc_format_tag)
                        },
                        onClick = {
                            message = if (formatInProgress) {
                                onCancelClearFuid()
                            } else {
                                onClearFuid()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NeuButton(
                        text = if (cuidTestInProgress) {
                            stringResource(R.string.misc_cuid_test_cancel)
                        } else {
                            stringResource(R.string.misc_cuid_test)
                        },
                        onClick = {
                            message = if (cuidTestInProgress) {
                                onCancelCuidTest()
                            } else {
                                showCuidDisclaimerDialog = true
                                ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuButton(
                        text = stringResource(
                            R.string.misc_export_tag_package_with_count,
                            selfTagCount
                        ),
                        onClick = { message = onExportTagPackage() },
                        modifier = Modifier.weight(1f)
                    )
                    NeuButton(
                        text = stringResource(R.string.misc_import_tag_package),
                        onClick = { showImportTypeDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuButton(
                        text = stringResource(R.string.misc_clear_self_tags),
                        onClick = { showClearSelfTagsConfirmDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    NeuButton(
                        text = stringResource(R.string.misc_clear_share_tags),
                        onClick = { showClearShareTagsConfirmDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                NeuPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.misc_force_overwrite_import))
                        AppSwitch(
                            checked = forceOverwriteImport,
                            onCheckedChange = onForceOverwriteImportChange
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuButton(
                        text = stringResource(R.string.action_backup_db),
                        onClick = { message = onBackupDatabase() },
                        modifier = Modifier.weight(1f)
                    )
                    NeuButton(
                        text = stringResource(R.string.action_import_db),
                        onClick = { showImportDatabaseConfirmDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (boostLink.isUsable) {
                    NeuButton(
                        text = stringResource(R.string.action_boost_open_bambu),
                        onClick = { uriHandler.openUri(boostLink.value) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                NeuPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.misc_device_id_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = installId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { if (it.length <= 16) nickname = it },
                            label = { Text(stringResource(R.string.misc_nickname_label)) },
                            placeholder = { Text(stringResource(R.string.misc_nickname_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NeuButton(
                            text = stringResource(R.string.misc_nickname_save),
                            onClick = {
                                val trimmed = nickname.trim()
                                if (trimmed.isEmpty() || trimmed.length > 16) {
                                    message = context.getString(R.string.misc_nickname_invalid)
                                    return@NeuButton
                                }
                                miscPrefs.edit().putString(KEY_NICKNAME, trimmed).apply()
                                coroutineScope.launch {
                                    val ok = AnalyticsReporter.saveNickname(context, trimmed)
                                    message = if (ok) {
                                        context.getString(R.string.misc_nickname_saved)
                                    } else {
                                        context.getString(R.string.misc_nickname_save_failed)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (footerLogos.isNotEmpty()) {
                    NeuPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            footerLogos.forEach { (fileName, logoBitmap) ->
                                val logoKey = fileName.substringBeforeLast('.').lowercase()
                                val linkConfig = logoLinks[logoKey]
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(logoShape)
                                        .let { base ->
                                            if (linkConfig?.isUsable == true) {
                                                base.clickable { uriHandler.openUri(linkConfig.value) }
                                            } else {
                                                base
                                            }
                                        }
                                ) {
                                    Image(
                                        bitmap = logoBitmap,
                                        contentDescription = fileName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                if (normalizedNoticeMessage.isNotBlank()) {
                    NeuPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val v = !noticeExpanded
                                        noticeExpanded = v
                                        miscPrefs.edit().putBoolean(KEY_NOTICE_EXPANDED, v).apply()
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.misc_notice_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (noticeExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(
                                visible = noticeExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = normalizedNoticeMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (normalizedAdMessage.isNotBlank()) {
                    NeuPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val v = !adExpanded
                                        adExpanded = v
                                        miscPrefs.edit().putBoolean(KEY_AD_EXPANDED, v).apply()
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.misc_ad_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (adExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(
                                visible = adExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = normalizedAdMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (appVersion.isNotBlank()) {
                    val versionTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val versionEggTextColor = MaterialTheme.colorScheme.onSurface
                    Box(modifier = Modifier.fillMaxWidth()) {
                        NeuPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    versionTapCount += 1
                                    versionEggNonce += 1
                                    versionEggVisible = false
                                    versionEggVisible = true
                                }
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.misc_version_format, appVersion),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = versionTextColor
                                )
                                when {
                                    isDownloadingUpdate -> Surface(
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.update_downloading),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                    pendingUpdateInfo != null -> Surface(
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .clickable { onStartUpdate(pendingUpdateInfo!!) }
                                            .padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.update_badge),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = versionEggVisible,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = (-12).dp),
                            enter = fadeIn() + scaleIn(
                                initialScale = 0.85f,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f)
                            ),
                            exit = fadeOut() + scaleOut(targetScale = 0.92f)
                        ) {
                            Surface(
                                shape = logoShape,
                                color = versionEggAccent,
                                tonalElevation = 0.dp,
                                shadowElevation = 6.dp,
                                border = BorderStroke(1.dp, versionEggAccent.copy(alpha = 0.95f))
                            ) {
                                Text(
                                    text = stringResource(versionEggMessageRes),
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 7.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = versionEggTextColor
                                )
                            }
                        }
                    }
                }
            } // end scrollable Column
        } // end outer Column
    }
}
