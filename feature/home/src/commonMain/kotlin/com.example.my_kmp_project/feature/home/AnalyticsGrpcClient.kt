package com.example.my_kmp_project.feature.home

/** 数据分析列表分页（对应服务端 `analytics.v1.ListAnalyticsRecordsResponse`）。 */
data class AnalyticsPage(
    val items: List<AnalyticsRecordRow>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

/**
 * 数据分析 gRPC 客户端（`analytics.v1.AnalyticsService`，明文 :9090）。
 *
 * 仅 Android 走真实 gRPC；iOS / OHOS 无产物，调用直接抛 [UnsupportedOperationException]。
 */
expect class AnalyticsGrpcClient() {
    /** 分页拉取记录，[page] 从 1 开始，[pageSize] 服务端上限 50。 */
    suspend fun listRecords(page: Int, pageSize: Int): AnalyticsPage

    /** 拉取单条记录；服务端无对应 id 时返回 null。 */
    suspend fun getRecord(id: Long): AnalyticsRecordRow?
}
