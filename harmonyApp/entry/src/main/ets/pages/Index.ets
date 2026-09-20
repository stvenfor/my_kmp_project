import { ArkUIViewController, Compose } from '@cpf-kmp-cmp/compose';
import { BusinessError } from '@kit.BasicServicesKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { deviceInfo } from '@kit.BasicServicesKit';
import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import libentry from 'libentry.so';
import { registerVideoInteropBuilders } from '../ComposeVideoInterops';
import { registerWebInteropBuilders } from '../ComposeWebInterops';

const DOMAIN = 0x0000;

/** Synced from KMP DesignTokens / Flutter VercelTokens.light */
class DesignTokens {
  static readonly primary: string = '#171717';
  static readonly ink: string = '#171717';
  static readonly body: string = '#4D4D4D';
  static readonly mute: string = '#888888';
  static readonly canvas: string = '#FFFFFF';
  static readonly canvasSoft2: string = '#F5F5F5';
  static readonly link: string = '#0070F3';
  static readonly hairline: string = '#EBEBEB';
}

enum AppPhase {
  Splash,
  Privacy,
  Main
}

enum MainTab {
  Home = 0,
  Chat = 1,
  Community = 2,
  Mine = 3
}

@Entry
@Component
struct Index {
  @State phase: AppPhase = AppPhase.Splash;
  @State privacyAccepted: boolean = false;
  @State tab: MainTab = MainTab.Home;
  @State isLoggedIn: boolean = false;
  @State showLogin: boolean = false;
  @State showIsland: boolean = false;
  @State stubTitle: string = '';
  @State showStub: boolean = false;
  @State errorMessage: string = '';
  private islandController: ArkUIViewController | undefined = undefined;
  private pref: preferences.Preferences | undefined = undefined;

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

  private openMineIsland() {
    const abi: string = deviceInfo.abiList;
    if (libentry === undefined) {
      this.errorMessage = `libentry.so failed (abi=${abi})`;
      return;
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
  HomePane() {
    List() {
      ForEach(['学习报告', '全部服务', '短视频', '直播', '课堂', '二手车'], (item: string) => {
        ListItem() {
          Text(item).fontColor(DesignTokens.ink).padding(16)
        }
        .onClick(() => {
          this.stubTitle = item;
          this.showStub = true;
        })
      })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  ChatPane() {
    Column() {
      Text('聊天').fontSize(18).fontColor(DesignTokens.ink)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  CommunityPane() {
    Column() {
      Text('社区').fontSize(18).fontColor(DesignTokens.ink)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  MinePane() {
    List() {
      ListItem() {
        Text(this.isLoggedIn ? '已登录' : '未登录').padding(16).fontColor(DesignTokens.ink)
      }
      if (!this.isLoggedIn) {
        ListItem() {
          Text('登录').padding(16).fontColor(DesignTokens.link)
        }.onClick(() => {
          this.showLogin = true;
        })
      }
      ListItem() {
        Text('设置').padding(16).fontColor(DesignTokens.ink)
      }.onClick(() => this.openMineIsland())
      ListItem() {
        Text('个性化设置').padding(16).fontColor(DesignTokens.ink)
      }.onClick(() => this.openMineIsland())
      ListItem() {
        Text('短视频').padding(16).fontColor(DesignTokens.ink)
      }.onClick(() => {
        this.stubTitle = '短视频';
        this.showStub = true;
      })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  TabContent() {
    if (this.tab === MainTab.Home) {
      this.HomePane()
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

      if (this.showStub) {
        Column({ space: 16 }) {
          Text(this.stubTitle).fontSize(22).fontColor(DesignTokens.ink)
          Text('后续开放').fontColor(DesignTokens.body)
          Button('返回').onClick(() => {
            this.showStub = false;
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
    if (this.showStub) {
      this.showStub = false;
      return true;
    }
    if (this.showLogin) {
      this.showLogin = false;
      return true;
    }
    return false;
  }
}
