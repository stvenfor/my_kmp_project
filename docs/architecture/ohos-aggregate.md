# OHOS fat aggregate (Spike II / task 4.3)

Goal: Android/iOS may use multiple Gradle modules; OHOS still emits one `libkn.so`.

## Current Gradle include graph (real)

```text
settings.gradle.kts
├── :composeApp          ← active OHOS sharedLib (`baseName = kn`) + Harmony publish
├── :core:network        ← KMP lib (android / ios* / ohosArm64 / ohosX64)
│                          owns networkKtorMain + ohos net_http cinterop
├── :core:account        ← KMP lib (android / ios* / ohosArm64 / ohosX64)
│                          owns accountSettingsMain; depends on :core:network
└── :ohosAggregate       ← PLACEHOLDER (no KMP targets yet)
```

### Runtime dependency edges

```text
:composeApp
  implementation → :core:account
  implementation → :core:network
:core:account
  implementation → :core:network
:ohosAggregate
  (no deps — status task only)
```

### Publish path (unchanged)

```bash
./gradlew :composeApp:publishDebugBinariesToHarmonyApp
```

Still copies `libkn.so` + `libkn_api.h` + composeResources into `harmonyApp/`.
Host ArkTS continues to load `MainArkUIViewController` from that binary.

## Status: Partial

| Item | State |
|------|-------|
| Extract `:core:network` / `:core:account` | Done (common + android + ios + ohos sources) |
| Intermediate source sets moved | Done |
| `:composeApp` consumes project deps | Done |
| `:ohosAggregate` owns link / publish | **Not yet** — placeholder only |
| OHOS link smoke (`linkDebugSharedOhosArm64`) | Deferred / not gated on 4.3 |

## Target shape (next step)

```text
:core:network
:core:account
:component:pay / push / …
:feature:*
:ohosAggregate   // api(all needed modules); sole OHOS sharedLib + publish* tasks
:composeApp      // Android application + iOS framework only (no ohos binaries)
```

Migration checklist when promoting `:ohosAggregate`:

1. Move OHOS `sharedLib { baseName = "kn" }`, linkerOpts, `moduleIncludes`, and remaining cinterops (`resource`, `avplayer`) from `:composeApp` → `:ohosAggregate`.
2. Point Harmony publish Copy tasks at `:ohosAggregate` outputs.
3. Keep `MainArkUIViewController` symbol stable (or update ArkTS + aggregate together).
4. Verify `linkDebugSharedOhosArm64` + DevEco load.

## Owner

Spike II Agent-Modules (task 4.3). Notes: `openspec/changes/architect-production-foundation/notes/core-modules-extraction.md`.
