package com.tigernet.backend.inventory.entity;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inbound.entity.ImportReceiptDetailEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.entity.WarehouseLocationEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// vòng đời từng đơn vị hàng (serial) hoặc từng lô (không serial) tại một kho
@Entity
@Getter @Setter
@Table(name = "Inventory")
public class InventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_ID", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse; // kho hiện tại đang giữ hàng

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Location_ID")
    private WarehouseLocationEntity location;

    @Column(name = "Serial", unique = true)
    private String serial; // duy nhất toàn hệ thống

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Import_Detail_ID")
    private ImportReceiptDetailEntity importDetail;

    @Column(name = "Quantity_On_Hand", nullable = false)
    private Integer quantityOnHand;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private InventoryStatus status;

    @Column(name = "Price_In", precision = 18, scale = 2)
    private BigDecimal priceIn;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "Date_In")
    private LocalDateTime dateIn;

    @Column(name = "Date_Out")
    private LocalDateTime dateOut;

    @Version
    @Column(name = "Version", nullable = false)
    private Integer version; // khóa lạc quan: 2 giao dịch cùng sửa 1 đơn vị thì giao dịch sau bị từ chối
}
