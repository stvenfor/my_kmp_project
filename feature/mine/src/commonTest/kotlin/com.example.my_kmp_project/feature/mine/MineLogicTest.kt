package com.example.my_kmp_project.feature.mine

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MineContactValidatorsTest {
    @Test
    fun nameAndMobileRules() {
        assertTrue(MineContactValidators.isNameValid("张三"))
        assertFalse(MineContactValidators.isNameValid(""))
        assertFalse(MineContactValidators.isNameValid("一二三四五六七八九十十一三"))
        assertTrue(MineContactValidators.isCnMobile("13800115172"))
        assertTrue(MineContactValidators.isCnMobile("138-0011-5172"))
        assertFalse(MineContactValidators.isCnMobile("23800115172"))
        assertEquals("138****5172", MineContactValidators.maskPhone("13800115172"))
    }
}

class AddressMockStoreTest {
    @BeforeTest
    fun reset() {
        AddressMockStore.resetForTests()
    }

    /** 数据已改为远端接口，测试只覆盖不依赖网络的表单校验。 */
    @Test
    fun validateRules() {
        assertEquals(
            "请填写收货人",
            AddressMockStore.validate("", "13800115172", "省", "市", "区", "详"),
        )
        assertEquals(
            "请输入以 1 开头的 11 位手机号",
            AddressMockStore.validate("甲", "123", "省", "市", "区", "详"),
        )
        assertEquals(
            "请填写详细地址",
            AddressMockStore.validate("甲", "13800115172", "省", "市", "区", ""),
        )
        assertEquals(
            "请选择省市区",
            AddressMockStore.validate("甲", "13800115172", "", "市", "区", "详"),
        )
        assertNull(
            AddressMockStore.validate("甲", "13800115172", "省", "市", "区", "详"),
        )
        assertTrue(AddressMockStore.addresses().isEmpty())
    }
}

class WalletMockStoreTest {
    @BeforeTest
    fun reset() {
        WalletMockStore.resetForTests()
    }

    /** 数据已改为远端接口，测试只覆盖不依赖网络的表单校验。 */
    @Test
    fun validateRules() {
        assertEquals("请输入金额", WalletMockStore.validateRecharge(""))
        assertEquals("请输入有效金额", WalletMockStore.validateRecharge("abc"))
        assertNull(WalletMockStore.validateRecharge("10.5"))
        assertEquals("请填写银行名与卡号后四位", WalletMockStore.validateBindCard("", "12"))
        assertEquals("卡号后四位须为数字", WalletMockStore.validateBindCard("招商银行", "12a4"))
        assertNull(WalletMockStore.validateBindCard("招商银行", "1234"))
        assertTrue(WalletMockStore.cards().isEmpty())
    }
}

class MineProfileLogicTest {
    @Test
    fun dirtyAndValidate() {
        assertFalse(MineProfileLogic.isDirty("a", "a", false))
        assertTrue(MineProfileLogic.isDirty("b", "a", false))
        assertTrue(MineProfileLogic.isDirty("a", "a", true))
        assertEquals("昵称不能为空", MineProfileLogic.validateSave("  ", true))
        assertEquals("没有需要保存的修改", MineProfileLogic.validateSave("x", false))
        assertNull(MineProfileLogic.validateSave("x", true))
    }
}
