package com.example.my_kmp_project.feature.mine.httpdebug

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import com.example.my_kmp_project.core.network.decodeListPage
import com.example.my_kmp_project.core.network.getApi
import com.example.my_kmp_project.core.network.pageQuery
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Mine「HTTP 调试页」的 transactions 变体 —— Dart `MineApi.fetchHarmonyIndex`
 * （`features/settings/lib/mine/api/mine_api.dart:8-25`，d7876a1）。契约表 #14。
 *
 * 与 home 侧 #12 是**同一个 path、两套查询方言**（契约表 §5 第 8 条）：
 * - #12 `TransactionApi`：`limit=size&offset=page*size`，page **0-based**，且兼容「裸 body 即数据」；
 * - #14 本类：`page=1&size=20`，page **1-based**，只认 `code/message/data` 信封。
 * 因此这里用 [pageQuery] 而不调用 `feature/home` 的 helper（分层门禁也不允许跨 feature import）。
 *
 * 失败语义（保留 Dart 原样，不做「顺手修正」）：
 * - **完全不看业务码**：Dart 只取 `result.data?.data`，`code != 0` 但 `data` 有值时调试页照样渲染；
 *   反过来 `code == 0` 而 `data` 缺失也抛。
 * - `data` 缺失/null → 抛 [NetworkError.Business]（Dart `HttpRequestException`），
 *   文案固定 `接口返回数据为空`，不读 envelope 的 `message`（mine_api.dart:20-21）。
 * - 行不是 object 时被丢弃（Dart `ListData.fromJson` 的 `whereType<Map<String, dynamic>>`）。
 */
public class MineHttpTestApi(
    private val api: ApiClient = NetworkFacade.api(),
) {

    /**
     * #14 `GET /transactions?page=1&size=20` → [HarmonyIndexModel]。
     *
     * 页码与条数在 Dart 里是写死的常量（mine_api.dart:13），这里同样不开参数。
     */
    public suspend fun fetchHarmonyIndex(): HarmonyIndexModel {
        val response = api.getApi<JsonElement>(TRANSACTIONS_PATH, pageQuery(PAGE, SIZE))
        val data: JsonElement = response.data
            ?: throw NetworkError.Business(response.code, EMPTY_DATA_MESSAGE)
        return harmonyIndexOf(decodeListPage<JsonObject>(data))
    }

    public companion object {
        /** Dart `MineHttpConfig.transactionsPath`（mine_http_config.dart:8）。 */
        public const val TRANSACTIONS_PATH: String = "/api/v1/transactions"

        /** Dart 调用点常量 `page: 1, size: 20`（mine_api.dart:13）。 */
        internal const val PAGE: Int = 1
        internal const val SIZE: Int = 20

        /** Dart `HttpRequestException(message: '接口返回数据为空')`（mine_api.dart:21）。 */
        internal const val EMPTY_DATA_MESSAGE: String = "接口返回数据为空"
    }
}
