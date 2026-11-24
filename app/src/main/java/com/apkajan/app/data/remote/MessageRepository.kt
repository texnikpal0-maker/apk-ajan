package com.apkajan.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import com.apkajan.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uzak JSON biçimi (bkz. proje kökündeki remote/messages.json):
 * {
 *   "warnings": { "az": ["..."], "tr": ["..."], "en": ["..."], "ru": ["..."] },
 *   "pomodoro": { "az": "...", "tr": "...", "en": "...", "ru": "..." }
 * }
 */
data class RemoteMessages(
    val warnings: Map<String, List<String>>,
    val pomodoro: Map<String, String>
)

enum class MessageSource { REMOTE, FALLBACK }

data class WarningText(val text: String, val source: MessageSource)

/**
 * Uyarı cümlelerini sağlar:
 *  1) İnternet varsa ve adres girilmişse uzak JSON'dan seçili dilde çeker (30 dk bellekte tutar).
 *  2) İnternet yoksa / istek hata verirse / o dilde liste boşsa → FallbackMessages.
 */
class MessageRepository(
    context: Context,
    private val settings: SettingsRepository
) {
    private val appContext = context.applicationContext

    @Volatile private var cache: RemoteMessages? = null
    @Volatile private var cacheUrl: String = ""
    @Volatile private var cacheAt: Long = 0L
    @Volatile private var lastWarning: String? = null

    suspend fun limitWarning(lang: String, count: Int, limit: Int): WarningText {
        val remoteList = remote()?.warnings?.get(lang).orEmpty().filter { it.isNotBlank() }
        val (list, source) = if (remoteList.isNotEmpty()) {
            remoteList to MessageSource.REMOTE
        } else {
            (FallbackMessages.warnings[lang] ?: FallbackMessages.warnings.getValue("en")) to MessageSource.FALLBACK
        }
        // Art arda aynı cümle gelmesin.
        val candidates = if (list.size > 1) list.filter { it != lastWarning } else list
        val picked = candidates.random()
        lastWarning = picked
        val text = picked
            .replace("{count}", count.toString())
            .replace("{limit}", limit.toString())
        return WarningText(text, source)
    }

    suspend fun pomodoroWarning(lang: String): WarningText {
        val remoteText = remote()?.pomodoro?.get(lang)
        return if (!remoteText.isNullOrBlank()) {
            WarningText(remoteText, MessageSource.REMOTE)
        } else {
            WarningText(
                FallbackMessages.pomodoro[lang] ?: FallbackMessages.pomodoro.getValue("en"),
                MessageSource.FALLBACK
            )
        }
    }

    private suspend fun remote(): RemoteMessages? = withContext(Dispatchers.IO) {
        val url = settings.remoteUrl.value.trim()
        if (url.isBlank() || !isOnline()) return@withContext null

        val cached = cache
        if (cached != null && cacheUrl == url && SystemClock.elapsedRealtime() - cacheAt < CACHE_MS) {
            return@withContext cached
        }
        try {
            fetch(url).also {
                cache = it
                cacheUrl = url
                cacheAt = SystemClock.elapsedRealtime()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Uzak mesajlar alınamadı, yedek kullanılacak: ${e.message}")
            null
        }
    }

    private fun fetch(url: String): RemoteMessages {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parse(body)
        } finally {
            conn.disconnect()
        }
    }

    internal fun parse(json: String): RemoteMessages {
        val root = JSONObject(json)
        val warnings = HashMap<String, List<String>>()
        root.optJSONObject("warnings")?.let { obj ->
            for (lang in obj.keys()) {
                val arr = obj.optJSONArray(lang) ?: continue
                warnings[lang] = (0 until arr.length()).map { arr.optString(it) }
            }
        }
        val pomodoro = HashMap<String, String>()
        root.optJSONObject("pomodoro")?.let { obj ->
            for (lang in obj.keys()) pomodoro[lang] = obj.optString(lang)
        }
        return RemoteMessages(warnings, pomodoro)
    }

    private fun isOnline(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        private const val TAG = "MessageRepository"
        private const val TIMEOUT_MS = 4000
        private const val CACHE_MS = 30 * 60 * 1000L
    }
}
