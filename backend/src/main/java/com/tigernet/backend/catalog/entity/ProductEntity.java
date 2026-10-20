package com.tigernet.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@Table(name = "Products")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Product_Name", nullable = false)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Group_ID", nullable = false)
    private ProductGroupEntity group;

    @Column(name = "Unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "Default_Price_In", nullable = false, precision = 18, scale = 2)
    private BigDecimal defaultPriceIn;

    @Column(name = "Default_Price_Out", nullable = false, precision = 18, scale = 2)
    private BigDecimal defaultPriceOut;

    @Column(name = "Is_Serialized", nullable = false)
    private Boolean isSerialized;

    @Column(name = "Min_Stock_Level", nullable = false)
    private Integer minStockLevel;

    @Column(name = "Image_Path")
    private String imagePath; // object key trên object storage

    @Column(name = "Is_Deleted", nullable = false)
    private Boolean isDeleted = false; // xóa mềm
}
