package com.apkajan.app.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ana ekrandaki hızlı seçimler. TikTok'un bölgeye göre birden çok paket adı var. */
data class QuickApp(val name: String, val packages: List<String>, val color: Long)

object QuickApps {
    const val INSTAGRAM_PKG = "com.instagram.android"
    const val YOUTUBE_PKG = "com.google.android.youtube"
    val TIKTOK_PKGS = listOf(
        "com.zhiliaoapp.musically",  // TikTok (global)
        "com.ss.android.ugc.trill",  // TikTok (bazı bölgeler)
        "com.ss.android.ugc.aweme",  // Douyin
        "com.zhiliaoapp.musically.go" // TikTok Lite
    )

    val INSTAGRAM = QuickApp("Instagram", listOf(INSTAGRAM_PKG), 0xFFE1306C)
    val YOUTUBE = QuickApp("YouTube", listOf(YOUTUBE_PKG), 0xFFFF0000)
    val TIKTOK = QuickApp("TikTok", TIKTOK_PKGS, 0xFF25F4EE)

    val all = listOf(INSTAGRAM, YOUTUBE, TIKTOK)
    val defaultPackages: Set<String> = all.flatMap { it.packages }.toSet()
    val quickPackages: Set<String> = defaultPackages

    fun forPackage(pkg: String): QuickApp? = all.firstOrNull { pkg in it.packages }
}

data class InstalledApp(val packageName: String, val label: String)

class InstalledAppsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val labelCache = HashMap<String, String>()

    /** Ana ekrandan başlatılabilen tüm uygulamalar (kendimiz hariç), ada göre sıralı. */
    suspend fun launchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != appContext.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun isInstalled(pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun isAnyInstalled(app: QuickApp): Boolean = app.packages.any { isInstalled(it) }

    fun label(pkg: String): String {
        QuickApps.forPackage(pkg)?.let { return it.name }
        return synchronized(labelCache) {
            labelCache.getOrPut(pkg) {
                try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    pkg
                }
            }
        }
    }
}
