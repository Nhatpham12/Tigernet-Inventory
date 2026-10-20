package com.tigernet.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@Table(name = "VAT_Options")
public class VatOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VAT", nullable = false, unique = true, length = 10)
    private String vat; // nhãn hiển thị: NO, 0%, 5%...

    @Column(name = "Rate", precision = 5, scale = 2)
    private BigDecimal rate; // NULL = không chịu VAT
}
