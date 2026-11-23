# Dùng chung cho server HTTP (chưa có chứng chỉ) và HTTPS
client_max_body_size 25m;

# Đăng nhập: giới hạn chặt chống dò mật khẩu (zone trong ratelimit.conf). BFF không proxy
# /bff/api/auth/** nên đây là đường duy nhất tới /auth/login của backend. Làm mới token và
# đăng xuất đi theo /bff/ bên dưới: chặn chúng sẽ làm người dùng bị đăng xuất giữa chừng.
location = /bff/auth/login {
    limit_req zone=tn_login burst=20 nodelay;
    proxy_pass http://bff:8081;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# Bước mã 6 số của Quản trị: cùng giới hạn với đăng nhập (chống dò mã)
location /bff/auth/mfa/ {
    limit_req zone=tn_login burst=20 nodelay;
    proxy_pass http://bff:8081;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /bff/ {
    limit_req zone=tn_general burst=40 nodelay;
    proxy_pass http://bff:8081;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 120s;
}

location / {
    proxy_pass http://web:80;
    proxy_set_header Host $host;
}
