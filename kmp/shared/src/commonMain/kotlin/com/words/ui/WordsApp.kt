package com.words.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.words.ui.screens.GameOverScreen
import com.words.ui.screens.GameScreen
import com.words.ui.screens.HelpScreen
import com.words.ui.screens.SettingsScreen
import com.words.ui.screens.StatisticsScreen
import com.words.domain.model.GamePage
import com.words.presentation.GameViewModel

@Composable
fun WordsApp(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()

    when (state.currentPage) {
        GamePage.Game -> GameScreen(
            state = state,
            onIntent = viewModel::processIntent
        )
        GamePage.GameOver -> GameOverScreen(
            state = state,
            onIntent = viewModel::processIntent
        )
        GamePage.Statistics -> StatisticsScreen(
            state = state,
            onIntent = viewModel::processIntent
        )
        GamePage.Help -> HelpScreen(
            onIntent = viewModel::processIntent
        )
        GamePage.Settings -> SettingsScreen(
            state = state,
            onIntent = viewModel::processIntent
        )
    }
}
