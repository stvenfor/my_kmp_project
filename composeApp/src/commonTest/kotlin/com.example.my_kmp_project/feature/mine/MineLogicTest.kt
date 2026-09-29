package com.example.my_kmp_project.feature.mine

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MineContactValidatorsTest {
    @Test
    fun nameAndMobileRules() {
        assertTrue(MineContactValidators.isNameValid("张三"))
        assertFalse(MineContactValidators.isNameValid(""))
        assertFalse(MineContactValidators.isNameValid("一二三四五六七八九十十一"))
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

    @Test
    fun validateAndCrud() {
        assertEquals(
            "请填写收货人",
            AddressMockStore.validate("", "13800115172", "省", "市", "区", "详"),
        )
        assertEquals(
            "请输入以 1 开头的 11 位手机号",
            AddressMockStore.validate("甲", "123", "省", "市", "区", "详"),
        )
        assertNull(
            AddressMockStore.validate("甲", "13800115172", "省", "市", "区", "详"),
        )
        val before = AddressMockStore.addresses().size
        val created = AddressMockStore.save(
            id = null,
            name = "新客",
            phone = "13912345678",
            province = "广东省",
            city = "深圳市",
            district = "南山区",
            detail = "科技园",
            label = "公司",
            isDefault = true,
        )
        assertEquals(before + 1, AddressMockStore.addresses().size)
        assertTrue(created.isDefault)
        assertEquals(1, AddressMockStore.addresses().count { it.isDefault })
        assertTrue(AddressMockStore.setDefault("addr-1"))
        assertTrue(AddressMockStore.addresses().first { it.id == "addr-1" }.isDefault)
        assertTrue(AddressMockStore.delete(created.id))
    }
}

class WalletMockStoreTest {
    @BeforeTest
    fun reset() {
        WalletMockStore.resetForTests()
    }

    @Test
    fun rechargeAndBind() {
        assertEquals("请输入金额", WalletMockStore.validateRecharge(""))
        assertNull(WalletMockStore.validateRecharge("10.5"))
        val before = WalletMockStore.balanceFen()
        val entry = WalletMockStore.recharge("10", channel = 1, cardId = null)
        assertNotNull(entry)
        assertEquals(before + 1000, WalletMockStore.balanceFen())
        assertEquals("请填写银行名与卡号后四位", WalletMockStore.validateBindCard("", "12"))
        val card = WalletMockStore.bindCard("招商银行", "1234")
        assertNotNull(card)
        assertTrue(WalletMockStore.setDefaultCard(card.cardId))
        assertTrue(WalletMockStore.deleteCard(card.cardId))
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
