package com.m0h31h31.bamburfidreader.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.m0h31h31.bamburfidreader.BuildConfig
import com.m0h31h31.bamburfidreader.model.FilamentColorEntry
import com.m0h31h31.bamburfidreader.logging.logDebug
import com.m0h31h31.bamburfidreader.util.normalizeColorValue
import com.m0h31h31.bamburfidreader.utils.NetworkUtils
import java.io.File
import java.io.IOException
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private const val FILAMENT_JSON_NAME = "filaments_color_codes.json"
private const val FILAMENTS_TYPE_MAPPING_FILE = "filaments_type_mapping.json"
private const val CREALITY_MATERIAL_FILE = "creality_material_list.json"

private data class FilamentJsonSource(
    val jsonText: String,
    val lastModified: Long
)

private data class FilamentTypeMappingEntry(
    val baseType: String,
    val specificType: String
)

internal fun syncFilamentDatabase(context: Context, dbHelper: FilamentDbHelper) {
    // 同步filaments_color_codes.json
    val colorSource = readFilamentJsonFromExternal(context) ?: return
    logDebug("配置文件更新时间: ${colorSource.lastModified}")
    val colorCacheFile = File(context.cacheDir, FILAMENT_JSON_NAME)
    try {
        colorCacheFile.writeText(colorSource.jsonText, Charsets.UTF_8)
    } catch (_: IOException) {
        // Ignore cache write failures.
    }

    // 同步filaments_type_mapping.json
    val typeSource = readFilamentTypeMappingFromExternal(context) ?: return
    logDebug("耗材类型映射文件更新时间: ${typeSource.lastModified}")
    val typeCacheFile = File(context.cacheDir, FILAMENTS_TYPE_MAPPING_FILE)
    try {
        typeCacheFile.writeText(typeSource.jsonText, Charsets.UTF_8)
    } catch (_: IOException) {
        // Ignore cache write failures.
    }

    val db = dbHelper.writableDatabase
    val currentAppVersion = BuildConfig.VERSION_CODE.toString()
    val colorHash = NetworkUtils.calculateHash(
        "${colorSource.jsonText}\nappVersion:$currentAppVersion".toByteArray(Charsets.UTF_8)
    )
    val typeHash = NetworkUtils.calculateHash(
        "${typeSource.jsonText}\nappVersion:$currentAppVersion".toByteArray(Charsets.UTF_8)
    )
    val storedColorHash = dbHelper.getMetaValue(db, "filament_color_content_hash")
    val storedTypeHash = dbHelper.getMetaValue(db, "filament_type_content_hash")
    val currentLocale = Locale.getDefault().language.lowercase(Locale.US)
    val storedLocale = dbHelper.getMetaValue(db, FILAMENT_META_KEY_LOCALE)
    // 检查是否需要更新（用内容 hash，避免 lastModified 不可靠的问题）
    if (storedColorHash == colorHash && storedTypeHash == typeHash && storedLocale == currentLocale) {
        logDebug("配置文件未变化，跳过更新")
        return
    }

    val typeEntries = parseFilamentTypeMappingEntries(typeSource.jsonText)
    val entries = parseFilamentEntries(
        colorSource.jsonText,
        typeEntries.groupBy(
            keySelector = { it.baseType },
            valueTransform = { it.specificType }
        )
    )
    db.beginTransaction()
    try {
        // 清空并重新写入filaments表
        db.delete(FILAMENT_TABLE, null, null)
        val values = ContentValues()
        entries.forEach { entry ->
            values.clear()
            values.put("fila_id", entry.filaId)
            values.put("fila_color_code", entry.filaColorCode)
            values.put("color_code", entry.colorCode)
            values.put("fila_color_type", entry.colorType)
            values.put("fila_type", entry.filaType)
            val detailedType = entry.filaDetailedType
            if (detailedType.isNotBlank()) {
                values.put("fila_detailed_type", detailedType)
            }
            values.put("color_name_zh", entry.colorNameZh)
            values.put("color_name_en", entry.colorNameEn)
            values.put("color_values", entry.colorValues.joinToString(separator = ","))
            values.put("color_count", entry.colorCount)
            db.insertWithOnConflict(
                FILAMENT_TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }

        // 清空并重新写入filament_type_mapping表
        db.delete(FILAMENT_TYPE_MAPPING_TABLE, null, null)
        typeEntries.forEach { entry ->
            values.clear()
            values.put("base_type", entry.baseType)
            values.put("specific_type", entry.specificType)
            db.insertWithOnConflict(
                FILAMENT_TYPE_MAPPING_TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }

        dbHelper.setMetaValue(db, "filament_color_content_hash", colorHash)
        dbHelper.setMetaValue(db, "filament_type_content_hash", typeHash)
        dbHelper.setMetaValue(db, FILAMENT_META_KEY_LOCALE, currentLocale)
        db.setTransactionSuccessful()
        logDebug("配置数据写入完成: ${entries.size} 个颜色配置, ${typeEntries.size} 个类型映射")
    } finally {
        db.endTransaction()
    }
    rematchUnnamedInventoryColors(db)
}

private fun rematchUnnamedInventoryColors(db: SQLiteDatabase) {
    var updated = 0

    val invCursor = db.query(
        TRAY_UID_TABLE,
        arrayOf("tray_uid", "material_id", "color_code", "fila_color_code"),
        "material_id IS NOT NULL AND material_id != ''",
        null, null, null, null
    )
    invCursor.use {
        while (it.moveToNext()) {
            val trayUid = it.getString(0) ?: continue
            val materialId = it.getString(1) ?: continue
            val colorCode = it.getString(2).orEmpty()
            val filaColorCode = it.getString(3).orEmpty()
            val matched = findFilamentEntryInDb(db, materialId, colorCode)
                ?: findFilamentEntryByFilaColorCodeInDb(
                    db,
                    materialId,
                    filaColorCode.ifBlank { colorCode }
                )
                ?: continue
            val values = ContentValues()
            values.put("material_type", matched.filaType)
            values.put("material_detailed_type", matched.filaDetailedType)
            values.put("color_name", matched.resolvedColorName())
            values.put("color_name_en", matched.colorNameEn)
            values.put("fila_color_code", matched.filaColorCode)
            values.put("color_code", matched.colorCode)
            values.put("color_type", matched.colorType)
            values.put("color_values", matched.colorValues.joinToString(separator = ","))
            db.update(TRAY_UID_TABLE, values, "tray_uid = ?", arrayOf(trayUid))
            updated++
        }
    }

    val tagCursor = db.query(
        SHARE_TAGS_TABLE,
        arrayOf("file_uid", "material_id", "color_uid", "raw_data"),
        null,
        null, null, null, null
    )
    tagCursor.use {
        while (it.moveToNext()) {
            val fileUid = it.getString(0) ?: continue
            val parsed = parseBambuMaterialAndColorFromRawData(it.getString(3).orEmpty())
            val materialId = parsed?.first.orEmpty().ifBlank { it.getString(1).orEmpty() }
            val colorUid = parsed?.second.orEmpty().ifBlank { it.getString(2).orEmpty() }
            val matched = findFilamentEntryInDb(db, materialId, colorUid) ?: continue
            val values = ContentValues()
            values.put("material_id", materialId)
            values.put("color_uid", matched.colorCode)
            values.put("fila_color_code", matched.filaColorCode)
            values.put("material_type", matched.filaType)
            values.put("material_detailed_type", matched.filaDetailedType)
            values.put("color_name", matched.resolvedColorName())
            values.put("color_name_en", matched.colorNameEn)
            values.put("color_type", matched.colorType)
            values.put("color_values", matched.colorValues.joinToString(separator = ","))
            db.update(SHARE_TAGS_TABLE, values, "file_uid = ?", arrayOf(fileUid))
            updated++
        }
    }

    if (updated > 0) logDebug("颜色重新匹配完成: $updated 条记录已更新")
}

private fun parseBambuMaterialAndColorFromRawData(rawData: String): Pair<String, String>? {
    val block1Hex = rawData.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .drop(1)
        .firstOrNull()
        ?: return null
    val block1 = hexToBytes(block1Hex) ?: return null
    if (block1.size < 16) return null
    val rawColorCode = asciiOnly(block1.copyOfRange(0, 8))
    val materialId = asciiOnly(block1.copyOfRange(8, 16))
    val colorCode = normalizeBambuColorCode(rawColorCode)
    if (materialId.isBlank() || colorCode.isBlank()) return null
    return materialId to colorCode
}

private fun hexToBytes(value: String): ByteArray? {
    val cleaned = value.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    if (cleaned.length < 32 || cleaned.length % 2 != 0) return null
    return try {
        ByteArray(cleaned.length / 2) { index ->
            cleaned.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    } catch (_: Exception) {
        null
    }
}

private fun asciiOnly(bytes: ByteArray): String {
    val trimmed = bytes.dropLastWhile { it == 0x00.toByte() || it == 0xFF.toByte() }.toByteArray()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.all { it in 0x20..0x7E }) String(trimmed, Charsets.US_ASCII) else ""
}

private fun findFilamentEntryInDb(db: SQLiteDatabase, filaId: String, rawColorCode: String): FilamentColorEntry? {
    val colorCode = normalizeBambuColorCode(rawColorCode)
    if (filaId.isBlank() || colorCode.isBlank()) return null
    return findBambuFilamentMatch(
        queryFilamentEntriesInDb(db, "fila_id = ? AND color_code = ?", arrayOf(filaId, colorCode)),
        filaId,
        colorCode
    )
}

private fun findFilamentEntryByFilaColorCodeInDb(
    db: SQLiteDatabase,
    filaId: String,
    filaColorCode: String
): FilamentColorEntry? {
    if (filaId.isBlank() || filaColorCode.isBlank()) return null
    return queryFilamentEntriesInDb(
        db,
        "fila_id = ? AND fila_color_code = ?",
        arrayOf(filaId, filaColorCode)
    ).firstOrNull()
}

private fun queryFilamentEntriesInDb(
    db: SQLiteDatabase,
    selection: String,
    args: Array<String>
): List<FilamentColorEntry> {
    val list = mutableListOf<FilamentColorEntry>()
    db.query(
        FILAMENT_TABLE,
        arrayOf(
            "fila_color_code",
            "color_code",
            "fila_id",
            "fila_color_type",
            "fila_type",
            "fila_detailed_type",
            "color_name_zh",
            "color_name_en",
            "color_values",
            "color_count"
        ),
        selection,
        args,
        null,
        null,
        "fila_color_code ASC"
    ).use { c ->
        while (c.moveToNext()) {
            val cv = c.getString(8)?.split(',')
                ?.map { normalizeColorValue(it.trim()) }?.filter { it.isNotEmpty() }
                ?: emptyList()
            list.add(
                FilamentColorEntry(
                    filaColorCode = c.getString(0).orEmpty(),
                    colorCode = c.getString(1).orEmpty(),
                    filaId = c.getString(2).orEmpty(),
                    colorType = c.getString(3).orEmpty(),
                    filaType = c.getString(4).orEmpty(),
                    filaDetailedType = c.getString(5).orEmpty(),
                    colorNameZh = c.getString(6).orEmpty(),
                    colorNameEn = c.getString(7).orEmpty(),
                    colorValues = cv,
                    colorCount = c.getInt(9)
                )
            )
        }
    }
    return list
}

internal fun syncCrealityMaterialDatabase(context: Context, dbHelper: FilamentDbHelper) {
    val externalDir = context.getExternalFilesDir(null) ?: return
    val externalFile = File(externalDir, CREALITY_MATERIAL_FILE)
    if (!externalFile.exists()) {
        try {
            context.assets.open(CREALITY_MATERIAL_FILE).use { i ->
                externalFile.outputStream().use { o -> i.copyTo(o) }
            }
        } catch (_: IOException) { return }
    }
    if (!externalFile.exists()) return
    val jsonText = try { externalFile.readText(Charsets.UTF_8) } catch (_: IOException) { return }
    val db = dbHelper.writableDatabase
    val fileHash = jsonText.hashCode().toString()
    val storedHash = dbHelper.getMetaValue(db, "creality_material_hash")
    if (storedHash == fileHash) return
    try {
        val materials = JSONObject(jsonText).optJSONArray("materials") ?: return
        db.beginTransaction()
        try {
            db.delete(CREALITY_MATERIAL_TABLE, null, null)
            val values = ContentValues()
            for (i in 0 until materials.length()) {
                val m = materials.getJSONObject(i)
                values.clear()
                values.put("material_id", m.optString("id"))
                values.put("brand", m.optString("brand"))
                values.put("material_type", m.optString("meterialType"))
                values.put("name", m.optString("name"))
                values.put("min_temp", m.optInt("minTemp"))
                values.put("max_temp", m.optInt("maxTemp"))
                values.put("diameter", m.optString("diameter"))
                db.insertWithOnConflict(CREALITY_MATERIAL_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            dbHelper.setMetaValue(db, "creality_material_hash", fileHash)
            db.setTransactionSuccessful()
            logDebug("创想三维耗材数据写入完成: ${materials.length()} 条")
        } finally {
            db.endTransaction()
        }
    } catch (e: Exception) {
        logDebug("同步创想三维耗材数据失败: ${e.message}")
    }
}

private fun readFilamentJsonFromExternal(context: Context): FilamentJsonSource? {
    val externalDir = context.getExternalFilesDir(null) ?: return null
    val externalFile = File(externalDir, FILAMENT_JSON_NAME)
    try {
        val assetBytes = context.assets.open(FILAMENT_JSON_NAME).use { it.readBytes() }
        val prefs = context.getSharedPreferences("config_asset_hashes", Context.MODE_PRIVATE)
        val storedAssetHash = prefs.getString("color_codes_asset_hash", null)
        val assetHash = NetworkUtils.calculateHash(assetBytes)
        if (!externalFile.exists() || storedAssetHash != assetHash) {
            externalFile.outputStream().use { it.write(assetBytes) }
            prefs.edit().putString("color_codes_asset_hash", assetHash).apply()
        }
    } catch (_: IOException) {
        if (!externalFile.exists()) return null
    }
    if (!externalFile.exists()) return null
    val jsonText = try {
        externalFile.readText(Charsets.UTF_8)
    } catch (_: IOException) {
        return null
    }
    return FilamentJsonSource(jsonText, externalFile.lastModified())
}

private fun readFilamentTypeMappingFromExternal(context: Context): FilamentJsonSource? {
    val externalDir = context.getExternalFilesDir(null) ?: return null
    val externalFile = File(externalDir, FILAMENTS_TYPE_MAPPING_FILE)
    try {
        val assetBytes = context.assets.open(FILAMENTS_TYPE_MAPPING_FILE).use { it.readBytes() }
        val prefs = context.getSharedPreferences("config_asset_hashes", Context.MODE_PRIVATE)
        val storedAssetHash = prefs.getString("type_mapping_asset_hash", null)
        val assetHash = NetworkUtils.calculateHash(assetBytes)
        if (!externalFile.exists() || storedAssetHash != assetHash) {
            externalFile.outputStream().use { it.write(assetBytes) }
            prefs.edit().putString("type_mapping_asset_hash", assetHash).apply()
        }
    } catch (_: IOException) {
        if (!externalFile.exists()) return null
    }
    if (!externalFile.exists()) return null
    val jsonText = try {
        externalFile.readText(Charsets.UTF_8)
    } catch (_: IOException) {
        return null
    }
    return FilamentJsonSource(jsonText, externalFile.lastModified())
}

private fun parseFilamentEntries(
    jsonText: String,
    typeGroups: Map<String, List<String>>
): List<FilamentColorEntry> {
    val root = try {
        JSONObject(jsonText)
    } catch (_: Exception) {
        return emptyList()
    }
    val data = root.optJSONArray("data") ?: JSONArray()
    val entries = ArrayList<FilamentColorEntry>(data.length())
    for (i in 0 until data.length()) {
        val item = data.optJSONObject(i) ?: continue
        val filaId = item.optString("fila_id")
        if (filaId.isBlank()) {
            continue
        }
        val colorCode = normalizeBambuColorCode(item.optString("color_code"))
        if (colorCode.isBlank()) {
            continue
        }
        val detailedType = item.optString("fila_type")
        val baseType = resolveBambuBaseFilamentType(detailedType, typeGroups)
        val colorNames = item.optJSONObject("fila_color_name")
        val colorNameZh = resolveColorName(colorNames, "zh")
        val colorNameEn = resolveColorName(colorNames, "en")
        val colorsArray = item.optJSONArray("fila_color")
        val colorValues = ArrayList<String>()
        if (colorsArray != null) {
            for (j in 0 until colorsArray.length()) {
                val value = normalizeColorValue(colorsArray.optString(j))
                if (value.isNotBlank()) {
                    colorValues.add(value)
                }
            }
        }
        entries.add(
            FilamentColorEntry(
                filaColorCode = item.optString("fila_color_code"),
                colorCode = colorCode,
                filaId = filaId,
                colorType = item.optString("fila_color_type"),
                filaType = baseType,
                filaDetailedType = detailedType,
                colorNameZh = colorNameZh,
                colorNameEn = colorNameEn,
                colorValues = colorValues.toList(),
                colorCount = colorValues.size
            )
        )
    }
    return entries
}

private fun parseFilamentTypeMappingEntries(jsonText: String): List<FilamentTypeMappingEntry> {
    val root = try {
        JSONObject(jsonText)
    } catch (_: Exception) {
        return emptyList()
    }
    val entries = ArrayList<FilamentTypeMappingEntry>()
    val keys = root.keys()
    while (keys.hasNext()) {
        val baseType = keys.next()
        val specificTypes = root.optJSONArray(baseType)
        if (specificTypes != null) {
            for (i in 0 until specificTypes.length()) {
                val specificType = specificTypes.optString(i)
                if (specificType.isNotBlank()) {
                    entries.add(
                        FilamentTypeMappingEntry(
                            baseType = baseType,
                            specificType = specificType
                        )
                    )
                }
            }
        }
    }
    return entries
}

private fun resolveColorName(colorNames: JSONObject?, language: String): String {
    if (colorNames == null) {
        return ""
    }
    val normalized = language.lowercase(Locale.US)
    val direct = colorNames.optString(normalized).orEmpty()
    if (direct.isNotBlank()) {
        return direct
    }
    val fallback = colorNames.optString("en").orEmpty()
    if (fallback.isNotBlank()) {
        return fallback
    }
    val zh = colorNames.optString("zh").orEmpty()
    if (zh.isNotBlank()) {
        return zh
    }
    val keys = colorNames.keys()
    if (keys.hasNext()) {
        val firstKey = keys.next()
        return colorNames.optString(firstKey).orEmpty()
    }
    return ""
}
