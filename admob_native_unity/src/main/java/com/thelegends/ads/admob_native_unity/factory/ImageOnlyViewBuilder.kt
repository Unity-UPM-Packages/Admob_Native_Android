package com.thelegends.ads.admob_native_unity.factory

import android.graphics.Color
import android.widget.ImageView
import com.thelegends.ads.admob_native_unity.model.ElementData
import com.thelegends.ads.admob_native_unity.model.ImageData

/**
 * Builds a single [ImageView] for elements that carry an image component but no text.
 *
 * Rendering logic:
 *  - If [ImageData.imagePath] is non-empty: loads the bitmap (async, cached) with optional NinePatch
 *  - Otherwise: fills the view with [ImageData.color] as a solid background color
 */
class ImageOnlyViewBuilder : ElementViewBuilder {

    override fun canHandle(data: ElementData): Boolean =
        data.hasImage && !data.hasText

    override fun build(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult {
        val imgData   = data.imageData!!
        val imageView = buildImageView(ctx, imgData)
        return ViewCreationResult(view = imageView, primaryKey = data.elementType)
    }
}

// ── Shared image-view construction helper ────────────────────────────────────

/**
 * Creates a [ImageView] configured for FIT_XY scaling, applying either
 * an async bitmap load or a solid background color depending on [imgData].
 * Used by both [ImageOnlyViewBuilder] and [CompositeViewBuilder].
 */
internal fun buildImageView(ctx: ViewBuilderContext, imgData: ImageData): ImageView {
    return ImageView(ctx.context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        if (imgData.imagePath.isNotEmpty()) {
            setBackgroundColor(Color.TRANSPARENT)
            ctx.imageLoader.display(
                ctx.context, this, imgData.imagePath,
                imgData.border, ctx.screenHeight, ctx.referenceHeight
            )
        } else {
            setBackgroundColor(parseColorSafe(imgData.color))
        }
    }
}

private fun parseColorSafe(htmlColor: String): Int {
    return try {
        var str = htmlColor.removePrefix("#")
        if (str.length == 8) str = str.substring(6, 8) + str.substring(0, 6)
        Color.parseColor("#$str")
    } catch (e: Exception) {
        Color.TRANSPARENT
    }
}
