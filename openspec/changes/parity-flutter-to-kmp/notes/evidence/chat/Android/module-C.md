# Module C — Chat（Android 验收）

Date: 2026-09-26

## `/chat`

| 项 | 结果 |
|----|------|
| Soft-auth | PASS — 未登录进 Chat → LoginScreen（短信 OTP）→ resume |
| 入口 | Tab「聊天」、深链 `myai:///chat` |
| UI | 大标题「消息」+ 搜索/发起；分组白卡片列表 |
| 业务 | MockImEngine(seedDemo) 三会话（真融云 IM：platform-gap） |
| 证据 | `chat.{flutter,kmp}.png` |

## `/chat/detail`

| 项 | 结果 |
|----|------|
| 入口 | 列表行点击；深链 `myai:///chat/detail?peerName=` |
| 无参 | 对齐 Flutter「缺少会话参数」（`chat_detail_missing.kmp.png`） |
| UI | 顶栏标题 + 气泡 + 输入「发送消息…」；底栏在详情隐藏 |
| 证据 | `chat_detail.{flutter,kmp}.png` |

## 差距

- 真 IM SDK（RongCloud）未接线；列表为 Flutter 同级 mock
- Flutter 详情含语音/表情/+ 面板；KMP 现为文本+图片 mock
