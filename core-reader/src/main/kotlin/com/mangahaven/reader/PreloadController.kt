package com.mangahaven.reader

/**
 * 预加载控制器接口。
 * 控制相邻页面的预取策略。
 */
interface PreloadController {

    /**
     * 通知当前页码变化，触发预加载。
     * @param currentPage 当前页码（0-indexed）
     * @param totalPages 总页数
     */
    suspend fun onPageChanged(currentPage: Int, totalPages: Int)

    /**
     * 取消所有正在进行的预加载。
     */
    suspend fun cancelAll()
}
