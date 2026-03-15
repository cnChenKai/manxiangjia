package com.mangahaven.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    itemId: String, // unused directly as it's injected via SavedStateHandle into ViewModel
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 沉浸界面的可见性
    var overlayVisible by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val totalPages = uiState.totalPages.coerceAtLeast(1)
    val startIndex = uiState.currentPage.coerceIn(0, totalPages - 1)
    
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { totalPages }
    )

    // 同步翻页进度到 ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    // 主阅读器背景 (暗色或纯黑增加沉浸感)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.totalPages > 0) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 点击屏幕唤出/隐藏菜单
                        overlayVisible = !overlayVisible
                    }
            ) { page ->
                // Page Content
                val imageBitmap = rememberPageImage(pageProvider = uiState.pageProvider, index = page)
                
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Page $page",
                        modifier = Modifier.fillMaxSize(),
                        // 这里默认 Fit，后续可加入手势缩放模块
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.DarkGray)
                    }
                }
            }
        } else {
            // Empty State
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无漫画内容", color = Color.White)
            }
        }

        // --- Top Overlay Bar ---
        AnimatedVisibility(
            visible = overlayVisible,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.book?.title ?: "未知漫画",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Top actions */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            )
        }

        // --- Bottom Overlay Bar ---
        AnimatedVisibility(
            visible = overlayVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 页码和进度条
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${pagerState.currentPage + 1}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        
                        // 由于 Pager 的 page 是基于 Slider 控制的，由于 Compose Slider 问题，我们需要对状态转换
                        Slider(
                            value = pagerState.currentPage.toFloat(),
                            onValueChange = { /* 拖动期间预留动作，如不实时跳转可使用 onValueChangeFinished */ },
                            onValueChangeFinished = { /* 当手指抬起时同步 pager */ },
                            valueRange = 0f..((uiState.totalPages - 1).coerceAtLeast(1).toFloat()),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            enabled = false // 目前仅用于展示进度，禁止拖拉直接翻页防止复杂同步问题
                        )
                        Text("${uiState.totalPages}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }

                    // 底部控制按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { /* TODO: Switch Mode */ }) {
                            Text("阅读设置", color = Color.White)
                        }
                        IconButton(onClick = { /* TODO: Settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
