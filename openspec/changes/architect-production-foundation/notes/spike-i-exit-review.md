# Spike I Exit Review

**Change:** `architect-production-foundation`  
**Reviewer role:** Agent-Gate  
**Date:** 2026-09-04  
**Verdict:** **Conditional Pass** — exit Spike I when owners treat **2.4** as accepted Partial and docs/compile stay green; Spike II owns remaining Must gaps.

---

## Scorecard (tasks 1.x–3.x, 5.1)

| Task | Result | Evidence |
|------|--------|----------|
| **1.1** ADR + Excalidraw | **Pass** | `docs/adr/0001-production-architecture.md`; `docs/architecture/my-kmp-architecture.excalidraw` |
| **1.2** MODULES / openspec five-layer | **Pass** | Package map in `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/MODULES.md` (not repo-root `MODULES.md`); OpenSpec change under `openspec/changes/architect-production-foundation/` |
| **1.3** Layer dep guard script | **Pass** | `scripts/check-layer-deps.sh` runs and reports violations (Detekt deferred as planned) |
| **2.1** `AuthSessionState` Flow SoT | **Pass** | `feature/auth/AuthSessionState.kt` — `StateFlow` snapshot + mirrored Compose fields |
| **2.2** `AppContainer` subcomponents | **Pass** | `app/AppContainer.kt` — `Network` / `Session` / `Bridge` / `Navigation` |
| **2.3** sealed `AppRoute` + `AppNavigator` stub | **Pass** | `core/router/AppNavigator.kt` (`AppRoute`, `AppNavigator`, `LocalAppNavigator`) |
| **2.4** Migrate ≥1 secondary route off Home `when` | **Partial** (tasks now `[x]`) | Proof path via `LocalAppNavigator` (`AppRoute.InAppWeb` / `AllServices`) accepted in `tasks.md`; **Home still imports sibling features** (layer script). Full hub migration unfinished. |
| **2.5** Android compile | **Pass** (prior Spike I) | Task marked `[x]`; re-verify this gate run below |
| **3.1** Package ownership map | **Pass** | Same `MODULES.md` five-layer + feature list |
| **3.2** `component/{push,webview,pay,chat}` ports | **Pass** | `component/push`, `webview`, `pay`, `chat` present; PayGateway in `component.pay` |
| **3.3** OHOS aggregate notes | **Pass** (scaffold) | `docs/architecture/ohos-aggregate.md` — Gradle aggregate still future (4.3) |
| **3.4** Android compile; OHOS link smoke | **Partial** | Android compile task `[x]`; **OHOS link smoke deferred** (explicit in tasks / ohos-aggregate) |
| **5.1** Must/Nice matrix | **Pass** | `openspec/changes/architect-production-foundation/notes/must-matrix.md` (updated this gate for Push Partial) |

---

## Honest leftovers

### feature→feature violations (still open)

`bash scripts/check-layer-deps.sh` (2026-09-04, Agent-Gate re-run) — **6** violations (earlier gate snapshot had 8; `MineScreen`→`commerce` may already be cleared by parallel work):

| File | Imports |
|------|---------|
| `feature/home/HomeScreen.kt` | `feature.classroom`, `feature.friend`, `feature.live`, `feature.media`, `feature.scan`, `feature.web` |

Exit does **not** require zero violations; Spike II / nav migration (2.4 completion + shell-owned routing) clears them.

### OHOS

- Link / DevEco smoke: **deferred** (3.4 Partial).
- Aggregate Gradle module: notes only; extraction is **4.3**.

### Push / Pay / Design (Spike II bleed)

| Item | Status at Gate |
|------|----------------|
| **4.2** Push skeleton | Ports + deep-link handoff + expect/actual stubs shipped; **vendor SDK not wired ⇒ Must Partial** |
| **4.4** DesignSystem alias | Lightweight `core/design/DesignSystem.kt` + `MineTheme` Accent/PageBg bridge |
| **4.1** Pay sandbox | Matrix already **Partial** (sandbox adapter); not owned by this gate |
| **2.4** | tasks `[x]` with navigator proof; layer deps still Fail on Home sibling imports |

---

## Compile / scripts (this gate)

| Check | Result |
|-------|--------|
| `bash scripts/check-layer-deps.sh` | Exit 1, **6** feature→feature imports (Home only; recorded above; allowed for Spike I exit honesty) |
| `./gradlew :composeApp:compileDebugKotlinAndroid` | **Pass** (BUILD SUCCESSFUL; expect/actual class Beta warnings on `PlatformPushSdk` only) |

Gate-owned syntax surfaces: `component/push/**`, `core/design/DesignSystem.kt`, `feature/mine/MineTheme.kt`.

---

## Spike I exit decision

**Recommendation: Conditional Pass**

Conditions considered met for exit:

1. Docs & boundaries (1.x) present and enforceable via script.
2. Person-1 foundation (2.1–2.3, 2.5) in tree; **2.4 Partial** documented (not Fail).
3. Person-2 components + OHOS notes (3.1–3.3); **3.4 Partial** only for deferred OHOS link smoke.
4. Must matrix published (5.1); Push behavior annotated Partial after 4.2 skeleton.

**Blockers that would turn this into Fail (none observed as absolute):** missing ADR/MODULES/script, missing Auth Flow SoT, missing AppContainer, or Android compile permanently red on gate-owned files.

**Do not claim full Spike I green** until 2.4 is either checked with agreed proof or fully migrated, and do not claim Must Push/Pay until platform SDKs land.

---

## Spike II carry-over (explicit)

- Finish 2.4 / clear `check-layer-deps` Home+Mine hubs  
- 4.1 Pay sandbox ≥1 channel  
- 4.2 platform Push SDK (token + click) — skeleton only today  
- 4.3 `:core:network` / `:core:account` + OHOS aggregate  
- 4.4 continue DesignSystem adoption beyond alias  
- OHOS link smoke when DevEco capacity available  
