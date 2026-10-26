package com.tigernet.backend.payment.entity;

import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import com.tigernet.backend.payment.enums.PaymentMethod;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import com.tigernet.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// mỗi lần thu/chi một dòng; (referenceType, referenceId) trỏ mềm tới phiếu gốc
@Entity
@Getter @Setter
@Table(name = "Payments")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Payment_Code", nullable = false, unique = true, length = 30)
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "Direction", nullable = false, length = 3)
    private PaymentDirection direction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Party_ID", nullable = false)
    private PartyEntity party; // bên trả (IN) hoặc bên nhận (OUT)

    @Enumerated(EnumType.STRING)
    @Column(name = "Reference_Type", nullable = false, length = 30)
    private PaymentReferenceType referenceType;

    @Column(name = "Reference_ID", nullable = false)
    private Integer referenceId;

    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Method", nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(name = "Paid_At", nullable = false)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private PaymentEntryStatus status = PaymentEntryStatus.COMPLETED;

    @Column(name = "Note")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Recorded_By", nullable = false)
    private UserEntity recordedBy;

    @Column(name = "Created_At", nullable = false)
    private LocalDateTime createdAt;
}
