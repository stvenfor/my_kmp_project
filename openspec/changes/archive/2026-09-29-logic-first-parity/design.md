## Context

See `proposal.md` — Why. Full Parity Phase (ADR 0003) currently defines Route Acceptance Packet with Android Screenshot Diff Gate as a hard seam. Domain tickets #2–#24 closed largely via pixel/SoT evidence; Shared Presentation Logic depth is uneven (many SoT bitmaps, thin engines). Ownership (ADR 0002) stays: Native Shell + Mine Island.

## Goals / Non-Goals

**Goals:**
- Make Logic Acceptance Packet the sole hard acceptance seam for the next phase.
- Prefer shared Kotlin (commonMain feature engines / UiState) as the place logic lives; native shells consume or thin-bridge.
- Give agents a clear stop rule: no new pixel SoT campaigns; no mse gate blocking merges/tickets.
- Keep gap registry honesty and three-platform open-path.

**Non-Goals:**
- Rewriting all native UI to match Flutter chrome.
- Deleting screenshot tooling or past evidence.
- Vendor SDK integrations.
- Changing shell ownership model.

## Decisions

1. **Acceptance pivot (not ownership pivot)**  
   Keep ADR 0002 UI ownership. Change only what “done” means: logic packet > pixel packet for this phase.  
   _Alt considered:_ Pause all parity work — rejected; logic debt is the next ROI.

2. **Logic SoT = Flutter source behavior, not screenshots**  
   Compare against Flutter controllers/repos/mock seeds (e.g. MockIm, community seed, classroom models). Screenshots are informal aids only.  
   _Alt:_ Keep Flutter Android screenshots as SoT — rejected for this phase (encourages bitmap skins).

3. **Where logic lives**  
   Prefer `composeApp` commonMain feature packages (`ImEngine`, community/live/classroom engines, commerce FlaggedPayGateway, etc.). If iOS/OHOS still duplicate mock in Swift/ArkTS, migrate or wrap toward shared Kotlin when touching that domain.  
   _Alt:_ Logic-only in each native shell — rejected (triples work, diverges).

4. **Inventory status**  
   Add `logic-pass`; do not strip historical `packet-pass`. Session note in `parity-inventory.md` + `CONTEXT.md` + ADR 0003 consequence line stating Screenshot Diff Gate deferred.  
   _Alt:_ Reset all rows to `todo` — rejected (loses pixel history).

5. **Domain order (apply phase default)**  
   Prioritize domains where UI is SoT-skinned or logic is thinnest relative to Flutter: classroom, media/short video, AI stream, mall/wallet commerce rules, community/chat engines already partial — audit then deepen. Exact ticket order left to tasks.md checklist.  
   _Alt:_ Random route list — rejected; follow Flutter feature modules.

6. **Tests**  
   Prefer `commonTest` behavioral tests (state transitions, seed counts, toast-only resolve null, pay honesty). Manual three-platform open-path smoke remains part of the packet; no emulator mse requirement.

## Risks / Trade-offs

- [Visual drift grows] → Mitigation: phase is explicit and temporary; pixel gate resumes later; don’t delete evidence.
- [Agents keep shipping SoT bitmaps by habit] → Mitigation: tasks + inventory session note + “Non-goals” in tickets.
- [Native shell still has parallel mocks] → Mitigation: when a domain is touched, prefer shared Kotlin and delete duplicate shell mock if safe.
- [“logic-pass” gamed with empty tests] → Mitigation: each domain task lists Flutter SoT files and required scenarios in tasks.md.

## Migration Plan

1. Land docs/process updates (CONTEXT, ADR 0003 note, inventory session banner, optional #1 comment).
2. Publish Logic Acceptance Packet checklist under `openspec/changes/logic-first-parity/notes/`.
3. Audit inventory domains for logic gaps vs Flutter; fill `logic-pass` progressively.
4. End phase: explicit inventory/CONTEXT flip restoring Screenshot Diff Gate as hard seam (future change).

Rollback: revert docs session notes; resume pixel gate as in ADR 0003 original text.

## Open Questions

- None blocking; first audit domain can be chosen at apply time from tasks priority list.
