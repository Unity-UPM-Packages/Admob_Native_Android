# AdMob Native iOS Implementation

## 📁 Cấu trúc Files

```
iOS/
├── AdmobNativeBridge.h         # C header (interface cho Unity)
├── AdmobNativeBridge.swift     # C bridge implementation
├── NativeAdCallbacks.swift     # Callback protocol
├── IShowBehavior.swift         # Show behavior interface
├── BaseShowBehavior.swift      # Base implementation
├── CountdownDecorator.swift    # Countdown decorator
├── PositionDecorator.swift     # Position decorator
└── AdmobNativeController.swift # Main controller
```

## 🎯 Tương đương với Android

| iOS Swift | Android Kotlin | Mô tả |
|-----------|----------------|-------|
| `NativeAdCallbacks.swift` | `NativeAdCallbacks.kt` | Callback interface |
| `IShowBehavior.swift` | `IShowBehavior.kt` | Show behavior protocol |
| `BaseShowBehavior.swift` | `BaseShowBehavior.kt` | Base show implementation |
| `CountdownDecorator.swift` | `CountdownDecorator.kt` | Countdown với 3 timers |
| `PositionDecorator.swift` | `PositionDecorator.kt` | Position decorator |
| `AdmobNativeController.swift` | `AdmobNativeController.kt` | Main controller |
| `AdmobNativeBridge.swift` | N/A | C bridge cho Unity |

## 🏗️ Setup trong Xcode

### 1. Tạo Project Test

```bash
# Trong Xcode:
File -> New -> Project
-> iOS -> App
-> Product Name: "AdmobNativeTestApp"
-> Language: Swift
-> Interface: Storyboard
```

### 2. Tạo Framework Target

```bash
File -> New -> Target
-> iOS -> Framework
-> Product Name: "AdmobNative"
-> Language: Swift
```

### 3. Thêm Files vào Framework Target

- Select tất cả files .swift và .h
- Trong File Inspector, check vào "AdmobNative" target
- Đảm bảo "AdmobNativeBridge.h" được set là Public header

### 4. Cài đặt AdMob SDK qua CocoaPods

Tạo `Podfile` ở thư mục gốc project:

```ruby
platform :ios, '13.0'

target 'AdmobNativeTestApp' do
  use_frameworks!
  pod 'Google-Mobile-Ads-SDK', '~> 11.13.0'
end

target 'AdmobNative' do
  use_frameworks!
  pod 'Google-Mobile-Ads-SDK', '~> 11.13.0'
end
```

Chạy:
```bash
pod install
```

Sau đó mở `.xcworkspace` thay vì `.xcodeproj`

### 5. Configure Build Settings

**Framework Target (AdmobNative):**
- Build Settings -> Defines Module: YES
- Build Settings -> Module Name: AdmobNative
- Build Settings -> Swift Language Version: Swift 5
- Build Phases -> Headers -> Move "AdmobNativeBridge.h" to "Public"

## 🧪 Test trong App Target

### 1. Tạo Test Layout (.xib)

Trong App target, tạo file mới:
```
File -> New -> File -> View -> Empty
Tên: "MediumNativeAdLayout.xib"
```

**Setup trong Interface Builder:**
1. Set File's Owner = `GADNativeAdView` class
2. Thêm các UI elements (UILabel, UIImageView, UIButton, v.v.)
3. Gán **Tag** cho từng element theo bảng:
   - 101: Headline (UILabel)
   - 102: Body (UILabel)
   - 103: Media View (GADMediaView)
   - 104: Icon (UIImageView)
   - 105: Call to Action (UIButton)
   - 110: Close Button (UIImageView)
   - 111: Countdown Text (UILabel)
   - 112: Progress Bar (UIProgressView)

### 2. Update Info.plist

Thêm GADApplicationIdentifier:

```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-3940256099942544~1458002511</string>
```

### 3. Implement Test Code

Trong `ViewController.swift`:

```swift
import UIKit
import AdmobNative

class ViewController: UIViewController {
    
    var controller: AdmobNativeController?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Initialize Mobile Ads SDK
        GADMobileAds.sharedInstance().start { status in
            print("✅ AdMob SDK initialized")
            self.testNativeAd()
        }
    }
    
    func testNativeAd() {
        // Tạo callback handler
        let callbacks = TestCallbacks()
        
        // Tạo controller
        controller = AdmobNativeController(
            viewController: self,
            callbacks: callbacks
        )
        
        // Configure với builder pattern
        controller?
            .withCountdown(initial: 2.0, duration: 5.0, closeDelay: 1.0)
            .withPosition(x: 0, y: 100)
        
        // Load ad
        let testAdUnitId = "ca-app-pub-3940256099942544/3986624511" // Test ID
        let request = GADRequest()
        
        controller?.loadAd(adUnitId: testAdUnitId, request: request)
    }
}

// MARK: - Test Callbacks

class TestCallbacks: NSObject, NativeAdCallbacks {
    func onAdLoaded() {
        print("✅ TEST: Ad loaded")
    }
    
    func onAdFailedToLoad(error: Error) {
        print("❌ TEST: Ad failed to load: \(error.localizedDescription)")
    }
    
    func onAdShow() {
        print("📺 TEST: Ad shown")
    }
    
    func onAdClosed() {
        print("🚪 TEST: Ad closed")
    }
    
    func onPaidEvent(precisionType: Int, valueMicros: Int64, currencyCode: String) {
        print("💰 TEST: Paid event: \(valueMicros) \(currencyCode)")
    }
    
    func onAdDidRecordImpression() {
        print("👁️ TEST: Impression recorded")
    }
    
    func onAdClicked() {
        print("👆 TEST: Ad clicked")
    }
    
    func onVideoStart() {
        print("▶️ TEST: Video started")
    }
    
    func onVideoEnd() {
        print("⏹️ TEST: Video ended")
    }
    
    func onVideoMute(isMuted: Bool) {
        print("🔇 TEST: Video mute: \(isMuted)")
    }
    
    func onVideoPlay() {
        print("▶️ TEST: Video play")
    }
    
    func onVideoPause() {
        print("⏸️ TEST: Video pause")
    }
    
    func onAdShowedFullScreenContent() {
        print("📱 TEST: Showed full screen")
    }
    
    func onAdDismissedFullScreenContent() {
        print("📱 TEST: Dismissed full screen")
    }
}
```

### 4. Tự động show ad khi loaded

Trong `TestCallbacks`, thêm reference đến controller:

```swift
class TestCallbacks: NSObject, NativeAdCallbacks {
    weak var controller: AdmobNativeController?
    
    func onAdLoaded() {
        print("✅ TEST: Ad loaded")
        // Auto show với layout name
        controller?.showAd(layoutName: "MediumNativeAdLayout")
    }
    
    // ... rest of methods
}

// Trong ViewController:
func testNativeAd() {
    let callbacks = TestCallbacks()
    controller = AdmobNativeController(viewController: self, callbacks: callbacks)
    callbacks.controller = controller  // Set reference
    
    // ... rest of code
}
```

## 🚀 Build & Run

1. Select scheme: **AdmobNativeTestApp**
2. Select destination: **iPhone Simulator** hoặc thiết bị thật
3. Cmd + R để build và run
4. Theo dõi console log để debug

## ✅ Expected Flow

```
✅ AdMob SDK initialized
📡 AdmobNativeController: Loading ad for unit ID: ca-app-pub-...
✅ Ad loaded successfully
✅ TEST: Ad loaded
📺 AdmobNativeController: Showing ad with layout: MediumNativeAdLayout
🎨 Applied PositionDecorator: (0, 100)
⏱️ Applied CountdownDecorator: initial=2.0s, duration=5.0s, closeDelay=1.0s
✅ BaseShowBehavior: Ad view populated successfully
✅ BaseShowBehavior: Ad view displayed successfully
📺 TEST: Ad shown
⏱️ CountdownDecorator: Starting Phase 1 - Initial delay (2.0s)
✅ Phase 1 complete - Starting countdown
⏱️ CountdownDecorator: Starting Phase 2 - Countdown (5.0s)
✅ Phase 2 complete - Starting close button delay
⏱️ CountdownDecorator: Starting Phase 3 - Close button delay (1.0s)
✅ Phase 3 complete - Close button now clickable
👆 Close button tapped - Destroying ad
🚪 TEST: Ad closed
🗑️ AdmobNativeController: Destroying ad
✅ BaseShowBehavior: Ad view destroyed
```

## 🐛 Common Issues

### Issue 1: "Module 'AdmobNative' not found"
**Solution:** 
- Check Framework target được build successfully
- Verify "Defines Module" = YES
- Clean build folder (Shift + Cmd + K)

### Issue 2: Layout không load được
**Solution:**
- Verify .xib file được add vào App target
- Check tên layout chính xác (case-sensitive)
- Đảm bảo root view của .xib là GADNativeAdView

### Issue 3: Tags không work
**Solution:**
- Mở .xib trong Interface Builder
- Select từng UI element
- Check "Tag" field trong Attributes Inspector
- Verify tags match với constants (101-112)

### Issue 4: Countdown không hiển thị
**Solution:**
- Verify tags 110, 111, 112 được gán đúng
- Check các views không bị hidden trong .xib
- Log rootView hierarchy để debug

## 📦 Build Framework cho Unity

Sau khi test thành công:

```bash
# 1. Select scheme: AdmobNative
# 2. Select destination: Any iOS Device (arm64)
# 3. Product -> Build (Cmd + B)

# 4. Tìm framework trong:
DerivedData/.../Build/Products/Release-iphoneos/AdmobNative.framework

# 5. Copy framework này vào Unity project
```

## 📝 Notes

- **Threading**: Tất cả UI operations đã được wrap trong `DispatchQueue.main.async`
- **Memory**: Sử dụng `weak` references để tránh retain cycles
- **Timers**: NSTimer tự động cleanup khi invalidate
- **Decorator Pattern**: Hoàn toàn giống Kotlin implementation
- **Tag System**: Sử dụng integers thay vì resource IDs như Android
