package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Mutable home secondary pipelines (used-car / after-sales / new-car follow).
 * Selection ids drive detail screens (Flutter route arg / controller selection).
 */
internal object HomeSecondaryStore {
    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var selectedUsedCarId by mutableStateOf<String?>(null)
    var selectedAfterSalesId by mutableStateOf<String?>(null)
    var selectedNewCarFollowId by mutableStateOf<String?>(null)

    /** Advisor demo: Flutter `can_create` true for store staff. */
    var afterSalesCanCreate by mutableStateOf(true)

    private var usedCarSeq = 3
    private var afterSalesSeq = 441
    private var newCarSeq = 3

    private val usedCars = mutableListOf(
        UsedCarOrderRow(
            id = "uc-1",
            kindLabel = "置换",
            statusLabel = "待审核",
            submittedDate = "2026-09-22",
            vehicleModel = "2021 帝豪",
            plateNo = "京A·88X21",
            modelYear = 2021,
            mileageKm = 32000,
            amountLabel = "评估价",
            amount = 86000,
            customerName = "张先生",
            vin = "LGXC14DF5M0123456",
        ),
        UsedCarOrderRow(
            id = "uc-2",
            kindLabel = "专卖",
            statusLabel = "已通过",
            submittedDate = "2026-09-18",
            vehicleModel = "2020 星越L",
            plateNo = "沪B·6K902",
            modelYear = 2020,
            mileageKm = 41000,
            amountLabel = "成交价",
            amount = 152000,
            customerName = "李女士",
            vin = "LGXC16DF8L0654321",
        ),
        UsedCarOrderRow(
            id = "uc-3",
            kindLabel = "收车",
            statusLabel = "已提交",
            submittedDate = "2026-09-15",
            vehicleModel = "2019 博越",
            plateNo = "粤C·19H33",
            modelYear = 2019,
            mileageKm = 55000,
            amountLabel = "收车价",
            amount = 79000,
            customerName = "王先生",
            vin = "LGXC14CF3K0987654",
        ),
    )

    private val afterSales = mutableListOf(
        AfterSalesDetailRow(
            id = "as-441",
            title = "工单 AS-441",
            kindLabel = "保养",
            customerName = "陈先生",
            customerPhone = "139****2201",
            plateNo = "京A·88K21",
            mileageKm = 28600,
            serviceDate = "2026-09-25",
            content = "更换机油机滤，检查刹车片；客户要求加急。",
            appointmentId = 8821,
        ),
        AfterSalesDetailRow(
            id = "as-438",
            title = "工单 AS-438",
            kindLabel = "维修",
            customerName = "周女士",
            customerPhone = "137****6610",
            plateNo = "京N·5U902",
            mileageKm = 42100,
            serviceDate = "2026-09-24",
            content = "前杠钣喷索赔，待配件到店。",
            appointmentId = null,
        ),
    )

    private val _newCarFollows = mutableListOf(
        NewCarFollowRow(
            id = "nc-1",
            customerName = "孙某",
            phone = "138****2101",
            vehicle = "银河 L7",
            stage = "跟进中",
            intentBand = "高",
            nextFollow = "今日 15:00",
            owner = "销售顾问",
        ),
        NewCarFollowRow(
            id = "nc-2",
            customerName = "吴某",
            phone = "139****8820",
            vehicle = "星愿",
            stage = "报价",
            intentBand = "中",
            nextFollow = "明日 10:30",
            owner = "销售顾问",
        ),
        NewCarFollowRow(
            id = "nc-3",
            customerName = "赵某",
            phone = "186****4412",
            vehicle = "星越 L",
            stage = "试驾",
            intentBand = "低",
            nextFollow = "09-20 已逾期",
            owner = "网销",
            overdue = true,
        ),
    )

    fun usedCarOrders(): List<UsedCarOrderRow> = usedCars.toList()

    fun findUsedCar(id: String?): UsedCarOrderRow? =
        id?.let { wanted -> usedCars.firstOrNull { it.id == wanted } }
            ?: usedCars.firstOrNull()

    fun selectUsedCar(id: String) {
        selectedUsedCarId = id
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

    fun createUsedCar(
        kindLabel: String,
        customerName: String,
        vehicleModel: String,
        plateNo: String,
        vin: String,
        mileageKm: Int,
        modelYear: Int,
        amount: Int,
    ): UsedCarOrderRow {
        usedCarSeq += 1
        val row = UsedCarOrderRow(
            id = "uc-$usedCarSeq",
            kindLabel = kindLabel,
            statusLabel = "已提交",
            submittedDate = "2026-09-29",
            vehicleModel = vehicleModel,
            plateNo = plateNo,
            modelYear = modelYear,
            mileageKm = mileageKm,
            amountLabel = when (kindLabel) {
                "置换" -> "评估价"
                "专卖" -> "成交价"
                else -> "收车价"
            },
            amount = amount,
            customerName = customerName.substringBefore(' ').ifBlank { customerName },
            vin = vin,
        )
        usedCars.add(0, row)
        selectedUsedCarId = row.id
        bump()
        return row
    }

    fun afterSalesDetails(): List<AfterSalesDetailRow> = afterSales.toList()

    fun findAfterSales(id: String?): AfterSalesDetailRow? =
        id?.let { wanted -> afterSales.firstOrNull { it.id == wanted } }
            ?: afterSales.firstOrNull()

    fun selectAfterSales(id: String) {
        selectedAfterSalesId = id
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

    fun createAfterSales(
        name: String,
        phone: String,
        title: String,
        plate: String,
        mileageKm: Int?,
        date: String,
        content: String,
        kindLabel: String = "保养",
    ): AfterSalesDetailRow {
        afterSalesSeq += 1
        val row = AfterSalesDetailRow(
            id = "as-$afterSalesSeq",
            title = title.ifBlank { "工单 AS-$afterSalesSeq" },
            kindLabel = kindLabel,
            customerName = name,
            customerPhone = phone,
            plateNo = plate,
            mileageKm = mileageKm,
            serviceDate = date,
            content = content,
            appointmentId = null,
        )
        afterSales.add(0, row)
        selectedAfterSalesId = row.id
        bump()
        return row
    }

    fun newCarFollows(): List<NewCarFollowRow> = _newCarFollows.toList()

    fun findNewCarFollow(id: String?): NewCarFollowRow? =
        id?.let { wanted -> _newCarFollows.firstOrNull { it.id == wanted } }
            ?: _newCarFollows.firstOrNull()

    fun selectNewCarFollow(id: String) {
        selectedNewCarFollowId = id
    }

    fun validateNewCarFollowCreate(name: String, phone: String): String? {
        if (name.isBlank() || phone.isBlank()) return "请填写客户姓名和手机号"
        return null
    }

    fun createNewCarFollow(
        name: String,
        phone: String,
        vehicle: String,
        intentBand: String,
    ): NewCarFollowRow {
        newCarSeq += 1
        val row = NewCarFollowRow(
            id = "nc-$newCarSeq",
            customerName = name,
            phone = phone,
            vehicle = vehicle.ifBlank { "意向车型待定" },
            stage = "跟进中",
            intentBand = intentBand,
            nextFollow = "今日待排",
            owner = "销售顾问",
        )
        _newCarFollows.add(0, row)
        selectedNewCarFollowId = row.id
        bump()
        return row
    }
}
