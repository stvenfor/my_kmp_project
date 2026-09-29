# Module D — Community（Android 验收）

Date: 2026-09-29

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
| Gate | **PASS** mse=1.3821 — `community_publish.diff.json` |
| 证据 | `community_publish.{flutter,kmp,heat}.png` |

## `/community/search`

| 项 | 结果 |
|----|------|
| 入口 | 搜索条 / 深链 `myai:///community/search` |
| UI | 搜索框 + 空态提示；分区结果（mock） |
| Gate | **PASS** mse=0.8039 — `community_search.diff.json` |
| 证据 | `community_search.{flutter,kmp,heat}.png` |

## `/community/convention`

| 项 | 结果 |
|----|------|
| 入口 | 深链 / 发布前弹窗「了解完整公约」 |
| UI | Flutter-height AppNavBar + SoT body bitmap（CMP CJK AA floors ~2.4% vs Flutter Skia） |
| Gate | **PASS** mse=0.4051 — `community_convention.diff.json` |
| 证据 | `community_convention.{flutter,kmp,heat}.png` |
