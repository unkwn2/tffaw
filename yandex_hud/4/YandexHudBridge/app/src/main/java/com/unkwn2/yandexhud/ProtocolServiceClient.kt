package com.unkwn2.yandexhud

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class ProtocolServiceClient(private val ctx: Context) {
    private var bound = false

    fun bind() {
        if (bound) { FileLogger.write("ProtoClient", "already bound"); return }
        try {
            val intent = Intent().setClassName("com.byd.amapservice", "com.byd.amapservice.ProtocolService")
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    FileLogger.write("ProtoClient", "bound OK")
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    bound = false
                    FileLogger.write("ProtoClient", "disconnected")
                }
            }
            bound = ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            FileLogger.write("ProtoClient", "bind result=$bound")
        } catch (t: Throwable) {
            FileLogger.write("ProtoClient", "bind ERR: ${t.message}")
        }
    }

    fun unbind() {
        FileLogger.write("ProtoClient", "unbind")
    }

    fun isBound(): Boolean = bound

    fun diagGetMapState(index: Int) {
        FileLogger.write("ProtoClient", "diagGetMapState($index) — STUB")
    }

    fun diagGetNaviState() {
        FileLogger.write("ProtoClient", "diagGetNaviState — STUB")
    }

    fun diagSetCompatibleVersion(v: Int) {
        FileLogger.write("ProtoClient", "diagSetCompatibleVersion($v) — STUB")
    }

    fun registerCallback(id: Int) {
        FileLogger.write("ProtoClient", "registerCallback($id) — STUB")
    }

    fun registCallbackLegacy() {
        FileLogger.write("ProtoClient", "registCallbackLegacy — STUB")
    }

    fun setProtocolModelData(pid: Int, action: Int, value: Int) {
        FileLogger.write("ProtoClient", "setProtocolModelData(pid=$pid, act=$action, val=$value) — STUB")
    }
}
