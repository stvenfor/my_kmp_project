package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.platform.loadString
import com.example.my_kmp_project.core.platform.loadStringList
import com.example.my_kmp_project.core.platform.saveString
import com.example.my_kmp_project.core.platform.saveStringList

/**
 * Mine root store selection + function order + settings theme/locale prefs
 * (Flutter CurrentStoreService / MineFunctionRepository / SettingsViewModel).
 */
internal object MinePrefsStore {
    const val SelectedStoreIdKey = "current_store.selected_store_id"
    const val FunctionOrderKey = "mine_function_order_ids"
    const val DarkModeKey = "settings.dark_mode"
    const val LocaleZhKey = "settings.locale_zh"

    var version by mutableIntStateOf(0)
        private set

    private fun bump() {
        version++
    }

    var selectedStoreId by mutableStateOf(loadSelectedStoreId())
        private set

    // Explicit MutableState avoids JVM clash with setDarkMode / setLocaleZh.
    private val darkModeState = mutableStateOf(loadString(DarkModeKey) == "true")
    val darkMode: Boolean get() = darkModeState.value

    private val localeZhState = mutableStateOf(loadString(LocaleZhKey) != "false")
    val localeZh: Boolean get() = localeZhState.value

    fun loadSelectedStoreId(): String =
        loadString(SelectedStoreIdKey)?.takeIf { it.isNotBlank() }
            ?: MineStoreCatalog.defaultStoreId

    fun switchStore(id: String): MineStoreOption? {
        val store = MineStoreCatalog.stores.firstOrNull { it.id == id } ?: return null
        selectedStoreId = store.id
        saveString(SelectedStoreIdKey, store.id)
        bump()
        return store
    }

    fun storeName(): String = MineStoreCatalog.resolveName(selectedStoreId)

    /** Per-store demo stats (Flutter MineRepository after switch). */
    fun statsForStore(storeId: String): List<MineStat> = when (storeId) {
        "2" -> listOf(
            MineStat("860", "加入天数"),
            MineStat("16", "员工数"),
            MineStat("1402", "店铺天数"),
            MineStat("5120", "累计客户"),
        )
        else -> MineCatalog.demoStats
    }

    fun functionOrderIds(): List<String> {
        val saved = loadStringList(FunctionOrderKey)
        val catalogIds = MineCatalog.functions.map { it.id }
        if (saved.isNullOrEmpty()) return catalogIds
        val known = saved.filter { it in catalogIds }
        val missing = catalogIds.filter { it !in known }
        return known + missing
    }

    fun reorderFunctions(orderedIds: List<String>) {
        val catalogIds = MineCatalog.functions.map { it.id }.toSet()
        val cleaned = orderedIds.filter { it in catalogIds }
        saveStringList(FunctionOrderKey, cleaned)
        bump()
    }

    fun orderedFunctions(): List<MineFunctionEntry> {
        val byId = MineCatalog.functions.associateBy { it.id }
        return functionOrderIds().mapNotNull { byId[it] }
    }

    fun setDarkMode(enabled: Boolean) {
        darkModeState.value = enabled
        saveString(DarkModeKey, if (enabled) "true" else "false")
        bump()
    }

    fun setLocaleZh(zh: Boolean) {
        localeZhState.value = zh
        saveString(LocaleZhKey, if (zh) "true" else "false")
        bump()
    }
}
