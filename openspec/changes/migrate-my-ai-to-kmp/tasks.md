## 1. Modular skeleton

- [ ] 1.1 Create Gradle modules `:core:network`, `:core:account`, `:core:design`, `:core:router`, `:core:platform` and wire `settings.gradle.kts` + version catalog
- [ ] 1.2 Move existing façades/immersive helpers into core modules; keep `:composeApp` as assembler + platform entries
- [ ] 1.3 Add empty `:feature:shell`, `:feature:auth`, `:feature:home`, `:feature:chat`, `:feature:community`, `:feature:settings` modules with dependency rules (features → cores only)
- [ ] 1.4 Verify `./gradlew :composeApp:compileDebugKotlinAndroid` still succeeds after the move

## 2. App shell & three-platform chrome

- [ ] 2.1 Replace Demo two-tab shell with four-tab shell (Home / Chat / Community / Mine) using shared router registration
- [ ] 2.2 Implement splash → privacy consent → main flow without requiring login
- [ ] 2.3 Soft-auth gate Chat/Community; pending destination resume after login
- [ ] 2.4 Ensure immersive insets + hide tab bar on secondary pages on Android/iOS/OHOS
- [ ] 2.5 Smoke: same splash→Home path on Android; publish OHOS binaries and spot-check shell; iOS simulator shell smoke

## 3. Auth & session

- [ ] 3.1 Port session model + persistence via `:core:account` (Settings on A/iOS; OHOS actual)
- [ ] 3.2 Implement login (password + OTP) and register UI/domain against shared network façade
- [ ] 3.3 Wire force-logout / 401 handling to clear session and re-apply tab gates
- [ ] 3.4 Unit/integration tests for envelope + session clear behavior where feasible

## 4. Home & Settings features

- [ ] 4.1 Migrate Home root IA and primary entries from Flutter `module_home`
- [ ] 4.2 Migrate key Home secondary destinations (all services, search, learning report, strategy hubs)
- [ ] 4.3 Migrate Mine root + settings / personalized settings from `module_settings`
- [ ] 4.4 Show effective API host/environment in Mine/Settings
- [ ] 4.5 Parity checklist: Home + Mine primary flows on Android/iOS/OHOS

## 5. Community & Chat (mock IM)

- [ ] 5.1 Define `ImEngine` interface + mock engine module
- [ ] 5.2 Migrate Community feed, publish entry, image preview
- [ ] 5.3 Migrate Chat list + detail on mock engine
- [ ] 5.4 Confirm guest gate + authenticated flows; Android compile + OHOS/iOS smoke

## 6. Media (video / music)

- [ ] 6.1 Define shared player abstractions + Android/iOS/OHOS adapters (stub OK where SDK missing)
- [ ] 6.2 Migrate video hub / short-video entry screens
- [ ] 6.3 Migrate music list + now playing + Home mini-player affordance
- [ ] 6.4 Parity checklist for play/pause/exit on available targets; document OHOS gaps if any

## 7. Commerce & platform bridges

- [ ] 7.1 Define `PayGateway` + channel availability states; migrate membership/pay UI
- [ ] 7.2 Integrate WeChat/Alipay adapters per target behind flags
- [ ] 7.3 Shared WebView host + JS bridge method set used by Home H5 entries
- [ ] 7.4 Deep link / push entry routing into shared nav graph
- [ ] 7.5 Scan/QR capability with permission failure UX

## 8. Remaining product modules & polish

- [ ] 8.1 Migrate friend / live / classroom feature surfaces using established patterns
- [ ] 8.2 Remove Demo-only placeholder copy; align app display name (keep package id unless release decides otherwise)
- [ ] 8.3 Full three-platform regression against app-shell + auth + each migrated feature checklist
- [ ] 8.4 `openspec validate --change migrate-my-ai-to-kmp` clean; prepare archive notes per completed capability
