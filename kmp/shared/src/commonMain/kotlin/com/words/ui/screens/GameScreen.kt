package com.words.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.words.presentation.GameState
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
    var containerWidth by remember { mutableStateOf(0.dp) }

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
                        Icon(Icons.Default.Info, contentDescription = "Statistics")
                    }
                    IconButton(onClick = { onIntent(GameIntent.ShowHelp) }) {
                        Icon(Icons.Default.Info, contentDescription = "Help")
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
                .padding(16.dp)
                .onSizeChanged { size ->
                    containerWidth = size.width.dp
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Game Grid
            GameGrid(
                grid = state.grid,
                currentRow = state.attempts,
                showInvalidWordAnimation = state.showInvalidWordAnimation,
                containerWidth = containerWidth
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Virtual Keyboard
            VirtualKeyboard(
                keys = state.keys,
                keyboardState = state.keyboardState,
                containerWidth = containerWidth,
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
    showInvalidWordAnimation: Boolean,
    containerWidth: androidx.compose.ui.unit.Dp
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (showInvalidWordAnimation && currentRow < grid.size) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    // Calculate responsive cell size
    val numberOfLetters = if (grid.isNotEmpty()) grid[0].size else 5
    val letterSpacing = 4.dp  // Reduced from 6dp to fit more content
    val spaceBetweenLetters = letterSpacing * (numberOfLetters - 1)
    val availableWidth = containerWidth  // Already has padding applied in GameScreen
    val calculatedCellSize = (availableWidth - spaceBetweenLetters) / numberOfLetters
    // Scale cell size based on word length to ensure everything fits
    val cellSize = when {
        numberOfLetters <= 5 -> calculatedCellSize.coerceAtMost(56.dp)
        numberOfLetters <= 7 -> calculatedCellSize.coerceAtMost(48.dp)
        numberOfLetters <= 9 -> calculatedCellSize.coerceAtMost(40.dp)
        else -> calculatedCellSize.coerceAtMost(36.dp)
    }
    val fontSizeValue = (cellSize.value * 0.4).coerceAtMost(20.0)
    val fontSize = fontSizeValue.sp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(letterSpacing)
    ) {
        grid.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(letterSpacing),
                modifier = if (rowIndex == currentRow) {
                    Modifier.scale(animatedScale)
                } else {
                    Modifier
                }
            ) {
                row.forEach { letter ->
                    LetterCell(letter, cellSize = cellSize, fontSize = fontSize)
                }
            }
        }
    }
}

@Composable
fun LetterCell(
    letter: Letter,
    cellSize: androidx.compose.ui.unit.Dp = 56.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp
) {
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
            .size(cellSize)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(2.dp, borderColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.value,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun VirtualKeyboard(
    keys: List<List<Key>>,
    keyboardState: Map<Char, KeyFormat>,
    containerWidth: androidx.compose.ui.unit.Dp,
    onKeyClick: (Key) -> Unit
) {
    val keySpacing = 4.dp
    val rowSpacing = 4.dp  // Reduced from 6dp
    val availableWidth = containerWidth  // Already has padding applied in GameScreen

    // Responsive keyboard height - very conservative
    val keyboardHeightValue = (containerWidth.value * 0.08).coerceIn(36.0, 44.0)
    val keyboardHeight = keyboardHeightValue.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(keySpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyboardHeight)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        keyboardState = keyboardState,
                        buttonHeight = keyboardHeight,
                        onKeyClick = onKeyClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(keyboardHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    key: Key,
    keyboardState: Map<Char, KeyFormat>,
    buttonHeight: androidx.compose.ui.unit.Dp = 48.dp,
    onKeyClick: (Key) -> Unit,
    modifier: Modifier = Modifier
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

    // Responsive font size based on button size and key type
    val fontSizeValue = when (key) {
        is Key.Letter -> (buttonHeight.value * 0.4).coerceAtMost(16.0)
        Key.Enter, Key.Delete -> (buttonHeight.value * 0.3).coerceAtMost(11.0)
    }
    val baseFontSize = fontSizeValue.sp

    Button(
        onClick = { onKeyClick(key) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = baseFontSize,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
