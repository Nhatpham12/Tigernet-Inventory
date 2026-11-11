package com.tigernet.backend.payment.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentMethod;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import com.tigernet.backend.payment.repository.PaymentRepository;
import com.tigernet.backend.user.service.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ghi từng lần thu/chi; Paid/Remain trên phiếu gốc do service của phiếu đó cập nhật
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PaymentService {
    static final String CODE_RECEIPT = "THU"; // phiếu thu (IN)
    static final String CODE_PAYMENT = "CHI"; // phiếu chi (OUT)

    PaymentRepository paymentRepository;
    CurrentUserService currentUserService;

    // warehouseCode chỉ dùng để dựng số phiếu {THU|CHI}-{Warehouse_Code}-{yyyy}-{0001}
    public PaymentEntity record(PaymentDirection direction, PartyEntity party, PaymentReferenceType referenceType,
                                Integer referenceId, BigDecimal amount, PaymentMethod method,
                                String note, String warehouseCode) {
        return record(direction, party, referenceType, referenceId, amount, method, note, warehouseCode, null);
    }

    // paidAt null = bây giờ (phiếu thu/chi lập tay có thể ghi lùi ngày)
    public PaymentEntity record(PaymentDirection direction, PartyEntity party, PaymentReferenceType referenceType,
                                Integer referenceId, BigDecimal amount, PaymentMethod method,
                                String note, String warehouseCode, LocalDateTime paidAt) {
        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentCode(nextPaymentCode(direction, warehouseCode));
        payment.setDirection(direction);
        payment.setParty(party);
        payment.setReferenceType(referenceType);
        payment.setReferenceId(referenceId);
        payment.setAmount(MoneyUtils.round(amount));
        if (method != null) payment.setMethod(method);
        payment.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now());
        payment.setNote(note);
        payment.setRecordedBy(currentUserService.getCurrentUser());
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentEntity> getPayments(PaymentReferenceType referenceType, Integer referenceId) {
        return paymentRepository.findByReferenceTypeAndReferenceIdOrderByPaidAtDescIdDesc(referenceType, referenceId);
    }

    @Transactional(readOnly = true)
    public List<PaymentEntity> search(PaymentDirection direction, Integer partyId, PaymentReferenceType referenceType,
                                      Integer referenceId, LocalDate from, LocalDate to) {
        return paymentRepository.search(direction, partyId, referenceType, referenceId,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null);
    }

    @Transactional(readOnly = true)
    public PaymentEntity findPayment(Integer paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    private String nextPaymentCode(PaymentDirection direction, String warehouseCode) {
        String prefix = DocumentCodeGenerator.prefix(
                direction == PaymentDirection.IN ? CODE_RECEIPT : CODE_PAYMENT, warehouseCode);
        String lastCode = paymentRepository.findFirstByPaymentCodeStartingWithOrderByPaymentCodeDesc(prefix)
                .map(PaymentEntity::getPaymentCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
