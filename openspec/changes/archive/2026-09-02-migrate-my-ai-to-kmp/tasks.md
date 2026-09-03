## 1. Modular skeleton

- [x] 1.1 Create Gradle modules `:core:network`, `:core:account`, `:core:design`, `:core:router`, `:core:platform` and wire `settings.gradle.kts` + version catalog
- [x] 1.2 Move existing façades/immersive helpers into core modules; keep `:composeApp` as assembler + platform entries
- [x] 1.3 Add empty `:feature:shell`, `:feature:auth`, `:feature:home`, `:feature:chat`, `:feature:community`, `:feature:settings` modules with dependency rules (features → cores only)
- [x] 1.4 Verify `./gradlew :composeApp:compileDebugKotlinAndroid` still succeeds after the move

> Note: OHOS single-`libkn` constraint → **logical package modules** inside `:composeApp` (see `MODULES.md`) instead of separate Gradle libraries for this slice.

## 2. App shell & three-platform chrome

- [x] 2.1 Replace Demo two-tab shell with four-tab shell (Home / Chat / Community / Mine) using shared router registration
- [x] 2.2 Implement splash → privacy consent → main flow without requiring login
- [x] 2.3 Soft-auth gate Chat/Community; pending destination resume after login
- [x] 2.4 Ensure immersive insets + hide tab bar on secondary pages on Android/iOS/OHOS
- [x] 2.5 Smoke: same splash→Home path on Android; publish OHOS binaries and spot-check shell; iOS simulator shell smoke

## 3. Auth & session

- [x] 3.1 Port session model + persistence via `:core:account` (Settings on A/iOS; OHOS actual)
- [x] 3.2 Implement login (password + OTP) and register UI/domain against shared network façade
- [x] 3.3 Wire force-logout / 401 handling to clear session and re-apply tab gates
- [x] 3.4 Unit/integration tests for envelope + session clear behavior where feasible

## 4. Home & Settings features

- [x] 4.1 Migrate Home root IA and primary entries from Flutter `module_home`
- [x] 4.2 Migrate key Home secondary destinations (all services, search, learning report, strategy hubs)
- [x] 4.3 Migrate Mine root + settings / personalized settings from `module_settings`
- [x] 4.4 Show effective API host/environment in Mine/Settings
- [x] 4.5 Parity checklist: Home + Mine primary flows on Android/iOS/OHOS

## 5. Community & Chat (mock IM)

- [x] 5.1 Define `ImEngine` interface + mock engine module
- [x] 5.2 Migrate Community feed, publish entry, image preview
- [x] 5.3 Migrate Chat list + detail on mock engine
- [x] 5.4 Confirm guest gate + authenticated flows; Android compile + OHOS/iOS smoke

## 6. Media (video / music)

- [x] 6.1 Define shared player abstractions + Android/iOS/OHOS adapters (stub OK where SDK missing)
- [x] 6.2 Migrate video hub / short-video entry screens
- [x] 6.3 Migrate music list + now playing + Home mini-player affordance
- [x] 6.4 Parity checklist for play/pause/exit on available targets; document OHOS gaps if any

## 7. Commerce & platform bridges

- [x] 7.1 Define `PayGateway` + channel availability states; migrate membership/pay UI
- [x] 7.2 Integrate WeChat/Alipay adapters per target behind flags
- [x] 7.3 Shared WebView host + JS bridge method set used by Home H5 entries
- [x] 7.4 Deep link / push entry routing into shared nav graph
- [x] 7.5 Scan/QR capability with permission failure UX

## 8. Remaining product modules & polish

- [x] 8.1 Migrate friend / live / classroom feature surfaces using established patterns
- [x] 8.2 Remove Demo-only placeholder copy; align app display name (keep package id unless release decides otherwise)
- [x] 8.3 Full three-platform regression against app-shell + auth + each migrated feature checklist
- [x] 8.4 `openspec validate --change migrate-my-ai-to-kmp` clean; prepare archive notes per completed capability
