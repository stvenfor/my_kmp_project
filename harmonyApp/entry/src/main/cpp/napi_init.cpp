#include "libkn_api.h"
#include "napi/native_api.h"
#include "hilog/log.h"
#include <rawfile/raw_file_manager.h>
#include <cstring>
#include <string>

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

extern "C" void KnSetOhosHost(int kind, int routeCode);
extern "C" void KnSetOhosSecondaryRoute(void* routePtr);
extern "C" void KnApplyAuthSession(void* tokenPtr, void* userIdPtr, void* displayNamePtr, void* phonePtr);

extern "C" int KnAuthLoginWithPasswordBlocking(void* account, void* password, void* errBuf, int errCap);
extern "C" int KnAuthLoginWithOtpBlocking(void* phone, void* code, void* errBuf, int errCap);
extern "C" int KnAuthSendPhoneOtpBlocking(void* phone, void* errBuf, int errCap);
extern "C" int KnAuthRegisterBlocking(void* email, void* password, void* displayName, void* errBuf, int errCap);
extern "C" int KnAuthLoginWithHuaweiBlocking(void* code, void* deviceId, void* errBuf, int errCap);
extern "C" int KnAuthCopyToken(void* buf, int cap);
extern "C" int KnAuthCopyDisplayName(void* buf, int cap);
extern "C" int KnAuthCopyUserId(void* buf, int cap);
extern "C" int KnAuthCopyPhone(void* buf, int cap);
extern "C" int KnAuthIsLoggedIn(void);
extern "C" void KnAuthLogout(void);

static napi_value NapiSetOhosHost(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr, nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    int32_t kind = 0;
    int32_t routeCode = 0;
    if (argc >= 1 && args[0] != nullptr) {
        napi_get_value_int32(env, args[0], &kind);
    }
    if (argc >= 2 && args[1] != nullptr) {
        napi_get_value_int32(env, args[1], &routeCode);
    }
    KnSetOhosHost(kind, routeCode);
    return nullptr;
}

static napi_value NapiSetOhosSecondaryRoute(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    static const char kDefault[] = "/home/search";
    if (argc < 1 || args[0] == nullptr) {
        KnSetOhosSecondaryRoute(const_cast<char*>(kDefault));
        return nullptr;
    }
    size_t len = 0;
    napi_get_value_string_utf8(env, args[0], nullptr, 0, &len);
    char* buf = new char[len + 1];
    napi_get_value_string_utf8(env, args[0], buf, len + 1, &len);
    buf[len] = '\0';
    KnSetOhosSecondaryRoute(buf);
    delete[] buf;
    return nullptr;
}

static char* ReadUtf8Arg(napi_env env, napi_value value) {
    if (value == nullptr) {
        return nullptr;
    }
    size_t len = 0;
    napi_get_value_string_utf8(env, value, nullptr, 0, &len);
    char* buf = new char[len + 1];
    napi_get_value_string_utf8(env, value, buf, len + 1, &len);
    buf[len] = '\0';
    return buf;
}

static napi_value NapiApplyAuthSession(napi_env env, napi_callback_info info) {
    size_t argc = 4;
    napi_value args[4] = {nullptr, nullptr, nullptr, nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    char* token = argc >= 1 ? ReadUtf8Arg(env, args[0]) : nullptr;
    char* userId = argc >= 2 ? ReadUtf8Arg(env, args[1]) : nullptr;
    char* displayName = argc >= 3 ? ReadUtf8Arg(env, args[2]) : nullptr;
    char* phone = argc >= 4 ? ReadUtf8Arg(env, args[3]) : nullptr;
    KnApplyAuthSession(token, userId, displayName, phone);
    delete[] token;
    delete[] userId;
    delete[] displayName;
    delete[] phone;
    return nullptr;
}

enum class AuthOp {
    LoginPassword,
    LoginOtp,
    SendOtp,
    Register,
    Huawei,
};

struct AuthAsyncCtx {
    AuthOp op = AuthOp::LoginPassword;
    char* a = nullptr;
    char* b = nullptr;
    char* c = nullptr;
    char err[512]{};
    int failed = 0;
    napi_deferred deferred = nullptr;
    napi_async_work work = nullptr;
};

static void AuthExecute(napi_env env, void* data) {
    (void)env;
    auto* ctx = static_cast<AuthAsyncCtx*>(data);
    switch (ctx->op) {
        case AuthOp::LoginPassword:
            ctx->failed = KnAuthLoginWithPasswordBlocking(ctx->a, ctx->b, ctx->err, (int)sizeof(ctx->err));
            break;
        case AuthOp::LoginOtp:
            ctx->failed = KnAuthLoginWithOtpBlocking(ctx->a, ctx->b, ctx->err, (int)sizeof(ctx->err));
            break;
        case AuthOp::SendOtp:
            ctx->failed = KnAuthSendPhoneOtpBlocking(ctx->a, ctx->err, (int)sizeof(ctx->err));
            break;
        case AuthOp::Register:
            ctx->failed = KnAuthRegisterBlocking(ctx->a, ctx->b, ctx->c, ctx->err, (int)sizeof(ctx->err));
            break;
        case AuthOp::Huawei:
            ctx->failed = KnAuthLoginWithHuaweiBlocking(ctx->a, ctx->b, ctx->err, (int)sizeof(ctx->err));
            break;
    }
}

static void AuthComplete(napi_env env, napi_status status, void* data) {
    auto* ctx = static_cast<AuthAsyncCtx*>(data);
    napi_value undefined = nullptr;
    napi_get_undefined(env, &undefined);
    if (status != napi_ok || ctx->failed != 0) {
        napi_value msg = nullptr;
        const char* text = ctx->err[0] != '\0' ? ctx->err : "登录失败，请稍后重试";
        napi_create_string_utf8(env, text, NAPI_AUTO_LENGTH, &msg);
        napi_value err = nullptr;
        napi_create_error(env, nullptr, msg, &err);
        napi_reject_deferred(env, ctx->deferred, err);
    } else {
        napi_resolve_deferred(env, ctx->deferred, undefined);
    }
    delete[] ctx->a;
    delete[] ctx->b;
    delete[] ctx->c;
    napi_delete_async_work(env, ctx->work);
    delete ctx;
}

static napi_value StartAuthPromise(napi_env env, AuthOp op, char* a, char* b, char* c) {
    napi_value promise = nullptr;
    napi_deferred deferred = nullptr;
    napi_create_promise(env, &deferred, &promise);

    auto* ctx = new AuthAsyncCtx();
    ctx->op = op;
    ctx->a = a;
    ctx->b = b;
    ctx->c = c;
    ctx->deferred = deferred;

    napi_value resource = nullptr;
    napi_create_string_utf8(env, "AuthBridge", NAPI_AUTO_LENGTH, &resource);
    napi_create_async_work(env, nullptr, resource, AuthExecute, AuthComplete, ctx, &ctx->work);
    napi_queue_async_work(env, ctx->work);
    return promise;
}

static napi_value NapiAuthLoginWithPassword(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr, nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    return StartAuthPromise(
        env,
        AuthOp::LoginPassword,
        argc >= 1 ? ReadUtf8Arg(env, args[0]) : nullptr,
        argc >= 2 ? ReadUtf8Arg(env, args[1]) : nullptr,
        nullptr);
}

static napi_value NapiAuthLoginWithOtp(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr, nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    return StartAuthPromise(
        env,
        AuthOp::LoginOtp,
        argc >= 1 ? ReadUtf8Arg(env, args[0]) : nullptr,
        argc >= 2 ? ReadUtf8Arg(env, args[1]) : nullptr,
        nullptr);
}

static napi_value NapiAuthSendPhoneOtp(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = {nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    return StartAuthPromise(
        env,
        AuthOp::SendOtp,
        argc >= 1 ? ReadUtf8Arg(env, args[0]) : nullptr,
        nullptr,
        nullptr);
}

static napi_value NapiAuthRegister(napi_env env, napi_callback_info info) {
    size_t argc = 3;
    napi_value args[3] = {nullptr, nullptr, nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    return StartAuthPromise(
        env,
        AuthOp::Register,
        argc >= 1 ? ReadUtf8Arg(env, args[0]) : nullptr,
        argc >= 2 ? ReadUtf8Arg(env, args[1]) : nullptr,
        argc >= 3 ? ReadUtf8Arg(env, args[2]) : nullptr);
}

static napi_value NapiAuthLoginWithHuawei(napi_env env, napi_callback_info info) {
    size_t argc = 2;
    napi_value args[2] = {nullptr, nullptr};
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    return StartAuthPromise(
        env,
        AuthOp::Huawei,
        argc >= 1 ? ReadUtf8Arg(env, args[0]) : nullptr,
        argc >= 2 ? ReadUtf8Arg(env, args[1]) : nullptr,
        nullptr);
}

static napi_value NapiCopyStringGetter(
    napi_env env,
    int (*copyFn)(void*, int)) {
    char buf[2048];
    buf[0] = '\0';
    copyFn(buf, (int)sizeof(buf));
    napi_value out = nullptr;
    napi_create_string_utf8(env, buf, NAPI_AUTO_LENGTH, &out);
    return out;
}

static napi_value NapiAuthToken(napi_env env, napi_callback_info info) {
    (void)info;
    return NapiCopyStringGetter(env, KnAuthCopyToken);
}

static napi_value NapiAuthDisplayName(napi_env env, napi_callback_info info) {
    (void)info;
    return NapiCopyStringGetter(env, KnAuthCopyDisplayName);
}

static napi_value NapiAuthUserId(napi_env env, napi_callback_info info) {
    (void)info;
    return NapiCopyStringGetter(env, KnAuthCopyUserId);
}

static napi_value NapiAuthPhone(napi_env env, napi_callback_info info) {
    (void)info;
    return NapiCopyStringGetter(env, KnAuthCopyPhone);
}

static napi_value NapiAuthIsLoggedIn(napi_env env, napi_callback_info info) {
    (void)info;
    napi_value out = nullptr;
    napi_get_boolean(env, KnAuthIsLoggedIn() != 0, &out);
    return out;
}

static napi_value NapiAuthLogout(napi_env env, napi_callback_info info) {
    (void)info;
    KnAuthLogout();
    return nullptr;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    OH_LOG_INFO(LOG_APP, "libentry Init: register exports then Compose ArkUI bootstrap");
    // Register named exports first so ArkTS import succeeds even if Compose init fails.
    napi_property_descriptor desc[] = {
        {"MainArkUIViewController", nullptr, NapiMainArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"SetOhosHost", nullptr, NapiSetOhosHost, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"SetOhosSecondaryRoute", nullptr, NapiSetOhosSecondaryRoute, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"ApplyAuthSession", nullptr, NapiApplyAuthSession, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthLoginWithPassword", nullptr, NapiAuthLoginWithPassword, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthLoginWithOtp", nullptr, NapiAuthLoginWithOtp, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthSendPhoneOtp", nullptr, NapiAuthSendPhoneOtp, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthRegister", nullptr, NapiAuthRegister, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthLoginWithHuawei", nullptr, NapiAuthLoginWithHuawei, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthToken", nullptr, NapiAuthToken, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthDisplayName", nullptr, NapiAuthDisplayName, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthUserId", nullptr, NapiAuthUserId, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthPhone", nullptr, NapiAuthPhone, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthIsLoggedIn", nullptr, NapiAuthIsLoggedIn, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"AuthLogout", nullptr, NapiAuthLogout, nullptr, nullptr, nullptr, napi_default, nullptr},
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
