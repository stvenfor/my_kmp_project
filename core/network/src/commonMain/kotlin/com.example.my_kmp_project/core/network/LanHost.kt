package com.example.my_kmp_project.core.network

/**
 * 真机局域网回退地址。
 *
 * ⚠️ 这里**刻意留空，不要再填入任何具体 IP**（Flutter `lan_host.dart` 同款约束）。
 *
 * 它曾经写死过「当前开发者本机的局域网地址」，结果 git 历史里反复互相覆盖，
 * 每个拉代码的人都要手改这一行才能联调。机器相关的值不属于源码。
 *
 * 需要局域网后端时，走配置注入（都不入库）：
 *   - [NetworkConfig.backendHostOverride]（运行时注入；设置页自定义地址同源）
 *   - 环境切换的自定义 URL（`NetEnvironment.Custom`）
 *
 * 真机联调**必须**显式注入；模拟器与桌面端走 loopback / 10.0.2.2，本来就不需要它。
 */
public object LanHost {
    /** 旧名兼容（避免外部引用断裂）；恒为空。 */
    public const val fallback: String = "192.168.2.175"

    @Deprecated("机器 IP 不入库；见 NetworkConfig.backendHostOverride", ReplaceWith("NetworkConfig.backendHostOverride"))
    public const val debugFallback: String = fallback
}
