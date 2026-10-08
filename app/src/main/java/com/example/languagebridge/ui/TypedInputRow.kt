package com.example.languagebridge.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.languagebridge.data.Language
import com.example.languagebridge.ui.theme.AppColors
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.intl.LocaleList
@Composable
fun TypedInputRow(
    language: Language,
    onSend: (Language, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typedText by remember { mutableStateOf("") }

    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = typedText,
            onValueChange = { typedText = it },
            placeholder = { Text(language.displayName, color = AppColors.TextSecondary) },
            keyboardOptions = KeyboardOptions(hintLocales = LocaleList(language.speechLocale)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedBorderColor = AppColors.AccentBlue,
                unfocusedBorderColor = AppColors.TextSecondary,
                cursorColor = AppColors.AccentBlue,
            ),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = {
                if (typedText.isNotBlank()) {
                    onSend(language, typedText)
                    typedText = ""
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue),
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text("➤", color = AppColors.TextPrimary)
        }
    }
}