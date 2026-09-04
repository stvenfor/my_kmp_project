# Logical modules (composeApp → multi-module)

OHOS KMP currently requires a single `libkn.so`. Strategy: **multi-module on Android/iOS**,
**aggregate/fat link on OHOS**. See `docs/adr/0001-production-architecture.md` and
`docs/architecture/ohos-aggregate.md`.

## Gradle modules (Spike II / 4.3)

```
:composeApp          # app shell + features + components + remaining core.*
:core:network        # HTTP façades, Ktor (android/ios), OHOS cinterop client
:core:account        # AccountFacade + session/privacy stores
:ohosAggregate       # placeholder — libkn.so still linked from :composeApp
```

**Dependency direction (Gradle):**

- `:composeApp` → `:core:account` → `:core:network`
- `:composeApp` → `:core:network`
- `:ohosAggregate` (placeholder, no targets yet)

## Five layers (aligned with tpj-flt semantics)

```
com.example.my_kmp_project
├── app/                 # ① App Shell — assemble only (:composeApp)
├── feature/             # ② product surfaces — no sibling feature imports
├── component/           # ③ vertical bridges — push / webview / pay / chat ports
├── core/                # ④ horizontal commons
│   ├── network/         # → Gradle :core:network
│   ├── account/         # → Gradle :core:account
│   └── router / design / platform / ui  # still in :composeApp packages
└── (platform shells: iosApp / harmonyApp)  # ⑤
```

**Dependency direction (hard):**

- `feature` → `component` / `core` only
- `component` / `core` ↛ `feature` / `app`
- Route constants + `Navigator` owned by shell/core router — not ad-hoc Home hubs
- Flutter parity 解冻后仅可在上述边界内改：见 `openspec/changes/architect-production-foundation/notes/parity-unfreeze.md`

## Features

`auth`, `chat`, `community`, `home`, `mine`, `shell`, `classroom`, `commerce`,
`friend`, `live`, `media`, `scan`, `web` — UI + feature repositories only.

## Intermediate source sets (now owned by core modules)

| Source set | Module | Used by | Purpose |
|------------|--------|---------|---------|
| `networkKtorMain` | `:core:network` | android + ios | Ktor client |
| `accountSettingsMain` | `:core:account` | android + ios | multiplatform-settings KV |

OHOS HTTP lives in `:core:network` (`ohosMain` + `net_http` cinterop). Fat `libkn.so` link remains on `:composeApp` until `:ohosAggregate` takes over.
