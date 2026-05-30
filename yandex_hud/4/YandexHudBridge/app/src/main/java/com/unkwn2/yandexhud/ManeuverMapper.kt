package com.unkwn2.yandexhud

object ManeuverMapper {
    const val FRONT = 1
    const val LEFT_FRONT = 2
    const val RIGHT_FRONT = 3
    const val LEFT = 4
    const val RIGHT = 5
    const val LEFT_BACK = 6
    const val RIGHT_BACK = 7
    const val LEFT_UTURN = 8
    const val RIGHT_UTURN = 9
    const val UTURN = 10
    const val RING = 11
    const val LEFT_RING = 12
    const val RIGHT_RING = 13
    const val DEST = 14
    const val SERVICE = 15
    const val TOLL = 16
    const val DEST_FINAL = 17

    // Yandex -> BYD mapping (GAODE icon compatibility)
    // AmapService uses gaode[] array for this mapping
    fun yandexToByd(yandexAction: String): Int = when (yandexAction) {
        "straight", "go_ahead" -> FRONT
        "turn_slight_left", "keep_left" -> LEFT_FRONT
        "turn_slight_right", "keep_right" -> RIGHT_FRONT
        "turn_left" -> LEFT
        "turn_right" -> RIGHT
        "turn_sharp_left" -> LEFT_BACK
        "turn_sharp_right" -> RIGHT_BACK
        "uturn", "uturn_left" -> UTURN
        "roundabout", "leave_roundabout" -> RING
        "finish", "arrive" -> DEST_FINAL
        else -> FRONT
    }
}
