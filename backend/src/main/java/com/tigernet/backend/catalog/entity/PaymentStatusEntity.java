package com.tigernet.backend.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "Payment_Statuses")
public class PaymentStatusEntity {
    // tên các trạng thái được seed ở V2__seed_reference_data.sql
    public static final String PAID = "PAID";
    public static final String UNPAID = "UNPAID";
    public static final String PARTIAL = "PARTIAL";
    public static final String RENTING = "RENTING";
    public static final String RETURNED = "RETURNED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Payment_Status", nullable = false, unique = true, length = 50)
    private String paymentStatus;
}
