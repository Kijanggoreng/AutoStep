package com.autostep.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.util.Collections

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private var runBtn: TextView? = null
    private var statusTv: TextView? = null
    private val engineListener: () -> Unit = { updateRunUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        window.statusBarColor = Pal.BG
        window.navigationBarColor = Pal.BG
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(24), dp(16), dp(32))
        val sv = ScrollView(this)
        sv.setBackgroundColor(Pal.BG)
        sv.addView(content)
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        Engine.addListener(engineListener)
        render()
    }

    override fun onPause() {
        super.onPause()
        Engine.removeListener(engineListener)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun gap(h: Int) {
        content.addView(View(this), LinearLayout.LayoutParams(1, dp(h)))
    }

    // ------------------------------------------------------------------ render
    private fun render() {
        content.removeAllViews()
        content.addView(label("AutoStep", 28f, Pal.TEXT, true))
        content.addView(label("Step-by-step tap, hold, swipe & keyboard automation", 13f, Pal.MUTED))
        gap(16)
        content.addView(setupCard())
        gap(12)
        content.addView(scriptCard())
        gap(12)
        content.addView(stepsCard())
        updateRunUi()
    }

    private fun updateRunUi() {
        runBtn?.let {
            it.text = if (Engine.running) "Stop" else "Run script"
            it.background = rounded(if (Engine.running) Pal.RED else Pal.GREEN, 12)
        }
        statusTv?.text = Engine.status
    }

    private fun statusRow(title: String, ok: Boolean, btnText: String, action: () -> Unit): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(8), 0, dp(0))
        val dot = label(if (ok) "ON " else "OFF", 12f, if (ok) Pal.GREEN else Pal.RED, true)
        dot.minWidth = dp(34)
        row.addView(dot)
        row.addView(label(title, 14f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(pill(btnText, Pal.FIELD, action))
        return row
    }

    private fun isImeEnabled(): Boolean =
        (Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: "").contains(packageName)

    private fun isImeSelected(): Boolean =
        (Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: "").contains(packageName)

    private fun setupCard(): View {
        val c = card()
        c.addView(label("Setup", 16f, Pal.TEXT, true))
        val svc = AutoAccessibilityService.instance
        c.addView(statusRow("Accessibility service (taps/swipes)", svc != null, "Settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        c.addView(statusRow("Keyboard enabled", isImeEnabled(), "Enable") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        c.addView(statusRow("Keyboard selected (active)", isImeSelected(), "Select") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        val shown = svc?.isFloatingShown == true
        c.addView(statusRow("Floating panel", shown, if (shown) "Hide" else "Show") {
            val s = AutoAccessibilityService.instance
            if (s == null) toast("Enable the accessibility service first")
            else {
                if (shown) s.hideFloating() else s.showFloating()
                content.postDelayed({ render() }, 300)
            }
        })
        return c
    }

    // ------------------------------------------------------------------ script card
    private fun EditText.onChange(f: (String) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(e: Editable?) { f(e?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun EditText.int(def: Int): Int = text.toString().trim().toIntOrNull() ?: def
    private fun EditText.lng(def: Long): Long = text.toString().trim().toLongOrNull() ?: def

    private fun edit(value: String, hint: String = "", numeric: Boolean = true): EditText {
        val e = EditText(this)
        e.setText(value)
        e.hint = hint
        e.setTextColor(Pal.TEXT)
        e.setHintTextColor(Pal.MUTED)
        e.textSize = 15f
        e.inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        e.background = rounded(Pal.FIELD, 10)
        e.setPadding(dp(12), dp(8), dp(12), dp(8))
        e.isSingleLine = true
        return e
    }

    private fun scriptCard(): View {
        val c = card()
        val s = Store.current()
        c.addView(label("Script", 16f, Pal.TEXT, true))
        c.addView(label(s.name, 20f, Pal.ACCENT, true))

        val btns = LinearLayout(this)
        btns.orientation = LinearLayout.HORIZONTAL
        btns.setPadding(0, dp(8), 0, dp(8))
        fun small(t: String, a: () -> Unit) {
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.rightMargin = dp(6)
            btns.addView(pill(t, Pal.FIELD, a), lp)
        }
        small("Switch") {
            val names = Store.scripts.map { it.name }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Choose script").setItems(names) { _, i ->
                Store.currentIndex = i; Store.save(); render()
            }.show()
        }
        small("New") { askName("New script", "Script ${Store.scripts.size + 1}") { n ->
            Store.scripts.add(Script(n)); Store.currentIndex = Store.scripts.size - 1; Store.save(); render()
        } }
        small("Rename") { askName("Rename script", s.name) { n -> s.name = n; Store.save(); render() } }
        small("Delete") {
            AlertDialog.Builder(this).setTitle("Delete \"${s.name}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    Store.scripts.remove(s)
                    if (Store.scripts.isEmpty()) Store.scripts.add(Script("Script 1"))
                    Store.currentIndex = 0; Store.save(); render()
                }.setNegativeButton("Cancel", null).show()
        }
        c.addView(btns)

        c.addView(label("Loops (0 = infinite)", 12f, Pal.MUTED))
        val loops = edit(s.loops.toString())
        loops.onChange { s.loops = it.toIntOrNull() ?: 1; Store.save() }
        c.addView(loops)
        c.addView(label("Start delay (seconds, time to switch to your game)", 12f, Pal.MUTED).apply { setPadding(0, dp(8), 0, 0) })
        val delay = edit(s.startDelaySec.toString())
        delay.onChange { s.startDelaySec = it.toIntOrNull() ?: 0; Store.save() }
        c.addView(delay)

        val run = pill("Run script", Pal.GREEN) {
            if (Engine.running) Engine.stop()
            else {
                Engine.start(applicationContext, Store.current())
                if (Engine.running) moveTaskToBack(true)
            }
        }
        runBtn = run
        val rlp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        rlp.topMargin = dp(14)
        c.addView(run, rlp)
        val st = label(Engine.status, 12f, Pal.MUTED)
        st.setPadding(0, dp(8), 0, 0)
        statusTv = st
        c.addView(st)
        return c
    }

    private fun askName(title: String, def: String, ok: (String) -> Unit) {
        val e = edit(def, "Name", false)
        AlertDialog.Builder(this).setTitle(title).setView(e)
            .setPositiveButton("OK") { _, _ ->
                val n = e.text.toString().trim()
                if (n.isNotEmpty()) ok(n)
            }.setNegativeButton("Cancel", null).show()
    }

    // ------------------------------------------------------------------ steps card
    private fun stepsCard(): View {
        val c = card()
        val steps = Store.current().steps
        c.addView(label("Steps (${steps.size})", 16f, Pal.TEXT, true))
        if (steps.isEmpty()) {
            c.addView(label(
                "No steps yet. Add one below, or open your game and use the floating panel (... > +Tap, +Hold, +Swipe) to record steps by tapping on the screen.",
                13f, Pal.MUTED
            ).apply { setPadding(0, dp(8), 0, dp(8)) })
        }
        steps.forEachIndexed { i, st -> c.addView(stepRow(i, st)) }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(8)
        c.addView(pill("+ Add step", Pal.ACCENT) { chooseType() }, lp)
        return c
    }

    private fun stepRow(i: Int, st: Step): View {
        val steps = Store.current().steps
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(10), dp(10), dp(4), dp(10))
        row.background = rounded(Pal.FIELD, 12)
        row.alpha = if (st.enabled) 1f else 0.5f
        val rlp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        rlp.topMargin = dp(8)
        row.layoutParams = rlp

        val num = label("${i + 1}", 14f, Pal.ACCENT, true)
        num.minWidth = dp(28)
        row.addView(num)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(label(st.title(), 14f, Pal.TEXT, true))
        col.addView(label(st.detail(), 12f, Pal.MUTED))
        row.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        fun act(t: String, a: () -> Unit) {
            val v = label(t, 16f, Pal.MUTED, true)
            v.setPadding(dp(8), dp(6), dp(8), dp(6))
            v.setOnClickListener { a() }
            row.addView(v)
        }
        act("^") { if (i > 0) { Collections.swap(steps, i, i - 1); Store.save(); render() } }
        act("v") { if (i < steps.size - 1) { Collections.swap(steps, i, i + 1); Store.save(); render() } }
        act("++") { steps.add(i + 1, st.copy()); Store.save(); render() }
        act("x") { steps.removeAt(i); Store.save(); render() }
        row.setOnClickListener { showEditor(i, st) }
        return row
    }

    private fun chooseType() {
        val types = StepType.values()
        AlertDialog.Builder(this).setTitle("Add step")
            .setItems(types.map { it.label }.toTypedArray()) { _, i -> showEditor(null, defaultStep(types[i])) }
            .show()
    }

    private fun defaultStep(t: StepType): Step {
        val dm = resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        return when (t) {
            StepType.TAP -> Step(StepType.TAP, x = w / 2, y = h / 2)
            StepType.RANDOM_TAP -> Step(StepType.RANDOM_TAP, x = w / 10, y = h / 4, x2 = w * 9 / 10, y2 = h * 3 / 4)
            StepType.HOLD -> Step(StepType.HOLD, x = w / 2, y = h / 2, durationMs = 1000)
            StepType.SWIPE -> Step(StepType.SWIPE, x = w / 2, y = h * 3 / 4, x2 = w / 2, y2 = h / 4, durationMs = 300)
            StepType.KEY -> Step(StepType.KEY, key = "a", durationMs = 60)
            StepType.TEXT -> Step(StepType.TEXT, text = "hello")
            StepType.WAIT -> Step(StepType.WAIT, durationMs = 1000, delayMs = 0)
        }
    }

    // ------------------------------------------------------------------ picker bridge
    private fun pick(mode: PickMode, cb: (PickResult) -> Unit) {
        val svc = AutoAccessibilityService.instance
        if (svc == null) {
            toast("Enable the accessibility service first")
            return
        }
        svc.startPick(mode, 5000) { res ->
            if (res != null) runOnUiThread { cb(res) }
            svc.openApp()
        }
        moveTaskToBack(true)
    }

    // ------------------------------------------------------------------ step editor
    private fun showEditor(index: Int?, base: Step) {
        val s = base.copy()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(8), dp(20), dp(8))
        val sv = ScrollView(this)
        sv.addView(box)
        val readers = mutableListOf<() -> Unit>()

        fun note(t: String) {
            box.addView(label(t, 12f, Pal.MUTED).apply { setPadding(0, dp(8), 0, 0) })
        }

        fun addNum(l: String, v: Any): EditText {
            box.addView(label(l, 12f, Pal.MUTED).apply { setPadding(0, dp(10), 0, dp(2)) })
            val e = edit(v.toString())
            box.addView(e)
            return e
        }

        fun addPair(l1: String, v1: Int, l2: String, v2: Int): Pair<EditText, EditText> {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            fun col(l: String, v: Int, right: Boolean): EditText {
                val colV = LinearLayout(this)
                colV.orientation = LinearLayout.VERTICAL
                colV.setPadding(0, dp(10), if (right) 0 else dp(6), 0)
                colV.addView(label(l, 12f, Pal.MUTED))
                val e = edit(v.toString())
                colV.addView(e)
                row.addView(colV, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                return e
            }
            val a = col(l1, v1, false)
            val b = col(l2, v2, true)
            box.addView(row)
            return Pair(a, b)
        }

        fun addSwitch(t: String, v: Boolean): Switch {
            val sw = Switch(this)
            sw.text = t
            sw.isChecked = v
            sw.setTextColor(Pal.TEXT)
            sw.setPadding(0, dp(12), 0, dp(4))
            box.addView(sw)
            return sw
        }

        fun addPick(t: String, mode: PickMode, cb: (PickResult) -> Unit) {
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10)
            box.addView(pill(t, Pal.FIELD) { pick(mode, cb) }, lp)
        }

        when (s.type) {
            StepType.TAP -> {
                val (ex, ey) = addPair("X", s.x, "Y", s.y)
                addPick("Pick on screen (5s to switch app)", PickMode.POINT) { r ->
                    ex.setText(r.x1.toString()); ey.setText(r.y1.toString())
                }
                val ej = addNum("Random offset +- px (0 = exact)", s.jitter)
                readers.add { s.x = ex.int(s.x); s.y = ey.int(s.y); s.jitter = ej.int(0) }
            }
            StepType.RANDOM_TAP -> {
                val (el, et) = addPair("Left", s.x, "Top", s.y)
                val (er, eb) = addPair("Right", s.x2, "Bottom", s.y2)
                addPick("Pick area on screen (5s to switch app)", PickMode.REGION) { r ->
                    el.setText(r.x1.toString()); et.setText(r.y1.toString())
                    er.setText(r.x2.toString()); eb.setText(r.y2.toString())
                }
                readers.add { s.x = el.int(s.x); s.y = et.int(s.y); s.x2 = er.int(s.x2); s.y2 = eb.int(s.y2) }
            }
            StepType.HOLD -> {
                val (ex, ey) = addPair("X", s.x, "Y", s.y)
                addPick("Pick on screen (5s to switch app)", PickMode.POINT) { r ->
                    ex.setText(r.x1.toString()); ey.setText(r.y1.toString())
                }
                val d1 = addNum("Hold duration (ms)", s.durationMs)
                val d2 = addNum("Random up to (ms) - 0 = fixed", s.durationMaxMs)
                val inf = addSwitch("Hold forever (until Stop)", s.infinite)
                note("Forever = the script stays on this step until you press Stop; later steps never run.")
                readers.add {
                    s.x = ex.int(s.x); s.y = ey.int(s.y)
                    s.durationMs = d1.lng(1000).coerceAtLeast(1); s.durationMaxMs = d2.lng(0); s.infinite = inf.isChecked
                }
            }
            StepType.SWIPE -> {
                val (ex, ey) = addPair("From X", s.x, "From Y", s.y)
                val (ex2, ey2) = addPair("To X", s.x2, "To Y", s.y2)
                addPick("Draw swipe on screen (5s to switch app)", PickMode.SWIPE) { r ->
                    ex.setText(r.x1.toString()); ey.setText(r.y1.toString())
                    ex2.setText(r.x2.toString()); ey2.setText(r.y2.toString())
                }
                val d = addNum("Swipe duration (ms)", s.durationMs)
                readers.add {
                    s.x = ex.int(s.x); s.y = ey.int(s.y); s.x2 = ex2.int(s.x2); s.y2 = ey2.int(s.y2)
                    s.durationMs = d.lng(300).coerceAtLeast(1)
                }
            }
            StepType.KEY -> {
                box.addView(label("Key mode", 12f, Pal.MUTED).apply { setPadding(0, dp(10), 0, dp(2)) })
                val sp = Spinner(this)
                sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, KeyMode.values().map { it.label })
                sp.setSelection(s.keyMode.ordinal)
                box.addView(sp)
                box.addView(label("Key (for Fixed mode)", 12f, Pal.MUTED).apply { setPadding(0, dp(10), 0, dp(2)) })
                val ek = edit(s.key, "a, A, 5, @, ENTER, SPACE, LEFT, F5...", false)
                box.addView(ek)
                note("Names: " + KeyInjector.named.keys.joinToString(", "))
                val d1 = addNum("Hold time (ms) - e.g. 38000 = 38 s", s.durationMs)
                val d2 = addNum("Random up to (ms) - 0 = fixed", s.durationMaxMs)
                val inf = addSwitch("Hold forever (until Stop)", s.infinite)
                readers.add {
                    s.keyMode = KeyMode.values()[sp.selectedItemPosition]
                    s.key = ek.text.toString().trim().ifEmpty { "a" }
                    s.durationMs = d1.lng(60).coerceAtLeast(1); s.durationMaxMs = d2.lng(0); s.infinite = inf.isChecked
                }
            }
            StepType.TEXT -> {
                box.addView(label("Text to type", 12f, Pal.MUTED).apply { setPadding(0, dp(10), 0, dp(2)) })
                val et = edit(s.text, "text", false)
                et.isSingleLine = false
                box.addView(et)
                readers.add { s.text = et.text.toString() }
            }
            StepType.WAIT -> {
                val d1 = addNum("Wait (ms)", s.durationMs)
                val d2 = addNum("Random up to (ms) - 0 = fixed", s.durationMaxMs)
                readers.add { s.durationMs = d1.lng(1000); s.durationMaxMs = d2.lng(0) }
            }
        }

        if (s.type == StepType.KEY || s.type == StepType.TEXT) {
            note("Needs: AutoStep keyboard selected AND an input field/game focused.")
        }
        val rep = addNum("Repeat this step (times)", s.repeat)
        val dl1 = addNum("Pause after step (ms)", s.delayMs)
        val dl2 = addNum("Random pause up to (ms) - 0 = fixed", s.delayMaxMs)
        val en = addSwitch("Enabled", s.enabled)
        readers.add {
            s.repeat = rep.int(1).coerceAtLeast(1)
            s.delayMs = dl1.lng(0).coerceAtLeast(0); s.delayMaxMs = dl2.lng(0); s.enabled = en.isChecked
        }

        AlertDialog.Builder(this)
            .setTitle((if (index == null) "New " else "Edit ") + s.type.label)
            .setView(sv)
            .setPositiveButton("Save") { _, _ ->
                readers.forEach { it() }
                val steps = Store.current().steps
                if (index == null) steps.add(s) else if (index in steps.indices) steps[index] = s
                Store.save()
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
