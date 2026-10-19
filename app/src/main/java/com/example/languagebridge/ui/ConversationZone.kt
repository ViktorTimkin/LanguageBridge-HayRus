package com.example.languagebridge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.languagebridge.data.ConversationTurn
import com.example.languagebridge.data.Language
import com.example.languagebridge.ui.theme.AppColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationZone(
    language: Language,
    conversation: List<ConversationTurn>,
    listState: LazyListState,
    hasMicPermission: Boolean,
    isBusy: Boolean,
    onPressStart: (Language) -> Unit,
    onPressEnd: (Language) -> Unit,
    onSpeakNormal: (ConversationTurn) -> Unit,
    onSpeakSlow: (ConversationTurn) -> Unit,
    flipped: Boolean,
    contentFlippedExtra: Boolean = false,
    textScale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val isPressed by interactionSource.collectIsPressedAsState()

    var isRecordingSession by remember { mutableStateOf(value = false) }

    LaunchedEffect(isPressed) {
        if (isPressed && hasMicPermission) {
            isRecordingSession = true
            onPressStart(language)
        } else if (!isPressed && isRecordingSession) {
            isRecordingSession = false
            onPressEnd(language)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.ZoneBackground)
            .then(if (flipped) Modifier.rotate(180f) else Modifier),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .then(if (contentFlippedExtra) Modifier.rotate(180f) else Modifier),
        ) {
            items(conversation) { turn ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = turn.textFor(language),
                        color = AppColors.TextPrimary,
                        fontSize = (16 * textScale).sp,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.BubbleBackground)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    val textToCopy =
                                        if (language == Language.ARMENIAN) turn.armenianText else turn.russianText
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "HayRus",
                                            textToCopy
                                        )
                                    )
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onSpeakNormal(turn) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Повторить на обычной скорости",
                                tint = AppColors.AccentBlue
                            )
                        }
                        IconButton(
                            onClick = { onSpeakSlow(turn) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AppColors.AccentBlue.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(x = (-3).dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Повторить медленно",
                                    tint = AppColors.AccentBlue,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(x = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {},
            interactionSource = interactionSource,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = AppColors.TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppColors.flagBrush(language))
                .background(Color.Black.copy(alpha = 0.35f)),
        ) {
            Text(
                text = when {
                    !hasMicPermission -> "Нет разрешения на микрофон"
                    isPressed -> "Слушаю..."
                    isBusy -> "Обработка..."
                    else -> language.holdToSpeakPrompt
                },
                color = AppColors.TextPrimary,
                modifier = Modifier.then(if (contentFlippedExtra) Modifier.rotate(180f) else Modifier),
            )
        }
    }
}
