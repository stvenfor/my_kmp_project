## Context

See `proposal.md` — Why. Current inventory:

| Path | Role |
|------|------|
| `core/ui/PlatformNetworkImage.kt` | `expect` + `secureNetworkImageUrl` |
| `*.android.kt` / `*.ios.kt` | Coil3 `AsyncImage` + placeholder |
| `*.ohos.kt` | Placeholder only (`url` unused) |
| Catalog `coil = "3.3.0"` | Stock Central; no `ohosArm64` |
| Call sites | `HomeRouteHost`, `CommunityRouteHost`, `FriendScreen`, `NativeJetpackTabs` |

Local `painterResource` usage is widespread and out of scope. CPF Maven already serves `io.coil-kt.coil3:coil-compose:3.3.0-0.3.0` / `coil-network-ktor3:3.3.0-0.3.0` with `ohosArm64` (probed 200). Ktor is already `3.3.3-0.3.0` on OH arm64 + CIO.

## Goals / Non-Goals

**Goals:**

- One shared Compose wrapper for remote URLs used by all features.
- CPF Coil on Android / iOS / ohosArm64 with catalog pin `3.3.0-0.3.0`.
- Collapse duplicated android/ios actuals; OH arm64 uses real Coil.
- Keep `secureNetworkImageUrl` behavior.

**Non-Goals:**

- Custom `ImageLoader` DI / singleton factory unless Coil OH requires it for Ktor engine sharing.
- Migrating local assets to Coil.
- ohosX64 Coil if Maven has no variant (placeholder fallback).

## Decisions

### D1 — Version pin: `3.3.0-0.3.0` (not `-1.0.0`, not plain `3.3.0`)

- **Choice:** Align with Kotlin/CMP/Ktor `-0.3.0` line already in catalog.
- **Alternatives:** Stay `3.3.0` (no OH); jump `3.3.0-1.0.0` (needs toolchain bump, rejected like Ktor Option A).
- **Note:** Git branch `main-3.3.0-OH` ≠ Maven version; use suffixed Maven coords.

### D2 — Wrapper shape: thin Compose API, keep name or alias

- **Choice:** Prefer a single `@Composable` in `commonMain` named `NetworkImage` (or keep `PlatformNetworkImage` as the public name) wrapping Coil `AsyncImage`, plus keep `secureNetworkImageUrl` as a shared helper. User-facing “工具类” = this wrapper + URL helper, not a non-Compose `object XxxUtil`.
- **Alternatives:** Keep three-way expect/actual forever (works but duplicates android/ios); put Coil only on an intermediate source set without common API (worse for features).
- **Migration:** Update call sites to the shared symbol; delete redundant platform files when commonMain can see Coil.

### D3 — Dependency placement: intermediate source set vs commonMain

- **Choice (preferred):** Introduce `imageCoilMain` (or put Coil on `commonMain` if all shipped targets resolve). Wire:
  - Android: `coil-compose` + `coil-network-okhttp`
  - iOS + ohosArm64: `coil-compose` + `coil-network-ktor3`
  - ohosX64: no Coil dep; thin actual or expect split only for that target
- **Why not blanket commonMain first:** ohosX64 historically lacks CPF klibs (same pattern as Ktor D4). If Gradle metadata proves Coil also has no ohosX64, keep Coil off pure commonMain and use an intermediate set that android/ios/ohosArm64 depend on; leave placeholder actual on ohosX64/ohosMain-without-arm64.
- **Ponytail:** Reuse existing `PlatformNetworkImage` surface; do not invent a second parallel API.

### D4 — Network engine mapping

| Target | Coil network artifact | Notes |
|--------|----------------------|--------|
| Android | `coil-network-okhttp` **stock `3.3.0`** | CPF `3.3.0-0.3.0` okhttp module is metadata-only (no androidJvm) |
| iOS | `coil-network-ktor3` CPF `3.3.0-0.3.0` | Current |
| ohosArm64 | `coil-network-ktor3` CPF `3.3.0-0.3.0` | Shares CPF Ktor stack |

`coil-compose` uses CPF `3.3.0-0.3.0` on all Coil targets (has Android `release*` + ios + ohosArm64).

Do **not** force Coil to share the app `HttpClient` instance unless OH docs require it; default Coil Ktor networking is enough for v1.

### D5 — Replace scope

Replace **only** network-image paths:

1. Implement shared wrapper.
2. Point existing `PlatformNetworkImage` call sites at it (rename in place if API identical).
3. Remove android/ios duplicate actual bodies once common implementation exists.
4. Leave all `painterResource` call sites untouched.

### D6 — Acceptance evidence

- Android: compile + spot-check avatar/cover screens.
- iOS: same.
- OH arm64: resolve insight for `coil-compose` ohosArm64; publish binaries; visual check Community/Home remote images.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Coil OH + Ktor version skew | Pin Coil `3.3.0-0.3.0` with Ktor `3.3.3-0.3.0`; run dependency insight before merge |
| Binary size / link time on Harmony | Measure after publish; keep Curl unused for Coil |
| ohosX64 placeholder regression vs arm64 | Document in AGENTS/catalog comments; gate demos on arm64 |
| Call site miss (native shell) | Grep `AsyncImage` / `PlatformNetworkImage` / `coil3` before done |
| HTTPS failures on OH image hosts | Reuse ATS upgrade; fail → placeholder per spec |

## Migration Plan

1. Bump catalog Coil → `3.3.0-0.3.0`; wire OH arm64 deps.
2. Land shared wrapper; keep expect/actual as thin shims if needed for ohosX64.
3. Switch call sites; delete dead platform files.
4. Compile android + ios + ohosArm64; publish Harmony; visual smoke.
5. Rollback: revert catalog + restore placeholder ohos actual (single commit revert friendly).

## Open Questions

- None blocking: default Coil Ktor client on OH is acceptable for first land; sharing app `HttpClient` can be a follow-up if auth cookies are ever required for images.
