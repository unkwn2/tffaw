package com.unkwn2.yandexhud.proxy

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

class EntryPoint {
    companion object {
        private const val TAG = "HudProxy"
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
        private const val TX_NATIVE_SEND_INFO = 10

        private val INSTR_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice",
            "android.hardware.bydauto.BYDAutoInstrumentDevice"
        )
        private val ABS_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.AbsBYDAutoDevice"
        )
        private val EVENT_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.BYDAutoEventValue"
        )
        private val TEST_CLASS_NAMES = arrayOf(
            "android.hardware.bydauto.test.BYDAutoTestDevice"
        )
        private val CONTAINER_CLASS_NAMES = arrayOf(
            "android.os.AutoContainerManager"
        )

        @SuppressLint("PrivateApi")
        @JvmStatic
        fun main(args: Array<String>) {
            val pid = android.os.Process.myPid()
            val uid = android.os.Process.myUid()
            flog("=== EntryPoint START === PID=$pid UID=$uid")

            try {
                val context = getSystemContext()
                if (context == null) {
                    Log.e(TAG, "EntryPoint: system context failed")
                    flog("FATAL: system context null")
                    return
                }
                flog("systemCtx OK: ${context.javaClass.name}")

                val service = ProxyBinder(context)
                flog("ProxyBinder created: instr=${service.instrDeviceInfo} test=${service.testDeviceInfo} container=${service.containerInfo}")

                val intent = Intent("com.unkwn2.yandexhud.PROXY_CONNECTED").apply {
                    setPackage("com.unkwn2.yandexhud")
                    putExtra("proxy_binder", com.unkwn2.yandexhud.ProxyBinderParcelable(service))
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
                    flog("PROXY_CONNECTED broadcast sent via appCtx")
                } catch (t: Throwable) {
                    flog("broadcast via appCtx ERR ${t.javaClass.simpleName}: ${t.message}")
                }

                writeHandshakeFile()

                flog("entering Looper.loop()")
                android.os.Looper.prepareMainLooper()
                android.os.Looper.loop()
            } catch (t: Throwable) {
                flog("FATAL CRASH in main:")
                flog("  ${t.javaClass.name}: ${t.message}")
                t.stackTrace.take(20).forEach { flog("    at $it") }
                val cause = t.cause
                if (cause != null) {
                    flog("  Caused by: ${cause.javaClass.name}: ${cause.message}")
                    cause.stackTrace.take(10).forEach { flog("    at $it") }
                }
            }
        }

        private fun writeHandshakeFile() {
            try {
                val dir = File("/storage/emulated/0/Android/data/com.unkwn2.yandexhud/files")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "proxy_handshake.txt")
                PrintWriter(FileWriter(f, false)).use { pw ->
                    pw.println("pid=${android.os.Process.myPid()}")
                    pw.println("uid=${android.os.Process.myUid()}")
                    pw.println("ts=${System.currentTimeMillis()}")
                    pw.println("status=READY")
                }
                flog("handshake file written")
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
                getSysCtx.invoke(activityThread) as? android.content.Context
            } catch (t: Throwable) {
                flog("getSystemContext ERR ${t.javaClass.simpleName}: ${t.message}")
                null
            }
        }

        private fun flog(msg: String) {
            try {
                val dir = File("/storage/emulated/0/Android/data/com.unkwn2.yandexhud/files")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, "hud_proxy.log")
                PrintWriter(FileWriter(f, true)).use { pw ->
                    pw.println("[${System.currentTimeMillis()}] $msg")
                }
            } catch (_: Throwable) {}
            Log.i(TAG, msg)
        }
    }

    class ProxyBinder(ctx: android.content.Context) : Binder() {
        private var instrDevice: Any? = null
        private var instrCls: Class<*>? = null
        private var testDevice: Any? = null
        private var testCls: Class<*>? = null
        private var containerMgr: Any? = null
        private var containerCls: Class<*>? = null
        private var nativeContainerBinder: IBinder? = null
        private var nativeContainerProxy: Any? = null
        private var systemCtx: android.content.Context = ctx
        private var probeResult: String = "not probed"

        val instrDeviceInfo get() = if (instrDevice != null) "OK:${instrCls?.simpleName}" else "NULL"
        val testDeviceInfo get() = if (testDevice != null) "OK:${testCls?.simpleName}" else "NULL"
        val containerInfo get() = if (containerMgr != null) "OK:${containerMgr?.javaClass?.simpleName}" else "NULL"

        init {
            probeClasses()
            initAllDevices()
        }

        @SuppressLint("PrivateApi")
        private fun probeClasses() {
            val sb = StringBuilder("=== CLASS PROBE ===\n")
            val classLists = mapOf(
                "INSTR" to INSTR_CLASS_NAMES,
                "ABS" to ABS_CLASS_NAMES,
                "EVT" to EVENT_CLASS_NAMES,
                "TEST" to TEST_CLASS_NAMES,
                "CONTAINER" to CONTAINER_CLASS_NAMES
            )
            for ((label, names) in classLists) {
                for (name in names) {
                    try {
                        val c = Class.forName(name)
                        sb.append("$label FOUND: $name\n")
                        sb.append("  methods: ${c.methods.map { it.name }.take(25).joinToString(", ")}\n")
                    } catch (e: ClassNotFoundException) {
                        sb.append("$label MISS:  $name\n")
                    } catch (e: Throwable) {
                        sb.append("$label ERR:   $name -> ${e.javaClass.simpleName}: ${e.message}\n")
                    }
                }
            }
            probeResult = sb.toString().trimEnd()
            flog(probeResult)
        }

        @SuppressLint("PrivateApi")
        private fun initAllDevices() {
            for (className in INSTR_CLASS_NAMES) {
                try {
                    val klass = Class.forName(className)
                    instrCls = klass
                    val getInstance = klass.getMethod("getInstance", android.content.Context::class.java)
                    instrDevice = getInstance.invoke(null, systemCtx)
                    if (instrDevice != null) {
                        flog("instrDevice OK via $className")
                        break
                    }
                } catch (t: Throwable) {
                    flog("instrDevice $className ERR ${t.javaClass.simpleName}: ${t.message}")
                }
            }

            for (className in TEST_CLASS_NAMES) {
                try {
                    val klass = Class.forName(className)
                    testCls = klass
                    val getInstance = klass.getMethod("getInstance", android.content.Context::class.java)
                    testDevice = getInstance.invoke(null, systemCtx)
                    if (testDevice != null) {
                        flog("testDevice OK via $className")
                        break
                    }
                } catch (t: Throwable) {
                    flog("testDevice $className ERR ${t.javaClass.simpleName}: ${t.message}")
                }
            }

            for (className in CONTAINER_CLASS_NAMES) {
                try {
                    val klass = Class.forName(className)
                    containerCls = klass
                    try {
                        val init = klass.getMethod("init", android.content.Context::class.java)
                        init.invoke(null, systemCtx)
                        val getMgr = klass.getMethod("getAutoContainerManager")
                        containerMgr = getMgr.invoke(null)
                        if (containerMgr != null) {
                            flog("containerMgr OK via init+getAutoContainerManager")
                            break
                        }
                    } catch (t: Throwable) {
                        flog("container init+getMgr ERR ${t.javaClass.simpleName}: ${t.message}")
                    }
                    for (svcName in listOf("auto_container", "Auto_container")) {
                        try {
                            val svc = systemCtx.getSystemService(svcName)
                            containerMgr = svc
                            if (svc != null) {
                                flog("containerMgr OK via getSystemService($svcName) = ${svc.javaClass.name}")
                                break
                            }
                        } catch (t: Throwable) {
                            flog("container $svcName ERR ${t.javaClass.simpleName}: ${t.message}")
                        }
                    }
                    if (containerMgr != null) break
                } catch (e: ClassNotFoundException) {
                    flog("container $className not found")
                }
            }

            flog("initAllDevices done: instr=${instrDevice != null} test=${testDevice != null} container=${containerMgr != null}")
            initNativeContainer()
        }

        @SuppressLint("PrivateApi")
        private fun initNativeContainer() {
            try {
                val smClass = Class.forName("android.os.ServiceManager")
                val checkSvc = smClass.getMethod("checkService", String::class.java)
                val binder = checkSvc.invoke(null, "AutoContainerNative") as? IBinder
                if (binder == null) {
                    flog("initNativeContainer: AutoContainerNative binder is null")
                    return
                }
                nativeContainerBinder = binder
                try {
                    val stubCls = Class.forName("android.os.IAutoContainer\$Stub")
                    val asInterface = stubCls.getMethod("asInterface", IBinder::class.java)
                    nativeContainerProxy = asInterface.invoke(null, binder)
                    flog("initNativeContainer OK: IAutoContainer proxy obtained, cls=${nativeContainerProxy?.javaClass?.name}")
                } catch (t: Throwable) {
                    flog("initNativeContainer asInterface ERR ${t.javaClass.simpleName}: ${t.message}")
                }
            } catch (t: Throwable) {
                flog("initNativeContainer ERR ${t.javaClass.simpleName}: ${t.message}")
            }
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
                    val extra = buildString {
                        append(" instr=${instrDevice != null}")
                        append(" test=${testDevice != null}")
                        append(" container=${containerMgr != null}")
                    }
                    reply?.writeString("pong from EntryPoint$extra")
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
                TX_NATIVE_SEND_INFO -> {
                    data.enforceInterface(DESC)
                    val type = data.readInt()
                    val id = data.readInt()
                    val extra = data.readString() ?: ""
                    val result = doNativeSendInfo(type, id, extra)
                    reply?.writeNoException()
                    reply?.writeString(result)
                    return true
                }
                else -> return super.onTransact(code, data, reply, flags)
            }
        }

        @SuppressLint("PrivateApi")
        private fun doSetFeature(featureId: Int, value: Int): String {
            val dev = instrDevice ?: return "ERR: instrDevice not init"
            return try {
                val absCls = Class.forName(ABS_CLASS_NAMES[0])
                val evCls = Class.forName(EVENT_CLASS_NAMES[0])
                val evCtor = evCls.declaredConstructors.firstOrNull {
                    it.parameterTypes.isEmpty() || (it.parameterTypes.size == 1 && it.parameterTypes[0] == Int::class.java)
                } ?: return "ERR: no EventValue ctor"
                evCtor.isAccessible = true
                val evInst = if (evCtor.parameterTypes.isEmpty()) {
                    evCtor.newInstance().also {
                        val intField = evCls.getDeclaredField("intValue")
                        intField.isAccessible = true
                        intField.set(it, value)
                    }
                } else {
                    evCtor.newInstance(value)
                }
                val setM = absCls.declaredMethods.firstOrNull {
                    it.name == "set" && it.parameterTypes.size == 2 &&
                        it.parameterTypes[0] == IntArray::class.java &&
                        it.parameterTypes[1] == evCls
                } ?: return "ERR: no set(int[], EventValue)"
                setM.isAccessible = true
                val res = setM.invoke(dev, intArrayOf(featureId), evInst)
                "setFeature(0x${Integer.toHexString(featureId)}, $value) = $res"
            } catch (t: Throwable) {
                val c = t.cause ?: t
                "setFeature ERR ${c.javaClass.simpleName}: ${c.message}"
            }
        }

        @SuppressLint("PrivateApi")
        private fun doGetFeature(featureId: Int): String {
            val dev = instrDevice ?: return "ERR: instrDevice not init"
            return try {
                val absCls = Class.forName(ABS_CLASS_NAMES[0])
                val getM = absCls.declaredMethods.firstOrNull {
                    it.name == "get" && it.parameterTypes.size == 1 && it.parameterTypes[0] == IntArray::class.java
                } ?: return "ERR: no get(int[])"
                getM.isAccessible = true
                val res = getM.invoke(dev, intArrayOf(featureId))
                "getFeature(0x${Integer.toHexString(featureId)}) = $res"
            } catch (t: Throwable) {
                val c = t.cause ?: t
                "getFeature ERR ${c.javaClass.simpleName}: ${c.message}"
            }
        }

        @SuppressLint("PrivateApi")
        private fun doSendGuidance(turnKind: Int, dist: Int): String {
            val dev = instrDevice ?: return "ERR: instrDevice not init"
            val cls = instrCls ?: return "ERR: instrCls null"
            return try {
                val m = cls.getMethod("sendSimpleGuidanceInfo", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                val res = m.invoke(dev, turnKind, dist)
                "sendGuidance($turnKind, $dist) = $res"
            } catch (t: Throwable) {
                val c = t.cause ?: t
                "sendGuidance ERR ${c.javaClass.simpleName}: ${c.message}"
            }
        }

        @SuppressLint("PrivateApi")
        private fun doSendNaviStatus(status: Int): String {
            val dev = instrDevice ?: return "ERR: instrDevice not init"
            val cls = instrCls ?: return "ERR: instrCls null"
            return try {
                val m = cls.getMethod("sendAutoNaviStatus", Int::class.javaPrimitiveType)
                val res = m.invoke(dev, status)
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
            sb.append("nativeContainer=").append(nativeContainerProxy != null).append("\n")
            if (instrDevice != null && instrCls != null) {
                try {
                    val m = instrCls!!.getMethod("getFeatureList")
                    val features = m.invoke(instrDevice) as? IntArray
                    sb.append("features count=").append(features?.size ?: 0).append("\n")
                    features?.take(80)?.forEach { sb.append("  0x").append(Integer.toHexString(it)).append("\n") }
                } catch (t: Throwable) {
                    sb.append("getFeatureList ERR ${t.message}\n")
                }
                try {
                    val m = instrCls!!.getMethod("getGetPermission")
                    sb.append("getPerm=").append(m.invoke(instrDevice)).append("\n")
                } catch (_: Throwable) {}
                try {
                    val m = instrCls!!.getMethod("getSetPermission")
                    sb.append("setPerm=").append(m.invoke(instrDevice)).append("\n")
                } catch (_: Throwable) {}
            }
            if (testDevice != null && testCls != null) {
                try {
                    sb.append("testMethods: ${testCls!!.methods.map { "${it.name}(${it.parameterTypes.map { t -> t.simpleName }})" }.take(15).joinToString(", ")}\n")
                } catch (_: Throwable) {}
            }
            if (containerMgr != null) {
                try {
                    sb.append("containerMethods: ${containerMgr!!.javaClass.methods.map { "${it.name}(${it.parameterTypes.map { t -> t.simpleName }})" }.take(15).joinToString(", ")}\n")
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
        private fun doTestSet(featureId: Int, canBytes: ByteArray): String {
            val dev = testDevice ?: return "ERR: BYDAutoTestDevice not available"
            val cls = testCls ?: return "ERR: testCls null"
            return try {
                val evCls = Class.forName(EVENT_CLASS_NAMES[0])
                val evCtor = evCls.declaredConstructors.firstOrNull { it.parameterTypes.isEmpty() }
                    ?: return "ERR: no EventValue empty ctor"
                evCtor.isAccessible = true
                val evInst = evCtor.newInstance()
                val bufField = evCls.getDeclaredField("bufferDataValue")
                bufField.isAccessible = true
                bufField.set(evInst, canBytes)
                val setM = cls.getMethod("set", IntArray::class.java, evCls)
                val res = setM.invoke(dev, intArrayOf(featureId), evInst)
                "testSet(0x${Integer.toHexString(featureId)}, ${canBytes.size}b) = $res"
            } catch (t: Throwable) {
                val c = t.cause ?: t
                "testSet ERR ${c.javaClass.simpleName}: ${c.message}"
            }
        }

        @SuppressLint("PrivateApi")
        private fun doNativeSendInfo(type: Int, id: Int, extra: String): String {
            val proxy = nativeContainerProxy
            if (proxy == null) return "ERR: AutoContainerNative not available"
            return try {
                val m = proxy.javaClass.getMethod("sendInfo", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
                val res = m.invoke(proxy, type, id, extra)
                "nativeSendInfo($type, $id, \"$extra\") = $res"
            } catch (t: Throwable) {
                val c = t.cause ?: t
                "nativeSendInfo ERR ${c.javaClass.simpleName}: ${c.message}"
            }
        }
    }
}
