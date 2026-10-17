package com.example.languagebridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.languagebridge.data.AppPreferences
import com.example.languagebridge.data.AzureTranslationService
import com.example.languagebridge.data.Language
import com.example.languagebridge.ui.ConversationZone
import com.example.languagebridge.ui.TranslatorViewModel
import com.example.languagebridge.ui.TypedInputRow
import com.example.languagebridge.ui.theme.AppColors
import com.example.languagebridge.ui.theme.LanguageBridgeTheme
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.lifecycle.lifecycleScope
import com.example.languagebridge.data.UpdateChecker
import com.example.languagebridge.data.UpdateInfo
import kotlinx.coroutines.launch
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.LinearProgressIndicator
import com.example.languagebridge.data.UpdateInstaller
class MainActivity : ComponentActivity() {

    private var micPermissionGranted by mutableStateOf(value = false)
    private var availableUpdate by mutableStateOf<UpdateInfo?>(null)
    private var updateProgress by mutableStateOf<Float?>(null)
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
        val initialTextScaleIndex = AppPreferences.loadTextScaleIndex(this)

        val service = AzureTranslationService(
            speechKey = BuildConfig.AZURE_SPEECH_KEY,
            speechRegion = BuildConfig.AZURE_SPEECH_REGION,
            translatorKey = BuildConfig.AZURE_TRANSLATOR_KEY,
        )
        lifecycleScope.launch {
            availableUpdate = UpdateChecker.check(BuildConfig.VERSION_NAME)
        }
        setContent {
            LanguageBridgeTheme {
                availableUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { if (updateProgress == null) availableUpdate = null },
                        title = { Text("Доступна версия ${update.version}") },
                        text = {
                            Column {
                                Text(update.notes.replace("* ", "• ").ifBlank { "Вышло обновление HayRus." })
                                updateProgress?.let { progress ->
                                    Text(
                                        text = "Загрузка ${(progress * 100).toInt()}%",
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { startUpdate(update) },
                                enabled = updateProgress == null,
                            ) { Text("Обновить") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { availableUpdate = null },
                                enabled = updateProgress == null,
                            ) { Text("Позже") }
                        },
                    )
                }
            }
        }
    }
    private fun startUpdate(update: UpdateInfo) {
        val apkUrl = update.apkUrl
        if (apkUrl == null) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl)))
            return
        }

        if (!packageManager.canRequestPackageInstalls()) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            )
            Toast.makeText(
                this,
                "Разрешите установку для HayRus и нажмите «Обновить» ещё раз",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        lifecycleScope.launch {
            updateProgress = 0f
            val file = UpdateInstaller.download(this@MainActivity, apkUrl) { updateProgress = it }
            if (file == null) {
                updateProgress = null
                Toast.makeText(this@MainActivity, "Не удалось скачать обновление", Toast.LENGTH_LONG).show()
                return@launch
            }
            UpdateInstaller.install(this@MainActivity, file)
            updateProgress = null
            availableUpdate = null
        }
    }
}

@Composable
fun Greeting(
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