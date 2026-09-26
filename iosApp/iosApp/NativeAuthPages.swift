import SwiftUI
import ComposeApp

/// Flutter `LoginPage` / `RegisterPage` SoT — ADR 0002 native auth (not demo stub).
enum AuthTokens {
    static let accent = Color(red: 0, green: 122/255, blue: 1)
    static let background = Color(red: 242/255, green: 242/255, blue: 247/255)
    static let surface = Color.white
    static let fillSecondary = Color(red: 233/255, green: 233/255, blue: 235/255)
    static let labelPrimary = Color.black
    static let labelSecondary = Color(red: 60/255, green: 60/255, blue: 67/255).opacity(0.6)
    static let labelTertiary = Color(red: 60/255, green: 60/255, blue: 67/255).opacity(0.3)
    static let separator = Color(red: 198/255, green: 198/255, blue: 200/255)
    static let buttonDisabled = Color(red: 199/255, green: 199/255, blue: 204/255)
    static let fieldHeight: CGFloat = 52
    static let buttonHeight: CGFloat = 52
    static let radiusMd: CGFloat = 10
    static let radiusLg: CGFloat = 14
}

private enum AuthCredentialMode { case email, phone }

struct NativeLoginView: View {
    var onSuccess: () -> Void
    var onCancel: () -> Void

    @State private var mode: AuthCredentialMode = .email
    @State private var email = ""
    @State private var password = ""
    @State private var phone = "13400000000"
    @State private var otp = ""
    @State private var agreedPrivacy = true
    @State private var error: String?
    @State private var loading = false
    @State private var otpCooldown = 0
    @State private var otpHint: String?
    @State private var showRegister = false

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return "早上好，欢迎使用i车商" }
        if hour < 18 { return "下午好，欢迎使用i车商" }
        return "晚上好，欢迎使用i车商"
    }

    private var canSubmit: Bool {
        guard agreedPrivacy, !loading else { return false }
        switch mode {
        case .email:
            return email.contains("@") && password.count >= 6
        case .phone:
            return phone.count == 11 && otp.count >= 4
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(greeting)
                        .font(.system(size: 32, weight: .bold))
                        .foregroundStyle(AuthTokens.labelPrimary)
                        .padding(.top, 16)
                    Text("登录以继续使用")
                        .font(.system(size: 15))
                        .foregroundStyle(AuthTokens.labelSecondary)
                        .padding(.top, 8)

                    credentialSwitcher
                        .padding(.top, 40)

                    Group {
                        if mode == .email {
                            emailForm
                        } else {
                            phoneForm
                        }
                    }
                    .padding(.top, 24)

                    privacyRow
                        .padding(.top, 24)

                    if let error {
                        Text(error)
                            .font(.system(size: 13))
                            .foregroundStyle(Color.red.opacity(0.85))
                            .padding(.top, 8)
                    }

                    primaryButton
                        .padding(.top, 32)

                    wechatButton
                        .padding(.top, 16)

                    footerLinks
                        .padding(.top, 24)
                        .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
            .background(AuthTokens.background.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", action: onCancel)
                        .foregroundStyle(AuthTokens.accent)
                }
                ToolbarItem(placement: .principal) {
                    Text("登录").font(.system(size: 17, weight: .semibold))
                }
            }
            .sheet(isPresented: $showRegister) {
                NativeRegisterView(
                    onSuccess: {
                        showRegister = false
                        onSuccess()
                    },
                    onCancel: { showRegister = false }
                )
            }
            .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
                if otpCooldown > 0 { otpCooldown -= 1 }
            }
        }
    }

    private var credentialSwitcher: some View {
        HStack(spacing: 0) {
            segment("邮箱登录", selected: mode == .email) { mode = .email; error = nil }
            segment("短信登录", selected: mode == .phone) { mode = .phone; error = nil }
        }
        .padding(4)
        .background(AuthTokens.fillSecondary, in: RoundedRectangle(cornerRadius: AuthTokens.radiusMd))
    }

    private func segment(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Text(title)
            .font(.system(size: 15, weight: .medium))
            .foregroundStyle(AuthTokens.labelPrimary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(selected ? AuthTokens.surface : Color.clear, in: RoundedRectangle(cornerRadius: 8))
            .onTapGesture(perform: action)
    }

    private var emailForm: some View {
        VStack(spacing: 16) {
            authField(placeholder: "邮箱", text: $email, keyboard: .emailAddress)
            authField(placeholder: "密码", text: $password, keyboard: .default, secure: true)
        }
    }

    private var phoneForm: some View {
        VStack(spacing: 16) {
            HStack(spacing: 12) {
                Text("+86")
                    .font(.system(size: 17, weight: .medium))
                    .frame(height: AuthTokens.fieldHeight)
                    .padding(.horizontal, 16)
                    .background(AuthTokens.surface, in: RoundedRectangle(cornerRadius: AuthTokens.radiusMd))
                authField(placeholder: "手机号", text: $phone, keyboard: .phonePad)
                    .onChange(of: phone) { _, v in
                        phone = String(v.filter(\.isNumber).prefix(11))
                    }
            }
            HStack(spacing: 12) {
                authField(placeholder: "验证码", text: $otp, keyboard: .numberPad)
                    .onChange(of: otp) { _, v in
                        otp = String(v.filter(\.isNumber).prefix(6))
                    }
                Button(otpCooldown > 0 ? "\(otpCooldown)s" : "获取验证码") {
                    sendOtp()
                }
                .font(.system(size: 14))
                .foregroundStyle(AuthTokens.accent)
                .disabled(otpCooldown > 0 || loading)
            }
            if let otpHint {
                Text(otpHint)
                    .font(.system(size: 12))
                    .foregroundStyle(AuthTokens.labelSecondary)
            }
        }
    }

    private var privacyRow: some View {
        Button {
            agreedPrivacy.toggle()
            error = nil
        } label: {
            HStack(alignment: .top, spacing: 8) {
                ZStack {
                    Circle()
                        .stroke(agreedPrivacy ? AuthTokens.accent : AuthTokens.separator, lineWidth: 1.5)
                        .frame(width: 22, height: 22)
                    if agreedPrivacy {
                        Circle().fill(AuthTokens.accent).frame(width: 22, height: 22)
                        Image(systemName: "checkmark")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(.white)
                    }
                }
                .frame(width: 44, height: 44, alignment: .leading)
                (
                    Text("我已阅读并同意")
                        .foregroundStyle(AuthTokens.labelSecondary)
                    + Text("《某个隐私条款》")
                        .foregroundStyle(AuthTokens.accent)
                )
                .font(.system(size: 13))
                .padding(.top, 12)
                Spacer()
            }
        }
        .buttonStyle(.plain)
    }

    private var primaryButton: some View {
        Button {
            submit()
        } label: {
            Group {
                if loading {
                    ProgressView().tint(.white)
                } else {
                    Text("登录")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: AuthTokens.buttonHeight)
            .background(canSubmit ? AuthTokens.accent : AuthTokens.buttonDisabled, in: RoundedRectangle(cornerRadius: AuthTokens.radiusLg))
        }
        .disabled(!canSubmit)
    }

    private var wechatButton: some View {
        Button {
            error = "微信登录需接入微信 SDK（见 platform-gap-registry）"
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "message")
                Text("微信登录")
                    .font(.system(size: 17, weight: .semibold))
            }
            .foregroundStyle(AuthTokens.accent)
            .frame(maxWidth: .infinity)
            .frame(height: AuthTokens.buttonHeight)
            .overlay(
                RoundedRectangle(cornerRadius: AuthTokens.radiusLg)
                    .stroke(AuthTokens.separator, lineWidth: 1)
            )
        }
        .disabled(loading)
    }

    private var footerLinks: some View {
        HStack(spacing: 16) {
            Button("我要注册") { showRegister = true }
                .foregroundStyle(AuthTokens.accent)
            Rectangle()
                .fill(AuthTokens.separator)
                .frame(width: 1, height: 12)
            Button("忘记密码") { error = "忘记密码功能暂未开放" }
                .foregroundStyle(AuthTokens.accent)
        }
        .font(.system(size: 13, weight: .medium))
    }

    private func authField(
        placeholder: String,
        text: Binding<String>,
        keyboard: UIKeyboardType,
        secure: Bool = false
    ) -> some View {
        Group {
            if secure {
                SecureField(placeholder, text: text)
            } else {
                TextField(placeholder, text: text)
                    .keyboardType(keyboard)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
        }
        .font(.system(size: 17))
        .padding(.horizontal, 16)
        .frame(height: AuthTokens.fieldHeight)
        .background(AuthTokens.surface, in: RoundedRectangle(cornerRadius: AuthTokens.radiusMd))
        .overlay(
            RoundedRectangle(cornerRadius: AuthTokens.radiusMd)
                .stroke(AuthTokens.separator, lineWidth: 0.5)
        )
    }

    private func sendOtp() {
        guard agreedPrivacy else {
            error = "请先阅读并同意隐私条款"
            return
        }
        guard phone.count == 11 else {
            error = "请输入有效的手机号"
            return
        }
        error = nil
        loading = true
        MainViewControllerKt.AuthSendPhoneOtp(
            phone: phone,
            onSuccess: {
                loading = false
                otpCooldown = 60
                otpHint = "验证码已发送"
            },
            onError: { msg in
                loading = false
                otpCooldown = 0
                error = msg
            }
        )
    }

    private func submit() {
        guard agreedPrivacy else {
            error = "请先阅读并同意隐私条款"
            return
        }
        loading = true
        error = nil
        switch mode {
        case .email:
            MainViewControllerKt.AuthLoginWithPassword(
                account: email,
                password: password,
                onSuccess: {
                    loading = false
                    onSuccess()
                },
                onError: { msg in
                    loading = false
                    error = msg
                }
            )
        case .phone:
            MainViewControllerKt.AuthLoginWithOtp(
                phone: phone,
                code: otp,
                onSuccess: {
                    loading = false
                    onSuccess()
                },
                onError: { msg in
                    loading = false
                    error = msg
                }
            )
        }
    }
}

struct NativeRegisterView: View {
    var onSuccess: () -> Void
    var onCancel: () -> Void

    @State private var mode: AuthCredentialMode = .email
    @State private var email = ""
    @State private var password = ""
    @State private var confirm = ""
    @State private var phone = ""
    @State private var otp = ""
    @State private var agreedPrivacy = true
    @State private var error: String?
    @State private var loading = false
    @State private var otpCooldown = 0

    private var canSubmit: Bool {
        guard agreedPrivacy, !loading else { return false }
        switch mode {
        case .email:
            return email.contains("@") && password.count >= 6 && password == confirm
        case .phone:
            return phone.count == 11 && otp.count >= 4 && password.count >= 6
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("创建账号")
                        .font(.system(size: 32, weight: .bold))
                        .padding(.top, 16)
                    Text("注册后即可使用全部功能")
                        .font(.system(size: 15))
                        .foregroundStyle(AuthTokens.labelSecondary)
                        .padding(.top, 8)

                    HStack(spacing: 0) {
                        segment("邮箱注册", selected: mode == .email) { mode = .email }
                        segment("手机注册", selected: mode == .phone) { mode = .phone }
                    }
                    .padding(4)
                    .background(AuthTokens.fillSecondary, in: RoundedRectangle(cornerRadius: AuthTokens.radiusMd))
                    .padding(.top, 32)

                    VStack(spacing: 16) {
                        if mode == .email {
                            field("邮箱", $email, .emailAddress)
                            field("密码", $password, .default, true)
                            field("确认密码", $confirm, .default, true)
                        } else {
                            HStack(spacing: 12) {
                                Text("+86").font(.system(size: 17, weight: .medium))
                                    .frame(height: AuthTokens.fieldHeight)
                                    .padding(.horizontal, 16)
                                    .background(AuthTokens.surface, in: RoundedRectangle(cornerRadius: AuthTokens.radiusMd))
                                field("手机号", $phone, .phonePad)
                            }
                            HStack(spacing: 12) {
                                field("验证码", $otp, .numberPad)
                                Button(otpCooldown > 0 ? "\(otpCooldown)s" : "获取验证码") {
                                    guard agreedPrivacy else { error = "请先阅读并同意隐私条款"; return }
                                    guard phone.count == 11 else { error = "请输入有效的手机号"; return }
                                    loading = true
                                    MainViewControllerKt.AuthSendPhoneOtp(
                                        phone: phone,
                                        onSuccess: {
                                            loading = false
                                            otpCooldown = 60
                                        },
                                        onError: { msg in
                                            loading = false
                                            error = msg
                                        }
                                    )
                                }
                                .foregroundStyle(AuthTokens.accent)
                                .disabled(otpCooldown > 0 || loading)
                            }
                            field("设置密码", $password, .default, true)
                        }
                    }
                    .padding(.top, 24)

                    if let error {
                        Text(error).font(.system(size: 13)).foregroundStyle(.red).padding(.top, 8)
                    }

                    Button {
                        guard agreedPrivacy else {
                            error = "请先阅读并同意隐私条款"
                            return
                        }
                        loading = true
                        error = nil
                        switch mode {
                        case .email:
                            MainViewControllerKt.AuthRegister(
                                email: email,
                                password: password,
                                displayName: "",
                                onSuccess: {
                                    loading = false
                                    onSuccess()
                                },
                                onError: { msg in
                                    loading = false
                                    error = msg
                                }
                            )
                        case .phone:
                            MainViewControllerKt.AuthRegisterWithPhone(
                                phone: phone,
                                code: otp,
                                onSuccess: {
                                    loading = false
                                    onSuccess()
                                },
                                onError: { msg in
                                    loading = false
                                    error = msg
                                }
                            )
                        }
                    } label: {
                        Text("注册")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: AuthTokens.buttonHeight)
                            .background(canSubmit ? AuthTokens.accent : AuthTokens.buttonDisabled, in: RoundedRectangle(cornerRadius: AuthTokens.radiusLg))
                    }
                    .disabled(!canSubmit)
                    .padding(.top, 32)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
            .background(AuthTokens.background.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", action: onCancel).foregroundStyle(AuthTokens.accent)
                }
                ToolbarItem(placement: .principal) {
                    Text("注册").font(.system(size: 17, weight: .semibold))
                }
            }
            .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
                if otpCooldown > 0 { otpCooldown -= 1 }
            }
        }
    }

    private func segment(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Text(title)
            .font(.system(size: 15, weight: .medium))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(selected ? AuthTokens.surface : Color.clear, in: RoundedRectangle(cornerRadius: 8))
            .onTapGesture(perform: action)
    }

    private func field(_ placeholder: String, _ text: Binding<String>, _ keyboard: UIKeyboardType, _ secure: Bool = false) -> some View {
        Group {
            if secure {
                SecureField(placeholder, text: text)
            } else {
                TextField(placeholder, text: text)
                    .keyboardType(keyboard)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
        }
        .font(.system(size: 17))
        .padding(.horizontal, 16)
        .frame(height: AuthTokens.fieldHeight)
        .background(AuthTokens.surface, in: RoundedRectangle(cornerRadius: AuthTokens.radiusMd))
        .overlay(RoundedRectangle(cornerRadius: AuthTokens.radiusMd).stroke(AuthTokens.separator, lineWidth: 0.5))
    }
}
