## 1. Catalog & dependency wiring

- [x] 1.1 Add `uuid = "0.8.4-1.0.0"` and `uuid = { module = "com.benasher44:uuid", version.ref = "uuid" }` to `gradle/libs.versions.toml` with CPF OH / ohosArm64 / keep-off-ohosX64 comments
- [x] 1.2 In `composeApp/build.gradle.kts`, create intermediate `uuidCpfMain` depending on `commonMain` with `implementation(libs.uuid)`; wire `androidMain`, `iosMain`, and `ohosArm64Main` to it; do not attach `ohosX64Main`
- [x] 1.3 Add `uuidCpfMain` row to the intermediate source-set table in `AGENTS.md`

## 2. Thin facade

- [x] 2.1 Add `expect object AppUuid` (or equivalent) in `composeApp` `commonMain` under `core` with `random(): String` and `parseOrNull(value: String): String?`
- [x] 2.2 Implement CPF `actual` on `uuidCpfMain` using `com.benasher44.uuid` (v4 random + parse → canonical lowercase string)
- [x] 2.3 Implement documented stub `actual` for `ohosX64` so the expect resolves without CPF klib

## 3. Smoke tests

- [x] 3.1 Add `commonTest` covering non-blank, `8-4-4-4-12` lowercase hex shape, round-trip equality, and two successive `random()` values differing
- [x] 3.2 Grep for direct `com.benasher44` imports outside the CPF actual file; none allowed in feature code

## 4. Build & acceptance

- [x] 4.1 `./gradlew :composeApp:testDebugUnitTest` passes (includes UUID smoke)
- [x] 4.2 `./gradlew :composeApp:compileDebugKotlinAndroid` succeeds
- [x] 4.3 Narrowest available iOS compile task for `:composeApp` succeeds
- [x] 4.4 Compile `ohosArm64` with UUID resolved; if `-1.0.0` ABI fails, fall back catalog pin to `0.8.4-0.3.0` and re-verify (document in catalog comment)
