package com.example.my_kmp_project.feature.mine

import com.example.my_kmp_project.feature.home.HomeRoutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Flutter MineController SoT — id-based tap dispatch. */
class MineTapDispatchTest {
    @Test
    fun undevelopedFunctionsToast() {
        val sms = MineTapDispatch.onFunction("sms", loggedIn = true)
        assertTrue(sms is MineTapResult.Toast)
        assertTrue((sms as MineTapResult.Toast).message.contains("开发中"))

        val qr = MineTapDispatch.onFunction("qr_pay", loggedIn = true) as MineTapResult.Toast
        assertTrue(qr.message.contains("开发中"))

        val poster = MineTapDispatch.onFunction("poster", loggedIn = false) as MineTapResult.Toast
        assertTrue(poster.message.contains("开发中"))
    }

    @Test
    fun calculatorAndQaNavigateWithoutLogin() {
        assertEquals(
            MineTapResult.Navigate(MineRoutes.Calculator),
            MineTapDispatch.onFunction("calculator", loggedIn = false),
        )
        assertEquals(
            MineTapResult.Navigate(MineRoutes.HttpTest),
            MineTapDispatch.onFunction("qa", loggedIn = false),
        )
    }

    @Test
    fun mallRequiresLogin() {
        assertEquals(
            MineTapResult.RequireLogin(MineRoutes.Mall),
            MineTapDispatch.onQuickService("mall", loggedIn = false),
        )
        assertEquals(
            MineTapResult.Navigate(MineRoutes.Mall),
            MineTapDispatch.onQuickService("mall", loggedIn = true),
        )
    }

    @Test
    fun menuToastsAndAddressAuth() {
        assertEquals(
            MineTapResult.Toast("商务合作"),
            MineTapDispatch.onMenu("cooperation", loggedIn = true),
        )
        assertEquals(
            MineTapResult.Toast("意见反馈"),
            MineTapDispatch.onMenu("feedback", loggedIn = false),
        )
        assertEquals(
            MineTapResult.RequireLogin(MineRoutes.Addresses),
            MineTapDispatch.onMenu("address", loggedIn = false),
        )
        assertEquals(
            MineTapResult.Navigate(MineRoutes.Settings),
            MineTapDispatch.onMenu("settings", loggedIn = false),
        )
    }

    @Test
    fun usedCarNavigatesWhenLoggedIn() {
        assertEquals(
            MineTapResult.Navigate(HomeRoutes.UsedCar),
            MineTapDispatch.onFunction("used_car", loggedIn = true),
        )
        assertEquals(
            MineTapResult.RequireLogin(HomeRoutes.UsedCar),
            MineTapDispatch.onFunction("used_car", loggedIn = false),
        )
    }

    @Test
    fun fromLabelNoLongerOpensUndevelopedFunctions() {
        assertEquals(null, MineRoutes.fromLabel("短信模板"))
        assertEquals(null, MineRoutes.fromLabel("店铺收款码"))
        assertEquals(null, MineRoutes.fromLabel("商家海报"))
        assertEquals(MineRoutes.Calculator, MineRoutes.fromLabel("购车计算器"))
        assertEquals(MineRoutes.HttpTest, MineRoutes.fromLabel("选买问答"))
    }
}
