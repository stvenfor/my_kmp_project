# Design: 同步 Flutter 09-24 → 09-29 增量

源仓：`/Users/qpy/Desktop/github/my_ai_project` @ `d7876a1`。原则：**逻辑优先（logic-first）**、commonMain 优先、feature ↛ feature、支付/推送/IM SDK 走 `component/*`、tokens 走 `DemoColors`。

## D1. 鉴权决策核心（对应 Flutter d7876a1）

Flutter 把「401/403/互踢/续期」判定收口到 `classifyAuthHttp`（业务码 > HTTP status > 文案兜底）。KMP 等价物放 `feature/auth/AuthHttpDecision.kt`：

```kotlin
enum class AuthHttpDecision { ForceLogout, TryRefresh, Forbidden, None }

object AuthBizCode {
    const val UNAUTHORIZED = 10002
    const val ACCOUNT_NOT_REGISTERED = 10003
    const val SESSION_REPLACED = 10021
    const val SESSION_INVALID = 10022
    const val TOKEN_INVALID = 10023
    const val INVALID_OTP = 10024
    fun isForceLogout(code: Int?) = code == SESSION_REPLACED || code == SESSION_INVALID
    fun isSessionGone(code: Int?) = isForceLogout(code) || code == TOKEN_INVALID
    fun isTryRefresh(code: Int?) = code == TOKEN_INVALID
}

fun classifyAuthHttp(statusCode: Int?, code: Int?, message: String): AuthHttpDecision
fun isAuthHttpHardFailure(statusCode: Int?, code: Int?, message: String): Boolean // ForceLogout || TryRefresh；403 不算
```

决策矩阵（严格对齐）：10021/10022 → ForceLogout；无 code 且文案含「其他设备登录/会话无效」→ ForceLogout；**仅** statusCode==403 → Forbidden（不得用 10003 判 403）；10023 或 401 或无 code 文案像过期 token → TryRefresh；否则 None。

`AuthFailure` sealed class 携带固定中文文案（密码错误 / 账号未注册，请先注册 / 会话无效，请重新登录 / 登录已失效，请重新登录 …），`mapAuthFailure` 改为经 `classifyAuthHttp` 分类后映射文案。会话侧：ForceLogout → 清 `AccountFacade` + `AuthSessionState.clearLocal()`（本地清、不调远端 logout）+ 提示；TryRefresh → 预留 token refresh 钩子（BFF `/user/refresh` 契约已存在时接，无 refresh token 时按 Flutter `handleRefreshExhausted` 收尾）；Forbidden → 不动会话。

## D2. LAN 分流（对应 10654a9）

问题：KMP `LanHost.fallback = "172.16.0.34"` 是某台机器的 IP，随源码入库（Flutter 侧已专门修复过同类问题）。

方案：`LanHost.fallback = ""`；`NetworkConfig` 增加 `backendHostOverride`（运行时注入，来源：debug 环境选择页/自定义地址，与 `baseUrlOverride` 合并语义：显式注入 > 环境默认）。`remapBackendLocalhost` 保持平台语义（Android 模拟器 10.0.2.2；iOS 模拟器/macOS 不改；真机未注入 → 告警一次、原样返回）。**不从源码携带任何机器 IP。**

## D3. IM 引擎契约（对应 b70315c/90c7b46 的 mock 同级面）

`ImEngine` 扩展（保持 `MockImEngine` 为默认实现，`ImEngineStore.engine` 不变）：

- `ImMessage` + `voiceDurationSeconds: Int = 0`、`localPath: String?`、`remoteUrl: String?`；
- `sendVoice(conversationId, label, durationSeconds)`：预览 `[语音]`，mock 管线沿用 280ms 成功 + 2s 已读；
- `deleteMessage(conversationId, messageId): Boolean`（Flutter「已删除」本地移除）；
- 撤回窗口仍 180s；状态文案沿用 statusLabel。

Flutter `ImChatRepository` 的双分支（SDK / mock）映射为 KMP `ChatPort` 组件层的可插拔：本变更只落**契约与 mock**，真融云 SDK 由后续变更接 `component/chat`（Registry 登记 `stub`）。

## D4. IM HTTP 契约（BFF 真接口，mock 容错回退）

Flutter `ImFriendApi`/`ImGroupApi` 走 Go BFF（`RongImConfig` 路径表）。KMP 落 `feature/chat/ImBffContracts.kt`（数据模型 + 路径常量 + `NetworkFacade` 调用封装），好友页调用处**容错回退**：

- 路径表：`/api/v1/im/users/search?q=`、`/api/v1/im/friends`、`/api/v1/im/friend-requests`（GET 列表 / POST 申请 / `/{id}/respond`）、`/api/v1/im/private-admission?peer_user_id=`、`/api/v1/im/groups/store/sync`、`/api/v1/im/groups/free`（+ `/invite|/kick|/quit|/dismiss`）、备份 `/api/v1/im/backup/outbound|inbound|recall`。
- 字段对齐 snake_case（`user_id/display_name/avatar_url/phone_masked`…）。
- `friendlyError` 文案矩阵逐条移植（already pending → 已发送过申请，等待对方同意；already friends → 你们已经是好友了；cannot friend yourself → 不能添加自己；user not found → 用户不存在；not the request recipient → 无权处理该申请）。
- 网络/未注册失败 → 回退 `FriendDirectory` mock（Flutter：`listIncomingRequests` 失败不阻断好友页）。

## D5. 语音管线（对应 0fbfe18）

逻辑层 `feature/chat/VoicePipeline.kt`（commonMain）：

- `VoiceCheckStage { Permission, RecordStart, RecordStop, FileValid, Play, Ok }`、`VoiceCheckReport(passed, failedStage, message, path, bytes, elapsedMs, error)`、`symptomCode`（record_start_failed / record_file_invalid / play_failed / ok）。
- `expect class PlatformVoiceRecorder`：`ensurePermission(): Boolean`、`start(path)`（aacLc 16k/32k/单声道）、`stop(): VoiceRecording?`、`cancel()`；`expect class PlatformVoicePlayer`：`play(pathOrUrl)`、`stop()`。Android actual（MediaRecorder/MediaPlayer）；iOS/OHOS stub → Registry。
- 按压状态机（对齐 `ChatDetailViewModel`）：按下即录（无长按延时）、200ms tick 计秒、上滑取消判定、`minVoiceMs = 600`（< 600ms 提示「说话时间太短」）、60s 自动发送、结束时 8s 等待启动 Future、`pressId` 防竞态。
- 播放：`resolveSource`（本地文件存在且 >0B 优先，退远程 URL）、`mimeForPath`（.m4a→audio/mp4 …）、300ms 三帧波纹动画 tick、完成回调停动画。
- 自检 `VoicePipelineSelfCheck.run(recordMs = 1500)`：权限 → 录 1.5s → 校验 ≥100B → 播放 800ms → 停；报告含 symptomCode。

UI：`ChatDetailScreen` 输入面板四模式（text/voice/emoji/more）+ 按住说话按钮 + 全屏 HUD（黑 45% 遮罩、140dp 圆角卡、取消态红 0xFFE53935 + 删除图标、「松开手指，取消发送 / 手指上滑，取消发送」）+ 语音气泡（宽 `88 + 3.2·s` dp，clamp 88–200，`n"` + 三条波纹，播放帧高 8/14/10 + frame 增量）+ 更多面板（相册/拍摄/语音自检，56dp 白卡 + 28dp 图标）。emoji 集 16 个沿用。

## D6. 会话空态与连接态（对应 0fbfe18 chat_page）

`ImConnectionState { Disconnected, Connecting, Connected, Failed }` → 标签「未连接/连接中/已连接/连接失败」。空态：96dp 圆形图标容器（surface 底 + separator 描边）+「还没有消息」headline +「加个好友，发一条问候吧」subhead +（连接态提示 caption）+ 44dp 高「去通讯录」FilledButton（accent、radiusLg、白字 w600）+「下拉也可刷新」caption 文本按钮。搜索空态仍「没有匹配的会话」。

## D7. 相机权限门闸（对应 0fbfe18 camera_permission_gate）

`PermissionGate`（core/ui 已有）扩展 `ensureCameraPermission`：拒绝 → toast「需要相机权限才能拍摄，请在系统弹窗中允许」；永久拒绝 → 引导设置「相机权限已被关闭。请在系统设置中开启后，再回来拍摄。」。图像选择（相册/拍摄）经平台 picker；失败 toast「选择图片失败」。

## D8. 验收与登记

- 单测：决策矩阵全分支、`_looksLikeExpiredToken` 兜底、`isAuthHttpHardFailure`（403 不算）、语音阈值（600ms/60s/100B/symptomCode）、LAN remap（注入>平台>真机告警）、好友 API 容错回退、`friendlyError` 文案、语音气泡宽度公式。
- `platform-gap-registry.md`：新增「融云 SDK（三端）」「iOS/OHOS 语音录制播放」行，状态 stub/partial + blocker + follow_up。
- `parity-inventory.md`：chat/friend 相关路由补 `logic-pass` 注记（本次同步范围）。

## 风险

- `ImMessage` 加字段是 ABI 变更，commonTest 需同步（MockImEngineTest）。
- Android `MediaRecorder` 需要 `RECORD_AUDIO` 权限清单项（AndroidManifest 已有麦克风？需核查；无则补）。
- 403 判定不得复用 `ACCOUNT_NOT_REGISTERED`(10003)。
