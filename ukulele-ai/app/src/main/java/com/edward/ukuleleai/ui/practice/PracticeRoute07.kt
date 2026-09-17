package com.edward.ukuleleai.ui.practice

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.Song

/**
 * 0.10 keeps this route name for navigation compatibility, but the legacy overlay was removed.
 * All play-along controls now live in one safe-area-aware practice screen.
 */
@Composable
fun PracticeRoute07(
    song: Song,
    onExit: () -> Unit,
    onEditAnalysis: () -> Unit = {},
    viewModel: PracticeViewModel = viewModel()
) {
    PracticeRoute(
        song = song,
        onExit = onExit,
        onEditAnalysis = onEditAnalysis,
        viewModel = viewModel
    )
}
