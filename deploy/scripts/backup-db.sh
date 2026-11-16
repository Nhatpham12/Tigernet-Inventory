#!/usr/bin/env bash
# Sao lưu database bằng mysqldump vào /opt/tigernet/backups (giữ BACKUP_KEEP_DAYS ngày).
# Nếu .env có BACKUP_S3_BUCKET thì đẩy thêm lên S3 (cần AWS CLI + IAM role của EC2).
# Cron gợi ý (2 giờ sáng hằng ngày):  0 2 * * * /opt/tigernet/deploy/scripts/backup-db.sh >> /opt/tigernet/backups/backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; . ./.env; set +a

db="${DB_NAME:-tigernet_dev}"
dir="backups"
mkdir -p "$dir"
file="$dir/${db}_$(date +%Y%m%d_%H%M%S).sql.gz"

if ! docker compose ps --status running mysql | grep -q mysql; then
  echo "MySQL chưa chạy, bỏ qua sao lưu"; exit 0
fi

docker compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
  mysqldump -uroot --single-transaction --routines --triggers --no-tablespaces --databases "$db" \
  | gzip > "$file"
echo "Đã sao lưu: $file ($(du -h "$file" | cut -f1))"

find "$dir" -name "${db}_*.sql.gz" -mtime +"${BACKUP_KEEP_DAYS:-14}" -delete

if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
  aws s3 cp "$file" "s3://${BACKUP_S3_BUCKET}/tigernet/$(basename "$file")" --only-show-errors
  echo "Đã đẩy lên s3://${BACKUP_S3_BUCKET}/tigernet/"
fi

# Khôi phục:  gunzip -c backups/<file>.sql.gz | docker compose exec -T -e MYSQL_PWD=... mysql mysql -uroot
