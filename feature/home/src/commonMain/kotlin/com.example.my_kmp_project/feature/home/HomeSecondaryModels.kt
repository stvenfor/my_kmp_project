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

object HomeSecondaryMock {
    /** Prefer [HomeSecondaryStore] for mutable pipelines; kept for ledger/analytics. */
    val usedCarOrders: List<UsedCarOrderRow>
        get() = HomeSecondaryStore.usedCarOrders()
    val usedCars: List<HomeListRow>
        get() = usedCarOrders.map {
            HomeListRow(it.vehicleModel, "${it.amount / 10000.0}万 · ${it.mileageKm / 10000.0}万公里")
        }
    val ledger = listOf(
        LedgerTransaction(
            id = 1,
            type = "收入",
            category = "新车定金",
            amount = 5_000.0,
            date = "2026-09-24",
            note = "星越L 意向金",
        ),
        LedgerTransaction(
            id = 2,
            type = "支出",
            category = "售后配件",
            amount = 1_280.5,
            date = "2026-09-23",
            note = "工单 AS-441",
        ),
        LedgerTransaction(
            id = 3,
            type = "收入",
            category = "二手车过户费",
            amount = 12_600.0,
            date = "2026-09-22",
            note = null,
        ),
    )
    val analytics = listOf(
        HomeListRow("本周线索转化", "转化率 12.4% · 环比 +1.2pp"),
        HomeListRow("试驾到店", "到店 86 · 成交 11"),
    )
    val partners = listOf(
        HomeListRow("王小明", "销售顾问 · 待确认加入"),
        HomeListRow("李华", "售后技师 · 待确认加入"),
        HomeListRow("赵倩", "网销 · 待确认加入"),
    )
    val followUps = listOf(
        HomeListRow("张先生 · 星越L", "意向跟进 · 今日回访"),
        HomeListRow("刘女士 · 帝豪", "报价中 · 明电联"),
    )
    val appointments = listOf(
        HomeListRow("陈先生 · 保养", "今日 14:00 · 工位 A2"),
        HomeListRow("周女士 · 钣喷", "今日 16:30 · 工位 B1"),
    )
    val orders = listOf(
        HomeListRow("订单 #NC-9021", "星瑞 · 待门店审核"),
        HomeListRow("订单 #NC-9018", "缤越 · 待门店审核"),
    )
    val afterSales: List<HomeListRow>
        get() = HomeSecondaryStore.afterSalesDetails().map {
            HomeListRow(it.title, "${it.kindLabel} · ${it.customerName}")
        }
    val afterSalesDetails: List<AfterSalesDetailRow>
        get() = HomeSecondaryStore.afterSalesDetails()
    val newCars: List<HomeListRow>
        get() = HomeSecondaryStore.newCarFollows().map {
            HomeListRow("客户 ${it.customerName}", "${it.vehicle} · ${it.stage}")
        }
    val newCarFollows: List<NewCarFollowRow>
        get() = HomeSecondaryStore.newCarFollows()
}
