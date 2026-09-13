## 1. Catalog and Android/iOS baseline

- [x] 1.1 In `gradle/libs.versions.toml`, set `ktor = "3.3.3-0.3.0"` and add library aliases for `ktor-client-cio` and `ktor-client-curl` (Curl may be unused until spike)
- [x] 1.2 Confirm eazytec Maven resolves `io.ktor:ktor-client-core:3.3.3-0.3.0` with `ohosArm64` metadata (Gradle dependency insight or module file check)
- [x] 1.3 Sync `:core:network` Android/iOS only; run `./gradlew :core:network:testDebugUnitTest` and fix any Ktor 3.3.3 API deltas in `networkKtorMain`

## 2. Wire OHOS arm64 to shared Ktor client

- [x] 2.1 Update `core/network/build.gradle.kts` so `ohosArm64Main` (and `ohosMain` as appropriate) depends on `networkKtorMain` without pulling Ktor onto `ohosX64` until D4 policy is applied
- [x] 2.2 Add OHOS engine dependency (start with `ktor-client-cio`) on the OHOS arm64 source set
- [x] 2.3 Implement OHOS `createPlatformApiClient` / bootstrap to build `HttpClient` + reuse `KtorApiClient` / `configureDemoHttpClient`
- [x] 2.4 Keep existing cinterop + `OhosApiClient` compilable behind a clear switch or parallel entry until HTTPS gate passes
- [x] 2.5 Compile arm64: `./gradlew :core:network:compileKotlinOhosArm64` (or equivalent) and fix resolution/link errors

## 3. HTTPS / engine spike (gate)

- [x] 3.1 Publish debug binaries and install on OHOS arm64 device; smoke one HTTPS business URL + one auth-sensitive call
- [x] 3.2 If CIO HTTPS fails, switch OHOS engine to `ktor-client-curl`, add linker/package steps for curl/ssl/crypto as required, retest HTTPS
- [x] 3.3 Record spike result in change notes (CIO vs Curl, `libkn.so` size delta, any linker flags)
- [x] 3.4 Only if HTTPS gate passes: make Ktor the default OHOS `ApiClient` path

> **3.2 note:** N/A — CIO HTTPS confirmed usable; Curl path not taken.

## 4. Retire cinterop (post-gate)

- [x] 4.1 Remove OHOS usage of `OhosHttpTransport` / `OhosApiClient` from the default path
- [x] 4.2 Remove `net_http` cinterop registration from `:core:network` when unused
- [x] 4.3 Remove `-lnet_http` / `libdemo_net_http.a` linker wiring from `:composeApp` (and any remaining references)
- [x] 4.4 Update `AGENTS.md` / architecture notes: OHOS network = CPF Ktor (engine noted), not cinterop

> **4.x note:** Retired on **ohosArm64** only. **ohosX64** still registers cinterop + `-lnet_http` (D4).

## 5. ohosX64 and docs parity

- [x] 5.1 Apply design D4: x64 keeps cinterop fallback **or** document network-on-x64 unsupported and exclude Ktor deps for that target
- [x] 5.2 Verify `ohosX64` compile still succeeds under the chosen policy
- [x] 5.3 Confirm Android OkHttp + iOS Darwin still used after version bump; spot-check login/API on at least Android

## 6. Optional full-stack path (only if Option A blocked)

- [x] 6.1 If `3.3.3-0.3.0` is insufficient, plan separate bump to Kotlin/CMP/Ktor `-1.0.0` line (`3.3.3-1.0.0`) — do not mix lines
- [x] 6.2 Re-run sections 1–5 against the `-1.0.0` matrix

> **6.x note:** Option A (`3.3.3-0.3.0`) resolved + compiled for `ohosArm64`; no `-1.0.0` bump required. Tasks marked done as N/A.
