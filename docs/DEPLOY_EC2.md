# Kế hoạch deploy bản dev lên AWS EC2 (Docker + Let's Encrypt)

Mục tiêu: mỗi lần merge vào nhánh `dev`, GitHub Actions tự build, đóng gói Docker, đẩy image lên GHCR
và cập nhật máy chủ EC2 chạy bản demo, có HTTPS miễn phí từ Let's Encrypt (certbot), tự gia hạn.

## 1. Kiến trúc

```
Internet ──80/443──▶ [proxy: nginx]──/bff/*──▶ [bff :8081] ──▶ [backend :8080] ──▶ [mysql :3306]
                         │   └────────/*──────▶ [web: nginx + Flutter Web]
                         └── /.well-known/acme-challenge ◀── [certbot] (gia hạn mỗi 12 giờ)
```

- Tất cả chạy bằng `docker-compose.yml` ở gốc repo, trên **một** EC2.
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
| `deploy/scripts/bootstrap-ec2.sh` | Cài Docker, swap, ufw trên EC2 mới |
| `deploy/scripts/init-letsencrypt.sh` | Cấp chứng chỉ lần đầu |
| `deploy/scripts/deploy.sh` | Kéo image, khởi động lại, kiểm tra sức khỏe, tự rollback |
| `deploy/scripts/backup-db.sh` | mysqldump + xoay vòng + (tùy chọn) đẩy S3 |
| `deploy/scripts/seed-demo-users.sh` | Tạo 5 tài khoản demo (xem `docs/NGUOI_DUNG_MAU.md`) |
| `.github/workflows/dev-ci-cd.yml` | CI/CD nhánh dev |

## 2. Luồng CI/CD (`.github/workflows/dev-ci-cd.yml`)

| Sự kiện | Việc chạy |
|---|---|
| PR vào `dev` | `mvnw verify` backend + BFF, `flutter analyze` + `flutter test` frontend |
| Push/merge vào `dev` | Như trên → build 3 image `tigernet-backend`, `tigernet-bff`, `tigernet-web` → đẩy `ghcr.io/nhatpham12/...` với tag `dev` và `sha-<commit>` → deploy lên EC2 (nếu `DEPLOY_ENABLED=true`) |
| Chạy tay (Run workflow) | Để trống `image_tag`: chạy đủ như push. Nhập `image_tag` (VD `sha-1a2b...`): bỏ qua build, deploy lại đúng bản đó (rollback) |

Không đụng nhánh `main`. Bước deploy dùng GitHub environment `dev` (có thể bật "Required reviewers" nếu muốn duyệt tay).

### Secrets và biến trên GitHub

Settings → Environments → tạo `dev`, rồi thêm:

| Loại | Tên | Giá trị |
|---|---|---|
| Secret | `EC2_HOST` | Elastic IP hoặc tên miền của EC2 |
| Secret | `EC2_USER` | `ubuntu` |
| Secret | `EC2_SSH_KEY` | Private key của cặp khóa deploy (tạo riêng, không dùng khóa cá nhân) |
| Secret | `EC2_SSH_KNOWN_HOSTS` | Kết quả `ssh-keyscan -t ed25519 <EC2_HOST>` |
| Variable (repo) | `DEPLOY_ENABLED` | `true` khi EC2 đã sẵn sàng (chưa đặt thì CI chỉ build, không deploy) |
| Variable (env) | `DEV_URL` | `https://dev.<tên-miền>` (hiện link trên trang Actions) |

Image trên GHCR dùng `GITHUB_TOKEN` của workflow để đẩy và để EC2 kéo về lúc deploy, không cần PAT.
Mật khẩu ứng dụng (DB, JWT, mail, DEMO_PASSWORD) **không** nằm trên GitHub: chỉ ở `/opt/tigernet/.env` trên EC2.

## 3. Các bước dựng lần đầu

### 3.1. Tạo EC2

- Region: `ap-southeast-1` (Singapore) độ trễ thấp về VN. Nếu dữ liệu bắt buộc lưu ở VN thì cân nhắc VPS trong nước.
- AMI: **Ubuntu Server 24.04 LTS** (x86_64).
- Loại máy: tối thiểu `t3.medium` (2 vCPU, 4 GB RAM) vì có MySQL + 2 JVM. `t3.small` (2 GB) chạy được nhờ swap
  nhưng chậm khi khởi động.
- Ổ đĩa: gp3 30 GB.
- Key pair: tạo mới (để SSH quản trị). Khóa riêng cho CI tạo ở bước 3.4.
- Gắn **Elastic IP** để IP không đổi khi stop/start.
- IAM role (tùy chọn): quyền `s3:PutObject` vào bucket sao lưu nếu dùng `BACKUP_S3_BUCKET`.

### 3.2. Security group

| Cổng | Nguồn | Lý do |
|---|---|---|
| 22/tcp | IP văn phòng/nhà của quản trị + dải IP GitHub Actions *, hoặc 0.0.0.0/0 khi chỉ cho đăng nhập bằng khóa | SSH (quản trị + CI deploy) |
| 80/tcp | 0.0.0.0/0, ::/0 | HTTP, thử thách ACME của Let's Encrypt, chuyển hướng sang HTTPS |
| 443/tcp | 0.0.0.0/0, ::/0 | HTTPS |

Không mở 3306, 8080, 8081. (*) Dải IP runner GitHub thay đổi thường xuyên; cách đơn giản là mở 22 cho mọi nơi
nhưng tắt đăng nhập mật khẩu (mặc định của AMI Ubuntu) và chỉ dùng khóa.

### 3.3. Tên miền / DNS

- Tạo bản ghi **A**: `dev.<tên-miền>` → Elastic IP (Route 53 hoặc nhà cung cấp tên miền hiện có). TTL 300.
- Kiểm tra: `nslookup dev.<tên-miền>` trả về đúng Elastic IP trước khi xin chứng chỉ.

### 3.4. Cài đặt máy chủ

```bash
ssh -i <khoa-quan-tri>.pem ubuntu@<ELASTIC_IP>
# chép deploy/scripts/bootstrap-ec2.sh lên (scp) rồi:
sudo bash bootstrap-ec2.sh      # Docker Engine + compose, swap 2 GB, ufw 22/80/443, tạo /opt/tigernet
exit                            # SSH lại để nhóm docker có hiệu lực
```

Khóa cho CI (trên máy của bạn):

```bash
ssh-keygen -t ed25519 -f tigernet_ci -C "github-actions-tigernet" -N ""
ssh-copy-id -i tigernet_ci.pub ubuntu@<ELASTIC_IP>      # hoặc dán vào ~/.ssh/authorized_keys
ssh-keyscan -t ed25519 <ELASTIC_IP>                      # -> EC2_SSH_KNOWN_HOSTS
```

Dán nội dung `tigernet_ci` vào secret `EC2_SSH_KEY`, rồi xóa file khóa riêng trên máy.

### 3.5. Biến môi trường trên EC2

```bash
cd /opt/tigernet
# lần đầu chưa có file: chép từ repo (hoặc chạy workflow 1 lần với DEPLOY_ENABLED=false rồi scp deploy/.env.example)
nano .env && chmod 600 .env
```

Giá trị cần điền (mẫu đầy đủ ở `deploy/.env.example`):

| Biến | Ghi chú |
|---|---|
| `REGISTRY` | `ghcr.io/nhatpham12` (chữ thường) |
| `DOMAIN` | `dev.<tên-miền>` |
| `LETSENCRYPT_EMAIL` | Email nhận cảnh báo hết hạn chứng chỉ |
| `DB_NAME` | `tigernet_dev` |
| `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Sinh ngẫu nhiên: `openssl rand -base64 24` |
| `JWT_SIGNER_KEY` | `openssl rand -base64 64 \| tr -d '\n'` (>= 64 ký tự) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | App password Gmail nếu cần gửi mail, để trống được |
| `DEMO_PASSWORD` | Mật khẩu chung của 5 tài khoản demo |

Thông tin doanh nghiệp in trên biểu mẫu: chép `backend/company.example.yaml` thành
`/opt/tigernet/deploy/config/application.yaml` và điền (CI không ghi đè file này).

### 3.6. Lần deploy đầu

1. Đặt biến `DEPLOY_ENABLED=true` trên GitHub, vào Actions → **dev CI/CD** → Run workflow (nhánh `dev`).
   CI chép `docker-compose.yml`, `deploy/`, script SQL lên `/opt/tigernet` rồi chạy `deploy.sh`.
   Lúc này proxy chạy **HTTP** (chưa có chứng chỉ): mở `http://dev.<tên-miền>` để kiểm tra.
2. Cấp chứng chỉ:
   ```bash
   cd /opt/tigernet
   # thử trước với máy chủ staging của Let's Encrypt cho chắc: LETSENCRYPT_STAGING=1 trong .env
   ./deploy/scripts/init-letsencrypt.sh
   # thành công thì đặt LETSENCRYPT_STAGING=0, xóa chứng chỉ thử rồi chạy lại:
   #   docker compose --profile tls run --rm --entrypoint certbot certbot delete --cert-name dev.<tên-miền>
   ```
   Script: bật proxy HTTP → `certbot certonly --webroot` → khởi động lại proxy (tự chuyển sang HTTPS, HTTP 301 sang HTTPS)
   → bật container `certbot` gia hạn tự động.
3. Tạo tài khoản demo: `./deploy/scripts/seed-demo-users.sh`
4. Mở `https://dev.<tên-miền>`, đăng nhập `demo_admin`.

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
- Ra ngoài máy: đặt `BACKUP_S3_BUCKET` (bucket bật versioning + lifecycle 30 ngày), EC2 có IAM role ghi bucket,
  cài AWS CLI (`sudo snap install aws-cli --classic`). Thêm: snapshot EBS hằng tuần bằng Amazon Data Lifecycle Manager.
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
- CloudWatch Agent hoặc Uptime Kuma để cảnh báo khi trang không truy cập được.
