# 19 · 社交内容域全解：`feature/media` + `feature/community` + `feature/chat`

> 覆盖范围：`feature/media/`（10 个 `.kt`）、`feature/community/`（9 个 `.kt`）、`feature/chat/`（9 个 `.kt`）+ 三份 `build.gradle.kts`。
> 为讲清「谁调用 / 被谁装配」，另引用了 `component/chat/`（端口与 mock 引擎）、`composeApp/`（装配层）、`core/router|ui|platform|network`（被消费侧）。
> 本文结论均来自**整篇读源码**后的静态证据；**未执行 gradle / build / test**，故「测试存在」≠「测试当前通过」。

## 0. 阅读约定（路径缩写）

| 缩写 | 展开后的真实路径 |
|---|---|
| `<FM>` | `feature/media/src/commonMain/kotlin/com.example.my_kmp_project/feature/media` |
| `<FM-a>` / `<FM-i>` / `<FM-o>` | `feature/media/src/{androidMain,iosMain,ohosMain}/kotlin/com.example.my_kmp_project/feature/media` |
| `<FC>` | `feature/community/src/commonMain/kotlin/com.example.my_kmp_project/feature/community` |
| `<FC-http>` | `<FC>/http` |
| `<FK>` | `feature/chat/src/commonMain/kotlin/com.example.my_kmp_project/feature/chat` |
| `<CC>` | `component/src/commonMain/kotlin/com.example.my_kmp_project/component/chat` |
| `<APP>` | `composeApp/src/commonMain/kotlin/com.example.my_kmp_project/app` |
| `<APP-a>` | `composeApp/src/androidMain/kotlin/com.example.my_kmp_project/nativeshell` |

`文件:行号` 一律指**仓库相对路径**的行号。

---

## 1. 三模块定位 + 依赖实证

### 1.1 分层位置

三模块都在分层图的 ② `:feature:*` 业务叶子层：只向下依赖 `core:*` 与 `:component`，**彼此零依赖**，也不允许被 `core`/`component` 反向 import；跨模块跳转一律经 ① `composeApp` 装配层 + `CrossFeatureScreens` 端口。

| 模块 | 职责（模块头注释实证） | Gradle 依赖边（实证） | 是否被 `:composeApp` 直接装配 |
|---|---|---|---|
| `:feature:media` | 「短视频 / 跟读 / 音乐三个岛 + 平台播放器 expect-actual」`feature/media/build.gradle.kts:1`；「android·ios·ohos 的 MediaPlayer actual 已跟着模块搬进各自 source set，`:composeApp` 不再持有」`:2-3` | `api(projects.core.ui)`:15；`api(projects.core.router)`:17；`api(projects.core.network)`:18；测试 `implementation(projects.core.network)`:21 | 是：`<APP>/AppShell.kt:68` import `MediaEntryScreen`，`:470` `AppRoute.Media -> MediaEntryScreen(...)`；`<APP>/AppCrossFeatureScreens.kt:14-16,64,69-70,73-74` |
| `:feature:community` | 「社区信息流 / 发帖 / 搜索（mock 引擎 + http 仓储两套实现）」`feature/community/build.gradle.kts:1` | `api(projects.core.ui)`:14；`api(projects.core.router)`:16；`api(projects.core.network)`:17；测试 `implementation(projects.core.network)`:20 | 是：`<APP>/AppShell.kt:63` import、`:400` `MainTab.Community -> CommunityScreen()`；`<APP-a>/NativeAndroidMain.kt:570` 直挂 `CommunityRouteHost(...)` |
| `:feature:chat` | 「IM 会话 / 群组 / 资料 / 消息备份 API + 聊天页」；「**引擎抽象（ImEngine / MockImEngine / ImEngineStore）在 `:component`，本模块只做 API 与 UI**」`feature/chat/build.gradle.kts:1-2` | `api(projects.core.ui)`:15；`api(projects.core.network)`:17；`api(projects.component)`:20（`:18-19` 注明 component 以 api 带出 `core.base`（`AppUuid`）+ `core.router`/`core.network`）；测试 `implementation(projects.core.network)`:23 | 是：`<APP>/AppShell.kt:60` import、`:394-399` `MainTab.Chat -> ChatScreen(onOpenFriends = { shellRoute = AppRoute.Friend ... })`；`<APP-a>/NativeJetpackTabs.kt:94,114` 直接用 `ImEngineStore.engine` |

三个 `build.gradle.kts` 形状一致（插件块 `kotlinMultiplatform` → `androidLibrary` → `composeMultiplatform` → `composeCompiler`，media `:5-10` / community `:4-9` / chat `:5-10`；`android { namespace = ... }` 分别在 media `:26`、community `:25`、chat `:28`），因为 target 集、Android 基础配置与底层依赖由根工程统一接线：

| 共享接线内容 | 实证 |
|---|---|
| 作用域谓词（只有 `:core:*` / `:feature:*` 吃这套）+ 仓库顺序（eazytec 第一并排除 `com.android.tools.build`） | `build.gradle.kts:35`、`:40-55` |
| android target + JVM 17 + compileSdk/minSdk；库模块一律不产 framework（`iosX64/iosArm64/iosSimulatorArm64/ohosArm64/ohosX64`） | `build.gradle.kts:70-92`、`:94-102` |
| serialization 插件 + `coroutines`/`atomicfu`/`serialization-json` + `kotlin-test`；`iosMain` / `ohosMain` 中间 source set 层级 | `build.gradle.kts:104-116`、`:118-127` |
| 模块清单（community / media / chat 同批 include） | `settings.gradle.kts:36-43`（注释「模块之间禁止相互依赖（R7 + check-layer-deps.sh 第 1/3 条）」`:36`，列表 `:40`） |
| 层门三条轴（feature→feature import / 低层向上 import / `implementation(projects.feature.X)`） | `scripts/check-layer-deps.sh:1-9`，实现 `:19-28`（轴 1）、`:30-38`（轴 2）、`:40+`（轴 3） |

### 1.2 `core.platform` 的真实使用面（决定第 4 节结论）

三个模块对 `core:*` 的横向能力**只用到了极小一撮**，全部可枚举：

| 被 import 的符号 | 调用点 | 用途 |
|---|---|---|
| `core.platform.showPlatformToast` | `<FM>/VideoRouteHost.kt:56`、`<FC>/CommunityRouteHost.kt:59` | 发布成功 / mock 操作提示 |
| `core.platform.loadStringList` / `saveStringList` / `platformTodayYmd` | `<FC>/CommunityRouteHost.kt:56-58`（用于 `:168-169`、`:228`） | 社区公约「当天已确认」闸门 |
| `core.platform.loadString` / `saveString` | `<FK>/ImSessionApi.kt:7-8`（用于 `:118-119`） | mock IM userId 映射表持久化 |

反向证据（同一轮 grep，命中 0 行）：`feature/media`、`feature/community`、`feature/chat` 三目录内**不存在** `openUrl`、`PlatformWebView`、`PlatformCameraScan`、`CameraScan`、`copyToClipboard`、`WebBridgeHost`、`pickImage`、`MultipartBody`、`uploadFile` 任何一处引用 —— 即三模块**没有调用任何 WebView / 相机 / 打开外链 / 选图 / 文件上传的平台能力**。详见第 4 节。

`core.platform` 侧对应定义（备查）：`core/platform/src/commonMain/.../PlatformOpen.kt:4-14`（`openUrl` / `copyToClipboard` / `showPlatformToast` / `openSystemNotificationSettings` 的 expect）、`core/platform/src/commonMain/.../StringListStore.kt:16-31`（`loadStringList`/`saveStringList`/`loadString`/`saveString` 共用 store 名 `"home_favorites"`）+ `:34`（`expect fun platformTodayYmd()`）。

### 1.3 `core.network` 门面（三模块共用同一套语义）

| 能力 | 定义 | 三模块用法 |
|---|---|---|
| `NetworkFacade.api(): ApiClient` | `core/network/src/commonMain/.../NetworkFacade.kt:9`（对象）、`:34`（`fun api(): ApiClient = client`） | 仓储默认参数：`<FM>/ShortVideoRepository.kt:125`、`<FC-http>/HttpCommunityPostRepository.kt:79`（`client ?: NetworkFacade.api()` **每次调用重解析**，为的是刷新 token 后拿新实例）、`<FK>/ImSessionApi.kt:141`、`ImUserProfileApi.kt:40`、`ImGroupApi.kt:85`、`ImMessageBackupApi.kt:149` |
| `ApiResponse<T>{code,message,data,isSuccess}` + `NetworkError.Business/Transport` | `ApiResponse.kt:10`、`:21`（`isSuccess = code == BFF_OK \\|\\| code == OK`）、`:29-30` | 三模块一律 `if (!response.isSuccess) throw NetworkError.Business(...)` |
| `NetworkJson`（宽松、`explicitNulls=false`、`encodeDefaults`） vs `NetworkJsonExplicitNulls` | `NetworkJson.kt:6` / `:19` | 需要「显式 null 上送」的两处刻意用后者：`<FM>/ShortVideoRepository.kt:162-165`、`<FC-http>/HttpCommunityPostRepository.kt:163-165` |
| 助手 `getApi`/`postApi`/`deleteApi`/`deleteApiOk`/`getListPage`（`ApiClient.kt:29,34,49,143,84`）、`queryOf`/`pageQuery`/`decodeListPage`（`ListPage.kt:92,99,52`）、`decodeEnvelopeData`（`ApiEnvelopeParser.kt:101`）、`ListPage<T>.hasMore(pageSize)`（`ListPage.kt:43`）；A 型分页方言（`list` / `items` / 裸数组 + `pagination`）说明见 `openspec/changes/sync-flutter-full-parity/notes/api-contract-2026-10-16.md:251-254` | 同左 | 社区侧**故意绕开** `getListPage`（理由 `<FC-http>/HttpCommunityPostRepository.kt:265-272`：`decodeListPage` 会把缺失/畸形 `data` 归一成空页，而 Dart 此时抛错），用点在 `<FC-http>:281`；短视频用点在 `<FM>/ShortVideoRepository.kt:148` |

### 1.4 路由入口（谁把这些屏幕拉起来）

| 路由常量 | 定义 | 装配/分发点 |
|---|---|---|
| `ContentRoutes.Music=/music/list`、`MusicNowPlaying=/music/now_playing`、`MediaEntry=/media/entry` | `core/router/src/commonMain/.../FeatureRoutes.kt:157-177`（`:161-163`） | `<APP>/AppCrossFeatureScreens.kt:63-70` |
| `VideoRoutes`（9 条 `/video*`） | `FeatureRoutes.kt:180-198` | `<APP>/AppCrossFeatureScreens.kt:33`（`handlesSiblingScreen`）、`:44` 与 `:73` → `VideoRouteHost(route,onBack,onNavigate)` |
| `CommunityRoutes`（7 条 `/community*`） | `FeatureRoutes.kt:200-217` | Android：`<APP-a>/NativeAndroidMain.kt:569-578`（overlay 分支）+ `:711-726`（`JetpackCommunityRoot` 的预览/播放出口）；CMP：`<FC>/CommunityScreen.kt:86-98` |
| `AppRoutes.Chat.CHAT=/chat`、`DETAIL=/chat/detail` | `core/router/src/commonMain/.../AppRoutes.kt:22-25`；`MainTab.Chat` 枚举 `:37`+`:43` | `core/router/src/commonMain/.../DeepLinkRouter.kt:60,62,91-92`；字符串常量 `AppRoutePath.kt:43-44` |
| `AppRoute.Media`（`data object Media : AppRoute`） | `core/router/src/commonMain/.../AppNavigator.kt:20` | `<APP>/AppShell.kt:246,470` |
| `/video/short/play` 等字符串路径 | `core/router/src/commonMain/.../AppRoutePath.kt:74-78`、跟读四条 `:107-110`、音乐两条 `:97-98` | 供 iOS/OHOS `NativeFeatureCatalog` 侧对齐 |

---

## 2. 文件清单表

### 2.1 `feature/media`（10 个 `.kt`）

| 路径 | 作用 | 关键行号 |
|---|---|---|
| `<FM>/MediaPlayer.kt` | 播放器契约 + 跨屏音乐会话单例 | 接口 `:8-15`；`StubMediaPlayer :18-43`；`expect createMediaPlayer() :45`；`MusicSession :48-73`（样例 URL `:56-57`） |
| `<FM-a>/MediaPlayer.android.kt` | `android.media.MediaPlayer` 真实音频解码 actual | `actual :12`；`setSource :26-66`；`play :68-87`；`pause/stop/release :89-113`；`releaseInternal :115-122`；「Video surface hosting remains a follow-up」`:16` |
| `<FM-i>/MediaPlayer.ios.kt` | `AVPlayer` 音频 actual + `AVAudioSession` 配置 | `actual :21-22`；`setSource :33-46`；`play :48-62`；`ensureAudioSession :90-96` |
| `<FM-o>/MediaPlayer.ohos.kt` | **OHOS 空实现** | `actual = StubMediaPlayer()` `:7`；缺口注释 `:4-5` |
| `<FM>/ShortVideoModels.kt` | 短视频 72–78 号契约的线上形状 + 宽松解码 | 枚举 `:25-39`；`TopicBrief :45-51`；`Stats :58-67`；`Profile :76-84`；`ItemModel :93-143`；三个 `*FromJson :151-214`；宽松 reader `:217-236` |
| `<FM>/ShortVideoRepository.kt` | 短视频端口 + HTTP 实现 | `interface :23-76`；`ShortVideoListPage :81-84`；路径表 `:90-97`；请求体 `:104-111`；`HttpShortVideoRepository :124-223` |
| `<FM>/VideoHubScreen.kt` | 「音视频」入口岛（选择页 / 视频中心 / 播放页） | `MockVideo :45-49`；`MediaEntryRoute :50-54`；`MediaEntryScreen :58-99`；`VideoHubScreen :147-237`；`VideoPlayScreen :239-305` |
| `<FM>/MusicScreens.kt` | 音乐列表 / Now Playing / MiniPlayerBar | `MockTrack :35-39`；`FlutterTracks :42-49`；`MusicListScreen :51-87`；`MusicNowPlayingRoute :89-103`；`NowPlayingScreen :105-168`；`MiniPlayerBar :171-223` |
| `<FM>/VideoRouteHost.kt` | `/video*` 九条路由的宿主（SoT 整幅位图 mock） | `VideoRouteHost :125-169`；`ShortVideoPage :260-275`；**无人调用的死代码 `:277-414`**；`ShortVideoPlayPage :416-473`；跟读四条 `:510-587` |
| `<FM>/../../../../commonTest/.../ShortVideoHttpRepositoryTest.kt` | 仓储契约测试（14 个 `@Test`） | 完整路径 `feature/media/src/commonTest/kotlin/com.example.my_kmp_project/feature/media/ShortVideoHttpRepositoryTest.kt`；`FakeApiClient :35-102`；用例 `:119-388` |

### 2.2 `feature/community`（9 个 `.kt`）

| 路径 | 作用 | 关键行号 |
|---|---|---|
| `<FC>/CommunityScreen.kt` | 社区主 tab（本地路由状态机 + 信息流） | `CommunityScreen :52-100`；`CommunityFeedContent :102-207`；`EnginePostCard :209-294` |
| `<FC>/MockCommunityEngine.kt` | 5 个数据行类型 + 内存 mock 引擎（当前**唯一被 UI 使用**的实现） | 行类型 `:12-57`；`CommunityMockStore :60-62`；`MockCommunityEngine :69-401`（`posts :86-111`、`createPost :139-185`、`ensureSeed :336-379`） |
| `<FC>/CommunityRouteHost.kt` | 7 条二级路由宿主（发帖 / 搜索 / 公约 / 话题 / 预览 / 播放 / 评论） | `CommunityPublishBus :77-81`；`CommunityRouteHost :83-146`；`CommunityPublishScreen :148-329`；`CommunitySearchScreen :363-516`；`CommunityImagePreviewScreen :606-660`；`CommunityVideoPlayScreen :662-687`；`CommunityCommentScreen :689-782` |
| `<FC-http>/CommunityPostModels.kt` | #59–#71 的响应/请求模型 + 宽容时间解析 | `CommunityDefaultSource :31`；`parseCommunityTimeMs :47-87`；`TopicModel :155-188`；`CommentModel :194-220`；`PostModel :236-316`；`CommunityUserHit :332-347`；`CommunitySearchAllResult :357-372`；`PostCreateRequest :379-389`；`CommentCreateRequest :396-400` |
| `<FC-http>/CommunityPostRepository.kt` | 14 个 suspend 方法的端口（**当前无生产实现被接线**） | 诚实声明 `:20-23`；`fetchPosts :32-36`、`fetchComments :42`、`addComment :45-49`、`toggleLike :58`、`deletePost :61`、`createPost :69-75`、`fetchTopics :78-81`、`searchTopics :88-92`、`followUser :95`、`unfollowUser :98`、`searchAll :104-108`、`searchPosts :111-115`、`searchTopicsPage :118-122`、`searchUsers :125-129` |
| `<FC-http>/HttpCommunityPostRepository.kt` | 端口唯一 HTTP 实现 | 路径表 `CommunityApiPaths :30-57`；类 `:75-295`；`api` 逐调用解析 `:79`；各实现 `:82-246`；`searchPage :253-263`、`listPage :273-282`、`requireData :284-287`、`requireCodeOnly :290-294`；`communityFailure :305-312`、`communityQuery :320-324` |
| `<FC>/../../commonTest/.../MockCommunityEngineTest.kt` | mock 引擎语义（7 个 `@Test`） | 路径 `feature/community/src/commonTest/kotlin/com.example.my_kmp_project/feature/community/MockCommunityEngineTest.kt:14-84` |
| `.../commonTest/.../http/CommunityPostModelDecodeTest.kt` | 解码方言（10 个 `@Test`） | `:96-354` |
| `.../commonTest/.../http/HttpCommunityPostRepositoryTest.kt` | HTTP 契约（18 个 `@Test`） | `RecordingApiClient :41-113`；用例 `:146-487` |

### 2.3 `feature/chat`（9 个 `.kt`）

| 路径 | 作用 | 关键行号 |
|---|---|---|
| `<FK>/ChatScreen.kt` | 会话列表 + 详情（唯一 UI 文件） | `ChatScreen :55-100`；`ChatListContent :102-221`；`ConversationRow :223-315`；`ChatDetailScreen :317-440`；`MessageBubble :442-469` |
| `<FK>/RongImConfig.kt` | IM 路径常量 + **mock 开关** | 说明 `:3-20`；`object RongImConfig :21`；5 条路径 `:24-36`；`CONNECT_TIMEOUT_SECONDS :39`；`PROFILE_CACHE_TTL_MS :42`；`var forceMockIm :48`；`var rongAppKey = "" :54`；`useMockImFor :60-64`；`useMockIm :67`；`imFreeGroupActionPath :77-78` |
| `<FK>/ImJsonRead.kt` | 4 个 `JsonObject` 宽容取值扩展 | `imJsonStr :27-33`、`imJsonText :35-39`、`imJsonInt :45-51`、`imJsonItems :57-60` |
| `<FK>/ImSessionApi.kt` | #100 会话 token + mock userId 注册表 | `ImSessionResult :29-41`；`ImSessionRequest :51-54`；`ImMockUserIdRegistry :63-106`；key 常量 `:110`；`createPersistentImMockUserIdRegistry :116-121`；`GAP(R5) :133-137`；`ImSessionApi :140-194`（mock 分支 `:155-163`、真实分支 `:165-181`） |
| `<FK>/ImUserProfileApi.kt` | #101 IM 用户资料 | `ImUserProfile :18-22`；`GAP(R5) :35-37`；`ImUserProfileApi :39-104`（空列表短路 `:46-48`、mock `:50-59`、真实 GET `:61-66`） |
| `<FK>/ImGroupApi.kt` | #109–#114 群组 HTTP | 结果/请求模型 `:15-62`；`GAP(R5) :79-83`；`ImGroupApi :84-177`（`syncStoreGroup :89`、`createFreeGroup :101`、`invite :118`、`kick :128`、`quit :138`、`dismiss :145`、`postOk :149-157`、`encodedMembers :159-162`、`EMPTY_OBJECT_BODY :166`）；本地 `postApiJson :184-201` |
| `<FK>/ImMessageBackupApi.kt` | #102 消息备份（HTTP + mock 两套 + ISO8601 工具） | 事件模型 `:28-68`；方向枚举 `:71-86`；`interface ImBackupService :100-134`；`HttpImBackupService :148-255`；`MockImBackupService :263-346`；`createImBackupService :352-356`；`ImBackupPayloads :364-387`；`ImIso8601Utc :396-513` |
| `<FK>/../../commonTest/.../MockImEngineTest.kt` | 引擎语义（4 个 `@Test`） | `feature/chat/src/commonTest/kotlin/com.example.my_kmp_project/feature/chat/MockImEngineTest.kt:15-59` |
| `.../commonTest/.../ImChatApiContractTest.kt` | IM HTTP 契约（32 个 `@Test`） | `FakeImApiClient :593-654`、`ImFakeCall :656-661`、手搓 `runNow :667-684`；用例 `:34-591` |

---

## 3. 逐文件详解

### 3.1 `feature/media`

#### 3.1.1 `<FM>/MediaPlayer.kt`（73 行）

| 类型 / 成员 | 完整签名（`:行`） | 参数语义 | 返回 | 内部行为与副作用 |
|---|---|---|---|---|
| `MediaPlayerController` | `interface MediaPlayerController { val isPlaying: Boolean (:9); fun setSource(url: String) (:10); fun play() (:11); fun pause() (:12); fun stop() (:13); fun release() (:14) }`（`:8-15`） | `url` = 媒体地址（当前实现只吃 http(s) 音频串） | 各方法 `Unit`；`isPlaying` 为 Compose 可观察状态 | 纯契约，无副作用 |
| `StubMediaPlayer` | `class StubMediaPlayer : MediaPlayerController`（`:18-43`） | — | — | 只翻 `isPlaying` 布尔 + 记 `sourceUrl`（`:23-27`），**不触碰任何解码器**；`release()` 只把状态清空（`:39-42`） |
| `createMediaPlayer` | `expect fun createMediaPlayer(): MediaPlayerController`（`:45`） | — | 平台实现 | 三端 actual 见 3.1.2–3.1.4 |
| `MusicSession` | `object MusicSession`（`:48-73`），注释 `:47` 明确「Cross-screen music session so Home can show `MiniPlayerBar`」 | — | — | 单例，`trackTitle`（`:49-50`）/`artist`（`:51-52`）为 `mutableStateOf` + `private set`；`val player = createMediaPlayer()`（`:53`）在对象首次触达时构造 |
| `MusicSession.SAMPLE_AUDIO_URL` | `const val SAMPLE_AUDIO_URL = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"`（`:56-57`） | — | 公网样例 MP3 | 三处屏幕用它当「播放」的真实声源：`<FM>/MusicScreens.kt:71`（经 `start`）、`<FM>/VideoHubScreen.kt:288`、`<FM>/VideoRouteHost.kt:436` |
| `MusicSession.isActive` | `val isActive: Boolean get() = trackTitle != null`（`:59`） | — | 是否有会话 | 只读派生 |
| `MusicSession.start` | `fun start(title: String, artistName: String, audioUrl: String = SAMPLE_AUDIO_URL)`（`:61-66`） | 曲名 / 艺人名 / 音频 URL | `Unit` | 顺序：写 title+artist → `player.setSource(audioUrl)` → `player.play()`；**真实副作用 = 平台播放器起播** |
| `MusicSession.dismiss` | `fun dismiss()`（`:68-72`） | — | `Unit` | `player.stop()` + 清 title/artist；**没有调 `player.release()`** —— Android 侧 `stop()` 已释放原生播放器（`<FM-a>:98-106`），iOS 侧 `stop()` 只是 `pause()`（`<FM-i>:70-74`），因此 iOS 上 AVPlayer 实例与会话激活态会一直留到进程结束 |

调用面：`MusicSession.start` ← `<FM>/MusicScreens.kt:71`（整图点击）、`:94`（Now Playing 兜底自动起播）；`MusicSession.dismiss` ← `<FM>/MusicScreens.kt:154`、`:218`；`MusicSession.player` ← `<FM>/MusicScreens.kt:110`、`:177`。

#### 3.1.2 `<FM-a>/MediaPlayer.android.kt`（123 行）

| 成员 | 签名（`:行`） | 行为 |
|---|---|---|
| `actual fun createMediaPlayer()` | `actual fun createMediaPlayer(): MediaPlayerController = AndroidMediaPlayer()`（`:12`） | — |
| `setSource` | `override fun setSource(url: String)`（`:26-66`） | 先 `releaseInternal(keepUrl = false)`，再 `MediaPlayer()` + `setDataSource(url)` + `prepareAsync()`，挂 `setOnPreparedListener{ if (wantPlay) start() }`（`:31-60`）；出错走 `setOnErrorListener` 打日志（`:61-65`）。日志 TAG `"AndroidMediaPlayer"`（`:10`） |
| `play` | `override fun play()`（`:68-87`） | 未 prepared 时**容忍**：置 `wantPlay = true`，等 prepared 回调起播 |
| `pause` / `stop` / `release` | `:89-96` / `:98-106` / `:108-113` | `stop()` 会真正 `release()` 原生对象并清空；`release()` 同 |
| `releaseInternal(keepUrl)` | `private fun releaseInternal(keepUrl: Boolean)`（`:115-122`） | 保不保 `pendingUrl` 的两种清理 |

文件注释 `:15-16` 是这一层最重要的自证：「Real Android `MediaPlayer` for audio streams (music / sample URL). **Video surface hosting remains a follow-up**; audio decode is enough for 7.2 smoke.」→ Android 侧只有音频解码，**没有任何视频画面输出**。

#### 3.1.3 `<FM-i>/MediaPlayer.ios.kt`（97 行）

| 成员 | 签名（`:行`） | 行为 |
|---|---|---|
| `actual fun createMediaPlayer()` | `@OptIn(ExperimentalForeignApi::class) actual fun createMediaPlayer(): MediaPlayerController = IosAvMediaPlayer()`（`:21-22`） | — |
| `setSource` | `override fun setSource(url: String)`（`:33-46`） | `pendingUrl = url` → **`wantPlay = false`（`:35`）** → `releaseInternal(keepUrl = true)` → `NSURL.URLWithString(url) ?: return`（`:37`）→ `ensureAudioSession()` → `AVPlayerItem(uRL = nsUrl)` → `replaceCurrentItemWithPlayerItem(item)` → `if (wantPlay) { play(); ... }`（`:42-45`）。由于 `:35` 刚把它清零，**`setSource` 自身永远不会起播**；实际起播依赖紧随其后的 `play()`（`<FM>/MediaPlayer.kt:64-65`（`MusicSession.start`） 正是这个顺序） |
| `play` | `override fun play()`（`:48-62`） | `wantPlay = true` + `ensureAudioSession()`；player 为空时先用 `pendingUrl` 建 item 再播 |
| `pause` | `:64-68` | `p.pause()` |
| `stop` | `override fun stop()`（`:70-74`） | **只 `pause()`**，不置空 item、不 release |
| `release` | `:76-84` | 真正 `replaceCurrentItemWithPlayerItem(null)` + 置空 |
| `ensureAudioSession` | `:90-96` | `AVAudioSession.sharedInstance()` 设 `AVAudioSessionCategoryPlayback` + `setActive(true)` |

注释 `:18-19`：「iOS AVPlayer audio — parity with Android MediaPlayer for music smoke. **Video surface hosting remains a follow-up.**」

#### 3.1.4 `<FM-o>/MediaPlayer.ohos.kt`（7 行）—— **OHOS 是空实现**

```
:4  // OHOS: no AVPlayer/MediaKit bridge yet — stub only.
:5  // Enumerated in platform-gap-registry as missing for media playback.
:7  actual fun createMediaPlayer(): MediaPlayerController = StubMediaPlayer()
```

这一行就是「哪端是空实现」的答案：**OHOS 全端播放器为 `StubMediaPlayer`**，`isPlaying` 只是布尔；缺口登记在 `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md:22`（`media playback | partial | partial | stub`）、`:33`（`music | partial | partial | stub`）。

#### 3.1.5 `<FM>/ShortVideoModels.kt`（236 行）

| 类型 | 签名（`:行`） | 语义要点 |
|---|---|---|
| `ShortVideoStatus` | `enum class ShortVideoStatus { Normal, Reviewing, Uploading }` + `companion fun fromWire(raw: String?)`（`:25-39`） | 未知/空值一律回落 `Normal` |
| `ShortVideoTopicBrief` | `data class ShortVideoTopicBrief(id: String, name: String)`（`:45-51`），`val displayName: String get() = "#$name"` | — |
| `ShortVideoStatsModel` | `data class ShortVideoStatsModel(videoCount: String, viewCount: String, likeCount: String)`（`:58-67`），`companion EMPTY = ShortVideoStatsModel("0","0","0")` | **三个计数刻意是 String**：#72 的线上把数字序列化成了字符串，测试 `feature/media/src/commonTest/.../ShortVideoHttpRepositoryTest.kt:119` 锁死该行为 |
| `ShortVideoProfileModel` | `data class ShortVideoProfileModel(userId, displayName, avatarUrl, roleBadge, storeName, isMe: Boolean = true, stats)`（`:76-84`） | `isMe` 默认 true（Dart 缺省即「自己的主页」） |
| `ShortVideoItemModel` | `data class ShortVideoItemModel(id, title, coverUrl, videoUrl, viewCount: Int?, likeCount: Int, duration: Double, aspectRatio: Double = 1.25, topic: ShortVideoTopicBrief?, isLiked: Boolean, status)`（`:93-143`） | `withCounts(likeCount: Int?, viewCount: Int?, isLiked: Boolean?): ShortVideoItemModel`（`:115-125`）= 片段合并，**传 null 表示保留原值**；`companion fun fromLikeFragment(json: JsonObject): ShortVideoItemModel?`（`:133-141`） |
| `shortVideoItemFromJson` | `fun shortVideoItemFromJson(el: JsonElement?): ShortVideoItemModel?`（`:151-173`） | 非 `JsonObject` → null；字段用宽松 reader 取值 |
| `shortVideoLikeFragmentFromJson` | `fun shortVideoLikeFragmentFromJson(el: JsonElement?): ShortVideoItemModel?`（`:180-187`） | #76/#77 只回 `{id,like_count,is_liked}` |
| `shortVideoProfileFromJson` | `fun shortVideoProfileFromJson(el: JsonElement?): ShortVideoProfileModel?`（`:193-214`） | `stats` 子对象缺 → `EMPTY` |
| 宽松 reader | `JsonObject.textOf(:217)`、`intOf(:219)`、`doubleOf(:221)`、`isTrue(:223)`、`flexibleInt(:226-230)`、`flexibleDouble(:232-236)` | 数字可能是 JSON number、字符串或 bool，全部吸收 |

#### 3.1.6 `<FM>/ShortVideoRepository.kt`（223 行）

端口 `interface ShortVideoRepository`（`:23-76`），逐条对应契约表 #72–#78（`api-contract-2026-10-16.md:177-187`）：

| 方法 | 签名 | 契约 | 关键语义 |
|---|---|---|---|
| `fetchProfile` | `suspend fun fetchProfile(userId: String? = null): ShortVideoProfileModel`（`:26`） | #72 | 空/空白 `user_id` 不上送 |
| `fetchList` | `suspend fun fetchList(scope: String, userId: String? = null, page: Int = 0, pageSize: Int = 5): ShortVideoListPage`（`:34-39`） | #73 | 服务端 **1-based** |
| `create` | `suspend fun create(title: String, topicId: String?): ShortVideoItemModel`（`:46`） | #74 | 上传字段恒为 null 占位 |
| `delete` | `suspend fun delete(id: String)`（`:52`） | #75 | `data` 可缺可空 |
| `toggleLike` | `suspend fun toggleLike(id: String, liked: Boolean): ShortVideoItemModel`（`:59`） | #76/#77 | **同一路径切动词** |
| `reportView` | `suspend fun reportView(id: String): Int?`（`:69-74` 注释 + 声明） | #78 | **约定「永不抛错」**：fire-and-forget，失败返回 null |

| 定义 | 位置 | 内容 |
|---|---|---|
| `data class ShortVideoListPage(list: List<ShortVideoItemModel>, hasMore: Boolean)` | `:81-84` | — |
| `object ShortVideoApiPaths` | `:90-97` | `ROOT = "/api/v1/short-videos"`（`:91`）、`PROFILE = "$ROOT/profile"`（`:92`）、`like(id)= "$ROOT/$id/like"`（`:94`）、`view(id)`（`:95`）、`item(id)`（`:96`） |
| `@Serializable data class ShortVideoCreateRequest` | `:104-111` | 六键 snake_case：`title, video_url, cover_url, duration, aspect_ratio, topic_id`，注释 `:101-103` 说明「Dart 总发六键、上传字段占位 null」 |
| `class HttpShortVideoRepository(private val api: ApiClient = NetworkFacade.api())` | `:124-223` | 唯一实现 |
| `fetchProfile` 实现 | `:129-135`，请求在 `:130`（`api.getApi(PROFILE, queryOf("user_id" to userId)) { shortVideoProfileFromJson(it) }`） | `requireData` 失败抛 Business |
| `fetchList` 实现 | `:137-159`；query 组装 `:143-147`（`scope` / `page = page+1` / `size` + 可选 `user_id`）；**分页判定 `:156` `hasMore = data.hasMore(pageSize)`** | 走 `api.getListPage<JsonObject>(ROOT, query)`（`:148`） |
| `create` 实现 | `:161-169`；`:162-165` 用 `NetworkJsonExplicitNulls.encodeToString(...)`；请求 `:166` | 保住 `video_url:null` 等显式 null |
| `delete` 实现 | `:171-177`，`:172` `api.deleteApi(item(id), emptyMap()) { }` | 容忍 `data` 缺失 |
| `toggleLike` 实现 | `:179-188`，`:182` POST / `:184` DELETE | 返回片段后用 `withCounts` 合并 |
| `reportView` 实现 | `:190-197` | `runCatching { ... }` 包裹（`:190`），任何失败 → null |
| `requireData` / `businessError` | `:199-205` / `:208-214` | 服务端 message 优先，空回落中文 |
| 中文兜底串 | `:217-221` | 「加载资料失败 / 加载小视频失败 / 发布失败 / 删除失败 / 操作失败」 |

**被谁调用**：仅 `feature/media/src/commonTest/.../ShortVideoHttpRepositoryTest.kt`（全仓 grep `HttpShortVideoRepository` 只命中定义文件与测试文件）→ **生产链路里没有短视频 HTTP 消费者**，UI 用的是 mock（见 3.1.9）。

#### 3.1.7 `<FM>/VideoHubScreen.kt`（305 行）

| Composable | 签名（`:行`） | 内部行为 | 副作用 / 被谁调用 |
|---|---|---|---|
| `MediaEntryScreen` | `@Composable fun MediaEntryScreen(onBack: () -> Unit)`（`:58-99`） | 本地 `var route by remember { mutableStateOf(MediaEntryRoute.Chooser) }`（`:59`）三态机：`Chooser` → 两张 `MediaChooserCard`；`VideoHub` → `VideoHubScreen`；`Music` → `MusicListScreen`。三个分支各包一层 `ReportMainTabRoot(isRoot = false)`（`:64`、`:91`、`:95`） | 无 IO。调用：`<APP>/AppShell.kt:470`、`<APP>/AppCrossFeatureScreens.kt:70`、`:74`（未知 content 路由的兜底也落这里） |
| `MediaEntryRoute` | `private enum class MediaEntryRoute { Chooser, VideoHub, Music }`（`:50-54`） | 模块私有，不外露 | — |
| `MediaChooserCard` | `@Composable private fun MediaChooserCard(title: String, subtitle: String, cover: Any?, onClick: () -> Unit)`（`:101-145`） | 封面用 `painterResource(Res.drawable.home_transliter_home_cover_01 / music_defaults_music_record)` | 纯展示 |
| `VideoHubScreen` | `@Composable fun VideoHubScreen(onBack: () -> Unit)`（`:147-237`） | 硬编码 4 条 `MockVideo`（`:150-157`：口语跟读第一课 / 周末活动花絮 / 学习打卡短视频 / 校园开放日回顾），横幅文案 `:184`「Mock 视频列表（音频轨可用 Android MediaPlayer；画面 Surface 待接）」，点条目 → `VideoPlayScreen` | 无 IO。外部无调用者（仅 `MediaEntryScreen`） |
| `VideoPlayScreen` | `@Composable private fun VideoPlayScreen(video: MockVideo, onBack: () -> Unit)`（`:239-305`） | `val player = remember { createMediaPlayer() }`（`:244`）；播放按钮 `:284-291`：`player.setSource(MusicSession.SAMPLE_AUDIO_URL)` + `player.play()`（`:288-289`） | **真实副作用：起播一条公网 MP3**；`video.videoUrl` 之类的画面源不存在，MockVideo 只有 `id/title/durationLabel`（`:45-49`） |

#### 3.1.8 `<FM>/MusicScreens.kt`（223 行）

| Composable | 签名（`:行`） | 行为与真相 |
|---|---|---|
| `FlutterTracks` | `private val FlutterTracks = listOf(MockTrack(...))`（`:42-49`） | 6 条硬编码曲目（`t1`…`t6`），无任何网络来源 |
| `MusicListScreen` | `@Composable fun MusicListScreen(onBack: () -> Unit, onOpenNowPlaying: (() -> Unit)? = null)`（`:52-87`） | 整屏一张位图 `Res.drawable.music_list_body` + `ContentScale.FillBounds`（`:64-76`）；**整图 clickable** = `MusicSession.start(tracks.first().title, ...)` 再 `onOpenNowPlaying?.invoke()`（`:69-73`）；返回箭头是 `color = Color.Transparent` 的隐形 "‹"（`:76-85`）。无列表、无滚动、无曲目选择 |
| `MusicNowPlayingRoute` | `@Composable fun MusicNowPlayingRoute(onBack: () -> Unit)`（`:89-103`） | 若 `MusicSession.trackTitle == null` 则**自动** `MusicSession.start(fallback...)`（`:93-95`）→ 进入该路由即真实起播；随后 `ReportMainTabRoot(isRoot = false)`（`:98`） |
| `NowPlayingScreen` | `@Composable private fun NowPlayingScreen(track: MockTrack, onBack: () -> Unit)`（`:105-168`） | 状态文案 `:139`「播放中（Stub）/ 已暂停」；缺口说明 `:162`「播放器缺口：OHOS/真机解码见 platform-gap-registry」；`:154` `MusicSession.dismiss()` |
| `MiniPlayerBar` | `@Composable fun MiniPlayerBar(modifier: Modifier = Modifier, onOpenNowPlaying: (() -> Unit)? = null)`（`:171-223`） | `:176` 无会话时 `return`；**全仓无任何调用点**（`<FM>/MediaPlayer.kt:47`（`MusicSession` 的 KDoc） 的 KDoc 只是「计划给 Home 用」的注释） |

调用：`MusicListScreen` ← `<APP>/AppCrossFeatureScreens.kt:64`；`MusicNowPlayingRoute` ← `:69`。

#### 3.1.9 `<FM>/VideoRouteHost.kt`（588 行）

| 定义 | 签名（`:行`） | 行为 |
|---|---|---|
| `VideoRouteHost` | `@Composable fun VideoRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {})`（`:125-169`） | `when` 覆盖 `VideoRoutes` 9 条：`Hub :132-136`、`Short :137-142`、`ShortPlay :143`、`ShortPublish :144-147`、`ShortHelp :148`、`TransliterVideos :149-153`、`TransliterVideoDetail :154-157`、`TransliterWorks :158-161`、`TransliterWorkDetail :162`；未匹配 → 回落 Hub（`:163-167`） |
| 发布分支 | `:144-147` | `onSubmit = { showPlatformToast("已提交（mock）"); onBack() }` —— **不写引擎、不打 HTTP**，连 mock 都不留 |
| `VideoMock` | `private object VideoMock`（`:106-123`） | `shorts` 5 条、`transliter` 3 条、`works` 2 条，全硬编码 |
| `FlutterSoTFullFrame` | `@Composable private fun FlutterSoTFullFrame(drawable: Any, contentDescription: String, onBack: () -> Unit, onTap: (() -> Unit)? = null)`（`:231-258`） | Flutter 截图整幅铺满 + 隐形 "‹" 返回；被 `ShortVideoPage`、跟读详情/作品等复用 |
| `ShortVideoPage` | `:260-275` | 只渲染一张 SoT 位图，`onHelp/onPublish/onPlay` 仅用于路由跳转 |
| `ShortVideoPlayPage` | `@Composable private fun ShortVideoPlayPage(onBack: () -> Unit)`（`:416-473`） | `val player = remember { createMediaPlayer() }`（`:420`）；播放键 `:436` `player.setSource(MusicSession.SAMPLE_AUDIO_URL)`；点赞是本地 toggle；评论 `:458`、分享 `:461` = `showPlatformToast("评论（mock）"/"分享（mock）")` |
| `ShortVideoHelpPage` | `:486-508` | 静态 4 步文案 |
| `TransliterVideoListPage` | `:510-555` | 数据源 `VideoMock.transliter`（`:526`） |
| `TransliterVideoDetailPage` / `TransliterWorkListPage` / `TransliterWorkDetailPage` | `:557-567` / `:568-578` / `:579-587` | 全是 SoT 位图页 |

**死代码（实测无调用点）**：`ShortVideoProfileCard()`（`:277-309`）、`ShortVideoPublishTile(onClick)`（`:311-342`）、`ShortVideoCoverTile(item, onPlay)`（`:344-405`，内部 `:353` 还有「视频上传中，请稍后再试」提示）、`StatCell(label, value)`（`:408-414`，只被 `:304-306` 的 `ShortVideoProfileCard` 用）。文件里 `:79-85` import 了 6 个路由对象（`AiRoutes`/`ClassroomRoutes`/`CommunityRoutes`/`ContentRoutes`/`HomeRoutes`/`MineRoutes`）但**未使用**。

调用：`VideoRouteHost` ← `<APP>/AppCrossFeatureScreens.kt:44`、`:73`（入口判定同文件 `:33` `route.startsWith("/video")`）。

#### 3.1.10 `feature/media/src/commonTest/.../ShortVideoHttpRepositoryTest.kt`（390 行）

`FakeApiClient : ApiClient`（`:35-102`）以 `"VERB path"` 为 key 投放响应；`putRaw/patchRaw/putApi/patchApi`（`:61-63`、`:80-90`）直接 `error("PUT 不在 /api/v1 契约内")`，`postForm`（`:53`）、`getAbsoluteRaw`（`:58`）同样报错 —— 用测试锁死「本模块不许出现契约外动词」。`repoWith(vararg fixtures)`（`:107-114`）。14 个 `@Test` 与行号：`:119` stats 字符串化、`:140` 空白 user_id 省略 + stats 默认、`:156` message→中文兜底、`:184` list query（`{scope,page=+1,size=5,user_id}`）+ 全字段映射、`:215` 无 pagination 时回落满页、`:234` `pagination.hasMore=false` 压过满页、`:248` create 显式 null、`:272` 无话题仍发 `topic_id:null`、`:284` delete 容忍缺/空 data、`:298` delete 失败、`:310` like POST 片段 + `withCounts` 合并、`:336` unlike 走 DELETE、`:347` like 兜底「操作失败」、`:358` view 返回计数并吞掉一切失败（含传输错）。

### 3.2 `feature/community`

#### 3.2.1 `<FC>/CommunityScreen.kt`（294 行）

`@Composable fun CommunityScreen()`（`:52-100`，声明行 `:53`）—— 无参、无返回值，状态全在函数内：

| 片段 | 行号 | 行为 |
|---|---|---|
| 目的地状态机 | `:54` | `var destination by remember { mutableStateOf<String?>(null) }`；`null` = 根信息流 |
| 引擎 | `:55` | `val engine = remember { CommunityMockStore.engine }` ← **UI 唯一数据源是 mock 引擎** |
| 监听（正确写法） | `:59-62` | `DisposableEffect(engine) { val unsub = engine.observe { revision++ }; onDispose { unsub() } }` |
| 根上报 | `:66` | `ReportMainTabRoot(isRoot = true)`（tab 根，符合沉浸区规范） |
| tab 映射 | `:67-71` | `feedTab` 1→`"hot"`、2→`"following"`、其余→`"latest"` |
| 取数 | `:72` | `val posts = remember(feedTab, revision) { engine.posts(tab = tabKey) }` —— **只取第 0 页，UI 没有分页/加载更多** |
| 互动 | `:77-78` | `onToggleLike = { post -> engine.toggleLike(post.id, liked = !post.isLiked) }` |
| 评论出口 | `:79-81` | `destination = "${CommunityRoutes.Comment}?postId=${post.id}"`（带 query 的字符串路由） |
| 子路由 | `:86-98` | 委托 `CommunityRouteHost(route = dest, onBack = {...}, onNavigate = { destination = it })`；`onBack` 映射 `:89-95`：`TopicSelect → Publish`，其余（含 Comment 前缀）→ `null` |

| 私有 Composable | 签名 | 要点 |
|---|---|---|
| `CommunityFeedContent` | `@Composable private fun CommunityFeedContent(posts, feedTab, onFeedTab, onPublish, onSearch, onToggleLike, onOpenComment, metaLabel)`（`:102-207`） | 三个 tab 文案「最新/热门/关注」（`:113`）；`statusBarsPadding()`（`:118`）；空态分 tab 文案（`:185`：`feedTab == 2` → 「还没有关注的人，去搜索关注吧」，否则「暂无动态」） |
| `EnginePostCard` | `@Composable private fun EnginePostCard(post: CommunityFeedItem, meta: String, onLike: () -> Unit, onComment: () -> Unit)`（`:209-294`） | 头像取昵称首字母；媒体行 `:261`「视频」/「图片 ×n」；预览评论 `:268` `previewComments.take(2)`；计数 `:281`「已赞 N / 赞 N」、`:287`「评论 N」，点击分别 `onLike`/`onComment`（`:284`、`:290`） |

#### 3.2.2 `<FC>/MockCommunityEngine.kt`（401 行）—— 当前线上唯一数据源

行类型（全部 `data class`，模块内私有可见但 public）：`CommunityFeedComment :12-20`、`CommunityFeedItem :22-38`（含 `images: List<String>`、`videoUrl/videoCoverUrl: String?`、`likeCount`、`commentCount`、`isLiked`、`isMine`、`previewComments`）、`CommunityUserHit :40-45`、`CommunityTopicHit :47-50`、`CommunitySearchAllResult :52-57`。

| 成员 | 签名（`:行`） | 算法 / 副作用 |
|---|---|---|
| `CommunityMockStore` | `object CommunityMockStore { val engine: MockCommunityEngine by lazy { MockCommunityEngine() } }`（`:60-62`） | **进程级单例**，`<APP-a>/NativeJetpackTabs.kt:845` 与 CMP 侧共享同一实例 |
| `observe` | `fun observe(listener: () -> Unit): () -> Unit`（`:77-80`） | 返回注销函数；内部 `notifyListeners()`（`:82`） |
| `posts` | `fun posts(tab: String, page: Int = 0, pageSize: Int = 10): List<CommunityFeedItem>`（`:86-111`） | 先 `ensureSeed()`；`hot`/`热门` 按 `likeCount * 2 + commentCount` 降序再按时间降序（`:94-98`）；`following`/`关注` 过滤 `followedUserIds`（`:101`）；其余按 `publishAtMs` 降序（`:104`）；分页 = `subList(page*pageSize, min(...))`（`:106-110`），越界返回 `emptyList()` |
| `toggleLike` | `fun toggleLike(postId: String, liked: Boolean): CommunityFeedItem?`（`:113-125`） | `max(0, likeCount ± 1)`，写回并 notify |
| 关注三件套 | `followUser(:127)` / `unfollowUser(:132)` / `isFollowed(:137)` | 内存 `Set<String>` |
| `createPost` | `fun createPost(content: String, mediaType: String = "none", source: String = "来自 Android"): CommunityFeedItem`（`:139-185`） | id = `"post_new_$nowMs"`；`mediaType == "image"` → 塞 3 张 picsum（`:157-165`）；`"video"` → 固定 `bee.mp4` URL + 封面（`:166-175`）；插入表头（`:181`）。**与用户选的任何文件无关** |
| `ingestPublishedBody` | `fun ingestPublishedBody(body: String?, source: String = "来自 Android")`（`:187-192`） | 按正文去重后入表，用于 Android Native tab 与 CMP 覆盖层之间「跨形态补写一条」 |
| `comments` / `addComment` | `comments(postId): List<CommunityFeedComment>`（`:194-197`）；`addComment(postId, content, replyToNickname: String? = null): CommunityFeedComment?`（`:200-230`） | 空正文或未知帖子 → `null`；成功则 commentCount+1 且预览只留 2 条（`:223`） |
| 搜索四件套 | `searchPosts(:232-241)`、`searchUsers(:243-262)`、`searchTopics(:264-272)`、`searchAll(q, pageSize = 5)(:274-280)` | 全部是**内存 contains 过滤**；`searchUsers` 用 `distinctBy { userId }` 并回填 `isFollowed` |
| `defaultPostId` / `metaLabel` | `:282-285` / `:287-288` | 供无 query 的评论路由兜底 / 「N小时前 · 来自 X」文案 |
| 种子与字典 | `companion object :290-333`：`mockTopics :291-296`（4 条）、`pageSlice :298-302`、`sampleContents :304-312`（7 条）、`nicknames :314-316`（8 条）、`avatarUrl(seed) = "https://i.pravatar.cc/200?img=$seed" :318`、`formatPublishTime :320-333` | 时间格式：刚刚 / N分钟前 / N小时前 / 昨天 / N天前 / 更早 |
| `ensureSeed` | `private fun ensureSeed()`（`:336-379`） | `Random(42)` 固定种子生成 **35 条**：`isVideo = i % 10 == 0`、`publishAtMs = now - (i*17 + 0..30) 分钟`、`source` 交替「来自 iPhone」/「来自 Android」、`isLiked = i % 4 == 0`、`isMine = i == 0` |
| `seedComments` | `private fun seedComments(postId: String, now: Long, index: Int)`（`:381-400`） | 每帖 0..3 条评论 |
| `nowMs` | `private fun nowMs(): Long`（`:9-10`，用 `kotlin.time.Clock`） | — |

#### 3.2.3 `<FC>/CommunityRouteHost.kt`（782 行）

`@Composable fun CommunityRouteHost(route: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {}, previewUrls: List<String> = emptyList(), previewIndex: Int = 0, videoUrl: String? = null)`（`:83-146`）

| 路由 | 行号 | 落地行为（**全部走内存引擎，零 HTTP**） |
|---|---|---|
| `Publish` | `:93-105` | `onPublished = { body, mediaType -> CommunityMockStore.engine.createPost(body, mediaType, source = "来自 Android"); CommunityPublishBus.lastPublishedBody = body; ...; showPlatformToast("发布成功"); onBack() }` |
| `Search` | `:106` | `CommunitySearchScreen(onBack)` |
| `Convention` | `:107` | `CommunityConventionScreen(onBack)` |
| `TopicSelect` | `:108-115` | 选中 → `CommunityPublishBus.pendingTopic = name` + toast「已选 #x」+ `onBack()` |
| `ImagePreview` | `:116-122` | `previewUrls.ifEmpty { listOf("https://picsum.photos/seed/preview/800/800") }` |
| `VideoPlay` | `:123-126` | `coverUrl = videoUrl ?: "https://picsum.photos/seed/video_0/640/360"` |
| `Comment`（常量路由） | `:127-130` | **`postId = engine.defaultPostId()`** —— 走常量路由时拿不到真实帖子 |
| `else` + `/community/comment?postId=` 前缀 | `:131-136` | 解析 `postId=`，空则回落 `defaultPostId()` |
| 未知路由 | `:137-144` | `ReportMainTabRoot(isRoot = false)` + `MineTopBar("社区")` + 文案「未知路由 $route」 |

`object CommunityPublishBus { var lastPublishedBody: String? = null; var lastMediaType: String = "none"; var pendingTopic: String? = null }`（`:77-81`）：跨发布形态的内存总线，**除 `pendingTopic` 外从不清洗**（`:101` 置 null）。

| 私有 Composable | 签名（`:行`） | 细节 |
|---|---|---|
| `CommunityPublishScreen` | `:148-329`（`:149`） | `BasicTextField` 正文（`:285`）；三枚媒体 chip「无媒体/图片/视频」（`:299-301`，`MediaChip` 定义 `:331-359`）；**免责文案 `:305`「发布后将使用默认示例媒体（不上传所选文件）」**；公约每日闸门 `:166-171` + `:228` 写回 `saveStringList(ConventionAckKey, listOf(platformTodayYmd()))`，key 常量 `:361` `"community_convention_ack_date"`；发布正文组装 `:272` `val text = if (topic != null) "$body\n#$topic" else body` → `:273` `onPublished(text, mediaType)` |
| `CommunitySearchScreen` | `:363-516` | 4 个 tab「全部/动态/话题/用户」（`:369`）；`DisposableEffect(engine){ observe → revision++; onDispose unsub }`（`:371-374`）；四路结果 `remember(query, revision)`（`:375-378`）；内联 `@Composable fun UserFollowRow`（`:380-407`，`:400-401` follow/unfollow）；空 query 且 tab0 时展示热门话题（`:471-481`，`:475` `engine.searchTopics("")`） |
| `ConventionFlutterNavBar` | `:522-560` | 自绘导航条（对齐 Flutter 截图） |
| `CommunityConventionScreen` | `:562-582` | 只渲染静态位图 `Res.drawable.community_convention_body`（`:568-570`），无富文本、无网络 |
| `CommunityTopicSelectScreen` | `:584-604` | **5 条硬编码话题**，与 `MockCommunityEngine.mockTopics`（4 条）不同源 |
| `CommunityImagePreviewScreen` | `:606-660`（`:607`） | `PlatformNetworkImage` 加载 URL + 「上一张/下一张」下标步进；**无缩放/手势** |
| `CommunityVideoPlayScreen` | `:662-687`（`:663`） | 只显示封面图 + 文本「▶ 播放（mock）」（`:679`）+ 说明「真播放器随 Video 模块 H.3；此处可点路径验收。」（`:682`）→ **完全不解码视频** |
| `CommunityCommentScreen` | `:689-782`（`:690`） | **监听器泄漏**：`:695-697` 用 `LaunchedEffect(Unit) { engine.observe { revision++ } }`，返回的注销函数被丢弃、永不调用（同文件搜索页 `:371-374` 是正确写法；`<APP-a>/NativeJetpackTabs.kt:850-852` 同样用了 `LaunchedEffect(Unit)`）；发表 `:770` `engine.addComment(postId, text)` → toast「评论成功/评论失败」（`:764-778`） |

#### 3.2.4 `<FC-http>/CommunityPostModels.kt`（400 行）

| 类型 / 函数 | 签名（`:行`） | 要点 |
|---|---|---|
| `CommunityDefaultSource` | `const val CommunityDefaultSource = "来自 iPhone"`（`:31`） | #60 的默认 source |
| `communityNowMs` / `parseCommunityTimeMs` | `:33-34` / `fun parseCommunityTimeMs(text: String?): Long?`（`:47-87`） | 宽容 ISO-8601；**注释 `:43-46` 明确：无时区偏移的裸串按 UTC 解释，与 Dart 的本地时区解释存在差异**；私有支撑 `parseCivilDate :90-99`、`dayMillis :101`、`parseTimeOfDay :104-129`、`String.toIntStrict :132-137`、`daysFromCivil`（Howard Hinnant 算法）`:140-147` |
| `TopicModel` | `@Serializable data class TopicModel(...)`（`:155-188`） | 线上键用 `*Raw` 可空字段承接，派生属性 `id/name/heat/isAskEveryone/heatLabel/displayName`；`heatLabel` 的「x.x万」规则 `:175-184` |
| `CommentModel` | `@Serializable data class CommentModel(...)`（`:194-220`） | `createTimeMs(nowMs: Long = communityNowMs())`（默认参数便于测试注入） |
| `PostModel` | `@Serializable data class PostModel(...)`（`:236-316`） | **同一形状兼吃「完整动态」与「点赞片段」**；`images = (imagesRaw ?: imageUrlsRaw).orEmpty().filter { it.isNotEmpty() }`（`:275-276`，`images` 优先于 `image_urls`）；`isAskEveryone = top \|\| topic`（`:304-305`）；`hasImages/hasVideo`（`:312-315`） |
| `postModelFromLikeFragment` | `fun postModelFromLikeFragment(...)`（`:322-323`） | 片段解码入口 |
| `CommunityUserHit` | `@Serializable data class CommunityUserHit(...)`（`:332-347`） | **与同包 mock 的 `CommunityUserHit`（`<FC>/MockCommunityEngine.kt:40`）重名**，`:328-330` 已注明；`http` 子包版本才是线上模型 |
| `CommunitySearchAllResult` | `@Serializable ...`（`:357-372`） | 三个子页是 `JsonElement`，各自 `decodeListPage` |
| `PostCreateRequest` | `@Serializable ...`（`:379-389`） | snake_case 六键，配合 `NetworkJsonExplicitNulls` |
| `CommentCreateRequest` | `@Serializable ...`（`:396-400`） | `reply_to_nickname` 可空 ⇒ 由 `explicitNulls=false` 自动省略该键 |

#### 3.2.5 `<FC-http>/CommunityPostRepository.kt`（130 行）

`interface CommunityPostRepository`（声明块 `:32-129`，14 个 suspend 方法），与契约 #59–#71 一一对应：

| 方法组 | 行号 | 契约 | 分页 / 备注 |
|---|---|---|---|
| `fetchPosts(page, pageSize = 10, tab = "latest")` | `:32-36` | #59 | 返回 `List<PostModel>`，**接口层把 `ListPage` 摊平成 list** |
| `fetchComments(postId)` | `:42` | #61 | 内部写死 page1/size50 |
| `addComment(postId, content, replyToNickname: String? = null)` | `:45-49` | #62 | — |
| `toggleLike(postId, liked)` / `deletePost(postId)` / `createPost(content, mediaType = "none", topicId: String? = null, isAskEveryone = false, source = CommunityDefaultSource)` | `:58` / `:61` / `:69-75` | #63/#64、#65、#60 | 点赞同路径切动词 |
| `fetchTopics(page = 0, pageSize = 20)` / `searchTopics(query, page = 0, pageSize = 20)` | `:78-81` / `:88-92` | #66、#67 | **两个不同端点** |
| `followUser(userId)` / `unfollowUser(userId)` | `:95` / `:98` | #70/#71 | 同路径切动词 |
| `searchAll(q, page = 0, pageSize = 5)` | `:104-108` | #68 | 返回 `CommunitySearchAllResult` |
| `searchPosts` / `searchTopicsPage` / `searchUsers` | `:111-115` / `:118-122` / `:125-129` | #69 | 返回 `ListPage<T>`（同端点靠 `type`） |

`:20-23` 是本文件最重要的诚实声明：**`MockCommunityEngine` 并不实现这个端口**（理由：mock 的方法是同步的、行类型不同名、且缺 `deletePost`/`fetchTopics`/分页搜索）。也就是说端口与现用实现之间**没有可替换关系**，接线上前必须先补适配层。

#### 3.2.6 `<FC-http>/HttpCommunityPostRepository.kt`（324 行）

| 片段 | 行号 | 内容 |
|---|---|---|
| 路径注释 | `:23-24`、`:26-28` | 「path 参数不做 URL 编码」；鉴权头由 `NetworkFacade` 拦截器统一注入，本层不再手工加 |
| `object CommunityApiPaths` | `:30-57` | `PREFIX="/api/v1/community"`（`:32`）、`POSTS="$PREFIX/posts"`（`:35`）、`TOPICS`（`:38`）、`TOPICS_SEARCH="$TOPICS/search"`（`:41`）、`SEARCH`（`:44`）、`postComments(postId)`（`:47`）、`postLike(postId)`（`:50`）、`postById(postId)`（`:53`）、`userFollow(userId)`（`:56`） |
| 类与 api 解析 | `:75-79` | `class HttpCommunityPostRepository(private val client: ApiClient? = null)`，`private val api: ApiClient get() = client ?: NetworkFacade.api()` —— **每次调用重解析**，为拿刷新后的 token |
| `fetchPosts` | `:82-91` | `listPage<PostModel>(POSTS, communityQuery(page + 1, pageSize, "tab" to tab), "加载动态失败").items` |
| `fetchComments` | `:94-99` | `pageQuery(page = 1, size = 50)`（`:97`，Dart 写死） |
| `addComment` | `:102-113` | `api.postApi<CommentCreateRequest, CommentModel>(...)`（`:107`） |
| `toggleLike` | `:120-128` | `:123` POST / `:125` DELETE，body `""` |
| `deletePost` | `:134-139` | `api.deleteApiOk(postById(postId))`（`:136`）+ `requireCodeOnly` |
| `createPost` | `:149-169` | `:163-165` `body = NetworkJsonExplicitNulls.encodeToString(body)`，`api.postApi(POSTS, body, ::decodePostFragment)` |
| `fetchTopics` / `searchTopics` | `:172-180` / `:183-192` | 两个不同端点；`searchTopics` 的 `q` **总发**（含空串） |
| `followUser` / `unfollowUser` | `:198-205` / `:208-213` | POST body `""` + `parseData = { null }`；DELETE 用 `deleteApiOk`（`:210`） |
| `searchAll` | `:216-224` | `api.getApi<CommunitySearchAllResult>(SEARCH, ... type = "all")`（`:221`） |
| `searchPosts` / `searchTopicsPage` / `searchUsers` | `:227-246` | 全部经 `searchPage(type = "post"/"topic"/"user")` |
| `searchPage` | `:253-263` | 同端点切 `type`，把响应切成单个 `ListPage<T>` |
| `listPage` | `:273-282` | `:278` `api.getApi(path, query) { element -> element }`，`:280-281` 两道判空后 `decodeListPage<T>(data)`；**绕开 `getListPage` 的注释在 `:265-272`** |
| `requireData` / `requireCodeOnly` | `:284-287` / `:290-294` | 前者 data 缺失也算失败 |
| `decodePostFragment` | `:298-299` | `decodeEnvelopeData<PostModel>(element)` |
| `communityFailure` | `:305-312` | `fun communityFailure(code: Int, message: String?, fallback: String): NetworkError.Business` —— message 非空则透传 |
| `communityQuery` | `:320-324` | `pageQuery(page, size) + required.toMap()`；`page` 已是 1-based |

**被谁调用**：全仓 grep `HttpCommunityPostRepository` / `CommunityPostRepository` 仅命中定义文件 + `commonTest` → **信息流与互动在生产里没有一个 HTTP 调用点**（详见第 5、7 节）。

### 3.3 `feature/chat`

> 前置：本模块的引擎抽象**不在本模块**。`<CC>/ChatPort.kt`（17 行）与 `<CC>/ImEngine.kt`（543 行）属于 ③ `:component`，`feature/chat/build.gradle.kts:2` 已写明「引擎抽象（ImEngine / MockImEngine / ImEngineStore）在 :component，本模块只做 API 与 UI」。

#### 3.3.1 上游端口一：`<CC>/ChatPort.kt`

```
:7  public interface ChatPort {
:8      val isReady: Boolean
:9      suspend fun ensureLoggedIn(userId: String, userSig: String): Result<Unit>
:10 }
:12 public object StubChatPort : ChatPort {
:13     override val isReady: Boolean = false
:14     override suspend fun ensureLoggedIn(userId: String, userSig: String): Result<Unit> =
:15         Result.failure(IllegalStateException("Chat SDK not wired"))
:16 }
```

* `isReady` 的**唯一真值实现就是 `false`**（`:13`），注释 `:3-6` 说明该端口是 Spike I 预留位、「Features must depend on this port, not vendor TIM/JMessage APIs directly」。
* 生产接线只有一处：`<APP>/AppContainer.kt:42 val chat: ChatPort = StubChatPort`（import 在 `:3-4`）—— **没有任何 feature 读 `container.chat`**（全仓 `ChatPort` 命中仅 `AppContainer.kt:3,4,42` + 定义文件）。

#### 3.3.2 上游端口二：`<CC>/ImEngine.kt`（UI 真正的数据来源）

| 定义 | 行号 | 内容 |
|---|---|---|
| `interface ImEngine` | `:17-36` | 10 个方法：`conversations()`（`:18`）、`messages(conversationId)`（`:19`）、`sendText(conversationId, body): ImMessage?`（`:21`）、`sendImage(conversationId, urlOrPath)`（`:22`）、`sendVoice(conversationId, label = "[语音]")`（`:23`）、`sendCustom(conversationId, title = "Demo 名片")`（`:24`）、`markConversationRead(conversationId)`（`:25`）、`ensureConversation(id, title, lastMessage = "", unreadCount = 0): String`（`:26-31`）、`recallMessage(conversationId, messageId): Boolean`（`:33`）、`observe(listener): ()->Unit`（`:34`）、`filterConversations(query)`（`:35`）。**全部同步、无 suspend** |
| 枚举 | `:38`、`:40`、`:42` | `ImMessageType{Text,Image,Voice,Custom,Time,System}`、`ImSendStatus{Sending,Success,Failed}`、`ImReadStatus{Unread,Read}` |
| `ImConversation` / `ImMessage` | `data class`（`:44-55`）/（`:57-88`） | 前者含 `updatedAtLabel`；后者 `val canRecall: Boolean`（`:72-76`）= 自己发的、非 Time/System、且 `nowMs() - createdAtMs <= 180_000L`（3 分钟），`fun statusLabel(): String`（`:78-87`）→ 发送中 / 发送失败 / 已读 / 送达 |
| `ChatAvatarUrls` / `ChatTimeFormat` | `object`（`:90-93`）/ `internal object`（`:95-138`） | 前者是 picsum 固定头像池；后者 `hm`/`listLabel`/`detailDividerLabel` **硬编码 +8 小时 CST 偏移**（`:97` `+ 8 * 3600`），非本地化时区 |
| `ImEngineStore` | `object ImEngineStore { val engine: ImEngine by lazy { MockImEngine(seedDemo = true) } }`（`:145-147`，`:146`） | **进程内共享的 mock 引擎单例** |
| `MockImEngine` | `class MockImEngine(private val seedDemo: Boolean = true)`（`:149-453`） | 作用域 `CoroutineScope(SupervisorJob() + Dispatchers.Default)`（`:152`） |

`MockImEngine` 关键内部行为：

| 行为 | 行号 | 真相 |
|---|---|---|
| 种子会话 | `:175-219` | 3 个 `mock_peer_01..03`「Mock好友N」，`i == 0` 未读 2；每个会话预置 2 条消息（`uid_m_1` / `uid_m_2`） |
| `conversations` / `filterConversations` / `messages` | `:225-243` | 纯内存读 |
| `markConversationRead` | `:245-262` | 未读清零 + 状态改 `Read` |
| `sendText/Image/Voice/Custom` | `:264-274` | 全部转发 `sendPipeline` |
| `recallMessage` | `:276-306` | 先判 `msg.canRecall`（`:282`）；成功则把该条**替换为 System 消息**「你撤回了一条消息」并重算预览 |
| `sendPipeline` | `:308-351` | 插入 `id = "local_${++localSeq}_${now}"`、状态 `Sending`（`:319`）→ `scope.launch { delay(280) → 改 Success + messageUid = "uid_$now"（:337-343）; delay(2000) → simulatePeerRead（:347-348）}` —— **280ms / 2s 是假的网络与已读时延** |
| `maybeInsertTimeDivider` | `:353-370` | 5 分钟规则插 `Time` 分隔条 |
| `upsertPreview` / `simulatePeerRead` | `:372-388` / `:390-396` | 会话摘要与已读位回写 |
| `ensureConversation` | `:398-452` | 按 `private_` / `group_` / `private_conv_mock_push` 归一化 id 后建会话 |
| `ChatDetailDeepLinkArgs` / `chatDetailArgsFromDeepLink(rawUri): ChatDetailDeepLinkArgs?` | `:458-463` / `:470-501` | 解析 `myai:///chat/detail?id=&peerName=&lastMessage=&unreadCount=`；私有 `percentDecode`（`:504-543`） |

**被谁调用**：`<APP>/AppShell.kt:394`（经 `ChatScreen` 默认参数）、`<APP-a>/NativeJetpackTabs.kt:94`（`remember { ImEngineStore.engine }`）、`:114`（`engine.ensureConversation(...)`）、`<APP-a>/NativeAndroidMain.kt:471`（`chatDetailArgsFromDeepLink(pending.rawUri)`）、`feature/friend/src/commonMain/.../FriendDirectory.kt:41,51`（加好友/发消息时建会话）。

#### 3.3.3 `<FK>/ChatScreen.kt`（469 行）

| 定义 | 签名（`:行`） | 行为 / 副作用 |
|---|---|---|
| `ChatScreen` | `@Composable fun ChatScreen(engine: ImEngine = remember { ImEngineStore.engine }, onOpenFriends: () -> Unit = {})`（`:55-100`，声明 `:56-59`） | 局部状态 `selectedConversationId/revision/searchOpen/query`（`:61-64`）；`DisposableEffect(engine) { observe → revision += 1; onDispose { unsub() } }`（`:66-69`，**正确注销**）。选中分支：`ReportMainTabRoot(isRoot = false)`（`:76`）→ `LaunchedEffect(selected.id) { engine.markConversationRead(selected.id) }`（`:77`）→ `ChatDetailScreen(...)`（`:78-84`，`onSend = { engine.sendText(...) }`、`onRecall = { engine.recallMessage(...) }`）。列表分支：`ReportMainTabRoot(isRoot = true)`（`:86`）→ `ChatListContent(conversations = remember(revision, query) { engine.filterConversations(query) }, ...)`（`:87-99`） |
| `ChatListContent` | `@Composable private fun ChatListContent(conversations, searchOpen, query, onQueryChange, onToggleSearch, onOpen, onOpenFriends)`（`:102-221`） | 标题「消息」32.sp（`:125`）；搜索开关用文本按钮 "✕"/"⌕"（`:133`）；"✎" → `onOpenFriends`（`:141`）；空态区分「没有匹配的会话」/「还没有消息」（`:177`）并给「去通讯录」CTA（`:191`） |
| `ConversationRow` | `:223-315` | 在线小圆点依赖 `conversation.isOnline`（`:250`，**mock 常量**）；未读徽章 `> 99` 显示 "99+"（`:296`、`:306`） |
| `ChatDetailScreen` | `@Composable fun ChatDetailScreen(conversation: ImConversation, messages: List<ImMessage>, onBack: () -> Unit, onSend: (String) -> Unit = {}, onRecall: (String) -> Boolean = { false })`（`:317-440`，声明 `:318-323`） | `LazyColumn(reverseLayout = true)`（`:343`）；`ImMessageType.Time, System` 居中特例（`:348`）；**撤回确认不是 Dialog，而是列表底部内联条**（`:368-396`，`recallTarget` 状态 `:326`，`:361` 只有 `msg.canRecall` 才置目标，`:381` 取消、`:389-391` 执行并清空）；发送按钮仅在 `draft.isNotBlank()` 时可点（`:428-431`） |
| `MessageBubble` | `@Composable private fun MessageBubble(message: ImMessage, onLongPress: () -> Unit = {})`（`:442-469`） | **形参名叫 onLongPress，但实现是 `.clickable(onClick = onLongPress)`（`:457`）—— 单击即触发撤回选中**，不是长按；气泡最大宽 280.dp（`:454`）；底部优先显示 `statusLabel()`，为空才显示时间（`:460-466`） |

**被谁调用**：`<APP>/AppShell.kt:60,394`；`ChatDetailScreen` 仅被同文件 `:78` 调用（public 但无外部消费者）。

#### 3.3.4 `<FK>/RongImConfig.kt`（78 行）

| 常量 / 成员 | 行号 | 语义 |
|---|---|---|
| 说明块 | `:3-20` | 逐条解释 mock 开关来源（Dart `USE_MOCK_IM` 宏） |
| `SESSION_PATH = "/api/v1/im/session"` | `:24` | #100 |
| `USERS_PROFILE_PATH = "/api/v1/im/users/profile"` | `:27` | #101 |
| `MESSAGES_BACKUP_PATH = "/api/v1/im/messages/backup"` | `:30` | #102 |
| `GROUPS_FREE_PATH = "/api/v1/im/groups/free"` | `:33` | #109（+ `:77-78` 派生 invite/kick/quit/dismiss） |
| `GROUPS_STORE_SYNC_PATH = "/api/v1/im/groups/store/sync"` | `:36` | #114 |
| `CONNECT_TIMEOUT_SECONDS = 15` | `:39` | 真 SDK 连接超时（**当前无人使用**，仅被测试断言） |
| `PROFILE_CACHE_TTL_MS = 24h` | `:42` | 同上 |
| `var forceMockIm = false` | `:48` | 强制 mock |
| `var rongAppKey = ""` | `:54` | **默认空 ⇒ 默认走 mock 分支**（`:13-15` 已说明） |
| `fun useMockImFor(appKey: String?): Boolean` | `:60-64` | `forceMockIm \|\| appKey.isNullOrBlank()` |
| `val useMockIm: Boolean` | `:67` | `useMockImFor(rongAppKey)` |
| `fun imFreeGroupActionPath(groupId: String, action: String): String` | `:77-78` | `"$GROUPS_FREE_PATH/$groupId/$action"` |

好友侧路径不在此模块：`feature/friend/src/commonMain/.../ImFriendApi.kt:25,28,31,34`（`/api/v1/im/friends`、`/friends/requests`、`/users/search`、`/private/admission`）—— 三模块**没有跨 feature import**（层门轴 1）。

#### 3.3.5 `<FK>/ImJsonRead.kt`（60 行）

| 函数 | 签名（`:行`） | 行为 |
|---|---|---|
| `imJsonStr` | `fun JsonObject.imJsonStr(vararg keys: String): String`（`:27-33`） | 依次试多个键名（snake/camel 兼容），取第一个非空文本；全缺 → `""` |
| `imJsonText` | `private fun imJsonText(el: JsonElement?): String?`（`:35-39`） | 吸收 `JsonPrimitive` 的 string/number/bool |
| `imJsonInt` | `fun JsonObject.imJsonInt(key: String): Int?`（`:45-51`） | 数字或数字串 |
| `imJsonItems` | `fun JsonObject.imJsonItems(key: String = "items"): List<JsonObject>`（`:57-60`） | 非 `JsonArray` → `emptyList()` |

`:23-25` 注释说明这三个「本可私有」的函数之所以 public，是因为模块被拆出去后需要跨文件可见。

#### 3.3.6 `<FK>/ImSessionApi.kt`（198 行）

| 定义 | 签名（`:行`） | 行为 |
|---|---|---|
| `ImSessionResult` | `public data class ImSessionResult(imUserId: String, token: String, expiresInSeconds: Int)` + `companion fun fromJson(json: JsonObject)`（`:29-41`） | 先读 `user_id` 再回落 `im_user_id` |
| `ImSessionRequest` | `@Serializable data class ImSessionRequest(display_name: String? = null)`（`:51-54`） | 配合 `NetworkJson`（`explicitNulls = false`）实现「空即省略键」 |
| `ImMockUserIdRegistry` | `class ImMockUserIdRegistry(private val load: () -> String? = { null }, private val save: (String) -> Unit = { }, private val newImUserId: () -> String = { "im_u_" + AppUuid.random().replace("-", "").take(16) })`（`:63-106`） | **默认注入的是空读写**，即进程内 map；`resolveImUserId(bizUserId): String`（`:75-82`）命中即返、否则生成 + `persist()`；`knownMappings(): Map<String,String>`（`:85`）；`ensureLoaded()`（`:87-99`）解析失败静默退回空表；`persist()`（`:101-105`）写回 |
| `IM_BIZ_TO_IM_USER_ID_KEY` | `const val ... = "im_biz_to_im_user_id_json"`（`:110`） | Dart SP key 对等 |
| `createPersistentImMockUserIdRegistry(key = IM_BIZ_TO_IM_USER_ID_KEY)` | `fun ...: ImMockUserIdRegistry`（`:116-121`） | 接 `loadString/saveString`（`:118-119`）；注释 `:112-115` 说「默认构造只在进程内记忆，启动侧接线时应改用它」—— **但全仓无任何调用点**，该 TODO 未落 |
| `ImSessionApi` | `class ImSessionApi(private val client: ApiClient = NetworkFacade.api(), private val registry: ImMockUserIdRegistry = ImMockUserIdRegistry(), private val useMock: () -> Boolean = { RongImConfig.useMockIm }, private val sleep: suspend (Long) -> Unit = { delay(it) }, private val nowMs: () -> Long = ::imCurrentTimeMillis)`（`:140-145`） | 五个可注入参数 = 五个可测点 |
| `createSession` | `suspend fun createSession(bizUserId: String, displayName: String? = null): ImSessionResult`（`:151-183`） | **mock 分支 `:155-163`：`sleep(150)` + `registry.resolveImUserId(bizUserId)` + `token = "mock_rong_token_${nowMs()}"` + 3600s，一次 HTTP 都不发**；真实分支 `:165-181`：`displayName` trim 后空则整键省略（`:165-169`）→ `client.postApi(SESSION_PATH, body)`（`:171`）→ 非 success / data 为 null / **token 空串** 三者之一即抛 `NetworkError.Business`（`:175-179`，兜底「获取 IM Token 失败」`:192`）。注释 `:126` 明确 `bizUserId` 只在 mock 分支用，真实分支不进请求体 |
| `GAP(R5)` | `:133-137` | 「真实分支拿到 token 后 Dart 立即 `RongEngineHolder.connectReal(session)` … 融云 IMLib（`rongcloud_im_wrapper_plugin: ^5.44.0`）在本工程 Android/iOS/OHOS 均无产物，故本 lane 只移植 BFF 侧 … **禁止伪装 SDK ready**」 |
| `imCurrentTimeMillis` | `@OptIn(ExperimentalTime::class) fun imCurrentTimeMillis(): Long`（`:197-198`） | — |

#### 3.3.7 `<FK>/ImUserProfileApi.kt`（104 行）

`data class ImUserProfile(imUserId, displayName, avatarUrl)`（`:18-22`）；`class ImUserProfileApi(private val client: ApiClient = NetworkFacade.api(), private val useMock: () -> Boolean = { RongImConfig.useMockIm }, private val sleep: suspend (Long) -> Unit = { delay(it) })`（`:39-43`）。

`suspend fun fetchProfiles(imUserIds: List<String>): Map<String, ImUserProfile>`（`:45-87`）：

| 步 | 行号 | 行为 |
|---|---|---|
| 清洗 | `:46` | `map { it.trim() }.filter { it.isNotEmpty() }` |
| 空列表短路 | `:47-48` | **直接返回空 map，不发 HTTP**（Dart 等价行为，测试 `ImChatApiContractTest.kt:151` 锁死） |
| mock 分支 | `:50-59` | `sleep(80)`（`:51`）+ `avatarUrl = "https://picsum.photos/seed/$id/200/200"`（`:56`）+ `mockNameFor`（`:89-95`，`im_u_` 前缀 + 末 4 位） |
| 真实分支 | `:61-73` | `client.getApi(USERS_PROFILE_PATH, mapOf("user_ids" to ids.joinToString(",")))`（`:61-65`，逗号拼接、单个 query key）；失败抛 Business，兜底「拉取 IM 用户资料失败」（`:68-73`） |
| 行映射 | `:74-85` | 以 `user_id`/`im_user_id` 为 key，id 空则跳过 |
| `GAP(R5)` | `:35-37` | 「Dart 侧本 API 只负责取资料；真实融云 SDK 的 userId 解析/在线状态」缺位 |

#### 3.3.8 `<FK>/ImGroupApi.kt`（201 行）

模型：`ImFreeGroupResult`（`:15-27`）、`ImStoreGroupResult`（`:33-43`）、`@Serializable ImFreeGroupCreateRequest(name, member_ids)`（`:46-50`）、`ImGroupMembersRequest(member_ids)`（`:53-56`）、`ImStoreGroupSyncRequest(store_id: String)`（`:59-62`，注释强调 **store_id 是字符串不是数字**）。

`class ImGroupApi(private val client: ApiClient = NetworkFacade.api())`（`:84-85`）：

| 方法 | 行号 | 端点 / body | 兜底文案 |
|---|---|---|---|
| `syncStoreGroup(storeId: String): ImStoreGroupResult` | `:89-98` | `#114 POST /im/groups/store/sync` | 「门店群同步失败」 |
| `createFreeGroup(name, memberIds): ImFreeGroupResult` | `:101-112` | `#109 POST /im/groups/free` | 「建群失败」 |
| `invite(groupId, memberIds)` / `kick(groupId, memberIds)` | `:118-122` / `:128-132` | `#110` / `#111`，body = `encodedMembers`（`:159-162`） | 「群操作失败」 |
| `quit(groupId)` / `dismiss(groupId)` | `:138-139` / `:145-146` | `#112` / `#113`，body = `EMPTY_OBJECT_BODY`（`:166`）；`:70-73` 注释解释要「显式空对象」而非「无 body」 | 同上 |
| `postOk(path, body)` | `:149-157` | `:150` `client.postApi(path, body) { Unit }`，**只看 `isSuccess`、丢弃 data** | 「群操作失败」`:169` |
| `private suspend fun <Res> ApiClient.postApiJson(...)` | `:184-201` | 文件内私有扩展，`:190` 调 `postApi`，data 缺失抛 Business | — |
| `GAP(R5)` | `:79-83` | 「建群成功后 Dart 还要 `RongEngineHolder.joinGroup/dismissGroup` 等 SDK 侧动作」 | — |

#### 3.3.9 `<FK>/ImMessageBackupApi.kt`（513 行）

| 定义 | 行号 | 内容 |
|---|---|---|
| `ImMessageBackupEvent` | `data class`（`:28-68`） | 7 个线上字段 `messageUid/direction/conversationId/messageType/payload/sentAtEpochMs/conversationType = "private"`；`val sentAtIso8601Utc: String`（`:39`）；`fun toJson(): JsonObject`（`:42-52`，`linkedMapOf` 保序）；`companion fun fromJson`（`:56-67`） |
| `ImBackupDirection` | `enum class ImBackupDirection(public val wireValue: String)`（`:71-86`） | `Out/In/Recall` + `fromWire(raw): ImBackupDirection`（默认 `Out`） |
| `interface ImBackupService` | `:100-134` | `backupOutbound(imUserId, conversationId, messageUid, type, payload, sentAtEpochMs)`（`:102-111`）、`backupInbound(...)`（`:112-121`）、`backupRecall(imUserId, conversationId, messageUid, recalledAtEpochMs)`（`:122-129`）、`flushPending()`（`:130`）、`val pendingCount: Int`（`:133`）；`GAP(R5)` 注释 `:91-98`（事件生产者是融云 SDK 回调，本仓无产物） |
| `HttpImBackupService` | `class HttpImBackupService(private val client: ApiClient = NetworkFacade.api())`（`:148-255`） | `queueMutex`（`:153`）；三个 backup 方法 `:155-206` 统一转 `enqueue`；`enqueue`（`:212-218`）在锁内「add + 判 `pending.size >= FLUSH_THRESHOLD`」，超阈值即 flush；`flushPending`（`:220-244`）锁内快照并清空 → `:232-235` 用 `NetworkJson` 组 `{"events":[...]}`（`:234`）→ `:237` `client.postApi(MESSAGES_BACKUP_PATH, body) { Unit }` → **失败或异常时 `pending.addAll(0, batch)`（`:242`）重新插回头部 = at-least-once**；`pendingCount`（`:248`）；`FLUSH_THRESHOLD = 5`（`:250`） |
| `MockImBackupService` | `class MockImBackupService(private val sleep: suspend (Long) -> Unit = { delay(it) })`（`:263-346`） | 语义照 Dart mock：`backupRecall` **故意不入队**（`:306-314`，空函数体 + 注释「仅日志语义，KMP 无 LogUtils 对等实现，故留空」）；`flushPending`（`:324-336`）逐条 `removeAt(0)` + `sleep(30)`（`:334`，`MOCK_POST_DELAY_MS = 30L` `:344`），一次 HTTP 都不发 |
| `createImBackupService(client, useMock = { RongImConfig.useMockIm }, sleep)` | `:352-356` | 开关工厂（**无生产调用者**） |
| `ImBackupPayloads` | `object`（`:364-387`） | `text(:365)`、`image(:367)`、`voice(:369)`、`custom(:374)`、`content(:380)`、`empty(:382)`、`stringValue(:385)` —— 与 Dart 字面量对齐（测试 `:387` 锁死） |
| `ImIso8601Utc` | `object`（`:396-513`） | `format(epochMs: Long): String`（`:406-433`，毫秒为 0 时不输出小数位）、`civilFromDays`（`:436-447`）、`floorDiv/floorMod/Long.pad`（`:449-457`）、`parseToEpochMillis(iso: String): Long?`（`:463-502`，接受 `Z` 与 `±HH:mm`，长度 <20 直接 null）、`daysFromCivil`（`:505-512`）。**手写 UTC 换算，不依赖 java.time / 平台日期库** |

#### 3.3.10 chat 测试

* `feature/chat/src/commonTest/.../MockImEngineTest.kt`（60 行，4 个 `@Test`）：`:15` 发送管线 + 未读、`:28` `ensureConversation` 私聊/群聊、`:38` 撤回窗口、`:52` 过滤。
* `feature/chat/src/commonTest/.../ImChatApiContractTest.kt`（684 行，32 个 `@Test`）：覆盖会话（`:34` 真实 body、`:54` 空 display_name、`:67` 空 token 抛错、`:85` mock 分支不发 HTTP、`:109` 注册表持久化、`:125-146` mock 开关与 Dart `useMockImFor` 对等含 `CONNECT_TIMEOUT_SECONDS = 15`）；资料（`:151` 空列表短路、`:161` 逗号 query + 按 id 建 map、`:188` 失败/picsum）；备份（`:213` 满 5 才 flush、`:234` 业务错重排、`:247` 传输异常重排、`:256-309` at-least-once 顺序长测、`:312-330` recall 形状、`:333` 空 flush 不打 HTTP、`:340` mock 备份、`:354` 工厂选择、`:362` ISO 往返、`:387` payload 字面量）；群组（`:405` 建群/邀请/踢人、`:428` quit/dismiss 显式 `{}`、`:447/:462` #110、`:473/:484` #111、`:496/:510` #112、`:522/:534` #113、`:543` 兜底文案、`:556/:566` #114）。测试基建：`FakeImApiClient`（`:593-654`）、`data class ImFakeCall`（`:656-661`）、手搓协程执行器 `runNow`（`:667-684`，一旦真的挂起就失败）。

## 4. media 专题：短视频 / 图片上传与播放到底做到哪一步

### 4.1 一句话结论

**「播放」= Android/iOS 播一条公网 MP3 的音频轨；OHOS 是纯布尔 stub。「上传」= 完全没有**：全模块没有任何取文件、选图、编码、multipart 的代码；HTTP 侧 #74 建单请求体的四个上传字段是**写死的 null**。

### 4.2 播放能力矩阵（逐端实证）

| 端 | actual 实现 | 真实做到 | 缺口自证 |
|---|---|---|---|
| Android | `<FM-a>/MediaPlayer.android.kt:12` → `AndroidMediaPlayer`，用 `android.media.MediaPlayer` + `setDataSource` + `prepareAsync`（`:32-60`） | **音频解码 + 起播/暂停/停止/释放**；`OnPreparedListener` 里按 `wantPlay` 续播（`:60`）；play 容忍「还没 prepared」（`:68-87`） | `:16`「**Video surface hosting remains a follow-up**; audio decode is enough for 7.2 smoke」→ 无 `SurfaceView`/`setSurface`，**不输出画面** |
| iOS | `<FM-i>/MediaPlayer.ios.kt:21-22` → `IosAvMediaPlayer`，`AVPlayer` + `AVPlayerItem` + `replaceCurrentItemWithPlayerItem`（`:39-41`） | **音频播放 + `AVAudioSession` 后台类别**（`ensureAudioSession :90-96` 设 `Playback` 并 `setActive(true)`） | `:18-19`「iOS AVPlayer audio — parity with Android MediaPlayer for music smoke. **Video surface hosting remains a follow-up.**」；`stop()` 只 `pause()`（`:70-74`）；`setSource` 内 `wantPlay` 先被清零（`:35`）再被判定（`:42`），**单独调 `setSource` 不会起播** |
| OHOS | `<FM-o>/MediaPlayer.ohos.kt:7` = `StubMediaPlayer()` | **零**：只翻 `isPlaying` 布尔（`<FM>/MediaPlayer.kt:18-43`） | `:4-5`「OHOS: no AVPlayer/MediaKit bridge yet — stub only. Enumerated in platform-gap-registry as missing for media playback.」；登记见 `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md:22`（`media playback \| partial \| partial \| stub`）与 `:33`（`music \| partial \| partial \| stub`） |

### 4.3 「上传」这条链的真实完成度

| 环节 | 是否存在代码 | 证据 |
|---|---|---|
| 选文件 / 选图 / 拍照 | **不存在** | `feature/media`+`feature/community`+`feature/chat` 三目录 grep `pickImage`/`CameraScan`/`PlatformCameraScan`/`image_picker` 命中 0 行；`platform-gap-registry.md:34` 明确 `image_picker \| missing \| missing \| missing \| no adapter` |
| 把文件读成字节 / multipart 上传 | **不存在** | 三目录 grep `MultipartBody`/`uploadFile` 命中 0 行；`core/network` 的 `ApiClient` 契约里本模块只用了 `getApi/postApi/deleteApi/getListPage`（见 3.1.6、3.2.6） |
| 建单请求体里的上传字段 | **恒为 null** | `<FM>/ShortVideoRepository.kt:104-111`：`video_url/cover_url/duration/aspect_ratio` 全是 `null`；`:162-165` 用 `NetworkJsonExplicitNulls` 把 null **显式上送**；契约行 `api-contract-2026-10-16.md:183`（#74「上传字段全 null」）。测试 `ShortVideoHttpRepositoryTest.kt:248`、`:272` 锁死 |
| 发布页 | **纯 toast** | `<FM>/VideoRouteHost.kt:144-147`：`showPlatformToast("已提交（mock）")` + `onBack()`，既不写 mock 引擎也不打 HTTP |
| 视频列表 / 详情 | **SoT 位图** | `<FM>/VideoRouteHost.kt:260-275`（`ShortVideoPage` 只铺一张截图）、`:231-258`（`FlutterSoTFullFrame` 通用整幅位图 + 隐形返回箭头） |
| 视频播放页 | **播 MP3 音频** | `<FM>/VideoRouteHost.kt:420`（`createMediaPlayer()`）、`:436`（`player.setSource(MusicSession.SAMPLE_AUDIO_URL)`）；`<FM>/VideoHubScreen.kt:244`、`:288` 同；`MockVideo` 结构只有 `id/title/durationLabel`（`<FM>/VideoHubScreen.kt:45-49`），**连 videoUrl 字段都没有** |

### 4.4 用了哪个平台能力？—— 答：几乎没用

| `core/platform` 能力 | 定义 | 三模块是否使用 |
|---|---|---|
| `PlatformOpen.openUrl` | `core/platform/src/commonMain/.../PlatformOpen.kt:4-8` | **未使用**（grep 0 命中） |
| `PlatformWebView` / `WebBridgeHost` | `core/platform` + `feature/web` | **未使用** |
| `PlatformCameraScan` | `core/platform`（扫码用） | **未使用** |
| `copyToClipboard` | `PlatformOpen.kt:9-11` | **未使用** |
| `showPlatformToast` | `PlatformOpen.kt:12-13` | **使用**：`<FM>/VideoRouteHost.kt:56`（导入），调用 `:145`、`:458`、`:461`、`:353` |
| `loadStringList`/`saveStringList`/`platformTodayYmd` | `StringListStore.kt:16-34` | 仅社区侧用（`<FC>/CommunityRouteHost.kt:56-58`）；**media 未用** |
| `loadString`/`saveString` | `StringListStore.kt:24-31` | 仅 `<FK>/ImSessionApi.kt:7-8`（且持久化注册表本身无生产调用者） |
| 图片加载 | `core/ui` 的 `expect fun PlatformNetworkImage(...)`（`core/ui/src/commonMain/.../PlatformNetworkImage.kt:23-29`），Coil actual `imageCoilMain/.../PlatformNetworkImage.coil.kt:20-34`，**OHOS 占位 actual** `ohosX64Main/.../PlatformNetworkImage.ohosX64.kt:16-30` | media **未直接使用**（封面全走 `painterResource` 本地位图）；社区在 `<FC>/CommunityRouteHost.kt:606-660` 图片预览里使用 |

结论：media 模块的平台耦合点只有 **① 播放器 expect/actual（三端）** 与 **② toast**，其余全是本地 drawable + 内存 mock。

### 4.5 值得记的两处真实缺陷（非编造，源码可查）

1. `MusicSession.dismiss()`（`<FM>/MediaPlayer.kt:68-72`）只调 `stop()` 不调 `release()`；Android 侧 `stop()` 内部已释放（`<FM-a>:98-106`）所以无害，**iOS 侧 `stop()` 只是 `pause()`**（`<FM-i>:70-74`），播放器实例与激活的 AudioSession 会一直存活。
2. `MiniPlayerBar`（`<FM>/MusicScreens.kt:171-223`）已实现但**全仓无调用点**；`<FM>/MediaPlayer.kt:47` 的 KDoc「so Home can show MiniPlayerBar」是**意图而非现状**。同理 `<FM>/VideoRouteHost.kt:277-414` 四个私有 Composable 无调用点。

---

## 5. community 专题：端点、分页、路由入口与出口

### 5.1 三条事实先摆明

1. **UI 层一条 HTTP 都不发**：`CommunityScreen.kt:55` 与 `CommunityRouteHost.kt:98,113,128,135,770,400-401` 全部读写 `CommunityMockStore.engine`；`HttpCommunityPostRepository` 在生产链路里**零调用**（第 7 节给出全量证据）。
2. **HTTP 端口已经写完并且被契约测试逐行锁死**（18 个 `@Test`），但端口与 mock 引擎**互不实现**（`<FC-http>/CommunityPostRepository.kt:20-23` 自证）。
3. **分页方言已在 HTTP 层做对，UI 层没有用上**：接口返回 `ListPage<T>` / `List<T>`，而信息流 UI 只调 `engine.posts(tab = tabKey)` 取第 0 页（`<FC>/CommunityScreen.kt:72`），没有「加载更多」。

### 5.2 帖子列表 / 详情 / 互动分别走哪些端点（契约 → KMP 落点）

| 用户动作 | 契约行 | HTTP | KMP 端口方法 | HTTP 实现行 | UI 实际走的（当前） |
|---|---|---|---|---|---|
| 进 tab / 切「最新·热门·关注」 | #59（`api-contract-2026-10-16.md:163`） | `GET /api/v1/community/posts?page=+1&size=10&tab=` | `fetchPosts(page, pageSize = 10, tab = "latest")`（`:32-36`） | `<FC-http>:82-91` | `engine.posts(tab)`（`<FC>/CommunityScreen.kt:72`）—— mock |
| 看帖子媒体 | — | 无端点（图片 URL 在 body 里） | — | — | `EnginePostCard` 只写「图片 ×n / 视频」文本（`<FC>/CommunityScreen.kt:261`），**不加载图片** |
| 点开评论 | #61（`:165`） | `GET /api/v1/community/posts/:id/comments?page=1&size=50` | `fetchComments(postId)`（`:42`） | `<FC-http>:94-99` | `engine.comments(postId)`（`<FC>/CommunityRouteHost.kt:698`）—— mock |
| 发表评论 | #62（`:166`） | `POST .../comments` body `{content, reply_to_nickname?}` | `addComment(...)`（`:45-49`） | `<FC-http>:102-113` | `engine.addComment(...)`（`:770`）—— mock |
| 点赞 / 取消赞 | #63/#64（`:167-168`） | `POST`/`DELETE .../posts/:id/like`（同路径切动词，无 body） | `toggleLike(postId, liked)`（`:58`） | `<FC-http>:120-128`（`:123`/`:125`） | `engine.toggleLike(...)`（`<FC>/CommunityScreen.kt:78`）—— mock |
| 删自己的帖 | #65（`:169`） | `DELETE /api/v1/community/posts/:id` | `deletePost(postId)`（`:61`） | `<FC-http>:134-139`（`deleteApiOk` + `requireCodeOnly`） | **UI 无入口**（端口有、屏幕没有） |
| 发帖 | #60（`:164`） | `POST /api/v1/community/posts`，六键 + 显式 null | `createPost(...)`（`:69-75`） | `<FC-http>:149-169` | `engine.createPost(...)`（`<FC>/CommunityRouteHost.kt:98`）—— mock，文案 `:305` 已声明「不上传所选文件」 |
| 话题列表 / 话题搜索 | #66/#67（`:170-171`） | `GET /community/topics` 与 `GET /community/topics/search`（**两个不同端点**） | `fetchTopics`（`:78-81`）/ `searchTopics`（`:88-92`） | `<FC-http>:172-180` / `:183-192` | `engine.searchTopics("")`（`<FC>/CommunityRouteHost.kt:475`）；话题选择页是 5 条硬编码（`:584-604`） |
| 搜索（全部） | #68（`:172`） | `GET /community/search?q=&type=all&page=&size=5` | `searchAll(q, page, pageSize = 5)`（`:104-108`） | `<FC-http>:216-224` | `engine.searchAll(query)`（`<FC>/CommunityRouteHost.kt:375`）—— mock |
| 搜索（单类） | #69（`:173`） | **同一端点** `GET /community/search` 靠 `type=post\\|topic\\|user` 切形状 | `searchPosts`/`searchTopicsPage`/`searchUsers`（`:111-129`） | `<FC-http>:227-246` → `searchPage :253-263` | `engine.searchPosts/searchTopics/searchUsers`（`:376-378`）—— mock |
| 关注 / 取关用户 | #70/#71（`:174-175`） | `POST`/`DELETE /community/users/:userId/follow`（无 body，data 可空） | `followUser`/`unfollowUser`（`:95,98`） | `<FC-http>:198-205` / `:208-213` | `engine.followUser/unfollowUser`（`<FC>/CommunityRouteHost.kt:400-401`）—— mock |

### 5.3 分页实现（三层拆开看）

| 层 | 做法 | 证据 |
|---|---|---|
| 页码语义 | **本地 0-based → 服务端 1-based**（`page + 1`），与 Dart 一致 | `<FC-http>:87`（fetchPosts）、`:177`（fetchTopics）、`:188`（searchTopics）、`:259`（searchPage）；`communityQuery` 的 KDoc 明确「[page] 已是服务端 1-based」（`:318`） |
| query 组装 | `pageQuery(page,size)` + **无条件补齐其余键**（Dart 连空 `q` 都发），刻意不用 `queryOf` 的省略规则 | `<FC-http>:320-324`；注释 `:314-317`；测试 `HttpCommunityPostRepositoryTest.kt:338`（empty q still sent） |
| 响应方言 | A 型：`list` / `items` / 裸数组 + `pagination`；**不使用 `getListPage`**，改为先取 `JsonElement` 再 `decodeListPage`，以便「`code==0` 但 `data` 缺失」仍抛错（Dart 语义） | `<FC-http>:273-282`（实现）与 `:265-272`（理由）；测试 `:415`（missing data on code 0 still throws） |
| `hasMore` | 由 `core/network` 的 `ListPage<T>.hasMore(pageSize)` 承担：`totalPages>0 → page<totalPages`；否则 `size>0 → page*size<total`；否则 `list.size >= pageSize` | 方言定义 `api-contract-2026-10-16.md:251-254`；社区用点 `<FC-http>`（`ListPage` 原样返回给 `search*` 三个方法），短视频用点 `<FM>/ShortVideoRepository.kt:156` |
| 评论页 | **写死** `page=1&size=50`（Dart 行为，不做分页 UI） | `<FC-http>:97`；测试 `HttpCommunityPostRepositoryTest.kt:171` |
| mock 分页 | `subList(page*pageSize, min(...))`，越界返回空 | `<FC>/MockCommunityEngine.kt:106-110`、`pageSlice :298-302` |
| UI 分页 | **无**：只渲染第 0 页 | `<FC>/CommunityScreen.kt:72`（`engine.posts(tab = tabKey)` 无 page 参数） |

### 5.4 路由入口与出口（全链路）

| 方向 | 具体路径 | 证据 |
|---|---|---|
| 入口 ①（CMP 主 tab） | `MainTab.Community → CommunityScreen()` | `<APP>/AppShell.kt:400`；tab 枚举 `core/router/.../AppRoutes.kt:37,44`；深链 `DeepLinkRouter.kt:64-67`（`/community`）与前缀回落 `:93-94` |
| 入口 ②（Android Native tab） | `MainTab.Community → JetpackCommunityRoot(onOpen, onPreviewImages, onPlayVideo)` | `<APP-a>/NativeAndroidMain.kt:711-726`；宿主 `NativeJetpackTabs.kt:840-872` |
| 入口 ③（二级覆盖层） | `ShellOverlay.CommunityRoute → CommunityRouteHost(route, ..., previewUrls, previewIndex, videoUrl)` | `<APP-a>/NativeAndroidMain.kt:569-578`（5 个参数全传） |
| 入口 ④（Screen 内部状态机） | `CommunityScreen` 的 `destination` 直接置为 `CommunityRoutes.Publish/Search`，评论用带 query 串 | `<FC>/CommunityScreen.kt:77-81`、委托 `:86-98` |
| 入口 ⑤（字符串路由解析） | `else -> if (route.startsWith("/community/comment"))` 解析 `postId=` | `<FC>/CommunityRouteHost.kt:131-136` |
| 出口：发帖 → 话题选择 / 公约 | `onNavigate(CommunityRoutes.TopicSelect)`、`onNavigate(CommunityRoutes.Convention)` | `<FC>/CommunityRouteHost.kt:95-96`；返回时 `TopicSelect → Publish`（`<FC>/CommunityScreen.kt:90`） |
| 出口：卡片 → 图片预览 / 视频播放 | Android Native 侧由回调抬到覆盖层：`openCommunityRoute(CommunityRoutes.ImagePreview, previewUrls = urls, previewIndex = index)`（`:713-719`）、`openCommunityRoute(CommunityRoutes.VideoPlay, videoUrl = url)`（`:720-725`） | `<APP-a>/NativeAndroidMain.kt:713-725`；CMP 侧**无这两个出口**（`CommunityScreen` 未传 preview 参数） |
| 出口：卡片 → 评论 | `destination = "${CommunityRoutes.Comment}?postId=${post.id}"` | `<FC>/CommunityScreen.kt:80` |
| 出口：发布后跨形态补写 | `CommunityPublishBus.lastPublishedBody`（`:848`）→ `engine.ingestPublishedBody(published, source = "来自 Android")`（`:854`） | `<APP-a>/NativeJetpackTabs.kt:848,854` |
| 出口：未知路由 | 渲染「未知路由 $route」 | `<FC>/CommunityRouteHost.kt:137-144` |
| 路由常量 | 7 条 `/community*` | `core/router/.../FeatureRoutes.kt:200-217`；字符串别名 `AppRoutePath.kt:45-48` |

### 5.5 沉浸区与规范符合性

| 规则 | 社区侧落实 | 证据 |
|---|---|---|
| tab 根才 `isRoot = true`，二级页 `false` | 根流 `:66` 为 true；其余二级页全部 false | `<FC>/CommunityScreen.kt:66`；`<FC>/CommunityRouteHost.kt:138`、`:102-146` 各二级页；`<FC>/CommunityScreen.kt:118` 用 `statusBarsPadding()` |
| 二级顶栏统一用 `MineTopBar` | 是 | `<FC>/CommunityRouteHost.kt:140`（未知路由页）、`:410`（搜索）、`:589`（选择话题）、`:666`（视频播放，黑底白字）、`:700`（评论）；签名 `core/design/src/commonMain/.../MineTopBar.kt:26-34`（`title, modifier, onBack: (()->Unit)? = null, actions: RowScope, containerColor, titleColor, titleFontSize = 18.sp`） |
| 不用裸 `TopAppBar` | 未见裸用；`CenterAlignedTopAppBar` 只出现在注释里（「Material3 CenterAlignedTopAppBar is taller…」`<FC>/CommunityRouteHost.kt:520`） | 三模块 grep `TopAppBar` 仅命中该注释行 |
| 公约「当日已确认」用平台日期 + StringList 存储 | 是 | `<FC>/CommunityRouteHost.kt:166-171`、`:228`、`:361` |

---

## 6. chat 专题（务必诚实）：IM 现在是假的

### 6.1 三条硬证据

| 命题 | 证据（file:line） |
|---|---|
| **IM 端口未就绪** | `<CC>/ChatPort.kt:13` `override val isReady: Boolean = false`（`StubChatPort` `:12-16` 是**唯一实现**）；`:14-15` `ensureLoggedIn` 恒返 `Result.failure(IllegalStateException("Chat SDK not wired"))`；装配点唯一且为 stub：`<APP>/AppContainer.kt:42 val chat: ChatPort = StubChatPort`（import `:3-4`），且**没有任何 feature 读 `container.chat`** |
| **引擎用 MockImEngine** | `<CC>/ImEngine.kt:145-147` —— `object ImEngineStore { val engine: ImEngine by lazy { MockImEngine(seedDemo = true) } }`（`:146`）；UI 默认参数直取：`<FK>/ChatScreen.kt:57` `engine: ImEngine = remember { ImEngineStore.engine }`；Android Native 同源：`<APP-a>/NativeJetpackTabs.kt:94` |
| **真融云 SDK 三端全缺（R5 缺口）** | `openspec/changes/parity-flutter-to-kmp/notes/platform-gap-registry.md:41`：`RongCloud IM 厂商 SDK \| missing \| missing \| missing`，同行写明「契约表 #100–114 的 **HTTP 侧已落**；融云 SDK 动作（connect、join/dismiss group、在线态、语音事件）无对等实现 → `// GAP(R5)`；`USE_MOCK_IM` 开关也未对等」；代码内标注 `<FK>/ImSessionApi.kt:133`、`ImUserProfileApi.kt:35`、`ImGroupApi.kt:79`、`ImMessageBackupApi.kt:91`；约束出处 `docs/adr/0005-three-long-lived-channels-separate-transports.md:23`「**不得**用 SSE 或自研 WS 伪造『聊天已接通』」 |

### 6.2 逐屏 mock 清单

| 屏 / 动作 | 当前数据源 | mock 的具体形态（file:line） | 真实的部分 |
|---|---|---|---|
| 会话列表 `ChatListContent` | `engine.filterConversations(query)`（`<FK>/ChatScreen.kt:88`） | 3 条种子 `mock_peer_01..03`「Mock好友N」，第 1 条未读 2（`<CC>/ImEngine.kt:175-219`）；在线点取决于常量 `conversation.isOnline`（`<FK>/ChatScreen.kt:250`）；徽章 99+ 封顶（`:306`） | 无 |
| 会话详情 `ChatDetailScreen` | `engine.messages(id)`（`<FK>/ChatScreen.kt:80`） | 每会话预置 2 条 `uid_m_1/uid_m_2`（`<CC>/ImEngine.kt:202,212`）；`reverseLayout = true`（`<FK>/ChatScreen.kt:343`）；撤回确认是列表底部内联条而非 Dialog（`:368-396`） | 5 分钟时间分隔条规则真实存在（`<CC>/ImEngine.kt:353-370`） |
| 发消息 | `engine.sendText(...)`（`<FK>/ChatScreen.kt:82`） | `sendPipeline`：插 `local_*`/`Sending`（`:319`）→ `delay(280)` 改 Success + 编造 `messageUid = "uid_$now"`（`:337,343`）→ `delay(2000)` 改已读（`:347-348`）—— **假时延，零网络** | 4 态 `statusLabel()`（`<CC>/ImEngine.kt:78-87`） |
| 撤回 | `engine.recallMessage(...)`（`<FK>/ChatScreen.kt:83`） | 本地把消息替换成 System「你撤回了一条消息」并重算预览（`<CC>/ImEngine.kt:276-306`）；**不上报 recall 事件**（`backupRecall` 无调用者） | 3 分钟窗口 `canRecall`（`:72-76`，`180_000L`） |
| 已读 | `engine.markConversationRead(id)`（`<FK>/ChatScreen.kt:77`，进入即调） | 纯内存清未读（`<CC>/ImEngine.kt:245-262`） | — |
| 深链进详情 | `chatDetailArgsFromDeepLink`（`<CC>/ImEngine.kt:470-501`）→ `<APP-a>/NativeAndroidMain.kt:471` | 随后 `ensureConversation(...)`（`<APP-a>/NativeJetpackTabs.kt:114`）**本地凭空造会话**（`<CC>/ImEngine.kt:398-452`） | id 归一化规则（`private_`/`group_`/`private_conv_mock_push`） |
| 通讯录→发消息 | `feature/friend/.../FriendDirectory.kt:41,51` | 同一个 `ImEngineStore` 单例，对端仍是 `mock_peer_*` | — |
| 语音消息 | `engine.sendVoice(...)`（`<CC>/ImEngine.kt:270-271`） | body 只记成字符串 `"[语音]"`；录音/播放三端 `missing`（`platform-gap-registry.md:47`） | 无 |

### 6.3 端口设计意图与接真实 SDK 的替换点

| 设计意图 | 落点 | 将来怎么换 |
|---|---|---|
| 聊天唯一入口是 IM 端口，不许蹭 SSE/WS | `docs/adr/0005-three-long-lived-channels-separate-transports.md:23`；`<CC>/ChatPort.kt:3-6`「Features must depend on this port, not vendor TIM/JMessage APIs directly」 | 用真实 `ChatPort` 替 `StubChatPort`（`<CC>/ChatPort.kt:12-16`），改注入点 `<APP>/AppContainer.kt:42` |
| UI 只依赖 `ImEngine` 10 个方法且**可注入** | 接口 `<CC>/ImEngine.kt:17-36`；注入点 `<FK>/ChatScreen.kt:56-57`（带默认值的参数） | 写 `RongImEngine : ImEngine` 替 `ImEngineStore`（`<CC>/ImEngine.kt:145-147`），或在 Store 内按 `RongImConfig.useMockIm`（`<FK>/RongImConfig.kt:67`）二选一 |
| mock 开关语义已与 Dart 对齐 | `forceMockIm :48`、`rongAppKey = "" :54`、`useMockImFor :60-64`、`useMockIm :67`；测试锁死 `ImChatApiContractTest.kt:125-146` | 启动侧赋真 `rongAppKey` 即翻假 —— 但**翻假后 connect 仍无人做**（`CONNECT_TIMEOUT_SECONDS :39` 目前只被测试 `:139` 断言） |
| token → connect 的断口 | `<FK>/ImSessionApi.kt:165-181` 拿到 token 后直接 return；缺口写在 `:133-137` | 补 `RongEngineHolder.connectReal` 等价调用；停用 `ImMockUserIdRegistry`（`:126` 注明真实分支「业务 UUID 就是融云 userId」），并把默认构造换成 `createPersistentImMockUserIdRegistry`（`:116-121`，当前零调用者） |
| 备份队列已备好，缺生产者 | 入队 + 阈值 `:212-218`（`FLUSH_THRESHOLD = 5 :250`）、flush `:220-244`、失败/异常**回插头部 = at-least-once** `:242`；`GAP(R5)` `:91-98` | 把 SDK 消息/撤回回调接到 `ImBackupService.backupOutbound/Inbound/Recall`（`:102-129`）即可上线；注意 Dart mock 语义是 recall 不入队（`:306-314`） |
| 群/资料/好友 HTTP 已就绪 | 第 7.3 节 9 条端点 | 在「建群成功 → 加入/解散群」链路里补 SDK 侧动作（`<FK>/ImGroupApi.kt:79-83`） |
| 时区与日期 | 硬编码 +8h（`<CC>/ImEngine.kt:95-138`，`:97`）；社区侧已承认 UTC/本地差异（`<FC-http>/CommunityPostModels.kt:43-46`） | 接真数据后统一改为带偏移解析 |
| 监听器生命周期 | 正确样板 `<CC>/ImEngine.kt:168-170` + `<FK>/ChatScreen.kt:66-69` | 真 SDK 事件量级上来前，先修 `<FC>/CommunityRouteHost.kt:695-697` 与 `<APP-a>/NativeJetpackTabs.kt:850-852` 两处 `LaunchedEffect(Unit)` 泄漏 |

---

## 7. 端点消费表（method → 调用点行号）

### 7.1 短视频 #72–#78（`<FM>/ShortVideoRepository.kt`）

| # | Method + Path | 实现（行） | 发出 HTTP 的行 | 上层消费者 |
|---|---|---|---|---|
| 72 | `GET /api/v1/short-videos/profile` | `fetchProfile :129-135` | `:130` `api.getApi(PROFILE, queryOf("user_id" to userId))` | **无** |
| 73 | `GET /api/v1/short-videos` | `fetchList :137-159` | `:148` `api.getListPage<JsonObject>(ROOT, query)` | **无** |
| 74 | `POST /api/v1/short-videos` | `create :161-169` | `:166` `api.postApi(ROOT, body)` | **无**（发布页 = toast `:145`） |
| 75 | `DELETE /api/v1/short-videos/:id` | `delete :171-177` | `:172` `api.deleteApi(item(id), emptyMap()) { }` | **无** |
| 76 | `POST /api/v1/short-videos/:id/like` | `toggleLike :179-188` | `:182` | **无**（点赞是本地 toggle） |
| 77 | `DELETE /api/v1/short-videos/:id/like` | 同上 | `:184` | **无** |
| 78 | `POST /api/v1/short-videos/:id/view` | `reportView :190-197` | `:191` | **无** |

### 7.2 社区 #59–#71（`<FC-http>/HttpCommunityPostRepository.kt`）

| # | Method + Path | 实现（行） | 发出 HTTP 的行 | 上层消费者 |
|---|---|---|---|---|
| 59 | `GET /community/posts` | `fetchPosts :82-91` | `:278`（公共 `listPage`） | **无**（UI 用 `engine.posts`，`<FC>/CommunityScreen.kt:72`） |
| 60 | `POST /community/posts` | `createPost :149-169` | `:163` | **无**（`engine.createPost`，`<FC>/CommunityRouteHost.kt:98`） |
| 61 | `GET /community/posts/:id/comments` | `fetchComments :94-99` | `:278` | **无** |
| 62 | `POST /community/posts/:id/comments` | `addComment :102-113` | `:107` | **无** |
| 63/64 | `POST`/`DELETE /community/posts/:id/like` | `toggleLike :120-128` | `:123` / `:125` | **无** |
| 65 | `DELETE /community/posts/:id` | `deletePost :134-139` | `:136`（`deleteApiOk`） | **无**（UI 无删除入口） |
| 66 | `GET /community/topics` | `fetchTopics :172-180` | `:278` | **无** |
| 67 | `GET /community/topics/search` | `searchTopics :183-192` | `:278` | **无** |
| 68 | `GET /community/search?type=all` | `searchAll :216-224` | `:221` | **无** |
| 69 | `GET /community/search?type=post\\|topic\\|user` | `:227-246` → `searchPage :253-263` | `:278` | **无** |
| 70/71 | `POST`/`DELETE /community/users/:id/follow` | `followUser :198-205` / `unfollowUser :208-213` | `:199` / `:210` | **无** |

### 7.3 IM BFF #100–#114（本域部分）

| # | Method + Path | 实现 | 发出 HTTP 的行 | 上层消费者 |
|---|---|---|---|---|
| 100 | `POST /im/session` | `ImSessionApi.createSession`（`<FK>/ImSessionApi.kt:151-183`） | `:171`（**仅 `useMock()==false` 才可达**；默认 `rongAppKey=""` ⇒ 走 `:155-163` 本地造 token、零 HTTP） | **无** |
| 101 | `GET /im/users/profile` | `<FK>/ImUserProfileApi.kt:45-87` | `:61`（空列表 `:47-48` 直接短路，不发 HTTP） | **无** |
| 102 | `POST /im/messages/backup` | `HttpImBackupService.flushPending`（`<FK>/ImMessageBackupApi.kt:220-244`） | `:237` | **无** |
| 109 | `POST /im/groups/free` | `createFreeGroup`（`<FK>/ImGroupApi.kt:101-112`） | `:104` → `postApiJson :190` | **无** |
| 110 | `POST /im/groups/free/:id/invite` | `invite :118-122` | `:150`（`postOk`） | **无** |
| 111 | `POST /im/groups/free/:id/kick` | `kick :128-132` | `:150` | **无** |
| 112 | `POST /im/groups/free/:id/quit` | `quit :138-139`（body 显式 `{}` `:166`） | `:150` | **无** |
| 113 | `POST /im/groups/free/:id/dismiss` | `dismiss :145-146` | `:150` | **无** |
| 114 | `POST /im/groups/store/sync` | `syncStoreGroup :89-98` | `:90` → `postApiJson :190` | **无** |
| 103–108 | 好友侧 5 条 | `feature/friend/.../ImFriendApi.kt:25,28,31,34`（**不在本域**，无跨 feature import） | — | — |

### 7.4 「三模块当前实际发出的业务请求 = 0」的举证

| 事实 | 证据 |
|---|---|
| UI 代码无任何 `NetworkFacade.api()` / 仓储实例化 | 上三表「上层消费者」列全为「无」；全仓 grep `HttpShortVideoRepository`/`HttpCommunityPostRepository`/`ImSessionApi`/`ImUserProfileApi`/`ImGroupApi`/`ImBackupService`/`createImBackupService` 只命中各自定义文件 + `commonTest`；`createPersistentImMockUserIdRegistry` 连测试都没有 |
| 唯一远程 IO 是加载 mock 数据里的静态图 | `<FC>/MockCommunityEngine.kt:318`（`i.pravatar.cc`）、`<FC>/CommunityRouteHost.kt:116-122`、`<FC>/MockCommunityEngine.kt:157-175`（picsum / bee.mp4 串）、`<CC>/ImEngine.kt:90-93`；加载器 `core/ui/src/commonMain/.../PlatformNetworkImage.kt:23-29`（Coil actual `imageCoilMain/.../PlatformNetworkImage.coil.kt:20-34`；**OHOS 占位 actual** `ohosX64Main/.../PlatformNetworkImage.ohosX64.kt:16-30`） |
| 音乐 MP3 走平台播放器，不经 `core/network` | `<FM>/MediaPlayer.kt:56-57` + `<FM-a>:26-66` + `<FM-i>:33-46` |

---

## 8. 测试与真实状态矩阵

### 8.1 测试清单（6 个文件 / 85 个 `@Test`）

| 文件 | 数 | 锁死的行为 | 覆盖真实链路？ |
|---|---|---|---|
| `feature/media/src/commonTest/.../ShortVideoHttpRepositoryTest.kt` | 14 | #72–#78 的 query/body/动词切换/中文兜底/`reportView` 吞错；`FakeApiClient :35-102` 对 PUT/PATCH/postForm/getAbsoluteRaw 一律 `error(...)`（`:53,58,61-63,80-90`） | 只测 HTTP 层；UI 与播放器**零测试** |
| `feature/community/src/commonTest/.../MockCommunityEngineTest.kt` | 7 | 35 条种子、搜索、评论计数、发帖+点赞、following 填充、媒体类型（`:14-84`） | **测的是 mock** |
| `.../commonTest/.../http/CommunityPostModelDecodeTest.kt` | 10 | 全字段解码、裸数组/items、`images` 压 `image_urls`、source 缺省规则、点赞片段默认、评论/话题/search_all 三子页、user hit 默认、`publish_time` 形状矩阵（`:96-354`） | 只测模型 |
| `.../commonTest/.../http/HttpCommunityPostRepositoryTest.kt` | 18 | 14 方法逐条；`RecordingApiClient :41-113`；create body 字符串全等（`:217-221`）；`code==0` 缺 data 仍抛错（`:415`）；无 `code` 判失败 `-1`（`:471`）；传输错直传（`:480`） | 只测 HTTP 层 |
| `feature/chat/src/commonTest/.../MockImEngineTest.kt` | 4 | 发送管线 + 未读、`ensureConversation` 私/群、撤回窗口、过滤（`:15-59`） | **测的是 mock** |
| `feature/chat/src/commonTest/.../ImChatApiContractTest.kt` | 32 | #100–#114 全量：mock 分支不发 HTTP（`:85`）、空列表短路（`:151`）、flush 阈值 5（`:213`）、失败/异常重排（`:234,247`）、at-least-once 顺序（`:256-309`）、ISO 往返（`:362`）、`{}` 显式空对象（`:428,496,522`）；`FakeImApiClient :593-654`、手搓 `runNow :667-684` | 只测 HTTP 层 |

### 8.2 能力真实状态矩阵

| 能力 | Android | iOS | OHOS | 判定依据 |
|---|---|---|---|---|
| 音频播放 | partial（真 `MediaPlayer`，无画面） | partial（真 `AVPlayer` + Playback 会话） | **stub**（仅布尔） | `<FM-a>:26-66`；`<FM-i>:33-46,90-96`；`<FM-o>:7`；`platform-gap-registry.md:22,33` |
| 视频画面播放 | 未实现 | 未实现 | 未实现 | `<FM-a>:16`、`<FM-i>:18-19` 两处 follow-up；`<FM>/VideoRouteHost.kt:436` 播 MP3；`<FC>/CommunityRouteHost.kt:679`「▶ 播放（mock）」 |
| 短视频 HTTP #72–#78 | 代码就绪、**未接线** | 同 | 同 | 7.1 表 |
| 社区流/互动 #59–#71 | 代码就绪、**未接线**；UI 走 mock | 同 | 同 | `<FC>/CommunityScreen.kt:55,72`；`platform-gap-registry.md:20`（follow_up「HttpPostRepository live feed later」） |
| 社区发帖 | mock 写内存 + 默认示例媒体 | 同 | 同 | `<FC>/CommunityRouteHost.kt:98,305`；`platform-gap-registry.md:21` |
| 社区搜索（4 类） | mock contains 过滤 | 同 | 同 | `<FC>/CommunityRouteHost.kt:375-378`；`<FC>/MockCommunityEngine.kt:232-280` |
| IM 连接/登录 | **missing** | missing | missing | `<CC>/ChatPort.kt:13-15`；`platform-gap-registry.md:41` |
| IM 收发/已读/撤回 | mock（280ms/2s） | mock | mock | `<CC>/ImEngine.kt:308-351,276-306,245-262`；`platform-gap-registry.md:19` |
| IM 列表/详情 UI | partial | partial | partial | `platform-gap-registry.md:18`；`<FK>/ChatScreen.kt`（`:457` 单击代长按） |
| IM BFF #100–#114 | 代码就绪 + 32 测、**未接线** | 同 | 同 | 7.3 表；四处 `// GAP(R5)` |
| 聊天语音录制/播放 | missing | missing | missing | `platform-gap-registry.md:47`；`<CC>/ImEngine.kt:270-271` |
| 网络图片（社区预览） | ready（Coil） | ready（Coil） | 占位 | `imageCoilMain/.../PlatformNetworkImage.coil.kt:20-34` vs `ohosX64Main/.../PlatformNetworkImage.ohosX64.kt:16-30` |
| 文件选择 / 上传 | missing | missing | missing | 三模块 grep `pickImage/MultipartBody/uploadFile/image_picker` 命中 0 行；`platform-gap-registry.md:34` |

### 8.3 已知缺陷 / 待办（源码可复核）

| # | 事项 | 证据 |
|---|---|---|
| 1 | 评论区监听器泄漏（`observe` 的注销函数被丢弃） | `<FC>/CommunityRouteHost.kt:695-697`（正确写法见同文件 `:371-374`）；Android Native 同类 `:850-852` |
| 2 | `MusicSession.dismiss()` 只 `stop()` 不 `release()`，iOS `stop()` 仅 `pause()` | `<FM>/MediaPlayer.kt:68-72`；`<FM-i>:70-74` |
| 3 | iOS `setSource` 永不自动起播（`wantPlay` 先清零再判） | `<FM-i>:35` vs `:42-45` |
| 4 | 死代码：`MiniPlayerBar` 与 4 个短视频私有 Composable 无调用点 | `<FM>/MusicScreens.kt:171-223`；`<FM>/VideoRouteHost.kt:277-309,311-342,344-405,408-414` |
| 5 | `VideoRouteHost` 6 个未使用路由 import | `<FM>/VideoRouteHost.kt:79-85` |
| 6 | `CommunityPublishBus` 只清 `pendingTopic`，另两字段永不清洗 | `<FC>/CommunityRouteHost.kt:77-81`（`:101`） |
| 7 | 话题选择页 5 条硬编码与引擎 `mockTopics` 4 条不同源 | `<FC>/CommunityRouteHost.kt:584-604` vs `<FC>/MockCommunityEngine.kt:291-296` |
| 8 | 常量路由进评论区拿不到真实 postId（回落 `defaultPostId()`） | `<FC>/CommunityRouteHost.kt:127-130` vs `:131-136` |
| 9 | mock 注册表默认无持久化，持久化工厂零调用 | `<FK>/ImSessionApi.kt:64-65,112-121` |
| 10 | `ChatTimeFormat` 硬编码 +8h | `<CC>/ImEngine.kt:95-138`（`:97`） |
| 11 | `parseCommunityTimeMs` 裸串按 UTC（与 Dart 本地时区有差） | `<FC-http>/CommunityPostModels.kt:43-47` |
| 12 | 信息流 UI 无分页；`deletePost` 无 UI 入口 | `<FC>/CommunityScreen.kt:72`；`<FC-http>/CommunityPostRepository.kt:61` |

### 8.4 本文档的未验证项（诚实边界）

| 项 | 原因 |
|---|---|
| 85 个测试当前是否全绿 | 按要求**未跑 gradle / build / test**，仅静态读测试源码 |
| 三端运行时表现（音频是否真出声、OHOS actual 选择、Coil 图能否加载） | 未运行任何端 |
| `docs/deep-dive/_verify/verify_docs.py` + `manifest.json` 的格式校验期望 | 本次未读该脚本，若校验规则有额外字段要求需回看 |
| 文中以「Dart :NN」形式出现的引用 | 全部转录自 KMP 源文件注释，本仓无 Dart 源，未逐条回查 SoT |


