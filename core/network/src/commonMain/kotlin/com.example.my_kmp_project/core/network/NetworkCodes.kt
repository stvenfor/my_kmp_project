package com.example.my_kmp_project.core.network

/**
 * Common API / business result codes for the demo network layer.
 *
 * 两套码表并存（对齐 Flutter，勿合并）：
 * - Go BFF `/api/v1` 信封：成功码 **0**（Dart `ResultModel.isSuccess`，result_model.dart:28）
 * - 遗留 iOS 对齐表 `commons/wys_network/lib/src/network_codes.dart`：`ok = 200`、`0 = 无网络`
 *
 * 因此 `0` 同值不同义：解析器在**缺 `code`** 时回退 [PASSTHROUGH]，绝不回退到 0，
 * 否则畸形响应会被 [ApiResponse.isSuccess] 误判为成功。
 */
public object NetworkCodes {
    /** Go BFF `/api/v1` 业务成功码（`code == 0`）。 */
    const val BFF_OK = 0

    const val NOT_NETWORK = 0
    const val OK = 200
    const val INVALID_AUTH = 100
    const val INVALID_MEMBER = 301
    const val EXPIRE_TOKEN = 401

    /**
     * 无权限（Flutter `NetworkCodes.forbidden`，d7876a1）。
     * HTTP 403 与业务 code 403 均映射至此：**不**触发登出 / token 刷新，
     * 由业务自行提示（镜像 `forbidden_status_test.dart`）。
     */
    const val FORBIDDEN = 403

    const val BUSY = 429
    const val INVALID_ACCOUNT = 500
    const val PASSTHROUGH = -1
}
