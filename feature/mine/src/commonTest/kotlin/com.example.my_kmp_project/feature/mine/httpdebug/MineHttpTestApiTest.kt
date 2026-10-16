package com.example.my_kmp_project.feature.mine.httpdebug

import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.feature.mine.MineRecordingApiClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 契约表 #14（Dart `MineApi.fetchHarmonyIndex` + `HarmonyIndexModel.fromListData`，d7876a1）：
 * `/api/v1/transactions` 的 **`page`/`size` 方言**（page 1-based，与 #12 的 `limit`/`offset`
 * 0-based 同 path 不同调法）、**不看业务码**只判 `data` 是否为 null，
 * 以及 `title = "category · amount"` 这套展示串与逐字段兜底。
 */
class MineHttpTestApiTest {

    private val client = MineRecordingApiClient()
    private val api = MineHttpTestApi(client)

    @Test
    fun requestsPageAndSizeNotLimitOffset() = runBlocking {
        client.stub("""{"code":0,"message":"ok","data":{"list":[]}}""")
        api.fetchHarmonyIndex()
        assertEquals("GET", client.last.method)
        assertEquals("/api/v1/transactions", client.last.path)
        // Dart mine_api.dart:13 写死 `{'page': 1, 'size': 20}`；limit/offset 是 #12 的写法
        assertEquals(mapOf("page" to "1", "size" to "20"), client.last.query)
    }

    @Test
    fun mapsRowsIntoSingleTransactionsSection() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"ok","data":{"list":[
              {"id":7,"type":"income","category":"工资","amount":1200.5,
               "date":"2026-10-01","note":"十月工资"},
              {"id":8,"type":"expense","category":"保养","amount":300,
               "date":"2026-10-02","note":null}
            ],"pagination":{"page":1,"size":20,"total":2}}}
            """.trimIndent(),
        )
        val model = api.fetchHarmonyIndex()

        assertEquals(1, model.allSections.size)
        val section = model.links!!
        assertEquals("transactions", section.name)
        assertEquals("my_go_study /api/v1/transactions", section.desc)
        assertEquals(2, section.articleCount)
        assertEquals(2, model.totalArticleCount)

        val first = section.articleList[0]
        assertEquals("7", first.id)
        assertEquals("工资 · 1200.5", first.title)
        assertEquals("十月工资", first.desc)
        assertEquals("2026-10-01", first.niceDate)
        assertEquals("income", first.author)
        assertNull(first.link)
        assertNull(first.chapterName)

        // JSON null 与缺键在 Dart 里同值（`?.toString()` → null）
        val second = section.articleList[1]
        assertEquals("保养 · 300", second.title)
        assertNull(second.desc)
    }

    @Test
    fun sparseRowFallsBackPerDartRules() = runBlocking {
        // amount 为 0 时 Dart 得到 "0"（`?? '-'` 只在 null 上兜底），缺 category 才用 未分类
        client.stub("""{"code":0,"message":"ok","data":[{"id":1,"amount":0},{"amount":null}]}""")
        val articles = api.fetchHarmonyIndex().links!!.articleList

        assertEquals(2, articles.size)
        assertEquals("未分类 · 0", articles[0].title)
        assertEquals("", articles[0].niceDate)
        assertNull(articles[0].author)
        assertEquals("未分类 · -", articles[1].title)
    }

    @Test
    fun acceptsItemsDialectAndSkipsNonObjectRows() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"ok","data":{"items":[
              "垃圾行",
              {"category":"退款","amount":"12","date":"2026-09-30","type":"refund"}
            ]}}
            """.trimIndent(),
        )
        val articles = api.fetchHarmonyIndex().links!!.articleList
        assertEquals(1, articles.size)
        // amount 是字符串时 Dart 直接插值，不加引号
        assertEquals("退款 · 12", articles[0].title)
        assertEquals("2026-09-30", articles[0].niceDate)
        assertEquals("refund", articles[0].author)
    }

    @Test
    fun nullDataThrowsEvenWhenCodeIsZero() = runBlocking {
        client.stub("""{"code":0,"message":"ok","data":null}""")
        val error = assertFailsWith<NetworkError.Business> { api.fetchHarmonyIndex() }
        assertEquals("接口返回数据为空", error.message)
        assertEquals(0, error.code)
    }

    @Test
    fun missingDataThrowsWhenCodeIsNonZeroAndIgnoresServerMessage() = runBlocking {
        // Dart 的抛错文案写死在调用点，不读 envelope message（mine_api.dart:20-21）
        client.stub("""{"code":50000,"message":"服务器开小差","data":null}""")
        val error = assertFailsWith<NetworkError.Business> { api.fetchHarmonyIndex() }
        assertEquals("接口返回数据为空", error.message)
        assertEquals(50000, error.code)
    }

    @Test
    fun nonZeroCodeStillRendersWhenDataPresent() = runBlocking {
        // 调试页完全不看 code：这是 Dart 的行为，保留而非「顺手修正」
        client.stub("""{"code":50000,"message":"boom","data":{"list":[{"category":"A","amount":1}]}}""")
        val model = api.fetchHarmonyIndex()
        assertEquals(1, model.totalArticleCount)
        assertEquals("A · 1", model.links!!.articleList[0].title)
    }

    @Test
    fun malformedDataYieldsEmptySectionNotCrash() = runBlocking {
        client.stub("""{"code":0,"message":"ok","data":"I am not a list"}""")
        val model = api.fetchHarmonyIndex()
        assertEquals(0, model.totalArticleCount)
        assertEquals(emptyList(), model.links!!.articleList)
    }
}
