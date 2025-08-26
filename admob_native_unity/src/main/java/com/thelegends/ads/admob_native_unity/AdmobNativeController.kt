// File: mylibrary/src/main/java/com/mycompany/admobnative/AdmobNativeController.kt
package com.thelegends.admob_native_unity

import android.app.Activity
import android.content.Context
import com.orbitalsonic.sonictimer.SonicCountDownTimer
import android.util.Log
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.ProgressBar
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.thelegends.ads.admob_native_unity.R

class AdmobNativeController(
    private val activity: Activity,
    private val callbacks: NativeAdCallbacks
) {

    private var loadedNativeAd: NativeAd? = null
    private var rootView: View? = null
    // Three sequential timers
    private var initialDelayTimer: SonicCountDownTimer? = null
    private var countdownTimer: SonicCountDownTimer? = null
    private var closeButtonDelayTimer: SonicCountDownTimer? = null

    private var countdownTimerDurationMillis: Long = 5000
    private var initialDelayBeforeCountdownMillis: Long = 5000
    private var closeButtonClickableDelayMillis: Long = 2000

    private val TAG = "AdmobNativeController"

    fun loadAd(adUnitId: String, adRequest: AdRequest) {
        destroyAd()

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
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Ad load failed.")
                    callbacks.onAdFailedToLoad(adError)
                }

                override fun onAdClicked() {
                    callbacks.onAdClicked()
                    Log.d(TAG, "Ad clicked.")
                }

                override fun onAdImpression() {
                    callbacks.onAdDidRecordImpression()
                    Log.d(TAG, "Ad impression recorded.")
                }

                override fun onAdOpened() {
                    callbacks.onAdShowedFullScreenContent()
                    Log.d(TAG, "Ad opened Full Screen Content.")
                }

                override fun onAdClosed() {
                    callbacks.onAdDismissedFullScreenContent()
                    Log.d(TAG, "Ad closed Full Screen Content.")
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
            .loadAd(adRequest)
    }

    fun showAd(layoutName: String) {
        val adToShow = loadedNativeAd ?: return
        this.activity.runOnUiThread {
            if (loadedNativeAd == null) return@runOnUiThread

            val layoutId = activity.resources.getIdentifier(
                layoutName,
                "layout",
                activity.packageName
            )

            if (layoutId == 0) {
                Log.e(TAG, "Layout resource not found for name: $layoutName. Ad will not be shown.")
                return@runOnUiThread
            }


            val inflatedView = activity.layoutInflater.inflate(layoutId, null);
            rootView = inflatedView

            val nativeAdView = inflatedView?.findViewById<NativeAdView>(R.id.native_ad_view) ?: return@runOnUiThread


            var layout: FrameLayout? = activity.findViewById(R.id.native_ad_view)
            if (layout == null) {
                layout = FrameLayout(activity)
                activity.addContentView(layout, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            } else {
                layout.removeAllViews()
            }


            populateNativeAdView(adToShow, nativeAdView)
            layout.addView(inflatedView)
            
            // Hide timing UI elements for simple showAd
            val closeButton = inflatedView.findViewById<ImageView>(R.id.ad_close_button)
            val progressBar = inflatedView.findViewById<ProgressBar>(R.id.ad_progress_bar)
            val countdownText = inflatedView.findViewById<TextView>(R.id.ad_countdown_text)
            
            closeButton?.visibility = View.GONE
            progressBar?.visibility = View.GONE
            countdownText?.visibility = View.GONE
            
            nativeAdView.elevation = 10f
            nativeAdView.bringToFront()

        }
    }

    fun showAd(
        layoutName: String, 
        initialDelaySeconds: Float, 
        countdownDurationSeconds: Float, 
        closeButtonDelaySeconds: Float
    ) {

        activity.runOnUiThread {
            // Set the custom timings using existing setter methods
            setInitialDelayBeforeCountdown(initialDelaySeconds)
            setCountdownTimerDuration(countdownDurationSeconds)
            setCloseButtonClickableDelay(closeButtonDelaySeconds)

            // Call the main showAd method
            showAd(layoutName)

            // Cast rootView back to View for timing logic
            rootView?.let { startCloseLogic(it) }
        }
    }

    fun destroyAd() {
        activity.runOnUiThread {
            // Cancel all timers
            initialDelayTimer?.cancelCountDownTimer()
            initialDelayTimer = null
            
            countdownTimer?.cancelCountDownTimer()
            countdownTimer = null
            
            closeButtonDelayTimer?.cancelCountDownTimer()
            closeButtonDelayTimer = null

            (rootView?.parent as? ViewGroup)?.removeView(rootView)
            rootView = null
        }
        loadedNativeAd?.destroy()
        loadedNativeAd = null
        Log.d(TAG, "Native ad has been destroyed.")
    }

    fun isAdAvailable(): Boolean = loadedNativeAd != null

    fun getResponseInfo(): ResponseInfo? {
        return loadedNativeAd?.responseInfo
    }

    fun setCountdownTimerDuration(durationSeconds: Float) {
        if (durationSeconds > 0) {
            this.countdownTimerDurationMillis = (durationSeconds * 1000).toLong()
            Log.d(TAG, "Countdown Timer Duration updated to ${durationSeconds}s")
        } else {
            Log.w(TAG, "Invalid Countdown Timer Duration received: $durationSeconds. Using default.")
        }
    }

    fun setInitialDelayBeforeCountdown(durationSeconds: Float) {
        if (durationSeconds >= 0) {
            this.initialDelayBeforeCountdownMillis = (durationSeconds * 1000).toLong()
            Log.d(TAG, "Initial Delay Before Countdown updated to ${durationSeconds}s")
        } else {
            Log.w(TAG, "Invalid Initial Delay received: $durationSeconds. Using default.")
        }
    }

    fun setCloseButtonClickableDelay(durationSeconds: Float) {
        if (durationSeconds >= 0) {
            this.closeButtonClickableDelayMillis = (durationSeconds * 1000).toLong()
            Log.d(TAG, "Close Button Clickable Delay updated to ${durationSeconds}s")
        } else {
            Log.w(TAG, "Invalid Close Button Clickable Delay received: $durationSeconds. Using default.")
        }
    }

    private fun findViewId(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "id", context.packageName)
    }


    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        val context = adView.context

        adView.mediaView = adView.findViewById(findViewId(context, "media_view"))
        adView.headlineView = adView.findViewById(findViewId(context, "primary"))
        adView.bodyView = adView.findViewById(findViewId(context, "body"))
        adView.callToActionView = adView.findViewById(findViewId(context, "cta"))
        adView.iconView = adView.findViewById(findViewId(context, "icon"))
        adView.starRatingView = adView.findViewById(findViewId(context, "rating_bar"))
        adView.advertiserView = adView.findViewById(findViewId(context, "secondary"))
        adView.storeView = adView.findViewById(findViewId(context,"ad_store"))
        adView.priceView = adView.findViewById(findViewId(context,"ad_price"))

        // 2. Headline
        (adView.headlineView as? TextView)?.text = nativeAd.headline

        // 3. Body
        val bodyView = adView.bodyView as? TextView
        if (nativeAd.body != null) {
            bodyView?.text = nativeAd.body
            bodyView?.visibility = android.view.View.VISIBLE
        } else {
            bodyView?.visibility = android.view.View.INVISIBLE
        }

        // 4. Call to Action
        val ctaButton = adView.callToActionView as? android.widget.Button
        if (nativeAd.callToAction != null) {
            ctaButton?.text = nativeAd.callToAction
            ctaButton?.visibility = android.view.View.VISIBLE
        } else {
            ctaButton?.visibility = android.view.View.INVISIBLE
        }

        // 5. Icon
        val iconView = adView.iconView as? android.widget.ImageView
        if (nativeAd.icon != null) {
            iconView?.setImageDrawable(nativeAd.icon?.drawable)
            iconView?.visibility = android.view.View.VISIBLE
        } else {
            iconView?.visibility = android.view.View.GONE
        }

        // 6. Star Rating
        val ratingBar = adView.starRatingView as? android.widget.RatingBar
        if (nativeAd.starRating != null) {
            ratingBar?.rating = nativeAd.starRating!!.toFloat()
            ratingBar?.visibility = android.view.View.VISIBLE
        } else {
            ratingBar?.visibility = android.view.View.INVISIBLE
        }

        // 7. Advertiser
        val advertiserView = adView.advertiserView as? TextView
        if (nativeAd.advertiser != null) {
            advertiserView?.text = nativeAd.advertiser
            advertiserView?.visibility = android.view.View.VISIBLE
        } else {
            advertiserView?.visibility = android.view.View.INVISIBLE
        }

        // 8. Store
        val storeView = adView.storeView as? TextView
        if (nativeAd.store != null) {
            storeView?.text = nativeAd.store
            storeView?.visibility = android.view.View.VISIBLE
        } else {
            storeView?.visibility = android.view.View.INVISIBLE
        }

        // 9. Price
        val priceView = adView.priceView as? TextView
        if (nativeAd.price != null) {
            priceView?.text = nativeAd.price
            priceView?.visibility = android.view.View.VISIBLE
        } else {
            priceView?.visibility = android.view.View.INVISIBLE
        }

        adView.setNativeAd(nativeAd)

        callbacks.onAdShow()
        Log.d(TAG, "Ad Opened")
    }

    private fun setupAdCallbacks(ad: NativeAd) {
        val videoLifecycleCallbacks =
            object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoStart() {
                    Log.d(TAG, "Video started.")
                    callbacks.onVideoStart()
                }

                override fun onVideoPlay() {
                    Log.d(TAG, "Video played.")
                    callbacks.onVideoPlay()
                }

                override fun onVideoPause() {
                    Log.d(TAG, "Video paused.")
                    callbacks.onVideoPause()
                }

                override fun onVideoEnd() {
                    Log.d(TAG, "Video ended.")
                    callbacks.onVideoEnd()
                }

                override fun onVideoMute(isMuted: Boolean) {
                    Log.d(TAG, "Video isMuted: $isMuted.")
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

    private fun startCloseLogic(rootView: View) {
        val closeButton = rootView.findViewById<ImageView>(R.id.ad_close_button)
        val progressBar = rootView.findViewById<ProgressBar>(R.id.ad_progress_bar)
        val countdownText = rootView.findViewById<TextView>(R.id.ad_countdown_text)

        // Cancel any existing timers
        initialDelayTimer?.cancelCountDownTimer()
        countdownTimer?.cancelCountDownTimer()
        closeButtonDelayTimer?.cancelCountDownTimer()

        // PHASE 1: Initial state - Hide everything
        closeButton?.visibility = View.GONE
        progressBar?.visibility = View.GONE
        countdownText?.visibility = View.GONE
        closeButton?.isClickable = false

        Log.d(TAG, "Starting Phase 1: Initial delay (${initialDelayBeforeCountdownMillis}ms)")

        // TIMER 1: Initial delay before showing progress/countdown
        initialDelayTimer = object : SonicCountDownTimer(initialDelayBeforeCountdownMillis, 100) {
            override fun onTimerTick(timeRemaining: Long) {
                // Silent countdown, no UI updates
            }

            override fun onTimerFinish() {
                Log.d(TAG, "Phase 1 completed. Starting Phase 2: Main countdown")
                startMainCountdown(closeButton, progressBar, countdownText)
            }
        }
        initialDelayTimer?.startCountDownTimer()

        // Setup close button click listener (will only work when enabled)
        closeButton?.setOnClickListener {
            if (closeButton.isClickable) {
                destroyAd()
                callbacks.onAdClosed()
                Log.d(TAG, "Ad Closed")
            }
        }
    }

    private fun startMainCountdown(closeButton: ImageView?, progressBar: ProgressBar?, countdownText: TextView?) {
        // PHASE 2: Show progress bar and countdown text
        progressBar?.progress = 100  // Start from 100% and decrease

        progressBar?.visibility = View.VISIBLE
        countdownText?.visibility = View.VISIBLE
        closeButton?.visibility = View.GONE

        Log.d(TAG, "Starting main countdown (${countdownTimerDurationMillis}ms)")

        // TIMER 2: Main countdown timer
        countdownTimer = object : SonicCountDownTimer(countdownTimerDurationMillis, 1000) {
            override fun onTimerTick(timeRemaining: Long) {
                val secondsRemaining = (timeRemaining / 1000).toInt()
                
                // Stop showing countdown when it reaches 0, move to next phase immediately
                if (secondsRemaining <= 0) {
                    onTimerFinish()
                    return
                }
                
                countdownText?.text = secondsRemaining.toString()
                
                // Progress decreases from 100% to 0% to show remaining time
                val progressPercent = (timeRemaining * 100 / countdownTimerDurationMillis).toInt().coerceAtLeast(0)
                progressBar?.progress = progressPercent
            }

            override fun onTimerFinish() {
                Log.d(TAG, "Phase 2 completed. Starting Phase 3: Close button delay")
                startCloseButtonDelay(closeButton, progressBar, countdownText)
            }
        }
        countdownTimer?.startCountDownTimer()
    }

    private fun startCloseButtonDelay(closeButton: ImageView?, progressBar: ProgressBar?, countdownText: TextView?) {
        // PHASE 3: Show close button but keep it non-clickable for a delay
        progressBar?.visibility = View.GONE
        countdownText?.visibility = View.GONE
        closeButton?.visibility = View.VISIBLE
        closeButton?.isClickable = false

        Log.d(TAG, "Starting close button delay (${closeButtonClickableDelayMillis}ms)")

        // TIMER 3: Close button clickable delay
        closeButtonDelayTimer = object : SonicCountDownTimer(closeButtonClickableDelayMillis, 100) {
            override fun onTimerTick(timeRemaining: Long) {
                // Silent countdown, no UI updates
            }

            override fun onTimerFinish() {
                Log.d(TAG, "Phase 3 completed. Close button is now clickable")
                closeButton?.isClickable = true
            }
        }
        closeButtonDelayTimer?.startCountDownTimer()
    }
}