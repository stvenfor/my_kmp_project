# ADR 0005 — 三条长连接通道各归各的传输，不得互相复用

- **Status:** Accepted (2026-10-17)
- **Date:** 2026-10-17
- **Related:** 上游 SoT ADR 0005 `my_ai_project/docs/adr/0005-sse-visit-conversation-not-realtime-im.md`（本文件是它的 KMP 落地版，编号巧合）、ADR 0004（BFF JSON 归 Kotlin）、契约表 §2.17 行 #96、`openspec/changes/sync-flutter-full-parity/`

## Context

Flutter 侧（SoT `d7876a1`）有三条「服务器往客户端持续推东西」的通道，形态完全不同：

| 通道 | Dart 位置 | 形态 |
|------|-----------|------|
| AI「小石头」流式回答 | `features/ai/lib/ai/api/ai_sse_api.dart` + `commons/network/lib/sse/sse_client.dart` | 一次性 HTTP `POST` + `text/event-stream`，读完即关 |
| 全局实时通知 / 断线补投 | `commons/realtime/lib/**`（`store/realtime_seq_store.dart`、`client/app_realtime_client.dart`） | 长连接 WS，ticket 鉴权、`seq` 单调水位、`notifyId` 去重环 |
| 单聊 / 群聊消息 | 融云 IM SDK + `http_im_*_service.dart` | 厂商 SDK 自管连接与存储，BFF 只签 token |

拆模块之后，KMP 侧这三条分别落在 `:core:network`(`SseTransport` / `RealtimeWsTransport`) + `core/sse`(`SseParser`) + `:component:realtime`(`AppRealtimeClient`) + `:component:chat`(`ChatPort`/`ImEngine`)。它们都在「流式」这个大类里，很容易顺手合并成一条「通用流」——尤其 AI 屏接 SSE 时，最省事的做法就是复用已经在跑的 realtime WS。

## Decision

1. **AI 流式只走 SSE**：`feature/ai` 的 `AiSseApi`（`:core:network` 的 `SseTransport` + `core.sse.SseParser`），端点 `POST /api/v1/sse/completions`，请求体 **camelCase**（`conversationId`/`clientRequestId`），一次提问一条流，协程取消即断流。禁止用 WS 承载 AI token 流。
2. **realtime WS 只承载全局通知与补投**：`AppRealtimeClient` 的 `seq` 水位（`realtime_last_seq`）和 `notifyId` 去重环（`realtime_notify_dedup_json`）是它的全部业务语义，不对外提供「请求-流式响应」接口，也不得被 AI/聊天调用。
3. **聊天只走 IM 端口**：`ChatPort` / `ImEngine` 是聊天唯一入口，BFF 侧只有 `/api/v1/im/**`（token 签发、关系、群、备份）。在真融云接入前由 gap registry 登记（R5），**不得**用 SSE 或自研 WS 伪造「聊天已接通」。
4. **三条通道的失败语义不许互相翻译**：SSE 前流（未升级 / 非 200）在传输层抛 `SseException`，body 绝不喂 `SseParser`；WS 走重连状态机 + `RealtimeWsError`（`Unsupported`/`BodyUnparsable`/`TicketInvalid`）；IM 走 SDK 回调码。写「统一异常」等于丢掉 Dart 侧可观测行为。
5. **AI 用「停留会话」（Visit Conversation）**：首句由服务端创建 `conversationId`，离开助手页即丢弃客户端持有的 id，不做跨次续聊，也不把助手气泡写进 IM 历史。KMP 侧这个 id 只允许活在 `feature/ai` 的页面状态里，不进 `:core:account`、不进 `KeyValueStore`（持久化它＝把上游明确丢弃的东西偷偷留下）。

## Consequences

- 想让 AI 复用 realtime WS（省一个传输实现）的方案一律否决；两者在 `:core:network` 里各留各的接口，重复的部分只有「平台 client 差异」，那本来就是 `expect/actual` 的事。
- `feature/ai` 的 SSE 接线（M3）只需要 `SseTransport`，不需要 `:component:realtime`，模块依赖面因此更小。
- OHOS 两条架构的可用性差异被限制在传输层内部：ohosArm64 用 CPF Ktor `3.3.3-0.3.0`，ohosX64 走 `net_http` cinterop 兜底，feature 层看不见这个分叉。
- 门禁可静态检查：`feature/ai` 里出现 `AppRealtimeClient` / `RealtimeWsTransport` 引用即为违规；`feature/chat` 里出现 `SseTransport` 同理。
