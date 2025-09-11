package com.thelegends.ads.admob_native_unity.showbehavior

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.thelegends.admob_native_unity.NativeAdCallbacks
import com.thelegends.ads.admob_native_unity.R

open class BaseShowBehavior : IShowBehavior {

    private val TAG = this.javaClass.simpleName
    internal var rootView: FrameLayout? = null
        private set

    private var activityRef: java.lang.ref.WeakReference<Activity>? = null


    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String,
        callbacks: NativeAdCallbacks
    ) {
        this.activityRef = java.lang.ref.WeakReference(activity)

        activity.runOnUiThread {

            val adContainer = FrameLayout(activity)
            this.rootView = adContainer

            activity.addContentView(adContainer, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            val layoutId = activity.resources.getIdentifier(
                layoutName,
                "layout",
                activity.packageName
            )

            if (layoutId == 0) {
                Log.e(TAG, "Layout resource not found for name: $layoutName. Ad will not be shown.")
                (this.rootView?.parent as? ViewGroup)?.removeView(this.rootView)
                this.rootView = null
                return@runOnUiThread
            }

            val adContentView = activity.layoutInflater.inflate(layoutId, adContainer, false)

            val nativeAdView = adContentView.findViewById<NativeAdView>(R.id.native_ad_view)
            if (nativeAdView == null) {
                Log.e(TAG, "NativeAdView with ID 'native_ad_view' not found in layout '$layoutName'.")
                (this.rootView?.parent as? ViewGroup)?.removeView(this.rootView)
                this.rootView = null
                return@runOnUiThread
            }

            populateNativeAdView(nativeAd, nativeAdView)
            adContainer.addView(adContentView)
            adContainer.bringToFront()
        }
    }

    override fun destroy() {
        val activity = activityRef?.get() ?: return
        activity.runOnUiThread {
            (rootView?.parent as? ViewGroup)?.removeView(rootView)
            rootView = null
            Log.d(TAG, "Base Ad view has been destroyed.")
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

        Log.d(TAG, "Ad view has been shown")
    }

}