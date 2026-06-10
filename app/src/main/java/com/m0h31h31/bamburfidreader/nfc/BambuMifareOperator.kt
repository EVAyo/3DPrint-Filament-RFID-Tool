package com.m0h31h31.bamburfidreader.nfc

import android.content.Context
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import androidx.annotation.StringRes
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.RawTagReadData
import com.m0h31h31.bamburfidreader.deriveBambuKeys
import java.io.IOException
import java.util.Locale

private const val BAMBU_SECTOR_COUNT = 16

sealed class BambuNfcOperation {
    data class ReadRaw(val readAllSectors: Boolean) : BambuNfcOperation()
    data object FormatToDefaultFf : BambuNfcOperation()
    data class WriteDumpWithFf(val sourceBlocks: List<ByteArray?>) : BambuNfcOperation()
    data class FormatThenWriteDump(val sourceBlocks: List<ByteArray?>) : BambuNfcOperation()
    data class VerifyDump(val sourceBlocks: List<ByteArray?>) : BambuNfcOperation()
}

sealed class BambuNfcResult {
    data class RawRead(val data: RawTagReadData) : BambuNfcResult()
    data class Message(val success: Boolean, val message: String) : BambuNfcResult()
    data class Failure(
        val message: String,
        val reason: BambuNfcFailureReason = BambuNfcFailureReason.EXCEPTION,
        val uidHex: String = "",
        val keyA0Hex: String = "",
        val keyB0Hex: String = "",
        val keyA1Hex: String = "",
        val keyB1Hex: String = "",
        val staleTag: Boolean = false
    ) : BambuNfcResult()
}

enum class BambuNfcFailureReason {
    UID_MISSING,
    MIFARE_UNSUPPORTED,
    EXCEPTION
}

data class BambuTrailerResetStage(
    val requiredKeyB: ByteArray,
    val trailer: ByteArray
)

object BambuFormatPlanner {
    private val defaultAccess = byteArrayOf(0xFF.toByte(), 0x07, 0x80.toByte(), 0x69)

    fun defaultFfTrailer(): ByteArray {
        return buildTrailer(
            keyA = ByteArray(6) { 0xFF.toByte() },
            access = defaultAccess,
            keyB = ByteArray(6) { 0xFF.toByte() }
        )
    }

    fun trailerResetStages(derivedKeyA: ByteArray, derivedKeyB: ByteArray): List<BambuTrailerResetStage> {
        return listOf(
            BambuTrailerResetStage(
                requiredKeyB = derivedKeyB,
                trailer = buildTrailer(derivedKeyA, defaultAccess, derivedKeyB)
            ),
            BambuTrailerResetStage(
                requiredKeyB = derivedKeyB,
                trailer = defaultFfTrailer()
            )
        )
    }

    fun canResetTrailer(result: MifareClassicSession.AuthResult): Boolean {
        return result is MifareClassicSession.AuthResult.Success &&
            result.keyType == MifareClassicSession.AuthKeyType.KEY_B
    }

    fun defaultAccessBytes(): ByteArray = defaultAccess.copyOf()

    fun buildTrailer(keyA: ByteArray, access: ByteArray, keyB: ByteArray): ByteArray {
        require(keyA.size == 6) { "KeyA must be 6 bytes" }
        require(access.size == 4) { "Access bytes must be 4 bytes" }
        require(keyB.size == 6) { "KeyB must be 6 bytes" }
        return ByteArray(16).apply {
            System.arraycopy(keyA, 0, this, 0, 6)
            System.arraycopy(access, 0, this, 6, 4)
            System.arraycopy(keyB, 0, this, 10, 6)
        }
    }
}

object BambuMifareOperator {
    fun run(
        tag: Tag,
        config: NfcCompatibilityConfig,
        operation: BambuNfcOperation,
        context: Context,
        logger: (String) -> Unit = {},
        appendLog: (String, String) -> Unit = { _, _ -> },
        onStatus: (String) -> Unit = {}
    ): BambuNfcResult {
        val uid = tag.id ?: return BambuNfcResult.Failure(
            message = context.nfcText(R.string.bambu_nfc_uid_missing),
            reason = BambuNfcFailureReason.UID_MISSING
        )
        val uidHex = uid.toHex()
        val sectorKeys = runCatching { deriveBambuKeys(uid) }.getOrElse { error ->
            return BambuNfcResult.Failure(
                message = context.nfcText(
                    R.string.bambu_nfc_key_derivation_failed_format,
                    error.message.orEmpty()
                ),
                uidHex = uidHex
            )
        }
        val keyA0Hex = sectorKeys.getOrNull(0)?.first?.toHex().orEmpty()
        val keyB0Hex = sectorKeys.getOrNull(0)?.second?.toHex().orEmpty()
        val keyA1Hex = sectorKeys.getOrNull(1)?.first?.toHex().orEmpty()
        val keyB1Hex = sectorKeys.getOrNull(1)?.second?.toHex().orEmpty()
        logDerivedKeys(uidHex, uid.size, sectorKeys, config, logger, appendLog)
        if (config.postKeyDerivationDelayMs > 0) {
            appendLog("D", "Waiting ${config.postKeyDerivationDelayMs}ms after Bambu key derivation before MIFARE access")
            Thread.sleep(config.postKeyDerivationDelayMs)
        }

        val mifare = MifareClassic.get(tag) ?: return BambuNfcResult.Failure(
            message = context.nfcText(
                R.string.bambu_nfc_mifare_unsupported
            ),
            reason = BambuNfcFailureReason.MIFARE_UNSUPPORTED,
            uidHex = uidHex,
            keyA0Hex = keyA0Hex,
            keyB0Hex = keyB0Hex,
            keyA1Hex = keyA1Hex,
            keyB1Hex = keyB1Hex
        )

        return try {
            mifare.connect()
            MifareClassicSession.applyTimeout(mifare, config.mifareTimeoutMs, appendLog)
            if (config.postConnectDelayMs > 0) Thread.sleep(config.postConnectDelayMs)
            appendLog("D", "Bambu operator connect OK UID=$uidHex sectorCount=${mifare.sectorCount}")

            when (operation) {
                is BambuNfcOperation.ReadRaw -> readRaw(
                    mifare = mifare,
                    uidHex = uidHex,
                    sectorKeys = sectorKeys,
                    readAllSectors = operation.readAllSectors,
                    config = config,
                    context = context,
                    logger = logger,
                    appendLog = appendLog
                )

                BambuNfcOperation.FormatToDefaultFf -> formatToDefaultFf(
                    mifare = mifare,
                    sectorKeys = sectorKeys,
                    config = config,
                    context = context,
                    appendLog = appendLog,
                    onStatus = onStatus
                )

                is BambuNfcOperation.WriteDumpWithFf -> writeDumpWithFf(
                    mifare = mifare,
                    sourceBlocks = operation.sourceBlocks,
                    config = config,
                    context = context,
                    appendLog = appendLog,
                    onStatus = onStatus
                )

                is BambuNfcOperation.FormatThenWriteDump -> {
                    val formatResult = formatToDefaultFf(
                        mifare = mifare,
                        sectorKeys = sectorKeys,
                        config = config,
                        context = context,
                        appendLog = appendLog,
                        onStatus = onStatus
                    )
                    if (formatResult is BambuNfcResult.Message && formatResult.success) {
                        writeDumpWithFf(
                            mifare = mifare,
                            sourceBlocks = operation.sourceBlocks,
                            config = config,
                            context = context,
                            appendLog = appendLog,
                            onStatus = onStatus
                        )
                    } else {
                        formatResult
                    }
                }

                is BambuNfcOperation.VerifyDump -> verifyDump(
                    mifare = mifare,
                    sourceBlocks = operation.sourceBlocks,
                    config = config,
                    context = context,
                    appendLog = appendLog
                )
            }
        } catch (e: Exception) {
            appendLog("E", "Bambu operator failed: ${e.javaClass.simpleName}: ${e.message}")
            BambuNfcResult.Failure(
                message = if (MifareClassicSession.isStaleTagException(e)) {
                    context.nfcText(R.string.bambu_nfc_tag_expired)
                } else {
                    e.message.orEmpty()
                },
                uidHex = uidHex,
                keyA0Hex = keyA0Hex,
                keyB0Hex = keyB0Hex,
                keyA1Hex = keyA1Hex,
                keyB1Hex = keyB1Hex,
                staleTag = MifareClassicSession.isStaleTagException(e)
            )
        } finally {
            try {
                mifare.close()
                appendLog("I", "Bambu operator closed MIFARE Classic")
            } catch (_: IOException) {
            }
        }
    }

    private fun logDerivedKeys(
        uidHex: String,
        uidLength: Int,
        sectorKeys: List<Pair<ByteArray, ByteArray>>,
        config: NfcCompatibilityConfig,
        logger: (String) -> Unit,
        appendLog: (String, String) -> Unit
    ) {
        val preview = sectorKeys.take(4).mapIndexed { sector, keys ->
            "S$sector A=${keys.first.toHex()} B=${keys.second.toHex()}"
        }.joinToString(separator = " | ")
        val summary = "Bambu key derivation UID=$uidHex uidBytes=$uidLength sectors=${sectorKeys.size} mode=${config.mode} postKeyDelay=${config.postKeyDerivationDelayMs}ms reconnectAfterFailedAuth=${config.reconnectAfterFailedAuth} $preview"
        logger(summary)
        appendLog("I", summary)
        sectorKeys.forEachIndexed { sector, keys ->
            appendLog("D", "Bambu derived key S$sector KeyA=${keys.first.toHex()} KeyB=${keys.second.toHex()}")
        }
    }

    private fun readRaw(
        mifare: MifareClassic,
        uidHex: String,
        sectorKeys: List<Pair<ByteArray, ByteArray>>,
        readAllSectors: Boolean,
        config: NfcCompatibilityConfig,
        context: Context,
        logger: (String) -> Unit,
        appendLog: (String, String) -> Unit
    ): BambuNfcResult {
        logger("UID: $uidHex")
        appendLog("I", "Start Bambu read UID: $uidHex")
        val rawBlocks = MutableList<ByteArray?>(mifare.blockCount) { null }
        val errors = ArrayList<String>()
        val targetSectorCount = minOf(BAMBU_SECTOR_COUNT, mifare.sectorCount)
        val sectorsToRead = if (readAllSectors) {
            0 until targetSectorCount
        } else {
            0..minOf(4, targetSectorCount - 1)
        }

        for (sector in sectorsToRead) {
            val keyA = sectorKeys.getOrNull(sector)?.first
            if (keyA == null) {
                errors.add(context.nfcText(R.string.bambu_nfc_read_missing_keya_format, sector))
                continue
            }
            val auth = authenticate(
                mifare = mifare,
                sector = sector,
                keysA = listOf(keyA),
                keysB = emptyList(),
                config = config,
                appendLog = appendLog
            )
            if (auth is MifareClassicSession.AuthResult.StaleTag) {
                return staleFailure(context, auth.message, uidHex, sectorKeys)
            }
            if (auth !is MifareClassicSession.AuthResult.Success) {
                val error = context.nfcText(R.string.bambu_nfc_read_auth_failed_format, sector)
                logger(error)
                appendLog("W", error)
                if (sector == 0 || sector == 1) errors.add(error)
                continue
            }

            val startBlock = mifare.sectorToBlock(sector)
            val blockCount = mifare.getBlockCountInSector(sector)
            for (offset in 0 until blockCount) {
                val blockIndex = startBlock + offset
                val block = readBlockWithRetry(
                    mifare = mifare,
                    blockIndex = blockIndex,
                    config = config,
                    context = context,
                    appendLog = appendLog
                ) {
                    authenticate(mifare, sector, listOf(keyA), emptyList(), config, appendLog)
                }
                when (block) {
                    is BlockReadResult.Success -> rawBlocks[blockIndex] = block.data
                    is BlockReadResult.Stale -> return staleFailure(context, block.message, uidHex, sectorKeys)
                    is BlockReadResult.Failure -> {
                        val error = context.nfcText(R.string.bambu_nfc_read_block_failed_format,
                            blockIndex,
                            block.message
                        )
                        appendLog("W", error)
                        if (sector == 0 || sector == 1) errors.add(error)
                        break
                    }
                }
                if (config.readInterBlockDelayMs > 0) Thread.sleep(config.readInterBlockDelayMs)
            }
        }

        val nullableSectorKeys = sectorKeys.map { Pair(it.first as ByteArray?, it.second as ByteArray?) }
        return BambuNfcResult.RawRead(
            RawTagReadData(
                uidHex = uidHex,
                keyA0Hex = sectorKeys.getOrNull(0)?.first?.toHex().orEmpty(),
                keyB0Hex = sectorKeys.getOrNull(0)?.second?.toHex().orEmpty(),
                keyA1Hex = sectorKeys.getOrNull(1)?.first?.toHex().orEmpty(),
                keyB1Hex = sectorKeys.getOrNull(1)?.second?.toHex().orEmpty(),
                sectorKeys = nullableSectorKeys,
                rawBlocks = rawBlocks,
                errors = errors
            )
        )
    }

    private fun formatToDefaultFf(
        mifare: MifareClassic,
        sectorKeys: List<Pair<ByteArray, ByteArray>>,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit,
        onStatus: (String) -> Unit
    ): BambuNfcResult {
        if (mifare.sectorCount < BAMBU_SECTOR_COUNT) {
            return BambuNfcResult.Message(
                false,
                context.nfcText(R.string.bambu_nfc_format_insufficient_sectors_format,
                    mifare.sectorCount
                )
            )
        }
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        val zeroBlock = ByteArray(16)
        val originalBlock0 = run {
            val auth = authenticate(mifare, 0, listOf(sectorKeys[0].first), listOf(ffKey), config, appendLog)
            if (auth is MifareClassicSession.AuthResult.StaleTag) return staleMessage(context, auth.message)
            if (auth !is MifareClassicSession.AuthResult.Success) {
                return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_sector0_auth_failed))
            }
            when (val block = readBlockWithRetry(mifare, 0, config, context, appendLog) {
                authenticate(mifare, 0, listOf(sectorKeys[0].first), listOf(ffKey), config, appendLog)
            }) {
                is BlockReadResult.Success -> block.data
                is BlockReadResult.Stale -> return staleMessage(context, block.message)
                is BlockReadResult.Failure -> return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_read_block0_failed))
            }
        }

        val targetSectorCount = minOf(BAMBU_SECTOR_COUNT, mifare.sectorCount)
        for (sector in 0 until targetSectorCount) {
            onStatus(context.nfcText(R.string.bambu_nfc_format_trailer_status_format, sector + 1, targetSectorCount))
            val reset = resetTrailerToDefaultFf(mifare, sector, sectorKeys[sector], config, context, appendLog)
            when (reset) {
                is StepResult.Ok -> Unit
                is StepResult.Stale -> return staleMessage(context, reset.message)
                is StepResult.Failed -> return BambuNfcResult.Message(
                    false,
                    context.nfcText(R.string.bambu_nfc_format_trailer_reset_failed_format,
                        sector,
                        reset.message
                    )
                )
            }
        }

        for (sector in 0 until targetSectorCount) {
            onStatus(context.nfcText(R.string.bambu_nfc_format_clearing_status_format, sector + 1, targetSectorCount))
            val auth = authenticate(mifare, sector, listOf(ffKey), listOf(ffKey), config, appendLog)
            if (auth is MifareClassicSession.AuthResult.StaleTag) return staleMessage(context, auth.message)
            if (auth !is MifareClassicSession.AuthResult.Success) {
                return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_sector_ff_auth_failed_format, sector))
            }
            val startBlock = mifare.sectorToBlock(sector)
            val blockCount = mifare.getBlockCountInSector(sector)
            for (offset in 0 until blockCount) {
                val blockIndex = startBlock + offset
                val isTrailer = offset == blockCount - 1
                if (blockIndex == 0 || isTrailer) continue
                when (val write = writeBlockWithRetry(mifare, blockIndex, zeroBlock, config, context, appendLog) {
                    authenticate(mifare, sector, listOf(ffKey), listOf(ffKey), config, appendLog)
                }) {
                    is StepResult.Ok -> Unit
                    is StepResult.Stale -> return staleMessage(context, write.message)
                    is StepResult.Failed -> return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_zero_write_failed_format, blockIndex))
                }
            }
        }

        for (sector in 0 until targetSectorCount) {
            val auth = authenticate(mifare, sector, listOf(ffKey), listOf(ffKey), config, appendLog)
            if (auth is MifareClassicSession.AuthResult.StaleTag) return staleMessage(context, auth.message)
            if (auth !is MifareClassicSession.AuthResult.Success) {
                return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_verify_ff_auth_failed_format, sector))
            }
            val startBlock = mifare.sectorToBlock(sector)
            val blockCount = mifare.getBlockCountInSector(sector)
            for (offset in 0 until blockCount) {
                val blockIndex = startBlock + offset
                val isTrailer = offset == blockCount - 1
                if (isTrailer) continue
                val block = readBlockWithRetry(mifare, blockIndex, config, context, appendLog) {
                    authenticate(mifare, sector, listOf(ffKey), listOf(ffKey), config, appendLog)
                }
                when (block) {
                    is BlockReadResult.Success -> {
                        if (blockIndex == 0 && !block.data.contentEquals(originalBlock0)) {
                            return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_verify_block0_changed))
                        }
                        if (blockIndex != 0 && !block.data.all { it == 0.toByte() }) {
                            return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_verify_block_not_zero_format, blockIndex))
                        }
                    }
                    is BlockReadResult.Stale -> return staleMessage(context, block.message)
                    is BlockReadResult.Failure -> return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_format_verify_read_failed_format, blockIndex))
                }
            }
        }
        return BambuNfcResult.Message(true, context.nfcText(R.string.bambu_nfc_format_success))
    }

    private fun resetTrailerToDefaultFf(
        mifare: MifareClassic,
        sector: Int,
        sectorKey: Pair<ByteArray, ByteArray>,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit
    ): StepResult {
        val trailerBlock = mifare.sectorToBlock(sector) + mifare.getBlockCountInSector(sector) - 1
        val stages = BambuFormatPlanner.trailerResetStages(sectorKey.first, sectorKey.second)
        for (stage in stages) {
            val auth = authenticate(mifare, sector, emptyList(), listOf(stage.requiredKeyB), config, appendLog)
            if (auth is MifareClassicSession.AuthResult.StaleTag) return StepResult.Stale(auth.message)
            if (!BambuFormatPlanner.canResetTrailer(auth)) {
                return tryResetDefaultTrailerWithFf(mifare, sector, trailerBlock, config, context, appendLog)
            }
            when (val write = writeBlockWithRetry(mifare, trailerBlock, stage.trailer, config, context, appendLog) {
                authenticate(mifare, sector, emptyList(), listOf(stage.requiredKeyB), config, appendLog)
            }) {
                is StepResult.Ok -> Unit
                is StepResult.Stale -> return write
                is StepResult.Failed -> return write
            }
            if (config.writeInterBlockDelayMs > 0) Thread.sleep(config.writeInterBlockDelayMs)
        }
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        val verify = authenticate(mifare, sector, listOf(ffKey), listOf(ffKey), config, appendLog)
        if (verify is MifareClassicSession.AuthResult.StaleTag) return StepResult.Stale(verify.message)
        return if (verify is MifareClassicSession.AuthResult.Success) {
            StepResult.Ok
        } else {
            StepResult.Failed(context.nfcText(R.string.bambu_nfc_trailer_ff_verify_failed))
        }
    }

    private fun tryResetDefaultTrailerWithFf(
        mifare: MifareClassic,
        sector: Int,
        trailerBlock: Int,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit
    ): StepResult {
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        val auth = authenticate(mifare, sector, emptyList(), listOf(ffKey), config, appendLog)
        if (auth is MifareClassicSession.AuthResult.StaleTag) return StepResult.Stale(auth.message)
        if (!BambuFormatPlanner.canResetTrailer(auth)) {
            return StepResult.Failed(context.nfcText(R.string.bambu_nfc_trailer_keyb_auth_failed))
        }
        return writeBlockWithRetry(mifare, trailerBlock, BambuFormatPlanner.defaultFfTrailer(), config, context, appendLog) {
            authenticate(mifare, sector, emptyList(), listOf(ffKey), config, appendLog)
        }
    }

    private fun writeDumpWithFf(
        mifare: MifareClassic,
        sourceBlocks: List<ByteArray?>,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit,
        onStatus: (String) -> Unit
    ): BambuNfcResult {
        if (sourceBlocks.isEmpty()) {
            return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_write_source_empty))
        }
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        val targetSectorCount = minOf(BAMBU_SECTOR_COUNT, mifare.sectorCount)
        for (sector in 0 until targetSectorCount) {
            onStatus(context.nfcText(R.string.bambu_nfc_write_sector_status_format, sector + 1, targetSectorCount))
            val auth = authenticate(mifare, sector, listOf(ffKey), listOf(ffKey), config, appendLog)
            if (auth is MifareClassicSession.AuthResult.StaleTag) return staleMessage(context, auth.message)
            if (auth !is MifareClassicSession.AuthResult.Success) {
                return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_write_sector_ff_auth_failed_format, sector))
            }
            val startBlock = mifare.sectorToBlock(sector)
            val blockCount = mifare.getBlockCountInSector(sector)
            for (offset in 0 until blockCount) {
                val blockIndex = startBlock + offset
                val sourceIndex = sector * 4 + offset
                val data = sourceBlocks.getOrNull(sourceIndex)
                    ?: return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_write_block_missing_format, sourceIndex))
                if (data.size != 16) {
                    return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_write_block_size_format, sourceIndex))
                }
                when (val write = writeBlockWithRetry(mifare, blockIndex, data, config, context, appendLog) {
                    authenticate(mifare, sector, listOf(ffKey), listOf(ffKey), config, appendLog)
                }) {
                    is StepResult.Ok -> Unit
                    is StepResult.Stale -> return staleMessage(context, write.message)
                    is StepResult.Failed -> return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_write_block_error_format, blockIndex))
                }
            }
        }
        return BambuNfcResult.Message(true, context.nfcText(R.string.bambu_nfc_write_success))
    }

    private fun verifyDump(
        mifare: MifareClassic,
        sourceBlocks: List<ByteArray?>,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit
    ): BambuNfcResult {
        if (sourceBlocks.size < 64) {
            return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_verify_insufficient_blocks))
        }
        val targetSectorCount = minOf(BAMBU_SECTOR_COUNT, mifare.sectorCount)
        for (sector in 0 until targetSectorCount) {
            val trailer = sourceBlocks.getOrNull(sector * 4 + 3)
                ?: return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_verify_trailer_missing_format, sector))
            if (trailer.size != 16) {
                return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_verify_trailer_size_format, sector))
            }
            val sourceKeyA = trailer.copyOfRange(0, 6)
            val sourceKeyB = trailer.copyOfRange(10, 16)
            val auth = authenticate(mifare, sector, listOf(sourceKeyA), listOf(sourceKeyB), config, appendLog)
            if (auth is MifareClassicSession.AuthResult.StaleTag) return staleMessage(context, auth.message)
            if (auth !is MifareClassicSession.AuthResult.Success) {
                return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_verify_sector_auth_failed_format, sector))
            }
            val startBlock = mifare.sectorToBlock(sector)
            val blockCount = mifare.getBlockCountInSector(sector)
            for (offset in 0 until blockCount) {
                val blockIndex = startBlock + offset
                val expected = sourceBlocks.getOrNull(sector * 4 + offset)
                    ?: return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_verify_block_missing_format, blockIndex))
                val actual = readBlockWithRetry(mifare, blockIndex, config, context, appendLog) {
                    authenticate(mifare, sector, listOf(sourceKeyA), listOf(sourceKeyB), config, appendLog)
                }
                when (actual) {
                    is BlockReadResult.Success -> {
                        if (!equivalentBlock(blockIndex, expected, actual.data)) {
                            return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_verify_block_mismatch_format, blockIndex))
                        }
                    }
                    is BlockReadResult.Stale -> return staleMessage(context, actual.message)
                    is BlockReadResult.Failure -> return BambuNfcResult.Message(false, context.nfcText(R.string.bambu_nfc_verify_block_read_failed_format, blockIndex))
                }
            }
        }
        return BambuNfcResult.Message(true, context.nfcText(R.string.bambu_nfc_verify_success))
    }

    private fun authenticate(
        mifare: MifareClassic,
        sector: Int,
        keysA: List<ByteArray?>,
        keysB: List<ByteArray?>,
        config: NfcCompatibilityConfig,
        appendLog: (String, String) -> Unit
    ): MifareClassicSession.AuthResult {
        return MifareClassicSession.authenticateSectorWithRetryDetailed(
            mifare = mifare,
            sectorIndex = sector,
            keysA = keysA,
            keysB = keysB,
            retryCount = config.authRetryCount,
            reconnectDelayMs = config.reconnectDelayMs,
            keyOrder = MifareClassicSession.KeyOrder.INTERLEAVED_BY_INDEX,
            ensureConnectedBeforeAttempt = true,
            reconnectAfterFailedAttempt = config.reconnectAfterFailedAuth,
            mifareTimeoutMs = config.mifareTimeoutMs,
            postConnectDelayMs = config.postConnectDelayMs,
            appendLog = appendLog
        )
    }

    private fun readBlockWithRetry(
        mifare: MifareClassic,
        blockIndex: Int,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit,
        reauthenticate: () -> MifareClassicSession.AuthResult
    ): BlockReadResult {
        for (attempt in 0..config.blockRetryCount) {
            try {
                val raw = mifare.readBlock(blockIndex)
                val data = when {
                    raw.size == 16 -> raw
                    raw.size > 16 -> raw.copyOf(16)
                    else -> return BlockReadResult.Failure(
                        context.nfcText(R.string.bambu_nfc_io_invalid_length_format, raw.size)
                    )
                }
                return BlockReadResult.Success(data)
            } catch (e: Exception) {
                if (MifareClassicSession.isStaleTagException(e)) return BlockReadResult.Stale(e.message.orEmpty())
                appendLog("D", "read block=$blockIndex failed attempt=$attempt: ${e.message}")
                MifareClassicSession.reconnect(
                    mifare = mifare,
                    reconnectDelayMs = config.reconnectDelayMs,
                    mifareTimeoutMs = config.mifareTimeoutMs,
                    postConnectDelayMs = config.postConnectDelayMs,
                    appendLog = appendLog
                )
                val auth = reauthenticate()
                if (auth is MifareClassicSession.AuthResult.StaleTag) return BlockReadResult.Stale(auth.message)
                if (auth !is MifareClassicSession.AuthResult.Success) {
                    return BlockReadResult.Failure(context.nfcText(R.string.bambu_nfc_io_reauth_failed))
                }
            }
        }
        return BlockReadResult.Failure(context.nfcText(R.string.bambu_nfc_io_retry_exhausted))
    }

    private fun writeBlockWithRetry(
        mifare: MifareClassic,
        blockIndex: Int,
        data: ByteArray,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit,
        reauthenticate: () -> MifareClassicSession.AuthResult
    ): StepResult {
        for (attempt in 0..config.blockRetryCount) {
            try {
                if (config.writeInterBlockDelayMs > 0) Thread.sleep(config.writeInterBlockDelayMs)
                mifare.writeBlock(blockIndex, data)
                if (config.writeVerificationDelayMs > 0) Thread.sleep(config.writeVerificationDelayMs)
                if (!config.verifyEachWriteBlock || isTrailerBlock(mifare, blockIndex)) return StepResult.Ok
                val verify = readBlockWithRetry(mifare, blockIndex, config, context, appendLog, reauthenticate)
                when (verify) {
                    is BlockReadResult.Success -> if (verify.data.contentEquals(data)) return StepResult.Ok
                    is BlockReadResult.Stale -> return StepResult.Stale(verify.message)
                    is BlockReadResult.Failure -> Unit
                }
            } catch (e: Exception) {
                if (MifareClassicSession.isStaleTagException(e)) return StepResult.Stale(e.message.orEmpty())
                appendLog("D", "write block=$blockIndex failed attempt=$attempt: ${e.message}")
                MifareClassicSession.reconnect(
                    mifare = mifare,
                    reconnectDelayMs = config.reconnectDelayMs,
                    mifareTimeoutMs = config.mifareTimeoutMs,
                    postConnectDelayMs = config.postConnectDelayMs,
                    appendLog = appendLog
                )
                val auth = reauthenticate()
                if (auth is MifareClassicSession.AuthResult.StaleTag) return StepResult.Stale(auth.message)
                if (auth !is MifareClassicSession.AuthResult.Success) {
                    return StepResult.Failed(context.nfcText(R.string.bambu_nfc_io_reauth_failed))
                }
            }
        }
        return StepResult.Failed(context.nfcText(R.string.bambu_nfc_io_retry_exhausted))
    }

    private fun readBlockWithRetry(
        mifare: MifareClassic,
        blockIndex: Int,
        config: NfcCompatibilityConfig,
        context: Context,
        appendLog: (String, String) -> Unit
    ): BlockReadResult {
        return readBlockWithRetry(mifare, blockIndex, config, context, appendLog) {
            MifareClassicSession.AuthResult.Failure(
                context.nfcText(R.string.bambu_nfc_io_no_reauth_available)
            )
        }
    }

    private fun isTrailerBlock(mifare: MifareClassic, blockIndex: Int): Boolean {
        val sector = mifare.blockToSector(blockIndex)
        val firstBlock = mifare.sectorToBlock(sector)
        return blockIndex == firstBlock + mifare.getBlockCountInSector(sector) - 1
    }

    private fun equivalentBlock(blockIndex: Int, expected: ByteArray, actual: ByteArray): Boolean {
        if (blockIndex % 4 != 3) return expected.contentEquals(actual)
        val e = expected.copyOf()
        val a = actual.copyOf()
        for (i in 0..5) {
            e[i] = 0
            a[i] = 0
        }
        for (i in 10..15) {
            e[i] = 0
            a[i] = 0
        }
        return e.contentEquals(a)
    }

    private fun staleFailure(
        context: Context,
        message: String,
        uidHex: String,
        sectorKeys: List<Pair<ByteArray, ByteArray>>
    ): BambuNfcResult.Failure {
        return BambuNfcResult.Failure(
            message = context.nfcText(R.string.bambu_nfc_tag_expired),
            uidHex = uidHex,
            keyA0Hex = sectorKeys.getOrNull(0)?.first?.toHex().orEmpty(),
            keyB0Hex = sectorKeys.getOrNull(0)?.second?.toHex().orEmpty(),
            keyA1Hex = sectorKeys.getOrNull(1)?.first?.toHex().orEmpty(),
            keyB1Hex = sectorKeys.getOrNull(1)?.second?.toHex().orEmpty(),
            staleTag = true
        ).also {
            if (message.isNotBlank()) {
                // Keep the original Android message in debug logs via callers' appendLog paths.
            }
        }
    }

    private fun staleMessage(context: Context, message: String): BambuNfcResult.Message {
        val text = if (message.isBlank()) {
            context.nfcText(R.string.bambu_nfc_tag_expired)
        } else {
            context.nfcText(R.string.bambu_nfc_tag_expired_detail_format,
                message
            )
        }
        return BambuNfcResult.Message(false, text)
    }
}

private sealed class BlockReadResult {
    data class Success(val data: ByteArray) : BlockReadResult()
    data class Failure(val message: String) : BlockReadResult()
    data class Stale(val message: String) : BlockReadResult()
}

private sealed class StepResult {
    data object Ok : StepResult()
    data class Failed(val message: String) : StepResult()
    data class Stale(val message: String) : StepResult()
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { "%02X".format(Locale.US, it) }

private fun Context.nfcText(@StringRes resId: Int, vararg args: Any): String {
    return getString(resId, *args)
}
