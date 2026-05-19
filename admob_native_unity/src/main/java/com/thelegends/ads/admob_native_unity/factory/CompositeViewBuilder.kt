package com.thelegends.ads.admob_native_unity.factory

import android.widget.FrameLayout
import android.widget.TextView
import com.thelegends.ads.admob_native_unity.model.ElementData
import com.thelegends.ads.admob_native_unity.model.NormBounds

/**
 * Builds a layered container ([FrameLayout]) for elements that have both an image background
 * and an overlaid text label — the most common pattern for CTA buttons and icon+label combos.
 *
 * View hierarchy produced:
 * ```
 * FrameLayout (container)            ← registered as elementType
 *   ├── ImageView (background)       ← no tag; fills container
 *   └── TextView  (overlay label)   ← tag = NormBounds(textRect); registered as "${elementType}_Text"
 * ```
 *
 * The outer container's [NormBounds] tag (including font size) is applied by
 * [NativeAdUnityRenderer] after this builder returns, so font scaling works correctly.
 */
class CompositeViewBuilder : ElementViewBuilder {

    override fun canHandle(data: ElementData): Boolean =
        data.hasText && data.hasImage

    override fun build(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult {
        val textData  = data.textData!!
        val imageData = data.imageData!!

        val container = FrameLayout(ctx.context)

        // Layer 1 — background image (no tag, fills container via MATCH_PARENT params)
        val imageView = buildImageView(ctx, imageData)
        container.addView(
            imageView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Layer 2 — text overlay
        val tv = buildTextView(ctx, textData, includeFontPadding = true)

        // Tag the TextView with its optional sub-rect for inner-child layout pass
        tv.tag = buildInnerTextBounds(textData)

        container.addView(
            tv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        return ViewCreationResult(
            view       = container,
            primaryKey = data.elementType,
            additionalRegistrations = mapOf("${data.elementType}_Text" to tv)
        )
    }

    /**
     * Converts the text component's optional [rectTransform] into a [NormBounds] tag.
     * Falls back to full-coverage (0,0)→(1,1) when no sub-rect is defined.
     */
    private fun buildInnerTextBounds(textData: com.thelegends.ads.admob_native_unity.model.TextData): NormBounds {
        val rt = textData.rectTransform
        return if (rt != null) {
            NormBounds(
                xMin             = rt.anchorMin.first,
                yMin             = rt.anchorMin.second,
                xMax             = rt.anchorMax.first,
                yMax             = rt.anchorMax.second,
                originalFontSize = textData.fontSize
            )
        } else {
            NormBounds(0f, 0f, 1f, 1f, textData.fontSize)
        }
    }
}
