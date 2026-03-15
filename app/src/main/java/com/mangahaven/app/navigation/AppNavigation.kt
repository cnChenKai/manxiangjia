package com.mangahaven.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mangahaven.feature.library.LibraryScreen
import com.mangahaven.feature.reader.ReaderScreen
import com.mangahaven.feature.settings.SettingsScreen

/**
 * 应用导航图。
 * 添加了平滑的页面切换过渡动画。
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LIBRARY,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        // 书架页
        composable(
            route = AppRoutes.LIBRARY,
            enterTransition = { null }, // 首页没有进入退出动画
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            LibraryScreen(
                onNavigateToReader = { itemId ->
                    navController.navigate(AppRoutes.readerRoute(itemId))
                },
                onNavigateToSettings = {
                    navController.navigate(AppRoutes.SETTINGS)
                },
            )
        }

        // 阅读器页 (沉浸式体验，可以使用淡入淡出或者从底部弹出，为了连贯用统一左右滑)
        composable(
            route = AppRoutes.READER,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            ReaderScreen(
                itemId = itemId,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // 设置页
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
