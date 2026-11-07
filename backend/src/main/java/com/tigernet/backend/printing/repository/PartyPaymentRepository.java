package com.tigernet.backend.printing.repository;

import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Truy vấn thanh toán theo đối tác cho biên bản đối chiếu công nợ (tách riêng để không sửa module payment). */
public interface PartyPaymentRepository extends JpaRepository<PaymentEntity, Integer> {
    List<PaymentEntity> findByPartyIdAndDirectionAndStatusAndPaidAtBeforeOrderByPaidAtAscIdAsc(
            Integer partyId, PaymentDirection direction, PaymentEntryStatus status, LocalDateTime before);
}
