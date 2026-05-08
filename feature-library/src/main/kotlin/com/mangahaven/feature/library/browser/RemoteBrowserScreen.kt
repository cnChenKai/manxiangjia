package com.mangahaven.feature.library.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mangahaven.model.SourceEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteBrowserScreen(
    onNavigateBack: () -> Unit,
    viewModel: RemoteBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(uiState.sourceName, style = MaterialTheme.typography.titleMedium)
                        Text(uiState.currentPath, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.entries.isEmpty() && uiState.isRoot) {
                        item {
                            Text(
                                text = "空目录或没有内容",
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    if (!uiState.isRoot) {
                        item {
                            ListItem(
                                headlineContent = { Text("..") },
                                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                                modifier = Modifier.clickable { viewModel.navigateUp() }
                            )
                            HorizontalDivider()
                        }
                    }

                    items(uiState.entries) { entry ->
                        BrowserItem(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) {
                                    viewModel.navigateInto(entry.path)
                                }
                            },
                            onAdd = {
                                viewModel.addToLibrary(entry) { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserItem(
    entry: SourceEntry,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(entry.name) },
        supportingContent = { 
            if (!entry.isDirectory && entry.sizeBytes != null) {
                Text("${entry.sizeBytes!! / 1024} KB")
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (entry.isDirectory) {
                IconButton(onClick = onAdd) {
                    Icon(
                        Icons.Default.AddCircleOutline, 
                        contentDescription = "扫描并添加到书架",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    )
}
