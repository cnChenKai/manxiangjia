package com.mangahaven.feature.library.source

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mangahaven.model.Source
import com.mangahaven.model.SourceType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceConfigDialog(
    sourceType: SourceType,
    onDismiss: () -> Unit,
    onSave: (Source) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var urlOrIp by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val titleText = when (sourceType) {
        SourceType.WEBDAV -> "添加 WebDAV 源"
        SourceType.SMB -> "添加 SMB 源"
        SourceType.OPDS -> "添加 OPDS 源"
        else -> "添加内容源"
    }

    val urlLabel = when (sourceType) {
        SourceType.WEBDAV -> "URL (例如: http://192.168.1.5:8080/)"
        SourceType.OPDS -> "OPDS URL (例如: http://opds.example.com/catalog)"
        else -> "IP或主机名 (例如: 192.168.1.5)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = titleText) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("连接名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = urlOrIp,
                    onValueChange = { urlOrIp = it },
                    label = { Text(urlLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (sourceType == SourceType.SMB) {
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("域名 (可选)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // OPDS 源通常不需要认证，但保留可选认证字段
                if (sourceType != SourceType.OPDS) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名 (可选)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码 (可选)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && urlOrIp.isNotBlank()) {
                        val authRef = if (sourceType != SourceType.OPDS && username.isNotBlank() && password.isNotBlank()) {
                            if (sourceType == SourceType.SMB && domain.isNotBlank()) {
                                "$domain;$username:$password"
                            } else {
                                "$username:$password"
                            }
                        } else null

                        // OPDS 源标记为虚拟源，不缓存本地文件
                        val isVirtual = sourceType == SourceType.OPDS

                        val newSource = Source(
                            id = UUID.randomUUID().toString(),
                            type = sourceType,
                            name = name,
                            configJson = urlOrIp,
                            authRef = authRef,
                            isWritable = false,
                            lastSyncAt = null,
                            isVirtual = isVirtual,
                        )
                        onSave(newSource)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
