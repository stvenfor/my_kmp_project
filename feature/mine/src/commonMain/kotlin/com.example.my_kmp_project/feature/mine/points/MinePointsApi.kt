package com.example.my_kmp_project.feature.mine.points

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.postApiDecoded
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 签到 / 积分 / 成长任务的真实后端契约（`/api/v1/points` 下，`ApiClient` 由 App 层注入）。 */
public object MinePointsPaths {
    public const val STATUS: String = "/api/v1/points/status"
    public const val CHECK_IN: String = "/api/v1/points/check-in"
    public const val TASKS: String = "/api/v1/points/tasks"
    public fun claim(code: String): String = "/api/v1/points/tasks/$code/claim"
}

@Serializable
public data class MinePointsStatus(
    val balance: Long = 0,
    @SerialName("checked_in_today") val checkedInToday: Boolean = false,
    val streak: Int = 0,
    @SerialName("today_reward") val todayReward: Long = 0,
)

@Serializable
public data class MinePointsTask(
    val code: String = "",
    val title: String = "",
    val reward: Long = 0,
    /** incomplete | claimable | claimed */
    val progress: String = "incomplete",
) {
    public val isClaimable: Boolean get() = progress == "claimable"
    public val progressLabel: String get() = when (progress) {
        "claimable" -> "可领取"
        "claimed" -> "已领取"
        else -> "未完成"
    }
}

@Serializable
private data class MinePointsTasksData(
    val items: List<MinePointsTask> = emptyList(),
)

@Serializable
public data class MineCheckInResult(
    val points: Long = 0,
    val balance: Long = 0,
    val streak: Int = 0,
    val day: String = "",
)

public class MinePointsApi(private val api: ApiClient) {
    public suspend fun fetchStatus(): MinePointsStatus =
        requireData(api.getApi<MinePointsStatus>(MinePointsPaths.STATUS), "加载积分失败")

    public suspend fun checkIn(): MineCheckInResult =
        requireData(api.postApiDecoded(MinePointsPaths.CHECK_IN), "签到失败")

    public suspend fun fetchTasks(): List<MinePointsTask> =
        requireData(api.getApi<MinePointsTasksData>(MinePointsPaths.TASKS), "加载任务失败").items

    public suspend fun claim(code: String): MineCheckInResult =
        requireData(api.postApiDecoded(MinePointsPaths.claim(code)), "领取失败")

    private fun <T> requireData(response: ApiResponse<T>, fallback: String): T {
        val data = response.data
        if (!response.isSuccess || data == null) {
            throw NetworkError.Business(
                response.code,
                response.message?.takeIf { it.isNotEmpty() } ?: fallback,
            )
        }
        return data
    }
}
