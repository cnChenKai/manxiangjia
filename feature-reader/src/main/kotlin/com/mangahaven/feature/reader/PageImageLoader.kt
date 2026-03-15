package com.mangahaven.feature.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.mangahaven.data.files.PageProvider
import com.mangahaven.reader.crop.WhiteBorderCropper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 原生通过 BitmapFactory.decodeStream 读取流并转为 Compose ImageBitmap。
 * Phase 2 增强：支持白边裁切和双页合并模式。
 */
@Composable
fun rememberPageImage(
    pageProvider: PageProvider?,
    index: Int,
    enableCrop: Boolean = false,
    doublePageMode: Boolean = false,
    isCover: Boolean = false, // 封面或者不想双页的单页
    nextIndex: Int? = null, // 双页模式下的下一页
): ImageBitmap? {
    var imageBitmap by remember(pageProvider, index, enableCrop, doublePageMode, isCover, nextIndex) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(pageProvider, index, enableCrop, doublePageMode, isCover, nextIndex) {
        if (pageProvider == null || index < 0) {
            imageBitmap = null
            return@LaunchedEffect
        }

        imageBitmap = withContext(Dispatchers.IO) {
            try {
                if (doublePageMode && !isCover && nextIndex != null) {
                    // 双页模式 (拼接两页，由于大部分漫画从右边翻开第一页，通常是逆序拼凑，这里按 Right To Left 或 Left To Right?
                    // 标准合并逻辑：左图 + 右图 = 新图
                    val bmp1 = loadSinglePageBitmap(pageProvider, index, enableCrop)
                    val bmp2 = loadSinglePageBitmap(pageProvider, nextIndex, enableCrop)
                    
                    if (bmp1 != null && bmp2 != null) {
                        mergeBitmaps(bmp1, bmp2)?.asImageBitmap() ?: bmp1.asImageBitmap()
                    } else {
                        (bmp1 ?: bmp2)?.asImageBitmap()
                    }
                } else {
                    // 单页模式
                    loadSinglePageBitmap(pageProvider, index, enableCrop)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    return imageBitmap
}

private fun loadSinglePageBitmap(
    provider: PageProvider,
    index: Int,
    enableCrop: Boolean
): Bitmap? {
    return try {
        provider.openPage(index).use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
            if (bitmap != null && enableCrop) {
                // 执行裁切
                val cropped = WhiteBorderCropper.crop(bitmap)
                // 释放原始图防止内存泄漏
                if (cropped !== bitmap) bitmap.recycle()
                cropped
            } else {
                bitmap
            }
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 拼合两张 Bitmap 为一张双页 Bitmap。
 * 假设横屏并排显示，将两张图按最小高度缩放后左右横着拼接。
 */
private fun mergeBitmaps(leftBmp: Bitmap, rightBmp: Bitmap): Bitmap? {
    return try {
        val targetHeight = minOf(leftBmp.height, rightBmp.height)

        // 缩放左图
        val scaleLeft = targetHeight.toFloat() / leftBmp.height
        val newWidthLeft = (leftBmp.width * scaleLeft).toInt()
        val scaledLeft = Bitmap.createScaledBitmap(leftBmp, newWidthLeft, targetHeight, true)

        // 缩放右图
        val scaleRight = targetHeight.toFloat() / rightBmp.height
        val newWidthRight = (rightBmp.width * scaleRight).toInt()
        val scaledRight = Bitmap.createScaledBitmap(rightBmp, newWidthRight, targetHeight, true)

        // 创建拼接的空白画布
        val combinedWidth = newWidthLeft + newWidthRight
        val combinedBmp = Bitmap.createBitmap(combinedWidth, targetHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(combinedBmp)
        // 从左到右画，注意通常右向左漫画的排布是 pageN在右, pageN+1在左，具体在 UI 层通过传入的 bmp 顺序决定
        canvas.drawBitmap(scaledLeft, 0f, 0f, null)
        canvas.drawBitmap(scaledRight, newWidthLeft.toFloat(), 0f, null)

        scaledLeft.recycle()
        scaledRight.recycle()
        
        combinedBmp
    } catch (e: Exception) {
        null
    }
}
