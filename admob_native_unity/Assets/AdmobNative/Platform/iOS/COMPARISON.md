# iOS vs Android Implementation Comparison

## 📊 Architecture Comparison

### Core Components

| Component | Android (Kotlin) | iOS (Swift) | Status |
|-----------|------------------|-------------|--------|
| **Callbacks Interface** | `NativeAdCallbacks.kt` | `NativeAdCallbacks.swift` | ✅ Identical API |
| **Show Behavior** | `IShowBehavior.kt` | `IShowBehavior.swift` | ✅ Same pattern |
| **Base Show** | `BaseShowBehavior.kt` | `BaseShowBehavior.swift` | ✅ Same logic |
| **Countdown** | `CountdownDecorator.kt` | `CountdownDecorator.swift` | ✅ 3 timers |
| **Position** | `PositionDecorator.kt` | `PositionDecorator.swift` | ✅ Same API |
| **Controller** | `AdmobNativeController.kt` | `AdmobNativeController.swift` | ✅ Same methods |
| **Bridge** | N/A (AndroidJavaProxy) | `AdmobNativeBridge.swift` | ⚠️ C interface |

## 🔧 Key Differences

### 1. Layout Loading

**Android (XML):**
```kotlin
val layoutId = activity.resources.getIdentifier(
    layoutName,
    "layout",
    activity.packageName
)
val adContentView = activity.layoutInflater.inflate(layoutId, adContainer, false)
```

**iOS (XIB):**
```swift
let views = Bundle.main.loadNibNamed(layoutName, owner: nil, options: nil)
let adContentView = views?.first as? UIView
```

### 2. View Binding

**Android (Resource IDs):**
```kotlin
val headlineView = adView.findViewById<TextView>(R.id.primary)
```

**iOS (Tags):**
```swift
let headlineLabel = adView.viewWithTag(101) as? UILabel
```

### 3. Timer Implementation

**Android (SonicCountDownTimer):**
```kotlin
object : SonicCountDownTimer(durationMillis, intervalMillis) {
    override fun onTimerTick(timeRemaining: Long) { }
    override fun onTimerFinish() { }
}
```

**iOS (NSTimer):**
```swift
Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { timer in
    // tick logic
}
```

### 4. Threading

**Android (Handler):**
```kotlin
activity.runOnUiThread {
    // UI operations
}
```

**iOS (DispatchQueue):**
```swift
DispatchQueue.main.async {
    // UI operations
}
```

### 5. Bridge Mechanism

**Android (Direct JNI):**
```kotlin
// Kotlin class implements interface
class AdmobNativeController(
    private val activity: Activity,
    private val callbacks: NativeAdCallbacks  // C# object!
)

// C# can call directly
_kotlinController = new AndroidJavaObject(
    "com.thelegends.admob_native_unity.AdmobNativeController",
    activity,
    this  // C# object passed as callback
);
```

**iOS (C Function Pointers):**
```swift
// Swift uses protocol
protocol NativeAdCallbacks { }

// Bridge wrapper converts to C function pointers
class BridgeCallbacks: NativeAdCallbacks {
    var onAdLoadedCallback: VoidCallback?
    
    func onAdLoaded() {
        onAdLoadedCallback?()  // Call C function pointer
    }
}

// C# uses DllImport
[DllImport("__Internal")]
private static extern void AdmobNative_LoadAd(IntPtr handle, string adUnitId);
```

## 🎨 Decorator Pattern Flow

Both platforms use identical decorator assembly:

```
Android/iOS Flow:
showAd(layoutName) {
    var behavior = BaseShowBehavior()
    
    if (positionConfig != null) {
        behavior = PositionDecorator(behavior, x, y)
    }
    
    if (countdownConfig != null) {
        behavior = CountdownDecorator(behavior, initial, duration, closeDelay)
    }
    
    behavior.show(...)
}
```

## ⏱️ Timer Phases (Identical)

### Phase 1: Initial Delay
- **Purpose**: Silent delay before showing countdown
- **Duration**: `initialDelaySeconds`
- **UI State**: All hidden

### Phase 2: Countdown
- **Purpose**: Show countdown to user
- **Duration**: `countdownDurationSeconds`
- **UI State**: Progress bar + countdown text visible
- **Updates**: Every 1 second

### Phase 3: Close Button Delay
- **Purpose**: Delay before button becomes clickable
- **Duration**: `closeButtonDelaySeconds`
- **UI State**: Close button visible but disabled

## 📱 UI Tag Mapping

| Tag | Element | Android ID | iOS Tag | View Type |
|-----|---------|------------|---------|-----------|
| 101 | Headline | `primary` | 101 | UILabel / TextView |
| 102 | Body | `body` | 102 | UILabel / TextView |
| 103 | Media | `media_view` | 103 | GADMediaView |
| 104 | Icon | `icon` | 104 | UIImageView / ImageView |
| 105 | CTA | `cta` | 105 | UIButton / Button |
| 106 | Rating | `rating_bar` | 106 | UIImageView / RatingBar |
| 107 | Advertiser | `secondary` | 107 | UILabel / TextView |
| 108 | Store | `ad_store` | 108 | UILabel / TextView |
| 109 | Price | `ad_price` | 109 | UILabel / TextView |
| 110 | Close | `ad_close_button` | 110 | UIImageView / ImageView |
| 111 | Countdown | `ad_countdown_text` | 111 | UILabel / TextView |
| 112 | Progress | `ad_progress_bar` | 112 | UIProgressView / ProgressBar |

## 🔄 API Consistency

### Load Ad
```csharp
// Unity C# - Same for both platforms
_client.LoadAd(adUnitId, request);
```

```kotlin
// Android - Kotlin receives call
fun loadAd(adUnitId: String, adRequest: AdRequest)
```

```swift
// iOS - Swift receives call via bridge
func loadAd(adUnitId: String, request: GADRequest)
```

### Show Ad
```csharp
// Unity C# - Same for both platforms
_client.ShowAd(layoutName);
```

### Builder Pattern
```csharp
// Unity C# - Same for both platforms
_client.WithCountdown(2f, 5f, 1f);
_client.WithPosition(0, 100);
```

Both Android and iOS controllers have identical methods!

## ✅ Testing Checklist

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| Load Ad | ✅ | 🔄 | Test with test ad unit |
| Show Ad | ✅ | 🔄 | Verify layout loads |
| Countdown Phase 1 | ✅ | 🔄 | Initial delay works |
| Countdown Phase 2 | ✅ | 🔄 | Text + progress updates |
| Countdown Phase 3 | ✅ | 🔄 | Close button clickable |
| Position Decorator | ✅ | 🔄 | Custom X/Y positioning |
| Video Callbacks | ✅ | 🔄 | Play, pause, mute, end |
| Paid Event | ✅ | 🔄 | Revenue tracking |
| Click Tracking | ✅ | 🔄 | Click events |
| Impression | ✅ | 🔄 | Impression tracking |
| Destroy Ad | ✅ | 🔄 | Cleanup properly |

## 🎯 Unity Integration (Next Steps)

After iOS native testing is complete:

1. ✅ Create `AdmobNativePlatformIOSClient.cs`
2. ✅ Add `[DllImport]` declarations
3. ✅ Implement callback marshalling with `[MonoPInvokeCallback]`
4. ✅ Update `AdmobNativePlatform.cs` factory
5. ✅ Build framework and add to Unity Plugins/iOS/
6. ✅ Test in Unity Editor (uses DummyClient)
7. ✅ Build for iOS device and test end-to-end

## 🏆 Goals Achieved

✅ **API Parity**: iOS API matches Android exactly from Unity's perspective
✅ **Decorator Pattern**: Same architecture on both platforms
✅ **Timer Logic**: Identical 3-phase countdown behavior
✅ **Builder Pattern**: Fluent API preserved on both sides
✅ **Event System**: Same callback signatures
✅ **Tag System**: Unified view binding approach

## 📝 Key Takeaways

1. **Platform differences are hidden**: Unity C# code doesn't care about Android vs iOS
2. **Same developer experience**: Load, configure, show - identical on both
3. **Consistent callbacks**: All events fire with same signatures
4. **Decorator flexibility**: Can mix and match decorators the same way
5. **Easy maintenance**: Bug fixes apply to both platforms similarly
