package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("SetTextI18n")
class HudV17Activity : AppCompatActivity() {
    private lateinit var logView: TextView
    private lateinit var scroll: ScrollView
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var proxyBinder: IBinder? = null

    companion object {
        private const val DESC = "com.sr.openbyd.ipc.ICarControl"
        private const val FID_NAV_FUSION = 1276174390 // 0x4C10E036 = carsettings SET

        // CAN HUD Register IDs (from SEAL 06 EV / DiLink 5.0)
        private const val CAN_NAVI_ACTIVE = 1138753594   // 0x43E0003A navi active (2=Active, 4=Standby)
        private const val CAN_HUD_SCREEN = 1276174357   // 0x4C10E015 HUD layout (3=Navi active screen)
        private const val CAN_TURN_ARROW = 1139806224   // 0x43F01010 turn arrow icon ID
        private const val CAN_TURN_DIST = 1139806232    // 0x43F01018 distance to next turn (meters)
        private const val CAN_NEXT_ROAD = 1140461576    // 0x43FA1008 next road name (UTF-16LE bytes)
        private const val CAN_REST_DIST = 1139810344    // 0x43F02028 remaining route distance (meters)
        private const val CAN_REST_HOUR = 1139810320    // 0x43F02010 remaining hours
        private const val CAN_REST_MIN = 1139810328     // 0x43F02018 remaining minutes
        private const val CAN_REST_SEC = 1139810334     // 0x43F0201E remaining seconds

        // ICarControl transaction codes — valid ONLY on proxy binder, NOT on autoservice
        private const val TX_GET_API_VERSION = 1
        private const val TX_PING = 2
        private const val TX_GET_TASK_ID = 15
        private const val TX_GET_TOP_ACTIVITY = 16
        private const val TX_LAUNCH_AND_FORCE = 17
        private const val TX_GET_INSTRUMENT_FEATURE = 18
        private const val TX_SCRAP_BYD_AUTO = 19
        private const val TX_GET_SETTING_FEATURE = 21
        private const val TX_SET_INSTRUMENT_FEATURE = 22
        private const val TX_SET_SETTING_FEATURE = 23
        private const val TX_GET_SYSTEM_PROPERTY = 24
        private const val TX_SEND_AUTO_NAVI_STATUS = 25
        private const val TX_SEND_SIMPLE_GUIDANCE = 26
        private const val TX_SEND_NEXT_PATH_NAME = 27
        private const val TX_SEND_REST_ROUTE_INFO = 28
        private const val TX_SEND_SAFE_GUIDANCE = 29
        private const val TX_SEND_DESTINATION_STATUS = 30
        private const val TX_SEND_CAMERA_GUIDANCE = 31
        private const val TX_SEND_ADDRESS_INFO = 32
    }

    private val proxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            log("PROXY_CONNECTED received!")
            try {
                val extras = intent.extras
                if (extras != null) {
                    for (key in extras.keySet()) {
                        val v = extras.get(key)
                        log("  extra[$key] = $v (${v?.javaClass?.simpleName})")
                        if (v is IBinder) {
                            proxyBinder = v
                            log("  GOT PROXY BINDER from intent extra! desc=${try{v.interfaceDescriptor}catch(_:Throwable){"?"}}")
                        }
                    }
                }
            } catch (t: Throwable) {
                log("  extras scan err: ${t.message}")
            }
            tryPickBinderFromParcelable(intent)
        }
    }

    private fun tryPickBinderFromParcelable(intent: Intent) {
        try {
            val p = intent.getParcelableExtra<android.os.Parcelable>("proxy_binder")
            if (p != null) {
                log("  proxy_binder parcelable: class=${p.javaClass.name}")
                try {
                    val f = p.javaClass.getDeclaredField("binder")
                    f.isAccessible = true
                    val b = f.get(p) as? IBinder
                    if (b != null) {
                        proxyBinder = b
                        log("  GOT BINDER via ProxyBinderParcelable.binder reflection! desc=${try{b.interfaceDescriptor}catch(_:Throwable){"?"}}")
                    }
                } catch (t: Throwable) {
                    log("  binder field reflect err: ${t.message}")
                }
            }
        } catch (t: Throwable) {
            log("  parcelable pick err: ${t.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.init(this)

        try {
            registerReceiver(proxyReceiver, IntentFilter("com.sr.openbyd.PROXY_CONNECTED"), Context.RECEIVER_NOT_EXPORTED)
            log("Registered PROXY_CONNECTED receiver")
        } catch (t: Throwable) {
            log("Register receiver err: ${t.message}")
        }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(6, 6, 6, 6) }
        val status = TextView(this).apply { text = "v17 — ICarControl proxy + Setting FIDs"; textSize = 14f }
        root.addView(status)
        logView = TextView(this).apply { textSize = 9f; setTextColor(0xFF00FF00.toInt()) }
        scroll = ScrollView(this).apply { addView(logView) }
        val scrollLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(scroll, scrollLp)

        fun btn(label: String, fn: () -> Unit): Button {
            val b = Button(this).apply { text = label; textSize = 10f; setOnClickListener { fn() } }
            root.addView(b)
            return b
        }

        btn("== C-BROADCAST (cluster OK) ==") {}
        btn("1. Activate HUD navi=2") { cbSend(2, 0) }
        btn("2. Arrow+Dist (3,500)") { cbSend(3, 500) }
        btn("3. Full CB seq 9s") { cbFullSequence() }
        btn("4. Close HUD navi=4") { cbSend(4, 0) }

        btn("== PROXY BINDER PATH ==") {}
        btn("5. Start proxy process") { startProxy() }
        btn("6. Ping proxy (tx2)") { proxyPing() }
        btn("7. Read NavFusion (tx21)") { proxyReadNavFusion() }
        btn("8. Enable NavFusion (tx23)") { proxyEnableNavFusion() }
        btn("9. Full HUD seq (tx25-28)") { proxyFullSequence() }

        btn("== AUTOSERVICE SCAN ==") {}
        btn("A. Scan autoservice tx1-40") { scanAutoservice() }
        btn("B. Try autoservice setSetting") { autoserviceTrySetSetting() }

        btn("== UTILS ==") {}
        btn("C. Check proxy status") { checkProxy() }
        btn("D. Grant notif listener") { grantNotif() }

        btn("== GPS SPOOF (Amap China test) ==") {}
        btn("E. Set GPS Beijing (once)") { gpsSetBeijing() }
        btn("F. GPS Route Beijing (moving)") { gpsStartRoute() }
        btn("G. Stop GPS route") { gpsStopRoute() }
        btn("H. Check GPS status") { gpsCheck() }
        btn("I. Grant mock location") { gpsEnableMock() }

        btn("== CAN DIRECT (BYDAutoTestDevice) ==") {}
        btn("J. CAN: Activate navi") { canActivate() }
        btn("K. CAN: Arrow+Dist") { canArrowDist() }
        btn("L. CAN: Road name") { canRoadName() }
        btn("L2. CAN: Rest route") { canRestRoute() }
        btn("M. CAN: Full HUD seq") { canFullSequence() }
        btn("N. CAN: Read navi status") { canReadNaviStatus() }
        btn("N2. CAN: Close navi") { canClose() }

        btn("EXPORT LOG") { exportLog() }
        btn("CLEAR LOG") { clearLog() }

        setContentView(root)
        log("v17 ready — uid=${android.os.Process.myUid()}")
        log("CRITICAL: NavFusion FID=0x4C10E036 is SETTING, use tx21(read)/tx23(write)")
        log("ICarControl TX only valid on proxy binder, NOT on autoservice")
    }

    override fun onDestroy() {
        try { unregisterReceiver(proxyReceiver) } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun log(s: String) {
        handler.post {
            val t = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
            logView.append("$t  $s\n")
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
        FileLogger.write("v17", s)
    }

    private fun runCmd(vararg args: String): Pair<Int, String> = try {
        val proc = Runtime.getRuntime().exec(args)
        proc.outputStream.close()
        val out = proc.inputStream.bufferedReader().readText().trim()
        val err = proc.errorStream.bufferedReader().readText().trim()
        val exit = proc.waitFor()
        exit to if (out.isNotEmpty()) out else err
    } catch (t: Throwable) { -1 to "ERR: ${t.message}" }

    // === ICarControl raw binder transact ===

    private fun iccTransact(tx: Int, writeArgs: (Parcel) -> Unit): String? {
        val binder = proxyBinder
        if (binder == null) {
            log("  NO PROXY BINDER — start proxy first (btn 5)")
            return null
        }
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESC)
            writeArgs(data)
            val ok = binder.transact(tx, data, reply, 0)
            if (!ok) {
                log("  transact($tx) returned false")
                return null
            }
            reply.readException()
            reply.readString()
        } catch (t: Throwable) {
            log("  transact($tx) err: ${t.message}")
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private fun iccTransactInt(tx: Int, writeArgs: (Parcel) -> Unit): Int? {
        val binder = proxyBinder
        if (binder == null) {
            log("  NO PROXY BINDER — start proxy first (btn 5)")
            return null
        }
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESC)
            writeArgs(data)
            val ok = binder.transact(tx, data, reply, 0)
            if (!ok) {
                log("  transact($tx) returned false")
                return null
            }
            reply.readException()
            reply.readInt()
        } catch (t: Throwable) {
            log("  transact($tx) err: ${t.message}")
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    // === C-Broadcast methods ===

    private fun cbSend(type: Int, value: Int) {
        log("CB: type=$type val=$value")
        val i = Intent("action.com.byd.protocol.AUTONAVI_STANDARD_BROADCAST_SEND")
        i.setPackage("com.byd.amapservice")
        i.putExtra("setKey", "KEY_TYPE")
        i.putExtra("setType", type)
        i.putExtra("setValue", value)
        sendBroadcast(i)
    }

    private fun cbFullSequence() {
        log("=== CB FULL SEQ ===")
        cbSend(2, 0)
        handler.postDelayed({ cbSend(3, 500) }, 500)
        handler.postDelayed({
            val i = Intent("action.com.byd.protocol.AUTONAVI_STANDARD_BROADCAST_SEND")
            i.setPackage("com.byd.amapservice")
            i.putExtra("setKey", "KEY_TYPE")
            i.putExtra("setType", 100)
            i.putExtra("setStringValue", "Moskovskaya ul.")
            sendBroadcast(i)
            log("CB: road='Moskovskaya ul.'")
        }, 1000)
        handler.postDelayed({ cbSend(7, 200) }, 3000)
        handler.postDelayed({ cbSend(1, 100) }, 6000)
        handler.postDelayed({ cbSend(4, 0) }, 9000)
    }

    // === Proxy binder path ===

    private fun startProxy() {
        log("Starting openbyd_proxy (uid 2000)...")
        Thread {
            val (e1, _) = runCmd("pkill", "-9", "-f", "openbyd_proxy")
            log("  kill existing: exit=$e1")
            try { Thread.sleep(500) } catch (_: InterruptedException) {}

            val cmd = "nohup app_process " +
                "-Djava.class.path=/system/framework/services.jar:/system/framework/dilink-services.jar:/data/app/~~*/com.sr.openbyd-*/base.apk " +
                "-Djava.library.path=/system/lib64:/product/lib64 " +
                "/system/bin " +
                "--nice-name=openbyd_proxy " +
                "com.sr.openbyd.proxy.EntryPoint " +
                "--uid=2000 " +
                "> /dev/null 2>&1 &"

            val (e2, o2) = runCmd("sh", "-c", cmd)
            log("  start proxy: exit=$e2")
            try { Thread.sleep(3000) } catch (_: InterruptedException) {}

            val (e3, o3) = runCmd("pidof", "openbyd_proxy")
            log("  pidof: exit=$e3 pid='$o3'")

            if (proxyBinder != null) {
                log("  BINDER already acquired!")
            } else {
                log("  Binder NOT yet received — broadcast goes to com.sr.openbyd only")
                log("  Will try to acquire from BYDOpen via reflection...")
                tryAcquireFromBydOpen()
            }
        }.start()
    }

    private fun tryAcquireFromBydOpen() {
        try {
            val pkg = packageManager.getPackageInfo("com.sr.openbyd", 0)
            log("  BYDOpen installed: uid=${pkg.applicationInfo?.uid}")
        } catch (t: Throwable) {
            log("  BYDOpen NOT installed: ${t.message}")
            return
        }

        try {
            val ctx = createPackageContext("com.sr.openbyd", Context.CONTEXT_IGNORE_SECURITY)
            val cl = Class.forName("com.sr.openbyd.proxy.ProxyManager", true, ctx.classLoader)
            val instanceField = cl.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            val instance = instanceField.get(null)
            val ccField = cl.getDeclaredField("carControl")
            ccField.isAccessible = true
            val cc = ccField.get(instance)
            if (cc != null) {
                val asBinder = cc.javaClass.getMethod("asBinder").invoke(cc) as? IBinder
                if (asBinder != null) {
                    proxyBinder = asBinder
                    log("  GOT BINDER from ProxyManager.carControl via reflection!")
                    log("  descriptor=${try{asBinder.interfaceDescriptor}catch(_:Throwable){"?"}}")
                    return
                }
            }
            log("  ProxyManager.carControl = null (proxy not yet connected to BYDOpen)")
        } catch (t: Throwable) {
            log("  ProxyManager reflect err: ${t.message}")
        }

        try {
            log("  Trying to send PROXY_CONNECTED broadcast to BYDOpen to trigger connection...")
            val i = Intent("com.sr.openbyd.PROXY_CONNECTED")
            i.setPackage("com.sr.openbyd")
            sendBroadcast(i)
            log("  Broadcast sent (no binder extra — won't give BYDOpen the actual binder)")
        } catch (t: Throwable) {
            log("  Broadcast err: ${t.message}")
        }
    }

    private fun proxyPing() {
        log("Proxy: ping (tx=2)")
        val result = iccTransact(TX_PING) {}
        log("  ping => '$result'")

        log("Proxy: getApiVersion (tx=1)")
        val ver = iccTransactInt(TX_GET_API_VERSION) {}
        log("  apiVersion => $ver")
    }

    private fun proxyReadNavFusion() {
        log("Proxy: getSettingFeatureValue(0x4C10E036) tx=21")
        val result = iccTransact(TX_GET_SETTING_FEATURE) { data ->
            data.writeInt(FID_NAV_FUSION)
        }
        log("  NavFusion state => '$result' (expect '1'=ON, '2'=OFF)")
    }

    private fun proxyEnableNavFusion() {
        log("Proxy: setSettingFeatureValue(0x4C10E036, 1) tx=23")
        val result = iccTransact(TX_SET_SETTING_FEATURE) { data ->
            data.writeInt(FID_NAV_FUSION)
            data.writeInt(1)
        }
        log("  set NavFusion=ON => '$result'")

        handler.postDelayed({
            log("Verifying NavFusion after set...")
            proxyReadNavFusion()
        }, 1000)
    }

    private fun proxyFullSequence() {
        log("=== PROXY FULL HUD SEQ ===")
        Thread {
            log("[1/5] sendAutoNaviStatus(2) tx=25")
            val r1 = iccTransact(TX_SEND_AUTO_NAVI_STATUS) { it.writeInt(2) }
            log("  => '$r1'")
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[2/5] sendSimpleGuidanceInfo(3, 500) tx=26")
            val r2 = iccTransact(TX_SEND_SIMPLE_GUIDANCE) { d -> d.writeInt(3); d.writeInt(500) }
            log("  => '$r2'")
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[3/5] sendNextPathName('Test Road') tx=27")
            val r3 = iccTransact(TX_SEND_NEXT_PATH_NAME) { it.writeString("Test Road") }
            log("  => '$r3'")
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[4/5] sendRestRouteInfo(0, 15, 5000) tx=28")
            val r4 = iccTransact(TX_SEND_REST_ROUTE_INFO) { d -> d.writeInt(0); d.writeInt(15); d.writeLong(5000) }
            log("  => '$r4'")
            try { Thread.sleep(5000) } catch (_: InterruptedException) {}

            log("[5/5] sendAutoNaviStatus(4) tx=25 (close)")
            val r5 = iccTransact(TX_SEND_AUTO_NAVI_STATUS) { it.writeInt(4) }
            log("  => '$r5'")
            log("=== PROXY SEQ DONE ===")
        }.start()
    }

    // === Autoservice scan (BYDAutoServer, NOT ICarControl) ===

    private fun scanAutoservice() {
        log("=== SCAN autoservice tx1-40 (BYDAutoServer) ===")
        Thread {
            for (tx in 1..40) {
                try {
                    val cmd = arrayOf("service", "call", "autoservice", tx.toString(), "i32", "0")
                    val proc = Runtime.getRuntime().exec(cmd)
                    proc.outputStream.close()
                    val out = proc.inputStream.bufferedReader().readText().trim()
                    val exit = proc.waitFor()
                    if (out.isNotEmpty() && !out.contains("Bad transaction") && !out.contains("Transaction too large")) {
                        log("  tx=$tx exit=$exit => '$out'")
                    }
                } catch (_: Throwable) {}
            }
            log("SCAN DONE — these are BYDAutoServer TX codes, different from ICarControl")
        }.start()
    }

    private fun autoserviceTrySetSetting() {
        log("=== Try autoservice setSetting for NavFusion ===")
        Thread {
            for (tx in 1..30) {
                val formats = listOf(
                    arrayOf("service", "call", "autoservice", tx.toString(), "i32", FID_NAV_FUSION.toString(), "i32", "1"),
                    arrayOf("service", "call", "autoservice", tx.toString(), "i32", "1023", "i32", FID_NAV_FUSION.toString(), "i32", "1"),
                    arrayOf("service", "call", "autoservice", tx.toString(), "i32", "0", "i32", FID_NAV_FUSION.toString(), "i32", "1")
                )
                for ((fi, cmd) in formats.withIndex()) {
                    try {
                        val proc = Runtime.getRuntime().exec(cmd)
                        proc.outputStream.close()
                        val out = proc.inputStream.bufferedReader().readText().trim()
                        val exit = proc.waitFor()
                        if (out.isNotEmpty() && !out.contains("Bad transaction") && !out.contains("Transaction too large") && !out.startsWith("Result: Bundle")) {
                            log("  tx=$tx fmt=$fi exit=$exit => '$out'")
                        }
                    } catch (_: Throwable) {}
                }
            }
            log("AUTOSERVICE SETTING SCAN DONE")
        }.start()
    }

    // === Utils ===

    private fun checkProxy() {
        log("Checking proxy status...")
        Thread {
            val (e, o) = runCmd("pidof", "openbyd_proxy")
            log("  pidof: exit=$e '$o'")
            try {
                val pi = packageManager.getPackageInfo("com.sr.openbyd", 0)
                log("  BYDOpen pkg: uid=${pi.applicationInfo?.uid}")
            } catch (t: Throwable) {
                log("  BYDOpen NOT found: ${t.message}")
            }
            log("  proxyBinder = ${if (proxyBinder != null) "ACQUIRED" else "NULL"}")
            if (proxyBinder != null) {
                try {
                    log("  descriptor = ${proxyBinder!!.interfaceDescriptor}")
                    log("  isBinderAlive = ${proxyBinder!!.isBinderAlive}")
                    log("  pingBinder = ${proxyBinder!!.pingBinder()}")
                } catch (t: Throwable) {
                    log("  binder probe err: ${t.message}")
                }
            }
        }.start()
    }

    private fun grantNotif() {
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
        val cn = ComponentName(this, HudNavListenerService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(cn.flattenToString()) == true
        log("  NotificationListener enabled=$enabled")
        if (!enabled) {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Throwable) {}
        }
    }

    private fun exportLog() {
        try {
            val src = FileLogger.getFile() ?: return log("No log file")
            val dir = android.os.Environment.getExternalStorageDirectory()
            val usbDir = java.io.File(dir, "Download")
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
            val dst = java.io.File(usbDir, "yandex_hud_v17_${ts}.log")
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

    // === GPS Spoof for Amap China test ===

    private val beijingRoute = arrayOf(
        doubleArrayOf(39.9042, 116.4074),
        doubleArrayOf(39.9142, 116.4074),
        doubleArrayOf(39.9242, 116.4074),
        doubleArrayOf(39.9242, 116.4174),
        doubleArrayOf(39.9142, 116.4174),
        doubleArrayOf(39.9042, 116.4174),
    )
    private var gpsRouteIdx = 0
    private var gpsRunning = false
    private var gpsThread: Thread? = null

    private fun gpsSetBeijing() {
        log("GPS: Setting to Beijing (39.9042, 116.4074)")
        gpsInjectLocation(39.9042, 116.4074)
    }

    private fun gpsStartRoute() {
        if (gpsRunning) { log("GPS: Route already running"); return }
        gpsRunning = true
        gpsRouteIdx = 0
        log("GPS: Starting Beijing route (${beijingRoute.size} points, 2s interval)")
        gpsThread = Thread {
            while (gpsRunning) {
                val p = beijingRoute[gpsRouteIdx]
                gpsInjectLocation(p[0], p[1])
                gpsRouteIdx = (gpsRouteIdx + 1) % beijingRoute.size
                try { Thread.sleep(2000) } catch (_: InterruptedException) { break }
            }
        }.apply { isDaemon = true; start() }
    }

    private fun gpsStopRoute() {
        gpsRunning = false
        gpsThread?.interrupt()
        gpsThread = null
        log("GPS: Route stopped")
    }

    private fun gpsCheck() {
        log("GPS: Checking current location...")
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            for (p in lm.getProviders(true)) {
                val loc = lm.getLastKnownLocation(p)
                if (loc != null) {
                    log("  $p: lat=${loc.latitude} lon=${loc.longitude} acc=${loc.accuracy} time=${loc.time}")
                } else {
                    log("  $p: no last known location")
                }
            }
            val mockEnabled = Settings.Secure.getString(contentResolver, "mock_location")
            log("  mock_location setting=$mockEnabled")
        } catch (t: Throwable) {
            log("  GPS check ERR: ${t.message}")
        }
    }

    private fun gpsEnableMock() {
        log("GPS: Granting mock location permission (bypass UI)...")
        val pkg = packageName
        Thread {
            // Method 1: appops set — THE KEY BYPASS (shell uid 2000 has this privilege)
            try {
                val (e1, o1) = runCmd("appops", "set", pkg, "android:mock_location", "allow")
                log("  appops set mock_location allow: exit=$e1 '$o1'")
            } catch (t: Throwable) {
                log("  appops ERR: ${t.message}")
            }

            // Method 2: settings put secure mock_location_app (bypasses UI picker)
            try {
                val (e2, o2) = runCmd("settings", "put", "secure", "mock_location_app", pkg)
                log("  settings put mock_location_app=$pkg: exit=$e2 '$o2'")
            } catch (t: Throwable) {
                log("  settings put ERR: ${t.message}")
            }

            // Method 3: old toggle
            try {
                val (e3, o3) = runCmd("settings", "put", "secure", "mock_location", "1")
                log("  settings put mock_location=1: exit=$e3 '$o3'")
            } catch (t: Throwable) {
                log("  settings put mock_location ERR: ${t.message}")
            }

            // Verify
            try {
                val (e4, o4) = runCmd("appops", "get", pkg, "android:mock_location")
                log("  VERIFY appops: exit=$e4 '$o4'")
            } catch (_: Throwable) {}
            try {
                val (e5, o5) = runCmd("settings", "get", "secure", "mock_location_app")
                log("  VERIFY mock_location_app: exit=$e5 '$o5'")
            } catch (_: Throwable) {}

            // Method 4: cmd location test provider from shell
            try {
                val (e6, o6) = runCmd("cmd", "location", "add-test-provider", "gps")
                log("  cmd location add-test-provider: exit=$e6 '$o6'")
            } catch (t: Throwable) {
                log("  cmd location ERR: ${t.message}")
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun gpsInjectLocation(lat: Double, lon: Double) {
        var injected = false

        // Method 1: cmd location set-test-provider-location (shell-level, no app permission needed)
        try {
            val latStr = String.format("%.6f", lat)
            val lonStr = String.format("%.6f", lon)
            val (e0, o0) = runCmd("cmd", "location", "set-test-provider-location", "gps",
                "--location", "$latStr,$lonStr")
            if (e0 == 0 && !o0.contains("Error") && !o0.contains("Unknown")) {
                log("  GPS inject OK via cmd location: $lat, $lon")
                injected = true
            } else {
                log("  cmd location: exit=$e0 '$o0'")
            }
        } catch (t: Throwable) {
            log("  cmd location ERR: ${t.message}")
        }

        // Method 2: Android LocationManager TestProvider (needs mock_location permission)
        if (!injected) {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                try { lm.removeTestProvider(LocationManager.GPS_PROVIDER) } catch (_: Throwable) {}
                try {
                    lm.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false,
                        true, true, true, 0, 5)
                    lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
                    val loc = Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = lat
                        longitude = lon
                        altitude = 50.0
                        bearing = 0f
                        speed = 0f
                        accuracy = 5f
                        time = System.currentTimeMillis()
                        elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                    }
                    lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc)
                    log("  GPS inject OK via TestProvider: $lat, $lon")
                    injected = true
                } catch (t: Throwable) {
                    log("  TestProvider ERR: ${t.javaClass.simpleName}: ${t.message?.take(80)}")
                }
            } catch (t: Throwable) {
                log("  LocationManager ERR: ${t.message}")
            }
        }

        // Method 3: BYDAutoLocationDevice reflection (likely SecurityException but worth trying)
        if (!injected) {
            try {
                val cls = Class.forName("android.hardware.bydauto.location.BYDAutoLocationDevice")
                val getInstance = cls.getDeclaredMethod("getInstance", Context::class.java)
                getInstance.isAccessible = true
                val dev = getInstance.invoke(null, this@HudV17Activity)
                if (dev != null) {
                    val method = cls.getDeclaredMethod("setLocationInfo",
                        Int::class.javaPrimitiveType, Double::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType, Double::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType, Float::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType)
                    method.isAccessible = true
                    val result = method.invoke(dev, 2, lat, 1, lon, 50, 0f, 0.0, 1, 12)
                    log("  BYDAutoLocationDevice.setLocationInfo($lat,$lon)=$result")
                    injected = true
                } else {
                    log("  BYDAutoLocationDevice.getInstance=null")
                }
            } catch (t: Throwable) {
                val c2 = t.cause ?: t
                log("  BYD Location ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(60)}")
            }
        }

        if (!injected) {
            log("  ALL GPS inject methods failed — press button I first to grant permission")
        }
    }

    // === CAN Direct via BYDAutoTestDevice.set() ===

    @SuppressLint("PrivateApi")
    private fun getTestDevice(): Any? {
        return try {
            val cls = Class.forName("android.hardware.bydauto.test.BYDAutoTestDevice")
            val gi = cls.getDeclaredMethod("getInstance", Context::class.java)
            gi.isAccessible = true
            gi.invoke(null, this@HudV17Activity)
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  TestDevice ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            null
        }
    }

    @SuppressLint("PrivateApi")
    private fun canSet(fid: Int, value: Int): Boolean {
        try {
            val dev = getTestDevice() ?: run { log("  TestDevice null"); return false }
            val cls = dev.javaClass
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getDeclaredMethod("set",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            setMethod.isAccessible = true
            val result = setMethod.invoke(dev, fid, 0, value)
            log("  set(0x${Integer.toHexString(fid)}, $value) = $result")
            return result != null && result != 0
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  canSet ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return false
        }
    }

    @SuppressLint("PrivateApi")
    private fun canSetBytes(fid: Int, bytes: ByteArray): Boolean {
        try {
            val dev = getTestDevice() ?: run { log("  TestDevice null"); return false }
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getDeclaredMethod("set",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, ByteArray::class.java)
            setMethod.isAccessible = true
            val result = setMethod.invoke(dev, fid, 0, bytes)
            log("  setBytes(0x${Integer.toHexString(fid)}, ${bytes.size}B) = $result")
            return result != null && result != 0
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  canSetBytes ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return false
        }
    }

    @SuppressLint("PrivateApi")
    private fun canGet(fid: Int): Int {
        try {
            val dev = getTestDevice() ?: run { log("  TestDevice null"); return -1 }
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val getMethod = absCls.getDeclaredMethod("get",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            getMethod.isAccessible = true
            val result = getMethod.invoke(dev, fid, 0) as? Int ?: -1
            log("  get(0x${Integer.toHexString(fid)}) = $result")
            return result
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  canGet ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return -1
        }
    }

    private fun canActivate() {
        log("CAN: Activate navigation (HUD screen + navi status)")
        canSet(CAN_HUD_SCREEN, 3)
        canSet(CAN_NAVI_ACTIVE, 2)
    }

    private fun canArrowDist() {
        log("CAN: Turn arrow=3 (RIGHT) + distance=500m")
        canSet(CAN_TURN_ARROW, 3)
        canSet(CAN_TURN_DIST, 500)
    }

    private fun canRoadName() {
        log("CAN: Next road name = 'Moskovskaya ul.'")
        val name = "Moskovskaya ul."
        val bytes = name.toByteArray(Charsets.UTF_16LE)
        canSetBytes(CAN_NEXT_ROAD, bytes)
    }

    private fun canRestRoute() {
        log("CAN: Rest route = 0h 15m 0s 50000m")
        canSet(CAN_REST_HOUR, 0)
        canSet(CAN_REST_MIN, 15)
        canSet(CAN_REST_SEC, 0)
        canSet(CAN_REST_DIST, 50000)
    }

    private fun canFullSequence() {
        log("=== CAN FULL HUD SEQ ===")
        Thread {
            log("[1/5] Activate navi + HUD screen")
            canSet(CAN_HUD_SCREEN, 3)
            canSet(CAN_NAVI_ACTIVE, 2)
            try { Thread.sleep(500) } catch (_: InterruptedException) {}

            log("[2/5] Turn arrow RIGHT + 500m")
            canSet(CAN_TURN_ARROW, 3)
            canSet(CAN_TURN_DIST, 500)
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[3/5] Road name 'Moskovskaya ul.'")
            val bytes = "Moskovskaya ul.".toByteArray(Charsets.UTF_16LE)
            canSetBytes(CAN_NEXT_ROAD, bytes)
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[4/5] Rest route 0h 15m 50km")
            canSet(CAN_REST_HOUR, 0)
            canSet(CAN_REST_MIN, 15)
            canSet(CAN_REST_SEC, 0)
            canSet(CAN_REST_DIST, 50000)
            try { Thread.sleep(5000) } catch (_: InterruptedException) {}

            log("[5/5] Change arrow to LEFT + 200m")
            canSet(CAN_TURN_ARROW, 7)
            canSet(CAN_TURN_DIST, 200)
            try { Thread.sleep(5000) } catch (_: InterruptedException) {}

            log("[6/6] Close navi")
            canSet(CAN_NAVI_ACTIVE, 4)
            canSet(CAN_HUD_SCREEN, 0)
            log("=== CAN SEQ DONE ===")
        }.start()
    }

    private fun canReadNaviStatus() {
        log("CAN: Reading navi status registers...")
        val r1 = canGet(CAN_NAVI_ACTIVE)
        log("  NAVI_ACTIVE (0x43E0003A) = $r1 (expect 2=Active, 4=Standby)")
        val r2 = canGet(CAN_HUD_SCREEN)
        log("  HUD_SCREEN (0x4C10E015) = $r2 (expect 3=Navi screen)")
        val r3 = canGet(CAN_TURN_ARROW)
        log("  TURN_ARROW (0x43F01010) = $r3")
        val r4 = canGet(CAN_TURN_DIST)
        log("  TURN_DIST (0x43F01018) = $r4")
    }

    private fun canClose() {
        log("CAN: Close navigation")
        canSet(CAN_NAVI_ACTIVE, 4)
        canSet(CAN_HUD_SCREEN, 0)
    }
}
