package com.unkwn2.yandexhud

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var client: ProtocolServiceClient
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.init(applicationContext)

        client = ProtocolServiceClient(applicationContext)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        scroll.addView(root)

        status = TextView(this).apply {
            text = "Log: ${FileLogger.path()}"
            textSize = 12f
        }
        root.addView(status)

        addBtn(root, "== HUD ACTIVATION (new) ==") {}
        addBtn(root, "H1  SettingDevice: HUD=Navi + NavFusion=ON") {
            HudSettingDirect.activateHudNavi(applicationContext)
        }
        addBtn(root, "H2  SettingDevice: READ HUD state") {
            HudSettingDirect.readHudState(applicationContext)
        }
        addBtn(root, "H3  SettingDevice: HUD=Default + NavFusion=OFF") {
            HudSettingDirect.deactivateHudNavi(applicationContext)
        }
        addBtn(root, "H4  RESTART AMAPSERVICE (force-stop + start)") {
            restartAmapService()
        }
        addBtn(root, "H5  pm grant SETTING_SET (run in BugJaeger)") {
            tryPmGrant()
        }

        addBtn(root, "== BROADCAST PATH (v20) ==") {}
        addBtn(root, "11  START AMAPSERVICE (both pkgs)") {
            HudBroadcaster.startAmapService(applicationContext)
        }
        addBtn(root, "12  KEY_TYPE=10003 STATE=8  GAODE START") {
            HudBroadcaster.sendStateGaodeStart(applicationContext)
        }
        addBtn(root, "13  KEY_TYPE=10003 STATE=10 ALT START") {
            HudBroadcaster.sendStateAltStart(applicationContext)
        }
        addBtn(root, "14  KEY_TYPE=10003 STATE=12 STOP") {
            HudBroadcaster.sendStateStop(applicationContext)
        }
        addBtn(root, "15  GUIDE SIMPLE  RIGHT 200m") {
            HudBroadcaster.sendGuideSimple(applicationContext, ManeuverMapper.RIGHT, 200)
        }
        addBtn(root, "16  GUIDE sweep icons 0..28") {
            Thread {
                for (i in 0..28) {
                    HudBroadcaster.sendGuideSimple(applicationContext, i, 150 + i * 10)
                    Thread.sleep(200)
                }
            }.start()
        }
        addBtn(root, "17  FULL INIT+GUIDE (STATE8 then 10001)") {
            Thread {
                HudBroadcaster.sendStateGaodeStart(applicationContext)
                Thread.sleep(300)
                val cal = Calendar.getInstance()
                val etaH = (cal.get(Calendar.HOUR_OF_DAY) + 1) % 24
                val etaM = cal.get(Calendar.MINUTE)
                HudBroadcaster.sendGuideFull(
                    applicationContext,
                    turnIcon = ManeuverMapper.RIGHT,
                    nextTurnIcon = ManeuverMapper.FRONT,
                    segRemainDist = 200,
                    nextSegRemainDist = 500,
                    routeRemainDist = 5200,
                    routeRemainTimeMin = 25,
                    nextRoadName = "测试路",
                    nextNextRoadName = "下一条路",
                    etaHour = etaH,
                    etaMinute = etaM
                )
            }.start()
        }
        addBtn(root, "18  FULL INIT+GUIDE  LEFT TURN") {
            Thread {
                HudBroadcaster.sendStateGaodeStart(applicationContext)
                Thread.sleep(300)
                val cal = Calendar.getInstance()
                val etaH = (cal.get(Calendar.HOUR_OF_DAY) + 1) % 24
                val etaM = cal.get(Calendar.MINUTE)
                HudBroadcaster.sendGuideFull(
                    applicationContext,
                    turnIcon = ManeuverMapper.LEFT,
                    nextTurnIcon = ManeuverMapper.RIGHT_FRONT,
                    segRemainDist = 350,
                    nextSegRemainDist = 800,
                    routeRemainDist = 12000,
                    routeRemainTimeMin = 45,
                    nextRoadName = "列宁大街",
                    nextNextRoadName = "胜利路",
                    etaHour = etaH,
                    etaMinute = etaM
                )
            }.start()
        }
        addBtn(root, "19  FULL STOP (STATE=12)") {
            HudBroadcaster.sendStateStop(applicationContext)
        }

        addBtn(root, "== PROTOCOL PATH (B/D/E) ==") {}
        addBtn(root, "01  BIND ProtocolService") { client.bind(); refresh() }
        addBtn(root, "02  UNBIND") { client.unbind(); refresh() }
        addBtn(root, "03  PATH E  getMapState(0..12) sweep") {
            for (i in 0..12) client.diagGetMapState(i)
        }
        addBtn(root, "04  PATH E  getNaviState + isForegroundState") {
            client.diagGetNaviState()
        }
        addBtn(root, "05  PATH E  setICompatibleIDVersion(1)") {
            client.diagSetCompatibleVersion(1)
        }
        addBtn(root, "06  PATH D  SUBSCRIBE 32001..32005 + 50001") {
            client.registerCallback(NaviState.CB_ROAD_TYPE)
            client.registerCallback(NaviState.CB_ROAD_SPEED_LIMIT)
            client.registerCallback(NaviState.CB_NAVI_STATUS)
            client.registerCallback(NaviState.CB_TBT_INFO)
            client.registerCallback(NaviState.CB_ACTION_DIS)
            client.registerCallback(NaviState.CB_REST_CARD_STATUS)
        }
        addBtn(root, "07  PATH D  registCallBack legacy (single)") {
            client.registCallbackLegacy()
        }
        addBtn(root, "08  PATH B  setProtocolModelData 30406 NAV_START") {
            client.setProtocolModelData(
                NaviState.PID_NAV_START,
                NaviState.ACTION_NAV_START_DRIVE,
                Int.MIN_VALUE
            )
        }
        addBtn(root, "09  PATH B  setProtocolModelData 30000 op=HUD_ON") {
            client.setProtocolModelData(
                NaviState.PID_MAP_OPERA,
                Int.MIN_VALUE,
                NaviState.OPERA_HUD_ON
            )
        }
        addBtn(root, "10  PATH B  setProtocolModelData 30000 op=HUD_OFF") {
            client.setProtocolModelData(
                NaviState.PID_MAP_OPERA,
                Int.MIN_VALUE,
                NaviState.OPERA_HUD_OFF
            )
        }

        addBtn(root, "== UTILS ==") {}
        addBtn(root, "20  OPEN Accessibility settings") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        addBtn(root, "21  SHOW LOG PATH") { refresh() }
        addBtn(root, "22  CLEAR LOG") { FileLogger.clear(); refresh() }

        setContentView(scroll)
        refresh()
        FileLogger.log("HUD-Main", "v21 created. Log at ${FileLogger.path()}")
    }

    private fun restartAmapService() {
        Thread {
            runCatching {
                val p = Runtime.getRuntime().exec(arrayOf("am", "force-stop", "com.byd.amapservice"))
                p.waitFor()
                FileLogger.log("HUD-Main", "force-stop rc=${p.exitValue()}")
            }.onFailure { FileLogger.log("HUD-Main", "force-stop FAIL: $it") }
            Thread.sleep(2000)
            runCatching {
                val p = Runtime.getRuntime().exec(arrayOf(
                    "am", "startservice",
                    "-n", "com.byd.amapservice/com.example.amapservice.AmapService"
                ))
                p.waitFor()
                FileLogger.log("HUD-Main", "startservice rc=${p.exitValue()}")
            }.onFailure { FileLogger.log("HUD-Main", "startservice FAIL: $it") }
            Thread.sleep(1000)
            HudBroadcaster.sendStateGaodeStart(applicationContext)
            Thread.sleep(300)
            val cal = Calendar.getInstance()
            val etaH = (cal.get(Calendar.HOUR_OF_DAY) + 1) % 24
            val etaM = cal.get(Calendar.MINUTE)
            HudBroadcaster.sendGuideFull(
                applicationContext,
                turnIcon = ManeuverMapper.RIGHT,
                nextTurnIcon = ManeuverMapper.FRONT,
                segRemainDist = 200,
                nextSegRemainDist = 500,
                routeRemainDist = 5200,
                routeRemainTimeMin = 25,
                nextRoadName = "测试路",
                nextNextRoadName = "下一条路",
                etaHour = etaH,
                etaMinute = etaM
            )
        }.start()
    }

    private fun tryPmGrant() {
        Thread {
            for (perm in listOf(
                "com.byd.permission.BYDAUTO_SETTING_SET",
                "com.byd.permission.BYDAUTO_INSTRUMENT_SET"
            )) {
                runCatching {
                    val p = Runtime.getRuntime().exec(arrayOf(
                        "pm", "grant", "com.unkwn2.yandexhud", perm
                    ))
                    p.waitFor()
                    val out = p.inputStream.bufferedReader().readText()
                    val err = p.errorStream.bufferedReader().readText()
                    FileLogger.log("HUD-Main", "pm grant $perm rc=${p.exitValue()} out=$out err=$err")
                }.onFailure { FileLogger.log("HUD-Main", "pm grant $perm FAIL: $it") }
            }
        }.start()
    }

    private fun addBtn(parent: LinearLayout, label: String, onClick: () -> Unit) {
        val b = Button(this).apply {
            text = label
            setOnClickListener {
                runCatching { onClick() }.onFailure { FileLogger.log("HUD-Main", "btn ex: $it") }
                refresh()
            }
        }
        parent.addView(b, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun refresh() {
        status.text = "bound=${client.isBound()}  log=${FileLogger.path()}"
    }

    override fun onDestroy() {
        client.unbind()
        super.onDestroy()
    }
}
