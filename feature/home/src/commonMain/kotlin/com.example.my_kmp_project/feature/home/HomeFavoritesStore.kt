package com.example.my_kmp_project.feature.home

/**
 * 常用服务收藏（全部服务页顶部）。
 *
 * 数据源改为服务端：`GET /api/v1/home/catalog` 的 favorite_ids 为准，
 * 点按增删走 `POST/DELETE /api/v1/home/favorites`；本地只保留 3~8 的交互上下限。
 */
class HomeFavoritesStore(
    private val content: HomeContentStore,
) {
    companion object {
        const val MinFavorites = 3
        const val MaxFavorites = 8
    }

    /** 当前收藏 service_id 快照。 */
    fun snapshot(): Set<String> = content.favoriteIds()

    fun canRemove(): Boolean = content.canRemoveFavorite()

    fun canAdd(): Boolean = content.canAddFavorite()

    /** 取消收藏；以服务端返回结果为准。 */
    suspend fun remove(id: String): Boolean {
        if (!canRemove() || id !in content.favoriteIds()) return false
        return content.setFavorite(id, add = false)
    }

    /** 新增收藏；以服务端返回结果为准。 */
    suspend fun add(id: String): Boolean {
        if (!canAdd() || id in content.favoriteIds()) return false
        return content.setFavorite(id, add = true)
    }

    /** 常用服务展示项（按收藏顺序，取不到资源时由 UI 兜底首字）。 */
    fun favoriteItems(): List<AllServiceItem> = content.favoriteItems()
}
