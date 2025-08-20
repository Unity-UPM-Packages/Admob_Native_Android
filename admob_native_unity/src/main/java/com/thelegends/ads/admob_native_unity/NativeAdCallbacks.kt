// File: mylibrary/src/main/java/com/mycompany/admobnative/NativeAdCallbacks.kt
package com.thelegends.admob_native_unity

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError

/**
 * Interface định nghĩa các callback cho AdmobNativeController.
 * Cả ứng dụng test (Android Studio) và Unity đều sẽ implement interface này.
 */
interface NativeAdCallbacks {
    // Tải quảng cáo
    fun onAdLoaded()
    fun onAdFailedToLoad(error: LoadAdError)
    fun onAdShow();
    fun onAdClosed();

    // Doanh thu
    fun onPaidEvent(precisionType: Int, valueMicros: Long, currencyCode: String)
    fun onAdDidRecordImpression()
    fun onAdClicked()

    // Video
    fun onVideoStart()
    fun onVideoEnd()
    fun onVideoMute(isMuted: Boolean)
    fun onVideoPlay()
    fun onVideoPause()

}