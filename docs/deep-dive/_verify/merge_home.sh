#!/usr/bin/env bash
# Merge the three feature:home fragments into the single doc 16.
set -euo pipefail
cd /Users/qpy/Desktop/github/my_kmp_project/docs/deep-dive
for f in 16a-home-api-and-state.md 16b-home-screens.md 16c-home-assets-and-charts.md; do
  [ -s "$f" ] || { echo "MISSING/EMPTY: $f"; exit 1; }
done
{
  echo "# 16 · feature:home 详解"
  echo
  cat <<'INTRO'
## 16.0 合订导读

`feature/home` 是全仓最大的业务模块，实测规模（`find feature/home -name '*.kt'`，不含 `build/`）：

| 源集 | 文件数 | 行数 |
|-|-|-|
| `commonMain` | 34 | 11094 |
| `commonTest` | 7 | 1433 |
| `ohosMain` | 1 | 33（只有 `AnalyticsCharts.ohos.kt` 的图表占位 actual） |
| 合计 | 43 | 12716 |

单一 agent 读不完，因此本册由三个互斥子域合订：

- **16A** 数据与接口层：`api/` 8 个文件逐符号、aftersales 与 newcar、24 个端点总表、四个 Store、`HomeMockData` 消费点、6 个测试文件逐用例。
- **16B** 屏与分区：12 个屏文件逐函数、`HomeRouteHost` 的 26 行分派表与 20 个二级屏、首页→二级页跳转链路、沉浸式/inset 合规。
- **16C** 图标与图表：`HomeIcons` 资产矩阵、`AnalyticsCharts` 三端实现差异（含 OHOS 只有 `Box` 占位这一事实）。

读法建议：接手首页业务先 16A 的端点总表，改 UI 先 16B 的 `HomeRouteHost` 分派表，评估图表回归成本只看 16C.5。

INTRO
  cat 16a-home-api-and-state.md
  echo
  cat 16b-home-screens.md
  echo
  cat 16c-home-assets-and-charts.md
  echo
  cat <<'TAIL'

## 16.99 三册口径冲突与统一验收

三册各自的「未验证疑点」原文保留在各节末尾，跨册汇总如下：

1. `api/` 里 6 个 API 类在生产代码零调用（16A 以 grep 实证）——契约实现已完成、UI 尚未接线，不是缺实现。
2. iOS 与 OHOS 的二级路由分发由 common 链路推断（16B 未见 swift/ets 侧引用），需实机确认。
3. 图表依赖是死重量：`chartsVicoMain` 已接线但代码里没有任何 Vico 调用（16C.5 grep 实测），OHOS 两个 actual 只剩 `Box`。
4. 三册重叠处以各自 `file:line` 为准（16B 已把 4 行误标的 Todo 数据源改回真实来源）。

机械验收（`_verify/verify_docs.py`）：`feature/home` 范围内 44 个文件全部被写到、无缺失目录、每条 `path:line` 引用可解析且行号不越界。
TAIL
} > 16-feature-home.md
wc -l 16-feature-home.md
