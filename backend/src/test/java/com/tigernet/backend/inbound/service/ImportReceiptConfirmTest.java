package com.tigernet.backend.inbound.service;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inbound.dto.request.ImportReceiptDetailRequest;
import com.tigernet.backend.inbound.dto.request.ImportReceiptUpdateRequest;
import com.tigernet.backend.inbound.entity.ImportReceiptDetailEntity;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inbound.mapper.ImportReceiptMapper;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;
import com.tigernet.backend.partner.entity.SupplierEntity;
import com.tigernet.backend.partner.repository.SupplierRepository;
import com.tigernet.backend.partner.service.PartyService;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.service.WarehouseAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Xác nhận nhập kho cộng tồn qua StockService; serial bắt buộc theo SP; bên bán phải là NCC. */
@ExtendWith(MockitoExtension.class)
class ImportReceiptConfirmTest {
    @Mock ImportReceiptRepository importReceiptRepository;
    @Mock ImportReceiptMapper importReceiptMapper;
    @Mock WarehouseAccessService warehouseAccess;
    @Mock StockService stockService;
    @Mock SupplierRepository supplierRepository;
    @Mock PartyService partyService;
    @Mock ProductService productService;
    @InjectMocks ImportReceiptService service;

    private static ProductEntity product(int id, boolean serialized) {
        ProductEntity p = new ProductEntity();
        p.setId(id);
        p.setIsSerialized(serialized);
        return p;
    }

    private static ImportReceiptDetailEntity line(ProductEntity p, int qty, String serial) {
        ImportReceiptDetailEntity d = new ImportReceiptDetailEntity();
        d.setProduct(p);
        d.setQuantity(qty);
        d.setSerial(serial);
        d.setPriceIn(BigDecimal.TEN);
        return d;
    }

    private ImportReceiptEntity receipt(ImportStatus status, ImportReceiptDetailEntity... lines) {
        ImportReceiptEntity r = new ImportReceiptEntity();
        r.setId(7);
        r.setStatus(status);
        r.setWarehouse(new WarehouseEntity());
        r.getDetails().addAll(List.of(lines));
        when(importReceiptRepository.findByIdForUpdate(7)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void confirmReceivesEveryLineInProductOrderAndMarksConfirmed() {
        ProductEntity bulk = product(9, false);
        ProductEntity serialized = product(3, true);
        ImportReceiptDetailEntity bulkLine = line(bulk, 5, null);
        ImportReceiptDetailEntity serialLine = line(serialized, 1, "SN-1");
        ImportReceiptEntity r = receipt(ImportStatus.PENDING, bulkLine, serialLine);
        when(importReceiptRepository.save(r)).thenReturn(r);

        service.confirmReceipt(7);

        StockRef ref = StockRef.of(StockRef.IMPORT, 7);
        InOrder order = inOrder(stockService);
        order.verify(stockService).receive(serialized, r.getWarehouse(), 1, List.of("SN-1"), BigDecimal.TEN,
                serialLine, ref);
        order.verify(stockService).receive(bulk, r.getWarehouse(), 5, List.of(), BigDecimal.TEN, bulkLine, ref);
        assertEquals(ImportStatus.CONFIRMED, r.getStatus());
    }

    @Test
    void confirmedOrCancelledReceiptCannotBeConfirmedAgain() {
        receipt(ImportStatus.CONFIRMED, line(product(1, false), 1, null));
        AppException e = assertThrows(AppException.class, () -> service.confirmReceipt(7));
        assertEquals(ErrorCode.RECEIPT_NOT_EDITABLE, e.getErrorCode());
        verifyNoInteractions(stockService);
    }

    @Test
    void stockErrorLeavesReceiptUnconfirmed() {
        ImportReceiptEntity r = receipt(ImportStatus.DRAFT, line(product(1, true), 1, "SN-DUP"));
        when(stockService.receive(any(), any(), anyInt(), any(), any(), any(), any()))
                .thenThrow(new AppException(ErrorCode.SERIAL_EXISTED));
        assertThrows(AppException.class, () -> service.confirmReceipt(7));
        assertEquals(ImportStatus.DRAFT, r.getStatus());
        verify(importReceiptRepository, never()).save(any());
    }

    @Test
    void serializedProductNeedsSerialAndBulkProductRejectsIt() {
        receipt(ImportStatus.DRAFT);
        when(productService.findProduct(1)).thenReturn(product(1, true));
        when(productService.findProduct(2)).thenReturn(product(2, false));

        ImportReceiptUpdateRequest missingSerial = ImportReceiptUpdateRequest.builder()
                .details(List.of(detail(1, null))).build();
        assertEquals(ErrorCode.SERIAL_COUNT_MISMATCH, assertThrows(AppException.class,
                () -> service.updateReceipt(7, missingSerial)).getErrorCode());

        ImportReceiptUpdateRequest unexpectedSerial = ImportReceiptUpdateRequest.builder()
                .details(List.of(detail(2, "SN-X"))).build();
        assertEquals(ErrorCode.SERIAL_COUNT_MISMATCH, assertThrows(AppException.class,
                () -> service.updateReceipt(7, unexpectedSerial)).getErrorCode());
    }

    @Test
    void sellerMustBeActiveSupplier() {
        receipt(ImportStatus.DRAFT);
        when(supplierRepository.findByPartyId(4)).thenReturn(Optional.empty());
        SupplierEntity deleted = new SupplierEntity();
        deleted.setIsDeleted(true);
        when(supplierRepository.findByPartyId(5)).thenReturn(Optional.of(deleted));

        for (int partyId : new int[]{4, 5}) {
            ImportReceiptUpdateRequest request = ImportReceiptUpdateRequest.builder().sourcePartyId(partyId).build();
            assertEquals(ErrorCode.SOURCE_NOT_SUPPLIER, assertThrows(AppException.class,
                    () -> service.updateReceipt(7, request)).getErrorCode());
        }
        verify(importReceiptRepository, never()).save(any());
        verify(partyService).findParty(eq(4));
    }

    private static ImportReceiptDetailRequest detail(int productId, String serial) {
        ImportReceiptDetailRequest d = new ImportReceiptDetailRequest();
        d.setProductId(productId);
        d.setQuantity(1);
        d.setPriceIn(BigDecimal.TEN);
        d.setSerial(serial);
        return d;
    }
}
