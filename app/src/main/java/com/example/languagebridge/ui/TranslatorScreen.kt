package com.example.languagebridge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.languagebridge.data.AppPreferences
import com.example.languagebridge.data.Language
import com.example.languagebridge.ui.theme.AppColors

@Composable
fun TranslatorScreen(
    viewModel: TranslatorViewModel,
    hasMicPermission: Boolean,
    initialTopLanguage: Language,
    onTopLanguageChanged: (Language) -> Unit,
    initialTextScaleIndex: Int,
    onTextScaleChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var topLanguage by remember { mutableStateOf(initialTopLanguage) }
    val bottomLanguage = topLanguage.other()

    var topZoneFlipped by remember { mutableStateOf(true) }
    var showTyping by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var textScaleIndex by remember { mutableStateOf(initialTextScaleIndex) }
    val textScale = AppPreferences.TEXT_SCALES[textScaleIndex]

    val topListState = rememberLazyListState()
    val bottomListState = rememberLazyListState()

    LaunchedEffect(viewModel.conversation.size) {
        val lastIndex = viewModel.conversation.size - 1
        if (lastIndex >= 0) {
            topListState.animateScrollToItem(lastIndex)
            bottomListState.animateScrollToItem(lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {

        ConversationZone(
            language = topLanguage,
            conversation = viewModel.conversation,
            listState = topListState,
            hasMicPermission = hasMicPermission,
            isBusy = viewModel.isBusy,
            onPressStart = { lang -> viewModel.startListening(lang) },
            onPressEnd = { lang -> viewModel.stopListening(lang) },
            onSpeakNormal = { turn -> viewModel.speakTurn(turn, topLanguage, slow = false) },
            onSpeakSlow = { turn -> viewModel.speakTurn(turn, topLanguage, slow = true) },
            flipped = true,
            contentFlippedExtra = topZoneFlipped,
            textScale = textScale,
            modifier = Modifier.weight(1f),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.MiddlePanelBackground)
                .padding(vertical = 6.dp),
        ) {
            AnimatedVisibility(visible = showTyping) {
                TypedInputRow(
                    language = topLanguage,
                    placeholderFlipped = !topZoneFlipped,
                    onSend = { lang, text -> viewModel.translateTyped(lang, text) },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { showTyping = !showTyping },
                    modifier = Modifier.size(40.dp),
                ) {
                    Text(
                        text = "⌨",
                        fontSize = 24.sp,
                        color = if (showTyping) AppColors.AccentBlue else AppColors.TextSecondary,
                    )
                }
                Button(
                    onClick = {
                        topLanguage = topLanguage.other()
                        onTopLanguageChanged(topLanguage)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue),
                ) {
                    Text("⇅ Поменять стороны", color = AppColors.TextPrimary, fontSize = 13.sp)
                }
                Button(
                    onClick = { topZoneFlipped = !topZoneFlipped },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue),
                ) {
                    Text("🔄 Верхний чат", color = AppColors.TextPrimary, fontSize = 13.sp)
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Меню",
                            tint = AppColors.TextPrimary,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = AppColors.BubbleBackground,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Очистить чат", color = AppColors.ErrorRed) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = AppColors.ErrorRed,
                                )
                            },
                            onClick = {
                                viewModel.clearConversation()
                                menuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Размер текста: ${AppPreferences.TEXT_SCALE_LABELS[textScaleIndex]}",
                                    color = AppColors.TextPrimary,
                                )
                            },
                            leadingIcon = { Text("Aa", color = AppColors.AccentBlue) },
                            onClick = {
                                textScaleIndex =
                                    (textScaleIndex + 1) % AppPreferences.TEXT_SCALES.size
                                onTextScaleChanged(textScaleIndex)
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showTyping) {
                TypedInputRow(
                    language = bottomLanguage,
                    onSend = { lang, text -> viewModel.translateTyped(lang, text) },
                )
            }
            viewModel.errorMessage?.let {
                Text(
                    text = "Ошибка: $it",
                    color = AppColors.ErrorRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        ConversationZone(
            language = bottomLanguage,
            conversation = viewModel.conversation,
            listState = bottomListState,
            hasMicPermission = hasMicPermission,
            isBusy = viewModel.isBusy,
            onPressStart = { lang -> viewModel.startListening(lang) },
            onPressEnd = { lang -> viewModel.stopListening(lang) },
            onSpeakNormal = { turn -> viewModel.speakTurn(turn, bottomLanguage, slow = false) },
            onSpeakSlow = { turn -> viewModel.speakTurn(turn, bottomLanguage, slow = true) },
            flipped = false,
            textScale = textScale,
            modifier = Modifier.weight(1f),
        )
    }
}
