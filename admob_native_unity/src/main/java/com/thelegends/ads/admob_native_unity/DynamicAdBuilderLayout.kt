package com.thelegends.ads.admob_native_unity

import android.content.Context
import android.widget.FrameLayout

/**
 * Giai Đoạn 3 & 5: Mâm Chứa Quảng Cáo Cục Bộ (Tượng trưng cho RootAdView bên Unity).
 * Sẽ biến hình (Override onLayout) dập khuôn 100% tỷ lệ từ JSON ở Giai đoạn 5.
 */
class DynamicAdBuilderLayout(context: Context) : FrameLayout(context) {

    // Kho lưu trữ JSON Blueprint
    var jsonPayload: String? = null
    
    // Hệ Quy Chiếu Độ Phân Giải từ Unity
    var referenceWidth: Float = 1080f
    var referenceHeight: Float = 1920f

    init {
        // Tắt cắt xén để bóng (shadow) hoặc nút lồi ra ngoài viền không bị chém mất.
        clipChildren = false
        clipToPadding = false
    }

    // Giai đoạn 5 sẽ đổ Code Toán Học vào đây
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        
        // TODO: Đo size vật lý thật của bảng quảng cáo này (measuredWidth, measuredHeight)
        // TODO: Biến dịch tọa độ JSON Anchor (0.0 -> 1.0) thành Tọa độ Pixel dán đè lên con
    }
}
