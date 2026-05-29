package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.DisplayMetrics
import android.view.Display
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProbeActivity : AppCompatActivity() {
    private lateinit var logView: TextView
    private lateinit var statusView: TextView
    private var protoBinder: IBinder? = null
    private var protoBound = false
    private val handler = Handler(Looper.getMainLooper())
    private val AIDL_PROTO = "com.autosdk.protocol.service.IProtocolService"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_probe)
        logView = findViewById(R.id.probeLog)
        statusView = findViewById(R.id.probeStatus)
        FileLogger.init(this)

        log("HudProbe v14f — HUD path discovery")
        log("UID=${android.os.Process.myUid()} PID=${android.os.Process.myPid()}")
        log("LOG FILE: ${FileLogger.getFile()?.absolutePath}")

        findViewById<Button>(R.id.btnEnumDisplays).setOnClickListener { enumDisplays() }
        findViewById<Button>(R.id.btnGetProps).setOnClickListener { getHudProps() }
        findViewById<Button>(R.id.btnProbeInstr).setOnClickListener { probeInstrument() }
        findViewById<Button>(R.id.btnProbeAllDevices).setOnClickListener { probeAllBydDevices() }

        findViewById<Button>(R.id.btnProtoBind).setOnClickListener { bindProto() }
        findViewById<Button>(R.id.btnProtoSendGuide).setOnClickListener { protoSendGuide() }
        findViewById<Button>(R.id.btnProtoSendNavi).setOnClickListener { protoSendNavi() }
        findViewById<Button>(R.id.btnProtoSetModel).setOnClickListener { protoSetModel() }

        findViewById<Button>(R.id.btnProbeContainer).setOnClickListener { probeContainerService() }
        findViewById<Button>(R.id.btnProbeAllSvc).setOnClickListener { probeAllServices() }
        findViewById<Button>(R.id.btnReadCfg).setOnClickListener { readConfig() }
        findViewById<Button>(R.id.btnDirect39).setOnClickListener { trySendInfo(1000, 39, "") }
        findViewById<Button>(R.id.btnProbeTest).setOnClickListener { probeTest() }
        findViewById<Button>(R.id.btnBroadcastCan).setOnClickListener { broadcastCan() }
        findViewById<Button>(R.id.btnCameraGuide).setOnClickListener { tryInstrMethod("sendCameraGuidanceInfo", 0, 200, 1) }
        findViewById<Button>(R.id.btnSafeGuide).setOnClickListener { tryInstrMethod("sendSafeGuidanceInfo", 0, 200, 1) }
        findViewById<Button>(R.id.btnThreeLineLyrics).setOnClickListener { tryInstrStringMethod("sendThreeLineLyrics", "Line1", "Line2", "Line3") }
        findViewById<Button>(R.id.btnSendAddr).setOnClickListener { tryInstrAddrMethod("sendAddressInfo", 0, "Test Address") }
        findViewById<Button>(R.id.btnNextPath).setOnClickListener { tryInstrSingleString("sendNextPathName", "Test Road") }
        findViewById<Button>(R.id.btnDestSet).setOnClickListener { tryInstrMethod("sendDestinationSetStatus", 1, 0, 0) }
        findViewById<Button>(R.id.btnRestRoute).setOnClickListener { tryInstrRestRoute() }
        findViewById<Button>(R.id.btnViewSet).setOnClickListener { tryViewSwitch() }
        findViewById<Button>(R.id.btnClearInfo).setOnClickListener { tryClearInfo() }
        findViewById<Button>(R.id.btnHudMenu1).setOnClickListener { tryAutoContainerSendInfo(86) }
        findViewById<Button>(R.id.btnHudMenu2).setOnClickListener { tryAutoContainerSendInfo(87) }
        findViewById<Button>(R.id.btnSmartNavGreen).setOnClickListener { tryAutoContainerSendInfo(56) }
        findViewById<Button>(R.id.btnSmartNavWhite).setOnClickListener { tryAutoContainerSendInfo(57) }
        findViewById<Button>(R.id.btnSmartNavOff).setOnClickListener { tryAutoContainerSendInfo(58) }
        findViewById<Button>(R.id.btnHudSweep).setOnClickListener { sweepAutoContainerFeatures() }
        findViewById<Button>(R.id.btnCveCanCollect).setOnClickListener { probeCanDataCollect() }
        findViewById<Button>(R.id.btnCveLogcat).setOnClickListener { probeSdcardLogs() }
        findViewById<Button>(R.id.btnSdkDevices).setOnClickListener { probeBydSdkDevices() }
        findViewById<Button>(R.id.btnSetFeat86).setOnClickListener { setInstrumentFeature(86, 1) }
        findViewById<Button>(R.id.btnSetFeat87).setOnClickListener { setInstrumentFeature(87, 1) }
        findViewById<Button>(R.id.btnGetFeat86).setOnClickListener { getInstrumentFeatures() }
        findViewById<Button>(R.id.btnSetFeatSweep).setOnClickListener { sweepSetInstrumentFeatures() }
        findViewById<Button>(R.id.btnMoveTask).setOnClickListener { tryMoveTaskToDisplay() }
        findViewById<Button>(R.id.btnAutoRun).setOnClickListener { autoRunAll() }
        findViewById<Button>(R.id.btnExportUsb).setOnClickListener { exportToUsb() }
        findViewById<Button>(R.id.btnClearProbe).setOnClickListener {
            FileLogger.clear()
            logView.text = ""
            log("cleared")
        }
    }

    private fun enumDisplays() {
        log("=== ENUM ALL DISPLAYS ===")
        try {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displays = dm.displays
            log("Display count: ${displays.size}")
            for (d in displays) {
                val mi = DisplayMetrics()
                d.getMetrics(mi)
                val ri = DisplayMetrics()
                d.getRealMetrics(ri)
                log("  ID=${d.displayId} name='${d.name}' valid=${d.isValid} state=${d.state}")
                log("    metrics=${mi.widthPixels}x${mi.heightPixels} dpi=${mi.densityDpi}")
                log("    real=${ri.widthPixels}x${ri.heightPixels} refresh=${d.refreshRate}Hz")
                log("    flags=0x${Integer.toHexString(d.flags)}")
            }

            log("--- Search for HUD/cluster displays ---")
            for (d in displays) {
                val n = d.name.lowercase()
                if (n.contains("cluster") || n.contains("hud") || n.contains("instrument") ||
                    n.contains("projection") || n.contains("fission") || n.contains("screen") && d.displayId != 0) {
                    log("  *** CANDIDATE: ID=${d.displayId} name='${d.name}' ${d.width}x${d.height}")
                }
            }
        } catch (t: Throwable) {
            log("DisplayManager ERR ${t.javaClass.simpleName}: ${t.message}")
        }

        log("--- try shell context displays ---")
        try {
            val shellCtx = createPackageContext("com.android.shell", 0)
            val dm2 = shellCtx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displays2 = dm2.displays
            log("Shell context displays: ${displays2.size}")
            for (d in displays2) {
                log("  ID=${d.displayId} name='${d.name}' ${d.width}x${d.height}")
            }
        } catch (t: Throwable) {
            log("shell context ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun getHudProps() {
        log("=== HUD SYSTEM PROPERTIES ===")
        try {
            val spCls = Class.forName("android.os.SystemProperties")
            val get = spCls.getMethod("get", String::class.java)
            val props = arrayOf(
                "ro.vehicle.type",
                "ro.build.display.id",
                "persist.byd.hud.type",
                "persist.byd.arhud.type",
                "ro.byd.hud.support",
                "ro.byd.arhud.support",
                "ro.byd.instrument.type",
                "ro.byd.cluster.type",
                "ro.byd.display.type",
                "ro.byd.model.name",
                "ro.byd.vehicle.model",
                "ro.byd.car.series",
                "ro.byd.di.version",
                "ro.byd.dilink.version",
                "persist.sys.byd.hud.enable",
                "persist.sys.byd.arhud.enable",
                "ro.hardware.chipname",
                "ro.hardware.display",
                "ro.soc.model",
                "ro.product.first_api_level",
                "ro.byd.hud.resolution",
                "ro.byd.arhud.resolution",
                "ro.byd.cluster.resolution",
                "ro.byd.navidata.source",
                "persist.byd.navidata.hud",
                "ro.byd.wuhu.support",
                "ro.byd.whud.support",
                "ro.byd.instrument.hud",
                "ro.build.version.di",
                "ro.byd.display.cluster",
                "ro.byd.display.hud"
            )
            for (p in props) {
                try {
                    val v = get.invoke(null, p) as? String
                    if (!v.isNullOrEmpty() && v != "unknown") {
                        log("  $p = $v")
                    }
                } catch (_: Throwable) {}
            }

            log("--- grep byd/hud props ---")
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", "getprop | grep -iE 'byd.*hud|byd.*arhud|byd.*cluster|byd.*instrument|byd.*navi|vehicle.*type|byd.*display|byd.*whud|byd.*wuhu|byd.*model|byd.*series'"))
                val out = proc.inputStream.bufferedReader().readText().trim()
                if (out.isNotEmpty()) {
                    out.lines().take(60).forEach { log("  $it") }
                }
                proc.waitFor()
            } catch (t: Throwable) {
                log("  getprop grep ERR ${t.javaClass.simpleName}")
            }
        } catch (t: Throwable) {
            log("SystemProperties ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun probeInstrument() {
        log("=== PROBE BYDAutoInstrumentDevice + FEATURES ===")
        val classNames = arrayOf(
            "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice",
            "android.hardware.bydauto.BYDAutoInstrumentDevice"
        )
        for (name in classNames) {
            try {
                val c = Class.forName(name)
                log("FOUND: $name")
                try {
                    val getInstance = c.getMethod("getInstance", Context::class.java)
                    val dev = getInstance.invoke(null, this@ProbeActivity)
                    log("  getInstance(ctx) = $dev")
                    if (dev != null) {
                        try {
                            val fc = c.getMethod("getFeatureList")
                            val features = fc.invoke(dev) as? IntArray
                            log("  === FEATURE LIST (${features?.size ?: 0} features) ===")
                            features?.forEach { fid ->
                                val hex = "0x${Integer.toHexString(fid)}"
                                val label = featureLabel(fid)
                                log("    $hex $label")
                            }
                        } catch (t: Throwable) {
                            log("  getFeatureList ERR ${t.javaClass.simpleName}: ${t.message}")
                        }
                        try {
                            val sn = c.getMethod("sendSimpleGuidanceInfo", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                            log("  sendSimpleGuidanceInfo method: $sn")
                        } catch (t: Throwable) {
                            log("  sendSimpleGuidanceInfo method ERR ${t.javaClass.simpleName}: ${t.message}")
                        }
                        try {
                            val sns = c.getMethod("sendAutoNaviStatus", Int::class.javaPrimitiveType)
                            log("  sendAutoNaviStatus method: $sns")
                        } catch (t: Throwable) {
                            log("  sendAutoNaviStatus method ERR ${t.javaClass.simpleName}: ${t.message}")
                        }
                    }
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  getInstance ERR ${c2.javaClass.simpleName}: ${c2.message}")
                }
            } catch (e: ClassNotFoundException) {
                log("MISS: $name")
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun probeAllBydDevices() {
        log("=== ALL BYD AUTO DEVICE CLASSES ===")
        val deviceClasses = arrayOf(
            "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice",
            "android.hardware.bydauto.test.BYDAutoTestDevice",
            "android.hardware.bydauto.setting.BYDAutoSettingDevice",
            "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice",
            "android.hardware.bydauto.driving.BYDAutoDrivingDevice",
            "android.hardware.bydauto.energy.BYDAutoEnergyDevice",
            "android.hardware.bydauto.aircondition.BYDAutoAirConditionDevice",
            "android.hardware.bydauto.door.BYDAutoDoorDevice",
            "android.hardware.bydauto.seat.BYDAutoSeatDevice",
            "android.hardware.bydauto.light.BYDAutoLightDevice",
            "android.hardware.bydauto.acoustics.BYDAutoAcousticsDevice",
            "android.hardware.bydauto.tirepressure.BYDAutoTirePressureDevice",
            "android.hardware.bydauto.radar.BYDAutoRadarDevice",
            "android.hardware.bydauto.camera.BYDAutoCameraDevice",
            "android.hardware.bydauto.gateway.BYDAutoGatewayDevice",
            "android.hardware.bydauto.maintenance.BYDAutoMaintenanceDevice",
            "android.hardware.bydauto.hud.BYDAutoHudDevice",
            "android.hardware.bydauto.arhud.BYDAutoArhudDevice",
            "android.hardware.bydauto.display.BYDAutoDisplayDevice",
            "android.hardware.bydauto.projection.BYDAutoProjectionDevice",
            "android.hardware.bydauto.cluster.BYDAutoClusterDevice",
            "android.hardware.bydauto.navi.BYDAutoNaviDevice"
        )
        for (cls in deviceClasses) {
            try {
                val c = Class.forName(cls)
                val simple = c.simpleName
                log("FOUND: $simple")
                try {
                    val gi = c.getMethod("getInstance", Context::class.java)
                    val dev = gi.invoke(null, this@ProbeActivity)
                    if (dev != null) {
                        log("  getInstance() = OK -> ${dev.javaClass.name}")
                        try {
                            val fl = c.getMethod("getFeatureList")
                            val features = fl.invoke(dev) as? IntArray
                            if (features != null && features.isNotEmpty()) {
                                val hudRelated = features.filter { f ->
                                    val h = Integer.toHexString(f).lowercase()
                                    h.startsWith("34c") || h.startsWith("28c") || h.startsWith("4c1") ||
                                    h.contains("hud") || h.contains("arhud") || h.contains("navi") ||
                                    h.startsWith("43e") || h.startsWith("43f") || h.startsWith("40c")
                                }
                                if (hudRelated.isNotEmpty()) {
                                    log("  *** HUD-RELATED features: ${hudRelated.size} ***")
                                    hudRelated.forEach { log("    0x${Integer.toHexString(it)} ${featureLabel(it)}") }
                                }
                                log("  total features: ${features.size}")
                            }
                        } catch (_: Throwable) {}
                    } else {
                        log("  getInstance() = null (no permission?)")
                    }
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  getInstance ERR ${c2.javaClass.simpleName}: ${c2.message?.take(80)}")
                }
            } catch (_: ClassNotFoundException) {
                // skip
            }
        }
    }

    private fun featureLabel(fid: Int): String {
        val h = Integer.toHexString(fid).lowercase()
        return when {
            h.startsWith("34c0") -> "[ARHUD]"
            h.startsWith("28c0") -> "[METER/CLUSTER]"
            h.startsWith("40c0") -> "[METER_MASK]"
            h.startsWith("43e0") -> "[CAN_NAVI_STATE]"
            h.startsWith("43f0") -> "[CAN_TURN_ICON]"
            h.startsWith("4c10") -> "[CANDIDATE_HUD]"
            h.startsWith("aa00") -> "[RAW_CAN_WRITE]"
            else -> ""
        }
    }

    private fun bindProto() {
        if (protoBound && protoBinder != null) {
            log("ProtocolService already bound")
            return
        }
        log("=== BIND ProtocolService ===")
        val intent = Intent("action.com.byd.protocol.ProtocolService")
        intent.setPackage("com.byd.amapservice")
        val bound = bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                protoBinder = binder
                protoBound = true
                log("ProtocolService CONNECTED: $binder")
                try {
                    val desc = binder.interfaceDescriptor
                    log("  interface: $desc")
                } catch (t: Throwable) {
                    log("  descriptor ERR: ${t.message}")
                }
                statusView.text = "ProtocolService BOUND"
            }
            override fun onServiceDisconnected(name: ComponentName) {
                protoBinder = null
                protoBound = false
                log("ProtocolService DISCONNECTED")
            }
        }, Context.BIND_AUTO_CREATE)
        log("bindService returned=$bound")
    }

    private fun protoSendGuide() {
        val b = protoBinder
        if (b == null) { log("ProtocolService not bound — tap BIND first"); return }
        log("=== PROTO sendSimpleGuidanceInfo(3, 200) ===")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_PROTO)
            data.writeInt(3)
            data.writeInt(200)
            val ok = b.transact(0x0A, data, reply, 0)
            if (ok) {
                reply.readException()
                val res = reply.readString() ?: "<null>"
                log("  RESULT: $res")
            } else {
                log("  transact failed")
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            reply.recycle(); data.recycle()
        }
    }

    private fun protoSendNavi() {
        val b = protoBinder
        if (b == null) { log("ProtocolService not bound"); return }
        log("=== PROTO sendAutoNaviStatus(2) ===")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_PROTO)
            data.writeInt(2)
            val ok = b.transact(0x0B, data, reply, 0)
            if (ok) {
                reply.readException()
                val res = reply.readString() ?: "<null>"
                log("  RESULT: $res")
            } else {
                log("  transact failed")
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            reply.recycle(); data.recycle()
        }
    }

    private fun protoSetModel() {
        val b = protoBinder
        if (b == null) { log("ProtocolService not bound"); return }
        log("=== PROTO setProtocolModelData(pid=30000, act=0x80000000, op=3) ===")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_PROTO)
            data.writeInt(30000)
            data.writeInt(-2147483648)
            data.writeInt(3)
            val ok = b.transact(0x02, data, reply, 0)
            if (ok) {
                reply.readException()
                val res = reply.readInt()
                log("  RESULT: $res")
            } else {
                log("  transact failed")
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            reply.recycle(); data.recycle()
        }
    }

    @SuppressLint("PrivateApi")
    private fun probeContainerService() {
        log("=== PROBE getSystemService ===")
        for (name in listOf("auto_container", "Auto_container", "auto_Container")) {
            try {
                val svc = getSystemService(name)
                if (svc != null) {
                    log("GOT '$name' -> ${svc.javaClass.name}")
                    val methods = svc.javaClass.methods.take(20).map {
                        "${it.name}(${it.parameterTypes.map { t -> t.simpleName }.joinToString(",")})"
                    }
                    log("  methods: ${methods.joinToString("; ")}")
                } else {
                    log("'$name' -> null")
                }
            } catch (t: Throwable) {
                log("'$name' ERR ${t.javaClass.simpleName}: ${t.message}")
            }
        }
        log("--- ServiceManager direct ---")
        try {
            val smCls = Class.forName("android.os.ServiceManager")
            val checkSvc = smCls.getMethod("checkService", String::class.java)
            for (svcName in listOf("AutoContainerNative", "auto_container")) {
                val binder = checkSvc.invoke(null, svcName) as? IBinder
                log("checkService('$svcName') = $binder")
            }
        } catch (t: Throwable) {
            log("ServiceManager ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun probeAllServices() {
        log("=== ALL BYD/AUTO SERVICES ===")
        try {
            val smCls = Class.forName("android.os.ServiceManager")
            val listSvc = smCls.getMethod("listServices")
            val services = listSvc.invoke(null) as? Array<String> ?: return log("null")
            val keywords = listOf("byd", "auto", "container", "cluster", "hud", "instrument",
                "dilink", "diplus", "projection", "navi", "display", "car", "vehicle",
                "meter", "canbus", "gateway", "hmi", "screen", "arhud")
            services.filter { keywords.any { it.contains(it, ignoreCase = true) } }
                .sorted().forEach { log("  $it") }
        } catch (t: Throwable) {
            log("ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun readConfig() {
        log("=== container_comm_cfg.json ===")
        for (p in listOf("/system/etc/container_comm_cfg.json", "/product/etc/container_comm_cfg.json",
                         "/vendor/etc/container_comm_cfg.json", "/system_ext/etc/container_comm_cfg.json")) {
            try {
                val f = java.io.File(p)
                if (f.exists()) { log("FOUND: $p\n${f.readText().take(4000)}") }
            } catch (t: Throwable) { log("$p ERR: ${t.message}") }
        }
    }

    @SuppressLint("PrivateApi")
    private fun trySendInfo(type: Int, id: Int, extra: String) {
        log("=== DIRECT sendInfo($type, $id) ===")
        for (name in listOf("auto_container", "Auto_container")) {
            try {
                val svc = getSystemService(name) ?: continue
                val m = svc.javaClass.getMethod("sendInfo",
                    Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
                val res = m.invoke(svc, type, id, extra)
                log("  '$name' sendInfo($type,$id) = $res *** OK ***")
            } catch (se: SecurityException) {
                log("  '$name' SEC: ${se.message}")
            } catch (t: Throwable) {
                val c = t.cause ?: t
                log("  '$name' ERR ${c.javaClass.simpleName}: ${c.message}")
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun probeTest() {
        log("=== PROBE BYDAutoTestDevice ===")
        for (name in arrayOf("android.hardware.bydauto.test.BYDAutoTestDevice")) {
            try {
                val c = Class.forName(name)
                log("FOUND: $name")
                try {
                    val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
                    log("  getInstance = $dev")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  getInstance ERR ${c2.javaClass.simpleName}: ${c2.message}")
                }
            } catch (_: ClassNotFoundException) { log("MISS: $name") }
        }
    }

    private fun broadcastCan() {
        log("=== BROADCAST CAN ===")
        try {
            val i = Intent("AUTONAVI_STANDARD_BROADCAST_SEND")
            i.setPackage("com.byd.amapservice")
            i.putExtra("KEY_TYPE", 1107)
            i.putExtra("IS_BYD_MAP", 1)
            i.putExtra("TURN_KIND", 3)
            sendBroadcast(i)
            log("  AUTONAVI TURN_KIND=3 sent")
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryInstrMethod(method: String, a1: Int, a2: Int, a3: Int) {
        log("=== $method($a1, $a2, $a3) ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val m = when {
                a3 != 0 || a2 != 0 -> c.getMethod(method, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                a2 != 0 -> c.getMethod(method, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                else -> c.getMethod(method, Int::class.javaPrimitiveType)
            }
            val res = m.invoke(dev, a1, a2, a3)
            log("  $method($a1,$a2,$a3) = $res *** CHECK HUD ***")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  ERR ${c2.javaClass.simpleName}: ${c2.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryInstrStringMethod(method: String, s1: String, s2: String, s3: String) {
        log("=== $method('$s1','$s2','$s3') ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val m = c.getMethod(method, String::class.java, String::class.java, String::class.java)
            val res = m.invoke(dev, s1, s2, s3)
            log("  $method = $res *** CHECK HUD ***")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  ERR ${c2.javaClass.simpleName}: ${c2.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryInstrAddrMethod(method: String, type: Int, addr: String) {
        log("=== $method($type, '$addr') ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val m = c.getMethod(method, Int::class.javaPrimitiveType, String::class.java)
            val res = m.invoke(dev, type, addr)
            log("  $method($type,'$addr') = $res *** CHECK HUD ***")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  ERR ${c2.javaClass.simpleName}: ${c2.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryInstrSingleString(method: String, s: String) {
        log("=== $method('$s') ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val m = c.getMethod(method, String::class.java)
            val res = m.invoke(dev, s)
            log("  $method('$s') = $res *** CHECK HUD ***")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  ERR ${c2.javaClass.simpleName}: ${c2.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryInstrRestRoute() {
        log("=== sendRestRouteInfo(50,10,9999L) ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val m = c.getMethod("sendRestRouteInfo", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Long::class.javaPrimitiveType)
            val res = m.invoke(dev, 50, 10, 9999L)
            log("  sendRestRouteInfo(50,10,9999) = $res *** CHECK HUD ***")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  ERR ${c2.javaClass.simpleName}: ${c2.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryViewSwitch() {
        log("=== setViewSwitch / setDrivingInfoSwitch ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            for (v in 0..5) {
                try {
                    val m1 = c.getMethod("setViewSwitch", Int::class.javaPrimitiveType)
                    val r1 = m1.invoke(dev, v)
                    log("  setViewSwitch($v) = $r1")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  setViewSwitch($v) ERR ${c2.javaClass.simpleName}: ${c2.message?.take(60)}")
                }
            }
            for (v in 0..5) {
                try {
                    val m2 = c.getMethod("setDrivingInfoSwitch", Int::class.javaPrimitiveType)
                    val r2 = m2.invoke(dev, v)
                    log("  setDrivingInfoSwitch($v) = $r2")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  setDrivingInfoSwitch($v) ERR ${c2.javaClass.simpleName}: ${c2.message?.take(60)}")
                }
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryClearInfo() {
        log("=== clearInfo(0..5) ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val m = c.getMethod("clearInfo", Int::class.javaPrimitiveType)
            for (v in 0..5) {
                try {
                    val r = m.invoke(dev, v)
                    log("  clearInfo($v) = $r")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  clearInfo($v) ERR ${c2.javaClass.simpleName}: ${c2.message?.take(60)}")
                }
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryAutoContainerSendInfo(featureId: Int) {
        log("=== AutoContainerManager.sendInfo(1000, $featureId, '') ===")
        try {
            val acmCls = Class.forName("android.os.AutoContainerManager")
            val initRes = acmCls.getMethod("init", Context::class.java).invoke(null, this@ProbeActivity)
            log("  AutoContainerManager.init() = $initRes")
            val mgr = acmCls.getMethod("getAutoContainerManager").invoke(null)
            if (mgr == null) { log("  getAutoContainerManager = null"); return }
            log("  mgr class = ${mgr.javaClass.name}")
            val sendInfo = acmCls.getMethod("sendInfo",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            val res = sendInfo.invoke(mgr, 1000, featureId, "")
            log("  sendInfo(1000,$featureId,'') = $res *** CHECK HUD+CLUSTER ***")
        } catch (se: SecurityException) {
            log("  SecurityException: ${se.message}")
            log("  Trying direct binder transact...")
            tryDirectContainerTransact(1000, featureId)
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  ERR ${c2.javaClass.simpleName}: ${c2.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryDirectContainerTransact(type: Int, id: Int) {
        try {
            val smCls = Class.forName("android.os.ServiceManager")
            val getService = smCls.getMethod("getService", String::class.java)
            val binder = getService.invoke(null, "auto_container") as? IBinder
            if (binder == null) {
                log("  auto_container binder null, trying AutoContainerNative...")
                val binder2 = getService.invoke(null, "AutoContainerNative") as? IBinder
                if (binder2 == null) { log("  both null"); return }
                doContainerTransact(binder2, type, id)
            } else {
                doContainerTransact(binder, type, id)
            }
        } catch (t: Throwable) {
            log("  direct transact ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun doContainerTransact(binder: IBinder, type: Int, id: Int) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("android.os.IAutoContainer")
            data.writeInt(type)
            data.writeInt(id)
            data.writeString("")
            val ok = binder.transact(0x1, data, reply, 0)
            reply.readException()
            val res = reply.readInt()
            log("  transact(1, $type, $id) ok=$ok res=$res *** CHECK HUD ***")
        } catch (t: Throwable) {
            log("  transact ERR ${t.javaClass.simpleName}: ${t.message?.take(80)}")
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    @SuppressLint("PrivateApi")
    private fun sweepAutoContainerFeatures() {
        log("=== SWEEP AutoContainerManager.sendInfo(1000, 0..50) ===")
        try {
            val acmCls = Class.forName("android.os.AutoContainerManager")
            val initRes = acmCls.getMethod("init", Context::class.java).invoke(null, this@ProbeActivity)
            val mgr = acmCls.getMethod("getAutoContainerManager").invoke(null)
            if (mgr == null) { log("  mgr null"); return }
            val sendInfo = acmCls.getMethod("sendInfo",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            for (fid in 0..50) {
                try {
                    val res = sendInfo.invoke(mgr, 1000, fid, "")
                    log("  sendInfo(1000,$fid) = $res")
                } catch (se: SecurityException) {
                    log("  sendInfo(1000,$fid) SEC")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    val msg = c2.message?.take(40) ?: ""
                    log("  sendInfo(1000,$fid) ERR ${c2.javaClass.simpleName}:$msg")
                }
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun probeCanDataCollect() {
        log("=== CVE-2025-28169: CanDataCollect probe ===")
        try {
            val pm = packageManager
            for (pkg in listOf("com.byd.candatacollect", "com.byd.candatacollection", "com.byd.can_data_collect")) {
                try {
                    val info = pm.getPackageInfo(pkg, 0)
                    log("  PACKAGE FOUND: $pkg ver=${info.versionName} uid=${info.applicationInfo?.uid}")
                } catch (_: Throwable) { log("  package NOT found: $pkg") }
            }
            val canApk = java.io.File("/system/priv-app/CanDataCollect/CanDataCollect.apk")
            log("  /system/priv-app/CanDataCollect/ exists=${canApk.exists()}")
            if (canApk.exists()) log("  size=${canApk.length()}")

            val canApk2 = java.io.File("/system/priv-app/CanDataCollect.apk")
            log("  /system/priv-app/CanDataCollect.apk exists=${canApk2.exists()}")

            val receivers = pm.queryBroadcastReceivers(
                Intent("com.byd.data_collection_notify"), 0)
            log("  receivers for com.byd.data_collection_notify: ${receivers.size}")
            for (r in receivers) {
                log("    pkg=${r.activityInfo.packageName} cls=${r.activityInfo.name}")
            }

            try {
                val smCls = Class.forName("android.os.ServiceManager")
                val getService = smCls.getMethod("getService", String::class.java)
                for (svc in listOf("candatacollect", "CanDataCollect", "byd_can_data")) {
                    val b = getService.invoke(null, svc) as? IBinder
                    log("  ServiceManager.getService('$svc') = ${if (b != null) "FOUND ${b.interfaceDescriptor}" else "null"}")
                }
            } catch (t: Throwable) { log("  ServiceManager probe ERR: ${t.message}") }

            log("  *** CanDataCollect probe done — NO broadcast sent ***")
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun probeSdcardLogs() {
        log("=== CVE-2024-54728: /sdcard/logs probe ===")
        for (path in listOf(
            "/sdcard/logs", "/storage/emulated/0/logs",
            "/sdcard/Android/data/com.byd.diplus/files/logs",
            "/sdcard/byd_logs", "/storage/emulated/0/byd_logs",
            "/data/local/tmp", "/sdcard/debug_logs"
        )) {
            try {
                val dir = java.io.File(path)
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()
                    log("  $path EXISTS — ${files?.size ?: 0} files")
                    files?.sortedByDescending { it.lastModified() }?.take(10)?.forEach {
                        log("    ${it.name} size=${it.length()} mod=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(it.lastModified())}")
                    }
                } else {
                    log("  $path — not found")
                }
            } catch (t: Throwable) {
                log("  $path ERR: ${t.message?.take(50)}")
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun probeBydSdkDevices() {
        log("=== BYD SDK devices (Speed/Location/Energy/Climate) ===")
        val devices = listOf(
            "android.hardware.bydauto.speed.BYDAutoSpeedDevice",
            "android.hardware.bydauto.location.BYDAutoLocationDevice",
            "android.hardware.bydauto.energy.BYDAutoEnergyDevice",
            "android.hardware.bydauto.climate.BYDAutoClimateDevice",
            "android.hardware.bydauto.charging.BYDAutoChargingDevice",
            "android.hardware.bydauto.statistic.BYDAutoStatisticDevice",
            "android.hardware.bydauto.sensor.BYDAutoSensorDevice",
            "android.hardware.bydauto.setting.BYDAutoSettingDevice",
            "android.hardware.bydauto.hud.BYDAutoHudDevice",
            "android.hardware.bydauto.arhud.BYDAutoArhudDevice",
            "android.hardware.bydauto.nav.BYDAutoNavDevice",
            "android.hardware.bydauto.navigation.BYDAutoNavigationDevice",
            "com.byd.devices.BYDAutoHudDevice",
            "com.byd.devices.BYDAutoArhudDevice",
            "com.byd.autosdk.devices.BYDAutoHudDevice"
        )
        for (clsName in devices) {
            try {
                val c = Class.forName(clsName)
                log("  FOUND: $clsName")
                val methods = c.declaredMethods.filter { it.name != "hashCode" && it.name != "equals" }
                    .take(20).map { "${it.name}(${it.parameterTypes.map { it.simpleName }})" }
                log("    methods: ${methods.joinToString(", ")}")
                try {
                    val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
                    log("    getInstance = $dev")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("    getInstance ERR ${c2.javaClass.simpleName}: ${c2.message?.take(60)}")
                }
            } catch (_: ClassNotFoundException) {
                // skip
            } catch (t: Throwable) {
                log("  $clsName ERR ${t.javaClass.simpleName}: ${t.message?.take(60)}")
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun setInstrumentFeature(featureId: Int, value: Int) {
        log("=== setInstrumentFeatureValue($featureId, $value) ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val evtCls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val evt = evtCls.getConstructor().newInstance()
            evtCls.getField("intValue").set(evt, value)
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getMethod("set", IntArray::class.java, evtCls)
            val res = setMethod.invoke(dev, intArrayOf(featureId), evt)
            log("  set([$featureId], value=$value) = $res *** CHECK HUD ***")
        } catch (t: Throwable) {
            val c2 = t.cause ?: t
            log("  ERR ${c2.javaClass.simpleName}: ${c2.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun getInstrumentFeatures() {
        log("=== getInstrumentFeatureValue(80..95) ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val getMethod = absCls.getMethod("get", IntArray::class.java)
            for (fid in 80..95) {
                try {
                    val res = getMethod.invoke(dev, intArrayOf(fid))
                    log("  get([$fid]) = $res")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  get([$fid]) ERR ${c2.javaClass.simpleName}: ${c2.message?.take(60)}")
                }
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun sweepSetInstrumentFeatures() {
        log("=== SWEEP setInstrumentFeatureValue(0..90, 1) ===")
        try {
            val c = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            val dev = c.getMethod("getInstance", Context::class.java).invoke(null, this@ProbeActivity)
            if (dev == null) { log("  device null"); return }
            val evtCls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val setMethod = absCls.getMethod("set", IntArray::class.java, evtCls)
            for (fid in 0..90) {
                try {
                    val evt = evtCls.getConstructor().newInstance()
                    evtCls.getField("intValue").set(evt, 1)
                    val res = setMethod.invoke(dev, intArrayOf(fid), evt)
                    log("  set([$fid], 1) = $res")
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  set([$fid], 1) ERR ${c2.javaClass.simpleName}: ${c2.message?.take(40)}")
                }
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryMoveTaskToDisplay() {
        log("=== moveTaskToDisplay ===")
        try {
            val taskId = taskId
            log("  our taskId = $taskId")
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            for (d in dm.displays) {
                if (d.displayId == 0) continue
                try {
                    val atmCls = Class.forName("android.app.ActivityTaskManager")
                    val getService = atmCls.getMethod("getService")
                    val atm = getService.invoke(null)
                    val methods = atm.javaClass.methods
                    val moveMethod = methods.find {
                        (it.name == "moveRootTaskToDisplay" || it.name == "moveTaskToDisplay") &&
                        it.parameterTypes.size == 2
                    }
                    if (moveMethod != null) {
                        val res = moveMethod.invoke(atm, taskId, d.displayId)
                        log("  ${moveMethod.name}($taskId, ${d.displayId}) = $res *** CHECK ALL SCREENS ***")
                    } else {
                        log("  moveTaskToDisplay method NOT found on ATM")
                    }
                } catch (t: Throwable) {
                    val c2 = t.cause ?: t
                    log("  display ${d.displayId} ERR ${c2.javaClass.simpleName}: ${c2.message?.take(60)}")
                }
            }
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun autoRunAll() {
        log("========================================")
        log("=== AUTO RUN ALL PROBES ===")
        log("========================================")
        Thread {
            val probes = listOf<() -> Unit>(
                { enumDisplays() },
                { getHudProps() },
                { probeAllBydDevices() },
                { probeBydSdkDevices() },
                { probeCanDataCollect() },
                { probeSdcardLogs() },
                { probeAllServices() },
                { getInstrumentFeatures() },
            )
            for ((i, probe) in probes.withIndex()) {
                handler.post {
                    log("--- Probe ${i + 1}/${probes.size} ---")
                    statusView.text = "Running probe ${i + 1}/${probes.size}..."
                }
                Thread.sleep(500)
                handler.post(probe)
                Thread.sleep(2000)
            }
            handler.post {
                log("========================================")
                log("=== ALL PROBES COMPLETE ===")
                log("=== Log file: ${FileLogger.getFile()?.absolutePath} ===")
                log("=== Press COPY LOG TO USB ===")
                statusView.text = "ALL DONE — copy log to USB"
            }
        }.start()
    }

    private fun exportToUsb() {
        log("=== EXPORT LOG ===")
        try {
            val src = FileLogger.getFile()
            if (src == null || !src.exists()) { log("  no log file"); return }
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())

            // Always copy to internal app data (accessible via file manager)
            val intDir = getExternalFilesDir(null) ?: filesDir
            val intDst = java.io.File(intDir, "yandex_hud_probe_$ts.log")
            src.inputStream().use { inp -> intDst.outputStream().use { out -> inp.copyTo(out) } }
            log("  APP DATA: ${intDst.absolutePath} (${intDst.length()} bytes)")

            // Try USB
            val usbBase = java.io.File("/storage/4A21-0000/Download")
            if (usbBase.exists()) {
                val usbDst = java.io.File(usbBase, "yandex_hud_probe_$ts.log")
                src.inputStream().use { inp -> usbDst.outputStream().use { out -> inp.copyTo(out) } }
                log("  USB: ${usbDst.absolutePath} (${usbDst.length()} bytes)")
            } else {
                log("  USB: /storage/4A21-0000/Download not found, skipped")
            }

            // Try /sdcard/Download
            val sdDl = java.io.File("/storage/emulated/0/Download")
            if (sdDl.exists()) {
                val sdDst = java.io.File(sdDl, "yandex_hud_probe_$ts.log")
                src.inputStream().use { inp -> sdDst.outputStream().use { out -> inp.copyTo(out) } }
                log("  SDCARD: ${sdDst.absolutePath} (${sdDst.length()} bytes)")
            }

            statusView.text = "Log exported — see log for paths"
        } catch (t: Throwable) {
            log("  ERR ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    override fun onDestroy() {
        if (protoBound) {
            try { unbindService(object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {}
                override fun onServiceDisconnected(name: ComponentName) {}
            }) } catch (_: Throwable) {}
        }
        super.onDestroy()
    }

    private fun log(s: String) {
        FileLogger.write("Probe", s)
        handler.post {
            logView.append(s + "\n")
            val scroll = logView.parent as? ScrollView
            scroll?.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }
}
