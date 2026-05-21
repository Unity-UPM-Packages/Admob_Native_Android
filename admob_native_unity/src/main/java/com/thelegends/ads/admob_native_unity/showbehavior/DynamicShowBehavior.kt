package com.thelegends.ads.admob_native_unity.showbehavior

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.admob_native_unity.NativeAdCallbacks
import com.thelegends.ads.admob_native_unity.NativeAdUnityRenderer
import com.thelegends.ads.admob_native_unity.NativeAdLayerManager

/**
 * Orchestrates the rendering of dynamic Native Ads using JSON blueprints.
 * Injects the generated UI into specialized Z-ordered layers (Buckets) to ensure
 * consistency across various screen types and notch configurations.
 */
class DynamicShowBehavior(
    private val jsonPayload: String, 
    private val zLayerName: String
) : IShowBehavior {

    private val TAG = "DynamicShowBehavior"
    var renderer: NativeAdUnityRenderer? = null
    private var targetBucket: FrameLayout? = null
    private var activityRef: java.lang.ref.WeakReference<Activity>? = null
    private var rootView: View? = null

    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String
    ) {
        this.activityRef = java.lang.ref.WeakReference(activity)

        activity.runOnUiThread {
            Log.d(TAG, "Activating Dynamic Native UI for: $layoutName, Layer: $zLayerName")
            
            // 0. Ensure the Global Bucket Layers are initialized
            NativeAdLayerManager.init(activity)

            // 1. Resolve target layer
            targetBucket = when (zLayerName) {
                "Banner" -> NativeAdLayerManager.layerBanner
                "FullScreen" -> NativeAdLayerManager.layerFullscreen
                else -> NativeAdLayerManager.layerBanner
            }

            if (targetBucket == null) {
                Log.e(TAG, "Critical Fault: Suitable Z-Layer Bucket not found!")
                return@runOnUiThread
            }

            // 3. Construct Content View & Pre-render JSON to extract Root boundary
            val customLayout = NativeAdUnityRenderer(activity)
            customLayout.buildFromJson(jsonPayload)

            // Fetch the localized Pixel Rect of the RootAdView to size the AdMob NativeAdView anchor
            val rootRect = customLayout.getRootPixelRect()
            val adWidth  = if (rootRect.width()  > 0) rootRect.width()  else ViewGroup.LayoutParams.MATCH_PARENT
            val adHeight = if (rootRect.height() > 0) rootRect.height() else ViewGroup.LayoutParams.MATCH_PARENT

            // 2. Tạo NativeAdView với kích thước chính xác (không còn MATCH_PARENT → không block touch)
            val adMobView = com.google.android.gms.ads.nativead.NativeAdView(activity)
            val adMobParams = FrameLayout.LayoutParams(adWidth, adHeight).apply {
                leftMargin = rootRect.left
                topMargin  = rootRect.top
            }
            adMobView.layoutParams = adMobParams

            // customLayout fills the NativeAdView container (using normalized local coordinates)
            customLayout.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            this.renderer = customLayout
            this.rootView = adMobView

            // 4. Composition: Inject Custom Layout into the AdMob View
            adMobView.addView(customLayout)

            // 5. Data Binding (Giai đoạn 6) - Mapping NativeAd assets to the rendered views
            AdDataBinder.bind(nativeAd, adMobView, customLayout)

            // 6. Nhấn nút để SDK Ghi Nhận View/Click
            adMobView.setNativeAd(nativeAd)

            // 6. Nhét toàn bộ cụm vào thùng chứa Z-Layer
            targetBucket?.addView(adMobView)
            
            // Lôi thùng chứa ra đầu
            targetBucket?.visibility = View.VISIBLE
        }
    }

    override fun destroy() {
        val activity = activityRef?.get() ?: return
        activity.runOnUiThread {
            // Decouple NativeAdView (rootView) from the target bucket
            val adMobView = this.rootView
            if (adMobView != null) {
                val parent = adMobView.parent as? ViewGroup
                parent?.removeView(adMobView)
            }

            // Hide bucket if empty to prevent touch interference
            if (targetBucket?.childCount == 0) {
                targetBucket?.visibility = View.GONE
            }

            renderer = null
            this.rootView = null
        }
    }

    override fun getRootView(): View? {
        return this.rootView
    }

    override fun getRegisteredViews(): Map<String, View>? {
        return renderer?.registeredViews
    }
}
