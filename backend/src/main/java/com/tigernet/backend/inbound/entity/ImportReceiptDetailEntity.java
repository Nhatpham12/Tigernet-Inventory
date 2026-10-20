package com.tigernet.backend.inbound.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@Table(name = "Import_Receipt_Details")
public class ImportReceiptDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Import_Receipt_ID", nullable = false)
    private ImportReceiptEntity importReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Price_In", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceIn;

    @Column(name = "Serial")
    private String serial;

    @Column(name = "Tracking_Code", length = 100)
    private String trackingCode;

    @Column(name = "Line_Total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal; // = Quantity × Price_In
}
