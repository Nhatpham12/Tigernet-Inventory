# Dùng chung cho server HTTP (chưa có chứng chỉ) và HTTPS
client_max_body_size 25m;

# Không giới hạn số request theo IP (bỏ 28/11/2026: cả văn phòng có thể đi chung 1 IP qua Hestia/router).
# Chống dò mật khẩu còn ở backend: sai 5 lần từ cùng IP thì khoá cặp (tài khoản, IP) 15 phút.
location /bff/ {
    proxy_pass http://bff:8081;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 120s;
}

# HTTP/1.1 bắt buộc: gzip của container web mặc định chỉ nén request HTTP/1.1,
# proxy mặc định HTTP/1.0 làm main.dart.js/canvaskit.wasm (~10 MB) gửi không nén.
location / {
    proxy_pass http://web:80;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
}
