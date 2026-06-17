package com.m0h31h31.bamburfidreader.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.m0h31h31.bamburfidreader.model.InventoryItem
import com.m0h31h31.bamburfidreader.model.NfcUiState
import com.m0h31h31.bamburfidreader.model.ReaderBrand
import com.m0h31h31.bamburfidreader.nfc.NfcCompatibilityMode
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.ThemeMode
import java.util.Locale
import kotlin.math.roundToInt

private const val WEB_ORANGE = "#ff6a1a"

private class WorkbenchBridge(
    private val onAction: (String) -> Unit
) {
    @JavascriptInterface
    fun action(name: String) {
        onAction(name)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WorkbenchWebView(
    html: String,
    modifier: Modifier = Modifier,
    onAction: (String) -> Unit = {}
) {
    val bridge = remember(onAction) { WorkbenchBridge(onAction) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.defaultTextEncodingName = "UTF-8"
                addJavascriptInterface(bridge, "Workbench")
            }
        },
        update = { view ->
            view.loadDataWithBaseURL("https://app.local/", html, "text/html", "UTF-8", null)
        }
    )
}

@Composable
fun WebWorkbenchReaderScreen(
    state: NfcUiState,
    voiceStatus: String,
    readerBrand: ReaderBrand,
    onBrandChange: (ReaderBrand) -> Unit,
    onTrayOutbound: (String) -> Unit,
    onReportAnomaly: ((String) -> Unit)?,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    WorkbenchWebView(
        html = buildReaderHtml(state, voiceStatus, readerBrand),
        modifier = modifier,
        onAction = { action ->
            when (action) {
                "outbound" -> if (state.trayUidHex.isNotBlank()) onTrayOutbound(state.trayUidHex)
                "anomaly" -> if (state.uidHex.isNotBlank()) onReportAnomaly?.invoke(state.uidHex)
                "more" -> onMore()
                "brand:bambu" -> onBrandChange(ReaderBrand.BAMBU)
                "brand:creality" -> onBrandChange(ReaderBrand.CREALITY)
                "brand:snapmaker" -> onBrandChange(ReaderBrand.SNAPMAKER)
            }
        }
    )
}

@Composable
fun WebWorkbenchDataScreen(
    groups: Map<String, List<InventoryItem>>,
    mergeEnabled: Boolean,
    onToggleMerge: () -> Unit,
    modifier: Modifier = Modifier
) {
    WorkbenchWebView(
        html = buildDataHtml(groups, mergeEnabled),
        modifier = modifier,
        onAction = { action ->
            if (action == "toggleMerge") onToggleMerge()
        }
    )
}

@Composable
fun WebWorkbenchMiscScreen(
    statusText: String,
    uiStyle: AppUiStyle,
    onUiStyleChange: (AppUiStyle) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    readAllSectors: Boolean,
    onReadAllSectorsChange: (Boolean) -> Unit,
    saveKeysToFile: Boolean,
    onSaveKeysToFileChange: (Boolean) -> Unit,
    nfcCompatibilityMode: NfcCompatibilityMode,
    onNfcCompatibilityModeChange: (NfcCompatibilityMode) -> Unit,
    bambuTagEnabled: Boolean,
    onBambuTagEnabledChange: (Boolean) -> Unit,
    crealityEnabled: Boolean,
    onCrealityEnabledChange: (Boolean) -> Unit,
    snapmakerTagEnabled: Boolean,
    onSnapmakerTagEnabledChange: (Boolean) -> Unit,
    inventoryEnabled: Boolean,
    onInventoryEnabledChange: (Boolean) -> Unit,
    autoDetectBrand: Boolean,
    onAutoDetectBrandChange: (Boolean) -> Unit,
    onClearFuid: () -> Unit,
    onBackupDatabase: () -> Unit,
    onImportDatabase: () -> Unit,
    onSelectImportTagPackage: () -> Unit,
    modifier: Modifier = Modifier
) {
    WorkbenchWebView(
        html = buildMiscHtml(
            statusText = statusText,
            uiStyle = uiStyle,
            themeMode = themeMode,
            readAllSectors = readAllSectors,
            saveKeysToFile = saveKeysToFile,
            nfcCompatibilityMode = nfcCompatibilityMode,
            bambuTagEnabled = bambuTagEnabled,
            crealityEnabled = crealityEnabled,
            snapmakerTagEnabled = snapmakerTagEnabled,
            inventoryEnabled = inventoryEnabled,
            autoDetectBrand = autoDetectBrand
        ),
        modifier = modifier,
        onAction = { action ->
            when (action) {
                "style:web" -> onUiStyleChange(AppUiStyle.MODERN_WORKBENCH)
                "style:compose" -> onUiStyleChange(AppUiStyle.MODERN_WORKBENCH_COMPOSE)
                "style:neumorphic" -> onUiStyleChange(AppUiStyle.NEUMORPHIC)
                "theme:system" -> onThemeModeChange(ThemeMode.SYSTEM)
                "theme:light" -> onThemeModeChange(ThemeMode.LIGHT)
                "theme:dark" -> onThemeModeChange(ThemeMode.DARK)
                "readAll" -> onReadAllSectorsChange(!readAllSectors)
                "saveKeys" -> onSaveKeysToFileChange(!saveKeysToFile)
                "nfc:fast" -> onNfcCompatibilityModeChange(NfcCompatibilityMode.FAST)
                "nfc:balanced" -> onNfcCompatibilityModeChange(NfcCompatibilityMode.BALANCED)
                "nfc:stable" -> onNfcCompatibilityModeChange(NfcCompatibilityMode.STABLE)
                "feature:bambu" -> onBambuTagEnabledChange(!bambuTagEnabled)
                "feature:creality" -> onCrealityEnabledChange(!crealityEnabled)
                "feature:snapmaker" -> onSnapmakerTagEnabledChange(!snapmakerTagEnabled)
                "feature:inventory" -> onInventoryEnabledChange(!inventoryEnabled)
                "feature:autoDetect" -> onAutoDetectBrandChange(!autoDetectBrand)
                "format" -> onClearFuid()
                "backup" -> onBackupDatabase()
                "importDb" -> onImportDatabase()
                "importPackage" -> onSelectImportTagPackage()
            }
        }
    )
}

private fun buildReaderHtml(
    state: NfcUiState,
    voiceStatus: String,
    readerBrand: ReaderBrand
): String {
    val title = state.displayDetailedType.ifBlank { state.displayType }.ifBlank { "PLA Basic" }
    val subtitle = listOf(state.displayColorName.ifBlank { "玉石白" }, state.displayFilaColorCode.ifBlank { state.displayColorCode })
        .filter { it.isNotBlank() }
        .joinToString(" - ")
        .ifBlank { "玉石白 - 10100" }
    val uid = state.uidHex.ifBlank { "06FF8FC9" }
    val trayUid = state.trayUidHex.ifBlank { uid }
    val total = state.totalWeightGrams.takeIf { it > 0 } ?: 1000
    val grams = state.remainingGrams.takeIf { it > 0 }
        ?: (total * state.remainingPercent.coerceIn(0f, 100f) / 100f).roundToInt().takeIf { it > 0 }
        ?: total
    val percent = if (total > 0) grams * 100f / total else state.remainingPercent
    val color = state.displayColors.firstOrNull()?.toCssColor() ?: "#f8f7f2"
    val brandTabs = listOf(
        "brand:bambu" to "▥ 拓竹" to (readerBrand == ReaderBrand.BAMBU),
        "brand:creality" to "△ 创想" to (readerBrand == ReaderBrand.CREALITY),
        "brand:snapmaker" to "U1 快造" to (readerBrand == ReaderBrand.SNAPMAKER)
    )
    val details = listOf(
        "⌑" to "卡 UID" to uid,
        "◌" to "颜色类型" to state.displayColorType.ifBlank { "单色" },
        "▣" to "耗材重量" to "$total 克",
        "◉" to "耗材直径" to "1.75 毫米",
        "≋" to "烘干温度" to state.secondaryFields.find { it.label.contains("烘") || it.label.contains("Dry", true) }?.value.orEmpty().ifBlank { "55 °C" },
        "⊥" to "喷嘴最高温度" to state.secondaryFields.find { it.label.contains("喷") || it.label.contains("Print", true) }?.value.orEmpty().ifBlank { "230 °C" },
        "□" to "生产日期" to state.secondaryFields.find {
            it.label.contains("生产") || it.label.contains("Mfg", true) || it.label.contains("Date", true)
        }?.value.orEmpty().ifBlank { "2025-12-10 13:36" }
    )
    return page(
        body = """
        <main class="screen reader">
          <section class="topline">
            <div class="status"><span class="check">✓</span><b>${state.status.ifBlank { "读取成功" }.esc()}</b></div>
            <button class="voice">${voiceStatus.esc()}</button>
          </section>
          <section class="card hero-card">
            <div class="hero-top">
              <div class="swatch" style="background:${color.esc()}"></div>
              <div class="material">
                <h1>${title.esc()}</h1>
                <p>${subtitle.esc()}</p>
                <span class="uid">▣ 卡 UID： ${uid.esc()}</span>
              </div>
              <button class="mini" onclick="Workbench.action('outbound')">⇧ 出库</button>
            </div>
            <div class="dash"></div>
            <div class="remaining">
              <div><span>剩余量</span><strong>${"%.1f".format(Locale.US, percent)}<small>%</small></strong></div>
              <p>${grams}g / ${total}g</p>
            </div>
            <div class="bar"><i style="width:${percent.coerceIn(0f, 100f)}%"></i></div>
            <div class="stepper"><button>−</button><b>${grams}</b><span>克</span><button class="plus">+</button></div>
          </section>
          <section class="actions">
            <button class="primary" onclick="Workbench.action('outbound')">⇲ 出库</button>
            <button class="danger" onclick="Workbench.action('anomaly')">⚠ 异常上报</button>
            <button onclick="Workbench.action('more')">⊙ 更多</button>
          </section>
          <section class="segments">
            ${brandTabs.joinToString("") { (actionLabel, selected) ->
                val (action, label) = actionLabel
                """<button class="${if (selected) "active" else ""}" onclick="Workbench.action('$action')">${label.esc()}</button>"""
            }}
          </section>
          <section class="card details">
            <header><h2>耗材详情</h2><button>▣ 复制</button></header>
            ${details.joinToString("") { (iconLabel, value) ->
                val (icon, label) = iconLabel
                """<div class="row"><span class="ico">$icon</span><span>${label.esc()}</span><b>${value.esc()}</b></div>"""
            }}
          </section>
        </main>
        """
    )
}

private fun buildDataHtml(groups: Map<String, List<InventoryItem>>, mergeEnabled: Boolean): String {
    val allItems = groups.values.flatten()
    val firstGroupName = groups.keys.firstOrNull() ?: "PLA Basic"
    val items = groups[firstGroupName].orEmpty().ifEmpty { allItems }
    val cards = items.take(35).mapIndexed { index, item ->
        val color = item.colorValues.firstOrNull()?.toCssColor() ?: item.colorCode.toCssColor()
        val badge = if (mergeEnabled) (index * 17 % 320 + 5).toString() else ""
        """
        <article class="color-card">
          <div class="color" style="background:${color.esc()}"></div>
          ${if (badge.isNotBlank()) """<em>$badge</em>""" else ""}
          <strong>${item.resolvedColorName().ifBlank { "颜色" }.esc()}</strong>
          <span>${item.filaColorCode.ifBlank { item.colorCode }.esc()}</span>
          <small>${item.trayUid.takeLast(2).ifBlank { "A0" }.esc()}</small>
        </article>
        """
    }.joinToString("")
    return page(
        body = """
        <main class="screen data">
          <section class="search"><span>⌕</span><input placeholder="搜索颜色、料号、材质"/><button>☰</button></section>
          <section class="stats"><b>▨ 总数 <i>${allItems.size}</i></b><span></span><b>▽ 当前筛选 <i>${items.size}</i></b></section>
          <section class="chips"><button class="active">PLA Basic</button><button>PLA Matte</button><button>PETG HF</button><button>ABS</button></section>
          <section class="card palette">
            <header><h1>▨ ${firstGroupName.esc()} <small>(${items.size})</small></h1><b>⌃</b></header>
            <div class="grid">${cards}</div>
          </section>
          <section class="notice"><h2>⚠ 写入注意事项</h2><p>1. 标签必须紧贴手机 NFC 区域，写入过程中不要移动。</p><p>2. 写入或校验失败时，移开标签重新贴上，程序将自动重试。</p><p>3. 不要写入已有的标签，相同标签会被识别为一卷料。</p><p><b>4. 写入可能失败！作者不对任何后果负责！</b></p></section>
          <section class="bottom-actions"><button>▭ 空卡写入</button><button class="primary">✎ CUID改写</button></section>
        </main>
        """
    )
}

private fun buildMiscHtml(
    statusText: String,
    uiStyle: AppUiStyle,
    themeMode: ThemeMode,
    readAllSectors: Boolean,
    saveKeysToFile: Boolean,
    nfcCompatibilityMode: NfcCompatibilityMode,
    bambuTagEnabled: Boolean,
    crealityEnabled: Boolean,
    snapmakerTagEnabled: Boolean,
    inventoryEnabled: Boolean,
    autoDetectBrand: Boolean
): String {
    fun switch(on: Boolean, action: String) =
        """<button class="switch ${if (on) "on" else ""}" onclick="Workbench.action('$action')"><i></i></button>"""
    fun seg(action: String, label: String, selected: Boolean) =
        """<button class="${if (selected) "active" else ""}" onclick="Workbench.action('$action')">${label.esc()}</button>"""
    fun feature(title: String, subtitle: String, on: Boolean, action: String) =
        """<div class="setting"><div><b>${title.esc()}</b><span>${subtitle.esc()}</span></div>${switch(on, action)}</div>"""
    return page(
        body = """
        <main class="screen misc">
          <header class="misc-head"><div><h1>配置</h1><p>阅读、写入与数据管理</p></div><b>⚙</b></header>
          <section class="status-banner"><span></span><b>${statusText.esc()}</b><i>◉</i></section>
          <section class="card config"><h2>◉ 外观</h2>
            <div class="setting"><div><b>界面风格</b><span>选择应用界面风格</span></div><div class="seg">${seg("style:web", "Web", uiStyle == AppUiStyle.MODERN_WORKBENCH)}${seg("style:compose", "Compose", uiStyle == AppUiStyle.MODERN_WORKBENCH_COMPOSE)}${seg("style:neumorphic", "原生", uiStyle == AppUiStyle.NEUMORPHIC)}</div></div>
            <div class="setting"><div><b>深色模式</b><span>跟随系统</span></div><div class="seg">${seg("theme:system", "跟随系统", themeMode == ThemeMode.SYSTEM)}${seg("theme:light", "浅色", themeMode == ThemeMode.LIGHT)}${seg("theme:dark", "深色", themeMode == ThemeMode.DARK)}</div></div>
            <div class="setting"><div><b>主题配色</b><span>当前配色：橙橙</span></div><span class="orange-dot"></span></div>
          </section>
          <section class="card config"><h2>)) NFC</h2>
            ${feature("读取全部扇区数据并保存文件", "读取全部扇区数据并保存到文件", readAllSectors, "readAll")}
            ${feature("保存秘钥到文件", "将读取的秘钥保存到文件", saveKeysToFile, "saveKeys")}
            <div class="setting"><div><b>NFC兼容模式</b><span>当择NFC读写兼容策略</span></div><div class="seg">${seg("nfc:fast", "快速", nfcCompatibilityMode == NfcCompatibilityMode.FAST)}${seg("nfc:balanced", "均衡", nfcCompatibilityMode == NfcCompatibilityMode.BALANCED)}${seg("nfc:stable", "稳定", nfcCompatibilityMode == NfcCompatibilityMode.STABLE)}</div></div>
          </section>
          <section class="card config"><h2>▦ 功能入口</h2>
            ${feature("拓竹 RFID", "开启拓竹耗材标签相关功能", bambuTagEnabled, "feature:bambu")}
            ${feature("创想三维 RFID", "开启创想三维耗材标签相关功能", crealityEnabled, "feature:creality")}
            ${feature("快造 U1 RFID", "开启快造 U1 耗材标签相关功能", snapmakerTagEnabled, "feature:snapmaker")}
            ${feature("库存与数据功能", "开启后显示库存和数据页面", inventoryEnabled, "feature:inventory")}
            ${feature("自动识别 RFID", "刷卡时自动检测品牌并跳转到对应识别页", autoDetectBrand, "feature:autoDetect")}
          </section>
          <section class="card config maintenance"><h2>◎ 数据维护</h2>
            <button onclick="Workbench.action('format')">◇<b>格式化标签</b><span>清空标签并复位</span></button>
            <button onclick="Workbench.action('importPackage')">⇩<b>导入标签包</b><span>导入共享标签包</span></button>
            <button onclick="Workbench.action('backup')">◎<b>备份数据库</b><span>导出并备份本地数据</span></button>
            <button onclick="Workbench.action('importDb')">⇧<b>导入数据库</b><span>从文件导入数据库</span></button>
          </section>
        </main>
        """
    )
}

private fun page(body: String): String = """
<!doctype html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"/>
<style>
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent} body{margin:0;background:#fbfaf9;color:#172033;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","Noto Sans SC",sans-serif}.screen{padding:18px 16px 24px}.card{background:white;border:1px solid #e4e7ec;border-radius:18px;box-shadow:0 8px 24px rgba(20,25,35,.07)}button{font:inherit;border:0;background:white;color:#172033}.topline{display:flex;align-items:center;justify-content:space-between;margin:8px 6px 18px}.status{display:flex;align-items:center;gap:12px;font-size:22px}.check{display:grid;place-items:center;width:24px;height:24px;border-radius:50%;background:#16a34a;color:white;font-weight:800}.voice{border:1px solid #d9dee7;border-radius:999px;padding:10px 16px;font-weight:700}.hero-card{padding:20px 18px}.hero-top{display:flex;gap:18px;align-items:flex-start}.swatch{width:82px;height:82px;border-radius:14px;border:1px solid #dedede}.material{flex:1}.material h1{margin:6px 0 8px;font-size:28px;line-height:1;font-weight:800}.material p{margin:0 0 12px;font-size:19px;color:#475569}.uid{display:inline-block;border:1px solid #e1e5ea;border-radius:8px;padding:6px 10px;color:#697386;background:#fafafa}.mini{color:$WEB_ORANGE;background:#fff1ea;border-radius:12px;padding:9px 12px;font-weight:800}.dash{border-top:1px dashed #d8dde5;margin:22px 0}.remaining{display:flex;justify-content:space-between;align-items:flex-end}.remaining span{display:block;color:#697386;margin-bottom:4px}.remaining strong{font-size:34px;color:$WEB_ORANGE}.remaining small{font-size:20px}.remaining p{color:#697386}.bar{height:8px;background:#e9edf3;border-radius:999px;overflow:hidden}.bar i{display:block;height:100%;background:$WEB_ORANGE;border-radius:999px}.stepper{display:grid;grid-template-columns:50px 1fr 50px 50px;align-items:center;border:1px solid #dde3ea;border-radius:14px;margin-top:22px;height:58px;text-align:center}.stepper button{margin:8px;border:1px solid #d8dee8;border-radius:10px;height:42px;font-size:28px}.stepper b{font-size:24px}.stepper .plus{color:$WEB_ORANGE;border-color:$WEB_ORANGE}.actions,.segments,.bottom-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:14px}.actions button,.segments button,.bottom-actions button{height:54px;border:1px solid #dfe4ea;border-radius:12px;font-weight:800;font-size:17px}.actions .primary,.bottom-actions .primary{background:$WEB_ORANGE;color:white;border-color:$WEB_ORANGE}.actions .danger{color:#ef342c}.segments{border:1px solid #dfe4ea;border-radius:14px;padding:4px;gap:4px}.segments button{height:46px;border-color:transparent;color:#5f6875}.segments .active{background:#fff1ea;color:$WEB_ORANGE;border-color:$WEB_ORANGE}.details{margin-top:18px;padding:16px}.details header,.palette header{display:flex;justify-content:space-between;align-items:center}.details h2,.palette h1{margin:0;font-size:20px}.details header button{color:$WEB_ORANGE;font-weight:800}.row{display:grid;grid-template-columns:34px 1fr auto;gap:8px;align-items:center;padding:15px 0;border-top:1px solid #e6e9ee}.row:first-of-type{border-top:0}.row span{color:#697386}.row b{font-size:17px}.search{display:grid;grid-template-columns:1fr 42px;gap:14px;align-items:center}.search>span{position:absolute;margin-left:16px;font-size:24px}.search input{height:54px;border:1px solid #dfe4ea;border-radius:16px;padding:0 18px 0 48px;font-size:19px;background:white}.search button{font-size:24px}.stats{display:flex;justify-content:center;gap:30px;padding:22px 0 18px}.stats span{width:1px;background:#d4d8df}.stats i{color:$WEB_ORANGE;font-style:normal;margin-left:8px}.chips{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:14px}.chips button{height:46px;border:1px solid #dfe4ea;border-radius:999px;font-size:17px}.chips .active{border-color:$WEB_ORANGE;color:$WEB_ORANGE}.palette{padding:16px}.grid{display:grid;grid-template-columns:repeat(5,1fr);gap:14px;margin-top:14px}.color-card{position:relative;height:112px;border:1px solid #e5e8ef;border-radius:10px;overflow:hidden;text-align:center;background:white}.color-card .color{height:58px}.color-card em{position:absolute;right:3px;top:3px;background:rgba(20,25,35,.82);color:white;border-radius:999px;min-width:26px;padding:2px 5px;font-style:normal}.color-card strong{display:block;margin-top:5px;font-size:14px}.color-card span,.color-card small{display:block;font-size:12px}.notice{margin-top:14px;border:1px solid #e4e7ec;border-radius:14px;background:white;padding:13px 18px}.notice h2{color:$WEB_ORANGE;font-size:18px}.notice p{margin:7px 0}.notice b{color:$WEB_ORANGE}.bottom-actions{grid-template-columns:1fr 1fr}.misc-head{display:flex;justify-content:space-between;align-items:flex-start;margin:6px 10px 22px}.misc-head h1{font-size:34px;margin:0}.misc-head p{margin:6px 0;color:#697386;font-size:17px}.misc-head>b{color:$WEB_ORANGE;font-size:32px}.status-banner{display:flex;align-items:center;gap:12px;border:1px solid #ffd2bf;background:#fff9f5;border-radius:15px;padding:14px 16px;color:$WEB_ORANGE}.status-banner span,.orange-dot{width:18px;height:18px;border-radius:50%;background:$WEB_ORANGE;display:inline-block}.status-banner b{flex:1}.config{padding:14px;margin-top:14px}.config h2{font-size:20px;margin:0 0 12px}.setting{display:flex;align-items:center;justify-content:space-between;gap:12px;border-top:1px solid #f0e3dc;padding:12px 2px}.setting:first-of-type{border-top:0}.setting b{display:block;font-size:17px}.setting span{display:block;color:#697386}.seg{display:flex;border:1px solid #dde3ea;border-radius:10px;overflow:hidden}.seg button{padding:9px 14px;border-left:1px solid #dde3ea;color:#697386}.seg button:first-child{border-left:0}.seg .active{background:$WEB_ORANGE;color:white}.switch{width:46px;height:26px;border-radius:999px;background:#e5e7eb;padding:3px}.switch i{display:block;width:20px;height:20px;background:white;border-radius:50%}.switch.on{background:$WEB_ORANGE}.switch.on i{margin-left:20px}.maintenance{display:grid;grid-template-columns:1fr 1fr;gap:10px}.maintenance h2{grid-column:1/-1}.maintenance button{height:78px;border:1px solid #ffe0d2;border-radius:12px;color:$WEB_ORANGE;display:grid;grid-template-columns:38px 1fr;grid-template-rows:1fr 1fr;text-align:left;align-items:center;padding:12px}.maintenance button>b{color:#172033}.maintenance button>span{color:#697386}
</style>
</head>
<body>$body</body>
</html>
"""

private fun String.esc(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private fun String.toCssColor(): String {
    val raw = trim().removePrefix("#")
    return when (raw.length) {
        6 -> "#$raw"
        8 -> "#${raw.take(6)}"
        else -> if (startsWith("#")) this else "#f8f7f2"
    }
}
