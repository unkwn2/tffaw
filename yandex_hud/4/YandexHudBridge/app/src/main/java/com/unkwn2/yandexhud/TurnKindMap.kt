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
