// File: mylibrary/src/main/java/com/mycompany/admobnative/AdmobNativeController.kt
package com.thelegends.admob_native_unity

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.ads.admob_native_unity.decorator.*
import com.thelegends.ads.admob_native_unity.showbehavior.*

class AdmobNativeController(
    private val activity: Activity,
    private val callbacks: NativeAdCallbacks
) {

    private var loadedNativeAd: NativeAd? = null
    private var currentShowBehavior: IShowBehavior? = null

    private val TAG = "AdmobNativeController"

    fun loadAd(adUnitId: String, adRequest: AdRequest) {
//        destroyAd()

        Log.d(TAG, "Loading native ad for Ad Unit ID: $adUnitId")

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
                // THÀNH CÔNG!
                Log.d(TAG, "Ad loaded successfully.")
                this.loadedNativeAd = ad
                setupAdCallbacks(ad)
                callbacks.onAdLoaded()

                internalCallbackListeners.toList().forEach { it.onAdLoaded() }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Ad load failed.")
                    callbacks.onAdFailedToLoad(adError)
                    internalCallbackListeners.toList().forEach { it.onAdFailedToLoad(adError) }
                }

                override fun onAdClicked() {
                    callbacks.onAdClicked()
                    internalCallbackListeners.toList().forEach { it.onAdClicked() }
                    Log.d(TAG, "Ad clicked.")
                }

                override fun onAdImpression() {
                    callbacks.onAdDidRecordImpression()
                    internalCallbackListeners.toList().forEach { it.onAdDidRecordImpression() }
                    Log.d(TAG, "Ad impression recorded.")
                }

                override fun onAdOpened() {
                    callbacks.onAdShowedFullScreenContent()
                    internalCallbackListeners.toList().forEach { it.onAdShowedFullScreenContent() }
                    Log.d(TAG, "Ad opened Full Screen Content.")
                }

                override fun onAdClosed() {
                    callbacks.onAdDismissedFullScreenContent()
                    internalCallbackListeners.toList().forEach { it.onAdDismissedFullScreenContent() }
                    Log.d(TAG, "Ad closed Full Screen Content.")
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
            .loadAd(adRequest)

    }

    fun showAd(layoutName: String) {
        Log.d(TAG, "Show ads")
        val adToShow = loadedNativeAd ?: run {
            Log.e(TAG, "Ad not available. Call loadAd() first.")
//            resetAllConfigs()
            return
        }

        currentShowBehavior?.destroy()

        // Lắp ráp
        var behavior: BaseShowBehavior = BaseShowBehavior()
        if (countdownConfig != null) {
            behavior = CountdownDecorator(
                behavior,
                countdownConfig!!.initial,
                countdownConfig!!.duration,
                countdownConfig!!.closeDelay
            )
        }

        // Thực thi
        behavior.show(activity, adToShow, layoutName, callbacks)
        currentShowBehavior = behavior
//        resetAllConfigs()

        callbacks.onAdShow()
        internalCallbackListeners.toList().forEach { it.onAdShow() }
    }

    fun destroyAd() {
        resetAllConfigs()

        currentShowBehavior?.destroy()
        currentShowBehavior = null

        loadedNativeAd?.destroy()
        loadedNativeAd = null

        internalCallbackListeners.clear()

        callbacks.onAdClosed()
        internalCallbackListeners.toList().forEach { it.onAdClosed() }

        Log.d(TAG, "Native ad has been destroyed.")
    }

    fun isAdAvailable(): Boolean = loadedNativeAd != null

    fun getResponseInfo(): ResponseInfo? {
        return loadedNativeAd?.responseInfo
    }

    private fun setupAdCallbacks(ad: NativeAd) {
        val videoLifecycleCallbacks =
            object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoStart() {
                    Log.d(TAG, "Video started.")
                    internalCallbackListeners.toList().forEach { it.onVideoStart() }
                    callbacks.onVideoStart()
                }

                override fun onVideoPlay() {
                    Log.d(TAG, "Video played.")
                    internalCallbackListeners.toList().forEach { it.onVideoPlay() }
                    callbacks.onVideoPlay()
                }

                override fun onVideoPause() {
                    Log.d(TAG, "Video paused.")
                    internalCallbackListeners.toList().forEach { it.onVideoPause() }
                    callbacks.onVideoPause()
                }

                override fun onVideoEnd() {
                    Log.d(TAG, "Video ended.")
                    internalCallbackListeners.toList().forEach { it.onVideoEnd() }
                    callbacks.onVideoEnd()
                }

                override fun onVideoMute(isMuted: Boolean) {
                    Log.d(TAG, "Video isMuted: $isMuted.")
                    internalCallbackListeners.toList().forEach { it.onVideoMute(isMuted) }
                    callbacks.onVideoMute(isMuted)
                }
            }
        ad.mediaContent?.videoController?.videoLifecycleCallbacks = videoLifecycleCallbacks

        ad.setOnPaidEventListener { adValue ->
            Log.d(TAG, "Paid event received: $adValue")
            callbacks.onPaidEvent(
                adValue.precisionType,
                adValue.valueMicros,
                adValue.currencyCode
            )
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

    private fun resetAllConfigs() {
        countdownConfig = null
    }

    private val internalCallbackListeners = mutableListOf<NativeAdCallbacks>()

    fun addAdCallbackListener(listener: NativeAdCallbacks) {
        if (!internalCallbackListeners.contains(listener)) internalCallbackListeners.add(listener)
    }

    fun removeAdCallbackListener(listener: NativeAdCallbacks) {
        internalCallbackListeners.remove(listener)
    }


}