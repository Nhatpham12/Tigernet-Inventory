package com.tigernet.backend.inventory.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.enums.MovementType;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// sổ cái biến động tồn: chỉ ghi thêm, không sửa/xóa
@Entity
@Getter @Setter
@Table(name = "Stock_Movements")
public class StockMovementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "Movement_Type", nullable = false, length = 10)
    private MovementType movementType;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity; // luôn dương, chiều theo movementType

    @Column(name = "Balance_After", nullable = false)
    private Integer balanceAfter; // Stock_Summary.Qty_On_Hand ngay sau biến động

    @Column(name = "Reference_Type", nullable = false, length = 30)
    private String referenceType; // tên bảng phiếu, VD Import_Receipts

    @Column(name = "Reference_ID", nullable = false)
    private Integer referenceId;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
}
