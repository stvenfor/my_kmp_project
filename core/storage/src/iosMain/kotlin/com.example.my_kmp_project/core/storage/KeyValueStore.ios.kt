package com.example.my_kmp_project.core.storage

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

/**
 * iOS actual: one NSUserDefaults suite per store name.
 *
 * Suite-per-name matches the Android SharedPreferences file and the OHOS preference file, so
 * two stores can never collide on a key. Before this module both stores shared
 * standardUserDefaults; the one-time consequence on a development simulator is that old
 * values are not read from the old suite (and list values changed encoding anyway).
 */
public actual fun createKeyValueStore(name: String): KeyValueStore =
    SettingsKeyValueStore(NSUserDefaultsSettings(NSUserDefaults(suiteName = name)))
