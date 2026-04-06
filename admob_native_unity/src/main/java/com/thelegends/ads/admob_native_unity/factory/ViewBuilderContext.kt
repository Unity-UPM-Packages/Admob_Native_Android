package com.thelegends.ads.admob_native_unity.factory

import android.content.Context
import android.view.View
import com.thelegends.ads.admob_native_unity.image.ImageLoader

/**
 * Immutable bundle of dependencies passed to every [ElementViewBuilder.build] call.
 * Avoids long parameter lists and makes it easy to add new cross-cutting concerns
 * (e.g. a font provider) without changing every builder's signature.
 */
data class ViewBuilderContext(
    /** Android application/activity context for View construction. */
    val context: Context,
    /** Injected image loader — swap out for a test double during unit tests. */
    val imageLoader: ImageLoader,
    /** Physical screen height in pixels, used for NinePatch and font scaling. */
    val screenHeight: Float,
    /** Unity canvas reference height in pixels, used as the scaling denominator. */
    val referenceHeight: Float
)

/**
 * Result returned by [ElementViewBuilder.build].
 *
 * @param view                   The top-level view to insert into the ad container
 * @param primaryKey             Map key used to register [view] in [NativeAdUnityRenderer.registeredViews]
 * @param additionalRegistrations Extra (key → view) pairs to also register (e.g. "Headline_Text" → TextView)
 */
data class ViewCreationResult(
    val view: View,
    val primaryKey: String,
    val additionalRegistrations: Map<String, View> = emptyMap()
)
