package com.example.my_kmp_project.feature.home

import com.example.my_kmp_project.core.network.NetworkConfig
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import io.grpc.CallOptions
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.stub.ClientCalls
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Android 真实 gRPC 客户端：grpc-java + okhttp 明文传输。
 *
 * 工程没有 protoc/protoc-gen-grpc-java，所以这里不生成 protobuf 类，而是用
 * [MethodDescriptor] + 手写 marshal/unmarshal（[CodedInputStream] / [CodedOutputStream]）
 * 完成 `analytics.v1.AnalyticsService` 的请求编码与 35 字段响应解码。
 */
actual class AnalyticsGrpcClient actual constructor() {

    actual suspend fun listRecords(page: Int, pageSize: Int): AnalyticsPage =
        withContext(Dispatchers.IO) {
            decodeListResponse(call(LIST_METHOD, encodeListRequest(page, pageSize)))
        }

    actual suspend fun getRecord(id: Long): AnalyticsRecordRow? =
        withContext(Dispatchers.IO) {
            decodeGetResponse(call(GET_METHOD, encodeGetRequest(id)))
        }
}

private const val GRPC_PORT = 9090
private const val SERVICE_NAME = "analytics.v1.AnalyticsService"

private val AUTH_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
private val SESSION_KEY = Metadata.Key.of("x-session-id", Metadata.ASCII_STRING_MARSHALLER)
private val DEVICE_KEY = Metadata.Key.of("x-device-id", Metadata.ASCII_STRING_MARSHALLER)

private val CALL_OPTIONS: CallOptions = CallOptions.DEFAULT.withDeadlineAfter(15, TimeUnit.SECONDS)

/** 请求/响应统一按原始字节流编解码（无生成类）。 */
private object ByteArrayMarshaller : MethodDescriptor.Marshaller<ByteArray> {
    override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)

    override fun parse(stream: InputStream): ByteArray = stream.readBytes()
}

private val LIST_METHOD: MethodDescriptor<ByteArray, ByteArray> = unaryMethod("ListAnalyticsRecords")
private val GET_METHOD: MethodDescriptor<ByteArray, ByteArray> = unaryMethod("GetAnalyticsRecord")

private fun unaryMethod(name: String): MethodDescriptor<ByteArray, ByteArray> =
    MethodDescriptor.newBuilder<ByteArray, ByteArray>()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, name))
        .setRequestMarshaller(ByteArrayMarshaller)
        .setResponseMarshaller(ByteArrayMarshaller)
        .build()

private val channelCache = mutableMapOf<String, ManagedChannel>()

private fun channelFor(host: String): ManagedChannel = synchronized(channelCache) {
    channelCache.getOrPut(host) {
        ManagedChannelBuilder.forAddress(host, GRPC_PORT).usePlaintext().build()
    }
}

/** gRPC 主机与 HTTP 同 host（端口固定 [GRPC_PORT]）。 */
private fun grpcHost(): String {
    val base = NetworkConfig.effectiveBaseUrl().trim()
    val uriHost = runCatching { URI(base).host }.getOrNull()
    if (!uriHost.isNullOrBlank()) return uriHost
    return base.substringAfter("://", base)
        .substringBefore('/')
        .substringBefore(':')
        .ifBlank { "127.0.0.1" }
}

/** 对齐服务端 interceptor：authorization / x-session-id / x-device-id。 */
private fun authMetadata(): Metadata {
    val metadata = Metadata()
    NetworkConfig.accessToken.takeIf { it.isNotBlank() }?.let { metadata.put(AUTH_KEY, "Bearer $it") }
    NetworkConfig.sessionId.takeIf { it.isNotBlank() }?.let { metadata.put(SESSION_KEY, it) }
    NetworkConfig.deviceId.takeIf { it.isNotBlank() }?.let { metadata.put(DEVICE_KEY, it) }
    return metadata
}

private fun call(method: MethodDescriptor<ByteArray, ByteArray>, request: ByteArray): ByteArray {
    val channel = ClientInterceptors.intercept(
        channelFor(grpcHost()),
        MetadataUtils.newAttachHeadersInterceptor(authMetadata()),
    )
    return ClientCalls.blockingUnaryCall(channel, method, CALL_OPTIONS, request)
}

// ----- 请求编码（ListAnalyticsRecordsRequest / GetAnalyticsRecordRequest）-----

private fun encodeListRequest(page: Int, pageSize: Int): ByteArray {
    val buffer = ByteArrayOutputStream()
    val out = CodedOutputStream.newInstance(buffer)
    out.writeInt32(1, page)
    out.writeInt32(2, pageSize)
    out.flush()
    return buffer.toByteArray()
}

private fun encodeGetRequest(id: Long): ByteArray {
    val buffer = ByteArrayOutputStream()
    val out = CodedOutputStream.newInstance(buffer)
    out.writeInt64(1, id)
    out.flush()
    return buffer.toByteArray()
}

// ----- 响应解码 -----

private fun decodeListResponse(bytes: ByteArray): AnalyticsPage {
    val input = CodedInputStream.newInstance(bytes)
    val items = mutableListOf<AnalyticsRecordRow>()
    var total = 0L
    var page = 1
    var pageSize = 0
    while (true) {
        val tag = input.readTag()
        if (tag == 0) break
        when (tag) {
            // field 1: repeated AnalyticsRecord items
            10 -> items += decodeRecord(CodedInputStream.newInstance(input.readByteArray()))
            // field 2: int64 total
            16 -> total = input.readInt64()
            // field 3: int32 page
            24 -> page = input.readInt32()
            // field 4: int32 page_size
            32 -> pageSize = input.readInt32()
            else -> if (!input.skipField(tag)) break
        }
    }
    return AnalyticsPage(items = items, total = total, page = page, pageSize = pageSize)
}

private fun decodeGetResponse(bytes: ByteArray): AnalyticsRecordRow? {
    val input = CodedInputStream.newInstance(bytes)
    var item: AnalyticsRecordRow? = null
    while (true) {
        val tag = input.readTag()
        if (tag == 0) break
        when (tag) {
            // field 1: AnalyticsRecord item
            10 -> item = decodeRecord(CodedInputStream.newInstance(input.readByteArray()))
            else -> if (!input.skipField(tag)) break
        }
    }
    return item
}

/** 35 字段全量解析结果（UI 只需要其中一部分，其余保留以便排查）。 */
private class AnalyticsRecordProto(
    val id: Long = 0L,
    val code: String = "",
    val title: String = "",
    val subtitle: String = "",
    val category: String = "",
    val subCategory: String = "",
    val status: String = "",
    val priority: Int = 0,
    val region: String = "",
    val channel: String = "",
    val ownerName: String = "",
    val ownerTeam: String = "",
    val sourceSystem: String = "",
    val metricPv: Long = 0L,
    val metricUv: Long = 0L,
    val metricClick: Long = 0L,
    val metricConvert: Long = 0L,
    val metricRevenue: Double = 0.0,
    val metricCost: Double = 0.0,
    val metricRoi: Double = 0.0,
    val metricBounceRate: Double = 0.0,
    val metricAvgDurationSec: Int = 0,
    val scoreQuality: Double = 0.0,
    val scoreRisk: Double = 0.0,
    val tagPrimary: String = "",
    val tagSecondary: String = "",
    val flagFeatured: Boolean = false,
    val flagAnomaly: Boolean = false,
    val notes: String = "",
    val observedAt: Long = 0L,
    val windowStart: Long = 0L,
    val windowEnd: Long = 0L,
    val publishedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun toRow(): AnalyticsRecordRow = AnalyticsRecordRow(
        code = code,
        title = title,
        subtitle = subtitle,
        status = status,
        pv = metricPv.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        uv = metricUv.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        clicks = metricClick.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        converts = metricConvert.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        roi = metricRoi,
        featured = flagFeatured,
        anomaly = flagAnomaly,
        id = id,
    )
}

private fun decodeRecord(input: CodedInputStream): AnalyticsRecordRow {
    var id = 0L
    var code = ""
    var title = ""
    var subtitle = ""
    var category = ""
    var subCategory = ""
    var status = ""
    var priority = 0
    var region = ""
    var channel = ""
    var ownerName = ""
    var ownerTeam = ""
    var sourceSystem = ""
    var metricPv = 0L
    var metricUv = 0L
    var metricClick = 0L
    var metricConvert = 0L
    var metricRevenue = 0.0
    var metricCost = 0.0
    var metricRoi = 0.0
    var metricBounceRate = 0.0
    var metricAvgDurationSec = 0
    var scoreQuality = 0.0
    var scoreRisk = 0.0
    var tagPrimary = ""
    var tagSecondary = ""
    var flagFeatured = false
    var flagAnomaly = false
    var notes = ""
    var observedAt = 0L
    var windowStart = 0L
    var windowEnd = 0L
    var publishedAt = 0L
    var createdAt = 0L
    var updatedAt = 0L

    while (true) {
        val tag = input.readTag()
        if (tag == 0) break
        when (tag) {
            8 -> id = input.readInt64()
            18 -> code = input.readString()
            26 -> title = input.readString()
            34 -> subtitle = input.readString()
            42 -> category = input.readString()
            50 -> subCategory = input.readString()
            58 -> status = input.readString()
            64 -> priority = input.readInt32()
            74 -> region = input.readString()
            82 -> channel = input.readString()
            90 -> ownerName = input.readString()
            98 -> ownerTeam = input.readString()
            106 -> sourceSystem = input.readString()
            112 -> metricPv = input.readInt64()
            120 -> metricUv = input.readInt64()
            128 -> metricClick = input.readInt64()
            136 -> metricConvert = input.readInt64()
            // 18..21 是 double（wire type 1，tag = field*8+1）
            145 -> metricRevenue = input.readDouble()
            153 -> metricCost = input.readDouble()
            161 -> metricRoi = input.readDouble()
            169 -> metricBounceRate = input.readDouble()
            176 -> metricAvgDurationSec = input.readInt32()
            185 -> scoreQuality = input.readDouble()
            193 -> scoreRisk = input.readDouble()
            202 -> tagPrimary = input.readString()
            210 -> tagSecondary = input.readString()
            216 -> flagFeatured = input.readBool()
            224 -> flagAnomaly = input.readBool()
            234 -> notes = input.readString()
            240 -> observedAt = input.readInt64()
            248 -> windowStart = input.readInt64()
            256 -> windowEnd = input.readInt64()
            264 -> publishedAt = input.readInt64()
            272 -> createdAt = input.readInt64()
            280 -> updatedAt = input.readInt64()
            else -> if (!input.skipField(tag)) break
        }
    }

    return AnalyticsRecordProto(
        id = id,
        code = code,
        title = title,
        subtitle = subtitle,
        category = category,
        subCategory = subCategory,
        status = status,
        priority = priority,
        region = region,
        channel = channel,
        ownerName = ownerName,
        ownerTeam = ownerTeam,
        sourceSystem = sourceSystem,
        metricPv = metricPv,
        metricUv = metricUv,
        metricClick = metricClick,
        metricConvert = metricConvert,
        metricRevenue = metricRevenue,
        metricCost = metricCost,
        metricRoi = metricRoi,
        metricBounceRate = metricBounceRate,
        metricAvgDurationSec = metricAvgDurationSec,
        scoreQuality = scoreQuality,
        scoreRisk = scoreRisk,
        tagPrimary = tagPrimary,
        tagSecondary = tagSecondary,
        flagFeatured = flagFeatured,
        flagAnomaly = flagAnomaly,
        notes = notes,
        observedAt = observedAt,
        windowStart = windowStart,
        windowEnd = windowEnd,
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ).toRow()
}
