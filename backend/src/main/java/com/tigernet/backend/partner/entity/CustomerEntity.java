package com.tigernet.backend.partner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// vai trò Khách hàng của một Party
@Entity
@Getter @Setter
@Table(name = "Customers")
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "Party_ID", nullable = false, unique = true)
    private PartyEntity party;

    @Column(name = "Is_Deleted", nullable = false)
    private Boolean isDeleted = false;
}
