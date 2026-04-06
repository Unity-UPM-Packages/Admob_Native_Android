package com.thelegends.ads.admob_native_unity.utils

import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.widget.TextView
import android.graphics.Typeface
import androidx.annotation.ColorInt

/**
 * Converts a Unity RGBA HTML color string (#RRGGBBAA) to an Android color integer (#AARRGGBB).
 * Returns [Color.TRANSPARENT] on any parse failure.
 */
@ColorInt
fun parseUnityColor(rgbaHtml: String): Int {
    return try {
        var str = rgbaHtml.removePrefix("#")
        if (str.length == 8) {
            val rrggbb = str.substring(0, 6)
            val aa = str.substring(6, 8)
            str = aa + rrggbb
        }
        Color.parseColor("#$str")
    } catch (e: Exception) {
        Color.TRANSPARENT
    }
}

/**
 * Converts a Unity TextAnchor enum name to an Android [Gravity] flag combination.
 * Defaults to [Gravity.CENTER] for unknown or null values.
 */
fun parseGravity(alignment: String?): Int {
    return when (alignment) {
        "UpperLeft"    -> Gravity.TOP    or Gravity.START
        "UpperCenter"  -> Gravity.TOP    or Gravity.CENTER_HORIZONTAL
        "UpperRight"   -> Gravity.TOP    or Gravity.END
        "MiddleLeft"   -> Gravity.CENTER_VERTICAL or Gravity.START
        "MiddleCenter" -> Gravity.CENTER
        "MiddleRight"  -> Gravity.CENTER_VERTICAL or Gravity.END
        "LowerLeft"    -> Gravity.BOTTOM or Gravity.START
        "LowerCenter"  -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        "LowerRight"   -> Gravity.BOTTOM or Gravity.END
        else           -> Gravity.CENTER
    }
}

/**
 * Applies bold/italic [Typeface] styling to a [TextView].
 * No-op when both flags are false (system default typeface is preserved).
 */
fun applyTypeface(tv: TextView, isBold: Boolean, isItalic: Boolean) {
    val style = when {
        isBold && isItalic -> Typeface.BOLD_ITALIC
        isBold             -> Typeface.BOLD
        isItalic           -> Typeface.ITALIC
        else               -> return
    }
    tv.setTypeface(null, style)
}
