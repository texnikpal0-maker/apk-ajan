package com.apkajan.app

import android.app.Application
import android.content.Context
import com.apkajan.app.data.apps.InstalledAppsRepository
import com.apkajan.app.data.local.AppDatabase
import com.apkajan.app.data.remote.MessageRepository
import com.apkajan.app.data.settings.SettingsRepository
import com.apkajan.app.domain.CounterEngine
import com.apkajan.app.domain.PomodoroEngine
import com.apkajan.app.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ApkAjanApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        container.pomodoro.resumeIfNeeded()
    }
}

/**
 * Basit el yapımı bağımlılık kabı (DI). Tüm katmanlar tek süreçte paylaşılır:
 * Activity, AccessibilityService ve ön plan servisi aynı sayaç / Pomodoro durumunu görür.
 */
class AppContainer(app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(app)
    val database = AppDatabase.get(app)
    val usageDao = database.usageDao()
    val messages = MessageRepository(app, settings)
    val installedApps = InstalledAppsRepository(app)

    val counter = CounterEngine(usageDao, settings, appScope)
    val pomodoro = PomodoroEngine(app, settings, appScope)
}

val Context.container: AppContainer
    get() = (applicationContext as ApkAjanApp).container
