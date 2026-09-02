@file:OptIn(ExperimentalForeignApi::class)

package com.example.my_kmp_project.core.network

import kotlinx.cinterop.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.netstack.*
import kotlin.concurrent.Volatile
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal enum class OhosHttpMethod {
    Get,
    Post,
}

internal data class OhosHttpResult(
    val statusCode: Int,
    val body: String,
)

/**
 * libnet_http.so transport (OHOS API 20+).
 * Callbacks carry no user-data — requests are serialized with [flightMutex].
 */
internal object OhosHttpTransport {
    private val flightMutex = Mutex()

    @Volatile
    private var inFlightRef: StableRef<OhosHttpRequestHolder>? = null

    suspend fun get(url: String, headers: Map<String, String>): OhosHttpResult =
        execute(OhosHttpMethod.Get, url, headers, body = null, contentType = null)

    suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: String,
        contentType: String,
    ): OhosHttpResult = execute(OhosHttpMethod.Post, url, headers, body, contentType)

    private suspend fun execute(
        method: OhosHttpMethod,
        url: String,
        headers: Map<String, String>,
        body: String?,
        contentType: String?,
    ): OhosHttpResult = flightMutex.withLock {
        suspendCancellableCoroutine { cont ->
            val holder = OhosHttpRequestHolder(
                continuation = cont,
                url = url,
                method = method,
                headers = headers,
                body = body,
                contentType = contentType,
            )
            inFlightRef = StableRef.create(holder)
            cont.invokeOnCancellation {
                holder.cancelRequest()
                inFlightRef?.dispose()
                inFlightRef = null
            }
            holder.start()
        }
    }

    internal fun buildBusinessUrl(path: String, query: Map<String, String>): String {
        val base = NetworkConfig.effectiveBaseUrl().trimEnd('/')
        val trimmed = path.trimStart('/')
        val url = "$base/$trimmed"
        if (query.isEmpty()) return url
        val qs = query.entries.joinToString("&") { (k, v) ->
            "${encodeQuery(k)}=${encodeQuery(v)}"
        }
        return "$url?$qs"
    }

    private fun encodeQuery(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when {
                ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.' || ch == '~' -> append(ch)
                else -> ch.code.toString(16).uppercase().let { hex ->
                    append('%')
                    if (hex.length == 1) append('0')
                    append(hex)
                }
            }
        }
    }

    internal fun completeFlight() {
        inFlightRef?.dispose()
        inFlightRef = null
    }

    internal fun currentHolder(): OhosHttpRequestHolder? = inFlightRef?.get()
}

internal class OhosHttpRequestHolder(
    private val continuation: kotlinx.coroutines.CancellableContinuation<OhosHttpResult>,
    private val url: String,
    private val method: OhosHttpMethod,
    private val headers: Map<String, String>,
    private val body: String?,
    private val contentType: String?,
) {
    private var requestPtr: CPointer<Http_Request>? = null
    private var headersPtr: CPointer<*>? = null
    private var optionsBlock: CPointer<Demo_HttpRequestOptions>? = null
    private var pinnedBody: Pinned<ByteArray>? = null
    private var pinnedMethod: Pinned<ByteArray>? = null

    fun start() {
        try {
            val request = Demo_Http_CreateRequest(url)
            if (request == null) {
                fail("Demo_Http_CreateRequest returned null")
                return
            }
            requestPtr = request

            val httpHeaders = Demo_Http_CreateHeaders()
            if (httpHeaders == null) {
                fail("Demo_Http_CreateHeaders returned null")
                return
            }
            headersPtr = httpHeaders

            val merged = LinkedHashMap(headers)
            if (method == OhosHttpMethod.Post && !body.isNullOrEmpty()) {
                if (!merged.containsKey("Content-Type")) {
                    merged["Content-Type"] = contentType ?: "application/json"
                }
            }
            merged.forEach { (name, value) ->
                Demo_Http_SetHeaderValue(httpHeaders, name, value)
            }

            val timeoutSec = (NetworkConfig.TIMEOUT_MS / 1000L).coerceAtLeast(1L).toUInt()
            val options = nativeHeap.alloc<Demo_HttpRequestOptions>()
            optionsBlock = options.ptr
            val methodLabel = when (method) {
                OhosHttpMethod.Get -> "GET"
                OhosHttpMethod.Post -> "POST"
            }
            val methodBytes = methodLabel.encodeToByteArray()
            pinnedMethod = methodBytes.pin()
            options.options.method = pinnedMethod!!.addressOf(0).reinterpret()
            options.options.priority = 0u
            options.options.headers = httpHeaders
            options.options.readTimeout = timeoutSec
            options.options.connectTimeout = timeoutSec
            options.options.httpProtocol = OH_HTTP1_1
            options.options.httpProxy = null
            options.options.caPath = null
            options.options.resumeFrom = 0L
            options.options.resumeTo = 0L
            options.options.clientCert = null
            options.options.dnsOverHttps = null
            options.options.addressFamily = HTTP_ADDRESS_FAMILY_DEFAULT

            if (!body.isNullOrEmpty()) {
                val bytes = body.encodeToByteArray()
                pinnedBody = bytes.pin()
                options.body.buffer = pinnedBody!!.addressOf(0).reinterpret()
                options.body.length = bytes.size.toUInt()
            } else {
                options.body.buffer = null
                options.body.length = 0u
            }

            request.pointed.options = options.ptr.reinterpret()

            val events = cValue<Http_EventsHandler> {
                onDataReceive = null
                onUploadProgress = null
                onDownloadProgress = null
                onHeadersReceive = null
                onDataEnd = null
                onCanceled = null
            }

            val launchCode = Demo_Http_Request(request, responseCallbackStatic, events)
            if (launchCode != 0) {
                fail("Demo_Http_Request failed: $launchCode")
            }
        } catch (t: Throwable) {
            fail(t.message ?: "OHOS HTTP setup failed")
        }
    }

    fun cancelRequest() {
        requestPtr?.let { ptr ->
            Demo_Http_DestroyPtr(ptr)
            requestPtr = null
        }
    }

    private fun fail(message: String) {
        cleanup()
        OhosHttpTransport.completeFlight()
        if (continuation.isActive) {
            continuation.resumeWithException(NetworkError.Transport(message))
        }
    }

    private fun complete(result: OhosHttpResult) {
        cleanup()
        OhosHttpTransport.completeFlight()
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }

    private fun cleanup() {
        cancelRequest()
        headersPtr?.let { ptr ->
            Demo_Http_DestroyHeadersPtr(ptr)
            headersPtr = null
        }
        pinnedMethod?.unpin()
        pinnedMethod = null
        optionsBlock?.let {
            nativeHeap.free(it)
            optionsBlock = null
        }
        pinnedBody?.unpin()
        pinnedBody = null
    }

    fun onResponse(response: CPointer<Http_Response>?, errCode: UInt) {
        if (response == null || errCode != 0u) {
            fail("OHOS HTTP error: $errCode")
            return
        }
        val resp = response.pointed
        val chunkBody = readBuffer(resp.body)
        val status = resp.responseCode.toInt()
        resp.destroyResponse?.let { destroy ->
            memScoped {
                val respVar = alloc<CPointerVar<Http_Response>>()
                respVar.value = response
                destroy(respVar.ptr)
            }
        }
        complete(OhosHttpResult(statusCode = status, body = chunkBody))
    }

    private fun readBuffer(buffer: Http_Buffer): String {
        val ptr = buffer.buffer ?: return ""
        val len = buffer.length.toInt()
        if (len <= 0) return ""
        return ptr.readBytes(len).decodeToString()
    }

    private companion object {
        private val responseCallbackStatic = staticCFunction { response: CPointer<Http_Response>?, errCode: UInt ->
            OhosHttpTransport.currentHolder()?.onResponse(response, errCode)
            Unit
        }
    }
}
