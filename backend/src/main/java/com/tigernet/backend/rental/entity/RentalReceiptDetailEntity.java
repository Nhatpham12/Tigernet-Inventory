package com.tigernet.backend.rental.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@Table(name = "Rental_Receipt_Details")
public class RentalReceiptDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Rental_Receipt_ID", nullable = false)
    private RentalReceiptEntity rentalReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Inventory_ID")
    private InventoryEntity inventory;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Rental_Price", nullable = false, precision = 18, scale = 2)
    private BigDecimal rentalPrice;

    @Column(name = "Serial")
    private String serial;

    @Column(name = "Is_Returned", nullable = false)
    private Boolean isReturned = false;
}
