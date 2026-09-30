# KenPhoto AI v1.2.0 — Build Candidate

KenPhoto AI là ứng dụng Android xử lý ảnh **offline** bằng câu lệnh tiếng Việt. Bản này ưu tiên làm nét, nâng độ phân giải, tăng sáng/màu và chế độ Poster/Văn bản không sinh lại nội dung chữ.

## Chức năng hiện có

- Chọn 1 hoặc nhiều ảnh.
- Câu lệnh tiếng Việt: 4K/2K/1080p, 2×/4×, làm nét %, tăng sáng %, tương phản %, màu %, khử nhiễu.
- Preset: Tự động, Nét 2×, 4K, Poster, Khử nhiễu, Ảnh cũ, Ảnh người, Tăng sáng, Màu đẹp, 2K, 1080p, Nét mạnh.
- Chế độ Poster/Văn bản ưu tiên giữ nguyên chữ, số điện thoại và logo; không OCR rồi viết lại chữ.
- Khử nhiễu giữ biên, tự cân tương phản, điều chỉnh sáng/tương phản/bão hòa.
- Xử lý hàng loạt có tiến độ và nút HỦY.
- Xem Trước/Sau.
- Xuất JPG/PNG/WebP; chọn chất lượng JPG 85/90/95/100.
- Android 10+ lưu vào `Pictures/KenPhotoAI`.
- Giới hạn đầu ra theo RAM để giảm văng ứng dụng; manifest bật `largeHeap` cho tác vụ ảnh lớn.

## Điểm mới v1.2

- Hiển thị tiến độ hàng loạt theo phần trăm.
- Hiển thị kích thước đầu ra ước tính dựa trên ảnh xem trước.
- Gom logic tính kích thước đầu ra vào một hàm duy nhất để tránh lệch giữa phần xem trước và phần xử lý.
- Bật `largeHeap` và chặn clear-text network; app không cần Internet để xử lý ảnh.
- Nâng script Windows thành quy trình 1 nút: tự tìm Android SDK/JDK, kiểm tra Platform 36 + Build Tools 36.0.0, tải Gradle 9.5.0, build và chép APK ra thư mục gốc.
- Đã chạy kiểm thử parser câu lệnh và compile toàn bộ mã Java với bộ Android API stubs để bắt lỗi Java trước khi build SDK thật.

## Ví dụ câu lệnh

- `Làm nét 4K, giữ nguyên chữ và logo`
- `Poster văn bản 4K, bảo toàn chữ, số điện thoại và logo`
- `Ảnh người, làm nét nhẹ tự nhiên, khử nhiễu`
- `Phục hồi ảnh cũ, tự động cân bằng, làm nét 2x`
- `Làm nét 40%, tăng sáng 8%, màu 10%`
- `Làm nét 4x, chất lượng cao`
- `Làm nét 4K, tiết kiệm RAM`

## Cài/build APK trên Windows 10

### Cách dễ nhất

1. Cài Android Studio.
2. Trong SDK Manager, cài **Android SDK Platform 36** và **Android SDK Build-Tools 36.0.0**.
3. Giải nén project.
4. Bấm đúp `BUILD_APK_WINDOWS.bat`.
5. Khi thành công, file dễ tìm nhất là `KenPhotoAI-v1.2-debug.apk` ngay trong thư mục project.
6. Chép APK sang điện thoại và cài đặt. Android có thể yêu cầu cho phép “Cài ứng dụng không rõ nguồn gốc” cho ứng dụng dùng để mở APK.

### Build bằng Android Studio

Mở thư mục project → chờ Gradle Sync → `Build > Build APK(s)`. APK nằm trong `app\build\outputs\apk\debug\app-debug.apk`.

## Trạng thái AI Real-ESRGAN

Bản v1.2 **chưa nhúng Real-ESRGAN**. Hiện việc nâng kích thước dùng pipeline pixel xác định (deterministic), vì vậy Poster/Văn bản không bị AI tự bịa lại chữ. `UpscaleEngine` vẫn được giữ làm điểm cắm cho backend Real-ESRGAN/ncnn/Vulkan ở bản sau.

## Lưu ý chất lượng

Upscale thông thường có thể làm ảnh rõ hơn về hiển thị và tăng kích thước pixel, nhưng không thể khôi phục chính xác chi tiết đã mất trong ảnh gốc quá mờ/nén. Đây là lý do chế độ Poster ưu tiên không sinh lại nội dung.

## Build trên GitHub

Project có sẵn workflow `.github/workflows/build-apk.yml`. Nếu đưa project lên GitHub, có thể vào tab Actions → **Build KenPhoto AI APK** → **Run workflow** để nhận artifact APK. Xem `GITHUB_BUILD_VI.md`.
