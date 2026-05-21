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

/**
 * Base class for Native Ad rendering behaviors.
 * Supports both XML-based layouts and recursive view discovery.
 */
class XibShowBehavior : IShowBehavior {

    private val TAG = this.javaClass.simpleName
    private var rootView: FrameLayout? = null

    private var activityRef: java.lang.ref.WeakReference<Activity>? = null


    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String
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

            // Resolve NativeAdView: First by standard ID "native_ad_view", then by recursive search
            val nativeAdViewId = activity.resources.getIdentifier("native_ad_view", "id", activity.packageName)
            val nativeAdView = if (nativeAdViewId != 0) {
                adContentView.findViewById<NativeAdView>(nativeAdViewId)
            } else {
                findNativeAdViewRecursively(adContentView)
            }

            if (nativeAdView == null) {
                Log.e(TAG, "NativeAdView component not found in layout '$layoutName'. Ensure a NativeAdView exists in the hierarchy.")
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

    override fun getRootView(): View? {
        return this.rootView
    }

    override fun getRegisteredViews(): Map<String, View>? {
        return null // XIB doesn't map registered views out
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

        bindHeadline(nativeAd, adView)
        bindBody(nativeAd, adView)
        bindCallToAction(nativeAd, adView)
        bindIcon(nativeAd, adView)
        bindStarRating(nativeAd, adView)
        bindAdvertiser(nativeAd, adView)
        bindStore(nativeAd, adView)
        bindPrice(nativeAd, adView)

        adView.setNativeAd(nativeAd)
        Log.d(TAG, "Ad view has been successfully rendered.")
    }

    private fun bindHeadline(nativeAd: NativeAd, adView: NativeAdView) {
        (adView.headlineView as? TextView)?.text = nativeAd.headline
    }

    private fun bindBody(nativeAd: NativeAd, adView: NativeAdView) {
        val bodyView = adView.bodyView as? TextView
        if (nativeAd.body != null) {
            bodyView?.text = nativeAd.body
            bodyView?.visibility = android.view.View.VISIBLE
        } else {
            bodyView?.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindCallToAction(nativeAd: NativeAd, adView: NativeAdView) {
        val ctaButton = adView.callToActionView as? android.widget.Button
        if (nativeAd.callToAction != null) {
            ctaButton?.text = nativeAd.callToAction
            ctaButton?.visibility = android.view.View.VISIBLE
        } else {
            ctaButton?.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindIcon(nativeAd: NativeAd, adView: NativeAdView) {
        val iconView = adView.iconView as? android.widget.ImageView
        if (nativeAd.icon != null) {
            iconView?.setImageDrawable(nativeAd.icon?.drawable)
            iconView?.visibility = android.view.View.VISIBLE
        } else {
            iconView?.visibility = android.view.View.GONE
        }
    }

    private fun bindStarRating(nativeAd: NativeAd, adView: NativeAdView) {
        val ratingBar = adView.starRatingView as? android.widget.RatingBar
        if (nativeAd.starRating != null) {
            ratingBar?.rating = nativeAd.starRating!!.toFloat()
            ratingBar?.visibility = android.view.View.VISIBLE
        } else {
            ratingBar?.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindAdvertiser(nativeAd: NativeAd, adView: NativeAdView) {
        val advertiserView = adView.advertiserView as? TextView
        if (nativeAd.advertiser != null) {
            advertiserView?.text = nativeAd.advertiser
            advertiserView?.visibility = android.view.View.VISIBLE
        } else {
            advertiserView?.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindStore(nativeAd: NativeAd, adView: NativeAdView) {
        val storeView = adView.storeView as? TextView
        if (nativeAd.store != null) {
            storeView?.text = nativeAd.store
            storeView?.visibility = android.view.View.VISIBLE
        } else {
            storeView?.visibility = android.view.View.INVISIBLE
        }
    }

    private fun bindPrice(nativeAd: NativeAd, adView: NativeAdView) {
        val priceView = adView.priceView as? TextView
        if (nativeAd.price != null) {
            priceView?.text = nativeAd.price
            priceView?.visibility = android.view.View.VISIBLE
        } else {
            priceView?.visibility = android.view.View.INVISIBLE
        }
    }

    /**
     * Traverses the view tree to locate the first instance of a NativeAdView.
     * Useful for layouts where IDs are stripped or mapped dynamically.
     */
    private fun findNativeAdViewRecursively(view: View): NativeAdView? {
        if (view is NativeAdView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findNativeAdViewRecursively(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
}