package com.tigernet.backend.partner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// vai trò Nhà cung cấp của một Party
@Entity
@Getter @Setter
@Table(name = "Suppliers")
public class SupplierEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "Party_ID", nullable = false, unique = true)
    private PartyEntity party;

    @Column(name = "Contract_No", length = 100)
    private String contractNo;

    @Column(name = "Tracking_Code", length = 100)
    private String trackingCode;

    @Column(name = "Is_Deleted", nullable = false)
    private Boolean isDeleted = false;
}
