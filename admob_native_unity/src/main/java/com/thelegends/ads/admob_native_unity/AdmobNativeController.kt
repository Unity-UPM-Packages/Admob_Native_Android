// File: mylibrary/src/main/java/com/mycompany/admobnative/AdmobNativeController.kt
package com.thelegends.admob_native_unity

import android.app.Activity
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
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
import kotlinx.coroutines.Runnable

class AdmobNativeController(
    private val activity: Activity,
    private val callbacks: NativeAdCallbacks
) {

    private var loadedNativeAd: NativeAd? = null
    private var rootView: View? = null
    private var countdownTimer: CountDownTimer? = null
    private var countdownDurationMillis: Long = 5000

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
            })
            .withNativeAdOptions(adOptions)
            .build()
            .loadAd(adRequest)
    }

    fun showAd(layoutName: String) {

        this.activity.runOnUiThread {
            if (loadedNativeAd == null) return@runOnUiThread

            val layoutId = activity.resources.getIdentifier(
                layoutName,
                "layout",
                activity.packageName
            )

            if (layoutId == 0) {
                // Log lại lỗi để dễ dàng debug và thoát hàm một cách an toàn
                Log.e(TAG, "Layout resource not found for name: $layoutName. Ad will not be shown.")
                return@runOnUiThread
            }

            rootView = activity.layoutInflater.inflate(layoutId, null)
            val nativeAdView = rootView?.findViewById<NativeAdView>(R.id.native_ad_view)


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

            val currentAd = loadedNativeAd
            val view = rootView;
            if (currentAd != null && nativeAdView != null && view != null) {

                view.visibility = View.VISIBLE
                populateNativeAdView(currentAd, nativeAdView)

                layout.addView(rootView)

                handleCountdown(view)

                nativeAdView.elevation = 10f
                nativeAdView.bringToFront()
            }
        }
    }

    fun destroyAd() {
        activity.runOnUiThread {
            countdownTimer?.cancel()
            countdownTimer = null

            if (rootView != null) {
                (rootView?.parent as? ViewGroup)?.removeView(rootView)
            }

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

    fun setCountdownDuration(durationSeconds: Float) {
        if (durationSeconds > 0) {
            this.countdownDurationMillis = (durationSeconds * 1000).toLong() // Chuyển đổi sang mili giây
            Log.d(TAG, "Countdown duration updated to ${durationSeconds}s")
        } else {
            Log.w(TAG, "Invalid countdown duration received: $durationSeconds. Using default.")
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.media_view)
        adView.headlineView = adView.findViewById(R.id.primary)
        adView.bodyView = adView.findViewById(R.id.body)
        adView.callToActionView = adView.findViewById(R.id.cta)
        adView.iconView = adView.findViewById(R.id.icon)
        adView.starRatingView = adView.findViewById(R.id.rating_bar)
        adView.advertiserView = adView.findViewById(R.id.secondary)
//        adView.storeView = adView.findViewById(R.id.ad_store)
//        adView.priceView = adView.findViewById(R.id.ad_price)

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
//        val storeView = adView.storeView as? TextView
//        if (nativeAd.store != null) {
//            storeView?.text = nativeAd.store
//            storeView?.visibility = android.view.View.VISIBLE
//        } else {
//            storeView?.visibility = android.view.View.INVISIBLE
//        }
//
//        // 9. Price
//        val priceView = adView.priceView as? TextView
//        if (nativeAd.price != null) {
//            priceView?.text = nativeAd.price
//            priceView?.visibility = android.view.View.VISIBLE
//        } else {
//            priceView?.visibility = android.view.View.INVISIBLE
//        }

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

    private fun handleCountdown(rootView: View) {
        val closeButton = rootView.findViewById<ImageView>(R.id.ad_close_button)
        val progressBar = rootView.findViewById<ProgressBar>(R.id.ad_progress_bar)
        val countdownText = rootView.findViewById<TextView>(R.id.ad_countdown_text)

        countdownTimer?.cancel()
        closeButton?.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE
        countdownText?.visibility = View.VISIBLE

        countdownTimer = object : CountDownTimer(countdownDurationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                countdownText?.text = secondsRemaining.toString()
                progressBar?.progress = (millisUntilFinished * 100 / countdownDurationMillis).toInt()
            }

            override fun onFinish() {
                progressBar?.visibility = View.GONE
                countdownText?.visibility = View.GONE
                closeButton?.visibility = View.VISIBLE
            }
        }.start()

        closeButton?.setOnClickListener {
            destroyAd()
            callbacks.onAdClosed()
            Log.d(TAG, "Ad Closed")
        }
    }
}