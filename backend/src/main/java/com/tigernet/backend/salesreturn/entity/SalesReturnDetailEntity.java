package com.tigernet.backend.salesreturn.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.salesreturn.enums.ReturnCondition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Sales_Return_Details")
public class SalesReturnDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Sales_Return_ID", nullable = false)
    private SalesReturnEntity salesReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Export_Detail_ID", nullable = false)
    private ExportReceiptDetailEntity exportDetail; // dòng bán gốc

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Inventory_ID")
    private InventoryEntity inventory;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Serial")
    private String serial;

    @Enumerated(EnumType.STRING)
    @Column(name = "Return_Condition", nullable = false, length = 20)
    private ReturnCondition returnCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "Restock_Status", length = 20)
    private InventoryStatus restockStatus;
}
