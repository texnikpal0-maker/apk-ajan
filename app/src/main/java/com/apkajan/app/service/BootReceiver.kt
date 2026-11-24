package com.apkajan.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apkajan.app.container

/** Telefon yeniden açıldığında veya uygulama güncellendiğinde takibi kaldığı yerden sürdürür. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.applicationContext.container
        if (c.settings.monitoring.value || c.pomodoro.state.value.running) {
            MonitorService.start(context)
        }
    }
}
