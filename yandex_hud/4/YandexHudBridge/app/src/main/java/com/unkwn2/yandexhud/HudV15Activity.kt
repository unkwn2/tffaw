package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.view.Display
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("SetTextI18n")
class HudV15Activity : AppCompatActivity() {
    private lateinit var logView: TextView
    private lateinit var statusView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var pres3: Presentation? = null
    private var pres4: Presentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hud_v15)
        logView = findViewById(R.id.hudLog)
        statusView = findViewById(R.id.hudStatus)
        FileLogger.init(this)

        logMsg("HUD v15 — Presentation API + Logcat Capture")
        logMsg("UID=${android.os.Process.myUid()}")

        findViewById<Button>(R.id.btnPres3).setOnClickListener { showPresentation(3) }
        findViewById<Button>(R.id.btnPres4).setOnClickListener { showPresentation(4) }
        findViewById<Button>(R.id.btnPresDismiss).setOnClickListener { dismissAll() }
        findViewById<Button>(R.id.btnListDisplays).setOnClickListener { listDisplays() }
        findViewById<Button>(R.id.btnCaptureLogcat).setOnClickListener { captureLogcat() }
        findViewById<Button>(R.id.btnClearLogcat).setOnClickListener { clearLogcat() }
        findViewById<Button>(R.id.btnExportUsb).setOnClickListener { exportToUsb() }
        findViewById<Button>(R.id.btnProbeGB).setOnClickListener { probeGBDevice() }
        findViewById<Button>(R.id.btnServiceCall).setOnClickListener { serviceCallProbe() }
    }

    private fun logMsg(s: String) {
        handler.post {
            val t = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
            val line = "$t  $s"
            logView.append(line + "\n")
            FileLogger.write("V15", s)
            val sv = logView.parent as? ScrollView
            sv?.post { sv.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun listDisplays() {
        logMsg("=== LIST DISPLAYS ===")
        val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = dm.displays
        logMsg("Display count: ${displays.size}")
        for (d in displays) {
            val m = android.util.DisplayMetrics()
            d.getMetrics(m)
            logMsg("  ID=${d.displayId} name='${d.name}' ${m.widthPixels}x${m.heightPixels} flags=0x${Integer.toHexString(d.flags)} valid=${d.isValid} state=${d.state}")
            if (d.flags and Display.FLAG_PRESENTATION != 0) {
                logMsg("    *** PRESENTATION capable! ***")
            }
        }
    }

    private fun showPresentation(displayId: Int) {
        logMsg("=== Presentation on displayId=$displayId ===")
        try {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = dm.getDisplay(displayId)
            if (display == null || !display.isValid) {
                logMsg("  ERR: display $displayId not found or invalid")
                return
            }
            logMsg("  display: ${display.name} state=${display.state}")

            val pres = HudPresentation(this, display)
            logMsg("  Creating Presentation...")
            pres.show()
            logMsg("  Presentation.show() called — CHECK HUD NOW!")

            if (displayId == 3) pres3 = pres else pres4 = pres

            statusView.text = "Presentation shown on display $displayId"
        } catch (e: Exception) {
            logMsg("  ERR: ${e.javaClass.simpleName}: ${e.message}")
            if (e.message?.contains("permission", ignoreCase = true) == true) {
                logMsg("  >> Permission denied — this display may be restricted")
            }
            if (e.message?.contains("display", ignoreCase = true) == true) {
                logMsg("  >> Display error — may not accept external presentations")
            }
        }
    }

    private fun dismissAll() {
        logMsg("=== Dismiss all Presentations ===")
        try { pres3?.dismiss(); pres3 = null; logMsg("  Dismissed display 3") } catch (e: Exception) { logMsg("  dismiss3 ERR: ${e.message}") }
        try { pres4?.dismiss(); pres4 = null; logMsg("  Dismissed display 4") } catch (e: Exception) { logMsg("  dismiss4 ERR: ${e.message}") }
        statusView.text = "Presentations dismissed"
    }

    @SuppressLint("SetWorldReadable")
    private fun captureLogcat() {
        logMsg("=== CAPTURE LOGCAT (BYD featureIDs) ===")
        try {
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
            val outFile = java.io.File(getExternalFilesDir(null), "logcat_hud_$ts.log")
            val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time"))
            val grepPatterns = arrayOf("AbsBYDAuto", "set featureID", "BYDAutoDevice", "BYDAutoGB", "featureID", "hud", "arhud", "fusion", "navi", "0x6dc", "0x4c11", "0xaa00", "0x4470", "0x34c0", "0x28c0", "InstrumentDevice")
            val input = proc.inputStream.bufferedReader()
            val out = outFile.outputStream().bufferedWriter()
            var count = 0
            input.forEachLine { line ->
                val lower = line.lowercase()
                if (grepPatterns.any { p -> lower.contains(p.lowercase()) }) {
                    out.write(line + "\n")
                    count++
                }
            }
            out.flush()
            out.close()
            input.close()
            proc.destroy()
            logMsg("  Captured $count matching lines -> ${outFile.absolutePath}")
            logMsg("  File size: ${outFile.length()} bytes")

            val intDir = getExternalFilesDir(null) ?: filesDir
            val intDst = java.io.File(intDir, "yandex_hud_probe_$ts.log")
            val src = FileLogger.getFile()
            if (src != null && src.exists()) {
                src.inputStream().use { inp -> intDst.outputStream().use { out2 -> inp.copyTo(out2) } }
                logMsg("  APP DATA: ${intDst.absolutePath}")
            }

            val sdDl = java.io.File("/storage/emulated/0/Download")
            if (sdDl.exists()) {
                val sdDst = java.io.File(sdDl, "logcat_hud_$ts.log")
                outFile.inputStream().use { inp -> sdDst.outputStream().use { out2 -> inp.copyTo(out2) } }
                logMsg("  SDCARD: ${sdDst.absolutePath}")
            }

            val usbBase = java.io.File("/storage/4A21-0000/Download")
            if (usbBase.exists()) {
                val usbDst = java.io.File(usbBase, "logcat_hud_$ts.log")
                outFile.inputStream().use { inp -> usbDst.outputStream().use { out2 -> inp.copyTo(out2) } }
                logMsg("  USB: ${usbDst.absolutePath}")
            }

            statusView.text = "Logcat captured: $count lines"
        } catch (e: Exception) {
            logMsg("  ERR: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun clearLogcat() {
        logMsg("=== CLEAR LOGCAT ===")
        try {
            Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor()
            logMsg("  Logcat cleared — now toggle HUD settings, then CAPTURE")
            statusView.text = "Logcat cleared — toggle HUD settings now!"
        } catch (e: Exception) {
            logMsg("  ERR: ${e.message}")
        }
    }

    private fun exportToUsb() {
        logMsg("=== EXPORT LOG ===")
        try {
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
            val intDir = getExternalFilesDir(null) ?: filesDir
            val src = FileLogger.getFile()
            if (src == null || !src.exists()) { logMsg("  no log file"); return }

            val intDst = java.io.File(intDir, "yandex_hud_probe_$ts.log")
            src.inputStream().use { inp -> intDst.outputStream().use { out -> inp.copyTo(out) } }
            logMsg("  APP DATA: ${intDst.absolutePath} (${intDst.length()} bytes)")

            val usbBase = java.io.File("/storage/4A21-0000/Download")
            if (usbBase.exists()) {
                val usbDst = java.io.File(usbBase, "yandex_hud_probe_$ts.log")
                src.inputStream().use { inp -> usbDst.outputStream().use { out -> inp.copyTo(out) } }
                logMsg("  USB: ${usbDst.absolutePath}")
            }

            val sdDl = java.io.File("/storage/emulated/0/Download")
            if (sdDl.exists()) {
                val sdDst = java.io.File(sdDl, "yandex_hud_probe_$ts.log")
                src.inputStream().use { inp -> sdDst.outputStream().use { out -> inp.copyTo(out) } }
                logMsg("  SDCARD: ${sdDst.absolutePath}")
            }
            statusView.text = "Log exported"
        } catch (e: Exception) {
            logMsg("  ERR: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun probeGBDevice() {
        logMsg("=== PROBE BYDAutoGBDevice ===")
        try {
            val cl = Class.forName("android.hardware.bydauto.gb.BYDAutoGBDevice")
            logMsg("  FOUND: $cl")
            val methods = cl.declaredMethods
            for (m in methods) {
                logMsg("    ${m.name}(${m.parameterTypes.map { it.simpleName }})")
            }
            val getInstance = cl.getMethod("getInstance", Context::class.java)
            val inst = getInstance.invoke(null, this)
            logMsg("  getInstance = $inst")
        } catch (e: Exception) {
            logMsg("  ERR: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun serviceCallProbe() {
        logMsg("=== SERVICE CALL AUTOSERVICE PROBE ===")
        try {
            val svc = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "autoservice") as? android.os.IBinder
            if (svc == null) { logMsg("  autoservice binder = null"); return }
            logMsg("  autoservice binder = $svc")

            val descriptor = try {
                val p = Parcel.obtain()
                svc.transact(0x5f4e5448, p, p, 0)
                p.setDataPosition(0)
                p.readString() ?: "null"
            } catch (e: Exception) { "ERR: ${e.message}" }
            logMsg("  Interface descriptor: $descriptor")

            for (tx in 1..20) {
                try {
                    val reply = Parcel.obtain()
                    val data = Parcel.obtain()
                    data.writeInterfaceToken(descriptor ?: "android.gui.BYDAutoServer")
                    data.writeInt(1)
                    data.writeInt(0x6DC00810)
                    svc.transact(tx, data, reply, 0)
                    reply.setDataPosition(0)
                    val res = reply.readInt()
                    logMsg("  TX=$tx readInt=$res")
                    reply.recycle()
                    data.recycle()
                } catch (e: Exception) {
                    logMsg("  TX=$tx ERR: ${e.message?.take(80)}")
                }
            }
        } catch (e: Exception) {
            logMsg("  ERR: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
