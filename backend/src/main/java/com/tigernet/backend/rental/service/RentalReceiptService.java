package com.tigernet.backend.rental.service;

import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.service.LookupService;
import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.partner.entity.ReceiverEntity;
import com.tigernet.backend.partner.service.CustomerService;
import com.tigernet.backend.partner.service.ReceiverService;
import com.tigernet.backend.rental.dto.request.RentalReceiptCreationRequest;
import com.tigernet.backend.rental.dto.request.RentalReceiptDetailRequest;
import com.tigernet.backend.rental.dto.request.RentalReceiptUpdateRequest;
import com.tigernet.backend.rental.dto.response.RentalReceiptResponse;
import com.tigernet.backend.rental.entity.RentalReceiptDetailEntity;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.enums.RentalStatus;
import com.tigernet.backend.rental.enums.RentalType;
import com.tigernet.backend.rental.mapper.RentalMapper;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// phiếu thuê/mượn tự duyệt khi lập (Status = APPROVED); tiến trình theo Payment_Status RENTING -> RETURNED / CANCELLED.
// Chuyển hàng sang RENTED/LENT và trả về IN_STOCK khi hủy sẽ đi qua StockService
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class RentalReceiptService {
    static final String CODE_TYPE = "PT";

    RentalReceiptRepository rentalReceiptRepository;
    RentalMapper rentalMapper;
    WarehouseService warehouseService;
    CustomerService customerService;
    ReceiverService receiverService;
    ProductService productService;
    InventoryService inventoryService;
    LookupService lookupService;
    CurrentUserService currentUserService;

    @NonFinal
    @Value("${tigernet.rental.due-warning-days}")
    int dueWarningDays;

    public RentalReceiptResponse createReceipt(RentalReceiptCreationRequest request) {
        WarehouseEntity warehouse = warehouseService.findActiveWarehouse(request.getWarehouseId());

        RentalReceiptEntity receipt = new RentalReceiptEntity();
        receipt.setReceiptCode(nextReceiptCode(warehouse));
        receipt.setWarehouse(warehouse);
        receipt.setCustomer(customerService.findCustomer(request.getCustomerId()));
        applyReceiver(receipt, request.getReceiverId(), request.getReceiverName(),
                request.getReceiverAddress(), request.getReceiverPhone());
        receipt.setRentalType(request.getRentalType() != null ? request.getRentalType() : RentalType.RENT);
        receipt.setDateOut(request.getDateOut() != null ? request.getDateOut() : LocalDateTime.now());
        receipt.setDueDate(request.getDueDate());
        receipt.setDeposit(MoneyUtils.round(MoneyUtils.orZero(request.getDeposit())));
        receipt.setRentalPricePerDay(MoneyUtils.round(MoneyUtils.orZero(request.getRentalPricePerDay())));
        receipt.setPenaltyAmount(BigDecimal.ZERO);
        receipt.setPaymentStatus(lookupService.findPaymentStatus(PaymentStatusEntity.RENTING));
        receipt.setCreatedBy(currentUserService.getCurrentUser());
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setStatus(RentalStatus.APPROVED);
        receipt.setApprovedBy(receipt.getCreatedBy());
        receipt.setApprovedAt(receipt.getCreatedAt());
        validateDates(receipt);
        replaceDetails(receipt, request.getDetails());

        return rentalMapper.toRentalResponse(rentalReceiptRepository.save(receipt));
    }

    @Transactional(readOnly = true)
    public List<RentalReceiptResponse> getReceipts(Integer warehouseId, Integer customerId, RentalType rentalType,
                                                   String paymentStatus, LocalDate from, LocalDate to) {
        return rentalMapper.toRentalResponses(rentalReceiptRepository.search(
                warehouseId, customerId, rentalType, paymentStatus,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null));
    }

    // phiếu sắp đến hạn trong N ngày tới (mặc định theo cấu hình) và phiếu đã quá hạn
    @Transactional(readOnly = true)
    public List<RentalReceiptResponse> getExpiringReceipts(Integer days, Integer warehouseId) {
        int window = days != null ? days : dueWarningDays;
        return rentalMapper.toRentalResponses(rentalReceiptRepository.findDueBefore(
                PaymentStatusEntity.RENTING, LocalDateTime.now().plusDays(window), warehouseId));
    }

    @Transactional(readOnly = true)
    public RentalReceiptResponse getReceipt(Integer receiptId) {
        return rentalMapper.toRentalResponse(findReceipt(receiptId));
    }

    // chỉ sửa khi đang thuê và chưa trả dòng nào
    public RentalReceiptResponse updateReceipt(Integer receiptId, RentalReceiptUpdateRequest request) {
        RentalReceiptEntity receipt = findUntouchedReceipt(receiptId);
        if (request.getCustomerId() != null) {
            receipt.setCustomer(customerService.findCustomer(request.getCustomerId()));
        }
        boolean receiverChanged = request.getReceiverId() != null || request.getReceiverName() != null
                || request.getReceiverAddress() != null || request.getReceiverPhone() != null;
        if (receiverChanged) {
            applyReceiver(receipt, request.getReceiverId(), request.getReceiverName(),
                    request.getReceiverAddress(), request.getReceiverPhone());
        }
        if (request.getRentalType() != null) receipt.setRentalType(request.getRentalType());
        if (request.getDateOut() != null) receipt.setDateOut(request.getDateOut());
        if (request.getDueDate() != null) receipt.setDueDate(request.getDueDate());
        if (request.getDeposit() != null) receipt.setDeposit(MoneyUtils.round(request.getDeposit()));
        if (request.getRentalPricePerDay() != null) {
            receipt.setRentalPricePerDay(MoneyUtils.round(request.getRentalPricePerDay()));
        }
        validateDates(receipt);
        if (request.getDetails() != null) {
            replaceDetails(receipt, request.getDetails());
        }
        return rentalMapper.toRentalResponse(rentalReceiptRepository.save(receipt));
    }

    // hủy phiếu chưa trả dòng nào -> CANCELLED (phiếu giữ lại để tra cứu, không xóa)
    public RentalReceiptResponse cancelReceipt(Integer receiptId) {
        RentalReceiptEntity receipt = findUntouchedReceipt(receiptId);
        receipt.setPaymentStatus(lookupService.findPaymentStatus(PaymentStatusEntity.CANCELLED));
        return rentalMapper.toRentalResponse(rentalReceiptRepository.save(receipt));
    }

    public RentalReceiptEntity findReceipt(Integer receiptId) {
        return rentalReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_RECEIPT_NOT_FOUND));
    }

    // phiếu đang thuê/mượn (chưa trả hết, chưa hủy)
    public RentalReceiptEntity findRentingReceipt(Integer receiptId) {
        RentalReceiptEntity receipt = findReceipt(receiptId);
        if (!PaymentStatusEntity.RENTING.equals(receipt.getPaymentStatus().getPaymentStatus())) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return receipt;
    }

    private RentalReceiptEntity findUntouchedReceipt(Integer receiptId) {
        RentalReceiptEntity receipt = findRentingReceipt(receiptId);
        boolean anyReturned = receipt.getDetails().stream().anyMatch(d -> Boolean.TRUE.equals(d.getIsReturned()));
        if (anyReturned) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return receipt;
    }

    private void validateDates(RentalReceiptEntity receipt) {
        if (receipt.getDueDate().isBefore(receipt.getDateOut())) {
            throw new AppException(ErrorCode.INVALID_DUE_DATE);
        }
    }

    private void applyReceiver(RentalReceiptEntity receipt, Integer receiverId,
                               String name, String address, String phone) {
        ReceiverEntity receiver = receiverId != null
                ? receiverService.findReceiverOfCustomer(receiverId, receipt.getCustomer().getId())
                : null;
        receipt.setReceiver(receiver);
        receipt.setReceiverNameSnapshot(name != null ? name : receiver != null ? receiver.getReceiverName() : null);
        receipt.setReceiverAddressSnapshot(address != null ? address : receiver != null ? receiver.getAddress() : null);
        receipt.setReceiverPhoneSnapshot(phone != null ? phone : receiver != null ? receiver.getPhone() : null);
    }

    // Total_Rent = Σ (số lượng × giá thuê cả kỳ của dòng)
    private void replaceDetails(RentalReceiptEntity receipt, List<RentalReceiptDetailRequest> requests) {
        receipt.getDetails().clear();
        Set<Integer> inventoryIds = new HashSet<>();
        BigDecimal totalRent = BigDecimal.ZERO;

        for (RentalReceiptDetailRequest line : requests) {
            RentalReceiptDetailEntity detail = new RentalReceiptDetailEntity();
            detail.setRentalReceipt(receipt);
            detail.setProduct(productService.findProduct(line.getProductId()));
            detail.setQuantity(line.getQuantity());
            detail.setRentalPrice(MoneyUtils.round(line.getRentalPrice()));
            detail.setSerial(StringUtils.hasText(line.getSerial()) ? line.getSerial().trim() : null);
            detail.setIsReturned(false);

            if (line.getInventoryId() != null) {
                if (!inventoryIds.add(line.getInventoryId())) throw new AppException(ErrorCode.DUPLICATE_SERIAL);
                InventoryEntity inventory = inventoryService.findInventoryForLine(
                        line.getInventoryId(), line.getProductId(), receipt.getWarehouse().getId());
                detail.setInventory(inventory);
                if (detail.getSerial() == null) detail.setSerial(inventory.getSerial());
            }
            if (detail.getSerial() != null && line.getQuantity() != 1) {
                throw new AppException(ErrorCode.SERIAL_QUANTITY_INVALID);
            }
            receipt.getDetails().add(detail);
            totalRent = totalRent.add(MoneyUtils.lineTotal(line.getQuantity(), line.getRentalPrice()));
        }
        receipt.setTotalRent(totalRent);
    }

    private String nextReceiptCode(WarehouseEntity warehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, warehouse.getWarehouseCode());
        String lastCode = rentalReceiptRepository.findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(prefix)
                .map(RentalReceiptEntity::getReceiptCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
