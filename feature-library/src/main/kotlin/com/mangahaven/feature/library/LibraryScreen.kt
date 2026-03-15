package com.mangahaven.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Cloud
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

    var showImportMenu by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("漫享家 / MangaHaven") },
                actions = {
                    IconButton(onClick = onNavigateToSources) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "远程源")
                    }
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showImportMenu) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showImportMenu = false
                            fileLauncher.launch(arrayOf("application/zip", "application/x-cbz-compressed", "application/octet-stream"))
                        },
                        icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) },
                        text = { Text("导入 ZIP/CBZ") },
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isEmpty = allItems.isEmpty()

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
                        text = "点击右下角按钮导入本地目录或压缩包",
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
                    if (recentItems.isNotEmpty()) {
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
                                        onClick = { onNavigateToReader(item.id) }
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
                            text = "全部漫画",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(allItems, key = { it.id }) { item ->
                        LibraryGridItem(
                            item = item,
                            onClick = { onNavigateToReader(item.id) }
                        )
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
private fun RecentReadItem(item: LibraryItem, onClick: () -> Unit) {
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
                // Progress could go here
                Text(
                    text = "${item.pageCount ?: "?"} 页",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LibraryGridItem(modifier: Modifier = Modifier, item: LibraryItem, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .width(110.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.3f)),
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
