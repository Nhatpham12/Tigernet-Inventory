#!/usr/bin/env bash
# Chạy MỘT LẦN trên máy chủ mới (Ubuntu 22.04/24.04 hoặc Debian 12) với quyền sudo:
#   sudo bash bootstrap-server.sh
# Cài Docker Engine + compose plugin, tạo swap 2 GB (nếu chưa có swap), thư mục /opt/tigernet, bật tường lửa ufw.
# DEPLOY_USER: user sẽ chạy docker và nhận deploy từ CI (mặc định là user gọi sudo).
set -euo pipefail

[ "$(id -u)" = 0 ] || { echo "Chạy bằng sudo"; exit 1; }
DEPLOY_USER="${DEPLOY_USER:-${SUDO_USER:-}}"
[ -n "$DEPLOY_USER" ] && [ "$DEPLOY_USER" != root ] && id "$DEPLOY_USER" >/dev/null \
  || { echo "Đặt DEPLOY_USER=<user không phải root>, vd: sudo DEPLOY_USER=deploy bash $0"; exit 1; }

. /etc/os-release
case "$ID" in
  ubuntu|debian) ;;
  *) echo "Chỉ hỗ trợ Ubuntu/Debian (máy này: $ID)"; exit 1 ;;
esac

apt-get update
apt-get install -y ca-certificates curl gnupg ufw rsync

# Docker Engine từ kho chính thức của Docker
if ! command -v docker >/dev/null; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL "https://download.docker.com/linux/$ID/gpg" -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/$ID $VERSION_CODENAME stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
systemctl enable --now docker
usermod -aG docker "$DEPLOY_USER"

# Swap 2 GB: MySQL + 2 JVM trên máy 2–4 GB RAM (bỏ qua nếu máy đã có swap)
if [ -z "$(swapon --show --noheadings)" ]; then
  fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

mkdir -p /opt/tigernet/backups
chown -R "$DEPLOY_USER":"$DEPLOY_USER" /opt/tigernet

# Mở đúng cổng SSH đang chạy (máy được cấp có thể không dùng cổng 22) trước khi bật ufw,
# nếu không sẽ tự khoá mình ra ngoài.
ssh_port="$(sshd -T 2>/dev/null | awk '$1=="port"{print $2; exit}' || true)"
# dự phòng: cổng mà tiến trình sshd đang nghe
[ -n "$ssh_port" ] || ssh_port="$(ss -Htlnp 2>/dev/null | awk '/"sshd"/{n=split($4,a,":"); print a[n]; exit}' || true)"
if [ -n "$ssh_port" ]; then
  ufw allow "$ssh_port/tcp"
  ufw allow 80/tcp
  ufw allow 443/tcp
  ufw --force enable
  echo "ufw: mở SSH cổng $ssh_port, 80, 443."
else
  echo "CẢNH BÁO: không xác định được cổng SSH nên KHÔNG bật ufw (tránh tự khoá). Tự chạy:"
  echo "  sudo ufw allow <cổng-ssh>/tcp && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw enable"
fi

echo "Xong. Đăng xuất rồi SSH lại để nhóm docker có hiệu lực cho $DEPLOY_USER."
