package com.m0h31h31.bamburfidreader.cloud

import org.json.JSONObject
import java.util.Base64

class BambuCloudApiClient(
    private val transport: BambuCloudTransport,
    private val baseUrl: String = DEFAULT_BASE_URL
) : BambuCloudService {
    override suspend fun loginWithPassword(
        account: String,
        password: String
    ): BambuCloudApiResult<BambuCloudTokens> {
        val body = JSONObject()
            .put("account", account.trim())
            .put("password", password)
        return login(body)
    }

    override suspend fun loginWithCode(
        account: String,
        password: String,
        code: String
    ): BambuCloudApiResult<BambuCloudTokens> {
        val body = JSONObject()
            .put("account", account.trim())
            .put("password", password)
            .put("code", code.trim())
        return login(body)
    }

    override suspend fun loginWithCaptcha(
        account: String,
        password: String,
        captcha: BambuCloudCaptchaResult
    ): BambuCloudApiResult<BambuCloudTokens> {
        val body = JSONObject()
            .put("account", account.trim())
            .put("password", password)
        // 验证结果通过请求头 x-bbl-captcha-result 回提（Base64 编码的 JSON），body 不变。
        return login(body, mapOf(CAPTCHA_RESULT_HEADER to encodeCaptchaResult(captcha)))
    }

    /**
     * 按拓竹约定编码极验 v4 结果：snake_case JSON → Base64，放入 x-bbl-captcha-result 头。
     * 抓包确认的字段：captcha_id / lot_number / pass_token / gen_time / captcha_output。
     */
    private fun encodeCaptchaResult(captcha: BambuCloudCaptchaResult): String {
        val json = JSONObject()
            .put("captcha_id", captcha.captchaId)
            .put("lot_number", captcha.lotNumber)
            .put("pass_token", captcha.passToken)
            .put("gen_time", captcha.genTime)
            .put("captcha_output", captcha.captchaOutput)
        return Base64.getEncoder().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
    }

    override suspend fun fetchAccount(accessToken: String): BambuCloudApiResult<BambuCloudAccount> {
        val response = executeSafely(
            BambuCloudHttpRequest(
                method = "GET",
                url = "$baseUrl/v1/design-user-service/my/preference",
                headers = mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "Bearer $accessToken"
                )
            )
        )
        return parseSuccessResponse(response) { json ->
            BambuCloudAccount(
                uid = json.optLong("uid", 0L),
                name = json.optString("name"),
                handle = json.optString("handle"),
                avatarUrl = json.optString("avatar"),
                bio = json.optString("bio")
            )
        }
    }

    override suspend fun fetchPrinters(accessToken: String): BambuCloudApiResult<List<BambuCloudPrinter>> {
        val response = executeSafely(
            BambuCloudHttpRequest(
                method = "GET",
                url = "$baseUrl/v1/iot-service/api/user/bind",
                headers = mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "Bearer $accessToken"
                )
            )
        )
        return parseSuccessResponse(response) { json ->
            val devices = json.optJSONArray("devices")
            buildList {
                if (devices == null) return@buildList
                for (index in 0 until devices.length()) {
                    val device = devices.optJSONObject(index) ?: continue
                    add(
                        BambuCloudPrinter(
                            deviceId = device.optCleanString("dev_id"),
                            deviceName = device.optCleanString("name"),
                            modelName = device.optCleanString("dev_model_name"),
                            productName = device.optCleanString("dev_product_name"),
                            online = device.optBoolean("online", false),
                            taskId = device.optLong("print_job", 0L).takeIf { it > 0L }?.toString().orEmpty(),
                            taskName = "",
                            taskStatus = device.optCleanString("print_status"),
                            progress = null,
                            thumbnailUrl = "",
                            structure = device.optCleanString("dev_structure"),
                            nozzleDiameter = device.optNullableDouble("nozzle_diameter"),
                            accessCode = device.optCleanString("dev_access_code")
                        )
                    )
                }
            }
        }
    }

    override suspend fun fetchFilaments(
        accessToken: String,
        offset: Int,
        limit: Int
    ): BambuCloudApiResult<List<BambuCloudFilament>> {
        val response = executeSafely(
            BambuCloudHttpRequest(
                method = "GET",
                url = "$baseUrl/v1/design-user-service/my/filament/v2?offset=$offset&limit=$limit",
                headers = mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "Bearer $accessToken"
                )
            )
        )
        return parseSuccessResponse(response) { json ->
            val hits = json.optJSONArray("hits")
            buildList {
                if (hits == null) return@buildList
                for (index in 0 until hits.length()) {
                    val item = hits.optJSONObject(index) ?: continue
                    add(
                        BambuCloudFilament(
                            id = item.optLong("id", 0L),
                            createType = item.optCleanString("createType"),
                            vendor = item.optCleanString("filamentVendor"),
                            type = item.optCleanString("filamentType"),
                            name = item.optCleanString("filamentName"),
                            filamentId = item.optCleanString("filamentId"),
                            rfid = item.optCleanString("RFID"),
                            color = item.optCleanString("color"),
                            colors = item.optStringList("colors"),
                            netWeightGrams = item.optInt("netWeight", 0),
                            totalNetWeightGrams = item.optInt("totalNetWeight", 0),
                            trayIdName = item.optCleanString("trayIdName"),
                            inPrinter = item.optBoolean("inPrinter", false),
                            deviceId = item.optCleanString("devId"),
                            amsSerial = item.optCleanString("amsSn"),
                            slotId = item.optCleanString("slotId"),
                            amsId = item.optNullableInt("amsId"),
                            deviceName = item.optCleanString("deviceName")
                        )
                    )
                }
            }
        }
    }

    override suspend fun fetchTasks(
        accessToken: String,
        offset: Int,
        limit: Int,
        status: Int
    ): BambuCloudApiResult<BambuCloudTaskPage> {
        val response = executeSafely(
            BambuCloudHttpRequest(
                method = "GET",
                url = "$baseUrl/v1/user-service/my/tasks?limit=$limit&offset=$offset&status=$status",
                headers = mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "Bearer $accessToken"
                )
            )
        )
        return parseSuccessResponse(response) { json ->
            val hits = json.optJSONArray("hits")
            val tasks = buildList {
                if (hits == null) return@buildList
                for (index in 0 until hits.length()) {
                    val item = hits.optJSONObject(index) ?: continue
                    add(
                        BambuCloudTask(
                            id = item.optLong("id", 0L),
                            title = item.optCleanString("title"),
                            coverUrl = item.optCleanString("cover"),
                            status = item.optInt("status", 0),
                            failedType = item.optInt("failedType", 0),
                            startTimeMillis = parseIsoMillis(item.optCleanString("startTime")),
                            endTimeMillis = parseIsoMillis(item.optCleanString("endTime")),
                            weightGrams = item.optDouble("weight", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                            costTimeSeconds = item.optInt("costTime", 0),
                            deviceModel = item.optCleanString("deviceModel"),
                            deviceName = item.optCleanString("deviceName"),
                            repetitions = item.optInt("repetitions", 1).coerceAtLeast(1),
                            plateIndex = item.optInt("plateIndex", 0),
                            materials = parseTaskMaterials(item.optJSONArray("amsDetailMapping"))
                        )
                    )
                }
            }
            BambuCloudTaskPage(total = json.optInt("total", tasks.size), tasks = tasks)
        }
    }

    private fun parseTaskMaterials(array: org.json.JSONArray?): List<BambuCloudTaskMaterial> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    BambuCloudTaskMaterial(
                        filamentId = item.optCleanString("filamentId"),
                        filamentType = item.optCleanString("filamentType"),
                        color = item.optCleanString("targetColor").ifBlank { item.optCleanString("sourceColor") },
                        weightGrams = item.optDouble("weight", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                        nozzleId = item.optInt("nozzleId", 0)
                    )
                )
            }
        }
    }

    /** 解析 ISO-8601 时间（如 2026-06-21T02:00:45Z）为毫秒；失败返回 0。 */
    private fun parseIsoMillis(value: String): Long {
        if (value.isBlank()) return 0L
        return try {
            java.time.Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private suspend fun login(
        body: JSONObject,
        extraHeaders: Map<String, String> = emptyMap()
    ): BambuCloudApiResult<BambuCloudTokens> {
        val response = executeSafely(
            BambuCloudHttpRequest(
                method = "POST",
                url = "$baseUrl/v1/user-service/user/login",
                headers = buildMap {
                    put("Accept", "application/json")
                    put("Content-Type", "application/json; charset=UTF-8")
                    putAll(extraHeaders)
                },
                body = body.toString()
            )
        )
        val json = response.toJson()
        // 人机验证：响应里带 captchaId（可能伴随非 200 状态码与机器人提示）
        val captchaId = json.optCleanString("captchaId")
        if (captchaId.isNotBlank()) {
            return BambuCloudApiResult.CaptchaRequired(
                captchaId = captchaId,
                scene = json.optCleanString("captchaScene"),
                message = json.optString("error").ifBlank { json.optString("message") }
            )
        }
        if (response.statusCode != 200) {
            val message = response.failureMessage()
            return BambuCloudApiResult.Failure(
                message = message,
                statusCode = response.statusCode.takeIf { it > 0 },
                loginFailureReason = message.toLoginFailureReason()
            )
        }
        val loginType = json.optString("loginType")
        if (loginType == "verifyCode") {
            return BambuCloudApiResult.VerificationCodeRequired
        }
        return BambuCloudApiResult.Success(
            BambuCloudTokens(
                accessToken = json.optString("accessToken"),
                refreshToken = json.optString("refreshToken"),
                expiresInSeconds = json.optInt("expiresIn", 0),
                loginType = loginType
            )
        )
    }

    private suspend fun executeSafely(request: BambuCloudHttpRequest): BambuCloudHttpResponse {
        return try {
            transport.execute(request)
        } catch (e: Exception) {
            BambuCloudHttpResponse(
                statusCode = 0,
                body = JSONObject().put("message", e.message.orEmpty()).toString()
            )
        }
    }

    private fun <T> parseSuccessResponse(
        response: BambuCloudHttpResponse,
        parser: (JSONObject) -> T
    ): BambuCloudApiResult<T> {
        val json = response.toJson()
        if (response.statusCode !in 200..299) {
            return BambuCloudApiResult.Failure(
                message = response.failureMessage(),
                statusCode = response.statusCode.takeIf { it > 0 }
            )
        }
        return BambuCloudApiResult.Success(parser(json))
    }

    private fun BambuCloudHttpResponse.failureMessage(): String {
        val json = toJson()
        val rawBody = body.trim()
        return json.optString("message")
            .ifBlank { json.optString("error") }
            .ifBlank { rawBody }
            .ifBlank { "HTTP $statusCode" }
    }

    private fun String.toLoginFailureReason(): BambuCloudLoginFailureReason? {
        return when (trim()) {
            "Incorrect account or password." -> BambuCloudLoginFailureReason.ACCOUNT_OR_PASSWORD_INCORRECT
            "Incorrect code" -> BambuCloudLoginFailureReason.VERIFICATION_CODE_INCORRECT
            else -> null
        }
    }

    private fun BambuCloudHttpResponse.toJson(): JSONObject {
        return try {
            JSONObject(body.ifBlank { "{}" })
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun JSONObject.optCleanString(key: String): String {
        if (!has(key) || isNull(key)) return ""
        return optString(key).trim()
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key).takeIf { it >= 0 }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key).takeIf { !it.isNaN() && it > 0.0 }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.bambulab.cn"
        /** 人机验证结果回提头：值为 Base64 编码的极验结果 JSON。 */
        const val CAPTCHA_RESULT_HEADER = "x-bbl-captcha-result"
    }
}
