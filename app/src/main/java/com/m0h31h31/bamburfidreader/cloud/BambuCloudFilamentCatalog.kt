package com.m0h31h31.bamburfidreader.cloud

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.m0h31h31.bamburfidreader.data.FILAMENT_TABLE
import com.m0h31h31.bamburfidreader.data.FilamentDbHelper
import java.util.Locale

data class BambuCloudFilamentCatalogInfo(
    val colorName: String,
    val filamentNumber: String
)

object BambuCloudFilamentCatalog {
    fun lookup(
        context: Context,
        filament: BambuCloudFilament
    ): BambuCloudFilamentCatalogInfo? {
        val colorCode = normalizeTrayColorCode(filament.trayIdName)
        if (colorCode.isBlank()) return null
        val useChinese = context.resources.configuration.locales[0]
            ?.language
            ?.equals(Locale.CHINESE.language, ignoreCase = true) == true
        val dbHelper = FilamentDbHelper(context.applicationContext)
        return try {
            val db = dbHelper.readableDatabase
            val selection: String
            val args: Array<String>
            if (filament.filamentId.isNotBlank()) {
                selection = "fila_id = ? AND color_code = ?"
                args = arrayOf(filament.filamentId, colorCode)
            } else {
                selection = "color_code = ?"
                args = arrayOf(colorCode)
            }
            queryCatalogInfo(db, selection, args, useChinese)
                ?: if (filament.filamentId.isNotBlank()) {
                    queryCatalogInfo(db, "color_code = ?", arrayOf(colorCode), useChinese)
                } else {
                    null
                }
        } finally {
            dbHelper.close()
        }
    }

    internal fun normalizeTrayColorCode(trayIdName: String): String {
        val rawCode = trayIdName
            .substringAfter('-', missingDelimiterValue = trayIdName)
            .trim()
            .uppercase(Locale.US)
        if (rawCode.length != 3) return rawCode
        val prefix = rawCode.first()
        val numeric = rawCode.drop(1).toIntOrNull() ?: return rawCode
        return "$prefix$numeric"
    }

    private fun queryCatalogInfo(
        db: SQLiteDatabase,
        selection: String,
        args: Array<String>,
        useChinese: Boolean
    ): BambuCloudFilamentCatalogInfo? {
        val cursor = db.query(
            FILAMENT_TABLE,
            arrayOf("color_name_zh", "color_name_en", "fila_color_code"),
            selection,
            args,
            null,
            null,
            "fila_id ASC, fila_color_code ASC",
            "1"
        )
        cursor.use {
            if (!it.moveToFirst()) return null
            val zh = it.getString(0).orEmpty()
            val en = it.getString(1).orEmpty()
            val colorName = if (useChinese) {
                zh.ifBlank { en }
            } else {
                en.ifBlank { zh }
            }
            return BambuCloudFilamentCatalogInfo(
                colorName = colorName,
                filamentNumber = it.getString(2).orEmpty()
            )
        }
    }
}
