package com.m0h31h31.bamburfidreader.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.cloud.BambuCloudTaskMaterial
import com.m0h31h31.bamburfidreader.cost.CostCalculator
import com.m0h31h31.bamburfidreader.cost.CostConfig
import com.m0h31h31.bamburfidreader.cost.CostController
import com.m0h31h31.bamburfidreader.cost.CostBreakdown
import com.m0h31h31.bamburfidreader.cost.FeeTarget
import com.m0h31h31.bamburfidreader.cost.FeeUnit
import com.m0h31h31.bamburfidreader.cost.MaterialPrice
import com.m0h31h31.bamburfidreader.cost.Money
import com.m0h31h31.bamburfidreader.cost.OrderView
import com.m0h31h31.bamburfidreader.cost.OtherFee
import com.m0h31h31.bamburfidreader.cost.PerGramPrice
import com.m0h31h31.bamburfidreader.cost.PrintTaskRow
import com.m0h31h31.bamburfidreader.cost.QuoteInput
import com.m0h31h31.bamburfidreader.cost.TaskState
import com.m0h31h31.bamburfidreader.cost.buildOrderViews
import com.m0h31h31.bamburfidreader.ui.components.AppIcons
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CostScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val controller = remember(context) { CostController.get(context) }
    val cloud = remember(context) { BambuCloudController.get(context) }

    LaunchedEffect(Unit) {
        controller.ensureLoaded()
        cloud.ensureLoaded(context)
    }

    val tasks by controller.tasksState
    val orders by controller.ordersState
    val config by controller.configState
    val prices by controller.pricesState
    val syncing by controller.syncingState
    val statusMessage by controller.statusMessageState
    val showHidden by controller.showHiddenState
    val session by cloud.sessionState
    val materialTypesByFilaId = remember(prices) {
        prices.associate { normalizeFilaId(it.filaId) to it.filaType.trim() }
    }

    val orderViews = remember(tasks, orders, showHidden) {
        buildOrderViews(tasks.filter { if (showHidden) it.hidden else !it.hidden }, orders)
    }
    val stats = remember(tasks, orders) {
        CostCalculator.computeStats(buildOrderViews(tasks.filter { !it.hidden }, orders), includeFailed = false)
    }
    val hiddenCount = remember(tasks) { tasks.count { it.hidden } }

    val selected = remember { mutableStateListOf<Long>() }
    var showQuote by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }
    var showPrices by remember { mutableStateOf(false) }
    var chargeTarget by remember { mutableStateOf<OrderView?>(null) }
    var detailTarget by remember { mutableStateOf<OrderView?>(null) }
    var query by remember { mutableStateOf("") }
    val shownViews = remember(orderViews, query) {
        if (query.isBlank()) orderViews else orderViews.filter { matchesQuery(it, query) }
    }

    // 固定头部 + 仅列表滚动
    Column(
        modifier = modifier
            .fillMaxSize()
            .neuBackground()
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.size(4.dp))
        CostStatsCard(stats.orderCount, stats.totalCostCents, stats.totalRevenueCents, stats.totalProfitCents, stats.marginPercent, stats.avgOrderValueCents)
        CostToolbar(
            syncing = syncing,
            statusMessage = statusMessage,
            onSync = { controller.sync() },
            onQuote = { showQuote = true },
            onConfig = { showConfig = true },
            onPrices = { showPrices = true }
        )
        if (session == null) CostLoginHint()

        if (tasks.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CostSearchField(query, { query = it }, Modifier.weight(1f))
                if (hiddenCount > 0 || showHidden) {
                    SmallButton(
                        if (showHidden) stringResource(R.string.cost_hide_hidden) else stringResource(R.string.cost_show_hidden, hiddenCount),
                        { controller.toggleShowHidden() },
                        primary = showHidden
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (shownViews.isEmpty()) {
                CostEmpty()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 2.dp, bottom = if (selected.isNotEmpty()) 72.dp else 12.dp)
                ) {
                    items(shownViews, key = { it.orderId ?: -it.tasks.first().id }) { ov ->
                        val hiddenView = ov.orderId == null && ov.tasks.first().hidden
                        OrderCard(
                            ov = ov,
                            selectable = ov.orderId == null && !hiddenView,
                            selected = ov.orderId == null && selected.contains(ov.tasks.first().id),
                            hidden = hiddenView,
                            onToggleSelect = {
                                val id = ov.tasks.first().id
                                if (selected.contains(id)) selected.remove(id) else selected.add(id)
                            },
                            onSetCharge = { chargeTarget = ov },
                            onDissolve = { ov.orderId?.let { controller.dissolveOrder(it) } },
                            onRestore = { controller.restoreTasks(listOf(ov.tasks.first().id)) },
                            onShowDetail = { detailTarget = ov },
                            materialTypesByFilaId = materialTypesByFilaId
                        )
                    }
                }
            }
            // 浮动合并条:无需上滑回顶
            if (selected.isNotEmpty()) {
                MergeFloatingBar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                    count = selected.size,
                    onMerge = {
                        controller.aggregateIntoOrder(selected.toList(), context.getString(R.string.cost_default_order_name))
                        selected.clear()
                    },
                    onHide = {
                        controller.hideTasks(selected.toList())
                        selected.clear()
                    },
                    onCancel = { selected.clear() }
                )
            }
        }
    }

    if (showQuote) QuoteDialog(config = config, prices = prices, onDismiss = { showQuote = false })
    if (showConfig) ConfigDialog(config = config, onSave = { controller.saveConfig(it); showConfig = false }, onDismiss = { showConfig = false })
    if (showPrices) PricesDialog(prices = prices, onSet = { id, c -> controller.setMaterialPrice(id, c) }, onDismiss = { showPrices = false })
    chargeTarget?.let { ov ->
        ChargeDialog(ov = ov, onSave = { c -> controller.setActualCharge(ov, c); chargeTarget = null }, onDismiss = { chargeTarget = null })
    }
    detailTarget?.let { ov ->
        DetailDialog(ov = ov, controller = controller, materialTypesByFilaId = materialTypesByFilaId, onDismiss = { detailTarget = null })
    }
}

@Composable
private fun CostStatsCard(orders: Int, costCents: Long, revenueCents: Long, profitCents: Long, marginPercent: Double, avgCents: Long) {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(stringResource(R.string.cost_stat_orders), orders.toString(), Modifier.weight(1f))
                StatItem(stringResource(R.string.cost_stat_cost), Money.format(costCents), Modifier.weight(1f))
                StatItem(stringResource(R.string.cost_stat_revenue), Money.format(revenueCents), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(stringResource(R.string.cost_stat_profit), Money.format(profitCents), Modifier.weight(1f), profitColor(profitCents))
                StatItem(stringResource(R.string.cost_stat_margin), String.format(Locale.getDefault(), "%.1f%%", marginPercent), Modifier.weight(1f))
                StatItem(stringResource(R.string.cost_stat_avg), Money.format(avgCents), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CostToolbar(syncing: Boolean, statusMessage: String, onSync: () -> Unit, onQuote: () -> Unit, onConfig: () -> Unit, onPrices: () -> Unit) {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuButton(text = if (syncing) stringResource(R.string.cost_syncing) else stringResource(R.string.cost_sync), onClick = onSync, enabled = !syncing, modifier = Modifier.weight(1f))
                NeuButton(text = stringResource(R.string.cost_quote), onClick = onQuote, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuButton(text = stringResource(R.string.cost_config), onClick = onConfig, modifier = Modifier.weight(1f))
                NeuButton(text = stringResource(R.string.cost_prices), onClick = onPrices, modifier = Modifier.weight(1f))
            }
            if (statusMessage.isNotBlank()) {
                Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CostLoginHint() {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cost_login_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CostEmpty() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.cost_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MergeFloatingBar(modifier: Modifier, count: Int, onMerge: () -> Unit, onHide: () -> Unit, onCancel: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cost_merge_selected, count), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            SmallButton(stringResource(R.string.action_cancel), onCancel)
            Spacer(Modifier.width(6.dp))
            SmallButton(stringResource(R.string.cost_hide), onHide)
            Spacer(Modifier.width(6.dp))
            SmallButton(stringResource(R.string.cost_merge), onMerge, primary = true)
        }
    }
}

@Composable
private fun OrderCard(
    ov: OrderView,
    selectable: Boolean,
    selected: Boolean,
    hidden: Boolean,
    onToggleSelect: () -> Unit,
    onSetCharge: () -> Unit,
    onDissolve: () -> Unit,
    onRestore: () -> Unit,
    onShowDetail: () -> Unit,
    materialTypesByFilaId: Map<String, String>
) {
    var expanded by remember(ov) { mutableStateOf(false) }
    val isOrder = ov.orderId != null
    val first = ov.tasks.first()
    // 合并订单主条目显示所有耗材合计;单任务显示自身各耗材
    val materialLine = if (isOrder) {
        aggregateMaterials(ov.tasks, materialTypesByFilaId)
    } else {
        materialsSummary(first.materials, first.weightGrams, materialTypesByFilaId)
    }
    val durationSeconds = if (isOrder) ov.tasks.sumOf { it.costTimeSeconds } else first.costTimeSeconds
    NeuPanel(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectable) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelect() }, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(6.dp))
                }
                CoverImage(first.coverPath, 44.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f).clickable(onClick = onShowDetail)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isOrder && ov.tasks.size > 1) {
                                val rep = first.title.let { if (it.length > 8) it.take(8) + "…" else it }
                                "${ov.name} $rep${stringResource(R.string.cost_order_items_more, ov.tasks.size)}"
                            } else if (isOrder) {
                                ov.name
                            } else first.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (hidden) HiddenBadge()
                        else StateBadge(if (ov.anyFailed) TaskState.FAILED else if (ov.anyPrinting) TaskState.PRINTING else TaskState.SUCCESS)
                    }
                    Text(materialLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${formatDate(first.startTimeMillis)} · ${formatDuration(durationSeconds)} · ${stringResource(R.string.cost_cost_label)} ${Money.format(ov.costCents)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ov.actualChargeCents > 0) {
                    Text(
                        buildString {
                            append("${Money.format(ov.actualChargeCents)}  ")
                            append("${stringResource(R.string.cost_profit_label)} ${Money.format(ov.profitCents)} (${String.format(Locale.getDefault(), "%.0f%%", ov.marginPercent)})")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = profitColor(ov.profitCents),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (hidden) {
                    SmallButton(stringResource(R.string.cost_restore), onRestore, primary = true)
                } else {
                    if (isOrder) {
                        SmallButton(stringResource(R.string.cost_dissolve), onDissolve)
                        Spacer(Modifier.width(6.dp))
                        if (ov.tasks.size > 1) {
                            SmallButton(if (expanded) stringResource(R.string.cost_collapse) else stringResource(R.string.cost_expand), { expanded = !expanded })
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    SmallButton(stringResource(R.string.cost_set_charge), onSetCharge, primary = true)
                }
            }
            if (expanded && ov.tasks.size > 1) {
                Spacer(Modifier.size(2.dp))
                ov.tasks.forEach { t -> SubTaskRow(t, materialTypesByFilaId) }
            }
        }
    }
}

/** 合并订单展开后的子条目:左图 + 右两行。 */
@Composable
private fun SubTaskRow(t: PrintTaskRow, materialTypesByFilaId: Map<String, String>) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverImage(t.coverPath, 36.dp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(t.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${materialsSummary(t.materials, t.weightGrams, materialTypesByFilaId)} · ${formatDuration(t.costTimeSeconds)} · ${Money.format(t.computedCostCents)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HiddenBadge() {
    Box(modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(stringResource(R.string.cost_hidden_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StateBadge(state: TaskState) {
    if (state == TaskState.SUCCESS) return
    val (bg, fg, label) = when (state) {
        TaskState.PRINTING -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, stringResource(R.string.cost_state_printing))
        else -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, stringResource(R.string.cost_failed_badge))
    }
    Box(modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit, primary: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CoverImage(path: String, size: Dp) {
    val image = remember(path) {
        if (path.isNotBlank() && File(path).exists()) runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() else null
    }
    Box(modifier = Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text("3D", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------- Dialogs ----------------

@Composable
private fun ChargeDialog(ov: OrderView, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(if (ov.actualChargeCents > 0) Money.toPlain(ov.actualChargeCents) else "") }
    val cents = Money.parse(text) ?: 0L
    val profit = cents - ov.costCents
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cost_set_charge)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${stringResource(R.string.cost_cost_label)}: ${Money.format(ov.costCents)}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.cost_actual_charge)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Text("${stringResource(R.string.cost_profit_label)}: ${Money.format(profit)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = profitColor(profit))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(Money.parse(text) ?: 0L) }) { Text(stringResource(R.string.action_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun DetailDialog(ov: OrderView, controller: CostController, materialTypesByFilaId: Map<String, String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (ov.orderId != null) ov.name else ov.tasks.first().title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ov.tasks) { t -> DetailTaskSection(t, controller.breakdownOf(t), controller) }
                item {
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cost_detail_total_cost), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(Money.format(ov.costCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    if (ov.actualChargeCents > 0) {
                        BreakdownLine(stringResource(R.string.cost_actual_short), ov.actualChargeCents)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.cost_profit_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${Money.format(ov.profitCents)} (${String.format(Locale.getDefault(), "%.0f%%", ov.marginPercent)})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = profitColor(ov.profitCents))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
private fun DetailTaskSection(t: PrintTaskRow, bd: CostBreakdown, controller: CostController) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(t.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            InfoRow(stringResource(R.string.cost_detail_device), "${t.deviceName} ${t.deviceModel}".trim())
            InfoRow(stringResource(R.string.cost_detail_start), formatDateTime(t.startTimeMillis))
            InfoRow(stringResource(R.string.cost_detail_duration), formatDuration(t.costTimeSeconds))
            InfoRow(stringResource(R.string.cost_detail_weight), "%.1fg".format(t.weightGrams))
            if (t.repetitions > 1) InfoRow(stringResource(R.string.cost_detail_reps), "×${t.repetitions}")
            HorizontalDivider()
            t.materials.forEach { m ->
                val price = controller.priceFor(m.filamentId)
                val sub = Math.round(PerGramPrice.materialCents(m.weightGrams, price))
                val name = m.filamentType.ifBlank { m.filamentId }.ifBlank { "?" }
                BreakdownLine("$name ${"%.1f".format(m.weightGrams)}g × ${Money.symbol()}${PerGramPrice.toPlain(price)}/g", sub)
            }
            BreakdownLine(stringResource(R.string.cost_bd_material), bd.materialCents)
            BreakdownLine(stringResource(R.string.cost_bd_electricity), bd.electricityCents)
            BreakdownLine(stringResource(R.string.cost_bd_depreciation), bd.depreciationCents)
            if (bd.multicolorCents != 0L) BreakdownLine(stringResource(R.string.cost_bd_multicolor), bd.multicolorCents)
            if (bd.otherCents != 0L) BreakdownLine(stringResource(R.string.cost_bd_other), bd.otherCents)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cost_bd_total), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(Money.format(bd.totalCents), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuoteDialog(config: CostConfig, prices: List<MaterialPrice>, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf("") }
    var timeMin by remember { mutableStateOf("") }
    var colors by remember { mutableStateOf("1") }
    var plates by remember { mutableStateOf("1") }
    var selectedPrice by remember { mutableStateOf<MaterialPrice?>(null) }
    var pricePerG by remember { mutableStateOf(PerGramPrice.toPlain(config.defaultPricePerGCents)) }

    val input = QuoteInput(
        weightGrams = weight.toDoubleOrNull() ?: 0.0,
        pricePerGCents = PerGramPrice.parse(pricePerG) ?: config.defaultPricePerGCents,
        estTimeSeconds = ((timeMin.toDoubleOrNull() ?: 0.0) * 60).toInt(),
        deviceModel = "",
        colorCount = colors.toIntOrNull() ?: 1,
        plateCount = plates.toIntOrNull() ?: 1
    )
    val q = CostCalculator.computeQuote(input, config)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.cost_quote_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MaterialSearchField(prices = prices, selected = selectedPrice, onSelect = {
                    selectedPrice = it
                    pricePerG = PerGramPrice.toPlain(it.pricePerGCents)
                })
                LabeledField(stringResource(R.string.cost_quote_weight), weight) { weight = it }
                LabeledField(stringResource(R.string.cost_price_per_g), pricePerG) { pricePerG = it }
                LabeledField(stringResource(R.string.cost_quote_time_min), timeMin) { timeMin = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabeledField(stringResource(R.string.cost_quote_colors), colors, Modifier.weight(1f)) { colors = it }
                    LabeledField(stringResource(R.string.cost_quote_plates), plates, Modifier.weight(1f)) { plates = it }
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.cost_quote_breakdown), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    BreakdownLine(stringResource(R.string.cost_bd_material), q.materialCents)
                    BreakdownLine(stringResource(R.string.cost_bd_electricity), q.electricityCents)
                    BreakdownLine(stringResource(R.string.cost_bd_depreciation), q.depreciationCents)
                    if (q.multicolorCents != 0L) BreakdownLine(stringResource(R.string.cost_bd_multicolor), q.multicolorCents)
                    BreakdownLine("${stringResource(R.string.cost_bd_markup)} ×${config.quoteMarkup}", q.markedUpCents)
                    if (q.serviceCents != 0L) BreakdownLine(stringResource(R.string.cost_cfg_service), q.serviceCents)
                    if (q.shippingCents != 0L) BreakdownLine(stringResource(R.string.cost_cfg_shipping), q.shippingCents)
                    config.otherFees.filter { it.appliesToQuote() }.forEach { fee ->
                        BreakdownLine("${fee.name} (${feeUnitLabel(fee.unit)})", quoteOtherFeeCents(fee, input))
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(stringResource(R.string.cost_quote_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(Money.format(q.totalCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

/** 输入框带搜索下拉的耗材选择(combobox)。 */
@Composable
private fun MaterialSearchField(prices: List<MaterialPrice>, selected: MaterialPrice?, onSelect: (MaterialPrice) -> Unit) {
    var query by remember { mutableStateOf(selected?.filaType ?: "") }
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(query, prices, expanded) {
        if (!expanded) emptyList()
        else if (query.isBlank()) prices.take(30)
        else prices.filter { it.filaType.contains(query, true) || it.filaId.contains(query, true) }.take(30)
    }
    Box {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            label = { Text(stringResource(R.string.cost_quote_material)) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(AppIcons.ArrowDownward, contentDescription = null)
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
            properties = PopupProperties(focusable = false)
        ) {
            filtered.forEach { p ->
                DropdownMenuItem(
                    text = { Text("${p.filaType} (${p.filaId})  ${Money.symbol()}${PerGramPrice.toPlain(p.pricePerGCents)}/g") },
                    onClick = { onSelect(p); query = p.filaType; expanded = false }
                )
            }
        }
    }
}

@Composable
private fun ConfigDialog(config: CostConfig, onSave: (CostConfig) -> Unit, onDismiss: () -> Unit) {
    var electricity by remember { mutableStateOf(Money.toPlain(config.electricityPerKwhCents)) }
    var service by remember { mutableStateOf(Money.toPlain(config.serviceFeeCents)) }
    var shipping by remember { mutableStateOf(Money.toPlain(config.baseShippingCents)) }
    var waste by remember { mutableStateOf((config.multicolorWasteFactor * 100).toString()) }
    var surcharge by remember { mutableStateOf(Money.toPlain(config.multicolorSurchargeCents)) }
    var markup by remember { mutableStateOf(config.quoteMarkup.toString()) }
    var minOrder by remember { mutableStateOf(Money.toPlain(config.minOrderCents)) }
    var rounding by remember { mutableStateOf(Money.toPlain(config.roundingCents)) }
    var defaultPrice by remember { mutableStateOf(PerGramPrice.toPlain(config.defaultPricePerGCents)) }
    var power by remember { mutableStateOf(config.defaultPowerWatts.toString()) }
    var deprec by remember { mutableStateOf(Money.toPlain(config.defaultDepreciationPerHourCents)) }
    val otherFees = remember { config.otherFees.toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.cost_config_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.cost_config_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ConfigSectionHeader(
                        title = stringResource(R.string.cost_cfg_section_cost_impact),
                        subtitle = stringResource(R.string.cost_cfg_section_cost_impact_desc)
                    )
                }
                item { ConfigRow(stringResource(R.string.cost_cfg_electricity), electricity) { electricity = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_default_price), defaultPrice) { defaultPrice = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_waste), waste) { waste = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_surcharge), surcharge) { surcharge = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_power), power) { power = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_deprec), deprec) { deprec = it } }
                item {
                    ConfigSectionHeader(
                        title = stringResource(R.string.cost_cfg_section_quote_only),
                        subtitle = stringResource(R.string.cost_cfg_section_quote_only_desc)
                    )
                }
                item { ConfigRow(stringResource(R.string.cost_cfg_markup), markup) { markup = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_service), service) { service = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_shipping), shipping) { shipping = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_min), minOrder) { minOrder = it } }
                item { ConfigRow(stringResource(R.string.cost_cfg_rounding), rounding) { rounding = it } }
                item { ConfigSectionTitle(stringResource(R.string.cost_cfg_other_fees)) }
                items(otherFees) { fee ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fee.name} ${Money.format(fee.amountCents)}/${feeUnitLabel(fee.unit)} · ${feeTargetLabel(fee.target)}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { otherFees.remove(fee) }, modifier = Modifier.size(36.dp)) {
                                Icon(AppIcons.DeleteOutline, contentDescription = null)
                            }
                        }
                    }
                }
                item { AddFeeRow(onAdd = { otherFees.add(it) }) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    config.copy(
                        electricityPerKwhCents = Money.parse(electricity) ?: config.electricityPerKwhCents,
                        serviceFeeCents = Money.parse(service) ?: config.serviceFeeCents,
                        baseShippingCents = Money.parse(shipping) ?: config.baseShippingCents,
                        multicolorWasteFactor = (waste.toDoubleOrNull() ?: (config.multicolorWasteFactor * 100)) / 100.0,
                        multicolorSurchargeCents = Money.parse(surcharge) ?: config.multicolorSurchargeCents,
                        quoteMarkup = markup.toDoubleOrNull() ?: config.quoteMarkup,
                        minOrderCents = Money.parse(minOrder) ?: config.minOrderCents,
                        roundingCents = Money.parse(rounding) ?: config.roundingCents,
                        defaultPricePerGCents = PerGramPrice.parse(defaultPrice) ?: config.defaultPricePerGCents,
                        defaultPowerWatts = power.toIntOrNull() ?: config.defaultPowerWatts,
                        defaultDepreciationPerHourCents = Money.parse(deprec) ?: config.defaultDepreciationPerHourCents,
                        otherFees = otherFees.toList()
                    )
                )
            }) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun ConfigSectionHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfigSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
    )
}

@Composable
private fun ConfigRow(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun LabeledField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun AddFeeRow(onAdd: (OtherFee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(FeeUnit.ORDER) }
    var target by remember { mutableStateOf(FeeTarget.QUOTE) }
    var unitMenu by remember { mutableStateOf(false) }
    var targetMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 第一行:名称 + 金额
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactField(name, { name = it }, Modifier.weight(1f), placeholder = stringResource(R.string.cost_fee_name), number = false)
            CompactField(amount, { amount = it }, Modifier.width(80.dp), placeholder = Money.symbol())
        }
        // 第二行:单位 + 类别 + 添加
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box {
                SmallButton("${feeUnitLabel(unit)} ▾", { unitMenu = true })
                DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                    FeeUnit.values().forEach { u -> DropdownMenuItem(text = { Text(feeUnitLabel(u)) }, onClick = { unit = u; unitMenu = false }) }
                }
            }
            Box {
                SmallButton("${feeTargetLabel(target)} ▾", { targetMenu = true })
                DropdownMenu(expanded = targetMenu, onDismissRequest = { targetMenu = false }) {
                    FeeTarget.values().forEach { t -> DropdownMenuItem(text = { Text(feeTargetLabel(t)) }, onClick = { target = t; targetMenu = false }) }
                }
            }
            Spacer(Modifier.weight(1f))
            SmallButton(stringResource(R.string.cost_fee_add), {
                val cents = Money.parse(amount)
                if (name.isNotBlank() && cents != null) { onAdd(OtherFee(name.trim(), unit, cents, target)); name = ""; amount = "" }
            }, primary = true)
        }
    }
}

@Composable
private fun PricesDialog(prices: List<MaterialPrice>, onSet: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, prices) {
        if (query.isBlank()) prices else prices.filter { it.filaType.contains(query, true) || it.filaId.contains(query, true) || it.baseType.contains(query, true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.cost_prices_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.cost_prices_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.cost_prices_search)) },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.filaId }) { p -> PriceRow(p, onSet) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
private fun PriceRow(p: MaterialPrice, onSet: (String, Long) -> Unit) {
    var text by remember(p.filaId, p.pricePerGCents) { mutableStateOf(PerGramPrice.toPlain(p.pricePerGCents)) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = p.filaType.ifBlank { p.filaId },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            Text("${p.baseType} · ${p.filaId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
            CompactField(text, { text = it }, Modifier.width(82.dp).height(52.dp))
            Spacer(Modifier.width(6.dp))
            SmallButton(
                text = stringResource(R.string.cost_price_save),
                onClick = { PerGramPrice.parse(text)?.let { onSet(p.filaId, it) } },
                primary = true
            )
            }
        }
    }

@Composable
private fun CompactField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", number: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        textStyle = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}

@Composable
private fun feeUnitLabel(unit: FeeUnit): String = when (unit) {
    FeeUnit.ORDER -> stringResource(R.string.cost_fee_unit_order)
    FeeUnit.PLATE -> stringResource(R.string.cost_fee_unit_plate)
    FeeUnit.SECOND -> stringResource(R.string.cost_fee_unit_second)
}

@Composable
private fun feeTargetLabel(target: FeeTarget): String = when (target) {
    FeeTarget.COST -> stringResource(R.string.cost_fee_target_cost)
    FeeTarget.QUOTE -> stringResource(R.string.cost_fee_target_quote)
    FeeTarget.BOTH -> stringResource(R.string.cost_fee_target_both)
}

@Composable
private fun BreakdownLine(label: String, cents: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(Money.format(cents), style = MaterialTheme.typography.bodySmall)
    }
}

private fun quoteOtherFeeCents(fee: OtherFee, input: QuoteInput): Long = when (fee.unit) {
    FeeUnit.ORDER -> fee.amountCents
    FeeUnit.PLATE -> fee.amountCents * input.plateCount.coerceAtLeast(1)
    FeeUnit.SECOND -> fee.amountCents * input.estTimeSeconds
}

@Composable
private fun profitColor(cents: Long): Color = if (cents >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

private fun materialsSummary(
    materials: List<BambuCloudTaskMaterial>,
    fallbackWeight: Double,
    materialTypesByFilaId: Map<String, String>
): String {
    if (materials.isEmpty()) return "%.1fg".format(fallbackWeight)
    return materials.joinToString(" · ") { m ->
        val t = displayMaterialType(m, materialTypesByFilaId)
        "$t ${"%.1f".format(m.weightGrams)}g"
    }
}

@Composable
private fun CostSearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.cost_search), style = MaterialTheme.typography.bodySmall) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onChange("") }) { Icon(AppIcons.Close, contentDescription = null) }
        }) else null,
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

/** 按任务名 / 耗材类型 / 订单名匹配。 */
private fun matchesQuery(ov: OrderView, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    if (ov.name.lowercase().contains(q)) return true
    return ov.tasks.any { t ->
        t.title.lowercase().contains(q) ||
            t.materials.any { m -> m.filamentType.lowercase().contains(q) || m.filamentId.lowercase().contains(q) }
    }
}

/** 合并订单主条目:跨所有任务按耗材类型合计克重。 */
private fun aggregateMaterials(tasks: List<PrintTaskRow>, materialTypesByFilaId: Map<String, String>): String {
    val all = tasks.flatMap { it.materials }
    if (all.isEmpty()) return "%.1fg".format(tasks.sumOf { it.weightGrams })
    val byType = LinkedHashMap<String, Double>()
    all.forEach { m ->
        val t = displayMaterialType(m, materialTypesByFilaId)
        byType[t] = (byType[t] ?: 0.0) + m.weightGrams
    }
    return byType.entries.joinToString(" · ") { "${it.key} ${"%.1f".format(it.value)}g" }
}

private fun displayMaterialType(
    material: BambuCloudTaskMaterial,
    materialTypesByFilaId: Map<String, String>
): String =
    materialTypesByFilaId[normalizeFilaId(material.filamentId)]
        ?.ifBlank { null }
        ?: material.filamentType.ifBlank { material.filamentId }.ifBlank { "?" }

private fun normalizeFilaId(value: String): String = value.trim().uppercase(Locale.US)

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
private fun formatDate(millis: Long): String = if (millis > 0) dateFormat.format(Date(millis)) else "--"

private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
private fun formatDateTime(millis: Long): String = if (millis > 0) dateTimeFormat.format(Date(millis)) else "--"
