package com.example.my_kmp_project.feature.home

/**
 * In-memory favorite services for AllServices ↔ Home feature strip.
 * Flutter persists via controller; this keeps session-level edit until KV lands.
 */
internal object HomeFavoritesStore {
    const val MinFavorites = 3
    const val MaxFavorites = 8

    private val defaultIds: Set<String> =
        HomeMockData.favoriteServices.map { it.id }.toSet()

    private var ids: Set<String> = defaultIds

    fun snapshot(): Set<String> = ids

    fun canRemove(): Boolean = ids.size > MinFavorites

    fun canAdd(): Boolean = ids.size < MaxFavorites

    fun remove(id: String): Boolean {
        if (!canRemove() || id !in ids) return false
        ids = ids - id
        return true
    }

    fun add(id: String): Boolean {
        if (!canAdd() || id in ids) return false
        ids = ids + id
        return true
    }

    fun favoriteItems(): List<AllServiceItem> {
        val all = HomeMockData.favoriteServices +
            HomeMockData.catalogSections.flatMap { it.items }
        val byId = all.associateBy { it.id }
        return ids.mapNotNull { byId[it] }.ifEmpty {
            HomeMockData.favoriteServices.take(MinFavorites)
        }
    }
}
