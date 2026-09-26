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

const DOMAIN = 0x0000;

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
  private islandController: ArkUIViewController | undefined = undefined;
  private pref: preferences.Preferences | undefined = undefined;

  private readonly chatPeers: string[] = ['Mock好友1', 'Mock好友2', 'Mock好友3'];
  private readonly chatSnippets: string[] = ['晚上一起吃饭吗？', '你好', '你好'];
  private readonly chatTimes: string[] = ['22:50', '22:45', '22:40'];
  private readonly communityAuthors: string[] = ['张三', '李四', '王五'];
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
    const key = title.trim();
    // ADR 0002: Mine secondary → Compose island only
    if (key === '设置' || key === '/settings' || key === 'settings') {
      this.openMineIsland(false);
      return;
    }
    if (key === '个性化' || key === '/mine/personalized_settings' || key === 'personalized') {
      this.openMineIsland(true);
      return;
    }
    if (key === '/chat' || key === '消息' || key === '聊天') {
      this.tab = MainTab.Chat;
      if (!this.isLoggedIn) {
        this.showLogin = true;
      }
      return;
    }
    if (key === '/community' || key === '社区') {
      this.tab = MainTab.Community;
      if (!this.isLoggedIn) {
        this.showLogin = true;
      }
      return;
    }
    if (key === '/home' || key === '首页' || key === '/main') {
      this.tab = MainTab.Home;
      return;
    }
    if (key === '/mine' || key === '我的') {
      this.tab = MainTab.Mine;
      return;
    }
    // All other features: ArkTS native stub (一期后置)
    this.stubTitle = key.length > 0 ? key : '未命名入口';
    this.showStub = true;
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

        Row({ space: 8 }) {
          TextInput({ placeholder: '输入消息…', text: this.chatDraft })
            .layoutWeight(1)
            .height(40)
            .onChange((v: string) => { this.chatDraft = v; })
          Button('发送')
            .backgroundColor(DesignTokens.link)
            .onClick(() => {
              const t = this.chatDraft.trim();
              if (t.length === 0) {
                return;
              }
              this.chatMessages = this.chatMessages.concat([t]);
              this.chatDraft = '';
            })
        }.padding(12).width('100%').backgroundColor(DesignTokens.canvasSoft2)
      }
      .width('100%')
      .height('100%')
      .backgroundColor(DesignTokens.canvasSoft2)
    } else {
      Column() {
        Row() {
          Text('消息').fontSize(32).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink).layoutWeight(1)
          Text('⌕').fontSize(22).fontColor(DesignTokens.link).margin({ right: 16 })
          Text('✎').fontSize(20).fontColor(DesignTokens.link)
        }.padding({ left: 16, right: 16, top: 8, bottom: 8 }).width('100%')

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
          .onClick(() => this.openDeferred('发布动态'))
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
      .onClick(() => this.openDeferred('社区搜索'))

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
            Row({ space: 24 }) {
              Text('♥ 158').fontSize(14).fontColor('#EE0000')
              Text('💬 6').fontSize(14).fontColor(DesignTokens.body)
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
          Text('ⓘ').fontSize(18).fontColor(DesignTokens.link).margin({ right: 12 })
            .onClick(() => this.openMineIsland(true))
          Text('⚙').fontSize(18).fontColor(DesignTokens.link)
            .onClick(() => this.openMineIsland(false))
        }.width('100%')

        Column({ space: 8 }) {
          Text(this.isLoggedIn ? this.loginDisplayName : '访客')
            .fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          if (this.isLoggedIn) {
            Text(this.isLoggedIn ? '销售经理' : '未登录').fontSize(12).fontColor(DesignTokens.link)
            Text(this.isLoggedIn ? '[4S]北京大兴兴荣丰田汽车销售服务有限公司' : '登录后查看门店信息')
              .fontSize(12).fontColor(DesignTokens.body)
          } else {
            Button('登录').backgroundColor(DesignTokens.link).onClick(() => {
              this.showLogin = true;
            })
          }
        }
        .width('100%')
        .padding(16)
        .alignItems(HorizontalAlign.Start)
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
        Flex({ wrap: FlexWrap.Wrap }) {
          ForEach(['商城', '我的钱包', '我的课程', '我的订单'], (label: string) => {
            Column({ space: 6 }) {
              Text(label.substring(0, 1))
                .width(44).height(44).textAlign(TextAlign.Center)
                .fontColor(DesignTokens.link).backgroundColor('#1A0070F3').borderRadius(12)
              Text(label).fontSize(11).fontColor(DesignTokens.ink)
            }.width('25%').padding({ top: 8, bottom: 8 })
            .onClick(() => this.openDeferred(label))
          })
        }.width('100%')

        Column() {
          ForEach(['商务合作', '提醒事项', '邀请好友', '粉丝群', '意见反馈', '设置'], (label: string) => {
            Row() {
              Text(label).fontSize(15).fontColor(DesignTokens.ink).layoutWeight(1)
              Text('›').fontColor(DesignTokens.mute)
            }
            .width('100%')
            .padding(16)
            .onClick(() => {
              if (label === '设置') {
                this.openMineIsland(false);
              } else {
                this.openDeferred(label);
              }
            })
          })
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
    if (id === MainTab.Home || id === MainTab.Chat) {
      return active ? $r('app.media.main_tab_home_selected') : $r('app.media.main_tab_home_unselected');
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


      if (this.showStub) {
        Column({ space: 16 }) {
          Text(this.stubTitle).fontSize(22).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          Text('一期后置 · ArkTS 原生占位').fontSize(15).fontColor(DesignTokens.body)
          Text('ADR 0002：非 Mine 二级不进 Compose 岛')
            .fontSize(12)
            .fontColor(DesignTokens.mute)
            .textAlign(TextAlign.Center)
            .padding({ left: 24, right: 24 })
          Button('返回')
            .backgroundColor(DesignTokens.link)
            .onClick(() => {
              this.showStub = false;
            })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor(DesignTokens.canvasSoft2)
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
