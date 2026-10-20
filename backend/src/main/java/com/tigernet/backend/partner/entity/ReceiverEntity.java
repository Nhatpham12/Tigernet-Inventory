package com.tigernet.backend.partner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// người nhận hàng đã lưu của khách
@Entity
@Getter @Setter
@Table(name = "Receivers")
public class ReceiverEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Customer_ID", nullable = false)
    private CustomerEntity customer;

    @Column(name = "Receiver_Name", nullable = false, length = 150)
    private String receiverName;

    @Column(name = "Address")
    private String address;

    @Column(name = "Phone", length = 50)
    private String phone;

    @Column(name = "Is_Deleted", nullable = false)
    private Boolean isDeleted = false;
}
