package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import java.lang.reflect.Method

object BydHudController {
    private const val TAG = "BydHud"
    private const val CLS_INSTR = "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice"
    private const val CLS_ABS = "android.hardware.bydauto.AbsBYDAutoDevice"
    private const val CLS_EVENT_VALUE = "android.hardware.bydauto.BYDAutoEventValue"

    @Volatile private var instrDevice: Any? = null
    @Volatile private var instrCls: Class<*>? = null
    private val methodCache = mutableMapOf<String, Method>()

    var lastError: String? = null; private set

    @SuppressLint("PrivateApi")
    fun init(ctx: Context): String {
        return try {
            val klass = Class.forName(CLS_INSTR)
            instrCls = klass
            val getInstance = klass.getMethod("getInstance", Context::class.java)
            instrDevice = getInstance.invoke(null, ctx.applicationContext)
            val msg = "OK device=" + instrDevice + " cls=" + klass.simpleName
            FileLogger.write(TAG, "init " + msg)
            msg
        } catch (t: Throwable) {
            val cause = t.cause ?: t
            val msg = "init ERR " + cause.javaClass.simpleName + ": " + cause.message
            lastError = msg
            FileLogger.write(TAG, msg)
            Log.e(TAG, "init failed", t)
            msg
        }
    }

    fun isReady() = instrDevice != null && instrCls != null

    fun getPermissions(): String {
        if (!ensureReady()) return lastError ?: "not init"
        val get = invoke("getGetPermission")
        val set = invoke("getSetPermission")
        val devType = invoke("getDevicetype")
        return "getPerm=" + get + " | setPerm=" + set + " | devType=" + devType
    }

    fun getFeatureList(): String {
        if (!ensureReady()) return lastError ?: "not init"
        return try {
            val m = method("getFeatureList") ?: return "no method getFeatureList"
            val res = m.invoke(instrDevice) as? IntArray
            val count = res?.size ?: 0
            val list = res?.joinToString { Integer.toHexString(it) } ?: ""
            "features count=" + count + ": " + list
        } catch (t: Throwable) { "getFeatureList ERR " + (t.cause?.message ?: t.message) }
    }

    fun dumpPlatform(): String {
        val sb = StringBuilder()
        sb.append("Build: ").append(Build.BRAND).append("/").append(Build.MODEL)
            .append(" sdk=").append(Build.VERSION.SDK_INT).append("\n")
        val props = listOf(
            "ro.byd.product", "ro.byd.platform", "ro.byd.vehicle",
            "ro.byd.car.type", "ro.byd.car.series", "ro.byd.hardware",
            "persist.byd.platform", "persist.byd.car.type",
            "ro.build.product", "ro.product.name"
        )
        try {
            val sp = Class.forName("android.os.SystemProperties")
            val getM = sp.getMethod("get", String::class.java)
            for (p in props) {
                val v = getM.invoke(null, p) as? String ?: ""
                if (v.isNotEmpty()) sb.append("  ").append(p).append(" = ").append(v).append("\n")
            }
        } catch (t: Throwable) { sb.append("SystemProperties ERR ").append(t.message).append("\n") }
        return sb.toString().trimEnd()
    }

    @SuppressLint("PrivateApi")
    fun writeFeature(featureId: Int, intValue: Int): String {
        if (!ensureReady()) return lastError ?: "not init"
        return try {
            val absCls = Class.forName(CLS_ABS)
            val evCls = Class.forName(CLS_EVENT_VALUE)
            val evCtor = evCls.declaredConstructors.firstOrNull {
                it.parameterTypes.size == 1 && it.parameterTypes[0] == Int::class.java
            } ?: return "no BYDAutoEventValue(int) ctor"
            evCtor.isAccessible = true
            val evInst = evCtor.newInstance(intValue)
            val setM = absCls.declaredMethods.firstOrNull {
                it.name == "set" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == IntArray::class.java &&
                    it.parameterTypes[1] == evCls
            } ?: return "no AbsBYDAutoDevice.set(int[], BYDAutoEventValue)"
            setM.isAccessible = true
            val res = setM.invoke(instrDevice, intArrayOf(featureId), evInst)
            "writeFeature(0x" + Integer.toHexString(featureId) + ", " + intValue + ") = " + res
        } catch (t: Throwable) {
            val c = t.cause ?: t
            val msg = "writeFeature ERR " + c.javaClass.simpleName + ": " + c.message
            lastError = msg
            FileLogger.write(TAG, msg)
            msg
        }
    }

    @SuppressLint("PrivateApi")
    fun readFeature(featureId: Int): String {
        if (!ensureReady()) return lastError ?: "not init"
        return try {
            val absCls = Class.forName(CLS_ABS)
            val getM = absCls.declaredMethods.firstOrNull {
                it.name == "get" && it.parameterTypes.size == 1 && it.parameterTypes[0] == IntArray::class.java
            } ?: return "no AbsBYDAutoDevice.get(int[])"
            getM.isAccessible = true
            val res = getM.invoke(instrDevice, intArrayOf(featureId))
            "readFeature(0x" + Integer.toHexString(featureId) + ") = " + res
        } catch (t: Throwable) {
            val c = t.cause ?: t
            val msg = "readFeature ERR " + c.javaClass.simpleName + ": " + c.message
            lastError = msg
            FileLogger.write(TAG, msg)
            msg
        }
    }

    fun sendAutoNaviStatus(s: Int) = callInt("sendAutoNaviStatus", s)
    fun sendDestinationSetStatus(s: Int) = callInt("sendDestinationSetStatus", s)
    fun sendSimpleGuidanceInfo(turn: Int, dist: Int) = callIntInt("sendSimpleGuidanceInfo", turn, dist)
    fun sendNextPathName(n: String) = callStr("sendNextPathName", n)

    private fun ensureReady(): Boolean {
        if (!isReady()) { lastError = "device not init"; return false }
        return true
    }

    private fun method(name: String, vararg params: Class<*>): Method? {
        val key = name + "|" + params.joinToString { it.simpleName }
        methodCache[key]?.let { return it }
        return try {
            val m = if (params.isEmpty())
                instrCls!!.methods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
            else instrCls!!.getMethod(name, *params)
            if (m != null) methodCache[key] = m
            m
        } catch (t: Throwable) { null }
    }

    private fun invoke(name: String): Any? {
        return try { method(name)?.invoke(instrDevice) }
        catch (t: Throwable) { "ERR " + (t.cause?.message ?: t.message) }
    }

    private fun callInt(n: String, a: Int): String {
        return try {
            val m = method(n, Int::class.java) ?: return "no method " + n + "(int)"
            n + "(" + a + ") = " + m.invoke(instrDevice, a)
        } catch (t: Throwable) { formatErr(n, t) }
    }

    private fun callIntInt(n: String, a: Int, b: Int): String {
        return try {
            val m = method(n, Int::class.java, Int::class.java) ?: return "no method " + n + "(int,int)"
            n + "(" + a + "," + b + ") = " + m.invoke(instrDevice, a, b)
        } catch (t: Throwable) { formatErr(n, t) }
    }

    private fun callStr(n: String, s: String): String {
        return try {
            val m = method(n, String::class.java) ?: return "no method " + n + "(String)"
            n + "(\"" + s + "\") = " + m.invoke(instrDevice, s)
        } catch (t: Throwable) { formatErr(n, t) }
    }

    private fun formatErr(name: String, t: Throwable): String {
        val cause = t.cause ?: t
        val msg = name + " ERR " + cause.javaClass.simpleName + ": " + cause.message
        Log.e(TAG, msg, t); lastError = msg
        FileLogger.write(TAG, msg)
        return msg
    }
}
