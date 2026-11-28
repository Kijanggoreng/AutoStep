package com.autostep.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/** Colour palette used by the app, overlay and keyboard. */
object Pal {
    val BG = Color.parseColor("#0F1115")
    val CARD = Color.parseColor("#1A1D24")
    val FIELD = Color.parseColor("#252A33")
    val ACCENT = Color.parseColor("#6C8CFF")
    val GREEN = Color.parseColor("#2FBF71")
    val RED = Color.parseColor("#FF5C5C")
    val TEXT = Color.parseColor("#EDEFF4")
    val MUTED = Color.parseColor("#9AA3B2")
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun Context.rounded(color: Int, radiusDp: Int, strokeColor: Int? = null): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) setStroke(dp(1), strokeColor)
    }

fun Context.label(text: String, sizeSp: Float = 14f, color: Int = Pal.TEXT, bold: Boolean = false): TextView =
    TextView(this).apply {
        this.text = text
        textSize = sizeSp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

fun Context.pill(text: String, color: Int = Pal.ACCENT, onClick: () -> Unit): TextView =
    TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(9), dp(14), dp(9))
        background = rounded(color, 12)
        isClickable = true
        setOnClickListener { onClick() }
    }

fun Context.card(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(16), dp(14), dp(16), dp(14))
    background = rounded(Pal.CARD, 16)
}

/** Small pill used for quick actions and option selectors. */
fun Context.chip(text: String, color: Int = Pal.FIELD, onClick: () -> Unit): TextView =
    TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(7), dp(10), dp(7))
        background = rounded(color, 10)
        isClickable = true
        setOnClickListener { onClick() }
    }

/** Horizontal single-choice selector. selected = -1 means nothing highlighted. */
fun Context.chipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit): View {
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    val views = mutableListOf<TextView>()
    fun paint(sel: Int) {
        views.forEachIndexed { i, v -> v.background = rounded(if (i == sel) Pal.ACCENT else Pal.FIELD, 10) }
    }
    labels.forEachIndexed { i, l ->
        val v = chip(l, Pal.FIELD) { paint(i); onSelect(i) }
        views.add(v)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.rightMargin = dp(6)
        row.addView(v, lp)
    }
    paint(selected)
    val hs = HorizontalScrollView(this)
    hs.isHorizontalScrollBarEnabled = false
    hs.addView(row)
    return hs
}
