package com.fueltracker.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.fueltracker.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val releasePageUrl: String,
    val isBeta: Boolean = false
)

object AppUpdateManager {

    // Fetch releases list (includes pre-releases and beta builds)
    private const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/mousebt/FuelNoAd/releases?per_page=10"

    /**
     * Checks GitHub Releases for a newer version.
     * @param includeBeta Whether to include beta and pre-release builds in update checks.
     */
    suspend fun checkForUpdates(includeBeta: Boolean = true): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "FuelTracker-Android/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext Result.failure(Exception("暂未在 GitHub 找到已发布的版本"))
            }
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("检查更新失败: HTTP $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(responseBody)
            if (jsonArray.length() == 0) {
                return@withContext Result.failure(Exception("暂未发布任何版本"))
            }

            // Find matching release based on includeBeta preference
            var targetRelease: JSONObject? = null
            var isPrereleaseTarget = false

            for (i in 0 until jsonArray.length()) {
                val rel = jsonArray.getJSONObject(i)
                val tagName = rel.optString("tag_name", "").trim()
                val isPrerelease = rel.optBoolean("prerelease", false)
                    || tagName.contains("beta", ignoreCase = true)
                    || tagName.contains("alpha", ignoreCase = true)

                if (includeBeta || !isPrerelease) {
                    targetRelease = rel
                    isPrereleaseTarget = isPrerelease
                    break
                }
            }

            if (targetRelease == null) {
                return@withContext Result.failure(Exception("暂未在 GitHub 找到符合条件的正式版本发布"))
            }

            val release = targetRelease
            val tagName = release.optString("tag_name", "").trim()
            val releaseTitle = release.optString("name", "新版本发布")
            val releaseNotes = release.optString("body", "优化已知问题，提升使用体验。")
            val releasePageUrl = release.optString("html_url", "https://github.com/mousebt/FuelNoAd/releases")

            // Find APK in assets
            var apkDownloadUrl: String? = null
            var apkFileName = ""
            val assetsArray = release.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", null)
                        apkFileName = name
                        break
                    }
                }
            }

            val cleanRemote = tagName.removePrefix("v").removePrefix("V")
            val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V")

            val remoteBuildCode = extractBuildCode(apkFileName, releaseTitle, tagName)

            val hasUpdate = isNewerVersion(
                remoteVersion = cleanRemote,
                currentVersion = cleanCurrent,
                remoteBuildCode = remoteBuildCode,
                currentBuildCode = BuildConfig.VERSION_CODE
            )

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = tagName.ifBlank { "v$cleanRemote" },
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkDownloadUrl,
                    releasePageUrl = releasePageUrl,
                    isBeta = isPrereleaseTarget
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractBuildCode(vararg sources: String): Int? {
        val pattern = Pattern.compile("(?:Build|build|b)[-_\\s]?(\\d+)", Pattern.CASE_INSENSITIVE)
        for (src in sources) {
            val matcher = pattern.matcher(src)
            if (matcher.find()) {
                val num = matcher.group(1)?.toIntOrNull()
                if (num != null) return num
            }
        }
        return null
    }

    private fun extractBetaNumber(version: String): Int? {
        val pattern = Pattern.compile("beta\\.?(\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(version)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        if (version.contains("beta", ignoreCase = true)) {
            return 1
        }
        return null
    }

    /**
     * Compares remote and current version (SemVer and Build Code aware).
     */
    private fun isNewerVersion(
        remoteVersion: String,
        currentVersion: String,
        remoteBuildCode: Int?,
        currentBuildCode: Int
    ): Boolean {
        if (remoteVersion.isBlank()) return false

        // 1. If remote build code is known and strictly greater, it's definitely an update!
        if (remoteBuildCode != null && remoteBuildCode > currentBuildCode) {
            return true
        }

        if (remoteVersion.equals(currentVersion, ignoreCase = true)) {
            // Same version name, compare build code
            return if (remoteBuildCode != null) remoteBuildCode > currentBuildCode else false
        }

        try {
            val remoteBase = remoteVersion.split("-")[0].split(".")
            val currentBase = currentVersion.split("-")[0].split(".")

            val maxLength = maxOf(remoteBase.size, currentBase.size)
            for (i in 0 until maxLength) {
                val rNum = remoteBase.getOrNull(i)?.toIntOrNull() ?: 0
                val cNum = currentBase.getOrNull(i)?.toIntOrNull() ?: 0
                if (rNum > cNum) return true
                if (rNum < cNum) return false
            }

            // Base numbers are identical (e.g. 0.3.1 vs 0.3.1)
            val remoteIsBeta = remoteVersion.contains("beta", ignoreCase = true) || remoteVersion.contains("alpha", ignoreCase = true)
            val currentIsBeta = currentVersion.contains("beta", ignoreCase = true) || currentVersion.contains("alpha", ignoreCase = true)

            // If remote is stable and current is beta, that's an upgrade!
            if (!remoteIsBeta && currentIsBeta) {
                return true
            }

            // If remote is beta and current is stable, don't auto-upgrade to beta unless build code is higher
            if (remoteIsBeta && !currentIsBeta) {
                return false
            }

            // Both are beta (e.g. 0.3.1-beta.2 vs 0.3.1-beta.1)
            if (remoteIsBeta && currentIsBeta) {
                val rBetaNum = extractBetaNumber(remoteVersion)
                val cBetaNum = extractBetaNumber(currentVersion)
                if (rBetaNum != null && cBetaNum != null) {
                    if (rBetaNum > cBetaNum) return true
                    if (rBetaNum < cBetaNum) return false
                }
                if (remoteBuildCode != null && remoteBuildCode > currentBuildCode) {
                    return true
                }
            }
        } catch (_: Exception) {
            return remoteVersion != currentVersion
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
            e.printStackTrace()
        }
    }

    /**
     * Fallback to open the release webpage or browser download in external browser.
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
