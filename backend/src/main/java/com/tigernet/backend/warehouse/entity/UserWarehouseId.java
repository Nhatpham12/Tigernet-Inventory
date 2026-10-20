package com.tigernet.backend.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

// khóa chính ghép (User_ID, Warehouse_ID)
@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserWarehouseId implements Serializable {
    @Column(name = "User_ID")
    private Integer userId;

    @Column(name = "Warehouse_ID")
    private Integer warehouseId;
}
