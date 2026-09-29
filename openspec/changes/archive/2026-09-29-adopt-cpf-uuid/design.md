## Context

See `proposal.md` — Why. Current inventory:

| Item | State |
|------|--------|
| UUID dependency | None in catalog / `composeApp` |
| Call sites | None (greenfield wire-up) |
| CPF Maven | `com.benasher44:uuid:0.8.4-1.0.0` with `uuid-jvm`, `uuid-ios*`, `uuid-ohosarm64`; **no** `uuid-ohosx64` |
| Source pattern | Coil uses `imageCoilMain` for android+ios+ohosArm64; same constraint applies |

Source: [CPF-KMP-CMP/uuid](https://gitcode.com/CPF-KMP-CMP/uuid) (`main-0.8.4-OH`).

## Goals / Non-Goals

**Goals:**

- Pin CPF uuid `0.8.4-1.0.0` in catalog; resolve on Android / iOS / ohosArm64.
- One thin facade in `core` (generate + string round-trip); features call only the facade.
- `commonTest` smoke for shape / round-trip / uniqueness.
- Keep ohosX64 from breaking primary-target acceptance.

**Non-Goals:**

- New Gradle module; UUID v1/v6/v7 product rules; shell-layer (Swift/ArkTS) APIs.
- Migrating hypothetical future IDs in network payloads.

## Decisions

### D1 — Version pin: `0.8.4-1.0.0`

- **Choice:** User-selected latest CPF line on eazytec (`0.8.4-1.0.0`).
- **Alternatives:** `0.8.4-0.3.0` (align Ktor/Coil `-0.3.0`); plain `0.8.4` (no OH).
- **Note:** Git branch `main-0.8.4-OH` ≠ Maven suffix; use catalog pin `0.8.4-1.0.0`. Kotlin ABI of this artifact was built with `2.2.21-0.3.0` tooling metadata — acceptable for this land; if link/ABI errors appear, fall back to `0.8.4-0.3.0` in a follow-up.

### D2 — Dependency placement: intermediate `uuidCpfMain` (not bare commonMain)

- **Choice:** Mirror Coil — create `uuidCpfMain` depending on `commonMain`, with `implementation(libs.uuid)`. Wire:
  - `androidMain` → `uuidCpfMain`
  - `iosMain` → `uuidCpfMain`
  - `ohosArm64Main` → `uuidCpfMain`
  - `ohosX64Main` → **not** on `uuidCpfMain`
- **Why not commonMain:** missing `ohosX64` klib would fail OH x64 metadata resolution the same way Coil would.
- **Alternatives:** commonMain-only (breaks ohosX64); expect/actual with three copies of random logic (rejected — defeats CPF adoption).

### D3 — Facade shape: expect in commonMain, actual on uuidCpfMain + ohos stub

Public API (names indicative):

```kotlin
// commonMain
expect object AppUuid {
    fun random(): String
    fun parseOrNull(value: String): String?
}
```

| Source set | Actual |
|------------|--------|
| `uuidCpfMain` | `com.benasher44.uuid.Uuid` / `uuid4()` → lowercase `toString()`; parse via library |
| `ohosX64Main` (or `ohosMain` if only x64 lacks CPF) | Minimal stub: random 8-4-4-4-12 hex via `kotlin.random`; parse regex/normalize — **document as non-CPF**, only so the facade type exists if ohosX64 still compiles |

Prefer putting the CPF actual in `uuidCpfMain` so android/ios/ohosArm64 share one file. If Gradle requires an `actual` per leaf, duplicate is a last resort (ponytail: one shared actual file on the intermediate set).

Do **not** re-export `com.benasher44.uuid.Uuid` as the public type — keep `String` (or a tiny value type wrapping String) so call sites stay library-agnostic.

### D4 — API surface (minimal)

| Function | Behavior |
|----------|----------|
| `random()` | UUID v4 string, lowercase, hyphenated |
| `parseOrNull(value)` | Trim; invalid → `null`; valid → canonical lowercase string |

Skip: bytes API, nameUUIDFromBytes, custom RNG injection (YAGNI until a call site needs them).

### D5 — Smoke tests

- File under `composeApp/src/commonTest/.../core/...UuidTest.kt` (or adjacent).
- Assert: non-blank; matches `^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$`; round-trip; two `random()` values differ.
- Run via `./gradlew :composeApp:testDebugUnitTest` (Android host). No need for OH device tests in this change.

### D6 — Catalog / docs touchpoints

- `gradle/libs.versions.toml`: `uuid = "0.8.4-1.0.0"` + `uuid = { module = "com.benasher44:uuid", version.ref = "uuid" }` with short CPF comment (ohosArm64; keep off ohosX64/commonMain).
- Optional one-line note in `AGENTS.md` Map / intermediate source sets table — only if tasks include docs; prefer catalog comment alone unless AGENTS already lists intermediates (it does — add `uuidCpfMain` row).

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `-1.0.0` ABI vs project Kotlin `-0.3.0` mismatch | Compile android + ohosArm64 early; fallback pin `0.8.4-0.3.0` if needed |
| ohosX64 stub drifts from CPF | Document; do not use stub path for product acceptance; arm64 is SoT |
| Call sites import benasher44 directly later | Spec + thin facade; grep `com.benasher44` in apply |
| Empty files list in root `.module` native variants | Artifacts live under `uuid-<target>` coordinates; Gradle metadata still resolves via root module |

## Migration Plan

1. Catalog + `uuidCpfMain` wiring.
2. expect/actual facade + ohosX64 stub.
3. commonTest smoke; `testDebugUnitTest`.
4. Compile check: Android assemble or unit test; narrow OH compile if environment allows (`compileKotlinOhosArm64` or link task).
5. Rollback: revert catalog + source-set + facade files (no call-site migration yet).

## Open Questions

- None blocking. If `-1.0.0` fails ABI on this toolchain, pin moves to `0.8.4-0.3.0` without changing the facade contract.
