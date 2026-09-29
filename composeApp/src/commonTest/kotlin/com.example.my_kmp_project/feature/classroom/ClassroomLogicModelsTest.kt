package com.example.my_kmp_project.feature.classroom

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassroomLogicModelsTest {

    @BeforeTest
    fun resetGift() {
        ClassroomGiftClaimStore.resetForTest()
    }

    @Test
    fun classes_match_flutter_seed() {
        assertEquals(2, ClassroomLogicModels.classes.size)
        assertEquals("11490rKkz", ClassroomLogicModels.findClass(null).inviteCode)
        assertEquals("三年级2班", ClassroomLogicModels.findClass("class_002").name)
    }

    @Test
    fun gift_card_fields_match_flutter() {
        val g = ClassroomLogicModels.giftCard
        assertEquals("乌克丽丽", g.studentName)
        assertEquals("老坛酸菜", g.teacherName)
        assertEquals("1天 AI SVIP", g.duration)
    }

    @Test
    fun gift_claim_is_one_shot() {
        assertFalse(ClassroomGiftClaimStore.isClaimed())
        assertTrue(ClassroomGiftClaimStore.claim())
        assertTrue(ClassroomGiftClaimStore.isClaimed())
        assertFalse(ClassroomGiftClaimStore.claim())
    }

    @Test
    fun homework_rows_include_dubbing() {
        assertTrue(ClassroomLogicModels.homeworkRows.any { it.type == "配音" })
        assertEquals("hw_001", ClassroomLogicModels.homeworkRows.first().id)
    }
}
