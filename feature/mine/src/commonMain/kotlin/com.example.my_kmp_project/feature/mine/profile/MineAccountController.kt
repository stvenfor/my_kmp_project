package com.example.my_kmp_project.feature.mine.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.network.ApiClient

/**
 * 「我的」根页统计 / 门店切换 / 个人资料编辑共用的页面级状态。
 *
 * 数据全部来自 [MineProfileApi]；失败只写 [errorMessage]，
 * 不回落任何本地假数据（访客态由调用方自行决定展示占位）。
 */
public class MineAccountController(api: ApiClient) {
    private val profileApi = MineProfileApi(api)

    public var profile by mutableStateOf<MineProfileData?>(null)
        private set
    public var stores by mutableStateOf<List<MineStoreRow>>(emptyList())
        private set
    public var loading by mutableStateOf(false)
        private set
    public var storesLoading by mutableStateOf(false)
        private set
    public var switching by mutableStateOf(false)
        private set
    public var saving by mutableStateOf(false)
        private set
    public var errorMessage by mutableStateOf<String?>(null)
        private set

    public val stats: MineStoreStats? get() = profile?.stats

    public suspend fun loadProfile() {
        loading = true
        errorMessage = null
        try {
            profile = profileApi.fetchProfile()
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载资料失败"
        } finally {
            loading = false
        }
    }

    public suspend fun loadStores() {
        storesLoading = true
        try {
            stores = profileApi.fetchStores().rows
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载门店失败"
        } finally {
            storesLoading = false
        }
    }

    public suspend fun loadAll() {
        loadProfile()
        loadStores()
    }

    public suspend fun saveDisplayName(name: String): Result<MineProfileData> {
        saving = true
        return try {
            val updated = profileApi.updateDisplayName(name)
            profile = updated
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "保存失败"))
        } finally {
            saving = false
        }
    }

    public suspend fun switchStore(storeId: Int): Result<MineStoreStats> {
        switching = true
        errorMessage = null
        return try {
            val stats = profileApi.switchStore(storeId)
            profile = profile?.copy(stats = stats) ?: profileApi.fetchProfile()
            stores = stores.map { it.copy(isCurrent = it.storeId == storeId) }
            Result.success(stats)
        } catch (e: Exception) {
            errorMessage = e.message ?: "切换门店失败"
            Result.failure(IllegalStateException(e.message ?: "切换门店失败"))
        } finally {
            switching = false
        }
    }

    public fun clearError() {
        errorMessage = null
    }
}
