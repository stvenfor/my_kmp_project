#include "libkn_api.h"
#include "napi/native_api.h"
#include "hilog/log.h"
#include <rawfile/raw_file_manager.h>

#ifndef LOG_DOMAIN
#define LOG_DOMAIN 0x0000
#endif
#ifndef LOG_TAG
#define LOG_TAG "DemoNapi"
#endif

// androidx_compose_ui_arkui_init is declared in libkn_api.h as (void*, void*).
static void CallComposeArkUiInit(napi_env env, napi_value exports) {
    androidx_compose_ui_arkui_init(static_cast<void*>(env), static_cast<void*>(exports));
    OH_LOG_INFO(LOG_APP, "Compose ArkUI init ok");
}

static napi_value NapiMainArkUIViewController(napi_env env, napi_callback_info info) {
    OH_LOG_INFO(LOG_APP, "NapiMainArkUIViewController enter");
    // Kotlin/Native CAdapter exports opaque void* for napi_env / napi_value.
    napi_value result = reinterpret_cast<napi_value>(
        MainArkUIViewController(static_cast<void*>(env)));
    if (result == nullptr) {
        OH_LOG_ERROR(LOG_APP, "MainArkUIViewController returned null (Kotlin failed; hilog DemoKN)");
        napi_throw_error(
            env,
            "DemoKN",
            "MainArkUIViewController returned null — Kotlin failed before controller creation. "
            "Filter hilog tag DemoKN for stack trace.");
        return nullptr;
    }
    OH_LOG_INFO(LOG_APP, "MainArkUIViewController ok");
    return result;
}

static napi_value AudioOnPageHide(napi_env env, napi_callback_info info) {
    KnAudioOnPageHide();
    return nullptr;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    OH_LOG_INFO(LOG_APP, "libentry Init: register exports then Compose ArkUI bootstrap");
    // Register named exports first so ArkTS import succeeds even if Compose init fails.
    napi_property_descriptor desc[] = {
        {"MainArkUIViewController", nullptr, NapiMainArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AudioOnPageHide", nullptr, AudioOnPageHide, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    CallComposeArkUiInit(env, exports);
    return exports;
}
EXTERN_C_END

static napi_module demoModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = ((void*)0),
    .reserved = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void)
{
    napi_module_register(&demoModule);
}
