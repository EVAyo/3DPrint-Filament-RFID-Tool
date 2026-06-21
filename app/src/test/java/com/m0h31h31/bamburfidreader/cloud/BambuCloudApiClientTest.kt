package com.m0h31h31.bamburfidreader.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BambuCloudApiClientTest {
    @Test
    fun loginWithPasswordPostsAccountAndPasswordOnly() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "accessToken": "access-123",
                      "refreshToken": "refresh-123",
                      "loginType": "",
                      "expiresIn": 7776000
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudApiResult.Success)
        val tokens = (result as BambuCloudApiResult.Success).value
        assertEquals("access-123", tokens.accessToken)
        assertEquals("refresh-123", tokens.refreshToken)
        assertEquals(7776000, tokens.expiresInSeconds)
        assertEquals("POST", transport.lastRequest.method)
        assertEquals("https://api.bambulab.cn/v1/user-service/user/login", transport.lastRequest.url)
        assertTrue(transport.lastRequest.body.contains("\"account\":\"user@example.com\""))
        assertTrue(transport.lastRequest.body.contains("\"password\":\"secret\""))
        assertFalse(transport.lastRequest.body.contains("\"code\""))
    }

    @Test
    fun loginReturnsVerificationRequiredWhenApiRequestsCode() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "loginType": "verifyCode"
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudApiResult.VerificationCodeRequired)
    }

    @Test
    fun loginReturnsCaptchaRequiredWhenApiRequestsHumanVerification() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 400,
                body = """
                    {
                      "captchaId": "78120903a6338320170d5a4fa1c8e113",
                      "captchaScene": "verify_code",
                      "error": "We need to confirm that you are not a robot."
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudApiResult.CaptchaRequired)
        val captcha = result as BambuCloudApiResult.CaptchaRequired
        assertEquals("78120903a6338320170d5a4fa1c8e113", captcha.captchaId)
        assertEquals("verify_code", captcha.scene)
    }

    @Test
    fun loginWithCodePostsAccountPasswordAndCode() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "accessToken": "access-code",
                      "refreshToken": "refresh-code",
                      "loginType": "",
                      "expiresIn": 7776000
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithCode("user@example.com", "secret", "123456")

        assertTrue(result is BambuCloudApiResult.Success)
        assertEquals("POST", transport.lastRequest.method)
        assertEquals("https://api.bambulab.cn/v1/user-service/user/login", transport.lastRequest.url)
        assertTrue(transport.lastRequest.body.contains("\"account\":\"user@example.com\""))
        assertTrue(transport.lastRequest.body.contains("\"password\":\"secret\""))
        assertTrue(transport.lastRequest.body.contains("\"code\":\"123456\""))
    }

    @Test
    fun loginFailureUsesAccountPasswordMessageForIncorrectAccountOrPassword() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 400,
                body = """
                    {
                      "code": 2,
                      "error": "Incorrect account or password."
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudApiResult.Failure)
        assertEquals("Incorrect account or password.", (result as BambuCloudApiResult.Failure).message)
        assertEquals(400, result.statusCode)
        assertEquals(BambuCloudLoginFailureReason.ACCOUNT_OR_PASSWORD_INCORRECT, result.loginFailureReason)
    }

    @Test
    fun loginFailureUsesVerificationCodeMessageForIncorrectCode() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 400,
                body = """
                    {
                      "code": 2,
                      "error": "Incorrect code"
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithCode("user@example.com", "secret", "123456")

        assertTrue(result is BambuCloudApiResult.Failure)
        assertEquals("Incorrect code", (result as BambuCloudApiResult.Failure).message)
        assertEquals(400, result.statusCode)
        assertEquals(BambuCloudLoginFailureReason.VERIFICATION_CODE_INCORRECT, result.loginFailureReason)
    }

    @Test
    fun loginFailureFallsBackToApiErrorMessage() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 400,
                body = """
                    {
                      "code": 99,
                      "error": "Login temporarily unavailable"
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudApiResult.Failure)
        assertEquals("Login temporarily unavailable", (result as BambuCloudApiResult.Failure).message)
        assertEquals(400, result.statusCode)
        assertEquals(null, result.loginFailureReason)
    }

    @Test
    fun loginFailureShowsApiErrorMessageForNon400Status() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 418,
                body = """
                    {
                      "code": 418,
                      "error": "Session challenge required"
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudApiResult.Failure)
        assertEquals("Session challenge required", (result as BambuCloudApiResult.Failure).message)
        assertEquals(418, result.statusCode)
        assertEquals(null, result.loginFailureReason)
    }

    @Test
    fun loginFailureShowsRawBodyWhenNon200ResponseIsNotJson() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 418,
                body = "Login blocked by server policy"
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.loginWithPassword("user@example.com", "secret")

        assertTrue(result is BambuCloudApiResult.Failure)
        assertEquals("Login blocked by server policy", (result as BambuCloudApiResult.Failure).message)
        assertEquals(418, result.statusCode)
    }

    @Test
    fun fetchAccountUsesBearerTokenAndParsesPreferenceProfile() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "uid": 123456,
                      "name": "Bambu User",
                      "handle": "maker",
                      "avatar": "https://example.com/avatar.png",
                      "bio": "hello"
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.fetchAccount("access-123")

        assertTrue(result is BambuCloudApiResult.Success)
        val account = (result as BambuCloudApiResult.Success).value
        assertEquals(123456L, account.uid)
        assertEquals("Bambu User", account.name)
        assertEquals("maker", account.handle)
        assertEquals("https://example.com/avatar.png", account.avatarUrl)
        assertEquals("hello", account.bio)
        assertEquals("GET", transport.lastRequest.method)
        assertEquals("Bearer access-123", transport.lastRequest.headers["Authorization"])
    }

    @Test
    fun fetchPrintersUsesBearerTokenAndParsesPrintStatus() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "message": "success",
                      "devices": [
                        {
                          "dev_id": "printer-1",
                          "name": "Studio X1C",
                          "online": true,
                          "print_status": "RUNNING",
                          "print_job": 214306601,
                          "dev_model_name": "C12",
                          "dev_product_name": "X1 Carbon",
                          "dev_access_code": "982a8dd4",
                          "nozzle_diameter": 0.4,
                          "dev_structure": "CoreXY"
                        },
                        {
                          "dev_id": "printer-2",
                          "name": "Desk A1",
                          "online": false,
                          "print_status": "SUCCESS",
                          "print_job": 0,
                          "dev_model_name": "N2S",
                          "dev_product_name": "A1 mini",
                          "dev_access_code": "10843109",
                          "nozzle_diameter": 0.4,
                          "dev_structure": "I3"
                        }
                      ]
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.fetchPrinters("access-123")

        assertTrue(result is BambuCloudApiResult.Success)
        val printers = (result as BambuCloudApiResult.Success).value
        assertEquals(2, printers.size)
        assertEquals("printer-1", printers[0].deviceId)
        assertEquals("Studio X1C", printers[0].deviceName)
        assertEquals("C12", printers[0].modelName)
        assertEquals("X1 Carbon", printers[0].productName)
        assertTrue(printers[0].online)
        assertEquals("214306601", printers[0].taskId)
        assertEquals("RUNNING", printers[0].taskStatus)
        assertEquals("CoreXY", printers[0].structure)
        assertEquals(0.4, printers[0].nozzleDiameter)
        assertEquals("982a8dd4", printers[0].accessCode)
        assertFalse(printers[1].online)
        assertEquals("", printers[1].taskId)
        assertEquals("GET", transport.lastRequest.method)
        assertEquals("https://api.bambulab.cn/v1/iot-service/api/user/bind", transport.lastRequest.url)
        assertEquals("Bearer access-123", transport.lastRequest.headers["Authorization"])
    }

    @Test
    fun fetchFilamentsUsesBearerTokenAndParsesLibraryHits() = runBlocking {
        val transport = RecordingTransport(
            BambuCloudHttpResponse(
                statusCode = 200,
                body = """
                    {
                      "hits": [
                        {
                          "id": 1758290,
                          "createType": "ams",
                          "filamentVendor": "Bambu Lab",
                          "filamentType": "PLA",
                          "filamentName": "PLA Basic",
                          "filamentId": "GFA00",
                          "RFID": "1F78AB9554E34B46BC890D60A016E9CF",
                          "color": "#8E9089FF",
                          "colors": ["#8E9089FF"],
                          "netWeight": 760,
                          "totalNetWeight": 1000,
                          "trayIdName": "A00-D00",
                          "inPrinter": true,
                          "devId": "01P09C4A3000216",
                          "amsSn": "00600A492907413",
                          "slotId": "0",
                          "amsId": 0,
                          "deviceName": "P1"
                        }
                      ]
                    }
                """.trimIndent()
            )
        )
        val client = BambuCloudApiClient(transport)

        val result = client.fetchFilaments("access-123", offset = 0, limit = 20)

        assertTrue(result is BambuCloudApiResult.Success)
        val filaments = (result as BambuCloudApiResult.Success).value
        assertEquals(1, filaments.size)
        val filament = filaments.first()
        assertEquals(1758290L, filament.id)
        assertEquals("ams", filament.createType)
        assertEquals("Bambu Lab", filament.vendor)
        assertEquals("PLA", filament.type)
        assertEquals("PLA Basic", filament.name)
        assertEquals("GFA00", filament.filamentId)
        assertEquals("1F78AB9554E34B46BC890D60A016E9CF", filament.rfid)
        assertEquals("#8E9089FF", filament.color)
        assertEquals(listOf("#8E9089FF"), filament.colors)
        assertEquals(760, filament.netWeightGrams)
        assertEquals(1000, filament.totalNetWeightGrams)
        assertTrue(filament.inPrinter)
        assertEquals("01P09C4A3000216", filament.deviceId)
        assertEquals("00600A492907413", filament.amsSerial)
        assertEquals("0", filament.slotId)
        assertEquals(0, filament.amsId)
        assertEquals("P1", filament.deviceName)
        assertEquals("GET", transport.lastRequest.method)
        assertEquals(
            "https://api.bambulab.cn/v1/design-user-service/my/filament/v2?offset=0&limit=20",
            transport.lastRequest.url
        )
        assertEquals("Bearer access-123", transport.lastRequest.headers["Authorization"])
    }

    private class RecordingTransport(
        private val response: BambuCloudHttpResponse
    ) : BambuCloudTransport {
        lateinit var lastRequest: BambuCloudHttpRequest

        override suspend fun execute(request: BambuCloudHttpRequest): BambuCloudHttpResponse {
            lastRequest = request
            return response
        }
    }
}
