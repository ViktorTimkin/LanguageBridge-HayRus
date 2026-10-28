
package com.example.languagebridge.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object UpdateInstaller {

    suspend fun download(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {

        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "HayRus-update.apk")
        val tempFile = File(dir, "HayRus-update.tmp")

        var connection: HttpURLConnection? = null
        var completed = false

        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "HayRus-App")
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val total = connection.contentLengthLong
            var downloaded = 0L

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        downloaded += read

                        if (total > 0) {
                            onProgress(
                                (downloaded.toFloat() / total)
                                    .coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }

            if (downloaded == 0L || (total > 0 && downloaded != total)) {
                throw IOException("APK загружен не полностью")
            }

            if (file.exists() && !file.delete()) {
                throw IOException("Не удалось удалить старый APK")
            }

            if (!tempFile.renameTo(file)) {
                throw IOException("Не удалось сохранить APK")
            }

            completed = true
            onProgress(1f)
            file

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()

            if (!completed) {
                tempFile.delete()
            }
        }
    }

    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )

        params.setRequireUserAction(
            PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
        )

        val sessionId = installer.createSession(params)

        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("update.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val intent = Intent(
                context,
                InstallResultReceiver::class.java
            )

            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_MUTABLE,
            )

            session.commit(pending.intentSender)
        }
    }
}

class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (
            intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )
        ) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            Intent.EXTRA_INTENT,
                            Intent::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    }

                confirm?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> Unit

            else -> {
                val message = intent.getStringExtra(
                    PackageInstaller.EXTRA_STATUS_MESSAGE
                ) ?: "неизвестная ошибка"

                Toast.makeText(
                    context,
                    "Не удалось установить: $message",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
