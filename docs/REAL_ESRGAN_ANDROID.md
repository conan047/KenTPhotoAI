# Kế hoạch tích hợp Real-ESRGAN vào KenPhoto AI

Mục tiêu: thêm một backend AI chạy cục bộ, không cần API đám mây, trong khi giữ chế độ Poster/Văn bản dùng pipeline pixel an toàn.

## Kiến trúc đề xuất

1. Giữ `ImageProcessor` hiện tại cho Poster/Văn bản và máy không hỗ trợ Vulkan.
2. Triển khai `UpscaleEngine` bằng JNI/C++.
3. Dùng ncnn với Vulkan khi thiết bị hỗ trợ; fallback CPU khi cần.
4. Chạy model theo tile để giảm RAM/VRAM.
5. Chỉ dùng AI cho ảnh người/phong cảnh/ảnh cũ khi người dùng bật rõ ràng; không dùng AI sinh chi tiết trong chế độ bảo toàn chữ.

## Thành phần cần thêm

- Android NDK và CMake.
- ncnn Android Vulkan prebuilt hoặc tự build ncnn.
- Mã Real-ESRGAN-ncnn-vulkan thích nghi cho JNI.
- Model `.param` + `.bin` tương ứng.
- JNI bridge nhận Bitmap hoặc buffer và trả Bitmap.

## Model nên ưu tiên

- Ảnh thường: `realesrgan-x4plus`.
- Ảnh anime/minh họa: model anime nhỏ hơn.
- Nếu cần 2× thật sự, ưu tiên model 2× tương ứng thay vì luôn chạy 4× rồi thu nhỏ.

## Lưu ý về chữ/poster

Real-ESRGAN có thể tạo chi tiết có vẻ hợp lý nhưng không bảo đảm giữ nguyên từng nét chữ/số ở nguồn quá mờ. Vì vậy KenPhoto AI nên luôn giữ nút “Bảo toàn chữ” dùng pipeline không sinh ảnh.

## Cấu trúc thư mục dự kiến

```
app/src/main/cpp/
  CMakeLists.txt
  kenphoto_jni.cpp
  realesrgan_engine.cpp
app/src/main/assets/models/
  realesrgan-x4plus.param
  realesrgan-x4plus.bin
```

## Bước kiểm thử bắt buộc

- Ảnh 12 MP và 48 MP.
- Máy RAM 4 GB và 8 GB.
- GPU Qualcomm/Mali phổ biến.
- Ảnh poster có số điện thoại/tiếng Việt để xác nhận chế độ Poster không đổi chữ.
- So sánh 1×/2×/4×, thời gian chạy, nhiệt độ và mức RAM.
