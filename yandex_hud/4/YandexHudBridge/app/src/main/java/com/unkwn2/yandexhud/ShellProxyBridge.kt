package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShellProxyBridge {
    private const val TAG = "ShellProxy"
    private const val PROXY_ACTION = "com.unkwn2.yandexhud.PROXY_CONNECTED"
    private const val PROXY_PKG = "com.unkwn2.yandexhud"
    private const val BINDER_KEY = "proxy_binder"
    private const val AIDL_DESC = "com.unkwn2.yandexhud.IHudControl"

    @Volatile private var proxyBinder: IBinder? = null
    @Volatile private var connected = false

    fun isConnected(): Boolean = connected && proxyBinder?.isBinderAlive == true

    fun onProxyReceived(binder: IBinder) {
        proxyBinder = binder
        connected = true
        val desc = try { binder.interfaceDescriptor ?: "<null>" } catch (t: Throwable) { "ERR " + t.message }
        FileLogger.write(TAG, "proxy received, desc=$desc")
    }

    fun setInstrumentFeature(featureId: Int, value: Int): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(featureId)
            data.writeInt(value)
            val ok = binder.transact(1, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "setInstrumentFeature(0x${Integer.toHexString(featureId)}, $value) = $result")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "setInstrumentFeature ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun getInstrumentFeature(featureId: Int): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(featureId)
            val ok = binder.transact(2, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "getInstrumentFeature(0x${Integer.toHexString(featureId)}) = $result")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "getInstrumentFeature ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun sendSimpleGuidanceInfo(turnKind: Int, distMeters: Int): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(turnKind)
            data.writeInt(distMeters)
            val ok = binder.transact(3, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "sendSimpleGuidanceInfo($turnKind, $distMeters) = $result")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "sendSimpleGuidanceInfo ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun sendAutoNaviStatus(status: Int): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(status)
            val ok = binder.transact(4, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "sendAutoNaviStatus($status) = $result")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "sendAutoNaviStatus ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun scrapBydAuto(): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            val ok = binder.transact(5, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "scrapBydAuto = ${result.take(200)}")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "scrapBydAuto ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun ping(): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            val ok = binder.transact(6, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            return "ping ERR ${t.javaClass.simpleName}: ${t.message}"
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun probe(): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            val ok = binder.transact(7, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "probe: ${result.take(300)}")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            return "probe ERR ${t.javaClass.simpleName}: ${t.message}"
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun sendInfo(type: Int, id: Int, extra: String): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(type)
            data.writeInt(id)
            data.writeString(extra)
            val ok = binder.transact(8, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "sendInfo($type, $id, \"$extra\") = $result")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "sendInfo ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun testSet(featureId: Int, canBytes: ByteArray): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(featureId)
            data.writeByteArray(canBytes)
            val ok = binder.transact(9, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "testSet(0x${Integer.toHexString(featureId)}, ${canBytes.size}b) = $result")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "testSet ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun nativeSendInfo(type: Int, id: Int, extra: String): String {
        val binder = proxyBinder
        if (binder == null || !binder.isBinderAlive) return "proxy not connected"
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(AIDL_DESC)
            data.writeInt(type)
            data.writeInt(id)
            data.writeString(extra)
            val ok = binder.transact(10, data, reply, 0)
            if (ok) {
                reply.readException()
                val result = reply.readString() ?: "<null>"
                FileLogger.write(TAG, "nativeSendInfo($type, $id, \"$extra\") = $result")
                return result
            }
            return "transact failed"
        } catch (t: Throwable) {
            val msg = "nativeSendInfo ERR ${t.javaClass.simpleName}: ${t.message}"
            FileLogger.write(TAG, msg)
            return msg
        } finally {
            reply.recycle()
            data.recycle()
        }
    }
}
