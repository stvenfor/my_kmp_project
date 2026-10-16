# Tasks: 同步 Flutter 09-24 → 09-29 增量（IM / 语音 / 鉴权 / LAN）

> 执行权威以本文件为准；分组 W0–W8，粒度=单文件/单行为，任务标注来源 Flutter commit。
> 前置：读 `../parity-flutter-to-kmp/notes/boundary-gate.md`（feature↛feature；IM SDK 走 component/*；tokens 走 DemoColors）。
> 文中 `⏎` 表示文案内换行。

## W0. 基线与登记（脚手架）

- [ ] 0.1 建目录 `openspec/changes/sync-flutter-0929-im-auth-voice/{proposal,design,tasks}.md`（本变更）
- [ ] 0.2 在 `../parity-flutter-to-kmp/notes/platform-gap-registry.md` 登记：「融云 IMLib 真连（Android/iOS/OHOS）」= stub，blocker=三端 SDK 均未接入，follow_up=单独立项接 component/chat
- [ ] 0.3 同上登记：「语音录制/播放（iOS）」「语音录制/播放（OHOS）」= stub；Android 实装后标 partial/ready
- [ ] 0.4 基线绿：改动前 `./gradlew :composeApp:compileDebugKotlinAndroid` 通过
- [ ] 0.5 基线绿：`:composeApp:testDebugUnitTest` 通过

## W1. 鉴权决策核心（Flutter d7876a1 → feature/auth）

- [ ] 1.1 新建 `AuthBizCode.kt`：常量 10002/10003/10021/10022/10023/10024/50000 + `isForceLogout`/`isSessionReplaced`/`isSessionGone`/`isTryRefresh`
- [ ] 1.2 新建 `AuthHttpDecision.kt`：enum `{ ForceLogout, TryRefresh, Forbidden, None }` + `classifyAuthHttp(statusCode, code, message)`（优先级 code > status > 文案）
- [ ] 1.3 文案兜底：无 code 时含「其他设备登录」或「会话无效」→ ForceLogout；`looksLikeExpiredToken`（token 无效/token 已过期/JWT/jwt/token expired/Token expired/未提供 Authorization/未授权）→ TryRefresh
- [ ] 1.4 403 仅看 `statusCode == 403`（禁止用 10003 判定）；`isAuthHttpHardFailure` = ForceLogout || TryRefresh（403 返回 false）
- [ ] 1.5 新建 `AuthFailure.kt`：sealed class 固定中文文案（InvalidCredentials=密码错误；AccountNotRegistered=账号未注册，请先注册；SessionReplaced=账号已在其他设备登录，请重新登录；SessionInvalid=会话无效，请重新登录；SessionCleared=登录已失效，请重新登录；InvalidOtp=验证码错误或已失效；EmailAlreadyRegistered=该邮箱已注册；WeakPassword=密码至少 6 位；NetworkAuth=网络异常，请稍后重试；BackendService=认证服务暂时不可用，请稍后重试）
- [ ] 1.6 `mapAuthFailure` 改为经 `classifyAuthHttp`/`AuthBizCode` 分类映射；10023 或 401 → 「登录已失效，请重新登录」；403 → 「暂无权限」且不触发登出
- [ ] 1.7 `AuthRepository` 失败路径接 `AuthFailure`；`isSessionGone` 时清 `AccountFacade` + `AuthSessionState`
- [ ] 1.8 `NetworkError.Transport` 携带 HTTP status，供 classify 使用
- [ ] 1.9 单测 `AuthHttpDecisionTest`：矩阵全分支 + 403 非 hard + 10003 不判 Forbidden（对齐 Flutter forbidden_status_test.dart）
- [ ] 1.10 `AuthSessionClearTest` 扩展：ForceLogout 清会话；Forbidden 不清
- [ ] 1.11 验收：`--tests '*AuthHttpDecision*' --tests '*AuthSessionClear*'`

## W2. LAN 分流（Flutter 10654a9 → core/network）

- [ ] 2.1 `LanHost.kt`：`fallback = ""`，注释写明「机器 IP 不入库；注入走 NetworkConfig」
- [ ] 2.2 `NetworkConfig` 增 `@Volatile var backendHostOverride: String = ""` 与 `effectiveBackendHost`
- [ ] 2.3 `effectiveBaseUrl()`/`remapBackendLocalhost`：显式注入 host > 平台语义（Android 模拟器 10.0.2.2；iOS/macOS loopback 不改）> 原样返回
- [ ] 2.4 真机未注入：平台 actual 告警一次并原样返回（不阻断）
- [ ] 2.5 grep 全仓清除 `172.16.0.34` 等机器 IP 残留（含 docs/scripts）
- [ ] 2.6 单测：注入/平台重写/原样三分支 + 空注入不误伤
- [ ] 2.7 验收：Android 编译 + core/network 测试

## W3. IM 引擎契约扩展（b70315c/90c7b46 mock 同级面 → feature/chat）

- [ ] 3.1 `ImMessage` 增 `voiceDurationSeconds: Int = 0`、`localPath: String? = null`、`remoteUrl: String? = null`
- [ ] 3.2 `ImEngine` 增 `sendVoice(conversationId, durationSeconds, localPath = null)` 与 `deleteMessage(conversationId, messageId): Boolean`
- [ ] 3.3 `MockImEngine.sendVoice`：预览 `[语音]`，mock 管线（280ms 成功 + 私聊 2s peerRead）
- [ ] 3.4 `MockImEngine.deleteMessage`：移除消息 + 刷新会话预览（对齐「已删除」）
- [ ] 3.5 会话预览规则统一：image→[图片] / voice→[语音] / custom→[自定义消息] / system→content / time→skip / text→content；缺失会话自动建
- [ ] 3.6 `ImConversation` 增 `isGroup: Boolean = false`；`ensureConversation` 原生支持 `group_` 前缀
- [ ] 3.7 `MockImEngineTest` 扩展：voice 预览/时长、delete、group ensure
- [ ] 3.8 验收：`--tests '*MockImEngine*'`

## W4. IM BFF 契约 + 好友页接线（b70315c/90c7b46 → feature/chat + friend）

- [ ] 4.1 `ImBffContracts.kt`：路径常量（users/search、friends、friend-requests（GET/POST、/{id}/respond）、private-admission、groups/store/sync、groups/free（+invite/kick/quit/dismiss）、backup/outbound|inbound|recall）
- [ ] 4.2 `ImBffModels.kt`：`ImBffUser`（user_id/display_name/avatar_url/phone_masked）、`ImBffFriendRequest`（id/from_user_id/status/created_at）、`ImBffFreeGroup`（group_id/name/owner_user_id）、`ImBffStoreGroup`（store_id/group_id）—— snake_case 解析
- [ ] 4.3 `ImFriendApi` 封装（NetworkFacade）：search / listFriends / listIncomingRequests / requestFriend / respondFriend / canPrivateChat
- [ ] 4.4 `ImGroupApi` 封装：syncStoreGroup / createFreeGroup / invite / kick / quit / dismiss
- [ ] 4.5 `friendlyError` 文案矩阵移植：already pending→已发送过申请，等待对方同意；already friends→你们已经是好友了；cannot friend yourself→不能添加自己；user not found→用户不存在；not the request recipient→无权处理该申请
- [ ] 4.6 `FriendScreen` 接线：搜索/列表/申请/同意优先 BFF，失败回退 `FriendDirectory` mock（不阻断，对齐「旧后端无列表接口时不阻断好友页」）
- [ ] 4.7 建群入口走 `createFreeGroup`（mock 回退）→ `ensureConversation("group_$id")` → 打开会话；空列表 toast「请先添加好友再建群」；空名「请填写群名称」；成功「已创建群「{name}」」
- [ ] 4.8 好友空态对齐：80dp 圆容器 + person 图标 +「还没有好友」+「用上方搜索对方手机号，发送申请后⏎对方在「新的朋友」里同意即可聊天」
- [ ] 4.9 toast 文案对齐：申请「已向 {name} 发送好友申请⏎对方可在「通讯录 → 新的朋友」同意」；同意/拒绝「已添加 {name}」/「已拒绝」；单聊校验失败「需先成为好友才能单聊」
- [ ] 4.10 单测：friendlyError 矩阵、模型解析、BFF 失败回退路径
- [ ] 4.11 验收：Android 编译 + 单测

## W5. 语音管线（0fbfe18 → feature/chat + 平台 actual）

- [ ] 5.1 `VoicePipeline.kt`：`VoiceCheckStage { Permission, RecordStart, RecordStop, FileValid, Play, Ok }`、`VoiceCheckReport`（+symptomCode：record_start_failed/record_file_invalid/play_failed/ok）
- [ ] 5.2 `VoiceRecording`（localPath + durationSeconds + pathForSdk：Android 补 `file://`）
- [ ] 5.3 `expect class PlatformVoiceRecorder`：ensurePermission / start（AAC-LC 16kHz 32kbps 单声道 .m4a）/ stop（存在且 ≥100B；15×50ms 落盘等待）/ cancel；Android actual（MediaRecorder）；iOS/OHOS stub actual
- [ ] 5.4 `expect class PlatformVoicePlayer`：play / stop / onComplete；Android actual（MediaPlayer）；iOS/OHOS stub
- [ ] 5.5 `resolveSource` 候选链：localPath → content(非 http) → remoteUrl → content(http)，本地需存在且 >0B
- [ ] 5.6 `mimeForPath`：.m4a/.mp4→audio/mp4、.aac→audio/aac、.wav→audio/wav、.amr→audio/amr、.mp3→audio/mpeg
- [ ] 5.7 按压状态机（commonMain）：pressId 防竞态、200ms tick 计秒、60s 自动发送、上滑 cancelIntent、minVoiceMs=600（toast「说话时间太短」）、结束等待启动 8s、复位
- [ ] 5.8 播放状态机：playingVoiceId + 300ms 三帧动画 + 完成回调 + 本地失败远程重试 + toast「播放失败」/「语音文件不可用」
- [ ] 5.9 `VoicePipelineSelfCheck.run(recordMs=1500)`：权限→录→停→≥100B→播 800ms→停；文案（麦克风权限未授予/录音启动失败/录音停止异常/录音文件无效/播放失败/录音+播放验收通过）
- [ ] 5.10 AndroidManifest 核查/补 `RECORD_AUDIO`
- [ ] 5.11 单测：symptomCode、resolveSource、mime、阈值常量
- [ ] 5.12 验收：`--tests '*Voice*'` + Android 编译

## W6. 聊天 UI 对齐（0fbfe18 → ChatScreen.kt）

- [ ] 6.1 `ImConnectionState` 标签映射（未连接/连接中/已连接/连接失败）；ChatScreen 暴露连接态（mock 恒已连接）
- [ ] 6.2 空态 `ConversationListEmpty`：96dp 圆容器（surface 底 + separator 描边 + 40 图标）+「还没有消息」+「加个好友，发一条问候吧」+ 连接态 caption + 44dp「去通讯录」主按钮（accent/白/w600/radiusLg）+「下拉也可刷新」
- [ ] 6.3 输入面板四模式 `{ Text, Voice, Emoji, More }`：mic/keyboard 切换；有文本→发送键；无文本→表情+更多；面板高度 220 动画
- [ ] 6.4 按住说话 + 手势（按下即录、松开发送、上滑取消）+ 全屏 HUD（黑 45% 遮罩、140dp 圆角卡、mic 48 / 取消态红 0xFFE53935 + 删除图标、秒数、"、「松开手指，取消发送」/「手指上滑，取消发送」）
- [ ] 6.5 语音气泡：宽 `88 + 3.2 × duration` clamp[88,200] dp、"、三波纹（宽 3 圆角 2、静止 8/14/10、播放帧 +2/+1）、左右镜像
- [ ] 6.6 Emoji 面板：16 个 emoji 网格，点击追加
- [ ] 6.7 更多面板：相册/拍摄/语音自检（56dp 白卡 + 28 图标 + 13 标签）；拍摄过相机门闸；自检 toast 报告（含 symptomCode 与 bytes）
- [ ] 6.8 长按菜单：复制（文本，toast「已复制」）/ 删除（toast「已删除」）/ 撤回（超窗 toast「超过 3 分钟，无法撤回」；成功「已撤回」）
- [ ] 6.9 不引入新图片资源（文字/Canvas 表达图标），遵守 composeResources 约束
- [ ] 6.10 验收：Android 编译 + mock 引擎聊天冒烟

## W7. 相机权限门闸 + 杂项（0fbfe18 其余）

- [ ] 7.1 相机门闸：拒绝 toast「需要相机权限才能拍摄，请在系统弹窗中允许」；永久拒绝「相机权限已被关闭。请在系统设置中开启后，再回来拍摄。」+ 跳设置
- [ ] 7.2 图片选择（相册/拍摄）平台 actual；失败 toast「选择图片失败」
- [ ] 7.3 发送失败文案对齐：「发送失败」「图片发送失败」「语音发送失败」「录音文件无效，请重试」「无法开始录音，请检查麦克风权限」
- [ ] 7.4 短视频拍摄/社区发布共用同一门闸文案
- [ ] 7.5 验收：Android 编译 + 拒绝路径冒烟

## W8. 文档、登记与收口

- [ ] 8.1 `platform-gap-registry.md`：0.2/0.3 三行落地（flutter_ref/kmp_ref 路径齐全）
- [ ] 8.2 `parity-inventory.md`：chat、chat/detail、friend 行补本次增量注记
- [ ] 8.3 全量回归：`./gradlew :composeApp:compileDebugKotlinAndroid` && `./gradlew :composeApp:testDebugUnitTest`
- [ ] 8.4 `scripts/check-layer-deps.sh` 通过
- [ ] 8.5 change 收口 notes：落地清单 + 未落地（真 SDK）+ 后续立项建议

## 依赖与顺序

W1/W2 独立可先行；W3 是 W5/W6 的前置；W4 依赖 W3.6；W5 依赖 W3.1–3.3；W6 依赖 W3 + W5；W7 依赖 W6.7；W8 最后。全程遵守：commonMain 优先、expect/actual 平台拆分、`./gradlew` 包装器、每工作流结束跑最窄编译/测试。
