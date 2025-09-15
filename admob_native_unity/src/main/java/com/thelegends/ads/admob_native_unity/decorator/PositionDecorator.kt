package com.thelegends.ads.admob_native_unity.decorator

import com.thelegends.admob_native_unity.*
import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.nativead.NativeAd
import com.thelegends.ads.admob_native_unity.showbehavior.BaseShowBehavior


class PositionDecorator(
    private val wrappedBehavior: BaseShowBehavior,
    private val controller: AdmobNativeController,
    private val positionX: Int,
    private val positionY: Int
) : BaseShowBehavior() {


    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String,
        callbacks: NativeAdCallbacks
    ) {
        activity.runOnUiThread {
            wrappedBehavior.show(activity, nativeAd, layoutName, callbacks)

            this.rootView = wrappedBehavior.rootView

            val adContent = rootView?.findViewById<View>(
                activity.resources.getIdentifier("ad_content", "id", activity.packageName)
            )

            adContent?.visibility = View.INVISIBLE

            adContent?.post {
                val params = adContent.layoutParams as? FrameLayout.LayoutParams
                params?.let {
                    it.gravity = Gravity.TOP or Gravity.START
                    it.leftMargin = positionX
                    it.topMargin = positionY
                    adContent.layoutParams = it
                }

                adContent.visibility = View.VISIBLE
            }

        }

    }


    override fun destroy() {
        wrappedBehavior.destroy()
    }
}