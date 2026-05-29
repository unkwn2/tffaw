package com.unkwn2.yandexhud

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.content.Intent
import android.os.Bundle
import java.util.regex.Pattern

class HudNavListenerService : NotificationListenerService() {

    private val NAV_PACKAGES = setOf(
        "ru.yandex.yandexmaps",
        "ru.yandex.yandexnavi",
        "com.autonavi.amapauto",
        "com.google.android.apps.maps",
        "com.waze"
    )

    private var isActive = false
    private var lastIconId = -1
    private var lastDistance = -1
    private var lastRoadName = ""
    private var lastTitle = ""
    private var lastText = ""

    override fun onListenerConnected() {
        super.onListenerConnected()
        FileLogger.write("HudNav", "NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        if (isActive) closeHud()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val pkg = sbn.packageName ?: return
        if (pkg !in NAV_PACKAGES) return

        val n = sbn.notification ?: return
        val extras = n.extras ?: return
        if (n.flags and Notification.FLAG_ONGOING_EVENT == 0) return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        lastTitle = title
        lastText = text

        FileLogger.write("HudNav", "NOTIF from=$pkg title='$title' text='$text' sub='$subText'")

        val combined = "$title $text".lowercase()

        val iconId = parseDirection(combined)
        val distance = parseDistance(combined)
        val roadName = parseRoadName(title, text)

        FileLogger.write("HudNav", "PARSED iconId=$iconId dist=$distance road='$roadName'")

        if (iconId >= 0 && distance >= 0) {
            if (!isActive) activateHud()
            sendGuidance(iconId, distance)
            if (roadName.isNotBlank() && roadName != lastRoadName) {
                sendRoadName(roadName)
                lastRoadName = roadName
            }
            lastIconId = iconId
            lastDistance = distance
        }
    }

    private fun parseDirection(text: String): Int {
        return when {
            text.contains("slight left") || text.contains("keep left") ||
                text.contains("\u043f\u043b\u0430\u0432\u043d\u043e \u043d\u0430\u043b\u0435\u0432\u043e") ||
                text.contains("\u0434\u0435\u0440\u0436\u0438\u0442\u0435\u0441\u044c \u043b\u0435\u0432\u0435\u0435") -> 8
            text.contains("slight right") || text.contains("keep right") ||
                text.contains("\u043f\u043b\u0430\u0432\u043d\u043e \u043d\u0430\u043f\u0440\u0430\u0432\u043e") ||
                text.contains("\u0434\u0435\u0440\u0436\u0438\u0442\u0435\u0441\u044c \u043f\u0440\u0430\u0432\u0435\u0435") -> 2
            text.contains("u-turn") || text.contains("make a u-turn") ||
                text.contains("\u0440\u0430\u0437\u0432\u043e\u0440\u043e\u0442") ||
                text.contains("\u0440\u0430\u0437\u0432\u0435\u0440\u043d\u0438\u0442\u0435\u0441\u044c") -> 5
            text.contains("turn left") || text.contains(" left") ||
                text.contains("\u043d\u0430\u043b\u0435\u0432\u043e") ||
                text.contains("\u043f\u043e\u0432\u0435\u0440\u043d\u0438\u0442\u0435 \u043d\u0430\u043b\u0435\u0432\u043e") -> 7
            text.contains("turn right") || text.contains(" right") ||
                text.contains("\u043d\u0430\u043f\u0440\u0430\u0432\u043e") ||
                text.contains("\u043f\u043e\u0432\u0435\u0440\u043d\u0438\u0442\u0435 \u043d\u0430\u043f\u0440\u0430\u0432\u043e") -> 3
            text.contains("roundabout") || text.contains("exit") ||
                text.contains("\u043a\u043e\u043b\u044c\u0446\u043e") ||
                text.contains("\u043a\u0440\u0443\u0433\u043e\u0432\u043e\u0435") -> 9
            text.contains("straight") || text.contains("ahead") || text.contains("continue") ||
                text.contains("\u043f\u0440\u044f\u043c\u043e") ||
                text.contains("\u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0430\u0439\u0442\u0435") -> 1
            else -> -1
        }
    }

    private fun parseDistance(text: String): Int {
        val p = Pattern.compile("(?i)\\b(\\d+[\\.,]?\\d*)\\s*(m|\u043c|km|\u043a\u043c)\\b")
        val m = p.matcher(text)
        if (!m.find()) return -1
        val value = m.group(1)?.replace(",", ".")?.toFloatOrNull() ?: return -1
        val unit = m.group(2)?.lowercase() ?: return -1
        return if (unit == "km" || unit == "\u043a\u043c") (value * 1000).toInt()
        else value.toInt()
    }

    private fun parseRoadName(title: String, text: String): String {
        var road = "$title $text"
        for (prefix in listOf("on ", "onto ", "\u043d\u0430 ", "\u0432 \u0441\u0442\u043e\u0440\u043e\u043d\u0443 ")) {
            val idx = road.lowercase().indexOf(prefix)
            if (idx >= 0) {
                road = road.substring(idx + prefix.length).trim()
                break
            }
        }
        road = Pattern.compile("(?i)\\b(\\d+[\\.,]?\\d*)\\s*(m|\u043c|km|\u043a\u043c)\\b").matcher(road).replaceAll("").trim()
        return if (road.length in 3..25) road else ""
    }

    private fun activateHud() {
        FileLogger.write("HudNav", "ACTIVATE HUD (status=2)")
        sendCBroadcast(2, 0)
        tryServiceCall("sendAutoNaviStatus", "2")
        isActive = true
    }

    private fun closeHud() {
        FileLogger.write("HudNav", "CLOSE HUD (status=4)")
        sendCBroadcast(4, 0)
        tryServiceCall("sendAutoNaviStatus", "4")
        isActive = false
        lastIconId = -1
        lastDistance = -1
        lastRoadName = ""
    }

    private fun sendGuidance(iconId: Int, distance: Int) {
        FileLogger.write("HudNav", "GUIDANCE icon=$iconId dist=$distance")
        sendCBroadcast(iconId, distance)
        tryServiceCallGuidance(iconId, distance)
    }

    private fun sendRoadName(name: String) {
        FileLogger.write("HudNav", "ROAD='$name'")
        val i = Intent("action.com.byd.protocol.AUTONAVI_STANDARD_BROADCAST_SEND")
        i.setPackage("com.byd.amapservice")
        i.putExtra("setKey", "KEY_TYPE")
        i.putExtra("setType", 100)
        i.putExtra("setStringValue", name)
        sendBroadcast(i)
    }

    private fun sendCBroadcast(typeCode: Int, value: Int) {
        val i = Intent("action.com.byd.protocol.AUTONAVI_STANDARD_BROADCAST_SEND")
        i.setPackage("com.byd.amapservice")
        i.putExtra("setKey", "KEY_TYPE")
        i.putExtra("setType", typeCode)
        i.putExtra("setValue", value)
        sendBroadcast(i)
        FileLogger.write("HudNav", "C_BROADCAST type=$typeCode val=$value")
    }

    private fun tryServiceCall(method: String, vararg args: String) {
        try {
            val cmd = mutableListOf("service", "call", "autoservice")
            val txCode = getTxCode(method)
            if (txCode < 0) {
                FileLogger.write("HudNav", "SVC_CALL: no txCode for $method")
                return
            }
            cmd.add(txCode.toString())
            cmd.add("i32")
            cmd.add("0")
            for (arg in args) {
                cmd.add("i32")
                cmd.add(arg)
            }
            val proc = Runtime.getRuntime().exec(cmd.toTypedArray())
            proc.outputStream.close()
            val out = proc.inputStream.bufferedReader().readText().trim()
            val err = proc.errorStream.bufferedReader().readText().trim()
            val exit = proc.waitFor()
            FileLogger.write("HudNav", "SVC_CALL $method exit=$exit out='$out' err='$err'")
        } catch (t: Throwable) {
            FileLogger.write("HudNav", "SVC_CALL ERR: ${t.message}")
        }
    }

    private fun tryServiceCallGuidance(iconId: Int, distance: Int) {
        for (svc in listOf("autoservice", "auto_container", "AutoContainerNative")) {
            try {
                for (tx in listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)) {
                    val cmd = arrayOf("service", "call", svc, tx.toString(), "i32", "0", "i32", iconId.toString(), "i32", distance.toString())
                    val proc = Runtime.getRuntime().exec(cmd)
                    proc.outputStream.close()
                    val out = proc.inputStream.bufferedReader().readText().trim()
                    proc.waitFor()
                    if (out.isNotEmpty() && !out.contains("Transaction too large") && !out.contains("Bad transaction")) {
                        FileLogger.write("HudNav", "SVC_SCAN $svc tx=$tx icon=$iconId dist=$distance => '$out'")
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun getTxCode(method: String): Int = when (method) {
        "sendAutoNaviStatus" -> -1
        "sendSimpleGuidanceInfo" -> -1
        else -> -1
    }
}
