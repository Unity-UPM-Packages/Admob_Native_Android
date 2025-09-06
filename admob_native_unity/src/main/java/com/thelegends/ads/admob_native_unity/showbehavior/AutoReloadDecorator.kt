package com.thelegends.admob_native_unity

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.ads.admob_native_unity.showbehavior.BaseShowBehavior
import com.google.android.gms.ads.*

// ... các import khác

class AutoReloadDecorator(
    private val wrappedBehavior: BaseShowBehavior,
    private val adUnitId: String,
    private val intervalSeconds: Long,
    private val controller: AdmobNativeController // Vẫn cần controller để gọi loadAd
) : BaseShowBehavior() {

    private val TAG = "AutoReloadDecorator"
    private val handler = Handler(Looper.getMainLooper())
    private var reloadRunnable: Runnable? = null

    override fun show(activity: Activity, nativeAd: NativeAd, layoutName: String, callbacks: NativeAdCallbacks) {
        cancelReload()
        wrappedBehavior.show(activity, nativeAd, layoutName, callbacks)
        scheduleReload()
    }

    override fun destroy() {
        cancelReload()
        wrappedBehavior.destroy()
    }

    private fun scheduleReload() {
        reloadRunnable = Runnable {
            Log.d(TAG, "Timer finished. Requesting new ad load for '$adUnitId'.")
            controller.reloadAd(adUnitId, AdRequest.Builder().build())
        }
        handler.postDelayed(reloadRunnable!!, intervalSeconds * 1000)
    }

    private fun cancelReload() {
        reloadRunnable?.let { handler.removeCallbacks(it) }
        reloadRunnable = null
    }
}