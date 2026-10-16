package com.example.my_kmp_project.component.pay

/** iOS WeChat OpenSDK hook — null until WXApi is wired. */
internal actual fun platformWeChatPayAdapterOrNull(): PayChannelAdapter? = null

/** iOS Alipay SDK hook — null until AlipaySDK is wired. */
internal actual fun platformAlipayPayAdapterOrNull(): PayChannelAdapter? = null
