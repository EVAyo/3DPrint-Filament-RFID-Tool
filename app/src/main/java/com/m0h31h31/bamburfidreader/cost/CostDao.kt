package com.m0h31h31.bamburfidreader.cost

import android.content.ContentValues
import com.m0h31h31.bamburfidreader.cloud.BambuCloudTaskMaterial
import com.m0h31h31.bamburfidreader.data.COST_CONFIG_TABLE
import com.m0h31h31.bamburfidreader.data.FilamentDbHelper
import com.m0h31h31.bamburfidreader.data.MATERIAL_PRICE_TABLE
import com.m0h31h31.bamburfidreader.data.PRINT_ORDER_TABLE
import com.m0h31h31.bamburfidreader.data.PRINT_TASK_TABLE
import org.json.JSONArray
import org.json.JSONObject

/** 费用模块的本地读写。金额「分」。 */
class CostDao(private val dbHelper: FilamentDbHelper) {

    // ---- 配置 ----
    fun loadConfig(): CostConfig {
        dbHelper.readableDatabase.query(
            COST_CONFIG_TABLE, arrayOf("value"), "key=?", arrayOf(KEY_CONFIG), null, null, null
        ).use { c ->
            return if (c.moveToFirst()) CostConfigCodec.fromJson(c.getString(0)) else CostConfig.DEFAULT
        }
    }

    fun saveConfig(config: CostConfig) {
        val cv = ContentValues().apply {
            put("key", KEY_CONFIG)
            put("value", CostConfigCodec.toJson(config))
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            COST_CONFIG_TABLE, null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getMeta(key: String): String? {
        dbHelper.readableDatabase.query(
            COST_CONFIG_TABLE, arrayOf("value"), "key=?", arrayOf(key), null, null, null
        ).use { c -> return if (c.moveToFirst()) c.getString(0) else null }
    }

    fun setMeta(key: String, value: String) {
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            COST_CONFIG_TABLE, null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // ---- 耗材价 ----
    fun loadMaterialPrices(): List<MaterialPrice> {
        val out = ArrayList<MaterialPrice>()
        dbHelper.readableDatabase.query(
            MATERIAL_PRICE_TABLE,
            arrayOf("fila_id", "fila_type", "base_type", "price_per_g_cents"),
            null, null, null, null, "base_type ASC, fila_type ASC"
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    MaterialPrice(
                        filaId = c.getString(0).orEmpty(),
                        filaType = c.getString(1).orEmpty(),
                        baseType = c.getString(2).orEmpty(),
                        pricePerGCents = c.getLong(3)
                    )
                )
            }
        }
        return out
    }

    fun priceMap(): Map<String, Long> =
        loadMaterialPrices().associate { it.filaId to it.pricePerGCents }

    /** 仅当不存在时插入(幂等播种,不覆盖用户已设价格)。 */
    fun insertMaterialPriceIfAbsent(price: MaterialPrice) {
        val cv = ContentValues().apply {
            put("fila_id", price.filaId)
            put("fila_type", price.filaType)
            put("base_type", price.baseType)
            put("price_per_g_cents", price.pricePerGCents)
            put("updated_at", System.currentTimeMillis())
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            MATERIAL_PRICE_TABLE, null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun setMaterialPrice(filaId: String, cents: Long) {
        val cv = ContentValues().apply {
            put("price_per_g_cents", cents)
            put("updated_at", System.currentTimeMillis())
        }
        dbHelper.writableDatabase.update(MATERIAL_PRICE_TABLE, cv, "fila_id=?", arrayOf(filaId))
    }

    fun materialPriceCount(): Long {
        dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $MATERIAL_PRICE_TABLE", null).use { c ->
            return if (c.moveToFirst()) c.getLong(0) else 0L
        }
    }

    // ---- 任务 ----
    fun upsertTask(task: PrintTaskRow) {
        val cv = ContentValues().apply {
            put("id", task.id)
            put("title", task.title)
            put("cover_path", task.coverPath)
            put("device_model", task.deviceModel)
            put("device_name", task.deviceName)
            put("weight", task.weightGrams)
            put("cost_time", task.costTimeSeconds)
            put("start_time", task.startTimeMillis)
            put("status", task.status)
            put("failed_type", task.failedType)
            put("repetitions", task.repetitions)
            put("materials_json", materialsToJson(task.materials))
            put("computed_cost_cents", task.computedCostCents)
            task.orderId?.let { put("order_id", it) }
            put("synced_at", System.currentTimeMillis())
        }
        // 保留已有 order_id / cover_path:用 REPLACE 会丢字段,故先 update 再 insert。
        val db = dbHelper.writableDatabase
        val updated = db.update(PRINT_TASK_TABLE, cv, "id=?", arrayOf(task.id.toString()))
        if (updated == 0) db.insert(PRINT_TASK_TABLE, null, cv)
    }

    fun updateTaskCoverPath(id: Long, path: String) {
        val cv = ContentValues().apply { put("cover_path", path) }
        dbHelper.writableDatabase.update(PRINT_TASK_TABLE, cv, "id=?", arrayOf(id.toString()))
    }

    fun updateTaskCost(id: Long, cents: Long) {
        val cv = ContentValues().apply { put("computed_cost_cents", cents) }
        dbHelper.writableDatabase.update(PRINT_TASK_TABLE, cv, "id=?", arrayOf(id.toString()))
    }

    fun existingTaskIds(): Set<Long> {
        val out = HashSet<Long>()
        dbHelper.readableDatabase.rawQuery("SELECT id FROM $PRINT_TASK_TABLE", null).use { c ->
            while (c.moveToNext()) out.add(c.getLong(0))
        }
        return out
    }

    fun setTasksHidden(taskIds: List<Long>, hidden: Boolean) {
        if (taskIds.isEmpty()) return
        val cv = ContentValues().apply { put("hidden", if (hidden) 1 else 0) }
        val placeholders = taskIds.joinToString(",") { "?" }
        dbHelper.writableDatabase.update(
            PRINT_TASK_TABLE, cv, "id IN ($placeholders)", taskIds.map { it.toString() }.toTypedArray()
        )
    }

    fun loadTasks(): List<PrintTaskRow> {
        val out = ArrayList<PrintTaskRow>()
        dbHelper.readableDatabase.query(
            PRINT_TASK_TABLE, null, null, null, null, null, "start_time DESC"
        ).use { c ->
            val idx = { name: String -> c.getColumnIndexOrThrow(name) }
            val orderIdCol = c.getColumnIndex("order_id")
            val hiddenCol = c.getColumnIndex("hidden")
            while (c.moveToNext()) {
                out.add(
                    PrintTaskRow(
                        id = c.getLong(idx("id")),
                        title = c.getString(idx("title")).orEmpty(),
                        coverPath = c.getString(idx("cover_path")).orEmpty(),
                        deviceModel = c.getString(idx("device_model")).orEmpty(),
                        deviceName = c.getString(idx("device_name")).orEmpty(),
                        weightGrams = c.getDouble(idx("weight")),
                        costTimeSeconds = c.getInt(idx("cost_time")),
                        startTimeMillis = c.getLong(idx("start_time")),
                        status = c.getInt(idx("status")),
                        failedType = c.getInt(idx("failed_type")),
                        repetitions = c.getInt(idx("repetitions")),
                        materials = materialsFromJson(c.getString(idx("materials_json"))),
                        computedCostCents = c.getLong(idx("computed_cost_cents")),
                        orderId = if (orderIdCol >= 0 && !c.isNull(orderIdCol)) c.getLong(orderIdCol) else null,
                        hidden = hiddenCol >= 0 && c.getInt(hiddenCol) != 0
                    )
                )
            }
        }
        return out
    }

    // ---- 订单 ----
    fun loadOrders(): List<PrintOrder> {
        val out = ArrayList<PrintOrder>()
        dbHelper.readableDatabase.query(
            PRINT_ORDER_TABLE, null, null, null, null, null, "created_at DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    PrintOrder(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")).orEmpty(),
                        actualChargeCents = c.getLong(c.getColumnIndexOrThrow("actual_charge_cents")),
                        note = c.getString(c.getColumnIndexOrThrow("note")).orEmpty(),
                        createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        return out
    }

    /** 新建订单并把给定任务归入。返回订单 id。 */
    fun createOrderWithTasks(name: String, taskIds: List<Long>): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("actual_charge_cents", 0L)
            put("note", "")
            put("created_at", System.currentTimeMillis())
        }
        val orderId = db.insert(PRINT_ORDER_TABLE, null, cv)
        if (orderId > 0) setTasksOrder(taskIds, orderId)
        return orderId
    }

    fun setTasksOrder(taskIds: List<Long>, orderId: Long?) {
        if (taskIds.isEmpty()) return
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            if (orderId == null) putNull("order_id") else put("order_id", orderId)
        }
        val placeholders = taskIds.joinToString(",") { "?" }
        db.update(PRINT_TASK_TABLE, cv, "id IN ($placeholders)", taskIds.map { it.toString() }.toTypedArray())
    }

    fun setOrderCharge(orderId: Long, cents: Long) {
        val cv = ContentValues().apply { put("actual_charge_cents", cents) }
        dbHelper.writableDatabase.update(PRINT_ORDER_TABLE, cv, "id=?", arrayOf(orderId.toString()))
    }

    fun setOrderName(orderId: Long, name: String) {
        val cv = ContentValues().apply { put("name", name) }
        dbHelper.writableDatabase.update(PRINT_ORDER_TABLE, cv, "id=?", arrayOf(orderId.toString()))
    }

    /** 解散订单:清空成员任务的 order_id 并删除订单。 */
    fun deleteOrder(orderId: Long) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply { putNull("order_id") }
        db.update(PRINT_TASK_TABLE, cv, "order_id=?", arrayOf(orderId.toString()))
        db.delete(PRINT_ORDER_TABLE, "id=?", arrayOf(orderId.toString()))
    }

    /** 单条任务自成一单的实际收费:用负数 id 的伪订单存在 cost_config 里太脏,改存在 print_order 不合适。
     *  这里把"单任务实际收费"也用 print_order 表表达——单任务点收费时即时建一个只含它的订单。 */

    companion object {
        private const val KEY_CONFIG = "config"
        const val KEY_LAST_SYNC = "last_sync_at"

        fun materialsToJson(materials: List<BambuCloudTaskMaterial>): String {
            val arr = JSONArray()
            materials.forEach { m ->
                arr.put(
                    JSONObject()
                        .put("filamentId", m.filamentId)
                        .put("filamentType", m.filamentType)
                        .put("color", m.color)
                        .put("weight", m.weightGrams)
                        .put("nozzleId", m.nozzleId)
                )
            }
            return arr.toString()
        }

        fun materialsFromJson(raw: String?): List<BambuCloudTaskMaterial> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(
                            BambuCloudTaskMaterial(
                                filamentId = o.optString("filamentId").trim(),
                                filamentType = o.optString("filamentType").trim(),
                                color = o.optString("color").trim(),
                                weightGrams = o.optDouble("weight", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                                nozzleId = o.optInt("nozzleId", 0)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
