package com.m0h31h31.bamburfidreader.cloud

data class BambuCloudTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
    val loginType: String = ""
)

data class BambuCloudAccount(
    val uid: Long,
    val name: String,
    val handle: String,
    val avatarUrl: String,
    val bio: String
)

data class BambuCloudPrinter(
    val deviceId: String,
    val deviceName: String,
    val modelName: String,
    val productName: String,
    val online: Boolean,
    val taskId: String,
    val taskName: String,
    val taskStatus: String,
    val progress: Int?,
    val thumbnailUrl: String,
    val structure: String = "",
    val nozzleDiameter: Double? = null,
    val accessCode: String = ""
)

data class BambuCloudFilament(
    val id: Long,
    val createType: String,
    val vendor: String,
    val type: String,
    val name: String,
    val filamentId: String,
    val rfid: String,
    val color: String,
    val colors: List<String>,
    val netWeightGrams: Int,
    val totalNetWeightGrams: Int,
    val trayIdName: String,
    val inPrinter: Boolean,
    val deviceId: String,
    val amsSerial: String,
    val slotId: String,
    val amsId: Int?,
    val deviceName: String
)

data class BambuCloudSession(
    val tokens: BambuCloudTokens,
    val account: BambuCloudAccount,
    val savedAtMillis: Long,
    val expiresAtMillis: Long
)

enum class BambuCloudLoginFailureReason {
    ACCOUNT_OR_PASSWORD_INCORRECT,
    VERIFICATION_CODE_INCORRECT
}

sealed class BambuCloudApiResult<out T> {
    data class Success<T>(val value: T) : BambuCloudApiResult<T>()
    data object VerificationCodeRequired : BambuCloudApiResult<Nothing>()
    /** 登录触发了人机验证（captcha），需要在网页中完成验证。 */
    data class CaptchaRequired(
        val captchaId: String,
        val scene: String,
        val message: String
    ) : BambuCloudApiResult<Nothing>()
    data class Failure(
        val message: String,
        val statusCode: Int? = null,
        val loginFailureReason: BambuCloudLoginFailureReason? = null
    ) : BambuCloudApiResult<Nothing>()
}

data class BambuCloudHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)

data class BambuCloudHttpResponse(
    val statusCode: Int,
    val body: String
)

interface BambuCloudTransport {
    suspend fun execute(request: BambuCloudHttpRequest): BambuCloudHttpResponse
}

interface BambuCloudService {
    suspend fun loginWithPassword(
        account: String,
        password: String
    ): BambuCloudApiResult<BambuCloudTokens>

    suspend fun loginWithCode(
        account: String,
        password: String,
        code: String
    ): BambuCloudApiResult<BambuCloudTokens>

    suspend fun fetchAccount(accessToken: String): BambuCloudApiResult<BambuCloudAccount>

    suspend fun fetchPrinters(accessToken: String): BambuCloudApiResult<List<BambuCloudPrinter>>

    suspend fun fetchFilaments(
        accessToken: String,
        offset: Int,
        limit: Int
    ): BambuCloudApiResult<List<BambuCloudFilament>>
}

interface BambuCloudSessionStore {
    fun loadSession(): BambuCloudSession?
    fun saveSession(session: BambuCloudSession)
    fun clearSession()
}

sealed class BambuCloudRepositoryResult {
    data class Success(val session: BambuCloudSession) : BambuCloudRepositoryResult()
    data object VerificationCodeRequired : BambuCloudRepositoryResult()
    /** 需要人机验证：调用方应打开网页登录完成验证。 */
    data class CaptchaRequired(
        val captchaId: String,
        val scene: String,
        val message: String
    ) : BambuCloudRepositoryResult()
    data class Failure(
        val message: String,
        val loginFailureReason: BambuCloudLoginFailureReason? = null
    ) : BambuCloudRepositoryResult()
}

sealed class BambuCloudPrinterResult {
    data class Success(val printers: List<BambuCloudPrinter>) : BambuCloudPrinterResult()
    data class Failure(val message: String) : BambuCloudPrinterResult()
}

sealed class BambuCloudFilamentResult {
    data class Success(val filaments: List<BambuCloudFilament>) : BambuCloudFilamentResult()
    data class Failure(val message: String) : BambuCloudFilamentResult()
}
