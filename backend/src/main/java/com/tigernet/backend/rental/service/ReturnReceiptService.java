package com.tigernet.backend.rental.service;

import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.service.LookupService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.rental.dto.request.ReturnReceiptCreationRequest;
import com.tigernet.backend.rental.dto.request.ReturnReceiptDetailRequest;
import com.tigernet.backend.rental.dto.response.ReturnReceiptResponse;
import com.tigernet.backend.rental.entity.RentalReceiptDetailEntity;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptDetailEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptEntity;
import com.tigernet.backend.rental.mapper.RentalMapper;
import com.tigernet.backend.rental.repository.ReturnReceiptRepository;
import com.tigernet.backend.user.service.CurrentUserService;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.service.WarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// phiếu trả hàng thuê/mượn: đánh dấu dòng đã trả, tính phạt quá hạn, chốt phiếu thuê khi trả đủ.
// Đưa hàng về IN_STOCK / DAMAGED tại kho nhận sẽ đi qua StockService
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ReturnReceiptService {
    static final String CODE_TYPE = "TT";

    ReturnReceiptRepository returnReceiptRepository;
    RentalReceiptService rentalReceiptService;
    RentalMapper rentalMapper;
    WarehouseService warehouseService;
    InventoryService inventoryService;
    LookupService lookupService;
    CurrentUserService currentUserService;

    @NonFinal
    @Value("${tigernet.rental.penalty-after-days}")
    int penaltyAfterDays;

    public ReturnReceiptResponse createReceipt(ReturnReceiptCreationRequest request) {
        RentalReceiptEntity rental = rentalReceiptService.findRentingReceipt(request.getRentalReceiptId());
        WarehouseEntity warehouse = warehouseService.findActiveWarehouse(request.getWarehouseId());
        LocalDateTime returnDate = request.getReturnDate() != null ? request.getReturnDate() : LocalDateTime.now();

        ReturnReceiptEntity receipt = new ReturnReceiptEntity();
        receipt.setReceiptCode(nextReceiptCode(warehouse));
        receipt.setRentalReceipt(rental);
        receipt.setWarehouse(warehouse);
        receipt.setReturnDate(returnDate);
        receipt.setCompensationAmount(MoneyUtils.round(MoneyUtils.orZero(request.getCompensationAmount())));
        receipt.setDepositRefund(MoneyUtils.round(MoneyUtils.orZero(request.getDepositRefund())));
        receipt.setNote(request.getNote());
        receipt.setCreatedBy(currentUserService.getCurrentUser());
        receipt.setCreatedAt(LocalDateTime.now());
        addDetails(receipt, rental, request.getDetails());

        // quá hạn tính theo ngày; phạt = số ngày quá hạn × giá thuê/ngày khi quá hạn từ N ngày trở lên
        long daysOverdue = Math.max(0, ChronoUnit.DAYS.between(
                rental.getDueDate().toLocalDate(), returnDate.toLocalDate()));
        BigDecimal penalty = daysOverdue >= penaltyAfterDays
                ? MoneyUtils.round(rental.getRentalPricePerDay().multiply(BigDecimal.valueOf(daysOverdue)))
                : BigDecimal.ZERO;
        receipt.setDaysOverdue((int) daysOverdue);
        receipt.setPenaltyAmount(penalty);
        rental.setPenaltyAmount(rental.getPenaltyAmount().add(penalty));

        // trả đủ mọi dòng -> phiếu thuê chuyển RETURNED
        boolean allReturned = rental.getDetails().stream().allMatch(d -> Boolean.TRUE.equals(d.getIsReturned()));
        if (allReturned) {
            rental.setReturnDate(returnDate);
            rental.setPaymentStatus(lookupService.findPaymentStatus(PaymentStatusEntity.RETURNED));
        }

        return rentalMapper.toReturnResponse(returnReceiptRepository.save(receipt));
    }

    @Transactional(readOnly = true)
    public List<ReturnReceiptResponse> getReceipts(Integer warehouseId, Integer rentalReceiptId) {
        return rentalMapper.toReturnResponses(returnReceiptRepository.search(warehouseId, rentalReceiptId));
    }

    @Transactional(readOnly = true)
    public ReturnReceiptResponse getReceipt(Integer receiptId) {
        return rentalMapper.toReturnResponse(returnReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_RECEIPT_NOT_FOUND)));
    }

    private void addDetails(ReturnReceiptEntity receipt, RentalReceiptEntity rental,
                            List<ReturnReceiptDetailRequest> requests) {
        Map<Integer, RentalReceiptDetailEntity> rentalLines = new HashMap<>();
        rental.getDetails().forEach(line -> rentalLines.put(line.getId(), line));
        Map<Integer, Integer> returningNow = new HashMap<>(); // cộng dồn nếu 1 dòng thuê xuất hiện nhiều lần

        for (ReturnReceiptDetailRequest line : requests) {
            RentalReceiptDetailEntity rentalLine = rentalLines.get(line.getRentalDetailId());
            if (rentalLine == null || Boolean.TRUE.equals(rentalLine.getIsReturned())) {
                throw new AppException(ErrorCode.RENTAL_DETAIL_INVALID);
            }

            int returned = returningNow.merge(rentalLine.getId(), line.getQuantity(), Integer::sum)
                    + returnReceiptRepository.sumReturnedQuantity(rentalLine.getId());
            if (returned > rentalLine.getQuantity()) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
            }

            ReturnReceiptDetailEntity detail = new ReturnReceiptDetailEntity();
            detail.setReturnReceipt(receipt);
            detail.setRentalDetail(rentalLine);
            detail.setProduct(rentalLine.getProduct());
            detail.setInventory(line.getInventoryId() != null
                    ? inventoryService.findInventoryOfProduct(line.getInventoryId(), rentalLine.getProduct().getId())
                    : rentalLine.getInventory());
            detail.setQuantity(line.getQuantity());
            detail.setSerial(StringUtils.hasText(line.getSerial()) ? line.getSerial().trim() : rentalLine.getSerial());
            detail.setConditionNote(line.getConditionNote());
            receipt.getDetails().add(detail);

            if (returned == rentalLine.getQuantity()) {
                rentalLine.setIsReturned(true);
            }
        }
    }

    private String nextReceiptCode(WarehouseEntity warehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, warehouse.getWarehouseCode());
        String lastCode = returnReceiptRepository.findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(prefix)
                .map(ReturnReceiptEntity::getReceiptCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
