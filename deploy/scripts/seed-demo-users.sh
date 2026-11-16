#!/usr/bin/env bash
# Tạo (hoặc đặt lại) 5 tài khoản demo, mỗi chức danh một tài khoản, mật khẩu chung = DEMO_PASSWORD trong .env.
#   demo_admin (ADMIN), demo_manager (MANAGER), demo_accountant (ACCOUNTANT),
#   demo_warehouse (WAREHOUSE, kho MAIN), demo_sales (SALES, kho MAIN)
# Chạy lại bao nhiêu lần cũng được (upsert theo Username). Chỉ dùng cho môi trường dev/demo.
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; . ./.env; set +a

: "${DEMO_PASSWORD:?Đặt DEMO_PASSWORD trong .env}"
db="${DB_NAME:-tigernet_dev}"

# BCrypt cost 10 (Spring BCryptPasswordEncoder đọc được tiền tố \$2y\$). Mật khẩu đi qua stdin, không lộ trên dòng lệnh.
hash="$(printf '%s' "$DEMO_PASSWORD" | docker run --rm -i httpd:2.4-alpine htpasswd -niBC 10 demo | cut -d: -f2 | tr -d '\r\n')"
[[ "$hash" == \$2* ]] || { echo "Không tạo được BCrypt hash"; exit 1; }

docker compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql -uroot --default-character-set=utf8mb4 "$db" <<SQL
SET @h := '${hash}';
INSERT INTO Users (Username, Password_Hash, Full_Name, Role_ID, Is_Active)
SELECT u.username, @h, u.full_name, r.ID, TRUE
FROM (
  SELECT 'demo_admin' AS username, 'Demo Quản trị' AS full_name, 'ADMIN' AS role UNION ALL
  SELECT 'demo_manager',    'Demo Quản lý',  'MANAGER'    UNION ALL
  SELECT 'demo_accountant', 'Demo Kế toán',  'ACCOUNTANT' UNION ALL
  SELECT 'demo_warehouse',  'Demo Thủ kho',  'WAREHOUSE'  UNION ALL
  SELECT 'demo_sales',      'Demo Bán hàng', 'SALES'
) u JOIN Roles r ON r.Role_Name = u.role
ON DUPLICATE KEY UPDATE Password_Hash = VALUES(Password_Hash), Role_ID = VALUES(Role_ID),
                        Full_Name = VALUES(Full_Name), Is_Active = TRUE;

INSERT IGNORE INTO User_Warehouses (User_ID, Warehouse_ID, Is_Default)
SELECT u.ID, w.ID, TRUE FROM Users u JOIN Warehouses w ON w.Warehouse_Code = 'MAIN'
WHERE u.Username IN ('demo_warehouse', 'demo_sales');

SELECT u.Username, r.Role_Name, IFNULL(GROUP_CONCAT(w.Warehouse_Code), '-') AS Kho
FROM Users u JOIN Roles r ON r.ID = u.Role_ID
LEFT JOIN User_Warehouses uw ON uw.User_ID = u.ID LEFT JOIN Warehouses w ON w.ID = uw.Warehouse_ID
WHERE u.Username LIKE 'demo\\_%' GROUP BY u.Username, r.Role_Name ORDER BY u.Username;
SQL
