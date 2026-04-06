package com.thelegends.ads.admob_native_unity.factory

import android.graphics.Color
import android.widget.ImageView
import com.google.android.gms.ads.nativead.MediaView
import com.thelegends.ads.admob_native_unity.model.ElementData

/**
 * Builds an AdMob [MediaView] (video / cover image slot).
 * The SDK populates this view automatically after [NativeAdView.setNativeAd] is called.
 */
class MediaViewBuilder : ElementViewBuilder {

    override fun canHandle(data: ElementData): Boolean =
        data.elementType == "MediaView"

    override fun build(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult {
        val mediaView = MediaView(ctx.context)
        return ViewCreationResult(view = mediaView, primaryKey = data.elementType)
    }
}

/**
 * Builds a plain [ImageView] for the ad icon slot.
 * The SDK supplies the icon drawable after [NativeAdView.setNativeAd] is called.
 */
class IconViewBuilder : ElementViewBuilder {

    override fun canHandle(data: ElementData): Boolean =
        data.elementType == "IconView"

    override fun build(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult {
        val imageView = ImageView(ctx.context).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            setBackgroundColor(Color.TRANSPARENT)
        }
        return ViewCreationResult(view = imageView, primaryKey = data.elementType)
    }
}

/**
 * Builds a plain [android.view.View] placeholder for unrecognised element types
 * (e.g. RootAdView, which acts as a bounds anchor and needs no visual representation).
 * Always returns true from [canHandle] — must be the last builder in the list.
 */
class FallbackViewBuilder : ElementViewBuilder {

    override fun canHandle(data: ElementData): Boolean = true

    override fun build(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult {
        return ViewCreationResult(
            view       = android.view.View(ctx.context),
            primaryKey = data.elementType
        )
    }
}
