package com.m0h31h31.bamburfidreader.cost

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.m0h31h31.bamburfidreader.cloud.BambuCloudTask
import com.m0h31h31.bamburfidreader.cloud.BambuCloudTaskResult
import com.m0h31h31.bamburfidreader.data.FilamentDbHelper
import com.m0h31h31.bamburfidreader.logging.logDebug
import com.m0h31h31.bamburfidreader.ui.screens.BambuCloudController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 把任务按订单聚合;order_id 为空的任务各自成一单。 */
fun buildOrderViews(tasks: List<PrintTaskRow>, orders: List<PrintOrder>): List<OrderView> {
    val byOrder = tasks.filter { it.orderId != null }.groupBy { it.orderId!! }
    val orderViews = orders.mapNotNull { order ->
        val members = byOrder[order.id] ?: return@mapNotNull null
        OrderView(order.id, order.name.ifBlank { "订单 #${order.id}" }, members, order.actualChargeCents)
    }
    val standalone = tasks.filter { it.orderId == null }
        .map { OrderView(null, it.title, listOf(it), 0L) }
    // 订单与单任务统一按时间(订单取其最新任务的开始时间)倒序排,合并订单不再单独置顶
    return (orderViews + standalone)
        .sortedByDescending { ov -> ov.tasks.maxOfOrNull { it.startTimeMillis } ?: 0L }
}

fun isFirstCostSync(lastSyncAt: String?): Boolean = lastSyncAt.isNullOrBlank()

/**
 * 费用模块进程级单例:持有配置/耗材价/任务/订单状态,负责从云端同步打印历史并落库。
 * 复用 [BambuCloudController] 的 repository 拉取数据。
 */
class CostController private constructor(appContext: Context) {

    private val app = appContext
    private val dao = CostDao(FilamentDbHelper(appContext))
    private val cloud = BambuCloudController.get(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val configState = mutableStateOf(CostConfig.DEFAULT)
    val pricesState = mutableStateOf<List<MaterialPrice>>(emptyList())
    val tasksState = mutableStateOf<List<PrintTaskRow>>(emptyList())
    val ordersState = mutableStateOf<List<PrintOrder>>(emptyList())
    val showHiddenState = mutableStateOf(false)
    val syncingState = mutableStateOf(false)
    val statusMessageState = mutableStateOf("")

    private var priceMap: Map<String, Long> = emptyMap()
    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        scope.launch {
            withContext(Dispatchers.IO) {
                MaterialPriceSeeder.seedIfEmpty(app, dao)
            }
            reloadFromDb()
        }
    }

    fun isLoggedIn(): Boolean = cloud.repository.loadSession() != null

    private suspend fun reloadFromDb() {
        val (config, prices, tasks, orders) = withContext(Dispatchers.IO) {
            val cfg = dao.loadConfig()
            val pr = dao.loadMaterialPrices()
            priceMap = pr.associate { it.filaId to it.pricePerGCents }
            Quad(cfg, pr, dao.loadTasks(), dao.loadOrders())
        }
        configState.value = config
        pricesState.value = prices
        tasksState.value = tasks
        ordersState.value = orders
    }

    /** 列表视图:showHidden 时只显示隐藏任务,否则只显示未隐藏任务。 */
    fun orderViews(): List<OrderView> {
        val src = tasksState.value.filter { if (showHiddenState.value) it.hidden else !it.hidden }
        return buildOrderViews(src, ordersState.value)
    }

    /** 统计始终排除隐藏任务。 */
    fun stats(): CostStats =
        CostCalculator.computeStats(buildOrderViews(tasksState.value.filter { !it.hidden }, ordersState.value), includeFailed = false)

    fun hiddenCount(): Int = tasksState.value.count { it.hidden }

    fun toggleShowHidden() {
        showHiddenState.value = !showHiddenState.value
    }

    fun restoreTasks(taskIds: List<Long>) {
        if (taskIds.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) { dao.setTasksHidden(taskIds, false) }
            reloadFromDb()
        }
    }

    private fun priceOf(filaId: String): Long =
        priceMap[filaId] ?: configState.value.defaultPricePerGCents

    /** 公开:某耗材每克价(分),用于明细展示。 */
    fun priceFor(filaId: String): Long = priceOf(filaId)

    /** 公开:单条任务的成本拆解,用于明细弹窗。 */
    fun breakdownOf(task: PrintTaskRow): CostBreakdown = CostCalculator.computeTaskCost(
        materials = task.materials,
        fallbackWeightGrams = task.weightGrams,
        costTimeSeconds = task.costTimeSeconds,
        deviceModel = task.deviceModel,
        repetitions = task.repetitions,
        config = configState.value,
        priceOf = ::priceOf
    )

    private fun costOf(task: BambuCloudTask): Long {
        return CostCalculator.computeTaskCost(
            materials = task.materials,
            fallbackWeightGrams = task.weightGrams,
            costTimeSeconds = task.costTimeSeconds,
            deviceModel = task.deviceModel,
            repetitions = task.repetitions,
            config = configState.value,
            priceOf = ::priceOf
        ).totalCents
    }

    private fun costOf(task: PrintTaskRow): Long {
        return CostCalculator.computeTaskCost(
            materials = task.materials,
            fallbackWeightGrams = task.weightGrams,
            costTimeSeconds = task.costTimeSeconds,
            deviceModel = task.deviceModel,
            repetitions = task.repetitions,
            config = configState.value,
            priceOf = ::priceOf
        ).totalCents
    }

    /** 从云端分页同步全部打印历史。 */
    fun sync() {
        if (syncingState.value) return
        if (!isLoggedIn()) {
            statusMessageState.value = app.getString(com.m0h31h31.bamburfidreader.R.string.cost_need_login)
            return
        }
        syncingState.value = true
        statusMessageState.value = app.getString(
            if (isFirstCostSync(dao.getMeta(CostDao.KEY_LAST_SYNC))) {
                com.m0h31h31.bamburfidreader.R.string.cost_syncing_first_time
            } else {
                com.m0h31h31.bamburfidreader.R.string.cost_syncing
            }
        )
        scope.launch {
            val result = runCatching { syncAllPages() }
            result.onSuccess { count ->
                statusMessageState.value = app.getString(com.m0h31h31.bamburfidreader.R.string.cost_sync_done, count)
            }.onFailure { e ->
                logDebug("Cost sync failed: ${e.message}")
                statusMessageState.value = app.getString(
                    com.m0h31h31.bamburfidreader.R.string.cost_sync_failed,
                    e.message.orEmpty()
                )
            }
            reloadFromDb()
            syncingState.value = false
        }
    }

    private suspend fun syncAllPages(): Int = withContext(Dispatchers.IO) {
        val limit = 50
        var offset = 0
        var total = Int.MAX_VALUE
        var synced = 0
        val coverDir = File(app.cacheDir, "cost_covers").apply { mkdirs() }
        while (offset < total) {
            val page = when (val r = cloud.repository.fetchTasks(offset = offset, limit = limit, status = 0)) {
                is BambuCloudTaskResult.Success -> r.page
                is BambuCloudTaskResult.Failure -> throw IllegalStateException(r.message)
            }
            total = page.total
            if (page.tasks.isEmpty()) break
            for (task in page.tasks) {
                val existing = File(coverDir, "${task.id}.img")
                if (!existing.exists() && task.coverUrl.isNotBlank()) {
                    runCatching { downloadTo(task.coverUrl, existing) }
                }
                val coverPath = if (existing.exists()) existing.absolutePath else ""
                dao.upsertTask(
                    PrintTaskRow(
                        id = task.id,
                        title = task.title,
                        coverPath = coverPath,
                        deviceModel = task.deviceModel,
                        deviceName = task.deviceName,
                        weightGrams = task.weightGrams,
                        costTimeSeconds = task.costTimeSeconds,
                        startTimeMillis = task.startTimeMillis,
                        status = task.status,
                        failedType = task.failedType,
                        repetitions = task.repetitions,
                        materials = task.materials,
                        computedCostCents = costOf(task),
                        orderId = null // upsertTask 保留库内已有 order_id
                    )
                )
                synced++
            }
            offset += limit
        }
        dao.setMeta(CostDao.KEY_LAST_SYNC, System.currentTimeMillis().toString())
        synced
    }

    private fun downloadTo(urlStr: String, dest: File) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** 配置或耗材价变更后,重算所有任务成本。 */
    private fun recomputeAllCosts() {
        scope.launch {
            withContext(Dispatchers.IO) {
                dao.loadTasks().forEach { t -> dao.updateTaskCost(t.id, costOf(t)) }
            }
            reloadFromDb()
        }
    }

    fun saveConfig(config: CostConfig) {
        scope.launch {
            withContext(Dispatchers.IO) { dao.saveConfig(config) }
            configState.value = config
            recomputeAllCosts()
        }
    }

    fun setMaterialPrice(filaId: String, cents: Long) {
        scope.launch {
            withContext(Dispatchers.IO) { dao.setMaterialPrice(filaId, cents) }
            reloadFromDb()
            recomputeAllCosts()
        }
    }

    /** 隐藏选中的任务(从列表与统计中移除,可重新同步不会恢复)。 */
    fun hideTasks(taskIds: List<Long>) {
        if (taskIds.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) { dao.setTasksHidden(taskIds, true) }
            reloadFromDb()
        }
    }

    /** 把多条任务合并为一单。 */
    fun aggregateIntoOrder(taskIds: List<Long>, name: String) {
        if (taskIds.size < 2) return
        scope.launch {
            withContext(Dispatchers.IO) { dao.createOrderWithTasks(name, taskIds) }
            reloadFromDb()
        }
    }

    /** 拖拽合并:把源任务合并到目标条目中。目标是订单则并入该订单;目标是单任务则两者新建一单。 */
    fun mergeIntoTarget(sourceTaskIds: List<Long>, target: OrderView, defaultName: String) {
        if (sourceTaskIds.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) {
                val targetOrderId = target.orderId
                if (targetOrderId != null) {
                    dao.setTasksOrder(sourceTaskIds, targetOrderId)
                } else {
                    val targetTaskId = target.tasks.firstOrNull()?.id ?: return@withContext
                    if (sourceTaskIds.contains(targetTaskId)) return@withContext
                    dao.createOrderWithTasks(defaultName, listOf(targetTaskId) + sourceTaskIds)
                }
            }
            reloadFromDb()
        }
    }

    /** 给一笔订单或单条任务设置实际收费;单任务会即时建一个 1 任务订单。 */
    fun setActualCharge(orderView: OrderView, cents: Long) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val orderId = orderView.orderId
                if (orderId != null) {
                    dao.setOrderCharge(orderId, cents)
                } else {
                    val taskId = orderView.tasks.firstOrNull()?.id ?: return@withContext
                    val newId = dao.createOrderWithTasks(orderView.name, listOf(taskId))
                    if (newId > 0) dao.setOrderCharge(newId, cents)
                }
            }
            reloadFromDb()
        }
    }

    /** 从合并订单中移除单条任务;若移除后订单不足 2 条则解散整单。 */
    fun detachTaskFromOrder(order: OrderView, taskId: Long) {
        val orderId = order.orderId ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                if (order.tasks.size <= 2) {
                    dao.deleteOrder(orderId)
                } else {
                    dao.setTasksOrder(listOf(taskId), null)
                }
            }
            reloadFromDb()
        }
    }

    fun dissolveOrder(orderId: Long) {
        scope.launch {
            withContext(Dispatchers.IO) { dao.deleteOrder(orderId) }
            reloadFromDb()
        }
    }

    private data class Quad<A, B, C, D>(
        val component1: A,
        val component2: B,
        val component3: C,
        val component4: D
    )

    companion object {
        @Volatile
        private var instance: CostController? = null

        fun get(context: Context): CostController =
            instance ?: synchronized(this) {
                instance ?: CostController(context.applicationContext).also { instance = it }
            }
    }
}
