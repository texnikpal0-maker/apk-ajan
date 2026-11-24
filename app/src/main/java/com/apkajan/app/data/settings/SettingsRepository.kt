package com.apkajan.app.data.settings

import android.content.Context
import com.apkajan.app.core.AppLanguage
import com.apkajan.app.data.apps.QuickApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kullanıcı ayarları (SharedPreferences + StateFlow).
 * SharedPreferences seçildi çünkü dil, Activity.attachBaseContext içinde senkron okunmalı.
 * Gözlenen ayarlar StateFlow olarak sunulur; UI ve servisler anında güncellenir.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(readLanguage(context))
    val language: StateFlow<String> = _language.asStateFlow()
    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
        _language.value = lang
    }

    private val _limit = MutableStateFlow(prefs.getInt(KEY_LIMIT, 50))
    val limit: StateFlow<Int> = _limit.asStateFlow()
    fun setLimit(value: Int) {
        val v = value.coerceIn(1, 9999)
        prefs.edit().putInt(KEY_LIMIT, v).apply()
        _limit.value = v
    }

    /** Başlat/Durdur durumu. Telefon yeniden başlasa bile hatırlanır. */
    private val _monitoring = MutableStateFlow(prefs.getBoolean(KEY_MONITORING, false))
    val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()
    fun setMonitoring(on: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING, on).apply()
        _monitoring.value = on
    }

    private val _selectedPackages = MutableStateFlow(
        prefs.getStringSet(KEY_PACKAGES, null)?.toSet() ?: QuickApps.defaultPackages
    )
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()
    fun setSelectedPackages(set: Set<String>) {
        prefs.edit().putStringSet(KEY_PACKAGES, HashSet(set)).apply()
        _selectedPackages.value = set.toSet()
    }

    private val _remoteUrl = MutableStateFlow(prefs.getString(KEY_REMOTE_URL, DEFAULT_REMOTE_URL) ?: "")
    val remoteUrl: StateFlow<String> = _remoteUrl.asStateFlow()
    fun setRemoteUrl(url: String) {
        prefs.edit().putString(KEY_REMOTE_URL, url.trim()).apply()
        _remoteUrl.value = url.trim()
    }

    private val _workMinutes = MutableStateFlow(prefs.getInt(KEY_WORK_MIN, 25))
    val workMinutes: StateFlow<Int> = _workMinutes.asStateFlow()
    fun setWorkMinutes(m: Int) {
        val v = m.coerceIn(1, 180)
        prefs.edit().putInt(KEY_WORK_MIN, v).apply()
        _workMinutes.value = v
    }

    private val _breakMinutes = MutableStateFlow(prefs.getInt(KEY_BREAK_MIN, 5))
    val breakMinutes: StateFlow<Int> = _breakMinutes.asStateFlow()
    fun setBreakMinutes(m: Int) {
        val v = m.coerceIn(1, 60)
        prefs.edit().putInt(KEY_BREAK_MIN, v).apply()
        _breakMinutes.value = v
    }

    // ---- Sayaç defteri (gözlenmez, sadece CounterEngine kullanır) ----
    var counterDate: String
        get() = prefs.getString(KEY_COUNTER_DATE, "") ?: ""
        set(v) = prefs.edit().putString(KEY_COUNTER_DATE, v).apply()

    /** "Sıfırla"ya basıldığı andaki günlük toplam. Sayaç = günlük toplam - offset. */
    var counterOffset: Int
        get() = prefs.getInt(KEY_COUNTER_OFFSET, 0)
        set(v) = prefs.edit().putInt(KEY_COUNTER_OFFSET, v).apply()

    /** Bugün en son hangi kat için uyarı gösterildi (1 = limit, 2 = 2×limit ...). */
    var lastWarnedTier: Int
        get() = prefs.getInt(KEY_LAST_TIER, 0)
        set(v) = prefs.edit().putInt(KEY_LAST_TIER, v).apply()

    // ---- Pomodoro kalıcı durumu (süreç ölse bile devam edebilsin) ----
    var pomoRunning: Boolean
        get() = prefs.getBoolean(KEY_POMO_RUNNING, false)
        set(v) = prefs.edit().putBoolean(KEY_POMO_RUNNING, v).apply()
    var pomoPhase: String
        get() = prefs.getString(KEY_POMO_PHASE, "WORK") ?: "WORK"
        set(v) = prefs.edit().putString(KEY_POMO_PHASE, v).apply()
    var pomoEndAt: Long
        get() = prefs.getLong(KEY_POMO_END, 0L)
        set(v) = prefs.edit().putLong(KEY_POMO_END, v).apply()
    var pomoCycles: Int
        get() = prefs.getInt(KEY_POMO_CYCLES, 0)
        set(v) = prefs.edit().putInt(KEY_POMO_CYCLES, v).apply()

    companion object {
        const val PREFS = "apk_ajan_settings"

        /**
         * Varsayılan uzak mesaj adresi: bu depodaki remote/messages.json.
         * GitHub'da o dosyayı düzenleyince uygulama en geç 30 dk içinde yeni cümleleri kullanır.
         * Uygulamadaki ayar alanından başka bir adres girilebilir; boş bırakılırsa yerleşik mesajlar kullanılır.
         */
        const val DEFAULT_REMOTE_URL =
            "https://raw.githubusercontent.com/texnikpal0-maker/apk-ajan/main/remote/messages.json"

        private const val KEY_LANG = "language"
        private const val KEY_LIMIT = "limit"
        private const val KEY_MONITORING = "monitoring"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_REMOTE_URL = "remote_url"
        private const val KEY_WORK_MIN = "work_min"
        private const val KEY_BREAK_MIN = "break_min"
        private const val KEY_COUNTER_DATE = "counter_date"
        private const val KEY_COUNTER_OFFSET = "counter_offset"
        private const val KEY_LAST_TIER = "last_tier"
        private const val KEY_POMO_RUNNING = "pomo_running"
        private const val KEY_POMO_PHASE = "pomo_phase"
        private const val KEY_POMO_END = "pomo_end"
        private const val KEY_POMO_CYCLES = "pomo_cycles"

        /** attachBaseContext'te (container hazır olmadan) dil okumak için. */
        fun readLanguage(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, null)
                ?: AppLanguage.systemDefault()
    }
}
