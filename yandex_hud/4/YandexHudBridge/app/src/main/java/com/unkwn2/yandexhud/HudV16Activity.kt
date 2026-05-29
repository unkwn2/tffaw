package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("SetTextI18n")
class HudV16Activity : AppCompatActivity() {
    private lateinit var logView: TextView
    private lateinit var scroll: ScrollView
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.init(this)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16) }
        val status = TextView(this).apply { text = "HUD Bridge v16"; textSize = 18f }
        root.addView(status)
        logView = TextView(this).apply { textSize = 11f; setTextColor(0xFF00FF00.toInt()) }
        scroll = ScrollView(this).apply { addView(logView) }
        val scrollLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(scroll, scrollLp)

        fun btn(label: String, fn: () -> Unit): Button {
            val b = Button(this).apply { text = label; setOnClickListener { fn() } }
            root.addView(b)
            return b
        }

        btn("1. Activate HUD (navi=2)") { sendNaviStatus(2) }
        btn("2. Arrow RIGHT + 500m") { sendGuidance(3, 500) }
        btn("3. Arrow LEFT + 300m") { sendGuidance(7, 300) }
        btn("4. Arrow STRAIGHT + 200m") { sendGuidance(1, 200) }
        btn("5. Arrow SLIGHT RIGHT + 150m") { sendGuidance(2, 150) }
        btn("6. Arrow U-TURN + 50m") { sendGuidance(5, 50) }
        btn("7. Road Name test") { sendRoadName("Test Road") }
        btn("8. Close HUD (navi=4)") { sendNaviStatus(4) }
        btn("9. Full sequence test") { fullSequence() }

        btn("A. service call autoservice scan") { scanAutoservice() }
        btn("B. BYDOpen proxy connect") { connectBydOpenProxy() }
        btn("C. Grant notification listener") { grantNotifListener() }
        btn("D. Check notification listener") { checkNotifListener() }

        btn("EXPORT LOG to USB") { exportLog() }
        btn("CLEAR LOG") { clearLog() }

        scrollLp.weight = 1f

        setContentView(root)
        log("v16 HUD Bridge ready — uid=${android.os.Process.myUid()}")
    }

    private fun log(s: String) {
        handler.post {
            val t = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
            logView.append("$t  $s\n")
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
        FileLogger.write("v16", s)
    }

    private fun sendNaviStatus(status: Int) {
        log("sendAutoNaviStatus($status)")
        val i = Intent("action.com.byd.protocol.AUTONAVI_STANDARD_BROADCAST_SEND")
        i.setPackage("com.byd.amapservice")
        i.putExtra("setKey", "KEY_TYPE")
        i.putExtra("setType", status)
        i.putExtra("setValue", 0)
        sendBroadcast(i)
        log("  C broadcast sent type=$status")
        tryServiceCall("sendAutoNaviStatus", status)
    }

    private fun sendGuidance(iconId: Int, distance: Int) {
        log("sendSimpleGuidanceInfo(icon=$iconId, dist=$distance)")
        val i = Intent("action.com.byd.protocol.AUTONAVI_STANDARD_BROADCAST_SEND")
        i.setPackage("com.byd.amapservice")
        i.putExtra("setKey", "KEY_TYPE")
        i.putExtra("setType", iconId)
        i.putExtra("setValue", distance)
        sendBroadcast(i)
        log("  C broadcast sent type=$iconId val=$distance")
        tryServiceCallGuidance(iconId, distance)
    }

    private fun sendRoadName(name: String) {
        log("sendNextPathName('$name')")
        val i = Intent("action.com.byd.protocol.AUTONAVI_STANDARD_BROADCAST_SEND")
        i.setPackage("com.byd.amapservice")
        i.putExtra("setKey", "KEY_TYPE")
        i.putExtra("setType", 100)
        i.putExtra("setStringValue", name)
        sendBroadcast(i)
        log("  C broadcast sent road='$name'")
    }

    private fun fullSequence() {
        log("=== FULL SEQUENCE ===")
        sendNaviStatus(2)
        handler.postDelayed({ sendGuidance(3, 500) }, 500)
        handler.postDelayed({ sendRoadName("Moskovskaya st.") }, 1000)
        handler.postDelayed({ sendGuidance(7, 200) }, 3000)
        handler.postDelayed({ sendRoadName("Sovetskaya st.") }, 3500)
        handler.postDelayed({ sendGuidance(1, 100) }, 6000)
        handler.postDelayed({ sendNaviStatus(4) }, 9000)
    }

    private fun tryServiceCall(method: String, vararg args: Int) {
        try {
            for (svc in listOf("autoservice")) {
                for (tx in 1..30) {
                    val cmdParts = mutableListOf("service", "call", svc, tx.toString())
                    cmdParts.add("i32")
                    cmdParts.add("0")
                    for (a in args) {
                        cmdParts.add("i32")
                        cmdParts.add(a.toString())
                    }
                    val proc = Runtime.getRuntime().exec(cmdParts.toTypedArray())
                    proc.outputStream.close()
                    val out = proc.inputStream.bufferedReader().readText().trim()
                    val exit = proc.waitFor()
                    if (out.isNotEmpty() && !out.contains("Transaction too large")) {
                        log("  SVC $svc tx=$tx args=${args.toList()} => exit=$exit '$out'")
                    }
                }
            }
        } catch (t: Throwable) {
            log("  SVC ERR: ${t.message}")
        }
    }

    private fun tryServiceCallGuidance(iconId: Int, distance: Int) {
        try {
            for (svc in listOf("autoservice", "auto_container")) {
                val cmd = arrayOf("service", "call", svc, "1", "i32", "0", "i32", iconId.toString(), "i32", distance.toString())
                val proc = Runtime.getRuntime().exec(cmd)
                proc.outputStream.close()
                val out = proc.inputStream.bufferedReader().readText().trim()
                val exit = proc.waitFor()
                log("  SVC_GUID $svc => exit=$exit '$out'")
            }
        } catch (t: Throwable) {
            log("  SVC_GUID ERR: ${t.message}")
        }
    }

    private fun scanAutoservice() {
        log("=== SCANNING autoservice ===")
        Thread {
            try {
                for (tx in 1..50) {
                    val cmd = arrayOf("service", "call", "autoservice", tx.toString(), "i32", "0")
                    val proc = Runtime.getRuntime().exec(cmd)
                    proc.outputStream.close()
                    val out = proc.inputStream.bufferedReader().readText().trim()
                    val exit = proc.waitFor()
                    if (out.isNotEmpty() && !out.contains("Transaction too large") && !out.contains("Bad transaction")) {
                        log("  tx=$tx exit=$exit => '$out'")
                    }
                }
                log("SCAN DONE")
            } catch (t: Throwable) {
                log("SCAN ERR: ${t.message}")
            }
        }.start()
    }

    private fun connectBydOpenProxy() {
        log("Trying to connect to BYDOpen proxy...")
        try {
            val intent = Intent("com.sr.openbyd.PROXY_CONNECTED")
            intent.setPackage("com.sr.openbyd")
            sendBroadcast(intent)
            log("  Broadcast sent to com.sr.openbyd (no binder extra)")
        } catch (t: Throwable) {
            log("  ERR: ${t.message}")
        }
        try {
            val pi = packageManager.getPackageInfo("com.sr.openbyd", 0)
            log("  BYDOpen installed: uid=${pi.applicationInfo?.uid}")
        } catch (t: Throwable) {
            log("  BYDOpen NOT found")
        }
    }

    private fun grantNotifListener() {
        log("Granting notification listener...")
        try {
            val cmd = arrayOf("cmd", "notification", "allow_listener", "com.unkwn2.yandexhud/com.unkwn2.yandexhud.HudNavListenerService")
            val proc = Runtime.getRuntime().exec(cmd)
            val out = proc.inputStream.bufferedReader().readText().trim()
            val exit = proc.waitFor()
            log("  grant exit=$exit out='$out'")
        } catch (t: Throwable) {
            log("  grant ERR: ${t.message}")
        }
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {}
    }

    private fun checkNotifListener() {
        val cn = ComponentName(this, HudNavListenerService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(cn.flattenToString()) == true
        log("NotificationListener enabled=$enabled")
        log("  component=${cn.flattenToString()}")
    }

    private fun exportLog() {
        try {
            val src = FileLogger.getFile() ?: return log("No log file")
            val dir = android.os.Environment.getExternalStorageDirectory()
            val usbDir = java.io.File(dir, "Download")
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
            val dst = java.io.File(usbDir, "yandex_hud_v16_${ts}.log")
            src.copyTo(dst, overwrite = true)
            log("Exported to ${dst.absolutePath}")
        } catch (t: Throwable) {
            log("Export ERR: ${t.message}")
        }
    }

    private fun clearLog() {
        FileLogger.clear()
        logView.text = ""
        log("Log cleared")
    }
}
