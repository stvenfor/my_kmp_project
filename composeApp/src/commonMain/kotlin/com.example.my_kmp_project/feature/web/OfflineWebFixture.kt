package com.example.my_kmp_project.feature.web

/**
 * Offline-capable Home Web entry for CI / emulator without outbound net.
 * Percent-encoded `data:` URL keeps Chinese title/body visible without network.
 */
internal const val OfflineWebFixtureUrl =
    "data:text/html;charset=utf-8," +
        "%3C!DOCTYPE%20html%3E" +
        "%3Chtml%3E%3Chead%3E" +
        "%3Cmeta%20charset%3D%22utf-8%22%2F%3E" +
        "%3Cmeta%20name%3D%22viewport%22%20content%3D%22width%3Ddevice-width%2Cinitial-scale%3D1%22%2F%3E" +
        "%3Ctitle%3E%E7%BD%91%E9%A1%B5%E6%B5%8B%E8%AF%95%3C%2Ftitle%3E" +
        "%3Cstyle%3Ebody%7Bfont-family%3Asans-serif%3Bpadding%3A24px%3Bbackground%3A%23f5f5f5%3Bcolor%3A%23333%7D" +
        "h1%7Bfont-size%3A22px%3Bmargin%3A0%200%2012px%7Dp%7Bfont-size%3A15px%3Bline-height%3A1.5%7D%3C%2Fstyle%3E" +
        "%3C%2Fhead%3E%3Cbody%3E" +
        "%3Ch1%3E%E7%A6%BB%E7%BA%BF%E7%BD%91%E9%A1%B5%3C%2Fh1%3E" +
        "%3Cp%3E%E6%97%A0%E7%BD%91%E7%BB%9C%E4%B9%9F%E5%8F%AF%E6%98%BE%E7%A4%BA%E7%9A%84%E5%9B%BA%E5%AE%9A%E9%A1%B5%E9%9D%A2%EF%BC%88WebView%20offline%20fixture%EF%BC%89%3C%2Fp%3E" +
        "%3C%2Fbody%3E%3C%2Fhtml%3E"
