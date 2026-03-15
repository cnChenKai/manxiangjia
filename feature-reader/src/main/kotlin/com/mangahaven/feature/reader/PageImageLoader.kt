package com.mangahaven.feature.reader

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.mangahaven.data.files.PageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 这是一个简易的基于 PageProvider 的 Compose 图片加载组件。
 * Phase 1 为了快速且稳定地读取本地流而设计，未来可替换为 Coil 的定制 Fetcher。
 */
@Composable
fun rememberPageImage(
    pageProvider: PageProvider?,
    index: Int
): ImageBitmap? {
    var result by remember(pageProvider, index) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(pageProvider, index) {
        if (pageProvider == null || index < 0) return@LaunchedEffect
        
        withContext(Dispatchers.IO) {
            try {
                pageProvider.openPage(index).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        result = bitmap.asImageBitmap()
                    } else {
                        Timber.e("Failed to decode image at index $index")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error opening page index $index")
            }
        }
    }

    return result
}
