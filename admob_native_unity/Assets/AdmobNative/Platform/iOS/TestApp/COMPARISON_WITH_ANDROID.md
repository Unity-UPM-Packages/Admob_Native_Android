# iOS ViewController vs Android MainActivity

## 📊 Side-by-Side Comparison

### Structure

| Android (Kotlin) | iOS (Swift) |
|------------------|-------------|
| `MainActivity.kt` | `ViewController.swift` |
| Extends `AppCompatActivity` | Extends `UIViewController` |
| Implements `NativeAdCallbacks` | Uses separate `TestCallbacks` class |
| Uses XML layout | Uses programmatic UI |

---

## 🎨 UI Setup

### Android (XML)
```kotlin
// activity_main.xml
<LinearLayout>
    <Button android:id="@+id/init_sdk_button" />
    <Button android:id="@+id/load_ad_button" />
    <Button android:id="@+id/show_ad_button" />
    <Button android:id="@+id/show_ad_banner" />
</LinearLayout>

// MainActivity.kt
val initSdkButton: Button = findViewById(R.id.init_sdk_button)
```

### iOS (Programmatic)
```swift
// ViewController.swift
private let initSdkButton: UIButton = {
    let button = UIButton(type: .system)
    button.setTitle("Initialize SDK", for: .normal)
    // ... styling
    return button
}()

// In setupUI()
view.addSubview(initSdkButton)
NSLayoutConstraint.activate([...])
```

---

## 🚀 Initialization

### Android
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    admobNativeController1 = AdmobNativeController(this, this)
    admobNativeController2 = AdmobNativeController(this, this)
}
```

### iOS
```swift
override func viewDidLoad() {
    super.viewDidLoad()
    
    setupUI()
    setupActions()
    
    // Controllers tạo sau khi load ad
}
```

---

## 📱 Button Actions

### Android
```kotlin
initSdkButton.setOnClickListener {
    Log.d(TAG, "Initialize SDK button clicked.")
    MobileAds.initialize(this) { initializationStatus ->
        // ...
        Toast.makeText(this, "AdMob SDK Initialized!", Toast.LENGTH_SHORT).show()
    }
}
```

### iOS
```swift
@objc private func initSdkButtonTapped() {
    print("📱 Initialize SDK button clicked")
    GADMobileAds.sharedInstance().start { [weak self] status in
        // ...
        self?.showAlert(title: "Success", message: "AdMob SDK Initialized!")
    }
}
```

---

## 🎯 Load Ad

### Android
```kotlin
loadAdButton.setOnClickListener {
    Log.d(TAG, "Load Ad button clicked. Requesting a new ad...")
    val adRequest = AdRequest.Builder().build()
    val adRequest2 = AdRequest.Builder().build()
    admobNativeController1.loadAd(TEST_AD_UNIT_ID, adRequest)
    admobNativeController2.loadAd(TEST_AD_UNIT_ID, adRequest2)
}
```

### iOS
```swift
@objc private func loadAdButtonTapped() {
    print("📡 Load Ad button clicked. Requesting new ads...")
    
    let callbacks1 = TestCallbacks(viewController: self, controllerName: "Controller1")
    let callbacks2 = TestCallbacks(viewController: self, controllerName: "Controller2")
    
    admobNativeController1 = AdmobNativeController(viewController: self, callbacks: callbacks1)
    admobNativeController2 = AdmobNativeController(viewController: self, callbacks: callbacks2)
    
    callbacks1.controller = admobNativeController1
    callbacks2.controller = admobNativeController2
    
    let request1 = GADRequest()
    let request2 = GADRequest()
    
    admobNativeController1?.loadAd(adUnitId: TEST_AD_UNIT_ID, request: request1)
    admobNativeController2?.loadAd(adUnitId: TEST_AD_UNIT_ID, request: request2)
}
```

---

## 📺 Show Ad with Countdown

### Android
```kotlin
showAdButton.setOnClickListener {
    if (admobNativeController1.isAdAvailable()) {
        Log.d(TAG, "Show Ad button clicked. Showing the ad...")
        admobNativeController1
            .withCountdown(5f, 5f, 2f)
            .showAd(NATIVE_LAYOUT_NAME)
    } else {
        Toast.makeText(this, "Ad not available yet. Please load first.", Toast.LENGTH_SHORT).show()
    }
}
```

### iOS
```swift
@objc private func showAdButtonTapped() {
    guard let controller = admobNativeController1 else { return }
    
    if controller.isAdAvailable() {
        print("📺 Show Ad button clicked. Showing ad with countdown...")
        controller
            .withCountdown(initial: 5, duration: 5, closeDelay: 2)
            .showAd(layoutName: NATIVE_LAYOUT_NAME)
    } else {
        showAlert(title: "Warning", message: "Ad not available yet. Please load first.")
    }
}
```

---

## 📍 Show Ad with Position

### Android
```kotlin
showbbannerButton.setOnClickListener {
    if (admobNativeController2.isAdAvailable()) {
        Log.d(TAG, "Show Ad button clicked. Showing the ad...")
        admobNativeController2
            .withPosition(20, 20)
            .showAd("native_mrec")
    } else {
        Toast.makeText(this, "Ad not available yet. Please load first.", Toast.LENGTH_SHORT).show()
    }
}
```

### iOS
```swift
@objc private func showBannerButtonTapped() {
    guard let controller = admobNativeController2 else { return }
    
    if controller.isAdAvailable() {
        print("📺 Show Banner button clicked. Showing ad with position...")
        controller
            .withPosition(x: 20, y: 20)
            .showAd(layoutName: NATIVE_MREC_LAYOUT)
    } else {
        showAlert(title: "Warning", message: "Ad not available yet. Please load first.")
    }
}
```

---

## 🎤 Callbacks Implementation

### Android (Direct Implementation)
```kotlin
class MainActivity : AppCompatActivity(), NativeAdCallbacks {
    
    override fun onAdLoaded() {
        Log.d(TAG, "CALLBACK RECEIVED: onAdLoaded")
        Toast.makeText(this, "Ad Loaded Successfully!", Toast.LENGTH_SHORT).show()
    }
    
    override fun onAdFailedToLoad(error: LoadAdError) {
        val errorMessage = "Error Code: ${error.code}, Message: ${error.message}"
        Log.e(TAG, "CALLBACK RECEIVED: onAdFailedToLoad - $errorMessage")
    }
    
    // ... all other callbacks
}
```

### iOS (Separate Class)
```swift
class TestCallbacks: NSObject, NativeAdCallbacks {
    weak var viewController: ViewController?
    weak var controller: AdmobNativeController?
    
    func onAdLoaded() {
        print("✅ CALLBACK: Ad Loaded Successfully!")
        viewController?.showAlert(title: "Success", message: "Ad Loaded Successfully!")
        viewController?.enableShowButtons()
    }
    
    func onAdFailedToLoad(error: Error) {
        print("❌ CALLBACK: Ad Failed: \(error.localizedDescription)")
        viewController?.showAlert(title: "Error", message: error.localizedDescription)
    }
    
    // ... all other callbacks
}
```

---

## 🔔 User Notifications

### Android (Toast)
```kotlin
Toast.makeText(this, "AdMob SDK Initialized!", Toast.LENGTH_SHORT).show()
```

### iOS (Alert)
```swift
showAlert(title: "Success", message: "AdMob SDK Initialized!")

private func showAlert(title: String, message: String) {
    let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
    alert.addAction(UIAlertAction(title: "OK", style: .default))
    present(alert, animated: true)
}
```

---

## 📊 Status Updates

### Android (Toast)
```kotlin
Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
```

### iOS (Status Label)
```swift
private let statusLabel: UILabel = { ... }()

private func updateStatus(_ message: String) {
    DispatchQueue.main.async { [weak self] in
        self?.statusLabel.text = message
    }
}
```

---

## 🎭 Key Differences

### 1. Callbacks Pattern

**Android:** Activity implements interface directly
```kotlin
class MainActivity : AppCompatActivity(), NativeAdCallbacks
```

**iOS:** Separate callback class
```swift
class TestCallbacks: NSObject, NativeAdCallbacks
```

**Lý do:** iOS cần weak references để tránh retain cycles.

### 2. UI Creation

**Android:** XML layout + findViewById
```kotlin
setContentView(R.layout.activity_main)
val button = findViewById<Button>(R.id.button)
```

**iOS:** Programmatic UI + Auto Layout
```swift
let button = UIButton(type: .system)
view.addSubview(button)
NSLayoutConstraint.activate([...])
```

### 3. Layout Files

**Android:** XML resource IDs
```kotlin
showAd("native_template")  // Tìm R.layout.native_template
```

**iOS:** XIB filename
```swift
showAd(layoutName: "native_template")  // Tìm native_template.xib
```

### 4. Controller Lifecycle

**Android:** Created trong onCreate
```kotlin
admobNativeController1 = AdmobNativeController(this, this)
```

**iOS:** Created khi load ad
```swift
admobNativeController1 = AdmobNativeController(viewController: self, callbacks: callbacks1)
```

### 5. Thread Safety

**Android:** runOnUiThread
```kotlin
runOnUiThread {
    // UI updates
}
```

**iOS:** DispatchQueue.main
```swift
DispatchQueue.main.async {
    // UI updates
}
```

---

## ✅ Similarities (API Parity)

Both platforms use **identical** controller API:

```kotlin/swift
// Same methods
controller.loadAd(adUnitId, request)
controller.showAd(layoutName)
controller.isAdAvailable()
controller.destroyAd()

// Same builder pattern
controller
    .withCountdown(initial, duration, closeDelay)
    .withPosition(x, y)
    .showAd(layoutName)

// Same callbacks
onAdLoaded()
onAdFailedToLoad(error)
onAdShow()
onAdClosed()
onPaidEvent(...)
// etc...
```

---

## 🎯 Testing Flow (Identical)

1. ✅ Initialize SDK
2. ✅ Load Ad (wait for success)
3. ✅ Show Ad with Countdown → See 3 phases
4. ✅ Close ad
5. ✅ Show Ad with Position → See positioned ad

Both platforms produce the same user experience! 🚀
