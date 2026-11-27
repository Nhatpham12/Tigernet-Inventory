package com.tigernet.backend.rental.service;

import com.tigernet.backend.warehouse.service.WarehouseAccessService;
import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.service.LookupService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
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
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// phiếu trả hàng thuê/mượn: đánh dấu dòng đã trả, tính phạt quá hạn, chốt phiếu thuê khi trả đủ.
// Hàng trả về kho nhận (có thể khác kho cho thuê): tốt -> IN_STOCK, hỏng -> DAMAGED, qua StockService
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
    LookupService lookupService;
    CurrentUserService currentUserService;
    WarehouseAccessService warehouseAccess;
    StockService stockService;

    /** 1 dòng trả kèm cờ hỏng (cờ không lưu ở dòng trả, trạng thái nằm ở Inventory) */
    private record ReturnLine(ReturnReceiptDetailEntity detail, boolean damaged) { }

    @NonFinal
    @Value("${tigernet.rental.penalty-after-days}")
    int penaltyAfterDays;

    // nhân viên kho nhận lại lập phiếu: kiểm tra kho nhận (có thể khác kho đã cho thuê)
    public ReturnReceiptResponse createReceipt(ReturnReceiptCreationRequest request) {
        warehouseAccess.assertAccess(request.getWarehouseId());
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
        List<ReturnLine> lines = addDetails(receipt, rental, request.getDetails());

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

        ReturnReceiptEntity saved = returnReceiptRepository.save(receipt); // cần id phiếu cho thẻ kho
        restock(saved, rental, lines);
        return rentalMapper.toReturnResponse(saved);
    }

    /**
     * Đưa hàng trả vào kho nhận: hàng serial đúng đơn vị đã cho thuê, hàng không serial lấy lô đang cho thuê
     * của SP ở kho cho thuê (cũ trước). Đi theo id SP tăng dần để không khóa chéo.
     */
    private void restock(ReturnReceiptEntity receipt, RentalReceiptEntity rental, List<ReturnLine> lines) {
        StockRef ref = StockRef.of(StockRef.RETURN, receipt.getId());
        InventoryStatus rented = RentalReceiptService.rentedStatus(rental);
        List<ReturnLine> sorted = new ArrayList<>(lines);
        sorted.sort(Comparator.comparing((ReturnLine l) -> l.detail().getProduct().getId()));
        for (ReturnLine line : sorted) {
            ReturnReceiptDetailEntity d = line.detail();
            InventoryStatus target = line.damaged() ? InventoryStatus.DAMAGED : InventoryStatus.IN_STOCK;
            if (Boolean.TRUE.equals(d.getProduct().getIsSerialized())) {
                stockService.moveSerial(d.getInventory(), rented, target, receipt.getWarehouse(), ref);
            } else {
                stockService.moveQty(d.getProduct(), rental.getWarehouse(), receipt.getWarehouse(), d.getQuantity(),
                        rented, target, ref);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ReturnReceiptResponse> getReceipts(Integer warehouseId, Integer rentalReceiptId) {
        List<ReturnReceiptEntity> rows = returnReceiptRepository.search(warehouseId, rentalReceiptId);
        Set<Integer> allowed = warehouseAccess.allowedWarehouseIds();
        if (allowed != null) {
            // thấy phiếu trả nếu được giao kho nhận lại hoặc kho đã cho thuê
            rows = rows.stream().filter(r -> allowed.contains(r.getWarehouse().getId())
                    || allowed.contains(r.getRentalReceipt().getWarehouse().getId())).toList();
        }
        return rentalMapper.toReturnResponses(rows);
    }

    @Transactional(readOnly = true)
    public ReturnReceiptResponse getReceipt(Integer receiptId) {
        ReturnReceiptEntity receipt = returnReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_RECEIPT_NOT_FOUND));
        warehouseAccess.assertAnyAccess(receipt.getWarehouse(), receipt.getRentalReceipt().getWarehouse());
        return rentalMapper.toReturnResponse(receipt);
    }

    private List<ReturnLine> addDetails(ReturnReceiptEntity receipt, RentalReceiptEntity rental,
                                        List<ReturnReceiptDetailRequest> requests) {
        List<ReturnLine> result = new ArrayList<>();
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
            // hàng serial trả đúng đơn vị đã cho thuê; hàng không serial không gắn đơn vị
            InventoryEntity unit = rentalLine.getInventory();
            if (Boolean.TRUE.equals(rentalLine.getProduct().getIsSerialized())) {
                if (unit == null) throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH); // phiếu cũ chưa có serial
                if (line.getInventoryId() != null && !line.getInventoryId().equals(unit.getId())) {
                    throw new AppException(ErrorCode.RENTAL_DETAIL_INVALID);
                }
            }
            boolean damaged = Boolean.TRUE.equals(line.getDamaged());
            detail.setInventory(unit);
            detail.setQuantity(line.getQuantity());
            detail.setSerial(rentalLine.getSerial());
            detail.setConditionNote(StringUtils.hasText(line.getConditionNote()) ? line.getConditionNote()
                    : damaged ? "Hư hỏng" : null);
            receipt.getDetails().add(detail);
            result.add(new ReturnLine(detail, damaged));

            if (returned == rentalLine.getQuantity()) {
                rentalLine.setIsReturned(true);
            }
        }
        return result;
    }

    private String nextReceiptCode(WarehouseEntity warehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, warehouse.getWarehouseCode());
        String lastCode = returnReceiptRepository.findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(prefix)
                .map(ReturnReceiptEntity::getReceiptCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
