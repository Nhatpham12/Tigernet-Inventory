-- =====================================================================
-- TIGERNET — Control Plane (1 database dùng chung, do công ty vận hành)
-- Phase 2: chỉ cần khi chạy SaaS nhiều khách hàng trên cùng hạ tầng.
-- Không chứa dữ liệu nghiệp vụ kho của khách hàng.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE tenants (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    subdomain     VARCHAR(63)  NOT NULL,             -- khachhangA.tenapp.com
    status        VARCHAR(20)  NOT NULL DEFAULT 'TRIAL',
    plan_code     VARCHAR(30)  NOT NULL,
    trial_ends_at DATETIME     NULL,
    contact_name  VARCHAR(150) NULL,
    contact_email VARCHAR(150) NULL,
    contact_phone VARCHAR(20)  NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenants_code (code),
    UNIQUE KEY uk_tenants_subdomain (subdomain),
    CONSTRAINT ck_tenants_status CHECK (status IN ('PROVISIONING','TRIAL','ACTIVE','SUSPENDED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Thông tin kết nối DB của tenant — mật khẩu mã hoá (AES-GCM tầng ứng dụng hoặc Vault)
CREATE TABLE tenant_databases (
    tenant_id             BIGINT        NOT NULL,
    db_host               VARCHAR(255)  NOT NULL,
    db_port               INT           NOT NULL DEFAULT 3306,
    db_name               VARCHAR(64)   NOT NULL,
    db_username           VARCHAR(64)   NOT NULL,
    db_password_encrypted VARBINARY(512) NOT NULL,
    encryption_key_id     VARCHAR(50)   NOT NULL,    -- phục vụ xoay khoá
    schema_version        VARCHAR(20)   NULL,        -- phiên bản Flyway gần nhất
    last_migrated_at      DATETIME      NULL,
    last_migration_ok     BOOLEAN       NULL,
    PRIMARY KEY (tenant_id),
    UNIQUE KEY uk_tenant_databases_db (db_host, db_port, db_name),
    CONSTRAINT fk_tenant_databases_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tenant_branding (
    tenant_id       BIGINT       NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    logo_path       VARCHAR(500) NULL,
    primary_color   CHAR(7)      NULL,               -- #RRGGBB
    secondary_color CHAR(7)      NULL,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id),
    CONSTRAINT fk_tenant_branding_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE licenses (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT      NOT NULL,
    edition          VARCHAR(20) NOT NULL,           -- SAAS, ON_PREMISE
    license_key_hash CHAR(64)    NOT NULL,
    max_users        INT         NULL,
    issued_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       DATETIME    NOT NULL,
    last_check_at    DATETIME    NULL,               -- on-premise gọi về mỗi 24h
    last_check_ip    VARCHAR(45) NULL,
    is_revoked       BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_licenses_key (license_key_hash),
    KEY idx_licenses_tenant (tenant_id),
    CONSTRAINT fk_licenses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT ck_licenses_edition CHECK (edition IN ('SAAS','ON_PREMISE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Nhật ký backup / migration / provisioning của đội vận hành
CREATE TABLE tenant_operations (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   BIGINT       NOT NULL,
    op_type     VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    detail      VARCHAR(1000) NULL,
    started_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_tenant_operations_tenant (tenant_id, started_at),
    CONSTRAINT fk_tenant_operations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT ck_tenant_operations_type CHECK (op_type IN ('PROVISION','MIGRATE','BACKUP','RESTORE')),
    CONSTRAINT ck_tenant_operations_status CHECK (status IN ('RUNNING','SUCCESS','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
