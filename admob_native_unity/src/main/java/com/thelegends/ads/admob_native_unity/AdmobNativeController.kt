// File: mylibrary/src/main/java/com/mycompany/admobnative/AdmobNativeController.kt
package com.thelegends.admob_native_unity

import android.app.Activity
import android.util.Log
import android.view.View
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.ads.admob_native_unity.decorator.*
import com.thelegends.ads.admob_native_unity.showbehavior.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
class AdmobNativeController(
    private val activity: Activity,
    private val callbacks: NativeAdCallbacks
) {

    private val unityCallbackExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private fun notifyUnity(action: () -> Unit) {
        unityCallbackExecutor.execute(action)
    }

    private var loadedNativeAd: NativeAd? = null
    private var currentShowBehavior: IShowBehavior? = null

    private val TAG = "AdmobNativeController"

    fun loadAd(adUnitId: String, adRequest: AdRequest) {
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
                    }
                    notifyUnity { callbacks.onAdLoaded() }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        activity.runOnUiThread {
                            Log.d(TAG, "Ad load failed: ${adError.message}")
                        }
                        notifyUnity { callbacks.onAdFailedToLoad(adError) }
                    }

                    override fun onAdClicked() {
                        activity.runOnUiThread {
                            Log.d(TAG, "Ad clicked.")
                        }
                        notifyUnity { callbacks.onAdClicked() }
                    }

                    override fun onAdImpression() {
                        activity.runOnUiThread {
                            Log.d(TAG, "Ad impression recorded.")
                        }
                        notifyUnity { callbacks.onAdDidRecordImpression() }
                    }

                    override fun onAdOpened() {
                        activity.runOnUiThread {
                            Log.d(TAG, "Ad opened Full Screen Content.")
                        }
                        notifyUnity { callbacks.onAdShowedFullScreenContent() }
                    }

                    override fun onAdClosed() {
                        activity.runOnUiThread {
                            Log.d(TAG, "Ad closed Full Screen Content.")
                        }
                        notifyUnity { callbacks.onAdDismissedFullScreenContent() }
                    }
                })
                .withNativeAdOptions(adOptions)
                .build()
                .loadAd(adRequest)
        }
    }

    fun showAd(layoutName: String) {
        Log.d(TAG, "Show ads")
        val adToShow = loadedNativeAd ?: run {
            Log.e(TAG, "Ad not available. Call loadAd() first.")
            return
        }


        currentShowBehavior?.destroy()

        // Lắp ráp Lưỡng Cực (Hybrid Assembly)
        var behavior: BaseShowBehavior = if (layoutJsonConfig != null && zLayerConfig != null) {
            DynamicShowBehavior(layoutJsonConfig!!, zLayerConfig!!)
        } else {
            BaseShowBehavior()
        }

        if (positionConfig != null) {
            behavior = PositionDecorator(
                behavior,
                this,
                positionConfig!!.x,
                positionConfig!!.y
            )

        }

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

        notifyUnity { callbacks.onAdShow() }
    }

    fun destroyAd() {
        activity.runOnUiThread {
            resetAllConfigs()

            currentShowBehavior?.destroy()
            currentShowBehavior = null

            loadedNativeAd?.destroy()
            loadedNativeAd = null

            Log.d(TAG, "Native ad has been destroyed.")
        }
        notifyUnity { callbacks.onAdClosed() }
    }

    fun updateAdViewSize(widthPx: Int, heightPx: Int) {
        activity.runOnUiThread {

            val adContainer = (currentShowBehavior as? BaseShowBehavior)?.getRootView()
            val nativeAdView = adContainer?.findViewById<View>(
                activity.resources.getIdentifier("native_ad_view", "id", activity.packageName)
            )

            nativeAdView?.post {
                val params = nativeAdView.layoutParams
                params?.let {
                    it.height = heightPx
                    it.width = widthPx
                    nativeAdView.layoutParams = it
                    Log.d(TAG, "Updated ad size to $widthPx x $heightPx pixels")
                }

            }
        }
    }

    fun isAdAvailable(): Boolean = loadedNativeAd != null

    fun getResponseInfo(): ResponseInfo? {
        return loadedNativeAd?.responseInfo
    }

    private fun setupAdCallbacks(ad: NativeAd) {
        val videoLifecycleCallbacks =
            object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoStart() {
                    activity.runOnUiThread {
                        Log.d(TAG, "Video started.")
                    }
                    notifyUnity { callbacks.onVideoStart() }
                }

                override fun onVideoPlay() {
                    activity.runOnUiThread {
                        Log.d(TAG, "Video played.")
                    }
                    notifyUnity { callbacks.onVideoPlay() }
                }

                override fun onVideoPause() {
                    activity.runOnUiThread {
                        Log.d(TAG, "Video paused.")
                    }
                    notifyUnity { callbacks.onVideoPause() }
                }

                override fun onVideoEnd() {
                    activity.runOnUiThread {
                        Log.d(TAG, "Video ended.")
                    }
                    notifyUnity { callbacks.onVideoEnd() }
                }

                override fun onVideoMute(isMuted: Boolean) {
                    activity.runOnUiThread {
                        Log.d(TAG, "Video isMuted: $isMuted.")
                    }
                    notifyUnity { callbacks.onVideoMute(isMuted) }
                }
            }
        ad.mediaContent?.videoController?.videoLifecycleCallbacks = videoLifecycleCallbacks

        ad.setOnPaidEventListener { adValue ->
            activity.runOnUiThread {
                Log.d(TAG, "Paid event received: $adValue")
            }
            notifyUnity {
                callbacks.onPaidEvent(
                    adValue.precisionType,
                    adValue.valueMicros,
                    adValue.currencyCode
                )
            }
        }
    }

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

    //region PositionDecorator
    private data class PositionConfig(val x: Int, val y: Int)

    private var positionConfig: PositionConfig? = null

    fun withPosition(positionX: Int, positionY: Int): AdmobNativeController {
        this.positionConfig = PositionConfig(positionX, positionY)
        return this
    }

    //endregion

    //region Dynamic Native UI Support
    var layoutJsonConfig: String? = null
    var zLayerConfig: String? = null

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
        countdownConfig = null
        positionConfig = null
        layoutJsonConfig = null
        zLayerConfig = null
    }

    fun getWidthInPixels(): Float {
        val task = FutureTask(Callable<Int> {
            val adContainer = (currentShowBehavior as? BaseShowBehavior)?.getRootView()
            val viewToMeasure = adContainer?.findViewById<View>(
                activity.resources.getIdentifier("background", "id", activity.packageName)
            )

            if (viewToMeasure != null) {
                val displayMetrics = activity.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                val lp = viewToMeasure.layoutParams

                val widthSpec = android.view.ViewGroup.getChildMeasureSpec(
                    View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
                    0, lp.width
                )
                val heightSpec = android.view.ViewGroup.getChildMeasureSpec(
                    View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST),
                    0, lp.height
                )

                viewToMeasure.measure(widthSpec, heightSpec)
                viewToMeasure.measuredWidth
            } else {
                0
            }
        })

        activity.runOnUiThread(task)
        return try {
            task.get().toFloat()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get measured width: ${e.localizedMessage}")
            0f
        }
    }

    fun getHeightInPixels(): Float {
        val task = FutureTask(Callable<Int> {
            val adContainer = (currentShowBehavior as? BaseShowBehavior)?.getRootView()
            val viewToMeasure = adContainer?.findViewById<View>(
                activity.resources.getIdentifier("background", "id", activity.packageName)
            )

            if (viewToMeasure != null) {
                val displayMetrics = activity.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                val lp = viewToMeasure.layoutParams

                val widthSpec = android.view.ViewGroup.getChildMeasureSpec(
                    View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
                    0, lp.width
                )
                val heightSpec = android.view.ViewGroup.getChildMeasureSpec(
                    View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST),
                    0, lp.height
                )

                viewToMeasure.measure(widthSpec, heightSpec)
                viewToMeasure.measuredHeight
            } else {
                0
            }
        })

        activity.runOnUiThread(task)
        return try {
            task.get().toFloat()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get measured height: ${e.localizedMessage}")
            0f
        }
    }





}