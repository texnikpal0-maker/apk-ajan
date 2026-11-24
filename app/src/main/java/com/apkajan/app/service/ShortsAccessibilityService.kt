package com.apkajan.app.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.apkajan.app.AppContainer
import com.apkajan.app.R
import com.apkajan.app.container
import com.apkajan.app.core.LocaleHelper
import com.apkajan.app.domain.LimitWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Uygulamanın kalbi:
 *  - Ön plandaki uygulamayı izler (TYPE_WINDOW_STATE_CHANGED).
 *  - Seçili uygulamalarda kısa video kaydırmalarını sayar (TYPE_VIEW_SCROLLED → ShortVideoDetector).
 *  - Başlat aktifken seçili uygulama açıksa üstte canlı sayaç balonu gösterir.
 *  - Limit katına ulaşınca sert uyarı; Pomodoro çalışma aşamasında seçili uygulama açılınca Pomodoro uyarısı.
 */
class ShortsAccessibilityService : AccessibilityService() {

    private lateinit var c: AppContainer
    private lateinit var overlay: OverlayController
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val detector = ShortVideoDetector()

    private var foregroundPkg: String? = null
    private var lastPomodoroWarningAt = 0L
    private var imePackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        c = applicationContext.container
        overlay = OverlayController(this)
        imePackages = loadImePackages()

        // Sayaç / ayar / Pomodoro değiştikçe üst balonu güncelle
        scope.launch {
            combine(
                c.counter.state,
                c.settings.monitoring,
                c.settings.selectedPackages,
                c.pomodoro.state
            ) { _, _, _, _ -> Unit }.collect { updateOverlay() }
        }

        // Kademeli limit uyarıları
        scope.launch {
            c.counter.warnings.collect { showLimitWarning(it) }
        }

        if (c.settings.monitoring.value || c.pomodoro.state.value.running) {
            MonitorService.start(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::c.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                onWindowChanged(pkg, event.className?.toString().orEmpty())
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> onScrolled(pkg, event)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (::overlay.isInitialized) overlay.removeAll()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------

    private fun isTracked(pkg: String) = pkg in c.settings.selectedPackages.value

    private fun onWindowChanged(pkg: String, className: String) {
        // Kendi bindirme pencerelerimiz, sistem arayüzü ve klavye ön plan uygulamasını değiştirmez.
        // (Kendi Activity'miz açılırsa ise ön plan değişmiş sayılır → balon gizlenir.)
        if (pkg == packageName && !className.endsWith("Activity")) return
        if (pkg in IGNORED_PACKAGES || pkg in imePackages) return
        if (pkg == foregroundPkg) return
        foregroundPkg = pkg
        detector.reset()
        updateOverlay()
        if (isTracked(pkg)) maybePomodoroWarning(force = true)
    }

    private fun onScrolled(pkg: String, event: AccessibilityEvent) {
        if (!isTracked(pkg)) return
        if (foregroundPkg != pkg) {
            foregroundPkg = pkg
            updateOverlay()
        }
        maybePomodoroWarning(force = false)

        if (!c.settings.monitoring.value) return
        val screenHeight = resources.displayMetrics.heightPixels
        if (detector.isNewShortVideo(event, pkg, screenHeight)) {
            scope.launch(Dispatchers.IO) { c.counter.onShortVideo(pkg) }
        }
    }

    /** Başlat aktif + seçili uygulama önde → üstte "▶ 23 / 50" balonu. */
    private fun updateOverlay() {
        if (!::overlay.isInitialized) return
        val pkg = foregroundPkg
        val monitoring = c.settings.monitoring.value
        val pomo = c.pomodoro.state.value
        val visible = pkg != null && isTracked(pkg) && (monitoring || pomo.running)
        if (!visible) {
            overlay.hideCounter()
            return
        }
        val s = c.counter.state.value
        val text = buildString {
            if (monitoring) append("▶ ${s.count} / ${s.limit}")
            if (pomo.isWorking) {
                if (isNotEmpty()) append("  ·  ")
                append("🍅")
            }
        }
        val level = when {
            !monitoring -> OverlayController.Level.NORMAL
            s.count >= s.limit -> OverlayController.Level.OVER
            s.count >= s.limit * 0.8 -> OverlayController.Level.NEAR
            else -> OverlayController.Level.NORMAL
        }
        overlay.showCounter(text, level)
    }

    private fun localized() = LocaleHelper.wrap(this, c.settings.language.value)

    private fun showLimitWarning(w: LimitWarning) {
        scope.launch {
            val lang = c.settings.language.value
            val msg = c.messages.limitWarning(lang, w.count, w.limit)
            val lc = localized()
            val title = if (w.tier <= 1) {
                lc.getString(R.string.warning_title_first)
            } else {
                lc.getString(R.string.warning_title_next, w.tier)
            }
            overlay.showWarning(
                title = title,
                message = msg.text,
                footer = lc.getString(R.string.warning_stats, w.count, w.limit),
                okText = lc.getString(R.string.warning_ok),
                waitTextFormat = { s -> lc.getString(R.string.warning_ok_wait, s) },
                // Her katta kilit süresi biraz uzar: 3, 4, 5 ... (en fazla 8 sn)
                lockSeconds = (2 + w.tier).coerceAtMost(8)
            )
        }
    }

    /**
     * Pomodoro çalışma aşamasındayken seçili uygulama açılırsa: hemen uyarı.
     * Uygulamada kalıp kaydırmaya devam ederse en fazla dakikada bir tekrarlanır.
     */
    private fun maybePomodoroWarning(force: Boolean) {
        if (!c.pomodoro.state.value.isWorking) return
        if (overlay.isWarningShowing) return
        val now = SystemClock.elapsedRealtime()
        val gap = if (force) 5_000L else 60_000L
        if (now - lastPomodoroWarningAt < gap) return
        lastPomodoroWarningAt = now
        scope.launch {
            val lang = c.settings.language.value
            val msg = c.messages.pomodoroWarning(lang)
            val lc = localized()
            overlay.showWarning(
                title = lc.getString(R.string.pomodoro_warning_title),
                message = msg.text,
                footer = null,
                okText = lc.getString(R.string.warning_ok),
                waitTextFormat = { s -> lc.getString(R.string.warning_ok_wait, s) },
                lockSeconds = 3,
                accent = 0xFFE8572A.toInt()
            )
        }
    }

    private fun loadImePackages(): Set<String> = try {
        getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()
    } catch (e: Exception) {
        emptySet()
    }

    companion object {
        private val IGNORED_PACKAGES = setOf("com.android.systemui", "android")
    }
}
