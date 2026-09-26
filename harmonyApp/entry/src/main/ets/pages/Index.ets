import { ArkUIViewController, Compose } from '@cpf-kmp-cmp/compose';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { deviceInfo } from '@kit.BasicServicesKit';
import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import libentry from 'libentry.so';
import { registerVideoInteropBuilders } from '../ComposeVideoInterops';
import { registerWebInteropBuilders } from '../ComposeWebInterops';

import { DesignTokens, AppPhase, MainTab } from './NativeShellShared';
import { NativeHomeContent } from './NativeHomeContent';
import { HuaweiQuickLoginPane } from '../auth/HuaweiQuickLoginPane';
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
  @State chatPeer: string = '';
  @State chatDraft: string = '';
  @State chatMessages: string[] = [];
  @State chatSearchOpen: boolean = false;
  @State chatSearchQuery: string = '';
  @State chatVoiceMode: boolean = false;
  @State chatShowEmoji: boolean = false;
  @State chatShowMore: boolean = false;
  @State communityLiked: boolean = false;
  @State nativePane: string = '';
  @State featurePath: string = '';
  private islandController: ArkUIViewController | undefined = undefined;
  private pref: preferences.Preferences | undefined = undefined;

  private readonly chatPeers: string[] = ['Mock好友1', 'Mock好友2', 'Mock好友3'];
  private readonly chatSnippets: string[] = ['晚上一起吃饭吗？', '你好', '你好'];
  private readonly chatTimes: string[] = ['22:50', '22:45', '22:40'];
  private readonly communityAuthors: string[] = ['张三', '李四', '王五'];
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

  private openDeferred(title: string): void {
    const path = resolveNativePath(title);
    if (path === '/settings') {
      this.openMineIsland(false);
      return;
    }
    if (path === '/mine/personalized_settings') {
      this.openMineIsland(true);
      return;
    }
    if (path === '/chat') {
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
    if (path === '/home/search') {
      this.nativePane = 'search';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/all_services') {
      this.nativePane = 'all_services';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/learning_report' || title === '学习报告') {
      this.nativePane = 'learning_report';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/mine/purchase_calculator' || title === '购车计算器') {
      this.nativePane = 'purchase_calculator';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/friend') {
      this.nativePane = 'friend';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/community/search') {
      this.nativePane = 'community_search';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/community/publish') {
      this.nativePane = 'community_publish';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/community/comment') {
      this.nativePane = 'community_comment';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/community/image_preview') {
      this.nativePane = 'community_image_preview';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/mall') {
      this.nativePane = 'mall';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/wallet') {
      this.nativePane = 'wallet';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/video/short') {
      this.nativePane = 'short_video';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/live' || path === '/home/live_commerce' || title === '直播' || title === '直播带货') {
      this.nativePane = 'live';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/ai/stream') {
      this.nativePane = 'ai_stream';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/scan') {
      this.nativePane = 'scan';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/strategy') {
      this.nativePane = 'strategy';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/check_in_mall') {
      this.nativePane = 'check_in_mall';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/music/list') {
      this.nativePane = 'music';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/classroom/my_class') {
      this.nativePane = 'classroom';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/used_car') {
      this.nativePane = 'used_car';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/new_car_follow' || title === '新车成交' || title === '新车跟进') {
      this.nativePane = 'new_car_follow';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/club' || title === 'Club') {
      this.nativePane = 'club';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path === '/home/life_service' || title === '生活服务') {
      this.nativePane = 'life_service';
      this.featurePath = '';
      this.showStub = false;
      return;
    }
    if (path.indexOf('/home/todo/') === 0) {
      this.nativePane = 'todo';
      this.featurePath = path;
      this.showStub = false;
      return;
    }
    this.featurePath = path;
    this.nativePane = 'feature';
    this.showStub = false;
    this.showIsland = false;
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
    if (this.chatPeer.length > 0) {
      Column() {
        Row() {
          Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => {
            this.chatPeer = '';
            this.chatMessages = [];
            this.chatDraft = '';
          })
          Text(this.chatPeer).fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

        List({ space: 12 }) {
          ForEach(this.chatMessages, (body: string, index: number) => {
            ListItem() {
              Row() {
                if (index % 2 === 1) {
                  Blank().layoutWeight(1)
                }
                Text(body)
                  .fontSize(15)
                  .fontColor(index % 2 === 1 ? Color.White : DesignTokens.ink)
                  .padding({ left: 12, right: 12, top: 8, bottom: 8 })
                  .backgroundColor(index % 2 === 1 ? DesignTokens.link : DesignTokens.canvas)
                  .borderRadius(12)
                if (index % 2 === 0) {
                  Blank().layoutWeight(1)
                }
              }.width('100%')
            }
          })
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
                  this.chatMessages = this.chatMessages.concat([t]);
                  this.chatDraft = '';
                })
            }
          }.padding(12).width('100%')

          if (this.chatShowEmoji) {
            Flex({ wrap: FlexWrap.Wrap }) {
              ForEach(['😀', '😁', '😂', '🤣', '😊', '😍', '🥰', '😘', '👍', '🙏', '🔥', '🎉', '🚗', '🏠', '✅', '❤️'],
                (e: string) => {
                  Text(e).fontSize(28).width('12.5%').textAlign(TextAlign.Center).padding(4)
                    .onClick(() => { this.chatDraft = this.chatDraft + e; })
                })
            }.width('100%').height(160).padding(12).backgroundColor(DesignTokens.canvas)
          }

          if (this.chatShowMore) {
            Row({ space: 24 }) {
              ForEach([['照片', '[图片]'], ['拍摄', '[拍摄]'], ['文件', '[文件]'], ['位置', '[位置]']],
                (item: string[]) => {
                  Column({ space: 8 }) {
                    Text(item[0].substring(0, 1))
                      .width(52).height(52).textAlign(TextAlign.Center)
                      .fontColor(DesignTokens.link).backgroundColor(DesignTokens.canvasSoft2).borderRadius(12)
                    Text(item[0]).fontSize(12).fontColor(DesignTokens.body)
                  }.onClick(() => {
                    this.chatMessages = this.chatMessages.concat([item[1]]);
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
    } else {
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
        Column() {
          ForEach(this.chatPeers, (name: string, index: number) => {
            Row({ space: 12 }) {
              Text(name.substring(name.length - 1))
                .width(48)
                .height(48)
                .textAlign(TextAlign.Center)
                .fontColor(DesignTokens.link)
                .backgroundColor('#260070F3')
                .borderRadius(24)
              Column() {
                Text(name).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(this.chatSnippets[index]).fontSize(13).fontColor(DesignTokens.body).maxLines(1)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Column() {
                Text(this.chatTimes[index]).fontSize(11).fontColor(DesignTokens.body)
                if (index === 0) {
                  Text('2')
                    .fontSize(11)
                    .fontColor(Color.White)
                    .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                    .backgroundColor('#EE0000')
                    .borderRadius(10)
                    .margin({ top: 6 })
                }
              }
            }
            .padding(16)
            .width('100%')
            .onClick(() => {
              this.chatPeer = name;
              this.chatMessages = ['你好，在吗？', this.chatSnippets[index]];
              this.chatDraft = '';
            })
            if (index < this.chatPeers.length - 1) {
              Divider().strokeWidth(0.5).color(DesignTokens.hairline)
            }
          })
        }
        .width('100%')
        .margin({ left: 16, right: 16 })
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)
      }
      .width('100%')
      .height('100%')
      .backgroundColor(DesignTokens.canvasSoft2)
    }
  }

  @Builder
  CommunityPane() {
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

      List({ space: 12 }) {
        if (this.communityFilter === '热门') {
          ListItem() {
            Column({ space: 8 }) {
              Row() {
                Text('热').width(44).height(44).textAlign(TextAlign.Center).borderRadius(22)
                  .backgroundColor(DesignTokens.hairline).fontColor(DesignTokens.body)
                Column() {
                  Text('热门作者').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text('2小时前 · 热门').fontSize(14).fontColor(DesignTokens.body)
                }.alignItems(HorizontalAlign.Start).margin({ left: 12 }).layoutWeight(1)
              }.width('100%')
              Text('本周试驾排行榜出炉！#新车').fontSize(16).fontColor(DesignTokens.ink)
              Image($r('app.media.community_post_video')).width('62%').aspectRatio(858 / 570).borderRadius(6).objectFit(ImageFit.Cover)
              Row({ space: 24 }) {
                Text(this.communityLiked ? '♥ 已赞 1.2k' : '♡ 赞 1.2k').fontSize(14)
                  .fontColor(this.communityLiked ? '#EE0000' : DesignTokens.body)
                  .onClick(() => { this.communityLiked = !this.communityLiked; })
                Text('💬 86').fontSize(14).fontColor(DesignTokens.body)
                  .onClick(() => this.openDeferred('/community/comment'))
              }.width('100%')
            }
            .padding({ left: 16, top: 14, right: 12, bottom: 14 }).width('100%').alignItems(HorizontalAlign.Start)
            .backgroundColor(DesignTokens.canvas).borderRadius(8)
            .border({ width: 0.5, color: DesignTokens.hairline })
          }
        } else if (this.communityFilter === '关注') {
          ListItem() {
            Column({ space: 8 }) {
              Row() {
                Text('张').width(44).height(44).textAlign(TextAlign.Center).borderRadius(22)
                  .backgroundColor(DesignTokens.hairline).fontColor(DesignTokens.body)
                Column() {
                  Text('张三').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text('刚刚 · 关注').fontSize(14).fontColor(DesignTokens.body)
                }.alignItems(HorizontalAlign.Start).margin({ left: 12 }).layoutWeight(1)
              }.width('100%')
              Text('刚发了保养心得，求交流。').fontSize(16).fontColor(DesignTokens.ink)
              Image($r('app.media.community_post_a')).width('62%').aspectRatio(858 / 570).borderRadius(6).objectFit(ImageFit.Cover)
            }
            .padding({ left: 16, top: 14, right: 12, bottom: 14 }).width('100%').alignItems(HorizontalAlign.Start)
            .backgroundColor(DesignTokens.canvas).borderRadius(8)
            .border({ width: 0.5, color: DesignTokens.hairline })
          }
        } else {
        ListItem() {
          Column({ space: 8 }) {
            Row() {
              Text('张').width(44).height(44).textAlign(TextAlign.Center).borderRadius(22)
                .backgroundColor(DesignTokens.hairline).fontColor(DesignTokens.body)
              Column() {
                Text('张三').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text('7分钟前 · 来自 iPhone').fontSize(14).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).margin({ left: 12 }).layoutWeight(1)
              Text('⋯').fontColor(DesignTokens.body)
            }.width('100%')
            Text('今天去了 @张三 推荐的咖啡店，环境不错。\n#Flutter开发\n欢迎访问：https://flutter.dev')
              .fontSize(16).fontColor(DesignTokens.ink)
            Image($r('app.media.community_post_video'))
              .width('62%')
              .aspectRatio(858 / 570)
              .borderRadius(6)
              .objectFit(ImageFit.Cover)
              .onClick(() => this.openDeferred('/community/image_preview'))
            Row({ space: 24 }) {
              Text(this.communityLiked ? '♥ 已赞 158' : '♡ 赞 158').fontSize(14)
                .fontColor(this.communityLiked ? '#EE0000' : DesignTokens.body)
                .onClick(() => { this.communityLiked = !this.communityLiked; })
              Text('💬 6').fontSize(14).fontColor(DesignTokens.body)
                .onClick(() => this.openDeferred('/community/comment'))
              Text('↗ 分享').fontSize(14).fontColor(DesignTokens.body)
            }.width('100%')
            Column({ space: 4 }) {
              Text('李四：说得对！').fontSize(14).fontWeight(FontWeight.Medium).fontColor('#576B95')
              Text('赵六 回复 张三：同感 +1').fontSize(14).fontWeight(FontWeight.Medium).fontColor('#576B95')
            }.width('100%').padding(10).backgroundColor('#80F5F5F5').borderRadius(4)
          }
          .padding({ left: 16, top: 14, right: 12, bottom: 14 }).width('100%').alignItems(HorizontalAlign.Start)
          .backgroundColor(DesignTokens.canvas).borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
        }
        ListItem() {
          Column({ space: 8 }) {
            Row() {
              Text('李').width(44).height(44).textAlign(TextAlign.Center).borderRadius(22)
                .backgroundColor(DesignTokens.hairline).fontColor(DesignTokens.body)
              Column() {
                Text('李四').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text('42分钟前 · 来自 Android').fontSize(14).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).margin({ left: 12 }).layoutWeight(1)
              Text('⋯').fontColor(DesignTokens.body)
            }.width('100%')
            Text('周末 hiking，天气太好了！#户外').fontSize(16).fontColor(DesignTokens.ink)
            Row({ space: 4 }) {
              Image($r('app.media.community_post_a')).layoutWeight(1).aspectRatio(678 / 518).borderRadius(6).objectFit(ImageFit.Cover)
              Image($r('app.media.community_post_b')).layoutWeight(1).aspectRatio(678 / 518).borderRadius(6).objectFit(ImageFit.Cover)
            }.width('100%')
          }
          .padding({ left: 16, top: 14, right: 12, bottom: 14 }).width('100%').alignItems(HorizontalAlign.Start)
          .backgroundColor(DesignTokens.canvas).borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
        }
        }
      }.layoutWeight(1).padding({ left: 16, right: 16 })
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
              Text(this.isLoggedIn ? '[4S]北京沃德龙鼎吉利' : '登录后查看门店信息')
                .fontSize(12).fontColor(DesignTokens.body).layoutWeight(1)
              Text('▾').fontSize(10).fontColor(DesignTokens.body)
            }.width('100%')
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
          this.MetricCell(this.isLoggedIn ? '1028' : '0', '加入天数')
          this.MetricCell(this.isLoggedIn ? '28' : '0', '员工数')
          this.MetricCell(this.isLoggedIn ? '2059' : '0', '店铺天数')
          this.MetricCell(this.isLoggedIn ? '9366' : '0', '累计客户')
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
      NativeHomeContent({ onDeferred: (t: string): void => { this.openDeferred(t); } })
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
      if ((id === MainTab.Chat || id === MainTab.Community) && !this.isLoggedIn) {
        this.tab = id;
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

      if (this.nativePane === 'feature' && this.featurePath.length > 0) {
        this.NativeFeaturePane()
      }

      if (this.showLogin) {
        HuaweiQuickLoginPane({
          onSuccess: (session: AuthSession) => {
            this.applySession(session);
          },
          onCancel: () => {
            this.showLogin = false;
            this.tab = MainTab.Home;
          },
        })
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
