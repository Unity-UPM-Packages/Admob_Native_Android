package com.thelegends.ads.admob_native_unity.factory

import android.text.TextUtils
import android.widget.TextView
import com.thelegends.ads.admob_native_unity.model.ElementData
import com.thelegends.ads.admob_native_unity.model.TextData
import com.thelegends.ads.admob_native_unity.utils.applyTypeface
import com.thelegends.ads.admob_native_unity.utils.parseGravity
import com.thelegends.ads.admob_native_unity.utils.parseUnityColor

/**
 * Builds a single [TextView] for elements that carry only a text component.
 *
 * Notable behaviours:
 *  - [TextView.includeFontPadding] = false (improves vertical centering for single-line labels)
 *  - maxLines respects the parsed value (currently always 1)
 *  - Font size storage in [view.tag] is handled externally by the layout engine
 */
class TextOnlyViewBuilder : ElementViewBuilder {

    override fun canHandle(data: ElementData): Boolean =
        data.hasText && !data.hasImage

    override fun build(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult {
        val textData = data.textData!!
        val tv       = buildTextView(ctx, textData, includeFontPadding = false)
        return ViewCreationResult(view = tv, primaryKey = data.elementType)
    }
}

// ── Shared text-view construction helper ─────────────────────────────────────

/**
 * Creates a styled [TextView] from [textData].
 * Used by both [TextOnlyViewBuilder] and [CompositeViewBuilder].
 *
 * @param includeFontPadding true for composite overlays (preserves visual headroom),
 *                           false for standalone text labels (tighter vertical centering)
 */
internal fun buildTextView(
    ctx: ViewBuilderContext,
    textData: TextData,
    includeFontPadding: Boolean
): TextView {
    return TextView(ctx.context).apply {
        text                    = textData.textContent
        setTextColor(parseUnityColor(textData.color))
        gravity                 = parseGravity(textData.alignment)
        this.includeFontPadding = includeFontPadding
        maxLines                = textData.maxLines
        ellipsize               = TextUtils.TruncateAt.END
        applyTypeface(this, textData.isBold, textData.isItalic)
        // Font size is intentionally NOT set here — the NormBoundsLayoutEngine applies
        // density-aware scaling in onLayout() based on NormBounds.originalFontSize.
    }
}
