package com.unkwn2.yandexhud

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ProtocolServiceBridge {
    private const val TAG = "PSB"
    private const val PKG = "com.byd.amapservice"
    private const val CLS = "com.autosdk.protocol.service.ProtocolService"
    private const val ACTION = "action.com.byd.protocol.ProtocolService"
    private const val AIDL_DESC = "com.autosdk.protocol.IProtocolAidlInterface"
    private const val TX_SET_MODEL = 1
    private const val TX_REGISTER_CALLBACK = 3
    private const val TX_GET_NAVI_STATE = 4
    private const val TX_GET_MAP_STATE = 7

    @Volatile private var boundBinder: IBinder? = null
    @Volatile private var boundDesc: String? = null
    @Volatile private var conn: ServiceConnection? = null
    @Volatile private var appCtx: Context? = null
    private var callbackBinder: CallbackBinder? = null

    class CallbackBinder : Binder() {
        @Volatile var lastResult: String = ""
            private set

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                IBinder.INTERFACE_TRANSACTION -> {
                    reply?.writeString("com.autosdk.protocol.listener.IProtocolCallback")
                    return true
                }
                1 -> {
                    data.enforceInterface("com.autosdk.protocol.listener.IProtocolCallback")
                    val success = data.readString() ?: ""
                    lastResult = "onSuccess: $success"
                    FileLogger.write(TAG, "callback: $lastResult")
                    reply?.writeNoException()
                    return true
                }
                2 -> {
                    data.enforceInterface("com.autosdk.protocol.listener.IProtocolCallback")
                    val json = data.readString() ?: ""
                    lastResult = "onJSONResult: $json"
                    FileLogger.write(TAG, "callback: $lastResult")
                    reply?.writeNoException()
                    return true
                }
                3 -> {
                    data.enforceInterface("com.autosdk.protocol.listener.IProtocolCallback")
                    val errJson = data.readString() ?: ""
                    lastResult = "onFail: $errJson"
                    FileLogger.write(TAG, "callback: $lastResult")
                    reply?.writeNoException()
                    return true
                }
                else -> return super.onTransact(code, data, reply, flags)
            }
        }
    }

    fun isBound(): Boolean {
        val b = boundBinder ?: return false
        return b.isBinderAlive
    }

    fun getDescriptor(): String? = boundDesc

    fun getLastCallbackResult(): String = callbackBinder?.lastResult ?: "<no callback>"

    fun bind(ctx: Context): String {
        if (isBound()) return "already bound, desc=" + boundDesc
        appCtx = ctx.applicationContext
        val intent = Intent(ACTION).apply {
            setPackage(PKG)
            component = ComponentName(PKG, CLS)
        }
        val latch = CountDownLatch(1)
        val sb = StringBuilder()
        val sc = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val desc = try { binder.interfaceDescriptor ?: "<null>" } catch (t: Throwable) { "ERR " + t.message }
                boundBinder = binder
                boundDesc = desc
                sb.append("connected name=").append(name).append("\n")
                sb.append("interfaceDescriptor=").append(desc).append("\n")
                sb.append("isBinderAlive=").append(binder.isBinderAlive).append("\n")
                FileLogger.write(TAG, "bound desc=" + desc)
                latch.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName) {
                boundBinder = null
                boundDesc = null
                FileLogger.write(TAG, "disconnected " + name)
            }
        }
        conn = sc
        val ok = try {
            appCtx!!.bindService(intent, sc, Context.BIND_AUTO_CREATE)
        } catch (t: Throwable) {
            FileLogger.write(TAG, "bind threw " + t.message)
            return "bindService threw: " + t.message
        }
        sb.append("bindService returned=").append(ok).append("\n")
        FileLogger.write(TAG, "bindService returned=" + ok)
        if (!ok) return sb.toString()
        try { latch.await(4, TimeUnit.SECONDS) } catch (_: Throwable) {}
        return sb.toString().trimEnd()
    }

    fun unbind(): String {
        val c = conn ?: return "not bound"
        try { appCtx?.unbindService(c) } catch (_: Throwable) {}
        boundBinder = null
        boundDesc = null
        conn = null
        FileLogger.write(TAG, "unbind")
        return "unbound"
    }

    fun setNavControl(value: Int): String {
        val binder = boundBinder
        if (binder == null || !binder.isBinderAlive) return "not bound; call bind() first"
        val desc = boundDesc ?: return "no descriptor"
        val sb = StringBuilder()
        sb.append("setNavControl(").append(value).append(") desc=").append(desc).append("\n")
        FileLogger.write(TAG, "setNavControl(" + value + ") desc=" + desc)
        var anyOk = false
        for (code in 1..6) {
            for (strategy in arrayOf("ints", "model")) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(desc)
                    if (strategy == "ints") {
                        data.writeInt(30000)
                        data.writeInt(1)
                        data.writeInt(value)
                    } else {
                        data.writeInt(1)
                        data.writeInt(30000)
                        data.writeInt(1)
                        data.writeInt(value)
                        data.writeString("")
                    }
                    val ok = binder.transact(code, data, reply, 0)
                    val replyInt = try { reply.readInt() } catch (_: Throwable) { -1 }
                    val line = "  code=" + code + " strat=" + strategy + " ok=" + ok + " reply=" + replyInt
                    sb.append(line).append("\n")
                    FileLogger.write(TAG, line.trim())
                    if (ok) anyOk = true
                } catch (t: Throwable) {
                    val line = "  code=" + code + " strat=" + strategy + " ERR " + t.javaClass.simpleName + ": " + t.message
                    sb.append(line).append("\n")
                    FileLogger.write(TAG, line.trim())
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }
        }
        sb.append("anyOk=").append(anyOk)
        return sb.toString().trimEnd()
    }

    fun getNaviState(): String {
        val binder = boundBinder
        if (binder == null || !binder.isBinderAlive) return "not bound"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            val ok = binder.transact(TX_GET_NAVI_STATE, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readInt() != 0
                val msg = "getNaviState=$result"
                FileLogger.write(TAG, msg)
                return msg
            }
            return "getNaviState transact failed"
        } catch (t: Throwable) {
            val msg = "getNaviState ERR " + t.javaClass.simpleName + ": " + t.message
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun getMapState(type: Int): String {
        val binder = boundBinder
        if (binder == null || !binder.isBinderAlive) return "not bound"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(type)
            val ok = binder.transact(TX_GET_MAP_STATE, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                val msg = "getMapState($type)=$result"
                FileLogger.write(TAG, msg)
                return msg
            }
            return "getMapState($type) transact failed"
        } catch (t: Throwable) {
            val msg = "getMapState($type) ERR " + t.javaClass.simpleName + ": " + t.message
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun registerCallback(id: Int): String {
        val binder = boundBinder
        if (binder == null || !binder.isBinderAlive) return "not bound"
        if (callbackBinder == null) callbackBinder = CallbackBinder()
        val cb = callbackBinder!!
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeStrongBinder(cb)
            data.writeInt(id)
            val ok = binder.transact(TX_REGISTER_CALLBACK, data, reply, 0)
            if (ok) {
                reply.readException()
                val msg = "registerCallback($id) OK lastResult=" + cb.lastResult
                FileLogger.write(TAG, msg)
                return msg
            }
            return "registerCallback($id) transact failed"
        } catch (t: Throwable) {
            val msg = "registerCallback($id) ERR " + t.javaClass.simpleName + ": " + t.message
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun registerCallbackAll(): String {
        val ids = intArrayOf(32001, 32002, 32003, 32004, 32005, 50001)
        val sb = StringBuilder()
        for (id in ids) {
            sb.append(registerCallback(id)).append("\n")
        }
        return sb.toString().trimEnd()
    }
}
