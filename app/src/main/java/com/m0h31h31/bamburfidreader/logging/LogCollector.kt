package com.m0h31h31.bamburfidreader.logging

import android.content.Context
import android.util.Log
import com.m0h31h31.bamburfidreader.R
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val LOG_TAG = "BambuRfidReader"
private const val LOG_DIR_NAME = "logs"
private const val LOG_FILE_NAME = "bambu_rfid.log"

object LogCollector {
    private val lock = Any()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun append(context: Context?, level: String, message: String) {
        val targetContext = context ?: appContext ?: return
        val baseDir = targetContext.getExternalFilesDir(null) ?: targetContext.filesDir
        val logDir = File(baseDir, LOG_DIR_NAME)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        val line = "${formatter.format(Date())} [$level] $message\n"
        synchronized(lock) {
            File(logDir, LOG_FILE_NAME).appendText(line, Charsets.UTF_8)
        }
    }

    fun packageLogs(context: Context): String {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val logDir = File(baseDir, LOG_DIR_NAME)
        if (!logDir.exists()) {
            return context.getString(R.string.log_none_to_pack)
        }
        val logFiles = logDir.listFiles { file -> file.isFile }?.toList().orEmpty()
        if (logFiles.isEmpty()) {
            return context.getString(R.string.log_none_to_pack)
        }
        val archiveName =
            "logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.zip"
        val archive = File(baseDir, archiveName)
        return try {
            ZipOutputStream(archive.outputStream().buffered()).use { zip ->
                logFiles.forEach { file ->
                    FileInputStream(file).use { input ->
                        zip.putNextEntry(ZipEntry("${LOG_DIR_NAME}/${file.name}"))
                        input.copyTo(zip)
                        zip.closeEntry()
                    }
                }
            }
            context.getString(R.string.log_packed_format, archive.absolutePath)
        } catch (e: Exception) {
            logDebug("Log packaging failed: ${e.message}")
            context.getString(R.string.log_pack_failed)
        }
    }
}

fun logDebug(message: String) {
    Log.d(LOG_TAG, message)
    LogCollector.append(null, "D", message)
}
