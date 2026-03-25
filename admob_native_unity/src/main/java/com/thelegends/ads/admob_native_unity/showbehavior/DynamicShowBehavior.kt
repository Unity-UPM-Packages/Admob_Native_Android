package com.thelegends.ads.admob_native_unity.showbehavior

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.admob_native_unity.NativeAdCallbacks
import com.thelegends.ads.admob_native_unity.DynamicAdBuilderLayout
import com.thelegends.ads.admob_native_unity.DynamicAdsLayerManager

/**
 * GIAI ĐOẠN 5: Bộ Xử Lý Khởi Chạy UI Động (Thay thế BaseShowBehavior khi có JSON)
 * Ném toàn bộ Giao diện đúc từ JSON vào 3 Tầng Kính (Z-Layer) chống thủng Notch.
 */
class DynamicShowBehavior(
    private val jsonPayload: String, 
    private val zLayerName: String
) : BaseShowBehavior() {

    private val TAG = "DynamicShowBehavior"
    private var builderLayout: DynamicAdBuilderLayout? = null
    private var targetBucket: FrameLayout? = null
    private var activityRef: java.lang.ref.WeakReference<Activity>? = null

    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String,
        callbacks: NativeAdCallbacks
    ) {
        this.activityRef = java.lang.ref.WeakReference(activity)

        activity.runOnUiThread {
            Log.d(TAG, "Kích hoạt Dynamic Native UI cho: $layoutName, Layer: $zLayerName")
            
            // 0. Bắt buộc mọc rễ các Tầng Kính (Bucket) nếu chưa có
            DynamicAdsLayerManager.init(activity)

            // 1. Phân giải Tầng
            targetBucket = when (zLayerName) {
                "MRECLayer" -> DynamicAdsLayerManager.layerMrec
                "FullscreenLayer" -> DynamicAdsLayerManager.layerInter
                else -> DynamicAdsLayerManager.layerBanner
            }

            if (targetBucket == null) {
                Log.e(TAG, "Lỗi: Không tìm thấy Tầng Kính (Bucket) phù hợp!")
                return@runOnUiThread
            }

            // 2. Tạo NativeAdView xịn từ SDK (Vì class này final, không thể kế thừa)
            val adMobView = com.google.android.gms.ads.nativead.NativeAdView(activity)
            adMobView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // 3. Tạc tượng View nội dung của chúng ta
            val customLayout = DynamicAdBuilderLayout(activity)
            customLayout.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            this.builderLayout = customLayout
            
            // Dọn đường cho PositionDecorator (nếu có)
            this.rootView = adMobView

            // Đúc JSON vào Layout nội bộ
            customLayout.buildFromJson(jsonPayload)

            // 4. Nhét Custom Layout vào trong AdMob View (Composition)
            adMobView.addView(customLayout)

            // 5. Truyền Hồn (Giai đoạn 6 - Data Binding)
            val tvHeadline = (customLayout.registeredViews["Headline_Text"] as? android.widget.TextView) ?: (customLayout.registeredViews["Headline"] as? android.widget.TextView)
            tvHeadline?.text = nativeAd.headline
            adMobView.headlineView = customLayout.registeredViews["Headline"]

            val tvCta = (customLayout.registeredViews["CallToAction_Text"] as? android.widget.TextView) ?: (customLayout.registeredViews["CallToAction"] as? android.widget.TextView)
            tvCta?.text = nativeAd.callToAction
            adMobView.callToActionView = customLayout.registeredViews["CallToAction"]

            val tvBody = (customLayout.registeredViews["Body_Text"] as? android.widget.TextView) ?: (customLayout.registeredViews["Body"] as? android.widget.TextView)
            if (nativeAd.body.isNullOrEmpty()) {
                customLayout.registeredViews["Body"]?.visibility = View.INVISIBLE
            } else {
                tvBody?.text = nativeAd.body
                adMobView.bodyView = customLayout.registeredViews["Body"]
            }

            val tvAdv = (customLayout.registeredViews["Advertiser_Text"] as? android.widget.TextView) ?: (customLayout.registeredViews["Advertiser"] as? android.widget.TextView)
            if (nativeAd.advertiser.isNullOrEmpty()) {
               customLayout.registeredViews["Advertiser"]?.visibility = View.INVISIBLE
            } else {
               tvAdv?.text = nativeAd.advertiser
               adMobView.advertiserView = customLayout.registeredViews["Advertiser"]
            }

            val tvStore = (customLayout.registeredViews["Store_Text"] as? android.widget.TextView) ?: (customLayout.registeredViews["Store"] as? android.widget.TextView)
            if (nativeAd.store.isNullOrEmpty()) {
               customLayout.registeredViews["Store"]?.visibility = View.INVISIBLE
            } else {
               tvStore?.text = nativeAd.store
               adMobView.storeView = customLayout.registeredViews["Store"]
            }
            
            val tvPrice = (customLayout.registeredViews["Price_Text"] as? android.widget.TextView) ?: (customLayout.registeredViews["Price"] as? android.widget.TextView)
            if (nativeAd.price.isNullOrEmpty()) {
               customLayout.registeredViews["Price"]?.visibility = View.INVISIBLE
            } else {
               tvPrice?.text = nativeAd.price
               adMobView.priceView = customLayout.registeredViews["Price"]
            }
            
            val rbStar = (customLayout.registeredViews["StarRating_Text"] as? android.widget.TextView) ?: (customLayout.registeredViews["StarRating"] as? android.widget.TextView)
            if (nativeAd.starRating == null) {
               customLayout.registeredViews["StarRating"]?.visibility = View.INVISIBLE
            } else {
               rbStar?.text = nativeAd.starRating.toString()
               adMobView.starRatingView = customLayout.registeredViews["StarRating"]
            }

            // Xử lý Icon Mạng
            val ivIcon = customLayout.registeredViews["IconView"]
            if (ivIcon != null) {
                if (nativeAd.icon == null) {
                    ivIcon.visibility = View.INVISIBLE
                } else {
                    // View đôi khi là FrameLayout chứa ImageView, ép kiểu cẩn thận
                    val imageView = ivIcon as? android.widget.ImageView ?: (ivIcon as? FrameLayout)?.getChildAt(0) as? android.widget.ImageView
                    imageView?.setImageDrawable(nativeAd.icon?.drawable)
                    adMobView.iconView = ivIcon
                }
            }

            // Xử lý Media Mạng (Video/Ảnh Bìa)
            val mv = customLayout.registeredViews["MediaView"] as? com.google.android.gms.ads.nativead.MediaView
            if (mv != null) {
                adMobView.mediaView = mv
            }

            // 6. Nhấn nút để SDK Ghi Nhận View/Click
            adMobView.setNativeAd(nativeAd)

            // 6. Nhét toàn bộ cụm vào thùng chứa Z-Layer
            targetBucket?.addView(adMobView)
            
            // Lôi thùng chứa ra đầu
            targetBucket?.visibility = View.VISIBLE
            targetBucket?.bringToFront()
        }
    }

    override fun destroy() {
        val activity = activityRef?.get() ?: return
        activity.runOnUiThread {
            val parent = builderLayout?.parent as? ViewGroup
            parent?.removeView(builderLayout)
            builderLayout = null
            this.rootView = null
            Log.d(TAG, "Dynamic Ad view has been properly dissolved.")
        }
    }
}
