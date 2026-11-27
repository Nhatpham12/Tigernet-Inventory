package com.tigernet.backend.alert.service;

import com.tigernet.backend.alert.entity.AlertEntity;
import com.tigernet.backend.alert.enums.AlertType;
import com.tigernet.backend.alert.repository.AlertRepository;
import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mỗi sáng (mặc định 7:00 giờ VN) sinh cảnh báo RENT_DUE cho phiếu thuê/mượn sắp đến hạn trong N ngày hoặc đã quá hạn,
 * và đóng cảnh báo của phiếu không còn đang thuê. Chạy không có người dùng nên đọc thẳng repository.
 */
@Component
@RequiredArgsConstructor
public class RentDueAlertJob {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RentalReceiptRepository rentalRepository;
    private final AlertRepository alertRepository;
    private final StockAlertService alertService;

    @Value("${tigernet.rental.due-warning-days}")
    private int dueWarningDays;

    @Scheduled(cron = "${tigernet.alerts.rent-due-cron:0 0 7 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        for (AlertEntity a : alertRepository.search(null, AlertType.RENT_DUE, false)) {
            boolean stillRenting = a.getRefId() != null && rentalRepository.findById(a.getRefId())
                    .map(r -> PaymentStatusEntity.RENTING.equals(r.getPaymentStatus().getPaymentStatus()))
                    .orElse(false);
            if (!stillRenting) {
                a.setIsResolved(true);
                alertRepository.save(a);
            }
        }
        for (RentalReceiptEntity r : rentalRepository.findDueBefore(PaymentStatusEntity.RENTING,
                now.plusDays(dueWarningDays), null)) {
            boolean open = !alertRepository.findByAlertTypeAndRefTableAndRefIdAndIsResolvedFalse(
                    AlertType.RENT_DUE, StockAlertService.REF_RENTALS, r.getId()).isEmpty();
            if (!open) {
                alertService.open(AlertType.RENT_DUE, r.getWarehouse(), StockAlertService.REF_RENTALS, r.getId(),
                        "%s phiếu %s (%s), hạn %s".formatted(r.getDueDate().isBefore(now) ? "Quá hạn trả" : "Sắp đến hạn trả",
                                r.getReceiptCode(), r.getCustomer().getParty().getFullName(),
                                r.getDueDate().format(DATE)));
            }
        }
    }
}
