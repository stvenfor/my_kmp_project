package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.feature.home.aftersales.AfterSalesApi
import com.example.my_kmp_project.feature.home.aftersales.AfterSalesRecord
import com.example.my_kmp_project.feature.home.newcar.NewCarFollowApi
import com.example.my_kmp_project.feature.home.newcar.NewCarFollowFile
import com.example.my_kmp_project.feature.home.newcar.NewCarFollowTab
import com.example.my_kmp_project.feature.home.usedcar.HomeUsedCarApi
import com.example.my_kmp_project.feature.home.usedcar.HomeUsedCarCustomer
import com.example.my_kmp_project.feature.home.usedcar.HomeUsedCarOrderCreateRequest
import com.example.my_kmp_project.feature.home.usedcar.HomeUsedCarOrderItem
import com.example.my_kmp_project.feature.home.usedcar.HomeUsedCarStats
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 首页二级流水线（二手车 / 售后 / 新车跟进）：远端 API 缓存 + 选择态。
 *
 * - 二手车：`HomeUsedCarApi`（`/api/v1/used-car-orders`，摘要 / 列表 / 客户 / 建单）
 * - 售后：`AfterSalesApi`（`/api/v1/after-sales`，列表 / 建单，`can_create` 决定新建入口）
 * - 新车跟进：`NewCarFollowApi`（`/api/v1/new-car-follow-files`，列表 / 建档）
 *
 * 网络失败只写 [errorMessage]，绝不本地伪造插入；选中 id 仅驱动详情页。
 */
object HomeSecondaryStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedUsedCarId by mutableStateOf<String?>(null)
    var selectedAfterSalesId by mutableStateOf<String?>(null)
    var selectedNewCarFollowId by mutableStateOf<String?>(null)

    /** 数据分析选中记录 id（0 = 未选择），驱动详情页 `GetAnalyticsRecord`。 */
    var selectedAnalyticsId by mutableStateOf(0L)
        private set

    /** 数据台账选中行 id（`LedgerDetailScreen` 用）。 */
    var selectedLedgerId by mutableStateOf<Int?>(null)
        private set

    /** 选中台账行。 */
    fun selectLedger(id: Int) {
        selectedLedgerId = id
    }

    /** `GET /after-sales/records` 的 `can_create`（服务端 gate「新建工单」入口）。 */
    var afterSalesCanCreate by mutableStateOf(false)
        private set

    /** 二手车建单可选的客户（`GET /used-car-orders/customers`）。 */
    var usedCarCustomers by mutableStateOf<List<HomeUsedCarCustomer>>(emptyList())
        private set

    /** 二手车摘要统计（`GET /used-car-orders/summary`）。 */
    var usedCarStats by mutableStateOf(HomeUsedCarStats())
        private set

    private var usedCars by mutableStateOf<List<UsedCarOrderRow>>(emptyList())
    private var afterSales by mutableStateOf<List<AfterSalesDetailRow>>(emptyList())
    private var newCarRows by mutableStateOf<List<NewCarFollowRow>>(emptyList())

    fun usedCarOrders(): List<UsedCarOrderRow> = usedCars

    fun findUsedCar(id: String?): UsedCarOrderRow? =
        id?.let { wanted -> usedCars.firstOrNull { it.id == wanted } }

    fun selectUsedCar(id: String) {
        selectedUsedCarId = id
    }

    fun afterSalesDetails(): List<AfterSalesDetailRow> = afterSales

    fun findAfterSales(id: String?): AfterSalesDetailRow? =
        id?.let { wanted -> afterSales.firstOrNull { it.id == wanted } }

    fun selectAfterSales(id: String) {
        selectedAfterSalesId = id
    }

    fun newCarFollows(): List<NewCarFollowRow> = newCarRows

    fun findNewCarFollow(id: String?): NewCarFollowRow? =
        id?.let { wanted -> newCarRows.firstOrNull { it.id == wanted } }

    fun selectNewCarFollow(id: String) {
        selectedNewCarFollowId = id
    }

    fun selectAnalytics(id: Long) {
        selectedAnalyticsId = id
    }

    /** 拉取二手车业务单（列表为核心）；摘要失败只影响统计栏，不阻塞列表。 */
    suspend fun refreshUsedCars(api: HomeUsedCarApi = HomeUsedCarApi()): Result<Unit> = runCatching {
        loading = true
        usedCars = api.fetchList().map { it.toRow() }
        usedCarStats = runCatching { api.fetchSummary().resolvedStats }.getOrDefault(usedCarStats)
        errorMessage = null
    }.onFailure {
        errorMessage = it.message ?: "加载二手车业务单失败"
    }.also {
        loading = false
        bump()
    }

    /** 拉取建单可选客户；失败只记录 [errorMessage]。 */
    suspend fun refreshUsedCarCustomers(api: HomeUsedCarApi = HomeUsedCarApi()): Result<Unit> =
        runCatching {
            usedCarCustomers = api.fetchCustomers()
            errorMessage = null
        }.onFailure {
            errorMessage = it.message ?: "加载客户失败"
        }

    /**
     * 二手车建单（`POST /used-car-orders`）；成功后重新拉列表并选中新单。
     * 失败返回 false 并记录 [errorMessage]。
     */
    suspend fun createUsedCar(
        kind: String,
        customerId: Int,
        vehicleModel: String,
        plateNo: String,
        vin: String,
        mileageKm: Int,
        modelYear: Int,
        amount: Int,
        api: HomeUsedCarApi = HomeUsedCarApi(),
    ): Boolean = runCatching {
        val created = api.create(
            HomeUsedCarOrderCreateRequest(
                kind = kind,
                customerId = customerId,
                vehicleModel = vehicleModel,
                plateNo = plateNo,
                vin = vin,
                mileageKm = mileageKm,
                modelYear = modelYear,
                amount = amount.toDouble(),
            ),
        )
        selectedUsedCarId = created.resolvedOrderId
        refreshUsedCars(api)
        true
    }.getOrElse {
        errorMessage = it.message ?: "提交失败"
        bump()
        false
    }

    /** 拉取售后工单列表（`can_create` 同步服务端 gate）。 */
    suspend fun refreshAfterSales(api: AfterSalesApi = AfterSalesApi()): Result<Unit> = runCatching {
        loading = true
        val result = api.fetchRecords(page = 1, size = 50)
        afterSalesCanCreate = result.canCreate
        afterSales = result.list.map { it.toRow() }
        errorMessage = null
    }.onFailure {
        errorMessage = it.message ?: "加载售后工单失败"
    }.also {
        loading = false
        bump()
    }

    /**
     * 售后建单（`POST /after-sales/records`，`service_kind` 写字符串 `"1"` = 保养）；
     * 成功后重新拉列表并选中新单。
     */
    suspend fun createAfterSales(
        name: String,
        phone: String,
        title: String,
        plate: String,
        mileageKm: Int?,
        date: String,
        content: String,
        serviceKind: String = "1",
        api: AfterSalesApi = AfterSalesApi(),
    ): Boolean = runCatching {
        val created = api.create(
            serviceKind = serviceKind,
            title = title,
            serviceDate = date,
            customerName = name,
            customerPhone = phone,
            plateNo = plate,
            mileage = mileageKm,
            content = content,
        )
        selectedAfterSalesId = created.recordId.toString()
        refreshAfterSales(api)
        true
    }.getOrElse {
        errorMessage = it.message ?: "创建售后工单失败"
        bump()
        false
    }

    /** 拉取新车跟进档案（全部 tab，逾期由客户端按 `next_follow_up_at` 推导）。 */
    @OptIn(ExperimentalTime::class)
    suspend fun refreshNewCarFollows(api: NewCarFollowApi = NewCarFollowApi()): Result<Unit> =
        runCatching {
            loading = true
            val today = Clock.System.now().toString().take(10)
            newCarRows = api.fetchList(NewCarFollowTab.All, page = 1, size = 50).list
                .map { it.toRow(today) }
            errorMessage = null
        }.onFailure {
            errorMessage = it.message ?: "加载新车跟进失败"
        }.also {
            loading = false
            bump()
        }

    /**
     * 新建跟进档案（`POST /new-car-follow-files`）；成功后重新拉列表并选中新档。
     * [intentBand] 接受 UI 的中文意向（高/中/低）或原始 `H/A/B/E`。
     */
    suspend fun createNewCarFollow(
        name: String,
        phone: String,
        vehicle: String,
        intentBand: String,
        api: NewCarFollowApi = NewCarFollowApi(),
    ): Boolean = runCatching {
        val created = api.create(
            followLevel = newCarFollowLevel(intentBand),
            displayName = name,
            phone = phone,
            vehicleInterest = vehicle,
        )
        selectedNewCarFollowId = created.fileId
        refreshNewCarFollows(api)
        true
    }.getOrElse {
        errorMessage = it.message ?: "建档失败"
        bump()
        false
    }

    /** Test helper — 只清空内存缓存与选择态。 */
    fun resetForTests() {
        usedCars = emptyList()
        afterSales = emptyList()
        newCarRows = emptyList()
        usedCarCustomers = emptyList()
        usedCarStats = HomeUsedCarStats()
        afterSalesCanCreate = false
        selectedUsedCarId = null
        selectedAfterSalesId = null
        selectedNewCarFollowId = null
        selectedAnalyticsId = 0L
        selectedLedgerId = null
        loading = false
        errorMessage = null
        bump()
    }

    /**
     * Flutter UsedCarCreateController validation:
     * customer required; vehicle/plate/vin/mileage/year/amount>0 required.
     */
    fun validateUsedCarCreate(
        customerName: String,
        vehicleModel: String,
        plateNo: String,
        vin: String,
        mileageKm: Int?,
        modelYear: Int?,
        amount: Int?,
    ): String? {
        if (customerName.isBlank()) return "请选择客户"
        if (vehicleModel.isBlank() || plateNo.isBlank() || vin.isBlank() ||
            mileageKm == null || modelYear == null || amount == null || amount <= 0
        ) {
            return "请完整填写车况与金额"
        }
        return null
    }

    fun validateAfterSalesCreate(
        name: String,
        phone: String,
        title: String,
        plate: String,
        content: String,
    ): String? {
        if (name.isBlank() || phone.isBlank()) return "请填写客户姓名和手机号"
        if (title.isBlank() || plate.isBlank() || content.isBlank()) return "请完整填写工单信息"
        return null
    }

    fun validateNewCarFollowCreate(name: String, phone: String): String? {
        if (name.isBlank() || phone.isBlank()) return "请填写客户姓名和手机号"
        return null
    }
}

private fun HomeUsedCarOrderItem.toRow(): UsedCarOrderRow = UsedCarOrderRow(
    id = resolvedOrderId,
    kindLabel = resolvedKindLabel.ifEmpty { usedCarKindLabel(resolvedKind) },
    statusLabel = statusLabel.ifEmpty { "—" },
    submittedDate = resolvedSubmittedAt.take(10).ifEmpty { "—" },
    vehicleModel = resolvedVehicleModel,
    plateNo = resolvedPlateNo,
    modelYear = resolvedModelYear,
    mileageKm = resolvedMileageKm,
    amountLabel = resolvedAmountLabel,
    amount = resolvedAmount.toInt(),
    customerName = resolvedCustomerName,
    vin = resolvedVin,
    phone = resolvedPhone,
)

private fun AfterSalesRecord.toRow(): AfterSalesDetailRow = AfterSalesDetailRow(
    id = recordId.toString(),
    title = title.ifEmpty { "工单 AS-$recordId" },
    kindLabel = serviceKindLabel.ifEmpty { if (serviceKind == 1) "保养" else "维修" },
    customerName = customerName,
    customerPhone = customerPhone,
    plateNo = plateNo,
    mileageKm = mileage,
    serviceDate = serviceDate.take(10).ifEmpty { "—" },
    content = content,
    appointmentId = appointmentId,
)

private fun NewCarFollowFile.toRow(todayIso: String): NewCarFollowRow = NewCarFollowRow(
    id = fileId,
    customerName = customerName.ifEmpty { "客户" },
    phone = customerPhone,
    vehicle = vehicleInterest.ifEmpty { "意向车型待定" },
    stage = newCarStageLabel(stage),
    intentBand = intentBand,
    nextFollow = nextFollowUpAt?.take(16)?.replace('T', ' ') ?: "待安排",
    owner = ownerDisplayName.ifEmpty { "销售顾问" },
    overdue = newCarFollowOverdue(nextFollowUpAt, stage, todayIso),
)

/** 车种 API 值 → 中文字面量（服务端缺 `kind_label` 时的兜底）。 */
internal fun usedCarKindLabel(kind: String): String = when (kind) {
    "trade_in" -> "置换"
    "consign" -> "专卖"
    "purchase" -> "收车"
    else -> "二手车"
}

/** 跟进级别：UI 中文意向（高/中/低）→ 服务端 `H/A/B/E`。 */
internal fun newCarFollowLevel(intentBand: String): String = when (intentBand) {
    "H", "A", "B", "E" -> intentBand
    "高" -> "A"
    "中" -> "B"
    "低" -> "E"
    else -> "B"
}

/** 阶段英文值 → 中文文案（对齐 Flutter `NewCarFollowLabels.stage`）。 */
internal fun newCarStageLabel(raw: String): String = when (raw) {
    "new" -> "新建"
    "following" -> "跟进中"
    "test_drive" -> "试驾"
    "quoted" -> "已报价"
    "won" -> "成交"
    "lost" -> "战败"
    else -> raw.ifEmpty { "—" }
}

/** 逾期判定：未成交/未战败且 `next_follow_up_at` 早于今天（纯逻辑，可测）。 */
internal fun newCarFollowOverdue(nextFollowUpAt: String?, stage: String, todayIso: String): Boolean {
    if (stage == "won" || stage == "lost") return false
    val due = nextFollowUpAt?.take(10) ?: return false
    return due < todayIso
}
