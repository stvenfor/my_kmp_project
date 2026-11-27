package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.ApiResponse
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.patchApi
import com.example.my_kmp_project.core.network.postApi
import kotlinx.coroutines.CancellationException

/**
 * Flutter `UserProfileApi`（`features/auth/lib/api/user_profile_api.dart`，d7876a1）的 KMP 对等：
 * 契约表 **§2.2 #8–#11** 的纯 HTTP 层。
 *
 * 身份只认会话头：请求里**不再传** `user_id`（Dart 同注释 `user_profile_api.dart:7`）。
 * 四个出口都经 [NetworkFacade.api]（带 401 刷新装饰），与 Dart 走全局
 * `AuthTokenRefreshInterceptor` 一致；本对象不落库、不改会话。
 */
public class UserProfileHttpApi(
    private val client: ApiClient,
) {

    /**
     * 契约表 #8 `GET /profiles/me`（`user_profile_api.dart:12-24`）。
     *
     * [storeId] 非空时按该经销商**重算** `data.stats`（契约表 §5.5）；
     * 空白/ null 则省略 query key（[profileMeQuery]）。
     */
    suspend fun fetchMe(storeId: String? = null): Result<UserProfileData> = guard {
        val response = client
            .getApi<UserProfileData>(
                path = ProfileApiPaths.ME,
                query = profileMeQuery(storeId),
            )
        requireData(response)
    }

    /**
     * 契约表 #11 `GET /me/stores`（`user_profile_api.dart:26-39`）——
     * **path 前缀是 `/api/v1/me/`，不是 `/api/v1/profiles/`**，别写错。
     *
     * `data.current_store_id` 非 0 时服务端说了算，调用方需覆盖本地持久化的
     * 已选门店（契约表 §5.5）。
     */
    suspend fun listMyStores(): Result<UserStoreListData> = guard {
        val response = client.getApi<UserStoreListData>(ProfileApiPaths.MY_STORES)
        requireData(response)
    }

    /**
     * 契约表 #10 `POST /profiles/me/store`（`user_profile_api.dart:41-55`）：
     * `{store_id:int}`，返回**切换后的 `UserStoreStats`**（不是布尔成功位）。
     */
    suspend fun switchStore(storeId: Int): Result<UserStoreStatsData> = guard {
        val response = client
            .postApi<UserStoreSwitchBody, UserStoreStatsData>(
                path = ProfileApiPaths.ME_STORE,
                body = UserStoreSwitchBody(storeId = storeId),
            )
        requireData(response)
    }

    /**
     * 契约表 #9 `PATCH /profiles/me`（`user_profile_api.dart:57-66`）——
     * **auth 模块唯一的 PATCH**；稀疏 body 的 omit 规则见 [UserProfileUpdateBody.of]。
     *
     * `10001` → 「资料参数无效」（头像 base64/mime/过大），保留会话、只提示
     * （Dart `UnknownAuthFailure`，`user_profile_api.dart:97-102`）。
     */
    suspend fun updateMe(request: UserProfileUpdateBody): Result<UserProfileData> = guard {
        val response = client
            .patchApi<UserProfileUpdateBody, UserProfileData>(
                path = ProfileApiPaths.ME,
                body = request,
            )
        requireData(response)
    }

    /** Dart `UserProfileApi._requireData`（:81-86）：非成功码或 `data` 缺失即失败。 */
    private fun <T> requireData(response: ApiResponse<T>): Result<T> {
        val data = response.data
        return if (isAuthBusinessSuccess(response.code) && data != null) {
            Result.success(data)
        } else {
            Result.failure(profileFailure(response))
        }
    }

    private fun profileFailure(response: ApiResponse<*>): AuthHttpFailure =
        AuthHttpFailure(
            message = mapProfileFailure(response.code, response.message, response.httpStatus),
            code = response.code,
            statusCode = response.httpStatus,
        )

    /** Dart `UserProfileApi._guarded`（:68-78）：`AuthFailure` 透传，其余映射成中文文案。 */
    private suspend inline fun <T> guard(
        block: () -> Result<T>,
    ): Result<T> = try {
        block()
    } catch (e: AuthHttpFailure) {
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: NetworkError.Transport) {
        Result.failure(
            AuthHttpFailure(
                message = mapProfileFailure(null, e.message, e.httpStatus),
                statusCode = e.httpStatus,
            ),
        )
    } catch (e: Throwable) {
        Result.failure(AuthHttpFailure(message = mapProfileFailure(null, e.message, null)))
    }
}
