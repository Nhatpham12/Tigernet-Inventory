package com.tigernet.backend.partner.entity;

import com.tigernet.backend.partner.enums.PartyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// đối tác chung: một Party có thể vừa là NCC vừa là khách hàng
@Entity
@Getter @Setter
@Table(name = "Parties")
public class PartyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "Party_Type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "Full_Name", nullable = false)
    private String fullName;

    @Column(name = "Tax_Code", unique = true, length = 30)
    private String taxCode;

    @Column(name = "National_ID", length = 20)
    private String nationalId;

    @Column(name = "Address")
    private String address;

    @Column(name = "Phone", length = 50)
    private String phone;

    @Column(name = "Email", length = 150)
    private String email;

    @Column(name = "Is_Deleted", nullable = false)
    private Boolean isDeleted = false;
}
