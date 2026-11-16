Thư mục này được mount vào container backend tại `/app/config` (chỉ đọc).

Muốn phiếu in có thông tin doanh nghiệp: chép `backend/company.example.yaml` thành
`deploy/config/application.yaml` trên máy chủ rồi điền thông tin thật (lưu UTF-8).
Spring Boot tự nạp `./config/application.yaml`. File đó không commit lên repo.
