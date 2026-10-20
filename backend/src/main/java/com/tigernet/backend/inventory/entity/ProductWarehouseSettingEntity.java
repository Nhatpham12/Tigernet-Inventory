package com.tigernet.backend.inventory.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// ngưỡng tồn riêng theo (SP, kho); Min_Stock NULL = dùng Products.Min_Stock_Level
@Entity
@Getter @Setter
@Table(name = "Product_Warehouse_Settings")
public class ProductWarehouseSettingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @Column(name = "Min_Stock")
    private Integer minStock;

    @Column(name = "Max_Stock")
    private Integer maxStock;

    @Column(name = "Reorder_Point")
    private Integer reorderPoint;
}
