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

    // ghi vào claim "ver" của token; tăng lên là mọi token cũ của user mất hiệu lực (đăng xuất, buộc đăng xuất, đổi vai trò, khoá...)
    @Column(name = "Token_Version", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private int tokenVersion;

    // khoá TOTP (Base32) đã mã hoá AES-GCM bằng MFA_ENC_KEY; có giá trị mà chưa bật = đang cài đặt dở
    @Column(name = "Mfa_Secret")
    private String mfaSecret;

    @Column(name = "Mfa_Enabled", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean mfaEnabled;

    public void revokeTokens() {
        tokenVersion++;
    }
}
