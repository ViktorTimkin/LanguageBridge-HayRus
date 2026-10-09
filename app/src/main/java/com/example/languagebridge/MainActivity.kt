package com.example.languagebridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagebridge.data.AppPreferences
import com.example.languagebridge.data.AzureTranslationService
import com.example.languagebridge.data.Language
import com.example.languagebridge.ui.ConversationZone
import com.example.languagebridge.ui.TranslatorViewModel
import com.example.languagebridge.ui.TranslatorViewModelFactory
import com.example.languagebridge.ui.theme.LanguageBridgeTheme
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ButtonDefaults
import com.example.languagebridge.ui.TypedInputRow
import com.example.languagebridge.ui.theme.AppColors
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
class MainActivity : ComponentActivity() {

    private var micPermissionGranted by mutableStateOf(value = false)

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micPermissionGranted = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        micPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (!micPermissionGranted) {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        val initialTopLanguage = AppPreferences.loadTopLanguage(this)

        val service = AzureTranslationService(
            speechKey = BuildConfig.AZURE_SPEECH_KEY,
            speechRegion = BuildConfig.AZURE_SPEECH_REGION,
            translatorKey = BuildConfig.AZURE_TRANSLATOR_KEY,
        )

        setContent {
            LanguageBridgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: TranslatorViewModel = viewModel(
                        factory = TranslatorViewModelFactory(service)
                    )
                    Greeting(
                        viewModel = viewModel,
                        hasMicPermission = micPermissionGranted,
                        initialTopLanguage = initialTopLanguage,
                        onTopLanguageChanged = { lang ->
                            AppPreferences.saveTopLanguage(this@MainActivity, lang)
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(
    viewModel: TranslatorViewModel,
    hasMicPermission: Boolean,
    initialTopLanguage: Language,
    onTopLanguageChanged: (Language) -> Unit,
    modifier: Modifier = Modifier,
) {
    var topLanguage by remember { mutableStateOf(initialTopLanguage) }
    val bottomLanguage = topLanguage.other()

    var topZoneFlipped by remember { mutableStateOf(true) }
    var showTyping by remember { mutableStateOf(false) }
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

                IconButton(
                    onClick = { viewModel.clearConversation() },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Очистить чат",
                        tint = AppColors.ErrorRed,
                    )
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
            modifier = Modifier.weight(1f),
        )
    }
}