package com.example.my_kmp_project.feature.chat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * IM 契约族（契约表 §2.18）共用的**宽松取值**工具，逐条对齐 Dart 的
 * `(json['a'] ?? json['b'] ?? '').toString()` 读法：
 * - 键缺失 / `null` → 继续尝试下一个别名键（Dart `??` 语义）；
 * - 数字 / 布尔 → 取其字面量内容（Dart `.toString()`）；
 * - 对象 / 数组 → 返回其 JSON 文本（Dart 里等价于 `Map.toString()`，仅作兜底）。
 *
 * 契约表 §2.18 要求 `user_id|im_user_id`、`display_name`、`avatar_url` 的
 * snake_case / camelCase 别名在解码时都被容忍，故统一走本文件，而不是 `@Serializable`
 * 严格字段映射。
 *
 * 可见性 `internal`：仅 `feature/chat` lane 使用；`feature/friend` 因不得跨 feature import，
 * 在 `feature/friend/ImFriendApi.kt` 内自带一份等价私有实现。
 */
internal fun JsonObject.imJsonStr(vararg keys: String): String {
    for (key in keys) {
        val value = imJsonText(this[key]) ?: continue
        return value
    }
    return ""
}

private fun imJsonText(el: JsonElement?): String? = when (el) {
    null, is JsonNull -> null
    is JsonPrimitive -> el.contentOrNull ?: el.toString()
    else -> el.toString()
}

/**
 * Dart `ImSessionResult._asInt`（im_session_api.dart:27-32）：
 * `int` 直接用、其它 `num` 截断为整、字符串只接受**整数**字面量（`int.tryParse`）。
 */
internal fun JsonObject.imJsonInt(key: String): Int? {
    val el = this[key]
    if (el is JsonNull || el !is JsonPrimitive) return null
    el.intOrNull?.let { return it }
    el.doubleOrNull?.let { return it.toInt() }
    return el.contentOrNull?.toIntOrNull()
}

/**
 * Dart `(data['items'] as List? ?? const []).whereType<Map>()`
 * （im_user_profile_api.dart:48）：`items` 缺失 / 非数组 → 空表；数组里的非对象项跳过。
 */
internal fun JsonObject.imJsonItems(key: String = "items"): List<JsonObject> {
    val arr = this[key] as? JsonArray ?: return emptyList()
    return arr.filterIsInstance<JsonObject>()
}
