package com.tigernet.backend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "Users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer userId;

    @Column(name = "Full_Name")
    private String fullName;

    @Column(name = "Email")
    private String email;

    @Column(name = "Is_Active")
    private Boolean isActive;

    @Column(name = "Created_At")
    private LocalDateTime createdAt;

    @Column(name = "Password_Hash")
    private String passwordHash; // mật khẩu được mã hóa và lưu vào DB

    @Column(name = "Username")
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Role_ID")
    private RoleEntity role;
}
