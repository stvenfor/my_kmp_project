#include "libkn_api.h"
#include "napi/native_api.h"
#include "hilog/log.h"
#include <rawfile/raw_file_manager.h>
#include <dlfcn.h>

#ifndef LOG_DOMAIN
#define LOG_DOMAIN 0x0000
#endif
#ifndef LOG_TAG
#define LOG_TAG "DemoNapi"
#endif

using ArkUiInitFn = void (*)(napi_env, napi_value);

static void CallComposeArkUiInit(napi_env env, napi_value exports) {
    // Prefer libkn export; fall back to dlsym for CMP version renames.
    ArkUiInitFn initFn = reinterpret_cast<ArkUiInitFn>(
        dlsym(RTLD_DEFAULT, "androidx_compose_ui_arkui_init"));
    if (initFn == nullptr) {
        initFn = reinterpret_cast<ArkUiInitFn>(
            dlsym(RTLD_DEFAULT, "androidx_compose_ui_arkui_utils_init"));
    }
    if (initFn != nullptr) {
        initFn(env, exports);
        OH_LOG_INFO(LOG_APP, "Compose ArkUI init ok");
    } else {
        OH_LOG_ERROR(LOG_APP, "Compose ArkUI init symbol missing");
    }
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
    OH_LOG_INFO(LOG_APP, "libentry Init: Compose ArkUI bootstrap");
    CallComposeArkUiInit(env, exports);
    napi_property_descriptor desc[] = {
        {"MainArkUIViewController", nullptr, NapiMainArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AudioOnPageHide", nullptr, AudioOnPageHide, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
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
