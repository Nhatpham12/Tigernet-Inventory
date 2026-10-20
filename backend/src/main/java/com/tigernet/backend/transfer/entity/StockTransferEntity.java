package com.tigernet.backend.transfer.entity;

import com.tigernet.backend.transfer.enums.TransferStatus;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@Table(name = "Stock_Transfers")
public class StockTransferEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Transfer_Code", nullable = false, unique = true, length = 30)
    private String transferCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "From_Warehouse_ID", nullable = false)
    private WarehouseEntity fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "To_Warehouse_ID", nullable = false)
    private WarehouseEntity toWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Approved_By")
    private UserEntity approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Received_By")
    private UserEntity receivedBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "Approved_At")
    private LocalDateTime approvedAt;

    @Column(name = "Shipped_At")
    private LocalDateTime shippedAt;

    @Column(name = "Received_At")
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockTransferDetailEntity> details = new ArrayList<>();
}
