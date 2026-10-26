package com.tigernet.backend.warranty.entity;

import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warranty.enums.WarrantyClaimStatus;
import com.tigernet.backend.warranty.enums.WarrantyResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// vòng đời một lần bảo hành: RECEIVED -> SENT -> RETURNED -> CLOSED (hoặc REJECTED)
@Entity
@Getter @Setter
@Table(name = "Warranty_Claims")
public class WarrantyClaimEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Claim_Code", nullable = false, unique = true, length = 30)
    private String claimCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Export_Detail_ID", nullable = false)
    private ExportReceiptDetailEntity exportDetail; // dòng bán gốc, cho biết hạn bảo hành

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Inventory_ID")
    private InventoryEntity inventory; // đơn vị hàng đang bảo hành

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Received_Warehouse_ID", nullable = false)
    private WarehouseEntity receivedWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Restocked_Warehouse_ID")
    private WarehouseEntity restockedWarehouse; // kho nhận lại hàng sau bảo hành

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private WarrantyClaimStatus status = WarrantyClaimStatus.RECEIVED;

    @Column(name = "Received_At", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "Sent_At")
    private LocalDateTime sentAt;

    @Column(name = "Returned_At")
    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Result", length = 20)
    private WarrantyResult result;

    @Column(name = "Cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "Replacement_Serial")
    private String replacementSerial;

    @Column(name = "New_Warranty_Expiry")
    private LocalDate newWarrantyExpiry;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Handled_By", nullable = false)
    private UserEntity handledBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
}
