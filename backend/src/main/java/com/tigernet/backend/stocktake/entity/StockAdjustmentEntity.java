package com.tigernet.backend.stocktake.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// sinh tự động khi chốt phiếu kiểm kê, không tạo tay
@Entity
@Getter @Setter
@Table(name = "Stock_Adjustments")
public class StockAdjustmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Stocktake_ID", nullable = false)
    private StocktakeEntity stocktake;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @Column(name = "Adjust_Qty", nullable = false)
    private Integer adjustQty; // ±

    @Column(name = "Reason")
    private String reason;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
}
