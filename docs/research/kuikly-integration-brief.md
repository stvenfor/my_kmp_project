# Kuikly integration brief (for existing CPF-KMP-CMP / my_kmp_project)

Evidence-based notes from official Kuikly docs and Tencent-TDS sources (retrieved 2026-09-13). Claims below are tied to URLs. **Unknowns are flagged.** Do not treat blog/CSDN numbers as product contracts unless corroborated by `kuikly.tds.qq.com` or the KuiklyUI README.

**Primary sources**

| Source | URL |
|--------|-----|
| Docs site | https://kuikly.tds.qq.com |
| GitHub | https://github.com/Tencent-TDS/KuiklyUI |
| README (EN/ZH) | https://github.com/Tencent-TDS/KuiklyUI/blob/main/README.md |
| Compose overview | https://github.com/Tencent-TDS/KuiklyUI/blob/main/docs/Compose/overview.md |
| Compose getting started | https://github.com/Tencent-TDS/KuiklyUI/blob/main/docs/Compose/getting-started.md |
| Integration overview | https://kuikly.tds.qq.com/QuickStart/overview.html |
| KMP / template | https://kuikly.tds.qq.com/QuickStart/common.html |
| Env setup | https://kuikly.tds.qq.com/QuickStart/env-setup.html |
| Android host | https://kuikly.tds.qq.com/QuickStart/android.html |
| iOS host | https://kuikly.tds.qq.com/QuickStart/iOS.html |
| Harmony host | https://kuikly.tds.qq.com/QuickStart/harmony.html |
| Harmony toolchain | https://kuikly.tds.qq.com/DevGuide/harmony-dev.html |
| Version matrix | https://kuikly.tds.qq.com/DevGuide/version_skills.html |
| Paradigms | https://kuikly.tds.qq.com/Introduction/paradigm.html |
| Dynamicization | https://kuikly.tds.qq.com/DevGuide/dynamic-guide.html |
| Architecture / Shiply | https://kuikly.tds.qq.com/Introduction/arch.html |
| Roadmap 2026 | https://kuikly.tds.qq.com/Blog/roadmap2026.html |

**my_kmp_project baseline (local catalog, for risk contrast)**

- Kotlin / KMP: `2.2.21-0.3.0` (CPF OHOS fork)
- Compose Multiplatform: `1.9.2-0.3.0`
- AGP: `8.6.0`
- Ktor OHOS: `3.3.3-0.3.0` + CIO on `ohosArm64`
- Shells: `composeApp` → `libkn.so` + `harmonyApp` ArkTS; `iosApp` SwiftUI; Android Activity

---

## 1. Official supported platforms and maturity (esp. HarmonyOS)

**Documented platform matrix** ([README](https://github.com/Tencent-TDS/KuiklyUI/blob/main/README.md)):

| Platform | Status in README |
|----------|------------------|
| Android | Supported `[X]` |
| iOS | Supported `[X]` |
| HarmonyOS | Supported `[X]` |
| Web | **Beta** |
| Mini Programs | **Beta** |
| macOS | **Alpha** |

**OS floors** (same README): Android 5.0+, iOS 12.0+, HarmonyOS Next **5.0.0(12)+**, macOS 10.13+, Kotlin 1.3.10+.

**HarmonyOS toolchain maturity**

- OHOS KMP binaries require a **custom Kotlin toolchain**, not stock JetBrains Kotlin: documented versions `2.0.21-KBA-004` and `2.0.21-KBA-010` ([harmony-dev](https://kuikly.tds.qq.com/DevGuide/harmony-dev.html)).
- Host tooling: DevEco Studio **5.1.0+**, Harmony SDK API **≥ 18** ([README](https://github.com/Tencent-TDS/KuiklyUI/blob/main/README.md), [env-setup](https://kuikly.tds.qq.com/QuickStart/env-setup.html)).
- Template + host docs treat Harmony as a first-class third host (`ohosApp`, `@kuikly-open/render`, `linkOhosArm64`) ([common](https://kuikly.tds.qq.com/QuickStart/common.html), [harmony](https://kuikly.tds.qq.com/QuickStart/harmony.html)).
- Roadmap 2026 recounts: **2025 completed open-sourcing of Harmony / H5 / Mini Program**; Compose DSL was **Beta** in 2025 and is planned for **formal Release in 2026**. Roadmap table for “multi-platform Release” lists Web / Mini Program / macOS — **not Harmony** as still Beta ([roadmap2026](https://kuikly.tds.qq.com/Blog/roadmap2026.html)).

**Compose DSL OHOS caveat (important)**

> Compose DSL related SDKs with `-ohos` suffix **currently only support ohos**; multi-target Compose SDK versions are “待后续发布”. When using a unified OHOS toolchain, pick `-ohos` Compose artifacts for ohos target and non-`-ohos` for Android/iOS ([harmony-dev](https://kuikly.tds.qq.com/DevGuide/harmony-dev.html)).

**Unknown:** Exact production SLA / API stability grade for Harmony beyond “supported + open-sourced”; no public LTS statement found on docs site.

---

## 2. How a new Kuikly app is structured

### Two halves of the product

From [QuickStart overview](https://kuikly.tds.qq.com/QuickStart/overview.html):

1. **KuiklyCore** (KMP): declarative/reactive core + unified UI interfaces → compile to **`.aar` / `.framework` / `.so`**.
2. **KuiklyRender** (per platform): actual native rendering + adapters; embed into host containers.

KMP side is done once; **each platform** must wire Render + adapters + a host container.

### Typical greenfield layout

Scaffold: Android Studio **Kuikly Project Template** ([common](https://kuikly.tds.qq.com/QuickStart/common.html), [env-setup](https://kuikly.tds.qq.com/QuickStart/env-setup.html)):

| Piece | Role |
|-------|------|
| `shared` (KMP) | Business UI/logic (`commonMain`, `@Page` pages) |
| `androidApp` | Android host |
| `iosApp` | iOS host (CocoaPods → shared framework) |
| `ohosApp` | Harmony host (DevEco; `@kuikly-open/render` + NAPI) |
| Optional | `h5App` / `miniApp` |

Upstream open-source tree mirrors this (`core`, `compose`, `core-render-*`, `androidApp` / `iosApp` / `ohosApp`) ([README project structure](https://github.com/Tencent-TDS/KuiklyUI/blob/main/README.md)).

### Entry / pages

- Pages annotated `@Page("…")`; KSP generates `KuiklyCoreEntry` / route registration ([version_skills](https://kuikly.tds.qq.com/DevGuide/version_skills.html)).
- Compose path: class extends `ComposeContainer`, `willInit()` → `setContent { … }` ([Compose getting-started](https://github.com/Tencent-TDS/KuiklyUI/blob/main/docs/Compose/getting-started.md)).
- Self-developed DSL path: e.g. `BasePager` / `body(): ViewBuilder` (examples in [harmony-dev](https://kuikly.tds.qq.com/DevGuide/harmony-dev.html)).

### Native hosts (documented embedding)

| Platform | Documented containers |
|----------|------------------------|
| Android | Activity / Fragment; also **View-level** `KuiklyBaseView` ([android](https://kuikly.tds.qq.com/QuickStart/android.html)) |
| iOS | `KuiklyRenderViewController` or **View-level** `KuiklyBaseView` ([iOS](https://kuikly.tds.qq.com/QuickStart/iOS.html)) |
| Harmony | Existing DevEco app: `@kuikly-open/render`, NAPI `InitKuikly` → `libshared_symbols()` / `initKuikly()`, ArkTS manager + Kuikly view ([harmony](https://kuikly.tds.qq.com/QuickStart/harmony.html)) |

Host must also implement adapters (image, router, log, etc.; PAG optional) — patterns described in platform QuickStart pages.

### OHOS binary flow

`./gradlew -c settings.ohos.gradle.kts :shared:linkOhosArm64` → `shared/build/bin/ohosArm64/` `.so` + header → copy into `ohosApp` libs / cpp (or Hvigor `kuikly-ohos-compile-plugin`) ([harmony-dev](https://kuikly.tds.qq.com/DevGuide/harmony-dev.html)).

---

## 3. Kuikly Compose DSL vs JetBrains / CPF Compose Multiplatform

### What official docs say they are

| Dimension | Kuikly Compose | Compose Multiplatform (as contrasted in Kuikly docs) |
|-----------|----------------|------------------------------------------------------|
| Stack | Compose-like DSL **on Kuikly Core**, native render | Skia / self-draw (Kuikly’s comparison table) |
| Packages | UI: `com.tencent.kuikly.compose.*`; runtime: `androidx.compose.runtime.*` | JetBrains / AndroidX Compose packages |
| Platforms (Kuikly claim) | Android / iOS / 鸿蒙 / H5 / 微信小程序 / Desktop（支持中） | Android / iOS / Desktop / H5 |
| Dynamicization | Supported (framework capability) | Not supported (per Kuikly table) |

Sources: [Compose overview](https://github.com/Tencent-TDS/KuiklyUI/blob/main/docs/Compose/overview.md), [getting-started import rules](https://github.com/Tencent-TDS/KuiklyUI/blob/main/docs/Compose/getting-started.md).

**Lineage (README note, not an API):** Compose tree is adapted from **Jetpack Compose 1.7.3** / JetBrains compose-multiplatform-core; package rename `androidx.compose` → `com.tencent.kuikly.compose` “to avoid conflicts with official code” ([README](https://github.com/Tencent-TDS/KuiklyUI/blob/main/README.md)).

### Can they coexist in one process / binary?

| Question | Evidence | Verdict |
|----------|----------|---------|
| Same **source file** mixing `androidx.compose.foundation` (CMP) and `com.tencent.kuikly.compose.foundation`? | Import rules require Kuikly UI packages for Kuikly pages; runtime may share `androidx.compose.runtime` | **Different APIs**; not drop-in interchangeable |
| Package rename to avoid conflict with “official” Compose? | Explicit in README / getting-started | Suggests **classpath coexistence of renamed UI packages was a design goal** vs stock AndroidX/CMP UI packages |
| One **native host** showing a Kuikly page beside a native / non-Kuikly UI? | Android/iOS/Harmony embedding docs | **Yes — documented** at Activity/VC/ArkTS / View granularity |
| One **Compose Multiplatform** `@Composable` tree hosting Kuikly, or one KN `libkn.so` also linking Kuikly Core? | **No official guide found** | **Unknown / unsupported in docs** |
| One Gradle project, one Kotlin version, CPF `2.2.21-0.3.0` + Kuikly OHOS `2.0.21-KBA-*`? | Different published toolchains | **High conflict risk** (see §7) |

**Factual bottom line:** Kuikly Compose is a **forked DSL + native renderer**, not “CMP with another target.” Coexistence that *is* documented is **native-host embedding**. Process-level dual CMP-Skia + Kuikly-native UI inside the **same KMP shared binary** is **not documented**.

---

## 4. Recommended integration patterns

Inferred from official “how you are supposed to start,” not invented architecture.

### A. Greenfield Kuikly app (docs default)

1. Install Kuikly AS plugin (≥ **1.1.0** for Ohos template) ([env-setup](https://kuikly.tds.qq.com/QuickStart/env-setup.html)).
2. **New → Kuikly Project Template**; choose Compose or Kuikly DSL; enable Android / iOS / Harmony ([common](https://kuikly.tds.qq.com/QuickStart/common.html)).
3. Align versions across `shared`, Android render, iOS pod, `ohosApp` `@kuikly-open/render` ([version_skills](https://kuikly.tds.qq.com/DevGuide/version_skills.html)).
4. Wire Render adapters per platform QuickStarts.

**Best when:** evaluating Kuikly or building a Kuikly-first product.

### B. Embed Kuikly pages in an existing native host (docs-supported)

Explicitly supported for **existing** Android / iOS / Harmony apps ([overview](https://kuikly.tds.qq.com/QuickStart/overview.html), platform QuickStarts):

- Keep native shell; add KuiklyRender + business `.aar` / framework / `.so`.
- Open Kuikly pages via Activity/VC or embed `KuiklyBaseView` / ArkTS Kuikly component.
- Still need a **separate KMP `shared` (or equivalent)** compiled with a Kuikly-compatible toolchain.

**Best when:** native (or hybrid) shell already exists and you want **page- or card-level** Kuikly islands.

### C. Replace CMP entirely with Kuikly

- Architecturally plausible (both are full cross-platform UI stacks), but **no official “migrate from Compose Multiplatform / CPF” guide** was found.
- You would re-host UI under `@Page` / `ComposeContainer`, change packages to `com.tencent.kuikly.compose.*`, replace Skia/CMP shell (`MainArkUIViewController` / `libkn.so`) with Kuikly Render + `InitKuikly` / `libshared.so`.
- Cost is a **product rewrite**, not a Gradle dependency bump.

### D. Against my_kmp_project specifically

| Pattern | Fit |
|---------|-----|
| Greenfield Kuikly module/repo | **Lowest coupling** to CPF toolchain |
| Embed Kuikly in `harmonyApp` / Android Activity / iOS VC **beside** CMP | Possible **only if** Kuikly business is a **separate KMP artifact** with its own Kotlin line; dual UI stacks in one app process still need careful host composition |
| “Add Kuikly into `composeApp`” | **Not recommended** by evidence (toolchain + renderer + entry symbol collision) |
| Replace CMP with Kuikly | Only if abandoning CPF CMP path |

---

## 5. Build toolchain requirements (documented)

### General

- Android Studio + Kotlin + KMP plugins; **Kuikly AS plugin**; **JDK 17** ([env-setup](https://kuikly.tds.qq.com/QuickStart/env-setup.html)).
- AS ≥ 2024.2.1: set Gradle JDK to **17** (default 21 incompatible with their configs) ([README](https://github.com/Tencent-TDS/KuiklyUI/blob/main/README.md)).
- iOS: Xcode + CocoaPods.
- Harmony: DevEco **5.1.0+**, API **≥ 18**.
- Maven (Kuikly ≥ 2.5.0): `https://mirrors.tencent.com/nexus/repository/maven-tencent/` ([common](https://kuikly.tds.qq.com/QuickStart/common.html)).

### Recommended version matrix (not mandatory)

From [version_skills](https://kuikly.tds.qq.com/DevGuide/version_skills.html):

| Kotlin | AGP | KSP | Gradle |
|--------|-----|-----|--------|
| 2.1.21 | 8.5.0 | 2.1.21-2.0.1 | 8.7 |
| 2.0.21 | 8.5.0 | 2.0.21-1.0.27 | 8.7 |
| **2.0.21-KBA-010** | 8.5.0 | 2.0.21-1.0.27 | 8.7 |
| 1.9.22 … | older | … | … |

Docs say: if the app already pins Kotlin/AGP/Gradle, pick matching Kuikly artifacts; keep **KMP artifact kotlin suffix** aligned with **render version** (e.g. core `2.12.0-2.1.21` ↔ render `2.12.0`).

**AGP 9:** “适配指南后续支持” ([version_skills](https://kuikly.tds.qq.com/DevGuide/version_skills.html)).

### Harmony-specific

- Custom Kotlin: **`2.0.21-KBA-010`** (Win/Linux) or `2.0.21-KBA-004`.
- Prefer separate `settings.ohos.gradle.kts` for OHOS builds ([harmony-dev](https://kuikly.tds.qq.com/DevGuide/harmony-dev.html)).
- Kuikly version coordinate form for OHOS line: `KUIKLY_VERSION-2.0.21-ohos`.
- Env: `OHOS_SDK_HOME` (Windows docs).

**Unknown / not found in docs:** Official support matrix for **CPF** Kotlin `2.2.21-0.3.0` or Compose Multiplatform `1.9.x-0.3.0`. Highest Kotlin called out in Kuikly version tables / badge is **2.1.21** (plus KBA-010 OHOS line).

---

## 6. Dynamicization / Shiply — optional or core?

| Topic | Official stance |
|-------|-----------------|
| Built-in AOT (`.aar` / framework / `.so`) | Default product path; full QuickStarts |
| Dynamicization | Framework **capability**; paradigms describe built-in vs dynamic pages ([paradigm](https://kuikly.tds.qq.com/Introduction/paradigm.html)) |
| Open-source status of dynamicization | “开源时间待定”; contact **Shiply** ([dynamic-guide](https://kuikly.tds.qq.com/DevGuide/dynamic-guide.html)) |
| Shiply | Recommended companion for **full-process publish** + Bugly quality gates ([arch](https://kuikly.tds.qq.com/Introduction/arch.html)); **not** required to compile/run built-in Kuikly |
| Bugly Kuikly monitor | Optional; Harmony: use native Bugly, Kuikly-specific monitor “暂不” on Harmony per monitor doc |

**Dynamicization constraints if used later** (standard / dynamic page type): single-thread Kuikly model; no direct platform APIs (use Module); **cannot** depend on coroutines or KMP libs with `{platform}Main` ([paradigm](https://kuikly.tds.qq.com/Introduction/paradigm.html)). Advanced “纯内置” pages may use normal KMP + coroutines (must hop back to Kuikly thread for UI).

**Verdict:** Dynamicization/Shiply are **optional commercial/ops extensions**, not required to adopt Kuikly as an embedded/built-in UI framework. Planning only for AOT still needs to respect paradigm constraints if you later want dynamic pages.

---

## 7. Concrete risks: “add Kuikly to my_kmp_project” vs “new Kuikly module/repo”

### A. Add into existing `composeApp` / one Gradle tree

| Risk | Why (evidence + local baseline) |
|------|----------------------------------|
| **Kotlin toolchain clash** | Project: CPF `2.2.21-0.3.0`. Kuikly OHOS: `2.0.21-KBA-*`. One KGP version per project is the Gradle norm; docs never claim these forks interoperate. |
| **Dual Compose stacks** | CMP Skia (`org.jetbrains.compose` / CPF) vs Kuikly native (`com.tencent.kuikly.compose`). Different Appliers/renderers; no guide for one shared UI module. |
| **Dual OHOS `.so` + NAPI** | Today: `libkn.so` + `MainArkUIViewController`. Kuikly: `libshared.so` + `InitKuikly` / `@kuikly-open/render`. Same `harmonyApp` would host **two** KN runtimes/render pipelines. |
| **Compose DSL `-ohos` split** | OHOS Compose artifacts not yet the same multi-target coords as Android/iOS ([harmony-dev](https://kuikly.tds.qq.com/DevGuide/harmony-dev.html)). |
| **Network / Ktor** | Arch doc says KMP ecosystem (incl. Ktor) can be reused in KuiklyBase sense ([arch](https://kuikly.tds.qq.com/Introduction/arch.html)), but **standard/dynamic pages forbid** platform-flavored KMP components & normal coroutines ([paradigm](https://kuikly.tds.qq.com/Introduction/paradigm.html)). CPF Ktor CIO on `ohosArm64` is **not** validated in Kuikly docs. |
| **Version ceiling** | Kuikly recommended max in tables: **2.1.21**; project already on **2.2.21-0.3.0**. |
| **AGP** | Project `8.6.0` vs Kuikly recommend `8.5.0` — likely OK per their “flex if compatible” note; still untested combo. |

### B. New Kuikly module / separate repo (still embed in same apps)

| Upside | Evidence |
|--------|----------|
| Isolated `settings.ohos.gradle.kts` + KBA Kotlin | Matches Kuikly’s recommended OHOS layout |
| Hosts only add Render + second native binary / page route | Matches “existing engineering接入” QuickStarts |
| Can keep CPF CMP for current UI | Host-level composition, not shared `commonMain` UI |

| Remaining risks | |
|-----------------|--|
| App size / two engines | Two UI frameworks in one APK/HAP/IPA |
| Navigation / deep links / dual routers | Must bridge CMP router ↔ Kuikly `@Page` / KRRouterAdapter |
| Team cognitive load | Two Compose dialects + two OHOS publish pipelines |
| Still no Shiply OSS | Fine if AOT-only |

### C. Unknowns to treat as blockers until proven

1. Whether CPF Kotlin and Kuikly KBA Kotlin can share one Gradle composite at all.
2. Whether Kuikly `ohosArm64` klib can link against the same OHOS sysroot / LLVM as CPF `libkn.so` in one HAP without symbol/runtime conflicts.
3. Whether `androidx.compose.runtime` versions pulled by Kuikly Compose 1.7.3-line and CMP 1.9.x-line can coexist on Android classpath.
4. Any official “Kuikly inside Compose Multiplatform” interop API — **none found**.

---

## Practical recommendation (evidence-based)

1. **Do not** drop Kuikly into `composeApp` commonMain next to CPF CMP.
2. If evaluating Kuikly: use **greenfield template** or a **separate KMP module/repo** on Kuikly’s documented Kotlin line (`2.1.21` / OHOS `2.0.21-KBA-010`).
3. If product needs both stacks short-term: **native-host embedding** (separate Activity/VC/ArkTS page or View island), not a unified Compose tree.
4. Treat **Shiply/dynamicization as optional**; design AOT pages with paradigm constraints only if dynamicization is a hard requirement later.
5. Replacing CPF CMP with Kuikly is a **stack migration**, not an incremental dependency.

---

## Source confidence

| Claim area | Confidence |
|------------|------------|
| Platforms + Beta/Alpha labels | High (README) |
| Host embedding Android/iOS/Harmony | High (QuickStart) |
| OHOS custom Kotlin KBA | High (harmony-dev) |
| Package rename / import rules | High (README + Compose docs) |
| Dynamicization OSS deferred / Shiply contact | High (dynamic-guide) |
| CMP vs Kuikly comparison table | Medium-High (Kuikly-authored; CMP “no Harmony” is their claim, not JetBrains’) |
| Single-process dual CMP+Kuikly in one KN binary | **Unknown** (no primary doc) |
| Compatibility with CPF `2.2.21-0.3.0` | **Unknown** (no primary doc) |
