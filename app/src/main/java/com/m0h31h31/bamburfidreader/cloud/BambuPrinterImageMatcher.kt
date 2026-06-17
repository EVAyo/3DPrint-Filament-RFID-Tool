package com.m0h31h31.bamburfidreader.cloud

object BambuPrinterImageMatcher {
    const val ASSET_DIR = "BBL_Printer_IMG"

    fun matchAssetName(
        printer: BambuCloudPrinter,
        assetNames: List<String>
    ): String? {
        val normalizedAssets = assetNames.map { assetName ->
            assetName to normalizeAssetName(assetName)
        }
        val exactCandidates = listOf(
            printer.productName,
            printer.modelName,
            printer.deviceName
        ).map(::normalize)
            .filter { it.isNotBlank() }

        exactCandidates.forEach { candidate ->
            normalizedAssets.firstOrNull { (_, normalizedAsset) ->
                normalizedAsset == candidate
            }?.let { return it.first }
        }

        exactCandidates.forEach { candidate ->
            normalizedAssets.firstOrNull { (_, normalizedAsset) ->
                candidate.contains(normalizedAsset) || normalizedAsset.contains(candidate)
            }?.let { return it.first }
        }

        return null
    }

    private fun normalizeAssetName(assetName: String): String {
        return normalize(
            assetName
                .substringBeforeLast('.')
                .removePrefix("Bambu Lab ")
                .removeSuffix("_cover")
        )
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace("bambu lab", "")
            .filter { it.isLetterOrDigit() }
    }
}
