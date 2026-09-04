# Flutter parity 解冻政策（尖刀 II 后）

**状态：** 已解冻（任务 4.5）  
**前提：** 后续 parity 工作**只能**在 ADR 0001 五层边界内改；本文件是政策门，不是像素对等实施清单。

尖刀期间对等 UI **冻结**；尖刀 II 收口后解冻。解冻 ≠ 立刻做完整 1A；解冻 = 允许在**新边界**内重新开 parity 任务。

权威边界：`docs/adr/0001-production-architecture.md`、`composeApp/.../MODULES.md`、本 change `design.md`。

---

## 硬规则

| # | 规则 |
|---|------|
| R1 | `feature` ↛ `feature`（禁止 sibling import） |
| R2 | 跨 feature 跳转走 `AppNavigator` / `AppRoute`（`core` / shell），禁止 Home 本地 hub 堆 feature import |
| R3 | 支付 / 推送 / WebView / IM 端口走 `component/*`，禁止 feature 内直接调厂商 SDK |
| R4 | Design tokens 走 `DesignSystem` / `core.design`（含既有 `DemoColors` 桥），禁止 feature 私自再建平行色板 |
| R5 | 合入前跑 `scripts/check-layer-deps.sh`；失败则禁止合入 |

---

## 允许

- 在**单个** `feature/<name>/` 内做 Flutter→KMP UI/行为对等
- 经 `AppNavigator` 注册/消费二级路由；壳层持有导航图
- 扩展 `component/{pay,push,webview,chat}` 的 expect/actual 与沙箱/真机适配
- 在 `core.design` / `DesignSystem` 补 token、typography、chrome
- 更新 `openspec/changes/parity-flutter-to-kmp/` 的 matrix / registry / evidence
- 共享逻辑下沉到 `core/*`（network、account、router、platform）

## 禁止

- 恢复或扩大 `HomeScreen` / Mine hub 对其他 `feature.*` 的 import 网
- feature 互调 UI/Repository/ViewModel
- 在 feature 包内复制 Pay/Push SDK 调用绕过 `component/*`
- 以「对等紧急」绕过 `check-layer-deps.sh`
- 借 parity 名义拆 Gradle 模块或改 `settings.gradle.kts`（属 Modules 轨）
- 借 parity 名义重做 AppShell / 主 Tab 导航图（属 Layer 轨，除非单独批准）

---

## 如何开新 parity 任务

1. **引用本政策** — 新任务 / PR 描述链到本文件 + `parity-flutter-to-kmp/notes/boundary-gate.md`。
2. **划文件边界** — 列出拟改路径；确认无跨 feature import；跨域只经 Navigator / component / core。
3. **验收条** — 像素/行为证据 + `bash scripts/check-layer-deps.sh` 通过。
4. **登记缺口** — 三端未齐或 SDK 未接：写 `platform-gap-registry.md`，勿标 done。
5. **OpenSpec** — 优先在既有 `parity-flutter-to-kmp` 下增补 tasks；大块新域可另开 change，但仍须遵守 R1–R5。

---

## 与 `scripts/check-layer-deps.sh` 的关系

- 脚本扫描 `composeApp/.../feature/**/*.kt`，检测 `import …feature.<other>`。
- **解冻后仍强制**：parity PR 的 CI / 本地门禁必须绿。
- 违规修复方向：抽路由到 `AppNavigator`、共享类型下沉 `core`、桥接下沉 `component`——**不要**关掉脚本或加白名单 habbit。
- 历史 Partial（Home/Mine hub）属 Spike II 遗留；新 parity **不得**新增违规行。

---

## 与 Nice 轨（Flutter 全量 1A）的关系

Must 矩阵将「Flutter full pixel 1A」标为 **Nice**、独立轨。解冻后可继续该轨，但交付节奏与 Must 关门解耦；任何 1A 工作同样遵守本政策。
