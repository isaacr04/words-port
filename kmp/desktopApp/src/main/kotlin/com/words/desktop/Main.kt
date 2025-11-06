package com.words.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.words.data.PreferencesManager
import com.words.data.PreferencesStatisticsRepository
import com.words.data.ResourceWordListRepository
import com.words.presentation.GameViewModel
import com.words.ui.WordsApp
import com.words.ui.theme.WordsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Words!",
        state = windowState
    ) {
        // Create repositories
        val wordListRepository = ResourceWordListRepository()
        val preferencesManager = PreferencesManager()
        val statisticsRepository = PreferencesStatisticsRepository(preferencesManager)

        // Create ViewModel with a desktop-specific coroutine scope
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val viewModel = GameViewModel(
            wordListRepository = wordListRepository,
            statisticsRepository = statisticsRepository,
            coroutineScope = coroutineScope
        )

        WordsTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                WordsApp(viewModel)
            }
        }
    }
}
