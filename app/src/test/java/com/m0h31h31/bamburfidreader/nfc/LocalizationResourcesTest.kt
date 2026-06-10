package com.m0h31h31.bamburfidreader.nfc

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalizationResourcesTest {
    private val requiredKeys = listOf(
        "bambu_nfc_uid_missing",
        "bambu_nfc_mifare_unsupported",
        "bambu_nfc_tag_expired",
        "bambu_nfc_format_success",
        "bambu_nfc_write_success",
        "bambu_nfc_verify_success",
        "bambu_nfc_unexpected_read_result",
        "nfc_compat_test_running",
        "nfc_compat_mode_title",
        "nfc_compat_mode_changed_format",
        "nfc_compat_mode_fast",
        "nfc_compat_mode_balanced",
        "nfc_compat_mode_stable",
        "nfc_compat_mode_fast_desc",
        "nfc_compat_mode_balanced_desc",
        "nfc_compat_mode_stable_desc",
        "nfc_compat_write_ready",
        "nfc_compat_read_ready",
        "nfc_compat_cancel_test",
        "nfc_compat_read_test",
        "nfc_compat_write_test",
        "nfc_compat_success_format"
    )

    @Test
    fun newNfcUserTextExistsInDefaultEnglishAndSimplifiedChineseResources() {
        val resourceFiles = listOf(
            File("src/main/res/values/strings.xml"),
            File("src/main/res/values-en/strings.xml"),
            File("src/main/res/values-zh-rCN/strings.xml"),
            File("src/main/res/values-zh/strings.xml")
        )

        for (file in resourceFiles) {
            val text = file.readText()
            for (key in requiredKeys) {
                assertTrue("${file.path} missing $key", text.contains("name=\"$key\""))
            }
        }
    }

    @Test
    fun agentsRequiresNewTextToUseLocalizationResources() {
        val agents = File("../AGENTS.md").readText()

        assertTrue(
            agents.contains("新增用户可见文本") &&
                agents.contains("strings.xml") &&
                agents.contains("values-en") &&
                agents.contains("values-zh-rCN")
        )
    }
}
