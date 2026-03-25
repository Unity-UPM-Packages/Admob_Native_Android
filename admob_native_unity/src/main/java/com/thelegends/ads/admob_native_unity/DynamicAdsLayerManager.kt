package com.thelegends.ads.admob_native_unity

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Giai Đoạn 3: Hệ thống Thùng Chứa (Bucket Layer) Toàn Cục.
 * Quản lý Z-Order của các Native Ads (Banner nằm dưới, Fullscreen nằm trên).
 */
object DynamicAdsLayerManager {

    var layerBanner: FrameLayout? = null
    var layerMrec: FrameLayout? = null
    var layerInter: FrameLayout? = null

    private var isInitialized = false

    fun init(activity: Activity) {
        if (isInitialized) return
        isInitialized = true

        // Tìm Mâm gốc của toàn bộ App Unity đang chạy trên Android
        val rootViewGroup = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        // Tạo 3 FrameLayout Vô Dấu Vết (Touch Pass-through) và nhét vào Mâm gốc
        // Lệnh addView đưa vào sau sẽ nằm lơ lửng đè lên trên lớp trước (Z-Order)
        layerBanner = createPassThroughLayer(activity).also { rootViewGroup.addView(it) }
        layerMrec = createPassThroughLayer(activity).also { rootViewGroup.addView(it) }
        layerInter = createPassThroughLayer(activity).also { rootViewGroup.addView(it) }
    }

    private fun createPassThroughLayer(activity: Activity): FrameLayout {
        return FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Đóng vai trò là Lớp Đệm Vô Hình, phải cho phép ngón tay vuốt xuyên qua 
            // chạm tới Game Unity ở dưới khi không có Quảng cáo hiện trên vùng đó.
            isClickable = false
            isFocusable = false
            clipChildren = false
        }
    }

    fun getLayer(layerName: String): FrameLayout? {
        return when (layerName) {
            "BannerLayer" -> layerBanner
            "MRECLayer" -> layerMrec
            "FullscreenLayer" -> layerInter
            else -> layerBanner
        }
    }
}
