package com.tigernet.backend.alert.entity;

import com.tigernet.backend.alert.enums.AlertType;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "Alerts")
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "Alert_Type", nullable = false, length = 30)
    private AlertType alertType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID")
    private WarehouseEntity warehouse; // NULL = cảnh báo toàn công ty

    @Column(name = "Message", nullable = false)
    private String message;

    @Column(name = "Ref_Table", length = 50)
    private String refTable;

    @Column(name = "Ref_ID")
    private Integer refId;

    @Column(name = "Is_Resolved", nullable = false)
    private Boolean isResolved = false; // cờ dùng chung, "đã đọc" = đã xử lý

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
}
