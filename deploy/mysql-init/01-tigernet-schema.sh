#!/bin/bash
# Chạy MỘT LẦN khi volume MySQL còn trống (docker-entrypoint-initdb.d).
# Dựng schema + dữ liệu danh mục từ docs/Database_Script_v7.sql, đổi tên database "tigernet"
# thành $MYSQL_DATABASE (mặc định tigernet_dev), giống database/dev/create-dev-db.ps1.
set -euo pipefail
db="${MYSQL_DATABASE:?MYSQL_DATABASE chưa đặt}"
echo "tigernet: dựng schema cho database ${db}"
{
  sed -E "s/^([[:space:]]*(-- )?(DROP DATABASE IF EXISTS|CREATE DATABASE IF NOT EXISTS|USE))[[:space:]]+tigernet\b/\1 ${db}/" \
      /schema/Database_Script_v7.sql
  echo "SET FOREIGN_KEY_CHECKS = 1;"
  echo "USE \`${db}\`;"
  cat /schema/V6__user_page_permissions.sql
} | mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}"
