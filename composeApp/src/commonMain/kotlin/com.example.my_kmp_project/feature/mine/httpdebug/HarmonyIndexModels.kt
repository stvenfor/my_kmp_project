package com.example.my_kmp_project.feature.mine.httpdebug

import com.example.my_kmp_project.core.network.ListPage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Mine「HTTP 调试页」展示模型 —— Dart `HarmonyIndexModel` / `HarmonySectionModel` /
 * `HarmonyArticleModel`（`features/settings/lib/mine/model/harmony_index_model.dart`，d7876a1）。
 *
 * 契约表 #14：数据来自 `GET /api/v1/transactions` 的 `page`/`size` 方言，
 * 由 [MineHttpTestApi] 取回后交 [harmonyIndexOf] 映射。
 *
 * Dart 侧的 `fromJson` / `fromTransactions` 两个工厂在 d7876a1 已无调用点
 * （调试页只走 `fromListData`），因此不移植，避免留下无人消费的解析分支。
 */
public data class HarmonyIndexModel(
    val links: HarmonySectionModel? = null,
    val openSources: HarmonySectionModel? = null,
    val tools: HarmonySectionModel? = null,
) {
    /** Dart `allSections`：按 links、openSources、tools 顺序收非空段。 */
    public val allSections: List<HarmonySectionModel>
        get() = listOfNotNull(links, openSources, tools)

    /** Dart `totalArticleCount`：所有段的文章数之和。 */
    public val totalArticleCount: Int
        get() = allSections.sumOf { it.articleCount }
}

public data class HarmonySectionModel(
    val name: String? = null,
    val desc: String? = null,
    val articleList: List<HarmonyArticleModel> = emptyList(),
) {
    /** Dart `articleCount`（Dart 里 `articleList` 可为 null，这里默认空表，语义等价）。 */
    public val articleCount: Int get() = articleList.size
}

/**
 * Dart `HarmonyArticleModel.fromJson` 里 `id` 是 `num?`（直接塞进 dynamic），
 * 而调试页从不展示 `id`；KMP 不用 `Any`，与 title/desc 同走 [stringifyOrNothing]
 * 把 JSON 标量归一成字符串（数字 7 → "7"）。
 */
public data class HarmonyArticleModel(
    val id: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val link: String? = null,
    val chapterName: String? = null,
    val niceDate: String? = null,
    val author: String? = null,
)

/**
 * Dart `HarmonyIndexModel.fromListData`（harmony_index_model.dart:25-52）：
 * 整页 transactions 折成**一个**名为 `transactions` 的 section，
 * 每行 → `title = "category · amount"`、`desc = note`、`niceDate = date`、`author = type`。
 *
 * 入参行是原样透传的 `JsonObject`（Dart 转换器就是 identity `(item) => item`），
 * 由 core 的 `decodeListPage` 完成 `list` / `items` / 裸数组三方言归一。
 *
 * 逐字段兜底与 Dart 完全一致：`category` 缺失/null → `未分类`，`amount` 缺失/null → `-`
 * （注意 `amount` 为 0 时 Dart 得到 "0" 而不是 "-"，所以这里只在 null 上兜底），
 * `date` 缺失/null → 空串，`note`/`type` 缺失/null → null。
 */
public fun harmonyIndexOf(rows: ListPage<JsonObject>): HarmonyIndexModel =
    HarmonyIndexModel(
        links = HarmonySectionModel(
            name = SECTION_NAME,
            desc = SECTION_DESC,
            articleList = rows.items.map { item ->
                val category = stringifyOrNothing(item["category"]) ?: DEFAULT_CATEGORY
                val amount = stringifyOrNothing(item["amount"]) ?: AMOUNT_PLACEHOLDER
                HarmonyArticleModel(
                    id = stringifyOrNothing(item["id"]),
                    title = "$category · $amount",
                    desc = stringifyOrNothing(item["note"]),
                    niceDate = stringifyOrNothing(item["date"]) ?: "",
                    author = stringifyOrNothing(item["type"]),
                )
            },
        ),
    )

/** Dart `?.toString()`：null/JsonNull → null，字符串取原值，数字/布尔取其字面文本。 */
private fun stringifyOrNothing(el: JsonElement?): String? = when (el) {
    null, JsonNull -> null
    is JsonPrimitive -> el.content
    else -> el.toString()
}

private const val SECTION_NAME: String = "transactions"
private const val SECTION_DESC: String = "my_go_study /api/v1/transactions"
private const val DEFAULT_CATEGORY: String = "未分类"
private const val AMOUNT_PLACEHOLDER: String = "-"
