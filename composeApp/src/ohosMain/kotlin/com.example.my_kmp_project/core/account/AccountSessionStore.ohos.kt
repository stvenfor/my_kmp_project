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
 * OHOS file-backed session store (posix). multiplatform-settings has no ohosArm64
 * variant — keep this actual until Preferences/cinterop Settings exists.
 * Path under /tmp until a durable app sandbox path is wired.
 */
@OptIn(ExperimentalForeignApi::class)
private class FileAccountSessionStore(
    private val path: String = "/tmp/demo_account_user_json",
) : AccountSessionStore {
    @Volatile
    private var memory: String? = null

    override fun readUserJson(): String? {
        memory?.let { return it }
        val file = fopen(path, "r") ?: return null
        return try {
            val buffer = ByteArray(256 * 1024)
            val read = buffer.usePinned { pinned ->
                fread(pinned.addressOf(0), 1u, buffer.size.toULong(), file).toInt()
            }
            if (read <= 0) null
            else buffer.decodeToString(0, read).also { memory = it }
        } finally {
            fclose(file)
        }
    }

    override fun writeUserJson(json: String?) {
        val value = json?.takeIf { it.isNotBlank() }
        memory = value
        if (value == null) {
            remove(path)
            return
        }
        val bytes = value.encodeToByteArray()
        val file = fopen(path, "w") ?: return
        try {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
            }
        } finally {
            fclose(file)
        }
    }

    override fun clear() {
        memory = null
        remove(path)
    }
}

internal actual fun createAccountSessionStore(): AccountSessionStore =
    FileAccountSessionStore()
