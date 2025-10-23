//
//  AdmobNativeBridge.h
//  AdmobNative iOS Bridge
//
//  C interface for Unity integration
//

#ifndef AdmobNativeBridge_h
#define AdmobNativeBridge_h

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// MARK: - Callback Type Definitions

/// Callback không có tham số
typedef void (*VoidCallback)(void);

/// Callback với error message
typedef void (*ErrorCallback)(const char* errorMessage);

/// Callback cho paid event
typedef void (*PaidEventCallback)(int precisionType, long long valueMicros, const char* currencyCode);

/// Callback cho video mute
typedef void (*VideoMuteCallback)(bool isMuted);

// MARK: - Controller Management

/// Tạo một controller instance mới
/// @return Handle để sử dụng trong các function calls khác
void* AdmobNative_Create(void);

/// Destroy controller instance
/// @param handle Controller handle
void AdmobNative_Destroy(void* handle);

// MARK: - Callback Registration

/// Đăng ký tất cả callback functions
/// @param handle Controller handle
/// @param onAdLoaded Callback khi ad load thành công
/// @param onAdFailedToLoad Callback khi ad load thất bại
/// @param onAdShow Callback khi ad được hiển thị
/// @param onAdClosed Callback khi ad bị đóng
/// @param onPaidEvent Callback cho paid event
/// @param onAdDidRecordImpression Callback khi impression được ghi nhận
/// @param onAdClicked Callback khi ad được click
/// @param onVideoStart Callback khi video bắt đầu
/// @param onVideoEnd Callback khi video kết thúc
/// @param onVideoMute Callback khi video mute/unmute
/// @param onVideoPlay Callback khi video play
/// @param onVideoPause Callback khi video pause
/// @param onAdShowedFullScreenContent Callback khi show full screen
/// @param onAdDismissedFullScreenContent Callback khi dismiss full screen
void AdmobNative_RegisterCallbacks(
    void* handle,
    VoidCallback onAdLoaded,
    ErrorCallback onAdFailedToLoad,
    VoidCallback onAdShow,
    VoidCallback onAdClosed,
    PaidEventCallback onPaidEvent,
    VoidCallback onAdDidRecordImpression,
    VoidCallback onAdClicked,
    VoidCallback onVideoStart,
    VoidCallback onVideoEnd,
    VideoMuteCallback onVideoMute,
    VoidCallback onVideoPlay,
    VoidCallback onVideoPause,
    VoidCallback onAdShowedFullScreenContent,
    VoidCallback onAdDismissedFullScreenContent
);

// MARK: - Ad Operations

/// Load quảng cáo
/// @param handle Controller handle
/// @param adUnitId Ad unit ID
void AdmobNative_LoadAd(void* handle, const char* adUnitId);

/// Hiển thị quảng cáo
/// @param handle Controller handle
/// @param layoutName Tên layout (.xib file name)
void AdmobNative_ShowAd(void* handle, const char* layoutName);

/// Hủy quảng cáo hiện tại
/// @param handle Controller handle
void AdmobNative_DestroyAd(void* handle);

/// Kiểm tra ad có available không
/// @param handle Controller handle
/// @return true nếu ad available
bool AdmobNative_IsAdAvailable(void* handle);

// MARK: - Configuration (Builder Pattern)

/// Cấu hình countdown timer
/// @param handle Controller handle
/// @param initial Initial delay trước khi countdown (seconds)
/// @param duration Thời gian countdown (seconds)
/// @param closeDelay Delay trước khi close button clickable (seconds)
void AdmobNative_WithCountdown(void* handle, float initial, float duration, float closeDelay);

/// Cấu hình vị trí hiển thị
/// @param handle Controller handle
/// @param x X position (pixels)
/// @param y Y position (pixels)
void AdmobNative_WithPosition(void* handle, int x, int y);

// MARK: - Dimensions

/// Lấy width của ad view
/// @param handle Controller handle
/// @return Width in pixels, -1 nếu không available
float AdmobNative_GetWidthInPixels(void* handle);

/// Lấy height của ad view
/// @param handle Controller handle
/// @return Height in pixels, -1 nếu không available
float AdmobNative_GetHeightInPixels(void* handle);

#ifdef __cplusplus
}
#endif

#endif /* AdmobNativeBridge_h */
