package com.thelegends.ads.admob_native_unity.showbehavior

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.thelegends.ads.admob_native_unity.NativeAdUnityRenderer

object AdDataBinder {
    fun bind(nativeAd: NativeAd, adMobView: NativeAdView, customLayout: NativeAdUnityRenderer) {
        val registeredViews = customLayout.registeredViews

        val tvHeadline = (registeredViews["Headline_Text"] as? TextView) ?: (registeredViews["Headline"] as? TextView)
        tvHeadline?.text = nativeAd.headline
        adMobView.headlineView = registeredViews["Headline"]

        val tvCta = (registeredViews["CallToAction_Text"] as? TextView) ?: (registeredViews["CallToAction"] as? TextView)
        tvCta?.text = nativeAd.callToAction
        adMobView.callToActionView = registeredViews["CallToAction"]

        val tvBody = (registeredViews["Body_Text"] as? TextView) ?: (registeredViews["Body"] as? TextView)
        if (nativeAd.body.isNullOrEmpty()) {
            registeredViews["Body"]?.visibility = View.INVISIBLE
        } else {
            tvBody?.text = nativeAd.body
            adMobView.bodyView = registeredViews["Body"]
        }

        val tvAdv = (registeredViews["Advertiser_Text"] as? TextView) ?: (registeredViews["Advertiser"] as? TextView)
        if (nativeAd.advertiser.isNullOrEmpty()) {
            registeredViews["Advertiser"]?.visibility = View.INVISIBLE
        } else {
            tvAdv?.text = nativeAd.advertiser
            adMobView.advertiserView = registeredViews["Advertiser"]
        }

        val tvStore = (registeredViews["Store_Text"] as? TextView) ?: (registeredViews["Store"] as? TextView)
        if (nativeAd.store.isNullOrEmpty()) {
            registeredViews["Store"]?.visibility = View.INVISIBLE
        } else {
            tvStore?.text = nativeAd.store
            adMobView.storeView = registeredViews["Store"]
        }
        
        val tvPrice = (registeredViews["Price_Text"] as? TextView) ?: (registeredViews["Price"] as? TextView)
        if (nativeAd.price.isNullOrEmpty()) {
            registeredViews["Price"]?.visibility = View.INVISIBLE
        } else {
            tvPrice?.text = nativeAd.price
            adMobView.priceView = registeredViews["Price"]
        }
        
        val rbStar = (registeredViews["StarRating_Text"] as? TextView) ?: (registeredViews["StarRating"] as? TextView)
        if (nativeAd.starRating == null) {
            registeredViews["StarRating"]?.visibility = View.INVISIBLE
        } else {
            rbStar?.text = nativeAd.starRating.toString()
            adMobView.starRatingView = registeredViews["StarRating"]
        }

        val ivIcon = registeredViews["IconView"]
        if (ivIcon != null) {
            if (nativeAd.icon == null) {
                ivIcon.visibility = View.INVISIBLE
            } else {
                val imageView = ivIcon as? ImageView ?: (ivIcon as? FrameLayout)?.getChildAt(0) as? ImageView
                imageView?.setImageDrawable(nativeAd.icon?.drawable)
                adMobView.iconView = ivIcon
            }
        }

        val mv = registeredViews["MediaView"] as? com.google.android.gms.ads.nativead.MediaView
        if (mv != null) {
            adMobView.mediaView = mv
        }
    }
}
