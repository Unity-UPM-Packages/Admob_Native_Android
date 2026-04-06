package com.thelegends.ads.admob_native_unity.model

/**
 * Normalized anchor boundaries of the RootAdView element.
 * Defaults to full-screen (0,0) → (1,1) if no RootAdView is found in the JSON.
 */
data class RootBounds(
    val xMin: Float = 0f,
    val yMin: Float = 0f,
    val xMax: Float = 1f,
    val yMax: Float = 1f
)

/**
 * Top-level result from [AdLayoutParser]; contains all data needed to build and lay out the ad UI.
 *
 * @param referenceWidth  Unity Canvas reference width (pixels) used to compute the scaling baseline
 * @param referenceHeight Unity Canvas reference height (pixels) used to compute the scaling baseline
 * @param elements        Ordered list of parsed UI elements
 * @param rootBounds      Normalized screen-space bounds of the root ad container
 */
data class AdLayoutData(
    val referenceWidth: Float,
    val referenceHeight: Float,
    val elements: List<ElementData>,
    val rootBounds: RootBounds
)
