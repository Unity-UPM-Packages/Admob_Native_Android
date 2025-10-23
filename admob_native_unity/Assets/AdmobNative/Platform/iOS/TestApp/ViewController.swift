//
//  ViewController.swift
//  AdmobNativeTestApp
//
//  Test implementation tương tự MainActivity.kt
//

import UIKit
import GoogleMobileAds

class ViewController: UIViewController {
    
    // MARK: - Properties
    
    private var admobNativeController: AdmobNativeController?
    
    // Keep strong reference to callbacks to prevent deallocation
    private var callbacks: TestCallbacks?
    
    // Test Ad Unit ID của Google cho Native Ads
    private let TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/2521693316"
    
    // Tên layout files (.xib)
    private let NATIVE_LAYOUT_NAME = "native_template"
    
    // MARK: - UI Elements
    
    private let initSdkButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Initialize SDK", for: .normal)
        button.backgroundColor = .systemBlue
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .semibold)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private let loadAdButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Load Ad", for: .normal)
        button.backgroundColor = .systemGreen
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .semibold)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        button.isEnabled = false
        button.alpha = 0.5
        return button
    }()
    
    private let showAdButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Show Ad (Countdown)", for: .normal)
        button.backgroundColor = .systemOrange
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .semibold)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        button.isEnabled = false
        button.alpha = 0.5
        return button
    }()
    
    private let statusLabel: UILabel = {
        let label = UILabel()
        label.text = "Ready to test"
        label.textAlignment = .center
        label.numberOfLines = 0
        label.font = .systemFont(ofSize: 14)
        label.textColor = .secondaryLabel
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    // MARK: - Lifecycle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.backgroundColor = .systemBackground
        title = "AdMob Native Test"
        
        setupUI()
        setupActions()
        
        print("✅ ViewController loaded and ready")
    }
    
    // MARK: - UI Setup
    
    private func setupUI() {
        // Add all buttons to view
        view.addSubview(initSdkButton)
        view.addSubview(loadAdButton)
        view.addSubview(showAdButton)
        view.addSubview(statusLabel)
        
        // Layout constraints
        NSLayoutConstraint.activate([
            // Status Label - Top
            statusLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            statusLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            statusLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            
            // Init SDK Button
            initSdkButton.topAnchor.constraint(equalTo: statusLabel.bottomAnchor, constant: 40),
            initSdkButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            initSdkButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            initSdkButton.heightAnchor.constraint(equalToConstant: 50),
            
            // Load Ad Button
            loadAdButton.topAnchor.constraint(equalTo: initSdkButton.bottomAnchor, constant: 20),
            loadAdButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            loadAdButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            loadAdButton.heightAnchor.constraint(equalToConstant: 50),
            
            // Show Ad Button (Countdown)
            showAdButton.topAnchor.constraint(equalTo: loadAdButton.bottomAnchor, constant: 20),
            showAdButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            showAdButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            showAdButton.heightAnchor.constraint(equalToConstant: 50),
        ])
    }
    
    // MARK: - Actions Setup
    
    private func setupActions() {
        initSdkButton.addTarget(self, action: #selector(initSdkButtonTapped), for: .touchUpInside)
        loadAdButton.addTarget(self, action: #selector(loadAdButtonTapped), for: .touchUpInside)
        showAdButton.addTarget(self, action: #selector(showAdButtonTapped), for: .touchUpInside)
    }
    
    // MARK: - Button Actions
    
    @objc private func initSdkButtonTapped() {
        print("📱 Initialize SDK button clicked")
        updateStatus("Initializing AdMob SDK...")
        
        // Initialize AdMob SDK
        GADMobileAds.sharedInstance().start { [weak self] status in
            guard let self = self else { return }
            
            print("✅ AdMob SDK initialization complete")
            
            // Log adapter statuses
            for adapter in status.adapterStatusesByClassName {
                let adapterStatus = adapter.value
                print("Adapter: \(adapter.key)")
                print("  - Description: \(adapterStatus.description)")
                print("  - Latency: \(adapterStatus.latency)")
            }
            
            DispatchQueue.main.async {
                self.updateStatus("AdMob SDK Initialized ✅")
                self.showAlert(title: "Success", message: "AdMob SDK Initialized!")
                
                // Enable Load button
                self.loadAdButton.isEnabled = true
                self.loadAdButton.alpha = 1.0
                
                // Disable Init button
                self.initSdkButton.isEnabled = false
                self.initSdkButton.alpha = 0.5
            }
        }
    }
    
    @objc private func loadAdButtonTapped() {
        print("📡 Load Ad button clicked. Requesting ad...")
        updateStatus("Loading ad...")
        
        // Create callbacks and keep strong reference
        callbacks = TestCallbacks(viewController: self, controllerName: "NativeAd")
        
        // Create controller
        admobNativeController = AdmobNativeController(viewController: self, callbacks: callbacks!)
        
        // Keep reference to controller in callbacks
        callbacks?.controller = admobNativeController
        
        // Load ad
        let request = GADRequest()
        admobNativeController?.loadAd(adUnitId: TEST_AD_UNIT_ID, request: request)
    }
    
    @objc private func showAdButtonTapped() {
        guard let controller = admobNativeController else {
            showAlert(title: "Error", message: "Controller not initialized")
            return
        }
        
        if controller.isAdAvailable() {
            print("📺 Show Ad button clicked. Showing ad with countdown...")
            updateStatus("Showing ad with countdown...")
            
            controller
                .withCountdown(initial: 5, duration: 5, closeDelay: 2)
                .showAd(layoutName: NATIVE_LAYOUT_NAME)
        } else {
            showAlert(title: "Warning", message: "Ad not available yet. Please load first.")
            print("⚠️ Show Ad button clicked, but ad is not available")
        }
    }
    
    // MARK: - Helpers
    
    func updateStatus(_ message: String) {
        DispatchQueue.main.async { [weak self] in
            self?.statusLabel.text = message
            print("📊 Status: \(message)")
        }
    }
    
    func showAlert(title: String, message: String) {
        DispatchQueue.main.async { [weak self] in
            let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default))
            self?.present(alert, animated: true)
        }
    }
    
    func enableShowButton() {
        DispatchQueue.main.async { [weak self] in
            self?.showAdButton.isEnabled = true
            self?.showAdButton.alpha = 1.0
        }
    }
}

// MARK: - Test Callbacks

class TestCallbacks: NSObject, NativeAdCallbacks {
    
    weak var viewController: ViewController?
    weak var controller: AdmobNativeController?
    private let controllerName: String
    
    init(viewController: ViewController, controllerName: String) {
        self.viewController = viewController
        self.controllerName = controllerName
        super.init()
    }
    
    // MARK: - Load & Show
    
    func onAdLoaded() {
        let message = "[\(controllerName)] Ad Loaded Successfully! ✅"
        print("✅ CALLBACK: \(message)")
        viewController?.updateStatus(message)
        viewController?.showAlert(title: "Success", message: message)
        viewController?.enableShowButton()
    }
    
    func onAdFailedToLoad(error: Error) {
        let message = "[\(controllerName)] Ad Failed: \(error.localizedDescription)"
        print("❌ CALLBACK: \(message)")
        viewController?.updateStatus(message)
        viewController?.showAlert(title: "Error", message: message)
    }
    
    func onAdShow() {
        print("📺 CALLBACK: [\(controllerName)] onAdShow")
    }
    
    func onAdClosed() {
        print("🚪 CALLBACK: [\(controllerName)] onAdClosed")
        viewController?.updateStatus("Ad closed")
    }
    
    // MARK: - Revenue
    
    func onPaidEvent(precisionType: Int, valueMicros: Int64, currencyCode: String) {
        let value = Double(valueMicros) / 1000000.0
        let message = "💰 CALLBACK: [\(controllerName)] Paid Event - \(value) \(currencyCode) (precision: \(precisionType))"
        print(message)
    }
    
    func onAdDidRecordImpression() {
        print("👁️ CALLBACK: [\(controllerName)] onAdDidRecordImpression")
    }
    
    func onAdClicked() {
        print("👆 CALLBACK: [\(controllerName)] onAdClicked")
    }
    
    // MARK: - Video
    
    func onVideoStart() {
        print("▶️ CALLBACK: [\(controllerName)] onVideoStart")
    }
    
    func onVideoEnd() {
        print("⏹️ CALLBACK: [\(controllerName)] onVideoEnd")
    }
    
    func onVideoMute(isMuted: Bool) {
        print("🔇 CALLBACK: [\(controllerName)] onVideoMute - isMuted: \(isMuted)")
    }
    
    func onVideoPlay() {
        print("▶️ CALLBACK: [\(controllerName)] onVideoPlay")
    }
    
    func onVideoPause() {
        print("⏸️ CALLBACK: [\(controllerName)] onVideoPause")
    }
    
    // MARK: - Full Screen
    
    func onAdShowedFullScreenContent() {
        print("📱 CALLBACK: [\(controllerName)] onAdShowedFullScreenContent")
    }
    
    func onAdDismissedFullScreenContent() {
        print("📱 CALLBACK: [\(controllerName)] onAdDismissedFullScreenContent")
    }
}
