package com.tigernet.backend.rental.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Return_Receipt_Details")
public class ReturnReceiptDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Return_Receipt_ID", nullable = false)
    private ReturnReceiptEntity returnReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Rental_Detail_ID", nullable = false)
    private RentalReceiptDetailEntity rentalDetail; // dòng thuê gốc được trả

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

    @Column(name = "Condition_Note")
    private String conditionNote;
}
