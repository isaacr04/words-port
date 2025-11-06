package com.words.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.words.domain.game.GameEngine
import com.words.domain.model.GameState
import com.words.presentation.GameIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistics",
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Overall Statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Overall Statistics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn("Played", state.statistics.gamesPlayed.toString())
                        StatColumn("Won", state.statistics.gamesWon.toString())
                        StatColumn(
                            "Win %",
                            if (state.statistics.gamesPlayed > 0) {
                                "${(state.statistics.gamesWon * 100 / state.statistics.gamesPlayed)}%"
                            } else "0%"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn("Current Streak", state.statistics.currentStreak.toString())
                        StatColumn("Max Streak", state.statistics.maxStreak.toString())
                        StatColumn("Lost", state.statistics.gamesLost.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Guess Distribution
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
                        text = "Guess Distribution",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxCount = state.statistics.guessDistribution.maxOrNull() ?: 1

                    state.statistics.guessDistribution.forEachIndexed { index, count ->
                        GuessDistributionBar(
                            attempt = index + 1,
                            count = count,
                            maxCount = maxCount,
                            isCurrent = state.attempts == index && state.won
                        )
                        if (index < GameEngine.MAX_ATTEMPTS - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = { onIntent(GameIntent.NavigateToGame) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Back to Game",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onIntent(GameIntent.StartNewGame) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "New Game",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun GuessDistributionBar(
    attempt: Int,
    count: Int,
    maxCount: Int,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attempt number
        Text(
            text = "$attempt",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Bar
        val barColor = if (isCurrent) {
            Color(0xFF4CAF50)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        }

        val barWidth = if (maxCount > 0) (count.toFloat() / maxCount) else 0f

        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barWidth.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
