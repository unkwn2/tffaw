package com.unkwn2.yandexhud

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var logView: TextView
    private val ts = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val FID_CANDIDATE_HUD = 0x4c10a018
    private val FID_ARHUD_IMG_TYPE = 0x34c00026
    private val FID_ARHUD_IMG_COLOR = 0x34c00032
    private val FID_METER_NAVI_TYPE = 0x40c03032
    private val FID_CAN_NAVI_STATE = 0x43e0003a
    private val FID_CAN_TURN_ICON = 0x43f01030
    private val FID_CAN_CLEAR = 0x43f08018

    private var proxyReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusView = findViewById(R.id.status)
        logView = findViewById(R.id.log)
        logView.movementMethod = ScrollingMovementMethod()
        FileLogger.init(this)
        log("APK boot v12 — libadb-android + dilink-services + AutoContainerManager")
        statusView.text = "v12 ready. AUTO START PROXY or manual app_process."

        registerProxyReceiver()

        // --- Row 1: Green/Blue (Proxy + ADB) ---
        findViewById<Button>(R.id.btnAutoProxy).setOnClickListener {
            log("AUTO START PROXY ->")
            Thread {
                val result = AdbLocalClient.startProxy(this@MainActivity)
                runOnUiThread {
                    log("AUTO PROXY: $result")
                    if (result.startsWith("OK")) {
                        statusView.text = "AUTO PROXY STARTED — wait for broadcast"
                    }
                }
            }.start()
        }
        findViewById<Button>(R.id.btnCheckAdb).setOnClickListener {
            log("CHECK ADB ->")
            Thread {
                val result = AdbLocalClient.checkPort()
                runOnUiThread { log("ADB: $result") }
            }.start()
        }
        findViewById<Button>(R.id.btnProxyPing).setOnClickListener {
            log("PING -> " + ShellProxyBridge.ping())
        }
        findViewById<Button>(R.id.btnProxyScrap).setOnClickListener {
            log("SCRAP ->")
            log(ShellProxyBridge.scrapBydAuto())
        }
        findViewById<Button>(R.id.btnProxyGet).setOnClickListener {
            log("GET FIDs ->")
            log(ShellProxyBridge.getInstrumentFeature(FID_CANDIDATE_HUD))
            log(ShellProxyBridge.getInstrumentFeature(FID_ARHUD_IMG_TYPE))
            log(ShellProxyBridge.getInstrumentFeature(FID_ARHUD_IMG_COLOR))
            log(ShellProxyBridge.getInstrumentFeature(FID_CAN_NAVI_STATE))
        }
        findViewById<Button>(R.id.btnProxyProbe).setOnClickListener {
            log("PROBE ->")
            log(ShellProxyBridge.probe())
        }
        findViewById<Button>(R.id.btnCheckHandshake).setOnClickListener {
            log("HANDSHAKE FILE ->")
            checkHandshakeFile()
        }
        findViewById<Button>(R.id.btnAdbCmd).setOnClickListener {
            showAdbCommand()
        }
        findViewById<Button>(R.id.btnExportLog).setOnClickListener { exportLog() }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            FileLogger.clear()
            logView.text = ""
            log("log cleared")
        }

        // --- Row 2: Red (CAN real AmapService IDs) ---
        findViewById<Button>(R.id.btnCanNaviOn).setOnClickListener {
            log("CAN NAVI ON ->")
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_NAVI_STATE, 2))
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_TURN_ICON, 1001))
        }
        findViewById<Button>(R.id.btnCanNaviOff).setOnClickListener {
            log("CAN NAVI OFF ->")
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_NAVI_STATE, 1))
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_CLEAR, 0xFFFFFF))
        }
        findViewById<Button>(R.id.btnCanTurnStraight).setOnClickListener {
            log("CAN \u2191 -> set(0x43f01030, 1001)")
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_TURN_ICON, 1001))
        }
        findViewById<Button>(R.id.btnCanTurnRight).setOnClickListener {
            log("CAN \u2192 -> set(0x43f01030, 1003)")
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_TURN_ICON, 1003))
        }
        findViewById<Button>(R.id.btnCanTurnLeft).setOnClickListener {
            log("CAN \u2190 -> set(0x43f01030, 1007)")
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_TURN_ICON, 1007))
        }
        findViewById<Button>(R.id.btnCanTurnUturn).setOnClickListener {
            log("CAN UTURN -> set(0x43f01030, 1005)")
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_TURN_ICON, 1005))
        }
        findViewById<Button>(R.id.btnCanTurnRing).setOnClickListener {
            log("CAN RING -> set(0x43f01030, 1009)")
            log(ShellProxyBridge.setInstrumentFeature(FID_CAN_TURN_ICON, 1009))
        }
        findViewById<Button>(R.id.btnCanSweep).setOnClickListener {
            log("CAN SWEEP 1-12 ->")
            Thread {
                val r1 = ShellProxyBridge.setInstrumentFeature(FID_CAN_NAVI_STATE, 2)
                runOnUiThread { log("  naviON=$r1") }
                for (icon in 1..12) {
                    val code = 1000 + icon
                    val r = ShellProxyBridge.setInstrumentFeature(FID_CAN_TURN_ICON, code)
                    runOnUiThread { log("  icon=$icon code=$code -> $r") }
                    Thread.sleep(1500)
                }
            }.start()
        }

        // --- Row 3: Orange (SDK methods + HUD feature IDs) ---
        findViewById<Button>(R.id.btnProxySet).setOnClickListener {
            log("SET 0x4c10a018=3 ->")
            log(ShellProxyBridge.setInstrumentFeature(FID_CANDIDATE_HUD, 3))
        }
        findViewById<Button>(R.id.btnProxySetArhud).setOnClickListener {
            log("SET ARHUD IMG 1..5 ->")
            for (v in 1..5) log(ShellProxyBridge.setInstrumentFeature(FID_ARHUD_IMG_TYPE, v))
        }
        findViewById<Button>(R.id.btnProxySetColor).setOnClickListener {
            log("SET ARHUD CLR 1..5 ->")
            for (v in 1..5) log(ShellProxyBridge.setInstrumentFeature(FID_ARHUD_IMG_COLOR, v))
        }
        findViewById<Button>(R.id.btnProxySetMeter).setOnClickListener {
            log("SET METER 1..5 ->")
            for (v in 1..5) log(ShellProxyBridge.setInstrumentFeature(FID_METER_NAVI_TYPE, v))
        }
        findViewById<Button>(R.id.btnProxyGuide).setOnClickListener {
            log("GUIDE LEFT ->")
            log(ShellProxyBridge.sendSimpleGuidanceInfo(TurnKindMap.LEFT, 500))
        }
        findViewById<Button>(R.id.btnProxyGuideRight).setOnClickListener {
            log("GUIDE RIGHT ->")
            log(ShellProxyBridge.sendSimpleGuidanceInfo(TurnKindMap.RIGHT, 500))
        }
        findViewById<Button>(R.id.btnProxyNavi).setOnClickListener {
            log("NAVI STATUS 2 ->")
            log(ShellProxyBridge.sendAutoNaviStatus(2))
        }
        findViewById<Button>(R.id.btnProxyNaviOff).setOnClickListener {
            log("NAVI STATUS 4 ->")
            log(ShellProxyBridge.sendAutoNaviStatus(4))
        }

        // --- Row 4: Cyan (AutoContainerManager) ---
        findViewById<Button>(R.id.btnSendInfo86).setOnClickListener {
            log("SEND_INFO(1000, 86, \"\") HUD MENU 1 ->")
            log(ShellProxyBridge.sendInfo(1000, 86, ""))
        }
        findViewById<Button>(R.id.btnSendInfo87).setOnClickListener {
            log("SEND_INFO(1000, 87, \"\") HUD MENU 2 ->")
            log(ShellProxyBridge.sendInfo(1000, 87, ""))
        }
        findViewById<Button>(R.id.btnSendInfo39).setOnClickListener {
            log("SEND_INFO(1000, 39, \"\") SIMPLE NAVI ->")
            log(ShellProxyBridge.sendInfo(1000, 39, ""))
        }
        findViewById<Button>(R.id.btnSendInfo0).setOnClickListener {
            log("SEND_INFO(1000, 0, \"\") RESTORE VIDEO ->")
            log(ShellProxyBridge.sendInfo(1000, 0, ""))
        }
        findViewById<Button>(R.id.btnSendInfo1).setOnClickListener {
            log("SEND_INFO(1000, 1, \"\") DISCONNECT VIDEO ->")
            log(ShellProxyBridge.sendInfo(1000, 1, ""))
        }
        findViewById<Button>(R.id.btnSendInfoSweep).setOnClickListener {
            log("SEND_INFO SWEEP 0-50 ->")
            Thread {
                for (id in 0..50) {
                    val r = ShellProxyBridge.sendInfo(1000, id, "")
                    runOnUiThread { log("  id=$id -> $r") }
                    Thread.sleep(800)
                }
            }.start()
        }
        findViewById<Button>(R.id.btnCanRaw).setOnClickListener {
            log("CAN RAW TEST -> 0xAA00020F")
            log(ShellProxyBridge.testSet(0xAA00020F.toInt(), byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)))
        }

        // --- Row 4b: Purple (Native AutoContainerNative direct) ---
        findViewById<Button>(R.id.btnNativeInfo86).setOnClickListener {
            log("NATIVE sendInfo(1000,86) HUD1 ->")
            log(ShellProxyBridge.nativeSendInfo(1000, 86, ""))
        }
        findViewById<Button>(R.id.btnNativeInfo87).setOnClickListener {
            log("NATIVE sendInfo(1000,87) HUD2 ->")
            log(ShellProxyBridge.nativeSendInfo(1000, 87, ""))
        }
        findViewById<Button>(R.id.btnNativeInfo39).setOnClickListener {
            log("NATIVE sendInfo(1000,39) NAVI ->")
            log(ShellProxyBridge.nativeSendInfo(1000, 39, ""))
        }
        findViewById<Button>(R.id.btnNativeSweep).setOnClickListener {
            log("NATIVE SWEEP 0-100 ->")
            Thread {
                for (id in 0..100) {
                    val r = ShellProxyBridge.nativeSendInfo(1000, id, "")
                    runOnUiThread { log("  id=$id -> $r") }
                    Thread.sleep(600)
                }
            }.start()
        }
    }

    private fun registerProxyReceiver() {
        val filter = IntentFilter("com.unkwn2.yandexhud.PROXY_CONNECTED")
        proxyReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val p = intent.getParcelableExtra<ProxyBinderParcelable>("proxy_binder")
                if (p != null) {
                    ShellProxyBridge.onProxyReceived(p.binder)
                    log("PROXY CONNECTED! desc=${p.binder.interfaceDescriptor}")
                    statusView.text = "PROXY CONNECTED"
                } else {
                    log("PROXY broadcast received but no binder")
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(proxyReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(proxyReceiver, filter)
        }
        log("proxy receiver registered")
    }

    override fun onDestroy() {
        proxyReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    private fun checkHandshakeFile() {
        try {
            val dir = getExternalFilesDir(null) ?: filesDir
            val f = java.io.File(dir, "proxy_handshake.txt")
            if (f.exists()) {
                log(f.readText().trim())
            } else {
                log("no handshake file — proxy not started yet")
            }
        } catch (t: Throwable) {
            log("handshake ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun showAdbCommand() {
        val cmd = AdbLocalClient.getManualCommand(this)
        log("=== MANUAL (Bugjaeger terminal) ===")
        log("1) pm path com.unkwn2.yandexhud")
        log("2) $cmd")
        log("3) Back here: PROXY PING")
        log("=== OR: AUTO START PROXY ===")
        log("Proxy: ${ShellProxyBridge.isConnected()}")
    }

    private fun exportLog() {
        val text = FileLogger.readAll()
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("HUD log", text))
            Toast.makeText(this, "Log copied (" + text.length + " chars)", Toast.LENGTH_LONG).show()
            log("log copied (" + text.length + " chars)")
        } catch (t: Throwable) {
            log("export failed: " + t.message)
        }
    }

    private fun log(s: String) {
        val time = ts.format(Date())
        val line = time + "  " + s + "\n"
        logView.append(line)
        val scrollAmount = logView.layout?.getLineTop(logView.lineCount) ?: 0
        val viewHeight = logView.height
        if (scrollAmount > viewHeight) logView.scrollTo(0, scrollAmount - viewHeight)
        FileLogger.write("UI", s)
    }
}
