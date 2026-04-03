package com.thelegends.ads.admob_native_unity

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Giai Đoạn 3: Hệ thống Thùng Chứa (Bucket Layer) Toàn Cục.
 * Quản lý Z-Order của các Native Ads theo đúng Numerical Layer (Banner < MREC < Fullscreen).
 */
@SuppressLint("StaticFieldLeak")
object DynamicAdsLayerManager {

    var layerBanner: FrameLayout? = null
    var layerFullscreen: FrameLayout? = null

    private var isInitialized = false

    fun init(activity: Activity) {
        if (isInitialized) return
        isInitialized = true

        // Tìm Mâm gốc của toàn bộ App Unity đang chạy trên Android
        val rootViewGroup = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        // Tạo các FrameLayout Vô Dấn Vết (Touch Pass-through)
        // Dựa trên Plan: BannerLayer = 10, FullscreenLayer = 100
        layerBanner = createPassThroughLayer(activity, 10f).also { rootViewGroup.addView(it) }
        layerFullscreen = createPassThroughLayer(activity, 100f).also { rootViewGroup.addView(it) }
    }

    private fun createPassThroughLayer(activity: Activity, z: Float): FrameLayout {
        return FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Đóng vai trò là Lớp Đệm Vô Hình, phải cho phép ngón tay vuốt xuyên qua 
            isClickable = false
            isFocusable = false
            clipChildren = false
            translationZ = z // Khóa vĩnh viễn quyền đè lớp Z-Index
        }
    }

    fun getLayer(layerName: String): FrameLayout? {
        return when (layerName) {
            "Banner" -> layerBanner
            "FullScreen" -> layerFullscreen
            else -> layerBanner
        }
    }
}
