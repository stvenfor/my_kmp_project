# 04 · 四条主干线

> 骨架只是目录，主干线才是设计。跟读方法：**先读测试再读实现**，测试是行为说明书。  
> 每条线下面都给了「易改坏语义」，这些就是真正的知识点。

## 线 A：一次 HTTP 请求的完整生命周期

```
调用点（feature）
  → ApiClient / NetworkFacade（GET / PUT / PATCH / DELETE）
  → 路径参数与查询串拼接（形如 /api/v1/mall/orders/{orderId}）
  → envelope（ApiEnvelope）→ ApiEnvelopeParser
  → 业务码 → 领域错误映射（NetworkCodes + AuthBizCode：10003 forbidden、10021 / 10022）
  → 401 / 403 会话决策 → token 刷新 → core/account 会话状态
```

关键文件：

| 文件 | 作用 |
|-|-|
| `core/network/…/ApiClient.kt` | 请求门面 |
| `core/network/…/KtorApiClient.kt`（在 `networkKtorMain`） | Ktor 实现，OHOS arm64 走 CIO |
| `core/network/…/OhosHttpTransport.kt`（291 行，`ohosX64Main`） | ohosX64 的 `net_http` cinterop 回落 |
| `core/network/…/ApiEnvelope.kt` + `ApiEnvelopeParser.kt` | 统一响应壳解析 |
| `core/network/…/DemoApiHosts.kt` + `BackendHostPolicy.kt` + `BackendLocalhost.kt` + `LanHost.kt` | 环境主机规则（loopback / LAN） |
| `core/network/…/AuthRefreshingApiClientTest.kt`（325 行） | 401/403 与刷新行为的可执行说明 |

已定语义（两条最容易改错的结论，来自 `notes/api-contract-2026-10-16.md`）：

- §5.13：Dart 的 `_data()` 是 `message.isNotEmpty ? message : fallback`，**服务端文案非空时永远盖过本地兜底**。所以「fixture 写英文却期望中文」不是 bug，是测试期望错。
- §5.14：`buildJsonObject + put(String?)` 写的是 `JsonNull` 元素，**不受 `explicitNulls=false` 影响**；Dart 侧真实门控是 trim 后非空。别看到 null 就怀疑 `explicitNulls`。

自查题：`/api/v1/mall/orders/{orderId}` 这类路径参数在哪一层拼接？离线不可达时靠什么回落到同级 mock（R6：保持可演示，不许白屏）？

## 线 B：持久化 —— KV 只有一个契约

```
业务代码
  → core.storage.KeyValueStore（唯一契约）
      android / ios   → settingsMain 的 multiplatform-settings
      ohosArm64 / X64 → 直接实现 OH_Preferences NDK（-lohpreferences）
```

关键文件：`core/storage/…/KeyValueStore.kt`、`SettingsKeyValueStore.kt`、`InMemoryKeyValueStore.kt`、三端 actual、`KeyValueStoreContractTest.kt`（8 例契约测试）。`StringListValues.kt` 是列表型值的编码约定。

为什么不用 SQLDelight、也不建第二个持久化模块：用户 2026-10-16 明确决策「先做 KV：core 扩 multiplatform-settings」，且 `:core:database` 是另一条独立工程线。会话与隐私同意都走 `KeyValueStore`，`:core:account` 有 7 例测试消费它。

易改坏语义（`component/realtime/RealtimeSeqStore.kt` 的 KDoc 全是要点）：

- `lastSeq` 是 **64 位**（对齐 Dart `int`），`load()` 解析失败一律回 `0`
- `acceptSeq(null)`**恒放行**：控制帧 / 无 seq 业务帧不参与排序
- **只有严格大于水位才接受**，等号也算重复
- 接受即写盘（Dart 侧是 `unawaited(save())` 即发即忘）
- `setLastSeq` 只抬升不下降，等号不写
- 为什么接口是**同步**的：Dart `getString` 本来就是内存快照同步返回，`save()` 的字符串实参在挂起前已求值，所以「同步写」与 Dart 的可观测顺序等价，测试也不需要等落盘

遗留缺口（任务 #23）：`RealtimeKeyValueStore` 目前只有 `InMemoryRealtimeKeyValueStore`，生产侧没有构造点。收尾动作是给 `:component` 加 `api(projects.core.storage)`，写一个 `KeyValueStore → RealtimeKeyValueStore` 适配器 + 契约测试。

## 线 C：三条长连接互不复用（ADR-0005）

| 通道 | 唯一承载 | 实现 | 端点/入口 |
|-|-|-|-|
| AI | SSE | `core/sse/SseFrame.kt`、`SseParser.kt`、`AiSseDecoder.kt`、`core/network/Sse.kt`（281 行）、`KtorSseTransport` | `POST /api/v1/sse/completions`，camelCase 请求体 |
| Realtime | WebSocket | `component/realtime/`（13 个文件 / 2141 行）：状态机、心跳、重连退避、seq store、notifyId 去重环、outbound queue、`MockRealtimeWsTransport` | `ws-ticket` 换票 + `/realtime/sync` 增量补齐 |
| Chat | `ChatPort` / `ImEngine` | `component/chat/ChatPort.kt`、`ImEngine.kt`（真融云在 M11，当前是 mock + gap） | IM SDK 语义 |

硬规则：**三者的失败语义不得互相翻译**。`SseException`、`RealtimeWsError.Unsupported / BodyUnparsable / TicketInvalid`、SDK code 是三套独立枚举，任何跨通道的「顺手归一」都是违规。

静态判据（ADR-0005 里写成规则）：`feature/ai` 引用 `AppRealtimeClient` / `RealtimeWsTransport` 是违规；`feature/chat` 引用 `SseTransport` 是违规。

notifyId 去重环（`NotifyDedupStore`）与 seq 水位**故意独立**：Dart 注释说明「`sys.notify` 仍交给 Handler，notifyId 去重，避免 seq 已更新但 Banner 未展示」。细节：空 `notifyId` 恒放行；命中即拒绝；新 id 插到头部，超过 `RealtimeConfig.NOTIFY_DEDUP_MAX`（=200）从尾部截断；解析失败清空重来、不抛。

待办：M2.10（ohosArm64 CIO WebSocket 实测）、M2.11（`/live/room` 弹幕与在线人数、`/chat/detail` 收推接线）、M3.3（`AiStreamScreen` 接 `AiSseApi`，目前该屏仍是静态图 + mock toast）。

## 线 D：路由与三端分发

```
AppRoutePath / FeatureRoutes（常量）
  → canonicalize（别名归一，如 /mine/business → Cooperation）
  → ProductRouteDispatch（决定 host：Compose 岛 / 原生壳 / Toast 兜底）
  → ①Compose 岛 SecondaryRouteIsland      ②Swift NativeFeatureCatalog   ③ArkTS NativeFeatureCatalog.ets
```

别名表在 `core/router/FeatureRoutes.kt:146-150`：`/mine/sms_template → /mine/sms_templates`、`/mine/store_qr → /mine/shop_qr`、`/mine/business → /mine/cooperation`、`/mine/reminders → /mine/reminder`。

**关键认知**：原生壳拿到的是**规范化之后**的路径。壳里若仍写旧别名字面量，那个分支永远命中不了（2026-10-17 的 M10.A.2.i 修的就是这个）。

分发规则必须「具体前缀优先」：`/live/room` 会被 `/live` 吞、`/music/now_playing` 会被 `/music` 吞、`/mall/orders/detail` 会被 `/mall/orders` 吞。iOS 与 ArkTS 两侧同型守卫分别由 `scripts/check-ios-page-dispatch.sh`、`scripts/check-harmony-pane-dispatch.sh` 断言「定义数 == 分发数」（各 65 页）。

ADR-0002 解释了为什么 Mine 是「原生壳 + Compose 岛」混合形态：壳保持原生观感，二级页复用共享 Compose。
