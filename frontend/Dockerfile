# syntax=docker/dockerfile:1.7
# ---- Build: Flutter Web ----
FROM debian:bookworm-slim AS build
ARG FLUTTER_VERSION=3.47.9
# Rỗng = gọi BFF cùng tên miền với trang web (nginx chuyển /bff/ sang BFF).
ARG BFF_BASE_URL=
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl git unzip xz-utils \
 && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL "https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_${FLUTTER_VERSION}-stable.tar.xz" \
      | tar -xJ -C /opt \
 && git config --global --add safe.directory /opt/flutter
ENV PATH="/opt/flutter/bin:${PATH}" \
    FLUTTER_SUPPRESS_ANALYTICS=true
RUN flutter config --no-analytics --enable-web >/dev/null && flutter precache --web
WORKDIR /src
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get
COPY . .
RUN flutter build web --release --no-web-resources-cdn \
      --dart-define=BFF_BASE_URL=${BFF_BASE_URL}

# ---- Runtime: nginx phục vụ file tĩnh ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/build/web /usr/share/nginx/html
EXPOSE 80
