package com.tigernet.backend.supplierreturn.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.supplierreturn.enums.SupplierReturnResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Supplier_Return_Details")
public class SupplierReturnDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Supplier_Return_ID", nullable = false)
    private SupplierReturnEntity supplierReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Inventory_ID")
    private InventoryEntity inventory; // đơn vị hàng serial được gửi đi

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Serial")
    private String serial;

    @Enumerated(EnumType.STRING)
    @Column(name = "Result", length = 20)
    private SupplierReturnResult result; // null khi NCC chưa xử lý xong
}
