package com.tigernet.backend.rental.entity;

import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// phiếu trả hàng thuê/mượn
@Entity
@Getter @Setter
@Table(name = "Return_Receipts")
public class ReturnReceiptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Receipt_Code", nullable = false, unique = true, length = 30)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Rental_Receipt_ID", nullable = false)
    private RentalReceiptEntity rentalReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse; // kho nhận lại, có thể khác kho cho thuê

    @Column(name = "Return_Date", nullable = false)
    private LocalDateTime returnDate;

    @Column(name = "Days_Overdue", nullable = false)
    private Integer daysOverdue;

    @Column(name = "Penalty_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "Compensation_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal compensationAmount = BigDecimal.ZERO; // bồi thường hư hỏng/mất

    @Column(name = "Deposit_Refund", nullable = false, precision = 18, scale = 2)
    private BigDecimal depositRefund = BigDecimal.ZERO; // cọc hoàn lại cho khách

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "returnReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnReceiptDetailEntity> details = new ArrayList<>();
}
