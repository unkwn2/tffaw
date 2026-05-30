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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("SetTextI18n")
class HudV18Activity : AppCompatActivity() {
    private lateinit var logView: TextView
    private lateinit var scroll: ScrollView
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var proxyBinder: IBinder? = null
    private var instrumentDev: Any? = null
    private var settingDev: Any? = null
    private var testDev: Any? = null
    private var proxyServiceConn: Any? = null
    private var savedLayoutBeforeTest: Int = 1

    companion object {
        private const val DESC = "com.sr.openbyd.ipc.ICarControl"

        private const val TX_GET_API_VERSION = 1
        private const val TX_PING = 2
        private const val TX_GET_TASK_ID = 15
        private const val TX_GET_TOP_ACTIVITY = 16
        private const val TX_GET_INSTRUMENT_FEATURE = 18
        private const val TX_GET_SETTING_FEATURE = 21
        private const val TX_SET_INSTRUMENT_FEATURE = 22
        private const val TX_SET_SETTING_FEATURE = 23
        private const val TX_SEND_AUTO_NAVI_STATUS = 25
        private const val TX_SEND_SIMPLE_GUIDANCE = 26
        private const val TX_SEND_NEXT_PATH_NAME = 27
        private const val TX_SEND_REST_ROUTE_INFO = 28
    }

    @SuppressLint("PrivateApi")
    private object FIDs {
        // v18.2: hardcoded AmapService-confirmed values as PRIMARY defaults
        private val HARDCODED = mapOf(
            "INSTRUMENT_SEND_NAVI_STATUS_SET" to 0x43E0003A,
            "SET_NAVI_SCREEN_STATUS_SET" to 0x4C10E015,
            "NAVIGATION_FUSION_SWITCH_SET" to 0x4C10E036,
            "INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET" to 0x43FA1008,
            "INSTRUMENT_FRONT_CROSSING_DISTANCE_SET" to 0x43F01018,
            "INSTRUMENT_GUIDE_INFO_SIMPLE_SET" to 0x43F01010,
            "INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET" to 0x43F01030,
            "INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET" to 0x43F08030,
            "INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET" to 0x43F02010,
            "INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET" to 0x43F02018,
            "INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET" to 0x43F0201E,
            "INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET" to 0x43F02028,
            "INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET" to 0x43F02024,
            "INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET" to 0x43F09018,
            "INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET" to 0x43F09020,
            "INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET" to 0x43F09028,
            "INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET" to 0x43F08018,
            "INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET" to 0x43F08010,
            "SETTING_MPC_IFC_NAVI_ST_SET" to 0x4CA0004B,
            "ATOM_HUD_SWITCH" to 0x0780E026,
            "CENTER_PROJECTION_SWITCH" to 0x40C0C010,
            "CENTER_PROJECTION2_SWITCH" to 0x40C0C022,
            "FALLBACK_ILLUSTRATION_SET" to 0x40C0C019,
            "EASY_NAVI_GUIDE_SET" to 0x1F701010,
            "EASY_ENTRY_ACTION_SET" to 0x1F704010,
            "DIST_TARGET_HEAD_SET" to 0x1F701018,
            "NAVI_TYPE_SET" to 0x4C10A018,
            "SETTING_NAVI_CORP_SET" to 0x4CA00048,
            "SPEED_LIMIT_SET" to 0x4CA00040
        )

        fun get(name: String): Int = fieldMap[name] ?: HARDCODED[name] ?: 0

        var INSTRUMENT_SEND_NAVI_STATUS_SET = HARDCODED["INSTRUMENT_SEND_NAVI_STATUS_SET"]!!
        var SET_NAVI_SCREEN_STATUS_SET = HARDCODED["SET_NAVI_SCREEN_STATUS_SET"]!!
        var NAVIGATION_FUSION_SWITCH_SET = HARDCODED["NAVIGATION_FUSION_SWITCH_SET"]!!
        var INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET = HARDCODED["INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET"]!!
        var INSTRUMENT_FRONT_CROSSING_DISTANCE_SET = HARDCODED["INSTRUMENT_FRONT_CROSSING_DISTANCE_SET"]!!
        var INSTRUMENT_GUIDE_INFO_SIMPLE_SET = HARDCODED["INSTRUMENT_GUIDE_INFO_SIMPLE_SET"]!!
        var INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET = HARDCODED["INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET"]!!
        var INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET = HARDCODED["INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET"]!!
        var INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET = HARDCODED["INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET"]!!
        var INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET = HARDCODED["INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET"]!!
        var INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET = HARDCODED["INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET"]!!
        var INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET = HARDCODED["INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET"]!!
        var INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET = HARDCODED["INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET"]!!
        var INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET = HARDCODED["INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET"]!!
        var INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET = HARDCODED["INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET"]!!
        var INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET = HARDCODED["INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET"]!!
        var INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET = HARDCODED["INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET"]!!
        var INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET = HARDCODED["INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET"]!!
        var SETTING_MPC_IFC_NAVI_ST_SET = HARDCODED["SETTING_MPC_IFC_NAVI_ST_SET"]!!
        var loaded = false

        private val fieldMap = mutableMapOf<String, Int>()

        fun loadFromFramework(logFn: (String) -> Unit) {
            try {
                val cls = Class.forName("android.hardware.bydauto.BYDAutoFeatureIds")
                val resolve: (String) -> Int = { name ->
                    try {
                        val f = cls.getDeclaredField(name)
                        f.isAccessible = true
                        f.getInt(null)
                    } catch (_: Throwable) { 0 }
                }
                val resolveInst: (String) -> Int = { name ->
                    try {
                        val icls = Class.forName("android.hardware.bydauto.BYDAutoFeatureIds\$Instrument")
                        val f = icls.getDeclaredField(name)
                        f.isAccessible = true
                        f.getInt(null)
                    } catch (_: Throwable) { 0 }
                }

                fun apply(name: String, resolved: Int, resolveInstFn: ((String) -> Int)? = null) {
                    val effective = if (resolved != 0) resolved
                        else if (resolveInstFn != null) resolveInstFn("INSTRUMENT_${name}").let { if (it != 0) it else HARDCODED[name] ?: 0 }
                        else HARDCODED[name] ?: 0
                    fieldMap[name] = effective
                    when (name) {
                        "INSTRUMENT_SEND_NAVI_STATUS_SET" -> INSTRUMENT_SEND_NAVI_STATUS_SET = effective
                        "SET_NAVI_SCREEN_STATUS_SET" -> SET_NAVI_SCREEN_STATUS_SET = effective
                        "NAVIGATION_FUSION_SWITCH_SET" -> NAVIGATION_FUSION_SWITCH_SET = effective
                        "INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET" -> INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET = effective
                        "INSTRUMENT_FRONT_CROSSING_DISTANCE_SET" -> INSTRUMENT_FRONT_CROSSING_DISTANCE_SET = effective
                        "INSTRUMENT_GUIDE_INFO_SIMPLE_SET" -> INSTRUMENT_GUIDE_INFO_SIMPLE_SET = effective
                        "INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET" -> INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET = effective
                        "INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET" -> INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET = effective
                        "INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET" -> INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET = effective
                        "INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET" -> INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET = effective
                        "INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET" -> INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET = effective
                        "INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET" -> INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET = effective
                        "INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET" -> INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET = effective
                        "INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET" -> INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET = effective
                        "INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET" -> INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET = effective
                        "INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET" -> INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET = effective
                        "INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET" -> INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET = effective
                        "INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET" -> INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET = effective
                        "SETTING_MPC_IFC_NAVI_ST_SET" -> SETTING_MPC_IFC_NAVI_ST_SET = effective
                    }
                }

                apply("INSTRUMENT_SEND_NAVI_STATUS_SET", resolve("INSTRUMENT_SEND_NAVI_STATUS_SET"))
                apply("SET_NAVI_SCREEN_STATUS_SET", resolve("SET_NAVI_SCREEN_STATUS_SET"))
                apply("NAVIGATION_FUSION_SWITCH_SET",
                    resolve("NAVIGATION_FUSION_SWITCH_SET").let { if (it != 0) it else resolve("SETTING_NAVI_FUSION_SET") })
                apply("INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET", resolve("INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET"))
                apply("INSTRUMENT_FRONT_CROSSING_DISTANCE_SET", resolve("INSTRUMENT_FRONT_CROSSING_DISTANCE_SET"))
                apply("INSTRUMENT_GUIDE_INFO_SIMPLE_SET", resolve("INSTRUMENT_GUIDE_INFO_SIMPLE_SET"))
                apply("INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET", resolveInst("INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET"))
                apply("INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET", resolveInst("INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET"))
                apply("INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET", resolve("INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET"))
                apply("INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET", resolve("INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET"))
                apply("INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET", resolve("INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET"))
                apply("INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET", resolve("INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET"))
                apply("INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET", resolve("INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET"))
                apply("INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET", resolve("INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET"))
                apply("INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET", resolve("INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET"))
                apply("INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET", resolve("INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET"))
                apply("INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET", resolve("INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET"))
                apply("INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET", resolve("INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET"))
                apply("SETTING_MPC_IFC_NAVI_ST_SET", resolve("SETTING_MPC_IFC_NAVI_ST_SET"))
                loaded = true
                logFn("FIDs loaded (hardcoded defaults, framework overrides)")
            } catch (t: Throwable) {
                logFn("FIDs load ERR: ${t.message} — using hardcoded")
            }
        }

        fun fields(): List<Pair<String, Int>> = listOf(
            "INSTRUMENT_SEND_NAVI_STATUS_SET" to INSTRUMENT_SEND_NAVI_STATUS_SET,
            "SET_NAVI_SCREEN_STATUS_SET" to SET_NAVI_SCREEN_STATUS_SET,
            "NAVIGATION_FUSION_SWITCH_SET" to NAVIGATION_FUSION_SWITCH_SET,
            "INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET" to INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET,
            "INSTRUMENT_FRONT_CROSSING_DISTANCE_SET" to INSTRUMENT_FRONT_CROSSING_DISTANCE_SET,
            "INSTRUMENT_GUIDE_INFO_SIMPLE_SET" to INSTRUMENT_GUIDE_INFO_SIMPLE_SET,
            "INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET" to INSTRUMENT_GUIDE_INFO_AND_ROAD_AHEAD_DISTANCE_SET,
            "INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET" to INSTRUMENT_GUIDE_INFO_ADVANCED_ACTION_SET,
            "INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET" to INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET,
            "INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET" to INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET,
            "INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET" to INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET,
            "INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET" to INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET,
            "INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET" to INSTRUMENT_REMAIN_DRIVING_TIME_DAY_SET,
            "INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET" to INSTRUMENT_EXPECTED_ARRIVE_HOUR_SET,
            "INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET" to INSTRUMENT_EXPECTED_ARRIVE_MINUTE_SET,
            "INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET" to INSTRUMENT_EXPECTED_ARRIVE_SECOND_SET,
            "INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET" to INSTRUMENT_DISTANCE_OF_TARGET_AHEAD_ADVANCED_SET,
            "INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET" to INSTRUMENT_NAVI_LEAD_MSG_ADVANCED_SET,
            "SETTING_MPC_IFC_NAVI_ST_SET" to SETTING_MPC_IFC_NAVI_ST_SET
        )
    }

    private val proxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            log("PROXY_CONNECTED received! action=${intent.action}")

            var binder: IBinder? = null
            try {
                binder = intent.extras?.getBinder("proxy_binder")
            } catch (_: Throwable) {}
            if (binder == null) {
                binder = tryPickBinderFromParcelable(intent)
            }
            if (binder == null) {
                val extras = intent.extras
                if (extras != null) {
                    for (key in extras.keySet()) {
                        val v = extras.get(key)
                        if (v is IBinder) { binder = v; break }
                    }
                }
            }

            if (binder == null) { log("  no binder in intent"); return }

            val desc = try { binder.interfaceDescriptor } catch (_: Throwable) { "?" }
            log("  binder: desc=$desc alive=${binder.isBinderAlive}")

            when {
                desc == "com.sr.openbyd.ipc.ICarControl" -> {
                    proxyBinder = binder
                    log("  -> routed to ICarControl proxyBinder")
                }
                desc == "com.unkwn2.yandexhud.IHudControl" || intent.action == "com.unkwn2.yandexhud.PROXY_CONNECTED" -> {
                    ShellProxyBridge.onProxyReceived(binder)
                    log("  -> routed to ShellProxyBridge")
                }
                else -> {
                    proxyBinder = binder
                    log("  -> fallback: routed to ICarControl proxyBinder")
                }
            }
        }
    }

    private fun tryPickBinderFromParcelable(intent: Intent): IBinder? {
        try {
            val p = intent.getParcelableExtra<android.os.Parcelable>("proxy_binder")
            if (p != null) {
                log("  proxy_binder parcelable: class=${p.javaClass.name}")
                try {
                    val f = p.javaClass.getDeclaredField("binder")
                    f.isAccessible = true
                    val b = f.get(p) as? IBinder
                    if (b != null) {
                        log("  binder extracted via Parcelable reflection!")
                        return b
                    }
                } catch (t: Throwable) {
                    log("  binder field reflect err: ${t.message}")
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.init(this)

        try {
            registerReceiver(proxyReceiver, IntentFilter("com.sr.openbyd.PROXY_CONNECTED"), Context.RECEIVER_NOT_EXPORTED)
            log("Registered com.sr.openbyd.PROXY_CONNECTED receiver")
        } catch (t: Throwable) {
            log("Register openbyd rcvr err: ${t.message}")
        }
        try {
            registerReceiver(proxyReceiver, IntentFilter("com.unkwn2.yandexhud.PROXY_CONNECTED"), Context.RECEIVER_NOT_EXPORTED)
            log("Registered com.unkwn2.yandexhud.PROXY_CONNECTED receiver")
        } catch (t: Throwable) {
            log("Register own rcvr err: ${t.message}")
        }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 4, 4, 4) }
        val status = TextView(this).apply { text = "v20 — HUD via AmapService broadcast"; textSize = 13f; setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(0xFF333333.toInt()); setPadding(8, 4, 8, 4) }
        root.addView(status)

        val logPanel = ScrollView(this)
        scroll = logPanel
        logView = TextView(this).apply { textSize = 10f; setTextColor(0xFF00FF00.toInt()); setBackgroundColor(0xFF1A1A1A.toInt()); setPadding(6, 4, 6, 4) }
        logPanel.addView(logView)
        val logLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 3f)
        root.addView(logPanel, logLp)

        val btnScroll = HorizontalScrollView(this)
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(2, 2, 2, 2) }
        btnScroll.addView(btnRow)

        fun btn(label: String, fn: () -> Unit): Button {
            val b = Button(this).apply { text = label; textSize = 9f; setPadding(6, 0, 6, 0); setOnClickListener { fn() } }
            btnRow.addView(b)
            return b
        }

        // === TST: TestDevice PRIMARY path (only working device) ===
        btn("!:Init") { initDevices() }
        btn("F:FIDs") { dumpFeatureIds() }
        btn("R:Read") { readBaseline() }
        btn("T:TST+") { tstActivate() }
        btn("T2:TSeq") { tstFullSequence() }
        btn("T3:TStp") { tstClose() }
        btn("T4:Scr3") { tstScreenNavi() }
        btn("T5:Scr0") { tstScreenOff() }

        // === C Broadcast (proven: reaches cluster) ===
        btn("CB+") { cbSend(2, 0) }
        btn("CBS") { cbFullSequence() }
        btn("CB-") { cbSend(4, 0) }

        // === Instrument/Setting (BLOCKED for uid 10168) ===
        btn("1:Inst") { instActivate() }
        btn("2:ISeq") { instFullSequence() }
        btn("3:IOf") { instClose() }

        // === Proxy buttons ===
        btn("A:PrSt") { startProxy() }
        btn("A2:Bnd") { tryBindProxyService() }
        btn("B:Ping") { proxyPing() }
        btn("C:NavF") { proxyReadNavFusion() }
        btn("D:+NF") { proxyEnableNavFusion() }
        btn("E:HUD") { proxyFullSequence() }
        btn("X:Stp") { deactivateHudNav() }

        // === Sub/Unsub ===
        btn("S:Sub") { subscribeNavEvents() }
        btn("U:Uns") { unsubscribeNavEvents() }

        // === GPS ===
        btn("J:Mock") { gpsEnableMock() }
        btn("K:GPS") { gpsSetBeijing() }
        btn("L:Rte") { gpsStartRoute() }
        btn("M:GP-") { gpsStopRoute() }
        btn("T:BCAST") { tstBcast() }
        btn("T:DIAG") { tstDiag() }

        // === Utils ===
        btn("N:Log") { exportLog() }
        btn("O:Clr") { clearLog() }
        btn("P:Ntf") { grantNotif() }
        btn("Q:Stat") { checkProxy() }

        val btnLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(btnScroll, btnLp)

        setContentView(root)
        log("v20 ready — uid=${android.os.Process.myUid()}")
        log("TestDevice: DEAD (BYDAUTO_COMMAND_RESULT_FAILED on all 54 FIDs)")
        log("PRIMARY: AUTONAVI_STANDARD_BROADCAST_SEND → AmapService → HUD")
        log("Priority: Broadcast > Proxy > T:DIAG")
    }

    override fun onDestroy() {
        try { unsubscribeNavEvents() } catch (_: Throwable) {}
        try { unregisterReceiver(proxyReceiver) } catch (_: Throwable) {}
        try { (proxyServiceConn as? android.content.ServiceConnection)?.let { unbindService(it) } } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun log(s: String) {
        handler.post {
            val t = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
            logView.append("$t  $s\n")
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
        FileLogger.write("v18", s)
    }

    private fun runCmd(vararg args: String): Pair<Int, String> = try {
        val proc = Runtime.getRuntime().exec(args)
        proc.outputStream.close()
        val out = proc.inputStream.bufferedReader().readText().trim()
        val err = proc.errorStream.bufferedReader().readText().trim()
        val exit = proc.waitFor()
        exit to if (out.isNotEmpty()) out else err
    } catch (t: Throwable) { -1 to "ERR: ${t.message}" }

    // === BYDAutoEventValue factory ===

    @SuppressLint("PrivateApi")
    private fun makeEventValue(intVal: Int): Any? {
        return try {
            val cls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val ctor = cls.getDeclaredConstructor()
            ctor.isAccessible = true
            val ev = ctor.newInstance()
            val f = cls.getDeclaredField("intValue")
            f.isAccessible = true
            f.set(ev, intVal)
            ev
        } catch (t: Throwable) {
            log("  makeEventValue(int) ERR: ${t.message}")
            null
        }
    }

    @SuppressLint("PrivateApi")
    private fun makeEventValueBytes(bytes: ByteArray): Any? {
        return try {
            val cls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val ctor = cls.getDeclaredConstructor()
            ctor.isAccessible = true
            val ev = ctor.newInstance()
            val f = cls.getDeclaredField("bufferDataValue")
            f.isAccessible = true
            f.set(ev, bytes)
            ev
        } catch (t: Throwable) {
            log("  makeEventValue(bytes) ERR: ${t.message}")
            null
        }
    }

    // === Instrument/Setting/Test device acquisition ===

    @SuppressLint("PrivateApi")
    private fun initDevices() {
        log("=== INIT DEVICES ===")

        FIDs.loadFromFramework { log(it) }
        for ((name, value) in FIDs.fields()) {
            log("  $name = 0x${Integer.toHexString(value)} ($value)")
        }

        try {
            val cls = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val gi = cls.getDeclaredMethod("getInstance", Context::class.java)
            gi.isAccessible = true
            instrumentDev = gi.invoke(null, this)
            log("  InstrumentDevice: ${if (instrumentDev != null) "OK" else "NULL"}")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  InstrumentDevice ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
        }

        try {
            val cls = Class.forName("android.hardware.bydauto.setting.BYDAutoSettingDevice")
            val gi = cls.getDeclaredMethod("getInstance", Context::class.java)
            gi.isAccessible = true
            settingDev = gi.invoke(null, this)
            log("  SettingDevice: ${if (settingDev != null) "OK" else "NULL"}")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  SettingDevice ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
        }

        try {
            val cls = Class.forName("android.hardware.bydauto.test.BYDAutoTestDevice")
            val gi = cls.getDeclaredMethod("getInstance", Context::class.java)
            gi.isAccessible = true
            testDev = gi.invoke(null, this)
            log("  TestDevice: ${if (testDev != null) "OK" else "NULL"}")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  TestDevice ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
        }

        dumpFeatureIds()
    }

    // === Read baseline (get() on all devices — COMMON permission may work even if SET blocked) ===

    @SuppressLint("PrivateApi")
    private fun devGet(dev: Any?, fid: Int, label: String) {
        if (dev == null) { log("  $label: device null"); return }
        try {
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val evCls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val gm = absCls.getMethod("get", IntArray::class.java, Class::class.java)
            val evt = gm.invoke(dev, intArrayOf(fid), evCls)
            val result = evCls.getField("intValue").getInt(evt)
            log("  $label get(0x${Integer.toHexString(fid)}) = $result")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  $label get ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
        }
    }

    private fun readBaseline() {
        log("=== READ BASELINE (get on all devices) ===")
        // Existing 19 FIDs
        for ((name, fid) in FIDs.fields()) {
            if (fid == 0) continue
            readBaselineFid(fid, name)
        }
        // New FIDs from msg 52 analysis
        val extraFids = listOf(
            "ATOM_HUD_SWITCH" to 0x0780E026,
            "CENTER_PROJECTION" to 0x40C0C010,
            "CENTER_PROJECTION2" to 0x40C0C022,
            "FALLBACK_ILLUSTRATION" to 0x40C0C019,
            "EASY_NAVI_GUIDE" to 0x1F701010,
            "EASY_ENTRY_ACTION" to 0x1F704010,
            "DIST_TARGET_HEAD" to 0x1F701018,
            "DRIVING_INFO_SWITCH" to 0x3A20000A,
            "DISPLAY_CONTENT_FC" to 0x38B00042,
            "DRIVING_AMBIENT_SET" to 0x32B1102C,
            "NAVI_TYPE_SET" to 0x4C10A018,
            "NAVI_CORP_SET" to 0x4CA00048,
            "SPEED_LIMIT_SET" to 0x4CA00040,
            "DIST_TRAFFIC_LIGHT" to 0x43F11010
        )
        for ((name, fid) in extraFids) {
            readBaselineFid(fid, name)
        }
        // Save current HUD layout
        val curLayout = canGet(0x4C10E015)
        if (curLayout in 0..10) { savedLayoutBeforeTest = curLayout; log(" saved layout (TST) = $curLayout") }
        log("BASELINE DONE — if get() works but set() doesn't, COMMON permission = read-only")
    }

    private fun readBaselineFid(fid: Int, name: String) {
        val prefix = (fid shr 16) and 0xFFFF
        when {
            prefix == 0x4C10 || prefix == 0x4CA0 || prefix == 0x38B0 -> devGet(settingDev, fid, "Sett $name")
            prefix == 0x43E0 || prefix == 0x43F0 || prefix == 0x43FA || prefix == 0x43FB -> devGet(instrumentDev, fid, "Inst $name")
            prefix == 0x0780 || prefix == 0x40C0 || prefix == 0x1F70 || prefix == 0x3A20 || prefix == 0x32B1 -> devGet(testDev, fid, "Atom $name")
            else -> devGet(testDev, fid, "Test $name")
        }
        devGet(testDev, fid, "Test $name")
    }

    // === Event listener subscription ===

    private var eventListeners: MutableList<Any> = mutableListOf()

    @SuppressLint("PrivateApi")
    private fun subscribeNavEvents() {
        log("=== SUBSCRIBE NAV EVENTS ===")

        val settingEventFids = listOf(
            0x38B00032 to "SAFE_DRIVING",
            0x38B00034 to "NAV_FUSION",
            0x38B0E015 to "HUD_SCREEN",
            0x38B0E036 to "NAV_FUSION_SETT"
        )

        val listenerCls = try {
            Class.forName("android.hardware.bydauto.setting.AbsBYDAutoSettingListener")
        } catch (_: Throwable) {
            try {
                Class.forName("android.hardware.bydauto.BYDAutoEventListener")
            } catch (t: Throwable) {
                log("  No listener class found: ${t.message?.take(60)}")
                return
            }
        }
        log("  Listener class: ${listenerCls.name}")

        val settDev = settingDev ?: try {
            val cls = Class.forName("android.hardware.bydauto.setting.BYDAutoSettingDevice")
            val gi = cls.getDeclaredMethod("getInstance", Context::class.java)
            gi.isAccessible = true
            gi.invoke(null, this@HudV18Activity)
        } catch (_: Throwable) { null }

        if (settDev == null) {
            log("  SettingDevice null — cannot subscribe")
        } else {
            for ((fid, name) in settingEventFids) {
                try {
                    val listener = java.lang.reflect.Proxy.newProxyInstance(
                        listenerCls.classLoader,
                        arrayOf(listenerCls)
                    ) { _, method, args ->
                        if (args != null && args.size >= 2) {
                            val fidArg = args[0] as? Int ?: -1
                            val evtVal = args[1]
                            val intVal = try {
                                val f = evtVal.javaClass.getDeclaredField("intValue")
                                f.isAccessible = true
                                f.getInt(evtVal)
                            } catch (_: Throwable) {
                                try { evtVal.toString().toInt() } catch (_: Throwable) { -1 }
                            }
                            log("SETT_EVT $name fid=0x${Integer.toHexString(fidArg)} value=$intVal")
                        }
                        null
                    }

                    var subscribed = false
                    for (regName in listOf("registerEventListener", "registerListener", "addListener", "subscribe")) {
                        try {
                            for (m in settDev.javaClass.methods.filter { it.name == regName }) {
                                val ptypes = m.parameterTypes
                                if (ptypes.size == 2 && ptypes[0] == IntArray::class.java && listenerCls.isAssignableFrom(ptypes[1])) {
                                    val r = m.invoke(settDev, intArrayOf(fid), listener)
                                    eventListeners.add(listener)
                                    log("  $name via ${regName}(Int[],Listener): r=$r")
                                    subscribed = true; break
                                }
                                if (ptypes.size == 2 && ptypes[1] == IntArray::class.java && listenerCls.isAssignableFrom(ptypes[0])) {
                                    val r = m.invoke(settDev, listener, intArrayOf(fid))
                                    eventListeners.add(listener)
                                    log("  $name via ${regName}(Listener,Int[]): r=$r")
                                    subscribed = true; break
                                }
                            }
                            if (subscribed) break
                        } catch (t: Throwable) {
                            val c2 = t.cause ?: t
                            log("  $name $regName ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(50)}")
                        }
                    }
                    if (!subscribed) log("  $name: no working register method found")
                } catch (t: Throwable) {
                    log("  $name setup ERR: ${t.message?.take(60)}")
                }
            }
        }

        val instListenerCls = try {
            Class.forName("android.hardware.bydauto.instrument.AbsBYDAutoInstrumentListener")
        } catch (_: Throwable) { null }

        if (instListenerCls != null && instrumentDev != null) {
            val instEventFids = listOf(
                0x38B0003A to "NAVI_ACTIVE",
                0x38B01010 to "TURN_ARROW",
                0x38B01018 to "TURN_DIST"
            )
            for ((fid, name) in instEventFids) {
                try {
                    val listener = java.lang.reflect.Proxy.newProxyInstance(
                        instListenerCls.classLoader,
                        arrayOf(instListenerCls)
                    ) { _, method, args ->
                        if (args != null && args.size >= 2) {
                            val fidArg = args[0] as? Int ?: -1
                            val evtVal = args[1]
                            val intVal = try {
                                val f = evtVal.javaClass.getDeclaredField("intValue")
                                f.isAccessible = true
                                f.getInt(evtVal)
                            } catch (_: Throwable) { -1 }
                            log("INST_EVT $name fid=0x${Integer.toHexString(fidArg)} value=$intVal")
                        }
                        null
                    }

                    var subscribed = false
                    for (regName in listOf("registerEventListener", "registerListener", "addListener", "subscribe")) {
                        try {
                            for (m in instrumentDev!!.javaClass.methods.filter { it.name == regName }) {
                                val ptypes = m.parameterTypes
                                if (ptypes.size == 2 && ptypes[0] == IntArray::class.java && instListenerCls.isAssignableFrom(ptypes[1])) {
                                    m.invoke(instrumentDev, intArrayOf(fid), listener)
                                    eventListeners.add(listener)
                                    log("  INST $name via $regName OK")
                                    subscribed = true; break
                                }
                                if (ptypes.size == 2 && ptypes[1] == IntArray::class.java && instListenerCls.isAssignableFrom(ptypes[0])) {
                                    m.invoke(instrumentDev, listener, intArrayOf(fid))
                                    eventListeners.add(listener)
                                    log("  INST $name via $regName OK")
                                    subscribed = true; break
                                }
                            }
                            if (subscribed) break
                        } catch (t: Throwable) {
                            val c2 = t.cause ?: t
                            log("  INST $name $regName ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(50)}")
                        }
                    }
                    if (!subscribed) log("  INST $name: no register method found")
                } catch (t: Throwable) {
                    log("  INST $name setup ERR: ${t.message?.take(60)}")
                }
            }
        }

        log("EVENT SUBSCRIBE DONE — toggle NavFusion/HUD in Settings to see live events")
    }

    @SuppressLint("PrivateApi")
    private fun unsubscribeNavEvents() {
        log("Unsubscribing nav events...")
        eventListeners.clear()
        log("Cleared listener references")
    }

    // === TestDevice PRIMARY path (only device that works for uid 10168) ===

    private fun tstActivate() {
        log("=== TST: Activate navi via TestDevice (Layout=2 UI7 fullscreen) ===")
        canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 2)
        canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 2)
    }

    private fun tstFullSequence() {
        log("=== TST: FULL AmapService SEQUENCE via TestDevice ===")
        Thread {
            log("[TST 1/8] Activate: Layout=2 (UI7 fullscreen) + navi=2")
            canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 2)
            canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 2)
            try { Thread.sleep(200) } catch (_: InterruptedException) {}

            log("[TST 2/8] Road name 'Moskovskaya ul.' (UTF-16LE)")
            val nameBytes = "Moskovskaya ul.".toByteArray(Charsets.UTF_16LE)
            canSetBytes(FIDs.INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET, nameBytes)
            try { Thread.sleep(200) } catch (_: InterruptedException) {}

            log("[TST 3/8] Turn distance = 500m")
            canSet(FIDs.INSTRUMENT_FRONT_CROSSING_DISTANCE_SET, 500)
            try { Thread.sleep(200) } catch (_: InterruptedException) {}

            log("[TST 4/8] Turn arrow = 3 (RIGHT)")
            canSet(FIDs.INSTRUMENT_GUIDE_INFO_SIMPLE_SET, 3)
            try { Thread.sleep(200) } catch (_: InterruptedException) {}

            log("[TST 5/8] Route remaining: 0h 15m 0s 50000m")
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET, 0)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET, 15)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET, 0)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET, 50000)

            log("[TST] Waiting 5s — CHECK HUD for nav data...")
            try { Thread.sleep(5000) } catch (_: InterruptedException) {}

            log("[TST 6/8] Change arrow to 7 (LEFT) + 200m")
            canSet(FIDs.INSTRUMENT_GUIDE_INFO_SIMPLE_SET, 7)
            canSet(FIDs.INSTRUMENT_FRONT_CROSSING_DISTANCE_SET, 200)
            try { Thread.sleep(3000) } catch (_: InterruptedException) {}

            log("[TST 7/8] Route remaining: 0h 5m 30s 2000m")
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET, 0)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET, 5)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET, 30)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET, 2000)
            try { Thread.sleep(3000) } catch (_: InterruptedException) {}

            log("[TST 8/8] Close: navi=4 + screen=0")
            canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 4)
            canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
            log("=== TST SEQ DONE ===")
        }.start()
    }

    private fun tstClose() {
        log("=== TST: Close navi via TestDevice ===")
        canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 4)
        canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
    }

    private fun tstScreenNavi() {
        log("TST: HUD screen = 2 (fullscreen nav, UI7) via TestDevice")
        canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 2)
    }

    private fun tstScreenOff() {
        log("TST: HUD screen = 0 (off) via TestDevice")
        canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
    }

    // === T:SIG — dump TestDevice API signatures ===

    // === T:BCAST — diagnostic broadcast to AmapService ===

    private fun readMany(fids: List<Int>): String {
        return fids.joinToString(" ") { fid ->
            val v = canGet(fid)
            "0x${Integer.toHexString(fid)}=$v"
        }
    }

    private fun tstBcast() {
        log("=== T:BCAST: AUTONAVI_STANDARD_BROADCAST_SEND ===")
        Thread {
            val action = "AUTONAVI_STANDARD_BROADCAST_SEND"
            val intent = Intent(action)
            val resolved = packageManager.queryBroadcastReceivers(intent, 0)
            log("Receivers for $action: ${resolved.size}")
            for (ri in resolved) {
                log("  ${ri.activityInfo.packageName}/${ri.activityInfo.name}")
                log("    exported=${ri.activityInfo.exported} perm=${ri.activityInfo.permission}")
            }
            if (resolved.isEmpty()) {
                log("FAIL: no receivers visible — check exported/permission")
                return@Thread
            }

            // Phase 1: Start navi frame
            val startIntent = Intent(action).apply {
                putExtra("KEY_TYPE", 1)
                putExtra("EXTRA_STATE", 1)
                putExtra("IS_BYD_MAP", true)
                putExtra("IS_BYD_BAIDU_MAP", false)
                putExtra("EXTRA_IS_FOREGROUND", 1)
            }
            sendBroadcast(startIntent)
            log("Sent start frame (KEY_TYPE=1)")
            Thread.sleep(300L)

            val pre = readMany(listOf(
                0x4C10E036, 0x4C10E015, 0x43E0003A, 0x43E00038, 0x0780E026
            ))
            log("AFTER start frame: $pre")

            // Phase 2: Guidance frame
            val guideIntent = Intent(action).apply {
                putExtra("KEY_TYPE", 2)
                putExtra("EXTRA_STATE", 2)
                putExtra("IS_BYD_MAP", true)
                putExtra("NEW_ICON", 2)
                putExtra("NEXT_ROAD_NAME", "Testovaya ul.")
                putExtra("NEXT_SEG_REMAIN_DIS", 500)
                putExtra("ROUTE_REMAIN_DIS", 5000)
                putExtra("ROUTE_REMAIN_TIME", "15")
                putExtra("ETA_TEXT", "12:30")
                putExtra("SEG_REMAIN_DIS_AUTO", 500)
                putExtra("ROUTE_REMAIN_DIS_AUTO", 5000)
                putExtra("ROUTE_REMAIN_TIME_AUTO", 900)
                putExtra("EXTRA_IS_FOREGROUND", 1)
            }
            sendBroadcast(guideIntent)
            log("Sent guidance frame (KEY_TYPE=2)")
            Thread.sleep(500L)

            val post = readMany(listOf(
                0x43F01010, 0x43F01018, 0x43FA1008, 0x43F02028, 0x43E0003A
            ))
            log("AFTER guidance frame: $post")

            log("=== LOOK AT HUD + cluster NOW! ===")
            Thread.sleep(5000L)
            log("=== T:BCAST DONE ===")
        }.start()
    }

    // === T:DIAG — decode constants + check permissions ===

    private fun tstDiag() {
        log("=== T:DIAG: Permissions + Error Codes ===")
        Thread {
            // Decode -2147482648
            val rc = -2147483648
            log(" Set return -2147482648 = 0x${Integer.toHexString(rc)}")
            log("   = ${rc and 0x7FFFFFFF.toInt()} + sign bit")
            log("   = potentially TEST_COMMAND_FAILED or INVAILD_INT")
            try {
                val tdCls = Class.forName("android.hardware.bydauto.test.BYDAutoTestDevice")
                log(" TestDevice constants:")
                for (f in tdCls.declaredFields) {
                    f.isAccessible = true
                    try { log("  ${f.name} = ${f.getInt(null)}") } catch (_: Throwable) {}
                }
            } catch (t: Throwable) {
                log("  constants dump ERR: ${t.message}")
            }
            try {
                val evCls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
                log(" EventValue constants:")
                for (f in evCls.declaredFields) {
                    f.isAccessible = true
                    try { log("  ${f.name} = ${f.get(null)}") } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
            try {
                val getPerm = testDev?.javaClass?.getMethod("getSetPermission")?.invoke(testDev)
                log(" TestDevice.setPermission = $getPerm")
            } catch (_: Throwable) {}
            try {
                val pm = applicationContext.checkCallingOrSelfPermission("android.permission.BYDAUTO_TEST")
                log(" check BYDAUTO_TEST = ${if (pm == 0) "GRANTED" else "DENIED ($pm)"}")
            } catch (_: Throwable) {}
            log("=== T:DIAG DONE ===")
        }.start()
    }

    // === Instrument Device path (EXACT AmapService API) ===

    @SuppressLint("PrivateApi")
    private fun instSet(fid: Int, intVal: Int): Boolean {
        try {
            val dev = instrumentDev ?: run { log("  InstrumentDev null — init first"); return false }
            val ev = makeEventValue(intVal) ?: return false
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getDeclaredMethod("set", IntArray::class.java, Class.forName("android.hardware.bydauto.BYDAutoEventValue"))
            setMethod.isAccessible = true
            val result = setMethod.invoke(dev, intArrayOf(fid), ev)
            log("  inst.set([0x${Integer.toHexString(fid)}], intValue=$intVal) = $result")
            return true
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  instSet ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return false
        }
    }

    @SuppressLint("PrivateApi")
    private fun instSetBytes(fid: Int, bytes: ByteArray): Boolean {
        try {
            val dev = instrumentDev ?: run { log("  InstrumentDev null — init first"); return false }
            val ev = makeEventValueBytes(bytes) ?: return false
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getDeclaredMethod("set", IntArray::class.java, Class.forName("android.hardware.bydauto.BYDAutoEventValue"))
            setMethod.isAccessible = true
            val result = setMethod.invoke(dev, intArrayOf(fid), ev)
            log("  inst.set([0x${Integer.toHexString(fid)}], ${bytes.size}B) = $result")
            return true
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  instSetBytes ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return false
        }
    }

    private fun instActivate() {
        log("=== INST: Activate navi (AmapService path) ===")
        log("  Step 1: HUD screen=3 (SettingDevice)")
        settSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 3)
        log("  Step 2: NAVI_ACTIVE=2 (InstrumentDevice)")
        instSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 2)
    }

    private fun instFullSequence() {
        log("=== INST: FULL AmapService SEQUENCE ===")
        Thread {
            log("[1] Activate: screen=3 + navi=2")
            settSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 3)
            instSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 2)
            try { Thread.sleep(500) } catch (_: InterruptedException) {}

            log("[2] Road name 'Moskovskaya ul.' (UTF-16LE)")
            val nameBytes = "Moskovskaya ul.".toByteArray(Charsets.UTF_16LE)
            instSetBytes(FIDs.INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET, nameBytes)
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[3] Turn distance = 500m")
            instSet(FIDs.INSTRUMENT_FRONT_CROSSING_DISTANCE_SET, 500)
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[4] Turn arrow = 3 (RIGHT) — SIMPLE_SET")
            instSet(FIDs.INSTRUMENT_GUIDE_INFO_SIMPLE_SET, 3)
            try { Thread.sleep(300) } catch (_: InterruptedException) {}

            log("[5] Route remaining: 0h 15m 0s 50km")
            instSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET, 0)
            instSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET, 15)
            instSet(FIDs.INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET, 0)
            instSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET, 50000)
            try { Thread.sleep(8000) } catch (_: InterruptedException) {}

            log("[6] Change arrow to 7 (LEFT) + 200m")
            instSet(FIDs.INSTRUMENT_GUIDE_INFO_SIMPLE_SET, 7)
            instSet(FIDs.INSTRUMENT_FRONT_CROSSING_DISTANCE_SET, 200)
            try { Thread.sleep(5000) } catch (_: InterruptedException) {}

            log("[7] Close: navi=4 + screen=0")
            instSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 4)
            settSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
            log("=== INST SEQ DONE ===")
        }.start()
    }

    private fun instClose() {
        log("=== INST: Close navi ===")
        instSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 4)
        settSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
    }

    // === Setting Device path ===

    @SuppressLint("PrivateApi")
    private fun settSet(fid: Int, intVal: Int): Boolean {
        try {
            val dev = settingDev ?: run { log("  SettingDev null — init first"); return false }
            val ev = makeEventValue(intVal) ?: return false
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getDeclaredMethod("set", IntArray::class.java, Class.forName("android.hardware.bydauto.BYDAutoEventValue"))
            setMethod.isAccessible = true
            val result = setMethod.invoke(dev, intArrayOf(fid), ev)
            log("  sett.set([0x${Integer.toHexString(fid)}], intValue=$intVal) = $result")
            return true
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  settSet ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return false
        }
    }

    private fun settScreenNavi() {
        log("Setting: HUD screen = 3 (navi)")
        settSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 3)
    }

    private fun settScreenOff() {
        log("Setting: HUD screen = 0 (off)")
        settSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
    }

    // === CAN Direct via BYDAutoTestDevice (fallback) ===

    @SuppressLint("PrivateApi")
    private fun canSet(fid: Int, value: Int): Boolean {
        try {
            val dev = testDev ?: run {
                val cls = Class.forName("android.hardware.bydauto.test.BYDAutoTestDevice")
                val gi = cls.getDeclaredMethod("getInstance", Context::class.java)
                gi.isAccessible = true
                testDev = gi.invoke(null, this@HudV18Activity)
                testDev ?: run { log("  TestDevice null"); return false }
            }
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getDeclaredMethod("set", IntArray::class.java, Class.forName("android.hardware.bydauto.BYDAutoEventValue"))
            setMethod.isAccessible = true
            val ev = makeEventValue(value) ?: return false
            val result = setMethod.invoke(dev, intArrayOf(fid), ev)
            log("  can.set([0x${Integer.toHexString(fid)}], intValue=$value) = $result")
            return true
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  canSet ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return false
        }
    }

    @SuppressLint("PrivateApi")
    private fun canSetBytes(fid: Int, bytes: ByteArray): Boolean {
        try {
            val dev = testDev ?: run { log("  TestDevice null"); return false }
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getDeclaredMethod("set", IntArray::class.java, Class.forName("android.hardware.bydauto.BYDAutoEventValue"))
            setMethod.isAccessible = true
            val ev = makeEventValueBytes(bytes) ?: return false
            val result = setMethod.invoke(dev, intArrayOf(fid), ev)
            log("  can.set([0x${Integer.toHexString(fid)}], ${bytes.size}B) = $result")
            return true
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  canSetBytes ERR: ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
            return false
        }
    }

    @SuppressLint("PrivateApi")
    private fun canGet(fid: Int): Int {
        try {
            val dev = testDev ?: return -1
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val evCls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val gm = absCls.getMethod("get", IntArray::class.java, Class::class.java)
            val evt = gm.invoke(dev, intArrayOf(fid), evCls) ?: return -1
            val f = evCls.getField("intValue")
            return f.getInt(evt)
        } catch (t: Throwable) {
            return -1
        }
    }

    private fun canActivate() {
        log("CAN: Activate (Layout=2)")
        canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 2)
        canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 2)
    }

    private fun canFullSequence() {
        log("=== CAN FULL SEQ ===")
        Thread {
            canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 2)
            canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 2)
            try { Thread.sleep(500) } catch (_: InterruptedException) {}
            canSet(FIDs.INSTRUMENT_GUIDE_INFO_SIMPLE_SET, 3)
            canSet(FIDs.INSTRUMENT_FRONT_CROSSING_DISTANCE_SET, 500)
            val nameBytes = "Moskovskaya ul.".toByteArray(Charsets.UTF_16LE)
            canSetBytes(FIDs.INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_SET, nameBytes)
            try { Thread.sleep(300) } catch (_: InterruptedException) {}
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET, 0)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET, 15)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET, 0)
            canSet(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET, 50000)
            try { Thread.sleep(8000) } catch (_: InterruptedException) {}
            canSet(FIDs.INSTRUMENT_GUIDE_INFO_SIMPLE_SET, 7)
            canSet(FIDs.INSTRUMENT_FRONT_CROSSING_DISTANCE_SET, 200)
            try { Thread.sleep(5000) } catch (_: InterruptedException) {}
            canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 4)
            canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
            log("=== CAN SEQ DONE ===")
        }.start()
    }

    private fun canReadNaviStatus() {
        log("CAN: Read status")
        canGet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET)
        canGet(FIDs.SET_NAVI_SCREEN_STATUS_SET)
        canGet(FIDs.INSTRUMENT_GUIDE_INFO_SIMPLE_SET)
        canGet(FIDs.INSTRUMENT_FRONT_CROSSING_DISTANCE_SET)
    }

    private fun canClose() {
        log("CAN: Close")
        canSet(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET, 4)
        canSet(FIDs.SET_NAVI_SCREEN_STATUS_SET, 0)
    }

    // === FID Reflection Dump ===

    @SuppressLint("PrivateApi")
    private fun dumpFeatureIds() {
        log("=== DUMP BYDAutoFeatureIds (framework) ===")
        if (!FIDs.loaded) {
            FIDs.loadFromFramework { log(it) }
        }
        for ((name, value) in FIDs.fields()) {
            log("  $name = 0x${Integer.toHexString(value)} ($value)")
        }

        try {
            val cls = Class.forName("android.hardware.bydauto.BYDAutoFeatureIds")
            val allFields = cls.declaredFields.filter { 
                it.name.startsWith("INSTRUMENT_") || it.name.startsWith("SET_NAVI")
                || it.name.startsWith("NAVIGATION_") || it.name.startsWith("SETTING_")
                || it.name.startsWith("ATOM_") || it.name.startsWith("EASY_")
                || it.name.startsWith("CENTER_") || it.name.startsWith("HUD_")
                || it.name.startsWith("ARHUD_") || it.name.startsWith("DRIVING_")
                || it.name.startsWith("DISPLAY_") || it.name.startsWith("SPEED_")
                || it.name.startsWith("PRODUCT_") || it.name.startsWith("VEHICLE_")
                || it.name.startsWith("DIST_") || it.name.startsWith("DD_")
            }.sortedBy { it.name }
            if (allFields.isNotEmpty()) {
                log("  --- ALL INSTRUMENT/NAVI/SETTING/ATOM/HUD fields (non-zero only) ---")
                for (f in allFields) {
                    f.isAccessible = true
                    try {
                        val v = f.getInt(null)
                        if (v != 0) log("  ${f.name} = 0x${Integer.toHexString(v)} ($v)")
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}

        for (subName in listOf("Instrument", "Setting", "Test", "Common", "Product",
                               "Vehicle", "Atom", "Easy", "Center", "Driving", "Hud")) {
            try {
                val sub = Class.forName("android.hardware.bydauto.BYDAutoFeatureIds\$$subName")
                val subFields = sub.declaredFields.sortedBy { it.name }
                var printed = 0
                for (f in subFields) {
                    f.isAccessible = true
                    try {
                        val v = f.getInt(null)
                        if (v != 0) {
                            log("  $subName.${f.name} = 0x${Integer.toHexString(v)} ($v)")
                            printed++
                        }
                    } catch (_: Throwable) {}
                }
                if (printed > 0) log("  ($subName: $printed non-zero fields)")
            } catch (_: Throwable) {}
        }

        log("=== FID DUMP DONE ===")
    }

    // === ICarControl raw binder transact ===

    private fun parseProxyResult(reply: String?): Pair<Boolean, Int?> {
        if (reply.isNullOrBlank()) return false to null
        val success = reply.contains("SUCCESS", ignoreCase = true)
        val codeMatch = Regex("Returned code:\\s*(-?\\d+)").find(reply)
        val frameworkCode = codeMatch?.groupValues?.get(1)?.toIntOrNull()
        return success to frameworkCode
    }

    private fun logProxyResult(step: String, reply: String?) {
        val (ok, code) = parseProxyResult(reply)
        log("  $step ok=$ok fwCode=$code raw='${reply?.take(120)}'")
    }

    private fun iccTransact(tx: Int, writeArgs: (Parcel) -> Unit): String? {
        val binder = proxyBinder ?: run { log("  NO PROXY BINDER"); return null }
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESC)
            writeArgs(data)
            val ok = binder.transact(tx, data, reply, 0)
            if (!ok) { log("  transact($tx) returned false"); return null }
            reply.readException()
            reply.readString()
        } catch (t: Throwable) {
            if (t is android.os.DeadObjectException) { proxyBinder = null; log("  DeadObjectException — cleared proxyBinder") }
            log("  transact($tx) err: ${t.message}")
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private fun iccTransactInt(tx: Int, writeArgs: (Parcel) -> Unit): Int? {
        val binder = proxyBinder ?: run { log("  NO PROXY BINDER"); return null }
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESC)
            writeArgs(data)
            val ok = binder.transact(tx, data, reply, 0)
            if (!ok) { log("  transact($tx) returned false"); return null }
            reply.readException()
            reply.readInt()
        } catch (t: Throwable) {
            if (t is android.os.DeadObjectException) { proxyBinder = null; log("  DeadObjectException — cleared proxyBinder") }
            log("  transact($tx) err: ${t.message}")
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    // === C-Broadcast ===

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
        }, 1000)
        handler.postDelayed({ cbSend(7, 200) }, 3000)
        handler.postDelayed({ cbSend(1, 100) }, 6000)
        handler.postDelayed({ cbSend(4, 0) }, 9000)
    }

    // === Proxy binder ===

    private fun startProxy() {
        log("Starting yandexhud_proxy via libadb-android...")
        Thread {
            val result = AdbLocalClient.startProxy(this@HudV18Activity)
            log("  AdbLocal: $result")

            try { Thread.sleep(3000) } catch (_: InterruptedException) {}

            val (e3, o3) = runCmd("pidof", "yandexhud_proxy")
            log("  pidof: exit=$e3 pid='$o3'")

            if (ShellProxyBridge.isConnected()) {
                log("  ShellProxyBridge CONNECTED — binder alive")
            } else if (e3 == 0 && o3.isNotBlank()) {
                log("  Proxy PID=$o3 alive but binder NOT received — waiting 3s more...")
                try { Thread.sleep(3000) } catch (_: InterruptedException) {}
                if (ShellProxyBridge.isConnected()) {
                    log("  ShellProxyBridge CONNECTED (delayed)")
                } else {
                    log("  Proxy alive but no binder after 6s — check handshake file")
                    proxyDiag()
                }
            } else {
                log("  Proxy process died — checking logs...")
                proxyDiag()
                log("  Falling back: OpenBYD ICarControl binder...")
                tryAcquireFromBydOpen()
                if (proxyBinder == null) {
                    try {
                        val ri = Intent("com.sr.openbyd.PROXY_REQUEST_CONNECT")
                        ri.setPackage("com.sr.openbyd")
                        sendBroadcast(ri)
                        log("  PROXY_REQUEST_CONNECT sent")
                        try { Thread.sleep(2000) } catch (_: InterruptedException) {}
                    } catch (t: Throwable) {
                        log("  request broadcast err: ${t.message}")
                    }
                }
            }

            if (proxyBinder != null) {
                log("  ICarControl BINDER ACQUIRED — ready")
                try { log("  desc=${proxyBinder!!.interfaceDescriptor} alive=${proxyBinder!!.isBinderAlive}") } catch (_: Throwable) {}
            }
            if (ShellProxyBridge.isConnected()) {
                log("  ShellProxyBridge — ready")
            }
            if (proxyBinder == null && !ShellProxyBridge.isConnected()) {
                log("  NO BINDER (ICarControl or ShellProxy) — all methods failed")
            }
        }.start()
    }

    private fun proxyDiag() {
        try {
            val (_, lc) = runCmd("logcat", "-d", "-s", "HudProxy:I", "AndroidRuntime:E", "System.err:W")
            val lines = lc.lines().filter { it.contains("HudProxy") || it.contains("FATAL") || it.contains("AndroidRuntime") || it.contains("EntryPoint") || it.contains("yandexhud") }
            if (lines.isNotEmpty()) {
                log("  logcat (last 20 lines):")
                lines.takeLast(20).forEach { log("    $it") }
            } else {
                log("  logcat EMPTY — no crash traces")
            }
        } catch (t: Throwable) {
            log("  logcat capture err: ${t.message}")
        }
        try {
            val hs = java.io.File("/storage/emulated/0/Android/data/com.unkwn2.yandexhud/files/hud_proxy.log")
            if (hs.exists()) {
                log("  hud_proxy.log (last 15 lines):")
                hs.readLines().takeLast(15).forEach { log("    $it") }
            }
            val hf = java.io.File("/storage/emulated/0/Android/data/com.unkwn2.yandexhud/files/proxy_handshake.txt")
            if (hf.exists()) {
                log("  handshake: ${hf.readText().trim().replace("\n", " | ")}")
            }
        } catch (t: Throwable) {
            log("  proxy diag file read err: ${t.message}")
        }
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
                    log("  GOT BINDER from ProxyManager reflection!")
                    return
                }
            }
            log("  ProxyManager.carControl = null")
        } catch (t: Throwable) {
            log("  ProxyManager reflect err: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryBindProxyService() {
        log("=== TRY BIND SERVICE ===")
        val serviceIntents = listOf(
            Intent("com.sr.openbyd.PROXY_BIND").setPackage("com.sr.openbyd"),
            Intent("com.sr.openbyd.PROXY_SERVICE").setPackage("com.sr.openbyd"),
            Intent().setClassName("com.sr.openbyd", "com.sr.openbyd.proxy.ProxyService"),
            Intent().setClassName("com.sr.openbyd", "com.sr.openbyd.services.ClusterProjectionService"),
        )

        val conn = object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                log("  BOUND! component=${name.flattenToString()} desc=${try{service.interfaceDescriptor}catch(_:Throwable){"?"}}")
                proxyBinder = service
                proxyServiceConn = this
            }
            override fun onServiceDisconnected(name: ComponentName) {
                log("  DISCONNECTED: ${name.flattenToString()}")
                if (proxyServiceConn == this) {
                    proxyBinder = null
                    proxyServiceConn = null
                }
            }
        }

        for ((idx, intent) in serviceIntents.withIndex()) {
            try {
                val bound = bindService(intent, conn, Context.BIND_AUTO_CREATE)
                log("  intent[$idx] ${intent.action ?: intent.component?.flattenToString() ?: "?"}: bind=$bound")
                if (bound) {
                    try { Thread.sleep(2000) } catch (_: InterruptedException) {}
                    if (proxyBinder != null) {
                        log("  BINDER ACQUIRED via bindService!")
                        return
                    }
                }
            } catch (t: Throwable) {
                log("  intent[$idx] ERR: ${t.javaClass.simpleName}: ${t.message?.take(60)}")
            }
        }

        try { unbindService(conn) } catch (_: Throwable) {}
        log("  bindService failed for all intents — no exported proxy service")
    }

    private fun proxyHandshake(): Boolean {
        log("=== PROXY HANDSHAKE ===")
        val pingResult = iccTransact(TX_PING) {}
        logProxyResult("ping TX2", pingResult)
        val apiVer = iccTransactInt(TX_GET_API_VERSION) {}
        log("  getApiVersion TX1 => $apiVer")
        if (apiVer != null && apiVer > 1000000) {
            log("  WARN: apiVer=$apiVer looks like string-read-as-int — switching TX1 to iccTransact")
            val apiStr = iccTransact(TX_GET_API_VERSION) {}
            log("  getApiVersion (asString) => '$apiStr'")
        }
        val alive = pingResult != null
        log("  HANDSHAKE result: ${if (alive) "ALIVE" else "DEAD"}")
        return alive
    }

    private fun proxyPing() {
        if (!proxyHandshake()) return
        log("Proxy: ping (tx=2)")
        val result = iccTransact(TX_PING) {}
        logProxyResult("ping", result)
        val ver = iccTransactInt(TX_GET_API_VERSION) {}
        log("  apiVersion => $ver")
    }

    private fun proxyReadNavFusion() {
        log("Proxy: read NavFusion + HUD Layout")
        val r1 = iccTransact(TX_GET_SETTING_FEATURE) { it.writeInt(FIDs.NAVIGATION_FUSION_SWITCH_SET) }
        logProxyResult("NavFusion(0x4C10E036) (1=ON,2=OFF)", r1)
        val r2 = iccTransact(TX_GET_SETTING_FEATURE) { it.writeInt(FIDs.SET_NAVI_SCREEN_STATUS_SET) }
        logProxyResult("HUDLayout(0x4C10E015) (3=Navi)", r2)
        val layoutVal = r2?.let { parseProxyResult(it).second } ?: 1
        if (layoutVal in 0..10) savedLayoutBeforeTest = layoutVal
        log("  savedLayoutBeforeTest=$savedLayoutBeforeTest")
    }

    private fun proxyEnableNavFusion() {
        log("Proxy: setSettingFeatureValue(NavFusion=ON)")
        val r = iccTransact(TX_SET_SETTING_FEATURE) { d -> d.writeInt(FIDs.NAVIGATION_FUSION_SWITCH_SET); d.writeInt(1) }
        logProxyResult("NavFusion=ON(0x4C10E036,1)", r)
        handler.postDelayed({ proxyReadNavFusion() }, 1000)
    }

    private fun proxyFullSequence() {
        log("=== HUD NAV ACTIVATION (AmapService order) ===")
        Thread {
            if (!proxyHandshake()) { log("  ABORT: proxy not alive"); return@Thread }

            log("--- Phase 0: Meta-toggle NavFusion=ON ---")
            var r = iccTransact(TX_SET_SETTING_FEATURE) { d -> d.writeInt(FIDs.NAVIGATION_FUSION_SWITCH_SET); d.writeInt(1) }
            logProxyResult("[P0] setSetting(NavFusion=0x4C10E036, ON=1)", r)

            log("--- Phase 1: HUD Layout=Navi ---")
            r = iccTransact(TX_SET_SETTING_FEATURE) { d -> d.writeInt(FIDs.SET_NAVI_SCREEN_STATUS_SET); d.writeInt(3) }
            logProxyResult("[P1] setSetting(HUDLayout=0x4C10E015, Navi=3)", r)

            log("  -- 200ms pause for HAL layout switch --")
            try { Thread.sleep(200) } catch (_: InterruptedException) {}

            log("--- Phase 2: Activate navi pipe ---")
            r = iccTransact(TX_SEND_AUTO_NAVI_STATUS) { it.writeInt(2) }
            logProxyResult("[P2] sendAutoNaviStatus(Active=2)", r)
            try { Thread.sleep(100) } catch (_: InterruptedException) {}

            log("--- Phase 3: First nav data frame ---")
            r = iccTransact(TX_SEND_SIMPLE_GUIDANCE) { d -> d.writeInt(3); d.writeInt(500) }
            logProxyResult("[P3a] sendSimpleGuidance(arrow=3, dist=500m)", r)
            r = iccTransact(TX_SEND_NEXT_PATH_NAME) { it.writeString("\u5317\u4EAC\u8DEF") }
            logProxyResult("[P3b] sendNextPathName('北京路')", r)
            r = iccTransact(TX_SEND_REST_ROUTE_INFO) { d -> d.writeInt(0); d.writeInt(15); d.writeLong(5200L) }
            logProxyResult("[P3c] sendRestRouteInfo(hr=0,min=15,m=5200)", r)

            if (FIDs.INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET != 0) {
                r = iccTransact(TX_SET_INSTRUMENT_FEATURE) { d -> d.writeInt(FIDs.INSTRUMENT_NAVI_TRIP_INFO_HOUR_SET); d.writeInt(0) }
                logProxyResult("[P3d] setInstrument(HOUR, 0)", r)
            } else { log("[P3d] HOUR FID=0 — skipped, sendRestRouteInfo covers this") }
            if (FIDs.INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET != 0) {
                r = iccTransact(TX_SET_INSTRUMENT_FEATURE) { d -> d.writeInt(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MINUTE_SET); d.writeInt(15) }
                logProxyResult("[P3e] setInstrument(MIN, 15)", r)
            } else { log("[P3e] MIN FID=0 — skipped, sendRestRouteInfo covers this") }
            if (FIDs.INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET != 0) {
                r = iccTransact(TX_SET_INSTRUMENT_FEATURE) { d -> d.writeInt(FIDs.INSTRUMENT_NAVI_TRIP_REMAINING_SECOND_SET); d.writeInt(0) }
                logProxyResult("[P3f] setInstrument(SEC, 0)", r)
            } else { log("[P3f] SEC FID=0 — skipped, sendRestRouteInfo covers this") }
            if (FIDs.INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET != 0) {
                r = iccTransact(TX_SET_INSTRUMENT_FEATURE) { d -> d.writeInt(FIDs.INSTRUMENT_NAVI_TRIP_INFO_MILEAGE_SET); d.writeInt(50) }
                logProxyResult("[P3g] setInstrument(MILEAGE, 50km)", r)
            } else { log("[P3g] MILEAGE FID=0 — skipped, sendRestRouteInfo covers this") }

            log("=== HUD NAV SEQ DONE — stay active for manual observation ===")
        }.start()
    }

    private fun deactivateHudNav() {
        log("=== HUD NAV DEACTIVATE (restore Layout=$savedLayoutBeforeTest) ===")
        Thread {
            if (proxyBinder == null) { log("  NO PROXY BINDER"); return@Thread }
            var r = iccTransact(TX_SEND_AUTO_NAVI_STATUS) { it.writeInt(4) }
            logProxyResult("[stop] sendAutoNaviStatus(Standby=4)", r)
            r = iccTransact(TX_SET_SETTING_FEATURE) { d -> d.writeInt(FIDs.SET_NAVI_SCREEN_STATUS_SET); d.writeInt(savedLayoutBeforeTest) }
            logProxyResult("[stop] setSetting(HUDLayout=$savedLayoutBeforeTest)", r)
            r = iccTransact(TX_SET_INSTRUMENT_FEATURE) { d -> d.writeInt(FIDs.INSTRUMENT_SEND_NAVI_STATUS_SET); d.writeInt(4) }
            logProxyResult("[stop] setInstrument(NAVI_STATUS=Standby)", r)
        }.start()
    }

    // === GPS Spoof ===

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
        log("GPS: Beijing (39.9042, 116.4074)")
        gpsInjectLocation(39.9042, 116.4074)
    }

    private fun gpsStartRoute() {
        if (gpsRunning) { log("GPS: Route already running"); return }
        gpsRunning = true
        gpsRouteIdx = 0
        log("GPS: Starting Beijing route")
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

    private fun gpsEnableMock() {
        log("GPS: Granting mock location...")
        val pkg = packageName
        Thread {
            try { val (e, o) = runCmd("appops", "set", pkg, "android:mock_location", "allow"); log("  appops: exit=$e '$o'") } catch (t: Throwable) { log("  appops ERR: ${t.message}") }
            try { val (e, o) = runCmd("settings", "put", "secure", "mock_location_app", pkg); log("  settings: exit=$e '$o'") } catch (t: Throwable) { log("  settings ERR: ${t.message}") }
            try { val (e, o) = runCmd("appops", "get", pkg, "android:mock_location"); log("  verify: exit=$e '$o'") } catch (_: Throwable) {}
            try { val (e, o) = runCmd("cmd", "location", "add-test-provider", "gps"); log("  test-provider: exit=$e '$o'") } catch (t: Throwable) { log("  test-provider ERR: ${t.message}") }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun gpsInjectLocation(lat: Double, lon: Double) {
        var injected = false
        try {
            val latStr = String.format("%.6f", lat)
            val lonStr = String.format("%.6f", lon)
            val (e0, o0) = runCmd("cmd", "location", "set-test-provider-location", "gps", "--location", "$latStr,$lonStr")
            if (e0 == 0 && !o0.contains("Error") && !o0.contains("Unknown")) {
                log("  GPS inject OK: $lat, $lon")
                injected = true
            } else {
                log("  cmd location: exit=$e0 '$o0'")
            }
        } catch (t: Throwable) {
            log("  cmd location ERR: ${t.message}")
        }
        if (!injected) {
            try {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                try { lm.removeTestProvider(LocationManager.GPS_PROVIDER) } catch (_: Throwable) {}
                lm.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 1, 5)
                lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
                val loc = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = lat; longitude = lon; altitude = 50.0; bearing = 0f; speed = 0f; accuracy = 5f
                    time = System.currentTimeMillis(); elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                }
                lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc)
                log("  GPS inject OK via TestProvider")
                injected = true
            } catch (t: Throwable) {
                log("  TestProvider ERR: ${t.javaClass.simpleName}: ${t.message?.take(60)}")
            }
        }
        if (!injected) log("  ALL GPS methods failed — press J first")
    }

    // === Utils ===

    private fun exportLog() {
        try {
            val src = FileLogger.getFile() ?: return log("No log file")
            val dir = android.os.Environment.getExternalStorageDirectory()
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
            val dst = java.io.File(dir, "Download/yandex_hud_v20_${ts}.log")
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
    }

    private fun checkProxy() {
        log("Checking proxy status...")
        Thread {
            val (e, o) = runCmd("pidof", "yandexhud_proxy")
            log("  yandexhud_proxy pidof: exit=$e pid='$o'")
            val (e2, o2) = runCmd("pidof", "openbyd_proxy")
            if (e2 == 0 && o2.isNotBlank()) log("  openbyd_proxy pidof: exit=$e2 pid='$o2'")
            try {
                val pi = packageManager.getPackageInfo("com.sr.openbyd", 0)
                log("  BYDOpen pkg: uid=${pi.applicationInfo?.uid}")
            } catch (t: Throwable) {
                log("  BYDOpen NOT found")
            }
            log("  ICarControl binder = ${if (proxyBinder != null) "ACQUIRED" else "NULL"}")
            if (proxyBinder != null) {
                try { log("  desc=${proxyBinder!!.interfaceDescriptor} alive=${proxyBinder!!.isBinderAlive}") } catch (_: Throwable) {}
            }
            log("  ShellProxyBridge connected=${ShellProxyBridge.isConnected()}")
            try {
                val hs = java.io.File("/storage/emulated/0/Android/data/com.unkwn2.yandexhud/files/proxy_handshake.txt")
                if (hs.exists()) {
                    log("  handshake: ${hs.readText().trim().replace("\n", " | ")}")
                } else {
                    log("  NO handshake file")
                }
            } catch (t: Throwable) {
                log("  handshake read err: ${t.message}")
            }
            try {
                val plog = java.io.File("/storage/emulated/0/Android/data/com.unkwn2.yandexhud/files/hud_proxy.log")
                if (plog.exists()) {
                    log("  hud_proxy.log (last 8):")
                    plog.readLines().takeLast(8).forEach { log("    $it") }
                }
            } catch (_: Throwable) {}
        }.start()
    }
}
