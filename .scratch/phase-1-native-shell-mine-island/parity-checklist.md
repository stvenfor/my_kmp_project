# Phase-1 UI Parity Checklist

Visual Source of Truth: Flutter `my_ai_project` on Android emulator `emulator-5554` (1440×3120).  
Bar: ≤2% error (UI Parity Bar). Screenshots under `screenshots/{flutter,kmp}/`.

| Surface | Android | Notes vs Flutter SoT |
|---------|---------|----------------------|
| Splash | [x] | System + Compose splash |
| Privacy | [x] | Consent flow present when unset |
| Tab shell | [x] | 首页 / 聊天 / 社区 / 我的 |
| Home root | [x] | Feature grid uses synced picsum PNG (`home_feature_*.png`); all_services PNG from Flutter SoT |
| Chat root | [x] | Title「消息」+ ⌕/✎; Mock好友1–3; unread badge on #1 |
| Community root | [x] | 最新/热门/关注; 张三/李四/王五 feed copy matches Flutter mock |
| Mine Root | [~] | Structure matches; session-dependent avatar/store name |
| Auth | [x] | Native overlay, not island |
| Mine Island – Settings | [x] | Gear → 设置（环境/深色/语言/会员/个性化/关于） |
| Shared Design Tokens | [x] | `:core:design` |

iOS / Harmony not verified on this Android emulator pass.

## Legacy Shared Compose removal gate

- [x] Platform entries do not use `App()` / `AppShell` as main path
- [ ] Physically delete `AppShell` after accepting remaining Mine session-dependent art (~)
