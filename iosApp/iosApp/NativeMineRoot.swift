import SwiftUI

// MARK: - Mine root (native; island is Compose)

struct MineRootView: View {
    var isLoggedIn: Bool
    var onLogin: () -> Void
    var onLogout: () -> Void
    var onOpenSettings: () -> Void
    var onOpenPersonalized: () -> Void
    var onDeferred: (String) -> Void

    private let services = ["商城", "我的钱包", "我的课程", "我的订单"]
    private let functions: [(String, String, String?)] = [
        ("短信模板", "一键发送 轻松快捷", nil),
        ("购车计算器", "全款/贷款/保险全能算", "5830.00"),
        ("二手车", "置换/专卖/估价", nil),
        ("小视频", "用小视频秀车秀店", nil),
        ("售后专区", "售后维修保养记录", nil),
        ("店铺收款码", "常见问题 功能介绍", nil),
        ("选买问答", "在线解答客户问题", nil),
        ("商家海报", "置换/专卖/估价", nil),
    ]
    private let menu = ["商务合作", "提醒事项", "邀请好友", "粉丝群", "意见反馈", "设置"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("我的", font: .system(size: 32, weight: .bold), color: DesignTokens.ink)
                    Spacer()
                    Button(action: onOpenPersonalized) { Image(systemName: "info.circle").foregroundStyle(DesignTokens.link) }
                    Button(action: onOpenSettings) { Image(systemName: "gearshape").foregroundStyle(DesignTokens.link) }
                    Button(action: isLoggedIn ? onLogout : onLogin) {
                        Image(systemName: isLoggedIn ? "rectangle.portrait.and.arrow.right" : "person.badge.key")
                            .foregroundStyle(DesignTokens.link)
                    }
                }
                profileCard
                statsRow
                Text("常用服务", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                HStack {
                    ForEach(services, id: \.self) { label in
                        VStack(spacing: 8) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(DesignTokens.link.opacity(0.12))
                                .frame(width: 44, height: 44)
                            Text(label, font: .system(size: 12), color: DesignTokens.ink).lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                        .onTapGesture { onDeferred(label) }
                    }
                }
                Text("个人功能", font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(functions, id: \.0) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(DesignTokens.link.opacity(0.08))
                                .frame(width: 44, height: 44)
                            Spacer(minLength: 8)
                            Text(item.0, font: .system(size: 16, weight: .semibold), color: DesignTokens.ink)
                            Text(item.1, font: .system(size: 12), color: DesignTokens.body).lineLimit(2)
                            if let value = item.2 {
                                Text(value, font: .system(size: 16, weight: .bold), color: DesignTokens.link)
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, minHeight: 140, alignment: .leading)
                        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DesignTokens.hairline, lineWidth: 0.5))
                    }
                }
                VStack(spacing: 0) {
                    ForEach(menu, id: \.self) { label in
                        Button {
                            if label == "设置" { onOpenSettings() }
                            else if label == "粉丝群" { onDeferred("短视频") }
                            else { onDeferred(label) }
                        } label: {
                            HStack {
                                Text(label, color: DesignTokens.ink)
                                Spacer()
                                Image(systemName: "chevron.right").foregroundStyle(DesignTokens.body)
                            }
                            .padding(16)
                        }
                        .buttonStyle(.plain)
                        Divider().overlay(DesignTokens.hairline)
                    }
                }
                .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
            }
            .padding(16)
        }
        .background(DesignTokens.canvasSoft2)
    }

    private var profileCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(isLoggedIn ? "用户0000" : "访客", font: .system(size: 20, weight: .semibold), color: DesignTokens.ink)
            Text(isLoggedIn ? "销售经理" : "未登录", font: .system(size: 12), color: DesignTokens.link)
            Text(isLoggedIn ? "[4S]北京大兴兴荣丰田汽车销售服务有限公司" : "登录后查看门店信息", font: .system(size: 12), color: DesignTokens.body)
            if !isLoggedIn {
                Button("登录", action: onLogin)
                    .buttonStyle(.borderedProminent)
                    .tint(DesignTokens.link)
            } else {
                Text("138****0000", font: .system(size: 12), color: DesignTokens.body)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }

    private var statsRow: some View {
        HStack {
            ForEach([("1028", "加入天数"), ("28", "员工数"), ("2059", "店铺天数"), ("9366", "累计客户")], id: \.1) { item in
                VStack(spacing: 6) {
                    Text(isLoggedIn ? item.0 : "0", font: .system(size: 18, weight: .bold), color: DesignTokens.ink)
                    Text(item.1, font: .system(size: 12), color: DesignTokens.body)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 20)
        .background(DesignTokens.canvas, in: RoundedRectangle(cornerRadius: 12))
    }
}
