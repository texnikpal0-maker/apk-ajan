package com.apkajan.app.domain

import com.apkajan.app.core.DayKey
import com.apkajan.app.data.local.AppUsageDao
import com.apkajan.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CounterState(val date: String, val count: Int, val limit: Int) {
    val tier: Int get() = LimitPolicy.tierFor(count, limit)
    val nextWarningAt: Int get() = LimitPolicy.nextThreshold(count, limit)
    val overLimit: Boolean get() = count >= limit
}

/** Bir kata ulaşıldığında yayılan olay; erişilebilirlik servisi bunu uyarı penceresine çevirir. */
data class LimitWarning(val count: Int, val limit: Int, val tier: Int, val packageName: String)

/**
 * Günlük sayaç:
 *  - Her kısa video Room'a (gün + uygulama) +1 olarak yazılır → istatistik geçmişi kalıcıdır.
 *  - Gösterilen sayaç = bugünkü toplam − "Sıfırla" ofseti.
 *  - Gün değişince (gece yarısı) ofset ve uyarı katı otomatik sıfırlanır; eski günler veritabanında kalır.
 */
class CounterEngine(
    private val dao: AppUsageDao,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()

    private val _state = MutableStateFlow(CounterState(DayKey.today(), 0, settings.limit.value))
    val state: StateFlow<CounterState> = _state.asStateFlow()

    private val _warnings = MutableSharedFlow<LimitWarning>(extraBufferCapacity = 4)
    val warnings: SharedFlow<LimitWarning> = _warnings.asSharedFlow()

    init {
        scope.launch { refresh() }
        scope.launch {
            settings.limit.collect { newLimit ->
                mutex.withLock {
                    val count = _state.value.count
                    // Limit yükseltilirse eski kat bilgisi yeni limite göre düzeltilir.
                    settings.lastWarnedTier =
                        minOf(settings.lastWarnedTier, LimitPolicy.tierFor(count, newLimit))
                    _state.value = _state.value.copy(limit = newLimit)
                }
            }
        }
    }

    /** Gün değiştiyse defteri sıfırlar. Kilit içinde çağrılmalı. */
    private fun rollDayIfNeeded(): String {
        val today = DayKey.today()
        if (settings.counterDate != today) {
            settings.counterDate = today
            settings.counterOffset = 0
            settings.lastWarnedTier = 0
        }
        return today
    }

    /** Ön plan servisi dakikada bir çağırır: gece yarısı sayaç ekranda da 0'a döner. */
    suspend fun refresh() = mutex.withLock {
        val today = rollDayIfNeeded()
        val total = dao.totalFor(today)
        _state.value = CounterState(
            today,
            (total - settings.counterOffset).coerceAtLeast(0),
            settings.limit.value
        )
    }

    /** Erişilebilirlik servisi yeni bir kısa videoya geçildiğini algılayınca çağırır. */
    suspend fun onShortVideo(pkg: String) {
        val warning = mutex.withLock {
            val today = rollDayIfNeeded()
            dao.increment(today, pkg)
            val total = dao.totalFor(today)
            val count = (total - settings.counterOffset).coerceAtLeast(0)
            val limit = settings.limit.value
            _state.value = CounterState(today, count, limit)

            LimitPolicy.shouldWarn(count, limit, settings.lastWarnedTier)?.let { tier ->
                settings.lastWarnedTier = tier
                LimitWarning(count, limit, tier, pkg)
            }
        }
        if (warning != null) _warnings.emit(warning)
    }

    /** "Sıfırla": sadece canlı sayaç 0'a döner, istatistik geçmişi silinmez. */
    fun reset() {
        scope.launch {
            mutex.withLock {
                val today = rollDayIfNeeded()
                settings.counterOffset = dao.totalFor(today)
                settings.lastWarnedTier = 0
                _state.value = CounterState(today, 0, settings.limit.value)
            }
        }
    }
}
