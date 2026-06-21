package com.m0h31h31.bamburfidreader.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.m0h31h31.bamburfidreader.model.CrealityMaterial
import com.m0h31h31.bamburfidreader.model.InventoryItem
import com.m0h31h31.bamburfidreader.model.ShareTagDbMeta
import com.m0h31h31.bamburfidreader.model.ShareTagDbRow

internal const val FILAMENT_DB_NAME = "filaments.db"
private const val FILAMENT_DB_VERSION = 32
internal const val CREALITY_MATERIAL_TABLE = "creality_materials"
internal const val FILAMENT_TABLE = "filaments"
internal const val FILAMENT_TYPE_MAPPING_TABLE = "filament_type_mapping"
private const val FILAMENT_META_TABLE = "meta_v2"
internal const val FILAMENT_META_KEY_LOCALE = "filaments_locale"
internal const val TRAY_UID_TABLE = "filament_inventory"
internal const val SHARE_TAGS_TABLE = "share_tags"
internal const val SNAPMAKER_SHARE_TAGS_TABLE = "snapmaker_share_tags"
private const val ANOMALY_UIDS_TABLE = "anomaly_uids"

// 费用模块（打印报价 / 成本利润）
internal const val PRINT_TASK_TABLE = "print_task"
internal const val PRINT_ORDER_TABLE = "print_order"
internal const val MATERIAL_PRICE_TABLE = "material_price"
internal const val COST_CONFIG_TABLE = "cost_config"

class FilamentDbHelper(val context: Context) :
    SQLiteOpenHelper(context, FILAMENT_DB_NAME, null, FILAMENT_DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $CREALITY_MATERIAL_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                material_id TEXT NOT NULL UNIQUE,
                brand TEXT,
                material_type TEXT,
                name TEXT,
                min_temp INTEGER,
                max_temp INTEGER,
                diameter TEXT
            )
        """.trimIndent())
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $FILAMENT_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fila_id TEXT NOT NULL,
                fila_color_code TEXT NOT NULL,
                color_code TEXT NOT NULL,
                fila_color_type TEXT,
                fila_type TEXT,
                fila_detailed_type TEXT,
                color_name_zh TEXT,
                color_name_en TEXT,
                color_values TEXT,
                color_count INTEGER,
                UNIQUE (fila_id, color_code)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_filaments_fila_id_color_code ON $FILAMENT_TABLE (fila_id, color_code)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_filaments_fila_id_fila_color_code ON $FILAMENT_TABLE (fila_id, fila_color_code)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $FILAMENT_META_TABLE (
                meta_key TEXT PRIMARY KEY,
                value TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS "$TRAY_UID_TABLE" (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tray_uid TEXT UNIQUE NOT NULL,
                remaining_percent REAL NOT NULL,
                remaining_grams INTEGER,
                total_weight_grams INTEGER,
                filament_id INTEGER,
                material_id TEXT,
                material_type TEXT,
                material_detailed_type TEXT,
                color_name TEXT,
                color_name_en TEXT,
                fila_color_code TEXT,
                color_code TEXT,
                color_type TEXT,
                color_values TEXT,
                original_material TEXT,
                notes TEXT,
                production_date TEXT,
                FOREIGN KEY (filament_id) REFERENCES $FILAMENT_TABLE(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $FILAMENT_TYPE_MAPPING_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                base_type TEXT NOT NULL,
                specific_type TEXT NOT NULL,
                UNIQUE (base_type, specific_type)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_filament_type_mapping_base_type ON $FILAMENT_TYPE_MAPPING_TABLE (base_type)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $SHARE_TAGS_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_uid TEXT UNIQUE NOT NULL,
                tray_uid TEXT,
                material_id TEXT,
                material_type TEXT,
                material_detailed_type TEXT,
                color_uid TEXT,
                fila_color_code TEXT,
                color_name TEXT,
                color_name_en TEXT,
                color_type TEXT,
                color_values TEXT,
                raw_data TEXT,
                copy_count INTEGER NOT NULL DEFAULT 0,
                verified INTEGER NOT NULL DEFAULT 0,
                production_date TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $SNAPMAKER_SHARE_TAGS_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uid TEXT UNIQUE NOT NULL,
                vendor TEXT,
                manufacturer TEXT,
                main_type INTEGER NOT NULL DEFAULT 0,
                diameter INTEGER NOT NULL DEFAULT 0,
                weight INTEGER NOT NULL DEFAULT 0,
                rgb1 INTEGER NOT NULL DEFAULT 0,
                mf_date TEXT,
                raw_data TEXT,
                copy_count INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $ANOMALY_UIDS_TABLE (
                uid TEXT PRIMARY KEY NOT NULL,
                report_count INTEGER NOT NULL DEFAULT 1,
                synced_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createCostTables(db)
    }

    /** 费用模块的 4 张表（onCreate / onUpgrade 共用）。 */
    private fun createCostTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $PRINT_TASK_TABLE (
                id INTEGER PRIMARY KEY NOT NULL,
                title TEXT,
                cover_path TEXT,
                device_model TEXT,
                device_name TEXT,
                weight REAL NOT NULL DEFAULT 0,
                cost_time INTEGER NOT NULL DEFAULT 0,
                start_time INTEGER NOT NULL DEFAULT 0,
                status INTEGER NOT NULL DEFAULT 0,
                failed_type INTEGER NOT NULL DEFAULT 0,
                repetitions INTEGER NOT NULL DEFAULT 1,
                materials_json TEXT,
                computed_cost_cents INTEGER NOT NULL DEFAULT 0,
                order_id INTEGER,
                hidden INTEGER NOT NULL DEFAULT 0,
                synced_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $PRINT_ORDER_TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                actual_charge_cents INTEGER NOT NULL DEFAULT 0,
                note TEXT,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $MATERIAL_PRICE_TABLE (
                fila_id TEXT PRIMARY KEY NOT NULL,
                fila_type TEXT,
                base_type TEXT,
                price_per_g_cents INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $COST_CONFIG_TABLE (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS $FILAMENT_TABLE")
            db.execSQL("DROP TABLE IF EXISTS $FILAMENT_META_TABLE")
            db.execSQL("DROP TABLE IF EXISTS \"$TRAY_UID_TABLE\"")
            onCreate(db)
            return
        }
        if (oldVersion < 5) {
            addTrayColumn(db, "material_id", "TEXT")
            addTrayColumn(db, "material_type", "TEXT")
            addTrayColumn(db, "color_name", "TEXT")
            addTrayColumn(db, "color_code", "TEXT")
            addTrayColumn(db, "color_type", "TEXT")
            addTrayColumn(db, "color_values", "TEXT")
        }
        if (oldVersion < 8) {
            addTrayColumn(db, "remaining_grams", "INTEGER")
        }
        if (oldVersion < 7) {
            db.execSQL("DROP TABLE IF EXISTS meta")
            db.execSQL("DROP TABLE IF EXISTS $FILAMENT_META_TABLE")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $FILAMENT_META_TABLE (
                    meta_key TEXT PRIMARY KEY,
                    value TEXT
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 9) {
            // 为filament表添加id字段
            val tempFilamentTable = "${FILAMENT_TABLE}_temp"
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $tempFilamentTable (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fila_id TEXT NOT NULL,
                    fila_color_code TEXT NOT NULL,
                    fila_color_type TEXT,
                    fila_type TEXT,
                    color_name_zh TEXT,
                    color_values TEXT,
                    color_count INTEGER,
                    UNIQUE (fila_id, fila_color_code)
                )
                """.trimIndent()
            )
            db.execSQL(
                "INSERT INTO $tempFilamentTable (fila_id, fila_color_code, fila_color_type, fila_type, color_name_zh, color_values, color_count) " +
                "SELECT fila_id, fila_color_code, fila_color_type, fila_type, color_name_zh, color_values, color_count FROM $FILAMENT_TABLE"
            )
            db.execSQL("DROP TABLE IF EXISTS $FILAMENT_TABLE")
            db.execSQL("ALTER TABLE $tempFilamentTable RENAME TO $FILAMENT_TABLE")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_filaments_fila_id_color ON $FILAMENT_TABLE (fila_id, color_count)"
            )
            
            // 为filament_inventory表添加id和filament_id字段
            val tempInventoryTable = "${TRAY_UID_TABLE}_temp"
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS "$tempInventoryTable" (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tray_uid TEXT UNIQUE NOT NULL,
                    remaining_percent REAL NOT NULL,
                    remaining_grams INTEGER,
                    total_weight_grams INTEGER,
                    filament_id INTEGER,
                    FOREIGN KEY (filament_id) REFERENCES $FILAMENT_TABLE(id)
                )
                """.trimIndent()
            )
            // 这里需要处理数据迁移，将旧表中的数据迁移到新表
            // 由于我们需要通过fila_id和color_code关联到filament表的id，这里需要使用临时方案
            // 实际应用中，可能需要更复杂的数据迁移逻辑
            db.execSQL(
                "INSERT INTO \"$tempInventoryTable\" (tray_uid, remaining_percent, remaining_grams, total_weight_grams) " +
                "SELECT tray_uid, remaining_percent, remaining_grams, total_weight_grams FROM \"$TRAY_UID_TABLE\""
            )
            db.execSQL("DROP TABLE IF EXISTS \"$TRAY_UID_TABLE\"")
            db.execSQL("ALTER TABLE \"$tempInventoryTable\" RENAME TO \"$TRAY_UID_TABLE\"")
        }
        if (oldVersion < 10) {
            // 创建filament_type_mapping表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $FILAMENT_TYPE_MAPPING_TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    base_type TEXT NOT NULL,
                    specific_type TEXT NOT NULL,
                    UNIQUE (base_type, specific_type)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_filament_type_mapping_base_type ON $FILAMENT_TYPE_MAPPING_TABLE (base_type)"
            )
        }
        if (oldVersion < 11) {
            // 添加总克重字段
            addTrayColumn(db, "total_weight_grams", "INTEGER")
        }
        if (oldVersion < 12) {
            // 为filament表添加详细耗材类型字段
            try {
                db.execSQL("ALTER TABLE $FILAMENT_TABLE ADD COLUMN fila_detailed_type TEXT")
            } catch (_: Exception) {
                // Ignore duplicate column errors.
            }
        }
        if (oldVersion < 13) {
            // 为filament_inventory表添加详细材料类型字段
            addTrayColumn(db, "material_detailed_type", "TEXT")
        }
        if (oldVersion < 14) {
            // 为filament_inventory表添加原始耗材和备注字段
            addTrayColumn(db, "original_material", "TEXT")
            addTrayColumn(db, "notes", "TEXT")
        }
        if (oldVersion < 15) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $SHARE_TAGS_TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_uid TEXT UNIQUE NOT NULL,
                    tray_uid TEXT,
                    material_type TEXT,
                    color_uid TEXT,
                    color_name TEXT,
                    color_type TEXT,
                    color_values TEXT,
                    raw_data TEXT,
                    copy_count INTEGER NOT NULL DEFAULT 0,
                    verified INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
        if (oldVersion < 16) {
            try {
                db.execSQL("ALTER TABLE $SHARE_TAGS_TABLE ADD COLUMN raw_data TEXT")
            } catch (_: Exception) { }
        }
        if (oldVersion < 17) {
            try {
                db.execSQL("ALTER TABLE $SHARE_TAGS_TABLE ADD COLUMN production_date TEXT")
            } catch (_: Exception) { }
        }
        if (oldVersion < 18) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $CREALITY_MATERIAL_TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    material_id TEXT NOT NULL UNIQUE,
                    brand TEXT,
                    material_type TEXT,
                    name TEXT,
                    min_temp INTEGER,
                    max_temp INTEGER,
                    diameter TEXT
                )
            """.trimIndent())
        }
        if (oldVersion < 19) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $SNAPMAKER_SHARE_TAGS_TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uid TEXT UNIQUE NOT NULL,
                    vendor TEXT,
                    manufacturer TEXT,
                    main_type INTEGER NOT NULL DEFAULT 0,
                    diameter INTEGER NOT NULL DEFAULT 0,
                    weight INTEGER NOT NULL DEFAULT 0,
                    rgb1 INTEGER NOT NULL DEFAULT 0,
                    mf_date TEXT,
                    raw_data TEXT,
                    copy_count INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }
        if (oldVersion < 20) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $ANOMALY_UIDS_TABLE (
                    uid TEXT PRIMARY KEY NOT NULL,
                    report_count INTEGER NOT NULL DEFAULT 1,
                    synced_at INTEGER NOT NULL
                )
            """.trimIndent())
        }
        if (oldVersion < 21) {
            try {
                db.execSQL("ALTER TABLE $ANOMALY_UIDS_TABLE ADD COLUMN report_count INTEGER NOT NULL DEFAULT 1")
            } catch (_: Exception) {}
        }
        if (oldVersion < 22) {
            try {
                db.execSQL("ALTER TABLE $FILAMENT_TABLE ADD COLUMN color_name_en TEXT")
            } catch (_: Exception) {}
        }
        if (oldVersion < 23) {
            try {
                db.execSQL("ALTER TABLE \"$TRAY_UID_TABLE\" ADD COLUMN color_name_en TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $SHARE_TAGS_TABLE ADD COLUMN color_name_en TEXT")
            } catch (_: Exception) {}
            // Force full re-sync on next launch to populate color_name_en in filaments
            // and backfill inventory/share_tags via rematchUnnamedInventoryColors().
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_color_content_hash'")
            } catch (_: Exception) {}
        }
        if (oldVersion < 24) {
            recreateFilamentsTableWithColorCode(db)
            try {
                db.execSQL("ALTER TABLE $SHARE_TAGS_TABLE ADD COLUMN material_id TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_color_content_hash'")
            } catch (_: Exception) {}
        }
        if (oldVersion < 25) {
            addTrayColumn(db, "fila_color_code", "TEXT")
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_color_content_hash'")
            } catch (_: Exception) {}
        }
        if (oldVersion < 26) {
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_color_content_hash'")
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_type_content_hash'")
            } catch (_: Exception) {}
        }
        if (oldVersion < 27) {
            try {
                db.execSQL("ALTER TABLE $SHARE_TAGS_TABLE ADD COLUMN material_detailed_type TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_color_content_hash'")
            } catch (_: Exception) {}
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_type_content_hash'")
            } catch (_: Exception) {}
        }
        if (oldVersion < 28) {
            try {
                db.execSQL("ALTER TABLE $SHARE_TAGS_TABLE ADD COLUMN fila_color_code TEXT")
            } catch (_: Exception) {}
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_color_content_hash'")
            } catch (_: Exception) {}
            try {
                db.execSQL("DELETE FROM $FILAMENT_META_TABLE WHERE meta_key = 'filament_type_content_hash'")
            } catch (_: Exception) {}
        }
        if (oldVersion < 29) {
            addTrayColumn(db, "production_date", "TEXT")
        }
        if (oldVersion < 30) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_filaments_fila_id_fila_color_code ON $FILAMENT_TABLE (fila_id, fila_color_code)"
            )
        }
        if (oldVersion < 31) {
            createCostTables(db)
        }
        if (oldVersion < 32) {
            try {
                db.execSQL("ALTER TABLE $PRINT_TASK_TABLE ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
        }
    }

    private fun recreateFilamentsTableWithColorCode(db: SQLiteDatabase) {
        val tempFilamentTable = "${FILAMENT_TABLE}_v24"
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $tempFilamentTable (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fila_id TEXT NOT NULL,
                fila_color_code TEXT NOT NULL,
                color_code TEXT NOT NULL,
                fila_color_type TEXT,
                fila_type TEXT,
                fila_detailed_type TEXT,
                color_name_zh TEXT,
                color_name_en TEXT,
                color_values TEXT,
                color_count INTEGER,
                UNIQUE (fila_id, color_code)
            )
            """.trimIndent()
        )
        db.execSQL("DROP TABLE IF EXISTS $FILAMENT_TABLE")
        db.execSQL("ALTER TABLE $tempFilamentTable RENAME TO $FILAMENT_TABLE")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_filaments_fila_id_color_code ON $FILAMENT_TABLE (fila_id, color_code)"
        )
    }

    private fun addTrayColumn(db: SQLiteDatabase, column: String, type: String) {
        try {
            db.execSQL("ALTER TABLE \"$TRAY_UID_TABLE\" ADD COLUMN $column $type")
        } catch (_: Exception) {
            // Ignore duplicate column errors.
        }
    }

    // --- share_tags 相关方法 ---

    fun insertShareTag(
        db: SQLiteDatabase,
        fileUid: String,
        trayUid: String?,
        materialId: String? = null,
        materialType: String?,
        materialDetailedType: String? = null,
        colorUid: String?,
        filaColorCode: String? = null,
        colorName: String?,
        colorNameEn: String? = null,
        colorType: String?,
        colorValues: String?,
        rawData: String? = null,
        productionDate: String? = null
    ): Long {
        val values = ContentValues()
        values.put("file_uid", fileUid)
        if (!trayUid.isNullOrBlank()) values.put("tray_uid", trayUid)
        if (!materialId.isNullOrBlank()) values.put("material_id", materialId)
        if (!materialType.isNullOrBlank()) values.put("material_type", materialType)
        if (!materialDetailedType.isNullOrBlank()) values.put("material_detailed_type", materialDetailedType)
        if (!colorUid.isNullOrBlank()) values.put("color_uid", colorUid)
        if (!filaColorCode.isNullOrBlank()) values.put("fila_color_code", filaColorCode)
        if (!colorName.isNullOrBlank()) values.put("color_name", colorName)
        if (!colorNameEn.isNullOrBlank()) values.put("color_name_en", colorNameEn)
        if (!colorType.isNullOrBlank()) values.put("color_type", colorType)
        if (!colorValues.isNullOrBlank()) values.put("color_values", colorValues)
        if (!rawData.isNullOrBlank()) values.put("raw_data", rawData)
        if (!productionDate.isNullOrBlank()) values.put("production_date", productionDate)
        return db.insertWithOnConflict(SHARE_TAGS_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun updateShareTagProductionDate(db: SQLiteDatabase, fileUid: String, productionDate: String) {
        val values = ContentValues()
        values.put("production_date", productionDate)
        db.update(SHARE_TAGS_TABLE, values, "file_uid = ?", arrayOf(fileUid))
    }

    fun getShareTagMetaMap(db: SQLiteDatabase): Map<String, ShareTagDbMeta> {
        val result = mutableMapOf<String, ShareTagDbMeta>()
        val cursor = db.query(
            SHARE_TAGS_TABLE,
            arrayOf("id", "file_uid", "copy_count", "verified"),
            null, null, null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val fileUid = it.getString(1) ?: continue
                val copyCount = it.getInt(2)
                val verified = it.getInt(3) != 0
                result[fileUid.uppercase()] = ShareTagDbMeta(id, copyCount, verified)
            }
        }
        return result
    }

    fun getAllShareTagRows(db: SQLiteDatabase): List<ShareTagDbRow> {
        val result = mutableListOf<ShareTagDbRow>()
        val cursor = db.query(
            SHARE_TAGS_TABLE,
            arrayOf("id", "file_uid", "tray_uid", "material_id", "material_type", "material_detailed_type", "color_uid", "fila_color_code", "color_name", "color_name_en", "color_type", "color_values", "raw_data", "copy_count", "verified", "production_date"),
            null, null, null, null,
            "material_type ASC, color_uid ASC, file_uid ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(ShareTagDbRow(
                    id = it.getLong(0),
                    fileUid = it.getString(1) ?: "",
                    trayUid = it.getString(2),
                    materialId = it.getString(3),
                    materialType = it.getString(4),
                    materialDetailedType = it.getString(5),
                    colorUid = it.getString(6),
                    filaColorCode = it.getString(7),
                    colorName = it.getString(8),
                    colorNameEn = it.getString(9),
                    colorType = it.getString(10),
                    colorValues = it.getString(11),
                    rawData = it.getString(12),
                    copyCount = it.getInt(13),
                    verified = it.getInt(14) != 0,
                    productionDate = it.getString(15)
                ))
            }
        }
        return result
    }

    fun updateShareTagRawData(db: SQLiteDatabase, fileUid: String, rawData: String) {
        val values = ContentValues()
        values.put("raw_data", rawData)
        db.update(SHARE_TAGS_TABLE, values, "file_uid = ?", arrayOf(fileUid))
    }

    fun getExistingShareTrayUids(db: SQLiteDatabase): Set<String> {
        val result = mutableSetOf<String>()
        val cursor = db.query(
            SHARE_TAGS_TABLE,
            arrayOf("tray_uid"),
            "tray_uid IS NOT NULL AND tray_uid != ''",
            null, null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                val uid = it.getString(0)
                if (!uid.isNullOrBlank()) result.add(uid.uppercase())
            }
        }
        return result
    }

    fun incrementShareTagCopyCount(db: SQLiteDatabase, id: Long) {
        db.execSQL("UPDATE $SHARE_TAGS_TABLE SET copy_count = copy_count + 1 WHERE id = ?", arrayOf(id))
    }

    fun setShareTagVerified(db: SQLiteDatabase, id: Long, verified: Boolean) {
        val values = ContentValues()
        values.put("verified", if (verified) 1 else 0)
        db.update(SHARE_TAGS_TABLE, values, "id = ?", arrayOf(id.toString()))
    }

    fun resetShareTagByTrayUid(db: SQLiteDatabase, trayUid: String) {
        if (trayUid.isBlank()) return
        val values = ContentValues()
        values.put("copy_count", 0)
        values.put("verified", 0)
        db.update(SHARE_TAGS_TABLE, values, "tray_uid = ?", arrayOf(trayUid))
    }

    fun deleteShareTagByFileUid(db: SQLiteDatabase, fileUid: String) {
        db.delete(SHARE_TAGS_TABLE, "file_uid = ?", arrayOf(fileUid))
    }

    fun clearShareTagsTable(db: SQLiteDatabase): Int {
        return db.delete(SHARE_TAGS_TABLE, "1", null)
    }

    fun clearSnapmakerShareTagsTable(db: SQLiteDatabase): Int {
        return db.delete(SNAPMAKER_SHARE_TAGS_TABLE, "1", null)
    }

    // --- snapmaker_share_tags 相关方法 ---

    data class SnapmakerShareTagRow(
        val id: Long,
        val uid: String,
        val vendor: String?,
        val manufacturer: String?,
        val mainType: Int,
        val diameter: Int,
        val weight: Int,
        val rgb1: Int,
        val mfDate: String?,
        val rawData: String?,
        val copyCount: Int
    )

    fun insertSnapmakerShareTag(
        db: SQLiteDatabase,
        uid: String,
        vendor: String?,
        manufacturer: String?,
        mainType: Int,
        diameter: Int,
        weight: Int,
        rgb1: Int,
        mfDate: String?,
        rawData: String?
    ): Long {
        val values = ContentValues().apply {
            put("uid", uid)
            if (!vendor.isNullOrBlank()) put("vendor", vendor)
            if (!manufacturer.isNullOrBlank()) put("manufacturer", manufacturer)
            put("main_type", mainType)
            put("diameter", diameter)
            put("weight", weight)
            put("rgb1", rgb1)
            if (!mfDate.isNullOrBlank()) put("mf_date", mfDate)
            if (!rawData.isNullOrBlank()) put("raw_data", rawData)
        }
        return db.insertWithOnConflict(SNAPMAKER_SHARE_TAGS_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getAllSnapmakerShareTagRows(db: SQLiteDatabase): List<SnapmakerShareTagRow> {
        val result = mutableListOf<SnapmakerShareTagRow>()
        val cursor = db.query(
            SNAPMAKER_SHARE_TAGS_TABLE,
            arrayOf("id", "uid", "vendor", "manufacturer", "main_type", "diameter", "weight", "rgb1", "mf_date", "raw_data", "copy_count"),
            null, null, null, null,
            "vendor ASC, uid ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(SnapmakerShareTagRow(
                    id = it.getLong(0),
                    uid = it.getString(1) ?: "",
                    vendor = it.getString(2),
                    manufacturer = it.getString(3),
                    mainType = it.getInt(4),
                    diameter = it.getInt(5),
                    weight = it.getInt(6),
                    rgb1 = it.getInt(7),
                    mfDate = it.getString(8),
                    rawData = it.getString(9),
                    copyCount = it.getInt(10)
                ))
            }
        }
        return result
    }

    fun getAllSnapmakerShareTagUids(db: SQLiteDatabase): List<String> {
        val result = mutableListOf<String>()
        val cursor = db.query(SNAPMAKER_SHARE_TAGS_TABLE, arrayOf("uid"), null, null, null, null, null)
        cursor.use { while (it.moveToNext()) { result.add(it.getString(0) ?: "") } }
        return result
    }

    fun incrementSnapmakerShareTagCopyCount(db: SQLiteDatabase, id: Long) {
        db.execSQL("UPDATE $SNAPMAKER_SHARE_TAGS_TABLE SET copy_count = copy_count + 1 WHERE id = ?", arrayOf(id))
    }

    fun deleteSnapmakerShareTagByUid(db: SQLiteDatabase, uid: String) {
        db.delete(SNAPMAKER_SHARE_TAGS_TABLE, "uid = ?", arrayOf(uid))
    }

    // --- anomaly_uids 相关方法 ---

    fun saveAnomalyUids(db: SQLiteDatabase, uids: Map<String, Int>) {
        db.beginTransaction()
        try {
            db.delete(ANOMALY_UIDS_TABLE, null, null)
            val now = System.currentTimeMillis()
            for ((uid, count) in uids) {
                val cv = ContentValues()
                cv.put("uid", uid.uppercase().trim())
                cv.put("report_count", count)
                cv.put("synced_at", now)
                db.insertWithOnConflict(ANOMALY_UIDS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAnomalyUids(db: SQLiteDatabase): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val cursor = db.query(ANOMALY_UIDS_TABLE, arrayOf("uid", "report_count"), null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                val uid = it.getString(0)
                val count = it.getInt(1)
                if (!uid.isNullOrBlank()) result[uid] = count
            }
        }
        return result
    }

    fun getMetaValue(db: SQLiteDatabase, key: String): String? {
        val cursor = db.query(
            FILAMENT_META_TABLE,
            arrayOf("value"),
            "meta_key = ?",
            arrayOf(key),
            null,
            null,
            null
        )
        cursor.use {
            return if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun setMetaValue(db: SQLiteDatabase, key: String, value: String) {
        val values = ContentValues()
        values.put("meta_key", key)
        values.put("value", value)
        db.insertWithOnConflict(
            FILAMENT_META_TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteMetaValue(db: SQLiteDatabase, key: String) {
        db.delete(FILAMENT_META_TABLE, "meta_key = ?", arrayOf(key))
    }

    // ── Creality material queries ──────────────────────────────────────────────

    fun getCrealityBrands(db: SQLiteDatabase): List<String> {
        val result = mutableListOf<String>()
        val cursor = db.query(true, CREALITY_MATERIAL_TABLE, arrayOf("brand"),
            null, null, "brand", null, "brand ASC", null)
        cursor.use { c -> while (c.moveToNext()) { c.getString(0)?.let { result.add(it) } } }
        return result
    }

    fun getCrealityTypes(db: SQLiteDatabase, brand: String): List<String> {
        val result = mutableListOf<String>()
        val cursor = db.query(true, CREALITY_MATERIAL_TABLE, arrayOf("material_type"),
            "brand = ?", arrayOf(brand), "material_type", null, "material_type ASC", null)
        cursor.use { c -> while (c.moveToNext()) { c.getString(0)?.let { result.add(it) } } }
        return result
    }

    fun getCrealityMaterials(db: SQLiteDatabase, brand: String, type: String): List<CrealityMaterial> {
        val result = mutableListOf<CrealityMaterial>()
        val cursor = db.query(CREALITY_MATERIAL_TABLE,
            arrayOf("material_id", "brand", "material_type", "name", "min_temp", "max_temp", "diameter"),
            "brand = ? AND material_type = ?", arrayOf(brand, type), null, null, "name ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                val mid = c.getString(0) ?: return@use
                result.add(CrealityMaterial(
                    materialId = mid,
                    brand = c.getString(1).orEmpty(),
                    materialType = c.getString(2).orEmpty(),
                    name = c.getString(3).orEmpty(),
                    minTemp = c.getInt(4),
                    maxTemp = c.getInt(5),
                    diameter = c.getString(6).orEmpty()
                ))
            }
        }
        return result
    }

    fun getCrealityMaterialById(db: SQLiteDatabase, materialId: String): CrealityMaterial? {
        val cursor = db.query(CREALITY_MATERIAL_TABLE,
            arrayOf("material_id", "brand", "material_type", "name", "min_temp", "max_temp", "diameter"),
            "material_id = ?", arrayOf(materialId), null, null, null)
        return cursor.use { c ->
            if (c.moveToFirst()) CrealityMaterial(
                materialId = c.getString(0).orEmpty(),
                brand = c.getString(1).orEmpty(),
                materialType = c.getString(2).orEmpty(),
                name = c.getString(3).orEmpty(),
                minTemp = c.getInt(4),
                maxTemp = c.getInt(5),
                diameter = c.getString(6).orEmpty()
            ) else null
        }
    }

    fun getTrayRemainingPercent(db: SQLiteDatabase, trayUid: String): Float? {
        val cursor = db.query(
            TRAY_UID_TABLE,
            arrayOf("remaining_percent"),
            "tray_uid = ?",
            arrayOf(trayUid),
            null,
            null,
            null
        )
        cursor.use {
            return if (it.moveToFirst()) it.getFloat(0) else null
        }
    }

    fun getTrayRemainingGrams(db: SQLiteDatabase, trayUid: String): Int? {
        val cursor = db.query(
            TRAY_UID_TABLE,
            arrayOf("remaining_grams"),
            "tray_uid = ?",
            arrayOf(trayUid),
            null,
            null,
            null
        )
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else null
        }
    }

    fun upsertTrayRemaining(
        db: SQLiteDatabase,
        trayUid: String,
        percent: Float,
        grams: Int?,
        totalGrams: Int? = null
    ) {
        val values = ContentValues()
        // 只保留1位小数
        val roundedPercent = Math.round(percent * 10) / 10f
        values.put("remaining_percent", roundedPercent)
        if (grams != null) {
            values.put("remaining_grams", grams)
        }
        if (totalGrams != null) {
            values.put("total_weight_grams", totalGrams)
        }
        val updated = db.update(
            TRAY_UID_TABLE,
            values,
            "tray_uid = ?",
            arrayOf(trayUid)
        )
        if (updated == 0) {
            values.put("tray_uid", trayUid)
            db.insertWithOnConflict(
                TRAY_UID_TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    fun upsertTrayInventory(
        db: SQLiteDatabase,
        trayUid: String,
        remainingPercent: Float,
        remainingGrams: Int?,
        totalWeightGrams: Int? = null,
        filamentId: Long?,
        materialId: String? = null,
        materialType: String? = null,
        detailedMaterialType: String? = null,
        colorName: String? = null,
        colorNameEn: String? = null,
        filaColorCode: String? = null,
        colorCode: String? = null,
        colorType: String? = null,
        colorValues: String? = null,
        productionDate: String? = null
    ) {
        val values = ContentValues()
        values.put("remaining_percent", remainingPercent)
        if (remainingGrams != null) {
            values.put("remaining_grams", remainingGrams)
        }
        if (totalWeightGrams != null) {
            values.put("total_weight_grams", totalWeightGrams)
        }
        if (filamentId != null) {
            values.put("filament_id", filamentId)
        }
        if (materialId != null) {
            values.put("material_id", materialId)
        }
        if (materialType != null) {
            values.put("material_type", materialType)
        }
        if (detailedMaterialType != null) {
            values.put("material_detailed_type", detailedMaterialType)
        }
        if (colorName != null) {
            values.put("color_name", colorName)
        }
        if (colorNameEn != null) {
            values.put("color_name_en", colorNameEn)
        }
        if (filaColorCode != null) {
            values.put("fila_color_code", filaColorCode)
        }
        if (colorCode != null) {
            values.put("color_code", colorCode)
        }
        if (colorType != null) {
            values.put("color_type", colorType)
        }
        if (colorValues != null) {
            values.put("color_values", colorValues)
        }
        if (productionDate != null) {
            values.put("production_date", productionDate)
        }
        // UPDATE first to preserve original_material/notes; INSERT only for new rows.
        val updated = db.update(
            TRAY_UID_TABLE,
            values,
            "tray_uid = ?",
            arrayOf(trayUid)
        )
        if (updated == 0) {
            values.put("tray_uid", trayUid)
            db.insertWithOnConflict(
                TRAY_UID_TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    fun getFilamentId(db: SQLiteDatabase, filaId: String, colorCode: String): Long? {
        val cursor = db.query(
            FILAMENT_TABLE,
            arrayOf("id"),
            "fila_id = ? AND color_code = ?",
            arrayOf(filaId, normalizeBambuColorCode(colorCode)),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return null
    }

    fun queryInventory(db: SQLiteDatabase, keyword: String): List<InventoryItem> {
        val trimmed = keyword.trim()
        val selection: String?
        val selectionArgs: Array<String>?
        if (trimmed.isBlank()) {
            selection = null
            selectionArgs = null
        } else {
            selection = """
                tray_uid LIKE ? OR
                material_type LIKE ? OR
                material_detailed_type LIKE ? OR
                color_name LIKE ? OR
                fila_color_code LIKE ? OR
                color_code LIKE ? OR
                color_type LIKE ? OR
                color_values LIKE ? OR
                production_date LIKE ? OR
                CAST(remaining_percent AS TEXT) LIKE ?
            """.trimIndent()
            val pattern = "%$trimmed%"
            selectionArgs = Array(10) { pattern }
        }
        val sql = """
            SELECT
                tray_uid,
                material_type,
                material_detailed_type,
                color_name,
                color_name_en,
                fila_color_code,
                color_code,
                color_type,
                color_values,
                remaining_percent,
                remaining_grams,
                original_material,
                notes,
                production_date
            FROM
                "$TRAY_UID_TABLE"
            ${if (selection != null) "WHERE $selection" else ""}
            ORDER BY
                tray_uid ASC
        """.trimIndent()
        val cursor = db.rawQuery(sql, selectionArgs)
        cursor.use {
            val results = ArrayList<InventoryItem>()
            while (it.moveToNext()) {
                val colorValues = it.getString(8).orEmpty()
                    .split(",")
                    .map { value -> value.trim() }
                    .filter { value -> value.isNotBlank() }
                results.add(
                    InventoryItem(
                        trayUid = it.getString(0).orEmpty(),
                        materialType = it.getString(1).orEmpty(),
                        materialDetailedType = it.getString(2).orEmpty(),
                        colorName = it.getString(3).orEmpty(),
                        colorNameEn = it.getString(4).orEmpty(),
                        filaColorCode = it.getString(5).orEmpty(),
                        colorCode = it.getString(6).orEmpty(),
                        colorType = it.getString(7).orEmpty(),
                        colorValues = colorValues,
                        remainingPercent = it.getFloat(9),
                        remainingGrams = if (!it.isNull(10)) it.getInt(10) else null,
                        productionDate = it.getString(13).orEmpty(),
                        originalMaterial = it.getString(11).orEmpty(),
                        notes = it.getString(12).orEmpty()
                    )
                )
            }
            return results
        }
    }

    /**
     * 获取filament_inventory库的全部数据，用于数据页面显示
     */
    fun getAllInventory(db: SQLiteDatabase): List<InventoryItem> {
        val sql = """
            SELECT
                tray_uid,
                material_type,
                material_detailed_type,
                color_name,
                color_name_en,
                fila_color_code,
                color_code,
                color_type,
                color_values,
                remaining_percent,
                remaining_grams,
                original_material,
                notes,
                production_date
            FROM
                "$TRAY_UID_TABLE"
            ORDER BY
                tray_uid ASC
        """.trimIndent()
        val cursor = db.rawQuery(sql, null)
        cursor.use {
            val results = ArrayList<InventoryItem>()
            while (it.moveToNext()) {
                val colorValues = it.getString(8).orEmpty()
                    .split(",")
                    .map { value -> value.trim() }
                    .filter { value -> value.isNotBlank() }
                results.add(
                    InventoryItem(
                        trayUid = it.getString(0).orEmpty(),
                        materialType = it.getString(1).orEmpty(),
                        materialDetailedType = it.getString(2).orEmpty(),
                        colorName = it.getString(3).orEmpty(),
                        colorNameEn = it.getString(4).orEmpty(),
                        filaColorCode = it.getString(5).orEmpty(),
                        colorCode = it.getString(6).orEmpty(),
                        colorType = it.getString(7).orEmpty(),
                        colorValues = colorValues,
                        remainingPercent = it.getFloat(9),
                        remainingGrams = if (!it.isNull(10)) it.getInt(10) else null,
                        productionDate = it.getString(13).orEmpty(),
                        originalMaterial = it.getString(11).orEmpty(),
                        notes = it.getString(12).orEmpty()
                    )
                )
            }
            return results
        }
    }

    fun deleteTrayInventory(db: SQLiteDatabase, trayUid: String) {
        db.delete(
            TRAY_UID_TABLE,
            "tray_uid = ?",
            arrayOf(trayUid)
        )
    }

    fun upsertTrayNotes(
        db: SQLiteDatabase,
        trayUid: String,
        originalMaterial: String,
        notes: String
    ) {
        val values = ContentValues()
        values.put("original_material", originalMaterial)
        values.put("notes", notes)
        val updated = db.update(
            TRAY_UID_TABLE,
            values,
            "tray_uid = ?",
            arrayOf(trayUid)
        )
        if (updated == 0) {
            values.put("tray_uid", trayUid)
            values.put("remaining_percent", 100f)
            db.insertWithOnConflict(TRAY_UID_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun getTrayExtraFields(db: SQLiteDatabase, trayUid: String): Pair<String, String> {
        val cursor = db.query(
            TRAY_UID_TABLE,
            arrayOf("original_material", "notes"),
            "tray_uid = ?",
            arrayOf(trayUid),
            null, null, null
        )
        cursor.use {
            return if (it.moveToFirst()) {
                Pair(it.getString(0).orEmpty(), it.getString(1).orEmpty())
            } else {
                Pair("", "")
            }
        }
    }

}
