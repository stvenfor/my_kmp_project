package com.example.my_kmp_project.feature.home



data class HomeListRow(val title: String, val subtitle: String)

data class UsedCarOrderRow(
    val id: String,
    val kindLabel: String,
    val statusLabel: String,
    val submittedDate: String,
    val vehicleModel: String,
    val plateNo: String,
    val modelYear: Int,
    val mileageKm: Int,
    val amountLabel: String,
    val amount: Int,
    val customerName: String,
    val vin: String = "",
    val phone: String = "",
)

data class NewCarFollowRow(
    val id: String,
    val customerName: String,
    val phone: String,
    val vehicle: String,
    val stage: String,
    val intentBand: String,
    val nextFollow: String,
    val owner: String,
    val overdue: Boolean = false,
)

data class AnalyticsRecordRow(
    val code: String,
    val title: String,
    val subtitle: String,
    val status: String,
    val pv: Int,
    val uv: Int,
    val clicks: Int,
    val converts: Int,
    val roi: Double,
    val featured: Boolean = false,
    val anomaly: Boolean = false,
    /** 服务端记录 id（详情页 `GetAnalyticsRecord` 用）。 */
    val id: Long = 0L,
)
