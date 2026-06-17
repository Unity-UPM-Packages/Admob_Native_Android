// File: mylibrary/src/main/java/com/mycompany/admobnative/AdmobNativeController.kt
package com.thelegends.admob_native_unity

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
    private val loadCallback: IAdLoadCallback? = null,
    private val interactionCallback: IAdInteractionCallback? = null,
    private val videoCallback: IAdVideoCallback? = null,
    private val revenueCallback: IAdRevenueCallback? = null
) {
    // Secondary constructor for Unity JNI compatibility
    constructor(activity: Activity, callbacks: NativeAdCallbacks) : this(
        activity,
        loadCallback = callbacks,
        interactionCallback = callbacks,
        videoCallback = callbacks,
        revenueCallback = callbacks
    )
    // FIX #1: WeakReference prevents Activity Leak across configuration changes / session reuse
    private val activityRef: WeakReference<Activity> = WeakReference(activity)

    private var loadedNativeAd: NativeAd? = null
    private var currentShowBehavior: IShowBehavior? = null
    private var innerDynamicBehavior: DynamicShowBehavior? = null

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
                        loadCallback?.onAdLoaded()
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        activity.runOnUiThread {
                            Log.d(TAG, "Ad load failed: ${adError.message}")
                            loadCallback?.onAdFailedToLoad(adError)
                        }
                    }

                    override fun onAdClicked() {
                        activity.runOnUiThread {
                            interactionCallback?.onAdClicked()
                            Log.d(TAG, "Ad clicked.")
                        }
                    }

                    override fun onAdImpression() {
                        activity.runOnUiThread {
                            interactionCallback?.onAdDidRecordImpression()
                            Log.d(TAG, "Ad impression recorded.")
                        }
                    }

                    override fun onAdOpened() {
                        activity.runOnUiThread {
                            interactionCallback?.onAdShowedFullScreenContent()
                            Log.d(TAG, "Ad opened Full Screen Content.")
                        }
                    }

                    override fun onAdClosed() {
                        activity.runOnUiThread {
                            interactionCallback?.onAdDismissedFullScreenContent()
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

        var behavior: IShowBehavior = dynamicBehavior ?: XibShowBehavior()

        if (countdownConfig != null) {
            behavior = CountdownDecorator(
                behavior,
                countdownConfig!!.initial,
                countdownConfig!!.duration,
                countdownConfig!!.closeDelay,
                onClose = { destroyAd() }
            )
        }

        behavior.show(activity, adToShow, layoutName)
        currentShowBehavior = behavior

        interactionCallback?.onAdShow()
    }

    fun destroyAd() {
        val activity = activityRef.get() ?: run {
            Log.e(TAG, "Activity has been destroyed. Cannot destroy ad.")
            return
        }
        activity.runOnUiThread {
            resetAllConfigs()

            currentShowBehavior?.destroy()
            currentShowBehavior = null
            innerDynamicBehavior = null

            loadedNativeAd?.destroy()
            loadedNativeAd = null

            interactionCallback?.onAdClosed()

            Log.d(TAG, "Native ad has been destroyed.")
        }
    }

    fun updateAdViewSize(widthPx: Int, heightPx: Int) {
        val activity = activityRef.get() ?: run {
            Log.e(TAG, "Activity has been destroyed. Cannot update ad view size.")
            return
        }
        activity.runOnUiThread {
            val adContainer = currentShowBehavior?.getRootView() ?: return@runOnUiThread
            
            // For dynamic layout, the container itself is the NativeAdView.
            // For XML layout, look for the native_ad_view inside.
            val targetView = if (adContainer is com.google.android.gms.ads.nativead.NativeAdView) {
                adContainer
            } else {
                val id = activity.resources.getIdentifier("native_ad_view", "id", activity.packageName)
                if (id != 0) adContainer.findViewById<View>(id) else null
            }

            targetView?.post {
                val params = targetView.layoutParams
                if (params != null) {
                    params.width = widthPx
                    params.height = heightPx

                    if (params is FrameLayout.LayoutParams) {
                        val dynamicBehavior = unwrapDynamicBehavior()
                        if (dynamicBehavior != null) {
                            val originalRect = dynamicBehavior.renderer?.getRootPixelRect()
                            if (originalRect != null && originalRect.height() > 0) {
                                val originalHeight = originalRect.height()
                                val originalTop = originalRect.top
                                // Shift the top margin upwards by the height difference to keep the bottom anchored
                                params.topMargin = originalTop - (heightPx - originalHeight)
                                Log.d(TAG, "Adjusted dynamic ad topMargin to ${params.topMargin} (originalTop: $originalTop, originalHeight: $originalHeight, newHeight: $heightPx)")
                            }
                        }
                    }

                    targetView.layoutParams = params
                    Log.d(TAG, "Updated ad view size to $widthPx x $heightPx pixels")
                }
            }
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
                    videoCallback?.onVideoStart()
                }
            }

            override fun onVideoPlay() {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video played.")
                    videoCallback?.onVideoPlay()
                }
            }

            override fun onVideoPause() {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video paused.")
                    videoCallback?.onVideoPause()
                }
            }

            override fun onVideoEnd() {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video ended.")
                    videoCallback?.onVideoEnd()
                }
            }

            override fun onVideoMute(isMuted: Boolean) {
                activityRef.get()?.runOnUiThread {
                    Log.d(TAG, "Video isMuted: $isMuted.")
                    videoCallback?.onVideoMute(isMuted)
                }
            }
        }
        ad.mediaContent?.videoController?.videoLifecycleCallbacks = videoLifecycleCallbacks

        ad.setOnPaidEventListener { adValue ->
            activityRef.get()?.runOnUiThread {
                Log.d(TAG, "Paid event received: $adValue")
                revenueCallback?.onPaidEvent(
                    adValue.precisionType,
                    adValue.valueMicros,
                    adValue.currencyCode
                )
            }
        }
    }

    // ── Dimension Queries ─────────────────────────────────────────────────────

    fun getWidthInPixels(): Float {
        val activity = activityRef.get() ?: return -1f
        
        // 1. Dynamic layout path: return pre-calculated width (no JNI blocking / UI thread issues)
        val dynamicBehavior = unwrapDynamicBehavior()
        if (dynamicBehavior != null) {
            return dynamicBehavior.renderer?.getRootPixelRect()?.width()?.toFloat() ?: -1f
        }

        // 2. XML layout path: measure on UI thread (safely check Looper to prevent deadlocks)
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            return resolveXmlAdWidth(activity)
        }
        val task = java.util.concurrent.FutureTask(java.util.concurrent.Callable<Float> {
            resolveXmlAdWidth(activity)
        })
        activity.runOnUiThread(task)
        return try {
            task.get()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get XML ad width: ${e.localizedMessage}")
            -1f
        }
    }

    fun getHeightInPixels(): Float {
        val activity = activityRef.get() ?: return -1f
        
        // 1. Dynamic layout path: return pre-calculated height
        val dynamicBehavior = unwrapDynamicBehavior()
        if (dynamicBehavior != null) {
            return dynamicBehavior.renderer?.getRootPixelRect()?.height()?.toFloat() ?: -1f
        }

        // 2. XML layout path: measure on UI thread (safely check Looper to prevent deadlocks)
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            return resolveXmlAdHeight(activity)
        }
        val task = java.util.concurrent.FutureTask(java.util.concurrent.Callable<Float> {
            resolveXmlAdHeight(activity)
        })
        activity.runOnUiThread(task)
        return try {
            task.get()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get XML ad height: ${e.localizedMessage}")
            -1f
        }
    }

    private fun resolveXmlAdWidth(activity: Activity): Float {
        val adContainer = currentShowBehavior?.getRootView() ?: return -1f
        val viewToMeasure = findXmlViewToMeasure(activity, adContainer)
        
        val displayMetrics = activity.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val lp = viewToMeasure.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val widthSpec = android.view.ViewGroup.getChildMeasureSpec(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            0, lp.width
        )
        val heightSpec = android.view.ViewGroup.getChildMeasureSpec(
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST),
            0, lp.height
        )

        viewToMeasure.measure(widthSpec, heightSpec)
        return viewToMeasure.measuredWidth.toFloat()
    }

    private fun resolveXmlAdHeight(activity: Activity): Float {
        val adContainer = currentShowBehavior?.getRootView() ?: return -1f
        val viewToMeasure = findXmlViewToMeasure(activity, adContainer)
        
        val displayMetrics = activity.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val lp = viewToMeasure.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val widthSpec = android.view.ViewGroup.getChildMeasureSpec(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            0, lp.width
        )
        val heightSpec = android.view.ViewGroup.getChildMeasureSpec(
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST),
            0, lp.height
        )

        viewToMeasure.measure(widthSpec, heightSpec)
        return viewToMeasure.measuredHeight.toFloat()
    }

    private fun findXmlViewToMeasure(activity: Activity, adContainer: View): View {
        val ids = arrayOf("background", "ad_content", "native_ad_view")
        for (idStr in ids) {
            val id = activity.resources.getIdentifier(idStr, "id", activity.packageName)
            if (id != 0) {
                val view = adContainer.findViewById<View>(id)
                if (view != null) return view
            }
        }
        if (adContainer is ViewGroup && adContainer.childCount > 0) {
            val child = adContainer.getChildAt(0)
            if (child != null) return child
        }
        return adContainer
    }

    private fun unwrapDynamicBehavior(): DynamicShowBehavior? = innerDynamicBehavior

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