package com.tigernet.backend.inbound.entity;

import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.partner.entity.PartyEntity;
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
@Table(name = "Import_Receipts")
public class ImportReceiptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Receipt_Code", nullable = false, unique = true, length = 30)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Source_Party_ID", nullable = false)
    private PartyEntity sourceParty; // NCC, khách bán lẻ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @Column(name = "Date_In", nullable = false)
    private LocalDateTime dateIn;

    @Column(name = "Total_Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount; // = tổng Line_Total, service tự tính

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private ImportStatus status;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "importReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportReceiptDetailEntity> details = new ArrayList<>();
}
