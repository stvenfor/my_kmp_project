## 1. Process pivot (docs)

- [x] 1.1 Update `CONTEXT.md` Route Acceptance Packet / Parity Domain Ticket glossary: Screenshot Diff Gate deferred; Logic Acceptance Packet is hard gate; add `logic-pass` status meaning
- [x] 1.2 Add ADR 0003 consequence note: logic-first phase active; pixel gate optional until phase ends
- [x] 1.3 Banner `openspec/changes/parity-flutter-to-kmp/notes/parity-inventory.md`: logic-first session; status enum includes `logic-pass`; do not require mse for new completions
- [x] 1.4 Write `openspec/changes/logic-first-parity/notes/logic-acceptance-packet.md` checklist (open path ×3, Flutter behavior SoT paths, commonTest, gap registry, no new SoT bitmaps)
- [x] 1.5 Comment GitHub #1 with strategy pivot summary + link to this change (optional if `gh` available)

## 2. Logic gap audit

- [x] 2.1 Audit chat/friend vs Flutter MockIm (seed, send latency, read receipts) → inventory notes + gap rows
- [x] 2.2 Audit community feed/publish/search vs Flutter mock repo depth
- [x] 2.3 Audit classroom/homework/gift vs Flutter classroom models (not SoT pages alone)
- [x] 2.4 Audit mall/wallet/pay/membership honesty vs Flutter (FlaggedPayGateway, no fake Success)
- [x] 2.5 Audit media/AI/music engines vs Flutter mock (player/SSE remain gap-registered)
- [x] 2.6 Produce prioritized `logic-pass` backlog table under `notes/logic-backlog.md`

## 3. Shared logic hardening (by backlog priority)

- [x] 3.1 Implement highest-priority domain shared Kotlin engine/UiState to Flutter mock depth
- [x] 3.2 Add `commonTest` scenarios for that domain’s Logic Acceptance Packet
- [x] 3.3 Wire Android/iOS/OHOS open path to shared logic (remove duplicate shell mock if safe)
- [x] 3.4 Update inventory row(s) to `logic-pass` + gap registry; no Screenshot Diff Gate required
- [x] 3.5 Repeat 3.1–3.4 for next backlog domains until critical path covered

## 4. Guardrails

- [x] 4.1 Confirm no new `*_body.png` SoT bitmap campaigns landed during this phase (except fixing broken resources)
- [x] 4.2 Confirm `scripts/screenshot_diff_gate.py` still present but undocumented as hard gate in active session notes
