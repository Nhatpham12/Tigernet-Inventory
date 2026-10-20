package com.tigernet.backend.stocktake.entity;

import com.tigernet.backend.stocktake.enums.StocktakeStatus;
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
@Table(name = "Stocktakes")
public class StocktakeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Stocktake_Code", nullable = false, unique = true, length = 30)
    private String stocktakeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Warehouse_ID", nullable = false)
    private WarehouseEntity warehouse;

    @Column(name = "Stocktake_Date", nullable = false)
    private LocalDateTime stocktakeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private StocktakeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Created_By", nullable = false)
    private UserEntity createdBy;

    @OneToMany(mappedBy = "stocktake", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StocktakeDetailEntity> details = new ArrayList<>();
}
