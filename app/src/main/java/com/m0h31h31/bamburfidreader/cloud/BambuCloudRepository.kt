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
            is BambuCloudApiResult.CaptchaRequired -> {
                return BambuCloudRepositoryResult.CaptchaRequired(accountResult.captchaId, accountResult.scene, accountResult.message)
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
            is BambuCloudApiResult.CaptchaRequired -> BambuCloudPrinterResult.Failure(result.message)
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
            is BambuCloudApiResult.CaptchaRequired -> BambuCloudFilamentResult.Failure(result.message)
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

    /** 网页（人机验证）登录成功后，用拿到的 token 直接建立会话。 */
    suspend fun completeWebLogin(tokens: BambuCloudTokens): BambuCloudRepositoryResult {
        return completeLogin(BambuCloudApiResult.Success(tokens))
    }

    private suspend fun completeLogin(
        tokenResult: BambuCloudApiResult<BambuCloudTokens>
    ): BambuCloudRepositoryResult {
        val tokens = when (tokenResult) {
            is BambuCloudApiResult.Success -> tokenResult.value
            BambuCloudApiResult.VerificationCodeRequired -> {
                return BambuCloudRepositoryResult.VerificationCodeRequired
            }
            is BambuCloudApiResult.CaptchaRequired -> {
                return BambuCloudRepositoryResult.CaptchaRequired(tokenResult.captchaId, tokenResult.scene, tokenResult.message)
            }
            is BambuCloudApiResult.Failure -> {
                return BambuCloudRepositoryResult.Failure(
                    message = tokenResult.message,
                    loginFailureReason = tokenResult.loginFailureReason
                )
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
            is BambuCloudApiResult.CaptchaRequired -> {
                return BambuCloudRepositoryResult.CaptchaRequired(accountResult.captchaId, accountResult.scene, accountResult.message)
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
