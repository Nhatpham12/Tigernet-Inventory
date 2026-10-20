package com.tigernet.backend.salesreturn.entity;

import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.salesreturn.enums.ExchangeDirection;
import com.tigernet.backend.salesreturn.enums.ReturnType;
import com.tigernet.backend.salesreturn.enums.SalesReturnStatus;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// phiếu đổi/trả hàng đã bán
@Entity
@Getter @Setter
@Table(name = "Sales_Returns")
public class SalesReturnEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Receipt_Code", nullable = false, unique = true, length = 30)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Export_Receipt_ID", nullable = false)
    private ExportReceiptEntity exportReceipt; // phiếu bán gốc

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Party_ID", nullable = false)
    private PartyEntity party; // người mang hàng tới trả

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "Return_Type", nullable = false, length = 20)
    private ReturnType returnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "Exchange_Direction", length = 20)
    private ExchangeDirection exchangeDirection;

    @Column(name = "Return_Date", nullable = false)
    private LocalDateTime returnDate;

    @Column(name = "Days_Since_Delivery")
    private Integer daysSinceDelivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Policy_ID")
    private ReturnFeePolicyEntity policy;

    @Column(name = "Original_Value", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalValue;

    @Column(name = "Fee_Percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal feePercent;

    @Column(name = "Fee_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "New_Export_Receipt_ID", unique = true)
    private ExportReceiptEntity newExportReceipt; // phiếu xuất đổi hàng (1:1)

    @Column(name = "Refund_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private SalesReturnStatus status;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesReturnDetailEntity> details = new ArrayList<>();
}
