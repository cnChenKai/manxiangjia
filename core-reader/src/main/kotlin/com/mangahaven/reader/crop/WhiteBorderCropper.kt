package com.mangahaven.reader.crop

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import timber.log.Timber

/**
 * 白边裁切工具。
 * 分析图片四边的白色/近白色像素，返回裁切后的 Bitmap。
 */
object WhiteBorderCropper {

    /**
     * 默认亮度阈值。像素亮度 >= 此值被视为白色。
     * 范围 0~255，240 对大多数漫画扫描件效果良好。
     */
    private const val DEFAULT_BRIGHTNESS_THRESHOLD = 240

    /**
     * 最小采样间隔（避免逐像素扫描太慢）。
     */
    private const val SAMPLE_STEP = 4

    /**
     * 最大裁切比例。超过此比例视为异常图片，直接返回原图。
     */
    private const val MAX_CROP_RATIO = 0.35f

    /**
     * 对 Bitmap 进行白边裁切。
     * @param bitmap 原始 Bitmap
     * @param threshold 亮度阈值，默认 240
     * @return 裁切后的 Bitmap，异常时返回原图
     */
    fun crop(
        bitmap: Bitmap,
        threshold: Int = DEFAULT_BRIGHTNESS_THRESHOLD,
    ): Bitmap {
        return try {
            val rect = detectBorders(bitmap, threshold)
            if (rect == null) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
            }
        } catch (e: Exception) {
            Timber.e(e, "White border cropping failed, returning original")
            bitmap
        }
    }

    /**
     * 检测内容区域的 Rect。
     * @return 内容区域，如果裁切量过大则返回 null（视为异常图片）
     */
    fun detectBorders(
        bitmap: Bitmap,
        threshold: Int = DEFAULT_BRIGHTNESS_THRESHOLD,
    ): Rect? {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 10 || h < 10) return null

        val top = findTopBorder(bitmap, w, h, threshold)
        val bottom = findBottomBorder(bitmap, w, h, threshold)
        val left = findLeftBorder(bitmap, w, h, threshold)
        val right = findRightBorder(bitmap, w, h, threshold)

        // 验证裁切是否合理
        val cropWidth = left + (w - right)
        val cropHeight = top + (h - bottom)
        if (cropWidth.toFloat() / w > MAX_CROP_RATIO || cropHeight.toFloat() / h > MAX_CROP_RATIO) {
            Timber.d("Crop ratio too high (${cropWidth}/$w, ${cropHeight}/$h), skipping")
            return null
        }

        if (right <= left || bottom <= top) return null

        return Rect(left, top, right, bottom)
    }

    private fun findTopBorder(bitmap: Bitmap, w: Int, h: Int, threshold: Int): Int {
        for (y in 0 until h step SAMPLE_STEP) {
            if (!isRowWhite(bitmap, y, w, threshold)) return y
        }
        return 0
    }

    private fun findBottomBorder(bitmap: Bitmap, w: Int, h: Int, threshold: Int): Int {
        for (y in (h - 1) downTo 0 step SAMPLE_STEP) {
            if (!isRowWhite(bitmap, y, w, threshold)) return y + 1
        }
        return h
    }

    private fun findLeftBorder(bitmap: Bitmap, w: Int, h: Int, threshold: Int): Int {
        for (x in 0 until w step SAMPLE_STEP) {
            if (!isColumnWhite(bitmap, x, h, threshold)) return x
        }
        return 0
    }

    private fun findRightBorder(bitmap: Bitmap, w: Int, h: Int, threshold: Int): Int {
        for (x in (w - 1) downTo 0 step SAMPLE_STEP) {
            if (!isColumnWhite(bitmap, x, h, threshold)) return x + 1
        }
        return w
    }

    private fun isRowWhite(bitmap: Bitmap, y: Int, w: Int, threshold: Int): Boolean {
        for (x in 0 until w step SAMPLE_STEP) {
            if (!isPixelWhite(bitmap.getPixel(x, y), threshold)) return false
        }
        return true
    }

    private fun isColumnWhite(bitmap: Bitmap, x: Int, h: Int, threshold: Int): Boolean {
        for (y in 0 until h step SAMPLE_STEP) {
            if (!isPixelWhite(bitmap.getPixel(x, y), threshold)) return false
        }
        return true
    }

    private fun isPixelWhite(pixel: Int, threshold: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return r >= threshold && g >= threshold && b >= threshold
    }
}
