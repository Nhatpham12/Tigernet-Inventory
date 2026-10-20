package com.tigernet.backend.rental.dto.response;

import com.tigernet.backend.rental.enums.RentalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RentalReceiptResponse {
    private Integer id;
    private String receiptCode;
    private Integer customerId;
    private String customerName;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Integer receiverId;
    private String receiverNameSnapshot;
    private String receiverAddressSnapshot;
    private String receiverPhoneSnapshot;
    private RentalType rentalType;
    private LocalDateTime dateOut;
    private LocalDateTime dueDate;
    private BigDecimal deposit;
    private BigDecimal rentalPricePerDay;
    private BigDecimal totalRent;
    private BigDecimal penaltyAmount;
    private LocalDateTime returnDate;
    private Integer paymentStatusId;
    private String paymentStatus;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<RentalReceiptDetailResponse> details;
}
