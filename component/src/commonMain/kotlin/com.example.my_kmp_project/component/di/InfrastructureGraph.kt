package com.example.my_kmp_project.component.di

import com.example.my_kmp_project.component.realtime.AppRealtimeClient
import com.example.my_kmp_project.component.realtime.RealtimeStartupGate
import com.example.my_kmp_project.core.storage.KeyValueStore

/**
 * 存储绑定（DI 第三批）：所有持久化都必须经由命名 KV 视图取得，
 * 装配层不再散落 `createKeyValueStore(...)` 调用。
 */
public interface StorageProvider {
    /** 同一 [name] 的多个实例指向同一份持久化数据（见 core.storage 契约）。 */
    public fun keyValueStore(name: String): KeyValueStore
}

/**
 * Realtime 绑定（DI 第三批）：客户端与启动闸门由 AppGraph 提供，
 * [com.example.my_kmp_project.app.RealtimeRuntime] 只负责「何时首连 / 何时补连」。
 */
public interface RealtimeGraph {
    public val client: AppRealtimeClient
    public val startupGate: RealtimeStartupGate
}
