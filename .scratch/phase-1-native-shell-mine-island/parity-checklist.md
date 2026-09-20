# Phase-1 UI Parity Checklist

Visual Source of Truth: Flutter `my_ai_project` + DESIGN.md / VercelTokens.light  
Bar: ≤2% error (UI Parity Bar)

| Surface | Android | iOS | Harmony | Notes |
|---------|---------|-----|---------|-------|
| Splash | [ ] | [ ] | [ ] | |
| Privacy | [ ] | [ ] | [ ] | |
| Tab shell | [ ] | [ ] | [ ] | |
| Home root | [ ] | [ ] | [ ] | Deferred entries → stub |
| Chat root + gate | [ ] | [ ] | [ ] | |
| Community root + gate | [ ] | [ ] | [ ] | |
| Mine Root (native) | [ ] | [ ] | [ ] | Not CMP |
| Auth login/register | [ ] | [ ] | [ ] | Native; not island |
| Mine Island – Settings | [ ] | [ ] | [ ] | CMP island |
| Mine Island – Personalized | [ ] | [ ] | [ ] | |
| Mine Island – About | [ ] | [ ] | [ ] | |
| Shared Design Tokens | [x] | [x] | [x] | `:core:design` + platform mirrors |

## Legacy Shared Compose removal gate

- [ ] Main paths do not call `App()` / `AppShell` on any platform entry
- [ ] After parity checkboxes above, delete or quarantine unused shell-only CMP if no longer referenced

Status: ready for human visual pass; architecture wired 2026-09-20.
