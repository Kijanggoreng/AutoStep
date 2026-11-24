package com.autostep.app

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout

/**
 * Minimal keyboard. It shows a thin status bar; its real job is exposing
 * currentInputConnection so scripts can inject key events and text.
 */
class AutoKeyboardService : InputMethodService() {
    companion object {
        @Volatile var instance: AutoKeyboardService? = null
    }

    private val main = Handler(Looper.getMainLooper())

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
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = android.view.Gravity.CENTER_VERTICAL
        row.setBackgroundColor(Pal.CARD)
        row.setPadding(dp(14), dp(10), dp(14), dp(10))
        val tv = label("AutoStep keyboard - ready for scripts", 14f, Pal.TEXT)
        row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(pill("Switch keyboard", Pal.ACCENT) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        return row
    }

    /** True when a text field/app currently has an active input connection. */
    fun hasInput(): Boolean = currentInputConnection != null

    fun send(event: KeyEvent) {
        main.post { currentInputConnection?.sendKeyEvent(event) }
    }

    fun commitText(text: String) {
        main.post { currentInputConnection?.commitText(text, 1) }
    }
}
