package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.feature.mine.address.AddressApi
import com.example.my_kmp_project.feature.mine.mall.MineMallApi
import com.example.my_kmp_project.feature.mine.mall.MineMallOrderDetail
import com.example.my_kmp_project.feature.mine.mall.MineMallOrderRow
import com.example.my_kmp_project.feature.mine.mall.MineMallProduct
import com.example.my_kmp_project.feature.mine.mall.MineMallProductDetail
import kotlin.random.Random

/**
 * 商城四页的页面级状态（Compose `remember` 持有，非全局单例）。
 *
 * 数据全部来自 [MineMallApi]；失败只写 [errorMessage] 并保留上一次成功结果，
 * 不回落到任何本地假数据。
 */
internal class MineMallController(
    api: ApiClient,
    private val storeId: Int = 1,
) {
    private val mall = MineMallApi(api)
    private val addressApi = AddressApi(api)

    var products by mutableStateOf<List<MineMallProduct>>(emptyList())
        private set
    var orders by mutableStateOf<List<MineMallOrderRow>>(emptyList())
        private set
    var detail by mutableStateOf<MineMallProductDetail?>(null)
        private set
    var orderDetail by mutableStateOf<MineMallOrderDetail?>(null)
        private set

    var productsLoading by mutableStateOf(false)
        private set
    var ordersLoading by mutableStateOf(false)
        private set
    var detailLoading by mutableStateOf(false)
        private set
    var orderDetailLoading by mutableStateOf(false)
        private set

    var orderFilter by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun selectOrderFilter(filter: String) {
        orderFilter = filter
    }

    suspend fun loadProducts() {
        productsLoading = true
        errorMessage = null
        try {
            products = mall.fetchProducts(storeId = storeId)
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载商品失败"
        } finally {
            productsLoading = false
        }
    }

    suspend fun loadDetail(productId: Int) {
        detailLoading = true
        errorMessage = null
        try {
            detail = mall.fetchProductDetail(storeId = storeId, productId = productId)
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载商品详情失败"
        } finally {
            detailLoading = false
        }
    }

    suspend fun loadOrders() {
        ordersLoading = true
        errorMessage = null
        try {
            orders = mall.fetchOrders(status = orderFilter.ifBlank { null })
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载订单失败"
        } finally {
            ordersLoading = false
        }
    }

    suspend fun loadOrderDetail(orderId: Int) {
        orderDetailLoading = true
        errorMessage = null
        try {
            orderDetail = mall.fetchOrderDetail(orderId)
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载订单详情失败"
        } finally {
            orderDetailLoading = false
        }
    }

    /**
     * 立即购买：实体商品用默认收货地址，虚拟商品无需地址。
     * 返回订单号，失败返回可展示的中文原因。
     */
    suspend fun buyNow(skuId: Int, isVirtual: Boolean, qty: Int = 1): Result<Int> = try {
        var name = ""
        var phone = ""
        var address = ""
        if (!isVirtual) {
            val list = addressApi.list()
            val picked = list.firstOrNull { it.isDefault == true } ?: list.firstOrNull()
                ?: return Result.failure(IllegalStateException("请先在收货地址中添加地址"))
            name = picked.resolvedReceiverName
            phone = picked.resolvedReceiverPhone
            address = listOf(
                picked.resolvedProvince,
                picked.resolvedCity,
                picked.resolvedDistrict,
                picked.resolvedDetailAddress,
            ).filter { it.isNotBlank() }.joinToString(" ")
        }
        val key = "kmp-mall-${Random.nextInt(100_000, 999_999)}-$skuId"
        val orderId = mall.createOrder(
            storeId = storeId,
            skuId = skuId,
            qty = qty,
            idempotencyKey = key,
            receiverName = name,
            receiverPhone = phone,
            receiverAddress = address,
        )
        Result.success(orderId)
    } catch (e: Exception) {
        Result.failure(IllegalStateException(e.message ?: "下单失败"))
    }

    suspend fun pay(orderId: Int, paymentChannel: Int = 1): Result<Unit> = try {
        mall.payOrder(orderId = orderId, paymentChannel = paymentChannel)
        orderDetail = mall.fetchOrderDetail(orderId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(IllegalStateException(e.message ?: "支付失败"))
    }

    suspend fun cancel(orderId: Int): Result<Unit> = try {
        mall.cancelOrder(orderId)
        orderDetail = mall.fetchOrderDetail(orderId)
        orders = mall.fetchOrders(status = orderFilter.ifBlank { null })
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(IllegalStateException(e.message ?: "取消订单失败"))
    }

    fun clearError() {
        errorMessage = null
    }
}
