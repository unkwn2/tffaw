package com.unkwn2.yandexhud

import android.content.Context
import android.content.Intent
import android.os.Bundle

object HudBroadcaster {
    private const val ACTION = "AUTONAVI_STANDARD_BROADCAST_SEND"
    private const val TARGET_PKG = "com.byd.amapservice"
    private const val TAG = "HUD-Bcast"

    private const val KEY_TYPE_NAVI_DATA = 10001
    private const val KEY_TYPE_STATE = 10003
    private const val EXTRA_STATE_STOP = 12
    private const val EXTRA_STATE_GAODE_START = 8
    private const val NAVI_STATE_START = 1

    fun sendNaviStart(ctx: Context) {
        val b = Bundle().apply {
            putBoolean("IS_BYD_MAP", false)
            putBoolean("IS_BYD_BAIDU_MAP", false)
            putInt("KEY_TYPE", KEY_TYPE_STATE)
            putInt("EXTRA_STATE", EXTRA_STATE_GAODE_START)
        }
        send(ctx, b)
        FileLogger.write(TAG, "NAVI_START GAODE")
    }

    fun sendNaviStop(ctx: Context) {
        val b = Bundle().apply {
            putBoolean("IS_BYD_MAP", false)
            putBoolean("IS_BYD_BAIDU_MAP", false)
            putInt("KEY_TYPE", KEY_TYPE_STATE)
            putInt("EXTRA_STATE", EXTRA_STATE_STOP)
        }
        send(ctx, b)
        FileLogger.write(TAG, "NAVI_STOP")
    }

    fun sendGuideInfo(
        ctx: Context,
        turnIcon: Int,
        distanceMeters: Int,
        nextRoadName: String,
        routeRemainDist: Int,
        routeRemainTime: Int,
        etaText: String
    ) {
        val b = Bundle().apply {
            putBoolean("IS_BYD_MAP", false)
            putBoolean("IS_BYD_BAIDU_MAP", false)
            putInt("KEY_TYPE", KEY_TYPE_NAVI_DATA)
            putInt("TYPE", NAVI_STATE_START)
            putInt("NEW_ICON", turnIcon)
            putInt("NEXT_NEXT_TURN_ICON", 0)
            putInt("NEXT_SEG_REMAIN_DIS", distanceMeters)
            putInt("SEG_REMAIN_DIS", distanceMeters)
            putString("NEXT_ROAD_NAME", nextRoadName)
            putString("NEXT_NEXT_ROAD_NAME", "")
            putInt("ROUTE_REMAIN_DIS", routeRemainDist)
            putInt("ROUTE_REMAIN_TIME", routeRemainTime)
            putString("ETA_TEXT", etaText)
            putString("ROUTE_REMAIN_TIME_AUTO", etaText)
            putString("ROUTE_REMAIN_DIS_AUTO", "${routeRemainDist}米")
            putString("SEG_REMAIN_DIS_AUTO", "${distanceMeters}米")
            putInt("ROUNG_ABOUT_NUM", -1)
            putInt("NEXT_ROUNG_ABOUT_NUM", -1)
            putInt("TRAFFIC_LIGHT_NUM", 0)
        }
        send(ctx, b)
        FileLogger.write(TAG, "GUIDE icon=$turnIcon dist=$distanceMeters road=$nextRoadName")
    }

    fun sendTestFull(ctx: Context) {
        sendNaviStart(ctx)
        Thread.sleep(200)
        sendGuideInfo(
            ctx,
            turnIcon = 3,
            distanceMeters = 500,
            nextRoadName = "ул. Ленина",
            routeRemainDist = 15000,
            routeRemainTime = 45,
            etaText = "预计今天14时30分到达"
        )
    }

    fun sendTestStop(ctx: Context) {
        sendNaviStop(ctx)
    }

    fun sendIconSweep(ctx: Context) {
        sendNaviStart(ctx)
        Thread.sleep(200)
        for (i in 0..28) {
            sendGuideInfo(
                ctx,
                turnIcon = i,
                distanceMeters = 150 + i * 10,
                nextRoadName = "Test $i",
                routeRemainDist = 10000,
                routeRemainTime = 30,
                etaText = "预计今天15时0分到达"
            )
            Thread.sleep(800)
        }
    }

    private fun send(ctx: Context, extras: Bundle) {
        val i = Intent(ACTION).apply {
            setPackage(TARGET_PKG)
            putExtras(extras)
        }
        ctx.sendBroadcast(i)
    }
}
