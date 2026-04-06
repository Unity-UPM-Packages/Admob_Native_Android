package com.thelegends.ads.admob_native_unity.image

import android.content.Context
import android.widget.ImageView
import org.json.JSONObject

/**
 * Contract for asynchronous image loading used by ad element view builders.
 * Implementations are responsible for caching, background decoding, and optional NinePatch rendering.
 *
 * Inject a custom implementation for testing or alternate caching strategies.
 */
interface ImageLoader {

    /**
     * Loads a standard bitmap from [path] and sets it on [imageView].
     * Uses cache when available; decodes from disk in the background otherwise.
     * Implementations MUST use a [java.lang.ref.WeakReference] to avoid leaking detached views.
     */
    fun load(imageView: ImageView, path: String)

    /**
     * Loads a bitmap from [path] and applies it to [imageView], optionally as a NinePatch
     * if [border] metadata is provided.
     *
     * @param border    JSON object with keys: left, right, top, bottom (Unity pixel units) and ppuMultiplier
     * @param screenH   Physical screen height in pixels (for scaling computation)
     * @param refH      Unity reference height in pixels (for scaling computation)
     */
    fun display(
        context: Context,
        imageView: ImageView,
        path: String,
        border: JSONObject?,
        screenH: Float,
        refH: Float
    )
}
