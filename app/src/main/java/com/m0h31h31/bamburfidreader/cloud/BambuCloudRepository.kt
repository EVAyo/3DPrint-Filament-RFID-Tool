package com.m0h31h31.bamburfidreader.cloud

import android.content.Context

class BambuCloudRepository(
    private val service: BambuCloudService,
    private val sessionStore: BambuCloudSessionStore,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    constructor(context: Context) : this(
        service = BambuCloudApiClient(BambuCloudHttpTransport()),
        sessionStore = SharedPreferencesBambuCloudSessionStore(context)
    )

    fun loadSession(): BambuCloudSession? = sessionStore.loadSession()

    fun logout() {
        sessionStore.clearSession()
    }

    suspend fun refreshAccount(): BambuCloudRepositoryResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudRepositoryResult.Failure("Not logged in")
        val account = when (val accountResult = service.fetchAccount(currentSession.tokens.accessToken)) {
            is BambuCloudApiResult.Success -> accountResult.value
            BambuCloudApiResult.VerificationCodeRequired -> {
                return BambuCloudRepositoryResult.VerificationCodeRequired
            }
            is BambuCloudApiResult.Failure -> {
                return BambuCloudRepositoryResult.Failure(accountResult.message)
            }
        }
        val refreshed = currentSession.copy(account = account)
        sessionStore.saveSession(refreshed)
        return BambuCloudRepositoryResult.Success(refreshed)
    }

    suspend fun fetchPrinters(): BambuCloudPrinterResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudPrinterResult.Failure("Not logged in")
        return when (val result = service.fetchPrinters(currentSession.tokens.accessToken)) {
            is BambuCloudApiResult.Success -> BambuCloudPrinterResult.Success(result.value)
            BambuCloudApiResult.VerificationCodeRequired -> {
                BambuCloudPrinterResult.Failure("Verification code required")
            }
            is BambuCloudApiResult.Failure -> BambuCloudPrinterResult.Failure(result.message)
        }
    }

    suspend fun fetchFilaments(
        offset: Int = 0,
        limit: Int = 20
    ): BambuCloudFilamentResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudFilamentResult.Failure("Not logged in")
        return when (val result = service.fetchFilaments(currentSession.tokens.accessToken, offset, limit)) {
            is BambuCloudApiResult.Success -> BambuCloudFilamentResult.Success(result.value)
            BambuCloudApiResult.VerificationCodeRequired -> {
                BambuCloudFilamentResult.Failure("Verification code required")
            }
            is BambuCloudApiResult.Failure -> BambuCloudFilamentResult.Failure(result.message)
        }
    }

    suspend fun loginWithPassword(
        account: String,
        password: String
    ): BambuCloudRepositoryResult {
        return completeLogin(service.loginWithPassword(account, password))
    }

    suspend fun loginWithCode(
        account: String,
        password: String,
        code: String
    ): BambuCloudRepositoryResult {
        return completeLogin(service.loginWithCode(account, password, code))
    }

    private suspend fun completeLogin(
        tokenResult: BambuCloudApiResult<BambuCloudTokens>
    ): BambuCloudRepositoryResult {
        val tokens = when (tokenResult) {
            is BambuCloudApiResult.Success -> tokenResult.value
            BambuCloudApiResult.VerificationCodeRequired -> {
                return BambuCloudRepositoryResult.VerificationCodeRequired
            }
            is BambuCloudApiResult.Failure -> {
                return BambuCloudRepositoryResult.Failure(tokenResult.message)
            }
        }
        if (tokens.accessToken.isBlank()) {
            return BambuCloudRepositoryResult.Failure("Missing access token")
        }
        val account = when (val accountResult = service.fetchAccount(tokens.accessToken)) {
            is BambuCloudApiResult.Success -> accountResult.value
            BambuCloudApiResult.VerificationCodeRequired -> {
                return BambuCloudRepositoryResult.VerificationCodeRequired
            }
            is BambuCloudApiResult.Failure -> {
                return BambuCloudRepositoryResult.Failure(accountResult.message)
            }
        }
        val savedAt = nowMillis()
        val session = BambuCloudSession(
            tokens = tokens,
            account = account,
            savedAtMillis = savedAt,
            expiresAtMillis = savedAt + tokens.expiresInSeconds * 1000L
        )
        sessionStore.saveSession(session)
        return BambuCloudRepositoryResult.Success(session)
    }
}
