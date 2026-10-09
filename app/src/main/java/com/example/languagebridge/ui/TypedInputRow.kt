package com.example.languagebridge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.languagebridge.data.Language
import com.example.languagebridge.ui.theme.AppColors

@Composable
fun TypedInputRow(
    language: Language,
    onSend: (Language, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typedText by remember { mutableStateOf("") }

    val send: () -> Unit = {
        if (typedText.isNotBlank()) {
            onSend(language, typedText)
            typedText = ""
        }
    }

    BasicTextField(
        value = typedText,
        onValueChange = { typedText = it },
        singleLine = true,
        textStyle = TextStyle(color = AppColors.TextPrimary, fontSize = 15.sp),
        cursorBrush = SolidColor(AppColors.AccentBlue),
        keyboardOptions = KeyboardOptions(
            hintLocales = LocaleList(language.speechLocale),
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { send() }),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.BubbleBackground)
                    .padding(start = 16.dp, end = 4.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (typedText.isEmpty()) {
                        Text(
                            text = language.displayName,
                            color = AppColors.TextSecondary,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { send() },
                ) {
                    Text("➤", color = AppColors.AccentBlue)
                }
            }
        },
    )
}