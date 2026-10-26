package com.tigernet.backend.supplierreturn.entity;

import com.tigernet.backend.partner.entity.SupplierEntity;
import com.tigernet.backend.supplierreturn.enums.SupplierReturnStatus;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// phiếu trả hàng lỗi / gửi bảo hành lên NCC (RMA)
@Entity
@Getter @Setter
@Table(name = "Supplier_Returns")
public class SupplierReturnEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Return_Code", nullable = false, unique = true, length = 30)
    private String returnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Supplier_ID", nullable = false)
    private SupplierEntity supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse; // kho gửi hàng đi

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private SupplierReturnStatus status = SupplierReturnStatus.DRAFT;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "Sent_At")
    private LocalDateTime sentAt;

    @Column(name = "Received_Back_At")
    private LocalDateTime receivedBackAt;

    @Column(name = "Compensation_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal compensationAmount = BigDecimal.ZERO; // NCC bồi hoàn

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "supplierReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierReturnDetailEntity> details = new ArrayList<>();
}
