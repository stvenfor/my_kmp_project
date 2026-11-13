package com.example.my_kmp_project.component.push

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 推送注册上报：`POST /api/v1/push/devices`（契约表 §2.17 行 #99）。
 *
 * 逐行对齐 Dart `components/linking/lib/push/push_registration_api.dart:9-62`（d7876a1）：
 * - 请求体 **snake_case**：`registration_id` / `alias` / `device_id` / `platform` / `mock`（:39-45）
 * - `alias` / `device_id` **空则不写键**（:41-42 的条件收集），不是写空串
 * - `platform` 取值只有四种：`harmony` / `ios` / `android` / `unknown`（:54-61）
 * - `mock` 恒为 `false`（:44）——注意与请求参数 [report] 的 `mock` 是两回事：
 *   后者只是**本地开关**，为 true 时连日志都打、但**不发请求**（:34）
 * - `registrationId` 为空同样直接返回（:34）
 * - 响应**不解析**（Dart 的 converter 收到什么就返回一个 Map，从不做信封校验，:46-47），
 *   因此这里用 [ApiClient.postRaw] 并把返回值丢弃；
 * - **异常一律吞掉**（:49-51 的 `try/catch` + warn 日志），绝不对调用方抛出。
 *
 * // GAP(R5): Dart 的 registrationId / alias 来自 JPush（iOS/Android）与
 * // 华为 HPush（ohos）厂商 SDK；本仓库没有这些 SDK，
 * // [PlatformPushSdk.fetchRegistrationToken] 仍是占位（见 `PushBridge.kt:99-104`）。
 * // 因此「拿到真实 token」这条链路只有接口、没有实现，不虚构可用性。
 */
public class PushRegistrationApi(
    private val client: ApiClient = NetworkFacade.api(),
    private val analytics: PushRegistrationAnalytics = NoopPushRegistrationAnalytics,
    private val log: (String) -> Unit = { println(it) },
) {
    /**
     * Dart `report({required registrationId, alias, deviceId, platform, mock})`（:17-52）。
     *
     * **`alias` 是无效参数**：后端 `jpush_controller.go:53-59` `RegisterDevice` 为越权防护
     * **强制忽略客户端 alias**，写入时以 `Alias: user.ID`（会话用户）覆盖（h4 验收口径）。
     * 即客户端传任何 alias 都不影响注册结果。此处保留该键只为对齐既有请求体形状
     * （Dart 侧仍发 `alias`），**不改请求行为**、不删字段；后端收敛时再一并移除。
     */
    public suspend fun report(
        registrationId: String,
        /** 后端强制以会话用户覆盖，传值无效（见函数 KDoc）；仅保留在请求体形状里。 */
        alias: String? = null,
        deviceId: String? = null,
        platform: String? = null,
        mock: Boolean = false,
    ) {
        log(
            "[PushRegistrationApi] mock=$mock registrationId=$registrationId " +
                "alias=$alias platform=$platform",
        )
        analytics.trackPushRegister(
            registrationId = registrationId,
            alias = alias,
            mock = mock,
        )

        if (mock || registrationId.isEmpty()) return

        try {
            client.postRaw(
                path = PATH,
                body = NetworkJson.encodeToString(
                    JsonObject.serializer(),
                    buildJsonObject {
                        put(KEY_REGISTRATION_ID, registrationId)
                        if (!alias.isNullOrEmpty()) put(KEY_ALIAS, alias)
                        if (!deviceId.isNullOrEmpty()) put(KEY_DEVICE_ID, deviceId)
                        put(KEY_PLATFORM, platform ?: detectPlatform())
                        put(KEY_MOCK, false)
                    },
                ),
            )
        } catch (t: Throwable) {
            // Dart :50 —— `LogUtils.w('[PushRegistrationApi] report failed: $e')`，不重抛。
            log("[PushRegistrationApi] report failed: $t")
        }
    }

    public companion object {
        /** Dart `static const path`（:13）。 */
        public const val PATH: String = "/api/v1/push/devices"

        const val KEY_REGISTRATION_ID: String = "registration_id"
        const val KEY_ALIAS: String = "alias"
        const val KEY_DEVICE_ID: String = "device_id"
        const val KEY_PLATFORM: String = "platform"
        const val KEY_MOCK: String = "mock"

        /** 契约表 §2.17 #99 允许的四个平台值。 */
        public const val PLATFORM_HARMONY: String = "harmony"
        public const val PLATFORM_IOS: String = "ios"
        public const val PLATFORM_ANDROID: String = "android"
        public const val PLATFORM_UNKNOWN: String = "unknown"

        /**
         * Dart `_detectPlatform()`（:54-61）：`kIsWeb → unknown`，`ohos → harmony`，
         * 其余按 `Platform.isIOS` / `isAndroid`。
         * KMP 侧用既有的 [PlatformPushSdk.platformName]（android / ios / ohos）映射，
         * 映射不到 ⇒ `unknown`（等价 Dart 的兜底分支）。
         */
        public fun detectPlatform(): String = when (PlatformPushSdk.platformName()) {
            "ohos" -> PLATFORM_HARMONY
            PLATFORM_IOS -> PLATFORM_IOS
            PLATFORM_ANDROID -> PLATFORM_ANDROID
            else -> PLATFORM_UNKNOWN
        }
    }
}

/**
 * 上报埋点缝隙（Dart `LinkingAnalytics.trackPushRegister`，:28-32）。
 * `core/analytics` 目前是占位骨架，故这里只留接缝，默认 [NoopPushRegistrationAnalytics]。
 */
public fun interface PushRegistrationAnalytics {
    public fun trackPushRegister(registrationId: String, alias: String?, mock: Boolean)
}

/** 默认实现：不埋点（不假装已上报）。 */
public object NoopPushRegistrationAnalytics : PushRegistrationAnalytics {
    override fun trackPushRegister(registrationId: String, alias: String?, mock: Boolean) = Unit
}
