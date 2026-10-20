package com.tigernet.backend.warehouse.entity;

import com.tigernet.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "Warehouses")
public class WarehouseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Warehouse_Code", nullable = false, unique = true, length = 20)
    private String warehouseCode; // dùng làm tiền tố số phiếu, VD PN-MAIN-2026-0001

    @Column(name = "Warehouse_Name", nullable = false, unique = true, length = 150)
    private String warehouseName;

    @Column(name = "Address", nullable = false)
    private String address;

    @Column(name = "Phone", length = 50)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Manager_ID")
    private UserEntity manager;

    @Column(name = "Is_Active", nullable = false)
    private Boolean isActive;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
}
