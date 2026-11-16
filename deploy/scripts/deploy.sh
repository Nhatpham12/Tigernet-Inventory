#!/usr/bin/env bash
# Deploy một phiên bản image lên máy chủ (CI gọi qua SSH, hoặc chạy tay để rollback).
#   ./deploy/scripts/deploy.sh sha-1a2b3c4     # deploy tag cụ thể
#   ./deploy/scripts/deploy.sh --rollback      # quay về tag đã chạy trước đó
# Chạy trong /opt/tigernet. Trước khi đổi phiên bản: sao lưu DB. Sau khi đổi: kiểm tra sức khỏe,
# hỏng thì tự quay về tag cũ.
set -euo pipefail
cd "$(dirname "$0")/../.."

[ -f .env ] || { echo "Thiếu .env (chép từ deploy/.env.example)"; exit 1; }

current_tag() { grep -E '^IMAGE_TAG=' .env | tail -1 | cut -d= -f2-; }
set_tag() {
  if grep -qE '^IMAGE_TAG=' .env; then sed -i -E "s|^IMAGE_TAG=.*|IMAGE_TAG=$1|" .env
  else echo "IMAGE_TAG=$1" >> .env; fi
}

prev="$(current_tag)"
if [ "${1:-}" = "--rollback" ]; then
  target="$(cat .previous_image_tag 2>/dev/null || true)"
  [ -n "$target" ] || { echo "Không có tag trước đó để rollback"; exit 1; }
else
  target="${1:?Cần tag image, ví dụ sha-1a2b3c4}"
fi

# compose đọc DOMAIN để quyết định có chạy certbot không
domain="$(grep -E '^DOMAIN=' .env | tail -1 | cut -d= -f2-)"
profiles=()
[ -n "$domain" ] && [ "$domain" != "localhost" ] && profiles=(--profile tls)

health() {
  for _ in $(seq 1 36); do
    if docker compose exec -T proxy wget -q -O /dev/null http://backend:8080/v3/api-docs 2>/dev/null \
       && docker compose exec -T proxy wget -q -O /dev/null http://web/ 2>/dev/null \
       && docker compose exec -T proxy sh -c 'wget -S -O /dev/null http://bff:8081/bff/session 2>&1 | grep -q "HTTP/"'; then
      return 0
    fi
    sleep 5
  done
  return 1
}

echo "==> Sao lưu database trước khi deploy"
./deploy/scripts/backup-db.sh || echo "CẢNH BÁO: sao lưu lỗi (lần chạy đầu khi chưa có DB thì bỏ qua)"

echo "==> Deploy $target (đang chạy: ${prev:-chưa có})"
set_tag "$target"
docker compose "${profiles[@]}" pull backend bff web \
  || echo "CẢNH BÁO: không kéo được image (hết hạn đăng nhập GHCR?), dùng image có sẵn trên máy"
docker compose "${profiles[@]}" up -d --remove-orphans

if health; then
  [ -n "$prev" ] && [ "$prev" != "$target" ] && echo "$prev" > .previous_image_tag
  docker image prune -f >/dev/null || true   # chỉ xóa image mồ côi, giữ bản cũ để rollback
  echo "==> OK: đang chạy $target"
else
  echo "==> LỖI: hệ thống không lên sau 3 phút, quay về ${prev}"
  docker compose logs --tail=80 backend bff || true
  if [ -n "$prev" ] && [ "$prev" != "$target" ]; then
    set_tag "$prev"
    docker compose "${profiles[@]}" up -d --remove-orphans
  fi
  exit 1
fi
