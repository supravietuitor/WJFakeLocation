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
    onLoad: (name: String) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("�璅∪�蝞∠�")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("璅∪��妍") },
                    placeholder = { Text("靘�嚗振��詻郎�?) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Divider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "憸挽璅∪�嚗?,
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { /* �蝸摰嗅滬璅∪� */ },
                            label = { Text("摰?) },
                            selected = false
                        )
                        FilterChip(
                            onClick = { /* �蝸撌乩�璅∪� */ },
                            label = { Text("�砍") },
                            selected = false
                        )
                        FilterChip(
                            onClick = { /* �蝸摮行璅∪� */ },
                            label = { Text("摮行") },
                            selected = false
                        )
                    }
                }
            }
            
            Text(
                text = "�內嚗�摮芋撘�摮敶�����雿挽蝵殷�蝎曉漲�絲�漲蝑�",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onLoad(profileName)
                        }
                    },
                    enabled = profileName.isNotBlank()
                ) {
                    Text("�蝸")
                }
                
                Button(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onSave(profileName)
                        }
                    },
                    enabled = profileName.isNotBlank()
                ) {
                    Text("靽�")
                }
            }
        }
    )
}
