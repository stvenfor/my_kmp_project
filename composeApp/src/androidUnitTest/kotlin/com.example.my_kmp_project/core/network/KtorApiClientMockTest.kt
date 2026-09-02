package com.example.my_kmp_project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class KtorApiClientMockTest {

    private var savedToken = ""

    @BeforeTest
    fun save() {
        savedToken = NetworkConfig.accessToken
        NetworkConfig.accessToken = "test-token"
        NetworkConfig.buildMode = BuildMode.Debug
        NetworkConfig.netEnvironment = NetEnvironment.Test
        NetworkConfig.baseUrlOverride = ""
    }

    @AfterTest
    fun restore() {
        NetworkConfig.accessToken = savedToken
    }

    @Test
    fun business_path_sends_bearer() = runBlocking {
        var sawAuth = false
        val engine = MockEngine { request ->
            sawAuth = request.headers[HttpHeaders.Authorization] == "Bearer test-token"
            respond(
                content = """{"code":200,"message":"ok","data":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = KtorApiClient(HttpClient(engine) { configureDemoHttpClient() })
        client.getRaw("out-api/ping")
        assertTrue(sawAuth)
    }

    @Test
    fun auth_skip_host_skips_bearer() = runBlocking {
        var sawAuth = false
        val engine = MockEngine { request ->
            sawAuth = request.headers.contains(HttpHeaders.Authorization)
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = KtorApiClient(HttpClient(engine) { configureDemoHttpClient() })
        client.getAbsoluteRaw("https://cdn.demo.local/asset.json")
        assertFalse(sawAuth)
    }
}
