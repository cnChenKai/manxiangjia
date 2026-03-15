package com.mangahaven.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
) {
    // 沉浸模式下，菜单Overlay的可见性
    var overlayVisible by remember { mutableStateOf(false) }

    // 主阅读器背景 (暗色或纯黑增加沉浸感)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 点击屏幕中心唤出/隐藏菜单
                overlayVisible = !overlayVisible
            }
    ) {
        // --- Reader Content Placeholder ---
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "漫画页展示区域",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在阅读 Item ID: $itemId",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "(点击屏幕任意位置唤出菜单)",
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
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
                    Column {
                        Text(
                            text = "某本漫画的名字",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Top actions */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 页码和进度条
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = 0.1f,
                            onValueChange = { /* TODO */ },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text("100", style = MaterialTheme.typography.bodySmall)
                    }

                    // 底部控制按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { /* TODO: Switch Mode */ }) {
                            Text("阅读设置")
                        }
                        IconButton(onClick = { /* TODO: Settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                }
            }
        }
    }
}
