package com.example.my_kmp_project.feature.community

import kotlin.test.Test
import kotlin.test.assertEquals

class UnifiedCommentTextTest {
    @Test
    fun countsUnicodeCodePointsLikeGoRuneCount() {
        // 代理对（emoji）只算 1 个码点，不能按 UTF-16 单元 2 计。
        assertEquals(1, countCommentCodePoints("😀"))
        assertEquals(2, countCommentCodePoints("a😀"))
        assertEquals(4, countCommentCodePoints("ab😀😀"))
        assertEquals(0, countCommentCodePoints(""))
        assertEquals(
            UnifiedCommentController.MAX_CONTENT_LENGTH,
            countCommentCodePoints("😀".repeat(UnifiedCommentController.MAX_CONTENT_LENGTH)),
        )
    }
}
