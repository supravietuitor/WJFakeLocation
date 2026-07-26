// ApiKeySettingsScreen.kt
package com.steadywj.wjfakelocation.manager.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.steadywj.wjfakelocation.R
import com.steadywj.wjfakelocation.manager.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var apiKey by remember { mutableStateOf("") }
    var showGuideDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.api_key_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "è¿”å›ž")
                    }
                },
                actions = {
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "ä½¿ç”¨æŒ‡å—")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.warning),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.api_key_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(id = R.string.api_key_juhe)) },
                    placeholder = { Text(stringResource(id = R.string.api_key_input_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("API Key å°†åŠ å¯†å­˜å‚¨åˆ°æœ¬åœ°")
                    }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearApiKey() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("æ¸…é™¤ Key")
                    }
                    
                    Button(
                        onClick = { 
                            if (apiKey.isNotBlank()) {
                                viewModel.saveApiKey(apiKey)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Text(stringResource(id = R.string.api_key_save))
                    }
                }
            }
            
            uiState.showSuccessMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(message)
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearMessage()
                }
            }
            
            if (showGuideDialog) {
                AlertDialog(
                    onDismissRequest = { showGuideDialog = false },
                    title = {
                        Text(stringResource(id = R.string.api_key_guide))
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(id = R.string.api_key_guide_1))
                            Text(stringResource(id = R.string.api_key_guide_2))
                            Text(stringResource(id = R.string.api_key_guide_3))
                            Text(stringResource(id = R.string.api_key_guide_4))
                            Text(stringResource(id = R.string.api_key_guide_5))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showGuideDialog = false }) {
                            Text(stringResource(id = R.string.ok))
                        }
                    }
                )
            }
        }
    }
}
