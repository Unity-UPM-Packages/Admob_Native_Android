// File: app/src/main/java/com/yourcompany/admobnative/MainActivity.kt // Thay bằng package name của module app
package com.thelegends.ads.admobnative

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.thelegends.admob_native_unity.AdmobNativeController // Import controller của bạn
import com.thelegends.admob_native_unity.NativeAdCallbacks     // Import interface callback
import com.thelegends.ads.admobnative.R

class MainActivity : AppCompatActivity(), NativeAdCallbacks { // Implement interface

    private lateinit var admobNativeController: AdmobNativeController
    private val TAG = "MainActivityTest"

    // Sử dụng Ad Unit ID test của Google cho quảng cáo Native
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    // Tên file layout bạn đã tạo trong module library
    private val NATIVE_LAYOUT_NAME = "native_template"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Cần có layout cho app module

        // Tạo instance của controller, truyền vào Activity và chính nó làm listener
        admobNativeController = AdmobNativeController(this, this)

        // Thiết lập các nút bấm để test
        val initSdkButton: Button = findViewById(R.id.init_sdk_button)
        val loadAdButton: Button = findViewById(R.id.load_ad_button)
        val showAdButton: Button = findViewById(R.id.show_ad_button)

        initSdkButton.setOnClickListener {
            Log.d(TAG, "Initialize SDK button clicked.")

            // Gọi hàm khởi tạo của AdMob SDK
            MobileAds.initialize(this) { initializationStatus ->
                // Callback này được gọi khi khởi tạo xong
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status!!.description, status.latency))
                }

                // Thông báo cho người dùng biết đã xong
                Toast.makeText(this, "AdMob SDK Initialized!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "AdMob SDK initialization complete.")
            }
        }

        loadAdButton.setOnClickListener {
            Log.d(TAG, "Load Ad button clicked. Requesting a new ad...")
            val adRequest = AdRequest.Builder().build()
            admobNativeController.loadAd(TEST_AD_UNIT_ID, adRequest)
        }

        showAdButton.setOnClickListener {
            if (admobNativeController.isAdAvailable()) {
                Log.d(TAG, "Show Ad button clicked. Showing the ad...")
              admobNativeController
                  .withCountdown(5f,5f,2f)
                  .showAd(NATIVE_LAYOUT_NAME)
            } else {
                Toast.makeText(this, "Ad not available yet. Please load first.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Show Ad button clicked, but ad is not available.")
            }
        }
    }

    // === TRIỂN KHAI CÁC CALLBACK TỪ NativeAdCallbacks ===

    override fun onAdLoaded() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdLoaded")
        Toast.makeText(this, "Ad Loaded Successfully!", Toast.LENGTH_SHORT).show()
    }

    override fun onAdFailedToLoad(error: LoadAdError) {
        val errorMessage = "Error Code: ${error.code}, Message: ${error.message}"
        Log.e(TAG, "CALLBACK RECEIVED: onAdFailedToLoad - $errorMessage")
    }

    override fun onAdShow() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdShow")
    }

    override fun onAdClosed() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdClosed")
    }

    override fun onPaidEvent(precisionType: Int, valueMicros: Long, currencyCode: String) {
        val adValueString = "Value: ${valueMicros / 1000000.0} $currencyCode, Precision: $precisionType"
        Log.d(TAG, "CALLBACK RECEIVED: onPaidEvent - $adValueString")
    }

    override fun onAdDidRecordImpression() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdDidRecordImpression")
    }
    override fun onAdClicked() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdClicked")
    }
    override fun onVideoStart() {
        Log.d(TAG, "CALLBACK RECEIVED: onVideoStart")
    }
    override fun onVideoEnd() {
        Log.d(TAG, "CALLBACK RECEIVED: onVideoEnd")
    }
    override fun onVideoMute(isMuted: Boolean) {
        Log.d(TAG, "CALLBACK RECEIVED: onVideoMute - isMuted: $isMuted")
    }

    override fun onVideoPlay() {
        Log.d(TAG, "CALLBACK RECEIVED: onVideoPlay")
    }

    override fun onVideoPause() {
        Log.d(TAG, "CALLBACK RECEIVED: onVideoPause")
    }

    override fun onAdShowedFullScreenContent() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdShowedFullScreenContent")
    }

    override fun onAdDismissedFullScreenContent() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdDismissedFullScreenContent")
    }
}