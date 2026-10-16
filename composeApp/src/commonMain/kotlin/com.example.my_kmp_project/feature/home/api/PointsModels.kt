package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.FlexibleIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Flutter 签到 / 积分契约模型（`features/home/lib/home/model/points_models.dart`，d7876a1）。
 * 契约表 #22–#25。可空性策略见 [TransactionModel] 的 KDoc。
 *
 * 布尔线上字段一律读成 [JsonElement]（判断见 [homeWireLiteralTrue]）：
 * Dart 用的是 `json['k'] == true`，对非布尔值宽容当 false，
 * 而 kotlinx 遇到 `"signed":"yes"` 这类原始值会抛并让整个 `data` 被静默丢弃。
 */

/** #22 `GET /points/status` 的日历格（Dart `CheckInDayView`，points_models.dart:32-53）。 */
@Serializable
public data class CheckInDayView(
    @SerialName("day") val dayRaw: String? = null,
    @SerialName("signed") val signedRaw: JsonElement? = null,
    @SerialName("reward")
    @Serializable(with = FlexibleIntSerializer::class) val rewardRaw: Int? = null,
    @SerialName("is_today") val isTodayRaw: JsonElement? = null,
) {
    /** Dart `json['day']?.toString() ?? ''`（:42）。 */
    public val day: String get() = dayRaw.orEmpty()

    /** Dart `json['signed'] == true`（:43）——只有真 `true` 才算签过。 */
    public val signed: Boolean get() = homeWireLiteralTrue(signedRaw)

    /** Dart `(json['reward'] as num?)?.toInt() ?? 5`（:44）——缺省 5。 */
    public val reward: Int get() = rewardRaw ?: 5

    /** Dart `json['is_today'] == true`（:45）。 */
    public val isToday: Boolean get() = homeWireLiteralTrue(isTodayRaw)
}

/** #22 `GET /points/status` 的 `data`（Dart `PointsStatus`，:2-30）。 */
@Serializable
public data class PointsStatus(
    @SerialName("balance")
    @Serializable(with = FlexibleIntSerializer::class) val balanceRaw: Int? = null,
    @SerialName("checked_in_today") val checkedInTodayRaw: JsonElement? = null,
    @SerialName("streak")
    @Serializable(with = FlexibleIntSerializer::class) val streakRaw: Int? = null,
    @SerialName("today_reward")
    @Serializable(with = FlexibleIntSerializer::class) val todayRewardRaw: Int? = null,
    @SerialName("calendar") val calendarRaw: List<CheckInDayView>? = null,
) {
    /** Dart `(json['balance'] as num?)?.toInt() ?? 0`（:14）。 */
    public val balance: Int get() = balanceRaw ?: 0

    /** Dart `json['checked_in_today'] == true`（:15）。 */
    public val checkedInToday: Boolean get() = homeWireLiteralTrue(checkedInTodayRaw)

    /** Dart `(json['streak'] as num?)?.toInt() ?? 0`（:16）。 */
    public val streak: Int get() = streakRaw ?: 0

    /** Dart `(json['today_reward'] as num?)?.toInt() ?? 5`（:17）——缺省 5。 */
    public val todayReward: Int get() = todayRewardRaw ?: 5

    /**
     * Dart `calendar`：`json['calendar'] as List? ?? const []`，非 map 元素跳过（:12-21）。
     * 整体缺失 / 为 `null` → 空表；与 Dart 的差别（可忽略）：日历里混入**标量**元素时
     * Dart 静默跳过，这里会让整个 [PointsStatus] 解码失败并被调用点抛「加载签到状态失败」。
     */
    public val calendar: List<CheckInDayView> get() = calendarRaw ?: emptyList()
}

/** #23 `POST /points/check-in` 的 `data`（Dart `CheckInResult`，:55-76）。 */
@Serializable
public data class CheckInResult(
    @SerialName("points")
    @Serializable(with = FlexibleIntSerializer::class) val pointsRaw: Int? = null,
    @SerialName("balance")
    @Serializable(with = FlexibleIntSerializer::class) val balanceRaw: Int? = null,
    @SerialName("streak")
    @Serializable(with = FlexibleIntSerializer::class) val streakRaw: Int? = null,
    @SerialName("day") val dayRaw: String? = null,
) {
    /** Dart `(json['points'] as num?)?.toInt() ?? 0`（:64）。 */
    public val points: Int get() = pointsRaw ?: 0

    /** Dart `(json['balance'] as num?)?.toInt() ?? 0`（:65）。 */
    public val balance: Int get() = balanceRaw ?: 0

    /** Dart `(json['streak'] as num?)?.toInt() ?? 0`（:66）。 */
    public val streak: Int get() = streakRaw ?: 0

    /** Dart `json['day']?.toString() ?? ''`（:67）。 */
    public val day: String get() = dayRaw.orEmpty()
}

/**
 * Dart `TaskProgress`（points_models.dart:78-114）。
 * 线上只有 `incomplete` / `claimable` / `claimed` 三态，**其他值一律归 `incomplete`**
 * （`TaskProgressX.parse` 的 default 分支，:104-113）。
 */
public enum class TaskProgress {
    Incomplete,
    Claimable,
    Claimed,
    ;

    public companion object {
        public fun parse(raw: String?): TaskProgress = when (raw) {
            "claimable" -> Claimable
            "claimed" -> Claimed
            else -> Incomplete
        }
    }
}

/** #24 `GET /points/tasks` 的行模型（Dart `GrowthTask`，:80-101）。 */
@Serializable
public data class GrowthTask(
    @SerialName("code") val codeRaw: String? = null,
    @SerialName("title") val titleRaw: String? = null,
    @SerialName("reward")
    @Serializable(with = FlexibleIntSerializer::class) val rewardRaw: Int? = null,
    @SerialName("progress") val progressRaw: String? = null,
) {
    /**
     * 任务码是**字符串**（Dart `json['code']?.toString() ?? ''`，:90）；
     * #25 会把它原样拼进 URL。数字下发时由 `NetworkJson.isLenient` 读成其字面量，
     * 与 Dart 的 `toString()` 等价。
     */
    public val code: String get() = codeRaw.orEmpty()

    /** Dart `json['title']?.toString() ?? ''`（:91）。 */
    public val title: String get() = titleRaw.orEmpty()

    /** Dart `(json['reward'] as num?)?.toInt() ?? 0`（:92）。 */
    public val reward: Int get() = rewardRaw ?: 0

    /** Dart `TaskProgressX.parse(json['progress']?.toString())`（:93）。 */
    public val progress: TaskProgress get() = TaskProgress.parse(progressRaw)
}

/** #25 `POST /points/tasks/:code/claim` 的 `data`（Dart `ClaimTaskResult`，:116-134）。 */
@Serializable
public data class ClaimTaskResult(
    @SerialName("points")
    @Serializable(with = FlexibleIntSerializer::class) val pointsRaw: Int? = null,
    @SerialName("balance")
    @Serializable(with = FlexibleIntSerializer::class) val balanceRaw: Int? = null,
    @SerialName("day") val dayRaw: String? = null,
) {
    /** Dart `(json['points'] as num?)?.toInt() ?? 0`（:124）。 */
    public val points: Int get() = pointsRaw ?: 0

    /** Dart `(json['balance'] as num?)?.toInt() ?? 0`（:125）。 */
    public val balance: Int get() = balanceRaw ?: 0

    /** Dart `json['day']?.toString() ?? ''`（:126）。 */
    public val day: String get() = dayRaw.orEmpty()
}
