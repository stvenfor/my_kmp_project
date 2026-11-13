package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.NetworkJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * B4：Go `profile_routes.go:24-25`（GET|PATCH /me/profile）与
 * `access/router.go:55`（GET /me/permissions）契约：路径 + verb + 字段。
 */
class MeProfileContractTest {

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

    @Test
    fun me_profile_and_permissions_paths_match_backend_routes() {
        assertEquals("/api/v1/me/profile", ProfileApiPaths.ME_PROFILE)
        assertEquals("/api/v1/me/permissions", ProfileApiPaths.ME_PERMISSIONS)
    }

    @Test
    fun get_me_profile_uses_get_and_decodes_snake_case_legacy_body() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> PROFILE_LEGACY_JSON }
        val data = MeProfileApi(fake).getProfile(storeId = " 7 ")
        val call = fake.calls.single()
        assertEquals("GET", call.verb)
        assertEquals("/api/v1/me/profile", call.path)
        assertEquals(mapOf("store_id" to "7"), call.query)
        assertEquals("u-1", data.resolvedId)
        assertEquals("zhangsan", data.resolvedUserName)
        assertEquals("a@b.com", data.email)
        assertEquals(1, data.status)
        assertEquals("张三", data.displayName)
        assertEquals("https://cdn/a.png", data.avatarUrl)
        assertEquals(7, data.resolvedStats.resolvedStoreId)
        assertEquals("华东一店", data.resolvedStats.resolvedStoreName)
    }

    @Test
    fun patch_me_profile_uses_patch_and_sends_sparse_snake_case_body() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> PROFILE_LEGACY_JSON }
        val data = MeProfileApi(fake).updateProfile(
            MeProfilePatchBody(userName = "新名字", displayName = "新名字"),
        )
        val call = fake.calls.single()
        assertEquals("PATCH", call.verb)
        assertEquals("/api/v1/me/profile", call.path)
        val body = NetworkJson.parseToJsonElement(call.body) as JsonObject
        // 稀疏 PATCH：只发非 null 键，且全为 Go UpdateProfileInput 的 snake_case wire 名。
        assertEquals(setOf("user_name", "display_name"), body.keys)
        assertEquals("新名字", body.text("user_name"))
        assertEquals("新名字", body.text("display_name"))
        assertEquals("u-1", data.resolvedId)
    }

    @Test
    fun get_permissions_uses_get_and_decodes_envelope_permissions() = runBlocking {
        val fake = FakeAuthApiClient { _, _, _, _ -> PERMISSIONS_JSON }
        val data = MeProfileApi(fake).getPermissions(storeId = 7)
        val call = fake.calls.single()
        assertEquals("GET", call.verb)
        assertEquals("/api/v1/me/permissions", call.path)
        assertEquals(mapOf("store_id" to "7"), call.query)
        assertEquals(listOf("store:read", "order:write"), data.permissions)
    }

    @Test
    fun me_profile_queries_omit_blank_store_id() {
        assertEquals(mapOf("store_id" to "7"), meProfileQuery(" 7 "))
        assertEquals(emptyMap(), meProfileQuery("   "))
        assertEquals(emptyMap(), meProfileQuery(null))
        assertEquals(mapOf("store_id" to "7"), mePermissionsQuery(7))
        assertEquals(emptyMap(), mePermissionsQuery(0))
        assertEquals(emptyMap(), mePermissionsQuery(null))
    }

    companion object {
        /** Go `response.FromProfileLegacy`（snake_case 直出，非信封）。 */
        private val PROFILE_LEGACY_JSON = """
            {"id":"u-1","user_id":"u-1","user_name":"zhangsan","email":"a@b.com","status":1,
             "display_name":"张三","avatar_url":"https://cdn/a.png","phone":"13800138000",
             "stats":{"store_id":7,"store_name":"华东一店","days_joined":12,"employee_count":30,
                      "store_days":100,"total_customers":250,"role":2,"role_label":"总经理"}}
        """.trimIndent()

        /** Go `response.Success(c, gin.H{"permissions": codes})` 包成的标准信封。 */
        private val PERMISSIONS_JSON = """
            {"code":0,"message":"success","data":{"permissions":["store:read","order:write"]}}
        """.trimIndent()
    }
}
