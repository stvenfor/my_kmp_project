import { ArkUIViewController, Compose } from '@cpf-kmp-cmp/compose';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { deviceInfo } from '@kit.BasicServicesKit';
import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import { promptAction } from '@kit.ArkUI';
import libentry from 'libentry.so';
import { registerVideoInteropBuilders } from '../ComposeVideoInterops';
import { registerWebInteropBuilders } from '../ComposeWebInterops';

import { DesignTokens, AppPhase, MainTab } from './NativeShellShared';
import { NativeHomeContent } from './NativeHomeContent';
import { NativeLoginPane } from '../auth/NativeLoginPane';
import { AuthSession, sharedSessionStore } from '../auth/SessionStore';
import { PENDING_DEEP_LINK_KEY } from '../entryability/EntryAbility';
import { featurePageFor, FeatureRow, resolveNativePath } from './NativeFeatureCatalog';
import {
  NativeSearchPane,
  NativeAllServicesPane,
  NativeLearningReportPane,
  NativePurchaseCalculatorPane,
  NativeFriendPane,
  NativeCommunitySearchPane,
  NativeCommunityPublishPane,
  NativeCommunityCommentPane,
  NativeCommunityImagePreviewPane,
  NativeMallPane,
  NativeWalletPane,
  NativeShortVideoPane,
  NativeLivePane,
  NativeAiStreamPane,
  NativeScanPane,
  NativeStrategyPane,
  NativeCheckInMallPane,
  NativeMusicPane,
  NativeClassroomPane,
  NativeUsedCarPane,
  NativeNewCarFollowPane,
  NativeClubPane,
  NativeLifeServicePane,
  NativeTodoListPane,
  NativeWebPane,
  NativeAfterSalesPane,
  NativeLedgerPane,
  NativeSmsTemplatePane,
  NativeStoreQrPane,
  NativeQaPane,
  NativePosterPane,
  NativeBusinessPane,
  NativeRemindersPane,
  NativeFeedbackPane,
  NativeProfilePane,
  NativeAddressesPane,
  NativeDubbingFeedPane,
  NativeHotRankPane,
  NativeDataAnalyticsPane,
  NativeBusinessCardPane,
} from './NativeSecondaryPages';

const DOMAIN = 0x0000;

interface MineFunctionCard {
  title: string;
  subtitle: string;
  value: string;
}

interface MineMenuRow {
  label: string;
  badge: boolean;
}

/** Flutter MockImChatStore conversation / message shapes for ArkTS ChatPane. */
interface ChatConv {
  id: string;
  peerId: string;
  title: string;
  lastMessage: string;
  lastMessageAtMs: number;
  unreadCount: number;
  isOnline: boolean;
}

interface ChatMsg {
  id: string;
  body: string;
  isSelf: boolean;
  createdAtMs: number;
  sendStatus: string; // sending | success | failed
  readStatus: string; // unread | read
  type: string; // text | image | voice | custom | time
}

/** Flutter MockPostRepository post shape for ArkTS CommunityPane. */
interface CommunityFeedPost {
  id: string;
  userId: string;
  nickname: string;
  content: string;
  publishAtMs: number;
  source: string;
  images: string[];
  videoCoverUrl: string;
  likeCount: number;
  commentCount: number;
  isLiked: boolean;
  /** [nickname, content, replyToNickname?] — Flutter previewComments. */
  previewComments: string[][];
}

/**
 * ADR 0002: ArkTS owns splash / privacy / tab roots / Mine root / auth /
 * deferred secondary stubs. Compose hosts **Mine island only**.
 */
@Entry
@Component
struct Index {
  @State phase: AppPhase = AppPhase.Splash;
  @State privacyAccepted: boolean = false;
  @State tab: MainTab = MainTab.Home;
  @State isLoggedIn: boolean = false;
  @State showLogin: boolean = false;
  @State showIsland: boolean = false;
  @State communityFilter: string = '最新';
  @State errorMessage: string = '';
  @State stubTitle: string = '';
  @State showStub: boolean = false;
  @State loginDisplayName: string = '访客';
  @State chatSelectedId: string = '';
  @State chatDraft: string = '';
  @State chatConversations: ChatConv[] = [];
  @State chatMessagesById: Record<string, ChatMsg[]> = {};
  @State chatSearchOpen: boolean = false;
  @State chatSearchQuery: string = '';
  @State chatVoiceMode: boolean = false;
  @State chatShowEmoji: boolean = false;
  @State chatShowMore: boolean = false;
  @State chatTick: number = 0; // bump to force UI refresh after async send/read
  @State communityPosts: CommunityFeedPost[] = [];
  @State communityTick: number = 0;
  @State showSwitchStore: boolean = false;
  @State selectedStoreId: string = '1';
  @State nativePane: string = '';
  @State featurePath: string = '';
  private islandController: ArkUIViewController | undefined = undefined;
  private pref: preferences.Preferences | undefined = undefined;
  private chatLocalSeq: number = 0;
  private chatSeeded: boolean = false;
  private communitySeeded: boolean = false;
  private communityFollowed: string[] = [];
  private readonly mineStores: string[][] = [
    ['1', '[4S]北京沃德龙鼎吉利'],
    ['2', '[4S]北京腾远吉利'],
  ];

  private readonly communitySamples: string[] = [
    '今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev',
    '周末 hiking，天气太好了！#户外',
    '刚读完一本好书，推荐 @李四 也看看。',
    '分享一张随手拍～',
    '项目上线啦，感谢团队！#Flutter开发 https://dart.dev',
    '午餐打卡 @王五',
    '学习 GetX 状态管理中…',
  ];
  private readonly communityNicknames: string[] = [
    '张三', '李四', '王五', '赵六', '小明', '小红', '开发者', '产品经理',
  ];
  private readonly mineFunctions: MineFunctionCard[] = [
    { title: '短信模板', subtitle: '一键发送 轻松快捷', value: '' },
    { title: '购车计算器', subtitle: '全款/贷款/保险全能算', value: '5830.00' },
    { title: '二手车', subtitle: '置换/专卖/收车', value: '' },
    { title: '收支', subtitle: '个人收支记录', value: '' },
    { title: '小视频', subtitle: '用小视频秀车秀店', value: '' },
    { title: '售后专区', subtitle: '售后维修保养记录', value: '' },
    { title: '店铺收款码', subtitle: '常见问题 功能介绍', value: '' },
    { title: '选买问答', subtitle: '在线解答客户问题', value: '' },
    { title: '商家海报', subtitle: '置换/专卖/估价', value: '' },
  ];
  private readonly mineMenu: MineMenuRow[] = [
    { label: '收货地址', badge: false },
    { label: '商务合作', badge: false },
    { label: '提醒事项', badge: false },
    { label: '邀请好友', badge: false },
    { label: '粉丝群', badge: true },
    { label: '意见反馈', badge: false },
    { label: '设置', badge: false },
  ];
  private readonly communityBodies: string[] = [
    '今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发',
    '周末 hiking，天气太好了！#户外',
    '刚读完一本好书，推荐 @李四 也看看。',
  ];

  aboutToAppear() {
    registerVideoInteropBuilders();
    registerWebInteropBuilders();
    this.ensureChatSeed();
    this.ensureCommunitySeed();
    const ctx = getContext(this) as common.UIAbilityContext;
    preferences.getPreferences(ctx, { name: 'native_shell' }).then((p: preferences.Preferences) => {
      this.pref = p;
      this.privacyAccepted = p.getSync('privacy_accepted', false) as boolean;
    });
    sharedSessionStore.init(ctx).then(() => {
      const session = sharedSessionStore.load();
      if (session?.token) {
        this.applySession(session);
      }
    });
    setTimeout(() => {
      this.phase = this.privacyAccepted ? AppPhase.Main : AppPhase.Privacy;
      this.consumePendingDeepLink();
    }, 1200);
  }

  /** myai://… / path from EntryAbility → SecondaryRouteIsland. */
  private consumePendingDeepLink(): void {
    const uri = AppStorage.get<string>(PENDING_DEEP_LINK_KEY);
    if (!uri) {
      return;
    }
    AppStorage.delete(PENDING_DEEP_LINK_KEY);
    const route = this.routeFromDeepLinkUri(uri);
    if (route) {
      this.privacyAccepted = true;
      this.phase = AppPhase.Main;
      this.openDeferred(route);
    }
  }

  private routeFromDeepLinkUri(uri: string): string | undefined {
    const raw = uri.trim();
    if (!raw) {
      return undefined;
    }
    if (raw.startsWith('/')) {
      return raw;
    }
    // myai:///friend → /friend ; myai://home/search → /home/search
    const schemeSplit = raw.indexOf('://');
    if (schemeSplit < 0) {
      return raw.startsWith('/') ? raw : `/${raw}`;
    }
    let path = raw.substring(schemeSplit + 3);
    while (path.startsWith('/')) {
      path = path.substring(1);
    }
    if (!path) {
      return undefined;
    }
    return `/${path}`;
  }

  private applySession(session: AuthSession): void {
    this.isLoggedIn = true;
    this.loginDisplayName = session.userName || session.phone || '华为用户';
    this.showLogin = false;
  }

  private peerFromChatDetail(raw: String): string {
    const qIndex = raw.indexOf('?');
    if (qIndex < 0) {
      return 'mock_peer_01';
    }
    const query = raw.substring(qIndex + 1);
    const parts = query.split('&');
    let peer = '';
    for (let i = 0; i < parts.length; i++) {
      const eq = parts[i].indexOf('=');
      if (eq <= 0) {
        continue;
      }
      const key = parts[i].substring(0, eq);
      const value = decodeURIComponent(parts[i].substring(eq + 1).replace(/\+/g, ' '));
      if (key === 'peerName' || key === 'name' || key === 'title') {
        peer = value;
        break;
      }
      if ((key === 'id' || key === 'peerId') && peer.length === 0) {
        peer = value;
      }
    }
    return peer.length > 0 ? peer : 'mock_peer_01';
  }

  /** Flutter MockImChatStore.seed — 3 peers, private_$id, unread on first. */
  private ensureChatSeed(): void {
    if (this.chatSeeded) {
      return;
    }
    this.chatSeeded = true;
    const now = Date.now();
    const peers: string[] = ['mock_peer_01', 'mock_peer_02', 'mock_peer_03'];
    const convs: ChatConv[] = [];
    const msgs: Record<string, ChatMsg[]> = {};
    for (let i = 0; i < peers.length; i++) {
      const peer = peers[i];
      const storageId = `private_${peer}`;
      convs.push({
        id: storageId,
        peerId: peer,
        title: `Mock好友${i + 1}`,
        lastMessage: i === 0 ? '晚上一起吃饭吗？' : '你好',
        lastMessageAtMs: now - 5 * 60 * 1000 * (i + 1),
        unreadCount: i === 0 ? 2 : 0,
        isOnline: i % 2 === 0,
      });
      if (i === 0) {
        // Newest-first (Flutter insert(0) + reverse ListView).
        msgs[storageId] = [
          {
            id: 'm_2',
            body: '在的，有什么事？',
            isSelf: true,
            createdAtMs: now - 28 * 60 * 1000,
            sendStatus: 'success',
            readStatus: 'read',
            type: 'text',
          },
          {
            id: 'm_1',
            body: '你好，在吗？',
            isSelf: false,
            createdAtMs: now - 30 * 60 * 1000,
            sendStatus: 'success',
            readStatus: 'read',
            type: 'text',
          },
        ];
      }
    }
    convs.sort((a, b) => b.lastMessageAtMs - a.lastMessageAtMs);
    this.chatConversations = convs;
    this.chatMessagesById = msgs;
  }

  private chatHm(ms: number): string {
    const d = new Date(ms);
    const h = d.getHours().toString().padStart(2, '0');
    const m = d.getMinutes().toString().padStart(2, '0');
    return `${h}:${m}`;
  }

  private chatListTime(ms: number): string {
    const d = new Date(ms);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) {
      return this.chatHm(ms);
    }
    const y = new Date(now);
    y.setDate(y.getDate() - 1);
    if (d.toDateString() === y.toDateString()) {
      return '昨天';
    }
    return `${d.getMonth() + 1}/${d.getDate()}`;
  }

  private chatStatusLabel(m: ChatMsg): string {
    if (!m.isSelf || m.type === 'time') {
      return '';
    }
    if (m.sendStatus === 'sending') {
      return '发送中';
    }
    if (m.sendStatus === 'failed') {
      return '发送失败';
    }
    return m.readStatus === 'read' ? '已读' : '送达';
  }

  private visibleChatConversations(): ChatConv[] {
    const q = this.chatSearchQuery.trim().toLowerCase();
    if (q.length === 0) {
      return this.chatConversations;
    }
    return this.chatConversations.filter((c: ChatConv) =>
      c.title.toLowerCase().indexOf(q) >= 0 || c.lastMessage.toLowerCase().indexOf(q) >= 0);
  }

  private messagesFor(id: string): ChatMsg[] {
    // Newest-first in store; UI shows chronological (reverse for List).
    const list = this.chatMessagesById[id];
    return list !== undefined ? list : [];
  }

  private markChatRead(id: string): void {
    const list = this.chatMessagesById[id];
    if (list !== undefined) {
      const next: ChatMsg[] = [];
      for (let i = 0; i < list.length; i++) {
        const m = list[i];
        if (!m.isSelf && m.readStatus === 'unread') {
          next.push({
            id: m.id,
            body: m.body,
            isSelf: m.isSelf,
            createdAtMs: m.createdAtMs,
            sendStatus: m.sendStatus,
            readStatus: 'read',
            type: m.type,
          });
        } else {
          next.push(m);
        }
      }
      const map: Record<string, ChatMsg[]> = {};
      const keys = Object.keys(this.chatMessagesById);
      for (let k = 0; k < keys.length; k++) {
        map[keys[k]] = keys[k] === id ? next : this.chatMessagesById[keys[k]];
      }
      this.chatMessagesById = map;
    }
    const convs: ChatConv[] = [];
    for (let i = 0; i < this.chatConversations.length; i++) {
      const c = this.chatConversations[i];
      if (c.id === id) {
        convs.push({
          id: c.id,
          peerId: c.peerId,
          title: c.title,
          lastMessage: c.lastMessage,
          lastMessageAtMs: c.lastMessageAtMs,
          unreadCount: 0,
          isOnline: c.isOnline,
        });
      } else {
        convs.push(c);
      }
    }
    this.chatConversations = convs;
    this.chatTick = this.chatTick + 1;
  }

  private ensureChatConversation(rawId: string, title: string): string {
    this.ensureChatSeed();
    const storageId = (rawId.indexOf('private_') === 0 || rawId.indexOf('group_') === 0)
      ? rawId : `private_${rawId}`;
    const existing = this.chatConversations.find((c: ChatConv) => c.id === storageId);
    if (existing) {
      if (title.length > 0 && existing.title !== title) {
        const convs: ChatConv[] = [];
        for (let i = 0; i < this.chatConversations.length; i++) {
          const c = this.chatConversations[i];
          if (c.id === storageId) {
            convs.push({
              id: c.id,
              peerId: c.peerId,
              title: title,
              lastMessage: c.lastMessage,
              lastMessageAtMs: Date.now(),
              unreadCount: c.unreadCount > 0 ? c.unreadCount : 1,
              isOnline: c.isOnline,
            });
          } else {
            convs.push(c);
          }
        }
        convs.sort((a, b) => b.lastMessageAtMs - a.lastMessageAtMs);
        this.chatConversations = convs;
      }
      return storageId;
    }
    const peer = storageId.replace('private_', '');
    const now = Date.now();
    const conv: ChatConv = {
      id: storageId,
      peerId: peer,
      title: title.length > 0 ? title : '推送会话',
      lastMessage: '来自 Push/Deeplink 的 mock 会话',
      lastMessageAtMs: now,
      unreadCount: 1,
      isOnline: false,
    };
    this.chatConversations = [conv].concat(this.chatConversations);
    const map: Record<string, ChatMsg[]> = {};
    const keys = Object.keys(this.chatMessagesById);
    for (let k = 0; k < keys.length; k++) {
      map[keys[k]] = this.chatMessagesById[keys[k]];
    }
    map[storageId] = [{
      id: 'm_push',
      body: '来自 Push/Deeplink 的 mock 会话',
      isSelf: false,
      createdAtMs: now,
      sendStatus: 'success',
      readStatus: 'unread',
      type: 'text',
    }];
    this.chatMessagesById = map;
    return storageId;
  }

  private sendChat(id: string, type: string, body: string, preview: string): void {
    const text = body.trim();
    if (text.length === 0) {
      return;
    }
    const now = Date.now();
    let list: ChatMsg[] = this.messagesFor(id).slice();
    const latest = list.length > 0 ? list[0] : undefined;
    if (latest === undefined ||
      (latest.type !== 'time' && Math.abs(now - latest.createdAtMs) >= 5 * 60 * 1000)) {
      list = [{
        id: `time_${now}`,
        body: this.chatHm(now),
        isSelf: false,
        createdAtMs: now,
        sendStatus: 'success',
        readStatus: 'read',
        type: 'time',
      }].concat(list);
    }
    this.chatLocalSeq += 1;
    const localId = `local_${this.chatLocalSeq}_${now}`;
    list = [{
      id: localId,
      body: text,
      isSelf: true,
      createdAtMs: now,
      sendStatus: 'sending',
      readStatus: 'unread',
      type: type,
    }].concat(list);
    const map: Record<string, ChatMsg[]> = {};
    const keys = Object.keys(this.chatMessagesById);
    for (let k = 0; k < keys.length; k++) {
      map[keys[k]] = this.chatMessagesById[keys[k]];
    }
    map[id] = list;
    this.chatMessagesById = map;

    const convs: ChatConv[] = [];
    for (let i = 0; i < this.chatConversations.length; i++) {
      const c = this.chatConversations[i];
      if (c.id === id) {
        convs.push({
          id: c.id,
          peerId: c.peerId,
          title: c.title,
          lastMessage: preview,
          lastMessageAtMs: now,
          unreadCount: c.unreadCount,
          isOnline: c.isOnline,
        });
      } else {
        convs.push(c);
      }
    }
    convs.sort((a, b) => b.lastMessageAtMs - a.lastMessageAtMs);
    this.chatConversations = convs;
    this.chatTick = this.chatTick + 1;

    setTimeout(() => {
      this.patchChatMsg(id, localId, 'success', undefined);
      setTimeout(() => {
        this.patchChatMsg(id, localId, undefined, 'read');
      }, 2000);
    }, 280);
  }

  private patchChatMsg(id: string, localId: string, sendStatus?: string, readStatus?: string): void {
    const list = this.messagesFor(id);
    if (list.length === 0) {
      return;
    }
    const next: ChatMsg[] = [];
    for (let i = 0; i < list.length; i++) {
      const m = list[i];
      if (m.id === localId) {
        next.push({
          id: m.id,
          body: m.body,
          isSelf: m.isSelf,
          createdAtMs: m.createdAtMs,
          sendStatus: sendStatus !== undefined ? sendStatus : m.sendStatus,
          readStatus: readStatus !== undefined ? readStatus : m.readStatus,
          type: m.type,
        });
      } else {
        next.push(m);
      }
    }
    const map: Record<string, ChatMsg[]> = {};
    const keys = Object.keys(this.chatMessagesById);
    for (let k = 0; k < keys.length; k++) {
      map[keys[k]] = keys[k] === id ? next : this.chatMessagesById[keys[k]];
    }
    this.chatMessagesById = map;
    this.chatTick = this.chatTick + 1;
  }

  private openChatDetail(id: string): void {
    this.chatSelectedId = id;
    this.markChatRead(id);
    this.chatDraft = '';
    this.chatVoiceMode = false;
    this.chatShowEmoji = false;
    this.chatShowMore = false;
  }

  private chatDetailTitle(): string {
    for (let i = 0; i < this.chatConversations.length; i++) {
      if (this.chatConversations[i].id === this.chatSelectedId) {
        return this.chatConversations[i].title;
      }
    }
    return '';
  }

  private mineStoreName(): string {
    for (let i = 0; i < this.mineStores.length; i++) {
      if (this.mineStores[i][0] === this.selectedStoreId) {
        return this.mineStores[i][1];
      }
    }
    return this.mineStores[0][1];
  }

  @Builder
  SwitchStoreDialog() {
    Column() {
      Column() {
        Text('切换店铺')
          .fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Text(this.mineStores.length === 0 ? '暂无可切换的店铺' : '可切换多个店铺查看数据')
          .fontSize(13).fontColor(DesignTokens.body).margin({ top: 8, bottom: 20 })
        ForEach(this.mineStores, (store: string[]) => {
          Text(store[1])
            .fontSize(15).fontWeight(FontWeight.Medium)
            .fontColor(store[0] === this.selectedStoreId ? '#1B82D2' : DesignTokens.ink)
            .width('100%')
            .textAlign(TextAlign.Center)
            .padding({ top: 14, bottom: 14 })
            .border({
              width: 1,
              color: store[0] === this.selectedStoreId ? '#1B82D2' : DesignTokens.hairline,
              radius: 24,
            })
            .margin({ bottom: 12 })
            .onClick(() => {
              if (store[0] !== this.selectedStoreId) {
                this.selectedStoreId = store[0];
              }
              this.showSwitchStore = false;
            })
        }, (store: string[]) => store[0])
      }
      .width('100%')
      .padding({ left: 20, right: 20, top: 24, bottom: 20 })
      .backgroundColor(DesignTokens.canvas)
      .borderRadius(12)

      Text('✕')
        .fontSize(16).fontColor(Color.White)
        .width(36).height(36).textAlign(TextAlign.Center)
        .backgroundColor('#59000000')
        .borderRadius(18)
        .margin({ top: 20 })
        .onClick(() => { this.showSwitchStore = false; })
    }
    .width('80%')
    .alignItems(HorizontalAlign.Center)
  }

  /** Flutter MockPostRepository.seed — 35 posts, Random(42) shape. */
  private ensureCommunitySeed(): void {
    if (this.communitySeeded) {
      return;
    }
    this.communitySeeded = true;
    const now = Date.now();
    // LCG seed 42 — deterministic enough for mock (not Dart-identical).
    let seed = 42;
    const nextInt = (bound: number): number => {
      seed = (seed * 1103515245 + 12345) & 0x7fffffff;
      return seed % bound;
    };
    const list: CommunityFeedPost[] = [];
    for (let i = 0; i < 35; i++) {
      const id = `post_${i}`;
      const isVideo = i % 10 === 0;
      const imgCount = isVideo ? 0 : (i % 9) + 1;
      const images: string[] = [];
      for (let j = 0; j < imgCount; j++) {
        images.push(`https://picsum.photos/seed/${id}_${j}/400/400`);
      }
      list.push({
        id: id,
        userId: `user_${i % 8}`,
        nickname: this.communityNicknames[i % this.communityNicknames.length],
        content: this.communitySamples[i % this.communitySamples.length],
        publishAtMs: now - (i * 17 + nextInt(30)) * 60 * 1000,
        source: i % 2 === 0 ? '来自 iPhone' : '来自 Android',
        images: images,
        videoCoverUrl: isVideo ? `https://picsum.photos/seed/video_${i}/640/360` : '',
        likeCount: nextInt(200),
        commentCount: 2 + nextInt(8),
        isLiked: i % 4 === 0,
        previewComments: [
          [this.communityNicknames[(i + 1) % this.communityNicknames.length], '说得对！', ''],
          [
            this.communityNicknames[(i + 3) % this.communityNicknames.length],
            '同感 +1',
            this.communityNicknames[i % this.communityNicknames.length],
          ],
        ],
      });
    }
    this.communityPosts = list;
  }

  private communityFeed(): CommunityFeedPost[] {
    this.ensureCommunitySeed();
    let source = this.communityPosts.slice();
    if (this.communityFilter === '热门') {
      source.sort((a, b) => {
        const ha = a.likeCount * 2 + a.commentCount;
        const hb = b.likeCount * 2 + b.commentCount;
        if (hb !== ha) {
          return hb - ha;
        }
        return b.publishAtMs - a.publishAtMs;
      });
    } else if (this.communityFilter === '关注') {
      source = source.filter((p: CommunityFeedPost) => this.communityFollowed.indexOf(p.userId) >= 0);
    } else {
      source.sort((a, b) => b.publishAtMs - a.publishAtMs);
    }
    return source.slice(0, 10);
  }

  private communityMeta(p: CommunityFeedPost): string {
    const mins = Math.max(0, Math.floor((Date.now() - p.publishAtMs) / 60000));
    let rel = '刚刚';
    if (mins >= 1 && mins < 60) {
      rel = `${mins}分钟前`;
    } else if (mins < 60 * 24) {
      rel = `${Math.floor(mins / 60)}小时前`;
    } else if (mins < 60 * 48) {
      rel = '昨天';
    } else {
      rel = `${Math.floor(mins / (60 * 24))}天前`;
    }
    return `${rel} · ${p.source}`;
  }

  private toggleCommunityLike(id: string): void {
    const next: CommunityFeedPost[] = [];
    for (let i = 0; i < this.communityPosts.length; i++) {
      const p = this.communityPosts[i];
      if (p.id === id) {
        const liked = !p.isLiked;
        next.push({
          id: p.id,
          userId: p.userId,
          nickname: p.nickname,
          content: p.content,
          publishAtMs: p.publishAtMs,
          source: p.source,
          images: p.images,
          videoCoverUrl: p.videoCoverUrl,
          likeCount: Math.max(0, p.likeCount + (liked ? 1 : -1)),
          commentCount: p.commentCount,
          isLiked: liked,
        });
      } else {
        next.push(p);
      }
    }
    this.communityPosts = next;
    this.communityTick = this.communityTick + 1;
  }

  /** kind 0 = Mine island, kind 1 = secondary RoutePath (set via SetOhosSecondaryRoute). */
  private openComposeHost(kind: number, routeCode: number) {
    const abi: string = deviceInfo.abiList;
    if (libentry === undefined) {
      this.errorMessage = `libentry.so failed (abi=${abi})`;
      return;
    }
    const setHost = libentry.SetOhosHost;
    if (setHost !== undefined) {
      setHost(kind, routeCode);
    }
    const createController = libentry.MainArkUIViewController;
    if (createController === undefined) {
      this.errorMessage = 'MainArkUIViewController missing';
      return;
    }
    try {
      this.islandController = createController();
      this.showIsland = true;
    } catch (error) {
      this.errorMessage = String(error);
      hilog.error(DOMAIN, 'Compose', 'island create failed %{public}s', String(error));
    }
  }

  private openMineIsland(personalized: boolean) {
    this.openComposeHost(0, personalized ? 1 : 0);
  }

  private openSecondaryCompose(route: string): void {
    if (libentry !== undefined && libentry.SetOhosSecondaryRoute !== undefined) {
      libentry.SetOhosSecondaryRoute(route);
    }
    this.openComposeHost(1, 0);
  }

  private openDeferred(title: string): void {
    // Flutter MineController toast-only / Mine-root dialogs — never navigable routes.
    const toastOnly: string[] = [
      '切换门店', '切换店铺', '电子名片', '商务合作', '提醒事项',
      '邀请好友', '粉丝群', '意见反馈', '帮助中心', '头像', '新车成交', '请先登录',
      '/mine/business_card', '/mine/invite', '/mine/business',
      '/mine/reminders', '/mine/reminder', '/mine/fan_group', '/mine/feedback',
    ];
    if (toastOnly.indexOf(title) >= 0) {
      if (title === '切换门店' || title === '切换店铺') {
        if (this.isLoggedIn) {
          this.showSwitchStore = true;
        } else {
          promptAction.showToast({ message: '请先登录', duration: 2000 });
        }
        return;
      }
      promptAction.showToast({ message: title, duration: 2000 });
      return;
    }
    const path = resolveNativePath(title);
    if (path === '/settings') {
      this.openMineIsland(false);
      return;
    }
    if (path === '/mine/personalized_settings') {
      this.openMineIsland(true);
      return;
    }
    if (path === '/pay/membership') {
      this.openComposeHost(0, 2);
      return;
    }
    if (path === '/mine/about') {
      this.openComposeHost(0, 0);
      return;
    }
    if (path === '/chat' || title === '消息') {
      this.tab = MainTab.Chat;
      this.chatSelectedId = '';
      if (!this.isLoggedIn) {
        this.showLogin = true;
      }
      return;
    }
    if (path.indexOf('/chat/detail') === 0 || title.indexOf('/chat/detail') === 0) {
      const raw = title.indexOf('/chat/detail') === 0 ? title : path;
      const peer = this.peerFromChatDetail(raw);
      this.ensureChatSeed();
      const id = this.ensureChatConversation(peer, peer);
      this.openChatDetail(id);
      this.tab = MainTab.Chat;
      if (!this.isLoggedIn) {
        this.showLogin = true;
      }
      return;
    }
    if (path === '/community') {
      this.tab = MainTab.Community;
      if (!this.isLoggedIn) {
        this.showLogin = true;
      }
      return;
    }
    if (path === '/home' || path === '/main') {
      this.tab = MainTab.Home;
      return;
    }
    if (path === '/mine') {
      this.tab = MainTab.Mine;
      return;
    }
    // Flutter-aligned secondary UI via Compose SecondaryRouteIsland (commonMain hosts).
    this.nativePane = '';
    this.featurePath = '';
    this.showStub = false;
    this.openSecondaryCompose(path.length > 0 ? path : title);
  }


  @Builder
  SplashPane() {
    Column() {
      Text('i车商')
        .fontSize(32)
        .fontWeight(FontWeight.Medium)
        .fontColor(DesignTokens.ink)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  PrivacyPane() {
    Column({ space: 16 }) {
      Text('隐私政策').fontSize(22).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
      Text('请阅读并同意隐私政策后继续使用本应用。')
        .fontSize(16)
        .fontColor(DesignTokens.body)
        .textAlign(TextAlign.Center)
        .padding({ left: 24, right: 24 })
      Button('同意并继续')
        .backgroundColor(DesignTokens.link)
        .onClick(() => {
          this.privacyAccepted = true;
          this.pref?.putSync('privacy_accepted', true);
          this.pref?.flush();
          this.phase = AppPhase.Main;
        })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  AuthGatePane() {
    Column({ space: 16 }) {
      Text('请先登录').fontSize(18).fontColor(DesignTokens.ink)
      Button('去登录')
        .backgroundColor(DesignTokens.link)
        .onClick(() => {
          this.showLogin = true;
        })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  MetricCell(value: string, label: string) {
    Column({ space: 4 }) {
      Text(value).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
      Text(label).fontSize(11).fontColor(DesignTokens.mute)
    }.layoutWeight(1)
  }

  @Builder
  NativeFeaturePane() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => {
          this.nativePane = '';
          this.featurePath = '';
        })
        Column() {
          Text(featurePageFor(this.featurePath).title)
            .fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          if (featurePageFor(this.featurePath).subtitle.length > 0) {
            Text(featurePageFor(this.featurePath).subtitle)
              .fontSize(12).fontColor(DesignTokens.body)
          }
        }.alignItems(HorizontalAlign.Start).margin({ left: 8 })
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(featurePageFor(this.featurePath).rows, (item: FeatureRow) => {
          ListItem() {
            Row() {
              Column() {
                Text(item.title).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(item.detail).fontSize(13).fontColor(DesignTokens.body).margin({ top: 4 })
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              if (item.badge.length > 0) {
                Text(item.badge)
                  .fontSize(11)
                  .fontColor(DesignTokens.link)
                  .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                  .backgroundColor('#1A0070F3')
                  .borderRadius(10)
              }
            }.padding(16).width('100%')
          }
        })
      }
      .layoutWeight(1)
      .width('100%')
      .margin({ left: 16, right: 16, top: 12 })
      .backgroundColor(DesignTokens.canvas)
      .borderRadius(12)

      if (featurePageFor(this.featurePath).primaryAction.length > 0) {
        Button(featurePageFor(this.featurePath).primaryAction)
          .backgroundColor(DesignTokens.link)
          .margin(16)
          .width('90%')
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  ChatPane() {
    // chatTick kept in tree so async send/read patches refresh UI
    Text(`${this.chatTick}`).height(0).opacity(0)
    if (this.chatSelectedId.length > 0) {
      this.ChatDetailPane()
    } else {
      this.ChatListPane()
    }
  }

  @Builder
  ChatListPane() {
    Column() {
      Row() {
        Text('消息').fontSize(32).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink).layoutWeight(1)
        Text(this.chatSearchOpen ? '✕' : '⌕')
          .fontSize(20).fontColor(DesignTokens.link).margin({ right: 16 })
          .onClick(() => {
            this.chatSearchOpen = !this.chatSearchOpen;
            if (!this.chatSearchOpen) {
              this.chatSearchQuery = '';
            }
          })
        Text('✎').fontSize(20).fontColor(DesignTokens.link)
          .onClick(() => this.openDeferred('/friend'))
      }.padding({ left: 16, right: 16, top: 8, bottom: 8 }).width('100%')

      if (this.chatSearchOpen) {
        TextInput({ placeholder: '搜索会话名称或消息', text: this.chatSearchQuery })
          .height(40)
          .margin({ left: 16, right: 16, bottom: 8 })
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(10)
          .onChange((v: string) => { this.chatSearchQuery = v; })
      }

      if (this.visibleChatConversations().length === 0) {
        Column() {
          Blank()
          Text(this.chatSearchQuery.trim().length === 0 ? '还没有消息' : '没有匹配的会话')
            .fontSize(15).fontColor(DesignTokens.body)
          Blank()
        }.width('100%').layoutWeight(1)
      } else {
        List() {
          ForEach(this.visibleChatConversations(), (conv: ChatConv, index: number) => {
            ListItem() {
              Column() {
                Row({ space: 12 }) {
                  Stack({ alignContent: Alignment.BottomEnd }) {
                    Text(conv.title.substring(conv.title.length - 1))
                      .width(52).height(52).textAlign(TextAlign.Center)
                      .fontColor(DesignTokens.link).backgroundColor('#260070F3').borderRadius(26)
                    if (conv.isOnline) {
                      Text('')
                        .width(12).height(12)
                        .backgroundColor(DesignTokens.link)
                        .borderRadius(6)
                        .border({ width: 2, color: Color.White })
                    }
                  }
                  Column() {
                    Text(conv.title).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                    Text(conv.lastMessage).fontSize(13).fontColor(DesignTokens.body).maxLines(1)
                  }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                  Column() {
                    Text(this.chatListTime(conv.lastMessageAtMs)).fontSize(11).fontColor(DesignTokens.body)
                    if (conv.unreadCount > 0) {
                      Text(conv.unreadCount > 99 ? '99+' : `${conv.unreadCount}`)
                        .fontSize(11).fontColor(Color.White)
                        .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                        .backgroundColor('#EE0000').borderRadius(10).margin({ top: 6 })
                    }
                  }
                }
                .padding(16)
                .width('100%')
                .onClick(() => this.openChatDetail(conv.id))
                if (index < this.visibleChatConversations().length - 1) {
                  Divider().strokeWidth(0.5).color(DesignTokens.hairline).margin({ left: 76 })
                }
              }
            }
          }, (conv: ChatConv) => `${conv.id}_${conv.unreadCount}_${conv.lastMessageAtMs}_${this.chatTick}`)
        }
        .width('100%')
        .layoutWeight(1)
        .margin({ left: 16, right: 16 })
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  ChatDetailPane() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => {
          this.chatSelectedId = '';
          this.chatDraft = '';
          this.chatShowEmoji = false;
          this.chatShowMore = false;
          this.chatVoiceMode = false;
        })
        Text(this.chatDetailTitle())
          .fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      List({ space: 12 }) {
        // Chronological: reverse newest-first store (Flutter reverse:true ListView)
        ForEach(this.messagesFor(this.chatSelectedId).slice().reverse(), (msg: ChatMsg) => {
          ListItem() {
            if (msg.type === 'time') {
              Text(msg.body).fontSize(12).fontColor(DesignTokens.mute)
                .width('100%').textAlign(TextAlign.Center)
            } else {
              Row() {
                if (msg.isSelf) {
                  Blank().layoutWeight(1)
                }
                Column() {
                  Text(msg.type === 'image' ? '[图片]' : (msg.type === 'voice' ? '[语音]' : msg.body))
                    .fontSize(15)
                    .fontColor(msg.isSelf ? Color.White : DesignTokens.ink)
                    .padding({ left: 12, right: 12, top: 8, bottom: 8 })
                    .backgroundColor(msg.isSelf ? DesignTokens.link : DesignTokens.canvas)
                    .borderRadius(12)
                  Text(this.chatStatusLabel(msg).length > 0
                    ? this.chatStatusLabel(msg) : this.chatHm(msg.createdAtMs))
                    .fontSize(11).fontColor(DesignTokens.mute)
                    .margin({ top: 2 })
                }.alignItems(msg.isSelf ? HorizontalAlign.End : HorizontalAlign.Start)
                if (!msg.isSelf) {
                  Blank().layoutWeight(1)
                }
              }.width('100%')
            }
          }
        }, (msg: ChatMsg) => `${msg.id}_${msg.sendStatus}_${msg.readStatus}_${this.chatTick}`)
      }.layoutWeight(1).padding(16).width('100%')

      Column() {
        Row({ space: 8 }) {
          Text(this.chatVoiceMode ? '⌨' : '🎤')
            .fontSize(20).fontColor(DesignTokens.body).width(36).height(36).textAlign(TextAlign.Center)
            .onClick(() => {
              this.chatVoiceMode = !this.chatVoiceMode;
              this.chatShowEmoji = false;
              this.chatShowMore = false;
            })
          if (this.chatVoiceMode) {
            Text('按住 说话')
              .fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              .layoutWeight(1).height(40).textAlign(TextAlign.Center)
              .backgroundColor(DesignTokens.canvas).borderRadius(8)
              .border({ width: 0.5, color: DesignTokens.hairline })
              .onClick(() => this.sendChat(this.chatSelectedId, 'voice', '[语音]', '[语音]'))
          } else {
            TextInput({ placeholder: '输入消息…', text: this.chatDraft })
              .layoutWeight(1)
              .height(40)
              .onChange((v: string) => { this.chatDraft = v; })
          }
          Text('☺')
            .fontSize(22)
            .fontColor(this.chatShowEmoji ? DesignTokens.link : DesignTokens.body)
            .width(36).height(36).textAlign(TextAlign.Center)
            .onClick(() => {
              this.chatShowEmoji = !this.chatShowEmoji;
              this.chatShowMore = false;
              this.chatVoiceMode = false;
            })
          if (this.chatDraft.trim().length === 0) {
            Text('＋')
              .fontSize(28)
              .fontColor(this.chatShowMore ? DesignTokens.link : DesignTokens.body)
              .width(36).height(36).textAlign(TextAlign.Center)
              .onClick(() => {
                this.chatShowMore = !this.chatShowMore;
                this.chatShowEmoji = false;
              })
          } else {
            Text('发送')
              .fontSize(14).fontWeight(FontWeight.Medium).fontColor(Color.White)
              .padding({ left: 12, right: 12, top: 8, bottom: 8 })
              .backgroundColor(DesignTokens.link).borderRadius(18)
              .onClick(() => {
                const t = this.chatDraft.trim();
                if (t.length === 0) {
                  return;
                }
                this.sendChat(this.chatSelectedId, 'text', t, t);
                this.chatDraft = '';
                this.chatShowEmoji = false;
                this.chatShowMore = false;
                this.chatVoiceMode = false;
              })
          }
        }.padding(12).width('100%')

        if (this.chatShowEmoji) {
          Flex({ wrap: FlexWrap.Wrap }) {
            ForEach(['😀', '😂', '🥰', '😎', '🤔', '👍', '🙏', '🎉', '❤️', '🔥', '👋', '😭', '🤣', '😊', '🥳', '💪'],
              (e: string) => {
                Text(e).fontSize(28).width('12.5%').textAlign(TextAlign.Center).padding(4)
                  .onClick(() => { this.chatDraft = this.chatDraft + e; })
              })
          }.width('100%').height(160).padding(12).backgroundColor(DesignTokens.canvas)
        }

        if (this.chatShowMore) {
          Row({ space: 24 }) {
            ForEach([
              ['照片', 'image', 'https://picsum.photos/seed/chat/600', '[图片]'],
              ['拍摄', 'image', 'https://picsum.photos/seed/chat_cam/600', '[图片]'],
              ['文件', 'custom', '文件', '[自定义消息]'],
              ['位置', 'custom', '位置', '[自定义消息]'],
            ], (item: string[]) => {
              Column({ space: 8 }) {
                Text(item[0].substring(0, 1))
                  .width(52).height(52).textAlign(TextAlign.Center)
                  .fontColor(DesignTokens.link).backgroundColor(DesignTokens.canvasSoft2).borderRadius(12)
                Text(item[0]).fontSize(12).fontColor(DesignTokens.body)
              }.onClick(() => {
                this.sendChat(this.chatSelectedId, item[1], item[2], item[3]);
                this.chatShowMore = false;
              })
            })
          }
          .justifyContent(FlexAlign.Center)
          .width('100%')
          .padding({ top: 20, bottom: 20 })
          .backgroundColor(DesignTokens.canvas)
        }
      }.width('100%').backgroundColor(DesignTokens.canvasSoft2)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  CommunityPane() {
    Text(`${this.communityTick}`).height(0).opacity(0)
    Column() {
      Row() {
        Text('社区').fontSize(32).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink).layoutWeight(1)
        Text('+')
          .fontSize(22)
          .fontWeight(FontWeight.Bold)
          .fontColor(Color.White)
          .width(36)
          .height(36)
          .textAlign(TextAlign.Center)
          .backgroundColor(DesignTokens.link)
          .borderRadius(18)
          .onClick(() => this.openDeferred('/community/publish'))
      }.padding({ left: 16, right: 16, top: 8, bottom: 8 }).width('100%')

      Row({ space: 8 }) {
        Text('⌕').fontSize(16).fontColor(DesignTokens.body)
        Text('搜索动态、话题、用户').fontSize(14).fontColor(DesignTokens.body)
      }
      .width('100%')
      .height(40)
      .padding({ left: 12, right: 12 })
      .margin({ left: 16, right: 16 })
      .backgroundColor(DesignTokens.canvas)
      .borderRadius(10)
      .onClick(() => this.openDeferred('/community/search'))

      Row() {
        ForEach(['最新', '热门', '关注'], (item: string) => {
          Column({ space: 6 }) {
            Text(item)
              .fontSize(16)
              .fontWeight(this.communityFilter === item ? FontWeight.Medium : FontWeight.Regular)
              .fontColor(this.communityFilter === item ? DesignTokens.ink : DesignTokens.body)
            Row()
              .width(18)
              .height(3)
              .borderRadius(2)
              .backgroundColor(this.communityFilter === item ? DesignTokens.link : Color.Transparent)
          }
          .layoutWeight(1)
          .onClick(() => { this.communityFilter = item; })
        })
      }.padding({ top: 12, bottom: 12 }).width('100%')

      if (this.communityFeed().length === 0) {
        Column() {
          Blank()
          Text(this.communityFilter === '关注' ? '还没有关注的人，去最新里看看吧' : '暂无动态')
            .fontSize(15).fontColor(DesignTokens.body)
          Blank()
        }.width('100%').layoutWeight(1)
      } else {
        List({ space: 12 }) {
          ForEach(this.communityFeed(), (post: CommunityFeedPost) => {
            ListItem() {
              Column({ space: 8 }) {
                Row() {
                  Text(post.nickname.substring(post.nickname.length - 1))
                    .width(44).height(44).textAlign(TextAlign.Center).borderRadius(22)
                    .backgroundColor(DesignTokens.hairline).fontColor(DesignTokens.body)
                  Column() {
                    Text(post.nickname).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                    Text(this.communityMeta(post)).fontSize(14).fontColor(DesignTokens.body)
                  }.alignItems(HorizontalAlign.Start).margin({ left: 12 }).layoutWeight(1)
                  Text('⋯').fontColor(DesignTokens.body)
                }.width('100%')
                Text(post.content).fontSize(16).fontColor(DesignTokens.ink)
                if (post.videoCoverUrl.length > 0) {
                  Image(post.videoCoverUrl)
                    .width('100%')
                    .aspectRatio(16 / 9)
                    .borderRadius(4)
                    .objectFit(ImageFit.Cover)
                    .onClick(() => this.openDeferred('/community/image_preview'))
                } else if (post.images.length > 0) {
                  Flex({ wrap: FlexWrap.Wrap }) {
                    ForEach(post.images.slice(0, 9), (url: string, i: number) => {
                      Image(url)
                        .width('32%')
                        .aspectRatio(1)
                        .borderRadius(4)
                        .objectFit(ImageFit.Cover)
                        .margin({ right: '1%', bottom: 4 })
                        .onClick(() => this.openDeferred('/community/image_preview'))
                    }, (url: string) => url)
                  }.width('100%')
                }
                Row({ space: 24 }) {
                  Text(post.isLiked ? `♥ 已赞 ${post.likeCount}` : `♡ 赞 ${post.likeCount}`)
                    .fontSize(14)
                    .fontColor(post.isLiked ? '#EE0000' : DesignTokens.body)
                    .onClick(() => this.toggleCommunityLike(post.id))
                  Text(`💬 ${post.commentCount}`).fontSize(14).fontColor(DesignTokens.body)
                    .onClick(() => this.openDeferred('/community/comment'))
                  Text('↗ 分享').fontSize(14).fontColor(DesignTokens.body)
                }.width('100%')
                if (post.previewComments.length > 0) {
                  Column({ space: 4 }) {
                    ForEach(post.previewComments, (c: string[]) => {
                      if (c[2] !== undefined && c[2].length > 0) {
                        Text(`${c[0]} 回复 ${c[2]}：${c[1]}`)
                          .fontSize(13).fontColor(DesignTokens.body)
                      } else {
                        Text(`${c[0]}：${c[1]}`)
                          .fontSize(13).fontColor(DesignTokens.body)
                      }
                    }, (c: string[], idx: number) => `${post.id}_pc_${idx}`)
                  }
                  .width('100%')
                  .padding({ left: 10, right: 10, top: 8, bottom: 8 })
                  .backgroundColor(DesignTokens.canvasSoft2)
                  .borderRadius(6)
                  .alignItems(HorizontalAlign.Start)
                }
              }
              .padding({ left: 16, top: 14, right: 12, bottom: 14 }).width('100%').alignItems(HorizontalAlign.Start)
              .backgroundColor(DesignTokens.canvas).borderRadius(8)
              .border({ width: 0.5, color: DesignTokens.hairline })
            }
          }, (post: CommunityFeedPost) =>
            `${post.id}_${post.isLiked}_${post.likeCount}_${this.communityFilter}_${this.communityTick}`)
        }.layoutWeight(1).padding({ left: 16, right: 16 })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  MinePane() {
    Scroll() {
      Column({ space: 16 }) {
        Row() {
          Text('我的').fontSize(32).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink).layoutWeight(1)
          Text('ⓘ').fontSize(18).fontColor(DesignTokens.link).margin({ right: 8 })
            .onClick(() => this.openMineIsland(true))
          Text('📅').fontSize(16).margin({ right: 8 })
            .onClick(() => this.openDeferred('签到日历'))
          Text('⚙').fontSize(18).fontColor(DesignTokens.link).margin({ right: 8 })
            .onClick(() => this.openMineIsland(false))
          Text(this.isLoggedIn ? '👤' : '＋')
            .fontSize(16).fontColor(DesignTokens.link)
            .onClick(() => {
              if (this.isLoggedIn) {
                this.openDeferred('个人资料');
              } else {
                this.showLogin = true;
              }
            })
        }.width('100%')

        Row({ space: 16 }) {
          Column() {
            Text('头')
              .width(64).height(64)
              .textAlign(TextAlign.Center)
              .fontSize(22).fontColor(DesignTokens.link)
              .backgroundColor('#1A0070F3')
              .borderRadius(32)
          }
          .onClick(() => {
            if (this.isLoggedIn) {
              this.openDeferred('个人资料');
            } else {
              this.showLogin = true;
            }
          })

          Column({ space: 8 }) {
            Row({ space: 8 }) {
              Text(this.isLoggedIn ? (this.loginDisplayName || 'qa_user') : '访客')
                .fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text(this.isLoggedIn ? '销售顾问' : '未登录')
                .fontSize(11).fontWeight(FontWeight.Medium).fontColor(Color.White)
                .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                .backgroundColor(DesignTokens.link)
                .borderRadius(6)
            }
            Row({ space: 4 }) {
              Text(this.isLoggedIn ? this.mineStoreName() : '登录后查看门店信息')
                .fontSize(12).fontColor(DesignTokens.body).layoutWeight(1)
              Text('▾').fontSize(10).fontColor(DesignTokens.body)
            }
            .width('100%')
            .onClick(() => {
              if (this.isLoggedIn) {
                this.showSwitchStore = true;
              } else {
                this.showLogin = true;
              }
            })
            Row({ space: 16 }) {
              Text('电子名片')
                .fontSize(12).fontColor(DesignTokens.link)
                .padding({ left: 10, right: 10, top: 6, bottom: 6 })
                .backgroundColor('#140070F3')
                .borderRadius(20)
                .onClick(() => this.openDeferred('电子名片'))
              Text(this.isLoggedIn ? '138****5172' : '— — —')
                .fontSize(12).fontColor(DesignTokens.body)
            }
            if (!this.isLoggedIn) {
              Button('登录').backgroundColor(DesignTokens.link).onClick(() => {
                this.showLogin = true;
              })
            }
          }.alignItems(HorizontalAlign.Start).layoutWeight(1)
        }
        .width('100%')
        .padding(16)
        .alignItems(VerticalAlign.Top)
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)

        Row() {
          this.MetricCell('0', '加入天数')
          this.MetricCell('0', '员工数')
          this.MetricCell('0', '店铺天数')
          this.MetricCell('0', '累计客户')
        }
        .width('100%')
        .padding(12)
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)

        Text('常用服务').fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).width('100%')
        Row() {
          ForEach(['商城', '我的钱包', '我的订单'], (label: string) => {
            Column({ space: 6 }) {
              Text(label.substring(0, 1))
                .width(44).height(44).textAlign(TextAlign.Center)
                .fontColor(DesignTokens.link).backgroundColor('#1A0070F3').borderRadius(12)
              Text(label).fontSize(11).fontColor(DesignTokens.ink)
            }.layoutWeight(1).padding({ top: 8, bottom: 8 })
            .onClick(() => this.openDeferred(label))
          })
        }
        .width('100%')
        .padding(8)
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)

        Row() {
          Text('个人功能').fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).layoutWeight(1)
          Text('长按拖动顺序').fontSize(12).fontColor(DesignTokens.body)
        }.width('100%')
        Flex({ wrap: FlexWrap.Wrap }) {
          ForEach(this.mineFunctions, (item: MineFunctionCard) => {
            Column({ space: 4 }) {
              Text(item.title.substring(0, 1))
                .width(44).height(44).textAlign(TextAlign.Center)
                .fontColor(DesignTokens.link).backgroundColor('#140070F3').borderRadius(12)
              Blank().height(8)
              Text(item.title).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text(item.subtitle).fontSize(11).fontColor(DesignTokens.body).maxLines(2)
              if (item.value.length > 0) {
                Text(item.value).fontSize(15).fontWeight(FontWeight.Bold).fontColor(DesignTokens.link)
              }
            }
            .width('48%')
            .padding(16)
            .margin({ bottom: 12 })
            .alignItems(HorizontalAlign.Start)
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(12)
            .border({ width: 0.5, color: DesignTokens.hairline })
            .onClick(() => this.openDeferred(item.title))
          }, (item: MineFunctionCard) => item.title)
        }.width('100%').justifyContent(FlexAlign.SpaceBetween)

        Column() {
          ForEach(this.mineMenu, (item: MineMenuRow) => {
            Row() {
              Text(item.label).fontSize(15).fontColor(DesignTokens.ink).layoutWeight(1)
              if (item.badge) {
                Text('●').fontSize(8).fontColor(Color.Red).margin({ right: 8 })
              }
              Text('›').fontColor(DesignTokens.mute)
            }
            .width('100%')
            .padding(16)
            .onClick(() => {
              if (item.label === '设置') {
                this.openMineIsland(false);
              } else {
                this.openDeferred(item.label);
              }
            })
          }, (item: MineMenuRow) => item.label)
        }
        .width('100%')
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)
      }
      .padding(16)
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  TabContent() {
    if (this.tab === MainTab.Home) {
      NativeHomeContent({
        onDeferred: (t: string): void => { this.openDeferred(t); },
        displayName: this.loginDisplayName,
        isLoggedIn: this.isLoggedIn,
      })
    } else if (this.tab === MainTab.Chat) {
      if (this.isLoggedIn) {
        this.ChatPane()
      } else {
        this.AuthGatePane()
      }
    } else if (this.tab === MainTab.Community) {
      if (this.isLoggedIn) {
        this.CommunityPane()
      } else {
        this.AuthGatePane()
      }
    } else {
      this.MinePane()
    }
  }

  private tabIcon(id: MainTab): Resource {
    const active = this.tab === id;
    if (id === MainTab.Home) {
      return active ? $r('app.media.main_tab_home_selected') : $r('app.media.main_tab_home_unselected');
    }
    if (id === MainTab.Chat) {
      return active ? $r('app.media.main_tab_chat_selected') : $r('app.media.main_tab_chat_unselected');
    }
    if (id === MainTab.Community) {
      return active ? $r('app.media.main_tab_community_selected') : $r('app.media.main_tab_community_unselected');
    }
    return active ? $r('app.media.main_tab_me_selected') : $r('app.media.main_tab_me_unselected');
  }

  @Builder
  TabBtn(label: string, id: MainTab) {
    Column({ space: 2 }) {
      Image(this.tabIcon(id))
        .width(22)
        .height(22)
      Text(label)
        .fontSize(10)
        .fontWeight(this.tab === id ? FontWeight.Medium : FontWeight.Regular)
        .fontColor(this.tab === id ? DesignTokens.link : DesignTokens.body)
    }
    .layoutWeight(1)
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .onClick(() => {
      // Flutter MainPage: soft-auth keeps current tab; only present login.
      if ((id === MainTab.Chat || id === MainTab.Community) && !this.isLoggedIn) {
        this.showLogin = true;
        return;
      }
      this.tab = id;
    })
  }

  build() {
    Stack() {
      if (this.phase === AppPhase.Splash) {
        this.SplashPane()
      } else if (this.phase === AppPhase.Privacy) {
        this.PrivacyPane()
      } else {
        Column() {
          Column() {
            this.TabContent()
          }
          .layoutWeight(1)
          .width('100%')

          Row() {
            this.TabBtn('首页', MainTab.Home)
            this.TabBtn('聊天', MainTab.Chat)
            this.TabBtn('社区', MainTab.Community)
            this.TabBtn('我的', MainTab.Mine)
          }
          .width('100%')
          .height(49)
          .backgroundColor('#F2FFFFFF')
          .border({ width: { top: 1 }, color: DesignTokens.hairline })
        }
        .width('100%')
        .height('100%')
      }

      if (this.showIsland && this.islandController) {
        Column() {
          Compose({
            controller: this.islandController,
            libraryName: 'entry',
            onBackPressed: () => {
              if (this.islandController) {
                return this.islandController.onBackPress();
              }
              this.showIsland = false;
              return false;
            }
          })
          Button('关闭')
            .margin(12)
            .onClick(() => {
              this.showIsland = false;
              this.islandController = undefined;
            })
        }
        .width('100%')
        .height('100%')
        .backgroundColor(DesignTokens.canvas)
      }


      if (this.nativePane === 'search') {
        NativeSearchPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'all_services') {
        NativeAllServicesPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpen: (label: string): void => {
            this.nativePane = '';
            this.openDeferred(label);
          },
        })
      }

      if (this.nativePane === 'learning_report') {
        NativeLearningReportPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'purchase_calculator') {
        NativePurchaseCalculatorPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'friend') {
        NativeFriendPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpenChat: (): void => {
            this.nativePane = '';
            this.tab = MainTab.Chat;
          },
        })
      }

      if (this.nativePane === 'community_search') {
        NativeCommunitySearchPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'community_publish') {
        NativeCommunityPublishPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'community_comment') {
        NativeCommunityCommentPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'community_image_preview') {
        NativeCommunityImagePreviewPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'mall') {
        NativeMallPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpen: (p: string): void => {
            this.nativePane = '';
            this.openDeferred(p);
          },
        })
      }

      if (this.nativePane === 'wallet') {
        NativeWalletPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'short_video') {
        NativeShortVideoPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpen: (p: string): void => {
            this.nativePane = '';
            this.openDeferred(p);
          },
        })
      }

      if (this.nativePane === 'live') {
        NativeLivePane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'ai_stream') {
        NativeAiStreamPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'scan') {
        NativeScanPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'strategy') {
        NativeStrategyPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'check_in_mall') {
        NativeCheckInMallPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'music') {
        NativeMusicPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'classroom') {
        NativeClassroomPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpen: (p: string): void => {
            this.nativePane = '';
            this.openDeferred(p);
          },
        })
      }

      if (this.nativePane === 'used_car') {
        NativeUsedCarPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpen: (p: string): void => {
            this.nativePane = '';
            this.openDeferred(p);
          },
        })
      }

      if (this.nativePane === 'new_car_follow') {
        NativeNewCarFollowPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpen: (p: string): void => {
            this.nativePane = '';
            this.openDeferred(p);
          },
        })
      }

      if (this.nativePane === 'club') {
        NativeClubPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'life_service') {
        NativeLifeServicePane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'todo') {
        NativeTodoListPane({
          titleText: this.todoTitle(),
          rows: this.todoRows(),
          action: this.todoAction(),
          onClose: (): void => { this.nativePane = ''; },
        })
      }

      if (this.nativePane === 'web') {
        NativeWebPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'after_sales') {
        NativeAfterSalesPane({
          onClose: (): void => { this.nativePane = ''; },
          onOpen: (p: string): void => {
            this.nativePane = '';
            this.openDeferred(p);
          },
        })
      }

      if (this.nativePane === 'ledger') {
        NativeLedgerPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'sms_template') {
        NativeSmsTemplatePane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'store_qr') {
        NativeStoreQrPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'qa') {
        NativeQaPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'poster') {
        NativePosterPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'business') {
        NativeBusinessPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'reminders') {
        NativeRemindersPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'feedback') {
        NativeFeedbackPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'profile') {
        NativeProfilePane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'addresses') {
        NativeAddressesPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'dubbing') {
        NativeDubbingFeedPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'hot_rank') {
        NativeHotRankPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'data_analytics') {
        NativeDataAnalyticsPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'business_card') {
        NativeBusinessCardPane({ onClose: (): void => { this.nativePane = ''; } })
      }

      if (this.nativePane === 'feature' && this.featurePath.length > 0) {
        this.NativeFeaturePane()
      }

      if (this.showLogin) {
        NativeLoginPane({
          onSuccess: (session: AuthSession) => {
            this.applySession(session);
          },
          onCancel: () => {
            this.showLogin = false;
            this.tab = MainTab.Home;
          },
        })
      }

      if (this.showSwitchStore) {
        Column() {
          Column()
            .width('100%')
            .height('100%')
            .backgroundColor('#8C000000')
            .onClick(() => { this.showSwitchStore = false; })
          Column() {
            this.SwitchStoreDialog()
          }
          .width('100%')
          .height('100%')
          .justifyContent(FlexAlign.Center)
          .alignItems(HorizontalAlign.Center)
        }
        .width('100%')
        .height('100%')
      }

      if (this.errorMessage.length > 0) {
        Text(this.errorMessage)
          .fontSize(12)
          .fontColor('#B00020')
          .padding(8)
      }
    }
    .width('100%')
    .height('100%')
  }

  onPageHide() {
    if (libentry !== undefined && libentry.AudioOnPageHide !== undefined) {
      libentry.AudioOnPageHide();
    }
    if (this.islandController) {
      this.islandController.onPageHide();
    }
  }

  onPageShow() {
    if (this.islandController) {
      this.islandController.onPageShow();
    }
  }

  onBackPressed(): boolean {
    if (this.showSwitchStore) {
      this.showSwitchStore = false;
      return true;
    }
    if (this.showIsland && this.islandController) {
      return this.islandController.onBackPress();
    }
    if (this.showLogin) {
      this.showLogin = false;
      return true;
    }
    return false;
  }
}
