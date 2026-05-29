package com.unkwn2.yandexhud

import android.content.Context
import android.util.Log
import io.github.muntashirakon.adb.AdbStream
import java.io.File

object AdbLocalClient {
    private const val ADB_HOST = "127.0.0.1"
    private const val ADB_PORT = 5555

    fun startProxy(context: Context): String {
        return try {
            val manager = AdbManager.getInstance()
            val connected = manager.connect(ADB_HOST, ADB_PORT)
            if (!connected) {
                FileLogger.write("AdbLocal", "ADB connect failed")
                return "ERR: ADB connect failed"
            }
            FileLogger.write("AdbLocal", "ADB connected OK")
            val apkPath = context.applicationInfo.sourceDir
            val command = buildProxyCommand(apkPath)
            FileLogger.write("AdbLocal", "proxy cmd: " + command.take(200))
            val stream = manager.openStream("shell:")
            stream.openOutputStream().use { os ->
                os.write(command.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            Thread.sleep(2000)
            FileLogger.write("AdbLocal", "proxy launch script executed via libadb-android")
            "OK: proxy launched via libadb-android"
        } catch (t: Throwable) {
            val msg = "ADB ERR " + t.javaClass.simpleName + ": " + t.message
            FileLogger.write("AdbLocal", msg)
            Log.e("AdbLocal", "startProxy failed", t)
            msg
        }
    }

    fun checkPort(): String {
        return try {
            val manager = AdbManager.getInstance()
            val connected = manager.connect(ADB_HOST, ADB_PORT)
            if (connected) "ADB CONNECTED" else "ADB REFUSED"
        } catch (t: Throwable) {
            "ADB ERR " + t.javaClass.simpleName + ": " + t.message
        }
    }

    private fun buildProxyCommand(apkPath: String): String {
        return "pkill -9 -f yandexhud_proxy\n" +
            "pidof yandexhud_proxy | xargs kill -9 2>/dev/null\n" +
            "\n" +
            "nohup app_process \\\n" +
            "  -Djava.class.path=/system/framework/services.jar:/system/framework/dilink-services.jar:" + apkPath + " \\\n" +
            "  -Djava.library.path=/system/lib64:/product/lib64:" + apkPath + "!/lib/arm64-v8a \\\n" +
            "  /system/bin \\\n" +
            "  --nice-name=yandexhud_proxy \\\n" +
            "  com.unkwn2.yandexhud.proxy.EntryPoint \\\n" +
            "  --uid=2000 \\\n" +
            "  > /dev/null 2>&1 &\n" +
            "\n"
    }

    fun getManualCommand(context: Context): String {
        return buildProxyCommand(context.applicationInfo.sourceDir)
    }
}
