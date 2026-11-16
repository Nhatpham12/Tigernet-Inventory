#!/usr/bin/env bash
# Chạy MỘT LẦN trên EC2 mới (Ubuntu 24.04 LTS) với quyền sudo:
#   curl -fsSL https://raw.githubusercontent.com/<repo>/dev/deploy/scripts/bootstrap-ec2.sh | sudo bash
# hoặc chép file lên rồi: sudo bash bootstrap-ec2.sh
# Cài Docker Engine + compose plugin, tạo swap 2 GB, thư mục /opt/tigernet, bật tường lửa ufw.
set -euo pipefail

DEPLOY_USER="${DEPLOY_USER:-ubuntu}"

apt-get update
apt-get install -y ca-certificates curl gnupg ufw awscli || apt-get install -y ca-certificates curl gnupg ufw

# Docker Engine từ kho chính thức của Docker
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
usermod -aG docker "$DEPLOY_USER"

# Swap 2 GB: MySQL + 2 JVM trên máy 2–4 GB RAM
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

mkdir -p /opt/tigernet/backups
chown -R "$DEPLOY_USER":"$DEPLOY_USER" /opt/tigernet

ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

echo "Xong. Đăng xuất rồi SSH lại để nhóm docker có hiệu lực cho $DEPLOY_USER."
