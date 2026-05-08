package com.mangahaven.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mangahaven.model.TapZoneProfile
import com.mangahaven.data.local.repository.ResolvedReaderSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 点击区域动作类型。
 */
private enum class TapAction {
    PREVIOUS,       // 上一页
    NEXT,           // 下一页
    TOGGLE_OVERLAY, // 切换覆盖层
}

/**
 * 根据点击区域配置和点击位置判断应执行的动作。
 * @param normalizedX 归一化的 X 坐标 (0.0 ~ 1.0)
 * @param normalizedY 归一化的 Y 坐标 (0.0 ~ 1.0)
 */
private fun resolveTapAction(
    profile: TapZoneProfile,
    normalizedX: Float,
    normalizedY: Float,
): TapAction {
    return when (profile) {
        TapZoneProfile.DEFAULT -> {
            // 默认：左边缘上一页，右边缘下一页，中间切换覆盖层
            when {
                normalizedX < 0.3f -> TapAction.PREVIOUS
                normalizedX > 0.7f -> TapAction.NEXT
                else -> TapAction.TOGGLE_OVERLAY
            }
        }
        TapZoneProfile.LEFT_RIGHT -> {
            // 左右分区：左半边上一页，右半边下一页，中间窄条切换覆盖层
            when {
                normalizedX < 0.4f -> TapAction.PREVIOUS
                normalizedX > 0.6f -> TapAction.NEXT
                else -> TapAction.TOGGLE_OVERLAY
            }
        }
        TapZoneProfile.L_SHAPE -> {
            // L 形区域：左侧上一页，右下角下一页，中间切换覆盖层
            when {
                normalizedX < 0.3f -> TapAction.PREVIOUS
                normalizedX > 0.7f && normalizedY > 0.7f -> TapAction.NEXT
                else -> TapAction.TOGGLE_OVERLAY
            }
        }
        TapZoneProfile.KINDLE_STYLE -> {
            // Kindle 风格：左边缘上一页，右边缘下一页，中间切换覆盖层
            when {
                normalizedX < 0.2f -> TapAction.PREVIOUS
                normalizedX > 0.8f -> TapAction.NEXT
                else -> TapAction.TOGGLE_OVERLAY
            }
        }
    }
}

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
    val focusRequester = remember { FocusRequester() }

    // 缩放状态：scale, offsetX, offsetY
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 滑块拖拽相关状态
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }
    var sliderTooltipPage by remember { mutableIntStateOf(0) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        // 请求焦点以接收按键事件
        focusRequester.requestFocus()
    }

    if (uiState.isLoading || settings == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val totalPages = uiState.totalPages.coerceAtLeast(1)
    // 简单计算：如果双页且非垂直模式（通常垂直模式不双页），页数大致减半。细节处理如封面单跨这里简化为全双拼
    val isContinuousMode = settings.readingMode == ReadingMode.CONTINUOUS_VERTICAL
    val sheetsCount = if (settings.doublePageMode && !isContinuousMode && settings.readingMode != ReadingMode.VERTICAL) {
        (totalPages + 1) / 2
    } else {
        totalPages
    }

    // 将逻辑页转换为实际源文件索引数组
    val getSourceIndicesForSheet: (Int) -> List<Int> = { sheetIndex ->
        if (settings.doublePageMode && !isContinuousMode && settings.readingMode != ReadingMode.VERTICAL) {
            val baseIndex = sheetIndex * 2 + settings.pageOffset
            val p1 = baseIndex.coerceIn(0, totalPages - 1)
            val p2 = (baseIndex + 1).coerceIn(0, totalPages - 1)
            if (p1 == p2) listOf(p1) else listOf(p1, p2)
        } else {
            listOf((sheetIndex + settings.pageOffset).coerceIn(0, totalPages - 1))
        }
    }

    val startIndex = if (settings.doublePageMode && !isContinuousMode && settings.readingMode != ReadingMode.VERTICAL) {
        uiState.currentPage / 2
    } else {
        uiState.currentPage
    }.coerceIn(0, (sheetsCount - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { sheetsCount }
    )

    // 翻页时重置缩放状态
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // 反向同步 Pager to ViewModel（非连续模式）
    LaunchedEffect(pagerState.currentPage) {
        if (!isContinuousMode) {
            val indices = getSourceIndicesForSheet(pagerState.currentPage)
            viewModel.onPageChanged(indices.firstOrNull() ?: 0)
        }
    }

    // 同步滑块值到当前页
    LaunchedEffect(uiState.currentPage, isSliderDragging) {
        if (!isSliderDragging) {
            sliderValue = uiState.currentPage.toFloat()
        }
    }

    // 缩放状态：支持双指缩放和平移
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        // 只有缩放比例大于 1 时才允许平移
        if (newScale > 1f) {
            scale = newScale
            // 计算最大平移范围
            val maxOffsetX = (newScale - 1f) * 500f // 近似值，实际由容器大小决定
            val maxOffsetY = (newScale - 1f) * 500f
            offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = (offsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    // 主阅读器背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (!settings.volumeKeysPaging) return@onKeyEvent false

                // 统一处理按下和抬起，防止系统二次处理
                val isDown = event.type == KeyEventType.KeyDown
                val isUp = event.type == KeyEventType.KeyUp

                if (isDown) {
                    when (event.key) {
                        Key.VolumeDown, Key.DirectionDown, Key.DirectionRight, Key.PageDown, Key.Spacebar -> {
                            if (!isContinuousMode) {
                                coroutineScope.launch {
                                    val nextPage = (pagerState.currentPage + 1).coerceAtMost(sheetsCount - 1)
                                    pagerState.animateScrollToPage(nextPage)
                                }
                            }
                            true
                        }
                        Key.VolumeUp, Key.DirectionUp, Key.DirectionLeft, Key.PageUp -> {
                            if (!isContinuousMode) {
                                coroutineScope.launch {
                                    val prevPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(prevPage)
                                }
                            }
                            true
                        }
                        else -> false
                    }
                } else if (isUp) {
                    when (event.key) {
                        Key.VolumeDown, Key.DirectionDown, Key.DirectionRight, Key.PageDown, Key.Spacebar,
                        Key.VolumeUp, Key.DirectionUp, Key.DirectionLeft, Key.PageUp -> true
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        if (uiState.totalPages > 0) {
            when {
                // 连续滚动模式
                isContinuousMode -> {
                    ContinuousVerticalReader(
                        uiState = uiState,
                        settings = settings,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        transformableState = transformableState,
                        onTap = { normalizedX, normalizedY ->
                            val action = resolveTapAction(settings.tapZoneProfile, normalizedX, normalizedY)
                            when (action) {
                                TapAction.TOGGLE_OVERLAY -> overlayVisible = !overlayVisible
                                TapAction.PREVIOUS, TapAction.NEXT -> {
                                    // 连续模式下点击前后不做操作（由滚动实现）
                                    overlayVisible = !overlayVisible
                                }
                            }
                        },
                        onDoubleTap = {
                            // 双击切换 1x/2x 缩放
                            if (abs(scale - 1f) < 0.01f) {
                                scale = 2f
                            } else {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                        onPageChanged = { page -> viewModel.onPageChanged(page) },
                        onScaleChanged = { newScale -> scale = newScale },
                        onOffsetChanged = { x, y -> offsetX = x; offsetY = y }
                    )
                }
                // 分页模式（左到右、右到左、上下翻页）
                else -> {
                    // 构建点击区域手势修饰符
                    val tapModifier = Modifier
                        .fillMaxSize()
                        .pointerInput(settings.tapZoneProfile) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val normalizedX = offset.x / size.width
                                    val normalizedY = offset.y / size.height
                                    val action = resolveTapAction(settings.tapZoneProfile, normalizedX, normalizedY)
                                    when (action) {
                                        TapAction.TOGGLE_OVERLAY -> overlayVisible = !overlayVisible
                                        TapAction.NEXT -> {
                                            coroutineScope.launch {
                                                val nextPage = (pagerState.currentPage + 1).coerceAtMost(sheetsCount - 1)
                                                pagerState.animateScrollToPage(nextPage)
                                            }
                                        }
                                        TapAction.PREVIOUS -> {
                                            coroutineScope.launch {
                                                val prevPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                                pagerState.animateScrollToPage(prevPage)
                                            }
                                        }
                                    }
                                },
                                onDoubleTap = {
                                    // 双击切换 1x/2x 缩放
                                    if (abs(scale - 1f) < 0.01f) {
                                        scale = 2f
                                    } else {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            )
                        }

                    Box(modifier = tapModifier) {
                        when (settings.readingMode) {
                            ReadingMode.LEFT_TO_RIGHT -> {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    reverseLayout = false
                                ) { sheet ->
                                    ZoomablePageContent(
                                        uiState = uiState,
                                        settings = settings,
                                        indices = getSourceIndicesForSheet(sheet),
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        transformableState = transformableState,
                                        onScaleChanged = { scale = it },
                                        onOffsetChanged = { x, y -> offsetX = x; offsetY = y }
                                    )
                                }
                            }
                            ReadingMode.RIGHT_TO_LEFT -> {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    reverseLayout = true // 反向布局完美实现右向左阅读
                                ) { sheet ->
                                    ZoomablePageContent(
                                        uiState = uiState,
                                        settings = settings,
                                        indices = getSourceIndicesForSheet(sheet),
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        transformableState = transformableState,
                                        onScaleChanged = { scale = it },
                                        onOffsetChanged = { x, y -> offsetX = x; offsetY = y }
                                    )
                                }
                            }
                            ReadingMode.VERTICAL -> {
                                VerticalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { sheet ->
                                    ZoomablePageContent(
                                        uiState = uiState,
                                        settings = settings,
                                        indices = getSourceIndicesForSheet(sheet),
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        transformableState = transformableState,
                                        onScaleChanged = { scale = it },
                                        onOffsetChanged = { x, y -> offsetX = x; offsetY = y }
                                    )
                                }
                            }
                            ReadingMode.CONTINUOUS_VERTICAL -> {
                                // 已在上方处理
                            }
                        }
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
                    if (!isContinuousMode) {
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
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 显示当前页码（拖拽时显示目标页码）
                        Text(
                            if (isSliderDragging) "${sliderTooltipPage + 1}" else "${uiState.currentPage + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )

                        // 可拖拽的页面滑块
                        Slider(
                            value = sliderValue,
                            onValueChange = { newValue ->
                                isSliderDragging = true
                                sliderValue = newValue
                                sliderTooltipPage = newValue.roundToInt()
                            },
                            onValueChangeFinished = {
                                isSliderDragging = false
                                val targetPage = sliderValue.roundToInt().coerceIn(0, uiState.totalPages - 1)
                                if (!isContinuousMode) {
                                    coroutineScope.launch {
                                        val targetSheet = if (settings.doublePageMode && settings.readingMode != ReadingMode.VERTICAL) {
                                            targetPage / 2
                                        } else {
                                            targetPage
                                        }
                                        pagerState.scrollToPage(targetSheet.coerceIn(0, sheetsCount - 1))
                                    }
                                }
                                // 防抖保存进度
                                debounceJob?.cancel()
                                debounceJob = coroutineScope.launch {
                                    delay(300)
                                    viewModel.onPageChanged(targetPage)
                                }
                            },
                            valueRange = 0f..((uiState.totalPages - 1).coerceAtLeast(1).toFloat()),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            enabled = true
                        )

                        Text("${uiState.totalPages}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }

                    // 拖拽时显示页码提示浮层
                    if (isSliderDragging) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.9f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "第 ${sliderTooltipPage + 1} / ${uiState.totalPages} 页",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
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
        onVolumeKeysChange = viewModel::toggleVolumeKeysPaging,
        onPageOffsetChange = viewModel::updatePageOffset,
        onResetToGlobal = viewModel::resetToGlobalSettings,
        onTapZoneProfileChange = viewModel::updateTapZoneProfile
    )
}

/**
 * 支持缩放的页面内容组件。
 * 使用 transformable 支持双指缩放和平移。
 */
@Composable
private fun ZoomablePageContent(
    uiState: ReaderUiState,
    settings: ResolvedReaderSettings,
    indices: List<Int>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    transformableState: androidx.compose.foundation.gestures.TransformableState,
    onScaleChanged: (Float) -> Unit,
    onOffsetChanged: (Float, Float) -> Unit,
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
        // transformable 支持双指缩放和平移
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Page(s)",
                modifier = Modifier.fillMaxSize(),
                contentScale = if (settings.readingMode == ReadingMode.VERTICAL || settings.readingMode == ReadingMode.CONTINUOUS_VERTICAL) {
                    ContentScale.FillWidth
                } else {
                    ContentScale.Fit
                }
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.DarkGray)
        }
    }
}

/**
 * 连续垂直滚动阅读器（Webtoon 模式）。
 * 使用 LazyColumn 实现无限滚动，自动预加载相邻页面。
 */
@Composable
private fun ContinuousVerticalReader(
    uiState: ReaderUiState,
    settings: ResolvedReaderSettings,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    transformableState: androidx.compose.foundation.gestures.TransformableState,
    onTap: (normalizedX: Float, normalizedY: Float) -> Unit,
    onDoubleTap: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onOffsetChanged: (Float, Float) -> Unit,
) {
    val totalPages = uiState.totalPages
    if (totalPages <= 0) return

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.currentPage.coerceIn(0, totalPages - 1)
    )

    // 监听滚动位置，保存当前可见页面
    val firstVisibleIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    LaunchedEffect(firstVisibleIndex) {
        onPageChanged(firstVisibleIndex)
    }

    // 手势处理：点击区域 + 双击缩放
    val gestureModifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    onTap(offset.x / size.width, offset.y / size.height)
                },
                onDoubleTap = {
                    onDoubleTap()
                }
            )
        }

    LazyColumn(
        state = listState,
        modifier = gestureModifier
            .transformable(state = transformableState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            count = totalPages,
            key = { it }
        ) { pageIndex ->
            val imageBitmap = rememberPageImage(
                pageProvider = uiState.pageProvider,
                index = pageIndex,
                enableCrop = settings.enableCrop,
                doublePageMode = false,
                isCover = false,
                nextIndex = null
            )

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                // 加载占位符
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.DarkGray)
                }
            }
        }
    }
}
