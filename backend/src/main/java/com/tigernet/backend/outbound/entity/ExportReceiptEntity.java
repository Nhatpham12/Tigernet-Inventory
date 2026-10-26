package com.tigernet.backend.outbound.entity;

import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.entity.VatOptionEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.entity.ReceiverEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@Table(name = "Export_Receipts")
public class ExportReceiptEntity {
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

    // chụp lại người nhận lúc lập phiếu, in phiếu lấy từ đây
    @Column(name = "Receiver_Name_Snapshot", length = 150)
    private String receiverNameSnapshot;

    @Column(name = "Receiver_Address_Snapshot")
    private String receiverAddressSnapshot;

    @Column(name = "Receiver_Phone_Snapshot", length = 50)
    private String receiverPhoneSnapshot;

    @Column(name = "Date_Out", nullable = false)
    private LocalDateTime dateOut;

    @Column(name = "Delivery_Date")
    private LocalDateTime deliveryDate; // mốc tính hạn đổi/trả

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VAT_ID")
    private VatOptionEntity vat;

    @Column(name = "Deposit", nullable = false, precision = 18, scale = 2)
    private BigDecimal deposit;

    @Column(name = "Total_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Payment_Status_ID")
    private PaymentStatusEntity paymentStatus;

    @Column(name = "Paid", nullable = false, precision = 18, scale = 2)
    private BigDecimal paid;

    @Column(name = "Remain", nullable = false, precision = 18, scale = 2)
    private BigDecimal remain; // = Total − Paid

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private ExportStatus status;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Cancelled_By")
    private UserEntity cancelledBy;

    @Column(name = "Cancelled_At")
    private LocalDateTime cancelledAt;

    @Column(name = "Cancel_Reason")
    private String cancelReason;

    @OneToMany(mappedBy = "exportReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExportReceiptDetailEntity> details = new ArrayList<>();
}
