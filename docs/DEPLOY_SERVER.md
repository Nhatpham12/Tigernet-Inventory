# Deploy bản dev lên máy ảo (Docker + Let's Encrypt)

Mục tiêu: mỗi lần merge vào nhánh `dev`, GitHub Actions tự build, đóng gói Docker, đẩy image lên GHCR
và cập nhật máy ảo (Ubuntu/Debian, IP public, SSH) chạy bản demo, có HTTPS miễn phí từ Let's Encrypt (certbot), tự gia hạn.

## 1. Kiến trúc

```
Internet ──80/443──▶ [proxy: nginx]──/bff/*──▶ [bff :8081] ──▶ [backend :8080] ──▶ [mysql :3306]
                         │   └────────/*──────▶ [web: nginx + Flutter Web]
                         └── /.well-known/acme-challenge ◀── [certbot] (gia hạn mỗi 12 giờ)
```

- Tất cả chạy bằng `docker-compose.yml` ở gốc repo, trên **một** máy ảo.
- Chỉ container `proxy` mở cổng ra ngoài. MySQL, backend, BFF chỉ trong mạng Docker nội bộ.
- Frontend build với `BFF_BASE_URL` rỗng nên tự gọi BFF cùng tên miền (`https://<domain>/bff/...`), không có CORS,
  một image dùng được cho mọi tên miền.
- Database `tigernet_dev` dựng tự động lần đầu từ `docs/Database_Script_v7.sql` (+ `V6__user_page_permissions.sql`)
  qua `deploy/mysql-init/01-tigernet-schema.sh`. Dữ liệu nằm trong volume `tigernet_mysql-data`.

| File | Vai trò |
|---|---|
| `backend/Dockerfile`, `bff/Dockerfile` | Build JAR bằng Maven Wrapper, chạy trên JRE 21, user không phải root |
| `frontend/Dockerfile`, `frontend/nginx.conf` | Build Flutter Web (`--no-web-resources-cdn`), phục vụ bằng nginx |
| `docker-compose.yml` | mysql, backend, bff, web, proxy, certbot |
| `deploy/nginx/*` | Cấu hình proxy: tự chọn HTTP (chưa có chứng chỉ) hoặc HTTPS |
| `deploy/.env.example` | Mẫu biến môi trường (chép thành `.env`, không commit) |
| `deploy/scripts/bootstrap-server.sh` | Cài Docker, swap, ufw trên máy ảo mới |
| `deploy/scripts/init-letsencrypt.sh` | Cấp chứng chỉ lần đầu |
| `deploy/scripts/deploy.sh` | Kéo image, khởi động lại, kiểm tra sức khỏe, tự rollback |
| `deploy/scripts/backup-db.sh` | mysqldump + xoay vòng + (tùy chọn) đẩy S3 |
| `deploy/scripts/seed-demo-users.sh` | Tạo 5 tài khoản demo (xem `docs/NGUOI_DUNG_MAU.md`) |
| `.github/workflows/dev-ci-cd.yml` | CI/CD nhánh dev |

## 2. Luồng CI/CD (`.github/workflows/dev-ci-cd.yml`)

| Sự kiện | Việc chạy |
|---|---|
| PR vào `dev` | `mvnw verify` backend + BFF, `flutter analyze` + `flutter test` frontend |
| Push/merge vào `dev` | Như trên → build 3 image `tigernet-backend`, `tigernet-bff`, `tigernet-web` → đẩy `ghcr.io/nhatpham12/...` với tag `dev` và `sha-<commit>` → deploy lên máy ảo qua SSH (nếu `DEPLOY_ENABLED=true`) |
| Chạy tay (Run workflow) | Để trống `image_tag`: chạy đủ như push. Nhập `image_tag` (VD `sha-1a2b...`): bỏ qua build, deploy lại đúng bản đó (rollback) |

Không đụng nhánh `main`. Bước deploy dùng GitHub environment `dev` (có thể bật "Required reviewers" nếu muốn duyệt tay).

### Secrets và biến trên GitHub

Settings → Environments → tạo `dev`, rồi thêm:

| Loại | Tên | Giá trị |
|---|---|---|
| Secret | `VM_HOST` | IP public hoặc tên miền của máy ảo |
| Secret | `VM_USER` | User deploy trên máy ảo (vd `deploy`) |
| Secret | `VM_SSH_KEY` | Private key của cặp khoá CI (tạo riêng, không dùng khoá cá nhân) |
| Secret | `VM_SSH_KNOWN_HOSTS` | Kết quả `ssh-keyscan -p <cổng> -t ed25519 <VM_HOST>` |
| Variable (repo) | `DEPLOY_ENABLED` | `true` khi máy ảo đã sẵn sàng (chưa đặt thì CI chỉ build, không deploy) |
| Variable | `VM_SSH_PORT` | Cổng SSH nếu khác 22 (bỏ trống = 22) |
| Variable (env) | `DEV_URL` | `https://<tên-miền>` (hiện link trên trang Actions) |

Image trên GHCR dùng `GITHUB_TOKEN` của workflow để đẩy và để máy ảo kéo về lúc deploy, không cần PAT.
Mật khẩu ứng dụng (DB, JWT, mail, DEMO_PASSWORD) **không** nằm trên GitHub: chỉ ở `/opt/tigernet/.env` trên máy ảo.

## 3. Các bước dựng lần đầu

### 3.1. Yêu cầu máy ảo

Cần hỏi người cấp máy (sếp/IT) những thông tin sau:

| Thông tin | Yêu cầu |
|---|---|
| Hệ điều hành | Ubuntu 22.04/24.04 LTS hoặc Debian 12, x86_64 |
| Cấu hình | Tối thiểu 2 vCPU, 4 GB RAM (MySQL + 2 JVM). 2 GB chạy được nhờ swap nhưng chậm khi khởi động |
| Ổ đĩa | Từ 30 GB |
| IP public | Cố định (không đổi khi khởi động lại máy) |
| Tài khoản | Một user có quyền `sudo`, đăng nhập SSH bằng khoá. Nên tạo user riêng: `sudo adduser deploy && sudo usermod -aG sudo deploy` (tạo user `deploy` và cho dùng sudo) |
| Cổng SSH | Cổng đang dùng (nếu khác 22 thì đặt biến `VM_SSH_PORT` trên GitHub) |
| Tường lửa của nhà cung cấp | Nếu có (security group, firewall panel): mở 80, 443 cho mọi nơi; mở cổng SSH cho IP quản trị và GitHub Actions (xem 3.2) |

### 3.2. Tường lửa

| Cổng | Nguồn | Lý do |
|---|---|---|
| SSH (22 hoặc cổng riêng) | IP quản trị + GitHub Actions *, hoặc mọi nơi khi chỉ cho đăng nhập bằng khoá | Quản trị + CI deploy |
| 80/tcp | Mọi nơi | HTTP, thử thách ACME của Let's Encrypt, chuyển hướng sang HTTPS |
| 443/tcp | Mọi nơi | HTTPS |

Không mở 3306, 8080, 8081. `bootstrap-server.sh` bật `ufw` với đúng các cổng trên (tự nhận cổng SSH đang chạy).
(*) Dải IP runner GitHub thay đổi thường xuyên; cách đơn giản là mở SSH cho mọi nơi nhưng tắt đăng nhập mật khẩu
và root, chỉ dùng khoá. Làm **sau khi** đã SSH được bằng khoá, giữ phiên SSH hiện tại mở để sửa nếu lỗi:

```bash
# file cấu hình riêng, ghi đè mặc định: cấm đăng nhập bằng mật khẩu và cấm SSH bằng root
printf 'PasswordAuthentication no\nPermitRootLogin no\n' | sudo tee /etc/ssh/sshd_config.d/10-tigernet.conf
sudo sshd -t && sudo systemctl restart ssh   # kiểm tra cú pháp trước, hợp lệ mới khởi động lại SSH
sudo apt-get install -y fail2ban             # tự chặn IP dò mật khẩu SSH (jail sshd bật sẵn trên Ubuntu/Debian)
```

### 3.3. Tên miền / DNS

- Tạo bản ghi **A**: `<tên-miền>` (vd `tigernet.<công-ty>.vn`) → IP public của máy ảo. TTL 300.
- Kiểm tra: `nslookup <tên-miền>` trả về đúng IP trước khi xin chứng chỉ.

### 3.4. Cài đặt máy chủ

```bash
scp -P <cổng-ssh> deploy/scripts/bootstrap-server.sh <user>@<IP>:~   # chép script lên máy ảo
ssh -p <cổng-ssh> <user>@<IP>
sudo bash bootstrap-server.sh   # Docker Engine + compose, swap 2 GB (nếu chưa có), ufw SSH/80/443, tạo /opt/tigernet
exit                            # SSH lại để nhóm docker có hiệu lực
```

Muốn cho user khác (vd `deploy`) chạy docker: `sudo DEPLOY_USER=deploy bash bootstrap-server.sh`.

Khoá riêng cho CI (tạo trên máy của bạn, không dùng khoá cá nhân):

```bash
ssh-keygen -t ed25519 -f tigernet_ci -C "github-actions-tigernet" -N ""   # cặp khoá chỉ dùng cho CI
ssh-copy-id -p <cổng-ssh> -i tigernet_ci.pub <user>@<IP>                 # đưa public key vào ~/.ssh/authorized_keys
ssh-keyscan -p <cổng-ssh> -t ed25519 <IP>                                # -> secret VM_SSH_KNOWN_HOSTS
```

Dán nội dung `tigernet_ci` vào secret `VM_SSH_KEY`, rồi xoá file khoá riêng trên máy.

### 3.5. Biến môi trường trên máy chủ

```bash
cd /opt/tigernet
# lần đầu chưa có file: chép deploy/.env.example từ repo lên (scp) thành .env
nano .env && chmod 600 .env
```

Giá trị cần điền (mẫu đầy đủ ở `deploy/.env.example`):

| Biến | Ghi chú |
|---|---|
| `REGISTRY` | `ghcr.io/nhatpham12` (chữ thường) |
| `DOMAIN` | Tên miền ở 3.3 |
| `LETSENCRYPT_EMAIL` | Email nhận cảnh báo hết hạn chứng chỉ |
| `BFF_COOKIE_SECURE` | `true` (có HTTPS). Chỉ đặt `false` tạm thời khi chưa có chứng chỉ |
| `DB_NAME` | `tigernet_dev` |
| `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Sinh ngẫu nhiên: `openssl rand -base64 24` |
| `JWT_SIGNER_KEY` | `openssl rand -base64 64 \| tr -d '\n'` (>= 64 ký tự) |
| `MFA_ENC_KEY` | `openssl rand -base64 32`. Để trống = tắt xác thực 2 bước của Quản trị |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | App password Gmail nếu cần gửi mail, để trống được |
| `DEMO_PASSWORD` | Mật khẩu chung của 5 tài khoản demo |

Thông tin doanh nghiệp in trên biểu mẫu: chép `backend/company.example.yaml` thành
`/opt/tigernet/deploy/config/application.yaml` và điền (CI không ghi đè file này).

### 3.6. Lần deploy đầu

1. Trên GitHub: Settings → Environments → `dev`, thêm secrets ở mục 2; đặt biến `DEPLOY_ENABLED=true`
   (và `VM_SSH_PORT` nếu SSH không ở cổng 22). Vào Actions → **dev CI/CD** → Run workflow (nhánh `dev`).
   CI chép `docker-compose.yml`, `deploy/`, script SQL lên `/opt/tigernet` rồi chạy `deploy.sh`.
   Database trống được dựng tự động từ `docs/Database_Script_v7.sql` (đã gồm V7, V8), không cần áp SQL tay.
   Lúc này proxy chạy **HTTP** (chưa có chứng chỉ): mở `http://<tên-miền>` để kiểm tra
   (muốn đăng nhập được qua HTTP thì tạm đặt `BFF_COOKIE_SECURE=false`, xong bước 2 đặt lại `true`;
   mỗi lần đổi chạy `docker compose up -d bff` để BFF nhận giá trị mới).
2. Cấp chứng chỉ:
   ```bash
   cd /opt/tigernet
   # thử trước với máy chủ staging của Let's Encrypt cho chắc: LETSENCRYPT_STAGING=1 trong .env
   ./deploy/scripts/init-letsencrypt.sh
   # thành công thì đặt LETSENCRYPT_STAGING=0, xoá chứng chỉ thử rồi chạy lại:
   #   docker compose --profile tls run --rm --entrypoint certbot certbot delete --cert-name <tên-miền>
   ```
   Script: bật proxy HTTP → `certbot certonly --webroot` → khởi động lại proxy (tự chuyển sang HTTPS, HTTP 301 sang HTTPS)
   → bật container `certbot` gia hạn tự động.
3. Tạo tài khoản demo: `./deploy/scripts/seed-demo-users.sh`
4. Mở `https://<tên-miền>`, đăng nhập `demo_admin` (lần đầu phải cài xác thực 2 bước nếu đã đặt `MFA_ENC_KEY`).

## 4. Chứng chỉ Let's Encrypt

- Cấp: webroot qua `/.well-known/acme-challenge/` (proxy phục vụ từ volume `certbot-www`). Không cần dừng nginx.
- Gia hạn: container `certbot` chạy `certbot renew` mỗi 12 giờ; certbot chỉ gia hạn khi còn dưới 30 ngày.
  Proxy tự `nginx -s reload` mỗi 6 giờ để nhận chứng chỉ mới.
- Kiểm tra: `docker compose --profile tls run --rm --entrypoint certbot certbot certificates`
  và thử gia hạn: `... certbot renew --dry-run --webroot -w /var/www/certbot`.
- Giới hạn của Let's Encrypt: 5 chứng chỉ trùng tên miền mỗi tuần, nên thử bằng `LETSENCRYPT_STAGING=1` khi còn đang chỉnh.
- Đổi tên miền: sửa `DOMAIN` trong `.env`, chạy lại `init-letsencrypt.sh`.

## 5. Rollback

- **Tự động**: `deploy.sh` đợi tối đa 3 phút cho backend (`/v3/api-docs`), web và BFF trả lời; nếu không,
  in log và quay về tag đang chạy trước đó. Job CI báo đỏ.
- **Từ GitHub**: Actions → dev CI/CD → Run workflow → nhập `image_tag` của bản tốt (xem tab Packages hoặc lịch sử
  run: tag là `sha-<commit đầy đủ>`).
- **Trên máy chủ**: `cd /opt/tigernet && ./deploy/scripts/deploy.sh --rollback` (về bản ngay trước)
  hoặc `./deploy/scripts/deploy.sh sha-<commit>`.
- **Database**: backend dùng `ddl-auto: update` (chỉ thêm cột/bảng), nên quay về image cũ thường vẫn chạy.
  Nếu bản mới làm hỏng dữ liệu, khôi phục bản sao lưu tự động chụp ngay trước deploy (mục 6).

## 6. Sao lưu database

- `deploy.sh` luôn chạy `backup-db.sh` trước khi đổi phiên bản.
- Hằng ngày (crontab của `ubuntu`, `crontab -e`):
  ```
  0 2 * * * /opt/tigernet/deploy/scripts/backup-db.sh >> /opt/tigernet/backups/backup.log 2>&1
  ```
- File: `/opt/tigernet/backups/tigernet_dev_YYYYMMDD_HHMMSS.sql.gz`, giữ `BACKUP_KEEP_DAYS` (mặc định 14) ngày.
- Ra ngoài máy: bản sao lưu chỉ nằm trên ổ máy ảo, hỏng máy là mất. Nên chép thêm ra nơi khác:
  đặt `BACKUP_S3_BUCKET` (cài AWS CLI `sudo snap install aws-cli --classic` và cấu hình khoá có quyền ghi bucket),
  hoặc kéo định kỳ về máy khác bằng `rsync`. Hỏi người cấp máy ảo có snapshot ổ đĩa định kỳ không.
- Khôi phục:
  ```bash
  cd /opt/tigernet && set -a && . ./.env && set +a
  gunzip -c backups/<file>.sql.gz | docker compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql -uroot
  ```
  (file dump có `CREATE DATABASE`/`USE` nên ghi đè đúng database.)

## 7. Vận hành hằng ngày

```bash
cd /opt/tigernet
docker compose ps                       # trạng thái
docker compose logs -f --tail=200 backend
docker compose restart bff
docker stats                            # RAM/CPU
```

- Log container giới hạn 20 MB × 5 file mỗi service.
- Muốn dựng lại DB trắng (mất dữ liệu): `docker compose down`, `docker volume rm tigernet_mysql-data`,
  rồi `./deploy/scripts/deploy.sh <tag>` và `seed-demo-users.sh`.

## 8. Chạy thử trên máy local bằng Docker

```bash
cp deploy/.env.example .env      # giữ DOMAIN=localhost, REGISTRY=tigernet, IMAGE_TAG=local; điền mật khẩu, JWT_SIGNER_KEY
docker compose up -d --build     # lần đầu build Flutter khá lâu (~10 phút)
./deploy/scripts/seed-demo-users.sh
# mở http://localhost
```

## 9. Việc nên làm tiếp

- Chuyển sang Flyway (`database/tenant/`) và `ddl-auto: validate` trước khi lên staging/prod.
- Thêm Spring Boot Actuator `/actuator/health` cho kiểm tra sức khỏe thay vì `/v3/api-docs`, và tắt Swagger trên prod.
- Bản staging/prod dùng luồng riêng (nhánh `release`, tag trên `main`) và cần chủ dự án cho phép trước khi đụng `main`.
- Uptime Kuma (hoặc dịch vụ giám sát tương tự) để cảnh báo khi trang không truy cập được.
