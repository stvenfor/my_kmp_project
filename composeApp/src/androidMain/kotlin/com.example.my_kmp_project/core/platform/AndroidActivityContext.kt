package com.example.my_kmp_project.core.platform

import android.app.Activity
import kotlin.concurrent.Volatile

/** Holds the foreground Activity for SDK calls that require Activity context (pay). */
internal object AndroidActivityContext {
    @Volatile
    var currentActivity: Activity? = null

    fun install(activity: Activity) {
        currentActivity = activity
    }

    fun clear(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }
}
