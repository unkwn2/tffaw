package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.content.Context

object HudSettingDirect {
    private const val TAG = "HUD-Setting"

    @SuppressLint("PrivateApi")
    fun writeSetting(fid: Int, value: Int, ctx: Context): Int {
        return runCatching {
            val settCls = Class.forName("android.hardware.bydauto.setting.BYDAutoSettingDevice")
            val gi = settCls.getDeclaredMethod("getInstance", Context::class.java)
            gi.isAccessible = true
            val device = gi.invoke(null, ctx)
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val evCls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val ev = evCls.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            evCls.getDeclaredField("intValue").apply { isAccessible = true }.set(ev, value)
            val setM = absCls.getDeclaredMethod("set", IntArray::class.java, evCls)
            setM.isAccessible = true
            val rc = setM.invoke(device, intArrayOf(fid), ev) as Int
            FileLogger.write(TAG, "setSetting(0x${Integer.toHexString(fid)}, $value) rc=$rc (0x${Integer.toHexString(rc)})")
            rc
        }.onFailure {
            FileLogger.write(TAG, "setSetting ERR: ${it.javaClass.simpleName}: ${it.message}")
        }.getOrDefault(-1)
    }

    @SuppressLint("PrivateApi")
    fun readSetting(fid: Int, ctx: Context): Int {
        return runCatching {
            val settCls = Class.forName("android.hardware.bydauto.setting.BYDAutoSettingDevice")
            val gi = settCls.getDeclaredMethod("getInstance", Context::class.java)
            gi.isAccessible = true
            val device = gi.invoke(null, ctx)
            val absCls = Class.forName("android.hardware.bydauto.AbsBYDAutoDevice")
            val evCls = Class.forName("android.hardware.bydauto.BYDAutoEventValue")
            val gm = absCls.getMethod("get", IntArray::class.java, Class::class.java)
            val evt = gm.invoke(device, intArrayOf(fid), evCls)
            val v = evCls.getField("intValue").getInt(evt)
            FileLogger.write(TAG, "getSetting(0x${Integer.toHexString(fid)}) = $v")
            v
        }.onFailure {
            FileLogger.write(TAG, "getSetting ERR: ${it.javaClass.simpleName}: ${it.message}")
        }.getOrDefault(-1)
    }

    fun activateHudNavi(ctx: Context) {
        val r1 = writeSetting(0x4C10E036, 1, ctx)  // NAVIGATION_FUSION_SWITCH = ON
        val r2 = writeSetting(0x4C10E015, 3, ctx)  // SET_NAVI_SCREEN_STATUS = 3 (NAVI mode)
        FileLogger.write(TAG, "activateHudNavi: fusion=$r1 screen=$r2")
    }

    fun deactivateHudNavi(ctx: Context) {
        writeSetting(0x4C10E015, 1, ctx) // screen = 1 (Default)
        writeSetting(0x4C10E036, 2, ctx) // fusion = OFF
        FileLogger.write(TAG, "deactivateHudNavi done")
    }

    fun readHudState(ctx: Context) {
        val screen = readSetting(0x4C10E015, ctx)
        val fusion = readSetting(0x4C10E036, ctx)
        FileLogger.write(TAG, "HUD state: screen=$screen fusion=$fusion")
    }
}
