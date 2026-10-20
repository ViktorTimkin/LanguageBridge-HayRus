package com.example.languagebridge.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.languagebridge.data.UpdateInfo

@Composable
fun UpdateDialog(
    update: UpdateInfo,
    progress: Float?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text("Доступна версия ${update.version}") },
        text = {
            Column {
                Text(update.notes.replace("* ", "• ").ifBlank { "Вышло обновление HayRus." })
                progress?.let {
                    Text("Загрузка ${(it * 100).toInt()}%", modifier = Modifier.padding(top = 12.dp))
                    LinearProgressIndicator(
                        progress = { it },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate, enabled = progress == null) { Text("Обновить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = progress == null) { Text("Позже") }
        },
    )
}
