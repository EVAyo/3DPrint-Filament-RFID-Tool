package com.m0h31h31.bamburfidreader.cloud

import com.m0h31h31.bamburfidreader.logging.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class BambuCloudHttpTransport(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000
) : BambuCloudTransport {
    override suspend fun execute(request: BambuCloudHttpRequest): BambuCloudHttpResponse {
        return withContext(Dispatchers.IO) {
            val connection = URL(request.url).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                requestMethod = request.method
                request.headers.forEach { (key, value) ->
                    if (value.isNotBlank()) setRequestProperty(key, value)
                }
                if (request.body.isNotBlank()) {
                    doOutput = true
                }
            }
            try {
                if (request.body.isNotBlank()) {
                    connection.outputStream.use { output ->
                        output.write(request.body.toByteArray(Charsets.UTF_8))
                    }
                }
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val body = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                // 诊断日志：仅记录登录接口；含 token 时不打印 body，避免泄露凭证。
                // 用于对齐"携带极验结果重新登录"的回提契约（captcha 字段名/响应结构）。
                if (request.url.contains("user/login")) {
                    val captchaNote = if (request.headers.keys.any { it.equals("x-bbl-captcha-result", ignoreCase = true) }) " +captcha" else ""
                    val tokenNote = if (body.contains("accessToken")) " token" else ""
                    logDebug("Bambu login$captchaNote status=$statusCode$tokenNote")
                }
                BambuCloudHttpResponse(statusCode = statusCode, body = body)
            } finally {
                connection.disconnect()
            }
        }
    }
}
