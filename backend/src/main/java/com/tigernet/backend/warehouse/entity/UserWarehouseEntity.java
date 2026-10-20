package com.tigernet.backend.warehouse.entity;

import com.tigernet.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// phân công nhân viên vào kho (N-N)
@Entity
@Getter @Setter
@Table(name = "User_Warehouses")
public class UserWarehouseEntity {
    @EmbeddedId
    private UserWarehouseId id = new UserWarehouseId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_ID")
    private UserEntity user;

    @MapsId("warehouseId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID")
    private WarehouseEntity warehouse;

    @Column(name = "Is_Default", nullable = false)
    private Boolean isDefault; // mỗi user tối đa 1 kho mặc định: kiểm tra ở service

    @Column(name = "Assigned_At", nullable = false)
    private LocalDateTime assignedAt;
}
