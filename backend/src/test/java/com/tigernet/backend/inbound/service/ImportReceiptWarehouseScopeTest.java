package com.tigernet.backend.inbound.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inbound.dto.request.ImportReceiptCreationRequest;
import com.tigernet.backend.inbound.dto.request.ImportReceiptUpdateRequest;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.warehouse.entity.UserWarehouseEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.repository.UserWarehouseRepository;
import com.tigernet.backend.warehouse.service.WarehouseAccessService;
import com.tigernet.backend.warehouse.service.WarehouseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Thủ kho được giao kho 1 không sửa/hủy/lập được phiếu nhập của kho 2. */
@ExtendWith(MockitoExtension.class)
class ImportReceiptWarehouseScopeTest {
    @Mock ImportReceiptRepository importReceiptRepository;
    @Mock WarehouseService warehouseService;
    // WarehouseAccessService thật, đọc phân công kho từ repository giả
    UserWarehouseRepository userWarehouseRepository = mock(UserWarehouseRepository.class);
    @Spy WarehouseAccessService warehouseAccess = new WarehouseAccessService(userWarehouseRepository);
    @InjectMocks ImportReceiptService service;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS512").claim("userId", 5).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_WAREHOUSE"))));
        UserWarehouseEntity uw = new UserWarehouseEntity();
        uw.setWarehouse(warehouse(1));
        when(userWarehouseRepository.findByUserUserId(5)).thenReturn(List.of(uw));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static WarehouseEntity warehouse(int id) {
        WarehouseEntity w = new WarehouseEntity();
        w.setId(id);
        return w;
    }

    private void receiptInWarehouse(int receiptId, int warehouseId) {
        ImportReceiptEntity r = new ImportReceiptEntity();
        r.setId(receiptId);
        r.setWarehouse(warehouse(warehouseId));
        r.setStatus(ImportStatus.DRAFT);
        when(importReceiptRepository.findById(receiptId)).thenReturn(Optional.of(r));
        when(importReceiptRepository.findByIdForUpdate(receiptId)).thenReturn(Optional.of(r)); // đường sửa/hủy
    }

    @Test
    void cannotUpdateOrCancelReceiptOfOtherWarehouse() {
        receiptInWarehouse(10, 2);
        AppException update = assertThrows(AppException.class,
                () -> service.updateReceipt(10, new ImportReceiptUpdateRequest()));
        assertEquals(ErrorCode.ACCESS_DENIED, update.getErrorCode());
        AppException cancel = assertThrows(AppException.class, () -> service.cancelReceipt(10, "x"));
        assertEquals(ErrorCode.ACCESS_DENIED, cancel.getErrorCode());
        assertThrows(AppException.class, () -> service.getReceipt(10));
        verify(importReceiptRepository, never()).save(any());
    }

    @Test
    void cannotCreateReceiptInOtherWarehouse() {
        ImportReceiptCreationRequest request = new ImportReceiptCreationRequest();
        request.setWarehouseId(2);
        AppException e = assertThrows(AppException.class, () -> service.createReceipt(request));
        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(warehouseService, never()).findActiveWarehouse(any());
    }

    @Test
    void listWithoutWarehouseOnlyShowsAssignedOnes() {
        ImportReceiptEntity own = new ImportReceiptEntity();
        own.setWarehouse(warehouse(1));
        ImportReceiptEntity other = new ImportReceiptEntity();
        other.setWarehouse(warehouse(2));
        assertEquals(List.of(own),
                warehouseAccess.filter(List.of(own, other), null, ImportReceiptEntity::getWarehouse));
        // lọc theo kho không được giao: 403
        AppException e = assertThrows(AppException.class, () -> service.getReceipts(2, null, null, null, null));
        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
    }
}
