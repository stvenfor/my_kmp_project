package com.example.my_kmp_project.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal actual fun PlatformNetworkImage(
    url: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholder: DrawableResource,
    contentDescription: String?,
) {
    val placeholderPainter = painterResource(placeholder)
    if (url.isNullOrBlank()) {
        Box(modifier = modifier.background(Color(0xFFF0F0F0))) {
            Image(
                painter = placeholderPainter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
        return
    }
    AsyncImage(
        model = secureNetworkImageUrl(url),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholderPainter,
        error = placeholderPainter,
    )
}
