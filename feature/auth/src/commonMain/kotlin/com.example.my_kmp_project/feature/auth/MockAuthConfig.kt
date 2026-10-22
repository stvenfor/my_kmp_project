package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.platformIsReleaseBuild
import kotlin.random.Random

/**
 * 本地 Mock 认证开关 —— Flutter `commons/core/lib/dev/mock_auth_service.dart` 的 KMP 移植。
 *
 * Flutter 侧由 `.env` 的 `USE_MOCK_AUTH` 决定 `MockAuthService` / `BackendAuthService`；
 * KMP 没有 dart-define 那套注入，改为「debug 默认开、release 自动关」：
 * 默认值取 [platformIsReleaseBuild] 的反面，Android 入口再用
 * `ApplicationInfo.FLAG_DEBUGGABLE` 覆盖一次（见 `MainActivity`），避免 debug 库变体误判。
 *
 * 语义与 Flutter 完全一致：
 * - 手机号白名单 [mockTestPhones]（含 `13400000000`）→ [mockOtpCode] 即登录成功；
 * - 白名单之外的号码在 Mock 下被拒，提示语与 Flutter 相同；
 * - **不请求 Go BFF**，也不写任何远端会话，token 是本地假 token。
 *
 * Release 下 [useMockAuth] 为 false，所有调用照旧走 [UserAuthHttpApi] 真实链路。
 */
public object MockAuthConfig {

    /**
     * Mock 环境下固定测试手机号（与 Go `AUTH_DEV_TEST_PHONE` 及 Flutter
     * `MockAuthService.mockTestPhones` 对齐）。
     */
    public val mockTestPhones: Set<String> = setOf(
        "13400000000",
        "13400000001",
        "13400000002",
        "13400000003",
        "13400000004",
    )

    /** Mock 环境固定验证码。 */
    public const val mockOtpCode: String = "123456"

    /** 兼容旧引用（Flutter 同名常量）。 */
    public const val mockTestPhone: String = "13400000000"

    /** 每个测试号对应的展示名（Flutter `MockAuthService._displayNames`）。 */
    private val displayNames: Map<String, String> = mapOf(
        "13400000000" to "测试甲",
        "13400000001" to "测试乙",
        "13400000002" to "测试丙",
        "13400000003" to "测试丁",
        "13400000004" to "测试戊",
    )

    /**
     * 是否启用本地 Mock 认证。
     *
     * 默认：debug 开、release 关（[platformIsReleaseBuild] 取反）。
     * 平台入口可在启动时覆盖为真实 debug 标志。
     */
    public var useMockAuth: Boolean = !platformIsReleaseBuild()

    /** 白名单校验（入参允许带 `+86` / 空格 / 连字符）。 */
    public fun isMockPhone(raw: String): Boolean =
        AuthPhoneUtils.normalizeDigits(raw) in mockTestPhones

    /** Mock 下非白名单号码的统一提示语（与 Flutter 逐字一致）。 */
    public fun rejectedPhoneMessage(): String =
        "测试环境请使用 ${mockTestPhones.joinToString(" / ")}"

    /** 测试号展示名；非白名单回落为「用户 + 后四位」。 */
    public fun displayNameFor(raw: String): String {
        val digits = AuthPhoneUtils.normalizeDigits(raw)
        return displayNames[digits] ?: "用户${digits.takeLast(4)}"
    }

    /** `134****0000` 形态的脱敏手机号（Flutter `MockAuthService` 同款规则）。 */
    public fun maskedPhone(raw: String): String {
        val digits = AuthPhoneUtils.normalizeDigits(raw)
        return if (digits.length >= 11) {
            "${digits.take(3)}****${digits.drop(7)}"
        } else {
            digits
        }
    }

    /**
     * Mock 会话 token。Flutter 用 `DateTime.now().millisecondsSinceEpoch`，
     * KMP 这里只要求「每次登录不同」，用自增序号 + 随机后缀即可，
     * 免去为一个假 token 引入 expect/actual 平台时钟。
     */
    public fun issueToken(prefix: String): String {
        tokenSeq += 1
        return "mock_${prefix}_token_${tokenSeq}_${Random.nextInt(100_000, 999_999)}"
    }

    private var tokenSeq: Long = 0L
}
