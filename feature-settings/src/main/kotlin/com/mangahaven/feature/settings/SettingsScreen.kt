package com.mangahaven.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangahaven.data.files.remote.CrashUploader
import com.mangahaven.model.ReadingMode
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val privacyLockEnabled by viewModel.privacyLockEnabled.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSize.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 导入确认弹窗状态
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // SAF 文件选择器：导出
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportSettings(it) }
    }

    // SAF 文件选择器：导入（选中文件后弹确认框）
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    // 导入确认弹窗
    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("确认导入设置") },
            text = { Text("导入后将覆盖当前阅读偏好和应用设置，并新增或更新远程源配置。是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { viewModel.importSettings(it) }
                    pendingImportUri = null
                }) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 显示操作结果
    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBackupMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategoryTitle(title = "通用设置")
            SettingsSwitchItem(
                title = "应用隐私锁",
                subtitle = "从后台返回时要求验证指纹或密码",
                checked = privacyLockEnabled,
                onCheckedChange = viewModel::togglePrivacyLock
            )
            SettingsSwitchItem(
                title = "保持屏幕常亮",
                subtitle = "阅读时不息屏 (全局预设)",
                checked = readerSettings.keepScreenOn,
                onCheckedChange = viewModel::toggleKeepScreenOn
            )
            HorizontalDivider()
            
            SettingsCategoryTitle(title = "默认阅读选项")
            val readingModeLabel = if (readerSettings.readingMode == ReadingMode.LEFT_TO_RIGHT) "从左到右" else "从右到左"
            SettingsClickableItem(
                title = "默认阅读方向",
                subtitle = readingModeLabel,
                onClick = viewModel::updateReadingMode
            )
            SettingsSwitchItem(
                title = "双页模式",
                subtitle = "横屏时自动将两页拼合",
                checked = readerSettings.doublePageMode,
                onCheckedChange = viewModel::toggleDoublePageMode
            )
            SettingsSwitchItem(
                title = "白边裁切",
                subtitle = "自动检测并裁切图片白边",
                checked = readerSettings.enableCrop,
                onCheckedChange = viewModel::toggleCropEnabled
            )
            SettingsSwitchItem(
                title = "相邻页预加载",
                subtitle = "提前加载前后的图片，翻页更流畅",
                checked = readerSettings.enablePreload,
                onCheckedChange = viewModel::togglePreload
            )
            HorizontalDivider()

            SettingsCategoryTitle(title = "书库统计")
            val stats by viewModel.libraryStats.collectAsStateWithLifecycle()
            LibraryStatsSection(stats = stats, onRefresh = viewModel::refreshStats)
            HorizontalDivider()

            SettingsCategoryTitle(title = "数据与存储")
            SettingsClickableItem(
                title = "清理图片缓存",
                subtitle = "当前缓存: $cacheSize",
                onClick = viewModel::clearCache
            )
            SettingsClickableItem(
                title = "一键导出设置",
                subtitle = "导出阅读偏好和远程源（WebDAV/SMB/OPDS）配置到 JSON 文件",
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    exportLauncher.launch("mangahaven_backup_$timestamp.json")
                }
            )
            SettingsClickableItem(
                title = "一键导入设置",
                subtitle = "从之前导出的 JSON 文件恢复设置和远程源",
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )
            

            SettingsCategoryTitle(title = "开发者选项")
            val coroutineScope = rememberCoroutineScope()
            var isUploading by remember { mutableStateOf(false) }

            SettingsClickableItem(
                title = if (isUploading) "正在上传日志..." else "上传运行日志",
                subtitle = "将本地日志文件上传并获取短链接用于排错",
                onClick = {
                    if (isUploading) return@SettingsClickableItem
                    isUploading = true
                    coroutineScope.launch {
                        try {
                            // Find latest log file
                            val logsDir = File(context.filesDir, "logs")
                            val latestLog = logsDir.listFiles()?.maxByOrNull { it.lastModified() }

                            if (latestLog != null && latestLog.exists()) {
                                Toast.makeText(context, "开始上传...", Toast.LENGTH_SHORT).show()
                                val result = CrashUploader.uploadLogFile(context, latestLog)
                                if (result.isSuccess) {
                                    val url = result.getOrNull() ?: "Unknown URL"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Log URL", url)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "上传成功！链接已复制到剪贴板: $url", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "上传失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "没有找到本地日志文件", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "上传出错", Toast.LENGTH_SHORT).show()
                        } finally {
                            isUploading = false
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 书库统计卡片。
 * 显示条目总数、阅读状态分布等信息。
 */
@Composable
fun LibraryStatsSection(stats: LibraryStats, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 总数
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "总条目数",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${stats.totalItems}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // 阅读状态分布
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBadge(label = "未读", count = stats.unreadCount, color = MaterialTheme.colorScheme.error)
            StatBadge(label = "阅读中", count = stats.readingCount, color = MaterialTheme.colorScheme.tertiary)
            StatBadge(label = "已读", count = stats.completedCount, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 刷新按钮
        TextButton(
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("刷新统计")
        }
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
