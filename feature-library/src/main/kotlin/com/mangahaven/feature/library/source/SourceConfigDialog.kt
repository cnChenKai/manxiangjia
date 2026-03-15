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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (sourceType == SourceType.WEBDAV) "添加 WebDAV 源" else "添加 SMB 源") },
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
                    label = { Text(if (sourceType == SourceType.WEBDAV) "URL (例如: http://192.168.1.5:8080/)" else "IP或主机名 (例如: 192.168.1.5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && urlOrIp.isNotBlank()) {
                        val authRef = if (username.isNotBlank() && password.isNotBlank()) {
                            "$username:$password"
                        } else null
                        
                        val newSource = Source(
                            id = UUID.randomUUID().toString(),
                            type = sourceType,
                            name = name,
                            configJson = urlOrIp,
                            authRef = authRef,
                            isWritable = true,
                            lastSyncAt = null
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
