package com.apkajan.app.service

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.apkajan.app.data.apps.QuickApps

/**
 * TYPE_VIEW_SCROLLED olaylarından "yeni bir kısa videoya geçildi" sonucunu çıkarır.
 *
 * 1) Akış tanıma (hangi kaydırma kısa video akışı?):
 *    - YouTube  : kaydırılan görünüm kimliği "reel"/"shorts" içerir (Shorts oynatıcı).
 *                 Ana sayfa / abonelik akışı sayılmaz.
 *    - Instagram: kimlik "clips"/"reel" içerir (Reels görüntüleyici). Normal akış sayılmaz.
 *    - TikTok   : uygulamanın tamamı kısa video; ekranın yarısından uzun her dikey sayfa sayılır.
 *    - Diğer    : ekranın %60'ından uzun ViewPager/RecyclerView.
 * 2) Tekrar önleme:
 *    - Olay sayfa indeksi veriyorsa (fromIndex) yalnızca indeks değişince sayılır.
 *    - İndeks yoksa, bir kaydırma hareketinin birden çok olayı tek sayılsın diye zaman aralığı uygulanır.
 */
class ShortVideoDetector {

    private var lastIndex = -1
    private var lastSourceKey: String? = null
    private var lastCountAt = 0L

    fun reset() {
        lastIndex = -1
        lastSourceKey = null
    }

    fun isNewShortVideo(event: AccessibilityEvent, pkg: String, screenHeight: Int): Boolean {
        val source = event.source ?: return false
        val viewId = source.viewIdResourceName.orEmpty().lowercase()
        val rect = Rect().also { source.getBoundsInScreen(it) }
        val className = event.className?.toString().orEmpty()

        if (!looksLikeShortsFeed(pkg, viewId, className, rect.height(), screenHeight)) return false

        val now = SystemClock.elapsedRealtime()
        val key = viewId.ifEmpty { className }
        if (key != lastSourceKey) {
            lastSourceKey = key
            lastIndex = -1
        }

        val idx = event.fromIndex
        if (idx >= 0) {
            if (lastIndex == -1) {
                // İlk olay: başlangıç noktası. Bir sonraki indeks değişimi yeni video demektir.
                lastIndex = idx
                return false
            }
            if (idx == lastIndex) return false
            lastIndex = idx
            if (now - lastCountAt < MIN_GAP_INDEXED_MS) return false
            lastCountAt = now
            return true
        }

        if (now - lastCountAt < MIN_GAP_UNINDEXED_MS) return false
        lastCountAt = now
        return true
    }

    private fun looksLikeShortsFeed(
        pkg: String,
        viewId: String,
        className: String,
        height: Int,
        screenHeight: Int
    ): Boolean {
        val tall = { ratio: Float -> screenHeight > 0 && height >= screenHeight * ratio }
        return when {
            pkg == QuickApps.YOUTUBE_PKG -> viewId.contains("reel") || viewId.contains("shorts")
            pkg == QuickApps.INSTAGRAM_PKG -> viewId.contains("clips") || viewId.contains("reel")
            pkg in QuickApps.TIKTOK_PKGS -> tall(0.5f)
            else -> tall(0.6f) && (className.contains("ViewPager") || className.contains("RecyclerView"))
        }
    }

    companion object {
        private const val MIN_GAP_INDEXED_MS = 350L
        private const val MIN_GAP_UNINDEXED_MS = 1200L
    }
}
