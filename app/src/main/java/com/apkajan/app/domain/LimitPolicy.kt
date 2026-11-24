package com.apkajan.app.domain

/**
 * Kademeli (katlı) limit hesabı — saf fonksiyonlar, Android bağımlılığı yok.
 *
 * limit = 50 için:
 *   sayaç 0..49   → kat 0 (uyarı yok)
 *   sayaç 50..99  → kat 1 (ilk uyarı 50'de)
 *   sayaç 100..149→ kat 2 (ikinci uyarı 100'de) ... ve böyle devam eder.
 *
 * Kullanıcı uyarıyı kapatıp kaydırmaya devam ederse sayaç durmaz/sıfırlanmaz;
 * yalnızca bir sonraki kata ulaşınca yeni uyarı gelir.
 */
object LimitPolicy {

    fun tierFor(count: Int, limit: Int): Int = if (limit <= 0) 0 else count / limit

    /** Yeni bir kata çıkıldıysa o katı, aksi halde null döner. */
    fun shouldWarn(count: Int, limit: Int, lastWarnedTier: Int): Int? {
        val tier = tierFor(count, limit)
        return if (tier >= 1 && tier > lastWarnedTier) tier else null
    }

    /** Bir sonraki uyarının geleceği sayaç değeri (50 → 100 → 150 ...). */
    fun nextThreshold(count: Int, limit: Int): Int =
        if (limit <= 0) 0 else (tierFor(count, limit) + 1) * limit

    /** İlerleme çubuğu için mevcut kat içindeki oran (0..1). */
    fun progressInTier(count: Int, limit: Int): Float =
        if (limit <= 0) 0f else (count % limit).toFloat() / limit
}
