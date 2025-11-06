package com.words.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.words.domain.model.GameState
import com.words.domain.model.Key
import com.words.domain.model.KeyFormat
import com.words.domain.model.Letter
import com.words.presentation.GameIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameState,
    onIntent: (GameIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Words!",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onIntent(GameIntent.ShowStatistics) }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistics")
                    }
                    IconButton(onClick = { onIntent(GameIntent.ShowHelp) }) {
                        Icon(Icons.Default.Help, contentDescription = "Help")
                    }
                    IconButton(onClick = { onIntent(GameIntent.ShowSettings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Game Grid
            GameGrid(
                grid = state.grid,
                currentRow = state.attempts,
                showInvalidWordAnimation = state.showInvalidWordAnimation
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Virtual Keyboard
            VirtualKeyboard(
                keys = state.keys,
                keyboardState = state.keyboardState,
                onKeyClick = { key ->
                    when (key) {
                        is Key.Letter -> onIntent(GameIntent.EnterLetter(key.char))
                        Key.Enter -> onIntent(GameIntent.EnterWord)
                        Key.Delete -> onIntent(GameIntent.Backspace)
                    }
                }
            )
        }
    }
}

@Composable
fun GameGrid(
    grid: List<List<Letter>>,
    currentRow: Int,
    showInvalidWordAnimation: Boolean
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (showInvalidWordAnimation && currentRow < grid.size) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grid.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (rowIndex == currentRow) {
                    Modifier.scale(animatedScale)
                } else {
                    Modifier
                }
            ) {
                row.forEach { letter ->
                    LetterCell(letter)
                }
            }
        }
    }
}

@Composable
fun LetterCell(letter: Letter) {
    val backgroundColor = when (letter.format) {
        Letter.Format.NotUsed -> Color(0xFFE0E0E0)
        Letter.Format.NoMatch -> Color(0xFF757575)
        Letter.Format.Match -> Color(0xFFFFC107)
        Letter.Format.ExactMatch -> Color(0xFF4CAF50)
    }

    val textColor = if (letter.format == Letter.Format.NotUsed) {
        Color.Black
    } else {
        Color.White
    }

    val borderColor = if (letter.selected) {
        MaterialTheme.colorScheme.primary
    } else if (letter.incorrect) {
        Color.Red
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(2.dp, borderColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun VirtualKeyboard(
    keys: List<List<Key>>,
    keyboardState: Map<Char, KeyFormat>,
    onKeyClick: (Key) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(48.dp)
            ) {
                row.forEach { key ->
                    KeyButton(key, keyboardState, onKeyClick)
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    key: Key,
    keyboardState: Map<Char, KeyFormat>,
    onKeyClick: (Key) -> Unit
) {
    val backgroundColor = when (key) {
        is Key.Letter -> {
            when (keyboardState[key.char]) {
                KeyFormat.ExactMatch -> Color(0xFF4CAF50)
                KeyFormat.Match -> Color(0xFFFFC107)
                KeyFormat.NoMatch -> Color(0xFF757575)
                KeyFormat.Unused, null -> Color(0xFFE0E0E0)
            }
        }
        Key.Enter, Key.Delete -> Color(0xFFBDBDBD)
    }

    val textColor = when (key) {
        is Key.Letter -> {
            when (keyboardState[key.char]) {
                KeyFormat.ExactMatch, KeyFormat.Match, KeyFormat.NoMatch -> Color.White
                KeyFormat.Unused, null -> Color.Black
            }
        }
        Key.Enter, Key.Delete -> Color.Black
    }

    val text = when (key) {
        is Key.Letter -> key.char.toString()
        Key.Enter -> "ENTER"
        Key.Delete -> "⌫"
    }

    val modifier = if (key is Key.Letter) {
        Modifier.width(36.dp)
    } else {
        Modifier.width(60.dp)
    }

    Button(
        onClick = { onKeyClick(key) },
        modifier = modifier.fillMaxHeight(),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = if (key is Key.Letter) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
