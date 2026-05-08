package com.mangahaven.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mangahaven.model.LibraryItem
import com.mangahaven.model.LibraryItemType
import com.mangahaven.model.ReadingStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToReader: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSources: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val recentItems by viewModel.recentItems.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val isFavoriteFilter by viewModel.isFavoriteFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    var showImportMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // File Picker
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFile(it) }
    }

    // Directory Picker
    val dirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.importDirectory(it) }
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 个条目吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.batchDelete()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                // 多选模式工具栏
                TopAppBar(
                    title = { Text("已选 ${selectedIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitMultiSelect() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSelectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        IconButton(onClick = { viewModel.batchMarkAsRead() }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "标记已读")
                        }
                        IconButton(onClick = { viewModel.batchToggleFavorite() }) {
                            Icon(Icons.Default.Star, contentDescription = "切换收藏")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("漫享家 / MangaHaven") },
                    actions = {
                        IconButton(onClick = { viewModel.updateSortBy(if (sortBy == "TITLE") "RECENT_READ" else "TITLE") }) {
                            Icon(Icons.Default.Settings, contentDescription = "排序")
                        }
                        IconButton(onClick = onNavigateToSources) {
                            Icon(Icons.Default.CloudQueue, contentDescription = "远程源")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showImportMenu) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showImportMenu = false
                                fileLauncher.launch(arrayOf(
                                    "application/zip",
                                    "application/x-cbz-compressed",
                                    "application/x-zip-compressed",
                                    "application/vnd.comicbook+zip",
                                    "application/octet-stream"
                                ))
                            },
                            icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                            text = { Text("导入 ZIP/CBZ") },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showImportMenu = false
                                fileLauncher.launch(arrayOf("application/epub+zip"))
                            },
                            icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                            text = { Text("导入 EPUB") },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showImportMenu = false
                                fileLauncher.launch(arrayOf(
                                    "application/x-mobipocket-ebook",
                                    "application/x-mobi"
                                ))
                            },
                            icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                            text = { Text("导入 MOBI") },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showImportMenu = false
                                dirLauncher.launch(null)
                            },
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            text = { Text("导入漫画文件夹") },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { showImportMenu = !showImportMenu },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "导入本地漫画")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isEmpty = allItems.isEmpty() && searchQuery.isBlank()

            Column(modifier = Modifier.fillMaxSize()) {
                // Search & Filter Header
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索书名...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(100)
                )

                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = isFavoriteFilter == true,
                            onClick = { viewModel.updateFavoriteFilter(if (isFavoriteFilter == true) null else true) },
                            label = { Text("收藏") },
                            leadingIcon = { if (isFavoriteFilter == true) Icon(Icons.Default.Star, null, Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == ReadingStatus.UNREAD.name,
                            onClick = { viewModel.updateStatusFilter(if (statusFilter == ReadingStatus.UNREAD.name) null else ReadingStatus.UNREAD.name) },
                            label = { Text("未读") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == ReadingStatus.READING.name,
                            onClick = { viewModel.updateStatusFilter(if (statusFilter == ReadingStatus.READING.name) null else ReadingStatus.READING.name) },
                            label = { Text("阅读中") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = statusFilter == ReadingStatus.COMPLETED.name,
                            onClick = { viewModel.updateStatusFilter(if (statusFilter == ReadingStatus.COMPLETED.name) null else ReadingStatus.COMPLETED.name) },
                            label = { Text("已读") }
                        )
                    }
                }

                if (isEmpty) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "书架空空如也",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角按钮导入本地目录或网络挂载源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (recentItems.isNotEmpty() && !isMultiSelectMode) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
                            Text(
                                text = "继续阅读",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(recentItems, key = { it.id }) { item ->
                                    RecentReadItem(
                                        item = item,
                                        onClick = { onNavigateToReader(item.id) },
                                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                                    )
                                }
                            }
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
                        Text(
                            text = if (isMultiSelectMode) "选择条目" else "过滤结果",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(allItems, key = { it.id }) { item ->
                        val isSelected = item.id in selectedIds
                        LibraryGridItem(
                            item = item,
                            isSelected = isSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelection(item.id)
                                } else {
                                    onNavigateToReader(item.id)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    viewModel.enterMultiSelect(item.id)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onUpdateStatus = { viewModel.updateReadingStatus(item, it) }
                        )
                    }
                }
            }
            }

            if (isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun RecentReadItem(item: LibraryItem, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                if (item.coverPath != null) {
                    AsyncImage(
                        model = File(item.coverPath!!),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (item.isFavorite) {
                    Icon(Icons.Default.Star, "收藏", modifier = Modifier.padding(4.dp).size(16.dp), tint = Color.Yellow)
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.itemType == LibraryItemType.REMOTE_ENTRY) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = "云端", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("远程串流", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.pageCount ?: "?"} 页",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                        Icon(if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, "收藏", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGridItem(
    modifier: Modifier = Modifier,
    item: LibraryItem,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onUpdateStatus: (ReadingStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .width(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
                .then(borderModifier),
            contentAlignment = Alignment.Center
        ) {
            if (item.coverPath != null) {
                AsyncImage(
                    model = File(item.coverPath!!),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("暂无封面", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            if (item.itemType == LibraryItemType.REMOTE_ENTRY) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(2.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = "云端", modifier = Modifier.size(12.dp), tint = Color.White)
                }
            }
            if (item.isFavorite) {
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(2.dp)
                ) {
                    Icon(Icons.Default.Star, "收藏", modifier = Modifier.size(12.dp), tint = Color.Yellow)
                }
            }

            // 多选模式下的选中指示器
            if (isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (isSelected) "已选中" else "未选中",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .padding(2.dp)
                    )
                }
            }

            if (!isMultiSelectMode) {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.MoreVert, "更多菜单", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(if (item.isFavorite) "取消收藏" else "加入收藏") },
                            onClick = { onToggleFavorite(); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("标记为已读") },
                            onClick = { onUpdateStatus(ReadingStatus.COMPLETED); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("标记为未读") },
                            onClick = { onUpdateStatus(ReadingStatus.UNREAD); expanded = false }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
