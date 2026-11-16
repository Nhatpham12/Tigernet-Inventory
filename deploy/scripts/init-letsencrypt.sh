#!/usr/bin/env bash
# Cấp chứng chỉ Let's Encrypt LẦN ĐẦU cho $DOMAIN (webroot qua container proxy đang chạy HTTP).
# Điều kiện: DNS của DOMAIN đã trỏ về Elastic IP của EC2, security group mở cổng 80 và 443.
# Gia hạn sau đó do container certbot (profile tls) tự làm mỗi 12 giờ.
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; . ./.env; set +a

: "${DOMAIN:?Đặt DOMAIN trong .env}"
: "${LETSENCRYPT_EMAIL:?Đặt LETSENCRYPT_EMAIL trong .env}"
[ "$DOMAIN" != "localhost" ] || { echo "DOMAIN=localhost: không cấp được chứng chỉ"; exit 1; }

staging=()
[ "${LETSENCRYPT_STAGING:-0}" = "1" ] && staging=(--staging)

echo "==> Bật proxy ở chế độ HTTP"
docker compose up -d proxy

echo "==> Xin chứng chỉ cho $DOMAIN"
docker compose --profile tls run --rm --entrypoint certbot certbot \
  certonly --webroot -w /var/www/certbot -d "$DOMAIN" \
  --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email --non-interactive "${staging[@]}"

echo "==> Khởi động lại proxy để bật HTTPS, chạy certbot gia hạn tự động"
docker compose restart proxy
docker compose --profile tls up -d certbot
echo "==> Xong: https://$DOMAIN"
