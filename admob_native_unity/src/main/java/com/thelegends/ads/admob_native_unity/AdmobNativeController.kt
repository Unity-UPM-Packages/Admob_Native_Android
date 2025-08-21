// File: mylibrary/src/main/java/com/mycompany/admobnative/AdmobNativeController.kt
package com.thelegends.admob_native_unity

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.thelegends.ads.admob_native_unity.R

/**
 * Lớp chính xử lý logic AdMob Native.
 * Nó nhận Activity và một đối tượng NativeAdCallbacks để báo cáo kết quả.
 */
class AdmobNativeController(
    private val activity: Activity,
    private val callbacks: NativeAdCallbacks
) {

    private var loadedNativeAd: NativeAd? = null
    private var adView: NativeAdView? = null
    private var adContainer: FrameLayout? = null

    private val TAG = "AdmobNativeController"

    fun loadAd(adUnitId: String, adRequest: AdRequest) {
        destroyAd() // Hủy quảng cáo cũ trước khi tải mới

        Log.d(TAG, "Loading native ad for Ad Unit ID: $adUnitId")

        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true) // Yêu cầu video bắt đầu ở trạng thái tắt tiếng
            .setCustomControlsRequested(false)// dùng video control như các btn play, pause... của admob luôn hay custom
            .setClickToExpandRequested(false)// click vào video mở full video hay chuyển sang web của publisher như các định dạng khác
            .build()

        val adOptions = com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()

        AdLoader.Builder(activity, adUnitId)
            .forNativeAd { ad ->
                // THÀNH CÔNG!
                Log.d(TAG, "Ad loaded successfully.")
                this.loadedNativeAd = ad
                setupAdCallbacks(ad) // Gắn các callback
                callbacks.onAdLoaded() // Báo cáo về C#
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // THẤT BẠI!
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

    /**
     * Hiển thị quảng cáo đã tải.
     * @param layoutName Tên của file layout XML (ví dụ: "my_native_ad_layout").
     */
    fun showAd(layoutName: String) {
        val adToShow = loadedNativeAd
        if (adToShow == null) {
            Log.d(TAG, "showAd called but no ad is loaded.")
            return
        }

        activity.runOnUiThread {
            // **BƯỚC 1 (Giống Google): Chuẩn bị adView bằng cách inflate layout**
            val layoutId = activity.resources.getIdentifier(layoutName, "layout", activity.packageName)
            if (layoutId == 0) {
                Log.d(TAG, "Layout '$layoutName' not found!")
                // Có thể gọi callback thất bại ở đây nếu muốn
                // callbacks.onAdFailedToPresentFullScreenContent("Layout not found")
                return@runOnUiThread
            }
            val inflater = LayoutInflater.from(activity)
            val adView = inflater.inflate(layoutId, null) as NativeAdView

            val closeButton = adView.findViewById<ImageView>(R.id.ad_close_button)
            closeButton?.setOnClickListener {
                Log.d(TAG, "Close button clicked. Destroying ad.")
                destroyAd()
                callbacks.onAdClosed()
            }


            // **BƯỚC 2 (Giống Google): Điền dữ liệu vào adView**
            populateNativeAdView(adToShow, adView)

            // **BƯỚC 3 (Logic của chúng ta): Tìm hoặc tạo container**
            // Ví dụ của Google giả định container đã có sẵn. Chúng ta linh hoạt hơn.
            if (adContainer == null) {
                adContainer = FrameLayout(activity)
                val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
                rootView.addView(adContainer)
            }

            // **BƯỚC 4 (Giống Google): Thêm adView đã sẵn sàng vào container**
            adContainer?.removeAllViews()
            adContainer?.addView(adView)
            Log.d(TAG, "Native ad view has been shown.")
        }
    }

    fun destroyAd() {
        activity.runOnUiThread {
            adContainer?.removeAllViews()
            adContainer = null
            adView = null
        }
        loadedNativeAd?.destroy()
        loadedNativeAd = null
        Log.d(TAG, "Native ad has been destroyed.")
    }

    fun isAdAvailable(): Boolean = loadedNativeAd != null

    // Helper function để điền dữ liệu vào layout
    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // === BƯỚC 1: GÁN CÁC VIEW CHO SDK ===
        // Gán các view con cho các thuộc tính tương ứng của NativeAdView.
        // Việc này rất quan trọng để SDK có thể tự động xử lý click và impression.
        adView.mediaView = adView.findViewById(R.id.media_view)
        adView.headlineView = adView.findViewById(R.id.primary)
        adView.bodyView = adView.findViewById(R.id.body)
        adView.callToActionView = adView.findViewById(R.id.cta)
        adView.iconView = adView.findViewById(R.id.icon)
        adView.starRatingView = adView.findViewById(R.id.rating_bar)
        adView.advertiserView = adView.findViewById(R.id.secondary)
//        adView.storeView = adView.findViewById(R.id.ad_store) // Cần view có ID @+id/ad_store trong XML
//        adView.priceView = adView.findViewById(R.id.ad_price)   // Cần view có ID @+id/ad_price trong XML
        adView.mediaView?.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // === BƯỚC 2: ĐIỀN DỮ LIỆU VÀO TỪNG VIEW ===

        // 1. Media Content (Video/Hình ảnh)
        // Không cần code điền dữ liệu cho MediaView.
        // SDK sẽ tự động xử lý khi gọi setNativeAd(). Dòng adView.mediaView = ... ở Bước 1 là đủ.

        // 2. Headline (Tiêu đề)
        (adView.headlineView as? TextView)?.text = nativeAd.headline

        // 3. Body (Nội dung mô tả)
        val bodyView = adView.bodyView as? TextView
        if (nativeAd.body != null) {
            bodyView?.text = nativeAd.body
            bodyView?.visibility = android.view.View.VISIBLE
        } else {
            bodyView?.visibility = android.view.View.INVISIBLE
        }

        // 4. Call to Action (Nút kêu gọi hành động)
        val ctaButton = adView.callToActionView as? android.widget.Button
        if (nativeAd.callToAction != null) {
            ctaButton?.text = nativeAd.callToAction
            ctaButton?.visibility = android.view.View.VISIBLE
        } else {
            ctaButton?.visibility = android.view.View.INVISIBLE
        }

        // 5. Icon (Biểu tượng ứng dụng)
        val iconView = adView.iconView as? android.widget.ImageView
        if (nativeAd.icon != null) {
            iconView?.setImageDrawable(nativeAd.icon?.drawable)
            iconView?.visibility = android.view.View.VISIBLE
        } else {
            iconView?.visibility = android.view.View.GONE
        }

        // 6. Star Rating (Đánh giá sao)
        val ratingBar = adView.starRatingView as? android.widget.RatingBar
        if (nativeAd.starRating != null) {
            ratingBar?.rating = nativeAd.starRating!!.toFloat()
            ratingBar?.visibility = android.view.View.VISIBLE
        } else {
            ratingBar?.visibility = android.view.View.INVISIBLE
        }

        // 7. Advertiser (Nhà quảng cáo)
        val advertiserView = adView.advertiserView as? TextView
        if (nativeAd.advertiser != null) {
            advertiserView?.text = nativeAd.advertiser
            advertiserView?.visibility = android.view.View.VISIBLE
        } else {
            advertiserView?.visibility = android.view.View.INVISIBLE
        }

        // 8. Store (Cửa hàng)
//        val storeView = adView.storeView as? TextView
//        if (nativeAd.store != null) {
//            storeView?.text = nativeAd.store
//            storeView?.visibility = android.view.View.VISIBLE
//        } else {
//            storeView?.visibility = android.view.View.INVISIBLE
//        }
//
//        // 9. Price (Giá)
//        val priceView = adView.priceView as? TextView
//        if (nativeAd.price != null) {
//            priceView?.text = nativeAd.price
//            priceView?.visibility = android.view.View.VISIBLE
//        } else {
//            priceView?.visibility = android.view.View.INVISIBLE
//        }


        // === BƯỚC 3: KÍCH HOẠT TRACKING ===
        // Gán đối tượng NativeAd cho NativeAdView.
        // Đây là bước quan trọng nhất để SDK bắt đầu theo dõi impression, click
        // và tự động hiển thị video/hình ảnh vào MediaView.
        adView.setNativeAd(nativeAd)

        callbacks.onAdShow()
        Log.d(TAG, "Ad Opened")
    }

    // Helper function để gắn các callback vào NativeAd và VideoController
    private fun setupAdCallbacks(ad: NativeAd) {
        // Callbacks cho video (nếu có)
//        val mediaContent = ad.mediaContent
//        if (mediaContent != null && mediaContent.hasVideoContent()) {
//            mediaContent.videoController.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
//                override fun onVideoStart() = callbacks.onVideoStart()
//                override fun onVideoEnd() = callbacks.onVideoEnd()
//                override fun onVideoMute(isMuted: Boolean) = callbacks.onVideoMute(isMuted)
//            }
//        }

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

        // Callback cho Paid Event (Doanh thu)
        ad.setOnPaidEventListener { adValue ->
            Log.d(TAG, "Paid event received: $adValue")
            callbacks.onPaidEvent(
                adValue.precisionType,
                adValue.valueMicros,
                adValue.currencyCode
            )
        }
    }
}