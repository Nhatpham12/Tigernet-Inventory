#!/bin/sh
# Chọn cấu hình nginx: có chứng chỉ cho $DOMAIN thì bật HTTPS, chưa có thì chỉ HTTP.
# Sau đó nạp lại nginx mỗi 6 giờ để nhận chứng chỉ certbot vừa gia hạn.
set -eu
DOMAIN="${DOMAIN:-localhost}"
export DOMAIN
src=/etc/nginx/tigernet
if [ -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
    tpl="$src/https.conf.template"
    echo "tigernet: HTTPS cho ${DOMAIN}"
else
    tpl="$src/http-only.conf.template"
    echo "tigernet: chưa có chứng chỉ cho ${DOMAIN}, chạy HTTP"
fi
envsubst '${DOMAIN}' < "$tpl" > /etc/nginx/conf.d/default.conf
( while :; do sleep 6h; nginx -s reload; done ) &
