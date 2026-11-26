package com.example.my_kmp_project.core.database

import com.example.my_kmp_project.core.storage.createKeyValueStore

/**
 * 依赖注入缝：上层统一经这里拿 [Database]，不直接 new 具体 driver。
 *
 * 默认返回 [PersistentDatabase]，底层是 [createKeyValueStore] 提供的三端持久化 KV
 * （Android SharedPreferences / iOS NSUserDefaults / OHOS OH_Preferences），进程重启后数据仍在。
 * 接入平台 SQL driver 时，只改这一个工厂（或在此挂一个 driver 注册表），业务层与接口零改动。
 *
 * Android 侧须先 `AndroidAppContext.install(context)` 再创建库（见 core:storage）。
 */
object DatabaseProvider {

    private var factory: (String) -> Database = { name ->
        PersistentDatabase(createKeyValueStore("db_$name"), name)
    }

    /** 取一个逻辑库实例；同一 [name] 建议复用同一 driver。 */
    fun create(name: String = "app"): Database = factory(name)

    /** 替换默认 driver 工厂（例如接入平台 SQL / 测试用 [InMemoryDatabase]）。 */
    fun setFactory(factory: (String) -> Database) {
        this.factory = factory
    }
}
