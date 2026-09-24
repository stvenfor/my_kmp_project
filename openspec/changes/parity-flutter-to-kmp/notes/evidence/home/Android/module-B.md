# Module B — Home（Android 验收）

Date: 2026-09-25

## 可点路径

| 入口 | 路由 | 结果 |
|------|------|------|
| 搜索框 | `/home/search` | PASS — HomeSearchScreen |
| 扫一扫 | Scan overlay | PASS — Module A |
| 更多 | `/home/all_services` | PASS |
| 生活服务/直播带货/Club | feature containers | PASS — FeatureContentScreen |
| 二手车 | list→detail/create | PASS — mock CRUD |
| 新车跟进/成交 | list→detail/create | PASS |
| 待办四卡 | todo/* | PASS |
| 公司数据 | `/home/ledger` | PASS |
| 投资策略 | `/home/strategy` | PASS |
| 学习报告 | `/home/learning_report` | PASS |
| 配音/热榜 | dubbing_feed / hot_rank | PASS |
| 下拉刷新 | PullToRefreshBox | PASS |

## Compile

`./gradlew :composeApp:compileDebugKotlinAndroid -PandroidOnly=true` OK

## Gaps（登记，不挡 Android 可点验收）

- Todo/指标真 API：mock 对齐契约，见 B.1.18–21
- iOS/Harmony Home 同路径：W5/W6
- AllServices 常用写回首页顺序：编辑态可用，跨页持久化未做
