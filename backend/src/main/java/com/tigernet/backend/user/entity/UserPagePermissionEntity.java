package com.tigernet.backend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Trang (màn hình) một nhân viên được xem, do Quản trị chọn.
 * Nhân viên chưa có dòng nào thì dùng quyền mặc định theo vai trò (FE giữ ma trận mặc định).
 * Page_Key là đường dẫn trang trên FE, vd "/app/import-receipts".
 */
@Entity
@Getter @Setter
@Table(name = "User_Page_Permissions",
        uniqueConstraints = @UniqueConstraint(name = "UQ_UPP_User_Page", columnNames = {"User_ID", "Page_Key"}))
public class UserPagePermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "User_ID", nullable = false)
    private Integer userId;

    @Column(name = "Page_Key", nullable = false, length = 100)
    private String pageKey;
}
