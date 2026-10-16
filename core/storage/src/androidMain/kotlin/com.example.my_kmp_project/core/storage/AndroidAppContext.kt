package com.example.my_kmp_project.core.storage

import android.content.Context
import kotlin.concurrent.Volatile

/**
 * Process-wide Android application context, installed once from the entry Activity before
 * any [KeyValueStore] is created.
 *
 * This used to live in core:account as AndroidAccountContext. Persistence is the lowest
 * consumer of a Context in this project, so the holder sits here and account / platform /
 * feature code reads it from upstream.
 */
public object AndroidAppContext {
    @Volatile
    private var context: Context? = null

    public val applicationContext: Context?
        get() = context

    public fun install(context: Context) {
        this.context = context.applicationContext
    }

    /** Fail fast: a silent no-op store loses session and consent state without a trace. */
    internal fun requireContext(): Context =
        context ?: error("Call AndroidAppContext.install(context) before using KeyValueStore")
}
