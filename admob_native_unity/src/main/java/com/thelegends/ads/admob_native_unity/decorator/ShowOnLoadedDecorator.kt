package com.thelegends.admob_native_unity

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.ads.admob_native_unity.showbehavior.*


class ShowOnLoadedDecorator(
    private val wrappedBehavior: BaseShowBehavior,
    private val layoutName: String,
    private val controller: AdmobNativeController
) : BaseShowBehavior(), NativeAdCallbacks {

    init {
        controller.addAdCallbackListener(listener = this)
    }

    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String,
        callbacks: NativeAdCallbacks
    ) {

        wrappedBehavior.show(activity, nativeAd, layoutName, callbacks)
    }

    override fun destroy() {
        wrappedBehavior.destroy()
    }

    override fun onAdLoaded() {
        controller.showAd(this.layoutName)
    }

    // Thay thế tất cả các TODO bằng khối lệnh rỗng
    override fun onAdFailedToLoad(error: LoadAdError) {}
    override fun onAdShow() {}
    override fun onAdClosed() {}
    override fun onPaidEvent(precisionType: Int, valueMicros: Long, currencyCode: String) {}
    override fun onAdDidRecordImpression() {}
    override fun onAdClicked() {}
    override fun onVideoStart() {}
    override fun onVideoEnd() {}
    override fun onVideoMute(isMuted: Boolean) {}
    override fun onVideoPlay() {}
    override fun onVideoPause() {}
    override fun onAdShowedFullScreenContent() {}
    override fun onAdDismissedFullScreenContent() {}
}
