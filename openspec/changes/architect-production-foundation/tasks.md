## 1. Docs & boundaries

- [x] 1.1 ADR `docs/adr/0001-production-architecture.md` + Excalidraw `docs/architecture/my-kmp-architecture.excalidraw`
- [x] 1.2 Update `MODULES.md` / openspec context to five-layer + four tabs (AGENTS follow-up OK)
- [x] 1.3 Add dependency guard script `scripts/check-layer-deps.sh` (feature↛feature); Detekt later

## 2. Spike I — Person 1 (nav / auth / container)

- [x] 2.1 Introduce `AuthSessionState` Flow snapshot as SoT (Compose fields still mirrored)
- [x] 2.2 Add `AppContainer` with subcomponents (`Network` / `Session` / `Bridge` / `Navigation`)
- [x] 2.3 Add sealed `AppRoute` + `AppNavigator` stub
- [x] 2.4 Migrate at least one secondary route off `HomeScreen` local `when` as proof
- [x] 2.5 `./gradlew :composeApp:compileDebugKotlinAndroid`

## 3. Spike I — Person 2 (modules / components / OHOS)

- [x] 3.1 Package ownership map in `MODULES.md` (Gradle empty modules next)
- [x] 3.2 Create `component/{push,webview,pay,chat}` ports; move PayGateway into `component.pay`
- [x] 3.3 Scaffold OHOS aggregate build notes (`docs/architecture/ohos-aggregate.md`)
- [x] 3.4 `./gradlew :composeApp:compileDebugKotlinAndroid` (OHOS link smoke deferred)

## 4. Spike II (after I)

- [x] 4.1 Sandbox real Pay SDK path (no `Unavailable` placeholder) on Must matrix
- [x] 4.2 Push: token upload + notification click → deep link (three platforms) — skeleton only; platform SDK unwired = Must Partial (see exit review / must-matrix)
- [x] 4.3 Extract `:core:network` + `:core:account` behind aggregate for OHOS — Partial: modules extracted + Android compile; `:ohosAggregate` placeholder; see `notes/core-modules-extraction.md`
- [x] 4.4 DesignSystem alias migration for Mine/Membership tokens — lightweight `DesignSystem` + MineTheme Accent/PageBg bridge
- [x] 4.5 Unfreeze Flutter parity only inside new boundaries — policy: `notes/parity-unfreeze.md`; gate ref in `parity-flutter-to-kmp/notes/boundary-gate.md`

## 5. Gate

- [x] 5.1 Publish Must/Nice matrix under `openspec/changes/architect-production-foundation/notes/must-matrix.md`
- [x] 5.2 Spike I exit review with both owners; record Partial/Fail honestly → `notes/spike-i-exit-review.md` (Conditional Pass)
