package com.apkajan.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.apkajan.app.R
import com.apkajan.app.core.LocaleHelper
import com.apkajan.app.core.Permissions
import com.apkajan.app.data.settings.SettingsRepository
import com.apkajan.app.domain.CounterState
import com.apkajan.app.domain.PomodoroPhase
import com.apkajan.app.domain.PomodoroState
import com.apkajan.app.ui.MainActivity

object NotificationHelper {
    const val CHANNEL_MONITOR = "monitor"
    const val CHANNEL_POMODORO = "pomodoro"
    const val ID_MONITOR = 1001
    private const val ID_POMODORO = 1002

    private fun localized(ctx: Context): Context =
        LocaleHelper.wrap(ctx, SettingsRepository.readLanguage(ctx))

    fun createChannels(ctx: Context) {
        val lc = localized(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                lc.getString(R.string.notif_channel_monitor),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_POMODORO,
                lc.getString(R.string.notif_channel_pomodoro),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /** Ön plan servisinin kalıcı bildirimi: sayaç + (varsa) Pomodoro geri sayımı. */
    fun buildMonitorNotification(
        ctx: Context,
        counter: CounterState,
        pomodoro: PomodoroState,
        monitoring: Boolean
    ): Notification {
        val lc = localized(ctx)
        val b = NotificationCompat.Builder(ctx, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(lc.getString(R.string.notif_monitor_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent(ctx))

        val text = if (monitoring) {
            lc.getString(R.string.notif_monitor_text, counter.count, counter.limit)
        } else {
            lc.getString(R.string.pomodoro)
        }
        b.setContentText(text)

        if (pomodoro.running) {
            val phase = lc.getString(
                if (pomodoro.phase == PomodoroPhase.WORK) R.string.pomodoro_work else R.string.pomodoro_break
            )
            b.setSubText("🍅 $phase")
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(pomodoro.endAt)
                .setShowWhen(true)
        }
        return b.build()
    }

    fun notifyPomodoroPhase(ctx: Context, newPhase: PomodoroPhase, minutes: Int) {
        if (!Permissions.hasNotificationPermission(ctx)) return
        val lc = localized(ctx)
        val text = if (newPhase == PomodoroPhase.BREAK) {
            lc.getString(R.string.notif_work_done, minutes)
        } else {
            lc.getString(R.string.notif_break_done)
        }
        val n = NotificationCompat.Builder(ctx, CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🍅 " + lc.getString(R.string.pomodoro))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(ID_POMODORO, n)
        } catch (e: SecurityException) {
            // Bildirim izni yok: sessizce geç.
        }
    }
}
