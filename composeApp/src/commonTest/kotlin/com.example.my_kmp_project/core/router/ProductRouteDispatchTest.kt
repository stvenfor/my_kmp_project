package com.example.my_kmp_project.core.router

import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.mine.MineRoutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProductRouteDispatchTest {
    @Test
    fun mineQuickServicesMapToMallWalletOrders() {
        assertEquals(MineRoutes.Mall, ProductRouteDispatch.resolve("商城")!!.path)
        assertEquals(MineRoutes.Wallet, ProductRouteDispatch.resolve("我的钱包")!!.path)
        assertEquals(MineRoutes.MallOrders, ProductRouteDispatch.resolve("我的订单")!!.path)
        assertEquals(ProductRouteHost.Mine, ProductRouteDispatch.resolve("商城")!!.host)
    }

    @Test
    fun mineFunctionsCrossModuleToHomeAndContent() {
        assertEquals(HomeRoutes.UsedCar, ProductRouteDispatch.resolve("二手车")!!.path)
        assertEquals(ProductRouteHost.Home, ProductRouteDispatch.resolve("二手车")!!.host)
        assertEquals(HomeRoutes.Ledger, ProductRouteDispatch.resolve("收支")!!.path)
        assertEquals(MineRoutes.HttpTest, ProductRouteDispatch.resolve("选买问答")!!.path)
        assertEquals("/video/short", ProductRouteDispatch.resolve("小视频")!!.path)
        assertEquals(ProductRouteHost.Content, ProductRouteDispatch.resolve("小视频")!!.host)
    }

    @Test
    fun toastOnlyNeverNavigates() {
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("意见反馈")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("粉丝群")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("短信模板")!!.host)
        assertEquals("短信模板 开发中", ProductRouteDispatch.resolve("短信模板")!!.toastMessage)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("/mine/sms_templates")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("店铺收款码")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("/mine/shop_qr")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("商家海报")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("/mine/poster")!!.host)
    }

    @Test
    fun homeFeaturesMapCanonicalPaths() {
        assertEquals(HomeRoutes.LifeService, ProductRouteDispatch.resolve("生活服务")!!.path)
        assertEquals(HomeRoutes.AllServices, ProductRouteDispatch.resolve("更多")!!.path)
        assertEquals(ProductRouteHost.AllServices, ProductRouteDispatch.resolve("全部服务")!!.host)
        assertEquals("/ai/stream", ProductRouteDispatch.resolve("AI小石头")!!.path)
        assertEquals(ProductRouteHost.Content, ProductRouteDispatch.resolve("AI小石头")!!.host)
        assertEquals(MineRoutes.DealInvoiceDemo, ProductRouteDispatch.resolve("新车成交")!!.path)
        assertEquals(ProductRouteHost.Mine, ProductRouteDispatch.resolve("新车成交")!!.host)
    }

    @Test
    fun pathAliasesCanonicalize() {
        val qa = assertNotNull(ProductRouteDispatch.resolve("/mine/qa"))
        assertEquals(MineRoutes.HttpTest, qa.path)
        assertEquals(ProductRouteHost.Mine, qa.host)

        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("/mine/sms_template")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("/mine/store_qr")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("/mine/cooperation")!!.host)
        assertEquals(ProductRouteHost.Toast, ProductRouteDispatch.resolve("/mine/business")!!.host)
    }
}
