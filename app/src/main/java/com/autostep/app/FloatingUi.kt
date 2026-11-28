package com.autostep.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

enum class PickMode { POINT, SWIPE, REGION }

data class PickResult(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

/** Draggable floating control panel, full-screen coordinate picker and in-overlay step form. */
class FloatingUi(private val svc: AccessibilityService) {
    private val wm = svc.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())

    private var root: LinearLayout? = null
    private var panel: LinearLayout? = null
    private var runBtn: TextView? = null
    private var statusTv: TextView? = null
    private var lp: WindowManager.LayoutParams? = null
    private var picker: PickerView? = null
    private var form: FrameLayout? = null

    val isShown: Boolean get() = root != null

    private val engineListener: () -> Unit = { refresh() }

    fun show() {
        if (root != null) return
        build()
        Engine.addListener(engineListener)
        refresh()
    }

    fun hide() {
        Engine.removeListener(engineListener)
        closePicker()
        closeForm()
        root?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        root = null
        panel = null
        runBtn = null
        statusTv = null
    }

    fun openApp() {
        try {
            svc.startActivity(
                Intent(svc, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        } catch (e: Exception) {
            toast("Open AutoStep from your launcher")
        }
    }

    private fun toast(msg: String) = Toast.makeText(svc, msg, Toast.LENGTH_SHORT).show()

    private fun refresh() {
        val running = Engine.running
        runBtn?.let {
            it.text = if (running) "\u25A0 Stop" else "\u25B6 Run"
            it.background = svc.rounded(if (running) Pal.RED else Pal.GREEN, 12)
        }
        val script = Store.current()
        statusTv?.text = if (running) Engine.status
        else "${script.name}: ${script.steps.size} steps  |  ${Engine.status}"
    }

    private fun build() {
        val c: Context = svc
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = c.dp(8)
        params.y = c.dp(160)
        lp = params

        val r = LinearLayout(c)
        r.orientation = LinearLayout.VERTICAL
        r.background = c.rounded(Color.parseColor("#F01A1D24"), 16, Pal.ACCENT)
        r.setPadding(c.dp(6), c.dp(6), c.dp(6), c.dp(6))

        val header = LinearLayout(c)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val handle = c.label("\u2630", 20f, Pal.MUTED, true)
        handle.setPadding(c.dp(10), c.dp(4), c.dp(10), c.dp(4))
        handle.setOnTouchListener(object : View.OnTouchListener {
            var ox = 0
            var oy = 0
            var tx = 0f
            var ty = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                val p = lp ?: return false
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { ox = p.x; oy = p.y; tx = e.rawX; ty = e.rawY }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = ox + (e.rawX - tx).toInt()
                        p.y = oy + (e.rawY - ty).toInt()
                        root?.let { wm.updateViewLayout(it, p) }
                    }
                }
                return true
            }
        })

        val run = c.pill("\u25B6 Run", Pal.GREEN) { toggleRun() }
        val more = c.pill("+ Steps", Pal.FIELD) {
            panel?.let { it.visibility = if (it.visibility == View.GONE) View.VISIBLE else View.GONE }
        }
        val wrap = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        wrap.leftMargin = c.dp(4)
        header.addView(handle)
        header.addView(run, wrap)
        header.addView(more, LinearLayout.LayoutParams(wrap))
        r.addView(header)

        val pn = LinearLayout(c)
        pn.orientation = LinearLayout.VERTICAL
        pn.visibility = View.GONE
        pn.setPadding(c.dp(4), c.dp(8), c.dp(4), c.dp(2))
        val st = c.label("Idle", 12f, Pal.MUTED)
        pn.addView(st)
        pn.addView(c.label("Add a step (tap the screen to record):", 12f, Pal.TEXT, true)
            .apply { setPadding(0, c.dp(8), 0, c.dp(4)) })

        fun addRow(vararg items: Pair<String, () -> Unit>): LinearLayout {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            items.forEach { (text, action) ->
                val l = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                l.rightMargin = c.dp(4)
                l.bottomMargin = c.dp(4)
                row.addView(c.chip(text, Pal.ACCENT, action), l)
            }
            return row
        }
        pn.addView(addRow(
            "\uD83D\uDC46 Tap" to { quickAdd(PickMode.POINT) { Step(StepType.TAP, x = it.x1, y = it.y1) } },
            "\uD83C\uDFB2 Random" to {
                quickAdd(PickMode.REGION) {
                    Step(StepType.RANDOM_TAP, x = min(it.x1, it.x2), y = min(it.y1, it.y2),
                        x2 = max(it.x1, it.x2), y2 = max(it.y1, it.y2))
                }
            },
            "\u270B Hold" to {
                startPick(PickMode.POINT, 0) { res ->
                    if (res != null) showForm(Step(StepType.HOLD, x = res.x1, y = res.y1, durationMs = 1000)) { addStep(it) }
                }
            },
            "\u2194 Swipe" to {
                quickAdd(PickMode.SWIPE) {
                    Step(StepType.SWIPE, x = it.x1, y = it.y1, x2 = it.x2, y2 = it.y2, durationMs = 300)
                }
            }
        ))
        pn.addView(addRow(
            "\u2328 Key" to { showForm(Step(StepType.KEY, key = "a", durationMs = 60)) { addStep(it) } },
            "\uD83D\uDCDD Text" to { showForm(Step(StepType.TEXT, text = "")) { addStep(it) } },
            "\u23F1 Wait" to { showForm(Step(StepType.WAIT, durationMs = 1000, delayMs = 0)) { addStep(it) } },
            "\u21A9 Undo" to { undoStep() }
        ))
        pn.addView(addRow(
            "Open app" to { openApp() },
            "Hide panel" to { hide() }
        ))
        r.addView(pn)

        wm.addView(r, params)
        root = r
        panel = pn
        runBtn = run
        statusTv = st
    }

    private fun toggleRun() {
        if (Engine.running) {
            Engine.stop()
        } else {
            panel?.visibility = View.GONE
            Engine.start(svc.applicationContext, Store.current())
        }
    }

    private fun addStep(step: Step) {
        val script = Store.current()
        script.steps.add(step)
        Store.save()
        toast("Step #${script.steps.size} added: ${step.title()}")
        refresh()
    }

    private fun undoStep() {
        val script = Store.current()
        if (script.steps.isEmpty()) {
            toast("No steps to undo")
        } else {
            val removed = script.steps.removeAt(script.steps.size - 1)
            Store.save()
            toast("Removed: ${removed.title()}")
            refresh()
        }
    }

    private fun quickAdd(mode: PickMode, make: (PickResult) -> Step) {
        startPick(mode, 0) { res -> if (res != null) addStep(make(res)) }
    }

    // ---- step form shown on top of the current app ----
    private fun showForm(base: Step, onSave: (Step) -> Unit) {
        closeForm()
        val c: Context = svc
        val ed = StepEditor(svc, base) { mode, cb ->
            form?.visibility = View.GONE
            startPick(mode, 0) { res ->
                form?.visibility = View.VISIBLE
                if (res != null) cb(res)
            }
        }
        val overlay = FrameLayout(c)
        overlay.setBackgroundColor(Color.parseColor("#B3000000"))
        val card = LinearLayout(c)
        card.orientation = LinearLayout.VERTICAL
        card.background = c.rounded(Pal.CARD, 16)
        val head = LinearLayout(c)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(8))
        head.addView(c.chip("Cancel", Pal.FIELD) { closeForm() })
        val t = c.label(base.type.icon + " " + base.type.label, 16f, Pal.TEXT, true)
        t.gravity = Gravity.CENTER
        head.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(c.chip("Add step", Pal.GREEN) {
            val s = ed.commit()
            closeForm()
            onSave(s)
        })
        card.addView(head)
        card.addView(ed.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val clp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        clp.setMargins(c.dp(14), c.dp(40), c.dp(14), c.dp(24))
        overlay.addView(card, clp)
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        )
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        root?.visibility = View.GONE
        wm.addView(overlay, p)
        form = overlay
    }

    private fun closeForm() {
        form?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        form = null
        root?.visibility = View.VISIBLE
    }

    // ---- picker ----
    fun startPick(mode: PickMode, delayMs: Long, cb: (PickResult?) -> Unit) {
        if (delayMs > 0) {
            toast("Picker opens in ${delayMs / 1000}s - switch to your game now")
            main.postDelayed({ showPicker(mode, cb) }, delayMs)
        } else {
            showPicker(mode, cb)
        }
    }

    private fun showPicker(mode: PickMode, cb: (PickResult?) -> Unit) {
        closePicker()
        val pv = PickerView(svc, mode) { res ->
            closePicker()
            cb(res)
        }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 28) {
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        root?.visibility = View.GONE
        wm.addView(pv, p)
        picker = pv
    }

    private fun closePicker() {
        picker?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        picker = null
        root?.visibility = if (form != null) View.GONE else View.VISIBLE
    }
}

private class PickerView(
    ctx: Context,
    private val mode: PickMode,
    private val onDone: (PickResult?) -> Unit
) : FrameLayout(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sx = 0f; private var sy = 0f      // start, screen coords
    private var cx = 0f; private var cy = 0f      // current, screen coords
    private var lsx = 0f; private var lsy = 0f    // start, local (for drawing)
    private var lcx = 0f; private var lcy = 0f
    private var touching = false

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.parseColor("#66000000"))
        val cancel = ctx.pill("Cancel", Pal.RED) { onDone(null) }
        val cp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END)
        cp.setMargins(0, ctx.dp(48), ctx.dp(16), 0)
        addView(cancel, cp)
        val hintText = when (mode) {
            PickMode.POINT -> "Tap the exact spot you want"
            PickMode.SWIPE -> "Drag from the start point to the end point"
            PickMode.REGION -> "Drag a rectangle - taps will be random inside it"
        }
        val hint = ctx.label(hintText, 16f, Color.WHITE, true)
        hint.setPadding(ctx.dp(16), ctx.dp(10), ctx.dp(16), ctx.dp(10))
        hint.background = ctx.rounded(Color.parseColor("#CC000000"), 12)
        val hp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        hp.setMargins(0, 0, 0, ctx.dp(96))
        addView(hint, hp)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                sx = e.rawX; sy = e.rawY; cx = sx; cy = sy
                lsx = e.x; lsy = e.y; lcx = lsx; lcy = lsy
                touching = true
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                cx = e.rawX; cy = e.rawY; lcx = e.x; lcy = e.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                cx = e.rawX; cy = e.rawY
                touching = false
                val res = when (mode) {
                    PickMode.POINT -> PickResult(cx.toInt(), cy.toInt(), cx.toInt(), cy.toInt())
                    PickMode.SWIPE -> PickResult(sx.toInt(), sy.toInt(), cx.toInt(), cy.toInt())
                    PickMode.REGION -> PickResult(
                        min(sx, cx).toInt(), min(sy, cy).toInt(), max(sx, cx).toInt(), max(sy, cy).toInt()
                    )
                }
                onDone(res)
            }
            MotionEvent.ACTION_CANCEL -> touching = false
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!touching) return
        paint.color = Pal.ACCENT
        paint.strokeWidth = 6f
        paint.style = Paint.Style.STROKE
        when (mode) {
            PickMode.POINT -> {
                canvas.drawCircle(lcx, lcy, 40f, paint)
                canvas.drawLine(lcx - 70, lcy, lcx + 70, lcy, paint)
                canvas.drawLine(lcx, lcy - 70, lcx, lcy + 70, paint)
            }
            PickMode.SWIPE -> {
                canvas.drawLine(lsx, lsy, lcx, lcy, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(lsx, lsy, 18f, paint)
                paint.color = Pal.GREEN
                canvas.drawCircle(lcx, lcy, 18f, paint)
            }
            PickMode.REGION -> {
                canvas.drawRect(min(lsx, lcx), min(lsy, lcy), max(lsx, lcx), max(lsy, lcy), paint)
            }
        }
    }
}
