package com.apkajan.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.apkajan.app.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Kalıcı bildirimli ön plan servisi.
 *  - Süreci canlı tutar (sistem uygulamayı bellekten atmasın).
 *  - Bildirimde canlı sayaç ve Pomodoro geri sayımı gösterir.
 *  - Dakikada bir gün kontrolü yapar → gece yarısı sayaç otomatik 0'a döner.
 *  - Takip durdurulup Pomodoro da kapalıysa kendini kapatır.
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val c = applicationContext.container
        val first = NotificationHelper.buildMonitorNotification(
            this, c.counter.state.value, c.pomodoro.state.value, c.settings.monitoring.value
        )
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NotificationHelper.ID_MONITOR, first,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.ID_MONITOR, first)
        }

        scope.launch {
            combine(
                c.counter.state,
                c.pomodoro.state,
                c.settings.monitoring,
                c.settings.language
            ) { counter, pomo, monitoring, _ -> Triple(counter, pomo, monitoring) }
                .collect { (counter, pomo, monitoring) ->
                    if (!monitoring && !pomo.running) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                    val n = NotificationHelper.buildMonitorNotification(
                        this@MonitorService, counter, pomo, monitoring
                    )
                    try {
                        NotificationManagerCompat.from(this@MonitorService)
                            .notify(NotificationHelper.ID_MONITOR, n)
                    } catch (e: SecurityException) {
                        // Bildirim izni yoksa servis yine de çalışır.
                    }
                }
        }

        // Gün dönümü kontrolü
        scope.launch {
            while (isActive) {
                c.counter.refresh()
                delay(60_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java))
            } catch (e: Exception) {
                // Android 12+: arka plandan FGS başlatma kısıtı. Erişilebilirlik servisi
                // bağlandığında tekrar denenir.
                Log.w("MonitorService", "Ön plan servisi başlatılamadı: ${e.message}")
            }
        }
    }
}
