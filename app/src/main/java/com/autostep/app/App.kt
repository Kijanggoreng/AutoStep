package com.autostep.app

import android.app.Application
import android.content.Intent
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/** Catches any crash and shows the stack trace in CrashActivity (separate process) so it can be copied. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val text = "AutoStep crash\nThread: ${t.name}\n" +
                    "Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n$sw"
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra("trace", text.take(60000))
                )
                Thread.sleep(600)
            } catch (x: Throwable) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}
