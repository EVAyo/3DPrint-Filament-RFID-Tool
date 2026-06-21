package com.m0h31h31.bamburfidreader.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.cloud.BambuCloudTaskMaterial
import com.m0h31h31.bamburfidreader.cost.CostCalculator
import com.m0h31h31.bamburfidreader.cost.CostConfig
import com.m0h31h31.bamburfidreader.cost.CostController
import com.m0h31h31.bamburfidreader.cost.FeeUnit
import com.m0h31h31.bamburfidreader.cost.MaterialPrice
import com.m0h31h31.bamburfidreader.cost.Money
import com.m0h31h31.bamburfidreader.cost.OrderView
import com.m0h31h31.bamburfidreader.cost.OtherFee
import com.m0h31h31.bamburfidreader.cost.QuoteInput
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
    val includeFailed by controller.includeFailedState
    val syncing by controller.syncingState
    val statusMessage by controller.statusMessageState
    val session by cloud.sessionState

    val orderViews = remember(tasks, orders) { buildOrderViews(tasks, orders) }
    val stats = remember(orderViews, includeFailed) {
        CostCalculator.computeStats(orderViews, includeFailed)
    }

    val selected = remember { mutableStateListOf<Long>() }
    var showQuote by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }
    var showPrices by remember { mutableStateOf(false) }
    var chargeTarget by remember { mutableStateOf<OrderView?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .neuBackground()
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CostStatsCard(
                orders = stats.orderCount,
                costCents = stats.totalCostCents,
                profitCents = stats.totalProfitCents,
                marginPercent = stats.marginPercent,
                avgCents = stats.avgOrderValueCents,
                includeFailed = includeFailed,
                onToggleIncludeFailed = { controller.toggleIncludeFailed() }
            )
        }
        item {
            CostToolbar(
                syncing = syncing,
                statusMessage = statusMessage,
                onSync = { controller.sync() },
                onQuote = { showQuote = true },
                onConfig = { showConfig = true },
                onPrices = { showPrices = true }
            )
        }
        if (session == null) {
            item { CostLoginHint() }
        }
        if (selected.isNotEmpty()) {
            item {
                CostMergeBar(
                    count = selected.size,
                    onMerge = {
                        controller.aggregateIntoOrder(
                            selected.toList(),
                            context.getString(R.string.cost_default_order_name)
                        )
                        selected.clear()
                    },
                    onCancel = { selected.clear() }
                )
            }
        }
        if (orderViews.isEmpty()) {
            item { CostEmpty() }
        }
        items(orderViews, key = { it.orderId ?: -it.tasks.first().id }) { ov ->
            OrderCard(
                ov = ov,
                config = config,
                selectable = ov.orderId == null,
                selected = ov.orderId == null && selected.contains(ov.tasks.first().id),
                onToggleSelect = {
                    val id = ov.tasks.first().id
                    if (selected.contains(id)) selected.remove(id) else selected.add(id)
                },
                onSetCharge = { chargeTarget = ov },
                onDissolve = { ov.orderId?.let { controller.dissolveOrder(it) } }
            )
        }
    }

    if (showQuote) {
        QuoteDialog(config = config, prices = prices, onDismiss = { showQuote = false })
    }
    if (showConfig) {
        ConfigDialog(
            config = config,
            onSave = { controller.saveConfig(it); showConfig = false },
            onDismiss = { showConfig = false }
        )
    }
    if (showPrices) {
        PricesDialog(
            prices = prices,
            onSet = { filaId, cents -> controller.setMaterialPrice(filaId, cents) },
            onDismiss = { showPrices = false }
        )
    }
    chargeTarget?.let { ov ->
        ChargeDialog(
            ov = ov,
            onSave = { cents -> controller.setActualCharge(ov, cents); chargeTarget = null },
            onDismiss = { chargeTarget = null }
        )
    }
}

@Composable
private fun CostStatsCard(
    orders: Int,
    costCents: Long,
    profitCents: Long,
    marginPercent: Double,
    avgCents: Long,
    includeFailed: Boolean,
    onToggleIncludeFailed: () -> Unit
) {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(stringResource(R.string.cost_stat_orders), orders.toString(), Modifier.weight(1f))
                StatItem(stringResource(R.string.cost_stat_cost), Money.format(costCents), Modifier.weight(1f))
                StatItem(
                    stringResource(R.string.cost_stat_profit),
                    Money.format(profitCents),
                    Modifier.weight(1f),
                    valueColor = profitColor(profitCents)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatItem(
                    stringResource(R.string.cost_stat_margin),
                    String.format(Locale.getDefault(), "%.1f%%", marginPercent),
                    Modifier.weight(1f)
                )
                StatItem(stringResource(R.string.cost_stat_avg), Money.format(avgCents), Modifier.weight(1f))
                Row(
                    modifier = Modifier.weight(1f).clickable(onClick = onToggleIncludeFailed),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeFailed, onCheckedChange = { onToggleIncludeFailed() })
                    Text(
                        stringResource(R.string.cost_include_failed),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CostToolbar(
    syncing: Boolean,
    statusMessage: String,
    onSync: () -> Unit,
    onQuote: () -> Unit,
    onConfig: () -> Unit,
    onPrices: () -> Unit
) {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuButton(
                    text = if (syncing) stringResource(R.string.cost_syncing) else stringResource(R.string.cost_sync),
                    onClick = onSync,
                    enabled = !syncing,
                    modifier = Modifier.weight(1f)
                )
                NeuButton(text = stringResource(R.string.cost_quote), onClick = onQuote, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuButton(text = stringResource(R.string.cost_config), onClick = onConfig, modifier = Modifier.weight(1f))
                NeuButton(text = stringResource(R.string.cost_prices), onClick = onPrices, modifier = Modifier.weight(1f))
            }
            if (statusMessage.isNotBlank()) {
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CostLoginHint() {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.cost_login_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CostEmpty() {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.cost_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CostMergeBar(count: Int, onMerge: () -> Unit, onCancel: () -> Unit) {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.cost_merge_selected, count),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            Spacer(Modifier.width(4.dp))
            NeuButton(text = stringResource(R.string.cost_merge), onClick = onMerge)
        }
    }
}

@Composable
private fun OrderCard(
    ov: OrderView,
    config: CostConfig,
    selectable: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onSetCharge: () -> Unit,
    onDissolve: () -> Unit
) {
    var expanded by remember(ov) { mutableStateOf(false) }
    val isOrder = ov.orderId != null
    val first = ov.tasks.first()
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectable) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
                }
                CoverImage(first.coverPath, 52.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isOrder) "${ov.name} · ${stringResource(R.string.cost_order_items, ov.tasks.size)}" else first.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (ov.anyFailed) {
                            FailedBadge()
                        }
                    }
                    Text(
                        text = materialsSummary(first.materials, first.weightGrams),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDate(first.startTimeMillis)} · ${formatDuration(first.costTimeSeconds)} · ${stringResource(R.string.cost_cost_label)} ${Money.format(ov.costCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ov.actualChargeCents > 0) {
                    Text(
                        text = "${stringResource(R.string.cost_profit_label)} ${Money.format(ov.profitCents)} (${String.format(Locale.getDefault(), "%.0f%%", ov.marginPercent)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = profitColor(ov.profitCents),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (isOrder) {
                    TextButton(onClick = onDissolve) { Text(stringResource(R.string.cost_dissolve)) }
                    if (ov.tasks.size > 1) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) stringResource(R.string.cost_collapse) else stringResource(R.string.cost_expand))
                        }
                    }
                }
                NeuButton(text = stringResource(R.string.cost_set_charge), onClick = onSetCharge)
            }
            if (expanded && ov.tasks.size > 1) {
                ov.tasks.forEach { t ->
                    Text(
                        text = "· ${t.title} — ${materialsSummary(t.materials, t.weightGrams)} — ${Money.format(t.computedCostCents)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FailedBadge() {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            stringResource(R.string.cost_failed_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun CoverImage(path: String, size: Dp) {
    val image = remember(path) {
        if (path.isNotBlank() && File(path).exists()) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        } else null
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("3D", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.cost_actual_charge)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${stringResource(R.string.cost_profit_label)}: ${Money.format(profit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = profitColor(profit)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(Money.parse(text) ?: 0L) }) { Text(stringResource(R.string.action_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun QuoteDialog(config: CostConfig, prices: List<MaterialPrice>, onDismiss: () -> Unit) {
    var weight by remember { mutableStateOf("") }
    var timeMin by remember { mutableStateOf("") }
    var colors by remember { mutableStateOf("1") }
    var plates by remember { mutableStateOf("1") }
    var selectedPrice by remember { mutableStateOf(prices.firstOrNull()) }
    var pricePerG by remember { mutableStateOf(selectedPrice?.let { Money.toPlain(it.pricePerGCents) } ?: Money.toPlain(config.defaultPricePerGCents)) }

    val input = QuoteInput(
        weightGrams = weight.toDoubleOrNull() ?: 0.0,
        pricePerGCents = Money.parse(pricePerG) ?: config.defaultPricePerGCents,
        estTimeSeconds = ((timeMin.toDoubleOrNull() ?: 0.0) * 60).toInt(),
        deviceModel = "",
        colorCount = colors.toIntOrNull() ?: 1,
        plateCount = plates.toIntOrNull() ?: 1
    )
    val q = CostCalculator.computeQuote(input, config)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cost_quote_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialPicker(
                    prices = prices,
                    selected = selectedPrice,
                    onSelect = {
                        selectedPrice = it
                        pricePerG = Money.toPlain(it.pricePerGCents)
                    }
                )
                NumberField(weight, { weight = it }, stringResource(R.string.cost_quote_weight))
                NumberField(pricePerG, { pricePerG = it }, stringResource(R.string.cost_price_per_g))
                NumberField(timeMin, { timeMin = it }, stringResource(R.string.cost_quote_time_min))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(colors, { colors = it }, stringResource(R.string.cost_quote_colors), Modifier.weight(1f))
                    NumberField(plates, { plates = it }, stringResource(R.string.cost_quote_plates), Modifier.weight(1f))
                }
                Text(
                    "${stringResource(R.string.cost_quote_breakdown)}: ${Money.format(q.productionCents)} ×${config.quoteMarkup} +${Money.format(q.serviceCents + q.shippingCents + q.otherCents)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${stringResource(R.string.cost_quote_total)}: ${Money.format(q.totalCents)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
private fun MaterialPicker(prices: List<MaterialPrice>, selected: MaterialPrice?, onSelect: (MaterialPrice) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(selected?.let { "${it.filaType} (${it.filaId})" } ?: stringResource(R.string.cost_quote_material))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(R.string.cost_prices_search)) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            val filtered = remember(query, prices) {
                if (query.isBlank()) prices.take(50)
                else prices.filter { it.filaType.contains(query, true) || it.filaId.contains(query, true) }.take(50)
            }
            filtered.forEach { p ->
                DropdownMenuItem(
                    text = { Text("${p.filaType} (${p.filaId}) ${Money.format(p.pricePerGCents)}/g") },
                    onClick = { onSelect(p); expanded = false }
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
    var defaultPrice by remember { mutableStateOf(Money.toPlain(config.defaultPricePerGCents)) }
    var power by remember { mutableStateOf(config.defaultPowerWatts.toString()) }
    var deprec by remember { mutableStateOf(Money.toPlain(config.defaultDepreciationPerHourCents)) }
    val otherFees = remember { config.otherFees.toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cost_config_title)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NumberField(electricity, { electricity = it }, stringResource(R.string.cost_cfg_electricity)) }
                item { NumberField(defaultPrice, { defaultPrice = it }, stringResource(R.string.cost_cfg_default_price)) }
                item { NumberField(service, { service = it }, stringResource(R.string.cost_cfg_service)) }
                item { NumberField(shipping, { shipping = it }, stringResource(R.string.cost_cfg_shipping)) }
                item { NumberField(waste, { waste = it }, stringResource(R.string.cost_cfg_waste)) }
                item { NumberField(surcharge, { surcharge = it }, stringResource(R.string.cost_cfg_surcharge)) }
                item { NumberField(markup, { markup = it }, stringResource(R.string.cost_cfg_markup)) }
                item { NumberField(minOrder, { minOrder = it }, stringResource(R.string.cost_cfg_min)) }
                item { NumberField(rounding, { rounding = it }, stringResource(R.string.cost_cfg_rounding)) }
                item { NumberField(power, { power = it }, stringResource(R.string.cost_cfg_power)) }
                item { NumberField(deprec, { deprec = it }, stringResource(R.string.cost_cfg_deprec)) }
                item {
                    Text(stringResource(R.string.cost_cfg_other_fees), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                items(otherFees) { fee ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${fee.name} ${Money.format(fee.amountCents)}/${feeUnitLabel(fee.unit)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { otherFees.remove(fee) }) {
                            Icon(AppIcons.DeleteOutline, contentDescription = null)
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
                        defaultPricePerGCents = Money.parse(defaultPrice) ?: config.defaultPricePerGCents,
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
private fun AddFeeRow(onAdd: (OtherFee) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(FeeUnit.ORDER) }
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.cost_fee_name)) }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = amount, onValueChange = { amount = it }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), label = { Text("¥") }, modifier = Modifier.width(70.dp))
        Box {
            TextButton(onClick = { menu = true }) { Text(feeUnitLabel(unit)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                FeeUnit.values().forEach { u ->
                    DropdownMenuItem(text = { Text(feeUnitLabel(u)) }, onClick = { unit = u; menu = false })
                }
            }
        }
        TextButton(onClick = {
            val cents = Money.parse(amount)
            if (name.isNotBlank() && cents != null) {
                onAdd(OtherFee(name.trim(), unit, cents))
                name = ""; amount = ""
            }
        }) {
            Text(stringResource(R.string.cost_fee_add))
        }
    }
}

@Composable
private fun PricesDialog(prices: List<MaterialPrice>, onSet: (String, Long) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, prices) {
        if (query.isBlank()) prices
        else prices.filter { it.filaType.contains(query, true) || it.filaId.contains(query, true) || it.baseType.contains(query, true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cost_prices_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.cost_prices_search)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.filaId }) { p ->
                        PriceRow(p, onSet)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
private fun PriceRow(p: MaterialPrice, onSet: (String, Long) -> Unit) {
    var text by remember(p.filaId, p.pricePerGCents) { mutableStateOf(Money.toPlain(p.pricePerGCents)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(p.filaType.ifBlank { p.filaId }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${p.baseType} · ${p.filaId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(96.dp)
        )
        TextButton(onClick = { Money.parse(text)?.let { onSet(p.filaId, it) } }) {
            Text(stringResource(R.string.action_confirm))
        }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun feeUnitLabel(unit: FeeUnit): String = when (unit) {
    FeeUnit.ORDER -> stringResource(R.string.cost_fee_unit_order)
    FeeUnit.PLATE -> stringResource(R.string.cost_fee_unit_plate)
    FeeUnit.SECOND -> stringResource(R.string.cost_fee_unit_second)
}

@Composable
private fun profitColor(cents: Long): Color =
    if (cents >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

private fun materialsSummary(materials: List<BambuCloudTaskMaterial>, fallbackWeight: Double): String {
    if (materials.isEmpty()) return "${"%.1f".format(fallbackWeight)}g"
    val types = materials.map { it.filamentType.ifBlank { it.filamentId } }.filter { it.isNotBlank() }.distinct().joinToString("+")
    val weight = materials.sumOf { it.weightGrams }.takeIf { it > 0 } ?: fallbackWeight
    return "$types ${"%.1f".format(weight)}g"
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
private fun formatDate(millis: Long): String = if (millis > 0) dateFormat.format(Date(millis)) else "--"
