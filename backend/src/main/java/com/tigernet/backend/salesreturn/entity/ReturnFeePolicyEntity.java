package com.tigernet.backend.salesreturn.entity;

import com.tigernet.backend.salesreturn.enums.FeeActionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// mỗi dòng là một ô của ma trận (bậc thời gian × loại thao tác) -> tỷ lệ phí
@Entity
@Getter @Setter
@Table(name = "Return_Fee_Policy")
public class ReturnFeePolicyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Tier_Name", nullable = false, length = 30)
    private String tierName;

    @Column(name = "Max_Days", nullable = false)
    private Integer maxDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "Action_Type", nullable = false, length = 30)
    private FeeActionType actionType;

    @Column(name = "Fee_Percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal feePercent;

    @Column(name = "Is_Allowed", nullable = false)
    private Boolean isAllowed;

    @Column(name = "Effective_From")
    private LocalDate effectiveFrom;
}
