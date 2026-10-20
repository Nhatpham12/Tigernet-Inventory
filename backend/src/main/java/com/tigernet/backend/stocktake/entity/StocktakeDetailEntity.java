package com.tigernet.backend.stocktake.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Stocktake_Details")
public class StocktakeDetailEntity {
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

    @Column(name = "System_Qty", nullable = false)
    private Integer systemQty; // tồn trên hệ thống tại kho của phiếu

    @Column(name = "Actual_Qty", nullable = false)
    private Integer actualQty;

    @Column(name = "Difference", nullable = false)
    private Integer difference; // = Actual − System
}
