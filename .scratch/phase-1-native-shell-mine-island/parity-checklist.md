# Phase-1 UI Parity Checklist

Visual Source of Truth: Flutter `my_ai_project` + DESIGN.md / VercelTokens.light  
Bar: ≤2% error (UI Parity Bar)

Architecture wired in commit `f512da4`. Checkboxes below are **human visual pass**.

| Surface | Android | iOS | Harmony | Notes |
|---------|---------|-----|---------|-------|
| Splash | [ ] | [ ] | [ ] | Native shells |
| Privacy | [ ] | [ ] | [ ] | |
| Tab shell | [ ] | [ ] | [ ] | |
| Home root | [ ] | [ ] | [ ] | Android still embeds CMP `HomeScreen` body; iOS/Harmony structural native |
| Chat root + gate | [ ] | [ ] | [ ] | |
| Community root + gate | [ ] | [ ] | [ ] | |
| Mine Root (native) | [ ] | [ ] | [ ] | Not island |
| Auth login/register | [ ] | [ ] | [ ] | Android uses existing LoginScreen; iOS/Harmony demo login |
| Mine Island – Settings | [ ] | [ ] | [ ] | Shared CMP |
| Mine Island – Personalized | [ ] | [ ] | [ ] | |
| Mine Island – About | [ ] | [ ] | [ ] | |
| Shared Design Tokens | [x] | [x] | [x] | `:core:design` + Swift/ArkTS mirrors |

## Legacy Shared Compose removal gate

- [x] Main paths do not call `App()` / `AppShell` on platform entries
- [ ] After visual pass, delete or quarantine unused shell-only CMP (`AppShell` retained as reference per grill Q12=B)

Status: architecture complete 2026-09-20; pixel parity open.
