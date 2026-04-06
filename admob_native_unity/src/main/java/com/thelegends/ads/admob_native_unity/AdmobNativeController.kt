// File: mylibrary/src/main/java/com/mycompany/admobnative/AdmobNativeController.kt
package com.thelegends.admob_native_unity

import android.app.Activity
import android.util.Log
import android.view.View
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.ads.admob_native_unity.decorator.*
import com.thelegends.ads.admob_native_unity.showbehavior.*
import java.lang.ref.WeakReference

/**
 * Main Controller for AdMob Native Ads on Android.
 * Orchestrates the full lifecycle of a native ad: Load → Show → Interaction → Destroy.
 * Supports hybrid rendering (XML-based or Dynamic JSON-based).
 *
 * Design notes:
 * - [activity] is held as a [WeakReference] to prevent Activity leaks across configuration changes.
 * - [getWidthInPixels] / [getHeightInPixels] use async callbacks instead of blocking FutureTask
 *   to eliminate deadlock risk when called from the main thread.
 * - [layoutJsonConfig] and [zLayerConfig] are annotated @Volatile to prevent race conditions
 *   when written from Unity's JNI bridge thread and read on the Android UI thread.
 */
class AdmobNativeController(
    activity: Activity,
    private val callbacks: NativeAdCallbacks
) {
    // FIX #1: WeakReference prevents Activity Leak across configuration changes / session reuse
    private val activityRef: WeakReference<Activity> = WeakReference(activity)

    private var loadedNativeAd: NativeAd? = null
    private var currentShowBehavior: IShowBehavior? = null

    private val TAG = "AdmobNativeController"

    // ── Core Lifecycle ────────────────────────────────────────────────────────

    fun loadAd(adUnitId: String, adRequest: AdRequest) {
        val activity = activityRef.get() ?: run {
            Log.e(TAG, "Activity has been destroyed. Cannot load ad.")
            return
        }

        activity.runOnUiThread {
            Log.d(TAG, "Loading native ad for Ad Unit ID: $adUnitId on UI thread.")

            val videoOptions = VideoOptions.Builder()
                .setStartMuted(true)
                .setCustomControlsRequested(false)
                .setClickToExpandRequested(false)
                .build()

            val adOptions = com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build()

            AdLoader.Builder(activity, adUnitId)
                .forNativeAd { ad ->
                    activity.runOnUiThread {
                        Log.d(TAG, "Ad loaded successfully.")
                        this.loadedNativeAd = ad
                        setupAdCallbacks(ad)
                        callbacks.onAdLoaded()
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        activity.runOnUiThread {
                            Log.d(TAG, "Ad load failed: ${adError.message}")
                            callbacks.onAdFailedToLoad(adError)
                        }
                    }

                    override fun onAdClicked() {
                        activity.runOnUiThread {
                            callbacks.onAdClicked()
                            Log.d(TAG, "Ad clicked.")
                        }
                    }

                    override fun onAdImpression() {
                        activity.runOnUiThread {
                            callbacks.onAdDidRecordImpression()
                            Log.d(TAG, "Ad impression recorded.")
                        }
                    }

                    override fun onAdOpened() {
                        activity.runOnUiThread {
                            callbacks.onAdShowedFullScreenContent()
                            Log.d(TAG, "Ad opened Full Screen Content.")
                        }
                    }

                    override fun onAdClosed() {
                        activity.runOnUiThread {
                            callbacks.onAdDismissedFullScreenContent()
                            Log.d(TAG, "Ad closed Full Screen Content.")
                        }
                    }
                })
                .withNativeAdOptions(adOptions)
                .build()
                .loadAd(adRequest)
        }
    }

    fun showAd(layoutName: String) {
        val activity = activityRef.get() ?: run {
            Log.e(TAG, "Activity has been destroyed. Cannot show ad.")
            return
        }

        Log.d(TAG, "Show ads")
        val adToShow = loadedNativeAd ?: run {
            Log.e(TAG, "Ad not available. Call loadAd() first.")
            return
        }

        currentShowBehavior?.destroy()

        // Hybrid Assembly: Choose behavior based on whether a JSON layout is provided
        val dynamicBehavior: DynamicShowBehavior? = if (layoutJsonConfig != null && zLayerConfig != null) {
            DynamicShowBehavior(layoutJsonConfig!!, zLayerConfig!!)
        } else null

        // Capture direct reference before decorators wrap it, for reliable dimension queries later
        innerDynamicBehavior = dynamicBehavior

        var behavior: BaseShowBehavior = dynamicBehavior ?: BaseShowBehavior()

        if (countdownConfig != null) {
            behavior = CountdownDecorator(
                behavior,
                countdownConfig!!.initial,
                countdownConfig!!.duration,
                countdownConfig!!.closeDelay
            )
        }

        behavior.show(activity, adToShow, layoutName, callbacks)
        currentShowBehavior = behavior

        callbacks.onAdShow()
    }

    fun destroyAd() {
        activity.runOnUiThread {
            resetAllConfigs()

            currentShowBehavior?.destroy()
            currentShowBehavior = null
            innerDynamicBehavior = null

            loadedNativeAd?.destroy()
            loadedNativeAd = null

            callbacks.onAdClosed()

            Log.d(TAG, "Native ad has been destroyed.")
        }
    }

    fun isAdAvailable(): Boolean = loadedNativeAd != null

    fun getResponseInfo(): ResponseInfo? = loadedNativeAd?.responseInfo

    // ── Video & Revenue Callbacks ─────────────────────────────────────────────

    private fun setupAdCallbacks(ad: NativeAd) {
        val videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
            override fun onVideoStart() {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video started.")
                    callbacks.onVideoStart()
                }
            }

            override fun onVideoPlay() {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video played.")
                    callbacks.onVideoPlay()
                }
            }

            override fun onVideoPause() {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video paused.")
                    callbacks.onVideoPause()
                }
            }

            override fun onVideoEnd() {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video ended.")
                    callbacks.onVideoEnd()
                }
            }

            override fun onVideoMute(isMuted: Boolean) {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video isMuted: $isMuted.")
                    callbacks.onVideoMute(isMuted)
                }
            }
        }
        ad.mediaContent?.videoController?.videoLifecycleCallbacks = videoLifecycleCallbacks

        ad.setOnPaidEventListener { adValue ->
            activityRef.get()?.runOnUiThread {
                Log.d(TAG, "Paid event received: $adValue")
                callbacks.onPaidEvent(
                    adValue.precisionType,
                    adValue.valueMicros,
                    adValue.currencyCode
                )
            }
        }
    }

    // ── Dimension Queries ─────────────────────────────────────────────────────

    /**
     * Queries the rendered ad width asynchronously and delivers the result via [onResult].
     *
     * FIX #2: Replaced blocking FutureTask.get() with a non-blocking async callback to
     * eliminate the deadlock that occurred when this method was called from the UI thread.
     *
     * For Dynamic layouts, always returns the root ad rect width via [DynamicShowBehavior].
     * For XML layouts, falls back to the "ad_content" view lookup.
     *
     * @param onResult Called on the UI thread with the width in pixels, or -1 if unavailable.
     */
    fun getWidthInPixels(onResult: (Float) -> Unit) {
        val activity = activityRef.get() ?: run {
            onResult(-1f)
            return
        }
        activity.runOnUiThread {
            val width = resolveAdWidth(activity)
            Log.d(TAG, "Ad width resolved: $width px")
            onResult(width)
        }
    }

    /**
     * Queries the rendered ad height asynchronously and delivers the result via [onResult].
     *
     * FIX #2: Same deadlock fix as [getWidthInPixels].
     *
     * @param onResult Called on the UI thread with the height in pixels, or -1 if unavailable.
     */
    fun getHeightInPixels(onResult: (Float) -> Unit) {
        val activity = activityRef.get() ?: run {
            onResult(-1f)
            return
        }
        activity.runOnUiThread {
            val height = resolveAdHeight(activity)
            Log.d(TAG, "Ad height resolved: $height px")
            onResult(height)
        }
    }

    // FIX #3: Dynamic layout → read from rootPixelRect; XML layout → R.id fallback
    private fun resolveAdWidth(activity: Activity): Float {
        // DynamicShowBehavior path: use pre-computed pixel rect (no R.id dependency)
        val dynamicBehavior = unwrapDynamicBehavior()
        if (dynamicBehavior != null) {
            return dynamicBehavior.renderer?.getRootPixelRect()?.width()?.toFloat() ?: -1f
        }

        // XML layout path: find view by resource ID
        val adContent = findAdContentView(activity)
        return adContent?.width?.toFloat() ?: -1f
    }

    private fun resolveAdHeight(activity: Activity): Float {
        val dynamicBehavior = unwrapDynamicBehavior()
        if (dynamicBehavior != null) {
            return dynamicBehavior.renderer?.getRootPixelRect()?.height()?.toFloat() ?: -1f
        }

        val adContent = findAdContentView(activity)
        return adContent?.height?.toFloat() ?: -1f
    }

    /**
     * Returns the [DynamicShowBehavior] if it is the active behavior (possibly wrapped by decorators).
     * We keep a direct reference [innerDynamicBehavior] set at assembly time to avoid
     * traversing private decorator fields.
     */
    private var innerDynamicBehavior: DynamicShowBehavior? = null

    private fun unwrapDynamicBehavior(): DynamicShowBehavior? = innerDynamicBehavior

    private fun findAdContentView(activity: Activity): View? {
        val adContainer = (currentShowBehavior as? BaseShowBehavior)?.getRootView()
        val id = activity.resources.getIdentifier("ad_content", "id", activity.packageName)
        return if (id != 0) adContainer?.findViewById(id) else null
    }

    // ── Configuration DSL ─────────────────────────────────────────────────────

    //region CountdownDecorator
    private data class CountdownConfig(val initial: Float, val duration: Float, val closeDelay: Float)

    private var countdownConfig: CountdownConfig? = null

    fun withCountdown(initial: Float, duration: Float, closeDelay: Float): AdmobNativeController {
        if (initial < 0 || duration <= 0 || closeDelay < 0) {
            Log.w(TAG, "Invalid countdown timings. Configuration ignored.")
            this.countdownConfig = null
        } else {
            this.countdownConfig = CountdownConfig(initial, duration, closeDelay)
            Log.d(TAG, "Applying Countdown config: $initial, $duration, $closeDelay")
        }
        return this
    }
    //endregion

    //endregion

    //region Dynamic Native UI Support
    // FIX #4: @Volatile ensures visibility across Unity JNI bridge thread → Android UI thread
    @Volatile var layoutJsonConfig: String? = null
    @Volatile var zLayerConfig: String? = null

    fun withLayoutJson(jsonPayload: String): AdmobNativeController {
        this.layoutJsonConfig = jsonPayload
        return this
    }

    fun withZLayer(zLayer: String): AdmobNativeController {
        this.zLayerConfig = zLayer
        return this
    }
    //endregion

    private fun resetAllConfigs() {
        countdownConfig  = null
        layoutJsonConfig = null
        zLayerConfig     = null
    }
}