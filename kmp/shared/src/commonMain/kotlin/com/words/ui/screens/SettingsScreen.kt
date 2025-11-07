package com.words.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.words.presentation.GameState
import com.words.presentation.GameIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit
) {
    var showWordListDialog by remember { mutableStateOf(false) }
    var showWordLengthDialog by remember { mutableStateOf(false) }
    var showClearStatisticsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(GameIntent.NavigateToGame) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Word List Selection
            Text(
                text = "Game Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingItem(
                title = "Word List",
                value = state.wordListName,
                onClick = { showWordListDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingItem(
                title = "Word Length",
                value = "${state.wordLength} letters",
                onClick = { showWordLengthDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Statistics
            Text(
                text = "Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { onIntent(GameIntent.ShowStatistics) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Statistics",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showClearStatisticsDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Clear Statistics",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About
            Text(
                text = "About",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Words! - Kotlin Multiplatform",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A word guessing game where you have 6 attempts to guess the secret word.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Word List Dialog
    if (showWordListDialog) {
        AlertDialog(
            onDismissRequest = { showWordListDialog = false },
            title = { Text("Select Word List") },
            text = {
                Column {
                    state.availableWordLists.forEach { wordList ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onIntent(GameIntent.SelectWordList(wordList))
                                    showWordListDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = wordList == state.wordListName,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = wordList)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWordListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Word Length Dialog
    if (showWordLengthDialog) {
        AlertDialog(
            onDismissRequest = { showWordLengthDialog = false },
            title = { Text("Select Word Length") },
            text = {
                Column {
                    state.availableWordLengths.forEach { length ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onIntent(GameIntent.SelectWordLength(length))
                                    showWordLengthDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = length == state.wordLength,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "$length letters")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWordLengthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Statistics Dialog
    if (showClearStatisticsDialog) {
        AlertDialog(
            onDismissRequest = { showClearStatisticsDialog = false },
            title = { Text("Clear Statistics?") },
            text = {
                Text("This will permanently delete all your game statistics. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(GameIntent.ClearStatistics)
                        showClearStatisticsDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearStatisticsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
