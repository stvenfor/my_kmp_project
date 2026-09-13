## Context

See `proposal.md` for motivation. Current state that shapes the design:

- `:core:network` keeps Ktor on intermediate source set `networkKtorMain` → **android + ios only**.
- OHOS uses `OhosApiClient` + `OhosHttpTransport` + `net_http` cinterop (`demo_net_http_*`), with a **global flight mutex** (single in-flight request).
- Catalog pins `ktor = "3.3.1"` (no OH suffix). Comment states no `ohosArm64` variant.
- Kotlin/CMP line is CPF **`-0.3.0`**; Maven already prefers `maven.eazytec-cloud.com`.
- Evidence from CPF: recommended docs version `3.3.3-1.0.0`; same line also publishes `3.3.3-0.3.0` matching this repo’s Kotlin. Plain `3.3.1` / `3.3.3` have **no** `ohosArm64`. Branch name `main-3.3.3-OH` ≠ Maven version `3.3.3-OH` (404). Sources: [CPF ktor](https://gitcode.com/CPF-KMP-CMP/ktor), [三方库.md](https://gitcode.com/CPF-KMP-CMP/docs/blob/main/zh-cn/深入开发/三方库.md).

```
Today:
  commonMain (ApiClient expect)
       ├─ networkKtorMain → KtorApiClient  (android OkHttp / ios Darwin)
       └─ ohosMain → OhosApiClient + cinterop

Target (arm64):
  commonMain
       └─ networkKtorMain (or renamed shared client set)
            ├─ android OkHttp
            ├─ ios Darwin
            └─ ohosArm64 + OH engine (CIO and/or Curl)
```

## Goals / Non-Goals

**Goals:**

- Adopt CPF-published Ktor with `ohosArm64` so OHOS can share `KtorApiClient` (headers, envelope, token hooks).
- Keep Android/iOS engine choices stable while bumping versions.
- Make HTTPS and `ohosX64` gaps explicit with gates / fallbacks.
- Document impact (size, TLS, concurrency, version-line coupling) before deleting cinterop.

**Non-Goals:**

- Full CPF `-1.0.0` toolchain migration in the same change (optional path only).
- Enabling Coil on OHOS (separate change; only note version coupling).
- Reworking ArkTS/NAPI shell.

## Decisions

### D1 — Default version line: `3.3.3-0.3.0` (not unsuffixed `3.3.1`, not blind `3.3.3-1.0.0`)

| Option | Pros | Cons |
|--------|------|------|
| Stay `3.3.1` | No bump | **No `ohosArm64`** — cannot use CPF OH support |
| **`3.3.3-0.3.0`** | Matches Kotlin/coroutines `-0.3.0`; has `ohosArm64` | Docs “recommended” table points at `-1.0.0` |
| `3.3.3-1.0.0` | Matches CPF docs table | Pulls Kotlin stdlib `-1.0.0`; implies **whole-stack bump** |

**Choice:** Implement against **`ktor = "3.3.3-0.3.0"`** first. Treat `3.3.3-1.0.0` + Kotlin/CMP `-1.0.0` as **Option B** (wider blast radius), only if `-0.3.0` artifacts prove incomplete.

### D2 — Source-set wiring: extend `networkKtorMain` to OHOS arm64

**Choice:** Keep intermediate set (do not dump all Ktor into `commonMain` yet) so OHOS-only engine deps stay scoped:

1. `ohosArm64Main` / `ohosMain` **dependsOn** `networkKtorMain` (or split `networkKtorMain` → shared client + per-engine children).
2. Add OH engine dependency only on OHOS source sets.
3. Replace `createPlatformApiClient` OHOS actual to construct `HttpClient(Engine) { configureDemoHttpClient() }` + `KtorApiClient`.
4. Delete or `#ifdef`-retire `OhosApiClient` / `OhosHttpTransport` after HTTPS gate.

**Alternative considered:** CPF demo style — CIO in `commonMain` on all platforms. Rejected for now: would regress Android/iOS away from OkHttp/Darwin without clear benefit.

### D3 — OHOS engine: CIO first probe, Curl if HTTPS fails

| Engine | CPF docs | Maven ohosArm64 | TLS risk |
|--------|----------|-----------------|----------|
| **CIO** | Listed adapted | Yes | Native TLS session may be stubbed → **HTTPS likely broken** |
| **Curl** | Not in adapted module table | Yes (`ktor-client-curl`) | Static OpenSSL in fork; **~8MB** class size before strip |
| Keep cinterop | Current | N/A | Known working; serializes flights |

**Choice:**

1. Spike: CIO + HTTPS smoke on device.
2. If fail → Curl + linker opts for curl/ssl/crypto/z (and package `.a` if required).
3. If both fail → abort removal of cinterop; change stays “partial” (version bump only on a/i).

### D4 — `ohosX64` policy

Published CPF Ktor klibs observed for **arm64 only**. **Choice:** Keep `ohosX64` target but **do not** depend on Ktor there — either compile a thin cinterop fallback for x64 only, or mark network-on-x64 emulator unsupported in module docs and skip Ktor deps for that compilation. Prefer **x64 fallback to cinterop** during transition so emulator keep-alive does not block arm64 Ktor work.

### D5 — Catalog & modules to touch

- `gradle/libs.versions.toml`: `ktor` version; add `ktor-client-cio` and/or `ktor-client-curl` aliases.
- `core/network/build.gradle.kts`: source set graph, cinterop conditional, OH deps.
- OHOS actuals under `core/network/src/ohosMain/...`.
- `composeApp/build.gradle.kts`: drop `-lnet_http` / static `libdemo_net_http.a` when cinterop retired.
- Tests: extend `KtorApiClientMockTest`; add OHOS smoke checklist (device).

## Impact assessment (detailed)

### Code / architecture

| Area | Change | Effect |
|------|--------|--------|
| Shared API | Unchanged `ApiClient` surface | Features/account keep calling façade |
| OHOS transport | cinterop → Ktor | One implementation path for envelope/headers |
| Concurrency | Mutex removed after Ktor accept | Better parallel loads; new race/load characteristics |
| Dual stack during spike | Temporary | Higher maintenance until gate passes |

### Dependencies

| Artifact | Today | After (default path) |
|----------|-------|----------------------|
| `ktor-client-*` | `3.3.1` | `3.3.3-0.3.0` |
| OH engine | none | `cio` and/or `curl` |
| Kotlin line | `2.2.21-0.3.0` | unchanged (Option A) |
| Coil | `3.3.0` a/i only | still off OH unless separate CPF Coil align |

### Runtime / product

| Topic | Impact |
|-------|--------|
| HTTPS | **Critical gate** — CIO may fail; Curl may fix at size cost |
| Binary size | Curl+OpenSSL can add multi‑MB to `libkn.so` |
| Latency / parallelism | Likely better than single-flight cinterop |
| Behavior drift | Should **decrease** vs dual OhosApiClient vs KtorApiClient |
| Emulator x64 | May stay on cinterop or unsupported |

### Process / ops

- Must re-`publish*BinariesToHarmonyApp` and DevEco install after network native changes.
- Android unit tests should still pass with mock engine after version bump.
- Rollback: revert catalog + restore ohos actuals/cinterop (keep git until gate green).

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| CIO HTTPS stub on Native | Device spike; fall back to Curl or keep cinterop |
| Curl binary size / linker complexity | Measure stripped `libkn.so` delta; document; optional dynamic link if CPF provides |
| Version-line skew (`-0.3.0` vs docs `-1.0.0`) | Prefer matching Kotlin line; escalate to Option B only if needed |
| Mixing `3.3.3-1.0.0` Ktor with Kotlin `-0.3.0` | **Do not**; treat as unsupported |
| `ohosX64` resolve failure | Per-target deps / fallback (D4) |
| Coil/Ktor version coupling later | Note in tasks; out of scope unless enabling OH images |
| Auth header / 401 handling parity | Reuse `configureDemoHttpClient` + same handlers; compare responses on device |

## Migration Plan

1. **Catalog bump** to `3.3.3-0.3.0` on android/ios only — prove a/i still green.
2. **Wire ohosArm64** + CIO; keep cinterop code behind actual switch or parallel type.
3. **HTTPS + auth smoke** on arm64 device.
4. If fail → Curl path; retest.
5. If pass → switch default actual to Ktor; remove cinterop + linker flags; update AGENTS/architecture notes.
6. **Rollback:** restore previous catalog version and ohos actuals; republish binaries.

## Open Questions

- Does the **published** `ktor-client-cio-ohosarm64` klib differ from fork source regarding TLS stubs? (Answer with device spike, not docs alone.)
- Exact linker recipe for Curl static libs on this repo’s OH sysroot (copy from CPF demo vs sysroot).
- Whether product still needs `ohosX64` emulator for network demos in CI/dev loops.
