# 4.3 Core modules extraction notes

**Date:** 2026-09-04  
**Status:** Partial (Android compile path green; OHOS aggregate still placeholder)

## What landed

- `settings.gradle.kts` includes `:core:network`, `:core:account`, `:ohosAggregate`
- Sources moved out of `:composeApp` into KMP library modules (all targets: common/android/ios/ohos)
- Intermediate source sets moved:
  - `networkKtorMain` → `:core:network`
  - `accountSettingsMain` → `:core:account`
- OHOS `net_http` cinterop + static libs moved to `:core:network`
- `:composeApp` `implementation(projects.core.network)` / `projects.core.account`
- Cross-module APIs previously `internal` promoted to `public` (same package names retained)

## Module graph

```text
:composeApp → :core:account → :core:network
:composeApp → :core:network
:ohosAggregate (placeholder, no targets)
```

## OHOS honesty

- Library modules declare `ohosArm64` / `ohosX64` so `:composeApp` can resolve them on OHOS.
- Fat link + `publish*BinariesToHarmonyApp` remain on `:composeApp`.
- `:ohosAggregate` is an empty placeholder with `ohosAggregateStatus` task only.
- Full OHOS link smoke is **not** claimed green by this task.

## Follow-ups

1. Promote sharedLib/export/cinterop(resource, avplayer)/publish into `:ohosAggregate`
2. Optionally narrow `public` surface back to façades only
3. Detekt/Gradle import guards across module boundaries
