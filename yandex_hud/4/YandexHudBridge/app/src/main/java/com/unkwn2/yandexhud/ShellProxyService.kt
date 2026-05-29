package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.net.URL
import java.net.URLClassLoader

class ShellProxyService : Binder() {
    companion object {
        private const val TAG = "ShellProxySvc"
        private const val DESC = "com.unkwn2.yandexhud.IHudControl"
        private const val TX_SET_FEATURE = 1
        private const val TX_GET_FEATURE = 2
        private const val TX_SEND_GUIDANCE = 3
        private const val TX_SEND_NAVI_STATUS = 4
        private const val TX_SCRAP = 5
        private const val TX_PING = 6
        private const val TX_PROBE = 7
        private const val TX_SEND_INFO = 8
        private const val TX_TEST_SET = 9
        private const val TX_RUN_SHELL = 11
        private const val TX_SET_SETTING_FEATURE = 12

        private val INSTR_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice",
            "android.hardware.bydauto.BYDAutoInstrumentDevice",
            "com.byd.auto.instrument.BYDAutoInstrumentDevice",
            "com.byd.auto.sdk.BYDAutoInstrumentDevice"
        )
        private val ABS_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.AbsBYDAutoDevice",
            "com.byd.auto.AbsBYDAutoDevice"
        )
        private val EVENT_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.BYDAutoEventValue",
            "com.byd.auto.BYDAutoEventValue"
        )
        private val TEST_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.test.BYDAutoTestDevice",
            "com.byd.auto.test.BYDAutoTestDevice"
        )
        private val CONTAINER_CLASS_NAMES = arrayOf(
            "android.os.AutoContainerManager",
            "com.byd.os.AutoContainerManager"
        )
        private val SETTING_CLASS_NAMES = arrayOf(
            "com.byd.protocol.canbus.protocol.setting.SettingDevice",
            "com.byd.protocol.canbus.protocol.cabin.SettingDevice",
            "com.byd.kit.canbus.protocol.setting.SettingDevice",
            "com.byd.dilink.protocol.canbus.protocol.SettingDevice",
            "android.hardware.bydauto.setting.BYDAutoSettingDevice",
            "android.hardware.bydauto.BYDAutoSettingDevice",
            "com.byd.hardware.bydauto.setting.BYDAutoSettingDevice",
            "com.byd.auto.setting.BYDAutoSettingDevice"
        )

        @SuppressLint("PrivateApi")
        fun runAsShell() {
            Log.i(TAG, "ShellProxy starting via app_process...")
            flog("ShellProxy starting... PID=${android.os.Process.myPid()} UID=${android.os.Process.myUid()}")

            val context = getSystemContext()
            if (context == null) {
                Log.e(TAG, "Failed to get system context")
                flog("FAILED: system context is null")
                return
            }
            flog("systemCtx OK: ${context.javaClass.name}")

            val service = ShellProxyService()
            service.probeClasses()
            service.initDevice(context)
            flog("initDevice done: instrDevice=${service.instrDevice != null} instrCls=${service.instrCls?.name}")

            val intent = Intent("com.unkwn2.yandexhud.PROXY_CONNECTED").apply {
                setPackage("com.unkwn2.yandexhud")
                putExtra("proxy_binder", ProxyBinderParcelable(service))
            }

            try {
                context.sendBroadcast(intent)
                flog("PROXY_CONNECTED broadcast sent via systemCtx")
            } catch (t: Throwable) {
                flog("broadcast via systemCtx ERR ${t.javaClass.simpleName}: ${t.message}")
            }

            try {
                val appCtx = context.createPackageContext("com.unkwn2.yandexhud", 0)
                appCtx.sendBroadcast(intent)
                flog("PROXY_CONNECTED broadcast sent via appCtx fallback")
            } catch (t: Throwable) {
                flog("broadcast via appCtx ERR ${t.javaClass.simpleName}: ${t.message}")
            }

            writeHandshakeFile()

            flog("entering Looper.loop()")
            Log.i(TAG, "Entering Looper.loop()")
            android.os.Looper.loop()
        }

        private fun writeHandshakeFile() {
            try {
                val dir = File("/storage/emulated/0/Android/data/com.unkwn2.yandexhud/files")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "proxy_handshake.txt")
                PrintWriter(FileWriter(f, false)).use { pw: PrintWriter ->
                    pw.println("pid=${android.os.Process.myPid()}")
                    pw.println("uid=${android.os.Process.myUid()}")
                    pw.println("ts=${System.currentTimeMillis()}")
                    pw.println("status=READY")
                }
                flog("handshake file written: ${f.absolutePath}")
            } catch (t: Throwable) {
                flog("handshake file ERR ${t.javaClass.simpleName}: ${t.message}")
            }
        }

        @SuppressLint("PrivateApi")
        private fun getSystemContext(): android.content.Context? {
            return try {
                val atClass = Class.forName("android.app.ActivityThread")
                val systemMain = atClass.getDeclaredMethod("systemMain")
                val activityThread = systemMain.invoke(null)
                val getSysCtx = atClass.getDeclaredMethod("getSystemContext")
                val ctx = getSysCtx.invoke(activityThread)
                ctx as? android.content.Context
            } catch (t: Throwable) {
                Log.e(TAG, "getSystemContext failed", t)
                flog("getSystemContext ERR ${t.javaClass.simpleName}: ${t.message}")
                null
            }
        }

        private fun flog(msg: String) {
            FileLogger.write(TAG, msg)
            Log.i(TAG, msg)
        }
    }

    private var instrDevice: Any? = null
    private var instrCls: Class<*>? = null
    private var settingDevice: Any? = null
    private var settingCls: Class<*>? = null
    private var testDevice: Any? = null
    private var testCls: Class<*>? = null
    private var containerMgr: Any? = null
    private var containerCls: Class<*>? = null
    private var systemCtx: android.content.Context? = null
    private var probeResult: String = "not probed"

    @SuppressLint("PrivateApi")
    private fun probeClasses() {
        val sb = StringBuilder()
        sb.append("=== CLASS PROBE ===\n")

        for (name in INSTR_CLASS_NAMES) {
            try {
                val c = Class.forName(name)
                sb.append("INSTR FOUND: $name\n")
                sb.append("  methods: ${c.methods.map { it.name }.take(30).joinToString(", ")}\n")
            } catch (e: ClassNotFoundException) {
                sb.append("INSTR MISS:  $name\n")
            } catch (e: Throwable) {
                sb.append("INSTR ERR:   $name -> ${e.javaClass.simpleName}: ${e.message}\n")
            }
        }
        for (name in ABS_CLASS_NAMES) {
            try {
                Class.forName(name)
                sb.append("ABS FOUND:   $name\n")
            } catch (e: ClassNotFoundException) {
                sb.append("ABS MISS:    $name\n")
            }
        }
        for (name in EVENT_CLASS_NAMES) {
            try {
                Class.forName(name)
                sb.append("EVT FOUND:   $name\n")
            } catch (e: ClassNotFoundException) {
                sb.append("EVT MISS:    $name\n")
            }
        }
        for (name in TEST_CLASS_NAMES) {
            try {
                val c = Class.forName(name)
                sb.append("TEST FOUND:  $name\n")
                sb.append("  methods: ${c.methods.map { it.name }.take(20).joinToString(", ")}\n")
            } catch (e: ClassNotFoundException) {
                sb.append("TEST MISS:   $name\n")
            } catch (e: Throwable) {
                sb.append("TEST ERR:    $name -> ${e.javaClass.simpleName}: ${e.message}\n")
            }
        }
        for (name in CONTAINER_CLASS_NAMES) {
            try {
                val c = Class.forName(name)
                sb.append("CONTAINER FOUND: $name\n")
                sb.append("  methods: ${c.methods.map { it.name }.take(20).joinToString(", ")}\n")
            } catch (e: ClassNotFoundException) {
                sb.append("CONTAINER MISS:  $name\n")
            } catch (e: Throwable) {
                sb.append("CONTAINER ERR:   $name -> ${e.javaClass.simpleName}: ${e.message}\n")
            }
        }

        try {
            val bmm = URLClassLoader(arrayOf(URL("file:///system/framework/bmmcamera.jar")), null)
            bmm.loadClass("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            sb.append("bmmcamera.jar: INSTR class present\n")
        } catch (e: Throwable) {
            sb.append("bmmcamera.jar: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        try {
            val fw = Class.forName("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
            sb.append("framework.jar: INSTR class present\n")
        } catch (e: Throwable) {
            sb.append("framework.jar: ${e.javaClass.simpleName}: ${e.message}\n")
        }

        probeResult = sb.toString().trimEnd()
        flog(probeResult)
    }

    @SuppressLint("PrivateApi")
    fun initDevice(ctx: android.content.Context): Boolean {
        systemCtx = ctx
        val ok = ensureDevice()
        flog("initDevice: ensureDevice=$ok device=${instrDevice != null}")
        initSettingDevice(ctx)
        initTestDevice(ctx)
        initContainerManager(ctx)
        return ok
    }

    @SuppressLint("PrivateApi")
    private fun initTestDevice(ctx: android.content.Context) {
        for (className in TEST_CLASS_NAMES) {
            try {
                val klass = Class.forName(className)
                testCls = klass
                val getInstance = klass.getMethod("getInstance", android.content.Context::class.java)
                testDevice = getInstance.invoke(null, ctx)
                if (testDevice != null) {
                    flog("initTestDevice OK via $className")
                    return
                }
            } catch (t: Throwable) {
                flog("initTestDevice $className ERR ${t.javaClass.simpleName}: ${t.message}")
            }
        }
        flog("initTestDevice: no test device available")
    }

    @SuppressLint("PrivateApi")
    private fun initContainerManager(ctx: android.content.Context) {
        for (className in CONTAINER_CLASS_NAMES) {
            try {
                val klass = Class.forName(className)
                containerCls = klass
                try {
                    val init = klass.getMethod("init", android.content.Context::class.java)
                    init.invoke(null, ctx)
                    val getMgr = klass.getMethod("getAutoContainerManager")
                    containerMgr = getMgr.invoke(null)
                    if (containerMgr != null) {
                        flog("initContainer OK via $className init+getMgr")
                        return
                    }
                } catch (t: Throwable) {
                    flog("initContainer init+getMgr ERR ${t.javaClass.simpleName}: ${t.message}")
                }
                try {
                    val svc = ctx.getSystemService("auto_container")
                    containerMgr = svc
                    if (svc != null && svc.javaClass.name == className) {
                        flog("initContainer OK via getSystemService(auto_container)")
                        return
                    }
                    if (svc != null) {
                        flog("initContainer: auto_container returned ${svc.javaClass.name}")
                        return
                    }
                } catch (t: Throwable) {
                    flog("initContainer auto_container ERR ${t.javaClass.simpleName}: ${t.message}")
                }
                try {
                    containerMgr = ctx.getSystemService("Auto_container")
                    if (containerMgr != null) {
                        flog("initContainer OK via getSystemService(Auto_container)")
                        return
                    }
                } catch (t: Throwable) {
                    flog("initContainer Auto_container ERR ${t.javaClass.simpleName}: ${t.message}")
                }
            } catch (e: ClassNotFoundException) {
                flog("initContainer: $className not found")
            }
        }
        flog("initContainer: no container manager available")
    }

    @SuppressLint("PrivateApi")
    private fun ensureDevice(): Boolean {
        if (instrDevice != null) return true

        for (className in INSTR_CLASS_NAMES) {
            try {
                val klass = Class.forName(className)
                instrCls = klass
                val getInstance = klass.getMethod("getInstance", android.content.Context::class.java)
                val ctx = systemCtx ?: Companion.getSystemContext()
                if (ctx == null) {
                    flog("ensureDevice: no context for $className")
                    continue
                }
                instrDevice = getInstance.invoke(null, ctx)
                if (instrDevice != null) {
                    flog("ensureDevice OK via $className: $instrDevice")
                    return true
                }
            } catch (t: Throwable) {
                flog("ensureDevice $className ERR ${t.javaClass.simpleName}: ${t.message}")
            }
        }

        flog("ensureDevice FAILED: all class names exhausted")
        return false
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        when (code) {
            IBinder.INTERFACE_TRANSACTION -> {
                reply?.writeString(DESC)
                return true
            }
            TX_PING -> {
                data.enforceInterface(DESC)
                reply?.writeNoException()
                val extra = if (instrDevice != null) " (device OK)" else " (NO device)"
                reply?.writeString("pong from ShellProxy$extra")
                return true
            }
            TX_PROBE -> {
                data.enforceInterface(DESC)
                reply?.writeNoException()
                reply?.writeString(probeResult)
                return true
            }
            TX_SET_FEATURE -> {
                data.enforceInterface(DESC)
                val featureId = data.readInt()
                val value = data.readInt()
                val result = doSetFeature(featureId, value)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_GET_FEATURE -> {
                data.enforceInterface(DESC)
                val featureId = data.readInt()
                val result = doGetFeature(featureId)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_SEND_GUIDANCE -> {
                data.enforceInterface(DESC)
                val turnKind = data.readInt()
                val dist = data.readInt()
                val result = doSendGuidance(turnKind, dist)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_SEND_NAVI_STATUS -> {
                data.enforceInterface(DESC)
                val status = data.readInt()
                val result = doSendNaviStatus(status)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_SCRAP -> {
                data.enforceInterface(DESC)
                val result = doScrap()
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_SEND_INFO -> {
                data.enforceInterface(DESC)
                val type = data.readInt()
                val id = data.readInt()
                val extra = data.readString() ?: ""
                val result = doSendInfo(type, id, extra)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_TEST_SET -> {
                data.enforceInterface(DESC)
                val featureId = data.readInt()
                val canBytes = data.createByteArray() ?: ByteArray(0)
                val result = doTestSet(featureId, canBytes)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_SET_SETTING_FEATURE -> {
                data.enforceInterface(DESC)
                val featureId = data.readInt()
                val value = data.readInt()
                val result = doSetSettingFeature(featureId, value)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            TX_RUN_SHELL -> {
                data.enforceInterface(DESC)
                val command = data.readString() ?: ""
                val result = doRunShell(command)
                reply?.writeNoException()
                reply?.writeString(result)
                return true
            }
            else -> return super.onTransact(code, data, reply, flags)
        }
    }

    @SuppressLint("PrivateApi")
    private fun resolveClass(names: Array<String>): Class<*>? {
        for (name in names) {
            try {
                return Class.forName(name)
            } catch (_: ClassNotFoundException) {}
        }
        return null
    }

    @SuppressLint("PrivateApi")
    private fun doSetFeature(featureId: Int, value: Int): String {
        if (!ensureDevice()) return "ERR: device not init (class probe failed)"
        return try {
            val absCls = resolveClass(ABS_CLASS_NAMES)
                ?: return "ERR: AbsBYDAutoDevice not found"
            val evCls = resolveClass(EVENT_CLASS_NAMES)
                ?: return "ERR: BYDAutoEventValue not found"
            val evCtor = evCls.declaredConstructors.firstOrNull {
                it.parameterTypes.isEmpty() || (it.parameterTypes.size == 1 && it.parameterTypes[0] == Int::class.java)
            } ?: return "ERR: no BYDAutoEventValue ctor"
            evCtor.isAccessible = true
            val evInst = if (evCtor.parameterTypes.isEmpty()) {
                evCtor.newInstance()
            } else {
                evCtor.newInstance(value)
            }
            if (evCtor.parameterTypes.isEmpty()) {
                val intField = evCls.getDeclaredField("intValue")
                intField.isAccessible = true
                intField.set(evInst, value)
            }
            val setM = absCls.declaredMethods.firstOrNull {
                it.name == "set" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == IntArray::class.java &&
                    it.parameterTypes[1] == evCls
            } ?: return "ERR: no set(int[], BYDAutoEventValue)"
            setM.isAccessible = true
            val res = setM.invoke(instrDevice, intArrayOf(featureId), evInst)
            "setFeature(0x${Integer.toHexString(featureId)}, $value) = $res"
        } catch (t: Throwable) {
            val c = t.cause ?: t
            "setFeature ERR ${c.javaClass.simpleName}: ${c.message}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun doGetFeature(featureId: Int): String {
        if (!ensureDevice()) return "ERR: device not init"
        return try {
            val absCls = resolveClass(ABS_CLASS_NAMES)
                ?: return "ERR: AbsBYDAutoDevice not found"
            val getM = absCls.declaredMethods.firstOrNull {
                it.name == "get" && it.parameterTypes.size == 1 && it.parameterTypes[0] == IntArray::class.java
            } ?: return "ERR: no get(int[])"
            getM.isAccessible = true
            val res = getM.invoke(instrDevice, intArrayOf(featureId))
            "getFeature(0x${Integer.toHexString(featureId)}) = $res"
        } catch (t: Throwable) {
            val c = t.cause ?: t
            "getFeature ERR ${c.javaClass.simpleName}: ${c.message}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun doSendGuidance(turnKind: Int, dist: Int): String {
        if (!ensureDevice()) return "ERR: device not init"
        return try {
            val m = instrCls!!.getMethod("sendSimpleGuidanceInfo", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            val res = m.invoke(instrDevice, turnKind, dist)
            "sendGuidance($turnKind, $dist) = $res"
        } catch (t: Throwable) {
            val c = t.cause ?: t
            "sendGuidance ERR ${c.javaClass.simpleName}: ${c.message}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun doSendNaviStatus(status: Int): String {
        if (!ensureDevice()) return "ERR: device not init"
        return try {
            val m = instrCls!!.getMethod("sendAutoNaviStatus", Int::class.javaPrimitiveType)
            val res = m.invoke(instrDevice, status)
            "sendNaviStatus($status) = $res"
        } catch (t: Throwable) {
            val c = t.cause ?: t
            "sendNaviStatus ERR ${c.javaClass.simpleName}: ${c.message}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun doScrap(): String {
        val sb = StringBuilder()
        sb.append("instrCls=").append(instrCls?.name).append("\n")
        sb.append("testCls=").append(testCls?.name).append("\n")
        sb.append("containerCls=").append(containerCls?.name).append("\n")
        sb.append("containerMgr=").append(containerMgr != null).append("\n")
        if (instrDevice != null) {
            try {
                val m = instrCls!!.getMethod("getFeatureList")
                val features = m.invoke(instrDevice) as? IntArray
                sb.append("features count=").append(features?.size ?: 0).append("\n")
                features?.take(50)?.forEach { sb.append("  0x").append(Integer.toHexString(it)).append("\n") }
            } catch (t: Throwable) {
                sb.append("getFeatureList ERR ${t.message}\n")
            }
            try {
                val m = instrCls!!.getMethod("getGetPermission")
                val perm = m.invoke(instrDevice)
                sb.append("getPerm=").append(perm).append("\n")
            } catch (t: Throwable) {
                sb.append("getPerm ERR ${t.message}\n")
            }
            try {
                val m = instrCls!!.getMethod("getSetPermission")
                val perm = m.invoke(instrDevice)
                sb.append("setPerm=").append(perm).append("\n")
            } catch (t: Throwable) {
                sb.append("setPerm ERR ${t.message}\n")
            }
        }
        if (testDevice != null) {
            try {
                sb.append("testMethods: ${testCls!!.methods.map { it.name }.take(20).joinToString(", ")}\n")
            } catch (_: Throwable) {}
        }
        if (containerMgr != null) {
            try {
                sb.append("containerMethods: ${containerMgr!!.javaClass.methods.map { it.name }.take(20).joinToString(", ")}\n")
            } catch (_: Throwable) {}
        }
        return sb.toString().trimEnd()
    }

    @SuppressLint("PrivateApi")
    private fun doSendInfo(type: Int, id: Int, extra: String): String {
        val mgr = containerMgr ?: return "ERR: AutoContainerManager not available"
        return try {
            val m = mgr.javaClass.getMethod("sendInfo", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            val res = m.invoke(mgr, type, id, extra)
            "sendInfo($type, $id, \"$extra\") = $res"
        } catch (t: Throwable) {
            val c = t.cause ?: t
            "sendInfo ERR ${c.javaClass.simpleName}: ${c.message}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun initSettingDevice(ctx: android.content.Context) {
        for (className in SETTING_CLASS_NAMES) {
            try {
                val klass = Class.forName(className)
                settingCls = klass
                val getInstance = klass.getMethod("getInstance", android.content.Context::class.java)
                settingDevice = getInstance.invoke(null, ctx)
                if (settingDevice != null) {
                    flog("initSettingDevice OK via $className")
                    return
                }
            } catch (t: Throwable) {
                flog("initSettingDevice $className ERR ${t.javaClass.simpleName}: ${t.message}")
            }
        }
        flog("initSettingDevice: no setting device available")
    }

    @SuppressLint("PrivateApi")
    private fun doSetSettingFeature(featureId: Int, value: Int): String {
        if (settingDevice == null) return "ERR: settingDevice not init"
        return try {
            val absCls = resolveClass(ABS_CLASS_NAMES) ?: return "ERR: AbsBYDAutoDevice not found"
            val evCls = resolveClass(EVENT_CLASS_NAMES) ?: return "ERR: BYDAutoEventValue not found"
            val evCtor = evCls.declaredConstructors.firstOrNull {
                it.parameterTypes.isEmpty()
            } ?: return "ERR: no BYDAutoEventValue empty ctor"
            evCtor.isAccessible = true
            val evInst = evCtor.newInstance()
            val intField = evCls.getDeclaredField("intValue")
            intField.isAccessible = true
            intField.set(evInst, value)
            val setM = absCls.getDeclaredMethod("set", IntArray::class.java, evCls)
            val res = setM.invoke(settingDevice, intArrayOf(featureId), evInst)
            "setSetting(0x${Integer.toHexString(featureId)}, $value) = $res"
        } catch (t: Throwable) {
            val c = t.cause ?: t
            "setSetting ERR ${c.javaClass.simpleName}: ${c.message}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun doRunShell(command: String): String {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val out = proc.inputStream.bufferedReader().readText().trim()
            val err = proc.errorStream.bufferedReader().readText().trim()
            val exit = proc.waitFor()
            val result = if (out.isNotEmpty()) out else err
            "shell(exit=$exit): $result"
        } catch (t: Throwable) {
            "shell ERR ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    @SuppressLint("PrivateApi")
    private fun doTestSet(featureId: Int, canBytes: ByteArray): String {
        if (testDevice == null) return "ERR: BYDAutoTestDevice not available"
        return try {
            val evCls = resolveClass(EVENT_CLASS_NAMES) ?: return "ERR: BYDAutoEventValue not found"
            val evCtor = evCls.declaredConstructors.firstOrNull { it.parameterTypes.isEmpty() }
                ?: return "ERR: no BYDAutoEventValue empty ctor"
            evCtor.isAccessible = true
            val evInst = evCtor.newInstance()
            val bufField = evCls.getDeclaredField("bufferDataValue")
            bufField.isAccessible = true
            bufField.set(evInst, canBytes)
            val setM = testCls!!.getMethod("set", IntArray::class.java, evCls)
            val res = setM.invoke(testDevice, intArrayOf(featureId), evInst)
            "testSet(0x${Integer.toHexString(featureId)}, ${canBytes.size}b) = $res"
        } catch (t: Throwable) {
            val c = t.cause ?: t
            "testSet ERR ${c.javaClass.simpleName}: ${c.message}"
        }
    }
}
