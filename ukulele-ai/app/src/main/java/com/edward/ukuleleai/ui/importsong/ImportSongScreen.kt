package com.edward.ukuleleai.ui.importsong

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edward.ukuleleai.domain.Song

private val Background = Color(0xFF0D100F)
private val Primary = Color(0xFFF7F8F4)
private val Secondary = Color(0xFFA7B0AA)
private val Accent = Color(0xFFE9F45E)
private val exampleChart = """Title: My First Song
BPM: 80
| C | G | Am | F |
| C | G Am | F | G |"""

@Composable
fun ImportSongScreen(
    initialChart: String? = null,
    isEditing: Boolean = false,
    onCancel: () -> Unit,
    onSave: (String) -> Result<Song>,
    onSavedAndPlay: (Song) -> Unit
) {
    var chart by remember(initialChart) { mutableStateOf(initialChart ?: exampleChart) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Background).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(if (isEditing) "Edit song" else "Add song", color = Primary, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Edit title, BPM or chord bars. Everything stays on this phone.", color = Secondary, fontSize = 12.sp)
            }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242A27), contentColor = Primary)) { Text("Cancel") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = chart,
            onValueChange = { chart = it; error = null },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text(if (isEditing) "Edit chord chart" else "Paste chord chart") },
            textStyle = androidx.compose.ui.text.TextStyle(color = Primary, fontFamily = FontFamily.Monospace, fontSize = 15.sp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF3F4743),
                focusedLabelColor = Accent, unfocusedLabelColor = Secondary,
                cursorColor = Accent, focusedContainerColor = Color(0xFF151A18), unfocusedContainerColor = Color(0xFF151A18)
            )
        )
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFFA7A7), fontSize = 12.sp) }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Local only · no cloud save", color = Secondary, fontSize = 11.sp)
            Button(
                onClick = { onSave(chart).onSuccess(onSavedAndPlay).onFailure { error = it.message ?: "Could not parse chart." } },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)
            ) { Text(if (isEditing) "Save changes" else "Save & Play", fontWeight = FontWeight.Bold) }
        }
    }
}
