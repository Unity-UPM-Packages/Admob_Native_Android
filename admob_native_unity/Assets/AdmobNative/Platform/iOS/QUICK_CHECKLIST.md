# Quick Setup Checklist

## ☑️ Pre-requisites
- [ ] macOS computer
- [ ] Xcode 14+ installed
- [ ] CocoaPods installed (`sudo gem install cocoapods`)

## 📝 Setup Steps

### 1. Create Xcode Project
- [ ] Open Xcode
- [ ] New Project -> iOS -> App
- [ ] Name: `AdmobNativeTestApp`
- [ ] Language: Swift
- [ ] Interface: Storyboard

### 2. Cleanup
- [ ] Delete `SceneDelegate.swift` (if exists)
- [ ] Delete `Main.storyboard`
- [ ] Delete existing `ViewController.swift`
- [ ] Remove "Main" from Project Settings -> Main Interface
- [ ] Remove UISceneConfigurations from Info.plist

### 3. Add Framework Target
- [ ] File -> New -> Target
- [ ] Framework
- [ ] Name: `AdmobNative`

### 4. Add Swift Files to Framework
- [ ] Copy all `.swift` files to project
- [ ] Check target: `AdmobNative`
- [ ] Add `AdmobNativeBridge.h` as **Public** header

Files to add:
- [ ] `NativeAdCallbacks.swift`
- [ ] `IShowBehavior.swift`
- [ ] `BaseShowBehavior.swift`
- [ ] `CountdownDecorator.swift`
- [ ] `PositionDecorator.swift`
- [ ] `AdmobNativeController.swift`
- [ ] `AdmobNativeBridge.swift`
- [ ] `AdmobNativeBridge.h` ⚠️ Set as Public

### 5. Add Test Files to App Target
- [ ] Copy `ViewController.swift` (replace)
- [ ] Copy `AppDelegate.swift` (replace)
- [ ] Merge `Info.plist` (add GADApplicationIdentifier)

### 6. Install CocoaPods
```bash
cd /path/to/AdmobNativeTestApp/
pod install
```
- [ ] Copy `Podfile` to project root
- [ ] Run `pod install`
- [ ] **Open `.xcworkspace`** (NOT .xcodeproj)

### 7. Configure Framework Target
- [ ] Build Settings -> Defines Module: YES
- [ ] Build Settings -> Module Name: AdmobNative
- [ ] Build Phases -> Headers -> Move .h to Public

### 8. Link Framework to App
- [ ] App Target -> General -> Frameworks
- [ ] Add `AdmobNative.framework`
- [ ] Set to "Embed & Sign"

### 9. Create Layout Files
- [ ] New File -> View -> `native_template.xib`
- [ ] File's Owner Class: `GADNativeAdView`
- [ ] Root View Class: `GADNativeAdView`
- [ ] Add UI elements and assign Tags (101-112)

Optional:
- [ ] Create `native_mrec.xib` (same process)

### 10. Build & Test
- [ ] Select scheme: `AdmobNativeTestApp`
- [ ] Select device: iPhone Simulator
- [ ] Product -> Run (⌘R)

## 🧪 Test Sequence
1. [ ] Click "Initialize SDK" -> See "AdMob SDK Initialized!"
2. [ ] Click "Load Ad" -> See "Ad Loaded Successfully!"
3. [ ] Click "Show Ad (Countdown)" -> See countdown 5->1
4. [ ] Wait for close button -> Click to close
5. [ ] Click "Show Ad (Banner)" -> See positioned ad

## ✅ Success Criteria
- [ ] App launches without crash
- [ ] All 4 buttons visible
- [ ] SDK initializes successfully
- [ ] Ads load successfully
- [ ] Countdown decorator works (3 phases)
- [ ] Position decorator works
- [ ] Close button clickable after countdown
- [ ] Console logs show expected flow
- [ ] No memory leaks

## 🐛 If Something Goes Wrong
- Clean build: ⇧⌘K
- Delete DerivedData
- `pod deintegrate && pod install`
- Restart Xcode
- Check XCODE_SETUP.md for detailed troubleshooting

## 📱 Recommended Test Ad Unit ID
```
ca-app-pub-3940256099942544/3986624511
```

## 🎯 Expected First Run
```
✅ App launched
📱 Initialize SDK clicked
✅ AdMob SDK Initialized!
📡 Load Ad clicked
✅ Ad Loaded Successfully!
📺 Show Ad clicked
⏱️ Countdown: 5 -> 4 -> 3 -> 2 -> 1
✅ Close button clickable
👆 Closed
```

---

**Time Estimate:** 30-45 minutes
**Next Step:** Unity Integration (after iOS test passes)
