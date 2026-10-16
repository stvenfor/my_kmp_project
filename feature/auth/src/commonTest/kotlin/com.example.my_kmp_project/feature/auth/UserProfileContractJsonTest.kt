package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.ApiEnvelope
import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.dataOrNull
import com.example.my_kmp_project.core.network.decodeEnvelopeData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 契约表 §2.2 #8–#11（`features/auth/lib/api/user_profile_api.dart` +
 * `user_profile_models.dart` @ d7876a1）的 JSON 夹具测试：
 * `data` 字段映射（大小写别名 / `stats` snake_case）、
 * `store_id` query 省略、PATCH 的 omit-vs-send 规则、`/me` 与 `/profiles` 前缀分裂。
 */
class UserProfileContractJsonTest {

    private inline fun <reified T> dataOf(raw: String): T? =
        decodeEnvelopeData<T>(
            NetworkJson.decodeFromString(ApiEnvelope.serializer(), raw).dataOrNull(),
        )

    private inline fun <reified T> bodyOf(value: T): JsonObject =
        NetworkJson.parseToJsonElement(NetworkJson.encodeToString(value)) as JsonObject

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

    // ---- path 前缀（契约表 #11 备注）---------------------------------------

    @Test
    fun profile_paths_keep_the_me_prefix_split() {
        assertEquals("/api/v1/profiles/me", ProfileApiPaths.ME)
        assertEquals("/api/v1/profiles/me/store", ProfileApiPaths.ME_STORE)
        // #11：可切换门店列表前缀是 /api/v1/me/，**不是** /api/v1/profiles/。
        assertEquals("/api/v1/me/stores", ProfileApiPaths.MY_STORES)
        assertFalse(ProfileApiPaths.MY_STORES.startsWith("/api/v1/profiles"))
    }

    // ---- #8 GET /profiles/me?store_id= --------------------------------------

    @Test
    fun me_query_trims_store_id_and_omits_blank() {
        // Dart `if (storeId != null && storeId.trim().isNotEmpty) 'store_id': storeId.trim()`。
        assertEquals(mapOf("store_id" to "7"), profileMeQuery(" 7 "))
        assertEquals(emptyMap(), profileMeQuery("   "))
        assertEquals(emptyMap(), profileMeQuery(null))
        assertEquals(emptyMap(), profileMeQuery(""))
    }

    @Test
    fun profile_me_maps_camel_case_aliases_and_snake_stats() {
        val data = dataOf<UserProfileData>(
            """
            {
              "code": 0,
              "message": "success",
              "data": {
                "id": "u-1",
                "user_id": "u-1",
                "user_name": "zhangsan",
                "email": "a@b.com",
                "status": 1,
                "displayName": "张三",
                "avatarUrl": "https://cdn/a.png",
                "phone": "13800138000",
                "stats": {
                  "store_id": 7,
                  "store_name": "华东一店",
                  "days_joined": 12,
                  "employee_count": 30,
                  "store_days": 100,
                  "total_customers": 250,
                  "role": 2
                }
              },
              "timestamp": 1700000000
            }
            """.trimIndent(),
        )!!
        assertEquals("u-1", data.resolvedId)
        assertEquals("u-1", data.resolvedUserId)
        assertEquals("zhangsan", data.resolvedUserName)
        assertEquals("张三", data.resolvedDisplayName)
        assertEquals("a@b.com", data.resolvedEmail)
        assertEquals(1, data.resolvedStatus)
        assertEquals("https://cdn/a.png", data.resolvedAvatarUrl)
        assertEquals("13800138000", data.resolvedPhone)

        val stats = data.resolvedStats()
        assertEquals(7, stats.resolvedStoreId)
        assertEquals("华东一店", stats.resolvedStoreName)
        assertEquals(12, stats.resolvedDaysJoined)
        assertEquals(30, stats.resolvedEmployeeCount)
        assertEquals(100, stats.resolvedStoreDays)
        assertEquals(250, stats.resolvedTotalCustomers)
        assertEquals(2, stats.resolvedRole)
        // role_label 缺失 → 客户端按 role 推导（总经理）。
        assertEquals("总经理", stats.resolvedRoleLabel)
    }

    @Test
    fun profile_me_accepts_snake_case_aliases_too() {
        // 契约表 #8：「大小写混用都接受」。
        val data = dataOf<UserProfileData>(
            """
            {"code":0,"message":"success","data":{
              "user_name":"lisi","display_name":"李四","avatar_url":"https://cdn/b.png",
              "userName":"ignored","displayName":"ignored",
              "stats":{"storeId":8,"storeName":"华南店","daysJoined":1,"employeeCount":2,
                      "storeDays":3,"totalCustomers":4,"role":1,"roleLabel":"经理"}}}
            """.trimIndent(),
        )!!
        assertEquals("lisi", data.resolvedUserName)
        // display_name 与 displayName 同时出现时按 Dart 顺序取 camel（:30）。
        assertEquals("ignored", data.resolvedDisplayName)
        assertEquals("https://cdn/b.png", data.resolvedAvatarUrl)
        assertEquals(8, data.resolvedStats().resolvedStoreId)
        assertEquals("华南店", data.resolvedStats().resolvedStoreName)
        // 显式 roleLabel 优先于推导值。
        assertEquals("经理", data.resolvedStats().resolvedRoleLabel)
    }

    @Test
    fun profile_me_without_stats_falls_back_to_zero() {
        val data = dataOf<UserProfileData>(
            """{"code":0,"message":"success","data":{"id":"u-1","user_name":"x"}}""",
        )!!
        val stats = data.resolvedStats()
        assertEquals(0, stats.resolvedStoreId)
        assertEquals(0, stats.resolvedTotalCustomers)
        // Dart `UserStoreStats.zero`：role 0 → 销售顾问。
        assertEquals(0, stats.resolvedRole)
        assertEquals("销售顾问", stats.resolvedRoleLabel)
        // id 缺失时回落 user_id（:32-33）。
        assertEquals("u-1", data.resolvedId)
    }

    @Test
    fun profile_me_id_and_name_cross_fallbacks() {
        val data = dataOf<UserProfileData>(
            """{"code":0,"message":"success","data":{"user_id":"u-9","display_name":"昵称"}}""",
        )!!
        assertEquals("u-9", data.resolvedId)
        assertEquals("u-9", data.resolvedUserId)
        assertEquals("昵称", data.resolvedDisplayName)
        // userName 为空 → 取 displayName（:34）。
        assertEquals("昵称", data.resolvedUserName)
        // username 键也能兜住 displayName（:30）。
        val viaUsername = dataOf<UserProfileData>(
            """{"code":0,"message":"success","data":{"id":"u-9","username":"wangwu"}}""",
        )!!
        assertEquals("wangwu", viaUsername.resolvedDisplayName)
    }

    @Test
    fun stats_int_values_clamp_negative_to_zero() {
        // Dart `UserStoreStats._intOf`：负数 / 非数字 → 0（:167-171）。
        val stats = dataOf<UserStoreStatsData>(
            """
            {"code":0,"message":"success","data":
              {"store_id":-5,"days_joined":-1,"employee_count":3,"role":-2,"total_customers":9}}
            """.trimIndent(),
        )!!
        assertEquals(0, stats.resolvedStoreId)
        assertEquals(0, stats.resolvedDaysJoined)
        assertEquals(3, stats.resolvedEmployeeCount)
        assertEquals(0, stats.resolvedRole)
        assertEquals(9, stats.resolvedTotalCustomers)
        assertEquals("销售顾问", stats.resolvedRoleLabel)
    }

    @Test
    fun stats_int_values_accept_numeric_strings() {
        val stats = dataOf<UserStoreStatsData>(
            """
            {"code":0,"message":"success","data":{"store_id":"7","role":"1","store_days":"12"}}
            """.trimIndent(),
        )!!
        assertEquals(7, stats.resolvedStoreId)
        assertEquals(1, stats.resolvedRole)
        assertEquals(12, stats.resolvedStoreDays)
        assertEquals("销售经理", stats.resolvedRoleLabel)
    }

    // ---- #9 PATCH /profiles/me ---------------------------------------------

    @Test
    fun store_switch_path_is_me_path_plus_store_and_stores_uses_the_other_prefix() {
        // Dart `'$mePath/store'`（user_profile_api.dart:46）拼出的就是 #10。
        assertEquals("${ProfileApiPaths.ME}/store", ProfileApiPaths.ME_STORE)
        // #11 与 #8/#9/#10 不同前缀，绝不能被误并到 /profiles 下。
        assertTrue(ProfileApiPaths.MY_STORES != ProfileApiPaths.ME_STORE)
    }

    @Test
    fun patch_rename_writes_both_user_name_and_display_name() {
        // Dart `UpdateUserProfileRequest.toJson`：`map['user_name']=…; map['display_name']=…`。
        val body = bodyOf(UserProfileUpdateBody.of(displayName = " 张三 "))
        assertEquals(setOf("user_name", "display_name"), body.keys)
        assertEquals("张三", body.text("user_name"))
        assertEquals("张三", body.text("display_name"))
    }

    @Test
    fun patch_avatar_mime_only_travels_with_avatar_base64() {
        // 只改名 → 不带任何头像键。
        assertFalse(bodyOf(UserProfileUpdateBody.of(displayName = "x")).keys.any { it.startsWith("avatar") })

        // 带头像且不给 mime → 默认 image/jpeg（:194）。
        val withDefault = bodyOf(UserProfileUpdateBody.of(displayName = null, avatarBase64 = "AAAB"))
        assertEquals(setOf("avatar_base64", "avatar_mime"), withDefault.keys)
        assertEquals("AAAB", withDefault.text("avatar_base64"))
        assertEquals("image/jpeg", withDefault.text("avatar_mime"))

        // 显式 mime 覆盖默认值。
        val withMime = bodyOf(
            UserProfileUpdateBody.of(avatarBase64 = "AAAB", avatarMime = "image/png"),
        )
        assertEquals("image/png", withMime.text("avatar_mime"))

        // 空串 avatar_base64 视为「没带头像」（Dart `isNotEmpty` 判定）。
        assertEquals(
            emptySet(),
            bodyOf(UserProfileUpdateBody.of(avatarBase64 = "", avatarMime = "image/png")).keys,
        )

        // 稀疏 PATCH：全 null → 空对象，绝不发 null 值。
        assertEquals(emptySet(), bodyOf(UserProfileUpdateBody.of()).keys)
    }

    @Test
    fun patch_me_returns_updated_profile() {
        val data = dataOf<UserProfileData>(
            """
            {"code":0,"message":"success","data":
              {"id":"u-1","user_name":"新名字","displayName":"新名字",
               "stats":{"store_id":7,"role":0,"role_label":"销售顾问"}}}
            """.trimIndent(),
        )!!
        assertEquals("新名字", data.resolvedUserName)
        assertEquals("销售顾问", data.resolvedStats().resolvedRoleLabel)
    }

    @Test
    fun patch_me_10001_maps_to_profile_param_invalid_and_keeps_session() {
        // 契约表 #9 / §4：10001 → UnknownAuthFailure('资料参数无效')，不判登录失效。
        assertEquals("资料参数无效", mapProfileFailure(10001, null))
        assertEquals("头像过大", mapProfileFailure(10001, "头像过大"))
        // 无业务码但 message 命中头像/参数关键词：同样按「资料参数无效」族处理（Dart :97-102）。
        assertEquals("avatar_mime 不受支持", mapProfileFailure(null, "avatar_mime 不受支持"))
        // 401 / 10023：会话已被守卫清掉，文案不得伪装成可重试业务错。
        assertEquals(AuthFailure.SessionCleared().message, mapProfileFailure(null, null, 401))
        assertEquals(
            AuthFailure.SessionCleared().message,
            mapProfileFailure(AuthBizCode.TOKEN_INVALID, "token 无效"),
        )
        // 互踢 10021 走 auth 同款文案（保留会话判定交给上层，见 mapAuthFailure）。
        assertEquals(AuthFailure.SessionReplaced().message, mapProfileFailure(10021, "账号已在其他设备登录"))
    }

    // ---- #10 POST /profiles/me/store ---------------------------------------

    @Test
    fun switch_store_body_sends_int_store_id() {
        val body = bodyOf(UserStoreSwitchBody(storeId = 7))
        assertEquals(setOf("store_id"), body.keys)
        assertEquals("7", body.text("store_id"))
    }

    @Test
    fun switch_store_returns_new_stats_not_a_boolean() {
        val stats = dataOf<UserStoreStatsData>(
            """
            {"code":0,"message":"success","data":
              {"store_id":9,"store_name":"总店","days_joined":5,"employee_count":42,
               "store_days":365,"total_customers":1200,"role":1,"role_label":"经理"}}
            """.trimIndent(),
        )!!
        assertEquals(9, stats.resolvedStoreId)
        assertEquals("总店", stats.resolvedStoreName)
        assertEquals(5, stats.resolvedDaysJoined)
        assertEquals(42, stats.resolvedEmployeeCount)
        assertEquals(365, stats.resolvedStoreDays)
        assertEquals(1200, stats.resolvedTotalCustomers)
        assertEquals(1, stats.resolvedRole)
        assertEquals("经理", stats.resolvedRoleLabel)
    }

    // ---- #11 GET /me/stores ------------------------------------------------

    @Test
    fun my_stores_result_maps_list_and_current_store_id() {
        val data = dataOf<UserStoreListData>(
            """
            {
              "code": 0,
              "message": "success",
              "data": {
                "list": [
                  {"store_id": 7, "store_name": "华东一店", "role": 0, "is_current": true},
                  {"store_id": 8, "store_name": "华南店", "role": 2, "role_label": "总经理", "is_current": false},
                  {"store_id": 9, "store_name": "总店", "role": 1}
                ],
                "current_store_id": 7
              }
            }
            """.trimIndent(),
        )!!
        assertEquals(7, data.currentStoreId)
        assertEquals(3, data.rows.size)
        assertEquals(7, data.rows[0].storeId)
        assertTrue(data.rows[0].isCurrent)
        // role_label 缺失 → 由 role 推导。
        assertEquals("销售顾问", data.rows[0].roleLabel)
        assertEquals("总经理", data.rows[1].roleLabel)
        assertEquals("销售经理", data.rows[2].roleLabel)
        assertFalse(data.rows[1].isCurrent)
    }

    @Test
    fun my_stores_result_accepts_camel_aliases_and_missing_list() {
        val data = dataOf<UserStoreListData>(
            """
            {"code":0,"message":"success","data":
              {"list":[{"storeId":3,"storeName":"A","roleLabel":"_label","isCurrent":true}],
               "currentStoreId":3}}
            """.trimIndent(),
        )!!
        assertEquals(3, data.currentStoreId)
        assertEquals(3, data.rows.single().storeId)
        assertEquals("A", data.rows.single().storeName)
        assertEquals("_label", data.rows.single().roleLabel)
        assertTrue(data.rows.single().isCurrent)

        val empty = dataOf<UserStoreListData>(
            """{"code":0,"message":"success","data":{}}""",
        )!!
        assertEquals(emptyList(), empty.rows)
        // current_store_id = 0 → 服务端没表态，调用方保留本地已选门店。
        assertEquals(0, empty.currentStoreId)
    }

    @Test
    fun my_stores_is_current_only_accepts_literal_true() {
        // Dart `json['is_current'] == true || json['isCurrent'] == true`：非 true（含缺省）一律 false。
        val data = dataOf<UserStoreListData>(
            """
            {"code":0,"message":"success","data":{"list":[
              {"store_id":1,"is_current":false},{"store_id":2},
              {"store_id":3,"isCurrent":true}]}}
            """.trimIndent(),
        )!!
        assertEquals(
            listOf(false, false, true),
            data.rows.map { it.isCurrent },
        )
    }
}
