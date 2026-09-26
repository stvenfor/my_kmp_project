package com.example.my_kmp_project.feature.home

import com.example.my_kmp_project.core.platform.loadStringList
import com.example.my_kmp_project.core.platform.saveStringList

/**
 * Favorite services for AllServices — Flutter `AllServicesRepository`
 * key `home_favorite_service_ids_v2` (min 3 / max 8).
 */
internal object HomeFavoritesStore {
    const val MinFavorites = 3
    const val MaxFavorites = 8
    private const val PrefKey = "home_favorite_service_ids_v2"

    private val defaultIds: List<String> =
        HomeMockData.favoriteServices.map { it.id }

    private var ids: LinkedHashSet<String> = LinkedHashSet(loadOrDefault())

    private fun loadOrDefault(): List<String> {
        val stored = loadStringList(PrefKey) ?: return defaultIds
        return normalize(stored)
    }

    private fun normalize(raw: List<String>): List<String> {
        val all = (HomeMockData.favoriteServices + HomeMockData.catalogSections.flatMap { it.items })
            .associateBy { it.id }
        var next = raw.filter { it in all }.distinct()
        if (next.size > MaxFavorites) next = next.take(MaxFavorites)
        if (next.size < MinFavorites) {
            for (candidate in defaultIds) {
                if (next.size >= MinFavorites) break
                if (candidate !in next) next = next + candidate
            }
        }
        if (next.size < MinFavorites) next = defaultIds.take(MinFavorites)
        return next
    }

    private fun persist() {
        saveStringList(PrefKey, ids.toList())
    }

    fun snapshot(): Set<String> = ids.toSet()

    fun canRemove(): Boolean = ids.size > MinFavorites

    fun canAdd(): Boolean = ids.size < MaxFavorites

    fun remove(id: String): Boolean {
        if (!canRemove() || id !in ids) return false
        ids.remove(id)
        persist()
        return true
    }

    fun add(id: String): Boolean {
        if (!canAdd() || id in ids) return false
        ids.add(id)
        persist()
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
