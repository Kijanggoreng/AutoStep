package com.autostep.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Saves all scripts to filesDir/scripts.json. Shared in-memory between Activity and services. */
object Store {
    private var file: File? = null
    val scripts = mutableListOf<Script>()
    var currentIndex = 0

    @Synchronized
    fun init(ctx: Context) {
        if (file != null) return
        file = File(ctx.applicationContext.filesDir, "scripts.json")
        load()
    }

    fun current(): Script {
        if (scripts.isEmpty()) scripts.add(Script("Script 1"))
        if (currentIndex !in scripts.indices) currentIndex = 0
        return scripts[currentIndex]
    }

    @Synchronized
    fun save() {
        val f = file ?: return
        try {
            val root = JSONObject()
            root.put("current", currentIndex)
            root.put("scripts", JSONArray().also { arr -> scripts.forEach { arr.put(it.toJson()) } })
            f.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun load() {
        val f = file ?: return
        try {
            if (f.exists()) {
                val root = JSONObject(f.readText())
                currentIndex = root.optInt("current", 0)
                val arr = root.optJSONArray("scripts") ?: JSONArray()
                for (i in 0 until arr.length()) scripts.add(Script.fromJson(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (scripts.isEmpty()) scripts.add(Script("Script 1"))
    }
}
