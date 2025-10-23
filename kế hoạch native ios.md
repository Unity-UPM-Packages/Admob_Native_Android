### **Kế hoạch tổng thể: Xây dựng Plugin Native Ad cho iOS tích hợp vào Unity**

**Mục tiêu:** Tạo ra phiên bản iOS cho thư viện quảng cáo Native, có chức năng tương đương với phiên bản Android (bao gồm logic Decorator, timer, pause/resume) và hoạt động liền mạch với API C# đã có.

---

### **Giai đoạn I: Xây dựng "Động cơ" Native bên phía Xcode**

**Sản phẩm cuối cùng:** Một thư mục `AdmobNative.framework`.

| **Đầu mục công việc** | **Chi tiết hành động** | **File/Thành phần liên quan** | **Vai trò & Ý nghĩa** |
| :--- | :--- | :--- | :--- |
| **1. Chuẩn bị Môi trường** | - Sử dụng máy tính **macOS**.<br>- Cài đặt **Xcode** và **CocoaPods**. | `Xcode`, `Terminal` | Thiết lập "xưởng sản xuất" cần thiết. |
| **2. Tạo Project & Target** | - Tạo một project **App** mới trong Xcode (ví dụ: `AdmobNativeTestApp`).<br>- Thêm một **Target** mới loại **Framework** (ví dụ: `AdmobNative`). | `AdmobNativeTestApp` (Target), `AdmobNative` (Target) | Tạo "sân thử nghiệm" (App) và "sản phẩm" (Framework) trong cùng một môi trường. |
| **3. Tích hợp AdMob SDK** | - Tạo `Podfile` ở thư mục gốc.<br>- Thêm `pod 'Google-Mobile-Ads-SDK', '11.13.0'` cho cả hai target.<br>- Chạy `pod install`. | `Podfile`, `.xcworkspace` | Cung cấp thư viện AdMob cho cả "sản phẩm" và "sân thử nghiệm". |
| **4. Thiết kế Giao diện** | - Trong target `AdmobNative`, tạo một file **View** (`.xib`), ví dụ: `MediumNativeAdLayout.xib`.<br>- Đặt **Class** của View gốc là `GADNativeAdView`.<br>- Sử dụng **Interface Builder** và **Auto Layout** để thiết kế giao diện.<br>- Gán **`Tag`** (số nguyên) cho mỗi thành phần UI theo bảng quy ước đã thống nhất (Headline=101, MediaView=103...). | `MediumNativeAdLayout.xib` | Tạo ra "tấm toan" giao diện có thể tái sử dụng, không ràng buộc cứng với code. |
| **5. Xây dựng cấu trúc Decorator (Objective-C)** | - Tạo `@protocol IShowBehavior`.<br>- Tạo class `BaseShowBehavior` implement `IShowBehavior`.<br>- Tạo các class `CountdownDecorator`, `PositionDecorator`... implement `IShowBehavior`. | `IShowBehavior.h`<br>`BaseShowBehavior.h/.m`<br>`CountdownDecorator.h/.m`<br>`PositionDecorator.h/.m` | **Sao chép kiến trúc Decorator từ Kotlin.** `BaseShowBehavior` xử lý UI cơ bản, các `Decorator` "bọc" thêm chức năng. |
| **6. Tái tạo Logic Timer** | - Bên trong `CountdownDecorator.m`, sử dụng **`NSTimer`** để tái tạo lại chuỗi 3 timer (initial delay -> countdown -> clickable delay).<br>- Implement logic pause/resume cho các `NSTimer` này. | `CountdownDecorator.m` | Dịch lại logic timer từ `SonicCountDownTimer`/`Handler` của Kotlin sang `NSTimer` của iOS. |
| **7. Viết "Người lắp ráp"** | - Tạo class `AdmobNativeController.h/.m`.<br>- Class này sẽ quản lý `GADAdLoader`, nhận các cấu hình (`with...`), và lắp ráp chuỗi decorator trong hàm `showAd`. | `AdmobNativeController.h/.m` | "Bộ não" logic chính, tương đương với `AdmobNativeController.kt`. |
| **8. Xây dựng "Cây cầu"** | - Tạo cặp file `AdmobNativeBridge.h/.m`.<br>- **`.h`**: Khai báo các hàm `extern "C"` và các `typedef` cho con trỏ hàm callback.<br>- **`.m`**: Chứa định nghĩa của các hàm C-style. Sử dụng kiến trúc **Handle/Pointer** (`void*`) và `NSMutableDictionary` để quản lý nhiều instance `AdmobNativeController`. | `AdmobNativeBridge.h/.m` | **"Cửa ngõ" giao tiếp duy nhất** giữa C# và Objective-C. Che giấu toàn bộ sự phức tạp của Objective-C. |
| **9. Test độc lập** | - Trong `ViewController.m` của target App, `#import` file Bridge.<br>- Viết code để gọi các hàm Bridge, tạo controller, tải và hiển thị quảng cáo.<br>- Viết các hàm C-style để làm callback "giả" và truyền con trỏ của chúng vào Bridge.<br>- Build và chạy target App trên Simulator/thiết bị thật để debug. | `ViewController.m` | Đảm bảo "động cơ" iOS hoạt động hoàn hảo trước khi lắp ráp vào Unity. |
| **10. Build Framework** | - Trong Xcode, chọn Scheme là `AdmobNative`.<br>- Chọn "Any iOS Device (arm64)".<br>- Build project (Product -> Build).<br>- Lấy thư mục `AdmobNative.framework` từ thư mục "Products". | `AdmobNative.framework` | Đóng gói toàn bộ code, header, và tài nguyên (`.xib`) thành một sản phẩm duy nhất. |

---

### **Giai đoạn II: Tích hợp vào Unity và Hoàn thiện C#**

**Sản phẩm:** Cập nhật package UPM để hỗ trợ đầy đủ cho iOS.

| **Đầu mục công việc** | **Chi tiết hành động** | **File/Thành phần liên quan** | **Vai trò & Ý nghĩa** |
| :--- | :--- | :--- | :--- |
| **11. Import Framework** | - Kéo toàn bộ thư mục `AdmobNative.framework` vào `Packages/Ads Manager/Runtime/Plugins/iOS/` trong project Unity. | `AdmobNative.framework` | Đưa "động cơ" iOS vào trong project Unity. |
| **12. Tạo Client cho iOS** | - Tạo file `AdmobNativeIOSClient.cs` trong thư mục `Runtime/` của package.<br>- Cho nó implement `IAdmobNativePlatformClient`.<br>- Chứa một biến `private IntPtr _nativeControllerPtr;`. | `AdmobNativeIOSClient.cs` | "Người phiên dịch" chuyên biệt cho iOS. |
| **13. Khai báo `[DllImport]`** | - Bên trong `AdmobNativeIOSClient`, khai báo tất cả các hàm C-style từ `AdmobNativeBridge.h` bằng `[DllImport("__Internal")]`. | `AdmobNativeIOSClient.cs` | Tạo các "nút bấm" trên "điều khiển từ xa" C# để gọi đến Bridge. |
| **14. Xử lý Callbacks** | - Trong `AdmobNativeIOSClient.cs`, định nghĩa các `delegate` có chữ ký khớp với các `typedef` bên Objective-C.<br>- Viết các phương thức `static` được đánh dấu bằng **`[MonoPInvokeCallback]`**.<br>- Bên trong `Initialize()`, gọi hàm `AdmobNative_RegisterCallbacks(...)` để gửi các con trỏ hàm sang Objective-C. | `AdmobNativeIOSClient.cs` | Xây dựng cơ chế để Objective-C có thể "gọi điện" ngược lại cho C#. |
| **15. Implement Interface** | - Hoàn thiện các phương thức của `IAdmobNativePlatformClient` trong `AdmobNativeIOSClient.cs` bằng cách gọi đến các hàm `[DllImport]` tương ứng. | `AdmobNativeIOSClient.cs` | Hoàn thành vai trò "người phiên dịch". |
| **16. Cập nhật "Nhà máy"** | - Mở `AdmobNativePlatform.cs`.<br>- Hoàn thiện khối `#elif UNITY_IOS && !UNITY_EDITOR` để nó tạo `new AdmobNativeIOSClient()`. | `AdmobNativePlatform.cs` | Dạy cho "nhà máy" cách "sản xuất" client cho iOS. |
| **17. Viết Script Post-Build** | - Tạo một script Editor mới (ví dụ: `IOSBuildPostProcessor.cs`) sử dụng `[PostProcessBuild]`.<br>- Sử dụng API `UnityEditor.iOS.Xcode.PBXProject` để sửa đổi project Xcode được Unity export ra. | `IOSBuildPostProcessor.cs` | Tự động hóa các bước cấu hình bắt buộc cho iOS, giúp người dùng không cần phải làm thủ công. |
| **18. Build và Test** | - Chuyển nền tảng trong Unity sang iOS.<br>- Build project (không phải Export).<br>- Mở project Xcode được tạo ra.<br>- Build và chạy trên thiết bị thật để kiểm tra lần cuối. | `Unity`, `Xcode` | Kiểm tra toàn bộ hệ thống hoạt động cùng nhau trên một thiết bị iOS thật. |


### **Bảng quy ước `Tag` cho các thành phần UI trong `.xib`**

| Tag ID | Thành phần Giao diện | Loại View iOS | Tương ứng `android:id` | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **101** | **Headline** (Tiêu đề) | `UILabel` | `primary` | Thành phần quan trọng nhất. |
| **102** | **Body** (Nội dung mô tả) | `UILabel` | `body` | |
| **103** | **Media View** (Video/Ảnh chính) | `GADMediaView` | `media_view` | Bắt buộc phải có để hiển thị media. |
| **104** | **Icon** (Biểu tượng ứng dụng) | `UIImageView` | `icon` | |
| **105** | **Call to Action** (Nút hành động) | `UIButton` | `cta` | Nút "Cài đặt", "Tìm hiểu thêm"... |
| **106** | **Star Rating** (Đánh giá sao) | `UIImageView` | `rating_bar` | **Lưu ý:** iOS không có RatingBar, dùng UIImageView để hiển thị ảnh sao. |
| **107** | **Advertiser** (Nhà quảng cáo) | `UILabel` | `secondary` | Thường là tên công ty/ứng dụng. |
| **108** | **Store** (Cửa hàng) | `UILabel` | `ad_store` | Ví dụ: "App Store". |
| **109** | **Price** (Giá) | `UILabel` | `ad_price` | |
| | | | | |
| **110** | **Close Button** (Nút X) | `UIImageView` | `ad_close_button` | Dùng `UIImageView` để dễ dàng tùy chỉnh icon. |
| **111** | **Countdown Text** (Số đếm ngược) | `UILabel` | `ad_countdown_text` | Số "5", "4", "3"... |
| **112** | **Progress Bar** (Vòng tròn/Thanh đếm) | `UIProgressView` | `ad_progress_bar` | Hoặc một `UIView` tùy chỉnh nếu bạn tự vẽ. |

---

**Cách sử dụng:**

*   **Khi thiết kế `.xib`:** Mở **Attributes Inspector** cho mỗi thành phần và nhập số `Tag` tương ứng.
*   **Khi viết code `populate` trong Objective-C:** Dùng `[adView viewWithTag:101]` để lấy `UILabel` của headline, `[adView viewWithTag:103]` để lấy `GADMediaView`, v.v.
