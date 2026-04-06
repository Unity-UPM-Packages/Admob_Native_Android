package com.thelegends.ads.admob_native_unity

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Manages global ad container layers (Buckets) with fixed Z-order (TranslationZ).
 * Ensures Native Ads are displayed in the correct depth order (Banner < MREC < Fullscreen).
 */
@SuppressLint("StaticFieldLeak")
object NativeAdLayerManager {

    var layerBanner: FrameLayout? = null
    var layerFullscreen: FrameLayout? = null

    private var isInitialized = false

    /**
     * Initializes the layer system by injecting transparent, touch-pass-through
     * FrameLayouts directly into the Unity Activity's root decor view.
     */
    fun init(activity: Activity) {
        if (isInitialized) return
        isInitialized = true

        // Locate the root viewing area of the Unity Android application
        val rootViewGroup = activity.window.decorView.findViewById<ViewGroup>(R.id.content)

        // Create invisible, touch-pass-through layers
        // Z-order constants: BannerLayer = 10f, FullscreenLayer = 100f
        layerBanner = createPassThroughLayer(activity, 10f).also { rootViewGroup.addView(it) }
        layerFullscreen = createPassThroughLayer(activity, 100f).also { rootViewGroup.addView(it) }
    }

    private fun createPassThroughLayer(activity: Activity, z: Float): FrameLayout {
        return FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Act as a pass-through layer: allow touch events to go to Unity if no ad is intercepted
            isClickable = false
            isFocusable = false
            clipChildren = false
            translationZ = z // Lock the Z-Index permanently
        }
    }

    /**
     * Retrieves a specific layer by name for ad placement.
     */
    fun getLayer(layerName: String): FrameLayout? {
        return when (layerName) {
            "Banner" -> layerBanner
            "FullScreen" -> layerFullscreen
            else -> layerBanner
        }
    }
}
