package com.example.my_kmp_project.component.auth

/**
 * 登录策略契约（策略模式第 0 批）。
 *
 * 分层约定：
 * - 本文件只放**纯契约**，不依赖任何 feature、网络实现或平台 SDK；
 * - 具体策略（密码 / 短信 / 微信 / QQ / 华为）由 `feature:auth` 实现；
 * - 会话落库、错误兜底由上层 Orchestrator 统一处理，策略只负责「拿到会话载荷」。
 */

/**
 * 策略登录成功后返回的会话载荷。
 *
 * 刻意不直接返回 `LoggedInUser`：展示名 / userId 的兜底规则和落库动作应当只有一处，
 * 避免每个登录方式各写一份、后续新增方式漏掉字段。
 */
public data class AuthSessionPayload(
    public val token: String,
    public val refreshToken: String = "",
    public val sessionId: String = "",
    public val userId: String = "",
    public val displayName: String = "",
    public val email: String = "",
    public val phone: String = "",
    public val avatarUrl: String = "",
    public val deviceId: String = "",
)

/** 一次登录请求的凭据。 */
public sealed interface LoginCredentials {

    /** 账号密码（邮箱或用户名）。 */
    public data class Password(
        public val account: String,
        public val password: String,
    ) : LoginCredentials

    /** 手机号 + 验证码。 */
    public data class PhoneOtp(
        public val phone: String,
        public val code: String,
    ) : LoginCredentials

    /** 邮箱 + 验证码（后端契约就绪后启用）。 */
    public data class EmailOtp(
        public val email: String,
        public val code: String,
    ) : LoginCredentials

    /**
     * 交互式登录：策略内部拉起原生 SDK 取授权码（Compose 登录页使用）。
     * 微信 / QQ / 华为等无输入框的方式走这个。
     */
    public data object Interactive : LoginCredentials

    /**
     * 原生壳已自行取到授权码，直接把 code 交给策略（SwiftUI / ArkTS 使用）。
     * 这样 iOS / 鸿蒙不需要把第三方 SDK 桥进 Kotlin。
     *
     * [deviceId] 供华为等需要显式设备标识的方式使用；空白时策略回落到
     * `AuthDeviceContext.deviceId()`。
     */
    public data class ExternalCode(
        public val code: String,
        public val deviceId: String = "",
    ) : LoginCredentials
}

/** 登录结果。取消是独立分支，UI 不应把它当错误提示。 */
public sealed interface LoginOutcome {

    public data class Success(
        public val session: AuthSessionPayload,
    ) : LoginOutcome

    public data class Failure(
        public val error: LoginError,
    ) : LoginOutcome

    public data object Cancelled : LoginOutcome
}

/**
 * 统一登录错误。
 *
 * [code] 是跨 Compose / Swift / ArkTS 的稳定标识（原生活跃层可按它做分支）；
 * [clearsSession] 由策略判定（互踢 / 会话失效等），UI 不解析 HTTP 业务码。
 */
public sealed class LoginError(
    public val code: String,
    override val message: String,
    public val clearsSession: Boolean = false,
) : Exception(message) {

    /** 本地输入校验失败（空账号、密码太短、验证码格式错误等）。 */
    public class InvalidInput(
        message: String,
    ) : LoginError("invalid_input", message)

    /** 当前平台 / 当前构建不可用（SDK 未接、功能开关关闭、后端无契约）。 */
    public class Unavailable(
        public val methodId: String,
        reason: String? = null,
    ) : LoginError("unavailable", reason ?: "当前设备暂不支持该登录方式")

    /** 第三方授权被拒绝或 SDK 返回业务错误。 */
    public class ProviderRejected(
        message: String,
    ) : LoginError("provider_rejected", message)

    /** 网络不可达 / 超时。 */
    public class Network(
        message: String,
    ) : LoginError("network", message)

    /** 服务端返回业务错误；保留原始码供上层决策与埋点。 */
    public class Server(
        message: String,
        public val bizCode: Int? = null,
        public val httpStatus: Int? = null,
    ) : LoginError("server", message)

    /** 会话已废（互踢 / token 失效 / 账号不可用），调用方应清本地会话回登录页。 */
    public class SessionGone(
        message: String,
    ) : LoginError("session_gone", message, clearsSession = true)

    /** 兜底：无法归类的异常。 */
    public class Unknown(
        message: String,
    ) : LoginError("unknown", message)
}

/** 某种登录方式当前是否可用。 */
public sealed interface LoginAvailability {

    public data object Available : LoginAvailability

    public data class Unavailable(
        public val reason: String,
    ) : LoginAvailability
}

/**
 * 登录页渲染所需的描述信息。
 *
 * 同时暴露可用与不可用方式：UI 可以决定隐藏，或置灰并展示 [unavailableReason]。
 */
public data class LoginMethodDescriptor(
    public val id: String,
    public val displayName: String,
    public val interactive: Boolean = false,
    public val order: Int = 100,
    public val availability: LoginAvailability = LoginAvailability.Available,
) {
    public val available: Boolean
        get() = availability is LoginAvailability.Available

    public val unavailableReason: String?
        get() = (availability as? LoginAvailability.Unavailable)?.reason
}

/**
 * 一种登录方式。
 *
 * 实现约束：
 * - 必须无状态，可被多个登录页并发复用；
 * - 只负责「取凭证 → 换会话」，不写 `AccountFacade` / `AuthSessionState`；
 * - 取消协程必须原样上抛 `CancellationException`，不要吞成失败。
 */
public interface LoginStrategy {

    /** 稳定 id，用于 UI 排序、原生桥、开关与埋点（如 `password` / `wechat`）。 */
    public val id: String

    /** 展示名（如「微信登录」）。 */
    public val displayName: String

    /** 是否需要拉起原生 SDK 取码（无输入框）。 */
    public val interactive: Boolean
        get() = false

    /** UI 中的排序权重，越小越靠前。 */
    public val order: Int
        get() = 100

    /** 当前是否可用；默认可用，平台策略按 SDK / 开关 / 后端契约覆盖。 */
    public fun availability(): LoginAvailability = LoginAvailability.Available

    /** 执行登录。 */
    public suspend fun login(credentials: LoginCredentials): LoginOutcome
}

/**
 * 带“发送验证码”能力的登录方式。
 *
 * 发送验证码不是登录本身，所以不放进 [LoginStrategy] 必选方法；登录页可
 * `strategy as? OtpChallengeStrategy` 后展示重发入口。
 */
public interface OtpChallengeStrategy : LoginStrategy {

    public suspend fun sendOtp(target: String): Result<Unit>
}

/**
 * 登录页唯一的领域入口。
 *
 * Compose / SwiftUI / ArkTS 只依赖本接口：列方式、提交登录、消费统一错误，
 * 不直接引用 `AuthRepository`，也不写 `when (method)` 业务分支。
 */
public interface LoginController {

    /** 全部方式（含不可用），按 [LoginStrategy.order] 排序。 */
    public fun descriptors(): List<LoginMethodDescriptor>

    /**
     * 按方式 id 登录。
     *
     * @param methodId 必须是 [LoginStrategy.id]；未知 id 返回
     *   [LoginError.Unavailable]，不要抛异常。
     */
    public suspend fun login(methodId: String, credentials: LoginCredentials): LoginOutcome
}
