package com.tigernet.backend.rental.entity;

import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.entity.ReceiverEntity;
import com.tigernet.backend.rental.enums.RentalType;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// phiếu thuê/mượn không có trạng thái duyệt: tiến trình theo Payment_Status (RENTING/RETURNED/CANCELLED)
@Entity
@Getter @Setter
@Table(name = "Rental_Receipts")
public class RentalReceiptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Receipt_Code", nullable = false, unique = true, length = 30)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Customer_ID", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Receiver_ID")
    private ReceiverEntity receiver;

    @Column(name = "Receiver_Name_Snapshot", length = 150)
    private String receiverNameSnapshot;

    @Column(name = "Receiver_Address_Snapshot")
    private String receiverAddressSnapshot;

    @Column(name = "Receiver_Phone_Snapshot", length = 50)
    private String receiverPhoneSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "Rental_Type", nullable = false, length = 10)
    private RentalType rentalType;

    @Column(name = "Date_Out", nullable = false)
    private LocalDateTime dateOut;

    @Column(name = "Due_Date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "Deposit", nullable = false, precision = 18, scale = 2)
    private BigDecimal deposit;

    @Column(name = "Rental_Price_Per_Day", nullable = false, precision = 18, scale = 2)
    private BigDecimal rentalPricePerDay; // cơ sở tính phạt quá hạn

    @Column(name = "Total_Rent", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRent;

    @Column(name = "Penalty_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "Return_Date")
    private LocalDateTime returnDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Payment_Status_ID")
    private PaymentStatusEntity paymentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "rentalReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalReceiptDetailEntity> details = new ArrayList<>();
}
