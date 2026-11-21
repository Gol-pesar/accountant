package com.example.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val title: String,
    val notes: String,
    val releasePageUrl: String,
    val apkDownloadUrl: String?,
    val apkSizeBytes: Long?,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: AppReleaseInfo) : UpdateCheckResult()
    data class UpToDate(val latestTag: String?) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Checks GitHub Releases at https://github.com/Gol-pesar/accountant/releases
 * to notify the user whenever a newer APK version is published.
 */
object GitHubUpdateChecker {
    const val RELEASES_PAGE_URL = "https://github.com/Gol-pesar/accountant/releases"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/Gol-pesar/accountant/releases/latest"
    private const val ALL_RELEASES_API = "https://api.github.com/repos/Gol-pesar/accountant/releases?per_page=1"

    suspend fun checkForUpdate(
        currentVersionCode: Int,
        currentVersionName: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val releaseJson = fetchJsonObject(LATEST_RELEASE_API)
                ?: fetchFirstFromArray(ALL_RELEASES_API)
                ?: return@withContext UpdateCheckResult.Error("اطلاعات نسخه جدید از گیت‌هاب دریافت نشد")

            val info = parseReleaseJson(releaseJson)
            if (info.tagName.isBlank()) {
                return@withContext UpdateCheckResult.UpToDate(null)
            }

            if (isRemoteNewer(info.tagName, info.title, currentVersionCode, currentVersionName)) {
                UpdateCheckResult.UpdateAvailable(info)
            } else {
                UpdateCheckResult.UpToDate(info.tagName)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("خطا در اتصال به گیت‌هاب؛ اتصال اینترنت خود را بررسی کنید")
        }
    }

    internal fun parseReleaseJson(obj: JSONObject): AppReleaseInfo {
        val tag = obj.optString("tag_name", "").trim()
        val name = obj.optString("name", tag).ifBlank { tag }
        val body = obj.optString("body", "").trim()
        val htmlUrl = obj.optString("html_url", RELEASES_PAGE_URL).ifBlank { RELEASES_PAGE_URL }
        val publishedAt = obj.optString("published_at", "")

        var apkUrl: String? = null
        var apkSize: Long? = null
        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    apkUrl = downloadUrl
                    apkSize = asset.optLong("size", 0L).takeIf { it > 0L }
                    break
                }
            }
        }

        return AppReleaseInfo(
            tagName = tag,
            title = name,
            notes = body,
            releasePageUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            apkSizeBytes = apkSize,
            publishedAt = publishedAt
        )
    }

    /**
     * Compares GitHub release tag/title against the installed app version.
     * Supports both CI tags (`debug-apk-build-<RUN>-<ATTEMPT>`) and semantic versions (`v1.2.0`).
     */
    fun isRemoteNewer(
        remoteTag: String,
        remoteTitle: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): Boolean {
        val ciRegex = Regex("""(?:debug-apk-build-|build-?)(\d+)""", RegexOption.IGNORE_CASE)
        val ciMatch = ciRegex.find(remoteTag) ?: ciRegex.find(remoteTitle)
        if (ciMatch != null) {
            val remoteRunNumber = ciMatch.groupValues[1].toIntOrNull()
            if (remoteRunNumber != null) {
                // CI builds set versionCode = runNumber + 2
                val expectedRemoteVersionCode = remoteRunNumber + 2
                return expectedRemoteVersionCode > currentVersionCode
            }
        }

        val remoteSemver = parseSemver(remoteTag) ?: parseSemver(remoteTitle)
        val localSemver = parseSemver(currentVersionName)
        if (remoteSemver != null && localSemver != null) {
            return compareSemver(remoteSemver, localSemver) > 0
        }

        return remoteTag.isNotBlank() && !remoteTag.equals(currentVersionName, ignoreCase = true)
    }

    private fun parseSemver(input: String): List<Int>? {
        val match = Regex("""v?(\d+)\.(\d+)(?:\.(\d+))?""", RegexOption.IGNORE_CASE).find(input)
            ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
        return listOf(major, minor, patch)
    }

    private fun compareSemver(a: List<Int>, b: List<Int>): Int {
        val len = maxOf(a.size, b.size)
        for (i in 0 until len) {
            val va = a.getOrElse(i) { 0 }
            val vb = b.getOrElse(i) { 0 }
            if (va != vb) return va.compareTo(vb)
        }
        return 0
    }

    private fun fetchJsonObject(urlString: String): JSONObject? {
        val body = httpGet(urlString) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun fetchFirstFromArray(urlString: String): JSONObject? {
        val body = httpGet(urlString) ?: return null
        return runCatching {
            val arr = JSONArray(body)
            if (arr.length() > 0) arr.optJSONObject(0) else null
        }.getOrNull()
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "HesabdarMan-Android")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn?.disconnect()
        }
    }
}
