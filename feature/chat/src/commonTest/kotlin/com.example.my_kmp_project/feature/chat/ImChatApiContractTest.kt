package com.example.my_kmp_project.feature.chat

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiEnvelopeParser
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.HttpTextResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 契约表 §2.18 行 #100 / #101 / #102 / #109 / #110 / #111 / #112 / #113 / #114 的契约测试
 * （RongCloud IM BFF 侧）。
 * 对应 Dart：`im_session_api.dart` / `im_user_profile_api.dart` /
 * `http_im_backup_service.dart` / `im_group_api.dart`。
 */
class ImChatApiContractTest {

    // ---------- #100 POST /im/session ----------

    @Test
    fun session_real_body_carries_only_optional_display_name() = runNow {
        val client = FakeImApiClient {
            ok("""{"user_id":"u1","token":"tok","expires_in_seconds":"7200"}""")
        }
        val api = ImSessionApi(client = client, useMock = { false })

        val result = api.createSession(bizUserId = "biz-1", displayName = "  张三  ")

        assertEquals(1, client.calls.size)
        val call = client.calls.single()
        assertEquals("POST", call.method)
        assertEquals("/api/v1/im/session", call.path)
        val body = call.body.jsonObject()
        assertEquals(setOf("display_name"), body.keys)
        assertEquals("张三", body["display_name"].text())
        // Dart _asInt 容忍字符串数字（im_session_api.dart:27-32）
        assertEquals(ImSessionResult("u1", "tok", 7200), result)
    }

    @Test
    fun session_blank_display_name_omits_key_but_still_sends_object_body() = runNow {
        val client = FakeImApiClient { ok("""{"im_user_id":"u2","token":"t"}""") }
        val api = ImSessionApi(client = client, useMock = { false })

        val result = api.createSession(bizUserId = "biz-1", displayName = "   ")

        assertEquals("{}", client.calls.single().body)
        assertEquals(setOf(), client.calls.single().body.jsonObject().keys)
        // user_id 缺失时 im_user_id 兜底；expires_in_seconds 缺失 → 0
        assertEquals(ImSessionResult("u2", "t", 0), result)
    }

    @Test
    fun session_empty_token_throws_with_envelope_message() = runNow {
        val client = FakeImApiClient { ok("""{"user_id":"u1","token":"","expires_in_seconds":60}""") }
        val api = ImSessionApi(client = client, useMock = { false })

        val error = assertFailsWith<NetworkError.Business> {
            api.createSession(bizUserId = "biz", displayName = null)
        }
        assertEquals("ok", error.message)

        val failing = FakeImApiClient { """{"code":403,"message":"无权限","data":null}""" }
        val error2 = assertFailsWith<NetworkError.Business> {
            ImSessionApi(client = failing, useMock = { false }).createSession("biz")
        }
        assertEquals(403, error2.code)
        assertEquals("无权限", error2.message)
    }

    @Test
    fun session_mock_branch_mints_local_identity_without_http() = runNow {
        val client = FakeImApiClient { error("mock branch must not hit HTTP") }
        val registry = ImMockUserIdRegistry(newImUserId = { "im_u_fixed0000000000" })
        val api = ImSessionApi(
            client = client,
            registry = registry,
            useMock = { true },
            sleep = { },
            nowMs = { 1_700_000_000_000L },
        )

        val first = api.createSession(bizUserId = "biz-1", displayName = "张三")
        val second = api.createSession(bizUserId = "biz-1")

        assertEquals(0, client.calls.size)
        assertEquals("im_u_fixed0000000000", first.imUserId)
        assertEquals("mock_rong_token_1700000000000", first.token)
        assertEquals(3600, first.expiresInSeconds)
        // Dart registry 对同一 bizUserId 复用映射（im_user_id_registry.dart:16-25）
        assertEquals(first.imUserId, second.imUserId)
        assertEquals(setOf("biz-1"), registry.knownMappings().keys)
    }

    @Test
    fun mock_registry_persists_and_reloads_mapping() = runNow {
        val saved = mutableMapOf<String, String>()
        val first = ImMockUserIdRegistry(
            load = { saved["kv"] },
            save = { saved["kv"] = it },
            newImUserId = { "im_u_persist0000000" },
        )
        first.resolveImUserId("biz-9")
        assertEquals(1, saved.size)

        // 新实例从持久化 JSON 恢复（Dart SpManager 同一语义）
        val reloaded = ImMockUserIdRegistry(load = { saved["kv"] }, save = { saved["kv"] = it })
        assertEquals("im_u_persist0000000", reloaded.resolveImUserId("biz-9"))
    }

    @Test
    fun mock_switch_matches_dart_useMockImFor() {
        val savedForce = RongImConfig.forceMockIm
        val savedKey = RongImConfig.rongAppKey
        try {
            RongImConfig.forceMockIm = false
            assertTrue(RongImConfig.useMockImFor(null))
            assertTrue(RongImConfig.useMockImFor("   "))
            assertTrue(RongImConfig.useMockImFor("DEV_RONG_APP_KEY_PLACEHOLDER"))
            assertTrue(RongImConfig.useMockImFor("dev_rong_app_key_placeholder"))
            assertFalse(RongImConfig.useMockImFor("realappkey"))
            // USE_MOCK_IM=true 无条件生效（rong_im_config.dart:7-8）
            RongImConfig.forceMockIm = true
            assertTrue(RongImConfig.useMockImFor("realappkey"))
            RongImConfig.forceMockIm = false
            assertEquals(15, RongImConfig.CONNECT_TIMEOUT_SECONDS)
            RongImConfig.rongAppKey = ""
            assertTrue(RongImConfig.useMockIm)
        } finally {
            RongImConfig.forceMockIm = savedForce
            RongImConfig.rongAppKey = savedKey
        }
    }

    // ---------- #101 GET /im/users/profile ----------

    @Test
    fun profile_empty_id_list_short_circuits_without_http() = runNow {
        val client = FakeImApiClient { error("must not be called") }
        val api = ImUserProfileApi(client = client, useMock = { false })

        assertEquals(0, api.fetchProfiles(emptyList()).size)
        assertEquals(0, api.fetchProfiles(listOf("", "   ")).size)
        assertEquals(0, client.calls.size)
    }

    @Test
    fun profile_query_is_comma_joined_and_response_keyed_by_im_user_id() = runNow {
        val client = FakeImApiClient {
            ok(
                """{"items":[
                    {"user_id":"a","display_name":"甲","avatar_url":"https://x/a.png"},
                    {"im_user_id":"b","displayName":"乙","avatarUrl":"https://x/b.png"},
                    {"user_id":"","display_name":"跳过"}
                ]}""",
            )
        }
        val api = ImUserProfileApi(client = client, useMock = { false })

        val profiles = api.fetchProfiles(listOf(" a ", "", "b"))

        assertEquals(1, client.calls.size)
        assertEquals(mapOf("user_ids" to "a,b"), client.calls.single().query)
        assertEquals(RongImConfig.USERS_PROFILE_PATH, client.calls.single().path)
        assertEquals(
            mapOf(
                "a" to ImUserProfile("a", "甲", "https://x/a.png"),
                "b" to ImUserProfile("b", "乙", "https://x/b.png"),
            ),
            profiles,
        )
    }

    @Test
    fun profile_failure_throws_and_mock_builds_picsum_avatar() = runNow {
        val client = FakeImApiClient { """{"code":500,"message":"boom","data":null}""" }
        val api = ImUserProfileApi(client = client, useMock = { false })
        val error = assertFailsWith<NetworkError.Business> { api.fetchProfiles(listOf("a")) }
        assertEquals("boom", error.message)

        val mockApi = ImUserProfileApi(
            client = FakeImApiClient { error("no http") },
            useMock = { true },
            sleep = { },
        )
        val mockProfiles = mockApi.fetchProfiles(listOf("mock_peer_01", "u1234567"))
        assertEquals(2, mockProfiles.size)
        // Dart _mockName：含 mock_peer 取尾两位，否则「用户」+前四位（:64-70）
        assertEquals("Mock好友01", mockProfiles.getValue("mock_peer_01").displayName)
        assertEquals("用户u123", mockProfiles.getValue("u1234567").displayName)
        assertEquals(
            "https://picsum.photos/seed/mock_peer_01/200/200",
            mockProfiles.getValue("mock_peer_01").avatarUrl,
        )
    }

    // ---------- #102 POST /im/messages/backup ----------

    @Test
    fun backup_flushes_only_at_five_pending_events() = runNow {
        val client = FakeImApiClient { ok("""{"written":5}""") }
        val service = HttpImBackupService(client)

        repeat(4) { i ->
            service.backupOutbound("im-1", "private_peer", "uid-$i", "text", ImBackupPayloads.text("t$i"), SENT_AT)
        }
        assertEquals(4, service.pendingCount)
        assertEquals(0, client.calls.size)

        service.backupOutbound("im-1", "private_peer", "uid-4", "text", ImBackupPayloads.text("t4"), SENT_AT)
        assertEquals(0, service.pendingCount)
        val call = client.calls.single()
        assertEquals(RongImConfig.MESSAGES_BACKUP_PATH, call.path)
        val events = call.body.jsonObject().getValue("events") as JsonArray
        assertEquals(5, events.size)
        assertEquals(listOf("uid-0", "uid-1", "uid-2", "uid-3", "uid-4"), events.map { it.text("message_uid") })
        assertEquals("2023-11-14T22:13:20Z", events.first().text("sent_at"))
    }

    @Test
    fun backup_batch_is_requeued_at_head_on_business_failure() = runNow {
        val client = FakeImApiClient { """{"code":500,"message":"boom","data":null}""" }
        val service = HttpImBackupService(client)

        repeat(5) { i ->
            service.backupInbound("im-1", "private_peer", "uid-$i", "text", ImBackupPayloads.content("c"), SENT_AT)
        }
        // 整批回头重排（http_im_backup_service.dart:87），队列不清空
        assertEquals(5, service.pendingCount)
        assertEquals(1, client.calls.size)
    }

    @Test
    fun backup_batch_is_requeued_at_head_on_transport_exception() = runNow {
        val client = FakeImApiClient { throw NetworkError.Transport("socket died") }
        val service = HttpImBackupService(client)

        repeat(5) { i -> service.backupOutbound("im-1", "c", "u$i", "text", ImBackupPayloads.empty(), 1L) }
        assertEquals(5, service.pendingCount)
        assertEquals(1, client.calls.size)
    }

    /**
     * 契约表 #102 的 at-least-once 重排（§5.3）。
     *
     * 期望值以 Dart 为准（`http_im_backup_service.dart`）：
     * - :87 失败时 `_pending.insertAll(0, batch)` —— 整批回到**队头**，顺序不变、不清空；
     * - :28 每次 `_pending.add(...)` 之后**都**判 `if (_pending.length >= 5) await flushPending()`，
     *   判的是「当前队列总长」而不是「本次新增条数」；
     * - :74-75 flush 快照的是**整个** `_pending`。
     * 所以重排后的 5 条之上再来第 6 条时，第 6 条会立刻带着前面 5 条一起 flush（旧批次在前），
     * 剩下的新事件只有 3 条 —— 队列长度必须是 3，不是 4。
     */
    @Test
    fun at_least_once_requeued_batch_is_resent_before_newer_events() = runNow {
        var shouldFail = true
        val client = FakeImApiClient {
            if (shouldFail) """{"code":500,"message":"boom","data":null}""" else ok("""{"written":9}""")
        }
        val service = HttpImBackupService(client)

        // 1) 攒够 5 条触发 flush，失败 ⇒ 整批插回队头（Dart :87）
        repeat(5) { i -> service.backupOutbound("im-1", "c", "uid-$i", "text", ImBackupPayloads.text("b"), SENT_AT) }
        assertEquals(5, service.pendingCount)
        assertEquals(1, client.calls.size)

        // 2) 新事件继续入队：队头已有重排的 5 条，故 new-0 入队即满 6 条 >= 5 ⇒ 再次 flush，
        //    快照整队 ⇒ 旧批次必然排在新事件之前（Dart :28 + :74-75）
        shouldFail = false
        repeat(4) { i -> service.backupOutbound("im-1", "c", "new-$i", "text", ImBackupPayloads.text("n"), SENT_AT) }
        assertEquals(3, service.pendingCount)
        assertEquals(2, client.calls.size)
        assertEquals(
            listOf("uid-0", "uid-1", "uid-2", "uid-3", "uid-4", "new-0"),
            (client.calls[1].body.jsonObject().getValue("events") as JsonArray).map { it.text("message_uid") },
        )

        // 3) 剩余 3 条留在队里等下一次阈值/手动 flush —— 顺序仍是入队顺序
        service.flushPending()
        assertEquals(0, service.pendingCount)
        assertEquals(3, client.calls.size)
        assertEquals(
            listOf("new-1", "new-2", "new-3"),
            (client.calls.last().body.jsonObject().getValue("events") as JsonArray).map { it.text("message_uid") },
        )

        // 4) 一条都没丢：三批合计 5 + 6 + 3 = 14 条投递，去重后正好 9 个唯一 message_uid
        //    ⇒ at-least-once（旧批次被投两次，服务端按 message_uid 去重；客户端不去重、不丢批）
        val sent = client.calls.flatMap { call ->
            (call.body.jsonObject().getValue("events") as JsonArray).map { it.text("message_uid") }
        }
        assertEquals(9, sent.distinct().size)
        assertEquals(14, sent.size)
        val resentUids = (client.calls[1].body.jsonObject().getValue("events") as JsonArray).map { it.text("message_uid") }
        assertEquals(listOf("uid-0", "uid-1", "uid-2", "uid-3", "uid-4"), resentUids.take(5))
    }

    @Test
    fun backup_recall_sends_empty_payload_and_private_conversation_type() = runNow {
        val client = FakeImApiClient { ok("{}") }
        val service = HttpImBackupService(client)

        service.backupRecall("im-1", "private_peer", "uid-r", SENT_AT + 1L)
        assertEquals(1, service.pendingCount)
        service.flushPending()

        val recallEvent = (client.calls.single().body.jsonObject().getValue("events") as JsonArray).single()
        assertEquals("recall", recallEvent.text("direction"))
        assertEquals("recall", recallEvent.text("message_type"))
        assertEquals("private", recallEvent.text("conversation_type"))
        assertEquals("private_peer", recallEvent.text("target_id"))
        assertEquals("uid-r", recallEvent.text("message_uid"))
        assertEquals(0, recallEvent.jsonObject("payload").keys.size)
        assertEquals("2023-11-14T22:13:20.001Z", recallEvent.text("sent_at"))
        // imUserId 不进 body（Dart HttpImBackupService 收下即忽略）
        assertEquals(setOf("message_uid", "direction", "conversation_type", "target_id", "message_type", "payload", "sent_at"), recallEvent.jsonObject().keys)
    }

    @Test
    fun empty_flush_does_not_hit_http() = runNow {
        val client = FakeImApiClient { ok("{}") }
        HttpImBackupService(client).flushPending()
        assertEquals(0, client.calls.size)
    }

    @Test
    fun mock_backup_service_queues_without_http_and_recall_does_not_enqueue() = runNow {
        val service = MockImBackupService(sleep = { })
        assertEquals(0, service.pendingCount)
        service.backupRecall("im", "c", "uid", 1L)
        assertEquals(0, service.pendingCount)
        repeat(4) { i -> service.backupInbound("im", "c", "u$i", "text", ImBackupPayloads.empty(), 1L) }
        assertEquals(4, service.pendingCount)
        service.backupInbound("im", "c", "u4", "text", ImBackupPayloads.empty(), 1L)
        assertEquals(0, service.pendingCount)
        service.flushPending()
        assertEquals(0, service.pendingCount)
    }

    @Test
    fun backup_service_selection_follows_mock_switch() {
        val http = createImBackupService(client = FakeImApiClient { ok("{}") }, useMock = { false })
        val mock = createImBackupService(client = FakeImApiClient { ok("{}") }, useMock = { true }, sleep = { })
        assertTrue(http is HttpImBackupService)
        assertTrue(mock is MockImBackupService)
    }

    @Test
    fun iso8601_utc_format_and_parse_round_trip() {
        assertEquals("2023-11-14T22:13:20Z", ImIso8601Utc.format(SENT_AT))
        assertEquals("2023-11-14T22:13:20.001Z", ImIso8601Utc.format(SENT_AT + 1L))
        assertEquals("1970-01-01T00:00:00Z", ImIso8601Utc.format(0L))
        assertEquals("2000-02-29T12:00:00Z", ImIso8601Utc.format(951_825_600_000L))
        assertEquals(SENT_AT, ImIso8601Utc.parseToEpochMillis("2023-11-14T22:13:20Z"))
        assertEquals(SENT_AT + 1L, ImIso8601Utc.parseToEpochMillis("2023-11-14T22:13:20.001Z"))
        assertEquals(SENT_AT + 1L, ImIso8601Utc.parseToEpochMillis("2023-11-14T22:13:20.001000Z"))
        assertEquals(SENT_AT, ImIso8601Utc.parseToEpochMillis("2023-11-15T06:13:20+08:00"))
        assertNull(ImIso8601Utc.parseToEpochMillis("not-a-date"))

        val event = ImMessageBackupEvent(
            messageUid = "uid",
            direction = ImBackupDirection.Out,
            conversationId = "private_peer",
            messageType = "text",
            payload = ImBackupPayloads.text("hi"),
            sentAtEpochMs = SENT_AT + 123L,
        )
        val json = event.toJson()
        assertEquals(event, ImMessageBackupEvent.fromJson(json))
        assertEquals("hi", ImBackupPayloads.stringValue(json.jsonObject("payload"), "text"))
    }

    @Test
    fun payload_builders_match_dart_literals() {
        val voice = ImBackupPayloads.voice("/p.aac", 3)
        assertEquals(setOf("localPath", "duration"), voice.keys)
        // duration 是 JSON 数字而非字符串（Dart `{'localPath': …, 'duration': seconds}`）
        assertFalse((voice["duration"] as JsonPrimitive).isString)
        assertEquals("3", (voice["duration"] as JsonPrimitive).content)
        assertEquals(
            setOf("customType", "title"),
            ImBackupPayloads.custom("card", mapOf("title" to "Demo 名片")).keys,
        )
        assertEquals(0, ImBackupPayloads.empty().keys.size)
        assertEquals(setOf("content"), ImBackupPayloads.content("x").keys)
        assertEquals(setOf("localPath"), ImBackupPayloads.image("/p.png").keys)
    }

    // ---------- #109 / #110 / #111 / #112 / #113 / #114 群组 ----------

    @Test
    fun free_group_create_and_member_actions_send_expected_bodies() = runNow {
        val client = FakeImApiClient { ok("""{"group_id":"g1","name":"群","owner_user_id":"u1"}""") }
        val api = ImGroupApi(client)

        val created = api.createFreeGroup(name = "群", memberIds = listOf("u1", "u2"))
        assertEquals(ImFreeGroupResult("g1", "群", "u1"), created)
        assertEquals(RongImConfig.GROUPS_FREE_PATH, client.calls.last().path)
        val createBody = client.calls.last().body.jsonObject()
        assertEquals(setOf("name", "member_ids"), createBody.keys)
        assertEquals(
            listOf("u1", "u2"),
            (createBody.getValue("member_ids") as JsonArray).map { it.text() },
        )

        api.invite("g1", listOf("u3"))
        assertEquals("/api/v1/im/groups/free/g1/invite", client.calls.last().path)
        assertEquals(setOf("member_ids"), client.calls.last().body.jsonObject().keys)

        api.kick("g1", listOf("u3"))
        assertEquals("/api/v1/im/groups/free/g1/kick", client.calls.last().path)
    }

    @Test
    fun quit_and_dismiss_send_explicit_empty_object_body() = runNow {
        val client = FakeImApiClient { ok("{}") }
        val api = ImGroupApi(client)

        api.quit("g9")
        assertEquals("/api/v1/im/groups/free/g9/quit", client.calls.single().path)
        // §5.12：显式 `{}`，不是省略 body
        assertEquals("{}", client.calls.single().body)
        assertEquals(0, client.calls.single().body.jsonObject().keys.size)

        api.dismiss("g9")
        assertEquals("/api/v1/im/groups/free/g9/dismiss", client.calls.last().path)
        assertEquals("{}", client.calls.last().body)
        assertEquals(2, client.calls.size)
    }

    // ---------- #110 invite ----------

    @Test
    fun row_110_invite_posts_member_ids_and_ignores_the_raw_map_response() = runNow {
        val client = FakeImApiClient { ok("""{"added":["u3","u4"],"owner":"u1"}""") }

        // Dart `Future<void> invite(...)`（im_group_api.dart:86-93）：
        // 契约表里的「原始 Map」是 converter 形状，_postOk（:112）收下即丢弃 ⇒ KMP 返回 Unit
        ImGroupApi(client).invite("g7", listOf("u3", "u4"))

        val call = client.calls.single()
        assertEquals("POST", call.method)
        assertEquals("/api/v1/im/groups/free/g7/invite", call.path)
        assertEquals("""{"member_ids":["u3","u4"]}""", call.body)
        assertEquals(setOf("member_ids"), call.body.jsonObject().keys)
    }

    @Test
    fun row_110_invite_sends_member_ids_key_even_when_the_list_is_empty() = runNow {
        val client = FakeImApiClient { ok("{}") }
        ImGroupApi(client).invite("g7", emptyList())

        // Dart 无条件 `{'member_ids': memberIds}`（im_group_api.dart:91）：空表也要发键
        assertEquals("""{"member_ids":[]}""", client.calls.single().body)
    }

    // ---------- #111 kick ----------

    @Test
    fun row_111_kick_posts_member_ids_to_the_kick_path() = runNow {
        val client = FakeImApiClient { ok("""{"removed":2}""") }
        ImGroupApi(client).kick("g7", listOf("u3", "u4"))

        val call = client.calls.single()
        assertEquals("POST", call.method)
        assertEquals("/api/v1/im/groups/free/g7/kick", call.path)
        assertEquals("""{"member_ids":["u3","u4"]}""", call.body)
    }

    @Test
    fun row_111_kick_failure_keeps_server_message_over_the_fallback() = runNow {
        // §5.13：服务端 message 非空时盖过「群操作失败」兜底（im_group_api.dart:123）
        val client = FakeImApiClient { """{"code":403,"message":"not the owner","data":null}""" }
        val error = assertFailsWith<NetworkError.Business> { ImGroupApi(client).kick("g7", listOf("u3")) }

        assertEquals(403, error.code)
        assertEquals("not the owner", error.message)
    }

    // ---------- #112 quit ----------

    @Test
    fun row_112_quit_sends_an_explicit_empty_object_not_an_absent_body() = runNow {
        val client = FakeImApiClient { ok("""{"ok":true}""") }
        ImGroupApi(client).quit("g7")

        val call = client.calls.single()
        assertEquals("POST", call.method)
        assertEquals("/api/v1/im/groups/free/g7/quit", call.path)
        // §5.12 + 契约表 #112：Dart :105 传的是 `{}` 字面量，不是省略 body（空串在 Ktor 侧等于不发）
        assertEquals("{}", call.body)
        assertFalse(call.body.isEmpty())
        assertEquals(0, call.body.jsonObject().keys.size)
    }

    @Test
    fun row_112_quit_succeeds_when_data_is_null_because_only_code_is_checked() = runNow {
        // Dart `_postOk` 只看 `isSuccess`（im_group_api.dart:121）：code 0 + data null 也算成功
        val client = FakeImApiClient { """{"code":0,"message":"ok","data":null}""" }
        ImGroupApi(client).quit("g7")

        assertEquals(1, client.calls.size)
        assertEquals("{}", client.calls.single().body)
    }

    // ---------- #113 dismiss ----------

    @Test
    fun row_113_dismiss_sends_an_explicit_empty_object_and_ignores_data() = runNow {
        val client = FakeImApiClient { """{"code":0,"message":"ok","data":null}""" }
        ImGroupApi(client).dismiss("g7")

        val call = client.calls.single()
        assertEquals("POST", call.method)
        assertEquals("/api/v1/im/groups/free/g7/dismiss", call.path)
        assertEquals("{}", call.body)
        assertEquals(0, call.body.jsonObject().keys.size)
    }

    @Test
    fun row_113_dismiss_failure_falls_back_to_the_group_operation_message() = runNow {
        val client = FakeImApiClient { """{"code":500,"data":null}""" }
        val error = assertFailsWith<NetworkError.Business> { ImGroupApi(client).dismiss("g7") }

        assertEquals(500, error.code)
        assertEquals("群操作失败", error.message)
    }

    @Test
    fun group_action_failure_throws_with_friendly_fallbacks() = runNow {
        val client = FakeImApiClient { """{"code":403,"message":"not a member","data":null}""" }
        val api = ImGroupApi(client)
        val error = assertFailsWith<NetworkError.Business> { api.quit("g1") }
        assertEquals(403, error.code)
        assertEquals("not a member", error.message)

        val noMessage = FakeImApiClient { """{"code":1,"data":null}""" }
        val error2 = assertFailsWith<NetworkError.Business> { ImGroupApi(noMessage).kick("g1", listOf("u")) }
        assertEquals("群操作失败", error2.message)
    }

    @Test
    fun store_group_sync_sends_store_id_as_string() = runNow {
        val client = FakeImApiClient { ok("""{"store_id":"7","group_id":"g7"}""") }
        val result = ImGroupApi(client).syncStoreGroup("7")

        assertEquals(ImStoreGroupResult("7", "g7"), result)
        assertEquals(RongImConfig.GROUPS_STORE_SYNC_PATH, client.calls.single().path)
        assertEquals("""{"store_id":"7"}""", client.calls.single().body)
    }

    @Test
    fun store_group_result_stringifies_numeric_ids() {
        // Dart `(json['store_id'] ?? '').toString()`（im_group_api.dart:34-36）
        val json = NetworkJson.parseToJsonElement("""{"store_id":7,"group_id":42}""").jsonObject()
        assertEquals(ImStoreGroupResult("7", "42"), ImStoreGroupResult.fromJson(json))
    }

    private fun ok(dataJson: String): String = """{"code":0,"message":"ok","data":$dataJson}"""

    private fun String.jsonObject(): JsonObject =
        NetworkJson.parseToJsonElement(this) as? JsonObject ?: error("Not a JSON object: $this")

    private fun JsonElement?.jsonObject(): JsonObject =
        this as? JsonObject ?: error("Expected JSON object but was $this")

    private fun JsonElement?.jsonObject(key: String): JsonObject = jsonObject()[key].jsonObject()

    private fun JsonElement?.text(): String = (this as? JsonPrimitive)?.content ?: ""

    private fun JsonElement?.text(key: String): String = jsonObject()[key].text()

    private companion object {
        /** 2023-11-14T22:13:20Z —— 整数秒，便于断言 ISO 串无小数位。 */
        const val SENT_AT: Long = 1_700_000_000_000L
    }
}

/** 记录型假 client：只替换传输层，信封解析仍走真实 [ApiEnvelopeParser]。 */
private class FakeImApiClient(
    private val responder: (ImFakeCall) -> String,
) : ApiClient {
    val calls: MutableList<ImFakeCall> = mutableListOf()

    private fun reply(
        method: String,
        path: String,
        body: String,
        query: Map<String, String> = emptyMap(),
    ): String {
        val call = ImFakeCall(method, path, body, query)
        calls += call
        return responder(call)
    }

    override suspend fun getRaw(path: String, query: Map<String, String>): String =
        reply("GET", path, "", query)

    override suspend fun getAbsoluteRaw(url: String): String = reply("GET", url, "")

    override suspend fun postRaw(path: String, body: String): String = reply("POST", path, body)

    override suspend fun postForm(path: String, fields: Map<String, String>): HttpTextResponse =
        error("postForm unused in IM contracts")

    override suspend fun putRaw(path: String, body: String): String = reply("PUT", path, body)

    override suspend fun patchRaw(path: String, body: String): String = reply("PATCH", path, body)

    override suspend fun deleteRaw(path: String): String = reply("DELETE", path, "")

    override suspend fun <T> getApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("GET", path, "", query), parseData)

    override suspend fun <T> postApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("POST", path, body), parseData)

    override suspend fun <T> putApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("PUT", path, body), parseData)

    override suspend fun <T> patchApi(
        path: String,
        body: String,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("PATCH", path, body), parseData)

    override suspend fun <T> deleteApi(
        path: String,
        query: Map<String, String>,
        parseData: (JsonElement?) -> T?,
    ): ApiResponse<T> = ApiEnvelopeParser.parse(reply("DELETE", path, "", query), parseData)
}

data class ImFakeCall(
    val method: String,
    val path: String,
    val body: String,
    val query: Map<String, String>,
)

/**
 * commonTest 没有 kotlinx-coroutines-test 依赖，这里同步跑完挂起代码：
 * 桩 client 不真正挂起（注入的 sleep 均为 no-op），一旦真的挂起就直接失败。
 */
private fun runNow(block: suspend () -> Unit) {
    var failure: Throwable? = null
    var completed = false
    block.startCoroutine(
        object : Continuation<Unit> {
            override val context = kotlin.coroutines.EmptyCoroutineContext

            override fun resumeWith(result: kotlin.Result<Unit>) {
                completed = true
                failure = result.exceptionOrNull()
            }
        },
    )
    if (!completed) {
        throw IllegalStateException("Test coroutine suspended — stubs must not delay")
    }
    failure?.let { throw it }
}
