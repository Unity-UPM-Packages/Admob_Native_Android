package com.thelegends.ads.admob_native_unity.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

/**
 * LRU memory-cached [ImageLoader] implementation.
 *
 * Key design decisions:
 * - Cache allocation: 1/8 of the VM heap, consistent with the previous singleton approach.
 * - Thread pool: 2 threads (reduced from 4) — ad images are not performance-critical enough to
 *   justify 4 threads competing with the main UI thread.
 * - [WeakReference] on every async callback prevents leaking detached or recycled [ImageView]s
 *   (fixes the original dangling-reference bug).
 *
 * Use [LruImageLoader.shared] for the default app-wide instance, or inject a fresh instance per
 * [NativeAdUnityRenderer] for isolated testing.
 */
class LruImageLoader private constructor() : ImageLoader {

    companion object {
        /** App-wide shared instance. Use this in production. */
        val shared: LruImageLoader by lazy { LruImageLoader() }
    }

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // KB

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount / 1024
    }

    private val executor  = Executors.newFixedThreadPool(2)
    private val uiHandler = Handler(Looper.getMainLooper())

    // ── Public API ────────────────────────────────────────────────────────────

    override fun load(imageView: ImageView, path: String) {
        val cached = memoryCache.get(path)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        // WeakReference prevents holding the view alive after it has been detached
        val weakRef = WeakReference(imageView)
        executor.execute {
            try {
                val bitmap = BitmapFactory.decodeFile(path) ?: return@execute
                memoryCache.put(path, bitmap)
                uiHandler.post { weakRef.get()?.setImageBitmap(bitmap) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun display(
        context: Context,
        imageView: ImageView,
        path: String,
        border: JSONObject?,
        screenH: Float,
        refH: Float
    ) {
        if (border == null) {
            load(imageView, path)
            return
        }

        val cached = memoryCache.get(path)
        if (cached != null) {
            NinePatchApplier.apply(context, imageView, cached, border, screenH, refH)
            return
        }

        val weakRef = WeakReference(imageView)
        executor.execute {
            try {
                val bitmap = BitmapFactory.decodeFile(path) ?: return@execute
                memoryCache.put(path, bitmap)
                uiHandler.post {
                    weakRef.get()?.let { iv ->
                        NinePatchApplier.apply(context, iv, bitmap, border, screenH, refH)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
