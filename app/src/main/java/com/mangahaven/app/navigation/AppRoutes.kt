package com.mangahaven.app.navigation

/**
 * 导航路由常量。
 */
object AppRoutes {
    const val LIBRARY = "library"
    const val READER = "reader/{itemId}"
    const val SETTINGS = "settings"

    fun readerRoute(itemId: String): String = "reader/$itemId"
}
