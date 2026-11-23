-- V7: khoá tài khoản khi nhập sai mật khẩu nhiều lần + thu hồi token (đăng xuất, đổi mật khẩu/vai trò).
-- Áp tay trước khi chạy bản backend mới (prod dùng ddl-auto: validate).
--   Failed_Login_Count: số lần sai liên tiếp, đủ 5 thì khoá Locked_Until = now + 15 phút
--   Token_Version: ghi vào claim "ver" của JWT; tăng lên là mọi token cũ của user mất hiệu lực
ALTER TABLE Users
    ADD COLUMN Failed_Login_Count INT      NOT NULL DEFAULT 0,
    ADD COLUMN Locked_Until       DATETIME NULL,
    ADD COLUMN Token_Version      INT      NOT NULL DEFAULT 0;
