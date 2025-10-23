import Foundation
import GoogleMobileAds

/// Protocol định nghĩa các callback cho AdmobNativeController.
/// Tương đương với NativeAdCallbacks interface bên Kotlin.
@objc public protocol NativeAdCallbacks: AnyObject {
    // MARK: - Load & Show
    func onAdLoaded()
    func onAdFailedToLoad(error: Error)
    func onAdShow()
    func onAdClosed()
    
    // MARK: - Revenue
    func onPaidEvent(precisionType: Int, valueMicros: Int64, currencyCode: String)
    func onAdDidRecordImpression()
    func onAdClicked()
    
    // MARK: - Video
    func onVideoStart()
    func onVideoEnd()
    func onVideoMute(isMuted: Bool)
    func onVideoPlay()
    func onVideoPause()
    
    // MARK: - Full Screen
    func onAdShowedFullScreenContent()
    func onAdDismissedFullScreenContent()
}
