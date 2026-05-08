package com.mangahaven.feature.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mangahaven.model.ReadingMode
import com.mangahaven.model.TapZoneProfile
import com.mangahaven.data.local.repository.ResolvedReaderSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ResolvedReaderSettings,
    isVisible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onReadingModeChange: (ReadingMode, Boolean) -> Unit,
    onCropChange: (Boolean, Boolean) -> Unit,
    onDoublePageChange: (Boolean, Boolean) -> Unit,
    onVolumeKeysChange: (Boolean, Boolean) -> Unit,
    onPageOffsetChange: (Int) -> Unit,
    onResetToGlobal: () -> Unit,
    onTapZoneProfileChange: (TapZoneProfile) -> Unit,
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState()
    var applyToAll by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { onVisibleChange(false) },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "阅读设置",
                style = MaterialTheme.typography.titleLarge
            )

            // 全局开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("应用为全局默认（否则仅对本书生效）")
                Switch(
                    checked = applyToAll,
                    onCheckedChange = { applyToAll = it }
                )
            }
            if (settings.isOverridden && !applyToAll) {
                TextButton(onClick = onResetToGlobal) {
                    Text("本书存在独立设置，点击恢复为全局默认")
                }
            }
            HorizontalDivider()

            // 阅读方向
            Text("阅读方向", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ReadingMode.LEFT_TO_RIGHT -> "从左至右"
                        ReadingMode.RIGHT_TO_LEFT -> "从右至左"
                        ReadingMode.VERTICAL -> "上下翻页"
                        ReadingMode.CONTINUOUS_VERTICAL -> "连续滚动"
                    }
                    FilterChip(
                        selected = settings.readingMode == mode,
                        onClick = { onReadingModeChange(mode, applyToAll) },
                        label = { Text(label) }
                    )
                }
            }

            // 点击区域配置
            Text("点击区域", style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TapZoneProfile.values().forEach { profile ->
                    val label = when (profile) {
                        TapZoneProfile.DEFAULT -> "默认（左右边缘翻页）"
                        TapZoneProfile.LEFT_RIGHT -> "左右分区（半屏翻页）"
                        TapZoneProfile.L_SHAPE -> "L 形（左+右下翻页）"
                        TapZoneProfile.KINDLE_STYLE -> "Kindle（窄边翻页）"
                    }
                    val description = when (profile) {
                        TapZoneProfile.DEFAULT -> "左侧 30% 上一页，右侧 30% 下一页，中间切换菜单"
                        TapZoneProfile.LEFT_RIGHT -> "左半边上一页，右半边下一页，中间窄条切换菜单"
                        TapZoneProfile.L_SHAPE -> "左侧上一页，右下角下一页，其余切换菜单"
                        TapZoneProfile.KINDLE_STYLE -> "左边缘 20% 上一页，右边缘 20% 下一页，中间切换菜单"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.tapZoneProfile == profile,
                            onClick = { onTapZoneProfileChange(profile) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            HorizontalDivider()

            // 白边裁切
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("自动裁切白边")
                Switch(
                    checked = settings.enableCrop,
                    onCheckedChange = { onCropChange(it, applyToAll) }
                )
            }

            // 双页模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("横屏双页模式")
                Switch(
                    checked = settings.doublePageMode,
                    onCheckedChange = { onDoublePageChange(it, applyToAll) }
                )
            }

            // 音量键与手柄翻页
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("实体按键翻页 (音量键/手柄)")
                Switch(
                    checked = settings.volumeKeysPaging,
                    onCheckedChange = { onVolumeKeysChange(it, applyToAll) }
                )
            }

            // 页码倒退（因为封面存在，或单图组合导致乱序的修正）
            if (settings.doublePageMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("双页组合偏移 (±1修正错页)")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onPageOffsetChange(settings.pageOffset - 1) }) {
                            Text("-")
                        }
                        Text("${settings.pageOffset}")
                        IconButton(onClick = { onPageOffsetChange(settings.pageOffset + 1) }) {
                            Text("+")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
