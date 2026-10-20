package com.tigernet.backend.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

// khóa chính ghép (Product_ID, Warehouse_ID)
@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockSummaryId implements Serializable {
    @Column(name = "Product_ID")
    private Integer productId;

    @Column(name = "Warehouse_ID")
    private Integer warehouseId;
}
