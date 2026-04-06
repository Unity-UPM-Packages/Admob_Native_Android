package com.thelegends.ads.admob_native_unity.decorator

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import android.os.CountDownTimer
import com.thelegends.admob_native_unity.NativeAdCallbacks
import com.thelegends.ads.admob_native_unity.showbehavior.BaseShowBehavior
import com.thelegends.ads.admob_native_unity.showbehavior.DynamicShowBehavior

/**
 * GIAI ĐOẠN 7: Decorator đếm ngược thời gian.
 *
 * Triết lý thiết kế: Decorator này KHÔNG tự tạo View. Nó chỉ điều khiển BEHAVIOR.
 * Tất cả View (CloseButton, CountdownText) đều được tạo từ JSON/Unity bởi DynamicAdBuilderLayout,
 * và được lấy thông qua registeredViews["Decorator_CloseButton"], registeredViews["Decorator_CountdownText"].
 */
class CountdownDecorator(
    private val wrappedBehavior: BaseShowBehavior,
    private val initialDelaySeconds: Float,
    private val countdownDurationSeconds: Float,
    private val closeButtonDelaySeconds: Float
) : BaseShowBehavior() {

    private val TAG = "CountdownDecorator"

    private var initialDelayTimer: CountDownTimer? = null
    private var countdownTimer: CountDownTimer? = null
    private var closeButtonDelayTimer: CountDownTimer? = null

    private val countdownDurationMillis = (countdownDurationSeconds * 1000).toLong()
    private val initialDelayMillis = (initialDelaySeconds * 1000).toLong()
    private val closeButtonDelayMillis = (closeButtonDelaySeconds * 1000).toLong()

    override fun show(
        activity: Activity,
        nativeAd: NativeAd,
        layoutName: String,
        callbacks: NativeAdCallbacks
    ) {
        activity.runOnUiThread {
            // Bước 1: Cho wrapped behavior render UI trước
            wrappedBehavior.show(activity, nativeAd, layoutName, callbacks)
            this.rootView = wrappedBehavior.rootView

            // Bước 2: Lấy views từ registeredViews của DynamicAdBuilderLayout
            // (Nhất quán với cách DynamicShowBehavior lấy Headline, Body, CTA...)
            val dynamicBehavior = wrappedBehavior as? DynamicShowBehavior
            val registeredViews = dynamicBehavior?.builderLayout?.registeredViews

            if (registeredViews == null) {
                Log.w(TAG, "registeredViews không tồn tại. CountdownDecorator chỉ hoạt động với DynamicShowBehavior.")
                return@runOnUiThread
            }

            val closeButton  = registeredViews["CloseButton"]
            // Khi element có CẢ image + text, DynamicAdBuilderLayout lưu TextView vào key "${elementType}_Text"
            // Còn registeredViews["CountdownText"] là FrameLayout container.
            // Thử lấy TextView từ key _Text trước, fallback sang key gốc nếu chỉ có text thuần.
            val countdownText: TextView? = (registeredViews["CountdownText_Text"]
                ?: registeredViews["CountdownText"]) as? TextView
            // Container là FrameLayout bọc ngoài (ảnh nền + text) — cần ẩn/hiện cùng với text
            val countdownContainer: View? = registeredViews["CountdownText"]

            Log.d(TAG, "CloseButton found: ${closeButton != null}, CountdownText found: ${countdownText != null}, Container: ${countdownContainer != null}")

            startCloseLogic(closeButton, countdownText, countdownContainer, callbacks)
        }
    }

    override fun destroy() {
        cancelAllTimers()
        wrappedBehavior.destroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 1: Ẩn tất cả, chờ initialDelay trước khi đếm ngược
    // ─────────────────────────────────────────────────────────────────────────
    private fun startCloseLogic(
        closeButton: View?,
        countdownText: TextView?,
        countdownContainer: View?,
        callbacks: NativeAdCallbacks
    ) {
        cancelAllTimers()

        // Ẩn tất cả ngay từ đầu
        closeButton?.visibility = View.GONE
        closeButton?.isClickable = false
        // Ẩn cả container (ảnh nền) lẫn TextView bên trong
        countdownContainer?.visibility = View.GONE
        countdownText?.visibility = View.GONE

        // Gắn listener cho nút đóng (chỉ hoạt động khi isClickable = true)
        closeButton?.setOnClickListener {
            Log.d(TAG, "Nút đóng được nhấn. Huỷ quảng cáo.")
            callbacks.onAdClosed()
            destroy()
        }

        if (initialDelayMillis > 0) {
            initialDelayTimer = object : CountDownTimer(initialDelayMillis, 500) {
                override fun onTick(timeRemaining: Long) { /* Im lặng */ }
                override fun onFinish() {
                    startMainCountdown(closeButton, countdownText, countdownContainer)
                }
            }
            initialDelayTimer?.start()
        } else {
            startMainCountdown(closeButton, countdownText, countdownContainer)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 2: Hiện CountdownText, tick mỗi giây, format "${seconds}s remaining"
    // ─────────────────────────────────────────────────────────────────────────
    private fun startMainCountdown(closeButton: View?, countdownText: TextView?, countdownContainer: View?) {
        // Hiện container (ảnh nền) và TextView, ẩn nút đóng
        countdownContainer?.visibility = View.VISIBLE
        countdownText?.visibility = View.VISIBLE
        closeButton?.visibility = View.GONE

        // Hiển thị giây ban đầu ngay lập tức (không chờ tick đầu tiên)
        val initialSeconds = (countdownDurationMillis / 1000).toInt()
        countdownText?.text = "${initialSeconds}s remaining"

        countdownTimer = object : CountDownTimer(countdownDurationMillis, 1000) {
            override fun onTick(timeRemaining: Long) {
                val secondsRemaining = (timeRemaining / 1000).toInt()
                if (secondsRemaining <= 0) {
                    onFinish()
                    return
                }
                countdownText?.text = "${secondsRemaining}s remaining"
            }

            override fun onFinish() {
                // Ẩn cả container lẫn text
                countdownContainer?.visibility = View.GONE
                countdownText?.visibility = View.GONE
                startCloseButtonDelay(closeButton)
            }
        }
        countdownTimer?.start()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 3: Hiện CloseButton, chờ closeButtonDelay rồi mới cho click
    // ─────────────────────────────────────────────────────────────────────────
    private fun startCloseButtonDelay(closeButton: View?) {
        closeButton?.visibility = View.VISIBLE
        closeButton?.isClickable = false // Chưa cho click ngay

        if (closeButtonDelayMillis > 0) {
            closeButtonDelayTimer = object : CountDownTimer(closeButtonDelayMillis, 100) {
                override fun onTick(timeRemaining: Long) { /* Im lặng */ }
                override fun onFinish() {
                    closeButton?.isClickable = true
                    Log.d(TAG, "Nút đóng đã được kích hoạt.")
                }
            }
            closeButtonDelayTimer?.start()
        } else {
            // Không có delay, cho click ngay
            closeButton?.isClickable = true
        }
    }

    private fun cancelAllTimers() {
        initialDelayTimer?.cancel()
        initialDelayTimer = null

        countdownTimer?.cancel()
        countdownTimer = null

        closeButtonDelayTimer?.cancel()
        closeButtonDelayTimer = null
    }
}
