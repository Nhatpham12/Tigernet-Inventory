package com.tigernet.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Số lần nhập sai mật khẩu liên tiếp của một tài khoản từ một IP. Khoá theo cặp (tài khoản, IP) để
 * kẻ xấu cố tình nhập sai chỉ khoá được chính IP của họ, người dùng thật ở IP khác vẫn đăng nhập được.
 */
@Entity
@Getter @Setter
@Table(name = "Login_Failures")
public class LoginFailureEntity {
    @EmbeddedId
    private Key id;

    @Column(name = "Failed_Count", nullable = false)
    private int failedCount;

    @Column(name = "Locked_Until")
    private LocalDateTime lockedUntil;

    @Column(name = "Updated_At", nullable = false)
    private LocalDateTime updatedAt;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        @Column(name = "User_ID", nullable = false)
        private Integer userId;

        @Column(name = "Ip", nullable = false, length = 45)
        private String ip;
    }
}
