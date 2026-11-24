package com.autostep.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

/** Runs a script step by step on a background thread. */
object Engine {
    @Volatile private var _running = false
    val running: Boolean get() = _running

    @Volatile var stopRequested = false
        private set

    @Volatile var status = "Idle"
        private set

    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val rnd = Random()

    fun addListener(l: () -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }

    private fun setStatus(s: String) {
        status = s
        main.post { listeners.forEach { it() } }
    }

    private fun toast(ctx: Context, msg: String) {
        main.post { Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
    }

    fun start(ctx: Context, script: Script) {
        if (_running) return
        if (AutoAccessibilityService.instance == null) {
            toast(ctx, "Enable the AutoStep accessibility service first")
            return
        }
        val steps = script.steps.filter { it.enabled }.map { it.copy() }
        if (steps.isEmpty()) {
            toast(ctx, "Add at least one enabled step")
            return
        }
        val loops = script.loops
        val delaySec = script.startDelaySec
        stopRequested = false
        _running = true
        setStatus("Starting...")
        val app = ctx.applicationContext
        Thread({ runScript(app, steps, loops, delaySec) }, "AutoStep-engine").start()
    }

    fun stop() {
        stopRequested = true
    }

    private fun runScript(ctx: Context, steps: List<Step>, loops: Int, delaySec: Int) {
        try {
            var left = delaySec
            while (left > 0 && !stopRequested) {
                setStatus("Starting in ${left}s - switch to your game")
                sleepMs(1000)
                left--
            }
            var loop = 0
            outer@ while (!stopRequested && (loops == 0 || loop < loops)) {
                loop++
                val loopLabel = if (loops == 0) "$loop/inf" else "$loop/$loops"
                for ((i, st) in steps.withIndex()) {
                    val reps = st.repeat.coerceAtLeast(1)
                    for (r in 1..reps) {
                        if (stopRequested) break@outer
                        setStatus("Loop $loopLabel | step ${i + 1}/${steps.size}\n${st.title()}")
                        val err = execute(st)
                        if (err != null) {
                            toast(ctx, err)
                            stopRequested = true
                            break@outer
                        }
                        sleepMs(rndLong(st.delayMs, st.delayMaxMs))
                    }
                }
                sleepMs(1) // avoids a busy loop if every step is instant
            }
        } finally {
            _running = false
            setStatus(if (stopRequested) "Stopped" else "Finished")
        }
    }

    /** Returns an error message if the script must stop, otherwise null. */
    private fun execute(st: Step): String? {
        val svc = AutoAccessibilityService.instance ?: return "Accessibility service is not running"
        when (st.type) {
            StepType.TAP -> svc.tap(jit(st.x, st.jitter), jit(st.y, st.jitter))
            StepType.RANDOM_TAP -> svc.tap(
                rndInt(min(st.x, st.x2), max(st.x, st.x2)),
                rndInt(min(st.y, st.y2), max(st.y, st.y2))
            )
            StepType.HOLD -> svc.hold(st.x, st.y, rndLong(st.durationMs, st.durationMaxMs), st.infinite) { stopRequested }
            StepType.SWIPE -> svc.swipe(st.x, st.y, st.x2, st.y2, st.durationMs)
            StepType.KEY -> return KeyInjector.press(st, rndLong(st.durationMs, st.durationMaxMs)) { stopRequested }
            StepType.TEXT -> return KeyInjector.typeText(st.text) { stopRequested }
            StepType.WAIT -> sleepMs(rndLong(st.durationMs, st.durationMaxMs))
        }
        return null
    }

    fun sleepMs(ms: Long) {
        var left = ms
        while (left > 0 && !stopRequested) {
            val s = min(left, 50L)
            try { Thread.sleep(s) } catch (e: InterruptedException) { return }
            left -= s
        }
    }

    private fun jit(v: Int, j: Int): Int = if (j > 0) v + rnd.nextInt(2 * j + 1) - j else v
    private fun rndInt(a: Int, b: Int): Int = if (b > a) a + rnd.nextInt(b - a + 1) else a
    private fun rndLong(a: Long, b: Long): Long =
        if (b > a) a + (rnd.nextDouble() * (b - a)).toLong() else a
}
