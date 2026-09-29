## 1. Catalog & dependency wiring

- [x] 1.1 Bump `coil` in `gradle/libs.versions.toml` from `3.3.0` to `3.3.0-0.3.0`; update comments to state CPF OH line / ohosArm64 (same tone as Ktor)
- [x] 1.2 Confirm eazytec Maven resolves `io.coil-kt.coil3:coil-compose:3.3.0-0.3.0` and `coil-network-ktor3:3.3.0-0.3.0` with `ohosArm64` (dependency insight or module metadata)
- [x] 1.3 In `composeApp/build.gradle.kts`, add intermediate `imageCoilMain` (or equivalent) depending on `commonMain`, with `coil-compose`; wire android (+ okhttp), ios (+ ktor3), `ohosArm64Main` (+ ktor3); keep Coil off `ohosX64` if no variant

## 2. Shared network-image wrapper

- [x] 2.1 Implement shared Compose wrapper in `core.ui` (prefer common/`imageCoilMain`) wrapping Coil `AsyncImage`, preserving `secureNetworkImageUrl`, blank-URL placeholder, and error=placeholder behavior from current android/ios actuals
- [x] 2.2 Keep or thin `PlatformNetworkImage` as the stable call-site API (rename only if needed); provide `ohosX64`/unsupported placeholder actual per design D3
- [x] 2.3 Delete duplicated android/ios Coil bodies once the shared implementation compiles for those targets

## 3. Call-site migration

- [x] 3.1 Grep `PlatformNetworkImage`, `coil3.compose.AsyncImage`, and direct Coil imports; ensure feature/native-shell call sites only use the shared wrapper
- [x] 3.2 Leave all `painterResource` / local drawable usages unchanged
- [x] 3.3 Update AGENTS.md / catalog comments that say “Coil Android/iOS only” to reflect CPF OH arm64 support

## 4. Build & acceptance

- [x] 4.1 `./gradlew :composeApp:compileDebugKotlinAndroid` succeeds
- [x] 4.2 Compile iOS shared target used by the project (narrowest ios compile task available) succeeds
- [x] 4.3 Compile `ohosArm64` and confirm Coil artifacts link; `ohosX64` still compiles with placeholder fallback
- [x] 4.4 Smoke: Home/Community/Friend remote avatars/covers on Android; after `publish*BinariesToHarmonyApp`, visual check remote image on OH arm64 device/emulator
