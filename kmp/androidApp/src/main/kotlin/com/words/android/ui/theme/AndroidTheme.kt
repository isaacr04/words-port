package com.words.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.words.ui.theme.WordsTheme

@Composable
fun AndroidWordsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    WordsTheme(darkTheme = darkTheme, content = content)
}
