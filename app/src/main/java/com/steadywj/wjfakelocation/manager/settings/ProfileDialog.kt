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
            Text("Profile Management")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("Enter profile name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Divider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Profile List",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { },
                            label = { Text("Default") },
                            selected = false
                        )
                        FilterChip(
                            onClick = { },
                            label = { Text("Custom") },
                            selected = false
                        )
                        FilterChip(
                            onClick = { },
                            label = { Text("Recent") },
                            selected = false
                        )
                    }
                }
            }
            
            Text(
                text = "Saved profiles will appear here. You can save and load different location configurations.",
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
                    Text("Load")
                }
                
                Button(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onSave(profileName)
                        }
                    },
                    enabled = profileName.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    )
}