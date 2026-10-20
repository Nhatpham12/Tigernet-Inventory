package com.tigernet.backend.rental.dto.request;

import com.tigernet.backend.rental.enums.RentalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class RentalReceiptCreationRequest {
    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotNull(message = "Warehouse ID is required")
    private Integer warehouseId;

    // chọn người nhận đã lưu; các trường snapshot gửi lên sẽ ghi đè thông tin lấy từ receiver
    private Integer receiverId;
    private String receiverName;
    private String receiverAddress;
    private String receiverPhone;

    private RentalType rentalType; // null = RENT
    private LocalDateTime dateOut; // null = thời điểm hiện tại

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    @PositiveOrZero(message = "Deposit must be >= 0")
    private BigDecimal deposit;

    @PositiveOrZero(message = "Rental price per day must be >= 0")
    private BigDecimal rentalPricePerDay; // cơ sở tính phạt quá hạn

    @NotEmpty(message = "Receipt must have at least one line")
    @Valid
    private List<RentalReceiptDetailRequest> details;
}
