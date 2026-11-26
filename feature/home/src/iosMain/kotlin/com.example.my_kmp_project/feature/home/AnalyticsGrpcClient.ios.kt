package com.example.my_kmp_project.feature.home

/**
 * iOS 未接入：数据分析页只展示「仅 Android 已接入」提示，不做任何本地伪造数据。
 */
actual class AnalyticsGrpcClient actual constructor() {
    actual suspend fun listRecords(page: Int, pageSize: Int): AnalyticsPage =
        throw UnsupportedOperationException(NOT_SUPPORTED_MESSAGE)

    actual suspend fun getRecord(id: Long): AnalyticsRecordRow? =
        throw UnsupportedOperationException(NOT_SUPPORTED_MESSAGE)

    private companion object {
        const val NOT_SUPPORTED_MESSAGE = "数据分析 gRPC 仅 Android 已接入"
    }
}
