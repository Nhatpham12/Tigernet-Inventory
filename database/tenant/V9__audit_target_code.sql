-- V9: số giấy tờ của đối tượng trong nhật ký (PN-..., PX-..., CK-..., THU-/CHI-...), để Quản trị đọc và tìm theo số phiếu.
-- Backend chạy ddl-auto=update nên tự thêm cột khi deploy; file này để tạo tay trên DB không dùng Hibernate.
ALTER TABLE Audit_Logs ADD COLUMN Target_Code VARCHAR(50) NULL AFTER Target_ID;
CREATE INDEX IX_Audit_Target_Code ON Audit_Logs (Target_Code);
