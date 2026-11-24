package com.apkajan.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LimitPolicyTest {

    @Test
    fun noWarningBelowLimit() {
        assertNull(LimitPolicy.shouldWarn(count = 49, limit = 50, lastWarnedTier = 0))
    }

    @Test
    fun firstWarningAtLimit() {
        assertEquals(1, LimitPolicy.shouldWarn(count = 50, limit = 50, lastWarnedTier = 0))
    }

    @Test
    fun noRepeatWithinSameTier() {
        // Uyarı kapatıldı, kaydırma sürüyor: 51..99 arası yeni uyarı yok.
        for (c in 51..99) assertNull(LimitPolicy.shouldWarn(c, 50, lastWarnedTier = 1))
    }

    @Test
    fun nextWarningsAtMultiples() {
        assertEquals(2, LimitPolicy.shouldWarn(100, 50, lastWarnedTier = 1))
        assertEquals(3, LimitPolicy.shouldWarn(150, 50, lastWarnedTier = 2))
    }

    @Test
    fun simulatedScrollingSession() {
        var lastTier = 0
        val warnedAt = mutableListOf<Int>()
        for (count in 1..160) {
            LimitPolicy.shouldWarn(count, 50, lastTier)?.let { lastTier = it; warnedAt += count }
        }
        assertEquals(listOf(50, 100, 150), warnedAt)
    }

    @Test
    fun nextThreshold() {
        assertEquals(50, LimitPolicy.nextThreshold(0, 50))
        assertEquals(100, LimitPolicy.nextThreshold(50, 50))
        assertEquals(150, LimitPolicy.nextThreshold(120, 50))
    }
}
