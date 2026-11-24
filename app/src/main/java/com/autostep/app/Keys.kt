package com.autostep.app

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import java.util.Random

/**
 * Builds and sends key events through AutoKeyboardService (our own IME).
 * A "hold" = ACTION_DOWN, then repeated ACTION_DOWN with repeatCount++ (like a real held key), then ACTION_UP.
 */
object KeyInjector {
    private const val NO_IME =
        "AutoStep keyboard is not active. Enable it and select it as the current keyboard."
    private const val NO_FIELD =
        "No input field is focused. Tap a text field in the app/game (with AutoStep keyboard selected) first."

    private const val LETTERS = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.<>/?"
    private val rnd = Random()

    /** Names you can type in the "Key" field instead of a single character. */
    val named: Map<String, Int> = linkedMapOf(
        "ENTER" to KeyEvent.KEYCODE_ENTER,
        "SPACE" to KeyEvent.KEYCODE_SPACE,
        "BACKSPACE" to KeyEvent.KEYCODE_DEL,
        "TAB" to KeyEvent.KEYCODE_TAB,
        "ESC" to KeyEvent.KEYCODE_ESCAPE,
        "UP" to KeyEvent.KEYCODE_DPAD_UP,
        "DOWN" to KeyEvent.KEYCODE_DPAD_DOWN,
        "LEFT" to KeyEvent.KEYCODE_DPAD_LEFT,
        "RIGHT" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "SHIFT" to KeyEvent.KEYCODE_SHIFT_LEFT,
        "CTRL" to KeyEvent.KEYCODE_CTRL_LEFT,
        "ALT" to KeyEvent.KEYCODE_ALT_LEFT
    ).also { m ->
        for (n in 1..12) m["F$n"] = KeyEvent.KEYCODE_F1 + (n - 1)
    }

    private data class Part(val code: Int, val meta: Int)

    private fun pick(pool: String): String = pool[rnd.nextInt(pool.length)].toString()

    private fun resolveSpec(step: Step): String = when (step.keyMode) {
        KeyMode.FIXED -> step.key.trim().ifEmpty { "a" }
        KeyMode.RAND_LETTER -> pick(LETTERS)
        KeyMode.RAND_DIGIT -> pick(DIGITS)
        KeyMode.RAND_SYMBOL -> pick(SYMBOLS)
        KeyMode.RAND_ANY -> pick(LETTERS + LETTERS.uppercase() + DIGITS + SYMBOLS)
    }

    /** Converts "a", "A", "@", "ENTER" ... to key codes (+ meta state like SHIFT). Null if unmappable. */
    private fun parts(spec: String): List<Part>? {
        if (spec.length > 1) {
            val code = named[spec.uppercase()] ?: return null
            return listOf(Part(code, 0))
        }
        val map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
        val evs = map.getEvents(charArrayOf(spec[0])) ?: return null
        val downs = evs.filter { it.action == KeyEvent.ACTION_DOWN }.map { Part(it.keyCode, it.metaState) }
        return if (downs.isEmpty()) null else downs
    }

    private fun ev(downTime: Long, action: Int, p: Part, repeat: Int): KeyEvent =
        KeyEvent(
            downTime, SystemClock.uptimeMillis(), action, p.code, repeat, p.meta,
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        )

    /** Returns an error message, or null on success. */
    fun press(step: Step, holdMs: Long, stop: () -> Boolean): String? {
        val kb = AutoKeyboardService.instance ?: return NO_IME
        if (!kb.hasInput()) return NO_FIELD
        val spec = resolveSpec(step)
        val ps = parts(spec)
        if (ps == null) {          // symbol/unicode that has no key code: just insert the text
            kb.commitText(spec)
            return null
        }
        val downTime = SystemClock.uptimeMillis()
        try {
            ps.forEach { kb.send(ev(downTime, KeyEvent.ACTION_DOWN, it, 0)) }
            val mainKey = ps.last()
            val end = downTime + holdMs
            var rep = 0
            while (!stop() && (step.infinite || SystemClock.uptimeMillis() < end)) {
                Thread.sleep(50)
                if (stop()) break
                if (step.infinite || SystemClock.uptimeMillis() < end) {
                    rep++
                    if (holdMs > 120 || step.infinite) kb.send(ev(downTime, KeyEvent.ACTION_DOWN, mainKey, rep))
                }
            }
        } finally {
            ps.reversed().forEach { kb.send(ev(downTime, KeyEvent.ACTION_UP, it, 0)) }
        }
        return null
    }

    fun typeText(text: String, stop: () -> Boolean): String? {
        val kb = AutoKeyboardService.instance ?: return NO_IME
        if (!kb.hasInput()) return NO_FIELD
        for (ch in text) {
            if (stop()) break
            kb.commitText(ch.toString())
            Thread.sleep(35)
        }
        return null
    }
}
