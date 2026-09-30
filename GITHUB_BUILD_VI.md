# Build APK bằng GitHub Actions

Project đã có sẵn `.github/workflows/build-apk.yml`.

Sau khi đưa toàn bộ project lên một repository GitHub:

1. Mở tab **Actions** của repository.
2. Chọn **Build KenPhoto AI APK**.
3. Chọn **Run workflow**.
4. Khi workflow chạy thành công, tải artifact **KenPhotoAI-v1.2-debug**.
5. Giải nén artifact để lấy `KenPhotoAI-v1.2-debug.apk`.

Workflow dùng JDK 17, Android SDK Platform 36, Build Tools 36.0.0 và Gradle 9.5.0.
