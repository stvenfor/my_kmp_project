package com.example.my_kmp_project.core.account

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.remove
import kotlin.concurrent.Volatile

/**
 * OHOS file-backed privacy consent store (posix). multiplatform-settings has no
 * ohosArm64 variant — keep this actual until Preferences/cinterop Settings exists.
 * Path under /tmp until a durable app sandbox path is wired (same as account session).
 */
@OptIn(ExperimentalForeignApi::class)
private class FilePrivacyConsentStore(
    private val path: String = "/tmp/demo_privacy_accepted",
) : PrivacyConsentStore {
    @Volatile
    private var memory: Boolean? = null

    override fun isAccepted(): Boolean {
        memory?.let { return it }
        val file = fopen(path, "r") ?: return false
        return try {
            val buffer = ByteArray(8)
            val read = buffer.usePinned { pinned ->
                fread(pinned.addressOf(0), 1u, buffer.size.toULong(), file).toInt()
            }
            val accepted = read > 0 && buffer.decodeToString(0, read).trim() == "1"
            memory = accepted
            accepted
        } finally {
            fclose(file)
        }
    }

    override fun setAccepted(accepted: Boolean) {
        memory = accepted
        if (!accepted) {
            remove(path)
            return
        }
        val bytes = "1".encodeToByteArray()
        val file = fopen(path, "w") ?: return
        try {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
            }
        } finally {
            fclose(file)
        }
    }
}

public actual fun createPrivacyConsentStore(): PrivacyConsentStore =
    FilePrivacyConsentStore()
