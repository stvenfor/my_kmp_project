package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.DemoElevation
import com.example.my_kmp_project.core.design.DemoRadius
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.PlatformNetworkImage
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import com.example.my_kmp_project.feature.mine.mall.MineMallOrderDetail
import com.example.my_kmp_project.feature.mine.mall.MineMallOrderRow
import com.example.my_kmp_project.feature.mine.mall.MineMallProduct
import com.example.my_kmp_project.feature.mine.mall.mineMallTimeLabel
import kotlinx.coroutines.launch
import my_kmp_project.core.design.generated.resources.Res
import my_kmp_project.core.design.generated.resources.community_avatar

/**
 * 商城货架 —— 数据来自 `GET /api/v1/mall/stores/{storeId}/products`。
 * 原 PNG 假页（`mall_list_body`）已移除：失败只展示错误与重试，不回落本地数据。
 */
@Composable
internal fun MallListScreen(
    onBack: () -> Unit,
    onOpenDetail: (productId: Int) -> Unit,
    onOpenOrders: () -> Unit,
    controller: MineMallController,
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { controller.loadProducts() }

    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "商城", onBack = onBack, containerColor = MineTheme.Surface)
        Row(
            Modifier
                .fillMaxWidth()
                .background(MineTheme.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onOpenOrders) {
                Text("我的订单", color = DemoColors.Accent, fontSize = 14.sp)
            }
        }
        HorizontalDivider(color = MineTheme.Separator)

        when {
            controller.productsLoading && controller.products.isEmpty() -> CenterHint("加载商品中…")
            controller.errorMessage != null && controller.products.isEmpty() -> ErrorBlock(
                message = controller.errorMessage ?: "加载商品失败",
                onRetry = { scope.launch { controller.loadProducts() } },
            )
            controller.products.isEmpty() -> CenterHint("暂无在售商品")
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                controller.products.forEach { product ->
                    ProductRow(product = product, onClick = { onOpenDetail(product.productId) })
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: MineMallProduct, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .mineCard()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlatformNetworkImage(
            url = product.coverUrl,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(DemoRadius.Md)),
            placeholder = Res.drawable.community_avatar,
            contentDescription = product.title,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = DemoColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                product.subtitle?.takeIf { it.isNotBlank() } ?: if (product.isVirtual) "虚拟发放" else "门店自提/配送",
                fontSize = 12.sp,
                color = DemoColors.Muted,
            )
        }
        Text(product.priceLabel, color = MineTheme.Danger, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** 商品详情 —— `GET /api/v1/mall/stores/{storeId}/products/{productId}`。 */
@Composable
internal fun MallDetailScreen(
    onBack: () -> Unit,
    productId: Int?,
    controller: MineMallController,
    onCreated: (orderId: Int) -> Unit,
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    LaunchedEffect(productId) {
        if (productId != null) controller.loadDetail(productId)
    }

    Column(Modifier.fillMaxSize().background(MineTheme.Surface)) {
        MineTopBar(title = "商品详情", onBack = onBack, containerColor = MineTheme.Surface)
        if (productId == null) {
            CenterHint("未选择商品")
        } else {
            val detail = controller.detail
            val product = detail?.product
            when {
                controller.detailLoading && product == null -> CenterHint("加载详情中…")
                product == null -> ErrorBlock(
                    message = controller.errorMessage ?: "加载商品详情失败",
                    onRetry = { scope.launch { controller.loadDetail(productId) } },
                )
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    PlatformNetworkImage(
                        url = product.coverUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .shadow(
                                elevation = DemoElevation.Card,
                                shape = RoundedCornerShape(MineTheme.RadiusLg),
                                ambientColor = DemoElevation.CardShadow,
                                spotColor = DemoElevation.CardShadow,
                            )
                            .clip(RoundedCornerShape(MineTheme.RadiusLg)),
                        placeholder = Res.drawable.community_avatar,
                    contentDescription = product.title,
                )
                Spacer(Modifier.height(14.dp))
                Text(product.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = DemoColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(product.priceLabel, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MineTheme.Danger)
                Spacer(Modifier.height(8.dp))
                Text(
                    product.subtitle?.takeIf { it.isNotBlank() }
                        ?: if (product.isVirtual) "虚拟商品 · 下单后发放" else "实体商品 · 需填写收货地址",
                    fontSize = 13.sp,
                    color = DemoColors.TextSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Text("规格", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                detail.skus.forEach { sku ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Text(sku.label, Modifier.weight(1f), fontSize = 14.sp, color = DemoColors.TextPrimary)
                        Text("库存 ${sku.stockQty}", fontSize = 13.sp, color = DemoColors.Muted)
                        Spacer(Modifier.width(10.dp))
                        Text(sku.priceLabel, fontSize = 14.sp, color = MineTheme.Danger)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val sku = detail.skus.firstOrNull() ?: return@Button
                        scope.launch {
                            controller.buyNow(sku.skuId, product.isVirtual).fold(
                                onSuccess = { orderId ->
                                    showPlatformToast("下单成功")
                                    onCreated(orderId)
                                },
                                onFailure = { showPlatformToast(it.message ?: "下单失败") },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Accent),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("立即购买") }
                }
            }
        }
    }
}

/** 订单列表 —— `GET /api/v1/mall/orders?status=`。 */
@Composable
internal fun OrderListScreen(
    onBack: () -> Unit,
    onOpen: (orderId: Int) -> Unit,
    controller: MineMallController,
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    LaunchedEffect(controller.orderFilter) { controller.loadOrders() }

    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "我的订单", onBack = onBack, containerColor = MineTheme.Surface)
        Row(
            Modifier
                .fillMaxWidth()
                .background(MineTheme.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderFilters.forEach { (label, filter) ->
                val selected = controller.orderFilter == filter
                Text(
                    label,
                    fontSize = 13.sp,
                    color = if (selected) DemoColors.OnPrimary else DemoColors.TextPrimary,
                    modifier = Modifier
                        .clip(DemoRadius.Pill)
                        .background(if (selected) DemoColors.Accent else MineTheme.FillSecondary)
                        .clickable {
                            controller.selectOrderFilter(filter)
                            controller.clearError()
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
        HorizontalDivider(color = MineTheme.Separator)

        when {
            controller.ordersLoading && controller.orders.isEmpty() -> CenterHint("加载订单中…")
            controller.errorMessage != null && controller.orders.isEmpty() -> ErrorBlock(
                message = controller.errorMessage ?: "加载订单失败",
                onRetry = { scope.launch { controller.loadOrders() } },
            )
            controller.orders.isEmpty() -> CenterHint("暂无订单")
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                controller.orders.forEach { row ->
                    OrderRow(row = row, onClick = { onOpen(row.orderId) })
                }
            }
        }
    }
}

private val OrderFilters: List<Pair<String, String>> = listOf(
    "全部" to "",
    "待支付" to "0",
    "已支付" to "1,2",
    "已关闭" to "3,4",
)

@Composable
private fun OrderRow(row: MineMallOrderRow, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .mineCard()
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.orderNo, Modifier.weight(1f), fontSize = 13.sp, color = DemoColors.TextSecondary)
            Text(row.statusLabel, fontSize = 13.sp, color = statusColor(row.status), fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformNetworkImage(
                url = row.primaryCover,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(DemoRadius.Md)),
                placeholder = Res.drawable.community_avatar,
                contentDescription = row.primaryTitle,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(row.primaryTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("共 ${row.totalQty} 件", fontSize = 12.sp, color = DemoColors.Muted)
            }
            Text("¥${row.amount}", color = MineTheme.Danger, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Text(mineMallTimeLabel(row.createdAt), fontSize = 12.sp, color = DemoColors.Muted)
    }
}

/** 订单详情 —— `GET /api/v1/mall/orders/{orderId}`，支付/取消走真实接口。 */
@Composable
internal fun MallOrderDetailScreen(
    onBack: () -> Unit,
    orderId: Int?,
    controller: MineMallController,
) {
    ReportMainTabRoot(isRoot = false)
    val scope = rememberCoroutineScope()
    LaunchedEffect(orderId) {
        if (orderId != null) controller.loadOrderDetail(orderId)
    }

    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(title = "订单详情", onBack = onBack, containerColor = MineTheme.Surface)
        if (orderId == null) {
            CenterHint("未选择订单")
        } else {
            val detail = controller.orderDetail
            when {
                controller.orderDetailLoading && detail == null -> CenterHint("加载订单中…")
                detail == null -> ErrorBlock(
                    message = controller.errorMessage ?: "加载订单详情失败",
                    onRetry = { scope.launch { controller.loadOrderDetail(orderId) } },
                )
                else -> OrderDetailBody(detail = detail, controller = controller)
            }
        }
    }
}

@Composable
private fun OrderDetailBody(detail: MineMallOrderDetail, controller: MineMallController) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionCard {
            Text(detail.statusLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = statusColor(detail.status))
            Spacer(Modifier.height(8.dp))
            Text("订单号 ${detail.orderNo}", color = DemoColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("下单时间 ${mineMallTimeLabel(detail.createdAt)}", color = DemoColors.TextSecondary, fontSize = 13.sp)
        }
        detail.order?.let { order ->
            SectionCard {
                Text("商品", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                detail.items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlatformNetworkImage(
                            url = item.coverUrl,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                            placeholder = Res.drawable.community_avatar,
                            contentDescription = item.productTitle,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.productTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("x${item.qty}", fontSize = 12.sp, color = DemoColors.Muted)
                        }
                        Text("¥${item.lineAmount}", color = MineTheme.Danger, fontSize = 14.sp)
                    }
                }
            }
            if (order.receiverName.isNotBlank() || order.receiverAddress.isNotBlank()) {
                SectionCard {
                    Text("收货信息", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${order.receiverName}  ${order.receiverPhone}",
                        fontSize = 13.sp,
                        color = DemoColors.TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(order.receiverAddress, fontSize = 13.sp, color = DemoColors.TextSecondary)
                }
            }
        }
        SectionCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("实付金额", fontWeight = FontWeight.Medium)
                Text("¥${detail.amount}", color = MineTheme.Danger, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        if (detail.isUnpaid) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            controller.cancel(detail.orderId).fold(
                                onSuccess = { showPlatformToast("已取消订单") },
                                onFailure = { showPlatformToast(it.message ?: "取消失败") },
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, DemoColors.Divider, DemoRadius.Pill),
                ) { Text("取消订单", color = DemoColors.TextPrimary) }
                Button(
                    onClick = {
                        scope.launch {
                            controller.pay(detail.orderId).fold(
                                onSuccess = { showPlatformToast("支付成功") },
                                onFailure = { showPlatformToast(it.message ?: "支付失败") },
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Accent),
                    shape = DemoRadius.Pill,
                ) { Text("去支付") }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MineTheme.RadiusMd))
            .background(MineTheme.Surface)
            .border(MineTheme.Hairline, MineTheme.Separator, RoundedCornerShape(MineTheme.RadiusMd))
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun CenterHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = DemoColors.Muted, fontSize = 14.sp)
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = DemoColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onRetry) { Text("重试", color = DemoColors.Accent) }
    }
}

private fun statusColor(status: Int): Color = when (status) {
    0 -> Color(0xFFFF9500)
    1, 2 -> DemoColors.Accent
    else -> DemoColors.Muted
}
