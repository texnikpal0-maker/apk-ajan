package com.apkajan.app.domain

import android.content.Context
import com.apkajan.app.data.settings.SettingsRepository
import com.apkajan.app.service.MonitorService
import com.apkajan.app.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PomodoroPhase { WORK, BREAK }

data class PomodoroState(
    val running: Boolean = false,
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val endAt: Long = 0L,
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val completedCycles: Int = 0
) {
    /** Sosyal medya uyarısı yalnızca ÇALIŞMA aşamasında verilir; molada serbest. */
    val isWorking: Boolean get() = running && phase == PomodoroPhase.WORK

    fun remainingMillis(now: Long = System.currentTimeMillis()): Long = (endAt - now).coerceAtLeast(0L)

    val phaseTotalMillis: Long
        get() = (if (phase == PomodoroPhase.WORK) workMinutes else breakMinutes) * 60_000L
}

/**
 * Pomodoro zamanlayıcısı. Bitiş anı (endAt) kalıcı tutulur; süreç ölüp yeniden doğsa da kaldığı yerden sürer.
 * Çalışma bitince → mola, mola bitince → çalışma; her geçişte bildirim gönderilir.
 */
class PomodoroEngine(
    private val context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    private var ticker: Job? = null

    init {
        scope.launch {
            combine(settings.workMinutes, settings.breakMinutes) { w, b -> w to b }
                .collect { (w, b) -> _state.update { it.copy(workMinutes = w, breakMinutes = b) } }
        }
    }

    fun resumeIfNeeded() {
        if (_state.value.running) startTicker()
    }

    fun start() {
        val s = _state.value
        set(
            s.copy(
                running = true,
                phase = PomodoroPhase.WORK,
                endAt = System.currentTimeMillis() + s.workMinutes * 60_000L,
                completedCycles = 0
            )
        )
        startTicker()
        MonitorService.start(context)
    }

    fun stop() {
        ticker?.cancel()
        ticker = null
        set(_state.value.copy(running = false, phase = PomodoroPhase.WORK, endAt = 0L))
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                tick()
                delay(1000)
            }
        }
    }

    private fun tick() {
        val s = _state.value
        if (!s.running) return
        val now = System.currentTimeMillis()
        if (now < s.endAt) return
        if (s.phase == PomodoroPhase.WORK) {
            set(
                s.copy(
                    phase = PomodoroPhase.BREAK,
                    endAt = now + s.breakMinutes * 60_000L,
                    completedCycles = s.completedCycles + 1
                )
            )
            NotificationHelper.notifyPomodoroPhase(context, PomodoroPhase.BREAK, s.breakMinutes)
        } else {
            set(s.copy(phase = PomodoroPhase.WORK, endAt = now + s.workMinutes * 60_000L))
            NotificationHelper.notifyPomodoroPhase(context, PomodoroPhase.WORK, s.workMinutes)
        }
    }

    private fun set(s: PomodoroState) {
        _state.value = s
        settings.pomoRunning = s.running
        settings.pomoPhase = s.phase.name
        settings.pomoEndAt = s.endAt
        settings.pomoCycles = s.completedCycles
    }

    private fun load() = PomodoroState(
        running = settings.pomoRunning,
        phase = runCatching { PomodoroPhase.valueOf(settings.pomoPhase) }.getOrDefault(PomodoroPhase.WORK),
        endAt = settings.pomoEndAt,
        workMinutes = settings.workMinutes.value,
        breakMinutes = settings.breakMinutes.value,
        completedCycles = settings.pomoCycles
    )
}
