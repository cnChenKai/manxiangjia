package com.mangahaven.feature.settings

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangahaven.model.ReadingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val privacyLockEnabled by viewModel.privacyLockEnabled.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()

    Scaffold(
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

            SettingsCategoryTitle(title = "数据与存储")
            SettingsClickableItem(
                title = "清理图片缓存",
                subtitle = "当前缓存: 0 MB",
                onClick = { /* TODO */ }
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
