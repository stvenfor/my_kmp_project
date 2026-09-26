import { promptAction } from '@kit.ArkUI';
import { AuthSession, sharedSessionStore } from './SessionStore';
import { HuaweiQuickLoginPane } from './HuaweiQuickLoginPane';

/**
 * Flutter LoginPage SoT — 邮箱/短信分段 + 协议 + 主按钮 + 微信占位 + 页脚。
 * 华为一键登录作为 OHOS 附加入口（平台能力），不替代 Flutter 主流程。
 */
@Component
export struct NativeLoginPane {
  onSuccess: (session: AuthSession) => void = () => {};
  onCancel: () => void = () => {};

  @State mode: number = 0; // 0 email 1 phone
  @State email: string = '';
  @State password: string = '';
  @State phone: string = '13400000000';
  @State otp: string = '';
  @State agreed: boolean = true;
  @State error: string = '';
  @State loading: boolean = false;
  @State otpCooldown: number = 0;
  @State otpHint: string = '';
  @State showRegister: boolean = false;
  @State showHuawei: boolean = false;

  private greeting(): string {
    const h = new Date().getHours();
    if (h < 12) {
      return '早上好，欢迎使用i车商';
    }
    if (h < 18) {
      return '下午好，欢迎使用i车商';
    }
    return '晚上好，欢迎使用i车商';
  }

  private mockSession(): AuthSession {
    const deviceId = sharedSessionStore.deviceId();
    return {
      token: `mock_${Date.now()}`,
      refreshToken: `refresh_${Date.now()}`,
      sessionId: `sess_${Date.now()}`,
      userId: 'qa_user',
      userName: 'qa_user',
      phone: this.phone.length === 11 ? this.phone : '13400000000',
      deviceId,
    };
  }

  private canSubmit(): boolean {
    if (!this.agreed || this.loading) {
      return false;
    }
    if (this.mode === 0) {
      return this.email.indexOf('@') >= 0 && this.password.length >= 6;
    }
    return this.phone.length === 11 && this.otp.length >= 4;
  }

  private submit(): void {
    if (!this.agreed) {
      this.error = '请先阅读并同意隐私条款';
      return;
    }
    if (!this.canSubmit()) {
      return;
    }
    this.loading = true;
    this.error = '';
    setTimeout(() => {
      this.loading = false;
      const session = this.mockSession();
      sharedSessionStore.save(session);
      try {
        promptAction.showToast({ message: '登录成功', duration: 1500 });
      } catch (_e) {
        // ignore
      }
      this.onSuccess(session);
    }, 350);
  }

  private sendOtp(): void {
    if (!this.agreed) {
      this.error = '请先阅读并同意隐私条款';
      return;
    }
    if (this.phone.length !== 11) {
      this.error = '请输入有效的手机号';
      return;
    }
    this.error = '';
    this.otpCooldown = 60;
    this.otpHint = '验证码已发送';
    this.otp = '123456';
    const tick = setInterval(() => {
      if (this.otpCooldown <= 1) {
        clearInterval(tick);
        this.otpCooldown = 0;
      } else {
        this.otpCooldown -= 1;
      }
    }, 1000);
  }

  @Builder
  field(placeholder: string, value: string, onChange: (v: string) => void, password: boolean = false) {
    TextInput({ placeholder: placeholder, text: value })
      .type(password ? InputType.Password : InputType.Normal)
      .height(52)
      .padding({ left: 16, right: 16 })
      .backgroundColor(Color.White)
      .border({ width: 0.5, color: '#C6C6C8' })
      .borderRadius(10)
      .onChange((v: string) => onChange(v))
  }

  build() {
    if (this.showHuawei) {
      HuaweiQuickLoginPane({
        onSuccess: (s: AuthSession) => this.onSuccess(s),
        onCancel: () => { this.showHuawei = false; },
      })
    } else if (this.showRegister) {
      this.RegisterBody()
    } else {
      this.LoginBody()
    }
  }

  @Builder
  LoginBody() {
    Column() {
      Row() {
        Text('关闭').fontSize(16).fontColor('#007AFF').onClick(() => this.onCancel())
        Blank().layoutWeight(1)
        Text('登录').fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.Black)
        Blank().layoutWeight(1)
        Text('').width(40)
      }.padding({ left: 16, right: 16, top: 12, bottom: 8 }).width('100%')

      Scroll() {
        Column() {
          Text(this.greeting())
            .fontSize(32).fontWeight(FontWeight.Bold).fontColor(Color.Black)
            .margin({ top: 16 })
          Text('登录以继续使用')
            .fontSize(15).fontColor('#993C3C43')
            .margin({ top: 8 })

          Row() {
            Text('邮箱登录')
              .fontSize(15).fontWeight(FontWeight.Medium)
              .layoutWeight(1).textAlign(TextAlign.Center)
              .padding({ top: 10, bottom: 10 })
              .backgroundColor(this.mode === 0 ? Color.White : Color.Transparent)
              .borderRadius(8)
              .onClick(() => { this.mode = 0; this.error = ''; })
            Text('短信登录')
              .fontSize(15).fontWeight(FontWeight.Medium)
              .layoutWeight(1).textAlign(TextAlign.Center)
              .padding({ top: 10, bottom: 10 })
              .backgroundColor(this.mode === 1 ? Color.White : Color.Transparent)
              .borderRadius(8)
              .onClick(() => { this.mode = 1; this.error = ''; })
          }
          .padding(4)
          .margin({ top: 40 })
          .width('100%')
          .backgroundColor('#E9E9EB')
          .borderRadius(10)

          if (this.mode === 0) {
            Column({ space: 16 }) {
              this.field('邮箱', this.email, (v: string) => { this.email = v; this.error = ''; })
              this.field('密码', this.password, (v: string) => { this.password = v; this.error = ''; }, true)
            }.margin({ top: 24 }).width('100%')
          } else {
            Column({ space: 16 }) {
              Row({ space: 12 }) {
                Text('+86').fontSize(17).fontWeight(FontWeight.Medium)
                  .height(52).padding({ left: 16, right: 16 })
                  .backgroundColor(Color.White).borderRadius(10)
                TextInput({ placeholder: '手机号', text: this.phone })
                  .layoutWeight(1).height(52)
                  .padding({ left: 16, right: 16 })
                  .backgroundColor(Color.White)
                  .border({ width: 0.5, color: '#C6C6C8' })
                  .borderRadius(10)
                  .type(InputType.PhoneNumber)
                  .onChange((v: string) => {
                    this.phone = v.replace(/[^0-9]/g, '').substring(0, 11);
                    this.error = '';
                  })
              }.width('100%')
              Row({ space: 12 }) {
                TextInput({ placeholder: '验证码', text: this.otp })
                  .layoutWeight(1).height(52)
                  .padding({ left: 16, right: 16 })
                  .backgroundColor(Color.White)
                  .border({ width: 0.5, color: '#C6C6C8' })
                  .borderRadius(10)
                  .type(InputType.Number)
                  .onChange((v: string) => {
                    this.otp = v.replace(/[^0-9]/g, '').substring(0, 6);
                    this.error = '';
                  })
                Text(this.otpCooldown > 0 ? `${this.otpCooldown}s` : '获取验证码')
                  .fontSize(14).fontColor('#007AFF')
                  .onClick(() => this.sendOtp())
              }.width('100%')
              if (this.otpHint.length > 0) {
                Text(this.otpHint).fontSize(12).fontColor('#993C3C43')
              }
            }.margin({ top: 24 }).width('100%')
          }

          Row({ space: 8 }) {
            Column()
              .width(22).height(22).borderRadius(11)
              .backgroundColor(this.agreed ? '#007AFF' : Color.Transparent)
              .border({ width: 1.5, color: this.agreed ? '#007AFF' : '#C6C6C8' })
              .onClick(() => { this.agreed = !this.agreed; this.error = ''; })
            Text() {
              Span('我已阅读并同意').fontSize(13).fontColor('#993C3C43')
              Span('《某个隐私条款》').fontSize(13).fontColor('#007AFF')
            }.layoutWeight(1)
          }
          .margin({ top: 24 })
          .width('100%')
          .alignItems(VerticalAlign.Top)
          .onClick(() => { this.agreed = !this.agreed; this.error = ''; })

          if (this.error.length > 0) {
            Text(this.error).fontSize(13).fontColor('#B00020').margin({ top: 8 })
          }

          Text(this.loading ? '登录中…' : '登录')
            .fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .width('100%').height(52)
            .textAlign(TextAlign.Center)
            .margin({ top: 32 })
            .backgroundColor(this.canSubmit() ? '#007AFF' : '#C7C7CC')
            .borderRadius(14)
            .onClick(() => this.submit())

          Text('微信登录')
            .fontSize(17).fontWeight(FontWeight.Medium).fontColor('#007AFF')
            .width('100%').height(52)
            .textAlign(TextAlign.Center)
            .margin({ top: 16 })
            .border({ width: 1, color: '#C6C6C8' })
            .borderRadius(14)
            .onClick(() => { this.error = '微信登录需接入微信 SDK（见 platform-gap-registry）'; })

          Text('华为账号一键登录')
            .fontSize(14).fontColor('#007AFF')
            .width('100%').textAlign(TextAlign.Center)
            .margin({ top: 12 })
            .onClick(() => { this.showHuawei = true; })

          Row() {
            Text('我要注册').fontSize(13).fontWeight(FontWeight.Medium).fontColor('#007AFF')
              .onClick(() => { this.showRegister = true; })
            Column().width(1).height(12).backgroundColor('#C6C6C8').margin({ left: 16, right: 16 })
            Text('忘记密码').fontSize(13).fontWeight(FontWeight.Medium).fontColor('#007AFF')
              .onClick(() => { this.error = '忘记密码功能暂未开放'; })
          }
          .margin({ top: 24 })
          .justifyContent(FlexAlign.Center)
          .width('100%')
        }
        .padding({ left: 24, right: 24, bottom: 32 })
        .width('100%')
        .alignItems(HorizontalAlign.Start)
      }
      .layoutWeight(1)
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F2F2F7')
  }

  @Builder
  RegisterBody() {
    Column() {
      Row() {
        Text('关闭').fontSize(16).fontColor('#007AFF').onClick(() => { this.showRegister = false; })
        Blank().layoutWeight(1)
        Text('注册').fontSize(17).fontWeight(FontWeight.Medium)
        Blank().layoutWeight(1)
        Text('').width(40)
      }.padding({ left: 16, right: 16, top: 12, bottom: 8 }).width('100%')

      Scroll() {
        Column({ space: 16 }) {
          Text('创建账号').fontSize(32).fontWeight(FontWeight.Bold).margin({ top: 16 })
          Text('注册后即可使用全部功能').fontSize(15).fontColor('#993C3C43')
          this.field('邮箱', this.email, (v: string) => { this.email = v; })
          this.field('密码', this.password, (v: string) => { this.password = v; }, true)
          this.field('确认密码', this.password, (v: string) => { this.password = v; }, true)
          Text('注册')
            .fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .width('100%').height(52)
            .textAlign(TextAlign.Center)
            .margin({ top: 16 })
            .backgroundColor('#007AFF')
            .borderRadius(14)
            .onClick(() => this.submit())
        }
        .padding({ left: 24, right: 24, bottom: 32 })
        .width('100%')
        .alignItems(HorizontalAlign.Start)
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F2F2F7')
  }
}
