-- Trang (màn hình FE) từng nhân viên được xem, do Quản trị phân quyền.
-- Backend dùng ddl-auto=update nên bảng tự được tạo khi khởi động; script này để dựng DB thủ công.
CREATE TABLE IF NOT EXISTS User_Page_Permissions (
    ID       INT          NOT NULL AUTO_INCREMENT,
    User_ID  INT          NOT NULL,
    Page_Key VARCHAR(100) NOT NULL,
    CONSTRAINT PK_User_Page_Permissions PRIMARY KEY (ID),
    CONSTRAINT UQ_UPP_User_Page UNIQUE (User_ID, Page_Key),
    CONSTRAINT FK_UPP_User FOREIGN KEY (User_ID) REFERENCES Users(ID)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;
