package com.apkajan.app.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Ekranın üstündeki canlı sayaç balonu ve tam ekran sert uyarı penceresi.
 *
 * Pencere tipi: önce TYPE_ACCESSIBILITY_OVERLAY (erişilebilirlik servisine özel, ek izin gerektirmez,
 * tüm uygulamaların üstünde durur). Herhangi bir nedenle eklenemezse ve "diğer uygulamaların üzerinde
 * göster" izni verilmişse TYPE_APPLICATION_OVERLAY ile tekrar dener.
 *
 * Görünümler Compose yerine klasik View ile kuruldu: servis içinde yaşam döngüsü sahibi gerektirmez,
 * hafiftir ve her uygulamanın üstünde güvenle çalışır.
 */
class OverlayController(private val service: AccessibilityService) {

    enum class Level { NORMAL, NEAR, OVER }

    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private var pill: TextView? = null
    private var warningRoot: View? = null
    private var countdown: Runnable? = null

    val isWarningShowing: Boolean get() = warningRoot != null

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, service.resources.displayMetrics).toInt()

    private fun statusBarHeight(): Int {
        val id = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) service.resources.getDimensionPixelSize(id) else dp(24f)
    }

    private fun addWindow(view: View, params: WindowManager.LayoutParams): Boolean {
        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        try {
            wm.addView(view, params)
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Erişilebilirlik katmanı eklenemedi: ${e.message}")
        }
        if (Settings.canDrawOverlays(service)) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            try {
                wm.addView(view, params)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Uygulama katmanı da eklenemedi: ${e.message}")
            }
        }
        return false
    }

    private fun removeWindow(view: View?) {
        if (view == null) return
        try {
            wm.removeViewImmediate(view)
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------------ Üst sayaç

    fun showCounter(text: String, level: Level) {
        val view = pill ?: createPill()?.also { pill = it } ?: return
        view.text = text
        (view.background as GradientDrawable).setColor(
            when (level) {
                Level.NORMAL -> 0xE6222A3A.toInt()
                Level.NEAR -> 0xE6E08600.toInt()
                Level.OVER -> 0xE6D32F2F.toInt()
            }
        )
    }

    fun hideCounter() {
        removeWindow(pill)
        pill = null
    }

    private fun createPill(): TextView? {
        val tv = TextView(service).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
            background = GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat()
                setColor(0xE6222A3A.toInt())
            }
            elevation = dp(4f).toFloat()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight() + dp(4f)
        }
        return if (addWindow(tv, params)) tv else null
    }

    // ------------------------------------------------------------------ Uyarı penceresi

    /**
     * Tam ekran, dokunmayı engelleyen sert uyarı. "Anladım" düğmesi birkaç saniye kilitli kalır
     * ki mesaj okunmadan geçilmesin.
     */
    fun showWarning(
        title: String,
        message: String,
        footer: String?,
        okText: String,
        waitTextFormat: (Int) -> String,
        lockSeconds: Int = 3,
        accent: Int = 0xFFD32F2F.toInt()
    ) {
        dismissWarning()

        val root = FrameLayout(service).apply { setBackgroundColor(0xCC000000.toInt()) }

        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f), dp(24f), dp(24f), dp(20f))
            background = GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat()
                setColor(0xFF15161C.toInt())
                setStroke(dp(3f), accent)
            }
        }

        card.addView(TextView(service).apply {
            text = title
            setTextColor(accent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        card.addView(TextView(service).apply {
            text = message
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(16f), 0, dp(8f))
        })
        if (!footer.isNullOrBlank()) {
            card.addView(TextView(service).apply {
                text = footer
                setTextColor(0xFFB0B3C0.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12f))
            })
        }
        val button = Button(service).apply {
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(accent)
            }
            setOnClickListener { dismissWarning() }
        }
        card.addView(
            button,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52f)).apply {
                topMargin = dp(8f)
            }
        )

        root.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                leftMargin = dp(24f)
                rightMargin = dp(24f)
            }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        if (!addWindow(root, params)) return
        warningRoot = root

        // Düğme kilidi geri sayımı
        var left = lockSeconds
        button.isEnabled = false
        button.alpha = 0.5f
        button.text = waitTextFormat(left)
        val r = object : Runnable {
            override fun run() {
                left--
                if (left <= 0) {
                    button.isEnabled = true
                    button.alpha = 1f
                    button.text = okText
                } else {
                    button.text = waitTextFormat(left)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        countdown = r
        handler.postDelayed(r, 1000)
    }

    fun dismissWarning() {
        countdown?.let { handler.removeCallbacks(it) }
        countdown = null
        removeWindow(warningRoot)
        warningRoot = null
    }

    fun removeAll() {
        hideCounter()
        dismissWarning()
    }

    companion object {
        private const val TAG = "OverlayController"
    }
}
