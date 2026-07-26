// AddFavoriteDialog.kt
package com.steadywj.wjfakelocation.manager.favorites.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.steadywj.wjfakelocation.R

@Composable
fun AddFavoriteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("default") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(id = R.string.favorites_add))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("åç§°") },
                    placeholder = { Text("è¾“å…¥åœ°ç‚¹åç§°") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("åˆ†ç±»") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("é»˜è®¤") },
                            onClick = {
                                category = "default"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Done") },
                            onClick = {
                                category = "home"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("å…¬å¸") },
                            onClick = {
                                category = "work"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("å…¶ä»–") },
                            onClick = {
                                category = "other"
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, category)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(id = R.string.ok))
            }
        }
    )
}
