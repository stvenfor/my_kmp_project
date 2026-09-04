> **⚠ BOUNDARY GATE：** 尖刀期对等冻结已解除，但恢复/继续本 change 实施前必须满足 architect 五层边界。  
> 权威政策：[`../architect-production-foundation/notes/parity-unfreeze.md`](../architect-production-foundation/notes/parity-unfreeze.md)  
> 醒目摘要：[`notes/boundary-gate.md`](notes/boundary-gate.md) · 门禁：`bash scripts/check-layer-deps.sh`

## Context

See `proposal.md` for motivation (1A pixel UI + 2A real three-platform SDKs).

Baseline after archived `migrate-my-ai-to-kmp`: four-tab `AppShell`, soft auth gate, and feature packages exist, but most product domains are **stub/partial** (`MockImEngine`, `StubMediaPlayer`, `StubWebBridgeHost`, `FlaggedPayGateway` off, simulated scan, unwired home→media/web). Flutter truth source: `/Users/mac/Desktop/github/my_ai_project`. Main specs already live under `openspec/specs/*` from the migrate archive sync.

Constraints: single `:composeApp` / `libkn.so` for OHOS; common-first UI; thin `iosApp` / `harmonyApp`; no FanGroup branding.

## Goals / Non-Goals

**Goals:**
- Close stub→real gap for in-scope Flutter product surfaces with pixel acceptance.
- Introduce an authoritative **platform gap registry** artifact updated with every adapter.
- Standardize design tokens to Flutter `AppTheme`.
- Define phased delivery that still forbids claiming “done” when any of A/iOS/OHOS is incomplete.

**Non-Goals:**
- DoKit / `module_bfui` unless product later expands scope.
- Splitting Gradle multi-module if it blocks OHOS `libkn` (logical packages remain OK).
- Inventing backend APIs that Flutter does not use.

## Decisions

### D1 — Acceptance bar (1A / 2A)
- **Choice:** Pixel gate + three-platform real adapters; incomplete target ⇒ capability incomplete + registry row.
- **Alt:** Visual-approx / OHOS stub allowed — rejected by product (user 1A2A).

### D2 — Flutter mock IM
- **Choice:** Match Flutter’s current IM component behavior (today mock Rong wrapper). UI/send/receive must match; vendor SDK upgrade is a follow-up that updates the registry.
- **Alt:** Force native Rong on day one — deferred until Flutter product also switches.

### D3 — Gap registry format
- **Choice:** Single Markdown table `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md` during the change; promote at archive.
- Columns: `capability`, `android`, `ios`, `ohos`, `blocker`, `flutter_ref`, `kmp_ref`, `follow_up`.
- Status enum: `missing | stub | partial | ready`.

### D4 — Pixel comparison method
- **Choice:** Per-screen Flutter vs KMP screenshots on same logical size (phone portrait); checklist in tasks; tolerance = no intentional drift (spacing/color/type/assets).
- **Alt:** Automated screenshot CI — optional later; not a blocker for manual acceptance v1.

### D5 — Token migration
- **Choice:** Replace `DemoColors` values (and shared chrome typography) to Flutter `commons/ui` `AppTheme`; avoid dual palettes.
- **Alt:** Parallel token sets — rejected (drift risk).

### D6 — Platform SDK placement
- **Choice:** Interface in `commonMain` (`PayGateway`, `MediaPlayer`, `WebBridgeHost`, `ScanEngine`, `PushBridge`, `DeepLinkDelivery`); `androidMain` / `iosMain` / `ohosMain` actuals; host apps only configure keys/URL schemes.
- **Alt:** Feature-local SDK calls — rejected (breaks modularity).

### D7 — Delivery phases (implementation order)
1. Tokens + shell/privacy persistence + gap registry bootstrap  
2. Auth remote + Mine/Home IA pixel pass  
3. Bridges (WebView, scan, deeplink delivery)  
4. Community/Chat behavior parity  
5. Media real players + Home wiring  
6. Commerce real pay channels  
7. Friend/Live/Classroom depth  
8. Push + remaining registry closures + full acceptance matrix  

### D8 — Initial gap registry seed (current truth)

| capability | android | ios | ohos | blocker (summary) |
|---|---|---|---|---|
| design tokens vs Flutter | partial | partial | partial | DemoColors ≠ AppTheme |
| privacy persistence | stub | stub | stub | in-memory only |
| remote auth / OTP | stub | stub | stub | demo AuthRepository |
| WeChat login | missing | missing | missing | no fluwx-equivalent |
| home secondaries | partial | partial | partial | placeholders; media/web unwired |
| chat UI | partial | partial | partial | list/detail only |
| IM engine | stub | stub | stub | MockImEngine (Flutter also mock) |
| community publish | stub | stub | stub | labeled stub |
| media playback | stub | stub | stub | StubMediaPlayer |
| WeChat/Alipay pay | stub | stub | stub | flags off / no SDK |
| WebView + JS bridge | stub | stub | stub | StubWebBridgeHost |
| deeplink delivery | partial | partial | partial | parse only |
| scan/camera | stub | stub | stub | simulated |
| push entry | missing | missing | missing | no JPush-equivalent |
| friend/live | stub | stub | stub | mock lists |
| classroom | stub | stub | stub | list only vs Flutter multi-page |
| image_picker | missing | missing | missing | no adapter |
| face verify / BLE / bfui | missing | missing | missing | out of product scope unless added |

## Risks / Trade-offs

- [OHOS SDK gaps] → Registry + fail acceptance; do not ship silent stubs as done; schedule follow-ups explicitly.
- [Pixel cost high] → Prioritize chrome + top golden paths first; reuse Flutter assets where license allows.
- [Pay/push keys & signing] → Local config only; never commit; acceptance blocked until keys provided.
- [Single libkn size/OOM] → Keep adapters lean; OHOS release `optimized=false` remains unless measured.
- [Flutter mock IM confusion] → Distinguish “behavior parity with Flutter mock” vs “vendor SDK ready”.

## Migration Plan

1. Bootstrap registry + tokens (no user-facing break beyond colors).  
2. Replace stubs per phase behind interfaces; remove stub completion claims from UI copy.  
3. Per capability: Android evidence → iOS → OHOS; update registry rows in same PR.  
4. Full matrix sign-off; archive only when in-scope capabilities are `ready`×3 or explicitly incomplete in registry (never falsely marked done).  
5. Rollback: revert per-feature PR; registry row returns to prior status.

## Open Questions

- Exact backend base URLs/credentials for non-demo auth/pay in each environment (needed at apply time).  
- Whether invoice/BLE debug rows in Flutter settings are forced in-scope (default: out-of-scope per proposal).  
- Automated screenshot CI timing (post-manual matrix).
