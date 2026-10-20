package com.tigernet.backend.partner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Supplier_Bank_Accounts")
public class SupplierBankAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Supplier_ID", nullable = false)
    private SupplierEntity supplier;

    @Column(name = "Account_Name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "Account_Number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "Bank_Name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "Bank_Payment_Code", length = 100)
    private String bankPaymentCode;
}
