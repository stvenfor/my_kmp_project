# Home hub → shell navigation migration

**Date:** 2026-09-04  
**Owner:** Agent-Layer  
**Goal:** Clear `feature/home` → sibling feature imports; prove shell `shellRoute` overlay is extensible.

## Migrated destinations (Home string → `AppRoute`)

| Home key | `AppRoute` | Host |
|----------|------------|------|
| `scan` | `AppRoute.Scan` | `AppShell` → `ScanScreen` |
| `media` | `AppRoute.Media` | `AppShell` → `MediaEntryScreen` |
| `live` | `AppRoute.Live` | `AppShell` → `LiveScreen` |
| `friend` | `AppRoute.Friend` | `AppShell` → `FriendScreen` |
| `classroom` | `AppRoute.Classroom` | `AppShell` → `ClassroomScreen` |
| `web` | `AppRoute.InAppWeb(OfflineWebFixtureUrl)` | already shell-hosted |
| `services` | `AppRoute.AllServices` | already shell-hosted |

## Still local to `feature/home`

`search` → `HomeSearchScreen`, `report` → `LearningReportScreen`, `strategy` → `StrategyScreen` (same package; not layer violations).

## Fixture move

`OfflineWebFixtureUrl` moved from `feature/web` → `component/webview/WebViewComponent.kt` so Home no longer imports `feature.web`.

## Pattern

Same as existing Membership / AllServices / InAppWeb: `LocalAppNavigator.navigate` → `MainShell` binds route → `shellRoute` overlay → hide bottom bar → `clearShellRoute()` on back.
