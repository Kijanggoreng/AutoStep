package com.autostep.app

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A small but complete QWERTY keyboard. You can type with it normally, and scripts use
 * its input connection to inject key presses / text into whatever app has focus.
 */
class AutoKeyboardService : InputMethodService() {
    companion object {
        @Volatile var instance: AutoKeyboardService? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private var rootView: LinearLayout? = null
    private var shift = false
    private var symbols = false

    private val numbers = "1234567890"
    private val letterRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val symbolRows = listOf("!@#\$%^&*()", "-_=+[]{}\\|", ";:'\"<>/?")

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        r.setBackgroundColor(Pal.BG)
        r.setPadding(dp(3), dp(4), dp(3), dp(6))
        rootView = r
        rebuild()
        return r
    }

    // ---------------------------------------------------------------- keyboard UI
    private fun key(text: String, weight: Float = 1f, bg: Int = Pal.FIELD, onClick: () -> Unit): TextView {
        val v = TextView(this)
        v.text = text
        v.textSize = 18f
        v.setTextColor(Pal.TEXT)
        v.gravity = Gravity.CENTER
        v.background = rounded(bg, 8)
        v.isClickable = true
        v.setOnClickListener { onClick() }
        val lp = LinearLayout.LayoutParams(0, dp(46), weight)
        lp.setMargins(dp(2), dp(3), dp(2), dp(3))
        v.layoutParams = lp
        return v
    }

    private fun charKey(c: String): TextView = key(c) { typeChar(c) }

    private fun row(vararg views: TextView): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.HORIZONTAL
        views.forEach { l.addView(it) }
        return l
    }

    private fun rebuild() {
        val r = rootView ?: return
        r.removeAllViews()
        r.addView(row(*numbers.map { charKey(it.toString()) }.toTypedArray()))
        if (!symbols) {
            r.addView(row(*letterRows[0].map { charKey(cased(it)) }.toTypedArray()))
            r.addView(row(*letterRows[1].map { charKey(cased(it)) }.toTypedArray()))
            val third = mutableListOf<TextView>()
            third.add(key("\u21E7", 1.5f, if (shift) Pal.ACCENT else Pal.FIELD) { shift = !shift; rebuild() })
            letterRows[2].forEach { third.add(charKey(cased(it))) }
            third.add(key("\u232B", 1.5f) { backspace() })
            r.addView(row(*third.toTypedArray()))
        } else {
            r.addView(row(*symbolRows[0].map { charKey(it.toString()) }.toTypedArray()))
            r.addView(row(*symbolRows[1].map { charKey(it.toString()) }.toTypedArray()))
            val third = mutableListOf<TextView>()
            third.add(charKey("~"))
            symbolRows[2].forEach { third.add(charKey(it.toString())) }
            third.add(key("\u232B", 1.5f) { backspace() })
            r.addView(row(*third.toTypedArray()))
        }
        r.addView(row(
            key(if (symbols) "ABC" else "?123", 1.5f) { symbols = !symbols; rebuild() },
            key("\uD83C\uDF10", 1f) {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            },
            charKey(","),
            key("space", 4f) { typeChar(" ") },
            charKey("."),
            key("\u23CE", 1.5f, Pal.ACCENT) { enter() }
        ))
    }

    private fun cased(c: Char): String = if (shift) c.uppercaseChar().toString() else c.toString()

    private fun typeChar(c: String) {
        currentInputConnection?.commitText(c, 1)
        if (shift) { shift = false; rebuild() }
    }

    private fun backspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    private fun enter() {
        val ei = currentInputEditorInfo
        val action = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noEnterAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (ei != null && !noEnterAction &&
            action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    // ---------------------------------------------------------------- used by scripts
    /** True when a text field/app currently has an active input connection. */
    fun hasInput(): Boolean = currentInputConnection != null

    fun send(event: KeyEvent) {
        main.post { currentInputConnection?.sendKeyEvent(event) }
    }

    fun commitText(text: String) {
        main.post { currentInputConnection?.commitText(text, 1) }
    }
}
