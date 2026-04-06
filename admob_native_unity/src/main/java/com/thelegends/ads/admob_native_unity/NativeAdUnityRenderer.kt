package com.thelegends.ads.admob_native_unity

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.thelegends.ads.admob_native_unity.coordinate.AdCoordinateMapper
import com.thelegends.ads.admob_native_unity.factory.AdElementViewFactory
import com.thelegends.ads.admob_native_unity.factory.ViewBuilderContext
import com.thelegends.ads.admob_native_unity.image.ImageLoader
import com.thelegends.ads.admob_native_unity.image.LruImageLoader
import com.thelegends.ads.admob_native_unity.layout.NormBoundsLayoutEngine
import com.thelegends.ads.admob_native_unity.parser.AdLayoutParser

/**
 * Core rendering engine for AdMob Native Ads driven by Unity-exported JSON blueprints.
 *
 * Responsibilities (orchestration only — all heavy logic is delegated):
 *  1. Parse the JSON blueprint via [AdLayoutParser]
 *  2. Map Unity coordinates to Android pixel space via [AdCoordinateMapper]
 *  3. Create typed Android Views via [AdElementViewFactory]
 *  4. Measure and lay out children via [NormBoundsLayoutEngine]
 *
 * Public API is intentionally minimal and backward-compatible with [DynamicShowBehavior]:
 *  - [buildFromJson]      : inflate the ad UI from a JSON string
 *  - [getRootPixelRect]   : retrieve the pixel rect of the root ad container
 *  - [registeredViews]    : map of semantic key → View for AdMob data binding
 *
 * All dependencies are injectable via the constructor for isolated unit testing.
 *
 * @param imageLoader  Provides async, cached image loading. Defaults to the shared [LruImageLoader].
 * @param parser       Parses JSON into typed models. Defaults to [AdLayoutParser].
 * @param viewFactory  Creates Views from element models. Defaults to [AdElementViewFactory.createDefault].
 */
class NativeAdUnityRenderer(
    context: Context,
    private val imageLoader: ImageLoader        = LruImageLoader.shared,
    private val parser:      AdLayoutParser     = AdLayoutParser(),
    private val viewFactory: AdElementViewFactory = AdElementViewFactory.createDefault()
) : FrameLayout(context) {

    /** Semantic key → View map populated by [buildFromJson]. Used by [DynamicShowBehavior] for data binding. */
    val registeredViews: HashMap<String, View> = HashMap()

    private var rootPixelRect = Rect(0, 0, 0, 0)
    private var layoutEngine: NormBoundsLayoutEngine? = null

    init {
        clipChildren  = false
        clipToPadding = false
    }

    /**
     * Returns the pixel-accurate bounding rect of the RootAdView on the physical screen.
     * Used by [DynamicShowBehavior] to size and position the [NativeAdView] anchor.
     */
    fun getRootPixelRect(): Rect = rootPixelRect

    // ── Main entry point ──────────────────────────────────────────────────────

    /**
     * Inflates a complete Android ad UI from a Unity-exported JSON blueprint.
     *
     * All views are created fresh on each call; existing children and registered views are cleared.
     * Logs an error (without throwing) if the JSON cannot be parsed.
     *
     * @param jsonString Raw JSON blueprint string from [DynamicShowBehavior]
     */
    fun buildFromJson(jsonString: String) {
        removeAllViews()
        registeredViews.clear()

        val layoutData = runCatching { parser.parse(jsonString) }.getOrElse { e ->
            Log.e(TAG, "Failed to parse ad JSON blueprint", e)
            return
        }

        if (layoutData.elements.isEmpty()) {
            Log.w(TAG, "Ad blueprint contains no elements — nothing to render")
            return
        }

        val metrics = context.resources.displayMetrics
        val mapper  = AdCoordinateMapper(metrics, layoutData.rootBounds)

        rootPixelRect = mapper.rootPixelRect
        layoutEngine  = NormBoundsLayoutEngine(mapper.screenHeight, layoutData.referenceHeight)

        val buildCtx = ViewBuilderContext(
            context         = context,
            imageLoader     = imageLoader,
            screenHeight    = mapper.screenHeight,
            referenceHeight = layoutData.referenceHeight
        )

        for (element in layoutData.elements) {
            val result = viewFactory.create(buildCtx, element) ?: continue

            // Convert Unity screen-space anchors → local normalized bounds + attach font size
            val normBounds = mapper.toNormBounds(
                anchorMin = element.anchorMin,
                anchorMax = element.anchorMax,
                fontSize  = element.textData?.fontSize
            )
            result.view.tag = normBounds

            // Register all views (primary + any composite sub-views such as "${type}_Text")
            result.additionalRegistrations.forEach { (key, view) -> registeredViews[key] = view }
            registeredViews[result.primaryKey] = result.view

            addView(result.view)
        }
    }

    // ── Layout overrides ──────────────────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val engine = layoutEngine ?: return
        engine.onMeasure(this, measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Do NOT call super.onLayout — we fully control child positioning via NormBounds
        val engine = layoutEngine ?: return
        engine.onLayout(this, measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    // ─────────────────────────────────────────────────────────────────────────

    private companion object {
        private const val TAG = "NativeAdUnityRenderer"
    }
}
