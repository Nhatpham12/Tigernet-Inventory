package com.tigernet.backend.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// vị trí kệ/ô trong kho
@Entity
@Getter @Setter
@Table(name = "Warehouse_Locations")
public class WarehouseLocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @Column(name = "Location_Code", nullable = false, length = 30)
    private String locationCode;

    @Column(name = "Description")
    private String description;
}
