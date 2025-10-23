# Setup Xcode Project - Step by Step Guide

## 📋 Overview

Hướng dẫn này giúp bạn tạo Xcode project để test Swift implementation trước khi tích hợp vào Unity.

## 🚀 Quick Start

### 1. Tạo Project mới trong Xcode

```
1. Mở Xcode
2. File -> New -> Project
3. Chọn "iOS" tab
4. Chọn template: "App"
5. Click "Next"
```

**Project Settings:**
- Product Name: `AdmobNativeTestApp`
- Team: Chọn team của bạn (hoặc Personal Team)
- Organization Identifier: `com.thelegends.ads` (hoặc custom)
- Bundle Identifier: `com.thelegends.ads.AdmobNativeTestApp`
- Interface: **Storyboard** (hoặc SwiftUI, không quan trọng vì ta dùng code)
- Language: **Swift**
- ✅ Include Tests (optional)

Click "Next" và chọn folder để save project.

### 2. Xóa files không cần thiết

Trong project navigator, xóa các files sau (nếu có):
- ❌ `SceneDelegate.swift` (nếu dùng iOS 13+)
- ❌ `Main.storyboard` (ta sẽ dùng code)
- ❌ `ViewController.swift` (có sẵn, ta sẽ thay thế)

**Important:** Sau khi xóa `Main.storyboard`, vào project settings:
```
Target -> General -> Deployment Info
-> Main Interface: (để trống, xóa "Main")
```

Và xóa key trong `Info.plist`:
- Xóa `UISceneConfigurations` (nếu có)
- Xóa `UIMainStoryboardFile` = "Main"

### 3. Thêm Framework Target

```
File -> New -> Target
-> iOS tab
-> Framework
-> Product Name: "AdmobNative"
-> Language: Swift
-> Click "Finish"
```

### 4. Copy Files vào Project

#### A. Copy Swift Implementation Files

Từ folder `iOS/`, copy tất cả files vào project:

```
Source Files (add to Framework target):
├── NativeAdCallbacks.swift
├── IShowBehavior.swift
├── BaseShowBehavior.swift
├── CountdownDecorator.swift
├── PositionDecorator.swift
├── AdmobNativeController.swift
├── AdmobNativeBridge.swift
└── AdmobNativeBridge.h (IMPORTANT: Set as Public header)
```

**Cách add files:**
1. Right-click trên folder `AdmobNative` trong project navigator
2. "Add Files to AdmobNative..."
3. Select tất cả files Swift
4. ✅ Check "Copy items if needed"
5. ✅ Check target "AdmobNative"
6. Click "Add"

**Quan trọng cho Header file:**
1. Select `AdmobNativeBridge.h`
2. File Inspector (right panel) -> Target Membership
3. Trong row "AdmobNative", change từ "Project" → **"Public"**

#### B. Copy Test App Files

Từ folder `iOS/TestApp/`, copy vào App target:

```
Test Files (add to App target):
├── ViewController.swift (replace existing if any)
├── AppDelegate.swift (replace existing)
└── Info.plist (merge với existing)
```

**Cách merge Info.plist:**
- Mở `Info.plist` có sẵn
- Thêm key `GADApplicationIdentifier` với value test ID
- Thêm `SKAdNetworkItems` array (copy từ template)

### 5. Copy Podfile và Install Pods

```bash
# 1. Copy Podfile vào thư mục gốc của project
cp Podfile /path/to/AdmobNativeTestApp/

# 2. Mở Terminal, cd vào thư mục project
cd /path/to/AdmobNativeTestApp/

# 3. Install CocoaPods (nếu chưa có)
sudo gem install cocoapods

# 4. Install pods
pod install

# 5. Đợi... (có thể mất vài phút)

# 6. QUAN TRỌNG: Từ giờ chỉ mở file .xcworkspace
open AdmobNativeTestApp.xcworkspace
```

### 6. Configure Build Settings

#### A. Framework Target (AdmobNative)

```
Select project -> Target "AdmobNative" -> Build Settings
```

**Required Settings:**
- **Defines Module**: YES
- **Module Name**: AdmobNative
- **Install Path**: @rpath
- **Skip Install**: NO
- **Mach-O Type**: Dynamic Library
- **Swift Language Version**: Swift 5

**Headers:**
```
Build Phases -> Headers
-> Drag "AdmobNativeBridge.h" to "Public" section
```

#### B. App Target (AdmobNativeTestApp)

```
Select project -> Target "AdmobNativeTestApp" -> General
```

**Frameworks, Libraries, and Embedded Content:**
- Click "+" button
- Add "AdmobNative.framework" (from workspace)
- Set "Embed & Sign"

**Build Settings:**
- **Always Embed Swift Standard Libraries**: YES

### 7. Tạo Layout Files (.xib)

#### A. Tạo native_template.xib

```
File -> New -> File
-> View
-> Name: "native_template"
-> Target: AdmobNativeTestApp (App target)
-> Click "Create"
```

**Setup trong Interface Builder:**

1. **Set File's Owner:**
   - Click "File's Owner" in Document Outline
   - Identity Inspector -> Class: `GADNativeAdView`
   - Module: `GoogleMobileAds`

2. **Set Root View Class:**
   - Click root "View" 
   - Identity Inspector -> Class: `GADNativeAdView`
   - Module: `GoogleMobileAds`

3. **Add UI Elements và gán Tags:**

Drag các elements từ Object Library:

| Element | Class | Tag | Purpose |
|---------|-------|-----|---------|
| View | UIView | - | Background container |
| Label | UILabel | 101 | Headline |
| Label | UILabel | 102 | Body |
| View | GADMediaView | 103 | Media (video/image) |
| Image View | UIImageView | 104 | App Icon |
| Button | UIButton | 105 | Call to Action |
| Image View | UIImageView | 106 | Star Rating |
| Label | UILabel | 107 | Advertiser |
| Label | UILabel | 108 | Store |
| Label | UILabel | 109 | Price |
| Image View | UIImageView | 110 | Close Button |
| Label | UILabel | 111 | Countdown Text |
| Progress View | UIProgressView | 112 | Progress Bar |

**Gán Tag:**
- Select element
- Attributes Inspector
- View section -> Tag: (nhập số theo bảng)

**Setup Constraints:**
- Sử dụng Auto Layout để layout elements
- Đảm bảo GADMediaView có proper size (ví dụ: 300x250)

#### B. Tạo native_mrec.xib

Tương tự như trên, nhưng với layout khác (MREC size 300x250).

### 8. Build và Run

#### Build Framework First:

```
1. Select scheme: AdmobNative
2. Select destination: Any iOS Device (arm64)
3. Product -> Build (⌘B)
4. Check for errors
```

#### Run App:

```
1. Select scheme: AdmobNativeTestApp
2. Select destination: iPhone Simulator (hoặc device)
3. Product -> Run (⌘R)
```

## 📱 Testing Flow

Khi app chạy, bạn sẽ thấy 4 buttons:

### Button 1: Initialize SDK
- Click để initialize AdMob SDK
- Đợi alert "AdMob SDK Initialized!"
- Button "Load Ad" sẽ được enable

### Button 2: Load Ad
- Click để load 2 native ads
- Đợi alert "Ad Loaded Successfully!"
- Buttons "Show Ad" sẽ được enable

### Button 3: Show Ad (Countdown)
- Click để show ad với countdown decorator
- **Expected behavior:**
  ```
  Phase 1: Wait 5s (silent)
  Phase 2: Countdown 5->1 with progress bar
  Phase 3: Close button appears, wait 2s
  Phase 4: Close button clickable
  ```

### Button 4: Show Ad (Banner)
- Click để show ad với position decorator
- Ad xuất hiện tại vị trí (20, 20)

## ✅ Expected Console Output

```
✅ App launched successfully
✅ ViewController loaded and ready
📱 Initialize SDK button clicked
✅ AdMob SDK initialization complete
Adapter: com.google.ads.mediation.admob.AdMobAdapter
  - Description: Ready
  - Latency: 0.123
📡 Load Ad button clicked. Requesting new ads...
📡 AdmobNativeController: Loading ad for unit ID: ca-app-pub-...
✅ Ad loaded successfully
✅ CALLBACK: [Controller1] Ad Loaded Successfully! ✅
✅ CALLBACK: [Controller2] Ad Loaded Successfully! ✅
📺 Show Ad button clicked. Showing ad with countdown...
📺 AdmobNativeController: Showing ad with layout: native_template
⏱️ Applied CountdownDecorator: initial=5.0s, duration=5.0s, closeDelay=2.0s
✅ BaseShowBehavior: Ad view populated successfully
✅ BaseShowBehavior: Ad view displayed successfully
📺 CALLBACK: [Controller1] onAdShow
⏱️ CountdownDecorator: Starting Phase 1 - Initial delay (5.0s)
✅ Phase 1 complete - Starting countdown
⏱️ CountdownDecorator: Starting Phase 2 - Countdown (5.0s)
✅ Phase 2 complete - Starting close button delay
⏱️ CountdownDecorator: Starting Phase 3 - Close button delay (2.0s)
✅ Phase 3 complete - Close button now clickable
👆 Close button tapped - Destroying ad
🚪 CALLBACK: [Controller1] onAdClosed
🗑️ AdmobNativeController: Destroying ad
✅ BaseShowBehavior: Ad view destroyed
```

## 🐛 Troubleshooting

### Issue: "Module 'GoogleMobileAds' not found"
**Solution:**
```bash
pod install
# Make sure to open .xcworkspace, not .xcodeproj
```

### Issue: "AdmobNativeBridge.h not found"
**Solution:**
- Check header is in "Public" section (Build Phases -> Headers)
- Clean build folder: Product -> Clean Build Folder (⇧⌘K)

### Issue: Ad không load
**Solution:**
- Check Info.plist có `GADApplicationIdentifier`
- Check network connection
- Check test ad unit ID đúng
- Check console logs

### Issue: Layout không hiển thị
**Solution:**
- Verify .xib file trong App target
- Check Class của root view là `GADNativeAdView`
- Check các tags được gán đúng

### Issue: Countdown không chạy
**Solution:**
- Check tags 110, 111, 112 có trong .xib
- Verify UI elements không bị hidden
- Check console logs cho Phase transitions

## 📦 Next Steps

Sau khi test thành công:
1. ✅ Build framework (scheme: AdmobNative, device: Any iOS Device)
2. ✅ Copy .framework từ DerivedData
3. ✅ Tích hợp vào Unity project
4. ✅ Implement C# client
5. ✅ Test trong Unity

## 📝 Important Notes

- **Always open .xcworkspace** sau khi `pod install`
- **Test on real device** để verify hoàn toàn (simulator có thể khác)
- **Check console logs** để debug issues
- **Use test ad unit IDs** khi development (real IDs có thể bị ban)

---

Good luck! 🚀
