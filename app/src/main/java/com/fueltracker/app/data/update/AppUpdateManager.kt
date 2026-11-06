package com.fueltracker.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.fueltracker.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val releasePageUrl: String
)

object AppUpdateManager {

    private const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/mousebt/FuelNoAd/releases/latest"

    /**
     * Checks GitHub Releases for a newer version.
     */
    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_LATEST_RELEASE_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "FuelTracker-Android/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext Result.failure(Exception("暂无已发布的正式版本（Release）"))
            }
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("检查更新失败: HTTP $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "新版本发布")
            val releaseNotes = json.optString("body", "优化已知问题，提升使用体验。")
            val releasePageUrl = json.optString("html_url", "https://github.com/mousebt/FuelNoAd/releases")

            // Find APK in assets
            var apkDownloadUrl: String? = null
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            val cleanRemote = tagName.removePrefix("v").removePrefix("V")
            val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V")

            val hasUpdate = isNewerVersion(cleanRemote, cleanCurrent)

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = tagName.ifBlank { "v$cleanRemote" },
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkDownloadUrl,
                    releasePageUrl = releasePageUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compares remote and current version strings (SemVer-aware).
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        if (remote == current) return false

        try {
            // Strip suffixes like -beta, -alpha for numeric comparison
            val remoteCore = remote.split("-")[0].split(".")
            val currentCore = current.split("-")[0].split(".")

            val maxLength = maxOf(remoteCore.size, currentCore.size)
            for (i in 0 until maxLength) {
                val rNum = remoteCore.getOrNull(i)?.toIntOrNull() ?: 0
                val cNum = currentCore.getOrNull(i)?.toIntOrNull() ?: 0
                if (rNum > cNum) return true
                if (rNum < cNum) return false
            }

            // If numerical parts are equal, compare build metadata or assume update if remote doesn't contain beta
            if (remote.contains("beta") && !current.contains("beta")) return false
            if (!remote.contains("beta") && current.contains("beta")) return true
        } catch (_: Exception) {
            return remote != current
        }

        return false
    }

    /**
     * Downloads the APK file with progress reporting.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            if (!destinationDir.exists()) destinationDir.mkdirs()

            val outputFile = File(destinationDir, fileName)
            if (outputFile.exists()) outputFile.delete()

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "FuelTracker-Android/${BuildConfig.VERSION_NAME}")
            }

            // Handle HTTP redirect (GitHub Releases redirect to AWS S3)
            var currentConnection = connection
            var redirectCount = 0
            while (currentConnection.responseCode in listOf(301, 302, 303, 307, 308) && redirectCount < 5) {
                val newUrl = currentConnection.getHeaderField("Location")
                currentConnection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "FuelTracker-Android/${BuildConfig.VERSION_NAME}")
                }
                redirectCount++
            }

            val totalBytes = currentConnection.contentLength.toFloat()
            var downloadedBytes = 0L

            currentConnection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            onProgress((downloadedBytes / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches the system package installer for the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to opening file folder
            e.printStackTrace()
        }
    }

    /**
     * Opens the web release page or download link in browser.
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
