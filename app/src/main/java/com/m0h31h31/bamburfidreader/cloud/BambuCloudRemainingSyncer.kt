package com.m0h31h31.bamburfidreader.cloud

import android.content.ContentValues
import android.content.Context
import com.m0h31h31.bamburfidreader.data.FilamentDbHelper
import com.m0h31h31.bamburfidreader.data.TRAY_UID_TABLE
import java.util.Locale

data class BambuCloudRemainingSyncResult(
    val updatedCount: Int,
    val skippedCount: Int
)

object BambuCloudRemainingSyncer {
    fun syncToLocalInventory(
        context: Context,
        filaments: List<BambuCloudFilament>
    ): BambuCloudRemainingSyncResult {
        val dbHelper = FilamentDbHelper(context.applicationContext)
        var updated = 0
        var skipped = 0
        try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                filaments.forEach { filament ->
                    val trayUid = normalizeTrayUid(filament.rfid)
                    val percent = calculateRemainingPercent(
                        netWeightGrams = filament.netWeightGrams,
                        totalNetWeightGrams = filament.totalNetWeightGrams
                    )
                    if (trayUid.isBlank() || percent == null) {
                        skipped += 1
                        return@forEach
                    }
                    val values = ContentValues().apply {
                        put("remaining_percent", percent)
                        put("remaining_grams", filament.netWeightGrams.coerceAtLeast(0))
                        put("total_weight_grams", filament.totalNetWeightGrams.coerceAtLeast(0))
                    }
                    val changed = db.update(
                        TRAY_UID_TABLE,
                        values,
                        "UPPER(tray_uid) = ?",
                        arrayOf(trayUid)
                    )
                    if (changed > 0) {
                        updated += changed
                    } else {
                        skipped += 1
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            dbHelper.close()
        }
        return BambuCloudRemainingSyncResult(
            updatedCount = updated,
            skippedCount = skipped
        )
    }

    internal fun normalizeTrayUid(value: String): String {
        return value.trim().uppercase(Locale.US)
    }

    internal fun calculateRemainingPercent(
        netWeightGrams: Int,
        totalNetWeightGrams: Int
    ): Float? {
        if (totalNetWeightGrams <= 0) return null
        val raw = netWeightGrams.coerceAtLeast(0).toFloat() / totalNetWeightGrams.toFloat() * 100f
        return (Math.round(raw * 10f) / 10f).coerceIn(0f, 100f)
    }
}
