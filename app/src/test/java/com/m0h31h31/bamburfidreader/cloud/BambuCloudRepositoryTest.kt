package com.m0h31h31.bamburfidreader.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BambuCloudRepositoryTest {
    @Test
    fun loginWithPasswordFetchesProfileAndSavesSession() = runBlocking {
        val service = FakeCloudService(
            loginResult = BambuCloudApiResult.Success(
                BambuCloudTokens(
                    accessToken = "access-123",
                    refreshToken = "refresh-123",
                    expiresInSeconds = 60
                )
            ),
            accountResult = BambuCloudApiResult.Success(
                BambuCloudAccount(
                    uid = 42L,
                    name = "Bambu User",
                    handle = "maker",
                    avatarUrl = "",
                    bio = ""
                )
            )
        )
        val store = MemorySessionStore()
        val repository = BambuCloudRepository(service, store, nowMillis = { 1000L })

        val result = repository.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudRepositoryResult.Success)
        assertEquals("access-123", service.profileAccessToken)
        assertEquals(42L, store.savedSession?.account?.uid)
        assertEquals(61000L, store.savedSession?.expiresAtMillis)
    }

    @Test
    fun refreshAccountUpdatesSavedPreferenceProfile() = runBlocking {
        val service = FakeCloudService(
            loginResult = BambuCloudApiResult.Failure("unused"),
            accountResult = BambuCloudApiResult.Success(
                BambuCloudAccount(
                    uid = 99L,
                    name = "Fresh User",
                    handle = "fresh",
                    avatarUrl = "https://example.com/avatar.png",
                    bio = "new bio"
                )
            )
        )
        val store = MemorySessionStore().apply {
            savedSession = BambuCloudSession(
                tokens = BambuCloudTokens(
                    accessToken = "saved-access",
                    refreshToken = "saved-refresh",
                    expiresInSeconds = 60
                ),
                account = BambuCloudAccount(
                    uid = 42L,
                    name = "Old User",
                    handle = "old",
                    avatarUrl = "",
                    bio = ""
                ),
                savedAtMillis = 1000L,
                expiresAtMillis = 61000L
            )
        }
        val repository = BambuCloudRepository(service, store, nowMillis = { 2000L })

        val result = repository.refreshAccount()

        assertTrue(result is BambuCloudRepositoryResult.Success)
        assertEquals("saved-access", service.profileAccessToken)
        assertEquals(99L, store.savedSession?.account?.uid)
        assertEquals("Fresh User", store.savedSession?.account?.name)
        assertEquals("https://example.com/avatar.png", store.savedSession?.account?.avatarUrl)
        assertEquals(1000L, store.savedSession?.savedAtMillis)
        assertEquals(61000L, store.savedSession?.expiresAtMillis)
    }

    @Test
    fun fetchPrintersUsesSavedAccessToken() = runBlocking {
        val service = FakeCloudService(
            loginResult = BambuCloudApiResult.Failure("unused"),
            accountResult = BambuCloudApiResult.Failure("unused"),
            printerResult = BambuCloudApiResult.Success(
                listOf(
                    BambuCloudPrinter(
                        deviceId = "printer-1",
                        deviceName = "Studio X1C",
                        modelName = "BL-P001",
                        productName = "X1 Carbon",
                        online = true,
                        taskId = "task-1",
                        taskName = "AMS Riser",
                        taskStatus = "RUNNING",
                        progress = 42,
                        thumbnailUrl = ""
                    )
                )
            )
        )
        val store = MemorySessionStore().apply {
            savedSession = BambuCloudSession(
                tokens = BambuCloudTokens(
                    accessToken = "saved-access",
                    refreshToken = "saved-refresh",
                    expiresInSeconds = 60
                ),
                account = BambuCloudAccount(
                    uid = 42L,
                    name = "Old User",
                    handle = "old",
                    avatarUrl = "",
                    bio = ""
                ),
                savedAtMillis = 1000L,
                expiresAtMillis = 61000L
            )
        }
        val repository = BambuCloudRepository(service, store, nowMillis = { 2000L })

        val result = repository.fetchPrinters()

        assertTrue(result is BambuCloudPrinterResult.Success)
        assertEquals("saved-access", service.printerAccessToken)
        assertEquals("Studio X1C", (result as BambuCloudPrinterResult.Success).printers.first().deviceName)
    }

    @Test
    fun fetchFilamentsUsesSavedAccessToken() = runBlocking {
        val service = FakeCloudService(
            loginResult = BambuCloudApiResult.Failure("unused"),
            accountResult = BambuCloudApiResult.Failure("unused"),
            filamentResult = BambuCloudApiResult.Success(
                listOf(
                    BambuCloudFilament(
                        id = 1L,
                        createType = "ams",
                        vendor = "Bambu Lab",
                        type = "PLA",
                        name = "PLA Basic",
                        filamentId = "GFA00",
                        rfid = "rfid-1",
                        color = "#FFFFFFFF",
                        colors = listOf("#FFFFFFFF"),
                        netWeightGrams = 760,
                        totalNetWeightGrams = 1000,
                        trayIdName = "A00-D00",
                        inPrinter = true,
                        deviceId = "printer-1",
                        amsSerial = "ams-1",
                        slotId = "0",
                        amsId = 0,
                        deviceName = "P1"
                    )
                )
            )
        )
        val store = MemorySessionStore().apply {
            savedSession = BambuCloudSession(
                tokens = BambuCloudTokens(
                    accessToken = "saved-access",
                    refreshToken = "saved-refresh",
                    expiresInSeconds = 60
                ),
                account = BambuCloudAccount(
                    uid = 42L,
                    name = "Old User",
                    handle = "old",
                    avatarUrl = "",
                    bio = ""
                ),
                savedAtMillis = 1000L,
                expiresAtMillis = 61000L
            )
        }
        val repository = BambuCloudRepository(service, store, nowMillis = { 2000L })

        val result = repository.fetchFilaments()

        assertTrue(result is BambuCloudFilamentResult.Success)
        assertEquals("saved-access", service.filamentAccessToken)
        assertEquals("PLA Basic", (result as BambuCloudFilamentResult.Success).filaments.first().name)
    }

    private class FakeCloudService(
        private val loginResult: BambuCloudApiResult<BambuCloudTokens>,
        private val accountResult: BambuCloudApiResult<BambuCloudAccount>,
        private val printerResult: BambuCloudApiResult<List<BambuCloudPrinter>> = BambuCloudApiResult.Success(emptyList()),
        private val filamentResult: BambuCloudApiResult<List<BambuCloudFilament>> = BambuCloudApiResult.Success(emptyList()),
        private val filamentMutationResult: BambuCloudApiResult<Unit> = BambuCloudApiResult.Success(Unit)
    ) : BambuCloudService {
        var profileAccessToken: String = ""
        var printerAccessToken: String = ""
        var filamentAccessToken: String = ""
        var updateFilamentAccessToken: String = ""
        var lastFilamentUpdate: BambuCloudFilamentUpdate? = null
        var deleteFilamentAccessToken: String = ""
        var lastDeleteIds: List<Long> = emptyList()
        var lastDeleteRfids: List<String> = emptyList()

        override suspend fun loginWithPassword(
            account: String,
            password: String
        ): BambuCloudApiResult<BambuCloudTokens> = loginResult

        override suspend fun loginWithCode(
            account: String,
            password: String,
            code: String
        ): BambuCloudApiResult<BambuCloudTokens> = loginResult

        override suspend fun loginWithCaptcha(
            account: String,
            password: String,
            captcha: BambuCloudCaptchaResult
        ): BambuCloudApiResult<BambuCloudTokens> = loginResult

        override suspend fun fetchAccount(accessToken: String): BambuCloudApiResult<BambuCloudAccount> {
            profileAccessToken = accessToken
            return accountResult
        }

        override suspend fun fetchPrinters(accessToken: String): BambuCloudApiResult<List<BambuCloudPrinter>> {
            printerAccessToken = accessToken
            return printerResult
        }

        override suspend fun fetchFilaments(
            accessToken: String,
            offset: Int,
            limit: Int
        ): BambuCloudApiResult<List<BambuCloudFilament>> {
            filamentAccessToken = accessToken
            return filamentResult
        }

        override suspend fun updateFilament(
            accessToken: String,
            update: BambuCloudFilamentUpdate
        ): BambuCloudApiResult<Unit> {
            updateFilamentAccessToken = accessToken
            lastFilamentUpdate = update
            return filamentMutationResult
        }

        override suspend fun deleteFilaments(
            accessToken: String,
            ids: List<Long>,
            rfids: List<String>
        ): BambuCloudApiResult<Unit> {
            deleteFilamentAccessToken = accessToken
            lastDeleteIds = ids
            lastDeleteRfids = rfids
            return filamentMutationResult
        }

        override suspend fun fetchTasks(
            accessToken: String,
            offset: Int,
            limit: Int,
            status: Int
        ): BambuCloudApiResult<BambuCloudTaskPage> {
            return BambuCloudApiResult.Success(BambuCloudTaskPage(0, emptyList()))
        }
    }

    private class MemorySessionStore : BambuCloudSessionStore {
        var savedSession: BambuCloudSession? = null

        override fun loadSession(): BambuCloudSession? = savedSession

        override fun saveSession(session: BambuCloudSession) {
            savedSession = session
        }

        override fun clearSession() {
            savedSession = null
        }
    }
}
