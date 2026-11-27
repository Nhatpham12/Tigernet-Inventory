package com.tigernet.backend.outbound.service;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;
import com.tigernet.backend.outbound.dto.request.ExportReceiptCreationRequest;
import com.tigernet.backend.outbound.dto.request.ExportReceiptDetailRequest;
import com.tigernet.backend.outbound.dto.request.PaymentRequest;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.support.TestData;
import com.tigernet.backend.user.service.PageAccessPolicy;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Luồng phiếu xuất trên MySQL thật: tạo (kiểm tồn) → duyệt (giữ hàng) → hoàn tất (đã bán); hủy phiếu đã duyệt
 * trả lại hàng; phiếu đã thu tiền không hủy được.
 */
@SpringBootTest
@Transactional
class ExportReceiptFlowIntegrationTest {
    @Autowired ExportReceiptService service;
    @Autowired StockService stockService;
    @Autowired EntityManager em;

    TestData data;
    WarehouseEntity wh;
    ProductEntity bulk;
    ProductEntity serialized;
    Integer customerId;
    InventoryEntity unit;

    @BeforeEach
    void setUp() {
        data = TestData.of(em, "exp_it_");
        data.loginAs(PageAccessPolicy.MANAGER);
        wh = data.warehouse("W");
        bulk = data.product("B", false);
        serialized = data.product("S", true);
        customerId = data.customer("C").getId();
        em.flush();
        StockRef ref = StockRef.of(StockRef.IMPORT, 1);
        stockService.receive(bulk, wh, 5, List.of(), BigDecimal.TEN, null, ref);
        unit = stockService.receive(serialized, wh, 1, List.of(data.tag() + "-SN"), BigDecimal.TEN, null, ref).get(0);
        em.flush();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createConfirmCompleteMovesStockToSold() {
        Integer id = service.createReceipt(request(3, unit.getSerial(), BigDecimal.ZERO)).getId();
        assertSummary(bulk, 5, 0);

        service.confirmReceipt(id);
        assertSummary(bulk, 2, 3);
        assertSummary(serialized, 0, 1);

        // phiếu khác chỉ còn lấy được 2 cái, serial đã giữ thì không chọn lại được
        assertError(ErrorCode.INSUFFICIENT_STOCK, () -> service.createReceipt(request(3, null, BigDecimal.ZERO)));
        assertError(ErrorCode.INVENTORY_STATUS_INVALID,
                () -> service.createReceipt(request(1, unit.getSerial(), BigDecimal.ZERO)));

        assertEquals(ExportStatus.EXPORTED, service.completeReceipt(id).getStatus());
        assertSummary(bulk, 2, 0);
        assertSummary(serialized, 0, 0);
        em.flush();
        em.clear();
        assertEquals(InventoryStatus.SOLD, em.find(InventoryEntity.class, unit.getId()).getStatus());
        assertError(ErrorCode.RECEIPT_NOT_EDITABLE, () -> service.cancelReceipt(id, "x"));
    }

    @Test
    void cancellingConfirmedReceiptReleasesReservation() {
        Integer id = service.createReceipt(request(2, unit.getSerial(), BigDecimal.ZERO)).getId();
        service.confirmReceipt(id);
        assertEquals(ExportStatus.CANCELLED, service.cancelReceipt(id, "khách đổi ý").getStatus());
        assertSummary(bulk, 5, 0);
        assertSummary(serialized, 1, 0);
        em.flush();
        em.clear();
        assertEquals(InventoryStatus.IN_STOCK, em.find(InventoryEntity.class, unit.getId()).getStatus());
    }

    @Test
    void receiptWithPaymentCannotBeCancelled() {
        Integer id = service.createReceipt(request(1, null, BigDecimal.ZERO)).getId();
        service.recordPayment(id, PaymentRequest.builder().amount(BigDecimal.ONE).build());
        assertError(ErrorCode.RECEIPT_HAS_PAYMENTS, () -> service.cancelReceipt(id, "x"));
    }

    @Test
    void serializedLineNeedsUnitAndBulkLineRejectsSerial() {
        ExportReceiptCreationRequest noSerial = base(List.of(line(serialized, 1, null)), BigDecimal.ZERO);
        assertError(ErrorCode.SERIAL_COUNT_MISMATCH, () -> service.createReceipt(noSerial));
        ExportReceiptCreationRequest bulkWithSerial = base(List.of(line(bulk, 1, "X")), BigDecimal.ZERO);
        assertError(ErrorCode.SERIAL_COUNT_MISMATCH, () -> service.createReceipt(bulkWithSerial));
    }

    private void assertSummary(ProductEntity p, int onHand, int reserved) {
        em.flush();
        StockSummaryEntity s = em.find(StockSummaryEntity.class, new StockSummaryId(p.getId(), wh.getId()));
        em.refresh(s);
        assertEquals(onHand, s.getQtyOnHand(), "onHand " + p.getProductName());
        assertEquals(reserved, s.getQtyReserved(), "reserved " + p.getProductName());
    }

    private static void assertError(ErrorCode expected, org.junit.jupiter.api.function.Executable call) {
        assertEquals(expected, assertThrows(AppException.class, call).getErrorCode());
    }

    // bulkQty cái hàng không serial, kèm 1 dòng serial nếu có
    private ExportReceiptCreationRequest request(int bulkQty, String serial, BigDecimal paid) {
        List<ExportReceiptDetailRequest> lines = serial == null
                ? List.of(line(bulk, bulkQty, null))
                : List.of(line(bulk, bulkQty, null), line(serialized, 1, serial));
        return base(lines, paid);
    }

    private ExportReceiptCreationRequest base(List<ExportReceiptDetailRequest> lines, BigDecimal paid) {
        return ExportReceiptCreationRequest.builder().customerId(customerId).warehouseId(wh.getId())
                .paid(paid).details(lines).build();
    }

    private static ExportReceiptDetailRequest line(ProductEntity p, int qty, String serial) {
        return ExportReceiptDetailRequest.builder().productId(p.getId()).quantity(qty)
                .priceOut(new BigDecimal("150")).serial(serial).build();
    }
}
