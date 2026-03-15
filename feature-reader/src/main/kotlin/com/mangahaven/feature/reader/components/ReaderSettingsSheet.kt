package com.mangahaven.feature.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mangahaven.model.ReadingMode
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
    onPageOffsetChange: (Int) -> Unit,
    onResetToGlobal: () -> Unit
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
            Divider()

            // 阅读方向
            Text("阅读方向", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingMode.values().forEach { mode ->
                    val label = when (mode) {
                        ReadingMode.LEFT_TO_RIGHT -> "从左至右"
                        ReadingMode.RIGHT_TO_LEFT -> "从右至左"
                        ReadingMode.VERTICAL -> "上下滚动"
                    }
                    FilterChip(
                        selected = settings.readingMode == mode,
                        onClick = { onReadingModeChange(mode, applyToAll) },
                        label = { Text(label) }
                    )
                }
            }

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
