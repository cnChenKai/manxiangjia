package com.mangahaven.data.files.container

/**
 * 图片文件工具类。
 */
object ImageFileUtils {

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "avif"
    )

    private val IGNORED_ENTRIES = setOf(
        "__MACOSX", ".DS_Store", "Thumbs.db", "._.DS_Store", "desktop.ini"
    )

    /**
     * 判断文件名是否为图片文件。
     */
    fun isImageFile(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }

    /**
     * 判断条目是否应该被忽略。
     */
    fun shouldIgnore(path: String): Boolean {
        return IGNORED_ENTRIES.any { ignored ->
            path.contains(ignored, ignoreCase = true)
        }
    }

    /**
     * 获取 MIME 类型。
     */
    fun getMimeType(name: String): String? {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "avif" -> "image/avif"
            else -> null
        }
    }

    /**
     * 判断是否为归档压缩包文件。
     */
    fun isArchive(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in setOf("zip", "cbz", "rar", "cbr", "epub", "mobi")
    }

    /**
     * 自然排序比较器。
     * 支持文件名中数字的自然排序（如 page2.jpg < page10.jpg）。
     */
    val naturalOrderComparator: Comparator<String> = Comparator { a, b ->
        naturalCompare(a, b)
    }

    private fun naturalCompare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                // 比较数字块
                var numA = 0L
                while (i < a.length && a[i].isDigit()) {
                    numA = numA * 10 + (a[i] - '0')
                    i++
                }
                var numB = 0L
                while (j < b.length && b[j].isDigit()) {
                    numB = numB * 10 + (b[j] - '0')
                    j++
                }
                val cmp = numA.compareTo(numB)
                if (cmp != 0) return cmp
            } else {
                val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (cmp != 0) return cmp
                i++
                j++
            }
        }
        return a.length - b.length
    }
}
