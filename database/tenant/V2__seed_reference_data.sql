-- =====================================================================
-- TIGERNET — Dữ liệu chuẩn nạp khi tạo tenant mới
-- Tài khoản quản trị đầu tiên KHÔNG seed ở đây (tránh mật khẩu mặc định);
-- ứng dụng tạo khi provisioning tenant, mật khẩu ngẫu nhiên gửi qua email.
-- =====================================================================

INSERT INTO roles (code, name, description) VALUES
    ('STOREKEEPER', 'Thủ kho',           'Hàng vào/ra kho, serial, kiểm kê'),
    ('ACCOUNTANT',  'Kế toán',           'Thanh toán, công nợ, đối soát'),
    ('SALES',       'Bán hàng',          'Khách hàng, phiếu xuất bán, phiếu thuê'),
    ('MANAGER',     'Quản lý',           'Duyệt phiếu, báo cáo, giám sát'),
    ('ADMIN',       'Quản trị hệ thống', 'Người dùng, vai trò, cấu hình');

INSERT INTO vat_rates (code, name, rate_percent, is_default) VALUES
    ('KCT',   'Không chịu thuế', 0.00,  FALSE),
    ('VAT0',  'Thuế suất 0%',    0.00,  FALSE),
    ('VAT5',  'Thuế suất 5%',    5.00,  FALSE),
    ('VAT8',  'Thuế suất 8%',    8.00,  FALSE),
    ('VAT10', 'Thuế suất 10%',   10.00, TRUE);

INSERT INTO system_settings (setting_key, setting_value, value_type, description) VALUES
    ('rental.expiry_warning_days',   '7',  'INT', 'Cảnh báo trước khi hết hạn thuê (ngày)'),
    ('rental.penalty_min_overdue_days', '10', 'INT', 'Quá hạn từ số ngày này trở lên thì tính phạt'),
    ('auth.max_failed_logins',       '5',  'INT', 'Số lần đăng nhập sai liên tiếp trước khi khoá'),
    ('auth.lock_minutes',            '15', 'INT', 'Thời gian khoá tài khoản (phút)'),
    ('auth.otp_ttl_minutes',         '10', 'INT', 'Hiệu lực OTP đặt lại mật khẩu (phút)');

INSERT INTO warehouses (code, name) VALUES
    ('KHO-01', 'Kho chính');
