package com.tigernet.backend.outbound.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
@Table(name = "Export_Receipt_Details")
public class ExportReceiptDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Export_Receipt_ID", nullable = false)
    private ExportReceiptEntity exportReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Inventory_ID")
    private InventoryEntity inventory; // đơn vị cụ thể (hàng serial)

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Price_Out", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceOut;

    @Column(name = "Serial")
    private String serial;

    @Column(name = "Warranty_Months")
    private Integer warrantyMonths;

    @Column(name = "Warranty_Expiry")
    private LocalDate warrantyExpiry; // = Date_Out + Warranty_Months

    @Column(name = "Line_Total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal; // = Quantity × Price_Out
}
