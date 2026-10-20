package com.example.my_kmp_project.core.database

/**
 * 依赖注入缝：上层统一经这里拿 [Database]，不直接 new 具体 driver。
 *
 * 当前默认返回 [InMemoryDatabase]（OHOS fork 暂无 SQL klib）。接入平台 SQL driver 时，
 * 只改这一个工厂（或在此挂一个 driver 注册表），业务层与接口零改动。
 */
object DatabaseProvider {

    private var factory: (String) -> Database = { name -> InMemoryDatabase(name) }

    /** 取一个逻辑库实例；同一 [name] 建议复用同一 driver。 */
    fun create(name: String = "app"): Database = factory(name)

    /** 替换默认 driver 工厂（例如接入平台 SQL / 后续落盘实现）。 */
    fun setFactory(factory: (String) -> Database) {
        this.factory = factory
    }
}
