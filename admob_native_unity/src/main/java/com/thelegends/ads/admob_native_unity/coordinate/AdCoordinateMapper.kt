package com.thelegends.ads.admob_native_unity.coordinate

import android.graphics.Rect
import com.thelegends.ads.admob_native_unity.model.NormBounds
import com.thelegends.ads.admob_native_unity.model.RootBounds

/**
 * Transforms Unity screen-space anchor coordinates into Android pixel rectangles and
 * into normalized local-space [NormBounds] relative to the root ad container.
 *
 * Coordinate system differences handled here:
 *  - Unity: Y=0 at bottom, Y=1 at top
 *  - Android: Y=0 at top, increasing downward
 *
 * IMPORTANT: [screenWidthPx] and [screenHeightPx] must be the REAL full-screen
 * dimensions (including navigation bar / status bar area), NOT the values from
 * `context.resources.displayMetrics` which may exclude system UI decorations.
 * Unity's `Screen.width/height` always reports the full physical resolution,
 * so the native side must match to avoid coordinate misalignment on devices
 * with virtual navigation bars.
 *
 * @param screenWidthPx   Full physical screen width in pixels
 * @param screenHeightPx  Full physical screen height in pixels
 * @param rootBounds      Normalized Unity screen-space bounds of the RootAdView element
 */
class AdCoordinateMapper(
    screenWidthPx: Int,
    screenHeightPx: Int,
    private val rootBounds: RootBounds
) {
    val screenWidth:  Float = screenWidthPx.toFloat()
    val screenHeight: Float = screenHeightPx.toFloat()

    /** Pixel-precise bounding rect of the root ad container on the physical screen. */
    val rootPixelRect: Rect by lazy {
        val left   = (screenWidth  * rootBounds.xMin).toInt()
        val top    = (screenHeight * (1f - rootBounds.yMax)).toInt() // Y-axis flip
        val right  = (screenWidth  * rootBounds.xMax).toInt()
        val bottom = (screenHeight * (1f - rootBounds.yMin)).toInt() // Y-axis flip
        Rect(left, top, right, bottom)
    }

    private val rootW: Float = (rootBounds.xMax - rootBounds.xMin).takeIf { it > 0f } ?: 1f
    private val rootH: Float = (rootBounds.yMax - rootBounds.yMin).takeIf { it > 0f } ?: 1f

    /**
     * Projects a Unity screen-space anchor pair into local-space normalized bounds
     * relative to the root container. The output is clamped to [0..1].
     *
     * @param anchorMin   (x, y) bottom-left anchor in Unity screen space
     * @param anchorMax   (x, y) top-right anchor in Unity screen space
     * @param fontSize    Optional original font size to carry through for density-aware scaling
     */
    fun toNormBounds(
        anchorMin: Pair<Float, Float>,
        anchorMax: Pair<Float, Float>,
        fontSize: Float? = null
    ): NormBounds {
        val localXMin = ((anchorMin.first  - rootBounds.xMin) / rootW).coerceIn(0f, 1f)
        val localYMin = ((anchorMin.second - rootBounds.yMin) / rootH).coerceIn(0f, 1f)
        val localXMax = ((anchorMax.first  - rootBounds.xMin) / rootW).coerceIn(0f, 1f)
        val localYMax = ((anchorMax.second - rootBounds.yMin) / rootH).coerceIn(0f, 1f)
        return NormBounds(localXMin, localYMin, localXMax, localYMax, fontSize)
    }
}
