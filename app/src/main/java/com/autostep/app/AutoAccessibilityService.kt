package com.autostep.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Performs gestures (tap / hold / swipe) with dispatchGesture and hosts the floating panel
 * (TYPE_ACCESSIBILITY_OVERLAY, so no separate "draw over apps" permission is needed).
 */
class AutoAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: AutoAccessibilityService? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private var floating: FloatingUi? = null

    val isFloatingShown: Boolean get() = floating?.isShown == true

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Store.init(applicationContext)
        floating = FloatingUi(this)
        floating?.show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        floating?.hide()
        floating = null
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // ---- floating panel API (used by MainActivity) ----
    fun showFloating() { main.post { floating?.show() } }
    fun hideFloating() { main.post { floating?.hide() } }
    fun startPick(mode: PickMode, delayMs: Long, cb: (PickResult?) -> Unit) {
        main.post { floating?.startPick(mode, delayMs, cb) }
    }
    fun openApp() { main.post { floating?.openApp() } }

    // ---- gestures (call from a background thread) ----
    private fun clampX(x: Int): Float = x.coerceIn(0, resources.displayMetrics.widthPixels - 1).toFloat()
    private fun clampY(y: Int): Float = y.coerceIn(0, resources.displayMetrics.heightPixels - 1).toFloat()

    fun tap(x: Int, y: Int): Boolean = swipe(x, y, x, y, 40)

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): Boolean {
        val dur = durationMs.coerceIn(1L, GestureDescription.getMaxGestureDuration())
        val path = Path().apply {
            moveTo(clampX(x1), clampY(y1))
            if (x1 != x2 || y1 != y2) lineTo(clampX(x2), clampY(y2))
        }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, dur))
            .build()
        return dispatchAndWait(g, dur + 3000)
    }

    /**
     * Holds a finger down at (x, y). Long/infinite holds are sent as chained 1s strokes
     * (willContinue = true) so the finger never lifts until the end or until shouldStop() is true.
     */
    fun hold(x: Int, y: Int, totalMs: Long, infinite: Boolean, shouldStop: () -> Boolean) {
        val px = clampX(x)
        val py = clampY(y)
        val chunk = 1000L
        var remaining = totalMs.coerceAtLeast(1L)
        var prev: GestureDescription.StrokeDescription? = null
        while (true) {
            val stopNow = shouldStop()
            val thisMs = if (stopNow) 10L else if (infinite) chunk else minOf(chunk, remaining)
            val last = stopNow || (!infinite && remaining <= chunk)
            val path = Path().apply { moveTo(px, py) }
            val p = prev
            val stroke = if (p == null) {
                GestureDescription.StrokeDescription(path, 0, thisMs, !last)
            } else {
                p.continueStroke(path, 0, thisMs, !last)
            }
            val ok = dispatchAndWait(GestureDescription.Builder().addStroke(stroke).build(), thisMs + 3000)
            if (!ok || last) return
            prev = stroke
            remaining -= thisMs
        }
    }

    private fun dispatchAndWait(g: GestureDescription, timeoutMs: Long): Boolean {
        val latch = CountDownLatch(1)
        val ok = AtomicBoolean(false)
        val cb = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                ok.set(true)
                latch.countDown()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                latch.countDown()
            }
        }
        main.post {
            if (!dispatchGesture(g, cb, main)) latch.countDown()
        }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return ok.get()
    }
}
