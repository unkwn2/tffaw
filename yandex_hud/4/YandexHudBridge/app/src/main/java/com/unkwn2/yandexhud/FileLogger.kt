package com.unkwn2.yandexhud

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val TAG = "HudFile"
    private val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    @Volatile private var file: File? = null

    @Synchronized
    fun init(ctx: Context) {
        if (file != null) return
        val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        if (!dir.exists()) dir.mkdirs()
        file = File(dir, "hud.log")
        write("BOOT", "file logger init at " + file!!.absolutePath)
    }

    fun getFile(): File? = file

    @Synchronized
    fun write(tag: String, msg: String) {
        val f = file
        if (f != null) {
            try {
                PrintWriter(FileWriter(f, true)).use { pw ->
                    pw.print(ts.format(Date()))
                    pw.print("  [")
                    pw.print(tag)
                    pw.print("] ")
                    pw.println(msg)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "write failed", t)
            }
        }
        Log.i(tag, msg)
    }

    @Synchronized
    fun clear() {
        try { file?.delete() } catch (_: Throwable) {}
    }

    fun log(tag: String, msg: String) = write(tag, msg)

    fun path(): String = file?.absolutePath ?: "<no log>"

    fun readAll(): String {
        val f = file ?: return "<no log file>"
        if (!f.exists()) return "<log empty>"
        return try { f.readText() } catch (t: Throwable) { "read err " + t.message }
    }
}
