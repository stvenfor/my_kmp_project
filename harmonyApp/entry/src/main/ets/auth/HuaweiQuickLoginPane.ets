import { authentication, loginComponentManager, LoginWithHuaweiIDButton } from '@kit.AccountKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { promptAction, router } from '@kit.ArkUI';
import {
  APP_PRIVACY_URL,
  APP_USER_SERVICE_URL,
  HUAWEI_USER_AUTH_PROTOCOL,
} from './AuthConfig';
import { loginWithHuaweiCode } from './AuthApiClient';
import { AuthSession, sharedSessionStore } from './SessionStore';

const LOG_TAG = 'HuaweiQuickLogin';
const DOMAIN = 0x0000;

const USER_SERVICE_TAG = '用户服务协议';
const PRIVACY_TAG = '隐私协议';
const USER_AUTHENTICATION_TAG = '华为账号用户认证协议';

@Component
export struct HuaweiQuickLoginPane {
  onSuccess: (session: AuthSession) => void = () => {};
  onCancel: () => void = () => {};

  @State anonymousPhone: string = '';
  @State isSelected: boolean = false;
  @State busy: boolean = false;
  @State tip: string = '';
  @State previewFailed: boolean = false;

  private privacyText: loginComponentManager.PrivacyText[] = [
    { text: '已阅读并同意', type: loginComponentManager.TextType.PLAIN_TEXT },
    { text: '《用户服务协议》', tag: USER_SERVICE_TAG, type: loginComponentManager.TextType.RICH_TEXT },
    { text: '《隐私协议》', tag: PRIVACY_TAG, type: loginComponentManager.TextType.RICH_TEXT },
    { text: '和', type: loginComponentManager.TextType.PLAIN_TEXT },
    {
      text: '《华为账号用户认证协议》',
      tag: USER_AUTHENTICATION_TAG,
      type: loginComponentManager.TextType.RICH_TEXT,
    },
    { text: '。', type: loginComponentManager.TextType.PLAIN_TEXT },
  ];

  private controller: loginComponentManager.LoginWithHuaweiIDButtonController =
    new loginComponentManager.LoginWithHuaweiIDButtonController()
      .setAgreementStatus(loginComponentManager.AgreementStatus.NOT_ACCEPTED)
      .onClickLoginWithHuaweiIDButton((error: BusinessError | undefined,
        response: loginComponentManager.HuaweiIDCredential) => {
        this.handleButtonResult(error, response);
      })
      .onClickEvent((error: BusinessError, _clickEvent: loginComponentManager.ClickEvent) => {
        if (error) {
          hilog.error(DOMAIN, LOG_TAG, 'clickEvent err %{public}d %{public}s', error.code, error.message);
        }
      });

  private agreementDialog: CustomDialogController = new CustomDialogController({
    builder: AgreementConfirmDialog({
      onCancel: () => {
        this.agreementDialog.close();
        this.controller.setAgreementStatus(loginComponentManager.AgreementStatus.NOT_ACCEPTED);
      },
      onConfirm: () => {
        this.agreementDialog.close();
        this.isSelected = true;
        this.controller.setAgreementStatus(loginComponentManager.AgreementStatus.ACCEPTED);
        this.controller.continueLogin((error: BusinessError) => {
          if (error) {
            this.tip = error.message || '继续登录失败';
          }
        });
      },
    }),
    autoCancel: false,
    alignment: DialogAlignment.Center,
  });

  aboutToAppear(): void {
    this.loadAnonymousPhone();
  }

  private loadAnonymousPhone(): void {
    try {
      const authRequest = new authentication.HuaweiIDProvider().createAuthorizationWithHuaweiIDRequest();
      // 需 AGC 申请 quickLoginMobilePhone；未批时会失败并提示 tip。
      authRequest.scopes = 'quickLoginAnonymousPhone';
      authRequest.forceAuthorization = false;
      const controller = new authentication.AuthenticationController();
      controller.executeRequest(authRequest).then((response: authentication.AuthorizationWithHuaweiIDResponse) => {
        const extra = response.data?.extraInfo as Record<string, Object> | undefined;
        const phone = extra?.['quickLoginAnonymousPhone'] as string | undefined;
        if (phone) {
          this.anonymousPhone = phone;
        }
        if (!phone) {
          this.previewFailed = true;
          this.tip = '未拿到匿名号（未登录华为账号，或 AGC 未开通一键登录 scope）';
        }
      }).catch((error: BusinessError) => {
        this.previewFailed = true;
        this.tip = `预授权失败 ${error.code}: ${error.message}（若为 1001502014，请先在 AGC 申请 quickLoginMobilePhone）`;
        hilog.error(DOMAIN, LOG_TAG, 'preview auth failed %{public}d %{public}s', error.code, error.message);
      });
    } catch (e) {
      this.previewFailed = true;
      this.tip = `Account Kit 不可用: ${String(e)}`;
    }
  }

  private openProtocol(tag: string): void {
    let url = APP_USER_SERVICE_URL;
    if (tag === PRIVACY_TAG) {
      url = APP_PRIVACY_URL;
    } else if (tag === USER_AUTHENTICATION_TAG) {
      url = HUAWEI_USER_AUTH_PROTOCOL;
    }
    router.pushUrl({
      url: 'pages/WebPage',
      params: { url },
    }).catch((err: Error) => {
      hilog.error(DOMAIN, LOG_TAG, 'open protocol failed %{public}s', String(err));
    });
  }

  private handleButtonResult(error: BusinessError | undefined,
    response: loginComponentManager.HuaweiIDCredential): void {
    if (error) {
      // 未同意协议：弹确认框（对齐官方 AGREEMENT_STATUS_NOT_ACCEPTED）
      if (error.code === 1001502005) {
        this.agreementDialog.open();
        return;
      }
      this.tip = `${error.code}: ${error.message}`;
      return;
    }
    if (!this.isSelected) {
      this.agreementDialog.open();
      return;
    }
    const authCode = response?.authorizationCode;
    if (!authCode) {
      this.tip = '未返回 authorizationCode';
      return;
    }
    this.submitCode(authCode);
  }

  private async submitCode(authCode: string): Promise<void> {
    if (this.busy) {
      return;
    }
    this.busy = true;
    this.tip = '正在登录…';
    try {
      const deviceId = sharedSessionStore.deviceId();
      const session = await loginWithHuaweiCode(authCode, deviceId);
      sharedSessionStore.save(session);
      this.tip = '';
      this.onSuccess(session);
      try {
        promptAction.showToast({ message: '登录成功', duration: 1500 });
      } catch (_e) {
        // ignore toast failures
      }
    } catch (e) {
      this.tip = String(e);
      hilog.error(DOMAIN, LOG_TAG, 'bff login failed %{public}s', String(e));
    } finally {
      this.busy = false;
    }
  }

  build() {
    Column({ space: 16 }) {
      Text('登录').fontSize(22).fontWeight(FontWeight.Medium)
      if (this.anonymousPhone) {
        Text(this.anonymousPhone)
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
        Text('华为账号绑定号码')
          .fontSize(13)
          .fontColor('#666666')
      } else {
        Text(this.previewFailed ? '一键登录预览不可用' : '正在读取华为账号…')
          .fontSize(14)
          .fontColor('#666666')
      }

      Column() {
        LoginWithHuaweiIDButton({
          params: {
            style: loginComponentManager.Style.BUTTON_RED,
            extraStyle: {
              buttonStyle: new loginComponentManager.ButtonStyle().loadingStyle({ show: true }),
            },
            borderRadius: 24,
            loginType: loginComponentManager.LoginType.QUICK_LOGIN,
            supportDarkMode: true,
            verifyPhoneNumber: true,
          },
          controller: this.controller,
        })
      }
      .height(40)
      .width('80%')

      Row({ space: 8 }) {
        Checkbox({ name: 'hwPrivacy', group: 'hwPrivacyGroup' })
          .select(this.isSelected)
          .width(22)
          .height(22)
          .onChange((v: boolean) => {
            this.isSelected = v;
            this.controller.setAgreementStatus(
              v ? loginComponentManager.AgreementStatus.ACCEPTED :
                loginComponentManager.AgreementStatus.NOT_ACCEPTED
            );
          })
        Text() {
          ForEach(this.privacyText, (item: loginComponentManager.PrivacyText) => {
            if (item.type === loginComponentManager.TextType.PLAIN_TEXT && item.text) {
              Span(item.text).fontSize(12).fontColor('#666666')
            } else if (item.type === loginComponentManager.TextType.RICH_TEXT && item.text) {
              Span(item.text)
                .fontSize(12)
                .fontColor('#0070F3')
                .onClick(() => {
                  if (item.tag) {
                    this.openProtocol(item.tag);
                  }
                })
            }
          })
        }.layoutWeight(1)
      }
      .width('86%')
      .alignItems(VerticalAlign.Top)

      if (this.tip) {
        Text(this.tip)
          .fontSize(12)
          .fontColor('#B00020')
          .width('86%')
          .textAlign(TextAlign.Center)
      }

      if (this.busy) {
        LoadingProgress().width(28).height(28)
      }

      Button('取消')
        .backgroundColor('#E8E8E8')
        .fontColor('#333333')
        .onClick(() => this.onCancel())
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .padding(24)
  }
}

@CustomDialog
struct AgreementConfirmDialog {
  controller?: CustomDialogController;
  onCancel: () => void = () => {};
  onConfirm: () => void = () => {};

  build() {
    Column({ space: 16 }) {
      Text('请先阅读并同意相关协议').fontSize(16).fontWeight(FontWeight.Medium)
      Row({ space: 12 }) {
        Button('取消')
          .layoutWeight(1)
          .onClick(() => this.onCancel())
        Button('同意并登录')
          .layoutWeight(1)
          .backgroundColor('#E60012')
          .onClick(() => this.onConfirm())
      }.width('100%')
    }
    .padding(20)
    .width('80%')
  }
}
