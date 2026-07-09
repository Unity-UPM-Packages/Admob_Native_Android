package com.thelegends.ads.admob_native_unity.factory

import com.thelegends.ads.admob_native_unity.model.ElementData

/**
 * Strategy interface for building a single Android [android.view.View] from an [ElementData] descriptor.
 *
 * Implementations are registered in priority order inside [AdElementViewFactory].
 * The first builder whose [canHandle] returns true is selected; subsequent builders are skipped.
 *
 * Adding support for a new element type (Open/Closed Principle) only requires:
 *  1. Creating a new [ElementViewBuilder] implementation
 *  2. Registering it in [AdElementViewFactory.createDefault] — no existing code needs to change.
 */
interface ElementViewBuilder {

    /**
     * Returns true when this builder is capable of constructing a view for [data].
     * The check is typically based on [ElementData.elementType], [ElementData.hasText],
     * and [ElementData.hasImage].
     */
    fun canHandle(data: ElementData): Boolean

    /**
     * Constructs and returns a [ViewCreationResult] for [data].
     * Called only when [canHandle] previously returned true.
     *
     * Implementations MUST NOT cache [ViewBuilderContext.context]; it may refer to an Activity.
     */
    fun build(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult
}

/**
 * Dispatches element data to the correct [ElementViewBuilder] strategy.
 *
 * Builders are evaluated in the order they appear in [builders]; the first match wins.
 * Always inject a [FallbackViewBuilder] as the last entry to guarantee a result is returned.
 *
 * @param builders Ordered list of builders; inject a custom list to extend or override behaviour.
 */
class AdElementViewFactory(private val builders: List<ElementViewBuilder>) {

    /**
     * Returns the [ViewCreationResult] from the first compatible builder,
     * or null if no builder accepts [data] (should not happen if [FallbackViewBuilder] is registered).
     */
    fun create(ctx: ViewBuilderContext, data: ElementData): ViewCreationResult? =
        builders.firstOrNull { it.canHandle(data) }?.build(ctx, data)

    companion object {
        /**
         * Default factory covering all standard ad element types:
         * MediaView, IconView, Composite (text+image), Image-only, Text-only, and Fallback.
         */
        fun createDefault(): AdElementViewFactory = AdElementViewFactory(
            listOf(
                MediaViewBuilder(),
                IconViewBuilder(),
                MainImageViewBuilder(),
                CompositeViewBuilder(),
                ImageOnlyViewBuilder(),
                TextOnlyViewBuilder(),
                FallbackViewBuilder()
            )
        )
    }
}
