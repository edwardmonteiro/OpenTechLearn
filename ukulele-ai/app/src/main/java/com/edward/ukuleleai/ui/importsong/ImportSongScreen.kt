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
import androidx.compose.foundation.layout.weight
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

private val Background = Color(0xFF101312)
private val Primary = Color(0xFFF5F6F2)
private val Secondary = Color(0xFFA8B0AB)
private val Accent = Color(0xFFE8F46A)
private val exampleChart = """Title: My First Song
BPM: 80
| C | G | Am | F |
| C | G Am | F | G |"""

@Composable
fun ImportSongScreen(onCancel: () -> Unit, onSave: (String) -> Result<Song>, onSavedAndPlay: (Song) -> Unit) {
    var chart by remember { mutableStateOf(exampleChart) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Background).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Add song", color = Primary, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("One | section = one 4/4 bar.", color = Secondary, fontSize = 12.sp)
            }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A302D), contentColor = Primary)) { Text("Cancel") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = chart,
            onValueChange = { chart = it; error = null },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Paste chord chart") },
            textStyle = androidx.compose.ui.text.TextStyle(color = Primary, fontFamily = FontFamily.Monospace, fontSize = 15.sp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF4A514D),
                focusedLabelColor = Accent, unfocusedLabelColor = Secondary,
                cursorColor = Accent, focusedContainerColor = Color(0xFF181D1B), unfocusedContainerColor = Color(0xFF181D1B)
            )
        )
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFFA7A7), fontSize = 12.sp) }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Saved only on this phone.", color = Secondary, fontSize = 11.sp)
            Button(
                onClick = { onSave(chart).onSuccess(onSavedAndPlay).onFailure { error = it.message ?: "Could not parse chart." } },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)
            ) { Text("Save & Play", fontWeight = FontWeight.Bold) }
        }
    }
}
