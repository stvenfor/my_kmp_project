## 16C · feature:home 的图标与图表

> 定位说明：本分册覆盖 `:feature:home` 的**纯视觉资产层**——两组手写 `ImageVector` 图标库
> （`HomeIcons` 实心 / `HomeStrokeIcons` 描边）与 `AnalyticsCharts` expect/actual 三端图表。
> 数据与状态见 16A，屏幕编排见 16B；本分册对这些文件只留 grep 行号引用，不展开。
>
> 全部行号以仓库根为前缀 `feature/home/src/.../`（下表省略公共前缀
> `commonMain/kotlin/com.example.my_kmp_project/feature/home/`）。

### 16C.1 文件清单

| 路径（仓库相对） | 作用 | 规模/关键行号 |
|---|---|---|
| `feature/home/src/commonMain/.../home/HomeIcons.kt` | 10 个 Material 实心风 ImageVector（OHOS-safe，无 material-icons-extended） | 424 行；val 17–26，工厂 `solid()` 28–47 |
| `feature/home/src/commonMain/.../home/HomeStrokeIcons.kt` | 10 个描边风 ImageVector + 服务名→图标分发 | 252 行；val 26–53，`forServiceLabel` 56–65，工厂 `stroked()` 72–90 |
| `feature/home/src/commonMain/.../home/AnalyticsCharts.kt` | 图表 **expect** 契约（2 个 @Composable） | 32 行；expect 17–21 / 24–31 |
| `feature/home/src/chartsVicoMain/.../home/AnalyticsCharts.vico.kt` | Android+iOS actual：**纯 Canvas** 环图+柱图（Vico 已弃用，见 16C.5） | 156 行；ring 40–104，bars 106–156 |
| `feature/home/src/ohosMain/.../home/AnalyticsCharts.ohos.kt` | Harmony actual：**空白占位 Box** | 33 行；stub 15–21 / 24–32 |
| `feature/home/build.gradle.kts` | chartsVicoMain 中间源集接线 + `libs.vico.compose` 依赖 | 38 行；源集 14–23 |
| `feature/home/src/commonMain/composeResources/` | **未找到**（见 16C.6 实测命令） | — |

### 16C.2 HomeIcons —— 实心（fill）图标组

**组织方式**：不是资源引用，是**逐个手绘的 Material path 数据**。每个图标 = 一个
`private fun xxx() = solid { moveTo/curveTo/… }`，`solid()`（HomeIcons.kt:28–47）用
`ImageVector.Builder`（24×24dp、viewport 24f）生成**单条 fill 路径**：
`fill = SolidColor(Color.Black)`、`stroke = null`、`PathFillType.NonZero`（36–44 行）。
描边轮廓类图标（如 notifications_none）靠「外轮廓 + 反向内轮廓」在同一路径里挖洞。
public 项全部 `by lazy`（17–26 行），消费方以 `imageVector = HomeIcons.X` 传给 `Icon(...)`。

| public val（行号） | 私有构造器（行号） | 视觉语义（KDoc 注释） | 使用处（grep file:line） |
|---|---|---|---|
| `NotificationsNone` :17 | `notificationsNone()` :50 | 铃铛（`Icons.notifications_none_rounded`） | `HomeSections.kt:131` |
| `Search` :18 | `search()` :81 | 放大镜（`search_rounded`） | `HomeSections.kt:178` |
| `QrScan` :19 | `qrScan()` :106 | 扫码框（`qr_code_scanner_rounded`） | `HomeSections.kt:207` |
| `GridView` :20 | `gridView()` :226 | 四宫格（`grid_view_rounded`） | `HomeScreen.kt:244`（投资策略入口卡）、`composeApp/src/androidMain/.../nativeshell/NativeJetpackHome.kt:182` |
| `Analytics` :21 | `analytics()` :250 | 柱状图框（`analytics_outlined`） | `HomeScreen.kt:264`（学习报告入口卡）、`NativeJetpackHome.kt:201` |
| `PersonOutline` :22 | `personOutline()` :285 | 人形（`person_outline`） | `HomeSections.kt:858` |
| `ChatBubble` :23 | `chatBubble()` :313 | 聊天气泡（`chat_bubble_outline`） | `HomeSections.kt:876`（"chat" 分支）、`HomeStrokeIcons.kt:61`（"话术库"） |
| `Phone` :24 | `phone()` :334 | 听筒（`phone_outlined`） | `HomeSections.kt:877`（"phone" 分支） |
| `ChevronRight` :25 | `chevronRight()` :367 | 右箭头（`chevron_right_rounded`） | `HomeSections.kt:392, 661, 705, 878, 931` |
| `Article` :26 | `article()` :382 | 资讯（`article_outlined`） | `HomeSections.kt:979` |

数量统计（`grep -c "val .*: ImageVector by lazy"`）：**10 个 public val + 10 个 private 构造器 + 1 个 `solid()` 工厂**。
10 个 val 全部有消费处（Android `NativeJetpackHome` 是 commons 图标跨进 app 层的例子）。

### 16C.3 HomeStrokeIcons —— 描边（stroke）图标组

**为什么与 HomeIcons 分家**（文件头 KDoc 12–23 行，原文归纳）：
1. 描边图标直接 `stroke` 画开放路径，绕开 NonZero「外轮廓+反向内轮廓」挖洞问题，24×24 视口下像素可控；
2. Flutter SoT 首页服务九宫格/门店行用的是 outlined 变体，实心 Material 视觉偏重、与截图对不上；
3. 约束同 HomeIcons：全部手写 ImageVector，不引入 material-icons-extended，保证 ohosArm64/ohosX64 可编译。

**构造器**：`stroked()`（72–90 行）—— `fill = null`、`stroke = SolidColor(Color.Black)`、
线宽常量 `STROKE = 1.7f`（69 行）、`StrokeCap.Round`/`StrokeJoin.Round`/`strokeLineMiter = 4f`。
另有 `PathBuilder.roundRect(l,t,r,b,rad)` 扩展（93–104 行）：描边路径没有 arcTo 简写，
用四段 `curveToRelative` 以对角近似圆角半径。

| public val（行号） | 私有构造器（行号） | 对齐的 Flutter 图标 | 使用处（file:line） |
|---|---|---|---|
| `Storefront` :26 | `storefront()` :108 | `storefront_rounded`（遮阳棚+波浪+店体+门） | `HomeSections.kt:464`（门店选择行） |
| `ArrowDown` :29 | `arrowDown()` :137 | `keyboard_arrow_down_rounded` | `HomeSections.kt:482` |
| `SwapHoriz` :32 | `swapHoriz()` :143 | `swap_horiz_rounded`（换门店） | `HomeSections.kt:489` |
| `PhotoCamera` :35 | `photoCamera()` :156 | `photo_camera_outlined`（roundRect 机身+镜头） | 仅经 `forServiceLabel` "朋友圈" :57 |
| `Videocam` :38 | `videocam()` :173 | `videocam_outlined` | 仅 "视频号" :58 |
| `LiveTv` :41 | `liveTv()` :181 | `live_tv_outlined`（信号点+天线） | 仅 "直播" :59 |
| `PhotoLibrary` :44 | `photoLibrary()` :197 | `photo_library_outlined`（双卡+山水线） | 仅 "素材库" :60 |
| `School` :47 | `school()` :213 | `school_outlined`（学位帽+流苏） | 仅 "培训" :62 |
| `Insights` :50 | `insights()` :231 | `insights_outlined`（折线+箭头+基线） | 仅 "竞品分析" :63 |
| `Apps` :53 | `apps()` :246 | `apps_rounded`（四个 roundRect） | `forServiceLabel` else 分支 :64 |

**唯一的分发 API**：`fun forServiceLabel(label: String): ImageVector`（56–65 行），
when 覆盖 朋友圈/视频号/直播/素材库/话术库(→`HomeIcons.ChatBubble`)/培训/竞品分析，else→`Apps`。
消费处在 `HomeSections.kt:775`（44dp 圆角瓦片、accent@12% 底，服务九宫格 tile）。

数量统计：**10 个描边 val + 10 个 private 构造器 + 1 个 `stroked()` + 1 个 `roundRect()` 扩展 + 1 个 `forServiceLabel`**。
`HomeSections.kt` 直接点名的只有 3 个 val（Storefront :464 / ArrowDown :482 / SwapHoriz :489）
加 `forServiceLabel`（:775）；其余 7 个 val（PhotoCamera/Videocam/LiveTv/PhotoLibrary/School/
Insights/Apps）只经分发器 when 间接消费。

#### public 签名总览（两组图标）

`HomeIcons.kt:17–26` 全部同构：`val X: ImageVector by lazy { x() }`，共 10 项
（NotificationsNone / Search / QrScan / GridView / Analytics / PersonOutline / ChatBubble /
Phone / ChevronRight / Article）。`HomeStrokeIcons.kt:26–53` 同构 10 项（Storefront /
ArrowDown / SwapHoriz / PhotoCamera / Videocam / LiveTv / PhotoLibrary / School / Insights /
Apps）+ 唯一函数 `fun forServiceLabel(label: String): ImageVector`（:56）。类型均为
`androidx.compose.ui.graphics.vector.ImageVector`；两对象对外只有这 21 个符号，
没有任何带参 composable 或资源 id。

#### 描边几何要点（供改图标者，行号=HomeStrokeIcons.kt）

| 图标 | 分段结构 | 备注 |
|---|---|---|
| `storefront` | 遮阳棚 :110–115 → 棚下波浪 curveToRelative :117–124 → 店体 :126–129 → 门 :131–134 | 波浪以 `c(0,1.4,1.1,2.2,2.4,2.2)` 单元重复 |
| `photoCamera` | roundRect 机身 :158 → 取景凸台 :160–163 → 镜头 4 段 curve :165–170 | 机身 r=2.6f；镜头圆心约 (12,13.2)、半径 4f |
| `videocam` | roundRect 机身 :174 → 右侧梯形镜头 :175–178 | 机身右缘 15.5f，梯形外扩到 21.5f |
| `liveTv` | roundRect 屏体 :182 → 信号点 :184–189 → 双天线 :191–194 | 天线顶点 y=4.4f 高于屏体上沿 7.5f |
| `photoLibrary` | 后卡两边（不闭合）:199–202 → 前卡 roundRect :204 → 山水折线 :206–210 | 后卡故意不闭合，模拟叠卡遮挡 |
| `school` | 帽面平行四边形 `close()` :215–219 → 袍体 :221–225 → 流苏 :227–228 | 帽面 moveTo/lineTo×4+close |
| `insights` | 折线 :233–236 → 右上箭头 :238–240 → 基线 :242–243 | 与 HomeIcons.Analytics（实心柱）刻意区分 |
| `apps` | 4 个 roundRect :247–250 | r=1.6f，四角对齐 3.2f/14.8f 网格 |

### 16C.4 AnalyticsCharts —— expect 契约与三端矩阵

**expect 契约**（commonMain/AnalyticsCharts.kt，32 行，仅注释+两个 expect）：

| 签名（行号） | 参数语义 |
|---|---|
| `expect fun AnalyticsConversionRing(rate: Float?, modifier: Modifier = Modifier, size: Dp = 64.dp)` :16–21 | `rate` = **0..1 分数**（可空）。actual 端校验 `!= null && isFinite && >= 0`，再 `coerceIn(0,1)`；非法值环为空轨+中央显示 `—`（vico.kt:46–48） |
| `expect fun AnalyticsMiniBars(pv: Float, uv: Float, clicks: Float, converts: Float, modifier: Modifier = Modifier, height: Dp = 64.dp)` :23–31 | 4 根柱 = PV/UV/点击/转化；归一化基准是 **`pv.coerceAtLeast(1f)`**（vico.kt:118），即 PV 柱恒撑满高度 |

文件头 KDoc :8–14 说明这是对 Flutter `wys_chart` 列表 tile 的替身（stand-in）。
expect 声明逐字（:16–31）：

```kotlin
@Composable expect fun AnalyticsConversionRing(
    rate: Float?, modifier: Modifier = Modifier, size: Dp = 64.dp)
@Composable expect fun AnalyticsMiniBars(
    pv: Float, uv: Float, clicks: Float, converts: Float,
    modifier: Modifier = Modifier, height: Dp = 64.dp)
```

唯一消费方：`HomeRouteHost.kt:2118`（ring，size=64.dp）、`:2120–2126`（bars，
`Modifier.weight(1f).height(64.dp)`）—— 报表列表行「环+四柱」组合；:2017 注释同样写明
"Canvas ≈ wys_chart; CPF 无 Vico/KoalaPlot"。

#### chartsVicoMain 环图几何（AnalyticsCharts.vico.kt:40–104）

| 步骤 | 行号 | 实现 |
|---|---|---|
| 合法性 | :46–48 | `valid = rate != null && isFinite && >= 0`；`clamped = coerceIn(0f,1f)`；文案 `"${round(clamped*100)}%"` 或 `—` |
| 线宽 | :49/:57–58 | 轨道 7.dp，值弧 7.dp+2.dp（Butt cap），值弧比轨道粗 2dp |
| 直径/居中 | :59–63 | `diam = (minDimension - valueStroke.width).coerceAtLeast(1f)`，topLeft 双向居中 |
| 轨道弧 | :65–73 | 从 `-90° + 360°*clamped` 起画剩余 `360°*(1-clamped)`（下限 0.0001°，故 100% 时轨道几乎不可见） |
| 值弧 | :74–84 | 仅 valid 时画：-90° 起、sweep `360°*clamped` 下限 1°（0% 也留 1° 小头） |
| 中央文案 | :86–102 | 百分比 ExtraBold、字号 `size*0.18` sp；「转化率」SemiBold、`size*0.11` sp；行间距 `size*0.02` dp |

色板常量 :29–33：`ChartInk #171717 / ChartLink #0070F3 / ChartAccent #F5A623 / ChartGrid #EBEBEB / ChartMute #888888`。

**三端矩阵**：

| 源集 | 服务对象 | actual 实现 | 画出什么 |
|---|---|---|---|
| `chartsVicoMain` | androidMain + iosMain（build.gradle.kts:21–23） | 纯 Compose `Canvas`（drawArc/drawRoundRect），**零 Vico API 调用** | 环图：轨道弧 `ChartGrid`(#EBEBEB) :65–73 + 值弧 `ChartInk`(#171717) :74–84，线宽 7dp(+2dp 值弧) :49/:57；中央 `${round(rate*100)}%` + 「转化率」两行文字，字号按 size 的 18%/11% 缩放 :86–102。柱图：宽 10dp、圆角 4dp 的 `drawRoundRect` 四柱 :126–139，色序 `[#171717, #0070F3, #F5A623, #0070F3]` :117，等分槽位居中 :127/:132，下方 9sp 标签 PV/UV/点/转 :115/:143–154 |
| `ohosMain`（含 ohosArm64 + ohosX64） | 两端 OHOS target | `Box(modifier.size(size))` :20、`Box(modifier.height(height))` :32 | **什么都不画**——纯空白占位，仅保住布局尺寸使编译与排版通过（KDoc :10–13："CPF-KMP-CMP 三方库清单尚无 Vico / KoalaPlot…先留空占位"） |

**为什么拆中间源集**（对应 AGENTS.md 的 OHOS 限制模式）：
Vico（`com.patrykandpatrick.vico:multiplatform`）只出 Android/iOS klib/AAR，CPF 适配线没有
ohos 产物——放进 `commonMain` 会直接毒死 OHOS 编译。所以 `chartsVicoMain` 只挂
`androidMain`（build.gradle.kts:21）和 `iosMain`（:23，`matching{}` 惰性挂载，因为
`-PandroidOnly=true` 时 iosMain 不存在，见 :22 注释）；`ohosMain` 保持
`dependsOn(commonMain)`（根 build.gradle.kts:124–126），不会传递拿到 chartsVicoMain，
由 ohosMain 自己提供占位 actual。这与 AGENTS.md 表格中 `networkKtorMain`/`imageCoilMain`/
`uuidCpfMain` 是同一套路数，只是 chartsVicoMain 是 **feature:home 模块私有**源集，不在 AGENTS.md 的全局表里。

### 16C.5 build.gradle.kts 与版本目录核对

| 事实 | 出处 |
|---|---|
| `chartsVicoMain` 依赖仅一条：`implementation(libs.vico.compose)` | feature/home/build.gradle.kts:18 |
| 别名 `vico-compose` = `com.patrykandpatrick.vico:multiplatform`，`version.ref = "vico"` | gradle/libs.versions.toml:107 |
| `vico = "2.3.0"` | gradle/libs.versions.toml:46 |
| 目录注释明确"CPF 无 ohos 适配 — keep off commonMain/ohosMain" | gradle/libs.versions.toml:44 |
| 建源集注释："Vico 传递的是 stock Compose 1.9.2；工程自己的 compose.* 由 commonMain 胜出" | feature/home/build.gradle.kts:17 |
| **但当前源码零处 import Vico**：`grep -rn "import com.patrykandpatrick" --include="*.kt" .`（排除 build/）→ 无输出（exit=1） | 实测，见 16C.6 |

即：**依赖仍声明、名字仍叫 vico，实现已整体退化为 Canvas**——vico.kt:36–38 给出退化原因：
"Vico multi-series clusters all rods into one x-group and blows MSE"（Vico 多序列柱会把 4 根杆
聚进同一 x 组，视觉误差爆炸），于是 bars 也改用 Canvas；commonMain KDoc :11 仍写着
"Vico multiplatform 2.3 (bars) + Canvas ring"，属于**过期注释**（详见 16C.7 坑 1）。

### 16C.6 资源目录规模实测

`feature/home` **没有任何 composeResources**：commonMain 下只有 `kotlin/` 一个目录，
本模块的"资产"就是上述代码内 ImageVector。执行命令与输出：

```
$ ls feature/home/src/commonMain/
kotlin
$ find feature/home/src/commonMain/composeResources -type f
find: feature/home/src/commonMain/composeResources: No such file or directory
$ find feature/home -type d -name composeResources        # 全模块范围
（无输出）
$ find feature/home -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.webp" \
    -o -name "*.ttf" -o -name "*.otf" -o -name "*.svg" \)
（无输出）
```

对照（AGENTS.md 约定资源集中在 `:core:design`）：

```
$ find core/design/src/commonMain/composeResources -type f | awk -F. '{print $NF}' | sort | uniq -c
   2 jpg
 168 png
   1 ttf
$ find core/design/src/commonMain/composeResources -type f | wc -l
171
```

目录结构为 `core/design/src/commonMain/composeResources/{drawable,font}/`（png 168 + jpg 2 +
ttf 1，共 171 文件）。home 需要的位图都从这里取，不落地在自己的模块里。

### 16C.7 真实状态与坑

1. **"vico" 名不副实**：源集名、文件名（AnalyticsCharts.vico.kt）、commonMain KDoc
   （AnalyticsCharts.kt:11 "Vico … (bars)"）、build.gradle.kts 头注释（:1 "Vico 图表"）、
   以及 `libs.vico.compose` 依赖（build.gradle.kts:18）都指向 Vico，但代码实际零 Vico 调用
   （16C.5 实测 grep）。依赖目前是**死重量**：仍参与解析、仍传递 stock Compose 1.9.2 进
   android/ios 编译（:17 注释解释过冲突如何被 commonMain 仲裁压住）。删除前需确认无人计划回归 Vico。
2. **Harmony 图表区为空白**：ohosMain 两个 actual 是纯 `Box`（`AnalyticsCharts.ohos.kt:20`、`:32`），
   报表列表行在鸿蒙上只剩文字/状态胶囊（HomeRouteHost.kt:2114–2127 的行内布局），环与柱不渲染。
   占位只保尺寸，无任何"降级绘简图"逻辑。
3. **柱图归一化基准写死在 PV**：`maxY = pv.coerceAtLeast(1f)`（vico.kt:118），若业务数据
   uv>pv 会超出后 `coerceIn(0,1)` 截顶（:131），4 柱恒等高的场景需要注意。
4. **环图 0% 与非法值视觉不同但都近空**：`rate=0f` 合法（`valid=true`），值弧仍画 `coerceAtLeast(1f)`
   度（:78），显示 "0%"；`rate=null/NaN/负` 才显示 "—"（:48）。调用方语义要传分数不是百分数。
5. **挖洞 vs 描边两套 fill 规则并存**：HomeIcons 的 NonZero 反向内轮廓（如 :56–77 铃铛）与
   HomeStrokeIcons 的开放 stroke 路径（roundRect 近似 :93–104）不可混用；改图标时先判断该进哪个对象，
   判据即文件头 KDoc（HomeStrokeIcons.kt:12–23）。
6. **颜色全部硬编码**：图表 5 色（vico.kt:29–33）与描边/填充黑 `Color(0xFF000000)` 均未走
   `DemoColors` token（AGENTS.md 建议优先 token；这里是例外，改动需同步 Flutter SoT 色值）。
7. **forServiceLabel 跨对象引用**："话术库" 返回 `HomeIcons.ChatBubble`（HomeStrokeIcons.kt:61），
   是唯一一个实心图标从描边分发器流出的路径；字符串匹配服务名，改名即失配（fallback Apps :64）。
8. **chart 文案写死中文**（"转化率" vico.kt:96、标签 PV/UV/点/转 :115），无字符串资源层，
   与本模块其余屏幕的硬编码文案风格一致，非本分册引入的问题。

---
*校验方式：以上每条行号均来自对 5 个源文件整读 + 对消费方（HomeScreen/HomeSections/HomeRouteHost/
NativeJetpackHome）的 grep 行号引用；未通读消费方全文。*


