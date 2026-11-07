# 证据索引 · parity-flutter-to-kmp

> 唯一执行账本：[sync-flutter-full-parity/tasks-ultra-fine-v2.md](../../../sync-flutter-full-parity/tasks-ultra-fine-v2.md)。[tasks-execution.md](../../tasks-execution.md) 与 [tasks-fine.md](../../tasks-fine.md) 为历史账本，仅用于查漏，不再驱动执行；能力缺口见 [platform-gap-registry.md](../platform-gap-registry.md)。
>
> 状态词只用 `Migrated` / `Degraded` / `Placeholder` / `Deferred` / `Partial`（本页指**证据成熟度**，非能力判定）。**页面可开 ≠ 数据契约已接线**：路由能打开静态页不等于能力 `Migrated`。

本页按 **模块 / 平台 / 路由** 登记 `notes/evidence/` 下已有证据与缺口；所有链接均指向本仓库内已存在文件，无悬空链接、无外链。证据以 `<route>.{flutter|kmp}.png` 截图、`<route>.diff.json` 像素差异、`module-*.md` 结论为主（命名约定见 [pixel-acceptance-sop.md](../pixel-acceptance-sop.md)）。

| 状态词 | 含义（证据口径） |
|---|---|
| `Migrated` | 三端证据 + 数据/逻辑证据齐 |
| `Degraded` | 有证据但仅 mock / 壳，不足以判能力 |
| `Placeholder` | 仅静态页或单点截图，背后无数据通路 |
| `Deferred` | 无对应证据，已排期 / 延后 |
| `Partial` | 有 Android 证据，iOS/OHOS 或缺数据契约证据 |

## 1. 按模块（module）

`A/i/O` = Android / iOS / OHOS 是否存在该模块的证据子目录。`代表证据` 均为已存在文件。

| 模块 | A/i/O | 证据状态 | 代表证据（已存在） | 登记缺口 |
|---|---|---|---|---|
| ai | ✓/✓/✓ | `Placeholder` | [ai/Android/ai_stream.kmp.png](./ai/Android/ai_stream.kmp.png) | SSE 传输已落但 `AiStreamScreen` 仍 mock，UI 未接线（见 registry「AI stream / SSE 流式传输」） |
| auth | ✓/—/— | `Partial` | [auth/Android/login_password.diff.json](./auth/Android/login_password.diff.json) | iOS/OHOS 登录证据缺；public register 受 Supabase 配额限制 |
| bridges | ✓/✓/✓ | `Partial` | [bridges/Android/deeplink_chat.png](./bridges/Android/deeplink_chat.png) | OHOS camera 缺；deeplink 仅路由可达，数据契约另评 |
| chat | ✓/✓/✓ | `Partial` | [chat/Android/chat_detail.diff.json](./chat/Android/chat_detail.diff.json) | IM 仍 MockImEngine；厂商 SDK 未接 |
| classroom | ✓/✓/✓ | `Partial` | [classroom/Android/classroom_gift_claim.diff.json](./classroom/Android/classroom_gift_claim.diff.json) | 部分路由仍是 SoT chrome；realtime classroom 未接 |
| commerce | ✓/✓/✓ | `Partial` | [commerce/Android/channel_unavailable.png](./commerce/Android/channel_unavailable.png) | 真 SDK 支付未接；UI 保持不可用态 |
| community | ✓/✓/✓ | `Partial` | [community/Android/community_convention.diff.json](./community/Android/community_convention.diff.json) | 仍 MockCommunityEngine；live feed 未接 |
| content | ✓/—/— | `Deferred` | [content/Android/module-J.md](./content/Android/module-J.md) | 仅 1 份 Android 结论；无三端证据 |
| contract | ✓/—/— | `Deferred` | [contract/Android/module-23.md](./contract/Android/module-23.md) | 仅 1 份 Android 结论 |
| engineering | ✓/—/— | `Deferred` | [engineering/Android/module-K.md](./engineering/Android/module-K.md) | 仅 1 份 Android 结论 |
| friend | ✓/✓/✓ | `Partial` | [friend/Android/friend.diff.json](./friend/Android/friend.diff.json) | FriendDirectory ↔ ImEngineStore 为 mock peer；关系/厂商 SDK 未接 |
| harness | ✓/—/— | `Deferred` | [harness/Android/home.diff.json](./harness/Android/home.diff.json) | 门禁 tooling 对照图，非能力证据 |
| home | ✓/✓/✓ | `Partial` | [home/Android/home_after_sales.diff.json](./home/Android/home_after_sales.diff.json) | 二级详情/创建子路由在 iOS/OHOS 落 `genericList` 占位 |
| live | ✓/✓/✓ | `Partial` | [live/Android/live_room.diff.json](./live/Android/live_room.diff.json) | list→room 为 mock；无 realtime/push 流 |
| mall | ✓/—/— | `Partial` | [mall/Android/mall_detail.diff.json](./mall/Android/mall_detail.diff.json) | iOS/OHOS 证据缺 |
| media | ✓/✓/✓ | `Partial` | [media/Android/module-21.md](./media/Android/module-21.md) | video Surface/decode TBD；OHOS MediaKit 未接 |
| mine | ✓/✓/✓ | `Partial` | [mine/Android/mine_addresses_edit.diff.json](./mine/Android/mine_addresses_edit.diff.json) | Mine 设置岛仍 CMP；部分二级数据未接 |
| music | ✓/—/— | `Partial` | [music/Android/music_list.diff.json](./music/Android/music_list.diff.json) | iOS/OHOS 证据缺；OHOS 播放器仅占位 |
| shell | ✓/✓/✓ | `Partial` | [shell/Android/01_splash_or_privacy.png](./shell/Android/01_splash_or_privacy.png) | 四 Tab 壳三端可进；像素精修待迭代 |
| sot-flutter | —(基准) | `Migrated` | [sot-flutter/01-launch.png](./sot-flutter/01-launch.png) | Flutter SoT 基准截图（70 份），仅作对照源，不代表 KMP 能力 |
| video | ✓/—/— | `Partial` | [video/Android/video_short_help.diff.json](./video/Android/video_short_help.diff.json) | iOS/OHOS 证据缺；真播放器 surfaces 未接 |
| wallet | ✓/—/— | `Partial` | [wallet/Android/pay.diff.json](./wallet/Android/pay.diff.json) | iOS/OHOS 证据缺 |
| web | ✓/✓/✓ | `Partial` | [web/Android/web.diff.json](./web/Android/web.diff.json) | OHOS WebView 仅占位；三端仅离线 fixture |

> 模块级结论：绝大多数模块为 `Partial`——有 Android 像素 / `diff.json` 证据，但 iOS/OHOS 证据或数据契约证据未齐。`sot-flutter` 的 `Migrated` 仅表示 Flutter 基准截图已登记，不构成 KMP 能力判定；`Deferred` 行仅 1 份 Android 结论，按缺口登记。

## 2. 按平台（platform）

- **Android**：23 个模块均有证据目录，覆盖最广；`diff.json` / `heat.png` 像素回归主要集中在此。三端齐备示例见 [web/Android/web.diff.json](./web/Android/web.diff.json)、[web/iOS/web.kmp.png](./web/iOS/web.kmp.png)、[web/OHOS/web.kmp.png](./web/OHOS/web.kmp.png)。
- **iOS**：13 个模块有 `iOS/` 子目录（ai / bridges / chat / classroom / commerce / community / friend / home / live / media / mine / shell / web）；`NativeSecondaryPages.swift` 的 65 个 Native 页为静态元组，页面可开 ≠ 数据契约接线。分发守卫 `scripts/check-ios-page-dispatch.sh`（route→resolve→guard→struct）。
- **OHOS**：13 个模块有 `OHOS/` 子目录（同上 13 个）；Native pane 分发守卫 `scripts/check-harmony-pane-dispatch.sh`（route→id→render）。camera / WebView / MediaKit 仍为 `Deferred`。
- **平台缺口**：auth / content / contract / engineering / harness / mall / music / video / wallet 共 9 个模块缺 iOS/OHOS 平台子目录；`sot-flutter` 为无平台子目录的 Flutter 基准。

## 3. 按路由 / 缺口（route & gap）

| 路由 / 范围 | 状态 | 登记缺口 | 依据（已存在） |
|---|---|---|---|
| 65 条 Native 二级路由（如 `/mine/addresses`、`/home/ledger/detail`） | `Placeholder` | 页面可开 ≠ 数据契约接线：iOS `NativeSecondaryPages.swift` / OHOS `NativeFeaturePane` 为静态元组，无 store/API | [platform-gap-registry.md](../platform-gap-registry.md)「in-scope RoutePath UI (78)」；守卫 `scripts/check-ios-page-dispatch.sh` / `scripts/check-harmony-pane-dispatch.sh` |
| `/video/dubbing/videos` 等 dubbing 系列路由 | `Deferred` | Flutter 有、KMP `core/router` 无（drift 未清零，KMP 缺路由侧） | `scripts/audit-flutter-drift.sh` §(c)；暂未接阻塞门禁，缺口登记在 v2 |
| `/mine/settings`、`/scan` 等 extra 路由 | `Partial` | KMP 有、Flutter 路由表无（drift extra 侧），需逐条核对是否重复 / 占位 | `scripts/audit-flutter-drift.sh` §(c) |
| `/ai/stream` | `Placeholder` | SSE 传输层已落但 `AiStreamScreen` 仍 mock，UI 未接线 | [ai/Android/ai_stream.kmp.png](./ai/Android/ai_stream.kmp.png)；registry「AI stream」 |
| `/web`、`/wallet` 等主路由 | `Partial` | 三端可开且部分有像素证据，但数据契约 / 真 SDK 未齐 | [web/Android/web.diff.json](./web/Android/web.diff.json)、[wallet/Android/pay.diff.json](./wallet/Android/pay.diff.json) |
| 全量收口 | `Partial` | 真实剩余以唯一执行账本 v2 的 `[ ]`/`[~]` 为准；门禁 `scripts/parity_task_audit.py` 默认读 v2、真实剩余 > 0 即失败 | [tasks-ultra-fine-v2.md](../../../sync-flutter-full-parity/tasks-ultra-fine-v2.md)；[acceptance-matrix.md](../acceptance-matrix.md)、[parity-inventory.md](../parity-inventory.md) |
