package com.edward.ukuleleai.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edward.ukuleleai.domain.Song

@Composable
fun PracticeRoute07(song: Song, onExit: () -> Unit, viewModel: PracticeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        PracticeRoute(song, onExit, viewModel)
        if (state.backingAvailable) {
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 13.dp)
                    .background(if (state.backingEnabled) Color(0xFF263A32) else Color(0xFF171C19), RoundedCornerShape(15.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .then(if (!state.listeningEnabled) Modifier.noRippleClick { viewModel.toggleBacking() } else Modifier)
            ) {
                Text(
                    when { state.listeningEnabled -> "Original audio · silent while Listen is on"; state.backingEnabled -> "● Original audio ON"; else -> "Original audio OFF" },
                    color = if (state.backingEnabled) Color(0xFF70E0B6) else Color(0xFF858F89),
                    fontSize = 10.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = androidx.compose.foundation.clickable(onClick = onClick)
