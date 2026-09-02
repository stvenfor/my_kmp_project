@file:OptIn(ExperimentalForeignApi::class)

package com.example.my_kmp_project.core.ohos

import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.JsObject
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value

internal fun ohosJsObject(builder: OhosJsBuilder.() -> Unit): JsObject {
    val root = JsEnv.createObject() ?: error("JsEnv.createObject failed")
    OhosJsBuilder(root).builder()
    return JsObject(root)
}

internal class OhosJsBuilder(private val root: napi_value) {
    fun put(key: String, value: String) {
        JsEnv.setProperty(root, JsEnv.createStringUtf8(key), JsEnv.createStringUtf8(value))
    }

    fun put(key: String, value: Boolean) {
        JsEnv.setProperty(root, JsEnv.createStringUtf8(key), JsEnv.createInt32(if (value) 1 else 0))
    }

    fun put(key: String, value: Int) {
        JsEnv.setProperty(root, JsEnv.createStringUtf8(key), JsEnv.createInt32(value))
    }

    fun put(key: String, value: Long) {
        JsEnv.setProperty(root, JsEnv.createStringUtf8(key), JsEnv.createInt64(value))
    }
}
