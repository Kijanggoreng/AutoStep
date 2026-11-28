package com.autostep.app

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Step form shared by the app (inside an AlertDialog) and the floating panel (inside an overlay).
 * Uses only plain views styled in code, so it looks the same in both places.
 */
class StepEditor(
    private val ctx: Context,
    base: Step,
    private val pick: (PickMode, (PickResult) -> Unit) -> Unit
) {
    class Flag(var on: Boolean)

    private val s: Step = base.copy()
    private val box = LinearLayout(ctx)
    private val adv = LinearLayout(ctx)
    private val readers = mutableListOf<() -> Unit>()
    private val advBlocks = mutableListOf<() -> Unit>()
    private var cur: LinearLayout = box
    val view: View

    init {
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ctx.dp(16), ctx.dp(4), ctx.dp(16), ctx.dp(16))
        adv.orientation = LinearLayout.VERTICAL
        adv.visibility = View.GONE
        build()
        val sv = ScrollView(ctx)
        sv.addView(box)
        view = sv
    }

    fun commit(): Step {
        readers.forEach { it() }
        return s
    }

    // ---------------------------------------------------------------- small helpers
    private fun add(v: View, topDp: Int = 0) {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = ctx.dp(topDp)
        cur.addView(v, lp)
    }

    private fun title(t: String) = add(ctx.label(t, 12f, Pal.MUTED), 14)
    private fun note(t: String) = add(ctx.label(t, 12f, Pal.MUTED), 8)

    private fun field(value: String, hint: String = "", numeric: Boolean = true, decimal: Boolean = false): EditText {
        val e = EditText(ctx)
        e.setText(value)
        e.hint = hint
        e.setTextColor(Pal.TEXT)
        e.setHintTextColor(Pal.MUTED)
        e.textSize = 15f
        e.inputType = when {
            !numeric -> InputType.TYPE_CLASS_TEXT
            decimal -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            else -> InputType.TYPE_CLASS_NUMBER
        }
        e.background = ctx.rounded(Pal.FIELD, 10)
        e.setPadding(ctx.dp(12), ctx.dp(8), ctx.dp(12), ctx.dp(8))
        e.isSingleLine = true
        return e
    }

    private fun EditText.int(def: Int): Int = text.toString().trim().toIntOrNull() ?: def
    private fun EditText.ms(def: Long): Long = parseSec(text.toString(), def)

    private fun pair(l1: String, v1: Int, l2: String, v2: Int): Pair<EditText, EditText> {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        fun col(l: String, v: Int, last: Boolean): EditText {
            val c = LinearLayout(ctx)
            c.orientation = LinearLayout.VERTICAL
            c.setPadding(0, ctx.dp(8), if (last) 0 else ctx.dp(8), 0)
            c.addView(ctx.label(l, 12f, Pal.MUTED))
            val e = field(v.toString())
            c.addView(e)
            row.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            return e
        }
        val a = col(l1, v1, false)
        val b = col(l2, v2, true)
        add(row)
        return Pair(a, b)
    }

    private fun pickButton(text: String, mode: PickMode, cb: (PickResult) -> Unit) {
        add(ctx.pill(text, Pal.ACCENT) { pick(mode, cb) }, 10)
    }

    /** Seconds field with quick preset chips. */
    private fun seconds(l: String, ms: Long, presets: List<Pair<String, Long>> = emptyList()): EditText {
        title(l)
        val e = field(secText(ms), "seconds", true, true)
        add(e, 4)
        if (presets.isNotEmpty()) {
            add(ctx.chipRow(presets.map { it.first }, -1) { i -> e.setText(secText(presets[i].second)) }, 6)
        }
        return e
    }

    private fun toggle(text: String, initial: Boolean): Flag {
        val f = Flag(initial)
        val v = ctx.label("", 14f, Pal.TEXT)
        fun paint() {
            v.text = (if (f.on) "\u2611  " else "\u2610  ") + text
            v.background = ctx.rounded(if (f.on) Pal.ACCENT else Pal.FIELD, 10)
        }
        v.setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(12), ctx.dp(10))
        v.setOnClickListener { f.on = !f.on; paint() }
        paint()
        add(v, 10)
        return f
    }

    // ---------------------------------------------------------------- form
    private fun build() {
        val holdPresets = listOf("0.5s" to 500L, "1s" to 1000L, "3s" to 3000L, "5s" to 5000L, "10s" to 10000L, "30s" to 30000L)
        when (s.type) {
            StepType.TAP -> {
                title("Where to tap")
                val (ex, ey) = pair("X", s.x, "Y", s.y)
                pickButton("\uD83D\uDCCD  Pick on screen", PickMode.POINT) { r ->
                    ex.setText(r.x1.toString()); ey.setText(r.y1.toString())
                }
                readers.add { s.x = ex.int(s.x); s.y = ey.int(s.y) }
                advBlocks.add {
                    title("Random offset (+/- pixels, 0 = exact spot)")
                    val ej = field(s.jitter.toString()); add(ej, 4)
                    readers.add { s.jitter = ej.int(0) }
                }
            }
            StepType.RANDOM_TAP -> {
                title("Area (a random spot inside is tapped each time)")
                val (el, et) = pair("Left", s.x, "Top", s.y)
                val (er, eb) = pair("Right", s.x2, "Bottom", s.y2)
                pickButton("\uD83D\uDCCD  Draw area on screen", PickMode.REGION) { r ->
                    el.setText(r.x1.toString()); et.setText(r.y1.toString())
                    er.setText(r.x2.toString()); eb.setText(r.y2.toString())
                }
                readers.add { s.x = el.int(s.x); s.y = et.int(s.y); s.x2 = er.int(s.x2); s.y2 = eb.int(s.y2) }
            }
            StepType.HOLD -> {
                title("Where to hold")
                val (ex, ey) = pair("X", s.x, "Y", s.y)
                pickButton("\uD83D\uDCCD  Pick on screen", PickMode.POINT) { r ->
                    ex.setText(r.x1.toString()); ey.setText(r.y1.toString())
                }
                val d = seconds("Hold for (seconds)", s.durationMs, holdPresets)
                val inf = toggle("Hold forever (until you press Stop)", s.infinite)
                note("With 'forever' the script stays on this step until you press Stop.")
                readers.add {
                    s.x = ex.int(s.x); s.y = ey.int(s.y)
                    s.durationMs = d.ms(1000).coerceAtLeast(1); s.infinite = inf.on
                }
                advBlocks.add {
                    val m = seconds("Random hold: up to (seconds, 0 = fixed)", s.durationMaxMs)
                    readers.add { s.durationMaxMs = m.ms(0) }
                }
            }
            StepType.SWIPE -> {
                title("Swipe from / to")
                val (ex, ey) = pair("From X", s.x, "From Y", s.y)
                val (ex2, ey2) = pair("To X", s.x2, "To Y", s.y2)
                pickButton("\uD83D\uDCCD  Draw swipe on screen", PickMode.SWIPE) { r ->
                    ex.setText(r.x1.toString()); ey.setText(r.y1.toString())
                    ex2.setText(r.x2.toString()); ey2.setText(r.y2.toString())
                }
                val d = seconds("Swipe takes (seconds)", s.durationMs,
                    listOf("0.2s" to 200L, "0.3s" to 300L, "0.5s" to 500L, "1s" to 1000L, "2s" to 2000L))
                readers.add {
                    s.x = ex.int(s.x); s.y = ey.int(s.y); s.x2 = ex2.int(s.x2); s.y2 = ey2.int(s.y2)
                    s.durationMs = d.ms(300).coerceAtLeast(1)
                }
            }
            StepType.KEY -> {
                title("Which key?")
                val modes = KeyMode.values()
                add(ctx.chipRow(modes.map { it.label }, s.keyMode.ordinal) { i -> s.keyMode = modes[i] }, 4)
                title("Key (used by 'Fixed key'): a letter, digit, symbol or a name like ENTER")
                val ek = field(s.key, "a, A, 5, @, ENTER, SPACE...", false)
                add(ek, 4)
                val quick = listOf("W" to "w", "A" to "a", "S" to "s", "D" to "d", "Space" to "SPACE",
                    "Enter" to "ENTER", "\u2191" to "UP", "\u2193" to "DOWN", "\u2190" to "LEFT", "\u2192" to "RIGHT")
                add(ctx.chipRow(quick.map { it.first }, -1) { i -> ek.setText(quick[i].second) }, 6)
                val d = seconds("Hold the key for (seconds)", s.durationMs,
                    listOf("Quick tap" to 60L, "1s" to 1000L, "5s" to 5000L, "10s" to 10000L, "30s" to 30000L))
                val inf = toggle("Hold forever (until you press Stop)", s.infinite)
                note("Needs the AutoStep keyboard selected and an input field (or a game that reads keys) focused.")
                readers.add {
                    s.key = ek.text.toString().trim().ifEmpty { "a" }
                    s.durationMs = d.ms(60).coerceAtLeast(1); s.infinite = inf.on
                }
                advBlocks.add {
                    val m = seconds("Random hold: up to (seconds, 0 = fixed)", s.durationMaxMs)
                    readers.add { s.durationMaxMs = m.ms(0) }
                }
            }
            StepType.TEXT -> {
                title("Text to type")
                val et = field(s.text, "text", false)
                add(et, 4)
                note("Needs the AutoStep keyboard selected and an input field focused.")
                readers.add { s.text = et.text.toString() }
            }
            StepType.WAIT -> {
                val d = seconds("Wait for (seconds)", s.durationMs,
                    listOf("0.5s" to 500L, "1s" to 1000L, "2s" to 2000L, "5s" to 5000L, "10s" to 10000L))
                readers.add { s.durationMs = d.ms(1000) }
                advBlocks.add {
                    val m = seconds("Random wait: up to (seconds, 0 = fixed)", s.durationMaxMs)
                    readers.add { s.durationMaxMs = m.ms(0) }
                }
            }
        }

        // common basics
        val pause = seconds("Then wait (seconds) before the next step", s.delayMs,
            listOf("0" to 0L, "0.3s" to 300L, "0.5s" to 500L, "1s" to 1000L, "2s" to 2000L))
        title("Repeat this step (times)")
        val rep = field(s.repeat.toString())
        add(rep, 4)
        readers.add { s.delayMs = pause.ms(0).coerceAtLeast(0); s.repeat = rep.int(1).coerceAtLeast(1) }

        // more options (collapsed)
        val more = ctx.chip("More options \u25BE", Pal.FIELD) {}
        more.setOnClickListener {
            val open = adv.visibility == View.GONE
            adv.visibility = if (open) View.VISIBLE else View.GONE
            more.text = if (open) "Fewer options \u25B4" else "More options \u25BE"
        }
        add(more, 18)
        add(adv)
        cur = adv
        advBlocks.forEach { it() }
        val pauseMax = seconds("Random pause: up to (seconds, 0 = fixed)", s.delayMaxMs)
        val en = toggle("Step enabled", s.enabled)
        readers.add { s.delayMaxMs = pauseMax.ms(0); s.enabled = en.on }
        cur = box
    }
}
