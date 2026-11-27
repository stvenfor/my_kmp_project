package com.example.my_kmp_project.feature.chat

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.platform.loadString
import com.example.my_kmp_project.core.platform.saveString
import com.example.my_kmp_project.core.uuid.AppUuid
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 契约表 §2.18 行 #100 —— Dart `components/rongcloud_im/lib/api/im_session_api.dart:8-33`
 * 的 `ImSessionResult`。
 *
 * 解码逐字对齐 `ImSessionResult.fromJson`（im_session_api.dart:19-25）：
 * - `user_id` **优先**，缺失 / 为 null 才取 `im_user_id`，两者都无 → 空串；
 * - `expires_in_seconds` 容忍 int / 其它 num / 整数字符串，缺失 → `0`（Dart `_asInt` :27-32）。
 */
public data class ImSessionResult(
    public val imUserId: String,
    public val token: String,
    public val expiresInSeconds: Int,
) {
    public companion object {
        public fun fromJson(json: JsonObject): ImSessionResult = ImSessionResult(
            imUserId = json.imJsonStr("user_id", "im_user_id"),
            token = json.imJsonStr("token"),
            expiresInSeconds = json.imJsonInt("expires_in_seconds") ?: 0,
        )
    }
}

/**
 * `POST /api/v1/im/session` 请求体（Dart im_session_api.dart:66-70）：
 * **只有一个可选键** `display_name`，trim 后为空则整个键省略（Dart 的 `body` 保持 `{}`）。
 * 身份不放 body —— 由会话头（`Authorization` / `X-Session-ID` / `X-Device-ID`）携带（契约表 §0）。
 *
 * [NetworkJson] 关掉了 `explicitNulls`，`displayName = null` 编码成 `{}`，与 Dart 空 map 等价；
 * 注意不能传空串请求体（Ktor 侧 `body.isEmpty()` 会**不发** body，Dart 始终发 `{}`）。
 */
@Serializable
public data class ImSessionRequest(
    @SerialName("display_name") val displayName: String? = null,
)

/**
 * Dart `components/rongcloud_im/lib/registry/im_user_id_registry.dart:8-45` 的 mock 身份映射：
 * 业务 `userId` ↔ 本地生成的 `im_u_<uuid 去横线前 16 位>`（:21），并持久化到
 * SP key `im_biz_to_im_user_id_json`（`commons/storage/lib/prefs/sp_keys.dart:6`）。
 *
 * 真实分支不使用它（Dart im_session_api.dart:88 注释：业务 UUID 就是融云 userId）。
 */
public class ImMockUserIdRegistry(
    private val load: () -> String? = { null },
    private val save: (String) -> Unit = { },
    private val newImUserId: () -> String = {
        // Dart: 'im_u_' + uuid.v4().replaceAll('-','').substring(0, 16)（im_user_id_registry.dart:21）
        "im_u_" + AppUuid.random().replace("-", "").take(16)
    },
) {
    private val cache: MutableMap<String, String> = mutableMapOf()
    private var loaded = false

    /** Dart `ImUserIdRegistry.resolveImUserId`（im_user_id_registry.dart:16-25）。 */
    public fun resolveImUserId(bizUserId: String): String {
        ensureLoaded()
        cache[bizUserId]?.let { return it }
        val imUserId = newImUserId()
        cache[bizUserId] = imUserId
        persist()
        return imUserId
    }

    /** 已知映射快照（同 Dart `_cache`，UI/调试可读）。 */
    public fun knownMappings(): Map<String, String> = cache.toMap()

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val raw = runCatching { load() }.getOrNull()
        if (raw.isNullOrEmpty()) return
        // Dart：jsonDecode 失败 → 退回空表（im_user_id_registry.dart:28-40）
        val parsed = runCatching { NetworkJson.decodeFromString(JsonObject.serializer(), raw) }
            .getOrNull() ?: return
        parsed.forEach { (bizUserId, element) ->
            val imUserId = (element as? JsonPrimitive)?.contentOrNull
            if (bizUserId.isNotBlank() && !imUserId.isNullOrEmpty()) cache[bizUserId] = imUserId
        }
    }

    private fun persist() {
        val json = buildJsonObject {
            cache.forEach { (bizUserId, imUserId) -> put(bizUserId, imUserId) }
        }
        runCatching { save(NetworkJson.encodeToString(JsonObject.serializer(), json)) }
    }
}

/** Dart `SpKeys.imBizToImUserId`（commons/storage/lib/prefs/sp_keys.dart:6）。 */
public const val IM_BIZ_TO_IM_USER_ID_KEY: String = "im_biz_to_im_user_id_json"

/**
 * 平台持久化版 mock 注册表（Dart 用 `SpManager`，im_user_id_registry.dart:30/:43）。
 * 默认构造的 [ImMockUserIdRegistry] 只在进程内记忆，启动侧接线时应改用它。
 */
public fun createPersistentImMockUserIdRegistry(
    key: String = IM_BIZ_TO_IM_USER_ID_KEY,
): ImMockUserIdRegistry = ImMockUserIdRegistry(
    load = { loadString(key) },
    save = { saveString(key, it) },
)

/**
 * Dart `ImSessionApi`（im_session_api.dart:36-92）—— `POST /api/v1/im/session` 取 IM Token。
 *
 * 分支（契约表 #100、§5.11）：
 * 1. **mock 分支**（`RongImConfig.useMockImFor` 为真，:53-63）：不发 HTTP，等 150ms 后由
 *    [ImMockUserIdRegistry] 给业务 id 生成/复用 `im_u_*`，token = `mock_rong_token_<epochMillis>`，
 *    `expires_in_seconds = 3600`；
 * 2. **真实分支**（:66-90）：body 只带可选 `display_name`；信封非 `code==0`、`data` 缺失、
 *    或 **`token` 为空串** ⇒ 抛错（文案取信封 `message`，兜底「获取 IM Token 失败」）。
 *
 * // GAP(R5): 真实分支拿到 token 后 Dart 立即 `RongEngineHolder.connectReal(session)` →
 * // `_engine!.connect(session.token, RongImConfig.connectTimeoutSeconds)`
 * // （components/rongcloud_im/lib/engine/rong_engine_holder.dart:42-86，connect 调用在 :58；
 * // mock 侧对应 `connectMock` :34-40）。融云 IMLib（`rongcloud_im_wrapper_plugin: ^5.44.0`）
 * // 在本工程 Android/iOS/OHOS 均无产物，故本 lane 只移植 BFF 侧；连接与收发尚未接入，
 * // 禁止伪装 SDK ready。
 */
public class ImSessionApi(
    private val client: ApiClient,
    private val registry: ImMockUserIdRegistry = ImMockUserIdRegistry(),
    private val useMock: () -> Boolean = { RongImConfig.useMockIm },
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val nowMs: () -> Long = ::imCurrentTimeMillis,
) {
    /**
     * Dart `ImSessionApi.createSession`（im_session_api.dart:49-91）。
     * [bizUserId] 只在 mock 分支用于换本地 `im_u_*`；真实分支**不进请求体**。
     */
    public suspend fun createSession(
        bizUserId: String,
        displayName: String? = null,
    ): ImSessionResult {
        if (useMock()) {
            sleep(MOCK_SESSION_DELAY_MS)
            val imUserId = registry.resolveImUserId(bizUserId)
            return ImSessionResult(
                imUserId = imUserId,
                token = "mock_rong_token_${nowMs()}",
                expiresInSeconds = MOCK_SESSION_EXPIRES_SECONDS,
            )
        }

        val name = displayName?.trim().orEmpty()
        // 空串 → null → 整个键省略（Dart :67-70）
        val body = NetworkJson.encodeToString(
            ImSessionRequest.serializer(),
            ImSessionRequest(displayName = name.ifEmpty { null }),
        )
        val response = client.postApi(RongImConfig.SESSION_PATH, body) { element ->
            (element as? JsonObject)?.let { ImSessionResult.fromJson(it) }
        }
        val data = response.data
        if (!response.isSuccess || data == null || data.token.isEmpty()) {
            throw NetworkError.Business(
                code = response.code,
                message = response.message ?: FAILURE_FALLBACK,
            )
        }
        return data
    }

    private companion object {
        /** Dart `Future.delayed(150ms)`（im_session_api.dart:54）。 */
        const val MOCK_SESSION_DELAY_MS: Long = 150L

        /** Dart mock 分支固定 3600（im_session_api.dart:63）。 */
        const val MOCK_SESSION_EXPIRES_SECONDS: Int = 3600

        /** Dart `'获取 IM Token 失败'`（im_session_api.dart:84）。 */
        const val FAILURE_FALLBACK: String = "获取 IM Token 失败"
    }
}

/** Dart `DateTime.now().millisecondsSinceEpoch` 的 common 对等。 */
@OptIn(ExperimentalTime::class)
public fun imCurrentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
