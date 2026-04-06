package com.thelegends.ads.admob_native_unity.model

/**
 * Data container for normalized element coordinates transformed from Unity Screen Space.
 * Values are in [0..1] range relative to the root ad container bounds.
 *
 * @param xMin  Left edge (0 = container left)
 * @param yMin  Bottom edge in Unity space (0 = container bottom), flipped to Android top by layout engine
 * @param xMax  Right edge (1 = container right)
 * @param yMax  Top edge in Unity space (1 = container top)
 * @param originalFontSize Original Unity font size in pixels, used by layout engine for density-aware scaling
 */
data class NormBounds(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val originalFontSize: Float? = null
)
