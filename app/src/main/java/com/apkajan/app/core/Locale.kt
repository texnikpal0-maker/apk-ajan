package com.apkajan.app.core

import android.content.Context
import android.content.res.Configuration
import java.time.LocalDate
import java.util.Locale

/** Uygulama içinde seçilebilen 4 dil. */
enum class AppLanguage(val code: String, val label: String) {
    AZ("az", "AZ"),
    TR("tr", "TR"),
    EN("en", "EN"),
    RU("ru", "RU");

    companion object {
        fun from(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: EN

        /** Telefonun dili desteklenenlerden biriyse onu, değilse İngilizceyi seçer. */
        fun systemDefault(): String {
            val sys = Locale.getDefault().language
            return entries.firstOrNull { it.code == sys }?.code ?: EN.code
        }
    }
}

/**
 * Seçilen dile göre kaynakları (strings.xml) yükleyen Context üretir.
 * Activity (attachBaseContext), servis bindirmesi ve bildirimler aynı yardımcıyı kullanır,
 * böylece sistem dili ne olursa olsun tüm metinler seçilen dilde görünür.
 */
object LocaleHelper {
    fun wrap(base: Context, lang: String): Context {
        val locale = Locale(lang)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}

/** Günlük kayıt anahtarı: yyyy-MM-dd (yerel saat dilimi). Gün değişince sayaç kendiliğinden sıfırlanır. */
object DayKey {
    fun today(): String = LocalDate.now().toString()
    fun daysAgo(n: Long): String = LocalDate.now().minusDays(n).toString()
}
