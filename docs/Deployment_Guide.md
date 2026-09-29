# Hướng Dẫn Đóng Gói Và Triển Khai Ứng Dụng (Java Spring Boot + Flutter)

Mục tiêu: Hướng dẫn chi tiết từng bước để đóng gói一套 ứng dụng hoàn chỉnh gồm Backend (Java Spring Boot) và Frontend (Flutter) thành các container Docker, sau đó triển khai trên môi trường của khách hàng (On-Premise) hoặc server nội bộ.

## 1. Yêu Cầu Tiên Quyết
- Docker và Docker Compose đã được cài đặt trên máy chủ.
- Flutter SDK (phiên bản ổn định) và Java JDK (phiên bản 17 hoặc 21) đã được cài đặt trên máy build.
- File cấu hình `application.properties` hoặc `application.yml` của Spring Boot đã được chuẩn bị sẵn.

## 2. Đóng Gói Frontend (Flutter)
- **Bước 2.1:** Xây dựng phiên bản Web
  - Chạy lệnh `flutter build web --release` trong thư mục dự án Flutter.
  - Kết quả sẽ nằm trong thư mục `build/web`.
- **Bước 2.2:** Tạo Dockerfile cho Frontend
  - Sử dụng `nginx:alpine` làm nền tảng.
  - Copy toàn bộ nội dung thư mục `build/web` vào thư mục served của Nginx (`/usr/share/nginx/html`).
  - Copy file cấu hình `nginx.conf` tùy chỉnh để hỗ trợ SPA routing (chuyển hướng tất cả request không tìm thấy file về `index.html`).
- **Bước 2.3:** Build Docker Image
  - Lệnh: `docker build -t myapp-frontend -f Dockerfile.frontend .`

## 3. Đóng Gói Backend (Java Spring Boot)
- **Bước 3.1:** Xây dựng File JAR
  - Chạy lệnh `mvn clean package -DskipTests` (hoặc `gradle bootJar`) trong thư mục Backend.
- **Bước 3.2:** Tạo Dockerfile cho Backend
  - Sử dụng image `eclipse-temurin:21-jre-alpine` (hoặc bản JDK nếu cần build trong container).
  - Copy file JAR đã build vào container với tên `app.jar`.
  - Khai báo `ENTRYPOINT ["java", "-jar", "/app.jar"]`.
- **Bước 3.3:** Build Docker Image
  - Lệnh: `docker build -t myapp-backend -f Dockerfile.backend .`

## 4. Chuẩn Bị Database (MySQL)
- **Bước 4.1:** Tạo Script khởi tạo Schema
  - Tạo file `init.sql` chứa các câu lệnh `CREATE DATABASE`, `CREATE TABLE` và dữ liệu mặc định (nếu có).
  - Đặt file này vào thư mục mà Docker volume sẽ mount vào `/docker-entrypoint-initdb.d`.
- **Bước 4.2:** Cấu hình biến môi trường
  - Chuẩn bị file `.env` chứa các thông tin nhạy cảm: `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## 5. Tổng Hợp Với Docker Compose
- **Bước 5.1:** Tạo file `docker-compose.yml`
  - Định nghĩa 3 services: `frontend`, `backend`, `db`.
  - Services `db`: Sử dụng image `mysql:8.0`, mount volume lưu trữ dữ liệu (`db_data`) và mount script `init.sql`.
  - Services `backend`: Phụ thuộc vào `db`, sử dụng file `.env` để nạp cấu hình kết nối.
  - Services `frontend`: Phụ thuộc vào `backend`, map port `80:80`.
- **Bước 5.2:** Chạy hệ thống
  - Lệnh: `docker compose up -d`
  - Kiểm tra trạng thái: `docker compose ps`

## 6. Quy Trình Triển Khai Qua Git & CI/CD

Git không phải là nơi "deploy" trực tiếp, mà là nơi quản lý mã nguồn để kích hoạt các pipeline tự động build và đóng gói.

### 6.1 Triển khai cho SaaS (Mô hình chính)
Khách hàng truy cập qua domain chung (ví dụ: `app.tencongty.com`), dữ liệu nằm trên server của công ty bạn.
1. **Kích hoạt:** Developer push code lên Git repository.
2. **Build & Test (CI):**
   - CI Server clone code về.
   - Chạy các lệnh kiểm tra tự động (Unit test, Lint cho cả Flutter và Java).
3. **Package (CD):**
   - **Backend:** Dùng Maven/Gradle build file `.jar` -> Đóng gói thành Docker Image -> Push lên Registry (Docker Hub hoặc AWS ECR).
   - **Frontend:** Dùng `flutter build web` -> Đóng gói thành Docker Image (dùng Nginx) -> Push lên Registry.
4. **Deploy:**
   - Hệ thống CD pull image mới nhất về server production.
   - Chạy `docker compose up -d` hoặc cập nhật các pod trên Kubernetes.
   - **Lưu ý về Database:** Cần chạy script migration tự động trên các database của từng tenant khi có thay đổi schema (sử dụng Flyway/Liquibase).

### 6.2 Triển khai cho On-Premise (Gói Enterprise)
Khách hàng tự host trên server của họ, dữ liệu không rời khỏi hạ tầng khách hàng.
1. **Chuẩn bị Package:**
   - Team kỹ thuật build sẵn các file binary hoặc Docker Image ổn định (Stable Build).
   - Đóng gói thành 1 bộ bao gồm: `docker-compose.yml`, file `.sql` khởi tạo DB, và file `.env` cấu hình.
2. **Giao nộp:**
   - Cung cấp bộ cài đặt này cho khách hàng qua link tải an toàn.
3. **Triển khai tại chỗ (On-site):**
   - Khách hàng (hoặc kỹ thuật viên) thực hiện các lệnh sau trên server của họ:
     ```bash
     # Khởi động toàn bộ hệ thống
     docker compose up -d
     ```
4. **Cập nhật phiên bản:**
   - Khi có bản mới, công ty gửi file package mới.
   - Khách hàng thực hiện lệnh cập nhật:
     ```bash
     docker compose pull
     docker compose up -d
     ```

---

## 7. Lưu Ý Về Bản Quyền (License)
- Theo mô hình kinh doanh, hệ thống on-premise sẽ kết nối định kỳ về `License Server` của công ty bạn.
- Backend cần được cấu hình thêm biến môi trường `LICENSE_SERVER_URL` và `TENANT_ID`.
- Nếu License hết hạn, hệ thống sẽ tự động chuyển sang chế độ "Read-only" theo logic nghiệp vụ đãocode trong Backend.
