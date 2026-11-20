package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class RiftAvailableUpdate(
  val versionLabel: String,
  val headline: String,
  val downloadUrl: String,
  val isPlayStoreInstall: Boolean,
  val isPlayUpdateDownloaded: Boolean = false,
)

object RiftUpdateManager {
  const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/GratzDude/RIFT/releases/latest"
  const val GITHUB_RELEASES_WEB_URL = "https://github.com/GratzDude/RIFT/releases/latest"

  fun isHeadlessOrTestRuntime(): Boolean {
    return Build.FINGERPRINT.equals("robolectric", ignoreCase = true) ||
      System.getProperty("robolectric.offline") == "true"
  }

  fun isInstalledFromGooglePlay(context: Context): Boolean {
    return runCatching {
      val installer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getInstallerPackageName(context.packageName)
        }
      installer == "com.android.vending"
    }.getOrDefault(false)
  }

  suspend fun checkAndPromptPlayStoreUpdate(
    appUpdateManager: AppUpdateManager,
    launcher: ActivityResultLauncher<IntentSenderRequest>?,
    autoLaunchPrompt: Boolean,
  ): Pair<RiftAvailableUpdate?, String> {
    return try {
      val info = appUpdateManager.appUpdateInfo.await()
      when {
        info.installStatus() == InstallStatus.DOWNLOADED -> {
          val update =
            RiftAvailableUpdate(
              versionLabel = "Play Store Build #${info.availableVersionCode()}",
              headline = "Update downloaded from Google Play. Restart RIFT to finish installing.",
              downloadUrl = "",
              isPlayStoreInstall = true,
              isPlayUpdateDownloaded = true,
            )
          update to "PLAY UPDATE READY TO INSTALL"
        }
        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
          info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
          val updateType =
            when {
              info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
              info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
              else -> null
            }
          if (autoLaunchPrompt && launcher != null && updateType != null) {
            runCatching {
              appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(updateType).build(),
              )
            }
          }
          val update =
            RiftAvailableUpdate(
              versionLabel = "Play Store Build #${info.availableVersionCode()}",
              headline = "A newer RIFT build is available on Google Play.",
              downloadUrl = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
              isPlayStoreInstall = true,
              isPlayUpdateDownloaded = false,
            )
          update to "UPDATE AVAILABLE ON GOOGLE PLAY"
        }
        else -> {
          null to "v${BuildConfig.VERSION_NAME} UP TO DATE (GOOGLE PLAY)"
        }
      }
    } catch (_: Exception) {
      null to "v${BuildConfig.VERSION_NAME} UP TO DATE"
    }
  }

  suspend fun checkDirectReleaseUpdate(
    currentVersionCode: Int = BuildConfig.VERSION_CODE,
    currentVersionName: String = BuildConfig.VERSION_NAME,
  ): Pair<RiftAvailableUpdate?, String> =
    withContext(Dispatchers.IO) {
      if (isHeadlessOrTestRuntime()) {
        return@withContext null to "v$currentVersionName UP TO DATE"
      }
      var conn: HttpURLConnection? = null
      try {
        val url = URL(GITHUB_LATEST_RELEASE_API)
        conn =
          (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5500
            readTimeout = 5500
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "RIFT-Android/${BuildConfig.VERSION_NAME}")
          }
        val code = conn.responseCode
        if (code !in 200..299) {
          return@withContext null to "v$currentVersionName UP TO DATE"
        }
        val rawBody = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(rawBody)
        if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
          return@withContext null to "v$currentVersionName UP TO DATE"
        }
        val tagName = json.optString("tag_name", "").trim()
        val releaseTitle = json.optString("name", "").trim()
        val htmlUrl = json.optString("html_url", GITHUB_RELEASES_WEB_URL).ifBlank { GITHUB_RELEASES_WEB_URL }
        var apkDownloadUrl = ""
        val assets = json.optJSONArray("assets")
        if (assets != null) {
          for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val assetName = asset.optString("name", "").lowercase(Locale.US)
            val dl = asset.optString("browser_download_url", "").trim()
            if (dl.isNotEmpty() && assetName.endsWith(".apk")) {
              apkDownloadUrl = dl
              break
            }
          }
        }
        val targetUrl = apkDownloadUrl.ifEmpty { htmlUrl }
        val isNewer =
          isRemoteReleaseNewer(
            tagName = tagName,
            releaseTitle = releaseTitle,
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName,
          )
        if (isNewer) {
          val displayTag =
            when {
              tagName.isNotEmpty() -> tagName
              releaseTitle.isNotEmpty() -> releaseTitle.take(28)
              else -> "Latest Release"
            }
          val summary =
            releaseTitle
              .takeIf { it.isNotEmpty() && !it.equals(tagName, ignoreCase = true) }
              ?.take(72)
              ?: "New RIFT build ($displayTag) is ready to download."
          RiftAvailableUpdate(
            versionLabel = displayTag,
            headline = summary,
            downloadUrl = targetUrl,
            isPlayStoreInstall = false,
            isPlayUpdateDownloaded = false,
          ) to "NEW BUILD $displayTag AVAILABLE"
        } else {
          null to "v$currentVersionName UP TO DATE"
        }
      } catch (_: Exception) {
        null to "v$currentVersionName UP TO DATE"
      } finally {
        runCatching { conn?.disconnect() }
      }
    }

  internal fun isRemoteReleaseNewer(
    tagName: String,
    releaseTitle: String,
    currentVersionCode: Int,
    currentVersionName: String,
  ): Boolean {
    val combined = "$tagName $releaseTitle"
    // 1. Check explicit build number if present (e.g. "build 93", "build #93", "b93")
    val buildMatch = Regex("""(?i)\bbuild\s*#?\s*(\d+)\b""").find(combined)
    if (buildMatch != null) {
      val remoteBuild = buildMatch.groupValues[1].toIntOrNull()
      if (remoteBuild != null && remoteBuild != currentVersionCode) {
        return remoteBuild > currentVersionCode
      }
    }
    // 2. Compare semantic version tuple (e.g. "v1.0.93" vs "1.0.92")
    val remoteNums = extractSemverParts(tagName).ifEmpty { extractSemverParts(releaseTitle) }
    val localNums = extractSemverParts(currentVersionName)
    if (remoteNums.isNotEmpty() && localNums.isNotEmpty()) {
      val maxLen = maxOf(remoteNums.size, localNums.size)
      for (i in 0 until maxLen) {
        val r = remoteNums.getOrElse(i) { 0 }
        val l = localNums.getOrElse(i) { 0 }
        if (r != l) return r > l
      }
      return false
    }
    // 3. Single integer tag fallback (e.g. "v93")
    val singleNum = tagName.trimStart('v', 'V', '#').toIntOrNull()
    if (singleNum != null) {
      return singleNum > currentVersionCode
    }
    return false
  }

  private fun extractSemverParts(raw: String): List<Int> {
    val match = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""").find(raw) ?: return emptyList()
    return match.groupValues.drop(1).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
  }

  fun openExternalUpdateUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
      val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
    }
  }
}
