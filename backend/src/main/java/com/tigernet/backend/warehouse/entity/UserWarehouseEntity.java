package com.tigernet.backend.warehouse.entity;

import com.tigernet.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// phân công nhân viên vào kho (N-N); mỗi cặp (user, kho) chỉ một dòng
@Entity
@Getter @Setter
@Table(name = "User_Warehouses",
        uniqueConstraints = @UniqueConstraint(name = "UQ_UW_User_Warehouse", columnNames = {"User_ID", "Warehouse_ID"}))
public class UserWarehouseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_ID", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @Column(name = "Is_Default", nullable = false)
    private Boolean isDefault; // mỗi user tối đa 1 kho mặc định: kiểm tra ở service

    @Column(name = "Assigned_At", nullable = false)
    private LocalDateTime assignedAt;
}
