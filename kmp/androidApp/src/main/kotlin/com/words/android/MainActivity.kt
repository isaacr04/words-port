package com.words.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.words.android.ui.theme.AndroidWordsTheme
import com.words.data.AndroidWordListRepository
import com.words.ui.WordsApp
import com.words.data.PreferencesManager
import com.words.data.PreferencesStatisticsRepository
import com.words.presentation.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create repositories
        val wordListRepository = AndroidWordListRepository(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)
        val statisticsRepository = PreferencesStatisticsRepository(preferencesManager)

        // Create ViewModel
        val viewModel = GameViewModel(
            wordListRepository = wordListRepository,
            statisticsRepository = statisticsRepository,
            coroutineScope = lifecycleScope
        )

        setContent {
            AndroidWordsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WordsApp(viewModel)
                }
            }
        }
    }
}
