package com.autostep.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Collections

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private var runBtn: TextView? = null
    private var statusTv: TextView? = null
    private val engineListener: () -> Unit = { updateRunUi() }
    private val prefs by lazy { getSharedPreferences("autostep_prefs", MODE_PRIVATE) }

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
        if (!prefs.getBoolean("welcomed", false)) {
            prefs.edit().putBoolean("welcomed", true).apply()
            showWelcome()
        }
    }

    override fun onPause() {
        super.onPause()
        Engine.removeListener(engineListener)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun gap(h: Int) {
        content.addView(View(this), LinearLayout.LayoutParams(1, dp(h)))
    }

    private fun full(topDp: Int = 0): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(topDp)
        return lp
    }

    // ------------------------------------------------------------------ main layout
    private fun render() {
        content.removeAllViews()
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(label("AutoStep", 28f, Pal.TEXT, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(pill("Help", Pal.FIELD) { showHelp() })
        content.addView(head)
        content.addView(label("Record taps, holds, swipes and key presses, then replay them on any app or game.", 13f, Pal.MUTED))
        gap(16)

        val svc = AutoAccessibilityService.instance
        if (svc == null) {
            content.addView(accessibilityCard())
            gap(12)
            content.addView(scriptCard())
            gap(12)
            content.addView(stepsCard())
        } else {
            content.addView(scriptCard())
            gap(12)
            content.addView(stepsCard())
            gap(12)
            content.addView(readyCard(svc))
        }
        gap(12)
        content.addView(keyboardCard())
        gap(20)
        content.addView(label("AutoStep is open source. No internet permission - nothing leaves your phone.", 11f, Pal.MUTED))
        updateRunUi()
    }

    private fun updateRunUi() {
        runBtn?.let {
            it.text = if (Engine.running) "\u25A0  Stop" else "\u25B6  Run script"
            it.background = rounded(if (Engine.running) Pal.RED else Pal.GREEN, 12)
        }
        statusTv?.text = Engine.status
    }

    // ------------------------------------------------------------------ setup cards
    private fun openAppInfo() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            toast("Open Settings > Apps > AutoStep")
        }
    }

    private fun accessibilityCard(): View {
        val c = card()
        c.addView(label("Step 1: turn on the accessibility service", 16f, Pal.TEXT, true))
        c.addView(label(
            "Android only lets an app tap and swipe for you through its Accessibility service. " +
                "AutoStep ignores everything on your screen and has no internet permission.",
            13f, Pal.MUTED
        ).apply { setPadding(0, dp(6), 0, dp(10)) })
        c.addView(pill("Open Accessibility settings", Pal.ACCENT) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, full())
        c.addView(label(
            "Find AutoStep in the list (often under \"Downloaded apps\" or \"Installed apps\"), open it and switch it ON.",
            13f, Pal.MUTED
        ).apply { setPadding(0, dp(8), 0, 0) })

        val help = LinearLayout(this)
        help.orientation = LinearLayout.VERTICAL
        help.setPadding(dp(12), dp(12), dp(12), dp(12))
        help.background = rounded(Pal.FIELD, 12)
        help.addView(label("Can't switch it on? (\"Restricted setting\")", 14f, Pal.TEXT, true))
        help.addView(label(RESTRICTED_HELP, 13f, Pal.MUTED).apply { setPadding(0, dp(6), 0, dp(10)) })
        help.addView(pill("Open App info", Pal.FIELD) { openAppInfo() }, full())
        c.addView(help, full(14))
        return c
    }

    private fun readyCard(svc: AutoAccessibilityService): View {
        val c = card()
        c.addView(label("\u2713  Accessibility is on", 15f, Pal.GREEN, true))
        val shown = svc.isFloatingShown
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(10), 0, 0)
        row.addView(label(
            if (shown) "Floating panel is visible. Open your game and use it to record steps and press Run."
            else "Floating panel is hidden.",
            13f, Pal.MUTED
        ), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(pill(if (shown) "Hide" else "Show", Pal.FIELD) {
            if (shown) svc.hideFloating() else svc.showFloating()
            content.postDelayed({ render() }, 300)
        })
        c.addView(row)
        return c
    }

    private fun isImeEnabled(): Boolean = try {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.enabledInputMethodList.any { it.packageName == packageName }
    } catch (e: Exception) {
        false
    }

    private fun isImeSelected(): Boolean = try {
        (Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: "").contains(packageName)
    } catch (e: Exception) {
        false
    }

    private fun statusRow(title: String, ok: Boolean, btnText: String, action: () -> Unit): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(8), 0, 0)
        val dot = label(if (ok) "ON " else "OFF", 12f, if (ok) Pal.GREEN else Pal.RED, true)
        dot.minWidth = dp(34)
        row.addView(dot)
        row.addView(label(title, 14f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(pill(btnText, Pal.FIELD, action))
        return row
    }

    private fun keyboardCard(): View {
        val c = card()
        c.addView(label("Keyboard (optional)", 16f, Pal.TEXT, true))
        c.addView(label(
            "Only needed for \"Key press\" and \"Type text\" steps. It is a normal keyboard you can type with, " +
                "and scripts use it to press keys in the app or game that has a text field focused.",
            13f, Pal.MUTED
        ).apply { setPadding(0, dp(4), 0, dp(2)) })
        c.addView(statusRow("1. Enable AutoStep Keyboard", isImeEnabled(), "Enable") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        c.addView(statusRow("2. Select it as current keyboard", isImeSelected(), "Select") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
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

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(label(s.name, 18f, Pal.ACCENT, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(chip("Scripts \u22EF", Pal.FIELD) { scriptMenu() })
        c.addView(top)

        val run = pill("\u25B6  Run script", Pal.GREEN) {
            if (Engine.running) Engine.stop()
            else {
                Engine.start(applicationContext, Store.current())
                if (Engine.running) moveTaskToBack(true)
            }
        }
        runBtn = run
        c.addView(run, full(12))
        val st = label(Engine.status, 12f, Pal.MUTED)
        st.setPadding(0, dp(8), 0, 0)
        statusTv = st
        c.addView(st)
        c.addView(label(
            "After you press Run you get ${s.startDelaySec} seconds to switch to your game.",
            12f, Pal.MUTED
        ).apply { setPadding(0, dp(2), 0, dp(10)) })

        c.addView(label("Repeat the whole script", 12f, Pal.MUTED))
        val loops = edit(s.loops.toString(), "times (0 = forever)")
        loops.onChange { s.loops = it.toIntOrNull() ?: 1; Store.save() }
        val sel = if (s.loops == 1) 0 else if (s.loops == 0) 1 else -1
        c.addView(chipRow(listOf("Once", "Forever"), sel) { i ->
            val v = if (i == 0) 1 else 0
            loops.setText(v.toString())
        }, full(6))
        c.addView(label("or a number of times (0 = forever)", 12f, Pal.MUTED).apply { setPadding(0, dp(8), 0, dp(2)) })
        c.addView(loops)
        return c
    }

    private fun scriptMenu() {
        val s = Store.current()
        val items = arrayOf("Switch script", "New script", "Rename script", "Delete script", "Start delay: ${s.startDelaySec}s")
        AlertDialog.Builder(this).setTitle("Scripts").setItems(items) { _, i ->
            when (i) {
                0 -> {
                    val names = Store.scripts.map { it.name }.toTypedArray()
                    AlertDialog.Builder(this).setTitle("Choose script").setItems(names) { _, k ->
                        Store.currentIndex = k; Store.save(); render()
                    }.show()
                }
                1 -> askText("New script", "Script ${Store.scripts.size + 1}", false) { n ->
                    Store.scripts.add(Script(n)); Store.currentIndex = Store.scripts.size - 1; Store.save(); render()
                }
                2 -> askText("Rename script", s.name, false) { n -> s.name = n; Store.save(); render() }
                3 -> AlertDialog.Builder(this).setTitle("Delete \"${s.name}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        Store.scripts.remove(s)
                        if (Store.scripts.isEmpty()) Store.scripts.add(Script("Script 1"))
                        Store.currentIndex = 0; Store.save(); render()
                    }.setNegativeButton("Cancel", null).show()
                4 -> askText("Start delay (seconds)", s.startDelaySec.toString(), true) { n ->
                    s.startDelaySec = n.toIntOrNull() ?: 3; Store.save(); render()
                }
                else -> {}
            }
        }.show()
    }

    private fun askText(title: String, def: String, numeric: Boolean, ok: (String) -> Unit) {
        val e = edit(def, "", numeric)
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
                "No steps yet. Two ways to add them:\n\n" +
                    "1. Easiest: open your game, expand the floating panel (+ Steps) and tap Tap / Hold / Swipe, " +
                    "then touch the screen where you want it.\n\n" +
                    "2. Or press \"+ Add step\" below and fill in the form.",
                13f, Pal.MUTED
            ).apply { setPadding(0, dp(8), 0, dp(4)) })
        }
        steps.forEachIndexed { i, st -> c.addView(stepRow(i, st)) }
        c.addView(pill("+ Add step", Pal.ACCENT) { chooseType() }, full(10))
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
        row.layoutParams = full(8)

        val num = label("${i + 1}", 14f, Pal.ACCENT, true)
        num.minWidth = dp(24)
        row.addView(num)
        row.addView(label(st.type.icon, 18f))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(8), 0, 0, 0)
        col.addView(label(st.title(), 14f, Pal.TEXT, true))
        col.addView(label(st.detail(), 12f, Pal.MUTED))
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        fun act(t: String, a: () -> Unit) {
            val v = label(t, 16f, Pal.MUTED, true)
            v.setPadding(dp(8), dp(6), dp(8), dp(6))
            v.setOnClickListener { a() }
            row.addView(v)
        }
        act("\u25B2") { if (i > 0) { Collections.swap(steps, i, i - 1); Store.save(); render() } }
        act("\u25BC") { if (i < steps.size - 1) { Collections.swap(steps, i, i + 1); Store.save(); render() } }
        act("\u29C9") { steps.add(i + 1, st.copy()); Store.save(); render() }
        act("\u2715") { steps.removeAt(i); Store.save(); render() }
        row.setOnClickListener { showEditor(i, st) }
        return row
    }

    private fun chooseType() {
        val types = StepType.values()
        val items = types.map { "${it.icon}  ${it.label}\n     ${it.desc}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Add step")
            .setItems(items) { _, i -> showEditor(null, defaultStep(types[i])) }
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

    // ------------------------------------------------------------------ picker bridge + editor
    private fun pick(mode: PickMode, cb: (PickResult) -> Unit) {
        val svc = AutoAccessibilityService.instance
        if (svc == null) {
            toast("Turn on the accessibility service first")
            return
        }
        svc.startPick(mode, 5000) { res ->
            if (res != null) runOnUiThread { cb(res) }
            svc.openApp()
        }
        moveTaskToBack(true)
    }

    private fun showEditor(index: Int?, base: Step) {
        val ed = StepEditor(this, base) { mode, cb -> pick(mode, cb) }
        AlertDialog.Builder(this)
            .setTitle((if (index == null) "New: " else "Edit: ") + base.type.label)
            .setView(ed.view)
            .setPositiveButton("Save") { _, _ ->
                val s = ed.commit()
                val steps = Store.current().steps
                if (index == null) steps.add(s) else if (index in steps.indices) steps[index] = s
                Store.save()
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ------------------------------------------------------------------ help
    private fun showWelcome() {
        AlertDialog.Builder(this)
            .setTitle("Welcome to AutoStep")
            .setMessage(
                "1. Turn on the accessibility service (the app shows you how).\n\n" +
                    "2. Open your game and use the floating panel to record steps: tap where you want each tap, hold or swipe.\n\n" +
                    "3. Press Run. Press Stop any time.\n\n" +
                    "Tip: tap Help at the top for troubleshooting."
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showHelp() {
        val tv = label(HELP_TEXT, 13f, Pal.TEXT)
        tv.setPadding(dp(20), dp(8), dp(20), dp(8))
        val sv = ScrollView(this)
        sv.addView(tv)
        AlertDialog.Builder(this).setTitle("Help").setView(sv)
            .setPositiveButton("Close", null)
            .setNeutralButton("Open App info") { _, _ -> openAppInfo() }
            .show()
    }

    companion object {
        const val RESTRICTED_HELP =
            "Android blocks accessibility for apps installed outside the Play Store until you allow it:\n\n" +
                "1. Tap \"Open Accessibility settings\", tap AutoStep and try to switch it on. It will refuse - that is expected. Go back.\n" +
                "2. Open App info (button below, or long-press the AutoStep icon > App info).\n" +
                "3. Tap the three dots (top-right) > \"Allow restricted settings\" and confirm.\n" +
                "4. Come back here, open Accessibility settings again and switch AutoStep ON.\n\n" +
                "No three dots? Do step 1 first: the option only appears after you tried to enable the service once."

        const val HELP_TEXT =
            "QUICK START\n" +
                "- Turn on the accessibility service.\n" +
                "- Open your game, expand the floating panel (+ Steps) and record steps.\n" +
                "- Press Run. Press Stop (floating panel or app) to end.\n\n" +
                "CAN'T TURN ON ACCESSIBILITY?\n" + RESTRICTED_HELP + "\n\n" +
                "KEY PRESS / TYPE TEXT\n" +
                "- Enable AutoStep Keyboard and select it as your current keyboard.\n" +
                "- Keys only reach an app that has a text field focused. Most touch games do not read keys; " +
                "use Hold or Swipe on the on-screen joystick instead.\n\n" +
                "TIPS\n" +
                "- Coordinates are screen pixels. Re-record steps if you rotate the phone or change screen size.\n" +
                "- Drag the floating panel by its handle so it does not cover your tap targets.\n" +
                "- If your phone kills the service in the background, set AutoStep's battery usage to Unrestricted.\n" +
                "- \"Hold forever\" keeps the script on that step until you press Stop."
    }
}
