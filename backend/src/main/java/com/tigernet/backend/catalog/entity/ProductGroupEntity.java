package com.tigernet.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Product_Groups")
public class ProductGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Group_Name", nullable = false, unique = true, length = 100)
    private String groupName;

    @Column(name = "Is_Deleted", nullable = false)
    private Boolean isDeleted = false; // xóa mềm
}
