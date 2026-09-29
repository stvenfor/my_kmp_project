package com.example.my_kmp_project.feature.classroom

import kotlin.concurrent.Volatile

/**
 * Flutter `ClassroomMockData` subset for logic-first parity
 * (`features/classroom/lib/data/classroom_mock_data.dart`).
 */
internal data class ClassInfo(
    val id: String,
    val name: String,
    val inviteCode: String,
    val memberCount: Int,
)

internal data class GiftCardInfo(
    val studentName: String,
    val teacherName: String,
    val message: String,
    val date: String,
    val cardType: String,
    val duration: String,
)

internal data class HomeworkStatRow(
    val id: String,
    val title: String,
    val type: String,
    val due: String,
)

internal object ClassroomLogicModels {
    const val DefaultClassId = "class_001"
    const val DefaultHomeworkId = "hw_001"

    val classes = listOf(
        ClassInfo("class_001", "班级名称", "11490rKkz", 6),
        ClassInfo("class_002", "三年级2班", "88201aBxP", 32),
    )

    val giftCard = GiftCardInfo(
        studentName = "乌克丽丽",
        teacherName = "老坛酸菜",
        message = "本次作业完成的很棒！老师送你一张体验卡，以资鼓励",
        date = "2026-05-20",
        cardType = "班级会员卡",
        duration = "1天 AI SVIP",
    )

    val homeworkRows = listOf(
        HomeworkStatRow("hw_001", "配音作业 · 侏罗纪世界2", "配音", "今日 23:59"),
        HomeworkStatRow("hw_002", "同步作业 · Unit 3", "同步", "明日 18:00"),
        HomeworkStatRow("hw_003", "打卡作业 · 听力", "打卡", "本周六"),
    )

    fun findClass(id: String?): ClassInfo =
        classes.firstOrNull { it.id == id } ?: classes.first()
}

/** One-shot gift claim — Flutter claim page toggles claimed; no re-claim. */
internal object ClassroomGiftClaimStore {
    @Volatile
    private var claimed: Boolean = false

    fun isClaimed(): Boolean = claimed

    /** @return true if newly claimed, false if already claimed. */
    fun claim(): Boolean {
        if (claimed) return false
        claimed = true
        return true
    }

    /** Test reset only. */
    fun resetForTest() {
        claimed = false
    }
}
