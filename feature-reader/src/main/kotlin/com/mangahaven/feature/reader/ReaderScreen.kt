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
import androidx.compose.foundation.pager.VerticalPager
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
import com.mangahaven.feature.reader.components.ReaderSettingsSheet
import com.mangahaven.feature.reader.components.ThumbnailStrip
import com.mangahaven.model.ReadingMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    itemId: String, // unused directly as it's injected via SavedStateHandle into ViewModel
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    var overlayVisible by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (uiState.isLoading || settings == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val totalPages = uiState.totalPages.coerceAtLeast(1)
    // 简单计算：如果双页且非垂直模式（通常垂直模式不双页），页数大致减半。细节处理如封面单跨这里简化为全双拼
    val sheetsCount = if (settings.doublePageMode && settings.readingMode != ReadingMode.VERTICAL) {
        (totalPages + 1) / 2
    } else {
        totalPages
    }

    // 将逻辑页转换为实际源文件索引数组
    val getSourceIndicesForSheet: (Int) -> List<Int> = { sheetIndex ->
        if (settings.doublePageMode && settings.readingMode != ReadingMode.VERTICAL) {
            val baseIndex = sheetIndex * 2 + settings.pageOffset
            val p1 = baseIndex.coerceIn(0, totalPages - 1)
            val p2 = (baseIndex + 1).coerceIn(0, totalPages - 1)
            if (p1 == p2) listOf(p1) else listOf(p1, p2)
        } else {
            listOf((sheetIndex + settings.pageOffset).coerceIn(0, totalPages - 1))
        }
    }

    val startIndex = if (settings.doublePageMode && settings.readingMode != ReadingMode.VERTICAL) {
        uiState.currentPage / 2
    } else {
        uiState.currentPage
    }.coerceIn(0, (sheetsCount - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { sheetsCount }
    )

    // 反向同步 Pager to ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val indices = getSourceIndicesForSheet(pagerState.currentPage)
        // 使用该展示面包含的第一个索引作为当前进度保存
        viewModel.onPageChanged(indices.firstOrNull() ?: 0)
    }

    // 主阅读器背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.totalPages > 0) {
            val modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    overlayVisible = !overlayVisible
                }

            when (settings.readingMode) {
                ReadingMode.LEFT_TO_RIGHT -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = modifier,
                        reverseLayout = false
                    ) { sheet ->
                        ReaderPageContent(uiState, settings, getSourceIndicesForSheet(sheet))
                    }
                }
                ReadingMode.RIGHT_TO_LEFT -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = modifier,
                        reverseLayout = true // 反向布局完美实现右向左阅读
                    ) { sheet ->
                        ReaderPageContent(uiState, settings, getSourceIndicesForSheet(sheet))
                    }
                }
                ReadingMode.VERTICAL -> {
                    VerticalPager(
                        state = pagerState,
                        modifier = modifier
                    ) { sheet ->
                        ReaderPageContent(uiState, settings, getSourceIndicesForSheet(sheet))
                    }
                }
            }
        } else {
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
                    Text(
                        text = uiState.book?.title ?: "未知漫画",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
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
                Column {
                    // 缩略图条 (由 ViewModel 提供的 generator 生成缩略图)
                    ThumbnailStrip(
                        itemId = uiState.book?.id ?: "",
                        totalPages = uiState.totalPages,
                        currentPage = uiState.currentPage,
                        thumbnailGenerator = viewModel.thumbnailGenerator,
                        pageProvider = uiState.pageProvider,
                        onPageSelected = { targetPage ->
                            coroutineScope.launch {
                                // 这里简化跳页逻辑到对应 sheet
                                val targetSheet = if (settings.doublePageMode && settings.readingMode != ReadingMode.VERTICAL) {
                                    targetPage / 2
                                } else {
                                    targetPage
                                }
                                pagerState.scrollToPage(targetSheet.coerceIn(0, sheetsCount - 1))
                                viewModel.onPageChanged(targetPage)
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${uiState.currentPage + 1}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        val slideValue = if (sheetsCount > 1) {
                            (pagerState.currentPage.toFloat() / (sheetsCount - 1).toFloat()) * (uiState.totalPages - 1)
                        } else 0f
                        Slider(
                            value = slideValue,
                            onValueChange = {},
                            valueRange = 0f..((uiState.totalPages - 1).coerceAtLeast(1).toFloat()),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            enabled = false
                        )
                        Text("${uiState.totalPages}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("阅读设置", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    ReaderSettingsSheet(
        settings = settings,
        isVisible = showSettingsSheet,
        onVisibleChange = { showSettingsSheet = it },
        onReadingModeChange = viewModel::updateReadingMode,
        onCropChange = viewModel::toggleCrop,
        onDoublePageChange = viewModel::toggleDoublePage,
        onPageOffsetChange = viewModel::updatePageOffset,
        onResetToGlobal = viewModel::resetToGlobalSettings
    )
}

@Composable
private fun ReaderPageContent(
    uiState: ReaderUiState,
    settings: ResolvedReaderSettings,
    indices: List<Int>
) {
    if (indices.isEmpty()) return

    val p1 = indices[0]
    val p2 = if (indices.size > 1) indices[1] else null

    val isDoublePage = p2 != null

    val imageBitmap = rememberPageImage(
        pageProvider = uiState.pageProvider,
        index = p1,
        enableCrop = settings.enableCrop,
        doublePageMode = isDoublePage,
        isCover = false,
        nextIndex = p2
    )

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            // 垂直模式需要占据最大宽度高度支持下拉连续。横向模式自适应缩放
            contentDescription = "Page(s)",
            modifier = Modifier.fillMaxSize(),
            contentScale = if (settings.readingMode == ReadingMode.VERTICAL) ContentScale.FillWidth else ContentScale.Fit
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.DarkGray)
        }
    }
}
