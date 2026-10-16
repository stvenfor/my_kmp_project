package com.example.my_kmp_project.core.storage

/**
 * The project's only key-value persistence contract.
 *
 * Anything that must survive a process relaunch goes through this interface: session
 * tokens, privacy consent, favorites, IM session ids. Call sites MUST NOT touch
 * SharedPreferences / NSUserDefaults / OHOS Preferences directly.
 *
 * Value slots are deliberately narrow -- String, Boolean, List<String> -- so the platform
 * backends stay behaviourally identical and testable from commonMain. There is no
 * whole-store clear() and no contains(): nothing in the app needs them, and the OHOS
 * equivalents only exist from API 23 up. Delete a key with [remove].
 *
 * Backends (wired in core/storage/build.gradle.kts):
 *  - Android: SharedPreferencesSettings (multiplatform-settings)
 *  - iOS: NSUserDefaultsSettings(suiteName = name) (same library)
 *  - OHOS arm64 + x64: PreferencesKeyValueStore over the Harmony OH_Preferences NDK API --
 *    the app sandbox data dir, so state is durable. Neither multiplatform-settings line can
 *    cover all six targets (stock publishes no OHOS klib; the CPF line publishes no Android
 *    AAR), so OHOS implements this interface directly rather than splitting the story per
 *    architecture.
 *
 * A store is a cheap *view* over named data, not an owning resource: call
 * [createKeyValueStore] freely -- instances built from the same name address the same
 * persisted data.
 */
public interface KeyValueStore {
    /** Stored string, or null when the key was never written (or was removed). */
    public fun getString(key: String): String?

    /** Write [value]. An empty string is real data here; use [remove] to delete a key. */
    public fun putString(key: String, value: String)

    /**
     * Stored boolean, or [defaultValue] when absent. Reading a key that holds another type
     * is not defined behaviour (same rule as the underlying Settings contract).
     */
    public fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    public fun putBoolean(key: String, value: Boolean)

    /**
     * Stored list, or null when the key was never written. A stored empty list reads back as
     * an empty list, not null: callers use null to mean "no user choice yet".
     */
    public fun getStringList(key: String): List<String>?

    public fun putStringList(key: String, values: List<String>)

    public fun remove(key: String)
}

/**
 * Platform factory for a named store. Distinct [name]s address distinct persistent data --
 * do not share one name across unrelated concerns.
 */
public expect fun createKeyValueStore(name: String): KeyValueStore
