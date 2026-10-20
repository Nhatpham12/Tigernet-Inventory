package com.tigernet.backend.audit.entity;

import com.tigernet.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "Audit_Logs")
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_ID")
    private UserEntity user;

    @Column(name = "Action", nullable = false, length = 50)
    private String action; // LOGIN, CREATE_IMPORT, PAYMENT, PRINT...

    @Column(name = "Target_Table", length = 50)
    private String targetTable;

    @Column(name = "Target_ID")
    private Integer targetId;

    @Column(name = "Detail", columnDefinition = "JSON")
    private String detail; // chuỗi JSON hợp lệ

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
}
