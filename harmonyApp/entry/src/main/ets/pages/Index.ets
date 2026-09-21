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

const DOMAIN = 0x0000;

/**
 * ADR 0002: ArkTS owns splash / privacy / tab roots / Mine root / auth.
 * Compose is only pushed for Mine secondary island; deferred entries use ArkTS stubs.
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
  private islandController: ArkUIViewController | undefined = undefined;
  private pref: preferences.Preferences | undefined = undefined;

  private readonly chatPeers: string[] = ['Mock好友1', 'Mock好友2', 'Mock好友3'];
  private readonly chatSnippets: string[] = ['你好，最近怎么样？', '明天一起开会吧', '收到，谢谢'];
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
    setTimeout(() => {
      this.phase = this.privacyAccepted ? AppPhase.Main : AppPhase.Privacy;
    }, 1200);
  }

  /** kind 0 = Mine island, kind 1 = deferred feature page. */
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

  private openDeferred(title: string) {
    this.stubTitle = title;
    this.showStub = true;
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
    Column() {
      Row() {
        Text('消息').fontSize(28).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink).layoutWeight(1)
        Text('⌕').fontSize(20).fontColor(DesignTokens.ink).margin({ right: 12 })
        Text('✎').fontSize(20).fontColor(DesignTokens.ink)
      }.padding(16).width('100%')

      List() {
        ForEach(this.chatPeers, (name: string, index: number) => {
          ListItem() {
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
              if (index === 0) {
                Text('2')
                  .fontSize(11)
                  .fontColor(Color.White)
                  .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                  .backgroundColor('#EE0000')
                  .borderRadius(10)
              }
            }.padding(16).width('100%')
          }
        })
      }.layoutWeight(1)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  CommunityPane() {
    Column() {
      Text('社区').fontSize(18).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink).padding(16).width('100%')
      Row() {
        ForEach(['最新', '热门', '关注'], (item: string) => {
          Text(item)
            .fontSize(14)
            .fontWeight(this.communityFilter === item ? FontWeight.Medium : FontWeight.Regular)
            .fontColor(this.communityFilter === item ? DesignTokens.link : DesignTokens.mute)
            .layoutWeight(1)
            .textAlign(TextAlign.Center)
            .onClick(() => {
              this.communityFilter = item;
            })
        })
      }.padding({ bottom: 8 }).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.communityAuthors, (author: string, index: number) => {
          ListItem() {
            Column({ space: 8 }) {
              Text(author).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text(this.communityBodies[index]).fontSize(14).fontColor(DesignTokens.body)
            }.padding(16).width('100%').alignItems(HorizontalAlign.Start)
          }
        })
      }.layoutWeight(1)
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
          Blank().layoutWeight(1)
          Text('ⓘ').fontSize(18).fontColor(DesignTokens.ink).margin({ right: 12 })
            .onClick(() => this.openMineIsland(true))
          Text('⚙').fontSize(18).fontColor(DesignTokens.ink)
            .onClick(() => this.openMineIsland(false))
        }.width('100%')

        Column({ space: 8 }) {
          Text(this.isLoggedIn ? '用户0000' : '未登录')
            .fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          if (this.isLoggedIn) {
            Text('销售经理').fontSize(12).fontColor(DesignTokens.link)
            Text('[4S]北京大兴兴荣丰田汽车销售服务有限公司').fontSize(12).fontColor(DesignTokens.body)
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
              } else if (label === '粉丝群') {
                this.openDeferred('短视频');
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
      NativeHomeContent({ onDeferred: (t: string) => this.openDeferred(t) })
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

  @Builder
  TabBtn(label: string, id: MainTab) {
    Column() {
      Text(label)
        .fontSize(12)
        .fontColor(this.tab === id ? DesignTokens.link : DesignTokens.mute)
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
          .height(56)
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
          Text(this.stubTitle).fontSize(22).fontColor(DesignTokens.ink)
          Text('一期后置 · 原生占位').fontColor(DesignTokens.body)
          Button('返回').onClick(() => {
            this.showStub = false;
          })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor(DesignTokens.canvasSoft2)
      }

      if (this.showLogin) {
        Column({ space: 16 }) {
          Text('登录').fontSize(22).fontColor(DesignTokens.ink)
          Text('演示登录').fontColor(DesignTokens.body)
          Button('登录成功')
            .backgroundColor(DesignTokens.link)
            .onClick(() => {
              this.isLoggedIn = true;
              this.showLogin = false;
            })
          Button('取消').onClick(() => {
            this.showLogin = false;
            this.tab = MainTab.Home;
          })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor(DesignTokens.canvasSoft2)
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
