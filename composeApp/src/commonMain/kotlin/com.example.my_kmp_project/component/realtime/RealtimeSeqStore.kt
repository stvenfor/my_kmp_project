package com.example.my_kmp_project.component.realtime

import com.example.my_kmp_project.core.network.NetworkJson
import com.example.my_kmp_project.core.network.realtime.RealtimeConfig
import com.example.my_kmp_project.core.network.realtime.realtimeStringOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/**
 * Realtime 侧的键值持久化缝隙 —— Dart `SpManager`（`module_global_cache/prefs`）的最小对等。
 *
 * 只做 [getString] / [setString] 两件事，与 Dart 用到的读写面完全一致：
 * - `realtime_last_seq`（`commons/storage/lib/prefs/sp_keys.dart:4`）
 * - `realtime_notify_dedup_json`（同文件 :5）
 *
 * 为什么用**同步**接口：Dart 的 `SpManager.getString` 本来就是内存快照同步返回，
 * `setString` 虽然声明为 `Future` 但 [RealtimeSeqStore.acceptSeq] 用 `unawaited(save())`
 * 即发即忘（`store/realtime_seq_store.dart:30`），且 `save()` 的字符串实参在挂起前就已求值，
 * 因此「同步写」与 Dart 的可观测顺序等价；测试也不需要等待落盘。
 */
public interface RealtimeKeyValueStore {
    public fun getString(key: String): String?

    public fun setString(key: String, value: String)
}

/** 进程内实现：单测与 ohosX64 占位用（不假装跨进程持久化）。 */
public class InMemoryRealtimeKeyValueStore(
    initial: Map<String, String> = emptyMap(),
) : RealtimeKeyValueStore {
    private val data: MutableMap<String, String> = initial.toMutableMap()

    override fun getString(key: String): String? = data[key]

    override fun setString(key: String, value: String) {
        data[key] = value
    }
}

/** Dart `SpKeys` 里 realtime 相关的两个键名（sp_keys.dart:4-5），字面量不得改动。 */
public object RealtimePrefKeys {
    public const val LAST_SEQ: String = "realtime_last_seq"
    public const val NOTIFY_DEDUP: String = "realtime_notify_dedup_json"
}

/**
 * 全局 seq 水位（去重 + 断线补投起点）——
 * 逐行对齐 Dart `components/realtime/lib/store/realtime_seq_store.dart:9-40`（d7876a1）。
 *
 * 关键语义（都是容易「顺手修好」而破坏等价性的点）：
 * - [lastSeq] 是 **64 位**（Dart `int`），[load] 解析失败一律回 `0`（:16 `int.tryParse(...) ?? 0`）；
 * - [acceptSeq] 对 `null` seq **放行**（:27，控制帧/无 seq 业务帧不参与排序）；
 * - 只有**严格大于**水位才接受（:28 `seq <= _lastSeq ⇒ false`），等号也算重复；
 * - 接受即写盘（Dart `unawaited(save())`）；
 * - [setLastSeq] 单调不回退（:34-39），sync 响应里的 `latestSeq` 用它兜底。
 */
public class RealtimeSeqStore(
    private val kv: RealtimeKeyValueStore,
) {
    private var _lastSeq: Long = 0L

    /** Dart `int get lastSeq`（:12）。 */
    public val lastSeq: Long get() = _lastSeq

    /** Dart `load()`（:14-17）：脏数据 / 空值一律归零。 */
    public fun load() {
        _lastSeq = kv.getString(RealtimePrefKeys.LAST_SEQ)?.toLongOrNull() ?: 0L
    }

    /** Dart `save()`（:19-24）。 */
    public fun save() {
        kv.setString(RealtimePrefKeys.LAST_SEQ, _lastSeq.toString())
    }

    /** Dart `acceptSeq(int? seq)`（:26-32）：`null` 恒接受，否则要求严格递增。 */
    public fun acceptSeq(seq: Long?): Boolean {
        if (seq == null) return true
        if (seq <= _lastSeq) return false
        _lastSeq = seq
        save()
        return true
    }

    /** Dart `setLastSeq(int seq)`（:34-39）：只抬升，不下降，也不做等号写入。 */
    public fun setLastSeq(seq: Long) {
        if (seq > _lastSeq) {
            _lastSeq = seq
            save()
        }
    }
}

/**
 * notifyId 去重环 —— 对齐 Dart `NotifyDedupStore`（`realtime_seq_store.dart:42-71`）。
 *
 * 与 seq 水位**独立**：Dart 注释说明了原因（client:259「sys.notify 仍交给 Handler，
 * notifyId 去重，避免 seq 已更新但 Banner 未展示」）。
 *
 * 细节：
 * - 空 notifyId **恒放行**（:59）；
 * - 命中过 ⇒ 拒绝（:60）；
 * - 新 id 插到**头部**，超过 [RealtimeConfig.NOTIFY_DEDUP_MAX]（=200）从尾部截断（:61-64）；
 * - 解析失败 ⇒ 清空重来（:53-55），不抛。
 */
public class NotifyDedupStore(
    private val kv: RealtimeKeyValueStore,
) {
    private val ids: MutableList<String> = mutableListOf()

    /** 观测口（只读副本；调试页 / 单测用，Dart 侧无）。 */
    public val snapshot: List<String> get() = ids.toList()

    /** Dart `load()`（:45-56）。 */
    public fun load() {
        val raw = kv.getString(RealtimePrefKeys.NOTIFY_DEDUP)
        if (raw.isNullOrEmpty()) return
        val parsed = runCatching {
            (NetworkJson.parseToJsonElement(raw) as? JsonArray)
                ?.mapNotNull { realtimeStringOf(it) }
        }.getOrElse { null }
        if (parsed == null) {
            ids.clear()
            return
        }
        ids.clear()
        ids.addAll(parsed)
    }

    /**
     * Dart `shouldProcess(notifyId)`（:58-70）：判定 + 记账 + 落盘一步完成。
     * 返回 true 表示「这个 notifyId 没见过，该展示」。
     */
    public fun shouldProcess(notifyId: String): Boolean {
        if (notifyId.isEmpty()) return true
        if (ids.contains(notifyId)) return false
        ids.add(0, notifyId)
        if (ids.size > RealtimeConfig.NOTIFY_DEDUP_MAX) {
            repeat(ids.size - RealtimeConfig.NOTIFY_DEDUP_MAX) { ids.removeAt(ids.size - 1) }
        }
        kv.setString(RealtimePrefKeys.NOTIFY_DEDUP, encodeIds())
        return true
    }

    /** Dart `jsonEncode(_ids)`（:67）：紧凑 JSON 数组文本，与 Dart 默认分隔符一致。 */
    private fun encodeIds(): String = NetworkJson.encodeToString(
        JsonElement.serializer(),
        buildJsonArray { ids.forEach { add(JsonPrimitive(it)) } },
    )
}
