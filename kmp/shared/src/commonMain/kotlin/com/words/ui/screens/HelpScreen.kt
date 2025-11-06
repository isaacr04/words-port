package com.words.ui.screens

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
import com.words.presentation.GameIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onIntent: (GameIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "How to Play",
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
            // Introduction
            Text(
                text = "Objective",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Guess the secret word in 6 attempts or fewer. Each guess must be a valid word.",
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // How to Play
            Text(
                text = "How to Play",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "1. Enter a word using the virtual keyboard",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "2. Press ENTER to submit your guess",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "3. The color of the tiles will change to show how close your guess was",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Color Examples
            Text(
                text = "Examples",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Green Example
            ColorExample(
                letter = "W",
                color = Color(0xFF4CAF50),
                explanation = "The letter W is in the word and in the correct position"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Yellow Example
            ColorExample(
                letter = "I",
                color = Color(0xFFFFC107),
                explanation = "The letter I is in the word but in the wrong position"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gray Example
            ColorExample(
                letter = "U",
                color = Color(0xFF757575),
                explanation = "The letter U is not in the word at all"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tips
            Text(
                text = "Tips",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "• Start with common words that use different letters",
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Pay attention to the keyboard colors - they show which letters you've used",
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Letters can appear more than once in the same word",
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Back Button
            Button(
                onClick = { onIntent(GameIntent.NavigateToGame) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Got it! Let's Play",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ColorExample(
    letter: String,
    color: Color,
    explanation: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Letter tile
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Explanation
        Text(
            text = explanation,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
