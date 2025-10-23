import Foundation
import UIKit
import GoogleMobileAds

/// Decorator thêm chức năng countdown timer vào BaseShowBehavior.
/// Tương đương với CountdownDecorator.kt
@objc public class CountdownDecorator: BaseShowBehavior {
    
    private let wrappedBehavior: BaseShowBehavior
    private let initialDelaySeconds: TimeInterval
    private let countdownDurationSeconds: TimeInterval
    private let closeButtonDelaySeconds: TimeInterval
    
    // Timers
    private var initialDelayTimer: Timer?
    private var countdownTimer: Timer?
    private var closeButtonDelayTimer: Timer?
    private weak var progressContainer: UIView?
    private let progressLayer = CAShapeLayer()
    private let backgroundLayer = CAShapeLayer()
    private var countdownStartTime: Date?
    
    // Timer state
    private var countdownRemainingSeconds: Int = 0
    
    // UI References
    private weak var closeButton: UIImageView?
    private weak var countdownLabel: UILabel?
    private weak var callbacks: NativeAdCallbacks?
    
    // Tag constants for countdown UI (theo bảng quy ước)
    private let TAG_CLOSE_BUTTON = 110
    private let TAG_COUNTDOWN_TEXT = 111
    private let TAG_PROGRESS_BAR = 112
    
    // MARK: - Initialization
    
    public init(wrappedBehavior: BaseShowBehavior,
         initialDelay: Float,
         countdownDuration: Float,
         closeButtonDelay: Float) {
        self.wrappedBehavior = wrappedBehavior
        self.initialDelaySeconds = TimeInterval(initialDelay)
        self.countdownDurationSeconds = TimeInterval(countdownDuration)
        self.closeButtonDelaySeconds = TimeInterval(closeButtonDelay)
        super.init()
    }
    
    // MARK: - IShowBehavior Override
    
    public override func show(viewController: UIViewController,
                       nativeAd: GADNativeAd,
                       layoutName: String,
                       callbacks: NativeAdCallbacks) {
        self.callbacks = callbacks
        
        // Gọi wrapped behavior để hiển thị ad
        wrappedBehavior.show(viewController: viewController,
                            nativeAd: nativeAd,
                            layoutName: layoutName,
                            callbacks: callbacks)
        
        // Đợi một chút để view được add vào hierarchy
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            guard let self = self,
                  let rootView = self.wrappedBehavior.getRootView() else { return }
            self.startCloseLogic(rootView: rootView, callbacks: callbacks)
        }
    }
    
    public override func destroy() {
        // Cancel tất cả timers
        cancelAllTimers()

        progressLayer.removeFromSuperlayer()
        backgroundLayer.removeFromSuperlayer()
        
        // Destroy wrapped behavior
        wrappedBehavior.destroy()
    }
    
    // MARK: - Close Logic (3 Phases)
    
    private func startCloseLogic(rootView: UIView, callbacks: NativeAdCallbacks) {
        // Tìm các UI elements theo tag
        closeButton = rootView.viewWithTag(TAG_CLOSE_BUTTON) as? UIImageView
        progressContainer = rootView.viewWithTag(TAG_PROGRESS_BAR) as? UIView
        countdownLabel = rootView.viewWithTag(TAG_COUNTDOWN_TEXT) as? UILabel

        progressLayer.removeFromSuperlayer()
        backgroundLayer.removeFromSuperlayer()
        
        // Cancel any existing timers
        cancelAllTimers()
        
        // PHASE 1: Initial state - Hide everything
        closeButton?.isHidden = true
        countdownLabel?.isHidden = true
        closeButton?.isUserInteractionEnabled = false
        
        // Setup close button tap gesture
        if let closeButton = closeButton {
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(onCloseButtonTapped))
            closeButton.addGestureRecognizer(tapGesture)
        }
        
        // TIMER 1: Initial delay before showing countdown
        print("⏱️ CountdownDecorator: Starting Phase 1 - Initial delay (\(initialDelaySeconds)s)")
        startInitialDelayTimer()
    }
    
    // MARK: - Phase 1: Initial Delay Timer
    
    private func startInitialDelayTimer() {
        let interval = TimeInterval(initialDelaySeconds)
        
        initialDelayTimer = Timer.scheduledTimer(withTimeInterval: interval, repeats: false) { [weak self] _ in
            guard let self = self else { return }
            print("✅ Phase 1 complete - Starting countdown")
            self.startMainCountdown()
        }
    }

    private func updateCountdownUI(elapsedTime: TimeInterval) {
        let timeRemaining = self.countdownDurationSeconds - elapsedTime
        
        // Cập nhật số (chỉ khi giây thay đổi để tiết kiệm hiệu năng)
        let secondsRemaining = Int(ceil(timeRemaining))
        if countdownLabel?.text != "\(secondsRemaining)" {
            countdownLabel?.text = "\(secondsRemaining)"
        }
        
        // Cập nhật vòng tròn (luôn luôn để có animation mượt)
        let progress = timeRemaining / self.countdownDurationSeconds
        progressLayer.strokeEnd = CGFloat(progress)
    }

    private func setupProgressLayers() {
        guard let container = progressContainer else { return }

        let center = CGPoint(x: container.bounds.midX, y: container.bounds.midY)
        let radius = min(container.bounds.width, container.bounds.height) / 2 - 2 // -2 để có padding
        let startAngle = -CGFloat.pi / 2 // Bắt đầu từ đỉnh
        let endAngle = startAngle + (2 * .pi) // Kết thúc một vòng tròn đầy đủ

        // Vẽ vòng tròn nền (màu xám nhạt)
        let backgroundPath = UIBezierPath(arcCenter: center, radius: radius, startAngle: startAngle, endAngle: endAngle, clockwise: true)
        backgroundLayer.path = backgroundPath.cgPath
        backgroundLayer.strokeColor = UIColor.lightGray.withAlphaComponent(0.5).cgColor
        backgroundLayer.fillColor = UIColor.clear.cgColor
        backgroundLayer.lineWidth = 4.0
        container.layer.addSublayer(backgroundLayer)

        // Vẽ vòng tròn tiến trình (màu trắng)
        let progressPath = UIBezierPath(arcCenter: center, radius: radius, startAngle: startAngle, endAngle: endAngle, clockwise: true)
        progressLayer.path = progressPath.cgPath
        progressLayer.strokeColor = UIColor.white.cgColor
        progressLayer.fillColor = UIColor.clear.cgColor
        progressLayer.lineWidth = 4.0
        progressLayer.strokeEnd = 1.0 // Ban đầu đầy 100%
        container.layer.addSublayer(progressLayer)
    }
    
    // MARK: - Phase 2: Main Countdown Timer
    
    private func startMainCountdown() {
        // Show progress bar and countdown text
        progressContainer?.isHidden = false
        countdownLabel?.isHidden = false
        closeButton?.isHidden = true
        
        // Initialize countdown
        countdownRemainingSeconds = Int(countdownDurationSeconds)

        setupProgressLayers()

        countdownStartTime = Date()
        
        print("⏱️ CountdownDecorator: Starting Phase 2 - Countdown (\(countdownDurationSeconds)s)")
        
        // Timer fires every 1 second
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] timer in
            guard let self = self else {
                timer.invalidate()
                return
            }

            let elapsedTime = Date().timeIntervalSince(self.countdownStartTime ?? Date())

            if elapsedTime >= self.countdownDurationSeconds {
                print("✅ Phase 2 complete - Starting close button delay")
                progressContainer?.isHidden = true
                timer.invalidate()
                self.startCloseButtonDelay()
                return
            }
        
            // Update UI
            self.updateCountdownUI(elapsedTime: elapsedTime)
        }
    }
    
    // MARK: - Phase 3: Close Button Delay Timer
    
    private func startCloseButtonDelay() {
        // Hide progress and countdown, show close button
        countdownLabel?.isHidden = true
        closeButton?.isHidden = false
        closeButton?.isUserInteractionEnabled = false // Chưa cho click
        
        print("⏱️ CountdownDecorator: Starting Phase 3 - Close button delay (\(closeButtonDelaySeconds)s)")
        
        let interval = TimeInterval(closeButtonDelaySeconds)
        
        closeButtonDelayTimer = Timer.scheduledTimer(withTimeInterval: interval, repeats: false) { [weak self] _ in
            guard let self = self else { return }
            print("✅ Phase 3 complete - Close button now clickable")
            self.closeButton?.isUserInteractionEnabled = true
        }
    }
    
    // MARK: - Timer Management
    
    private func cancelAllTimers() {
        initialDelayTimer?.invalidate()
        initialDelayTimer = nil
        
        countdownTimer?.invalidate()
        countdownTimer = nil
        
        closeButtonDelayTimer?.invalidate()
        closeButtonDelayTimer = nil
    }
    
    // MARK: - Actions
    
    @objc private func onCloseButtonTapped() {
        guard closeButton?.isUserInteractionEnabled == true else {
            print("⚠️ Close button tapped but not yet enabled")
            return
        }
        
        print("👆 Close button tapped - Destroying ad")
        callbacks?.onAdClosed()
        destroy()
    }
    
    // MARK: - Pause/Resume Support (TODO: Future enhancement)
    
    // Note: NSTimer không có built-in pause/resume như SonicCountDownTimer
    // Cần implement thủ công nếu cần:
    // - Lưu remaining time khi pause
    // - Tạo timer mới với remaining time khi resume
}
