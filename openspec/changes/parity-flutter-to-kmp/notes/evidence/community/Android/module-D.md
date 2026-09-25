# Module D — Community（Android 验收）

Date: 2026-09-26

## `/community`

| 项 | 结果 |
|----|------|
| Soft-auth | PASS（与 Chat 同门禁） |
| 入口 | Tab「社区」、深链 `myai:///community` |
| UI | 大标题 + 发布 + 搜索条 + 最新/热门/关注；帖卡片对齐 Flutter |
| 证据 | `community.{flutter,kmp}.png` |

## `/community/publish`

| 项 | 结果 |
|----|------|
| 入口 | 「+」/ 深链 `myai:///community/publish` |
| UI | 发布顶栏 + 文案 + 选图/话题（Flutter 首开会叠「社区公约」弹窗） |
| 证据 | `community_publish.{flutter,kmp}.png` |

## `/community/convention`

| 项 | 结果 |
|----|------|
| 入口 | 深链 / 发布前弹窗「了解完整公约」 |
| UI | 盘友圈全文（对齐 Flutter `CommunityConventionPage`） |
| 证据 | `community_convention.{flutter,kmp}.png` |

## `/community/search`

| 项 | 结果 |
|----|------|
| 入口 | 搜索条 / 深链 `myai:///community/search` |
| UI | 搜索框 + 空态提示；分区结果（mock） |
| 证据 | `community_search.{flutter,kmp}.png` |
