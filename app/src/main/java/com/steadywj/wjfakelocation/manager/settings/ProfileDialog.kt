// ProfileDialog.kt
package com.steadywj.wjfakelocation.manager.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.steadywj.wjfakelocation.R

@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    onLoad: (name: String) -> Unit,
) {
    var profileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("情景模式管理")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("模式名称") },
                    placeholder = { Text("例如：家、公司、学校") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Divider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "预设模式",
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            onClick = { 
                                profileName = "家"
                                onLoad("家")
                            },
                            label = { Text("家") },
                            selected = false,
                        )
                        FilterChip(
                            onClick = { 
                                profileName = "公司"
                                onLoad("公司")
                            },
                            label = { Text("公司") },
                            selected = false,
                        )
                        FilterChip(
                            onClick = { 
                                profileName = "学校"
                                onLoad("学校")
                            },
                            label = { Text("学校") },
                            selected = false,
                        )
                    }
                }
            }

            Text(
                text = "提示：保存模式将存储当前的所有定位设置（精度、海拔、速度等）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onLoad(profileName)
                        }
                    },
                    enabled = profileName.isNotBlank(),
                ) {
                    Text("加载")
                }

                Button(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onSave(profileName)
                        }
                    },
                    enabled = profileName.isNotBlank(),
                ) {
                    Text("保存")
                }
            }
        },
    )
}
