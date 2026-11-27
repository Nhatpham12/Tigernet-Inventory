package com.tigernet.backend.inbound.service;

import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.inbound.dto.request.ImportReceiptCreationRequest;
import com.tigernet.backend.inbound.dto.request.ImportReceiptDetailRequest;
import com.tigernet.backend.inbound.dto.response.ImportReceiptResponse;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.entity.StockMovementEntity;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.support.TestData;
import com.tigernet.backend.user.service.PageAccessPolicy;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lập phiếu nhập → xác nhận trên MySQL thật: tồn, thẻ kho, serial và liên kết dòng nhập đều đúng. */
@SpringBootTest
@Transactional
class ImportReceiptConfirmIntegrationTest {
    @Autowired ImportReceiptService service;
    @Autowired EntityManager em;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmAddsStockForEveryLine() {
        TestData data = TestData.of(em, "imp_it_");
        data.loginAs(PageAccessPolicy.MANAGER);
        WarehouseEntity wh = data.warehouse("W");
        ProductEntity bulk = data.product("B", false);
        ProductEntity serialized = data.product("S", true);
        Integer supplierParty = data.supplier("N").getParty().getId();
        em.flush();

        ImportReceiptCreationRequest request = ImportReceiptCreationRequest.builder()
                .sourcePartyId(supplierParty).warehouseId(wh.getId()).status(ImportStatus.PENDING)
                .details(List.of(line(bulk, 5, null), line(serialized, 1, data.tag() + "-1"),
                        line(serialized, 1, data.tag() + "-2")))
                .build();
        ImportReceiptResponse created = service.createReceipt(request);
        em.flush();
        em.clear();

        assertEquals(ImportStatus.CONFIRMED, service.confirmReceipt(created.getId()).getStatus());
        em.flush();
        em.clear();

        assertEquals(5, summary(bulk, wh).getQtyOnHand());
        assertEquals(2, summary(serialized, wh).getQtyOnHand());
        List<InventoryEntity> units = em.createQuery(
                "select i from InventoryEntity i where i.product.id = :p", InventoryEntity.class)
                .setParameter("p", serialized.getId()).getResultList();
        assertEquals(2, units.size());
        assertTrue(units.stream().allMatch(u -> u.getStatus() == InventoryStatus.IN_STOCK
                && u.getImportDetail() != null));
        List<StockMovementEntity> card = em.createQuery(
                "select m from StockMovementEntity m where m.referenceType = :t and m.referenceId = :id",
                StockMovementEntity.class)
                .setParameter("t", StockRef.IMPORT).setParameter("id", created.getId()).getResultList();
        assertEquals(3, card.size()); // 1 lô + 2 serial

        AppException again = assertThrows(AppException.class, () -> service.confirmReceipt(created.getId()));
        assertEquals(ErrorCode.RECEIPT_NOT_EDITABLE, again.getErrorCode());
    }

    private StockSummaryEntity summary(ProductEntity p, WarehouseEntity w) {
        return em.find(StockSummaryEntity.class, new StockSummaryId(p.getId(), w.getId()));
    }

    private static ImportReceiptDetailRequest line(ProductEntity p, int qty, String serial) {
        return ImportReceiptDetailRequest.builder().productId(p.getId()).quantity(qty)
                .priceIn(new BigDecimal("100")).serial(serial).build();
    }
}
