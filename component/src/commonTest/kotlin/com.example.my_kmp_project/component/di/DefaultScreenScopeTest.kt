package com.example.my_kmp_project.component.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * DI 第一批：Screen 作用域关闭后必须拒绝读写 —— 页面退出后继续用旧作用域
 * 属于泄漏，应该在测试里立刻失败，而不是静默返回过期对象。
 */
class DefaultScreenScopeTest {

    @Test
    fun storesAndReturnsValuesByKey() {
        val scope = DefaultScreenScope()
        scope.put("controller", "page-controller")

        assertEquals("page-controller", scope.get<String>("controller"))
        assertEquals("page-controller", scope.require<String>("controller"))
        assertNull(scope.get<String>("missing"))
        assertFalse(scope.isClosed)
    }

    @Test
    fun closeMarksScopeClosedAndDropsValues() {
        val scope = DefaultScreenScope()
        scope.put("controller", "page-controller")

        scope.close()

        assertTrue(scope.isClosed)
        assertFailsWith<IllegalStateException> { scope.get<String>("controller") }
    }

    @Test
    fun writingAfterCloseIsRejected() {
        val scope = DefaultScreenScope()
        scope.close()

        assertFailsWith<IllegalStateException> { scope.put("late", "value") }
    }

    @Test
    fun missingKeyFailsWithAReadableMessage() {
        val scope = DefaultScreenScope()

        val error = assertFailsWith<IllegalStateException> { scope.require<String>("missing") }
        assertTrue(error.message.orEmpty().contains("missing"))
    }
}
