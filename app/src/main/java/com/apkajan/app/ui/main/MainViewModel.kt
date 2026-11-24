package com.apkajan.app.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.apkajan.app.AppContainer
import com.apkajan.app.ApkAjanApp
import com.apkajan.app.core.Permissions
import com.apkajan.app.data.apps.QuickApp
import com.apkajan.app.service.MonitorService

class MainViewModel(
    private val app: Application,
    private val c: AppContainer
) : ViewModel() {

    val counter = c.counter.state
    val monitoring = c.settings.monitoring
    val limit = c.settings.limit
    val selectedPackages = c.settings.selectedPackages
    val remoteUrl = c.settings.remoteUrl
    val language = c.settings.language
    val pomodoro = c.pomodoro.state
    val workMinutes = c.settings.workMinutes
    val breakMinutes = c.settings.breakMinutes

    fun isInstalled(q: QuickApp) = c.installedApps.isAnyInstalled(q)
    fun label(pkg: String) = c.installedApps.label(pkg)

    /** false dönerse erişilebilirlik servisi kapalıdır; UI kullanıcıyı yönlendirir. */
    fun start(): Boolean {
        if (!Permissions.isAccessibilityEnabled(app)) return false
        c.settings.setMonitoring(true)
        MonitorService.start(app)
        return true
    }

    fun stop() = c.settings.setMonitoring(false)

    fun resetCounter() = c.counter.reset()

    fun setLimit(v: Int) = c.settings.setLimit(v)

    fun toggleQuickApp(q: QuickApp) {
        val cur = selectedPackages.value
        val next = if (q.packages.any { it in cur }) cur - q.packages.toSet() else cur + q.packages
        c.settings.setSelectedPackages(next)
    }

    fun setRemoteUrl(url: String) = c.settings.setRemoteUrl(url)

    fun setWorkMinutes(m: Int) = c.settings.setWorkMinutes(m)
    fun setBreakMinutes(m: Int) = c.settings.setBreakMinutes(m)
    fun startPomodoro() = c.pomodoro.start()
    fun stopPomodoro() = c.pomodoro.stop()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ApkAjanApp
                MainViewModel(app, app.container)
            }
        }
    }
}
