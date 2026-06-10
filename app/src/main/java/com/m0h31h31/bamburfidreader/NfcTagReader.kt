package com.m0h31h31.bamburfidreader

import android.content.Context
import android.nfc.Tag
import com.m0h31h31.bamburfidreader.nfc.BambuMifareOperator
import com.m0h31h31.bamburfidreader.nfc.BambuNfcFailureReason
import com.m0h31h31.bamburfidreader.nfc.BambuNfcOperation
import com.m0h31h31.bamburfidreader.nfc.BambuNfcResult
import com.m0h31h31.bamburfidreader.nfc.NfcCompatibilityConfig
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

private const val KEY_LENGTH_BYTES = 6
private const val SECTOR_COUNT = 16

private val HKDF_SALT = byteArrayOf(
    0x9a.toByte(), 0x75.toByte(), 0x9c.toByte(), 0xf2.toByte(),
    0xc4.toByte(), 0xf7.toByte(), 0xca.toByte(), 0xff.toByte(),
    0x22.toByte(), 0x2c.toByte(), 0xb9.toByte(), 0x76.toByte(),
    0x9b.toByte(), 0x41.toByte(), 0xbc.toByte(), 0x96.toByte()
)

private val INFO_A = "RFID-A\u0000".toByteArray(Charsets.US_ASCII)
private val INFO_B = "RFID-B\u0000".toByteArray(Charsets.US_ASCII)

data class RawTagReadData(
    val uidHex: String,
    val keyA0Hex: String,
    val keyB0Hex: String,
    val keyA1Hex: String,
    val keyB1Hex: String,
    val sectorKeys: List<Pair<ByteArray?, ByteArray?>>,
    val rawBlocks: List<ByteArray?>,
    val errors: List<String>
)

enum class RawTagReadFailureReason {
    UID_MISSING,
    MIFARE_UNSUPPORTED,
    EXCEPTION
}

sealed class RawTagReadResult {
    data class Success(val data: RawTagReadData) : RawTagReadResult()
    data class Failure(
        val reason: RawTagReadFailureReason,
        val message: String,
        val uidHex: String = "",
        val keyA0Hex: String = "",
        val keyB0Hex: String = "",
        val keyA1Hex: String = "",
        val keyB1Hex: String = ""
    ) : RawTagReadResult()
}

object NfcTagReader {
    fun readRaw(
        tag: Tag,
        readAllSectors: Boolean,
        context: Context,
        compatibilityConfig: NfcCompatibilityConfig = NfcCompatibilityConfig.default(),
        logger: (String) -> Unit,
        appendLog: (String, String) -> Unit
    ): RawTagReadResult {
        return when (val result = BambuMifareOperator.run(
            tag = tag,
            config = compatibilityConfig,
            operation = BambuNfcOperation.ReadRaw(readAllSectors),
            context = context,
            logger = logger,
            appendLog = appendLog
        )) {
            is BambuNfcResult.RawRead -> RawTagReadResult.Success(result.data)
            is BambuNfcResult.Failure -> RawTagReadResult.Failure(
                reason = when (result.reason) {
                    BambuNfcFailureReason.UID_MISSING -> RawTagReadFailureReason.UID_MISSING
                    BambuNfcFailureReason.MIFARE_UNSUPPORTED -> RawTagReadFailureReason.MIFARE_UNSUPPORTED
                    BambuNfcFailureReason.EXCEPTION -> RawTagReadFailureReason.EXCEPTION
                },
                message = result.message,
                uidHex = result.uidHex,
                keyA0Hex = result.keyA0Hex,
                keyB0Hex = result.keyB0Hex,
                keyA1Hex = result.keyA1Hex,
                keyB1Hex = result.keyB1Hex
            )

            is BambuNfcResult.Message -> RawTagReadResult.Failure(
                reason = RawTagReadFailureReason.EXCEPTION,
                message = result.message
            )
        }
    }
}

fun deriveBambuKeys(uid: ByteArray): List<Pair<ByteArray, ByteArray>> {
    val keysA = deriveKeys(uid, INFO_A)
    val keysB = deriveKeys(uid, INFO_B)
    return keysA.zip(keysB)
}

private fun deriveKeys(uid: ByteArray, info: ByteArray): List<ByteArray> {
    val prk = hkdfExtract(HKDF_SALT, uid)
    val okm = hkdfExpand(prk, info, KEY_LENGTH_BYTES * SECTOR_COUNT)
    val keys = ArrayList<ByteArray>(SECTOR_COUNT)
    for (i in 0 until SECTOR_COUNT) {
        val start = i * KEY_LENGTH_BYTES
        keys.add(okm.copyOfRange(start, start + KEY_LENGTH_BYTES))
    }
    return keys
}

private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(salt, "HmacSHA256"))
    return mac.doFinal(ikm)
}

private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    val hashLen = mac.macLength
    val blocks = ceil(length.toDouble() / hashLen.toDouble()).toInt()
    var t = ByteArray(0)
    val output = ByteArrayOutputStream()
    for (i in 1..blocks) {
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(t)
        mac.update(info)
        mac.update(i.toByte())
        t = mac.doFinal()
        output.write(t)
    }
    return output.toByteArray().copyOf(length)
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { "%02X".format(Locale.US, it) }
