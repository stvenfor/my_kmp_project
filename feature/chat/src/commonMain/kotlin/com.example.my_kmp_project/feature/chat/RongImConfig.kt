package com.example.my_kmp_project.feature.chat

/**
 * Flutter `components/rongcloud_im/lib/config/rong_im_config.dart:1-30` 的 KMP 对等（契约层，
 * 覆盖契约表 §2.18 行 #100 / #101 / #102 / #109 / #110 / #111 / #112 / #113 / #114
 * 的路径与 Mock 开关）。
 *
 * Mock 开关（§5.11「mock 开关要一并移植」）在 Dart 由两处决定，这里逐条保留：
 * - `--dart-define=USE_MOCK_IM=true`（编译期常量，rong_im_config.dart:7）→ KMP 没有 dart-define，
 *   改为运行时显式开关 [forceMockIm]（默认 false，等价「未传 dart-define」）；
 * - `RONG_APP_KEY` 为空或含 `PLACEHOLDER`（rong_im_config.dart:9-10）→ [useMockImFor]。
 *
 * [rongAppKey] 默认空串，与 Dart `const String.fromEnvironment('RONG_APP_KEY', defaultValue: '')`
 * （rong_im_config.dart:15）一致 ⇒ **默认走 mock 分支**；真实融云 SDK 不在本工程可用
 * （R5 gap），本文件绝不伪称 SDK ready。
 *
 * 好友侧路径（`/im/friends*`、`/im/users/search`、`/im/private/admission`）在 Dart 同属本文件
 * （rong_im_config.dart:21-24），KMP 因「feature 不得互相 import」拆到
 * `feature/friend/ImFriendApi.kt` 的 `ImFriendApiPaths`，两侧常量取值必须一致。
 */
public object RongImConfig {

    /** Dart `RongImConfig.sessionPath`（rong_im_config.dart:18）。 */
    public const val SESSION_PATH: String = "/api/v1/im/session"

    /** Dart `RongImConfig.profilePath`（rong_im_config.dart:19）。 */
    public const val USERS_PROFILE_PATH: String = "/api/v1/im/users/profile"

    /** Dart `RongImConfig.backupPath`（rong_im_config.dart:20）。 */
    public const val MESSAGES_BACKUP_PATH: String = "/api/v1/im/messages/backup"

    /** Dart `RongImConfig.freeGroupPath`（rong_im_config.dart:25）。 */
    public const val GROUPS_FREE_PATH: String = "/api/v1/im/groups/free"

    /** Dart `RongImConfig.storeGroupSyncPath`（rong_im_config.dart:26）。 */
    public const val GROUPS_STORE_SYNC_PATH: String = "/api/v1/im/groups/store/sync"

    /** Dart `RongImConfig.connectTimeoutSeconds`（rong_im_config.dart:28）；真实 SDK 连接超时（R5）。 */
    public const val CONNECT_TIMEOUT_SECONDS: Int = 15

    /** Dart `RongImConfig.profileCacheTtl`（rong_im_config.dart:29，24 小时）。 */
    public const val PROFILE_CACHE_TTL_MS: Long = 24L * 60L * 60L * 1000L

    /**
     * 等价 `--dart-define=USE_MOCK_IM=true`（rong_im_config.dart:7）：
     * 为 true 时无条件走 mock 分支，不看 [rongAppKey]。
     */
    public var forceMockIm: Boolean = false

    /**
     * 等价 `String.fromEnvironment('RONG_APP_KEY', defaultValue: '')`（rong_im_config.dart:15）；
     * 真实 AppKey 由启动侧注入（`EnvironmentService.rongAppKey`，Dart im_session_api.dart:47）。
     */
    public var rongAppKey: String = ""

    /**
     * Dart `RongImConfig.useMockImFor`（rong_im_config.dart:6-11）**逐行对等**：
     * 强制开关 → trim 后空串 → 大写后包含 `PLACEHOLDER`。
     */
    public fun useMockImFor(appKey: String?): Boolean {
        if (forceMockIm) return true
        val key = appKey?.trim().orEmpty()
        return key.isEmpty() || key.uppercase().contains("PLACEHOLDER")
    }

    /** Dart `RongImConfig.useMockIm`（rong_im_config.dart:14-16）。 */
    public val useMockIm: Boolean get() = useMockImFor(rongAppKey)
}

/**
 * Flutter `ImGroupApi` 自由群动作路径（im_group_api.dart:90/:99/:105/:109
 * 的 `'${RongImConfig.freeGroupPath}/$groupId/<action>'`）。
 *
 * 契约表 #110（`invite`）、#111（`kick`）、#112（`quit`）、#113（`dismiss`）四条
 * 都是 `POST 本路径/$groupId/<动作>`，路径模板只有尾段不同。
 */
public fun imFreeGroupActionPath(groupId: String, action: String): String =
    "${RongImConfig.GROUPS_FREE_PATH}/$groupId/$action"
