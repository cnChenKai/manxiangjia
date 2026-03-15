package com.mangahaven.feature.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mangahaven.data.files.thumbnail.ThumbnailGenerator

@Composable
fun ThumbnailStrip(
    itemId: String,
    totalPages: Int,
    currentPage: Int,
    thumbnailGenerator: ThumbnailGenerator,
    pageProvider: com.mangahaven.data.files.PageProvider?,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 0 || pageProvider == null) return

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, currentPage - 2))
    
    // 监听进度，跟随滚动
    LaunchedEffect(currentPage) {
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem(maxOf(0, currentPage - 2))
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.DarkGray),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(totalPages, key = { it }) { index ->
            ThumbnailItem(
                itemId = itemId,
                pageIndex = index,
                isSelected = index == currentPage,
                thumbnailGenerator = thumbnailGenerator,
                pageProvider = pageProvider,
                onClick = { onPageSelected(index) }
            )
        }
    }
}

@Composable
private fun ThumbnailItem(
    itemId: String,
    pageIndex: Int,
    isSelected: Boolean,
    thumbnailGenerator: ThumbnailGenerator,
    pageProvider: com.mangahaven.data.files.PageProvider,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailPath by remember { mutableStateOf<String?>(null) }
    
    // 异步拉取缩略图地址
    LaunchedEffect(itemId, pageIndex) {
        thumbnailPath = thumbnailGenerator.getThumbnail(itemId, pageIndex, pageProvider)
    }

    Box(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black)
            .clickable { onClick() }
    ) {
        if (thumbnailPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailPath)
                    .crossfade(true)
                    .build(),
                contentDescription = "Thubmnail $pageIndex",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSelected) 2.dp else 0.dp) // 选中时显示边框
            )
        }
        
        // 页码角标
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("${pageIndex + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}
