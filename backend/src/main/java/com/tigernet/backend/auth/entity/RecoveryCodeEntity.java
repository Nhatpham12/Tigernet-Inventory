package com.tigernet.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Mã khôi phục dùng một lần khi mất điện thoại; chỉ lưu hash BCrypt. */
@Entity
@Getter @Setter
@Table(name = "User_Recovery_Codes")
public class RecoveryCodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "User_ID", nullable = false)
    private Integer userId;

    @Column(name = "Code_Hash", nullable = false)
    private String codeHash;
}
