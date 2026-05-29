#!/usr/bin/env bash
set -e
ROOT="YandexHudBridge"
rm -rf "$ROOT"
mkdir -p "$ROOT"

writefile() {
    local path="$1"
    local full="$ROOT/$path"
    mkdir -p "$(dirname "$full")"
    cat > "$full"
    echo "  + $path"
}

writefile "settings.gradle.kts" <<'EOF'
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "YandexHudBridge"
include(":app")
EOF

writefile "build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
EOF

writefile "gradle.properties" <<'EOF'
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
EOF

writefile ".gitignore" <<'EOF'
.gradle/
build/
.idea/
local.properties
*.iml
captures/
.cxx/
EOF

writefile "app/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.unkwn2.yandexhud"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.unkwn2.yandexhud"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "1.0-combined"
    }
    buildTypes {
        debug { isMinifyEnabled = false }
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
}
EOF

writefile "app/src/main/AndroidManifest.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.BYDAUTO_INSTRUMENT_COMMON" />
    <uses-permission android:name="android.permission.BYDAUTO_INSTRUMENT_SET" />
    <uses-permission android:name="android.permission.BYDAUTO_INSTRUMENT_GET" />
    <uses-permission android:name="com.byd.permission.BYDAUTO_INSTRUMENT_COMMON" />
    <uses-permission android:name="com.byd.permission.BYDAUTO_INSTRUMENT_SET" />
    <uses-permission android:name="com.byd.permission.BYDAUTO_INSTRUMENT_GET" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <queries>
        <package android:name="ru.yandex.yandexnavi" />
        <package android:name="com.byd.amapservice" />
        <package android:name="com.byd.launchermap" />
    </queries>
    <application
        android:allowBackup="false"
        android:label="YandexHudBridge"
        android:icon="@android:drawable/sym_def_app_icon"
        android:theme="@style/Theme.AppCompat.DayNight">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <service
            android:name=".HudAccessibilityService"
            android:exported="true"
            android:label="@string/accessibility_label"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>
</manifest>
EOF

writefile "app/src/main/res/values/strings.xml" <<'EOF'
<resources>
    <string name="accessibility_label">YandexHudBridge</string>
    <string name="accessibility_description">Reads Yandex Navigator and sends state to BYD HUD.</string>
</resources>
EOF

writefile "app/src/main/res/xml/accessibility_service_config.xml" <<'EOF'
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged|typeViewTextChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_description"
    android:notificationTimeout="100"
    android:packageNames="ru.yandex.yandexnavi" />
EOF

writefile "app/src/main/res/layout/activity_main.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="8dp">
    <TextView android:id="@+id/status"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp"
        android:padding="6dp"
        android:background="#222"
        android:textColor="#0f0"
        android:fontFamily="monospace"
        android:text="boot v1.0..." />
    <HorizontalScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        <LinearLayout android:orientation="horizontal"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content">
            <Button android:id="@+id/btnProtoBind" android:text="1.BIND PROTO"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnNav3" android:text="2.NAV(3) HUD ON"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnNav0" android:text="3.NAV(0) HUD OFF"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnInit" android:text="INIT SDK"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnPerm" android:text="PERMS"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnFeatures" android:text="FEATURES"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnPlatform" android:text="PLATFORM"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnLeft500" android:text="LEFT 500m"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnRight500" android:text="RIGHT 500m"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnRead" android:text="READ 4c10a018"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnW3" android:text="WRITE =3"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnExportLog" android:text="EXPORT LOG"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
            <Button android:id="@+id/btnClearLog" android:text="CLEAR LOG"
                android:layout_width="wrap_content" android:layout_height="wrap_content"/>
        </LinearLayout>
    </HorizontalScrollView>
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">
        <TextView android:id="@+id/log"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:fontFamily="monospace"
            android:padding="6dp"
            android:textColor="#ddd"
            android:background="#111"/>
    </ScrollView>
</LinearLayout>
EOF

writefile "app/src/main/java/com/unkwn2/yandexhud/TurnKindMap.kt" <<'EOF'
package com.unkwn2.yandexhud

object TurnKindMap {
    const val BLANK = 0
    const val FRONT = 1
    const val RIGHT_FRONT = 2
    const val RIGHT = 3
    const val RIGHT_BACK = 4
    const val BACK = 5
    const val LEFT_BACK = 6
    const val LEFT = 7
    const val LEFT_FRONT = 8
    const val RING = 9
    const val RING_OUT = 10
    const val LEFT_SIDE = 11
    const val RIGHT_SIDE = 12
    const val DEST = 24
    const val START = 23
    const val TOLLGATE = 31
    const val STRAIGHT = 77
    const val REST_AREA = 78

    const val NAVI_INVALID = 0
    const val NAVI_OPEN_NOT_SET_DEST = 1
    const val NAVI_OPEN_SET_DEST = 2
    const val NAVI_OPENING = 3
    const val NAVI_CLOSE = 4

    const val DEST_NOT_SET = 1
    const val DEST_SET_DONE = 2

    fun mapYandexAction(action: String?): Int = when (action?.lowercase()) {
        null, "" -> BLANK
        "straight", "прямо", "продолжайте движение" -> FRONT
        "left", "налево" -> LEFT
        "right", "направо" -> RIGHT
        "uturn", "u-turn", "разворот" -> BACK
        "roundabout", "круговое движение", "на круг" -> RING
        "exit roundabout", "съезд с круга" -> RING_OUT
        "destination", "финиш", "вы прибыли" -> DEST
        "start", "начало" -> START
        else -> FRONT
    }

    // BYD cluster uses GBK locale. Non-ASCII becomes garbled.
    // Filter to printable ASCII and trim to 20 chars.
    fun sanitizeRoadName(raw: String?): String {
        val s = raw ?: return ""
        val sb = StringBuilder()
        for (c in s) {
            if (c.code in 32..126) sb.append(c)
        }
        val out = sb.toString().trim()
        return if (out.length > 20) out.substring(0, 20) else out
    }
}
EOF

writefile "app/src/main/java/com/unkwn2/yandexhud/FileLogger.kt" <<'EOF'
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

    fun readAll(): String {
        val f = file ?: return "<no log file>"
        if (!f.exists()) return "<log empty>"
        return try { f.readText() } catch (t: Throwable) { "read err " + t.message }
    }
}
EOF

writefile "app/src/main/java/com/unkwn2/yandexhud/ProtocolServiceBridge.kt" <<'EOF'
package com.unkwn2.yandexhud

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ProtocolServiceBridge {
    private const val TAG = "PSB"
    private const val PKG = "com.byd.amapservice"
    private const val CLS = "com.autosdk.protocol.service.ProtocolService"
    private const val ACTION = "action.com.byd.protocol.ProtocolService"

    @Volatile private var boundBinder: IBinder? = null
    @Volatile private var boundDesc: String? = null
    @Volatile private var conn: ServiceConnection? = null
    @Volatile private var appCtx: Context? = null

    fun isBound(): Boolean {
        val b = boundBinder ?: return false
        return b.isBinderAlive
    }

    fun getDescriptor(): String? = boundDesc

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

    /**
     * setNavControl(value) — chain: doOperate(30000,1,value) -> mVoiceNavi.D(value)
     * -> setMeterNaviType(value) -> setNavControl(value) -> 0x4C10A018=value
     *
     * value=3 -> HUD ON, value=0 -> HUD OFF
     *
     * Two strategies tried for each transact code 1..6:
     *  - "ints": writeInt(30000), writeInt(1), writeInt(value)
     *  - "model": writeInt(1) marker + writeInt(30000), writeInt(1), writeInt(value), writeString("")
     *    (mimics Parcelable header for MapOperaModel)
     */
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
                        data.writeInt(1) // parcelable not-null marker
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
}
EOF

writefile "app/src/main/java/com/unkwn2/yandexhud/BydHudController.kt" <<'EOF'
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
EOF

writefile "app/src/main/java/com/unkwn2/yandexhud/MainActivity.kt" <<'EOF'
package com.unkwn2.yandexhud

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var statusView: TextView
    private lateinit var logView: TextView
    private val ts = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val FEATURE_4C10A018 = 0x4c10a018

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusView = findViewById(R.id.status)
        logView = findViewById(R.id.log)
        logView.movementMethod = ScrollingMovementMethod()
        FileLogger.init(this)
        log("APK boot v1.0-combined")
        val logPath = FileLogger.getFile()?.absolutePath ?: "<none>"
        log("Log file: " + logPath)
        statusView.text = "v1.0 ready. Press 1.BIND PROTO first."

        findViewById<Button>(R.id.btnProtoBind).setOnClickListener {
            log("BIND ->")
            log(ProtocolServiceBridge.bind(this))
        }
        findViewById<Button>(R.id.btnNav3).setOnClickListener {
            log("NAV(3) HUD ON ->")
            log(ProtocolServiceBridge.setNavControl(3))
        }
        findViewById<Button>(R.id.btnNav0).setOnClickListener {
            log("NAV(0) HUD OFF ->")
            log(ProtocolServiceBridge.setNavControl(0))
        }
        findViewById<Button>(R.id.btnInit).setOnClickListener {
            val r = BydHudController.init(this); log("INIT -> " + r)
            statusView.text = if (BydHudController.isReady()) "READY" else "INIT FAILED"
        }
        findViewById<Button>(R.id.btnPerm).setOnClickListener { log("PERMS -> " + BydHudController.getPermissions()) }
        findViewById<Button>(R.id.btnFeatures).setOnClickListener { log("FEATURES -> " + BydHudController.getFeatureList()) }
        findViewById<Button>(R.id.btnPlatform).setOnClickListener { log("PLATFORM ->\n" + BydHudController.dumpPlatform()) }
        findViewById<Button>(R.id.btnLeft500).setOnClickListener { log(BydHudController.sendSimpleGuidanceInfo(TurnKindMap.LEFT, 500)) }
        findViewById<Button>(R.id.btnRight500).setOnClickListener { log(BydHudController.sendSimpleGuidanceInfo(TurnKindMap.RIGHT, 500)) }
        findViewById<Button>(R.id.btnRead).setOnClickListener { log(BydHudController.readFeature(FEATURE_4C10A018)) }
        findViewById<Button>(R.id.btnW3).setOnClickListener { log(BydHudController.writeFeature(FEATURE_4C10A018, 3)) }
        findViewById<Button>(R.id.btnExportLog).setOnClickListener { exportLog() }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            FileLogger.clear()
            logView.text = ""
            log("log cleared")
        }
    }

    private fun exportLog() {
        val text = FileLogger.readAll()
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("HUD log", text))
            Toast.makeText(this, "Log copied (" + text.length + " chars)", Toast.LENGTH_LONG).show()
            log("log copied to clipboard (" + text.length + " chars)")
            val path = FileLogger.getFile()?.absolutePath ?: "<none>"
            log("adb pull " + path)
        } catch (t: Throwable) {
            log("export failed: " + t.message)
        }
    }

    private fun log(s: String) {
        val time = ts.format(Date())
        val line = time + "  " + s + "\n"
        logView.append(line)
        val scrollAmount = logView.layout?.getLineTop(logView.lineCount) ?: 0
        val viewHeight = logView.height
        if (scrollAmount > viewHeight) logView.scrollTo(0, scrollAmount - viewHeight)
        FileLogger.write("UI", s)
    }
}
EOF

writefile "app/src/main/java/com/unkwn2/yandexhud/HudAccessibilityService.kt" <<'EOF'
package com.unkwn2.yandexhud

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HudAccessibilityService : AccessibilityService() {
    private val tag = "HudA11y"
    private var lastTurn = -1
    private var lastDist = -1
    private var initialized = false
    private var hudEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private val rxDist = Regex("""(\d+)\s*(м|m|км|km)""", RegexOption.IGNORE_CASE)

    private val stopRunnable = Runnable {
        if (hudEnabled) {
            FileLogger.write(tag, "auto-stop: setNavControl(0)")
            ProtocolServiceBridge.setNavControl(0)
            hudEnabled = false
        }
    }

    override fun onServiceConnected() {
        FileLogger.init(this)
        FileLogger.write(tag, "service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName != "ru.yandex.yandexnavi") return
        if (!initialized) {
            FileLogger.write(tag, "first yandexnavi event -> bind + setNavControl(3) in 500ms")
            ProtocolServiceBridge.bind(this)
            handler.postDelayed({
                ProtocolServiceBridge.setNavControl(3)
                hudEnabled = true
            }, 500)
            initialized = true
        }
        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        collectText(root, texts)
        parseAndPush(texts)
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        node ?: return
        node.text?.let { out += it.toString() }
        node.contentDescription?.let { out += it.toString() }
        for (i in 0 until node.childCount) collectText(node.getChild(i), out)
    }

    private fun parseAndPush(texts: List<String>) {
        if (texts.isEmpty()) return
        var distMeters = -1
        var maneuver: String? = null
        var roadName: String? = null
        for (t in texts) {
            if (distMeters < 0) rxDist.find(t)?.let { m ->
                val v = m.groupValues[1].toIntOrNull() ?: 0
                val u = m.groupValues[2].lowercase()
                distMeters = if (u.startsWith("к") || u.startsWith("k")) v * 1000 else v
            }
            val low = t.lowercase()
            if (maneuver == null) maneuver = when {
                "налево" in low || "left" in low -> "left"
                "направо" in low || "right" in low -> "right"
                "разворот" in low || "uturn" in low -> "uturn"
                "прямо" in low || "straight" in low -> "straight"
                "круг" in low || "roundabout" in low -> "roundabout"
                else -> null
            }
            if (roadName == null && t.length in 3..40 && !t.contains("км") && !t.contains("м ")) {
                roadName = t
            }
        }
        if (maneuver == null && distMeters < 0) return
        val turn = TurnKindMap.mapYandexAction(maneuver)
        if (turn != lastTurn || distMeters != lastDist) {
            val safe = TurnKindMap.sanitizeRoadName(roadName)
            FileLogger.write(tag, "guide turn=" + turn + " dist=" + distMeters + " road=" + safe)
            if (BydHudController.isReady()) {
                BydHudController.sendSimpleGuidanceInfo(turn, if (distMeters > 0) distMeters else 0)
                if (safe.isNotEmpty()) BydHudController.sendNextPathName(safe)
            }
            lastTurn = turn; lastDist = distMeters
            handler.removeCallbacks(stopRunnable)
            handler.postDelayed(stopRunnable, 30_000)
        }
    }

    override fun onInterrupt() {}
}
EOF

echo ""
echo "==> v1.0-combined ready at: $(pwd)/$ROOT"
echo ""
echo "Sanity check: no '\$' in Kotlin sources (templates fully disabled):"
if grep -rn '[\$]' "$ROOT"/app/src/main/java/ 2>/dev/null; then
    echo "WARNING: found dollar signs in Kotlin. This will break templates again."
else
    echo "  OK: zero dollar signs in Kotlin."
fi
echo ""
echo "Next:"
echo "  1. Android Studio -> Open -> $(pwd)/$ROOT"
echo "  2. Gradle Sync, Build -> Build APK(s)"
echo "  3. adb install -r -t app/build/outputs/apk/debug/app-debug.apk"
