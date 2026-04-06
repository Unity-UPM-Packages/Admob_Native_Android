package com.thelegends.ads.admob_native_unity.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.NinePatch
import android.graphics.Rect
import android.graphics.drawable.NinePatchDrawable
import android.widget.ImageView
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Stateless utility for constructing and applying [NinePatchDrawable] to an [ImageView].
 *
 * The NinePatch chunk binary format follows the Android internal structure:
 *   1 byte  = 1 (wasDeserialized flag)
 *   1 byte  = numXDivs
 *   1 byte  = numYDivs
 *   1 byte  = numColors
 *   8 bytes = padding (reserved)
 *   16 bytes = bounds padding (4 ints, reserved as 0)
 *   4 bytes = padding (reserved)
 *   then xDivs, yDivs, colors arrays as int32 arrays
 */
object NinePatchApplier {

    private const val NINE_PATCH_NO_COLOR = 0x00000001

    /**
     * Scales [bitmap] according to the [border] metadata and applies it as a NinePatch background
     * to [imageView]. Falls back to a standard stretched bitmap if the chunk cannot be constructed.
     *
     * @param border    JSON with keys: left, right, top, bottom (Unity pixel units) and ppuMultiplier
     * @param screenH   Physical screen height in pixels
     * @param refH      Unity canvas reference height in pixels
     */
    fun apply(
        context: Context,
        imageView: ImageView,
        bitmap: Bitmap,
        border: JSONObject,
        screenH: Float,
        refH: Float
    ) {
        val ppum = border.optDouble("ppuMultiplier", 1.0).toFloat().takeIf { it > 0f } ?: 1f
        // Scale = (1 / ppuMultiplier) * (screenHeight / referenceHeight)
        val targetScale = (1f / ppum) * (screenH / refH)

        val oLeft   = border.optDouble("left",   0.0).toFloat()
        val oBottom = border.optDouble("bottom", 0.0).toFloat()
        val oRight  = border.optDouble("right",  0.0).toFloat()
        val oTop    = border.optDouble("top",    0.0).toFloat()

        // No border defined — display as a plain stretched bitmap
        if (oLeft == 0f && oBottom == 0f && oRight == 0f && oTop == 0f) {
            imageView.setImageBitmap(bitmap)
            return
        }

        val scaledBw = (bitmap.width  * targetScale).toInt().coerceAtLeast(1)
        val scaledBh = (bitmap.height * targetScale).toInt().coerceAtLeast(1)

        val scaledBitmap = try {
            if (abs(targetScale - 1f) < 0.01f) bitmap
            else Bitmap.createScaledBitmap(bitmap, scaledBw, scaledBh, true)
        } catch (e: Exception) {
            bitmap
        }

        val left   = (oLeft   * targetScale).toInt()
        val right  = (oRight  * targetScale).toInt()
        val top    = (oTop    * targetScale).toInt()
        val bottom = (oBottom * targetScale).toInt()

        val bw = scaledBitmap.width
        val bh = scaledBitmap.height

        try {
            val xDivs = intArrayOf(left, bw - right)
            val yDivs = intArrayOf(top, bh - bottom)

            if (xDivs[0] >= xDivs[1] || yDivs[0] >= yDivs[1]) {
                imageView.setImageBitmap(scaledBitmap)
                return
            }

            val numColors = 9
            val colors    = IntArray(numColors) { NINE_PATCH_NO_COLOR }

            val buffer = ByteBuffer
                .allocate(32 + (xDivs.size + yDivs.size + colors.size) * 4)
                .order(ByteOrder.nativeOrder())

            // Binary header (standard Android NinePatch chunk)
            buffer.put(1.toByte())              // wasDeserialized
            buffer.put(xDivs.size.toByte())     // numXDivs
            buffer.put(yDivs.size.toByte())     // numYDivs
            buffer.put(numColors.toByte())       // numColors
            buffer.putInt(0)                    // xDivsOffset (reserved)
            buffer.putInt(0)                    // yDivsOffset (reserved)
            buffer.putInt(0); buffer.putInt(0)  // padding left/right
            buffer.putInt(0); buffer.putInt(0)  // padding top/bottom
            buffer.putInt(0)                    // colorsOffset (reserved)

            for (v in xDivs) buffer.putInt(v)
            for (v in yDivs) buffer.putInt(v)
            for (c in colors) buffer.putInt(c)

            val chunkBytes = buffer.array()

            if (NinePatch.isNinePatchChunk(chunkBytes)) {
                scaledBitmap.density = context.resources.displayMetrics.densityDpi
                val drawable = NinePatchDrawable(
                    context.resources, scaledBitmap, chunkBytes, Rect(), null
                )
                imageView.setImageDrawable(null)
                imageView.background = drawable
            } else {
                imageView.setImageBitmap(scaledBitmap)
            }
        } catch (e: Exception) {
            imageView.setImageBitmap(scaledBitmap)
        }
    }
}
