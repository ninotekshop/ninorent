# NinoRent trên iOS

App dùng **Kotlin Multiplatform + Compose Multiplatform**. Toàn bộ UI và logic nằm ở module `:shared`
(`shared/src/commonMain`), chạy chung cho Android và iOS.

```
app/          Vỏ Android (MainActivity, manifest, icon)
shared/       Code dùng chung
  commonMain/   UI, model, Supabase, khai báo `expect` trong package platform/
  androidMain/  Cài đặt Android cho các `expect` (prefs, camera CameraX+ML Kit, PDF, BiometricPrompt...)
  iosMain/      Cài đặt iOS: gọi sang Swift qua interface IosBridge
iosApp/       Vỏ iOS (SwiftUI) + IosBridgeImpl.swift (camera AVFoundation, Face ID, PDF, in, chia sẻ, thông báo)
```

## Build trên Codemagic (không cần Mac)

1. Chạy workflow **`ios-simulator-check`** trước. Workflow này build cho iOS Simulator, không cần ký,
   dùng để kiểm tra code Kotlin/Swift biên dịch được. Xem log nếu có lỗi.
2. Để đưa lên TestFlight, chuẩn bị:
   - Tài khoản Apple Developer (99 USD/năm) và một app trong App Store Connect với bundle id `com.ninotek.ninorent`.
   - Trên Codemagic: thêm **App Store Connect API key** (Teams → Integrations), đặt tên khớp với
     `app_store_connect` trong `codemagic.yaml`. Codemagic tự tạo certificate và provisioning profile.
   - Điền `APP_STORE_APPLE_ID` trong workflow **`ios-app-store-workflow`**.

## Build trên Mac

```bash
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```

Cần JDK 17+ và Android SDK (`local.properties` có `sdk.dir`), vì Gradle vẫn cấu hình cả module Android.
Khi build, Xcode chạy `./gradlew :shared:embedAndSignAppleFrameworkForXcode` để dựng framework `Shared`.

## Việc đã biết còn thiếu / cần lưu ý

- **Gửi OTP quên mật khẩu qua SMTP chưa chạy trên iOS.** Màn hình tự chuyển sang OTP giả lập khi lỗi, giống khi offline.
  Nên chuyển việc gửi email sang server (ví dụ Supabase Edge Function); cũng giúp bỏ mật khẩu SMTP đang nằm
  trong `EmailSender.kt`.
- Ảnh CCCD/logo lưu ở thư mục Documents của app và Supabase chỉ giữ đường dẫn cục bộ, nên không xem được trên máy khác.
  Nên tải ảnh lên Supabase Storage.
- Code trong `iosMain` và Swift chưa từng được biên dịch/chạy trên thiết bị thật khi viết. Hãy kiểm tra kỹ:
  quét QR CCCD, chụp/chọn ảnh, Face ID, xuất/in/chia sẻ PDF, thông báo quá hạn.
- Nút Back và cử chỉ vuốt-quay-lại của Android (`BackHandler`) chưa có tương đương trên iOS; các màn hình dùng nút quay lại trên giao diện.
