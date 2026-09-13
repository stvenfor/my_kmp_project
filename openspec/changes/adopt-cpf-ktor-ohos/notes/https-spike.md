# HTTPS / engine spike notes (task 3.x)

Status: **gate passed** (user-confirmed CIO HTTPS on arm64 device).

## Host checks

| Check | Result |
|-------|--------|
| Maven `ktor-client-core` / `cio` `3.3.3-0.3.0` `ohosArm64` | HTTP 200 |
| `:core:network:testDebugUnitTest` | pass |
| `compileKotlinOhosArm64` / `compileKotlinOhosX64` | pass (post-retire) |

## Device checklist (3.1) — confirmed

| Field | Value |
|-------|-------|
| Date | 2026-09-13 |
| Engine | **CIO** (Curl not needed) |
| HTTPS business URL OK? | **yes** (user) |
| Auth-sensitive OK? | **yes** (user implied / CIO usable) |
| Curl path (3.2) | **N/A** — skipped |
| Notes | Proceeded to 3.4 + 4.x: arm64 Ktor-only; x64 keeps cinterop |

## Post-gate (3.4 / 4.x)

- ohosArm64: `KtorApiClient` + CIO only; `OhosArm64NetworkSwitch` removed
- ohosX64: still `OhosApiClient` + `net_http` cinterop (no CPF Ktor x64 klib)
- `:composeApp` links `-lnet_http` **only** for `ohosX64`
