package com.example.my_kmp_project.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource

/** Upgrade cleartext image URLs so iOS ATS can load them. */
internal fun secureNetworkImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return url
    return when {
        url.startsWith("https://", ignoreCase = true) -> url
        url.startsWith("http://", ignoreCase = true) -> "https://${url.substring(7)}"
        else -> url
    }
}

/**
 * Shared network-image API for feature UI.
 * Android / iOS / ohosArm64: CPF Coil via imageCoilMain; ohosX64: local placeholder.
 */
@Composable
internal expect fun PlatformNetworkImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: DrawableResource,
    contentDescription: String? = null,
)
