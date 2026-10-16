@file:OptIn(ExperimentalForeignApi::class)

package com.example.my_kmp_project.core.storage

import cnames.structs.OH_Preferences
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.ArkData.Preferences.OH_PreferencesOption_Create
import platform.ArkData.Preferences.OH_PreferencesOption_Destroy
import platform.ArkData.Preferences.OH_PreferencesOption_SetFileName
import platform.ArkData.Preferences.OH_Preferences_Delete
import platform.ArkData.Preferences.OH_Preferences_FreeString
import platform.ArkData.Preferences.OH_Preferences_GetBool
import platform.ArkData.Preferences.OH_Preferences_GetString
import platform.ArkData.Preferences.OH_Preferences_Open
import platform.ArkData.Preferences.OH_Preferences_SetBool
import platform.ArkData.Preferences.OH_Preferences_SetString

/**
 * OHOS actual, shared by ohosArm64 and ohosX64: the Harmony `OH_Preferences` NDK API
 * (`libohpreferences`; the linker option comes from the platform klib itself).
 *
 * Preference files live in the app sandbox data dir, so this is the M8.5 fix -- session and
 * privacy-consent state survive a reboot instead of being written to `/tmp` and vanishing.
 * Both architectures use the same backend on purpose: an arm64-only CPF klib would leave the
 * x64 emulator on a different persistence story than the device it is meant to predict.
 *
 * Bundle name and data group id are left unset: inside the app sandbox the system derives
 * both from the calling process, the only context this module is ever loaded in.
 */
public actual fun createKeyValueStore(name: String): KeyValueStore =
    PreferencesKeyValueStore(ohosPreferencesHandle(name))

/** `OH_Preferences_ErrCode.PREFERENCES_OK`. */
private const val PREFERENCES_OK_CODE = 0

/**
 * Handles are process-scoped and opened once per store name: `OH_Preferences_Open` allocates,
 * while callers create stores freely (`remember {}` blocks, lazy delegates). They are never
 * closed -- releasing a name would break every other live view of the same data.
 */
private val openedHandles = mutableMapOf<String, CPointer<OH_Preferences>>()

private fun ohosPreferencesHandle(name: String): CPointer<OH_Preferences> {
    openedHandles[name]?.let { return it }

    val option = OH_PreferencesOption_Create()
        ?: error("OH_PreferencesOption_Create failed for store '$name'")
    val handle = try {
        val setName = OH_PreferencesOption_SetFileName(option, name)
        if (setName != PREFERENCES_OK_CODE) {
            error("OH_PreferencesOption_SetFileName('$name') failed with code $setName")
        }
        memScoped {
            val errCode = alloc<IntVar>()
            val opened = OH_Preferences_Open(option, errCode.ptr)
            if (opened == null || errCode.value != PREFERENCES_OK_CODE) {
                error("OH_Preferences_Open('$name') failed with code ${errCode.value}")
            }
            opened
        }
    } finally {
        OH_PreferencesOption_Destroy(option)
    }

    openedHandles[name] = handle
    return handle
}

private class PreferencesKeyValueStore(
    private val handle: CPointer<OH_Preferences>,
) : KeyValueStore {
    override fun getString(key: String): String? = memScoped {
        val valueRef = alloc<CPointerVar<ByteVar>>()
        val lengthRef = alloc<UIntVar>()
        val code = OH_Preferences_GetString(handle, key, valueRef.ptr, lengthRef.ptr)
        try {
            if (code != PREFERENCES_OK_CODE) null else valueRef.value?.toKString()
        } finally {
            // The C side allocated this buffer; it must be released once copied out.
            val released = valueRef.value
            if (released != null) OH_Preferences_FreeString(released)
        }
    }

    override fun putString(key: String, value: String) {
        OH_Preferences_SetString(handle, key, value)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = memScoped {
        val valueRef = alloc<BooleanVar>()
        val code = OH_Preferences_GetBool(handle, key, valueRef.ptr)
        if (code != PREFERENCES_OK_CODE) defaultValue else valueRef.value
    }

    override fun putBoolean(key: String, value: Boolean) {
        OH_Preferences_SetBool(handle, key, value)
    }

    override fun getStringList(key: String): List<String>? =
        StringListValues.decode(getString(key))

    override fun putStringList(key: String, values: List<String>) {
        putString(key, StringListValues.encode(values))
    }

    override fun remove(key: String) {
        OH_Preferences_Delete(handle, key)
    }
}
