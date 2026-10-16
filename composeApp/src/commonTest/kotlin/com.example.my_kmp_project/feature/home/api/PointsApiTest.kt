package com.example.my_kmp_project.feature.home.api

import com.example.my_kmp_project.core.network.NetworkError
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 契约表 §2.5 #22–#25（Dart `points_api.dart` + `points_models.dart`，d7876a1）。
 *
 * 覆盖：`today_reward` / `reward` 的 **5** 缺省、`checked_in_today`/`signed`/`is_today`
 * 只认字面 `true`、`#23` 与 `#25` 的**无 body 动作 POST**（`:code` 是字符串任务码）、
 * `#24` 手搓转换器（`data.items` / 根 `items` / 缺 `code` 合成 0）与
 * `progress` 域 `incomplete|claimable|claimed`。
 *
 * **#26 不在此类**（`/mall/stores/:id/products` 归 commerce lane，契约表 §6 要求合并商品 DTO）。
 */
class PointsApiTest {

    private val client = HomeRecordingApiClient()
    private val api = PointsApi(client)

    @Test
    fun statusDecodesCalendarAndDartDefaults() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{
              "balance":320,"checked_in_today":false,"streak":3,"today_reward":8,
              "calendar":[
                {"day":"2026-10-14","signed":true,"reward":5,"is_today":false},
                {"day":"2026-10-15","signed":"yes","reward":0,"is_today":false},
                {"day":"2026-10-16"}
              ]
            }}
            """.trimIndent(),
        )
        val status = api.fetchStatus()
        assertEquals("/api/v1/points/status", client.last.path)
        assertEquals(320, status.balance)
        assertFalse(status.checkedInToday)
        assertEquals(3, status.streak)
        assertEquals(8, status.todayReward)
        assertEquals(3, status.calendar.size)

        assertEquals("2026-10-14", status.calendar[0].day)
        assertTrue(status.calendar[0].signed)
        // Dart `json['signed'] == true`：字符串 "yes" 不算签过（points_models.dart:43）
        assertFalse(status.calendar[1].signed)
        assertEquals(0, status.calendar[1].reward)
        // 缺 reward → Dart 的 ?? 5
        assertEquals(5, status.calendar[2].reward)
        assertTrue(status.calendar[2].isToday.not())
    }

    @Test
    fun statusAppliesTodayRewardDefaultFiveWhenAbsent() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"balance":10,"streak":1}}""")
        val status = api.fetchStatus()
        assertEquals(5, status.todayReward)
        assertFalse(status.checkedInToday)
        assertTrue(status.calendar.isEmpty())
    }

    @Test
    fun statusTreatsNonBooleanFlagsAsFalseInsteadOfFailingDecode() = runBlocking {
        // Dart `json['k'] == true` 对非布尔值宽容；kotlinx 若把线上字段声明成 Boolean?
        // 会因 `"yes"` / `"true"` 抛异常并让整个 data 被静默丢弃 → 必须吃下来当 false
        client.stub(
            """
            {"code":0,"message":"success","data":{
              "balance":1,"checked_in_today":"true",
              "calendar":[{"day":"2026-10-16","signed":"yes","is_today":1,"reward":"6"}]
            }}
            """.trimIndent(),
        )
        val status = api.fetchStatus()
        assertFalse(status.checkedInToday)
        assertEquals(1, status.calendar.size)
        assertFalse(status.calendar.single().signed)
        assertFalse(status.calendar.single().isToday)
        assertEquals(6, status.calendar.single().reward)

        client.stub("""{"code":0,"message":"success","data":{"checked_in_today":true}}""")
        assertTrue(api.fetchStatus().checkedInToday)
    }

    @Test
    fun statusFailureThrowsLoadSignInStateFailed() {
        client.stub("""{"code":500,"message":"boom"}""")
        assertEquals(
            "boom",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchStatus() } }.message,
        )

        // Dart `_data()`（points_api.dart:113-121）：`(model?.message.isNotEmpty ?? false) ? model!.message : fallback`
        // ⇒ 非空的服务端文案**永远盖过**中文兜底，`code:0` 且 `data` 缺失时也只报 "success"。
        client.stub("""{"code":0,"message":"success","data":null}""")
        assertEquals(
            "success",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchStatus() } }.message,
        )

        // 要拿到调用点的「加载签到状态失败」（points_api.dart:20），信封必须不带 message
        // （Dart `ResultModel.fromJson` 把缺失 message 记成空串，result_model.dart:17）。
        client.stub("""{"code":0,"data":null}""")
        assertEquals(
            "加载签到状态失败",
            assertFailsWith<NetworkError.Business> { runBlocking { api.fetchStatus() } }.message,
        )
    }

    @Test
    fun checkInIsBodylessPost() = runBlocking {
        client.stub(
            """{"code":0,"message":"success","data":{"points":8,"balance":328,"streak":4,"day":"2026-10-16"}}""",
        )
        val result = api.checkIn()
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/points/check-in", client.last.path)
        assertEquals("", client.last.body)
        assertEquals(8, result.points)
        assertEquals(328, result.balance)
        assertEquals(4, result.streak)
        assertEquals("2026-10-16", result.day)

        // Dart `_data()`（points_api.dart:113-121）：非空 message 盖过兜底 ⇒ "success"
        client.stub("""{"code":0,"message":"success","data":null}""")
        assertEquals(
            "success",
            assertFailsWith<NetworkError.Business> { runBlocking { api.checkIn() } }.message,
        )

        // 省略 message 才走到调用点中文兜底「签到失败」（points_api.dart:33）
        client.stub("""{"code":0,"data":null}""")
        assertEquals(
            "签到失败",
            assertFailsWith<NetworkError.Business> { runBlocking { api.checkIn() } }.message,
        )
    }

    @Test
    fun tasksReadDataItemsAndProgressDomain() = runBlocking {
        client.stub(
            """
            {"code":0,"message":"success","data":{"items":[
              {"code":"daily_check_in","title":"每日签到","reward":5,"progress":"claimable"},
              {"code":"first_order","title":"首单","reward":20,"progress":"claimed"},
              {"code":"invite","title":"邀请","reward":30,"progress":"weird-value"}
            ]}}
            """.trimIndent(),
        )
        val tasks = api.fetchTasks()
        assertEquals("/api/v1/points/tasks", client.last.path)
        assertEquals(3, tasks.size)
        assertEquals(TaskProgress.Claimable, tasks[0].progress)
        assertEquals(TaskProgress.Claimed, tasks[1].progress)
        // Dart TaskProgressX.parse 的 default 分支：其他一律 incomplete
        assertEquals(TaskProgress.Incomplete, tasks[2].progress)
        assertEquals("daily_check_in", tasks[0].code)
        // Dart `(json['reward'] as num?)?.toInt() ?? 0`（points_models.dart:92）：
        // invite 这行**带** `reward:30`，所以读到的就是 30；缺键才记 0（见下一个用例）。
        assertEquals(30, tasks[2].reward)
    }

    @Test
    fun taskRewardAndProgressDefaultWhenKeysAbsent() = runBlocking {
        // 真正的「缺省」形状：没有 `reward` 也没有 `progress`
        client.stub(
            """{"code":0,"message":"success","data":{"items":[{"code":"no_reward","title":"无奖励"}]}}""",
        )
        val task = api.fetchTasks().single()
        // Dart `(json['reward'] as num?)?.toInt() ?? 0`（points_models.dart:92）
        assertEquals(0, task.reward)
        // Dart `TaskProgressX.parse(null)`（:93 + :104-113 default 分支）→ incomplete
        assertEquals(TaskProgress.Incomplete, task.progress)
        assertEquals("no_reward", task.code)
    }

    @Test
    fun tasksReadRootItemsWhenDataIsNotAMap() = runBlocking {
        // Dart points_api.dart:44-46：`data` 不是 map（这里干脆没有）→ 读根 `items`
        client.stub(
            """{"code":0,"message":"success","items":[{"code":"t1","title":"任务一","reward":5,"progress":"incomplete"}]}""",
        )
        val tasks = api.fetchTasks()
        assertEquals(1, tasks.size)
        assertEquals("t1", tasks.single().code)
        assertEquals(TaskProgress.Incomplete, tasks.single().progress)
    }

    @Test
    fun tasksSynthesizeCodeZeroWhenCodeAbsent() = runBlocking {
        // Dart points_api.dart:51-58：body 无 `code` → 合成 code:0（否则 core 会判 -1 失败）
        client.stub("""{"items":[{"code":"t9","title":"任务九","reward":9}]}""")
        val tasks = api.fetchTasks()
        assertEquals(1, tasks.size)
        assertEquals("t9", tasks.single().code)
        assertEquals(9, tasks.single().reward)
        assertEquals(TaskProgress.Incomplete, tasks.single().progress)
    }

    @Test
    fun tasksYieldEmptyListWhenEnvelopeHasNoData() = runBlocking {
        // 手搓转换器永远给 list（Dart `const []`），不像 #15–#21 那样因 data 缺失抛错
        client.stub("""{"code":0,"message":"success"}""")
        assertTrue(api.fetchTasks().isEmpty())
    }

    @Test
    fun claimTaskUsesStringCodeInPathAndSendsNoBody() = runBlocking {
        client.stub("""{"code":0,"message":"success","data":{"points":20,"balance":348,"day":"2026-10-16"}}""")
        val result = api.claimTask("first_order")
        assertEquals("POST", client.last.method)
        assertEquals("/api/v1/points/tasks/first_order/claim", client.last.path)
        assertEquals("", client.last.body)
        assertEquals(20, result.points)
        assertEquals(348, result.balance)
        assertEquals("2026-10-16", result.day)

        client.stub("""{"code":1,"message":""}""")
        assertEquals(
            "领取失败",
            assertFailsWith<NetworkError.Business> { runBlocking { api.claimTask("first_order") } }.message,
        )
    }
}
