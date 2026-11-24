package com.autostep.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra("trace") ?: "No trace"
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0F1115"))
        root.setPadding(32, 64, 32, 32)

        val title = TextView(this)
        title.text = "AutoStep crashed - copy this and send it to Claude"
        title.setTextColor(Color.WHITE)
        title.textSize = 16f
        root.addView(title)

        val copy = TextView(this)
        copy.text = "COPY ERROR"
        copy.setTextColor(Color.WHITE)
        copy.textSize = 16f
        copy.setBackgroundColor(Color.parseColor("#6C8CFF"))
        copy.setPadding(32, 24, 32, 24)
        copy.setOnClickListener {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
        root.addView(copy)

        val body = TextView(this)
        body.text = trace
        body.setTextColor(Color.parseColor("#FF9A9A"))
        body.textSize = 11f
        body.setTextIsSelectable(true)
        val sv = ScrollView(this)
        sv.addView(body)
        root.addView(sv)
        setContentView(root)
    }
}
