package com.unkwn2.yandexhud

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class HudPresentation(context: Context, display: Display) : Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(context)
        root.setBackgroundColor(Color.parseColor("#CC000000"))
        val tv = TextView(context)
        tv.text = ">>> HUD NAV TEST <<<"
        tv.setTextColor(Color.GREEN)
        tv.textSize = 28f
        tv.gravity = Gravity.CENTER
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        root.addView(tv, lp)

        val sub = TextView(context)
        sub.text = "displayId=${display.displayId} ${display.name}"
        sub.setTextColor(Color.YELLOW)
        sub.textSize = 14f
        sub.gravity = Gravity.CENTER
        val lp2 = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp2.gravity = Gravity.CENTER or Gravity.BOTTOM
        root.addView(sub, lp2)

        val arrow = TextView(context)
        arrow.text = ">>>"
        arrow.setTextColor(Color.WHITE)
        arrow.textSize = 48f
        arrow.gravity = Gravity.CENTER
        val lp3 = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp3.gravity = Gravity.CENTER or Gravity.LEFT
        root.addView(arrow, lp3)

        setContentView(root)
    }
}
