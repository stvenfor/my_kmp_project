package com.example.my_kmp_project.feature.home.api

/**
 * home 契约层的日期/时间读法（`composeApp` 无 `kotlinx-datetime` 依赖，
 * 故 [java.time] 在 commonMain 不可用）。
 *
 * Dart 侧这些字段是 `DateTime?`（`home_todo_models.dart:174-179` 的 `_parseDateTime`：
 * `null` / 空串 / `DateTime.tryParse` 解析失败 → `null`）。KMP 保留 **ISO 串本身**，
 * 只把「解析不出来的值」归一为 null；格式化留给 UI lane（`formatTodoDate` /
 * `formatTodoDateTime` 的等价物不属于契约层）。
 */

/**
 * Dart `DateTime.tryParse` 对等：形如 `2026-10-16`、`2026-10-16T09:30`、
 * `2026-10-16T09:30:05.123`、`2026-10-16T09:30:05+08:00`、`...Z` → 原串（trim 后）；
 * `null` / 空白 / 其他 → null。
 */
internal fun homeIsoDateTimeOrNull(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    return if (HOME_ISO_DATE_TIME.matches(text)) text else null
}

/** `yyyy-MM-dd` + 可选时间/时区（容忍 `T` 与空格分隔，容忍缺秒与小数秒）。 */
private val HOME_ISO_DATE_TIME: Regex = Regex(
    "^\\d{4}-\\d{2}-\\d{2}([T ]\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?)?(Z|[+-]\\d{2}:?\\d{2})?$",
)

/**
 * Dart `dateRaw is String ? dateRaw.split('T').first : '$dateRaw'`
 * （`after_sales_models.dart:63-66`，契约表 #50）。
 *
 * bug-for-bug：JSON `null` 在 Dart 里落到 `'$null'` → 字符串 `"null"`（不是空串），
 * 非字符串（数字时间戳）也走同一插值；KMP 的 `isLenient` 解析已把数字标量读成其字面量，
 * 因此这里只需保留「按 `T` 截断」与 `null → "null"` 两点。
 */
internal fun homeDatePart(raw: String?): String = raw?.substringBefore("T") ?: "null"
