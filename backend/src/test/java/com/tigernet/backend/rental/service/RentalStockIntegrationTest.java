package com.tigernet.backend.rental.service;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;
import com.tigernet.backend.rental.dto.request.RentalReceiptCreationRequest;
import com.tigernet.backend.rental.dto.request.RentalReceiptDetailRequest;
import com.tigernet.backend.rental.dto.request.ReturnReceiptCreationRequest;
import com.tigernet.backend.rental.dto.request.ReturnReceiptDetailRequest;
import com.tigernet.backend.rental.dto.response.RentalReceiptResponse;
import com.tigernet.backend.rental.enums.RentalType;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Thuê / mượn / trả trên MySQL thật: lập phiếu chuyển hàng sang RENTED/LENT, hủy trả về kho,
 * phiếu trả đưa hàng về kho nhận (có thể khác kho), hàng hỏng thành DAMAGED.
 */
@SpringBootTest
@Transactional
class RentalStockIntegrationTest {
    @Autowired RentalReceiptService rentalService;
    @Autowired ReturnReceiptService returnService;
    @Autowired StockService stockService;
    @Autowired EntityManager em;

    TestData data;
    WarehouseEntity wh;
    WarehouseEntity other;
    ProductEntity bulk;
    ProductEntity serialized;
    Integer customerId;
    InventoryEntity unit;

    @BeforeEach
    void setUp() {
        data = TestData.of(em, "rent_it_");
        data.loginAs(PageAccessPolicy.MANAGER);
        wh = data.warehouse("W");
        other = data.warehouse("X");
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
    void rentThenReturnPartlyDamagedToAnotherWarehouse() {
        RentalReceiptResponse rental = rentalService.createReceipt(rent(RentalType.RENT, 4));
        assertSummary(bulk, wh, 1, 4);
        assertSummary(serialized, wh, 0, 1);
        assertUnit(InventoryStatus.RENTED, wh);

        // serial đang cho thuê thì không cho thuê lại được
        AppException again = assertThrows(AppException.class, () -> rentalService.createReceipt(rent(RentalType.RENT, 1)));
        assertEquals(ErrorCode.INVENTORY_STATUS_INVALID, again.getErrorCode());

        Integer bulkLine = lineId(rental, bulk);
        Integer serialLine = lineId(rental, serialized);
        // lần 1: trả 3 cái không serial về kho khác, 1 trong số đó hỏng
        returnService.createReceipt(giveBack(rental.getId(), other,
                List.of(back(bulkLine, 2, false), back(bulkLine, 1, true))));
        assertSummary(bulk, wh, 1, 1);
        StockSummaryEntity otherBulk = summary(bulk, other);
        assertEquals(2, otherBulk.getQtyOnHand());
        assertEquals(1, otherBulk.getQtyDamaged());

        // lần 2: trả nốt, serial hỏng về kho gốc
        returnService.createReceipt(giveBack(rental.getId(), wh,
                List.of(back(bulkLine, 1, false), back(serialLine, 1, true))));
        assertSummary(bulk, wh, 2, 0);
        StockSummaryEntity serialSummary = summary(serialized, wh);
        assertEquals(0, serialSummary.getQtyRented());
        assertEquals(1, serialSummary.getQtyDamaged());
        assertUnit(InventoryStatus.DAMAGED, wh);
    }

    @Test
    void lendUsesLentAndCancelReturnsStock() {
        RentalReceiptResponse lend = rentalService.createReceipt(rent(RentalType.LEND, 2));
        assertUnit(InventoryStatus.LENT, wh);
        assertSummary(bulk, wh, 3, 2); // Qty_Rented gom cả hàng thuê và mượn

        rentalService.cancelReceipt(lend.getId());
        assertSummary(bulk, wh, 5, 0);
        assertSummary(serialized, wh, 1, 0);
        assertUnit(InventoryStatus.IN_STOCK, wh);
    }

    @Test
    void rentingMoreThanAvailableIsRejected() {
        AppException e = assertThrows(AppException.class, () -> rentalService.createReceipt(rent(RentalType.RENT, 6)));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, e.getErrorCode());
    }

    private RentalReceiptCreationRequest rent(RentalType type, int bulkQty) {
        return RentalReceiptCreationRequest.builder().customerId(customerId).warehouseId(wh.getId())
                .rentalType(type).dueDate(LocalDateTime.now().plusDays(7))
                .details(List.of(
                        RentalReceiptDetailRequest.builder().productId(bulk.getId()).quantity(bulkQty)
                                .rentalPrice(BigDecimal.ONE).build(),
                        RentalReceiptDetailRequest.builder().productId(serialized.getId()).quantity(1)
                                .rentalPrice(BigDecimal.ONE).serial(unit.getSerial()).build()))
                .build();
    }

    private static ReturnReceiptCreationRequest giveBack(Integer rentalId, WarehouseEntity to,
                                                         List<ReturnReceiptDetailRequest> lines) {
        return ReturnReceiptCreationRequest.builder().rentalReceiptId(rentalId).warehouseId(to.getId())
                .details(lines).build();
    }

    private static ReturnReceiptDetailRequest back(Integer rentalDetailId, int qty, boolean damaged) {
        return ReturnReceiptDetailRequest.builder().rentalDetailId(rentalDetailId).quantity(qty)
                .damaged(damaged).build();
    }

    private static Integer lineId(RentalReceiptResponse rental, ProductEntity p) {
        return rental.getDetails().stream().filter(d -> d.getProductId().equals(p.getId()))
                .findFirst().orElseThrow().getId();
    }

    private void assertSummary(ProductEntity p, WarehouseEntity w, int onHand, int rented) {
        StockSummaryEntity s = summary(p, w);
        assertEquals(onHand, s.getQtyOnHand(), "onHand " + p.getProductName());
        assertEquals(rented, s.getQtyRented(), "rented " + p.getProductName());
    }

    private StockSummaryEntity summary(ProductEntity p, WarehouseEntity w) {
        em.flush();
        StockSummaryEntity s = em.find(StockSummaryEntity.class, new StockSummaryId(p.getId(), w.getId()));
        em.refresh(s);
        return s;
    }

    private void assertUnit(InventoryStatus status, WarehouseEntity w) {
        em.flush();
        InventoryEntity u = em.find(InventoryEntity.class, unit.getId());
        em.refresh(u);
        assertEquals(status, u.getStatus());
        assertEquals(w.getId(), u.getWarehouse().getId());
    }
}
