package com.thelegends.ads.admob_native_unity.layout

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.thelegends.ads.admob_native_unity.model.NormBounds

/**
 * Stateless layout engine that positions and measures children of [NativeAdUnityRenderer]
 * according to their [NormBounds] tags.
 *
 * Extracted from [NativeAdUnityRenderer.onMeasure] / [NativeAdUnityRenderer.onLayout] to:
 *  - Remove layout logic duplication between the two overrides
 *  - Allow the engine to be unit-tested independently of the View system
 *  - Keep [NativeAdUnityRenderer] focused on orchestration only
 *
 * Coordinate conventions (mirrors [AdCoordinateMapper]):
 *  - X: 0 = left edge, 1 = right edge (same in both Unity and Android)
 *  - Y: [NormBounds.yMin] = Unity bottom, [NormBounds.yMax] = Unity top
 *         → flipped to Android top-down via `1f - yMax` / `1f - yMin`
 *
 * @param screenHeight   Physical screen height used for density-aware font scaling
 * @param referenceHeight Unity canvas reference height used as the font-scaling denominator
 */
class NormBoundsLayoutEngine(
    private val screenHeight: Float,
    private val referenceHeight: Float
) {

    // ── onMeasure pass ────────────────────────────────────────────────────────

    /**
     * Measures all direct children of [viewGroup] using their [NormBounds] tags.
     * Also recursively measures inner children of composite [FrameLayout] containers.
     *
     * Must be called after [ViewGroup.onMeasure] has run so that [w]/[h] are valid.
     */
    fun onMeasure(viewGroup: ViewGroup, w: Float, h: Float) {
        if (w <= 0 || h <= 0) return
        for (i in 0 until viewGroup.childCount) {
            val child  = viewGroup.getChildAt(i)
            val bounds = child.tag as? NormBounds ?: continue

            val childW = (w * (bounds.xMax - bounds.xMin)).toInt()
            val childH = (h * (bounds.yMax - bounds.yMin)).toInt()

            child.measure(
                View.MeasureSpec.makeMeasureSpec(childW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(childH, View.MeasureSpec.EXACTLY)
            )
            measureInnerChildren(child, childW, childH)
        }
    }

    private fun measureInnerChildren(child: View, parentW: Int, parentH: Int) {
        // Only process exact FrameLayout containers (not subclasses like NativeAdView)
        if (child.javaClass != FrameLayout::class.java) return
        val container = child as FrameLayout

        for (j in 0 until container.childCount) {
            val inner       = container.getChildAt(j)
            val innerBounds = inner.tag as? NormBounds

            if (innerBounds != null) {
                val icW = (parentW * (innerBounds.xMax - innerBounds.xMin)).toInt().coerceAtLeast(0)
                val icH = (parentH * (innerBounds.yMax - innerBounds.yMin)).toInt().coerceAtLeast(0)
                inner.measure(
                    View.MeasureSpec.makeMeasureSpec(icW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(icH, View.MeasureSpec.EXACTLY)
                )
            } else {
                // No sub-rect defined — fill the entire container (e.g. background ImageView)
                inner.measure(
                    View.MeasureSpec.makeMeasureSpec(parentW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(parentH, View.MeasureSpec.EXACTLY)
                )
            }
        }
    }

    // ── onLayout pass ─────────────────────────────────────────────────────────

    /**
     * Positions all direct children of [viewGroup] using their [NormBounds] tags,
     * then applies density-aware font scaling and recursively lays out composite inner children.
     *
     * Called in place of [ViewGroup.onLayout] (no super call needed).
     */
    fun onLayout(viewGroup: ViewGroup, w: Float, h: Float) {
        if (w <= 0 || h <= 0) return
        for (i in 0 until viewGroup.childCount) {
            val child  = viewGroup.getChildAt(i)
            val bounds = child.tag as? NormBounds ?: continue

            val cLeft   = (w * bounds.xMin).toInt()
            val cRight  = (w * bounds.xMax).toInt()
            val cTop    = (h * (1f - bounds.yMax)).toInt() // Y-axis flip
            val cBottom = (h * (1f - bounds.yMin)).toInt() // Y-axis flip
            child.layout(cLeft, cTop, cRight, cBottom)

            if (child is FrameLayout && child.javaClass == FrameLayout::class.java) {
                layoutInnerChildren(child, cRight - cLeft, cBottom - cTop)
            }

            scaleFontIfNeeded(child, bounds)
        }
    }

    private fun layoutInnerChildren(container: FrameLayout, cWidth: Int, cHeight: Int) {
        for (j in 0 until container.childCount) {
            val inner       = container.getChildAt(j)
            val innerBounds = inner.tag as? NormBounds

            if (innerBounds != null) {
                val icLeft   = (cWidth  * innerBounds.xMin).toInt()
                val icRight  = (cWidth  * innerBounds.xMax).toInt()
                val icTop    = (cHeight * (1f - innerBounds.yMax)).toInt()
                val icBottom = (cHeight * (1f - innerBounds.yMin)).toInt()
                inner.layout(icLeft, icTop, icRight, icBottom)
            } else {
                // No sub-rect — fill the container (e.g. background ImageView)
                inner.layout(0, 0, cWidth, cHeight)
            }
        }
    }

    // ── Font scaling ──────────────────────────────────────────────────────────

    /**
     * Applies pixel-based font scaling to [child] (or its first inner [TextView]) using
     * the stored [NormBounds.originalFontSize] and the screen/reference height ratio.
     * No-op when [NormBounds.originalFontSize] is null.
     */
    private fun scaleFontIfNeeded(child: View, bounds: NormBounds) {
        val fontSize = bounds.originalFontSize ?: return
        val scaleY         = screenHeight / referenceHeight
        val finalPixelSize = fontSize * scaleY

        when (child) {
            is TextView   -> child.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalPixelSize)
            is FrameLayout -> {
                for (j in 0 until child.childCount) {
                    val inner = child.getChildAt(j)
                    if (inner is TextView) {
                        inner.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalPixelSize)
                        break
                    }
                }
            }
        }
    }
}
