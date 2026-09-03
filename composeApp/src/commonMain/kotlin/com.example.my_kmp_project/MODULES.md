# Logical modules (composeApp monomodule)

OHOS KMP currently requires feature/common code to live in `:composeApp` so `ohosArm64`/`ohosX64`
can link a single `libkn.so`. Gradle library extraction is deferred until OHOS-capable
library modules are validated.

Package layout (dependency direction: app → feature → core):

```
com.example.my_kmp_project
├── app/                 # assembler shell
├── core/
│   ├── account/
│   ├── design/
│   ├── network/
│   ├── platform/
│   ├── router/
│   └── ui/
└── feature/
    ├── auth/
    ├── chat/
    ├── community/
    ├── home/
    ├── mine/            # settings/mine surface
    └── shell/
```

Features MUST NOT import sibling feature internals except via shared core contracts.
