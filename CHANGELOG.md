# Changelog

## 1.2.0
- Tiến độ xử lý hàng loạt dạng phần trăm.
- Ước tính kích thước đầu ra trong preview câu lệnh.
- Đồng bộ thuật toán tính kích thước giữa preview và xử lý thật.
- Bật `largeHeap` cho ảnh lớn và tắt clear-text network.
- Nâng `BUILD_APK_WINDOWS.bat`: tự tìm SDK/JDK, kiểm tra Platform 36 + Build Tools 36.0.0, build rồi chép APK ra thư mục gốc.
- Thêm kiểm thử parser và kiểm tra compile Java bằng Android API stubs.

## 1.1.0
- Nâng parser câu lệnh.
- Thêm tự cân tương phản, saturation, edge-aware denoise.
- Làm nét tiết kiệm RAM hơn.
- Thêm giới hạn đầu ra theo RAM.
- Thêm hủy batch, chất lượng JPEG và preview câu lệnh.
- Sửa cấu hình AGP/Gradle.
- Thêm điểm cắm `UpscaleEngine` và tài liệu Real-ESRGAN.

## 1.0.0
- Chọn một/nhiều ảnh.
- Preset 2×/4K/Poster/Ảnh người/Ảnh cũ.
- Làm nét/khử nhiễu/tăng sáng cơ bản.
- Xem trước/sau và lưu JPG/PNG/WebP.
