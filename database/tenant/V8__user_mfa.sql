-- V8: xác thực 2 bước (TOTP) cho Quản trị. Áp tay trước khi chạy bản backend mới.
-- Mfa_Secret: khoá TOTP Base32 đã mã hoá AES-GCM bằng MFA_ENC_KEY (không bao giờ lưu dạng rõ)
ALTER TABLE Users
    ADD COLUMN Mfa_Secret  VARCHAR(255) NULL,
    ADD COLUMN Mfa_Enabled BOOLEAN      NOT NULL DEFAULT FALSE;

-- 8 mã khôi phục dùng một lần khi mất điện thoại; chỉ lưu hash BCrypt
CREATE TABLE User_Recovery_Codes (
    ID        INT          NOT NULL AUTO_INCREMENT,
    User_ID   INT          NOT NULL,
    Code_Hash VARCHAR(100) NOT NULL,
    CONSTRAINT PK_User_Recovery_Codes PRIMARY KEY (ID),
    CONSTRAINT FK_User_Recovery_Codes_User FOREIGN KEY (User_ID) REFERENCES Users(ID) ON DELETE CASCADE
) ENGINE=InnoDB;
