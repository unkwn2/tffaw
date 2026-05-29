package com.unkwn2.yandexhud

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HudAccessibilityService : AccessibilityService() {
    private val tag = "HudA11y"
    private var lastTurn = -1
    private var lastDist = -1
    private var initialized = false
    private var hudEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private val rxDist = Regex("""(\d+)\s*(м|m|км|km)""", RegexOption.IGNORE_CASE)

    private val stopRunnable = Runnable {
        if (hudEnabled) {
            FileLogger.write(tag, "auto-stop: setNavControl(0)")
            ProtocolServiceBridge.setNavControl(0)
            hudEnabled = false
        }
    }

    override fun onServiceConnected() {
        FileLogger.init(this)
        FileLogger.write(tag, "service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName != "ru.yandex.yandexnavi") return
        if (!initialized) {
            FileLogger.write(tag, "first yandexnavi event -> bind + setNavControl(3) in 500ms")
            ProtocolServiceBridge.bind(this)
            handler.postDelayed({
                ProtocolServiceBridge.setNavControl(3)
                hudEnabled = true
            }, 500)
            initialized = true
        }
        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        collectText(root, texts)
        parseAndPush(texts)
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        node ?: return
        node.text?.let { out += it.toString() }
        node.contentDescription?.let { out += it.toString() }
        for (i in 0 until node.childCount) collectText(node.getChild(i), out)
    }

    private fun parseAndPush(texts: List<String>) {
        if (texts.isEmpty()) return
        var distMeters = -1
        var maneuver: String? = null
        var roadName: String? = null
        for (t in texts) {
            if (distMeters < 0) rxDist.find(t)?.let { m ->
                val v = m.groupValues[1].toIntOrNull() ?: 0
                val u = m.groupValues[2].lowercase()
                distMeters = if (u.startsWith("к") || u.startsWith("k")) v * 1000 else v
            }
            val low = t.lowercase()
            if (maneuver == null) maneuver = when {
                "налево" in low || "left" in low -> "left"
                "направо" in low || "right" in low -> "right"
                "разворот" in low || "uturn" in low -> "uturn"
                "прямо" in low || "straight" in low -> "straight"
                "круг" in low || "roundabout" in low -> "roundabout"
                else -> null
            }
            if (roadName == null && t.length in 3..40 && !t.contains("км") && !t.contains("м ")) {
                roadName = t
            }
        }
        if (maneuver == null && distMeters < 0) return
        val turn = TurnKindMap.mapYandexAction(maneuver)
        if (turn != lastTurn || distMeters != lastDist) {
            val safe = TurnKindMap.sanitizeRoadName(roadName)
            FileLogger.write(tag, "guide turn=" + turn + " dist=" + distMeters + " road=" + safe)
            if (BydHudController.isReady()) {
                BydHudController.sendSimpleGuidanceInfo(turn, if (distMeters > 0) distMeters else 0)
                if (safe.isNotEmpty()) BydHudController.sendNextPathName(safe)
            }
            lastTurn = turn; lastDist = distMeters
            handler.removeCallbacks(stopRunnable)
            handler.postDelayed(stopRunnable, 30_000)
        }
    }

    override fun onInterrupt() {}
}
