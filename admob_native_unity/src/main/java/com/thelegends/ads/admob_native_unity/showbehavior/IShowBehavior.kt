package com.thelegends.ads.admob_native_unity.showbehavior

import android.app.Activity
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.admob_native_unity.NativeAdCallbacks

interface IShowBehavior {

    fun show(activity: Activity, nativeAd: NativeAd, layoutName: String, callbacks: NativeAdCallbacks)

    fun destroy()
}