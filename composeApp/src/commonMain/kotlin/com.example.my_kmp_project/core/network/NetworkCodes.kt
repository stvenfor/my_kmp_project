package com.example.my_kmp_project.core.network

/** Common API / business result codes for the demo network layer. */
internal object NetworkCodes {
    const val NOT_NETWORK = 0
    const val OK = 200
    const val INVALID_AUTH = 100
    const val INVALID_MEMBER = 301
    const val EXPIRE_TOKEN = 401
    const val BUSY = 429
    const val INVALID_ACCOUNT = 500
    const val PASSTHROUGH = -1
}
