package com.mangahaven.feature.library.source

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mangahaven.model.Source
import com.mangahaven.model.SourceType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBrowser: (String) -> Unit, // 传入 sourceId 进入该源的浏览器
    viewModel: SourceViewModel = hiltViewModel()
) {
    val sources by viewModel.sources.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var showWebDavDialog by remember { mutableStateOf(false) }
    var showSmbDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("远程源管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    text = { Text("添加 SMB") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { showSmbDialog = true },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ExtendedFloatingActionButton(
                    text = { Text("添加 WebDAV") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { showWebDavDialog = true }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(sources) { source ->
                SourceItem(
                    source = source,
                    onClick = { onNavigateToBrowser(source.id) },
                    onDelete = { viewModel.removeSource(source.id) }
                )
                Divider()
            }
        }
    }

    val onSaveSource: (Source) -> Unit = { source ->
        showWebDavDialog = false
        showSmbDialog = false
        coroutineScope.launch {
            snackbarHostState.showSnackbar("正在连接测试...")
        }
        viewModel.addSource(source) { success, msg ->
            coroutineScope.launch {
                if (success) {
                    snackbarHostState.showSnackbar("添加成功！")
                } else {
                    snackbarHostState.showSnackbar("连接失败: $msg")
                }
            }
        }
    }

    if (showWebDavDialog) {
        SourceConfigDialog(sourceType = SourceType.WEBDAV, onDismiss = { showWebDavDialog = false }, onSave = onSaveSource)
    }

    if (showSmbDialog) {
        SourceConfigDialog(sourceType = SourceType.SMB, onDismiss = { showSmbDialog = false }, onSave = onSaveSource)
    }
}

@Composable
private fun SourceItem(source: Source, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(source.name) },
        supportingContent = { Text(source.configJson) },
        leadingContent = {
            Icon(
                imageVector = if (source.type == SourceType.WEBDAV) Icons.Default.Public else Icons.Default.FolderShared,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            // 本地源不可删除
            if (source.type != SourceType.LOCAL) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    )
}
