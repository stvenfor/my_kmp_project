# Phase-1 UI Parity Checklist

Visual Source of Truth: Flutter `my_ai_project` + DESIGN.md / VercelTokens.light  
Bar: ≤2% error (UI Parity Bar)

Architecture wired in commit `f512da4`. Android emulator pass 2026-09-20 (device `emulator-5554`, 1440×3120).

Screenshots: `.scratch/phase-1-native-shell-mine-island/screenshots/{flutter,kmp}/`.

| Surface | Android | Notes vs Flutter SoT |
|---------|---------|----------------------|
| Splash | [x] | System + Compose splash; not pixel-identical to Flutter splash art |
| Privacy | [x] | Shown when consent unset; session already accepted on this emulator |
| Tab shell | [x] | Four tabs 首页/聊天/社区/我的 |
| Home root | [~] | Same greeting, search, banner, 10 services, task cards. Icon art is vector vs Flutter photos; extra analytics block below fold |
| Chat root + gate | [~] | Title「消息」and Mock好友1–3 match SoT copy. Missing Flutter search/compose icons and photo avatars |
| Community root + gate | [~] | Header, search, 最新/热门/关注 present. Feed copy still study-mock, not Flutter coffee/hiking posts |
| Mine Root | [~] | Structure matches (名片、统计、常用服务、个人功能). Avatar/store name depend on session |
| Auth | [x] | Native login overlay; not island |
| Mine Island – Settings | [x] | Opened from gear: 运行环境 / 深色 / 语言 / 会员 / 个性化 / 关于 |
| Shared Design Tokens | [x] | `:core:design` |

iOS / Harmony visual pass not on this Android emulator.

## Legacy Shared Compose removal gate

- [x] Main paths do not call `App()` / `AppShell` on platform entries
- [ ] Delete `AppShell` only after remaining ~ gaps (icons, community feed copy) are accepted


## Legacy Shared Compose removal gate

- [x] Main paths do not call `App()` / `AppShell` on platform entries
- [ ] After visual pass, delete or quarantine unused shell-only CMP (`AppShell` retained as reference per grill Q12=B)

Status: architecture complete 2026-09-20; pixel parity open.
