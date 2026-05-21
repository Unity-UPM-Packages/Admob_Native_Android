package com.thelegends.ads.admob_native_unity.showbehavior

import android.app.Activity
import android.view.View
import com.google.android.gms.ads.nativead.NativeAd

interface IShowBehavior {

    fun show(activity: Activity, nativeAd: NativeAd, layoutName: String)

    fun destroy()

    fun getRootView(): View?

    fun getRegisteredViews(): Map<String, View>?
}