// File: mylibrary/src/main/java/com/mycompany/admobnative/NativeAdCallbacks.kt
package com.thelegends.admob_native_unity

import com.google.android.gms.ads.LoadAdError

interface IAdLoadCallback {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: LoadAdError)
}

interface IAdInteractionCallback {
    fun onAdShow()
    fun onAdClosed()
    fun onAdDidRecordImpression()
    fun onAdClicked()
    fun onAdShowedFullScreenContent()
    fun onAdDismissedFullScreenContent()
}

interface IAdVideoCallback {
    fun onVideoStart()
    fun onVideoEnd()
    fun onVideoMute(isMuted: Boolean)
    fun onVideoPlay()
    fun onVideoPause()
}

interface IAdRevenueCallback {
    fun onPaidEvent(precisionType: Int, valueMicros: Long, currencyCode: String)
}

/**
 * Interface định nghĩa các callback tổng hợp (dành cho Unity JNI Bridge).
 * Các thành phần native nội bộ nên sử dụng các interface nhỏ lẻ ở trên (ISP).
 */
interface NativeAdCallbacks : IAdLoadCallback, IAdInteractionCallback, IAdVideoCallback, IAdRevenueCallback