package com.example.my_kmp_project.component.pay

/**
 * Android WeChat OpenSDK hook point.
 * Wire WXAPIFactory / PayReq here; until then return null so sandbox can fill.
 */
internal actual fun platformWeChatPayAdapterOrNull(): PayChannelAdapter? {
    // TODO(pay): return WeChatOpenSdkPayAdapter(appId, api) when OpenSDK + keys are configured
    return null
}

/**
 * Android Alipay SDK hook point.
 * Wire PayTask / OrderInfo here; until then return null so sandbox can fill.
 */
internal actual fun platformAlipayPayAdapterOrNull(): PayChannelAdapter? {
    // TODO(pay): return AlipaySdkPayAdapter(...) when Alipay SDK + keys are configured
    return null
}
