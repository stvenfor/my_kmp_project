# Acceptance report

Status: **Android-only Pass (partial three-platform)** — 2026-09-26.

> **禁止写「项目完成」。** 本轮仅 Android 端 in-scope RoutePath 与 Flutter SoT 并排证据收口；iOS / OHOS 一律见下方 `missing` 清单与 `platform-gap-registry.md`。

## Passed (Android, evidence under `notes/evidence/**/Android/`)

- **Shell:** Splash / Privacy / Main 四 Tab（`shell/`）
- **Auth:** guest→Login/OTP/Password/Register 并排（`auth/`）；远端 OTP 受 Supabase 配额限制见 registry
- **Home:** 根 + 全部服务/搜索 + 二级列表→详情并排（`home/`）
- **Chat / Community:** 列表→详情→发送；feed→发布并排（`chat/`、`community/`）
- **Mine / Settings / Profile / Addresses / Personalized:** 并排（`mine/`）
- **Mall / Orders / Wallet / Pay / Membership:** 并排；Pay 对齐 Flutter 占位「Pay 模块」；支付 SDK `stub`（`commerce/`）
- **Media / Music / Video / Classroom / Live / Friend / AI / Web:** 主路径并排（`media/`、`classroom/`、`live/`、`friend/`、`ai/`、`web/`）
- **Bridges:** deeplink 冷启动、scan deny、Web offline fixture（`bridges/`）

## iOS / OHOS `missing` 清单（本轮）

| 能力面 | iOS | OHOS | 说明 |
|---|---|---|---|
| in-scope RoutePath UI（78） | **missing** | **missing** | 未做设备像素验收；共享 Compose 未等同于端验收 |
| Shell / Auth / Home / Chat / Community / Mine / Mall | missing | missing | 同上 |
| Media playback（音视频） | stub / missing | missing | registry `media playback` / `short video` / `music` |
| WebView | partial | partial | Android ready；iOS/OHOS 未本轮验收 |
| Scan / Camera | partial | missing | registry |
| Push | missing | missing | registry |
| WeChat 登录 / 微信支付 / 支付宝 | missing / stub | missing / stub | registry；不可标业务完成 |
| Deeplink 冷启动设备验收 | pending | pending | 代码有；本轮未跑设备 |
| AI SSE / 真 IM / 直播推流 | missing | missing | Android 为 mock 对齐 Flutter mock |

完整状态表：`platform-gap-registry.md`。

## Incomplete / follow-up（不挡本轮 Android 结案）

- 三端真 SDK：Push、WeChat、Pay、IM、Live realtime、image_picker
- OHOS 持久化 prefs、真 WebView、相机 N-API
- Home 公司数据/待办、Community feed 真 API（仍 mock 对齐 Flutter）
- Music 全局 mini-player inset 仍 partial

## Sign-off

| role | name | date | 1A pixel (Android) | 2A three-platform |
|---|---|---|---|---|
| engineering | parity agent | 2026-09-26 | Pass（并排证据） | **Fail** — iOS/OHOS missing |
| product |  |  |  | 待签 |
