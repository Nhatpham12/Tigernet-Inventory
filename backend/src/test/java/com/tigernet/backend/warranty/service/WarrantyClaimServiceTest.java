package com.tigernet.backend.warranty.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warranty.dto.request.WarrantyStatusRequest;
import com.tigernet.backend.warranty.entity.WarrantyClaimEntity;
import com.tigernet.backend.warranty.enums.WarrantyClaimStatus;
import com.tigernet.backend.warranty.enums.WarrantyResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WarrantyClaimServiceTest {
    static final LocalDateTime NOW = LocalDateTime.parse("2026-11-07T10:00:00");

    private static WarrantyClaimEntity claim(WarrantyClaimStatus status) {
        WarrantyClaimEntity c = new WarrantyClaimEntity();
        c.setStatus(status);
        return c;
    }

    private static WarrantyStatusRequest to(WarrantyClaimStatus s) {
        return WarrantyStatusRequest.builder().status(s).build();
    }

    private static void assertInvalid(WarrantyClaimStatus from, WarrantyClaimStatus to) {
        AppException e = assertThrows(AppException.class,
                () -> WarrantyClaimService.applyStatus(claim(from), to(to), null, NOW));
        assertEquals(ErrorCode.WARRANTY_STATUS_INVALID, e.getErrorCode());
    }

    @Test
    void validChainReceivedToClosed() {
        WarrantyClaimEntity c = claim(WarrantyClaimStatus.RECEIVED);
        WarrantyClaimService.applyStatus(c, to(WarrantyClaimStatus.SENT), null, NOW);
        assertEquals(WarrantyClaimStatus.SENT, c.getStatus());
        assertEquals(NOW, c.getSentAt());

        WarehouseEntity wh = new WarehouseEntity();
        WarrantyStatusRequest returned = WarrantyStatusRequest.builder().status(WarrantyClaimStatus.RETURNED)
                .result(WarrantyResult.REPLACED).cost(new BigDecimal("150000.456")).replacementSerial("SN-NEW")
                .newWarrantyExpiry(LocalDate.parse("2027-11-07")).note("đổi máy mới").build();
        WarrantyClaimService.applyStatus(c, returned, wh, NOW.plusDays(3));
        assertEquals(WarrantyClaimStatus.RETURNED, c.getStatus());
        assertEquals(NOW.plusDays(3), c.getReturnedAt());
        assertEquals(WarrantyResult.REPLACED, c.getResult());
        assertEquals(0, new BigDecimal("150000.46").compareTo(c.getCost()));
        assertEquals("SN-NEW", c.getReplacementSerial());
        assertEquals(LocalDate.parse("2027-11-07"), c.getNewWarrantyExpiry());
        assertSame(wh, c.getRestockedWarehouse());

        WarrantyClaimService.applyStatus(c, to(WarrantyClaimStatus.CLOSED), null, NOW.plusDays(4));
        assertEquals(WarrantyClaimStatus.CLOSED, c.getStatus());
    }

    @Test
    void rejectedSetsResultAndCanOnlyClose() {
        WarrantyClaimEntity c = claim(WarrantyClaimStatus.RECEIVED);
        WarrantyClaimService.applyStatus(c, to(WarrantyClaimStatus.REJECTED), null, NOW);
        assertEquals(WarrantyResult.REJECTED, c.getResult());
        assertNull(c.getSentAt());
        WarrantyClaimService.applyStatus(c, to(WarrantyClaimStatus.CLOSED), null, NOW);
        assertEquals(WarrantyClaimStatus.CLOSED, c.getStatus());
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertInvalid(WarrantyClaimStatus.RECEIVED, WarrantyClaimStatus.RETURNED);
        assertInvalid(WarrantyClaimStatus.RECEIVED, WarrantyClaimStatus.CLOSED);
        assertInvalid(WarrantyClaimStatus.SENT, WarrantyClaimStatus.CLOSED);
        assertInvalid(WarrantyClaimStatus.SENT, WarrantyClaimStatus.REJECTED);
        assertInvalid(WarrantyClaimStatus.RETURNED, WarrantyClaimStatus.SENT);
        assertInvalid(WarrantyClaimStatus.REJECTED, WarrantyClaimStatus.SENT);
        assertInvalid(WarrantyClaimStatus.CLOSED, WarrantyClaimStatus.RECEIVED);
        assertInvalid(WarrantyClaimStatus.CLOSED, WarrantyClaimStatus.CLOSED);
    }

    @Test
    void failedTransitionLeavesClaimUntouched() {
        WarrantyClaimEntity c = claim(WarrantyClaimStatus.RECEIVED);
        assertThrows(AppException.class, () -> WarrantyClaimService.applyStatus(c,
                WarrantyStatusRequest.builder().status(WarrantyClaimStatus.CLOSED).note("x").build(), null, NOW));
        assertEquals(WarrantyClaimStatus.RECEIVED, c.getStatus());
        assertNull(c.getNote());
    }
}
