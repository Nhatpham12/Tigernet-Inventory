-- V7: thu hồi token (đăng xuất, đổi mật khẩu/vai trò, khoá user) + khoá đăng nhập theo (tài khoản, IP).
-- Áp tay trước khi chạy bản backend mới (prod dùng ddl-auto: validate).

-- Token_Version: ghi vào claim "ver" của JWT; tăng lên là mọi token cũ của user mất hiệu lực
ALTER TABLE Users
    ADD COLUMN Token_Version INT NOT NULL DEFAULT 0;

-- Sai mật khẩu 5 lần liên tiếp từ cùng 1 IP thì khoá cặp (User_ID, Ip) 15 phút.
-- Khoá theo cặp để kẻ xấu cố tình nhập sai chỉ khoá được IP của chính họ.
CREATE TABLE Login_Failures (
    User_ID      INT         NOT NULL,
    Ip           VARCHAR(45) NOT NULL,
    Failed_Count INT         NOT NULL DEFAULT 0,
    Locked_Until DATETIME    NULL,
    Updated_At   DATETIME    NOT NULL,
    CONSTRAINT PK_Login_Failures PRIMARY KEY (User_ID, Ip),
    CONSTRAINT FK_Login_Failures_User FOREIGN KEY (User_ID) REFERENCES Users(ID) ON DELETE CASCADE,
    INDEX IX_Login_Failures_Updated (Updated_At)
) ENGINE=InnoDB;
