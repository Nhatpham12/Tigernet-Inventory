package com.tigernet.backend.inventory.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// cache tổng tồn theo (SP, kho); nguồn sự thật vẫn là Inventory
@Entity
@Getter @Setter
@Table(name = "Stock_Summary")
public class StockSummaryEntity {
    @EmbeddedId
    private StockSummaryId id = new StockSummaryId();

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID")
    private ProductEntity product;

    @MapsId("warehouseId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID")
    private WarehouseEntity warehouse;

    @Column(name = "Qty_On_Hand", nullable = false)
    private Integer qtyOnHand = 0;

    @Column(name = "Qty_Reserved", nullable = false)
    private Integer qtyReserved = 0;

    @Column(name = "Qty_Rented", nullable = false)
    private Integer qtyRented = 0;

    @Column(name = "Qty_Damaged", nullable = false)
    private Integer qtyDamaged = 0;

    @Column(name = "Qty_In_Transit", nullable = false)
    private Integer qtyInTransit = 0;

    @Column(name = "Last_Updated", nullable = false)
    private LocalDateTime lastUpdated;
}
