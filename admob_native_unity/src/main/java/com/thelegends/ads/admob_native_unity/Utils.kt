package com.thelegends.ads.admob_native_unity

import android.content.Context
import kotlin.math.roundToInt

class Utils {
    companion object {
        fun dpToPx(dp: Int, context: Context): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        fun pxToDp(px: Int, context: Context): Float {
            val density = context.resources.displayMetrics.density
            if (density == 0f) {
                return px.toFloat() // tránh chia cho 0
            }
            return px / density
        }
    }
}

