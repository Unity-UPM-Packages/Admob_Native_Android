import Foundation
import UIKit
import GoogleMobileAds

/// Protocol định nghĩa hành vi hiển thị quảng cáo.
/// Tương đương với IShowBehavior interface bên Kotlin.
@objc public protocol IShowBehavior: AnyObject {
    /// Hiển thị quảng cáo native
    /// - Parameters:
    ///   - viewController: UIViewController hiện tại
    ///   - nativeAd: GADNativeAd đã được load
    ///   - layoutName: Tên của layout (.xib file)
    ///   - callbacks: Callback listener
    func show(viewController: UIViewController,
              nativeAd: GADNativeAd,
              layoutName: String,
              callbacks: NativeAdCallbacks)
    
    /// Hủy và xóa quảng cáo khỏi màn hình
    func destroy()
}
