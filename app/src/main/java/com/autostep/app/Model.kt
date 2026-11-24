package com.autostep.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class StepType(val label: String) {
    TAP("Tap"),
    RANDOM_TAP("Random tap (in area)"),
    HOLD("Hold"),
    SWIPE("Swipe"),
    KEY("Keyboard key"),
    TEXT("Type text"),
    WAIT("Wait")
}

enum class KeyMode(val label: String) {
    FIXED("Fixed key"),
    RAND_LETTER("Random letter"),
    RAND_DIGIT("Random digit"),
    RAND_SYMBOL("Random symbol"),
    RAND_ANY("Random any")
}

fun fmtMs(ms: Long): String =
    if (ms >= 1000) {
        if (ms % 1000 == 0L) "${ms / 1000}s" else String.format(Locale.US, "%.1fs", ms / 1000.0)
    } else "${ms}ms"

fun fmtRange(a: Long, b: Long): String = if (b > a) "${fmtMs(a)}-${fmtMs(b)}" else fmtMs(a)

data class Step(
    var type: StepType = StepType.TAP,
    var x: Int = 540,
    var y: Int = 1200,
    var x2: Int = 540,
    var y2: Int = 600,
    var durationMs: Long = 1000,
    var durationMaxMs: Long = 0,
    var infinite: Boolean = false,
    var jitter: Int = 0,
    var keyMode: KeyMode = KeyMode.FIXED,
    var key: String = "a",
    var text: String = "",
    var repeat: Int = 1,
    var delayMs: Long = 300,
    var delayMaxMs: Long = 0,
    var enabled: Boolean = true
) {
    fun title(): String = when (type) {
        StepType.TAP -> "Tap ($x, $y)" + (if (jitter > 0) "  +-$jitter px" else "")
        StepType.RANDOM_TAP -> "Random tap in ($x, $y) to ($x2, $y2)"
        StepType.HOLD -> "Hold ($x, $y) for " + (if (infinite) "forever" else fmtRange(durationMs, durationMaxMs))
        StepType.SWIPE -> "Swipe ($x, $y) to ($x2, $y2) in ${fmtMs(durationMs)}"
        StepType.KEY -> "Key " + (if (keyMode == KeyMode.FIXED) key else keyMode.label) +
            ", hold " + (if (infinite) "forever" else fmtRange(durationMs, durationMaxMs))
        StepType.TEXT -> "Type \"" + text.take(24) + "\""
        StepType.WAIT -> "Wait " + fmtRange(durationMs, durationMaxMs)
    }

    fun detail(): String {
        val parts = mutableListOf<String>()
        if (repeat > 1) parts.add("x$repeat")
        parts.add("then wait " + fmtRange(delayMs, delayMaxMs))
        if (!enabled) parts.add("disabled")
        return parts.joinToString(" | ")
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.name); put("x", x); put("y", y); put("x2", x2); put("y2", y2)
        put("durationMs", durationMs); put("durationMaxMs", durationMaxMs)
        put("infinite", infinite); put("jitter", jitter)
        put("keyMode", keyMode.name); put("key", key); put("text", text)
        put("repeat", repeat); put("delayMs", delayMs); put("delayMaxMs", delayMaxMs)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(o: JSONObject): Step = Step(
            type = runCatching { StepType.valueOf(o.optString("type", "TAP")) }.getOrDefault(StepType.TAP),
            x = o.optInt("x", 540), y = o.optInt("y", 1200),
            x2 = o.optInt("x2", 540), y2 = o.optInt("y2", 600),
            durationMs = o.optLong("durationMs", 1000), durationMaxMs = o.optLong("durationMaxMs", 0),
            infinite = o.optBoolean("infinite", false), jitter = o.optInt("jitter", 0),
            keyMode = runCatching { KeyMode.valueOf(o.optString("keyMode", "FIXED")) }.getOrDefault(KeyMode.FIXED),
            key = o.optString("key", "a"), text = o.optString("text", ""),
            repeat = o.optInt("repeat", 1), delayMs = o.optLong("delayMs", 300),
            delayMaxMs = o.optLong("delayMaxMs", 0), enabled = o.optBoolean("enabled", true)
        )
    }
}

class Script(
    var name: String,
    val steps: MutableList<Step> = mutableListOf(),
    var loops: Int = 1,          // 0 = infinite
    var startDelaySec: Int = 3
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name); put("loops", loops); put("startDelaySec", startDelaySec)
        put("steps", JSONArray().also { arr -> steps.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(o: JSONObject): Script {
            val s = Script(o.optString("name", "Script"), loops = o.optInt("loops", 1),
                startDelaySec = o.optInt("startDelaySec", 3))
            val arr = o.optJSONArray("steps") ?: JSONArray()
            for (i in 0 until arr.length()) s.steps.add(Step.fromJson(arr.getJSONObject(i)))
            return s
        }
    }
}
