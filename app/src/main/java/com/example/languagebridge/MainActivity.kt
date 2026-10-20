package com.example.languagebridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.languagebridge.data.AppPreferences
import com.example.languagebridge.data.UpdateChecker
import com.example.languagebridge.data.UpdateInfo
import com.example.languagebridge.data.UpdateInstaller
import com.example.languagebridge.data.speech.SpeechRecognitionService
import com.example.languagebridge.data.speech.SpeechSynthesisService
import com.example.languagebridge.data.translation.TextTranslationService
import com.example.languagebridge.ui.TranslatorScreen
import com.example.languagebridge.ui.TranslatorViewModel
import com.example.languagebridge.ui.TranslatorViewModelFactory
import com.example.languagebridge.ui.components.UpdateDialog
import com.example.languagebridge.ui.theme.LanguageBridgeTheme
import kotlinx.coroutines.launch

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

        val speechRecognition = SpeechRecognitionService(
            speechKey = BuildConfig.AZURE_SPEECH_KEY,
            speechRegion = BuildConfig.AZURE_SPEECH_REGION,
        )
        val speechSynthesis = SpeechSynthesisService(
            speechKey = BuildConfig.AZURE_SPEECH_KEY,
            speechRegion = BuildConfig.AZURE_SPEECH_REGION,
        )
        val textTranslation = TextTranslationService(
            translatorKey = BuildConfig.AZURE_TRANSLATOR_KEY,
        )
        val viewModel = ViewModelProvider(
            this,
            TranslatorViewModelFactory(speechRecognition, speechSynthesis, textTranslation)
        )[TranslatorViewModel::class.java]

        setContent {
            LanguageBridgeTheme {
                TranslatorScreen(
                    viewModel = viewModel,
                    hasMicPermission = micPermissionGranted,
                    initialTopLanguage = initialTopLanguage,
                    onTopLanguageChanged = { language ->
                        AppPreferences.saveTopLanguage(
                            this@MainActivity,
                            language
                        )
                    },
                    initialTextScaleIndex = initialTextScaleIndex,
                    onTextScaleChanged = { index ->
                        AppPreferences.saveTextScaleIndex(
                            this@MainActivity,
                            index
                        )
                    }
                )

                availableUpdate?.let { update ->
                    UpdateDialog(
                        update = update,
                        progress = updateProgress,
                        onUpdate = { startUpdate(update) },
                        onDismiss = { availableUpdate = null },
                    )
                }
            }
        }

        lifecycleScope.launch {
            availableUpdate = UpdateChecker.check(
                BuildConfig.VERSION_NAME
            )
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
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
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
                Toast.makeText(
                    this@MainActivity,
                    "Не удалось скачать обновление",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            UpdateInstaller.install(this@MainActivity, file)
            updateProgress = null
            availableUpdate = null
        }
    }
}

