package com.tigernet.backend.transfer.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Stock_Transfer_Details")
public class StockTransferDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Stock_Transfer_ID", nullable = false)
    private StockTransferEntity stockTransfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Inventory_ID")
    private InventoryEntity inventory;

    @Column(name = "Qty_Sent", nullable = false)
    private Integer qtySent;

    @Column(name = "Qty_Received")
    private Integer qtyReceived;

    @Column(name = "Serial")
    private String serial;

    @Column(name = "Note")
    private String note;
}
