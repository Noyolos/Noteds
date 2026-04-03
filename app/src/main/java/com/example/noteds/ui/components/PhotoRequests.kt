package com.example.noteds.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest

@Composable
fun rememberThumbnailImageRequest(
    data: Any?,
    size: Dp = 96.dp
): ImageRequest {
    val context = LocalContext.current
    val sizePx = remember(size, context) { size.roundToPxCompat(context.resources.displayMetrics.density) }

    return remember(context, data, sizePx) {
        ImageRequest.Builder(context)
            .data(data)
            .size(sizePx)
            .crossfade(true)
            .build()
    }
}

private fun Dp.roundToPxCompat(density: Float): Int = (value * density).toInt().coerceAtLeast(1)
